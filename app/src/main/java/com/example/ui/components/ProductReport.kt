package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contaminant
import com.example.data.model.WaterSystemReport
import com.example.data.repository.MonetizationConfig
import com.example.data.repository.ReferenceWaterData
import com.example.ui.theme.H2KnowThemeTokens
import java.util.Locale

fun waterReportPlainText(report: WaterSystemReport): String {
    val score = if (report.qualityScore < 0) "Not scored" else "${report.qualityScore} / 100"
    val people = if (report.population > 0) {
        String.format(Locale.US, "%,d", report.population)
    } else {
        "not listed"
    }
    val lines = mutableListOf(
        "H2Know water report",
        "${report.name} · ${report.city}, ${report.state} ${report.zip}",
        "Score: $score",
        verdictFor(report),
        "Source: ${report.sourceType}. Population: $people. EPA ID ${report.pwsid}.",
        report.recordNote,
        "Lead and copper samples:"
    )
    if (report.contaminants.isEmpty()) {
        lines.add("None in the public lead-and-copper table.")
    } else {
        report.contaminants.forEach { c ->
            lines.add("${c.name}: ${ReferenceWaterData.fmtVal(c.detected)} ${c.unit} (legal limit ${ReferenceWaterData.fmtVal(c.mcl)} ${c.unit})")
        }
    }
    val open = report.violations.count { it.status == "open" }
    lines.add("Open violations in the last 10 years: $open of ${report.violations.size} records.")
    lines.add("Not a certified lab test of your tap. Not affiliated with the U.S. EPA.")
    return lines.joinToString("\n")
}

private fun verdictFor(report: WaterSystemReport): String {
    val over = report.contaminants.count { it.exceedsMcl }
    val openHealth = report.violations.count { it.status == "open" && it.type == "health" }
    if (report.complianceStatus == "unavailable") {
        return "Part of the public record did not load, so there is no score."
    }
    if (report.complianceStatus == "insufficient_data" || report.qualityScore < 0) {
        return "There is not enough in the public record for a score."
    }
    if (report.complianceStatus == "under_enforcement" || over > 0 || openHealth > 0) {
        return "This system needs attention. A sample is over a legal limit, or a health-based violation is still open."
    }
    if (report.complianceStatus == "elevated_concern") {
        return "A compliance record is still open. The reported samples are under their legal limits."
    }
    if (report.contaminants.isEmpty()) {
        return "No open violations in this record. No lead or copper sample was published, so the score is only the violation history."
    }
    val lead = report.contaminants.find { it.code == "PB90" }
    if (lead != null && lead.detected > 0.0 && !lead.exceedsMcl) {
        return "Under the legal limits in this record. Lead was detected, and its health goal is zero."
    }
    return "Under the legal limits in this record."
}

@Composable
fun ProductReport(
    report: WaterSystemReport,
    watched: Boolean,
    notice: String?,
    onToggleWatch: () -> Unit,
    onDismissNotice: () -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val context = LocalContext.current
    val product = ReferenceWaterData.buildSuggestedProductsForReport(report).firstOrNull()
    val ten = report.historicalSeries[com.example.data.model.TrendTimeframe.TEN_YEAR]
    val maxViol = (ten?.points?.maxOfOrNull { it.violationCount } ?: 0).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${report.city}, ${report.state} · ${report.zip}",
                style = MaterialTheme.typography.labelLarge,
                color = palette.ink3
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (report.qualityScore < 0) "—" else report.qualityScore.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Medium,
                    color = palette.ink,
                    fontSize = 92.sp,
                    lineHeight = 92.sp
                )
                Text(
                    text = "  / 100",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.ink3,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            Text(text = verdictFor(report), style = MaterialTheme.typography.titleMedium, color = palette.ink)
            Text(
                text = "${report.name} · ${report.lastUpdated}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink3
            )
            if (!notice.isNullOrBlank()) {
                Text(text = notice, style = MaterialTheme.typography.bodyMedium, color = palette.ink2)
                Button(onClick = onDismissNotice, colors = ButtonDefaults.textButtonColors(contentColor = palette.brand)) {
                    Text("Dismiss")
                }
            }
        }

        ReportCard(title = "Contaminants", lead = report.recordNote.ifBlank {
            "Lab value beside the health goal. Only lead and copper come from the public sample table this app reads."
        }) {
            if (report.contaminants.isEmpty()) {
                Text("No lead or copper sample was in the public record.", color = palette.ink2)
            } else {
                report.contaminants.forEach { c ->
                    ContaminantRow(c)
                }
            }
        }

        ReportCard(title = "Source and utility", lead = report.sourceDescription) {
            Fact("Serves", if (report.population > 0) String.format(Locale.US, "%,d", report.population) else "Not listed")
            Fact("Source", report.sourceType)
            Fact("Operator", report.ownerType)
            Fact("EPA ID", report.pwsid)
            Fact("Drought", ReferenceWaterData.droughtLabel(report.droughtLevel))
            Text(
                text = "The gallon guide below is a planning figure for one person, not a meter reading from this utility.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink3,
                modifier = Modifier.padding(top = 8.dp)
            )
            Fact("Planning use", "${report.usage.recommendedGallonsPerDay} gallons / person / day")
        }

        ReportCard(
            title = "Record",
            lead = "Counts are compliance records that began each year. A past water score is not in this feed."
        ) {
            ten?.points?.forEach { pt ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pt.periodLabel, color = palette.ink3, modifier = Modifier.width(52.dp), fontSize = 13.sp)
                    Box(modifier = Modifier.weight(1f).height(6.dp)) {
                        val fraction = if (pt.violationCount == 0) 0f else (pt.violationCount / maxViol.toFloat()).coerceIn(0.08f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp)
                                .background(palette.brand.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                        )
                    }
                    Text(
                        text = pt.violationCount.toString(),
                        color = palette.ink3,
                        fontSize = 13.sp,
                        modifier = Modifier.width(28.dp)
                    )
                }
            }
            val open = report.violations.count { it.status == "open" }
            Text(
                text = if (open == 0) "No open violations in this record." else "$open open in the loaded record.",
                color = palette.ink2,
                modifier = Modifier.padding(top = 8.dp)
            )
            report.violations.take(8).forEach { v ->
                Text(
                    text = "${v.date} · ${v.contaminant} · ${v.status.replaceFirstChar { it.uppercase() }}",
                    color = palette.ink3,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        ReportCard(title = "Filter", lead = "Matched only to a contaminant this record actually reports.") {
            if (product == null) {
                Text(
                    "No filter is indicated. Nothing reported here is over a legal limit, and lead was not detected.",
                    color = palette.ink2
                )
            } else {
                Text(product.title, color = palette.ink, fontWeight = FontWeight.SemiBold)
                Text(product.matchReason, color = palette.ink2, modifier = Modifier.padding(top = 6.dp))
                Text(product.certificationBadge, color = palette.ink3, modifier = Modifier.padding(top = 6.dp))
                Text(product.disclosure, color = palette.ink3, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(product.amazonUrl)))
                    },
                    modifier = Modifier.padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface3, contentColor = palette.ink)
                ) {
                    Text(if (MonetizationConfig.AMAZON_ASSOCIATE_TAG.isBlank()) "See filters" else "See filters · affiliate")
                }
                if (MonetizationConfig.AMAZON_ASSOCIATE_TAG.isBlank()) {
                    Text(
                        "Affiliate tag is empty. Set AMAZON_ASSOCIATE_TAG in config.js when you have an Associates ID. No tag is sent until then.",
                        color = palette.ink3,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        ReportCard(
            title = "Saved addresses",
            lead = when {
                !MonetizationConfig.premiumEnabled ->
                    "Save an address on this device. The next time you open it, H2Know compares the score and the number of open violations."
                MonetizationConfig.checkoutConfigured ->
                    "Checkout is connected. You can save up to ${MonetizationConfig.savedAddressLimit} addresses on this device."
                else ->
                    "One address is included. Set STRIPE_PAYMENT_LINK in config.js to unlock more. Alerts compare the score and open-violation count the next time you open a saved address."
            }
        ) {
            Text(
                if (watched) "This address is saved on this device." else "Save this address to be told if the score or open violations change.",
                color = palette.ink2
            )
            Button(
                onClick = onToggleWatch,
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.surface3, contentColor = palette.ink)
            ) {
                Text(if (watched) "Remove address" else "Save address")
            }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "H2Know report · ${report.zip}")
                        putExtra(Intent.EXTRA_TEXT, waterReportPlainText(report))
                    }
                    context.startActivity(Intent.createChooser(send, "Save report"))
                },
                colors = ButtonDefaults.textButtonColors(contentColor = palette.brand)
            ) {
                Text("Download report")
            }
            if (MonetizationConfig.premiumEnabled) {
                Text(
                    "Downloading this report is free. Extra saved addresses are the paid tier.",
                    color = palette.ink3,
                    style = MaterialTheme.typography.bodySmall
                )
                if (MonetizationConfig.checkoutConfigured) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(MonetizationConfig.STRIPE_PAYMENT_LINK)))
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.brand)
                    ) { Text("Open checkout") }
                } else {
                    Text(
                        "Payments are not connected. Set STRIPE_PAYMENT_LINK in config.js when you have a Stripe Payment Link. Nothing is charged from this build.",
                        color = palette.ink3,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, lead: String, content: @Composable () -> Unit) {
    val palette = H2KnowThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface1,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = palette.ink)
            Text(lead, style = MaterialTheme.typography.bodyMedium, color = palette.ink2)
            HorizontalDivider(color = palette.border)
            content()
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    val palette = H2KnowThemeTokens.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = palette.ink3)
        Text(value, color = palette.ink, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ContaminantRow(c: Contaminant) {
    val palette = H2KnowThemeTokens.colors
    val flag = when {
        c.exceedsMcl -> "Over legal limit"
        c.healthGoal == 0.0 && c.detected > 0.0 -> "Health goal is zero"
        c.exceedsHealthGoal -> "Above health goal"
        else -> "Within health goal"
    }
    val date = c.sampleDate?.let { " · sample ended $it" }.orEmpty()
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(c.name.substringBefore(" ("), color = palette.ink, fontWeight = FontWeight.Medium)
            Text("${ReferenceWaterData.fmtVal(c.detected)} ${c.unit}", color = palette.ink)
        }
        Text(
            "$flag · limit ${ReferenceWaterData.fmtVal(c.mcl)} ${c.unit}$date",
            color = palette.ink3,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
