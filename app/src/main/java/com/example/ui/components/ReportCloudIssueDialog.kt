package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Contaminant
import com.example.data.repository.ReferenceWaterData
import com.example.ui.theme.H2KnowThemeTokens

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
                "Flagged from the public sample: ${it.plainShortName()} detected at ${ReferenceWaterData.fmtVal(it.detected)} ${it.unit} in $systemName."
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
                    text = "This is stored in Firebase for $systemName (ZIP $zip) after you are signed in.",
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
                    label = { Text("Details and when you noticed it") },
                    placeholder = { Text("Describe what you observed and when it started.") },
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
                Text("Submit Report", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.ink2)
            }
        }
    )
}
