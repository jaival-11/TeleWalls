package me.jaival.telewalls.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import me.jaival.telewalls.BuildConfig
import me.jaival.telewalls.data.repository.WallpaperTypeFilter
import me.jaival.telewalls.viewmodel.AuthViewModel
import me.jaival.telewalls.viewmodel.SettingsViewModel

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val description: String,
    val url: String
)

val OPEN_SOURCE_LIBRARIES = listOf(
    OpenSourceLibrary(
        name = "AndroidX Core KTX & Jetpack Compose",
        author = "Google / Android Open Source Project",
        license = "Apache License 2.0",
        description = "Modern toolkit for building native Android UI with Kotlin.",
        url = "https://developer.android.com/jetpack/compose"
    ),
    OpenSourceLibrary(
        name = "Hilt",
        author = "Google Inc.",
        license = "Apache License 2.0",
        description = "Dependency injection library for Android built on top of Dagger.",
        url = "https://dagger.dev/hilt/"
    ),
    OpenSourceLibrary(
        name = "Room Database",
        author = "Google Inc.",
        license = "Apache License 2.0",
        description = "Persistence library providing an abstraction layer over SQLite.",
        url = "https://developer.android.com/training/data-storage/room"
    ),
    OpenSourceLibrary(
        name = "DataStore Preferences",
        author = "Google Inc.",
        license = "Apache License 2.0",
        description = "Data storage solution allowing key-value storage asynchronously.",
        url = "https://developer.android.com/topic/libraries/architecture/datastore"
    ),
    OpenSourceLibrary(
        name = "Coil Compose",
        author = "Coil Contributors",
        license = "Apache License 2.0",
        description = "Image loading library for Android backed by Kotlin Coroutines.",
        url = "https://github.com/coil-kt/coil"
    ),
    OpenSourceLibrary(
        name = "Gson",
        author = "Google Inc.",
        license = "Apache License 2.0",
        description = "Java serialization/deserialization library to convert Objects into JSON.",
        url = "https://github.com/google/gson"
    ),
    OpenSourceLibrary(
        name = "AndroidX Palette",
        author = "Google Inc.",
        license = "Apache License 2.0",
        description = "Color extraction library for picking vibrant colors from bitmaps.",
        url = "https://developer.android.com/training/material/palette"
    ),
    OpenSourceLibrary(
        name = "Lottie Compose",
        author = "Airbnb Inc.",
        license = "Apache License 2.0",
        description = "Vector animation library for rendering After Effects animations natively.",
        url = "https://github.com/airbnb/lottie-android"
    ),
    OpenSourceLibrary(
        name = "TDLib (Telegram Database Library)",
        author = "Telegram Messenger Inc.",
        license = "BSL 1.0 (Boost Software License)",
        description = "Cross-platform library for building custom Telegram clients.",
        url = "https://core.telegram.org/tdlib"
    )
)

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val wallpaperType by settingsViewModel.wallpaperType.collectAsState()
    val reduceAnimations by settingsViewModel.reduceAnimations.collectAsState()
    val hiddenCategories by settingsViewModel.hiddenCategories.collectAsState()
    val syncFavorites by settingsViewModel.syncFavorites.collectAsState()
    val allCategories by settingsViewModel.allCategories.collectAsState()

    var showTypeDialog by remember { mutableStateOf(false) }
    var showHideCategoriesDialog by remember { mutableStateOf(false) }
    var showAppLicenseDialog by remember { mutableStateOf(false) }
    var showOpenSourceDialog by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Title
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -30 }
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    )
                    Text(
                        text = "TeleWalls Configuration",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TOP: Account Details Card (Profile picture, Name, Phone number)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { 40 }
            ) {
                AccountDetailsCard(
                    name = "Jaival Patel",
                    phone = "+91 98765 43210"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Settings Section Card
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Option 1: Type
                        SettingItemRow(
                            icon = Icons.Outlined.Wallpaper,
                            title = "Type",
                            subtitle = when (wallpaperType) {
                                WallpaperTypeFilter.BOTH -> "Phone, Desktop & Tablet Wallpapers"
                                WallpaperTypeFilter.PHONE -> "Phone Wallpapers Only (9:16)"
                                WallpaperTypeFilter.DESKTOP -> "Desktop & Tablet Wallpapers (16:9)"
                            },
                            onClick = { showTypeDialog = true }
                        )

                        // Option 2: Reduce animations toggle (default off)
                        SettingSwitchRow(
                            icon = Icons.Outlined.Animation,
                            title = "Reduce animations",
                            subtitle = "Minimize motion effects",
                            checked = reduceAnimations,
                            onCheckedChange = { settingsViewModel.setReduceAnimations(it) }
                        )

                        // Option 3: Hide Categories
                        SettingItemRow(
                            icon = Icons.Outlined.Category,
                            title = "Hide Categories",
                            subtitle = if (hiddenCategories.isEmpty()) "No categories hidden" else "${hiddenCategories.size} hidden (${hiddenCategories.joinToString(", ")})",
                            onClick = { showHideCategoriesDialog = true }
                        )

                        // Option 4: Sync favourites toggle (default on)
                        SettingSwitchRow(
                            icon = Icons.Outlined.Sync,
                            title = "Sync favourites",
                            subtitle = "Keep your saved wallpapers synced",
                            checked = syncFavorites,
                            onCheckedChange = { settingsViewModel.setSyncFavorites(it) }
                        )

                        // -- DIVIDER --
                        SettingsDivider()

                        // Option 5: Report Bug
                        SettingItemRow(
                            icon = Icons.Outlined.BugReport,
                            title = "Report Bug",
                            subtitle = "Report issues on GitHub",
                            onClick = {
                                openUrl(context, "https://github.com/jaival-11/TeleWalls/issues")
                            }
                        )

                        // Option 6: Request Feature
                        SettingItemRow(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Request Feature",
                            subtitle = "Suggest new ideas or feature improvements",
                            onClick = {
                                openUrl(context, "https://github.com/jaival-11/TeleWalls/issues")
                            }
                        )

                        // Option 7: App page
                        SettingItemRow(
                            icon = Icons.Outlined.Code,
                            title = "App page",
                            subtitle = "https://github.com/jaival-11/TeleWalls",
                            onClick = {
                                openUrl(context, "https://github.com/jaival-11/TeleWalls")
                            }
                        )

                        // Option 8: Contact Developer
                        SettingItemRow(
                            icon = Icons.Outlined.Email,
                            title = "Contact Developer",
                            subtitle = "jaival7909@gmail.com",
                            onClick = {
                                val appVersion = try { BuildConfig.VERSION_NAME } catch (e: Exception) { "1.0.0" }
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:jaival7909@gmail.com?subject=" + Uri.encode("TeleWalls - [$appVersion]"))
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // -- DIVIDER --
                        SettingsDivider()

                        // Option 9: Privacy Policy
                        SettingItemRow(
                            icon = Icons.Outlined.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "https://github.com/jaival-11/TeleWalls/blob/main/PRIVACY.md",
                            onClick = {
                                openUrl(context, "https://github.com/jaival-11/TeleWalls/blob/main/PRIVACY.md")
                            }
                        )

                        // Option 10: App License
                        SettingItemRow(
                            icon = Icons.Outlined.Gavel,
                            title = "App License",
                            subtitle = "Apache License 2.0",
                            onClick = { showAppLicenseDialog = true }
                        )

                        // Option 11: Open source licenses (EXPLICITLY NO ICON)
                        SettingItemRow(
                            icon = null,
                            title = "Open source licenses",
                            subtitle = "View third-party software licenses",
                            onClick = { showOpenSourceDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // FOOTER: Made with ❤️ by Jaival
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { 30 }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Made with ❤️ by ",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "Jaival",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    openUrl(context, "https://github.com/jaival-11")
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Type Selection Dialog
    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = {
                Text(
                    text = "Select Wallpaper Type",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    WallpaperTypeFilter.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    settingsViewModel.setWallpaperType(option)
                                    showTypeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (wallpaperType == option),
                                onClick = {
                                    settingsViewModel.setWallpaperType(option)
                                    showTypeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = option.label,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Hide Categories Multi-Select Dialog
    if (showHideCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showHideCategoriesDialog = false },
            title = {
                Text(
                    text = "Hide Categories",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                if (allCategories.isEmpty()) {
                    Text("No categories available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Select categories you want to hide from TeleWalls:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        allCategories.forEach { category ->
                            val isHidden = hiddenCategories.contains(category)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        settingsViewModel.toggleCategoryHidden(category)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isHidden,
                                    onCheckedChange = {
                                        settingsViewModel.toggleCategoryHidden(category)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHideCategoriesDialog = false }) {
                    Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // App License Dialog
    if (showAppLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showAppLicenseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Gavel, contentDescription = null, tint = primaryColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "TeleWalls App License", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\n" +
                                "Copyright 2026 Jaival Patel (jaival-11)\n\n" +
                                "Licensed under the Apache License, Version 2.0 (the \"License\"); " +
                                "you may not use this file except in compliance with the License. " +
                                "You may obtain a copy of the License at:\n\n" +
                                "http://www.apache.org/licenses/LICENSE-2.0\n\n" +
                                "Unless required by applicable law or agreed to in writing, software " +
                                "distributed under the License is distributed on an \"AS IS\" BASIS, " +
                                "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. " +
                                "See the License for the specific language governing permissions and " +
                                "limitations under the License.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppLicenseDialog = false }) {
                    Text("Close", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Open Source Licenses Dialog (Inspired by Simple File Manager)
    if (showOpenSourceDialog) {
        AlertDialog(
            onDismissRequest = { showOpenSourceDialog = false },
            title = {
                Text(
                    text = "Open Source Licenses",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "TeleWalls is built using the following open source software libraries:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OPEN_SOURCE_LIBRARIES.forEach { lib ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    openUrl(context, lib.url)
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lib.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = primaryColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = lib.license,
                                            color = primaryColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lib.author,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lib.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpenSourceDialog = false }) {
                    Text("Close", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// Account Details Card Component
@Composable
private fun AccountDetailsCard(
    name: String = "Jaival Patel",
    phone: String = "+91 98765 43210",
    photoPath: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!photoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = photoPath,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = name.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                        .ifBlank { "J" }
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info Column (Name & Phone Number)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 19.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Interactive Setting Item Row Component
@Composable
private fun SettingItemRow(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "clickScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            // Indent subtitle/text for items without icon (e.g. Open source licenses)
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Setting Item Row with Switch Component
@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = primaryColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

// Divider Line
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )
}

// Helper to open URLs
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
