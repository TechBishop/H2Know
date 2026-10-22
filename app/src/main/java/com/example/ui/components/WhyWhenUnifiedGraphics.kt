package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contaminant
import com.example.data.model.HistoricalPoint
import com.example.data.model.TrendDiagnostic
import com.example.data.repository.ReferenceWaterData
import com.example.ui.theme.H2KnowThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Converts cryptic EPA database codes (like 2950, PB90) into plain English names.
 */
fun Contaminant.plainShortName(): String = when (code) {
    "PB90" -> "Lead"
    "CU90" -> "Copper"
    "2950" -> "Chlorine Byproducts (TTHMs)"
    "2456" -> "Haloacetic Acids (HAA5)"
    "2806" -> "PFOA (Forever Chemical)"
    "2805" -> "PFOS (Forever Chemical)"
    "1040" -> "Nitrate (Fertilizer Runoff)"
    "1005" -> "Arsenic"
    "1025" -> "Fluoride"
    else -> name.substringBefore(" (")
}

fun Contaminant.plainUnitExplanation(): String = when (unit.lowercase()) {
    "ppb" -> "parts per billion (ppb)"
    "ppt" -> "parts per trillion (ppt)"
    "ppm", "mg/l" -> "parts per million (ppm)"
    else -> unit
}

@Composable
fun ScoreRingGraphic(score: Int) {
    val palette = H2KnowThemeTokens.colors
    val ringColor = when {
        score >= 80 -> palette.safe
        score >= 60 -> palette.caution
        else -> palette.danger
    }
    val gradeLabel = when {
        score >= 85 -> "GOOD"
        score >= 70 -> "FAIR"
        score >= 55 -> "WATCH"
        else -> "POOR"
    }
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f) * 270f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "score_ring_sweep"
    )

    Box(
        modifier = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(110.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ringColor.copy(alpha = 0.28f), Color.Transparent),
                    radius = size.minDimension * 0.56f
                ),
                radius = size.minDimension * 0.56f
            )
            val stroke = 11.dp.toPx()
            drawArc(
                color = palette.surface3,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = 135f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 27.sp,
                color = palette.ink
            )
            Text(
                text = "$gradeLabel / 100",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhyContaminantBenchmarkGraphic(
    contaminants: List<Contaminant>,
    selectedCode: String,
    onSelectContaminant: (String) -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface0,
        border = BorderStroke(1.dp, palette.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plain-English Color Key Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surface1,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HOW TO READ THIS CHART",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.brand
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendPill("Safe (Under Health Goal)", palette.safe)
                        LegendPill("Above Health Goal (Still Legal)", palette.caution)
                        LegendPill("Over EPA Legal Limit", palette.danger)
                    }
                    Text(
                        text = "• Yellow Dashed Marker = Ideal Health Goal   • Red Solid Marker = EPA Legal Limit",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp,
                        color = palette.ink2
                    )
                }
            }

            contaminants.forEach { c ->
                val isSelected = c.code == selectedCode
                val barColor = when {
                    c.exceedsMcl -> palette.danger
                    c.exceedsHealthGoal -> palette.caution
                    else -> palette.safe
                }
                val statusLabel = when {
                    c.exceedsMcl -> "OVER LEGAL LIMIT"
                    c.exceedsHealthGoal -> "ABOVE HEALTH GOAL"
                    else -> "SAFE LEVEL"
                }

                val maxScale = when {
                    c.mcl != null && c.mcl > 0 -> max(c.mcl * 1.30, c.detected * 1.15)
                    c.healthGoal != null && c.healthGoal > 0 -> max(c.healthGoal * 4.0, c.detected * 1.15)
                    else -> max(c.detected * 1.5, 1.0)
                }
                val targetRatio = (c.detected / maxScale).toFloat().coerceIn(0.06f, 1f)
                val animatedRatio by animateFloatAsState(
                    targetValue = targetRatio,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "bar_${c.code}"
                )
                val phgRatio = c.healthGoal?.let { (it / maxScale).toFloat().coerceIn(0.06f, 0.94f) }
                val mclRatio = c.mcl?.let { (it / maxScale).toFloat().coerceIn(0.12f, 0.96f) }

                Surface(
                    onClick = { onSelectContaminant(c.code) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) palette.surface2 else palette.surface1,
                    shadowElevation = if (isSelected) 10.dp else 2.dp,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) palette.brand else palette.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("why_contaminant_row_${c.code}")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = c.plainShortName(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = c.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = palette.ink2
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = barColor.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, barColor.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = statusLabel,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High-contrast 22dp bar with clear tracks and threshold lines
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        ) {
                            val trackH = 12.dp.toPx()
                            val trackY = (size.height - trackH) / 2f
                            // High-contrast track background
                            drawRoundRect(
                                color = palette.surface3,
                                topLeft = Offset(0f, trackY),
                                size = Size(size.width, trackH),
                                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                            )
                            // Detected value fill bar
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(0f, trackY),
                                size = Size(size.width * animatedRatio, trackH),
                                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                            )
                            // Ideal Health Goal marker (Yellow Dashed)
                            if (phgRatio != null) {
                                val x = size.width * phgRatio
                                drawLine(
                                    color = palette.caution,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 2.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                                )
                            }
                            // EPA Legal Limit marker (Red Solid)
                            if (mclRatio != null) {
                                val x = size.width * mclRatio
                                drawLine(
                                    color = palette.danger,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Explicit Plain-English Numbers Row (No guesswork!)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Detected: ${ReferenceWaterData.fmtVal(c.detected)} ${c.unit}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.ink
                            )
                            Text(
                                text = "Health Goal: ${c.healthGoal?.let { "${ReferenceWaterData.fmtVal(it)} ${c.unit}" } ?: "None"}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = palette.caution
                            )
                            Text(
                                text = "Legal Limit: ${c.mcl?.let { "${ReferenceWaterData.fmtVal(it)} ${c.unit}" } ?: "No EPA Limit"}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = palette.ink2
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedContaminantWhyBox(
    contaminant: Contaminant,
    onFlagContaminantIssue: (Contaminant) -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val plainName = contaminant.plainShortName()
    val whyOrigin = when (contaminant.category) {
        "Metals" -> "Enters tap water through corrosion of older service pipes, lead solder, or household brass plumbing fixtures rather than the water source itself."
        "PFAS" -> "Synthetic 'forever chemicals' from upstream industrial runoff, firefighting foam, or persistent environmental accumulation."
        "Disinfection Byproducts" -> "Forms when chlorine disinfectant added by the utility reacts with natural plant matter in river or reservoir water."
        "Inorganics" -> "Comes from agricultural fertilizer runoff, septic leaching, or natural mineral deposits in underground aquifers."
        else -> "Detected during routine EPA Safe Drinking Water utility sampling."
    }
    val nsfRemedy = when (contaminant.category) {
        "Metals" -> "Carbon Block or Reverse Osmosis filter (NSF/ANSI 53 or 58) — removes ~99% of lead and copper at your kitchen tap."
        "PFAS" -> "High-Density Carbon Block or Reverse Osmosis (NSF/ANSI 53 or 58) — certified to reduce PFOA & PFOS forever chemicals."
        "Disinfection Byproducts" -> "Activated Carbon Pitcher or Under-Sink Filter (NSF/ANSI 42 + 53) — absorbs chlorine taste and byproducts."
        "Inorganics" -> "Reverse Osmosis Filter (NSF/ANSI 58) — standard carbon water pitchers do not remove nitrates or arsenic."
        else -> "NSF/ANSI 53 Certified Tap Filter."
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface2,
        shadowElevation = 10.dp,
        border = BorderStroke(1.5.dp, palette.brand.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "WHY ${plainName.uppercase()} IS IN YOUR WATER",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.brand
                )
                Text(
                    text = "Measured in ${contaminant.plainUnitExplanation()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ink2
                )
            }

            Text(
                text = whyOrigin,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.ink,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Health Impact: ${contaminant.healthNote}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink2
            )

            val uriHandler = LocalUriHandler.current
            val amzFilterUrl = ReferenceWaterData.contaminantAmazonFilterUrl(contaminant)
            val amzTestKitUrl = ReferenceWaterData.contaminantAmazonTestKitUrl(contaminant)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surface1,
                border = BorderStroke(1.dp, palette.safe.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "RECOMMENDED HOME FILTER",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.safe
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = palette.cautionBg,
                            border = BorderStroke(1.dp, palette.caution.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "SUGGESTED LINKS · AMAZON",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.caution,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = nsfRemedy,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { runCatching { uriHandler.openUri(amzFilterUrl) } },
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.safe,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("amz_filter_btn_${contaminant.code}")
                        ) {
                            Text(
                                text = "Shop NSF Filter on Amazon",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { runCatching { uriHandler.openUri(amzTestKitUrl) } },
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, palette.caution.copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("amz_testkit_btn_${contaminant.code}")
                        ) {
                            Text(
                                text = "${plainName.substringBefore(" ")} Test Kit",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.caution,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = palette.caution
                            )
                        }

                        OutlinedButton(
                            onClick = { onFlagContaminantIssue(contaminant) },
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, palette.brand),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("flag_contaminant_issue_${contaminant.code}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = palette.brand
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Report ${plainName.substringBefore(" ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.brand,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhenUnifiedTimelineGraphic(
    points: List<HistoricalPoint>,
    diagnostics: List<TrendDiagnostic>,
    contaminant: Contaminant,
    selectedIndex: Int,
    onSelectPoint: (Int) -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIndex]
    val primaryDiagnostic = diagnostics.firstOrNull()
    val plainContaminantName = contaminant.plainShortName()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surface0,
        border = BorderStroke(1.dp, palette.borderStrong),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plain-English Key Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.surface1,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CHART KEY (TAP ANY POINT ON THE LINE TO INSPECT)",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.brand
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LegendPill("Blue Solid Line = Water Quality Score (0–100)", palette.brand)
                        LegendPill("Gold Dashed Line = $plainContaminantName (${contaminant.unit})", palette.caution)
                        LegendPill("Red Dot = EPA Violation Event", palette.danger)
                    }
                }
            }

            // Chart with explicit Y-Axis Score Labels on the left + High-Contrast Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(195.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Y-Axis Score Scale (100 down to 40)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 12.dp, bottom = 22.dp, end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("100", "80", "60", "40").forEach { yLabel ->
                        Text(
                            text = yLabel,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.ink2
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (points.size > 1) {
                                    val stepX = size.width / (points.size - 1).toFloat()
                                    val idx = (offset.x / stepX).roundToInt().coerceIn(0, points.lastIndex)
                                    onSelectPoint(idx)
                                }
                            }
                        }
                ) {
                    val padTop = 18.dp.toPx()
                    val padBottom = 24.dp.toPx()
                    val chartH = size.height - padTop - padBottom
                    val stepX = if (points.size > 1) size.width / (points.size - 1).toFloat() else size.width

                    // 4 horizontal gridlines matching 100, 80, 60, 40
                    listOf(0f, 0.333f, 0.666f, 1f).forEach { frac ->
                        val y = padTop + chartH * frac
                        drawLine(
                            color = palette.borderStrong.copy(alpha = 0.65f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 1. Score Curve (30..100 mapped cleanly to chart height)
                    val scoreOffsets = points.mapIndexed { i, pt ->
                        val x = i * stepX
                        val normalized = ((pt.qualityScore.coerceIn(30, 100) - 30f) / 70f).coerceIn(0f, 1f)
                        val y = padTop + chartH * (1f - normalized)
                        Offset(x, y)
                    }

                    val areaPath = Path().apply {
                        scoreOffsets.firstOrNull()?.let { first ->
                            moveTo(first.x, padTop + chartH)
                            lineTo(first.x, first.y)
                            scoreOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                            lineTo(scoreOffsets.last().x, padTop + chartH)
                            close()
                        }
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.brand.copy(alpha = 0.38f), Color.Transparent),
                            startY = padTop,
                            endY = padTop + chartH
                        )
                    )

                    val scoreLinePath = Path().apply {
                        scoreOffsets.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = scoreLinePath,
                        color = palette.brand,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 2. Selected Contaminant Overlay Curve (Gold Dashed)
                    val contValues = points.map { it.contaminantLevels[contaminant.code] ?: contaminant.detected }
                    val maxCont = max(
                        contValues.maxOrNull() ?: 1.0,
                        (contaminant.mcl ?: contaminant.healthGoal ?: 1.0) * 1.15
                    ).coerceAtLeast(0.01)
                    val contOffsets = contValues.mapIndexed { i, v ->
                        val x = i * stepX
                        val norm = (v / maxCont).toFloat().coerceIn(0.08f, 0.92f)
                        val y = padTop + chartH * (1f - norm)
                        Offset(x, y)
                    }
                    val contPath = Path().apply {
                        contOffsets.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = contPath,
                        color = palette.caution,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                        )
                    )

                    // 3. Data Nodes, Selected Vertical Cursor & Violation Dots
                    points.forEachIndexed { i, pt ->
                        val x = i * stepX
                        if (i == safeIndex) {
                            drawLine(
                                color = palette.ink.copy(alpha = 0.75f),
                                start = Offset(x, padTop),
                                end = Offset(x, padTop + chartH),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawCircle(
                                color = palette.brand.copy(alpha = 0.32f),
                                radius = 13.dp.toPx(),
                                center = scoreOffsets[i]
                            )
                        }
                        drawCircle(
                            color = palette.surface0,
                            radius = if (i == safeIndex) 7.dp.toPx() else 5.dp.toPx(),
                            center = scoreOffsets[i]
                        )
                        drawCircle(
                            color = if (i == safeIndex) palette.ink else palette.brand,
                            radius = if (i == safeIndex) 5.dp.toPx() else 3.5.dp.toPx(),
                            center = scoreOffsets[i]
                        )
                        if (pt.violationCount > 0) {
                            drawCircle(
                                color = palette.danger,
                                radius = 6.5.dp.toPx(),
                                center = Offset(x, padTop + chartH - 5.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Explicit Period Axis Labels aligned under the chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { idx, pt ->
                    val showLabel = points.size <= 6 || idx == 0 || idx == points.size / 2 || idx == points.lastIndex || idx == safeIndex
                    if (showLabel) {
                        val isSelected = idx == safeIndex
                        Text(
                            text = pt.periodLabel,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) palette.brand else palette.ink2
                        )
                    }
                }
            }

            // Plain-English Selected Period Readout Box
            val contValAtPoint = selectedPoint.contaminantLevels[contaminant.code] ?: contaminant.detected
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surface2,
                shadowElevation = 6.dp,
                border = BorderStroke(1.5.dp, palette.brand.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SELECTED TIME PERIOD: ${selectedPoint.periodLabel.uppercase()}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.brand
                    )

                    // 3 clear metric pills for the selected point
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimelineReadoutTile(
                            label = "QUALITY SCORE",
                            value = "${selectedPoint.qualityScore} / 100",
                            valueColor = palette.brand,
                            modifier = Modifier.weight(1f)
                        )
                        TimelineReadoutTile(
                            label = plainContaminantName.uppercase(),
                            value = "${ReferenceWaterData.fmtVal(contValAtPoint)} ${contaminant.unit}",
                            valueColor = palette.caution,
                            modifier = Modifier.weight(1f)
                        )
                        TimelineReadoutTile(
                            label = "EPA VIOLATIONS",
                            value = if (selectedPoint.violationCount == 0) "None (0)" else "${selectedPoint.violationCount} Logged",
                            valueColor = if (selectedPoint.violationCount > 0) palette.danger else palette.safe,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!selectedPoint.milestoneNote.isNullOrBlank()) {
                        Text(
                            text = selectedPoint.milestoneNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink
                        )
                    }
                    if (primaryDiagnostic != null) {
                        Text(
                            text = "Trend Takeaway: ${primaryDiagnostic.supplyIndication}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineReadoutTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = H2KnowThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.surface1,
        border = BorderStroke(1.dp, palette.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.ink2,
                maxLines = 1
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LegendPill(label: String, color: Color) {
    val palette = H2KnowThemeTokens.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.ink,
            fontWeight = FontWeight.SemiBold
        )
    }
}
