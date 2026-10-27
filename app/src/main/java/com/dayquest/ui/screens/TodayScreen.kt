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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.R
import com.dayquest.domain.AppState
import com.dayquest.ui.components.LevelPanel
import com.dayquest.ui.components.QuestTile
import com.dayquest.ui.components.StreakPanel
import com.dayquest.ui.theme.CardShape
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PanelShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif

@Composable
fun TodayScreen(
    state: AppState,
    onQuestClick: (String) -> Unit,
    onNotificationClick: () -> Unit,
    onEnjoyReward: (String) -> Unit = {},
    onOpenCover: () -> Unit = {},
    onResetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val streak = state.streak
    val subtitleText = if (state.securedToday) {
        "Streak secured. $streak ${if (streak == 1) "day" else "days"} running."
    } else if (streak == 0) {
        "Day 1. Finish one quest today to start your streak."
    } else {
        "$streak ${if (streak == 1) "day" else "days"} running. Finish one quest today and it reaches ${streak + 1}."
    }

    val visibleQuests = state.visibleQuests
    val remainingQuests = visibleQuests.filter { !state.claimed.contains(it.id) }
    val remainingXp = remainingQuests.sumOf { it.xp }
    val questsHeaderSubtitle = if (remainingQuests.isNotEmpty()) {
        "${remainingQuests.size} left · $remainingXp XP to earn"
    } else {
        "All cleared"
    }

    val waitingRewards = state.earned.entries.filter { !it.value.enjoyed }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Dq.Ground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Morning, ${state.name.ifEmpty { "Sam" }}.",
                        fontFamily = Serif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitleText,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Dq.Surface)
                            .border(1.dp, Dq.Control, CircleShape)
                            .clickable(role = Role.Button) { onOpenCover() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            fontFamily = Serif,
                            fontSize = 17.sp,
                            color = Dq.Text
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bell),
                            contentDescription = "Notifications",
                            tint = Dq.Text,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(PillShape)
                            .background(Dq.Surface)
                            .border(1.dp, Dq.Control, PillShape)
                            .clickable(role = Role.Button) { onResetClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Reset",
                            fontFamily = Sans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Dq.Muted
                        )
                    }
                }
            }
        }

        // Level Panel
        item {
            LevelPanel(state = state)
        }

        // Streak Panel
        item {
            StreakPanel(state = state)
        }

        // Reward Ready (if any reward waiting)
        if (waitingRewards.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(Dq.Surface)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reward ready",
                            fontFamily = Serif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Dq.Text
                        )
                        Text(
                            text = "${waitingRewards.size} waiting",
                            fontFamily = Sans,
                            fontSize = 13.sp,
                            color = Dq.Muted
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    val displayRewards = waitingRewards.take(3)
                    displayRewards.forEach { (key, reward) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Dq.Raised)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_gift),
                                contentDescription = null,
                                tint = Dq.Text,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reward.text,
                                    fontFamily = Sans,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Dq.Text,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "From ${reward.from}",
                                    fontFamily = Sans,
                                    fontSize = 12.sp,
                                    color = Dq.Muted
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(PillShape)
                                    .border(1.dp, Dq.Control, PillShape)
                                    .clickable(role = Role.Button) { onEnjoyReward(key) }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Enjoyed",
                                    fontFamily = Sans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Dq.Text
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (waitingRewards.size > 3) {
                        Text(
                            text = "+${waitingRewards.size - 3} more in Profile",
                            fontFamily = Sans,
                            fontSize = 12.sp,
                            color = Dq.Muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Today's Quests Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's quests",
                    fontFamily = Serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )
                Text(
                    text = questsHeaderSubtitle,
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted
                )
            }
        }

        // Quest Tiles 2-Column Grid
        if (visibleQuests.isEmpty() || remainingQuests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(CardShape)
                        .border(1.dp, Dq.Control, CardShape)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Every quest cleared.",
                            fontFamily = Serif,
                            fontSize = 18.sp,
                            color = Dq.Text
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Your streak is safe. New quests arrive at 6:00 tomorrow.",
                            fontFamily = Sans,
                            fontSize = 13.sp,
                            color = Dq.Muted
                        )
                    }
                }
            }
        } else {
            val chunks = visibleQuests.chunked(2)
            chunks.forEach { rowQuests ->
                item {
                    if (rowQuests.size == 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                val q = rowQuests[0]
                                QuestTile(
                                    quest = q,
                                    done = state.claimed.contains(q.id),
                                    stepsDone = state.stepsDone[q.id] ?: emptyList(),
                                    selected = state.selectedQuestId == q.id,
                                    onClick = { onQuestClick(q.id) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val q = rowQuests[1]
                                QuestTile(
                                    quest = q,
                                    done = state.claimed.contains(q.id),
                                    stepsDone = state.stepsDone[q.id] ?: emptyList(),
                                    selected = state.selectedQuestId == q.id,
                                    onClick = { onQuestClick(q.id) }
                                )
                            }
                        }
                    } else {
                        val q = rowQuests[0]
                        QuestTile(
                            quest = q,
                            done = state.claimed.contains(q.id),
                            stepsDone = state.stepsDone[q.id] ?: emptyList(),
                            selected = state.selectedQuestId == q.id,
                            onClick = { onQuestClick(q.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}
