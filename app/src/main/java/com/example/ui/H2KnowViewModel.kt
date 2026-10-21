package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Contaminant
import com.example.data.model.PlaceInfo
import com.example.data.model.TrendTimeframe
import com.example.data.model.WaterSystemReport
import com.example.data.model.WaterSystemSummary
import com.example.data.remote.LocationHistoryDocument
import com.example.data.remote.WaterIssueDocument
import com.example.data.local.WatchedSystemEntity
import com.example.data.repository.FirestoreWaterRepository
import com.example.data.repository.MonetizationConfig
import com.example.data.repository.ReferenceWaterData
import com.example.data.repository.WaterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FourWTab(
    val key: String,
    val label: String,
    val badge: String,
    val shortTitle: String
) {
    WHO("WHO", "Overview", "OVERVIEW", "Score & Local Reports"),
    WHERE("WHERE", "Source", "WATER SOURCE", "Origin, Treatment & Drought"),
    WHY("WHY", "Quality", "WATER QUALITY", "Contaminants & Filters"),
    WHEN("WHEN", "History", "HISTORY", "1Y / 5Y / 10Y Trends")
}

sealed interface CloudDataState<out T> {
    data object Loading : CloudDataState<Nothing>
    data object GuestRequired : CloudDataState<Nothing>
    data class Success<T>(val data: T) : CloudDataState<T>
    data class Error(val message: String) : CloudDataState<Nothing>
}

data class H2KnowUiState(
    val screen: String = "search", // "search" or "results"
    val zipQuery: String = "",
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val currentPlace: PlaceInfo? = null,
    val providers: List<WaterSystemSummary> = emptyList(),
    val isLoadingSystem: Boolean = false,
    val systemError: String? = null,
    val activeReport: WaterSystemReport? = null,
    val activeFourWTab: FourWTab = FourWTab.WHO,
    val selectedWhyCategoryFilter: String = "ALL", // "ALL", "EXCEEDS_PHG", "PFAS", "METALS", "DBP"
    val selectedWhyContaminantCode: String = "PB90",
    val selectedWhenTimeframe: TrendTimeframe = TrendTimeframe.TEN_YEAR,
    val selectedWhenPointIndex: Int? = null,
    val showReportIssueDialog: Boolean = false,
    val reportIssuePrefillContaminant: Contaminant? = null,
    val issueStatusFilter: String = "all", // "all", "open", "resolved"
    val cloudStatusBanner: String? = null,
    val isSavingHistory: Boolean = false
)

class H2KnowViewModel(
    private val waterRepository: WaterRepository,
    private val firestoreRepository: FirestoreWaterRepository,
    private val currentUserId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(H2KnowUiState())
    val uiState: StateFlow<H2KnowUiState> = _uiState.asStateFlow()

    val isGuestMode: Boolean
        get() = currentUserId == null

    val userPreferences = waterRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val watchedSystems: StateFlow<List<WatchedSystemEntity>> = waterRepository.watchedSystems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val cloudIssuesState: StateFlow<CloudDataState<List<WaterIssueDocument>>> =
        if (currentUserId == null) {
            flowOf<CloudDataState<List<WaterIssueDocument>>>(CloudDataState.GuestRequired)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = CloudDataState.GuestRequired
                )
        } else {
            firestoreRepository.observeUserWaterIssues(currentUserId)
                .map<List<WaterIssueDocument>, CloudDataState<List<WaterIssueDocument>>> {
                    CloudDataState.Success(it)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing water issues from Firestore", error)
                    emit(CloudDataState.Error(error.message ?: "Could not load cloud water issues"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = CloudDataState.Loading
                )
        }

    val cloudLocationHistoryState: StateFlow<CloudDataState<List<LocationHistoryDocument>>> =
        if (currentUserId == null) {
            flowOf<CloudDataState<List<LocationHistoryDocument>>>(CloudDataState.GuestRequired)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = CloudDataState.GuestRequired
                )
        } else {
            firestoreRepository.observeUserLocationHistory(currentUserId)
                .map<List<LocationHistoryDocument>, CloudDataState<List<LocationHistoryDocument>>> {
                    CloudDataState.Success(it)
                }
                .catch { error ->
                    Log.w(TAG, "Error observing location history from Firestore", error)
                    emit(CloudDataState.Error(error.message ?: "Could not load cloud location history"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = CloudDataState.Loading
                )
        }

    init {
        viewModelScope.launch {
            waterRepository.ensureSeedData()
        }
    }

    fun onZipInputChange(input: String) {
        val digits = input.filter { it.isDigit() }.take(5)
        val prev = _uiState.value.zipQuery
        _uiState.update { it.copy(zipQuery = digits, searchError = null) }
        if (digits.length == 5 && digits != prev) {
            searchZip(digits, autoSelectPrimary = false)
        }
    }

    fun searchZip(zipOverride: String? = null, autoSelectPrimary: Boolean = true) {
        val targetZip = (zipOverride ?: _uiState.value.zipQuery).filter { it.isDigit() }.take(5)
        if (targetZip.length != 5) {
            _uiState.update {
                it.copy(
                    searchError = "Enter a valid 5-digit U.S. ZIP code (e.g., 78701, 48502, 90210).",
                    screen = "search"
                )
            }
            return
        }

        // If providers for this exact ZIP are already loaded and user pressed Inspect, jump right into the report!
        val currentState = _uiState.value
        if (autoSelectPrimary && currentState.currentPlace?.zip == targetZip && currentState.providers.isNotEmpty()) {
            val primary = currentState.providers.firstOrNull { it.isPrimary } ?: currentState.providers.first()
            selectProvider(primary.pwsid, targetZip)
            return
        }

        _uiState.update {
            it.copy(
                zipQuery = targetZip,
                isSearching = true,
                searchError = null,
                screen = if (autoSelectPrimary) "results" else "search",
                isLoadingSystem = autoSelectPrimary,
                systemError = null
            )
        }
        viewModelScope.launch {
            runCatching {
                waterRepository.searchProvidersByZip(targetZip)
            }.onSuccess { (place, systems) ->
                if (systems.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            screen = "search",
                            isSearching = false,
                            isLoadingSystem = false,
                            currentPlace = place,
                            providers = emptyList(),
                            searchError = "No community water providers found near ${place.city}, ${place.state}."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            currentPlace = place,
                            providers = systems,
                            searchError = null
                        )
                    }
                    if (autoSelectPrimary) {
                        val primary = systems.firstOrNull { it.isPrimary } ?: systems.first()
                        selectProvider(primary.pwsid, place.zip)
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        screen = "search",
                        isSearching = false,
                        isLoadingSystem = false,
                        searchError = err.message ?: "Could not look up that ZIP right now."
                    )
                }
            }
        }
    }

    fun selectProvider(pwsid: String, zipHint: String? = null, autoLogHistory: Boolean = true) {
        val currentPlace = _uiState.value.currentPlace
            ?: zipHint?.let { ReferenceWaterData.placeFallback(it) }
        _uiState.update {
            it.copy(
                screen = "results",
                activeFourWTab = FourWTab.WHO,
                selectedWhyCategoryFilter = "ALL",
                isLoadingSystem = true,
                systemError = null,
                selectedWhenPointIndex = null,
                cloudStatusBanner = null
            )
        }
        viewModelScope.launch {
            runCatching {
                waterRepository.loadSystemReport(pwsid, currentPlace)
            }.onSuccess { report ->
                val defaultCont = report.contaminants.firstOrNull { it.exceedsHealthGoal }?.code
                    ?: report.contaminants.firstOrNull()?.code
                    ?: "PB90"
                _uiState.update { state ->
                    state.copy(
                        isLoadingSystem = false,
                        activeReport = report,
                        providers = state.providers.map { prov ->
                            if (prov.pwsid == report.pwsid) prov.copy(currentScore = report.qualityScore) else prov
                        },
                        selectedWhyContaminantCode = defaultCont,
                        systemError = null
                    )
                }
                noteWatchChanges(report)
                if (autoLogHistory && currentUserId != null && report.qualityScore >= 0) {
                    val existingForPwsid = (cloudLocationHistoryState.value as? CloudDataState.Success)
                        ?.data
                        ?.any { it.pwsid == report.pwsid } == true
                    if (!existingForPwsid) {
                        firestoreRepository.recordLocationHistoryFromReport(report)
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingSystem = false,
                        systemError = err.message ?: "Could not load that water system right now."
                    )
                }
            }
        }
    }

    fun toggleWatchCurrent() {
        val report = _uiState.value.activeReport ?: return
        viewModelScope.launch {
            val current = watchedSystems.value.find { it.pwsid == report.pwsid }
            if (current == null && watchedSystems.value.size >= MonetizationConfig.savedAddressLimit) {
                _uiState.update {
                    it.copy(
                        cloudStatusBanner = if (MonetizationConfig.checkoutConfigured) {
                            "Saved addresses are full (${MonetizationConfig.savedAddressLimit}). Remove one to add another."
                        } else {
                            "One saved address is included. Set STRIPE_PAYMENT_LINK in config.js to unlock more."
                        }
                    )
                }
                return@launch
            }
            waterRepository.toggleWatchSystem(report, current)
            _uiState.update {
                it.copy(
                    cloudStatusBanner = if (current == null) {
                        "Saved on this device. Open it again later to see if the score or open violations changed."
                    } else {
                        "Removed from saved addresses."
                    }
                )
            }
        }
    }

    private fun noteWatchChanges(report: WaterSystemReport) {
        val prev = watchedSystems.value.find { it.pwsid == report.pwsid } ?: return
        val openNow = report.violations.count { it.status == "open" }
        val notes = mutableListOf<String>()
        if (prev.lastCheckedScore >= 0 && report.qualityScore >= 0 && prev.lastCheckedScore != report.qualityScore) {
            notes.add("Score changed from ${prev.lastCheckedScore} to ${report.qualityScore} since you saved this address.")
        }
        if (prev.openViolationCount != openNow) {
            notes.add("Open violations changed from ${prev.openViolationCount} to $openNow.")
        }
        if (notes.isNotEmpty()) {
            _uiState.update { it.copy(cloudStatusBanner = notes.joinToString(" ")) }
        }
        if (prev.lastCheckedScore != report.qualityScore || prev.openViolationCount != openNow) {
            viewModelScope.launch {
                waterRepository.updateWatchPreferences(
                    prev.copy(lastCheckedScore = report.qualityScore, openViolationCount = openNow)
                )
            }
        }
    }

    fun selectFourWTab(tab: FourWTab) {
        _uiState.update { it.copy(activeFourWTab = tab) }
    }

    fun selectWhyCategoryFilter(filterKey: String) {
        val report = _uiState.value.activeReport
        val filtered = report?.contaminants?.filter { c ->
            when (filterKey) {
                "EXCEEDS_PHG" -> c.exceedsHealthGoal
                "PFAS" -> c.pfas || c.category.equals("PFAS", ignoreCase = true)
                "METALS" -> c.category.equals("Metals", ignoreCase = true)
                "DBP" -> c.category.contains("Byproduct", ignoreCase = true)
                else -> true
            }
        }.orEmpty()
        val nextSelected = if (filtered.any { it.code == _uiState.value.selectedWhyContaminantCode }) {
            _uiState.value.selectedWhyContaminantCode
        } else {
            filtered.firstOrNull()?.code ?: _uiState.value.selectedWhyContaminantCode
        }
        _uiState.update {
            it.copy(
                selectedWhyCategoryFilter = filterKey,
                selectedWhyContaminantCode = nextSelected
            )
        }
    }

    fun saveCurrentLocationSnapshotToCloud() {
        if (currentUserId == null) {
            _uiState.update {
                it.copy(cloudStatusBanner = "Sign in to save water reports to your account.")
            }
            return
        }
        val report = _uiState.value.activeReport ?: return
        _uiState.update { it.copy(isSavingHistory = true, cloudStatusBanner = null) }
        viewModelScope.launch {
            val result = firestoreRepository.recordLocationHistoryFromReport(report)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavingHistory = false,
                        cloudStatusBanner = "Saved ${report.name} (${report.zip}) to your saved locations."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSavingHistory = false,
                        cloudStatusBanner = err.localizedMessage ?: "Failed to save location snapshot."
                    )
                }
            }
        }
    }

    fun deleteLocationHistorySnapshot(historyId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            firestoreRepository.deleteLocationHistory(historyId)
        }
    }

    fun backToSearch() {
        _uiState.update {
            it.copy(
                screen = "search",
                activeFourWTab = FourWTab.WHO,
                systemError = null,
                cloudStatusBanner = null
            )
        }
    }

    fun selectWhyContaminant(code: String) {
        _uiState.update {
            it.copy(
                selectedWhyContaminantCode = code,
                selectedWhenPointIndex = null
            )
        }
    }

    fun selectWhenTimeframe(tf: TrendTimeframe) {
        _uiState.update {
            it.copy(
                selectedWhenTimeframe = tf,
                selectedWhenPointIndex = null
            )
        }
    }

    fun selectWhenTimelinePoint(index: Int?) {
        _uiState.update { it.copy(selectedWhenPointIndex = index) }
    }

    fun setIssueStatusFilter(filter: String) {
        _uiState.update { it.copy(issueStatusFilter = filter) }
    }

    fun setReportIssueDialogOpen(open: Boolean, prefillContaminant: Contaminant? = null) {
        if (open && currentUserId == null) {
            _uiState.update {
                it.copy(cloudStatusBanner = "Sign in with Google to report and sync water quality issues.")
            }
            return
        }
        _uiState.update {
            it.copy(
                showReportIssueDialog = open,
                reportIssuePrefillContaminant = if (open) prefillContaminant else null
            )
        }
    }

    fun dismissCloudBanner() {
        _uiState.update { it.copy(cloudStatusBanner = null) }
    }

    fun submitCloudWaterIssue(
        category: String,
        title: String,
        description: String,
        severity: String
    ) {
        if (currentUserId == null) return
        val report = _uiState.value.activeReport ?: return
        viewModelScope.launch {
            val res = firestoreRepository.createWaterIssue(
                pwsid = report.pwsid,
                systemName = report.name,
                zip = report.zip,
                category = category,
                title = title,
                description = description,
                severity = severity
            )
            res.onSuccess {
                _uiState.update {
                    it.copy(
                        activeFourWTab = FourWTab.WHO,
                        showReportIssueDialog = false,
                        reportIssuePrefillContaminant = null,
                        cloudStatusBanner = "Your local water report has been posted."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        showReportIssueDialog = false,
                        reportIssuePrefillContaminant = null,
                        cloudStatusBanner = err.localizedMessage ?: "Failed to save water quality issue."
                    )
                }
            }
        }
    }

    fun upvoteCloudWaterIssue(issue: WaterIssueDocument) {
        if (currentUserId == null) return
        viewModelScope.launch {
            firestoreRepository.incrementIssueAffectedCount(issue)
        }
    }

    fun resolveCloudWaterIssue(issue: WaterIssueDocument) {
        if (currentUserId == null) return
        viewModelScope.launch {
            firestoreRepository.resolveWaterIssue(issue)
        }
    }

    fun deleteCloudWaterIssue(issueId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            firestoreRepository.deleteWaterIssue(issueId)
        }
    }

    fun toggleTheme() {
        val currentDark = userPreferences.value?.isDarkTheme ?: true
        viewModelScope.launch {
            waterRepository.updateThemePreference(!currentDark)
        }
    }

    fun markInitialAuthPromptSeen() {
        viewModelScope.launch {
            waterRepository.markAuthPromptSeen()
        }
    }

    private companion object {
        const val TAG = "H2KnowVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
