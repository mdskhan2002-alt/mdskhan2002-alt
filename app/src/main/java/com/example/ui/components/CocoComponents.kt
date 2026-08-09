package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TropicalGreenPrimary

/**
 * Core Button styles for the CocoAura shared component library.
 */
enum class CocoButtonStyle {
    Primary,    // Deep organic forest green solid button
    Secondary,  // Vibrant leaf green outline button
    Accent,     // Sun-kissed golden amber solid button
    Text        // Minimal borderless clickable button
}

/**
 * CocoButton is a polished, fully accessible, and mobile-first responsive button component.
 * It strictly adheres to Android spacing, touch-target standards (min 48dp), and Material 3 design tokens.
 */
@Composable
fun CocoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CocoButtonStyle = CocoButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    fullWidth: Boolean = false,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Smooth subtle scaling effect on press/hover for visual polish and dynamic feedback
    val scaleFactor by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.96f
            isHovered -> 1.02f
            else -> 1.0f
        },
        label = "button_scale"
    )

    // Fluid responsive modifier: limit width on larger devices to prevent stretching awkwardly
    val baseModifier = if (fullWidth) {
        modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp) // Capped width as per adaptive constraints
    } else {
        modifier
    }

    val finalModifier = baseModifier
        .scale(scaleFactor)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .heightIn(min = 48.dp) // Standard Material 3 accessible minimum touch target

    // Derive colors based on tokens
    val containerColor = when (style) {
        CocoButtonStyle.Primary -> MaterialTheme.colorScheme.primary
        CocoButtonStyle.Secondary -> Color.Transparent
        CocoButtonStyle.Accent -> MaterialTheme.colorScheme.tertiary
        CocoButtonStyle.Text -> Color.Transparent
    }

    val contentColor = when (style) {
        CocoButtonStyle.Primary -> MaterialTheme.colorScheme.onPrimary
        CocoButtonStyle.Secondary -> MaterialTheme.colorScheme.primary
        CocoButtonStyle.Accent -> MaterialTheme.colorScheme.onTertiary
        CocoButtonStyle.Text -> MaterialTheme.colorScheme.primary
    }

    val border = when (style) {
        CocoButtonStyle.Secondary -> BorderStroke(
            width = 1.5.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
        else -> null
    }

    Button(
        onClick = { if (!isLoading && enabled) onClick() },
        modifier = finalModifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (style == CocoButtonStyle.Secondary || style == CocoButtonStyle.Text) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        interactionSource = interactionSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.wrapContentSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading...",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    letterSpacing = 0.5.sp
                )
            } else {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp)
                    )
                }

                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    letterSpacing = 0.5.sp
                )

                if (trailingIcon != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * CocoCardStyle defines the visual style of a CocoCard.
 */
enum class CocoCardStyle {
    Elevated,   // Soft shadow elevation for hierarchy separation
    Outlined,   // Clean, low-emphasis outlined card
    Filled,     // Styled surface background
    Branded     // Distinct organic gradient card with tropical green left-border accent
}

/**
 * CocoCard is a modern, responsive, and beautifully styled Material 3 card container.
 * Features customizable card styling, dynamic press animations, adaptive width capping,
 * and a decorative tropical accent option for distinctive branding.
 */
@Composable
fun CocoCard(
    modifier: Modifier = Modifier,
    style: CocoCardStyle = CocoCardStyle.Filled,
    onClick: (() -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 16.dp,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Interactive scale effect on click/hover for rich tactile feel
    val scaleFactor by animateFloatAsState(
        targetValue = when {
            onClick == null -> 1.0f
            isPressed -> 0.98f
            isHovered -> 1.01f
            else -> 1.0f
        },
        label = "card_scale"
    )

    // Base responsive modifier setup (max width caps for tablet/expanded viewing as per guide)
    val baseModifier = modifier
        .fillMaxWidth()
        .widthIn(max = 600.dp) // Maintain consistent look in expanded viewports
        .scale(scaleFactor)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)

    // Card Colors & Border matching CocoAura's specific color palette
    val cardColors = when (style) {
        CocoCardStyle.Elevated -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        CocoCardStyle.Outlined -> CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        CocoCardStyle.Filled -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        CocoCardStyle.Branded -> CardDefaults.cardColors(
            containerColor = Color.Transparent, // Drawn via gradient modifier
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val cardElevation = when (style) {
        CocoCardStyle.Elevated -> CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 2.dp)
        else -> CardDefaults.cardElevation(defaultElevation = 0.dp)
    }

    val border = when (style) {
        CocoCardStyle.Outlined -> BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
        else -> null
    }

    // Custom background styling for Branded style (gorgeous forest-green ambient gradient)
    val backgroundModifier = if (style == CocoCardStyle.Branded) {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)
            )
        )
        Modifier
            .drawBehind {
                drawRoundRect(
                    brush = gradientBrush,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
                // Left-side branding vertical bar accent
                drawRect(
                    color = TropicalGreenPrimary,
                    topLeft = Offset(0f, 0f),
                    size = Size(4.dp.toPx(), size.height)
                )
            }
    } else {
        Modifier
    }

    if (onClick != null) {
        Card(
            modifier = baseModifier.then(backgroundModifier),
            shape = RoundedCornerShape(16.dp),
            colors = cardColors,
            elevation = cardElevation,
            border = border,
            onClick = onClick,
            interactionSource = interactionSource
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = baseModifier.then(backgroundModifier),
            shape = RoundedCornerShape(16.dp),
            colors = cardColors,
            elevation = cardElevation,
            border = border
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}
