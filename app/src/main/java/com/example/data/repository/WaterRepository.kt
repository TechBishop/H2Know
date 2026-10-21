package com.example.data.repository

import android.content.Context
import com.example.data.local.CommunityTicketEntity
import com.example.data.local.H2KnowDao
import com.example.data.local.UserPreferenceEntity
import com.example.data.local.WatchedSystemEntity
import com.example.data.model.Contaminant
import com.example.data.model.IssueTicket
import com.example.data.model.PlaceInfo
import com.example.data.model.TreatmentStep
import com.example.data.model.ViolationRecord
import com.example.data.model.WaterSystemReport
import com.example.data.model.WaterSystemSummary
import com.example.data.model.WaterUsageTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

class WaterRepository(
    private val dao: H2KnowDao,
    private val appContext: Context? = null
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .build()

    private val systemReportCache = java.util.concurrent.ConcurrentHashMap<String, WaterSystemReport>()
    private val droughtCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, String>>()
    private var adjacencyCache: Map<String, List<Pair<String, String>>>? = null

    val watchedSystems: Flow<List<WatchedSystemEntity>> = dao.observeWatchedSystems()
    val allCommunityTickets: Flow<List<CommunityTicketEntity>> = dao.observeAllCommunityTickets()
    val userPreferences: Flow<UserPreferenceEntity?> = dao.observeUserPreferences()
    val allHomeSamples: Flow<List<com.example.data.local.HomeSampleEntity>> = dao.observeAllHomeSamples()
    val filterTracker: Flow<com.example.data.local.FilterTrackerEntity?> = dao.observeFilterTracker()

    suspend fun ensureSeedData() = withContext(Dispatchers.IO) {
        if (dao.getUserPreferencesOnce() == null) {
            dao.saveUserPreferences(UserPreferenceEntity())
        }
        if (dao.getFilterTrackerOnce() == null) {
            dao.saveFilterTracker(com.example.data.local.FilterTrackerEntity())
        }
        if (dao.getCommunityTicketCount() == 0) {
            ReferenceWaterData.initialSeededTickets().forEach { ticket ->
                dao.upsertCommunityTicket(ticket)
            }
        }
    }

    suspend fun logHomeSample(
        pwsid: String,
        contaminantCode: String,
        contaminantName: String,
        unit: String,
        measuredValue: Double,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        dao.insertHomeSample(
            com.example.data.local.HomeSampleEntity(
                id = "HOME-${UUID.randomUUID().toString().take(8).uppercase()}",
                pwsid = pwsid,
                contaminantCode = contaminantCode,
                contaminantName = contaminantName,
                unit = unit,
                measuredValue = measuredValue,
                notes = notes.ifBlank { "Home tap sample logged" },
                testedAt = today
            )
        )
    }

    suspend fun deleteHomeSample(id: String) = withContext(Dispatchers.IO) {
        dao.deleteHomeSample(id)
    }

    suspend fun updateFilterTracker(
        filterName: String,
        certificationBadge: String,
        cycleDays: Int,
        resetDateToday: Boolean,
        householdSize: Int? = null
    ) = withContext(Dispatchers.IO) {
        val current = dao.getFilterTrackerOnce() ?: com.example.data.local.FilterTrackerEntity()
        dao.saveFilterTracker(
            current.copy(
                filterName = filterName,
                certificationBadge = certificationBadge,
                cycleDays = cycleDays,
                installedEpochMs = if (resetDateToday) System.currentTimeMillis() else current.installedEpochMs,
                householdSize = householdSize ?: current.householdSize
            )
        )
    }

    suspend fun updateHouseholdSize(size: Int) = withContext(Dispatchers.IO) {
        val current = dao.getFilterTrackerOnce() ?: com.example.data.local.FilterTrackerEntity()
        dao.saveFilterTracker(current.copy(householdSize = size.coerceIn(1, 8)))
    }

    suspend fun searchProvidersByZip(zip: String): Pair<PlaceInfo, List<WaterSystemSummary>> =
        withContext(Dispatchers.IO) {
            val cleanZip = zip.filter { it.isDigit() }.take(5)
            require(cleanZip.length == 5) { "Enter a valid 5-digit U.S. ZIP code." }

            // Resolve place via Zippopotam.us or preset fallback
            val place = fetchPlaceFromZip(cleanZip)
                ?: ReferenceWaterData.placeFallback(cleanZip)
                ?: throw IllegalArgumentException("ZIP code $cleanZip was not recognized. Try a nearby 5-digit U.S. ZIP.")

            // A system's ZIP in SDWIS is often its mailing address, not the area it serves.
            // Match active community systems whose service area is this ZIP's county.
            val summaries = fetchEpaSystems(place, cleanZip)
            Pair(place, summaries)
        }

    suspend fun searchSystems(state: String, query: String): List<WaterSystemSummary> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        val compact = q.replace(" ", "").uppercase(Locale.US)
        val rows = if (Regex("^[A-Z]{2}\\d{3,9}$").matches(compact)) {
            fetchJsonArray("$EPA_BASE/WATER_SYSTEM/PWSID/$compact/JSON")
        } else {
            val st = state.trim().uppercase(Locale.US)
            if (st.isBlank()) return@withContext emptyList()
            val enc = URLEncoder.encode(q, "UTF-8")
            fetchJsonArray("$EPA_BASE/WATER_SYSTEM/PRIMACY_AGENCY_CODE/$st/PWS_TYPE_CODE/CWS/PWS_ACTIVITY_CODE/A/PWS_NAME/CONTAINING/$enc/rows/0:40/JSON")
        } ?: return@withContext emptyList()
        buildList {
            for (i in 0 until rows.length()) {
                val obj = rows.optJSONObject(i) ?: continue
                add(summaryFromSystem(obj, state, "", "", home = false, listed = false))
            }
        }
    }

    suspend fun loadSystemReport(pwsid: String, place: PlaceInfo?): WaterSystemReport =
        withContext(Dispatchers.IO) {
            systemReportCache[pwsid]?.let { return@withContext it }

            coroutineScope {
                val sysDeferred = async { fetchJsonArray("$EPA_BASE/WATER_SYSTEM/PWSID/$pwsid/JSON") }
                val violDeferred = async { fetchJsonArray("$EPA_BASE/VIOLATION/PWSID/$pwsid/rows/0:120/JSON") }
                val lcrDeferred = async { fetchJsonArray("$EPA_BASE/LCR_SAMPLE_RESULT/PWSID/$pwsid/rows/0:80/JSON") }
                val lcrSampleDeferred = async { fetchJsonArray("$EPA_BASE/LCR_SAMPLE/PWSID/$pwsid/rows/0:80/JSON") }
                val geoDeferred = async { fetchJsonArray("$EPA_BASE/GEOGRAPHIC_AREA/PWSID/$pwsid/JSON") }
                val droughtDeferred = async {
                    if (place != null) {
                        droughtCache[place.zip] ?: fetchDrought(place.lat, place.lon, place.state).also {
                            droughtCache[place.zip] = it
                        }
                    } else {
                        Pair("none", "No active drought reported for this location.")
                    }
                }

                val sysArr = sysDeferred.await()
                    ?: throw IllegalStateException("The EPA water-system record could not be loaded.")
                val violArr = violDeferred.await()
                val lcrArr = lcrDeferred.await()
                val lcrSamples = lcrSampleDeferred.await()
                val geoArr = geoDeferred.await() ?: JSONArray()
                val drought = droughtDeferred.await()

                val sysObj = sysArr.optJSONObject(0)
                val presetSummary = ReferenceWaterData.zipPresets
                    .mapNotNull { ReferenceWaterData.presetSystemsForZip(it.zip) }
                    .flatten()
                    .find { it.pwsid == pwsid }

                fun field(lower: String, upper: String): String {
                    val row = sysObj ?: return ""
                    return row.optString(lower, "").ifBlank { row.optString(upper, "") }
                }

                val name = field("pws_name", "PWS_NAME").ifBlank { presetSummary?.name ?: pwsid }
                val city = field("city_name", "CITY_NAME").ifBlank { presetSummary?.city ?: place?.city ?: "Not listed" }
                val state = field("state_code", "STATE_CODE").ifBlank { presetSummary?.state ?: place?.state ?: pwsid.take(2) }
                val zip = place?.zip ?: presetSummary?.zip ?: field("zip_code", "ZIP_CODE").filter { it.isDigit() }.take(5)
                val population = field("population_served_count", "POPULATION_SERVED_COUNT").toIntOrNull()?.takeIf { it > 0 } ?: 0
                val sourceType = field("primary_source_code", "PRIMARY_SOURCE_CODE").takeIf { it.isNotBlank() }?.let { sourceLabel(it) } ?: "Not listed"
                val ownerType = field("owner_type_code", "OWNER_TYPE_CODE").takeIf { it.isNotBlank() }?.let { ownerLabel(it) } ?: "Not listed"

                var county = ""
                for (i in 0 until geoArr.length()) {
                    val g = geoArr.optJSONObject(i) ?: continue
                    val c = g.optString("county_served", g.optString("COUNTY_SERVED", ""))
                    if (c.isNotBlank()) {
                        county = c
                        break
                    }
                }

                val violationsLoaded = violArr != null
                val samplesLoaded = lcrArr != null
                val violations = if (violArr == null) emptyList() else buildViolations10Y(pwsid, violArr)
                val contaminants = if (lcrArr == null) emptyList() else buildContaminantsDualBenchmark(lcrArr, lcrSamples)
                val (finalScore, riskLevel, complianceStatus) = scoreSystem(
                    violations = violations,
                    contaminants = contaminants,
                    violationsLoaded = violationsLoaded,
                    samplesLoaded = samplesLoaded
                )
                val recordNote = when {
                    !violationsLoaded || !samplesLoaded ->
                        "Part of the EPA record did not load, so this system is not scored."
                    contaminants.isEmpty() && violations.isEmpty() ->
                        "No lead or copper sample and no violation from the last 10 years were in the public record."
                    contaminants.isEmpty() ->
                        "No lead or copper sample was in the public record. The score uses violation history only. Other contaminants are not in the lead-and-copper table this app reads."
                    else ->
                        "Samples shown are lead and copper results from the EPA lead-and-copper table. Other contaminants are not filled in when that table does not include them."
                }

                val (seriesMap, alerts, _) = ReferenceWaterData.buildLongitudinalIntelligence(
                    pwsid = pwsid,
                    systemName = name,
                    zip = zip,
                    sourceType = sourceType,
                    currentScore = finalScore,
                    droughtLevel = drought.first,
                    contaminants = contaminants,
                    violations = violations
                )

                val officialTickets = ReferenceWaterData.buildOfficialTickets(
                    pwsid = pwsid,
                    systemName = name,
                    zip = zip,
                    violations = violations,
                    contaminants = contaminants
                )

                val treatmentSteps = when {
                    sourceType == "Not listed" -> listOf(
                        TreatmentStep(1, "Treatment not listed", "The public record did not include a source type, so treatment steps are not shown.")
                    )
                    sourceType.contains("Purchased", ignoreCase = true) -> listOf(
                        TreatmentStep(1, "Typical of purchased water", "These are common steps for water bought from a wholesale system, not a plant-specific record."),
                        TreatmentStep(2, "Residual disinfectant", "A chlorine or chloramine residual is commonly kept in the pipes."),
                        TreatmentStep(3, "Local storage", "Tanks and pumps usually hold pressure for delivery.")
                    )
                    sourceType.contains("Ground", ignoreCase = true) -> listOf(
                        TreatmentStep(1, "Typical of groundwater", "These are common steps for well water, not a plant-specific record."),
                        TreatmentStep(2, "Conditioning", "Some systems aerate or soften mineral-heavy groundwater."),
                        TreatmentStep(3, "Disinfection residual", "A disinfectant residual is commonly kept in the pipes.")
                    )
                    else -> listOf(
                        TreatmentStep(1, "Typical of surface water", "These are common steps for river or lake water, not a plant-specific record."),
                        TreatmentStep(2, "Clarification", "Sediment is often settled before filtration."),
                        TreatmentStep(3, "Filtration", "Sand or carbon beds are a common next step."),
                        TreatmentStep(4, "Disinfection", "A disinfectant is commonly added before the water leaves the plant.")
                    )
                }

                val cutMultiplier = when (drought.first) {
                    "D0" -> 0.90
                    "D1" -> 0.80
                    "D2" -> 0.65
                    "D3" -> 0.50
                    "D4" -> 0.40
                    else -> 1.0
                }
                val normalGpd = 90
                val recommendedGpd = (normalGpd * cutMultiplier).roundToInt()

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val people = if (population > 0) {
                    "${String.format(Locale.US, "%,d", population)} people"
                } else {
                    "a population the record does not list"
                }
                val report = WaterSystemReport(
                    pwsid = pwsid,
                    name = name,
                    city = city,
                    state = state,
                    zip = zip,
                    county = county,
                    population = population,
                    sourceType = sourceType,
                    ownerType = ownerType,
                    qualityScore = finalScore,
                    fiveYearScoreDelta = 0,
                    tenYearScoreDelta = 0,
                    riskLevel = riskLevel,
                    complianceStatus = complianceStatus,
                    droughtLevel = drought.first,
                    droughtDescription = drought.second,
                    contaminants = contaminants,
                    violations = violations,
                    alerts = alerts,
                    historicalSeries = seriesMap,
                    officialTickets = officialTickets,
                    treatment = treatmentSteps,
                    usage = WaterUsageTarget(normalGpd, recommendedGpd),
                    reportUrl = "https://echo.epa.gov/detailed-facility-report?fid=${URLEncoder.encode(pwsid, "UTF-8")}",
                    lastUpdated = todayStr,
                    phone = field("phone_number", "PHONE_NUMBER").ifBlank { "Not listed" },
                    contactAddress = listOf(
                        field("address_line1", "ADDRESS_LINE1"),
                        field("city_name", "CITY_NAME"),
                        field("zip_code", "ZIP_CODE")
                    ).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "Not listed" },
                    adminName = field("admin_name", "ADMIN_NAME").ifBlank { "Not listed" },
                    serviceConnections = field("service_connections_count", "SERVICE_CONNECTIONS_COUNT").toIntOrNull() ?: 0,
                    sourceDescription = "$name serves $people in $city, $state${if (county.isNotBlank()) " ($county County)" else ""}. Source: $sourceType. Operator: $ownerType. This is a community system EPA lists in that county. A ZIP on a water system is often a mailing address, not the pipes at that ZIP. Figures come from EPA SDWIS and the U.S. Drought Monitor. $recordNote",
                    labSamplesReported = contaminants.count { it.reported },
                    recordNote = recordNote
                )

                systemReportCache[pwsid] = report
                report
            }
        }

    suspend fun toggleWatchSystem(
        report: WaterSystemReport,
        currentlyWatched: WatchedSystemEntity?
    ) = withContext(Dispatchers.IO) {
        if (currentlyWatched != null) {
            dao.deleteWatchedSystem(report.pwsid)
        } else {
            dao.upsertWatchedSystem(
                WatchedSystemEntity(
                    pwsid = report.pwsid,
                    systemName = report.name,
                    zip = report.zip,
                    city = report.city,
                    state = report.state,
                    lastCheckedScore = report.qualityScore,
                    fiveYearDeltaPts = 0,
                    openViolationCount = report.violations.count { it.status == "open" }
                )
            )
        }
    }

    suspend fun updateWatchPreferences(entity: WatchedSystemEntity) = withContext(Dispatchers.IO) {
        dao.upsertWatchedSystem(entity)
    }

    suspend fun removeWatchedSystem(pwsid: String) = withContext(Dispatchers.IO) {
        dao.deleteWatchedSystem(pwsid)
    }

    suspend fun submitCommunityTicket(
        pwsid: String,
        systemName: String,
        zip: String,
        category: String,
        title: String,
        description: String,
        severity: String,
        linkedContaminantCode: String?,
        linkedContaminantName: String?
    ) = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val entity = CommunityTicketEntity(
            id = "RES-${UUID.randomUUID().toString().take(8).uppercase()}",
            pwsid = pwsid,
            systemName = systemName,
            zip = zip,
            category = category,
            title = title.trim(),
            description = description.trim(),
            status = "open",
            severity = severity,
            createdAt = today,
            linkedContaminantCode = linkedContaminantCode,
            linkedContaminantName = linkedContaminantName,
            affectedCount = 1,
            notesPipeDelimited = "Resident ticket logged and linked to $systemName ($pwsid)."
        )
        dao.upsertCommunityTicket(entity)
    }

    suspend fun upvoteCommunityTicket(ticket: IssueTicket) = withContext(Dispatchers.IO) {
        val existing = dao.getTicketById(ticket.id)
        if (existing != null) {
            dao.upsertCommunityTicket(existing.copy(affectedCount = existing.affectedCount + 1))
        } else {
            dao.upsertCommunityTicket(
                CommunityTicketEntity(
                    id = ticket.id,
                    pwsid = ticket.pwsid,
                    systemName = ticket.systemName,
                    zip = ticket.zip,
                    category = ticket.category,
                    title = ticket.title,
                    description = ticket.description,
                    status = ticket.status,
                    severity = ticket.severity,
                    createdAt = ticket.createdAt,
                    linkedContaminantCode = ticket.linkedContaminantCode,
                    linkedContaminantName = ticket.linkedContaminantName,
                    affectedCount = ticket.affectedCount + 1,
                    notesPipeDelimited = ticket.notes.joinToString("|")
                )
            )
        }
    }

    suspend fun addTicketNote(ticket: IssueTicket, noteText: String) = withContext(Dispatchers.IO) {
        val clean = noteText.trim()
        if (clean.isEmpty()) return@withContext
        val existing = dao.getTicketById(ticket.id)
        if (existing != null) {
            val updatedNotes = (existing.notesPipeDelimited.split("|").filter { it.isNotBlank() } + clean).joinToString("|")
            dao.upsertCommunityTicket(existing.copy(notesPipeDelimited = updatedNotes))
        } else {
            val updatedNotes = (ticket.notes + clean).joinToString("|")
            dao.upsertCommunityTicket(
                CommunityTicketEntity(
                    id = ticket.id,
                    pwsid = ticket.pwsid,
                    systemName = ticket.systemName,
                    zip = ticket.zip,
                    category = ticket.category,
                    title = ticket.title,
                    description = ticket.description,
                    status = ticket.status,
                    severity = ticket.severity,
                    createdAt = ticket.createdAt,
                    linkedContaminantCode = ticket.linkedContaminantCode,
                    linkedContaminantName = ticket.linkedContaminantName,
                    affectedCount = ticket.affectedCount,
                    notesPipeDelimited = updatedNotes
                )
            )
        }
    }

    suspend fun updateThemePreference(isDark: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getUserPreferencesOnce() ?: UserPreferenceEntity()
        dao.saveUserPreferences(current.copy(isDarkTheme = isDark))
    }

    suspend fun updateHouseholdProfile(profileKey: String) = withContext(Dispatchers.IO) {
        val current = dao.getUserPreferencesOnce() ?: UserPreferenceEntity()
        dao.saveUserPreferences(current.copy(householdProfileKey = profileKey))
    }

    suspend fun markAuthPromptSeen() = withContext(Dispatchers.IO) {
        val current = dao.getUserPreferencesOnce() ?: UserPreferenceEntity()
        dao.saveUserPreferences(current.copy(hasSeenAuthPrompt = true))
    }

    // Internal HTTP & EPA parsing helpers
    private fun fetchPlaceFromZip(zip: String): PlaceInfo? {
        return try {
            val req = Request.Builder()
                .url("https://api.zippopotam.us/us/$zip")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONObject(body)
                val places = root.optJSONArray("places") ?: return null
                val p = places.optJSONObject(0) ?: return null
                PlaceInfo(
                    zip = zip,
                    city = p.optString("place name", ""),
                    state = p.optString("state abbreviation", ""),
                    stateFull = p.optString("state", ""),
                    lat = p.optString("latitude", "0").toDoubleOrNull() ?: 0.0,
                    lon = p.optString("longitude", "0").toDoubleOrNull() ?: 0.0
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchCountyName(lat: Double, lon: Double): String? {
        return try {
            val url = "https://geo.fcc.gov/api/census/area?lat=$lat&lon=$lon&format=json"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val root = JSONObject(resp.body?.string() ?: return null)
                val name = root.optJSONArray("results")?.optJSONObject(0)?.optString("county_name", "").orEmpty()
                name.removeSuffix(" County").removeSuffix(" county").trim().ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class GeoSlot(
        val pwsid: String,
        val cities: MutableList<String> = mutableListOf(),
        val zips: MutableList<String> = mutableListOf(),
        var county: String,
        var state: String,
        var home: Boolean
    )

    private suspend fun fetchEpaSystems(place: PlaceInfo, zip: String): List<WaterSystemSummary> {
        val county = fetchCountyInfo(place.lat, place.lon)
            ?: throw IllegalStateException("This ZIP could not be matched to a county, so no water system was chosen.")
        val index = linkedMapOf<String, GeoSlot>()
        val homeGeo = fetchGeoCounty(county.name, place.state)
        absorbGeo(index, homeGeo, county.name, place.state, home = true)
        val homeIds = index.keys.toList()
        if (homeIds.isEmpty()) {
            throw IllegalStateException("No community water system was listed for ${county.name} County, ${place.state}.")
        }
        val homeRows = hydrateSystems(homeIds)
        val homeSummaries = homeRows.map { summaryFromGeo(it, index, place, zip, fallbackCounty = county.name) }
        val largest = homeSummaries.maxOfOrNull { it.population } ?: 0
        if (largest < 100_000 && county.fips.isNotBlank()) {
            val neighbors = loadAdjacency()[county.fips].orEmpty()
            for ((name, state) in neighbors) {
                val rows = try {
                    fetchGeoCounty(name, state)
                } catch (_: Exception) {
                    continue
                }
                absorbGeo(index, rows, name, state, home = false)
            }
        }
        val extraIds = index.keys.filter { it !in homeIds }
        val extraRows = if (extraIds.isEmpty()) emptyList() else hydrateSystems(extraIds)
        val extras = extraRows.map { summaryFromGeo(it, index, place, zip, fallbackCounty = "") }
        return (homeSummaries + extras).sortedWith(
            compareBy<WaterSystemSummary> { if (it.listedForPlace) 0 else if (it.homeCounty) 1 else 2 }
                .thenBy { it.name }
        )
    }

    private fun summaryFromGeo(
        obj: JSONObject,
        index: Map<String, GeoSlot>,
        place: PlaceInfo,
        zip: String,
        fallbackCounty: String
    ): WaterSystemSummary {
        val id = obj.optString("pwsid", obj.optString("PWSID", ""))
        val slot = index[id]
        val cityKey = place.city.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "")
        val listed = slot?.cities?.any {
            it.uppercase(Locale.US).replace(Regex("[^A-Z0-9]"), "") == cityKey
        } == true || slot?.zips?.contains(zip) == true
        return summaryFromSystem(
            obj = obj,
            state = slot?.state ?: place.state,
            zip = zip,
            county = slot?.county ?: fallbackCounty,
            home = slot?.home ?: true,
            listed = listed
        ).copy(city = place.city.ifBlank { obj.optString("city_name", "") })
    }

    private fun summaryFromSystem(
        obj: JSONObject,
        state: String,
        zip: String,
        county: String,
        home: Boolean,
        listed: Boolean
    ): WaterSystemSummary {
        val pwsid = obj.optString("pwsid", obj.optString("PWSID", ""))
        val name = obj.optString("pws_name", obj.optString("PWS_NAME", pwsid)).ifBlank { pwsid }
        val pop = obj.optString("population_served_count", obj.optString("POPULATION_SERVED_COUNT", "0")).toIntOrNull() ?: 0
        val src = obj.optString("primary_source_code", obj.optString("PRIMARY_SOURCE_CODE", ""))
        return WaterSystemSummary(
            pwsid = pwsid,
            name = name,
            city = obj.optString("city_name", obj.optString("CITY_NAME", "")),
            state = obj.optString("state_code", obj.optString("STATE_CODE", state)).ifBlank { state },
            zip = zip,
            population = pop,
            sourceType = sourceLabel(src),
            isPrimary = false,
            currentScore = -1,
            fiveYearTrend = "Unknown",
            fiveYearDeltaPts = 0,
            openTicketCount = 0,
            county = county,
            listedForPlace = listed,
            homeCounty = home
        )
    }

    private fun absorbGeo(index: MutableMap<String, GeoSlot>, geo: JSONArray, county: String, state: String, home: Boolean) {
        for (i in 0 until geo.length()) {
            val row = geo.optJSONObject(i) ?: continue
            val id = row.optString("pwsid", "").ifBlank { row.optString("PWSID", "") }
            if (id.isBlank()) continue
            val slot = index.getOrPut(id) { GeoSlot(id, county = county, state = state, home = home) }
            if (home) slot.home = true
            val city = row.optString("city_served", row.optString("CITY_SERVED", ""))
            val servedZip = row.optString("zip_code_served", row.optString("ZIP_CODE_SERVED", ""))
            if (city.isNotBlank() && city !in slot.cities) slot.cities.add(city)
            if (servedZip.isNotBlank() && servedZip !in slot.zips) slot.zips.add(servedZip)
        }
    }

    private fun fetchGeoCounty(county: String, state: String): JSONArray {
        val all = JSONArray()
        var start = 0
        val page = 100
        while (start < 600) {
            val enc = URLEncoder.encode(county, "UTF-8")
            val st = URLEncoder.encode(state, "UTF-8")
            val batch = fetchJsonArray(
                "$EPA_BASE/GEOGRAPHIC_AREA/COUNTY_SERVED/$enc/PRIMACY_AGENCY_CODE/$st/PWS_TYPE_CODE/CWS/PWS_ACTIVITY_CODE/A/rows/$start:${start + page}/JSON"
            )
            if (batch == null) {
                if (start == 0) {
                    throw IllegalStateException("The EPA service-area list could not be reached. Try again in a moment.")
                }
                break
            }
            if (batch.length() == 0) break
            for (i in 0 until batch.length()) all.put(batch.opt(i))
            if (batch.length() < page) break
            start += page
        }
        return all
    }

    private suspend fun hydrateSystems(ids: List<String>): List<JSONObject> = coroutineScope {
        val gate = Semaphore(6)
        ids.map { id ->
            async {
                gate.withPermit {
                    var row: JSONObject? = null
                    repeat(2) { attempt ->
                        if (row == null) {
                            row = fetchJsonArray("$EPA_BASE/WATER_SYSTEM/PWSID/$id/JSON")?.optJSONObject(0)
                            if (row == null && attempt == 0) {
                                // one quiet retry when a single system record misses
                            }
                        }
                    }
                    row
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun loadAdjacency(): Map<String, List<Pair<String, String>>> {
        adjacencyCache?.let { return it }
        val context = appContext ?: return emptyMap()
        return try {
            val text = context.assets.open("counties.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val map = mutableMapOf<String, List<Pair<String, String>>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val fips = keys.next()
                val arr = root.optJSONArray(fips) ?: continue
                val neighbors = buildList {
                    for (i in 0 until arr.length()) {
                        val parts = arr.optString(i).split(",")
                        if (parts.size >= 2 && parts[0].isNotBlank()) add(parts[0] to parts[1])
                    }
                }
                map[fips] = neighbors
            }
            map.also { adjacencyCache = it }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun fetchCountyInfo(lat: Double, lon: Double): CountyInfo? {
        return try {
            val url = "https://geo.fcc.gov/api/census/area?lat=$lat&lon=$lon&format=json"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val row = JSONObject(resp.body?.string() ?: return null)
                    .optJSONArray("results")?.optJSONObject(0) ?: return null
                val name = row.optString("county_name", "").removeSuffix(" County").removeSuffix(" county").trim()
                if (name.isBlank()) return null
                CountyInfo(name, row.optString("county_fips", ""))
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class CountyInfo(val name: String, val fips: String)

    private fun fetchDrought(lat: Double, lon: Double, @Suppress("UNUSED_PARAMETER") state: String): Pair<String, String> {
        try {
            val url = "$DROUGHT_URL?geometry=$lon,$lat&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=dm,period&returnGeometry=false&f=json"
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Pair("unknown", "Drought status could not be loaded. This is not a report of drought conditions.")
                }
                val root = JSONObject(resp.body?.string() ?: "{}")
                val features = root.optJSONArray("features")
                if (features == null || features.length() == 0) {
                    return Pair("none", "This location is not inside a mapped U.S. Drought Monitor area.")
                }
                val attr = features.optJSONObject(0)?.optJSONObject("attributes")
                if (attr != null && !attr.isNull("dm")) {
                    val dm = attr.optDouble("dm", -1.0).roundToInt()
                    if (dm in 0..4) {
                        val level = listOf("D0", "D1", "D2", "D3", "D4")[dm]
                        return Pair(level, "The U.S. Drought Monitor lists this point as ${ReferenceWaterData.droughtLabel(level)}.")
                    }
                }
                return Pair("none", "This location is not inside a mapped U.S. Drought Monitor area.")
            }
        } catch (_: Exception) {
            return Pair("unknown", "Drought status could not be loaded. This is not a report of drought conditions.")
        }
    }

    /** Null means the request failed. An empty array means the source returned no rows. */
    private fun fetchJsonArray(url: String): JSONArray? {
        return try {
            val req = Request.Builder().url(url).header("Accept", "application/json").build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string()?.trim() ?: return JSONArray()
                if (raw.startsWith("[")) JSONArray(raw)
                else if (raw.startsWith("{")) JSONArray().put(JSONObject(raw))
                else JSONArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEpaDate(raw: String): Pair<Int, String>? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        val isoRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})""")
        isoRegex.find(clean)?.let { m ->
            val yr = m.groupValues[1].toIntOrNull() ?: return null
            return Pair(yr, "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}")
        }
        val oracleRegex = Regex("""^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})""")
        oracleRegex.find(clean)?.let { m ->
            val dd = m.groupValues[1].padStart(2, '0')
            val monStr = m.groupValues[2].uppercase()
            val mm = when (monStr) {
                "JAN" -> "01"; "FEB" -> "02"; "MAR" -> "03"; "APR" -> "04"
                "MAY" -> "05"; "JUN" -> "06"; "JUL" -> "07"; "AUG" -> "08"
                "SEP" -> "09"; "OCT" -> "10"; "NOV" -> "11"; "DEC" -> "12"
                else -> return null
            }
            val rawYr = m.groupValues[3].toIntOrNull() ?: return null
            val yr = when {
                rawYr >= 100 -> rawYr
                rawYr >= 70 -> 1900 + rawYr
                else -> 2000 + rawYr
            }
            return Pair(yr, "$yr-$mm-$dd")
        }
        val slashRegex = Regex("""^(\d{1,2})/(\d{1,2})/(\d{4})""")
        slashRegex.find(clean)?.let { m ->
            val mm = m.groupValues[1].padStart(2, '0')
            val dd = m.groupValues[2].padStart(2, '0')
            val yr = m.groupValues[3].toIntOrNull() ?: return null
            return Pair(yr, "$yr-$mm-$dd")
        }
        return null
    }

    private fun buildViolations10Y(pwsid: String, violArr: JSONArray): List<ViolationRecord> {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val minYear = currentYear - 10
        val parsed = mutableListOf<ViolationRecord>()
        for (i in 0 until violArr.length()) {
            val r = violArr.optJSONObject(i) ?: continue
            val beginDate = r.optString(
                "compl_per_begin_date",
                r.optString(
                    "COMPL_PER_BEGIN_DATE",
                    r.optString("non_compl_per_begin_date", r.optString("NON_COMPL_PER_BEGIN_DATE", ""))
                )
            )
            val parsedDate = parseEpaDate(beginDate) ?: continue
            val yr = parsedDate.first
            val isoDate = parsedDate.second
            if (yr < minYear || yr > currentYear) continue
            val vid = r.optString("violation_id", r.optString("VIOLATION_ID", "${i + 1}"))
            val contCode = r.optString("contaminant_code", r.optString("CONTAMINANT_CODE", "")).takeIf { it.isNotBlank() }
            val catCode = r.optString("violation_category_code", r.optString("VIOLATION_CATEGORY_CODE", ""))
            val isHealth = r.optString("is_health_based_ind", r.optString("IS_HEALTH_BASED_IND", "N")).equals("Y", ignoreCase = true)
            val rtcDate = r.optString("rtc_date", "").ifBlank { r.optString("RTC_DATE", "") }.trim()
            val statusRaw = r.optString("compliance_status_code", "")
                .ifBlank { r.optString("COMPLIANCE_STATUS_CODE", "") }
                .trim()
                .uppercase()
            val resolved = rtcDate.isNotEmpty() || statusRaw == "R" || statusRaw == "V" || statusRaw.startsWith("RESOL")
            val status = when {
                resolved -> "resolved"
                statusRaw.startsWith("A") || statusRaw == "ADDRESSED" -> "addressed"
                else -> "open"
            }
            val vType = when {
                isHealth || catCode.contains("MCL", ignoreCase = true) -> "health"
                catCode.equals("MR", ignoreCase = true) -> "monitoring"
                catCode.equals("TT", ignoreCase = true) -> "treatment"
                else -> "other"
            }
            val contMeta = contCode?.let { ReferenceWaterData.contaminantCatalog[it] }
            val plainStatus = when (status) {
                "open" -> "Open"
                "addressed" -> "Addressed"
                else -> "Resolved"
            }
            val plainRuleDesc = when {
                isHealth -> "Health-based drinking water standard"
                vType == "treatment" -> "Treatment technique"
                vType == "monitoring" -> "Monitoring or reporting"
                else -> "Compliance record"
            }
            parsed.add(
                ViolationRecord(
                    id = "$pwsid-$vid",
                    date = isoDate,
                    year = yr,
                    contaminantCode = contCode,
                    contaminant = contMeta?.name?.substringBefore(" (") ?: "Compliance item",
                    type = vType,
                    status = status,
                    severity = if (isHealth) "serious" else "minor",
                    description = "$plainRuleDesc. $plainStatus. A compliance-period end date is not treated as returned to compliance."
                )
            )
        }
        return parsed.sortedByDescending { it.date }.take(45)
    }

    private fun normalizeLcr(code: String, raw: Double, unitRaw: String): Double? {
        val unit = unitRaw.lowercase().replace("µ", "u").replace("μ", "u").replace(" ", "")
        val isUg = unit.contains("ug") || unit == "ppb"
        val isMg = unit.contains("mg") || unit == "ppm"
        return when (code) {
            "PB90" -> when {
                isUg -> raw
                isMg -> raw * 1000.0
                unit.isBlank() && raw < 1.0 -> raw * 1000.0
                unit.isBlank() -> raw
                else -> null
            }
            "CU90" -> when {
                isMg -> raw
                isUg -> raw / 1000.0
                unit.isBlank() && raw > 20.0 -> raw / 1000.0
                unit.isBlank() -> raw
                else -> null
            }
            else -> null
        }
    }

    private fun buildContaminantsDualBenchmark(
        lcrArr: JSONArray,
        sampleArr: JSONArray?
    ): List<Contaminant> {
        val dates = mutableMapOf<String, String>()
        if (sampleArr != null) {
            for (i in 0 until sampleArr.length()) {
                val s = sampleArr.optJSONObject(i) ?: continue
                val id = s.optString("sample_id", s.optString("SAMPLE_ID", ""))
                val end = s.optString("sampling_end_date", s.optString("SAMPLING_END_DATE", ""))
                if (id.isNotBlank() && end.isNotBlank()) dates[id] = end.take(10)
            }
        }
        data class Row(val value: Double, val date: String)
        val rows = mutableMapOf<String, MutableList<Row>>()
        for (i in 0 until lcrArr.length()) {
            val s = lcrArr.optJSONObject(i) ?: continue
            val code = s.optString("contaminant_code", s.optString("CONTAMINANT_CODE", ""))
            if (code != "PB90" && code != "CU90") continue
            val raw = s.optString("sample_measure", s.optString("SAMPLE_MEASURE", "")).toDoubleOrNull() ?: continue
            val unit = s.optString("unit_of_measure", s.optString("UNIT_OF_MEASURE", ""))
            val sign = s.optString("result_sign_code", s.optString("RESULT_SIGN_CODE", "")).trim()
            val normalized = normalizeLcr(code, raw, unit) ?: continue
            val value = if (sign == "<" || sign.equals("ND", ignoreCase = true)) 0.0 else normalized
            val sid = s.optString("sample_id", s.optString("SAMPLE_ID", ""))
            rows.getOrPut(code) { mutableListOf() }.add(Row(value, dates[sid].orEmpty()))
        }
        return rows.mapNotNull { (code, list) ->
            val meta = ReferenceWaterData.contaminantCatalog[code] ?: return@mapNotNull null
            val sorted = list.sortedWith(compareByDescending<Row> { it.date }.thenByDescending { it.value })
            val latest = sorted.first()
            val prev = sorted.drop(1).firstOrNull()
            val rounded = (latest.value * 1000.0).roundToInt() / 1000.0
            val delta = if (prev != null && prev.value > 0.0) {
                (((rounded - prev.value) / prev.value) * 100.0).roundToInt()
            } else 0
            val dir = when {
                prev == null -> "unknown"
                rounded < prev.value -> "improving"
                rounded > prev.value -> "elevated"
                else -> "stable"
            }
            Contaminant(
                code = code,
                name = meta.name,
                category = meta.category,
                unit = meta.unit,
                detected = rounded,
                mcl = meta.mcl,
                healthGoal = meta.healthGoal,
                healthAgency = meta.healthAgency,
                healthNote = meta.healthNote,
                pfas = meta.pfas,
                regulated = true,
                trendDeltaPct1Y = delta,
                trendDirection = dir,
                reported = true,
                sampleDate = latest.date.ifBlank { null }
            )
        }
    }

    private fun scoreSystem(
        violations: List<ViolationRecord>,
        contaminants: List<Contaminant>,
        violationsLoaded: Boolean,
        samplesLoaded: Boolean
    ): Triple<Int, String, String> {
        if (!violationsLoaded || !samplesLoaded) {
            return Triple(-1, "unknown", "unavailable")
        }
        val reported = contaminants.filter { it.reported }
        if (reported.isEmpty() && violations.isEmpty()) {
            return Triple(-1, "unknown", "insufficient_data")
        }
        val open = violations.filter { it.status == "open" }
        val openHealth = open.count { it.type == "health" }
        val openOther = (open.size - openHealth).coerceAtLeast(0)
        val aboveLegal = reported.count { it.exceedsMcl }
        val penalty = min(54, openHealth * 18) + min(24, openOther * 6) + min(45, aboveLegal * 15)
        val score = (100 - penalty).coerceIn(0, 100)
        val compliance = when {
            openHealth > 0 || aboveLegal > 0 -> "under_enforcement"
            open.isNotEmpty() -> "elevated_concern"
            else -> "in_compliance"
        }
        val risk = when {
            score < 45 -> "critical"
            score < 65 -> "high"
            score < 80 -> "moderate"
            else -> "low"
        }
        return Triple(score, risk, compliance)
    }

    private fun sourceLabel(code: String): String = when (code.uppercase()) {
        "SW" -> "Surface water"
        "GW" -> "Groundwater"
        "GU" -> "Groundwater under surface influence"
        "SWP" -> "Purchased surface water"
        "GWP", "GUP" -> "Purchased groundwater"
        else -> "Not listed"
    }

    private fun ownerLabel(code: String): String = when (code.uppercase()) {
        "L" -> "Local government"
        "M" -> "Public / municipal"
        "S" -> "State agency"
        "F" -> "Federal government"
        "P" -> "Private utility"
        "N" -> "Tribal authority"
        else -> "Not listed"
    }

    companion object {
        private const val EPA_BASE = "https://data.epa.gov/efservice"
        private const val DROUGHT_URL =
            "https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/US_Drought_Intensity_v1/FeatureServer/3/query"
    }
}
