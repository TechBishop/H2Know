package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.focusProperties
import com.example.data.model.WaterSystemReport
import com.example.ui.theme.H2KnowThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

private const val STUDIO_PUBLISHER_NAME = "Aegis Civic Labs"
private const val EPA_SDWIS_URL = "https://www.epa.gov/enviro/sdwis-search"
private const val US_DROUGHT_MONITOR_URL = "https://droughtmonitor.unl.edu"
private const val DONATE_COFFEE_URL = "https://buymeacoffee.com/bishopsfolly"

fun getFormattedReportShareText(report: WaterSystemReport): String {
    val topContaminant = report.contaminants.firstOrNull()
    val contaminantLine = if (topContaminant != null) {
        val limitText = topContaminant.mcl?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"
        "• WHY (${topContaminant.name}): ${
            String.format(Locale.US, "%.2f", topContaminant.detected)
        } ${topContaminant.unit} (Legal Limit: $limitText ${topContaminant.unit})"
    } else {
        "• WHY: Key parameters within federal drinking water limits"
    }

    val delta5Y = if (report.fiveYearScoreDelta >= 0) "+${report.fiveYearScoreDelta}" else "${report.fiveYearScoreDelta}"
    val delta10Y = if (report.tenYearScoreDelta >= 0) "+${report.tenYearScoreDelta}" else "${report.tenYearScoreDelta}"
    return buildString {
        appendLine("H2Know 4W Water Quality Report — ZIP ${report.zip}")
        appendLine("Utility: ${report.name} (EPA ID: ${report.pwsid})")
        appendLine("Overall Score: ${report.qualityScore}/100 (Risk Level: ${report.riskLevel.uppercase()})")
        appendLine("• WHO: Serves ${String.format(Locale.US, "%,d", report.population)} residents in ${report.city}, ${report.state}")
        appendLine("• WHERE: ${report.sourceType} · ${report.sourceDescription}")
        appendLine(contaminantLine)
        appendLine("• WHEN: 5Y Trend $delta5Y pts · 10Y Trend $delta10Y pts · ${report.violations.size} EPA compliance records (10Y)")
        appendLine()
        append("Source: Public U.S. EPA SDWIS & U.S. Drought Monitor records via H2Know ($STUDIO_PUBLISHER_NAME).")
    }
}

fun shareWaterSystemReport(context: Context, report: WaterSystemReport) {
    val shareText = getFormattedReportShareText(report)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "H2Know Water Quality Report — ${report.name} (ZIP ${report.zip})")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share 4W Water Report"))
}

@Composable
fun H2KnowReportSnapshotDialog(
    report: WaterSystemReport,
    onDismiss: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var isCopied by remember { mutableStateOf(false) }

    val scoreColor = when {
        report.qualityScore >= 80 -> palette.safe
        report.qualityScore >= 60 -> palette.caution
        else -> palette.danger
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface1,
            shadowElevation = 24.dp,
            border = BorderStroke(1.dp, palette.borderStrong),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REPORT CARD SNAPSHOT",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.brand
                        )
                        Text(
                            text = "Formatted for mobile screenshots & sharing",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.ink2
                        )
                    }
                }

                // The Visual Snapshot Card (Crisp card for screenshots)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface0,
                    border = BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(scoreColor.copy(alpha = 0.6f), palette.borderStrong)
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card Topline
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "H2KNOW WATER AUDIT",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.brand
                            )
                            Text(
                                text = "ZIP ${report.zip} · ${report.city}, ${report.state}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.ink2
                            )
                        }

                        // Utility Name and Score
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = report.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "EPA PWSID ${report.pwsid}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.5.sp,
                                    color = palette.ink3
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = scoreColor.copy(alpha = 0.16f),
                                border = BorderStroke(1.5.dp, scoreColor.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${report.qualityScore}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = scoreColor
                                    )
                                    Text(
                                        text = report.riskLevel.uppercase(),
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                    )
                                }
                            }
                        }

                        // 4W Compact Key Summary
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SnapshotPill(
                                    pillar = "WHO",
                                    label = "Serves ${String.format(Locale.US, "%,d", report.population)}",
                                    modifier = Modifier.weight(1f)
                                )
                                SnapshotPill(
                                    pillar = "WHERE",
                                    label = report.sourceType,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val topContaminant = report.contaminants.firstOrNull()
                                val whyLabel = if (topContaminant != null) {
                                    "${topContaminant.name.substringBefore(" (")}: ${String.format(Locale.US, "%.1f", topContaminant.detected)} ${topContaminant.unit}"
                                } else {
                                    "Within legal limits"
                                }
                                SnapshotPill(
                                    pillar = "WHY",
                                    label = whyLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                val delta5Y = if (report.fiveYearScoreDelta >= 0) "+${report.fiveYearScoreDelta}" else "${report.fiveYearScoreDelta}"
                                SnapshotPill(
                                    pillar = "WHEN",
                                    label = "$delta5Y pts (5Y Trend)",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Data Source: Public U.S. EPA SDWIS Record · Independent Civic Audit",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 8.5.sp,
                            color = palette.ink3,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Actions below the card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            shareWaterSystemReport(context, report)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Sheet", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val shareText = getFormattedReportShareText(report)
                            clipboardManager.setText(AnnotatedString(shareText))
                            isCopied = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (isCopied) palette.safe else palette.brand,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCopied) "Copied!" else "Copy Text",
                            color = if (isCopied) palette.safe else palette.ink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Tip: You can screenshot this card anytime to share with family, roommates, or landlords.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SnapshotPill(
    pillar: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = H2KnowThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.surface2,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = pillar,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = palette.brand
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun shareH2KnowApp(context: Context) {
    val shareText = buildString {
        appendLine("H2Know — Civic Tap Water Intelligence")
        appendLine("Inspect the WHO, WHERE, WHY, and WHEN of any U.S. ZIP code's tap water using public U.S. EPA SDWIS & U.S. Drought Monitor records.")
        append("Published independently by $STUDIO_PUBLISHER_NAME.")
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "H2Know — Know Your Tap Water")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share H2Know"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun H2KnowStudioFooter(
    activeReport: WaterSystemReport?,
    onOpenPrivacyModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = H2KnowThemeTokens.colors
    val context = LocalContext.current
    var showSupportConfirmDialog by remember { mutableStateOf(false) }

    if (showSupportConfirmDialog) {
        H2KnowExternalSupportConfirmDialog(
            onDismiss = { showSupportConfirmDialog = false }
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = palette.surface1.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PUBLISHED BY ${STUDIO_PUBLISHER_NAME.uppercase()}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brand
                )
                Text(
                    text = "•",
                    fontSize = 10.sp,
                    color = palette.ink3
                )
                Text(
                    text = "INDEPENDENT CIVIC RELEASE",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.ink3
                )
            }

            Text(
                text = "Data sourced from public U.S. EPA Envirofacts (SDWIS) & U.S. Drought Monitor records. H2Know is an independent non-governmental project and is not affiliated with or endorsed by the U.S. EPA or any municipal water utility.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink2,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Privacy Policy & EPA Disclaimer Button
                Surface(
                    onClick = onOpenPrivacyModal,
                    shape = RoundedCornerShape(999.dp),
                    color = palette.surface2,
                    border = BorderStroke(1.dp, palette.borderStrong),
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .testTag("footer_privacy_policy_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppGood,
                            contentDescription = null,
                            tint = palette.safe,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Privacy & EPA Disclaimer",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.ink
                        )
                    }
                }

                // 2. Minimal Share Button (Shares active 4W report if viewing one, or the app otherwise)
                Surface(
                    onClick = {
                        if (activeReport != null) {
                            shareWaterSystemReport(context, activeReport)
                        } else {
                            shareH2KnowApp(context)
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = palette.brandSoft,
                    border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .testTag("footer_share_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = palette.brand,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeReport != null) "Share ZIP ${activeReport.zip} Report" else "Share H2Know",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.brand
                        )
                    }
                }

                // 3. Support / Buy Me a Coffee Button (non-focusable + confirmation modal so keyboard Enter never opens webpage)
                Surface(
                    onClick = { showSupportConfirmDialog = true },
                    shape = RoundedCornerShape(999.dp),
                    color = palette.donateBg,
                    border = BorderStroke(1.dp, palette.donate.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .testTag("footer_donate_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Buy Me a Coffee",
                            tint = palette.donate,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Buy Me a Coffee",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.donate
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun H2KnowExternalSupportConfirmDialog(
    onDismiss: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val uriHandler = LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surface1,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, palette.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = palette.donate,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Support H2Know",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "Would you like to open the external Buy Me a Coffee page in your browser?",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.borderStrong)
                    ) {
                        Text("Stay in App", color = palette.ink2)
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            runCatching { uriHandler.openUri(DONATE_COFFEE_URL) }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.donate)
                    ) {
                        Text("Open Webpage", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun H2KnowPrivacyAndEpaModal(
    onDismiss: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1,
            shadowElevation = 24.dp,
            border = BorderStroke(1.5.dp, palette.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIVACY POLICY & EPA DISCLOSURE",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.brand
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$STUDIO_PUBLISHER_NAME — Legal & Data Notice",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.ink,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close privacy policy",
                            tint = palette.ink2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section 1: Government Non-Affiliation & Official Public Sources (Google Play Requirement)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surface2,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "1. Government Non-Affiliation & Public Sources",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "H2Know is developed and published independently by $STUDIO_PUBLISHER_NAME. H2Know does NOT represent, and is not affiliated with or endorsed by, the U.S. Environmental Protection Agency (EPA), the U.S. Geological Survey (USGS), or any state or municipal water utility.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2
                            )
                            Text(
                                text = "All water system identifiers (PWSID), violation histories, lead/copper percentiles, and drought indices are retrieved from official public-domain federal databases:",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Surface(
                                    onClick = { uriHandler.openUri(EPA_SDWIS_URL) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.brandSoft,
                                    border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "EPA SDWIS Database",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.brand
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = palette.brand,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { uriHandler.openUri(US_DROUGHT_MONITOR_URL) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.brandSoft,
                                    border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "U.S. Drought Monitor",
                                            fontFamily = JetBrainsMonoFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.brand
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = palette.brand,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Privacy Policy & User Data Control
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surface2,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "2. Privacy Policy & Data Deletion",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• Anonymous ZIP Searches: You can search any U.S. ZIP code and inspect all 4W water reports without signing in. Entered ZIP codes are only sent to public EPA Envirofacts & Zippopotam.us endpoints to resolve local utilities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2
                            )
                            Text(
                                text = "• Optional Google Sign-In & Cloud Sync: If you choose to sign in with Google, H2Know stores your saved ZIP history snapshots and community-reported water issues in Google Firebase Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2
                            )
                            Text(
                                text = "• Instant Data Deletion & Zero Ad Tracking: H2Know contains zero third-party ad networks and never sells user data. Signed-in users can permanently delete any saved ZIP history snapshot or reported issue at any time using the trash icon next to the item.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2
                            )
                        }
                    }

                    // Section 3: Informational & Health Disclaimer
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surface2,
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "3. Informational & Health Disclaimer",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "H2Know scores and contaminant benchmarks are provided for educational and civic transparency purposes only and do not constitute medical advice or certified laboratory certification for individual tap plumbing. For certified lead or chemical testing at your tap, contact an EPA-accredited drinking water laboratory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.brand),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
