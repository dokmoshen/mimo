package app.mimo.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.mimo.notes.R

/**
 * The same transparent title bar used by Settings, now shared by every
 * secondary screen. Content scrolls underneath it and fades into [top fog].
 */
@Composable
fun TransparentScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleDot: Color? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleActionButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
            Spacer(Modifier.width(12.dp))
            if (titleDot != null) {
                Box(
                    Modifier
                        .size(11.dp)
                        .background(titleDot, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 60.dp, top = 2.dp),
            )
        }
    }
}

fun transparentHeaderHeight(statusBarTop: Dp, hasSubtitle: Boolean): Dp =
    statusBarTop + if (hasSubtitle) 88.dp else 64.dp

@Composable
fun TransparentTopFog(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val top = backdropTopColor()
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to top,
                    0.55f to top.copy(alpha = 0.94f),
                    0.82f to top.copy(alpha = 0.55f),
                    1f to Color.Transparent,
                )
            )
    )
}

@Composable
fun TransparentBottomFog(
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
) {
    val bottom = backdropBottomColor()
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.5f to bottom.copy(alpha = 0.30f),
                    0.8f to bottom.copy(alpha = 0.82f),
                    1f to bottom,
                )
            )
    )
}
