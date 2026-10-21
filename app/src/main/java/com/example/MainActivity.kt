package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.H2KnowDatabase
import com.example.data.local.UserPreferenceEntity
import com.example.data.repository.FirestoreWaterRepository
import com.example.data.repository.WaterRepository
import com.example.ui.FourWTab
import com.example.ui.H2KnowViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.components.FloatingFourWDock
import com.example.ui.components.H2KnowPrivacyAndEpaModal
import com.example.ui.components.H2KnowSearchAndCloudHome
import com.example.ui.components.H2KnowStreamlinedTopBar
import com.example.ui.components.H2KnowStudioFooter
import com.example.ui.components.ReportCloudIssueDialog
import com.example.ui.components.WhoWhereWhyWhenDashboard
import com.example.ui.theme.H2KnowTheme
import com.example.ui.theme.H2KnowThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(context) { H2KnowDatabase.getInstance(context).dao() }
    val userPrefs by dao.observeUserPreferences()
        .collectAsStateWithLifecycle(initialValue = UserPreferenceEntity(hasSeenAuthPrompt = true))

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showExplicitSignInPrompt by rememberSaveable { mutableStateOf(false) }

    val user = currentUser
    val hasSeenInitialPrompt = userPrefs?.hasSeenAuthPrompt ?: true

    if (user == null && (!hasSeenInitialPrompt || showExplicitSignInPrompt)) {
        H2KnowTheme(darkTheme = userPrefs?.isDarkTheme ?: true) {
            AuthScreen(
                onAuthSuccess = {
                    showExplicitSignInPrompt = false
                    scope.launch {
                        val current = dao.getUserPreferencesOnce() ?: UserPreferenceEntity()
                        dao.saveUserPreferences(current.copy(hasSeenAuthPrompt = true))
                    }
                },
                onContinueAsGuest = {
                    showExplicitSignInPrompt = false
                    scope.launch {
                        val current = dao.getUserPreferencesOnce() ?: UserPreferenceEntity()
                        dao.saveUserPreferences(current.copy(hasSeenAuthPrompt = true))
                    }
                }
            )
        }
    } else {
        H2KnowMainWorkspace(
            currentUser = user,
            onRequestSignIn = {
                showExplicitSignInPrompt = true
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun H2KnowMainWorkspace(
    currentUser: FirebaseUser?,
    onRequestSignIn: () -> Unit
) {
    val currentUserId = currentUser?.uid
    val viewModel: H2KnowViewModel = viewModel(
        key = currentUserId ?: "guest_mode",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val roomDb = H2KnowDatabase.getInstance(app)
                H2KnowViewModel(
                    waterRepository = WaterRepository(roomDb.dao()),
                    firestoreRepository = FirestoreWaterRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val cloudIssuesState by viewModel.cloudIssuesState.collectAsStateWithLifecycle()
    val cloudHistoryState by viewModel.cloudLocationHistoryState.collectAsStateWithLifecycle()

    val isDarkTheme = userPrefs?.isDarkTheme ?: true
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    var showPrivacyAndEpaModal by rememberSaveable { mutableStateOf(false) }

    // Automatically hide keyboard and reset scroll to top when opening results or switching water systems
    LaunchedEffect(uiState.screen, uiState.activeReport?.pwsid) {
        if (uiState.screen == "results") {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
        scrollState.scrollTo(0)
    }

    H2KnowTheme(darkTheme = isDarkTheme) {
        val palette = H2KnowThemeTokens.colors
        val isResultsScreen = uiState.screen == "results"

        BackHandler(enabled = isResultsScreen) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (uiState.activeFourWTab != FourWTab.WHO) {
                viewModel.selectFourWTab(FourWTab.WHO)
                scope.launch { scrollState.animateScrollTo(0) }
            } else {
                viewModel.backToSearch()
            }
        }

        val headerTitle = when {
            !isResultsScreen -> "H2Know"
            uiState.isLoadingSystem -> "Loading ZIP ${uiState.zipQuery}…"
            uiState.activeReport != null -> uiState.activeReport!!.name
            else -> "H2Know"
        }
        val headerSubtitle = uiState.activeReport?.let {
            "${uiState.activeFourWTab.badge} · ${uiState.activeFourWTab.shortTitle}"
        }

        Scaffold(
            containerColor = palette.surface0,
            topBar = {
                Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    H2KnowStreamlinedTopBar(
                        title = headerTitle,
                        subtitle = headerSubtitle,
                        showBackButton = isResultsScreen,
                        isDarkTheme = isDarkTheme,
                        userEmail = currentUser?.email,
                        isGuestMode = currentUser == null,
                        onBackClick = { viewModel.backToSearch() },
                        onToggleTheme = { viewModel.toggleTheme() },
                        onSignInClick = onRequestSignIn,
                        onSignOutClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        }
                    )
                }
            },
            bottomBar = {
                if (isResultsScreen && uiState.activeReport != null) {
                    FloatingFourWDock(
                        activeTab = uiState.activeFourWTab,
                        isGuestMode = currentUser == null,
                        onSelectTab = { tab ->
                            viewModel.selectFourWTab(tab)
                            scope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        },
                        onQuickActionClick = {
                            if (currentUser == null) {
                                onRequestSignIn()
                            } else {
                                viewModel.setReportIssueDialogOpen(true, null)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(palette.brand.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.88f, size.height * 0.12f),
                                radius = size.width * 0.65f
                            ),
                            radius = size.width * 0.65f,
                            center = Offset(size.width * 0.88f, size.height * 0.12f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(palette.safe.copy(alpha = 0.12f), Color.Transparent),
                                center = Offset(size.width * 0.12f, size.height * 0.78f),
                                radius = size.width * 0.6f
                            ),
                            radius = size.width * 0.6f,
                            center = Offset(size.width * 0.12f, size.height * 0.78f)
                        )
                    }
                    .padding(innerPadding)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isResultsScreen) {
                    Box(modifier = Modifier.widthIn(max = 840.dp)) {
                        H2KnowSearchAndCloudHome(
                            zipQuery = uiState.zipQuery,
                            isSearching = uiState.isSearching,
                            searchError = uiState.searchError,
                            currentPlace = uiState.currentPlace,
                            providers = uiState.providers,
                            cloudHistoryState = cloudHistoryState,
                            cloudIssuesState = cloudIssuesState,
                            onZipChange = { viewModel.onZipInputChange(it) },
                            onSubmitSearch = { overrideZip ->
                                viewModel.searchZip(overrideZip, autoSelectPrimary = true)
                            },
                            onSelectProvider = { pwsid, zipHint ->
                                viewModel.selectProvider(pwsid, zipHint)
                            },
                            onDeleteHistory = { viewModel.deleteLocationHistorySnapshot(it) },
                            onDeleteIssue = { viewModel.deleteCloudWaterIssue(it) },
                            onRequestSignIn = onRequestSignIn
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when {
                            uiState.isLoadingSystem -> {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = palette.surface1,
                                    shadowElevation = 16.dp,
                                    border = BorderStroke(1.dp, palette.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(36.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = palette.brand,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Opening 4W Water Report for ZIP ${uiState.zipQuery}…",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.ink,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            uiState.systemError != null -> {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = palette.dangerBg,
                                    border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Text(
                                            text = uiState.systemError!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.danger
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { viewModel.backToSearch() },
                                                colors = ButtonDefaults.buttonColors(containerColor = palette.brand)
                                            ) {
                                                Text("Back to Search", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            uiState.activeReport != null -> {
                                val report = uiState.activeReport!!

                                // Compact single-line dropdown / embedded list switcher when multiple utilities serve this ZIP
                                if (uiState.providers.size > 1) {
                                    var isProviderDropdownOpen by rememberSaveable(report.zip) { mutableStateOf(false) }
                                    val activeIdx = uiState.providers.indexOfFirst { it.pwsid == report.pwsid }
                                        .let { if (it >= 0) it + 1 else 1 }

                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = palette.surface1.copy(alpha = 0.96f),
                                        shadowElevation = 10.dp,
                                        border = BorderStroke(1.dp, palette.borderStrong),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    isProviderDropdownOpen = !isProviderDropdownOpen
                                                },
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "LOCAL PROVIDER ($activeIdx OF ${uiState.providers.size} IN ZIP ${report.zip})",
                                                            fontFamily = JetBrainsMonoFamily,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = palette.brand
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = report.name,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = palette.ink,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(999.dp),
                                                        color = if (isProviderDropdownOpen) palette.brand else palette.surface2,
                                                        border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.55f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = if (isProviderDropdownOpen) "Hide List ▲" else "Switch (${uiState.providers.size}) ▾",
                                                                fontFamily = JetBrainsMonoFamily,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isProviderDropdownOpen) Color.White else palette.brand
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (isProviderDropdownOpen) {
                                                val dropdownScroll = rememberScrollState()
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = palette.surface0,
                                                    border = BorderStroke(1.dp, palette.border),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 220.dp)
                                                            .verticalScroll(dropdownScroll)
                                                            .padding(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        uiState.providers.forEachIndexed { idx, sys ->
                                                            val isCurrent = sys.pwsid == report.pwsid
                                                            val scoreColor = when {
                                                                sys.currentScore >= 80 -> palette.safe
                                                                sys.currentScore >= 60 -> palette.caution
                                                                else -> palette.danger
                                                            }
                                                            Surface(
                                                                onClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    isProviderDropdownOpen = false
                                                                    if (!isCurrent) {
                                                                        viewModel.selectProvider(sys.pwsid, sys.zip)
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = if (isCurrent) palette.brandSoft else palette.surface2,
                                                                border = BorderStroke(
                                                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                                                    color = if (isCurrent) palette.brand else palette.border
                                                                ),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = "${idx + 1}. ${sys.name}",
                                                                                style = MaterialTheme.typography.labelLarge,
                                                                                color = if (isCurrent) palette.brand else palette.ink,
                                                                                fontWeight = FontWeight.Bold,
                                                                                maxLines = 1,
                                                                                overflow = TextOverflow.Ellipsis,
                                                                                modifier = Modifier.weight(1f, fill = false)
                                                                            )
                                                                            if (idx == 0) {
                                                                                Surface(
                                                                                    shape = RoundedCornerShape(6.dp),
                                                                                    color = palette.safeBg
                                                                                ) {
                                                                                    Text(
                                                                                        text = "PRIMARY",
                                                                                        fontFamily = JetBrainsMonoFamily,
                                                                                        fontSize = 9.sp,
                                                                                        fontWeight = FontWeight.Bold,
                                                                                        color = palette.safe,
                                                                                        maxLines = 1,
                                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                        Text(
                                                                            text = "Serves ${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(sys.population)} · ${sys.sourceType} · ${sys.pwsid}",
                                                                            fontFamily = JetBrainsMonoFamily,
                                                                            fontSize = 10.sp,
                                                                            color = palette.ink2,
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = scoreColor.copy(alpha = 0.16f),
                                                                        border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f))
                                                                    ) {
                                                                        Text(
                                                                            text = "${sys.currentScore}",
                                                                            fontFamily = JetBrainsMonoFamily,
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.ExtraBold,
                                                                            color = scoreColor,
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                WhoWhereWhyWhenDashboard(
                                    report = report,
                                    activeTab = uiState.activeFourWTab,
                                    selectedWhyCategoryFilter = uiState.selectedWhyCategoryFilter,
                                    selectedWhyContaminantCode = uiState.selectedWhyContaminantCode,
                                    selectedWhenTimeframe = uiState.selectedWhenTimeframe,
                                    selectedWhenPointIndex = uiState.selectedWhenPointIndex,
                                    issueStatusFilter = uiState.issueStatusFilter,
                                    cloudIssuesState = cloudIssuesState,
                                    cloudHistoryState = cloudHistoryState,
                                    cloudStatusBanner = uiState.cloudStatusBanner,
                                    isSavingHistory = uiState.isSavingHistory,
                                    onSelectTab = { tab ->
                                        viewModel.selectFourWTab(tab)
                                        scope.launch { scrollState.animateScrollTo(0) }
                                    },
                                    onSelectWhyCategoryFilter = { viewModel.selectWhyCategoryFilter(it) },
                                    onSelectWhyContaminant = { viewModel.selectWhyContaminant(it) },
                                    onSelectWhenTimeframe = { viewModel.selectWhenTimeframe(it) },
                                    onSelectWhenPoint = { viewModel.selectWhenTimelinePoint(it) },
                                    onSelectIssueStatusFilter = { viewModel.setIssueStatusFilter(it) },
                                    onSaveSnapshotToCloud = { viewModel.saveCurrentLocationSnapshotToCloud() },
                                    onOpenReportIssueModal = { cont ->
                                        viewModel.setReportIssueDialogOpen(true, cont)
                                    },
                                    onUpvoteIssue = { viewModel.upvoteCloudWaterIssue(it) },
                                    onResolveIssue = { viewModel.resolveCloudWaterIssue(it) },
                                    onDeleteIssue = { viewModel.deleteCloudWaterIssue(it) },
                                    onDismissBanner = { viewModel.dismissCloudBanner() },
                                    onRequestSignIn = onRequestSignIn
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Independent Umbrella Studio Footer (Aegis Civic Labs · Privacy & EPA Disclaimer · Share · Support)
                Box(modifier = Modifier.widthIn(max = 840.dp)) {
                    H2KnowStudioFooter(
                        activeReport = if (isResultsScreen) uiState.activeReport else null,
                        onOpenPrivacyModal = { showPrivacyAndEpaModal = true }
                    )
                }

                Spacer(modifier = Modifier.height(if (isResultsScreen) 96.dp else 24.dp))
            }

            if (showPrivacyAndEpaModal) {
                H2KnowPrivacyAndEpaModal(
                    onDismiss = { showPrivacyAndEpaModal = false }
                )
            }

            if (uiState.showReportIssueDialog && uiState.activeReport != null && currentUser != null) {
                val report = uiState.activeReport!!
                ReportCloudIssueDialog(
                    systemName = report.name,
                    zip = report.zip,
                    prefillContaminant = uiState.reportIssuePrefillContaminant,
                    onDismiss = { viewModel.setReportIssueDialogOpen(false) },
                    onSubmit = { category, title, description, severity ->
                        viewModel.submitCloudWaterIssue(
                            category = category,
                            title = title,
                            description = description,
                            severity = severity
                        )
                    }
                )
            }
        }
    }
}
