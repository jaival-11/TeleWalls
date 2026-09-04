package me.jaival.telewalls.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.jaival.telewalls.ui.components.AnimatedWallpaperCard
import me.jaival.telewalls.ui.components.CategoryChips
import me.jaival.telewalls.ui.components.glassmorphism
import me.jaival.telewalls.ui.components.ShimmerCard
import me.jaival.telewalls.ui.theme.NeonCyan
import me.jaival.telewalls.ui.theme.VibrantMagenta
import me.jaival.telewalls.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onWallpaperClick: (String) -> Unit
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TeleWalls",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    )
                    Text(
                        text = "TDLib Vault Storage",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Vault Sync Status Indicator
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF191C24))
                        .clickable { viewModel.syncWallpapers() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = "Sync",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshing) "Syncing..." else "Vault Live",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search wallpapers, tags, AMOLED...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = NeonCyan
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF141720),
                    unfocusedContainerColor = Color(0xFF141720),
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            CategoryChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Wallpaper Staggered Grid
            if (wallpapers.isEmpty() && isRefreshing) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(6) {
                        ShimmerCard()
                    }
                }
            } else if (wallpapers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Wallpapers Found",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload high-res photos to your Telegram Vault channel!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f))
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(wallpapers, key = { _, item -> item.id }) { index, wallpaper ->
                        AnimatedWallpaperCard(
                            wallpaper = wallpaper,
                            index = index,
                            onClick = { onWallpaperClick(wallpaper.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(wallpaper.id) }
                        )
                    }
                }
            }
        }
    }
}
