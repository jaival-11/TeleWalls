package me.jaival.telewalls.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.jaival.telewalls.ui.navigation.ScreenRoutes
import me.jaival.telewalls.ui.theme.NeonCyan
import kotlin.math.roundToInt

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val defaultBottomNavItems = listOf(
    NavItem(ScreenRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(ScreenRoutes.FAVORITES, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    NavItem(ScreenRoutes.UPLOAD, "Upload", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload),
    NavItem(ScreenRoutes.AUTH, "Vault", Icons.Filled.Lock, Icons.Outlined.Lock),
    NavItem(ScreenRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AnimatedBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    items: List<NavItem> = defaultBottomNavItems,
    modifier: Modifier = Modifier
) {
    var itemPositions by remember { mutableStateOf(List(items.size) { Offset.Zero }) }
    var itemWidths by remember { mutableStateOf(List(items.size) { 0f }) }

    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val isInitialized = itemWidths.all { it > 0f }

    val indicatorHeight = 48.dp
    val indicatorWidth = 54.dp
    val indicatorWidthPx = with(LocalDensity.current) { indicatorWidth.toPx() }

    val targetOffsetX = if (isInitialized) {
        itemPositions[selectedIndex].x + (itemWidths[selectedIndex] - indicatorWidthPx) / 2f
    } else {
        0f
    }

    val animatedOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pill_offset_x"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(34.dp))
            .glassmorphism(
                backgroundColor = Color(0xE60F1117),
                borderColor = Color(0x2200F0FF),
                shape = RoundedCornerShape(34.dp)
            )
    ) {
        if (isInitialized) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .align(Alignment.CenterStart)
                    .size(width = indicatorWidth, height = indicatorHeight)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.2f))
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route
                val coroutineScope = rememberCoroutineScope()
                var isPressed by remember { mutableStateOf(false) }

                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.82f else if (isSelected) 1.18f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "nav_icon_scale"
                )

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f),
                    animationSpec = tween(250),
                    label = "nav_icon_tint"
                )

                val offsetY by animateFloatAsState(
                    targetValue = if (isSelected) -4f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = 300f
                    ),
                    label = "nav_icon_offset_y"
                )

                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .onGloballyPositioned { coordinates ->
                            val newPositions = itemPositions.toMutableList()
                            newPositions[index] = coordinates.positionInParent()
                            itemPositions = newPositions

                            val newWidths = itemWidths.toMutableList()
                            newWidths[index] = coordinates.size.width.toFloat()
                            itemWidths = newWidths
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                isPressed = true
                                delay(90)
                                isPressed = false
                            }
                            onNavigate(item.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationY = offsetY
                            },
                        tint = iconTint
                    )
                }
            }
        }
    }
}
