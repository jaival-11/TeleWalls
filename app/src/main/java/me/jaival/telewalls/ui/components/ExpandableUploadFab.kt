package me.jaival.telewalls.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.jaival.telewalls.ui.theme.LocalReduceAnimations

@Composable
fun ExpandableUploadFab(
    isVisible: Boolean,
    onSingleUploadClick: () -> Unit,
    onMultiUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current
    var isExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Automatically collapse floating options if FAB is hidden on scroll
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            isExpanded = false
        }
    }

    // Bounce scale animation for the main plus/cross FAB button when clicked
    val fabBounceAnim = remember { Animatable(1f) }

    val triggerFabBounce = {
        if (!reduceAnimations) {
            coroutineScope.launch {
                fabBounceAnim.animateTo(
                    targetValue = 0.78f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )
                fabBounceAnim.animateTo(
                    targetValue = 1.16f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                fabBounceAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    // Smooth bouncy rotation (+ to × and × to +)
    val fabIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "fab_icon_rotation"
    )

    // Scaling bounce animation state for option items
    val optionsScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = if (reduceAnimations) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isExpanded) Spring.StiffnessMediumLow else Spring.StiffnessMedium
        ),
        label = "options_scale_anim"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim backdrop behind floating options
        AnimatedVisibility(
            visible = isVisible && isExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        triggerFabBounce()
                        isExpanded = false
                    }
            )
        }

        // Expandable FAB & floating speed dial options
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it * 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it * 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Floating options floating above the plus button
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + scaleIn(
                        initialScale = 0.35f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(150)),
                    exit = slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + scaleOut(
                        targetScale = 0.35f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Option 2: Multi upload (floating on top of Single Upload or top item)
                        FabOptionItem(
                            label = "Multi upload",
                            icon = Icons.Filled.Collections,
                            scale = optionsScale,
                            onClick = {
                                triggerFabBounce()
                                isExpanded = false
                                onMultiUploadClick()
                            }
                        )

                        // Option 1: Single Upload (floating above plus button)
                        FabOptionItem(
                            label = "Single Upload",
                            icon = Icons.Filled.AddPhotoAlternate,
                            scale = optionsScale,
                            onClick = {
                                triggerFabBounce()
                                isExpanded = false
                                onSingleUploadClick()
                            }
                        )
                    }
                }

                // Plus Floating Action Button (rotates to × with bounce animation)
                FloatingActionButton(
                    onClick = {
                        triggerFabBounce()
                        isExpanded = !isExpanded
                    },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isExpanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 14.dp
                    ),
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            scaleX = fabBounceAnim.value
                            scaleY = fabBounceAnim.value
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = if (isExpanded) "Close upload options" else "Open upload options",
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                rotationZ = fabIconRotation
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun FabOptionItem(
    label: String,
    icon: ImageVector,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceAnimations = LocalReduceAnimations.current
    var isPressed by remember { mutableStateOf(false) }

    val itemPressScale by animateFloatAsState(
        targetValue = if (isPressed && !reduceAnimations) 0.88f else 1f,
        animationSpec = if (reduceAnimations) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "option_press_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale * itemPressScale
                scaleY = scale * itemPressScale
                alpha = scale.coerceIn(0f, 1f)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
    ) {
        // Option text pill with glassmorphism styling
        Box(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .glassmorphism(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Mini FAB Option icon button
        SmallFloatingActionButton(
            onClick = {
                isPressed = true
                onClick()
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp
            ),
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
