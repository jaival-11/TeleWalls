package me.jaival.telewalls.ui.screens.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.jaival.telewalls.ui.components.CategoryChips
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.jaival.telewalls.core.util.ImageUtils
import me.jaival.telewalls.core.wallpaper.WallpaperTarget
import me.jaival.telewalls.ui.components.glassmorphism
import me.jaival.telewalls.viewmodel.DetailViewModel
import me.jaival.telewalls.viewmodel.WallpaperApplyState
import me.jaival.telewalls.viewmodel.WallpaperDownloadState
import java.io.File

@Composable
fun DetailScreen(
    wallpaperId: String,
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onColorClick: (String) -> Unit = {}
) {
    LaunchedEffect(wallpaperId) {
        viewModel.loadWallpaper(wallpaperId)
    }

    val wallpaper by viewModel.wallpaper.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val applyState by viewModel.applyState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val isLoadingFullImage by viewModel.isLoadingFullImage.collectAsState()
    val imageRefreshKey by viewModel.imageRefreshKey.collectAsState()
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    var showApplyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    var showEditMetadataDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editAuthor by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editWallpaperType by remember { mutableStateOf("Phone") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    var controlsVisible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // Intercept back action when controls are hidden to bring back controls
    BackHandler(enabled = !controlsVisible) {
        controlsVisible = true
    }

    LaunchedEffect(applyState) {
        if (applyState is WallpaperApplyState.Success) {
            Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetApplyState()
        } else if (applyState is WallpaperApplyState.Error) {
            Toast.makeText(context, (applyState as WallpaperApplyState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetApplyState()
        }
    }

    LaunchedEffect(downloadState) {
        if (downloadState is WallpaperDownloadState.Success) {
            Toast.makeText(context, "Wallpaper saved to Gallery!", Toast.LENGTH_SHORT).show()
            viewModel.resetDownloadState()
        } else if (downloadState is WallpaperDownloadState.Error) {
            Toast.makeText(context, (downloadState as WallpaperDownloadState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetDownloadState()
        }
    }

    val currentWall = wallpaper ?: return

    val parsedSize = remember(currentWall.resolution) {
        try {
            val parts = currentWall.resolution.lowercase().split("x")
            if (parts.size == 2) {
                val w = parts[0].trim().toInt()
                val h = parts[1].trim().toInt()
                if (w > 0 && h > 0) IntSize(w, h) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    val activeImageSize = if (imageSize.width > 0 && imageSize.height > 0) imageSize else (parsedSize ?: IntSize.Zero)

    val minAllowedScale = remember(containerSize, activeImageSize) {
        if (containerSize.width > 0 && containerSize.height > 0 && activeImageSize.width > 0 && activeImageSize.height > 0) {
            val containerAspect = containerSize.width.toFloat() / containerSize.height.toFloat()
            val imageAspect = activeImageSize.width.toFloat() / activeImageSize.height.toFloat()
            val fitScale = kotlin.math.min(containerAspect / imageAspect, imageAspect / containerAspect)
            (fitScale * 0.85f).coerceIn(0.05f, 1.0f)
        } else {
            0.15f
        }
    }

    val dynamicColors = currentWall.colors.mapNotNull { hex ->
        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { null }
    }
    val topBgColor = dynamicColors.firstOrNull() ?: MaterialTheme.colorScheme.surface
    val bottomBgColor = dynamicColors.getOrNull(1) ?: MaterialTheme.colorScheme.background

    val fullImageModel = remember(currentWall.localPath, imageRefreshKey) {
        currentWall.localPath?.takeIf { it.isNotBlank() && (it.startsWith("http") || (File(it).exists() && File(it).length() > 0)) }?.let {
            if (it.startsWith("http") || it.startsWith("content://") || it.startsWith("file://")) it else File(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topBgColor.copy(alpha = 0.8f), bottomBgColor, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        // Zoomable and pannable wallpaper image display box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = {
                            if (scale != 1f || offset != Offset.Zero) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                                offset = Offset.Zero
                            }
                        }
                    )
                }
                .pointerInput(minAllowedScale, containerSize, activeImageSize) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(minAllowedScale, 5f)
                        scale = newScale

                        if (containerSize.width > 0 && containerSize.height > 0) {
                            val cw = containerSize.width.toFloat()
                            val ch = containerSize.height.toFloat()

                            val (totalW, totalH) = if (activeImageSize.width > 0 && activeImageSize.height > 0) {
                                val iw = activeImageSize.width.toFloat()
                                val ih = activeImageSize.height.toFloat()
                                val cropScale = kotlin.math.max(cw / iw, ch / ih)
                                Pair(iw * cropScale * newScale, ih * cropScale * newScale)
                            } else {
                                Pair(cw * newScale, ch * newScale)
                            }

                            val maxX = kotlin.math.abs(totalW - cw) / 2f
                            val maxY = kotlin.math.abs(totalH - ch) / 2f

                            if (maxX > 0f || maxY > 0f) {
                                val newX = (offset.x + pan.x).coerceIn(-maxX, maxX)
                                val newY = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                offset = Offset(newX, newY)
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                if (!isLoadingFullImage && fullImageModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fullImageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentWall.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            val w = state.result.drawable.intrinsicWidth
                            val h = state.result.drawable.intrinsicHeight
                            if (w > 0 && h > 0) {
                                imageSize = IntSize(w, h)
                            }
                        }
                    )
                } else if (isLoadingFullImage || fullImageModel == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = primaryColor,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading wallpaper...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Unable to load wallpaper",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Subtle gradient overlay for readability (fades out when controls are hidden)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
        }

        // Top Action Bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row {
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = if (currentWall.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentWall.isFavorite) tertiaryColor else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            editTitle = currentWall.title
                            editAuthor = currentWall.author
                            editCategory = currentWall.category
                            editTags = currentWall.tags.joinToString(", ")
                            editDescription = currentWall.description
                            editWallpaperType = currentWall.wallpaperType.ifBlank { "Phone" }
                            showEditMetadataDialog = true
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Metadata",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            showDeleteConfirmationDialog = true
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Metadata & Actions Sheet
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .glassmorphism(
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentWall.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentWall.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    if (currentWall.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentWall.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metadata Info Row (Resolution, Size, Type, Author)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Resolution",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                            )
                            Text(
                                text = currentWall.resolution,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Column {
                            Text(
                                text = "Type",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                            )
                            Text(
                                text = currentWall.wallpaperType.ifBlank { "Phone" },
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Column {
                            Text(
                                text = "File Size",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                            )
                            Text(
                                text = "${(currentWall.sizeBytes / 1024 / 1024).coerceAtLeast(1)} MB",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Column {
                            Text(
                                text = "Vault Credit",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                            )
                            Text(
                                text = currentWall.author.ifBlank { "TeleWalls" },
                                style = MaterialTheme.typography.bodyMedium.copy(color = primaryColor, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // Palette Swatches
                    if (currentWall.colors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Palette:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentWall.colors.forEach { hex ->
                                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { primaryColor }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                            .clickable { onColorClick(hex) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showApplyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (applyState is WallpaperApplyState.Applying) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(imageVector = Icons.Filled.Wallpaper, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Apply Wallpaper", fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.downloadWallpaperToGallery(context)
                            },
                            enabled = downloadState !is WallpaperDownloadState.Downloading,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            if (downloadState is WallpaperDownloadState.Downloading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Wallpaper Target Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("Set Wallpaper", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select where to apply this wallpaper:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyWallpaper(WallpaperTarget.HOME_SCREEN)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Text("Home Screen", color = primaryColor)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyWallpaper(WallpaperTarget.LOCK_SCREEN)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Text("Lock Screen", color = primaryColor)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyWallpaper(WallpaperTarget.BOTH)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Both Screens", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    // Wallpaper Delete Confirmation Dialog
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = {
                Text(
                    text = "Delete Wallpaper",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this wallpaper? This action cannot be undone and will remove it from your Telegram Vault.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        viewModel.deleteWallpaper(onDeleted = onBackClick)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Wallpaper Edit Metadata Dialog
    if (showEditMetadataDialog) {
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = primaryColor,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AlertDialog(
            onDismissRequest = { showEditMetadataDialog = false },
            title = {
                Text(
                    text = "Edit Metadata",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Wallpaper Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Author
                    OutlinedTextField(
                        value = editAuthor,
                        onValueChange = { editAuthor = it },
                        label = { Text("Author Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wallpaper Type Dropdown
                    var typeDropdownExpanded by remember { mutableStateOf(false) }
                    val typeOptions = listOf("Phone", "Desktop/Tablet")

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editWallpaperType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Wallpaper Type") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Type",
                                    modifier = Modifier.clickable { typeDropdownExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { typeDropdownExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors
                        )
                        DropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            typeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option,
                                            color = if (editWallpaperType == option) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (editWallpaperType == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        editWallpaperType = option
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selection Header & Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        TextButton(
                            onClick = { showAddCategoryDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Category",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Create New",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CategoryChips(
                        selectedCategory = editCategory,
                        onCategorySelected = { editCategory = it },
                        categories = categories
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tags
                    OutlinedTextField(
                        value = editTags,
                        onValueChange = { editTags = it },
                        label = { Text("Tags (comma-separated: neon, dark, city)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMetadata(
                            title = editTitle,
                            author = editAuthor,
                            category = editCategory,
                            tags = editTags,
                            description = editDescription,
                            wallpaperType = editWallpaperType
                        ) {
                            showEditMetadataDialog = false
                            Toast.makeText(context, "Metadata updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditMetadataDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Add New Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryInput = ""
            },
            title = {
                Text(
                    text = "Create New Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for the new category to add it to TeleWalls & sync with Telegram:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.createCategory(trimmed) { created ->
                                editCategory = created
                            }
                            showAddCategoryDialog = false
                            newCategoryInput = ""
                        }
                    },
                    enabled = newCategoryInput.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategoryDialog = false
                        newCategoryInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


