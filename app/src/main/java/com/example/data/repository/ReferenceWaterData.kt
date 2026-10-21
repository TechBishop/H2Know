package com.example.data.repository

import com.example.data.local.CommunityTicketEntity
import com.example.data.model.Contaminant
import com.example.data.model.FilterRecommendation
import com.example.data.model.HistoricalPoint
import com.example.data.model.HistoricalTimeSeries
import com.example.data.model.HouseholdProfile
import com.example.data.model.IssueTicket
import com.example.data.model.PlaceInfo
import com.example.data.model.PriorityAction
import com.example.data.model.QualityChangeAlert
import com.example.data.model.SuggestedProductLink
import com.example.data.model.TreatmentStep
import com.example.data.model.TrendDiagnostic
import com.example.data.model.TrendTimeframe
import com.example.data.model.ViolationRecord
import com.example.data.model.WaterSystemReport
import com.example.data.model.WaterSystemSummary
import com.example.data.model.WaterUsageTarget
import com.example.data.model.WorkspaceTab
import com.example.data.model.ZipPreset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ContaminantRefMeta(
    val code: String,
    val name: String,
    val unit: String,
    val mcl: Double?,
    val healthGoal: Double?,
    val healthAgency: String,
    val category: String,
    val healthNote: String,
    val pfas: Boolean
)

data class BenchmarkComparisonSnapshot(
    val pwsid: String,
    val label: String,
    val location: String,
    val score: Int,
    val tenYearDelta: Int,
    val leadPpb: Double,
    val tthmPpb: Double,
    val pfoaPpt: Double,
    val openViolations: Int
)

object ReferenceWaterData {

    // No national median is published in the EPA tables this app reads.
    val comparisonBenchmarks: List<BenchmarkComparisonSnapshot> = emptyList()

    val zipPresets = listOf(
        ZipPreset("78701", "Austin", "TX", "Surface Water · DBP Cycle"),
        ZipPreset("48502", "Flint", "MI", "10Y Lead Recovery"),
        ZipPreset("07102", "Newark", "NJ", "Corrosion Upgrade"),
        ZipPreset("39201", "Jackson", "MS", "Infrastructure Watch"),
        ZipPreset("90210", "Beverly Hills", "CA", "Drought & Imported")
    )

    val contaminantCatalog: Map<String, ContaminantRefMeta> = mapOf(
        "PB90" to ContaminantRefMeta(
            code = "PB90",
            name = "Lead (90th percentile)",
            unit = "ppb",
            mcl = 15.0,
            healthGoal = 0.0,
            healthAgency = "EPA MCLG / AAP",
            category = "Metals",
            healthNote = "Health experts treat lead as unsafe at any level (0 ppb Public Health Goal), especially for infants, children, and expecting households. Primarily enters water from aging service lines and household plumbing.",
            pfas = false
        ),
        "CU90" to ContaminantRefMeta(
            code = "CU90",
            name = "Copper (90th percentile)",
            unit = "ppm",
            mcl = 1.3,
            healthGoal = 0.3,
            healthAgency = "OEHHA PHG",
            category = "Metals",
            healthNote = "Keep copper as low as practical (0.3 ppm health benchmark vs 1.3 ppm EPA Action Level) for infants and sensitive households to prevent gastrointestinal distress and liver strain.",
            pfas = false
        ),
        "2950" to ContaminantRefMeta(
            code = "2950",
            name = "Total Trihalomethanes (TTHMs)",
            unit = "ppb",
            mcl = 80.0,
            healthGoal = 0.8,
            healthAgency = "OEHHA PHG",
            category = "Disinfection byproducts",
            healthNote = "Formed when chlorine disinfectant reacts with natural organic matter in surface water. Warmer summer months typically drive seasonal spikes.",
            pfas = false
        ),
        "2456" to ContaminantRefMeta(
            code = "2456",
            name = "Haloacetic Acids (HAA5)",
            unit = "ppb",
            mcl = 60.0,
            healthGoal = 0.5,
            healthAgency = "Health Benchmark",
            category = "Disinfection byproducts",
            healthNote = "Disinfection byproduct class formed alongside TTHMs during chlorination. Activated carbon filtration significantly reduces tap levels.",
            pfas = false
        ),
        "1040" to ContaminantRefMeta(
            code = "1040",
            name = "Nitrate (as N)",
            unit = "ppm",
            mcl = 10.0,
            healthGoal = 1.0,
            healthAgency = "EWG / Epidemiological Goal",
            category = "Inorganic",
            healthNote = "Agricultural runoff and watershed fertilizer can elevate nitrate in spring. Infants under 6 months are highly vulnerable to methemoglobinemia.",
            pfas = false
        ),
        "1005" to ContaminantRefMeta(
            code = "1005",
            name = "Arsenic",
            unit = "ppb",
            mcl = 10.0,
            healthGoal = 0.004,
            healthAgency = "OEHHA PHG",
            category = "Metals",
            healthNote = "Naturally occurring trace metalloid in bedrock aquifers that concentrates during multi-year drought drawdown.",
            pfas = false
        ),
        "2806" to ContaminantRefMeta(
            code = "2806",
            name = "PFOA (Perfluorooctanoic Acid)",
            unit = "ppt",
            mcl = 4.0,
            healthGoal = 0.0,
            healthAgency = "EPA MCLG",
            category = "PFAS",
            healthNote = "EPA's maximum contaminant level goal for PFOA is zero. The federal legal limit is 4 ppt. This app does not invent a PFOA result when the public sample table does not include one.",
            pfas = true
        ),
        "2805" to ContaminantRefMeta(
            code = "2805",
            name = "PFOS (Perfluorooctanesulfonic Acid)",
            unit = "ppt",
            mcl = 4.0,
            healthGoal = 0.0,
            healthAgency = "EPA MCLG",
            category = "PFAS",
            healthNote = "Persistent fluorinated compound bioaccumulating over time. Newly mandated EPA monitoring has surfaced low-ppt detections nationwide.",
            pfas = true
        ),
        "1025" to ContaminantRefMeta(
            code = "1025",
            name = "Fluoride",
            unit = "ppm",
            mcl = 4.0,
            healthGoal = 0.7,
            healthAgency = "USPHS Optimal Level",
            category = "Inorganic",
            healthNote = "Public health dental guidance centers near 0.7 ppm for cavity prevention while avoiding dental fluorosis.",
            pfas = false
        )
    )

    fun initialSeededTickets(): List<CommunityTicketEntity> = emptyList()

    fun placeFallback(zip: String): PlaceInfo? = when (zip) {
        "78701" -> PlaceInfo("78701", "Austin", "TX", "Texas", 30.2711, -97.7437)
        "48502" -> PlaceInfo("48502", "Flint", "MI", "Michigan", 43.0125, -83.6875)
        "07102" -> PlaceInfo("07102", "Newark", "NJ", "New Jersey", 40.7357, -74.1724)
        "39201" -> PlaceInfo("39201", "Jackson", "MS", "Mississippi", 32.2988, -90.1848)
        "90210" -> PlaceInfo("90210", "Beverly Hills", "CA", "California", 34.0901, -118.4065)
        else -> null
    }

    fun presetSystemsForZip(zip: String): List<WaterSystemSummary>? = when (zip) {
        "78701" -> listOf(
            WaterSystemSummary(
                pwsid = "TX2270001",
                name = "AUSTIN WATER",
                city = "Austin",
                state = "TX",
                zip = "78701",
                population = 1045200,
                sourceType = "Surface water",
                isPrimary = true,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            ),
            WaterSystemSummary(
                pwsid = "TX2270049",
                name = "TRAVIS COUNTY WCID 10",
                city = "Austin",
                state = "TX",
                zip = "78701",
                population = 18450,
                sourceType = "Purchased surface water",
                isPrimary = false,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            )
        )
        "48502" -> listOf(
            WaterSystemSummary(
                pwsid = "MI0002310",
                name = "FLINT, CITY OF",
                city = "Flint",
                state = "MI",
                zip = "48502",
                population = 81252,
                sourceType = "Purchased surface water",
                isPrimary = true,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            ),
            WaterSystemSummary(
                pwsid = "MI0002635",
                name = "GENESEE COUNTY DRAIN COMM - WATER",
                city = "Flint",
                state = "MI",
                zip = "48502",
                population = 142000,
                sourceType = "Surface water",
                isPrimary = false,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            )
        )
        "07102" -> listOf(
            WaterSystemSummary(
                pwsid = "NJ0714001",
                name = "NEWARK WATER DEPARTMENT",
                city = "Newark",
                state = "NJ",
                zip = "07102",
                population = 311549,
                sourceType = "Surface water",
                isPrimary = true,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            )
        )
        "39201" -> listOf(
            WaterSystemSummary(
                pwsid = "MS0250008",
                name = "CITY OF JACKSON WATER SYSTEM",
                city = "Jackson",
                state = "MS",
                zip = "39201",
                population = 153701,
                sourceType = "Surface water",
                isPrimary = true,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            )
        )
        "90210" -> listOf(
            WaterSystemSummary(
                pwsid = "CA1910003",
                name = "BEVERLY HILLS PUBLIC WORKS",
                city = "Beverly Hills",
                state = "CA",
                zip = "90210",
                population = 42180,
                sourceType = "Purchased surface water",
                isPrimary = true,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            ),
            WaterSystemSummary(
                pwsid = "CA1910067",
                name = "LOS ANGELES DEPT OF WATER AND POWER",
                city = "Los Angeles",
                state = "CA",
                zip = "90210",
                population = 3898747,
                sourceType = "Surface water",
                isPrimary = false,
                currentScore = -1,
                fiveYearTrend = "Unknown",
                fiveYearDeltaPts = 0,
                openTicketCount = 0
            )
        )
        else -> null
    }

    fun stableHash(input: String): Int {
        var h = 0
        for (ch in input) {
            h = (h shl 5) - h + ch.code
        }
        return h and 0x7fffffff
    }

    fun findPresetByPwsid(pwsid: String): WaterSystemSummary? =
        zipPresets.mapNotNull { presetSystemsForZip(it.zip) }.flatten().find { it.pwsid == pwsid }

    /** Unknown until a report is scored from the public record. Never a preset or a hash. */
    fun stableScoreForPwsid(pwsid: String): Int = -1

    fun stableFiveYearDeltaForPwsid(pwsid: String): Int = 0

    /**
     * Year-by-year violation counts from the public record.
     * Historical scores and contaminant curves are not in this feed, so qualityScore stays -1.
     */
    fun buildLongitudinalIntelligence(
        pwsid: String,
        systemName: String,
        zip: String,
        sourceType: String,
        currentScore: Int,
        droughtLevel: String,
        contaminants: List<Contaminant>,
        violations: List<ViolationRecord>
    ): Triple<Map<TrendTimeframe, HistoricalTimeSeries>, List<QualityChangeAlert>, Pair<Int, Int>> {
        val yearNow = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        fun series(startYear: Int, frame: TrendTimeframe): HistoricalTimeSeries {
            val points = (startYear..yearNow).map { yr ->
                HistoricalPoint(
                    periodLabel = yr.toString(),
                    timestamp = yr.toString(),
                    qualityScore = -1,
                    violationCount = violations.count { it.year == yr },
                    droughtIndex = if (yr == yearNow) droughtIndex(droughtLevel) else 0,
                    contaminantLevels = emptyMap(),
                    milestoneNote = null
                )
            }
            val openCount = violations.count { it.status == "open" }
            return HistoricalTimeSeries(
                timeframe = frame,
                points = points,
                scoreDeltaPts = 0,
                diagnostics = listOf(
                    TrendDiagnostic(
                        id = "diag-${frame.key}-$pwsid",
                        timeframe = frame,
                        direction = "stable",
                        title = "Compliance counts",
                        summary = "Each point is the number of compliance records that began that year. A water score for past years is not in the public feed.",
                        supplyIndication = if (openCount == 0) {
                            "No open violations in the loaded record."
                        } else {
                            "$openCount open violation${if (openCount == 1) "" else "s"} have no return-to-compliance date."
                        },
                        recommendedAction = "Compare the latest reported sample with its legal limit."
                    )
                )
            )
        }

        val seriesMap = mapOf(
            TrendTimeframe.ONE_YEAR to series(yearNow, TrendTimeframe.ONE_YEAR),
            TrendTimeframe.FIVE_YEAR to series(yearNow - 4, TrendTimeframe.FIVE_YEAR),
            TrendTimeframe.TEN_YEAR to series(yearNow - 9, TrendTimeframe.TEN_YEAR)
        )

        val alerts = mutableListOf<QualityChangeAlert>()
        val openHealth = violations.filter { it.status == "open" && it.type == "health" }
        if (openHealth.isNotEmpty()) {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-open-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = "caution",
                    changeType = "new_violation",
                    deltaLabel = "${openHealth.size} open",
                    title = "${openHealth.size} open health-based violation${if (openHealth.size == 1) "" else "s"}",
                    message = "These records do not include a return-to-compliance date.",
                    supplyExplanation = "An open record is not the same as a current boil notice. Check the utility and the EPA facility report.",
                    detectedAt = "Current record",
                    linkedTab = WorkspaceTab.TICKETS
                )
            )
        }
        if (droughtLevel.startsWith("D")) {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-drought-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = if (droughtLevel == "D3" || droughtLevel == "D4") "caution" else "info",
                    changeType = "drought_shift",
                    deltaLabel = droughtLevel,
                    title = "Drought Monitor lists $droughtLevel",
                    message = droughtLabel(droughtLevel) + " at this map point.",
                    supplyExplanation = "This is the current drought category only. It is not a change in contaminant levels.",
                    detectedAt = "Current",
                    linkedTab = WorkspaceTab.SOURCE
                )
            )
        }
        return Triple(seriesMap, alerts, Pair(0, 0))
    }

    fun buildOfficialTickets(
        pwsid: String,
        systemName: String,
        zip: String,
        violations: List<ViolationRecord>,
        contaminants: List<Contaminant>
    ): List<IssueTicket> {
        val tickets = mutableListOf<IssueTicket>()
        val openViols = violations.filter { it.status == "open" }.take(4)
        openViols.forEachIndexed { idx, v ->
            tickets.add(
                IssueTicket(
                    id = "EPA-${v.id}",
                    pwsid = pwsid,
                    systemName = systemName,
                    zip = zip,
                    isOfficialEpa = true,
                    category = "EPA Enforcement",
                    title = "Open ${v.type} record: ${v.contaminant}",
                    description = "EPA record ${v.id} (${v.date}): ${v.description}.",
                    status = "open",
                    severity = if (v.type == "health" || v.severity == "serious") "high" else "medium",
                    createdAt = v.date,
                    linkedContaminantCode = v.contaminantCode,
                    linkedContaminantName = v.contaminant,
                    affectedCount = 1,
                    notes = listOf(
                        "Copied from the EPA violation table. It stays open here when the record has no return-to-compliance date."
                    )
                )
            )
        }

        return tickets
    }

    fun buildHouseholdActions(
        report: WaterSystemReport,
        profile: HouseholdProfile
    ): Pair<List<PriorityAction>, List<FilterRecommendation>> {
        val actions = mutableListOf<PriorityAction>()
        val hasLead = report.contaminants.any { it.code == "PB90" && it.detected > 0.0 }
        val hasDbp = report.contaminants.any { (it.code == "2950" || it.code == "2456") && it.ExceedsPhgSafe() }
        val hasPfas = report.contaminants.any { it.pfas && it.detected > 0.0 }
        val hasOpenHealth = report.violations.any { it.status == "open" && it.type == "health" }

        when (profile) {
            HouseholdProfile.INFANT_EXPECTING -> {
                actions.add(
                    PriorityAction(
                        priority = "high",
                        title = "Use NSF/ANSI 53 or 58 filtered water for infant formula & drinking",
                        description = "Infants and expecting mothers are uniquely sensitive to trace lead, nitrates, and PFAS. Target the 0 ppb Public Health Goal rather than the 15 ppb legal limit.",
                        badgeTag = "Infants & Expecting"
                    )
                )
                actions.add(
                    PriorityAction(
                        priority = "high",
                        title = "Never use hot tap water for formula or cooking",
                        description = "Warm or hot water dissolves lead and copper from household pipes and fixtures much faster than cold water. Always draw cold filtered water and heat on the stove.",
                        badgeTag = "Zero-Lead Protocol"
                    )
                )
            }
            HouseholdProfile.OLD_PLUMBING -> {
                actions.add(
                    PriorityAction(
                        priority = "high",
                        title = "Flush cold tap for 60 seconds after overnight stagnation",
                        description = "Homes built before 1986 often have lead solder, brass valves, or galvanized service lines. Flushing replaces water that sat in premise piping overnight with fresh main water.",
                        badgeTag = "Pre-1986 Plumbing"
                    )
                )
                actions.add(
                    PriorityAction(
                        priority = "medium",
                        title = "Clean faucet aerator screens quarterly",
                        description = "Particulate lead and pipe scale can trap inside faucet aerator meshes. Unscrew and rinse aerators every 3 months.",
                        badgeTag = "Fixture Maintenance"
                    )
                )
            }
            HouseholdProfile.SENSITIVE -> {
                actions.add(
                    PriorityAction(
                        priority = "high",
                        title = "Filter to strict Public Health Goals (PHG) for all drinking water",
                        description = "Multi-stage Reverse Osmosis + Activated Carbon reduces metals, disinfection byproducts (TTHMs/HAA5), and PFAS below health-benchmark thresholds.",
                        badgeTag = "Sensitive Health"
                    )
                )
            }
            HouseholdProfile.GENERAL -> {
                if (hasOpenHealth || report.contaminants.any { it.exceedsMcl }) {
                    actions.add(
                        PriorityAction(
                            priority = "high",
                            title = "Filter tap water used for drinking and cooking",
                            description = "Active compliance or legal limit exceedances are on file for ${report.name}. Point-of-use certified filtration is strongly advised.",
                            badgeTag = "Compliance Protection"
                        )
                    )
                } else {
                    actions.add(
                        PriorityAction(
                            priority = "medium",
                            title = "Use carbon filtration to reduce disinfection byproducts & improve taste",
                            description = "Even when water meets all EPA legal limits, activated carbon significantly lowers TTHMs, HAA5, and residual chlorine.",
                            badgeTag = "General Household"
                        )
                    )
                }
            }
        }

        if (report.droughtLevel != "none") {
            actions.add(
                PriorityAction(
                    priority = if (report.droughtLevel == "D3" || report.droughtLevel == "D4") "high" else "medium",
                    title = "Follow stage ${report.droughtLevel} drought outdoor conservation target (${report.usage.recommendedGallonsPerDay} gal/day)",
                    description = report.droughtDescription,
                    badgeTag = "Drought Context"
                )
            )
        }

        if (hasLead && profile == HouseholdProfile.GENERAL) {
            actions.add(
                PriorityAction(
                    priority = "medium",
                    title = "Run cold water 30–60s after sitting & use cold water for cooking",
                    description = "90th percentile lead is ${fmtVal(report.contaminants.find { it.code == "PB90" }?.detected)} ppb. Simple flushing minimizes premise plumbing pickup.",
                    badgeTag = "Lead Hygiene"
                )
            )
        }

        val filters = listOf(
            FilterRecommendation(
                name = "Under-Sink Reverse Osmosis + Carbon Block",
                type = "RO + Activated Carbon",
                certificationBadge = "NSF/ANSI 58 + 53 + P473",
                reductionPercent = 97,
                costTier = "Higher investment ($180–$350)",
                description = "Gold-standard barrier for Lead, Arsenic, Nitrate, Fluoride, PFOA/PFOS, and Disinfection Byproducts.",
                targets = listOf("Lead (PB90)", "PFOA/PFOS", "Nitrate", "Arsenic", "TTHMs"),
                recommendedForProfile = profile == HouseholdProfile.INFANT_EXPECTING || profile == HouseholdProfile.SENSITIVE || hasPfas
            ),
            FilterRecommendation(
                name = "Solid Carbon Block Faucet or Under-Sink Filter",
                type = "Catalytic / Solid Carbon Block",
                certificationBadge = "NSF/ANSI 53 + 42 + 401",
                reductionPercent = 92,
                costTier = "Moderate cost ($45–$120)",
                description = "Certified for 99% particulate & soluble Lead reduction plus TTHMs, HAA5, cysts, and chlorine taste/odor.",
                targets = listOf("Lead (PB90)", "Copper", "TTHMs", "HAA5", "Chlorine"),
                recommendedForProfile = profile == HouseholdProfile.OLD_PLUMBING || profile == HouseholdProfile.GENERAL || hasLead
            ),
            FilterRecommendation(
                name = "Advanced Pitcher Filter (Lead & DBP Rated)",
                type = "Ion-Exchange + Activated Carbon",
                certificationBadge = "NSF/ANSI 53 + 42",
                reductionPercent = 82,
                costTier = "Entry cost ($30–$55)",
                description = "Convenient renter-friendly option for reducing seasonal TTHMs, chlorine odor, and dissolved metals; verify NSF-53 seal on replacement cartridges.",
                targets = listOf("TTHMs", "Chlorine Taste", "Lead", "Copper"),
                recommendedForProfile = profile == HouseholdProfile.GENERAL && !hasOpenHealth && hasDbp
            )
        )

        return Pair(actions, filters)
    }

    private fun Contaminant.ExceedsPhgSafe(): Boolean = healthGoal != null && detected > healthGoal

    fun droughtIndex(level: String): Int = when (level) {
        "D0" -> 1
        "D1" -> 2
        "D2" -> 3
        "D3" -> 4
        "D4" -> 5
        else -> 0
    }

    fun droughtLabel(level: String): String = when (level) {
        "none" -> "No drought"
        "D0" -> "Abnormally dry (D0)"
        "D1" -> "Moderate drought (D1)"
        "D2" -> "Severe drought (D2)"
        "D3" -> "Extreme drought (D3)"
        "D4" -> "Exceptional drought (D4)"
        "unknown" -> "Not available"
        else -> level
    }

    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0

    fun fmtVal(v: Double?): String {
        if (v == null || v.isNaN()) return "—"
        if (v == 0.0) return "0"
        if (abs(v) < 0.01) return String.format("%.3f", v)
        if (abs(v) >= 100) return String.format("%.0f", v)
        return String.format("%.2f", v).trimEnd('0').trimEnd('.')
    }

    fun contaminantAmazonFilterUrl(contaminant: Contaminant): String = when (contaminant.code) {
        "PB90" -> "https://www.amazon.com/s?k=NSF+53+lead+water+filter+pitcher+faucet+under+sink"
        "CU90" -> "https://www.amazon.com/s?k=NSF+53+copper+and+lead+drinking+water+filter"
        "2950", "2456" -> "https://www.amazon.com/s?k=NSF+53+42+activated+carbon+water+filter+TTHM+chlorine+byproducts"
        "2806", "2805" -> "https://www.amazon.com/s?k=NSF+53+58+P473+PFAS+PFOA+PFOS+water+filter"
        "1040" -> "https://www.amazon.com/s?k=NSF+58+reverse+osmosis+nitrate+water+filter+system"
        "1005" -> "https://www.amazon.com/s?k=NSF+58+reverse+osmosis+arsenic+water+filter+system"
        "1025" -> "https://www.amazon.com/s?k=NSF+58+reverse+osmosis+fluoride+water+filter"
        else -> "https://www.amazon.com/s?k=NSF+53+58+certified+drinking+water+filter"
    }

    fun contaminantAmazonTestKitUrl(contaminant: Contaminant): String = when (contaminant.code) {
        "PB90", "CU90" -> "https://www.amazon.com/s?k=EPA+lead+and+copper+in+drinking+water+home+test+kit"
        "2806", "2805" -> "https://www.amazon.com/s?k=PFAS+forever+chemicals+mail+in+water+test+kit"
        "1040" -> "https://www.amazon.com/s?k=nitrate+nitrite+drinking+water+test+kit+strips"
        "1005" -> "https://www.amazon.com/s?k=arsenic+in+drinking+water+test+kit"
        "2950", "2456" -> "https://www.amazon.com/s?k=complete+home+drinking+water+test+kit+chlorine+VOC"
        else -> "https://www.amazon.com/s?k=16+in+1+drinking+water+test+kit+lead+copper+nitrate"
    }

    fun buildSuggestedProductsForReport(report: WaterSystemReport): List<SuggestedProductLink> {
        val reported = report.contaminants.filter { it.reported }
        val overLimit = reported.filter { it.exceedsMcl }.maxByOrNull { c ->
            val limit = c.mcl ?: 1.0
            if (limit <= 0.0) 0.0 else c.detected / limit
        }
        val optional = if (overLimit != null) {
            null
        } else {
            reported.filter { c ->
                (c.code == "CU90" && c.exceedsHealthGoal) || (c.code == "PB90" && c.detected > 0.0)
            }.maxByOrNull { it.detected }
        }
        val match = overLimit ?: optional ?: return emptyList()
        val overLegal = overLimit != null
        val query = when (match.code) {
            "PB90" -> "https://www.amazon.com/s?k=NSF+ANSI+53+lead+reduction+drinking+water+filter"
            "CU90" -> "https://www.amazon.com/s?k=NSF+ANSI+53+copper+reduction+drinking+water+filter"
            else -> "https://www.amazon.com/s?k=NSF+ANSI+certified+drinking+water+filter"
        }
        val title = when (match.code) {
            "PB90" -> "Lead-reduction filter"
            "CU90" -> "Copper-reduction filter"
            else -> "Certified drinking-water filter"
        }
        val reason = if (overLegal) {
            "${match.name.substringBefore(" (")} is ${fmtVal(match.detected)} ${match.unit}, over the legal limit of ${fmtVal(match.mcl)} ${match.unit}."
        } else if (match.code == "PB90") {
            "Lead was detected at ${fmtVal(match.detected)} ${match.unit}, under the 15 ppb action level. The health goal is zero. A filter is optional, not required by this sample."
        } else {
            "${match.name.substringBefore(" (")} is ${fmtVal(match.detected)} ${match.unit}, under the legal limit of ${fmtVal(match.mcl)} ${match.unit} and above the health goal of ${fmtVal(match.healthGoal)} ${match.unit}."
        }
        return listOf(
            SuggestedProductLink(
                id = "match-${match.code}",
                title = title,
                subtitle = if (overLegal) {
                    "Matched to a sample that is over its legal limit."
                } else {
                    "Optional. The sample is still under its legal limit."
                },
                certificationBadge = "Look for NSF/ANSI 53 or 58",
                priceTier = "Price set by the seller",
                partnerLabel = "Filter suggestion",
                matchReason = reason,
                targetCodes = listOf(match.code),
                amazonUrl = MonetizationConfig.amazonUrl(query),
                secondaryLabel = "",
                secondaryUrl = "",
                disclosure = MonetizationConfig.AFFILIATE_DISCLOSURE
            )
        )
    }

    fun buildWhereTabConservationProducts(report: WaterSystemReport): List<SuggestedProductLink> = emptyList()

    fun homeFeaturedKitLinks(): List<SuggestedProductLink> = emptyList()

    fun fmtSigned(n: Int): String = if (n > 0) "+$n" else n.toString()
}
