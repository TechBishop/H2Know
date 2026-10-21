package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.model.Contaminant
import com.example.data.model.TrendTimeframe
import com.example.data.model.WaterSystemReport
import com.example.data.remote.LocationHistoryDocument
import com.example.data.remote.WaterIssueDocument
import com.example.data.repository.ReferenceWaterData
import com.example.ui.CloudDataState
import com.example.ui.FourWTab
import com.example.ui.theme.H2KnowThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhoWhereWhyWhenDashboard(
    report: WaterSystemReport,
    activeTab: FourWTab,
    selectedWhyCategoryFilter: String,
    selectedWhyContaminantCode: String,
    selectedWhenTimeframe: TrendTimeframe,
    selectedWhenPointIndex: Int?,
    issueStatusFilter: String,
    cloudIssuesState: CloudDataState<List<WaterIssueDocument>>,
    cloudHistoryState: CloudDataState<List<LocationHistoryDocument>>,
    cloudStatusBanner: String?,
    isSavingHistory: Boolean,
    onSelectTab: (FourWTab) -> Unit,
    onSelectWhyCategoryFilter: (String) -> Unit,
    onSelectWhyContaminant: (String) -> Unit,
    onSelectWhenTimeframe: (TrendTimeframe) -> Unit,
    onSelectWhenPoint: (Int?) -> Unit,
    onSelectIssueStatusFilter: (String) -> Unit,
    onSaveSnapshotToCloud: () -> Unit,
    onOpenReportIssueModal: (Contaminant?) -> Unit,
    onUpvoteIssue: (WaterIssueDocument) -> Unit,
    onResolveIssue: (WaterIssueDocument) -> Unit,
    onDeleteIssue: (String) -> Unit,
    onDismissBanner: () -> Unit,
    onRequestSignIn: () -> Unit = {}
) {
    val palette = H2KnowThemeTokens.colors
    val isGuest = cloudIssuesState is CloudDataState.GuestRequired
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var showSnapshotModal by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = activeTab.ordinal,
        pageCount = { FourWTab.entries.size }
    )

    // Synchronize when activeTab is changed from outside (dock click)
    LaunchedEffect(activeTab) {
        if (pagerState.currentPage != activeTab.ordinal) {
            pagerState.animateScrollToPage(activeTab.ordinal)
        }
    }

    // Synchronize when the user swipes between tabs
    LaunchedEffect(pagerState.currentPage) {
        val swipedTab = FourWTab.entries[pagerState.currentPage]
        if (swipedTab != activeTab) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectTab(swipedTab)
        }
    }

    if (showSnapshotModal) {
        H2KnowReportSnapshotDialog(
            report = report,
            onDismiss = { showSnapshotModal = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (cloudStatusBanner != null) {
            Surface(
                onClick = onDismissBanner,
                shape = RoundedCornerShape(16.dp),
                color = palette.safeBg,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, palette.safe.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = palette.safe,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = cloudStatusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active Tab Context Pill Strip (shows current tab domain, swipe cue & 1-tap snapshot/share)
        ActiveFourWHeaderStrip(
            report = report,
            activeTab = activeTab,
            onSelectTab = onSelectTab,
            onOpenSnapshotModal = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showSnapshotModal = true
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { pageIdx -> FourWTab.entries[pageIdx].key },
            verticalAlignment = Alignment.Top
        ) { pageIdx ->
            when (FourWTab.entries[pageIdx]) {
                FourWTab.WHO -> WhoGovernanceAndIssuesTab(
                    report = report,
                    isGuest = isGuest,
                    issueStatusFilter = issueStatusFilter,
                    cloudIssuesState = cloudIssuesState,
                    onSelectIssueStatusFilter = onSelectIssueStatusFilter,
                    onOpenReportIssueModal = onOpenReportIssueModal,
                    onUpvoteIssue = onUpvoteIssue,
                    onResolveIssue = onResolveIssue,
                    onDeleteIssue = onDeleteIssue,
                    onRequestSignIn = onRequestSignIn
                )

                FourWTab.WHERE -> WhereWatershedAndTreatmentTab(
                    report = report
                )

                FourWTab.WHY -> WhyLabAndFiltrationTab(
                    report = report,
                    isGuest = isGuest,
                    selectedWhyCategoryFilter = selectedWhyCategoryFilter,
                    selectedWhyContaminantCode = selectedWhyContaminantCode,
                    onSelectWhyCategoryFilter = onSelectWhyCategoryFilter,
                    onSelectWhyContaminant = onSelectWhyContaminant,
                    onOpenReportIssueModal = onOpenReportIssueModal,
                    onRequestSignIn = onRequestSignIn
                )

                FourWTab.WHEN -> WhenTimelineAndHistoryTab(
                    report = report,
                    isGuest = isGuest,
                    selectedWhyContaminantCode = selectedWhyContaminantCode,
                    selectedWhenTimeframe = selectedWhenTimeframe,
                    selectedWhenPointIndex = selectedWhenPointIndex,
                    cloudHistoryState = cloudHistoryState,
                    isSavingHistory = isSavingHistory,
                    onSelectWhenTimeframe = onSelectWhenTimeframe,
                    onSelectWhenPoint = onSelectWhenPoint,
                    onSaveSnapshotToCloud = onSaveSnapshotToCloud,
                    onRequestSignIn = onRequestSignIn
                )
            }
        }
    }
}

@Composable
private fun ActiveFourWHeaderStrip(
    report: WaterSystemReport,
    activeTab: FourWTab,
    onSelectTab: (FourWTab) -> Unit,
    onOpenSnapshotModal: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val tabAccent = when (activeTab) {
        FourWTab.WHO -> palette.brand
        FourWTab.WHERE -> palette.safe
        FourWTab.WHY -> palette.caution
        FourWTab.WHEN -> palette.brand
    }
    val tabDescription = when (activeTab) {
        FourWTab.WHO -> "Utility Operator, Water Score & Resident Issues"
        FourWTab.WHERE -> "Water Source, Treatment Steps & Drought Level"
        FourWTab.WHY -> "Contaminant Levels vs. Health & Legal Limits"
        FourWTab.WHEN -> "1Y / 5Y / 10Y Score History & EPA Violations"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface1.copy(alpha = 0.92f),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, tabAccent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = activeTab.badge,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tabAccent
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = palette.surface2
                    ) {
                        Text(
                            text = "SWIPE ⇄",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.ink3,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
                Text(
                    text = tabDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink2
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onOpenSnapshotModal,
                shape = RoundedCornerShape(999.dp),
                color = palette.surface2,
                border = BorderStroke(1.dp, palette.borderStrong),
                modifier = Modifier.testTag("header_share_report_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share 4W Water Report Snapshot",
                        tint = palette.brand,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Share",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 1: WHO — Utility Governance, Score Gauge & Firestore Water Issues
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhoGovernanceAndIssuesTab(
    report: WaterSystemReport,
    isGuest: Boolean,
    issueStatusFilter: String,
    cloudIssuesState: CloudDataState<List<WaterIssueDocument>>,
    onSelectIssueStatusFilter: (String) -> Unit,
    onOpenReportIssueModal: (Contaminant?) -> Unit,
    onUpvoteIssue: (WaterIssueDocument) -> Unit,
    onResolveIssue: (WaterIssueDocument) -> Unit,
    onDeleteIssue: (String) -> Unit,
    onRequestSignIn: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val complianceLabel = when (report.complianceStatus) {
        "under_enforcement" -> "Under Enforcement Action"
        "elevated_concern" -> "Elevated Monitoring Concern"
        else -> "In EPA SDWIS Compliance"
    }
    val complianceColor = when (report.complianceStatus) {
        "under_enforcement" -> palette.danger
        "elevated_concern" -> palette.caution
        else -> palette.safe
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Card 1: Utility Identity, Score Gauge & Governance
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(palette.brand.copy(alpha = 0.42f), palette.border)
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillarBadge("01 · WHO OPERATES THIS SYSTEM", palette.brand, palette.brandSoft)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = complianceColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, complianceColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = complianceLabel.uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = complianceColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ScoreRingGraphic(score = report.qualityScore)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "PUBLIC WATER UTILITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.ink3
                        )
                        Text(
                            text = report.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Last SDWIS Sync: ${report.lastUpdated}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = palette.ink3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2x2 Governance Metadata Grid (Unique to WHO tab)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GovernanceStatTile(
                            label = "EPA PWSID",
                            value = report.pwsid,
                            subtext = "Federal System Registry ID",
                            modifier = Modifier.weight(1f)
                        )
                        GovernanceStatTile(
                            label = "GOVERNANCE",
                            value = report.ownerType,
                            subtext = "Operating Authority",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GovernanceStatTile(
                            label = "POPULATION SERVED",
                            value = String.format(Locale.US, "%,d", report.population),
                            subtext = "Permanent Residents",
                            modifier = Modifier.weight(1f)
                        )
                        GovernanceStatTile(
                            label = "RISK POSTURE",
                            value = report.riskLevel.replaceFirstChar { it.uppercase() },
                            subtext = "Composite Health Index",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Card 2: User-Reported Water Quality Issues (Exclusively in WHO Tab)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reported Water Quality Issues",
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.ink,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Resident reports synced via Firebase Firestore for ${report.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (isGuest) {
                        OutlinedButton(
                            onClick = onRequestSignIn,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, palette.brand),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("guest_sign_in_for_issues_button")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.brand)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sign In", style = MaterialTheme.typography.labelLarge, color = palette.brand)
                        }
                    } else {
                        Button(
                            onClick = { onOpenReportIssueModal(null) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.brand,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("open_report_issue_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Report Issue", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (cloudIssuesState) {
                    is CloudDataState.GuestRequired -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surface2,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Browsing in Guest Mode. Use the compact Sign In button above if you want to submit or vote on community water quality issues in Firebase Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    is CloudDataState.Loading -> {
                        Text(
                            text = "Loading reported water issues from Firestore…",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3
                        )
                    }

                    is CloudDataState.Error -> {
                        Text(
                            text = cloudIssuesState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.danger
                        )
                    }

                    is CloudDataState.Success -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("all" to "All", "open" to "Open", "resolved" to "Resolved").forEach { (key, label) ->
                                val active = issueStatusFilter == key
                                Surface(
                                    onClick = { onSelectIssueStatusFilter(key) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (active) palette.brandSoft else palette.surface2,
                                    border = BorderStroke(1.dp, if (active) palette.brand else palette.border)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) palette.brand else palette.ink2,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val issuesForSystem = cloudIssuesState.data.filter { issue ->
                            val matchesSystem = issue.pwsid == report.pwsid || issue.zip == report.zip
                            val matchesFilter = when (issueStatusFilter) {
                                "open" -> issue.status != "resolved"
                                "resolved" -> issue.status == "resolved"
                                else -> true
                            }
                            matchesSystem && matchesFilter
                        }
                        if (issuesForSystem.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.surface2,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No matching water quality issues logged for ${report.name} yet. Tap 'Report Issue' above or flag a contaminant in the WHY tab to log an issue to Firestore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.ink2,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                issuesForSystem.forEach { issue ->
                                    CloudIssueCard(
                                        issue = issue,
                                        onUpvote = { onUpvoteIssue(issue) },
                                        onResolve = { onResolveIssue(issue) },
                                        onDelete = { onDeleteIssue(issue.id) }
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

@Composable
private fun GovernanceStatTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    val palette = H2KnowThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface2,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = palette.brand
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = palette.ink3
            )
        }
    }
}

// ============================================================================
// TAB 2: WHERE — Hydrological Origin, Source-to-Tap Treatment & Drought Map
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhereWatershedAndTreatmentTab(
    report: WaterSystemReport
) {
    val palette = H2KnowThemeTokens.colors
    val droughtLevels = listOf("none", "D0", "D1", "D2", "D3", "D4")
    val activeDroughtIdx = droughtLevels.indexOfFirst {
        it.equals(report.droughtLevel, ignoreCase = true)
    }.coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Card 1: Watershed & Hydrological Origin
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(palette.safe.copy(alpha = 0.45f), palette.border)
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                PillarBadge("02 · WHERE YOUR WATER ORIGINATES", palette.safe, palette.safeBg)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${report.sourceType} Supply Basin",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = palette.safe,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Service Area: ZIP ${report.zip} · ${report.city}, ${report.state}${if (report.county.isNotBlank()) " · ${report.county} County" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface2,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "HYDROLOGICAL INTAKE PROFILE",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.safe
                        )
                        Text(
                            text = report.sourceDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2
                        )
                    }
                }
            }
        }

        // Card 2: Step-by-Step Source-to-Tap Treatment Pipeline
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "SOURCE-TO-TAP TREATMENT TRAIN",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brand
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "How Raw Water Is Conditioned Before Your Tap",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    report.treatment.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surface2,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.brandSoft,
                                    border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "0${step.stepNumber}",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.brand
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = step.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.ink,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = step.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.ink2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: U.S. Drought Monitor & Regional Conservation Target
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "U.S. DROUGHT MONITOR & WATERSHED STRESS",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeDroughtIdx >= 2) palette.caution else palette.safe
                    )
                    Text(
                        text = ReferenceWaterData.droughtLabel(report.droughtLevel),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Drought scale pills (None .. D4)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    droughtLevels.forEachIndexed { idx, lvl ->
                        val isActive = idx == activeDroughtIdx
                        val pillColor = when {
                            idx == 0 -> palette.safe
                            idx <= 2 -> palette.caution
                            else -> palette.danger
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) pillColor.copy(alpha = 0.22f) else palette.surface2,
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) pillColor else palette.border
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (lvl == "none") "NONE" else lvl,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) pillColor else palette.ink3
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = report.droughtDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink2
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surface2,
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Regional Daily Target per Person",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2
                        )
                        Text(
                            text = "${report.usage.recommendedGallonsPerDay} GPD (Baseline ${report.usage.normalGallonsPerDay} GPD)",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.brand
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 3: WHY — Lab Chemistry, PHG vs. MCL Benchmarks & Tap Filtration
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhyLabAndFiltrationTab(
    report: WaterSystemReport,
    isGuest: Boolean,
    selectedWhyCategoryFilter: String,
    selectedWhyContaminantCode: String,
    onSelectWhyCategoryFilter: (String) -> Unit,
    onSelectWhyContaminant: (String) -> Unit,
    onOpenReportIssueModal: (Contaminant?) -> Unit,
    onRequestSignIn: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val exceedsPhgCount = report.contaminants.count { it.exceedsHealthGoal }
    val exceedsMclCount = report.contaminants.count { it.exceedsMcl }

    val filteredContaminants = remember(report.contaminants, selectedWhyCategoryFilter) {
        report.contaminants.filter { c ->
            when (selectedWhyCategoryFilter) {
                "EXCEEDS_PHG" -> c.exceedsHealthGoal
                "PFAS" -> c.pfas || c.category.equals("PFAS", ignoreCase = true)
                "METALS" -> c.category.equals("Metals", ignoreCase = true)
                "DBP" -> c.category.contains("Byproduct", ignoreCase = true)
                else -> true
            }
        }.ifEmpty { report.contaminants }
    }

    val selectedContaminant = filteredContaminants.find { it.code == selectedWhyContaminantCode }
        ?: filteredContaminants.firstOrNull()

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = palette.surface1.copy(alpha = 0.95f),
        shadowElevation = 18.dp,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PillarBadge("03 · WHY (CONTAMINANTS & FILTERS)", palette.caution, palette.cautionBg)
                Text(
                    text = "$exceedsPhgCount Above Health Goal · $exceedsMclCount Over Legal Limit",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (exceedsMclCount > 0) palette.danger else palette.caution
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "What Was Detected in Your Tap Water",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any contaminant below to see where it comes from and which home water filter removes it.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chips (Unique to WHY Tab)
            val filters = listOf(
                "ALL" to "All (${report.contaminants.size})",
                "EXCEEDS_PHG" to "Above Health Goal ($exceedsPhgCount)",
                "PFAS" to "Forever Chemicals (PFAS)",
                "METALS" to "Metals (Lead/Copper)",
                "DBP" to "Chlorine Byproducts"
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (key, label) ->
                    val active = selectedWhyCategoryFilter == key
                    Surface(
                        onClick = { onSelectWhyCategoryFilter(key) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (active) palette.cautionBg else palette.surface2,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (active) palette.caution else palette.border
                        ),
                        modifier = Modifier.testTag("why_filter_$key")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) palette.caution else palette.ink2,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            WhyContaminantBenchmarkGraphic(
                contaminants = filteredContaminants,
                selectedCode = selectedContaminant?.code ?: "",
                onSelectContaminant = onSelectWhyContaminant
            )

            if (selectedContaminant != null) {
                Spacer(modifier = Modifier.height(16.dp))
                SelectedContaminantWhyBox(
                    contaminant = selectedContaminant,
                    onFlagContaminantIssue = {
                        if (isGuest) onRequestSignIn() else onOpenReportIssueModal(it)
                    }
                )
            }
        }
    }
}

// ============================================================================
// TAB 4: WHEN — 1Y / 5Y / 10Y Longitudinal Timeline, Violations & Cloud History
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhenTimelineAndHistoryTab(
    report: WaterSystemReport,
    isGuest: Boolean,
    selectedWhyContaminantCode: String,
    selectedWhenTimeframe: TrendTimeframe,
    selectedWhenPointIndex: Int?,
    cloudHistoryState: CloudDataState<List<LocationHistoryDocument>>,
    isSavingHistory: Boolean,
    onSelectWhenTimeframe: (TrendTimeframe) -> Unit,
    onSelectWhenPoint: (Int?) -> Unit,
    onSaveSnapshotToCloud: () -> Unit,
    onRequestSignIn: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val selectedContaminant = report.contaminants.find { it.code == selectedWhyContaminantCode }
        ?: report.contaminants.firstOrNull()
    val series = report.historicalSeries[selectedWhenTimeframe]
    val deltaPts = series?.scoreDeltaPts ?: 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Card 1: Unified 1Y / 5Y / 10Y Canvas Timeline
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PillarBadge("04 · WHEN (1Y / 5Y / 10Y TIMELINE)", palette.brand, palette.brandSoft)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surface0)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TrendTimeframe.entries.forEach { tf ->
                            val active = tf == selectedWhenTimeframe
                            Surface(
                                onClick = { onSelectWhenTimeframe(tf) },
                                shape = RoundedCornerShape(9.dp),
                                color = if (active) palette.brand else Color.Transparent,
                                modifier = Modifier.testTag("when_tf_${tf.key}")
                            ) {
                                Text(
                                    text = tf.shortLabel,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else palette.ink2,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${selectedWhenTimeframe.label} Trajectory (${if (deltaPts >= 0) "+$deltaPts" else "$deltaPts"} pts)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Longitudinal curve for ${selectedContaminant?.name?.substringBefore(" (") ?: "Lead"} & EPA compliance events. Tap any node on the timeline to inspect that period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink2
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (series != null && series.points.isNotEmpty() && selectedContaminant != null) {
                    WhenUnifiedTimelineGraphic(
                        points = series.points,
                        diagnostics = series.diagnostics,
                        contaminant = selectedContaminant,
                        selectedIndex = selectedWhenPointIndex ?: (series.points.lastIndex),
                        onSelectPoint = onSelectWhenPoint
                    )
                }
            }
        }

        // Card 2: 10-Year EPA SDWIS Compliance & Violation Log (Unique to WHEN Tab)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = "10-YEAR EPA SDWIS COMPLIANCE LOG (${report.violations.size})",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brand
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Historical Monitoring & Enforcement Events",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    report.violations.take(6).forEach { v ->
                        val isOpen = v.status == "open"
                        val vColor = when {
                            isOpen -> palette.danger
                            v.severity == "serious" -> palette.caution
                            else -> palette.safe
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surface2,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${v.date} · ${v.contaminant}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.ink,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = vColor.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = v.status.uppercase(),
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = vColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = v.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.ink2
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Firebase Firestore Location History Snapshots (Unique to WHEN Tab)
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 18.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Location History Snapshots",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.ink,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Track longitudinal score drift for ${report.name} in Firebase Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2
                        )
                    }
                    if (isGuest) {
                        OutlinedButton(
                            onClick = onRequestSignIn,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.brand)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = palette.brand)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Sign In", style = MaterialTheme.typography.labelSmall, color = palette.brand)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSaveSnapshotToCloud,
                            enabled = !isSavingHistory,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("save_cloud_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = palette.brand,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSavingHistory) "Saving…" else "Save Snapshot",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.brand
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val matchingHistory = (cloudHistoryState as? CloudDataState.Success)
                    ?.data
                    ?.filter { it.pwsid == report.pwsid || it.zip == report.zip }
                    .orEmpty()

                if (isGuest) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sign in with Google to record timestamped location history snapshots to Firebase Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else if (matchingHistory.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No saved snapshots for this system yet. Tap 'Save Snapshot' above to store a timestamped baseline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    val dateFormatter = remember {
                        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        matchingHistory.take(4).forEach { snap ->
                            val tsText = snap.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Just now"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surface2,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.ink2
                                    )
                                    Text(
                                        text = "${snap.exceedsHealthGoalCount} > PHG · ${snap.violationCount} Events",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 11.sp,
                                        color = palette.ink
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

@Composable
fun FloatingFourWDock(
    activeTab: FourWTab,
    isGuestMode: Boolean,
    onSelectTab: (FourWTab) -> Unit,
    onQuickActionClick: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = palette.surface1.copy(alpha = 0.96f),
            shadowElevation = 24.dp,
            tonalElevation = 10.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(palette.brand.copy(alpha = 0.5f), palette.border, palette.safe.copy(alpha = 0.4f))
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FourWTab.entries.forEach { tab ->
                    val isSelected = tab == activeTab
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(tab)
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = if (isSelected) palette.brandSoft else palette.surface2,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.brand else palette.border
                        ),
                        modifier = Modifier.testTag("dock_tab_${tab.key}")
                    ) {
                        Text(
                            text = tab.key,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) palette.brand else palette.ink,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickActionClick()
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = palette.brand,
                    modifier = Modifier.testTag("dock_quick_action_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGuestMode) Icons.Default.Person else Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGuestMode) "Sign In" else "Issue",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarBadge(text: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CloudIssueCard(
    issue: WaterIssueDocument,
    onUpvote: () -> Unit,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val isResolved = issue.status == "resolved"
    val badgeColor = when (issue.severity) {
        "high" -> palette.danger
        "medium" -> palette.caution
        else -> palette.safe
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface2,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = issue.category.uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isResolved) palette.safeBg else palette.brandSoft
                    ) {
                        Text(
                            text = issue.status.uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isResolved) palette.safe else palette.brand,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete issue",
                        tint = palette.ink3,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink2
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ZIP ${issue.zip} · ${issue.affectedCount} affected",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = palette.ink3
                )
                if (!isResolved) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onUpvote,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+1 Affected", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = onResolve,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resolve", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportCloudIssueDialog(
    systemName: String,
    zip: String,
    prefillContaminant: Contaminant? = null,
    onDismiss: () -> Unit,
    onSubmit: (category: String, title: String, description: String, severity: String) -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val categories = listOf("Taste / Odor", "Discoloration", "Pressure Loss", "Lead / Pipe Concern", "Boil Notice")
    val initialCategory = when (prefillContaminant?.category) {
        "Metals" -> "Lead / Pipe Concern"
        "Disinfection Byproducts" -> "Taste / Odor"
        else -> categories.first()
    }
    var selectedCategory by remember(prefillContaminant) { mutableStateOf(initialCategory) }
    var title by remember(prefillContaminant) {
        mutableStateOf(
            prefillContaminant?.let {
                "${it.name.substringBefore(" (")} (${ReferenceWaterData.fmtVal(it.detected)} ${it.unit}) concern"
            } ?: ""
        )
    }
    var description by remember(prefillContaminant) {
        mutableStateOf(
            prefillContaminant?.let {
                "Flagged from WHY Lab Benchmark chart: ${it.name} detected at ${ReferenceWaterData.fmtVal(it.detected)} ${it.unit} in $systemName."
            } ?: ""
        )
    }
    var severity by remember(prefillContaminant) {
        mutableStateOf(
            when {
                prefillContaminant?.exceedsMcl == true -> "high"
                prefillContaminant?.exceedsHealthGoal == true -> "medium"
                else -> "medium"
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface1,
        titleContentColor = palette.ink,
        textContentColor = palette.ink2,
        title = {
            Text("Report Water Quality Issue", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saved directly to Firebase Firestore in the WHO tab for $systemName (ZIP $zip).",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink3
                )
                Text("CATEGORY", style = MaterialTheme.typography.labelSmall, color = palette.ink3)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val active = cat == selectedCategory
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(999.dp),
                            color = if (active) palette.brand else palette.surface2
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else palette.ink2,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Short summary") },
                    placeholder = { Text("e.g. Strong chlorine odor on cold tap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_issue_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & when you noticed it") },
                    placeholder = { Text("Describe what you observed and when it started…") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cloud_issue_desc_input")
                )

                Text("SEVERITY", style = MaterialTheme.typography.labelSmall, color = palette.ink3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Low", "medium" to "Medium", "high" to "High").forEach { (key, label) ->
                        val active = severity == key
                        Surface(
                            onClick = { severity = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) palette.brand else palette.surface2
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else palette.ink2,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "$selectedCategory reported in ZIP $zip" }
                    val finalDesc = description.ifBlank { "Resident reported $selectedCategory for $systemName." }
                    onSubmit(selectedCategory, finalTitle, finalDesc, severity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                modifier = Modifier.testTag("submit_cloud_issue_button")
            ) {
                Text("Save to Firestore", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.ink2)
            }
        }
    )
}
