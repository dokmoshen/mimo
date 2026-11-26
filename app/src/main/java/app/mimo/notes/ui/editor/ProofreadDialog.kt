package app.mimo.notes.ui.editor

import app.mimo.notes.ui.components.MimoIcons
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.ui.text.ProofreadKind
import app.mimo.notes.ui.text.ProofreadPlan
import app.mimo.notes.ui.text.SpellingIssue
import app.mimo.notes.ui.text.buildProofreadDiff

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProofreadDialog(
    plan: ProofreadPlan,
    spellingIssues: List<SpellingIssue> = emptyList(),
    spellingLoading: Boolean = false,
    modelLoading: Boolean = false,
    modelNote: String? = null,
    aiAvailable: Boolean = false,
    aiLoading: Boolean = false,
    aiError: String? = null,
    onAiProofread: () -> Unit = {},
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onSpellingFix: (SpellingIssue, String) -> Unit = { _, _ -> },
) {
    val counts = plan.countByKind()
    val summary = buildList {
        counts[ProofreadKind.SPACING]?.let { add("${stringResource(R.string.proofread_spacing)}: $it") }
        counts[ProofreadKind.PUNCTUATION]?.let { add("${stringResource(R.string.proofread_punctuation)}: $it") }
        counts[ProofreadKind.CAPITALIZATION]?.let { add("${stringResource(R.string.proofread_capitalization)}: $it") }
        counts[ProofreadKind.TYPOGRAPHY]?.let { add("${stringResource(R.string.proofread_typography)}: $it") }
        counts[ProofreadKind.AI]?.let { add("${stringResource(R.string.proofread_ai)}: $it") }
    }.joinToString(" · ")
    val hasSpelling = spellingLoading || spellingIssues.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.proofread_title)) },
        text = {
            Column {
                if (spellingLoading || modelLoading || aiLoading) {
                    InkScanner()
                    Spacer(Modifier.height(12.dp))
                }
                if (plan.count > 0) {
                    Text(
                        stringResource(R.string.proofread_found, plan.count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (summary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        stringResource(
                            if (hasSpelling) R.string.proofread_rules_clean else R.string.proofread_clean
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (modelLoading) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.proofread_model_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (modelNote != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        modelNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (hasSpelling) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.proofread_spelling),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (spellingLoading && spellingIssues.isEmpty()) {
                        Text(
                            stringResource(R.string.proofread_spelling_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        spellingIssues.take(4).forEach { issue ->
                            Column(Modifier.padding(vertical = 3.dp)) {
                                Text(
                                    issue.word,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    issue.suggestions.forEach { suggestion ->
                                        Surface(
                                            onClick = { onSpellingFix(issue, suggestion) },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ) {
                                            Text(
                                                suggestion,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (aiAvailable) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = onAiProofread,
                        enabled = !aiLoading,
                    ) {
                        Icon(
                            MimoIcons.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                if (aiLoading) R.string.ai_checking else R.string.ai_proofread
                            ),
                        )
                    }
                    if (aiError != null) {
                        Text(
                            aiError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                if (plan.count > 0) {
                    val errorColor = MaterialTheme.colorScheme.error
                    val changeColor = MaterialTheme.colorScheme.primary
                    val diff = remember(plan.original, plan.corrected, errorColor, changeColor) {
                        buildProofreadDiff(
                            original = plan.original,
                            corrected = plan.corrected,
                            errorColor = errorColor,
                            changeColor = changeColor,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.proofread_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                        ) {
                            Text(
                                stringResource(R.string.proofread_before),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                diff.original,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.proofread_after),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                diff.corrected,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (diff.hasGapMarker) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.proofread_gap_marker),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.proofread_keep_format),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (plan.count > 0) {
                TextButton(onClick = onApply) {
                    Text(stringResource(R.string.proofread_apply), fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun InkScanner() {
    val transition = rememberInfiniteTransition(label = "proofreadScan")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "proofreadScanPhase",
    )
    val color = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
        val centerY = size.height / 2f
        drawLine(track, Offset(0f, centerY), Offset(size.width, centerY), 1.dp.toPx())
        val step = 13.dp.toPx()
        var x = step / 2f
        while (x < size.width) {
            val wave = 1f - kotlin.math.abs(x / size.width - phase)
            val r = (1.2f + 1.8f * wave.coerceIn(0f, 1f)).dp.toPx()
            drawCircle(
                color = color.copy(alpha = 0.25f + 0.55f * wave.coerceIn(0f, 1f)),
                radius = r,
                center = Offset(x, centerY),
            )
            x += step
        }
        val scanX = size.width * phase
        drawLine(
            color = color,
            start = Offset(scanX, 2.dp.toPx()),
            end = Offset(scanX, size.height - 2.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
