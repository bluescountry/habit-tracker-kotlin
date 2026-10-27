package com.dayquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.R
import com.dayquest.domain.AppState
import com.dayquest.ui.components.StepRow
import com.dayquest.ui.theme.CardShape
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PanelShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import com.dayquest.ui.theme.fill
import com.dayquest.ui.theme.glyph
import com.dayquest.ui.theme.ink
import com.dayquest.ui.theme.secondary

@Composable
fun QuestDetailScreen(
    questId: String,
    state: AppState,
    onBack: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onAdvanceStep: () -> Unit,
    onClaim: () -> Unit,
    onSetRewardClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val quest = state.quests.find { it.id == questId } ?: state.quests.firstOrNull() ?: return
    val isClaimed = state.claimed.contains(quest.id)
    val stepsDone = state.stepsDone[quest.id] ?: List(quest.steps.size) { false }
    val doneCount = stepsDone.count { it }
    val allDone = doneCount == quest.steps.size

    val category = quest.category
    val detailTitle = if (quest.isCustom || listOf("read", "listen", "watch").contains(quest.id)) {
        quest.title
    } else {
        quest.title.lowercase()
    }

    val rewardText = state.rewards[quest.id]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Dq.Ground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                // Top row with Back button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Dq.Text,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Quest",
                        fontFamily = Sans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Text
                    )
                }
            }

            // Hero Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(category.fill)
                        .padding(20.dp)
                ) {
                    Icon(
                        painter = painterResource(category.glyph),
                        contentDescription = null,
                        tint = category.ink,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = detailTitle,
                        fontFamily = Serif,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 40.sp,
                        color = category.ink
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = quest.story,
                        fontFamily = Sans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = category.secondary
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Reward",
                            fontFamily = Sans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = category.secondary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(
                            text = "+${quest.xp}",
                            fontFamily = Sans,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = category.ink
                        )
                        Text(
                            text = "XP",
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = category.ink,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            // Reward Row (Section 4.2.2)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Dq.Surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gift),
                        contentDescription = null,
                        tint = Dq.Text,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (!rewardText.isNullOrBlank()) {
                            Text(
                                text = rewardText,
                                fontFamily = Sans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Dq.Text,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Your reward",
                                fontFamily = Sans,
                                fontSize = 12.sp,
                                color = Dq.Muted
                            )
                        } else {
                            Text(
                                text = "Reward",
                                fontFamily = Sans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Dq.Text
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Treat yourself when you finish",
                                fontFamily = Sans,
                                fontSize = 12.sp,
                                color = Dq.Muted
                            )
                        }
                    }

                    if (!isClaimed) {
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(PillShape)
                                .border(1.dp, Dq.Control, PillShape)
                                .clickable(role = Role.Button) { onSetRewardClick() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (!rewardText.isNullOrBlank()) "Edit" else "Set",
                                fontFamily = Sans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Dq.Text
                            )
                        }
                    }
                }
            }

            // Steps header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Steps",
                        fontFamily = Sans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Text
                    )
                    Text(
                        text = "$doneCount of ${quest.steps.size} done",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                }
            }

            // Steps List
            itemsIndexed(quest.steps) { index, stepText ->
                StepRow(
                    text = stepText,
                    done = stepsDone.getOrElse(index) { false },
                    enabled = !isClaimed,
                    onToggle = { onToggleStep(index) }
                )
            }

            // Remove quest button (unclaimed only)
            if (!isClaimed) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(PillShape)
                            .border(1.dp, Dq.Control, PillShape)
                            .clickable(role = Role.Button) { onRemoveClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Remove quest",
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Dq.Muted
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(100.dp)) // padding for bottom button
            }
        }

        // Bottom pinned primary button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Dq.Ground)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            val (buttonBg, buttonTextColor, buttonLabel, isEnabled) = when {
                isClaimed -> Quad(Color(0xFF2A2A28), Color(0xFFB4B4B0), "Completed · +${quest.xp} XP earned", false)
                allDone -> Quad(Dq.Text, Dq.Ground, "Claim +${quest.xp} XP", true)
                doneCount == 0 -> Quad(Dq.Text, Dq.Ground, "Start quest", true)
                else -> Quad(Dq.Text, Dq.Ground, "Mark next step done", true)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(PillShape)
                    .background(buttonBg)
                    .clickable(
                        enabled = isEnabled,
                        role = Role.Button,
                        onClick = {
                            if (allDone) {
                                onClaim()
                            } else {
                                onAdvanceStep()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonLabel,
                    fontFamily = Sans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = buttonTextColor
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
