package app.mimo.notes.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import app.mimo.notes.ui.LocalMimoHaptics
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.preferences.NoteLayout
import app.mimo.notes.ui.theme.dockShape
import androidx.compose.ui.res.stringResource

@Composable
fun FloatingDock(
    layout: NoteLayout,
    quickMenuOpen: Boolean,
    menuOpen: Boolean,
    onMenu: () -> Unit,
    onCompose: () -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockWidth by animateDpAsState(
        targetValue = if (menuOpen) 150.dp else 222.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "dockWidth",
    )
    val composeOffset by animateDpAsState(
        targetValue = if (menuOpen) 17.dp else 84.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "dockComposeOffset",
    )
    val layoutOffset by animateDpAsState(
        targetValue = if (menuOpen) 85.dp else 167.dp,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "dockLayoutOffset",
    )

    val shape = dockShape()
    Surface(
        modifier = modifier.width(dockWidth),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            AnimatedVisibility(
                visible = !menuOpen,
                enter = fadeIn(tween(180)) +
                    slideInVertically(
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 2 },
                    ) +
                    scaleIn(
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        initialScale = 0.82f,
                    ),
                exit = fadeOut(tween(120)) +
                    slideOutVertically(
                        animationSpec = tween(230, easing = FastOutSlowInEasing),
                        targetOffsetY = { -it / 2 },
                    ) +
                    scaleOut(
                        animationSpec = tween(230, easing = FastOutSlowInEasing),
                        targetScale = 0.82f,
                    ),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 7.dp),
            ) {
                DockIconButton(
                    icon = MimoIcons.Menu,
                    contentDescription = stringResource(R.string.menu),
                    onClick = onMenu,
                )
            }
            ComposeButton(
                open = quickMenuOpen,
                onClick = onCompose,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = composeOffset),
            )
            DockIconButton(
                icon = if (layout == NoteLayout.GRID) MimoIcons.ViewAgenda else MimoIcons.GridView,
                contentDescription = stringResource(R.string.toggle_layout),
                onClick = onToggleLayout,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = layoutOffset),
            )
        }
    }
}

@Composable
internal fun DockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    IconButton(
        onClick = {
            haptics.tap()
            onClick()
        },
        modifier = modifier
            .pressScale(interaction, 0.9f)
            .size(48.dp),
        interactionSource = interaction,
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(tween(170)) + scaleIn(
                    initialScale = 0.65f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                )).togetherWith(fadeOut(tween(90)) + scaleOut(targetScale = 0.7f))
            },
            label = "dockIcon",
        ) { currentIcon ->
            Icon(
                currentIcon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun ComposeButton(
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalMimoHaptics.current
    val rotation by animateFloatAsState(
        if (open) 45f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fabRotation",
    )
    Surface(
        onClick = {
            haptics.press()
            onClick()
        },
        interactionSource = interaction,
        modifier = modifier
            .pressScale(interaction, 0.93f)
            .size(54.dp),
        shape = RoundedCornerShape(if (open) 26.dp else 18.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                MimoIcons.Add,
                contentDescription = stringResource(R.string.quick_capture),
                modifier = Modifier
                    .size(26.dp)
                    .rotate(rotation),
            )
        }
    }
}
