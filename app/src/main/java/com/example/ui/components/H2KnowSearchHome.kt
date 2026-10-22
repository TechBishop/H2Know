package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaceInfo
import com.example.data.model.WaterSystemSummary
import com.example.data.remote.LocationHistoryDocument
import com.example.data.remote.WaterIssueDocument
import com.example.data.repository.ReferenceWaterData
import com.example.ui.CloudDataState
import com.example.ui.theme.H2KnowThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay

@Composable
fun H2KnowStreamlinedTopBar(
    title: String,
    subtitle: String?,
    showBackButton: Boolean,
    isDarkTheme: Boolean,
    userEmail: String?,
    isGuestMode: Boolean,
    onBackClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val palette = H2KnowThemeTokens.colors
    val haptic = LocalHapticFeedback.current
    var showSupportConfirmDialog by remember { mutableStateOf(false) }

    if (showSupportConfirmDialog) {
        H2KnowExternalSupportConfirmDialog(
            onDismiss = { showSupportConfirmDialog = false }
        )
    }

    // Floating Glass Capsule Header Island
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surface1.copy(alpha = 0.94f),
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        palette.brand.copy(alpha = 0.40f),
                        palette.border,
                        palette.safe.copy(alpha = 0.28f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBackButton) {
                        Surface(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surface2,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("top_back_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Search",
                                    tint = palette.brand,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ZIPs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.ink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(palette.brand, palette.safe)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = palette.brand.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "Beta",
                                    color = palette.brand,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle ?: (userEmail ?: "Know your tap water"),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Original Buy Me a Coffee Donate / Support Button (non-focusable so keyboard Enter on ZIP never triggers it)
                    Surface(
                        onClick = {
                            showSupportConfirmDialog = true
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = palette.donateBg,
                        border = BorderStroke(1.dp, palette.donate.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .height(38.dp)
                            .focusProperties { canFocus = false }
                            .testTag("top_donate_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Support H2Know on Buy Me a Coffee",
                                tint = palette.donate,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Support",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.donate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleTheme()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.surface2)
                            .focusProperties { canFocus = false }
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme",
                            tint = palette.ink2,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!isGuestMode) {
                        IconButton(
                            onClick = onSignOutClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.surface2)
                                .focusProperties { canFocus = false }
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign out",
                                tint = palette.ink2,
                                modifier = Modifier.size(18.dp)
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
fun H2KnowSearchAndCloudHome(
    zipQuery: String,
    isSearching: Boolean,
    searchError: String?,
    currentPlace: PlaceInfo?,
    providers: List<WaterSystemSummary>,
    utilityHits: List<WaterSystemSummary> = emptyList(),
    cloudHistoryState: CloudDataState<List<LocationHistoryDocument>>,
    cloudIssuesState: CloudDataState<List<WaterIssueDocument>>,
    onZipChange: (String) -> Unit,
    onSubmitSearch: (String?) -> Unit,
    onSelectProvider: (pwsid: String, zipHint: String?) -> Unit,
    onSearchUtility: (String) -> Unit = {},
    onDeleteHistory: (String) -> Unit,
    onDeleteIssue: (String) -> Unit,
    onRequestSignIn: () -> Unit = {}
) {
    val palette = H2KnowThemeTokens.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    fun dismissKeyboardAndSubmit(overrideZip: String?) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        keyboardController?.hide()
        focusManager.clearFocus()
        onSubmitSearch(overrideZip)
    }

    // Automatically hide keyboard after the 5th ZIP digit finishes recomposing and laying out
    LaunchedEffect(zipQuery) {
        if (zipQuery.length == 5) {
            delay(60)
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Floating Hero Search Card
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface1.copy(alpha = 0.95f),
            shadowElevation = 20.dp,
            tonalElevation = 6.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        palette.brand.copy(alpha = 0.45f),
                        palette.border,
                        palette.safe.copy(alpha = 0.30f)
                    )
                )
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = palette.brandSoft,
                    border = BorderStroke(1.dp, palette.brand.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "U.S. TAP WATER QUALITY & FILTER GUIDE",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.brand,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your tap water, scored.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.ink,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter a 5-digit ZIP code. The score uses public EPA records for that utility, not a preset number.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink2
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = zipQuery,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(5)
                            onZipChange(clean)
                        },
                        placeholder = { Text("Enter 5-digit ZIP (e.g. 78701)") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = palette.brand)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { dismissKeyboardAndSubmit(null) },
                            onDone = { dismissKeyboardAndSubmit(null) }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.brand,
                            unfocusedBorderColor = palette.border,
                            focusedContainerColor = palette.surface2.copy(alpha = 0.6f),
                            unfocusedContainerColor = palette.surface2.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        dismissKeyboardAndSubmit(null)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag("zip_search_input")
                    )
                    Button(
                        onClick = { dismissKeyboardAndSubmit(null) },
                        enabled = !isSearching,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.brand,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("zip_search_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Inspect", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (searchError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.dangerBg,
                        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = searchError,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.danger,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                        )
                    }
                }

                // Instant Inline Results right below the Search Box (above preset chips!)
                AnimatedVisibility(
                    visible = providers.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it / 6 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WATER PROVIDERS IN ${currentPlace?.zip ?: zipQuery} (${providers.size})",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.safe,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentPlace?.city ?: ""}, ${currentPlace?.state ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.ink2,
                                maxLines = 1
                            )
                        }

                        var utilityQuery by remember(currentPlace?.zip, providers.size) { mutableStateOf("") }
                        val shown = providers.filter { provider ->
                            val q = utilityQuery.trim()
                            q.isEmpty() || provider.name.contains(q, ignoreCase = true) || provider.pwsid.contains(q, ignoreCase = true)
                        }
                        OutlinedTextField(
                            value = utilityQuery,
                            onValueChange = { utilityQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Name or PWSID") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchUtility(utilityQuery) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = palette.ink,
                                unfocusedTextColor = palette.ink,
                                focusedBorderColor = palette.brand,
                                unfocusedBorderColor = palette.borderStrong,
                                cursorColor = palette.brand
                            )
                        )
                        TextButton(onClick = { onSearchUtility(utilityQuery) }) {
                            Text("Search this state", color = palette.brand)
                        }
                        shown.forEach { provider ->
                            ProviderChoice(provider, onSelectProvider)
                        }
                        if (utilityHits.isNotEmpty()) {
                            Text(
                                text = "STATEWIDE MATCHES",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.brand
                            )
                            utilityHits.forEach { provider ->
                                ProviderChoice(provider, onSelectProvider)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "POPULAR CITIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ink3
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReferenceWaterData.zipPresets.forEach { preset ->
                        Surface(
                            onClick = { dismissKeyboardAndSubmit(preset.zip) },
                            shape = RoundedCornerShape(14.dp),
                            color = palette.surface2,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.testTag("preset_zip_${preset.zip}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.zip,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.brand
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${preset.city}, ${preset.state}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.ink
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Water Reports / Recent Searches
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surface1.copy(alpha = 0.92f),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.brandSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = palette.brand,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Saved Locations",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.ink,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quick access to your checked ZIP codes",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.ink3
                            )
                        }
                    }
                }

                when (cloudHistoryState) {
                    is CloudDataState.GuestRequired -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surface2,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sign in with Google to save your searched ZIP codes and report local water issues.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.ink2,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = onRequestSignIn,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, palette.brand),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Sign In", style = MaterialTheme.typography.labelSmall, color = palette.brand)
                                }
                            }
                        }
                    }

                    is CloudDataState.Loading -> {
                        Text(
                            text = "Loading saved locations…",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3
                        )
                    }

                    is CloudDataState.Error -> {
                        Text(
                            text = cloudHistoryState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.danger
                        )
                    }

                    is CloudDataState.Success -> {
                        val items = cloudHistoryState.data
                        if (items.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surface2,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Search any ZIP code above to save a water report here for quick comparison.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.ink2,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            items.take(6).forEach { hist ->
                                val displayScore = hist.qualityScore.toInt()
                                val scoreColor = when {
                                    displayScore >= 80 -> palette.safe
                                    displayScore >= 60 -> palette.caution
                                    else -> palette.danger
                                }
                                Surface(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onSelectProvider(hist.pwsid, hist.zip)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.surface2,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(1.dp, palette.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = "${hist.systemName} · ZIP ${hist.zip}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.ink,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${hist.city}, ${hist.state} · 5Y Trend ${if (hist.fiveYearDelta >= 0) "+${hist.fiveYearDelta}" else "${hist.fiveYearDelta}"} pts",
                                                fontFamily = JetBrainsMonoFamily,
                                                fontSize = 11.sp,
                                                color = palette.brand
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = scoreColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "$displayScore",
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = scoreColor,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        IconButton(onClick = { onDeleteHistory(hist.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete history",
                                                tint = palette.ink3,
                                                modifier = Modifier.size(18.dp)
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

        // 3. User-Reported Water Quality Issues (Stored in Firebase Firestore)
        if (cloudIssuesState is CloudDataState.Success && cloudIssuesState.data.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = palette.surface1.copy(alpha = 0.92f),
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your Cloud-Reported Water Issues (${cloudIssuesState.data.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink,
                        fontWeight = FontWeight.Bold
                    )
                    cloudIssuesState.data.take(4).forEach { issue ->
                        Surface(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onSelectProvider(issue.pwsid, issue.zip)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = palette.surface2,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${issue.title} · ZIP ${issue.zip}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.ink,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${issue.systemName} · ${issue.category} · ${issue.status.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.ink2
                                    )
                                }
                                IconButton(onClick = { onDeleteIssue(issue.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete issue",
                                        tint = palette.danger,
                                        modifier = Modifier.size(18.dp)
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
private fun ProviderChoice(
    provider: WaterSystemSummary,
    onSelectProvider: (pwsid: String, zipHint: String?) -> Unit
) {
    val palette = H2KnowThemeTokens.colors
    val people = if (provider.population > 0) {
        String.format(Locale.US, "%,d", provider.population)
    } else {
        "Not listed"
    }
    val where = listOf(provider.county, provider.state).filter { it.isNotBlank() }.joinToString(", ")
    Surface(
        onClick = { onSelectProvider(provider.pwsid, provider.zip) },
        shape = RoundedCornerShape(14.dp),
        color = palette.surface2,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_card_${provider.pwsid}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = provider.name + if (provider.listedForPlace) "  ·  Listed for this city" else "",
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${provider.pwsid} · $people people${if (where.isBlank()) "" else " · $where"}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.ink2
            )
        }
    }
}
