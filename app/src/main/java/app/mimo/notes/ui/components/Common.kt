package app.mimo.notes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.preferences.DesignLanguage
import app.mimo.notes.ui.theme.LocalDesignLanguage
import app.mimo.notes.ui.theme.LocalInterfaceBlur
import app.mimo.notes.ui.theme.LocalMimoDark

@Composable
fun backdropTopColor(): Color =
    if (LocalMimoDark.current) Color(0xFF161A17) else Color(0xFFFDF8F0)

@Composable
fun backdropBottomColor(): Color =
    if (LocalMimoDark.current) Color(0xFF0E110F) else Color(0xFFF4EEE3)

@Composable
fun MimoBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = LocalMimoDark.current
    val scheme = MaterialTheme.colorScheme
    val design = LocalDesignLanguage.current
    val blur = LocalInterfaceBlur.current
    val top = if (design == DesignLanguage.NOTHING) scheme.background else backdropTopColor()
    val bottom = if (design == DesignLanguage.NOTHING) scheme.background else backdropBottomColor()
    val glowPrimary = scheme.primary.copy(alpha = if (dark) 0.16f else 0.10f)
    val glowSecondary = scheme.secondary.copy(alpha = if (dark) 0.12f else 0.08f)
    val glowTertiary = scheme.tertiary.copy(alpha = if (dark) 0.12f else 0.07f)
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(top, bottom)))
            if (design == DesignLanguage.NOTHING) {
                val step = 26.dp.toPx()
                val dot = scheme.onBackground.copy(alpha = if (dark) 0.10f else 0.07f)
                var x = step / 2f
                while (x < size.width) {
                    var y = step / 2f
                    while (y < size.height) {
                        drawCircle(dot, radius = 1.1.dp.toPx(), center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary, Color.Transparent),
                        center = Offset(size.width * 0.08f, size.height * 0.04f),
                        radius = size.width * 0.95f,
                    ),
                    radius = size.width * 0.95f,
                    center = Offset(size.width * 0.08f, size.height * 0.04f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary, Color.Transparent),
                        center = Offset(size.width * 0.96f, size.height * 0.18f),
                        radius = size.width * 0.75f,
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.96f, size.height * 0.18f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowTertiary, Color.Transparent),
                        center = Offset(size.width * 0.16f, size.height * 0.92f),
                        radius = size.width * 0.85f,
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(size.width * 0.16f, size.height * 0.92f),
                )
            }
        }
        if (blur) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .blur(42.dp)
            ) {
                val radius = size.width * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = if (dark) 0.22f else 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.12f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * 0.12f, size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.secondary.copy(alpha = if (dark) 0.16f else 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.92f, size.height * 0.78f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * 0.92f, size.height * 0.78f),
                )
            }
        }
        PaperGrain(Modifier.fillMaxSize())
        content()
    }
}

@Composable
fun MimoLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    accent: Color = MaterialTheme.colorScheme.secondary,
) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = size.minDimension * 0.09f
        val long = size.minDimension * 0.34f
        val diagonal = size.minDimension * 0.24f
        drawLine(color, Offset(center.x, center.y - long), Offset(center.x, center.y + long), stroke, StrokeCap.Round)
        drawLine(color, Offset(center.x - long, center.y), Offset(center.x + long, center.y), stroke, StrokeCap.Round)
        drawLine(
            color,
            Offset(center.x - diagonal, center.y - diagonal),
            Offset(center.x + diagonal, center.y + diagonal),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(center.x + diagonal, center.y - diagonal),
            Offset(center.x - diagonal, center.y + diagonal),
            stroke,
            StrokeCap.Round,
        )
        drawCircle(accent, radius = size.minDimension * 0.105f, center = center)
    }
}

/**
 * The Mimo asterisk, but alive: the four strokes draw themselves in and the
 * mark breathes very slowly. Used on the welcome screen and loading state.
 */
@Composable
fun AnimatedMimoMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    accent: Color = MaterialTheme.colorScheme.secondary,
    animate: Boolean = true,
    breatheEnabled: Boolean = true,
) {
    val draw = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) {
            draw.snapTo(0f)
            draw.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
        } else {
            draw.snapTo(1f)
        }
    }
    val breathe = if (breatheEnabled) {
        val transition = rememberInfiniteTransition(label = "mimoBreathe")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "mimoBreatheScale",
        )
        value
    } else {
        1f
    }
    Canvas(modifier.graphicsLayer {
        scaleX = breathe
        scaleY = breathe
    }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = size.minDimension * 0.09f
        val long = size.minDimension * 0.34f
        val diagonal = size.minDimension * 0.24f
        val progress = draw.value

        fun strokeLine(from: Offset, to: Offset, start: Float) {
            val local = ((progress - start) / 0.55f).coerceIn(0f, 1f)
            if (local <= 0f) return
            drawLine(
                color = color,
                start = from,
                end = from + (to - from) * local,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        strokeLine(Offset(center.x, center.y - long), Offset(center.x, center.y + long), 0f)
        strokeLine(Offset(center.x - long, center.y), Offset(center.x + long, center.y), 0.12f)
        strokeLine(
            Offset(center.x - diagonal, center.y - diagonal),
            Offset(center.x + diagonal, center.y + diagonal),
            0.24f,
        )
        strokeLine(
            Offset(center.x + diagonal, center.y - diagonal),
            Offset(center.x - diagonal, center.y + diagonal),
            0.36f,
        )
        val dotProgress = ((progress - 0.62f) / 0.38f).coerceIn(0f, 1f)
        if (dotProgress > 0f) {
            drawCircle(
                accent,
                radius = size.minDimension * 0.105f * dotProgress,
                center = center,
            )
        }
    }
}

@Composable
fun CircleActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    size: Int = 48,
) {
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size((size * 0.46f).dp))
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmptyState(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    illustration: MimoIllustration? = null,
) {
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration != null) {
            MimoIllustrationArt(
                kind = illustration,
                modifier = Modifier
                    .width(190.dp)
                    .padding(bottom = 2.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape),
                contentAlignment = Alignment.Center,
            ) {
                MimoLogo(
                    Modifier
                        .size(86.dp)
                        .alpha(0.10f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    accent = MaterialTheme.colorScheme.secondary,
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Spacer(Modifier.height(if (illustration != null) 10.dp else 22.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            val haptics = LocalMimoHaptics.current
            Surface(
                onClick = {
                    haptics.press()
                    onAction()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun SearchPill(
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: (() -> Unit)? = null,
    filterActive: Boolean = false,
) {
    val haptics = LocalMimoHaptics.current
    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MimoIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                hint,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onFilterClick != null) {
                IconButton(onClick = {
                    haptics.tap()
                    onFilterClick()
                }) {
                    Icon(
                        MimoIcons.Tune,
                        contentDescription = null,
                        tint = if (filterActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun InlineSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    onBack: () -> Unit,
    onFilterClick: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val haptics = LocalMimoHaptics.current
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = CircleShape,
        color = scheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                haptics.tap()
                onBack()
            }) {
                Icon(MimoIcons.ArrowBack, contentDescription = "Назад", tint = scheme.onSurface)
            }
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { focusManager.clearFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    hint,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = {
                    haptics.tap()
                    onValueChange("")
                }) {
                    Icon(MimoIcons.Close, contentDescription = "Очистить", tint = scheme.onSurfaceVariant)
                }
            }
            if (onFilterClick != null) {
                IconButton(onClick = {
                    haptics.tap()
                    onFilterClick()
                }) {
                    Icon(MimoIcons.Tune, contentDescription = "Фильтры", tint = scheme.primary)
                }
            }
        }
    }
}

@Composable
fun MimoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingColor: Color? = null,
) {
    val haptics = LocalMimoHaptics.current
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "chipContainer",
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "chipContent",
    )
    Surface(
        onClick = {
            haptics.toggle(!selected)
            onClick()
        },
        modifier = modifier,
        shape = CircleShape,
        color = container,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = leadingColor ?: content, modifier = Modifier.size(16.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
    }
}

@Composable
fun LabelDot(color: Color, size: Int = 8) {
    Box(
        Modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}
