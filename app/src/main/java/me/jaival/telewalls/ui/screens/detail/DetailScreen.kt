package me.jaival.telewalls.ui.screens.detail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.jaival.telewalls.core.wallpaper.WallpaperTarget
import me.jaival.telewalls.ui.components.glassmorphism
import me.jaival.telewalls.ui.theme.NeonCyan
import me.jaival.telewalls.ui.theme.VibrantMagenta
import me.jaival.telewalls.viewmodel.DetailViewModel
import me.jaival.telewalls.viewmodel.WallpaperApplyState

@Composable
fun DetailScreen(
    wallpaperId: String,
    viewModel: DetailViewModel,
    onBackClick: () -> Unit
) {
    LaunchedEffect(wallpaperId) {
        viewModel.loadWallpaper(wallpaperId)
    }

    val wallpaper by viewModel.wallpaper.collectAsState()
    val applyState by viewModel.applyState.collectAsState()
    val context = LocalContext.current

    var showApplyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(applyState) {
        if (applyState is WallpaperApplyState.Success) {
            Toast.makeText(context, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetApplyState()
        } else if (applyState is WallpaperApplyState.Error) {
            Toast.makeText(context, (applyState as WallpaperApplyState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetApplyState()
        }
    }

    val currentWall = wallpaper ?: return

    val dynamicColors = currentWall.colors.mapNotNull { hex ->
        try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { null }
    }
    val topBgColor = dynamicColors.firstOrNull() ?: Color(0xFF0F1117)
    val bottomBgColor = dynamicColors.getOrNull(1) ?: Color(0xFF07080B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topBgColor.copy(alpha = 0.8f), bottomBgColor, Color(0xFF07080B))
                )
            )
    ) {
        // High resolution wallpaper image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentWall.localPath ?: currentWall.thumbnailPath ?: "")
                .crossfade(true)
                .build(),
            contentDescription = currentWall.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Subtle gradient overlay for readibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Top Action Bar
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
                    .background(Color(0x880F1117))
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
                        .background(Color(0x880F1117))
                ) {
                    Icon(
                        imageVector = if (currentWall.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentWall.isFavorite) VibrantMagenta else Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.deleteWallpaper(onDeleted = onBackClick)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x880F1117))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Metadata & Actions Sheet
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .glassmorphism(
                    backgroundColor = Color(0xE60F1117),
                    borderColor = Color(0x3300F0FF),
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
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentWall.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonCyan,
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

                // Metadata Info Row (Resolution, Size, Author)
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
                            style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // Palette Swatches
                if (currentWall.colors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Palette: ",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        currentWall.colors.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { NeonCyan }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
                            containerColor = NeonCyan,
                            contentColor = Color(0xFF07080B)
                        )
                    ) {
                        if (applyState is WallpaperApplyState.Applying) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF07080B))
                        } else {
                            Icon(imageVector = Icons.Filled.Wallpaper, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Apply Wallpaper", fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF191C24))
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = "Download", tint = Color.White)
                    }
                }
            }
        }
    }

    // Wallpaper Target Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("Set Wallpaper", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select where to apply this wallpaper:", color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyWallpaper(WallpaperTarget.HOME_SCREEN)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF191C24))
                    ) {
                        Text("Home Screen", color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyWallpaper(WallpaperTarget.LOCK_SCREEN)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF191C24))
                    ) {
                        Text("Lock Screen", color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyWallpaper(WallpaperTarget.BOTH)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Both Screens", color = Color(0xFF07080B), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF0F1117)
        )
    }
}
