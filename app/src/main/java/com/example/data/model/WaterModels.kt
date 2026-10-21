package com.example.data.model

enum class WorkspaceTab(val key: String, val label: String) {
    OVERVIEW("overview", "Pulse"),
    TRENDS("trends", "Lab & Trends"),
    TICKETS("tickets", "Community"),
    ACTIONS("recs", "Tap Plan"),
    // Legacy aliases mapped in UI if referenced by alerts
    CONTAMINANTS("contaminants", "Lab & Trends"),
    VIOLATIONS("violations", "Community"),
    SOURCE("source", "Tap Plan");

    fun normalizedPrimaryTab(): WorkspaceTab = when (this) {
        OVERVIEW -> OVERVIEW
        TRENDS, CONTAMINANTS -> TRENDS
        TICKETS, VIOLATIONS -> TICKETS
        ACTIONS, SOURCE -> ACTIONS
    }

    companion object {
        val primaryTabs = listOf(OVERVIEW, TRENDS, TICKETS, ACTIONS)
    }
}

enum class TrendTimeframe(val key: String, val label: String, val shortLabel: String) {
    ONE_YEAR("1y", "Past 1 Year", "1Y"),
    FIVE_YEAR("5y", "Past 5 Years", "5Y"),
    TEN_YEAR("10y", "Past 10 Years", "10Y")
}

enum class HouseholdProfile(
    val key: String,
    val label: String,
    val shortLabel: String,
    val subtitle: String,
    val badgeText: String
) {
    GENERAL(
        "general",
        "General Household",
        "General",
        "Standard adult household prioritizing taste, chlorine, and legal MCL safety.",
        "Standard"
    ),
    INFANT_EXPECTING(
        "infant",
        "Infants & Expecting",
        "Infants",
        "Strict protection against lead, nitrates, copper, and PFAS for formula & prenatal care.",
        "Zero-Lead & Nitrate"
    ),
    OLD_PLUMBING(
        "old_pipes",
        "Pre-1986 Plumbing",
        "Old Pipes",
        "Homes with older service lines, lead solder, or galvanized iron premise piping.",
        "Corrosion & Lead"
    ),
    SENSITIVE(
        "sensitive",
        "Sensitive Health",
        "Sensitive",
        "Immunocompromised or chronic health needs targeting strict Public Health Goals (PHG).",
        "Strict PHG"
    )
}

enum class ContaminantFilter(val key: String, val label: String) {
    ALL("all", "All"),
    EXCEEDS_PHG("exceeds_phg", "Above Health Goal"),
    PFAS("pfas", "PFAS"),
    METALS("metals", "Metals"),
    DBP("dbp", "Byproducts")
}

enum class TicketFilter(val key: String, val label: String) {
    RESIDENT("resident", "Resident Reports"),
    EPA_OFFICIAL("epa", "EPA Compliance (10Y)"),
    ALL("all", "All Activity"),
    OPEN_ONLY("open", "Open Only")
}

data class ZipPreset(
    val zip: String,
    val city: String,
    val state: String,
    val highlightTag: String
)

data class PlaceInfo(
    val zip: String,
    val city: String,
    val state: String,
    val stateFull: String,
    val lat: Double,
    val lon: Double
)

data class WaterSystemSummary(
    val pwsid: String,
    val name: String,
    val city: String,
    val state: String,
    val zip: String,
    val population: Int,
    val sourceType: String,
    val isPrimary: Boolean,
    val currentScore: Int,
    val fiveYearTrend: String, // "Improving", "Stable", "Declining"
    val fiveYearDeltaPts: Int,
    val openTicketCount: Int
)

data class Contaminant(
    val code: String,
    val name: String,
    val category: String,
    val unit: String,
    val detected: Double,
    val mcl: Double?,          // EPA Legal Limit
    val healthGoal: Double?,   // Stricter Public Health Goal (PHG / EWG / OEHHA benchmark)
    val healthAgency: String,  // e.g. "EPA MCLG / OEHHA PHG"
    val healthNote: String,
    val pfas: Boolean,
    val regulated: Boolean,
    val trendDeltaPct1Y: Int,  // e.g., +14 or -12. Zero when a prior sample is not in the feed.
    val trendDirection: String, // "improving", "stable", "elevated", "unknown"
    val reported: Boolean = true,
    val sampleDate: String? = null
) {
    val exceedsMcl: Boolean
        get() = mcl != null && detected > mcl

    val exceedsHealthGoal: Boolean
        get() = healthGoal != null && detected > healthGoal

    val mclMultiplier: Double?
        get() = if (mcl != null && mcl > 0.0) (Math.round((detected / mcl) * 10.0) / 10.0) else null

    val phgMultiplier: Double?
        get() = if (healthGoal != null && healthGoal > 0.0) (Math.round((detected / healthGoal) * 10.0) / 10.0) else null

    val mclPercent: Double?
        get() = if (mcl != null && mcl > 0.0) (Math.round((detected / mcl) * 1000.0) / 10.0) else null
}

data class ViolationRecord(
    val id: String,
    val date: String,
    val year: Int,
    val contaminantCode: String?,
    val contaminant: String,
    val type: String,     // "health", "monitoring", "treatment", "other"
    val status: String,   // "open", "addressed", "resolved"
    val severity: String, // "serious", "minor"
    val description: String
)

data class HistoricalPoint(
    val periodLabel: String, // e.g., "Q1 '26" or "2022"
    val timestamp: String,
    val qualityScore: Int,
    val violationCount: Int,
    val droughtIndex: Int,
    val contaminantLevels: Map<String, Double>,
    val milestoneNote: String? = null
)

data class TrendDiagnostic(
    val id: String,
    val timeframe: TrendTimeframe,
    val direction: String, // "improving", "stable", "elevated", "seasonal"
    val title: String,
    val summary: String,
    val supplyIndication: String,
    val recommendedAction: String,
    val relatedContaminantCode: String? = null
)

data class HistoricalTimeSeries(
    val timeframe: TrendTimeframe,
    val points: List<HistoricalPoint>,
    val scoreDeltaPts: Int,
    val diagnostics: List<TrendDiagnostic>
)

data class QualityChangeAlert(
    val id: String,
    val pwsid: String,
    val zip: String,
    val severity: String,   // "danger", "caution", "info", "positive"
    val changeType: String, // "contaminant_spike", "new_violation", "score_shift", "seasonal_byproduct", "long_term_improvement", "drought_shift"
    val deltaLabel: String, // e.g., "+22% vs 1Y avg", "-40% over 10Y"
    val title: String,
    val message: String,
    val supplyExplanation: String,
    val detectedAt: String,
    val linkedTab: WorkspaceTab,
    val relatedContaminantCode: String? = null
)

data class IssueTicket(
    val id: String,
    val pwsid: String,
    val systemName: String,
    val zip: String,
    val isOfficialEpa: Boolean,
    val category: String,
    val title: String,
    val description: String,
    val status: String,   // "open", "investigating", "addressed"
    val severity: String, // "high", "medium", "low"
    val createdAt: String,
    val linkedContaminantCode: String? = null,
    val linkedContaminantName: String? = null,
    val affectedCount: Int = 1,
    val notes: List<String> = emptyList()
)

data class TreatmentStep(
    val stepNumber: Int,
    val title: String,
    val description: String
)

data class WaterUsageTarget(
    val normalGallonsPerDay: Int,
    val recommendedGallonsPerDay: Int
)

data class FilterRecommendation(
    val name: String,
    val type: String,
    val certificationBadge: String,
    val reductionPercent: Int,
    val costTier: String,
    val description: String,
    val targets: List<String>,
    val recommendedForProfile: Boolean
)

data class PriorityAction(
    val priority: String, // "high", "medium", "low"
    val title: String,
    val description: String,
    val badgeTag: String
)

data class SuggestedProductLink(
    val id: String,
    val title: String,
    val subtitle: String,
    val certificationBadge: String,
    val priceTier: String,
    val partnerLabel: String,
    val matchReason: String,
    val targetCodes: List<String>,
    val amazonUrl: String,
    val secondaryLabel: String,
    val secondaryUrl: String,
    val disclosure: String = "If an affiliate tag is configured, H2Know may earn a commission at no extra cost to you. The suggestion is matched to a contaminant in this system's public record. It is not an EPA recommendation."
)

data class WaterSystemReport(
    val pwsid: String,
    val name: String,
    val city: String,
    val state: String,
    val zip: String,
    val county: String,
    val population: Int,
    val sourceType: String,
    val ownerType: String,
    val qualityScore: Int,
    val fiveYearScoreDelta: Int,
    val tenYearScoreDelta: Int,
    val riskLevel: String,        // "low", "moderate", "high", "critical"
    val complianceStatus: String, // "in_compliance", "elevated_concern", "under_enforcement"
    val droughtLevel: String,     // "none", "D0", "D1", "D2", "D3", "D4"
    val droughtDescription: String,
    val contaminants: List<Contaminant>,
    val violations: List<ViolationRecord>,
    val alerts: List<QualityChangeAlert>,
    val historicalSeries: Map<TrendTimeframe, HistoricalTimeSeries>,
    val officialTickets: List<IssueTicket>,
    val treatment: List<TreatmentStep>,
    val usage: WaterUsageTarget,
    val reportUrl: String,
    val lastUpdated: String,
    val sourceDescription: String,
    val labSamplesReported: Int = 0,
    val recordNote: String = ""
)
