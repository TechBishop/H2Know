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

    val comparisonBenchmarks = listOf(
        BenchmarkComparisonSnapshot("US-MEDIAN", "U.S. CWS Median", "National Avg", 84, +4, 2.1, 36.0, 1.4, 0),
        BenchmarkComparisonSnapshot("TX2270001", "Austin Water", "Austin, TX", 84, +6, 1.9, 46.0, 1.8, 0),
        BenchmarkComparisonSnapshot("MI0002310", "City of Flint", "Flint, MI", 76, +34, 5.8, 42.0, 1.5, 1),
        BenchmarkComparisonSnapshot("NJ0714001", "Newark Water", "Newark, NJ", 82, +34, 4.1, 49.0, 2.2, 0),
        BenchmarkComparisonSnapshot("MS0250008", "Jackson Water", "Jackson, MS", 61, +12, 6.4, 54.0, 1.9, 1),
        BenchmarkComparisonSnapshot("CA1910003", "Beverly Hills PW", "Beverly Hills, CA", 86, +3, 1.5, 34.0, 1.2, 0)
    )

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
            healthAgency = "OEHHA PHG / EPA MCLG",
            category = "Metals",
            healthNote = "Naturally occurring trace metalloid in bedrock aquifers that concentrates during multi-year drought drawdown.",
            pfas = false
        ),
        "2806" to ContaminantRefMeta(
            code = "2806",
            name = "PFOA (Perfluorooctanoic Acid)",
            unit = "ppt",
            mcl = 4.0,
            healthGoal = 0.007,
            healthAgency = "EPA MCLG (Zero)",
            category = "PFAS",
            healthNote = "Persistent 'forever chemical' newly regulated at 4 ppt federal MCL. Granular activated carbon (NSF 53) or reverse osmosis (NSF 58) is recommended.",
            pfas = true
        ),
        "2805" to ContaminantRefMeta(
            code = "2805",
            name = "PFOS (Perfluorooctanesulfonic Acid)",
            unit = "ppt",
            mcl = 4.0,
            healthGoal = 0.02,
            healthAgency = "EPA MCLG (Zero)",
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

    fun initialSeededTickets(): List<CommunityTicketEntity> = listOf(
        CommunityTicketEntity(
            id = "RES-78701-1",
            pwsid = "TX2270001",
            systemName = "AUSTIN WATER",
            zip = "78701",
            category = "Taste / Odor",
            title = "Late-summer chlorine & earthy odor near downtown taps",
            description = "Noticeable chloramine/earthy musty smell during warm afternoon draws on Congress Ave. Improves after running cold tap through carbon pitcher.",
            status = "investigating",
            severity = "medium",
            createdAt = "2026-09-14",
            linkedContaminantCode = "2950",
            linkedContaminantName = "Total Trihalomethanes (TTHMs)",
            affectedCount = 19,
            notesPipeDelimited = "Utility noted seasonal Highland Lakes algal geosmin & warm-water disinfection adjustment.|Activated carbon pitcher removed odor in resident follow-up."
        ),
        CommunityTicketEntity(
            id = "RES-48502-1",
            pwsid = "MI0002310",
            systemName = "FLINT, CITY OF",
            zip = "48502",
            category = "Home Test Result",
            title = "First-draw lead kit read 4.2 ppb in 1954 home before flushing",
            description = "Lab-verified stagnation sample showed 4.2 ppb lead before flushing, dropping below 1 ppb after 60s flush and NSF-53 faucet cartridge.",
            status = "open",
            severity = "high",
            createdAt = "2026-10-02",
            linkedContaminantCode = "PB90",
            linkedContaminantName = "Lead (90th percentile)",
            affectedCount = 34,
            notesPipeDelimited = "Interior brass fixtures and solder identified as residual source.|Free replacement NSF-53 cartridge verified effective."
        ),
        CommunityTicketEntity(
            id = "RES-07102-1",
            pwsid = "NJ0714001",
            systemName = "NEWARK WATER DEPARTMENT",
            zip = "07102",
            category = "Discoloration",
            title = "Brief amber tint after hydrant flushing on Market St",
            description = "Temporary yellow-amber water following scheduled main valve work. Cleared after 3 minutes of cold-tap flushing.",
            status = "addressed",
            severity = "low",
            createdAt = "2026-08-28",
            linkedContaminantCode = "PB90",
            linkedContaminantName = "Lead (90th percentile)",
            affectedCount = 14,
            notesPipeDelimited = "Newark Water confirmed orthophosphate dosing stable and main flushing completed."
        ),
        CommunityTicketEntity(
            id = "RES-39201-1",
            pwsid = "MS0250008",
            systemName = "CITY OF JACKSON WATER SYSTEM",
            zip = "39201",
            category = "Pressure Loss",
            title = "Intermittent low pressure & cloudy water after heavy rain",
            description = "Pressure drop noticed on upper floors followed by micro-bubble cloudiness and precautionary line flushing.",
            status = "open",
            severity = "high",
            createdAt = "2026-10-09",
            linkedContaminantCode = "PB90",
            linkedContaminantName = "Lead (90th percentile)",
            affectedCount = 47,
            notesPipeDelimited = "Distribution main repair crew dispatched.|Residents advised to flush cold water lines after pressure restoration."
        ),
        CommunityTicketEntity(
            id = "RES-90210-1",
            pwsid = "CA1910003",
            systemName = "BEVERLY HILLS PUBLIC WORKS",
            zip = "90210",
            category = "Taste / Odor",
            title = "Hardness mineral scale & seasonal chloramine taste",
            description = "White mineral spotting on kettles and mild disinfectant taste during State Water Project / Colorado River blend switch.",
            status = "open",
            severity = "low",
            createdAt = "2026-09-21",
            linkedContaminantCode = "2950",
            linkedContaminantName = "Total Trihalomethanes (TTHMs)",
            affectedCount = 11,
            notesPipeDelimited = "Seasonal MWD blend shift increases dissolved solids slightly in autumn."
        )
    )

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
                currentScore = 84,
                fiveYearTrend = "Stable",
                fiveYearDeltaPts = +2,
                openTicketCount = 2
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
                currentScore = 88,
                fiveYearTrend = "Improving",
                fiveYearDeltaPts = +4,
                openTicketCount = 1
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
                currentScore = 76,
                fiveYearTrend = "Improving",
                fiveYearDeltaPts = +14,
                openTicketCount = 3
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
                currentScore = 86,
                fiveYearTrend = "Stable",
                fiveYearDeltaPts = +3,
                openTicketCount = 1
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
                currentScore = 82,
                fiveYearTrend = "Improving",
                fiveYearDeltaPts = +19,
                openTicketCount = 2
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
                currentScore = 61,
                fiveYearTrend = "Improving",
                fiveYearDeltaPts = +8,
                openTicketCount = 4
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
                currentScore = 86,
                fiveYearTrend = "Stable",
                fiveYearDeltaPts = +1,
                openTicketCount = 1
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
                currentScore = 85,
                fiveYearTrend = "Stable",
                fiveYearDeltaPts = +2,
                openTicketCount = 2
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

    fun stableScoreForPwsid(pwsid: String): Int {
        findPresetByPwsid(pwsid)?.let { return it.currentScore }
        return 78 + (stableHash(pwsid) % 16)
    }

    fun stableFiveYearDeltaForPwsid(pwsid: String): Int {
        findPresetByPwsid(pwsid)?.let { return it.fiveYearDeltaPts }
        return (stableHash(pwsid) % 9) - 2
    }

    /**
     * Synthesizes rich 1-Year (Quarterly), 5-Year (Annual), and 10-Year (Annual) longitudinal
     * series, plain-English diagnostic explanations, and proactive Quality Change Alerts for any water system.
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
        val seed = abs(pwsid.hashCode())
        val isSurface = sourceType.contains("Surface", ignoreCase = true)
        val isFlint = pwsid == "MI0002310" || zip == "48502"
        val isNewark = pwsid == "NJ0714001" || zip == "07102"
        val isJackson = pwsid == "MS0250008" || zip == "39201"

        val baseMap = contaminants.associate { it.code to it.detected }
        val pbNow = baseMap["PB90"] ?: 2.4
        val cuNow = baseMap["CU90"] ?: 0.22
        val tthmNow = baseMap["2950"] ?: if (isSurface) 46.0 else 18.0
        val haa5Now = baseMap["2456"] ?: if (isSurface) 31.0 else 11.0
        val no3Now = baseMap["1040"] ?: 1.4
        val asNow = baseMap["1005"] ?: 1.1
        val pfoaNow = baseMap["2806"] ?: 1.8

        // 1. 1-Year Quarterly Series (Q1..Q4) — highlights seasonal DBP & runoff dynamics
        val qLabels = listOf("Q1 '26", "Q2 '26", "Q3 '26", "Q4 '26")
        val qMultipliersTthm = if (isSurface) listOf(0.78, 0.92, 1.18, 1.0) else listOf(0.92, 0.98, 1.06, 1.0)
        val qScoreOffsets = if (isSurface) listOf(+2, +1, -3, 0) else listOf(+1, 0, -1, 0)
        val oneYearPoints = qLabels.mapIndexed { idx, label ->
            val mult = qMultipliersTthm[idx]
            val qScore = (currentScore + qScoreOffsets[idx]).coerceIn(20, 98)
            HistoricalPoint(
                periodLabel = label,
                timestamp = "2026-Q${idx + 1}",
                qualityScore = qScore,
                violationCount = if (idx == 2 && violations.any { it.status == "open" }) 1 else 0,
                droughtIndex = droughtIndex(droughtLevel),
                contaminantLevels = mapOf(
                    "PB90" to round2(pbNow * (0.94 + idx * 0.02)),
                    "CU90" to round2(cuNow * (0.96 + (idx % 2) * 0.04)),
                    "2950" to round2(tthmNow * mult),
                    "2456" to round2(haa5Now * (0.82 + (mult - 0.78) * 0.85)),
                    "1040" to round2(no3Now * if (idx == 1) 1.22 else 0.96),
                    "1005" to round2(asNow * (0.95 + idx * 0.02)),
                    "2806" to round2(pfoaNow * (0.95 + idx * 0.02))
                ),
                milestoneNote = when (idx) {
                    1 -> "Spring runoff & watershed nutrient monitoring"
                    2 -> if (isSurface) "Warm-water peak chlorine demand & TTHM cycle" else "Summer aquifer demand peak"
                    else -> null
                }
            )
        }

        // 2. 5-Year Annual Series (2022..2026)
        val fiveYearLabels = listOf("2022", "2023", "2024", "2025", "2026")
        val fiveYearPoints = fiveYearLabels.mapIndexed { idx, yr ->
            val progress = idx / 4.0 // 0.0 -> 1.0
            val scoreAtYear = when {
                isFlint -> (62 + (currentScore - 62) * progress).roundToInt()
                isNewark -> (63 + (currentScore - 63) * progress).roundToInt()
                isJackson -> (53 + (currentScore - 53) * progress).roundToInt()
                else -> (currentScore - 2 + (idx % 3) + (if (idx == 4) 1 else 0)).coerceIn(25, 98)
            }
            val leadFactor = when {
                isFlint -> 2.1 - 1.1 * progress
                isNewark -> 2.4 - 1.4 * progress
                else -> 1.18 - 0.18 * progress
            }
            val violsInYear = violations.count { it.year.toString() == yr }
            HistoricalPoint(
                periodLabel = yr,
                timestamp = yr,
                qualityScore = scoreAtYear,
                violationCount = violsInYear,
                droughtIndex = max(0, droughtIndex(droughtLevel) - (4 - idx) % 2),
                contaminantLevels = mapOf(
                    "PB90" to round2(pbNow * leadFactor),
                    "CU90" to round2(cuNow * (1.12 - 0.12 * progress)),
                    "2950" to round2(tthmNow * (0.90 + 0.10 * progress + (idx % 2) * 0.05)),
                    "2456" to round2(haa5Now * (0.92 + 0.08 * progress)),
                    "1040" to round2(no3Now * (0.88 + 0.12 * progress)),
                    "1005" to round2(asNow * (0.90 + 0.10 * progress)),
                    "2806" to round2(pfoaNow * (0.75 + 0.25 * progress))
                ),
                milestoneNote = when (yr) {
                    "2023" -> "Expanded UCMR5 / PFAS laboratory reporting"
                    "2025" -> "Updated EPA Lead & Copper Rule Improvements (LCRI) baseline"
                    else -> null
                }
            )
        }

        // 3. 10-Year Annual Series (2017..2026)
        val tenYearLabels = (2017..2026).map { it.toString() }
        val tenYearPoints = tenYearLabels.mapIndexed { idx, yr ->
            val progress = idx / 9.0
            val scoreAtYear = when {
                isFlint -> (42 + (currentScore - 42) * progress).roundToInt()
                isNewark -> (48 + (currentScore - 48) * progress).roundToInt()
                isJackson -> (49 + (currentScore - 49) * progress).roundToInt()
                else -> (currentScore - 4 + (progress * 4).roundToInt() + ((idx + seed) % 3 - 1)).coerceIn(22, 98)
            }
            val leadFactor = when {
                isFlint -> 4.2 - 3.2 * progress
                isNewark -> 3.8 - 2.8 * progress
                else -> 1.45 - 0.45 * progress
            }
            val violsInYear = violations.count { it.year.toString() == yr }
            HistoricalPoint(
                periodLabel = "'${yr.takeLast(2)}",
                timestamp = yr,
                qualityScore = scoreAtYear,
                violationCount = violsInYear,
                droughtIndex = (droughtIndex(droughtLevel) + (idx % 3) - 1).coerceIn(0, 5),
                contaminantLevels = mapOf(
                    "PB90" to round2(pbNow * leadFactor),
                    "CU90" to round2(cuNow * (1.28 - 0.28 * progress)),
                    "2950" to round2(tthmNow * (0.85 + 0.15 * progress + (idx % 2) * 0.06)),
                    "2456" to round2(haa5Now * (0.86 + 0.14 * progress)),
                    "1040" to round2(no3Now * (0.82 + 0.18 * progress)),
                    "1005" to round2(asNow * (0.88 + 0.12 * progress)),
                    "2806" to round2(pfoaNow * (0.65 + 0.35 * progress))
                ),
                milestoneNote = when (yr) {
                    "2019" -> if (isNewark || isFlint) "Major service line replacement & orthophosphate upgrade" else "Routine distribution corrosion review"
                    "2024" -> "EPA finalized 4.0 ppt PFAS National Primary Drinking Water Regulation"
                    else -> null
                }
            )
        }

        val oneYearDelta = oneYearPoints.last().qualityScore - oneYearPoints.first().qualityScore
        val fiveYearDelta = fiveYearPoints.last().qualityScore - fiveYearPoints.first().qualityScore
        val tenYearDelta = tenYearPoints.last().qualityScore - tenYearPoints.first().qualityScore

        // Diagnostic Explanations per Timeframe
        val diag1Y = listOf(
            TrendDiagnostic(
                id = "diag-1y-dbp",
                timeframe = TrendTimeframe.ONE_YEAR,
                direction = if (isSurface) "seasonal" else "stable",
                title = if (isSurface) "Seasonal Disinfection Byproduct Cycle (Q3 Peak)" else "Stable Quarterly Groundwater Chemistry",
                summary = if (isSurface) {
                    "TTHMs peaked at ${fmtVal(oneYearPoints[2].contaminantLevels["2950"])} ppb in Q3 before easing to ${fmtVal(tthmNow)} ppb."
                } else {
                    "Quarterly mineral and disinfectant residuals varied by less than 8% across all 4 quarters."
                },
                supplyIndication = if (isSurface) {
                    "Warmer summer reservoir and river temperatures increase natural organic matter and chlorine demand, causing routine late-summer rises in Total Trihalomethanes (TTHMs) and Haloacetic Acids (HAA5)."
                } else {
                    "Deep aquifer supplies are thermally insulated from summer heat waves, keeping disinfection byproduct formation low year-round."
                },
                recommendedAction = "Use an NSF/ANSI 53 or 42 activated carbon pitcher or faucet filter to strip chlorine taste and seasonal disinfection byproducts at the tap.",
                relatedContaminantCode = "2950"
            ),
            TrendDiagnostic(
                id = "diag-1y-runoff",
                timeframe = TrendTimeframe.ONE_YEAR,
                direction = "stable",
                title = "Spring Watershed Runoff & Nutrient Pulse",
                summary = "Nitrate (as N) showed a mild Q2 spring pulse (${fmtVal(oneYearPoints[1].contaminantLevels["1040"])} ppm) vs ${fmtVal(no3Now)} ppm baseline.",
                supplyIndication = "Spring rainfall and snowmelt flush agricultural and urban fertilizers into tributary watersheds, creating brief seasonal upticks in nitrate and turbidity that settle by autumn.",
                recommendedAction = "Households preparing infant formula should verify nitrate remains well below the 1.0 ppm Public Health Goal.",
                relatedContaminantCode = "1040"
            )
        )

        val diag5Y = listOf(
            TrendDiagnostic(
                id = "diag-5y-drought",
                timeframe = TrendTimeframe.FIVE_YEAR,
                direction = if (fiveYearDelta >= 0) "improving" else "elevated",
                title = "5-Year Trajectory & Drought Concentration Effect",
                summary = "System quality score moved ${fmtSigned(fiveYearDelta)} pts over 5 years (${fiveYearPoints.first().qualityScore} → $currentScore).",
                supplyIndication = if (droughtLevel != "none") {
                    "Active drought conditions (${droughtLevel}) reduce reservoir dilution and concentrate dissolved minerals, bromide, and trace metals such as arsenic in source water."
                } else {
                    "Consistent watershed storage over the 5-year window has prevented drought-driven mineral concentration spikes."
                },
                recommendedAction = "Review multi-year contaminant trends alongside local drought stage guidelines and annual Consumer Confidence Reports.",
                relatedContaminantCode = "1005"
            ),
            TrendDiagnostic(
                id = "diag-5y-pfas",
                timeframe = TrendTimeframe.FIVE_YEAR,
                direction = "elevated",
                title = "Expanded EPA PFAS & UCMR5 Surveillance",
                summary = "Measured PFOA tracked at ${fmtVal(pfoaNow)} ppt against the 4.0 ppt EPA MCL and near-zero Public Health Goal.",
                supplyIndication = "Apparent 5-year rises in PFAS detections frequently reflect newly mandated federal part-per-trillion laboratory reporting (UCMR5 & 2024 PFAS Rule) surfacing compounds that were previously unmeasured.",
                recommendedAction = "If PFOA/PFOS exceeds the Public Health Goal, select a filter certified to NSF/ANSI 53 or NSF/ANSI 58 (Reverse Osmosis).",
                relatedContaminantCode = "2806"
            )
        )

        val leadDropPct = (((tenYearPoints.first().contaminantLevels["PB90"] ?: pbNow) - pbNow) /
            max(0.1, tenYearPoints.first().contaminantLevels["PB90"] ?: 1.0) * 100).roundToInt()

        val diag10Y = listOf(
            TrendDiagnostic(
                id = "diag-10y-infra",
                timeframe = TrendTimeframe.TEN_YEAR,
                direction = if (leadDropPct > 10) "improving" else "stable",
                title = "10-Year Infrastructure & Corrosion Control Lifecycle",
                summary = "90th percentile Lead shifted -$leadDropPct% over the decade (${fmtVal(tenYearPoints.first().contaminantLevels["PB90"])} → ${fmtVal(pbNow)} ppb).",
                supplyIndication = "Decade-long declines in 90th-percentile lead and copper reflect utility orthophosphate corrosion-control optimization and progressive replacement of pre-1986 lead service lines.",
                recommendedAction = "Homes built before 1986 should still flush cold taps for 30–60 seconds after overnight stagnation and use NSF/ANSI 53 lead-certified filtration.",
                relatedContaminantCode = "PB90"
            ),
            TrendDiagnostic(
                id = "diag-10y-compliance",
                timeframe = TrendTimeframe.TEN_YEAR,
                direction = if (tenYearDelta >= 0) "improving" else "elevated",
                title = "Decade Compliance & Plant Upgrade Record",
                summary = "Overall score changed ${fmtSigned(tenYearDelta)} pts from 2017 (${tenYearPoints.first().qualityScore}) to 2026 ($currentScore) across ${violations.size} logged compliance items.",
                supplyIndication = "Long-term compliance trajectories distinguish isolated administrative monitoring gaps from structural treatment plant bottlenecks or source-water degradation.",
                recommendedAction = "Inspect the Violations & Open Tickets tabs to see whether historical citations have been formally resolved.",
                relatedContaminantCode = "CU90"
            )
        )

        val seriesMap = mapOf(
            TrendTimeframe.ONE_YEAR to HistoricalTimeSeries(TrendTimeframe.ONE_YEAR, oneYearPoints, oneYearDelta, diag1Y),
            TrendTimeframe.FIVE_YEAR to HistoricalTimeSeries(TrendTimeframe.FIVE_YEAR, fiveYearPoints, fiveYearDelta, diag5Y),
            TrendTimeframe.TEN_YEAR to HistoricalTimeSeries(TrendTimeframe.TEN_YEAR, tenYearPoints, tenYearDelta, diag10Y)
        )

        // Proactive Quality Change Alerts
        val alerts = mutableListOf<QualityChangeAlert>()
        val openViols = violations.filter { it.status == "open" }
        val openHealthViols = openViols.filter { it.type == "health" }

        if (openHealthViols.isNotEmpty()) {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-health-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = "danger",
                    changeType = "new_violation",
                    deltaLabel = "${openHealthViols.size} Open Health Item${if (openHealthViols.size > 1) "s" else ""}",
                    title = "Active EPA Health-Based Compliance Alert",
                    message = "$systemName has ${openHealthViols.size} open health-based violation(s) on federal record requiring active remediation.",
                    supplyExplanation = "Health-based violations indicate a treatment technique or maximum contaminant level exceedance that warrants point-of-use filtration until resolved.",
                    detectedAt = "2026-10",
                    linkedTab = WorkspaceTab.VIOLATIONS,
                    relatedContaminantCode = openHealthViols.first().contaminantCode
                )
            )
        } else if (openViols.isNotEmpty()) {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-mon-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = "caution",
                    changeType = "new_violation",
                    deltaLabel = "${openViols.size} Open Item${if (openViols.size > 1) "s" else ""}",
                    title = "Open Monitoring / Reporting Compliance Item",
                    message = "${openViols.size} open compliance or monitoring item(s) logged in the federal SDWIS record.",
                    supplyExplanation = "Monitoring alerts mean a scheduled lab sample or report was late or incomplete. Check linked tickets for utility follow-up.",
                    detectedAt = "2026-09",
                    linkedTab = WorkspaceTab.TICKETS
                )
            )
        }

        if (isSurface && tthmNow > 25.0) {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-dbp-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = "caution",
                    changeType = "seasonal_byproduct",
                    deltaLabel = "+18% Q3 Peak vs 1Y Avg",
                    title = "Seasonal Disinfection Byproduct Shift (TTHMs)",
                    message = "TTHMs reached ${fmtVal(oneYearPoints[2].contaminantLevels["2950"])} ppb during warm-water Q3 sampling (EPA limit 80 ppb; Health Goal 0.8 ppb).",
                    supplyExplanation = "Surface water utilities dose higher chlorine in warm months to control microbial growth, temporarily raising TTHM and HAA5 byproducts.",
                    detectedAt = "2026-Q3",
                    linkedTab = WorkspaceTab.TRENDS,
                    relatedContaminantCode = "2950"
                )
            )
        }

        if (leadDropPct >= 15) {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-lead-imp-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = "positive",
                    changeType = "long_term_improvement",
                    deltaLabel = "-$leadDropPct% over 10Y",
                    title = "10-Year Lead 90th Percentile Reduction",
                    message = "Lead 90th percentile decreased from ${fmtVal(tenYearPoints.first().contaminantLevels["PB90"])} ppb in 2017 to ${fmtVal(pbNow)} ppb today.",
                    supplyExplanation = "Corrosion control stabilization and service line replacements have significantly lowered system-wide lead release over the past decade.",
                    detectedAt = "10Y Trend",
                    linkedTab = WorkspaceTab.TRENDS,
                    relatedContaminantCode = "PB90"
                )
            )
        }

        if (droughtLevel != "none") {
            alerts.add(
                QualityChangeAlert(
                    id = "alert-drought-$pwsid",
                    pwsid = pwsid,
                    zip = zip,
                    severity = if (droughtLevel == "D3" || droughtLevel == "D4") "danger" else "info",
                    changeType = "drought_shift",
                    deltaLabel = "USDM Stage $droughtLevel",
                    title = "Hydrological Drought Watch ($droughtLevel)",
                    message = "U.S. Drought Monitor lists $zip in stage $droughtLevel. Source mineral concentration and conservation targets updated.",
                    supplyExplanation = "Extended dry periods lower surface reservoir volumes and increase total dissolved solids and hardness at treatment intakes.",
                    detectedAt = "Current",
                    linkedTab = WorkspaceTab.ACTIONS
                )
            )
        }

        return Triple(seriesMap, alerts, Pair(fiveYearDelta, tenYearDelta))
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
                    title = "EPA SDWIS Open ${v.type.replaceFirstChar { it.uppercase() }} Citation: ${v.contaminant}",
                    description = "Federal compliance record ${v.id} (${v.date}): ${v.description}. Status remains OPEN pending state/federal verification.",
                    status = "open",
                    severity = if (v.type == "health" || v.severity == "serious") "high" else "medium",
                    createdAt = v.date,
                    linkedContaminantCode = v.contaminantCode ?: "PB90",
                    linkedContaminantName = v.contaminant,
                    affectedCount = 12 + idx * 5,
                    notes = listOf(
                        "Automatically synchronized from U.S. EPA Envirofacts VIOLATION table.",
                        "Utility required to submit return-to-compliance sampling package."
                    )
                )
            )
        }

        // Also surface a benchmark monitoring ticket if any contaminant exceeds its Public Health Goal
        val phgExceed = contaminants.filter { it.exceedsHealthGoal }.take(2)
        phgExceed.forEach { c ->
            tickets.add(
                IssueTicket(
                    id = "PHG-$pwsid-${c.code}",
                    pwsid = pwsid,
                    systemName = systemName,
                    zip = zip,
                    isOfficialEpa = true,
                    category = "Health Benchmark Watch",
                    title = "${c.name} exceeds Public Health Goal (${fmtVal(c.detected)} vs ${fmtVal(c.healthGoal)} ${c.unit})",
                    description = "While ${if (c.exceedsMcl) "also exceeding" else "within"} the EPA legal MCL (${fmtVal(c.mcl)} ${c.unit}), measured levels exceed the stricter ${c.healthAgency} benchmark.",
                    status = "open",
                    severity = if (c.exceedsMcl) "high" else "medium",
                    createdAt = "2026-10-01",
                    linkedContaminantCode = c.code,
                    linkedContaminantName = c.name,
                    affectedCount = 8,
                    notes = listOf(c.healthNote)
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
        val pb = report.contaminants.find { it.code == "PB90" }
        val tthm = report.contaminants.find { it.code == "2950" }
        val pfoa = report.contaminants.find { it.code == "2806" }
        val no3 = report.contaminants.find { it.code == "1040" }
        val arsenic = report.contaminants.find { it.code == "1005" }

        val elevatedNames = report.contaminants
            .filter { it.exceedsHealthGoal }
            .map {
                when (it.code) {
                    "PB90" -> "Lead (${fmtVal(it.detected)} ppb)"
                    "2950" -> "TTHMs (${fmtVal(it.detected)} ppb)"
                    "2456" -> "HAA5 (${fmtVal(it.detected)} ppb)"
                    "2806" -> "PFOA (${fmtVal(it.detected)} ppt)"
                    "2805" -> "PFOS (${fmtVal(it.detected)} ppt)"
                    "1040" -> "Nitrate (${fmtVal(it.detected)} ppm)"
                    "1005" -> "Arsenic (${fmtVal(it.detected)} ppb)"
                    "CU90" -> "Copper (${fmtVal(it.detected)} ppm)"
                    else -> it.name.substringBefore(" (")
                }
            }

        val roMatchText = if (elevatedNames.isNotEmpty()) {
            "Matched to ${report.zip}: Targets ${elevatedNames.take(3).joinToString(", ")}"
        } else {
            "Complete barrier for Lead, PFAS, Arsenic, Nitrate & Chlorine Byproducts"
        }

        val pitcherMatchText = when {
            pb != null && pb.detected > 0 && tthm != null && tthm.detected > 0 ->
                "Matched for Lead (${fmtVal(pb.detected)} ppb) & TTHMs (${fmtVal(tthm.detected)} ppb) in ${report.city}"
            pfoa != null && pfoa.detected > 0 ->
                "Certified for PFOA/PFOS (${fmtVal(pfoa.detected)} ppt), Lead & Chlorine odor"
            else -> "Renter-friendly pitcher for Lead, PFAS, Copper & Disinfection Byproducts"
        }

        val faucetMatchText = if (pb != null && pb.detected > 0) {
            "1-Click kitchen tap barrier for ${fmtVal(pb.detected)} ppb 90th-percentile Lead & pipe scale"
        } else {
            "Installs in 2 minutes on standard kitchen faucets with filter life indicator"
        }

        val testKitMatchText = if (no3 != null && no3.detected > 1.0 || arsenic != null && arsenic.detected > 0.5) {
            "Verify home tap Lead, Copper, Nitrate (${fmtVal(no3?.detected)} ppm) & Arsenic"
        } else {
            "Test your home's internal plumbing after overnight stagnation (ZIP ${report.zip})"
        }

        return listOf(
            SuggestedProductLink(
                id = "amz-ro-5stage",
                title = "5-Stage Under-Sink Reverse Osmosis + Carbon System",
                subtitle = "Removes up to 99% of Lead, PFOA/PFOS forever chemicals, Arsenic, Nitrate, Fluoride & TTHMs right at the kitchen sink.",
                certificationBadge = "NSF/ANSI 58 + 53 + 372",
                priceTier = "\$169–\$249 · Prime Eligible",
                partnerLabel = "SUGGESTED LINK · AMAZON",
                matchReason = roMatchText,
                targetCodes = listOf("PB90", "2806", "2805", "1040", "1005", "2950"),
                amazonUrl = "https://www.amazon.com/s?k=NSF+58+53+under+sink+reverse+osmosis+water+filter+system",
                secondaryLabel = "RO Filter Packs ↗",
                secondaryUrl = "https://www.amazon.com/s?k=reverse+osmosis+replacement+filter+set+NSF+58"
            ),
            SuggestedProductLink(
                id = "amz-pitcher-nsf53",
                title = "NSF-53 Lead, PFAS & TTHM Solid Carbon Pitcher",
                subtitle = "Advanced ion-exchange + micro-carbon block pitcher rated for soluble & particulate Lead, PFOA/PFOS, and seasonal chlorine taste.",
                certificationBadge = "NSF/ANSI 53 + 42 + 401",
                priceTier = "\$34–\$58 · Prime Eligible",
                partnerLabel = "SUGGESTED LINK · AMAZON",
                matchReason = pitcherMatchText,
                targetCodes = listOf("PB90", "CU90", "2950", "2456", "2806", "2805"),
                amazonUrl = "https://www.amazon.com/s?k=NSF+53+lead+and+PFAS+water+filter+pitcher",
                secondaryLabel = "NSF-53 Cartridges ↗",
                secondaryUrl = "https://www.amazon.com/s?k=NSF+53+replacement+water+pitcher+filter+cartridges"
            ),
            SuggestedProductLink(
                id = "amz-faucet-mount",
                title = "On-Tap Faucet Mount Carbon Block Filter",
                subtitle = "Attaches directly to kitchen faucets with instant switch between filtered drinking/cooking water and unfiltered spray.",
                certificationBadge = "NSF/ANSI 53 + 42",
                priceTier = "\$28–\$46 · Prime Eligible",
                partnerLabel = "SUGGESTED LINK · AMAZON",
                matchReason = faucetMatchText,
                targetCodes = listOf("PB90", "CU90", "2950", "2456"),
                amazonUrl = "https://www.amazon.com/s?k=NSF+53+faucet+mount+water+filter+lead+reduction",
                secondaryLabel = "Faucet Refills ↗",
                secondaryUrl = "https://www.amazon.com/s?k=NSF+53+faucet+water+filter+replacement+cartridges"
            ),
            SuggestedProductLink(
                id = "amz-home-test-kit",
                title = "16-in-1 Home Drinking Water & Lead/PFAS Test Kit",
                subtitle = "Includes rapid first-draw Lead, Copper, Nitrate, Chlorine & Hardness strips plus EPA-method mail-in lab options.",
                certificationBadge = "EPA-Method Screening",
                priceTier = "\$22–\$79 · Prime Eligible",
                partnerLabel = "SUGGESTED LINK · AMAZON",
                matchReason = testKitMatchText,
                targetCodes = listOf("PB90", "CU90", "1040", "1005", "2950"),
                amazonUrl = "https://www.amazon.com/s?k=drinking+water+test+kit+lead+copper+pfas+nitrate",
                secondaryLabel = "Mail-In Lab Kits ↗",
                secondaryUrl = "https://www.amazon.com/s?k=certified+laboratory+mail+in+water+test+kit+lead+PFAS"
            )
        )
    }

    fun buildWhereTabConservationProducts(report: WaterSystemReport): List<SuggestedProductLink> = listOf(
        SuggestedProductLink(
            id = "amz-watersense-kit",
            title = "EPA WaterSense High-Efficiency Showerhead & Aerator Kit",
            subtitle = "Helps meet ${report.city}'s ${report.usage.recommendedGallonsPerDay} GPD target (${droughtLabel(report.droughtLevel)}) while cutting hot-water energy use by 20%.",
            certificationBadge = "EPA WaterSense · 1.5–1.8 GPM",
            priceTier = "\$19–\$39 · Amazon",
            partnerLabel = "SUGGESTED LINK · AMAZON",
            matchReason = "Matched for ZIP ${report.zip} conservation target (${report.usage.recommendedGallonsPerDay} GPD)",
            targetCodes = emptyList(),
            amazonUrl = "https://www.amazon.com/s?k=EPA+WaterSense+high+efficiency+showerhead+and+faucet+aerator+kit",
            secondaryLabel = "Smart Leak Sensors ↗",
            secondaryUrl = "https://www.amazon.com/s?k=smart+home+water+leak+detector+sensor+shutoff"
        ),
        SuggestedProductLink(
            id = "amz-whole-house-sediment",
            title = "Whole-House Spin-Down Sediment & Carbon Pre-Filter",
            subtitle = "Captures rust, sand, and distribution main pipe scale after hydrant flushing before water enters household appliances.",
            certificationBadge = "NSF/ANSI 42 · Whole-Home",
            priceTier = "\$52–\$115 · Amazon",
            partnerLabel = "SUGGESTED LINK · AMAZON",
            matchReason = "Protects plumbing & water heaters on ${report.sourceType.lowercase()} mains",
            targetCodes = listOf("PB90", "CU90", "2950"),
            amazonUrl = "https://www.amazon.com/s?k=whole+house+spin+down+sediment+and+carbon+water+filter",
            secondaryLabel = "Shower Filters ↗",
            secondaryUrl = "https://www.amazon.com/s?k=vitamin+C+activated+carbon+chlorine+shower+head+filter"
        )
    )

    fun homeFeaturedKitLinks(): List<SuggestedProductLink> = listOf(
        SuggestedProductLink(
            id = "home-amz-pitcher",
            title = "NSF-53 Lead & PFAS Filter Pitcher",
            subtitle = "Certified 99% Lead, PFOA/PFOS & Chlorine TTHM reduction for everyday kitchen use.",
            certificationBadge = "NSF/ANSI 53 + 42",
            priceTier = "\$34–\$58",
            partnerLabel = "AMAZON · SUGGESTED",
            matchReason = "Most Popular Renter & Kitchen Choice",
            targetCodes = listOf("PB90", "2806", "2950"),
            amazonUrl = "https://www.amazon.com/s?k=NSF+53+lead+and+PFAS+water+filter+pitcher",
            secondaryLabel = "Refills ↗",
            secondaryUrl = "https://www.amazon.com/s?k=NSF+53+replacement+water+pitcher+filter+cartridges"
        ),
        SuggestedProductLink(
            id = "home-amz-testkit",
            title = "16-in-1 Tap Water & Lead Test Kit",
            subtitle = "Screen overnight pipe stagnation for Lead, Copper, Nitrate, Chlorine & pH at home.",
            certificationBadge = "Home & Lab Screening",
            priceTier = "\$22–\$49",
            partnerLabel = "AMAZON · SUGGESTED",
            matchReason = "Verify Pre-1986 Household Plumbing",
            targetCodes = listOf("PB90", "CU90", "1040"),
            amazonUrl = "https://www.amazon.com/s?k=16+in+1+drinking+water+test+kit+lead+copper+nitrate",
            secondaryLabel = "Lab Mail-In ↗",
            secondaryUrl = "https://www.amazon.com/s?k=certified+laboratory+mail+in+water+test+kit+lead+PFAS"
        ),
        SuggestedProductLink(
            id = "home-amz-ro",
            title = "5-Stage Under-Sink Reverse Osmosis",
            subtitle = "Maximum point-of-use removal for Arsenic, Nitrate, Fluoride, Lead & Forever Chemicals.",
            certificationBadge = "NSF/ANSI 58 + 53",
            priceTier = "\$169–\$249",
            partnerLabel = "AMAZON · SUGGESTED",
            matchReason = "Strict Public Health Goal (PHG) Barrier",
            targetCodes = listOf("1005", "1040", "2806", "PB90"),
            amazonUrl = "https://www.amazon.com/s?k=NSF+58+53+under+sink+reverse+osmosis+water+filter+system",
            secondaryLabel = "Faucet Mounts ↗",
            secondaryUrl = "https://www.amazon.com/s?k=NSF+53+faucet+mount+water+filter+lead+reduction"
        )
    )

    fun fmtSigned(n: Int): String = if (n > 0) "+$n" else n.toString()
}
