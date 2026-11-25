package app.mimo.notes.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mimo.notes.R
import app.mimo.notes.ui.text.ProofreadKind
import app.mimo.notes.ui.text.ProofreadPlan
import app.mimo.notes.ui.text.SpellingIssue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProofreadDialog(
    plan: ProofreadPlan,
    spellingIssues: List<SpellingIssue> = emptyList(),
    spellingLoading: Boolean = false,
    modelLoading: Boolean = false,
    modelNote: String? = null,
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
    }.joinToString(" · ")
    val hasSpelling = spellingLoading || spellingIssues.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.proofread_title)) },
        text = {
            Column {
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

                if (plan.count > 0) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.proofread_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                    ) {
                        Text(
                            plan.corrected,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                        )
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
