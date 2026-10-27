package com.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayquest.data.backup.BackupSummary
import com.dayquest.domain.Quest
import com.dayquest.ui.theme.CardShape
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.InputShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif

@Composable
fun RemoveQuestDialog(
    quest: Quest,
    onConfirmRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(CardShape)
                .background(Dq.Surface)
                .border(1.dp, Dq.Control, CardShape)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Remove '${quest.title}'?",
                    fontFamily = Serif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "You can restore it for 8 seconds after removing. Its past XP and history stay on your stats.",
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Dq.Muted
                )

                Spacer(Modifier.height(28.dp))

                // Actions: Keep it is primary, Remove is secondary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .background(Dq.Text)
                            .clickable(role = Role.Button) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keep it",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Ground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .border(1.dp, Dq.Control, PillShape)
                            .clickable(role = Role.Button) { onConfirmRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Remove",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewardDialog(
    title: String,
    body: String,
    initialText: String?,
    placeholder: String = "e.g. An iced latte",
    chips: List<String> = listOf("A coffee out", "One episode", "30 min of games", "A nice snack"),
    onSave: (String) -> Unit,
    onRemove: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText ?: "") }
    var isError by remember { mutableStateOf(false) }

    fun submit() {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            isError = true
        } else {
            isError = false
            onSave(trimmed)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(CardShape)
                .background(Dq.Surface)
                .border(1.dp, Dq.Control, CardShape)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = title,
                    fontFamily = Serif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = body,
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Dq.Muted
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= 40) {
                            text = it
                            if (isError && it.trim().isNotEmpty()) isError = false
                        }
                    },
                    placeholder = {
                        Text(text = placeholder, fontFamily = Sans, fontSize = 15.sp, color = Dq.Muted)
                    },
                    isError = isError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    shape = InputShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Dq.Ground,
                        unfocusedContainerColor = Dq.Ground,
                        focusedTextColor = Dq.Text,
                        unfocusedTextColor = Dq.Text,
                        cursorColor = Dq.Text,
                        focusedBorderColor = Dq.Text,
                        unfocusedBorderColor = Dq.Control,
                        errorBorderColor = Dq.Error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                if (isError) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Write a reward, or cancel.",
                        fontFamily = Sans,
                        fontSize = 12.sp,
                        color = Dq.Error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Suggestion chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    chips.forEach { chipText ->
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(Dq.Raised)
                                .border(1.dp, Dq.Control, PillShape)
                                .clickable { text = chipText; isError = false }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chipText,
                                fontFamily = Sans,
                                fontSize = 13.sp,
                                color = Dq.Text
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .background(Dq.Text)
                            .clickable(role = Role.Button) { submit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save reward",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Ground
                        )
                    }

                    if (onRemove != null && !initialText.isNullOrBlank()) {
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = "Remove",
                                fontFamily = Sans,
                                fontSize = 14.sp,
                                color = Dq.Error
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            color = Dq.Muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmRestoreDialog(
    summary: BackupSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(CardShape)
                .background(Dq.Surface)
                .border(1.dp, Dq.Control, CardShape)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Replace your current progress?",
                    fontFamily = Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )

                Spacer(Modifier.height(12.dp))

                val dateStr = summary.savedAt?.take(10) ?: "recently"
                Text(
                    text = "${summary.name} · Level ${summary.level} · ${summary.quests} quests · saved $dateStr. Your current quests, XP and streak will be overwritten.",
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Dq.Muted
                )

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .background(Dq.Text)
                            .clickable(role = Role.Button) { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Replace",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Ground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .border(1.dp, Dq.Control, PillShape)
                            .clickable(role = Role.Button) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Text
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(CardShape)
                .background(Dq.Surface)
                .border(1.dp, Dq.Control, CardShape)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Reset all progress?",
                    fontFamily = Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "This will clear all quests, XP, streaks, and custom rewards, returning the app to the first-day setup screen. This cannot be undone without a backup file.",
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Dq.Muted
                )

                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .border(1.dp, Dq.Control, PillShape)
                            .clickable(role = Role.Button) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Text
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(PillShape)
                            .background(Dq.Error)
                            .clickable(role = Role.Button) { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset",
                            fontFamily = Sans,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Text
                        )
                    }
                }
            }
        }
    }
}

