package com.dayquest.ui.screens

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
import com.dayquest.domain.GoalSlot
import com.dayquest.domain.RANK_REWARD
import com.dayquest.domain.RankStyle
import com.dayquest.domain.RewardKind
import com.dayquest.domain.STREAK_GOALS
import com.dayquest.domain.key
import com.dayquest.domain.rankInfo
import com.dayquest.domain.rating
import com.dayquest.domain.tierLow
import com.dayquest.ui.components.DqChip
import com.dayquest.ui.components.XpBar
import com.dayquest.ui.theme.CardShape
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PanelShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.RowShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: AppState,
    onRankStyleChange: (RankStyle) -> Unit,
    onSetGoal: (GoalSlot) -> Unit = {},
    onEnjoyReward: (String) -> Unit = {},
    onBackupNow: () -> Unit = {},
    onRestoreFromFile: () -> Unit = {},
    onResetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val r = rating(state)
    val rk = rankInfo(state)
    val isLevel = state.rankStyle == RankStyle.Level

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Dq.Ground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "Profile",
                    fontFamily = Serif,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%s · Level %d · %,d rating", state.name.ifEmpty { "Sam" }, state.level, r),
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted
                )
            }
        }

        // Show my level as panel
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PanelShape)
                    .background(Dq.Surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Show my level as",
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Dq.Text
                )

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RankStyle.entries.forEach { style ->
                        DqChip(
                            text = style.chipLabel,
                            selected = state.rankStyle == style,
                            onClick = { onRankStyleChange(style) },
                            role = Role.RadioButton
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Display only. Your level and XP stay the same. Every level you clear is worth 140 rating, every quest you complete today adds 35, and each game's ladder is mapped onto that.",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted,
                    lineHeight = 18.sp
                )
            }
        }

        // Rank card
        item {
            val cardTop = if (isLevel || rk == null) "Your level" else "Your ${state.rankStyle.label} rank"
            val rankName = if (isLevel || rk == null) "Level ${state.level}" else rk.label
            val fig = if (isLevel || rk == null) {
                String.format(Locale.US, "%,d / %,d XP", state.xp, state.cap)
            } else {
                "${rk.figure} ${rk.unit}".trim()
            }
            val rightInfo = if (isLevel || rk == null) {
                String.format(Locale.US, "%,d rating", r)
            } else {
                String.format(Locale.US, "Level %d · %,d rating", state.level, r)
            }
            val progress = if (isLevel || rk == null) {
                state.xp.toFloat() / state.cap.toFloat()
            } else {
                rk.progress
            }
            val foot = if (isLevel || rk == null) {
                String.format(Locale.US, "%,d XP to Level %d", state.cap - state.xp, state.level + 1)
            } else {
                rk.foot
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PanelShape)
                    .background(Dq.Surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = cardTop,
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = rankName,
                    fontFamily = Serif,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = fig,
                        fontFamily = Sans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Text
                    )
                    Text(
                        text = rightInfo,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                }

                Spacer(Modifier.height(14.dp))

                XpBar(progress = progress)

                Spacer(Modifier.height(10.dp))

                Text(
                    text = foot,
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted
                )
            }
        }

        // Ladder (visible only when not Level style)
        if (!isLevel && rk != null) {
            val tiers = state.rankStyle.tiers
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(Dq.Surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Ladder",
                        fontFamily = Sans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Text
                    )

                    Spacer(Modifier.height(14.dp))

                    tiers.forEachIndexed { i, tier ->
                        val isPast = i < rk.tierIndex
                        val isNow = i == rk.tierIndex
                        val isFuture = i > rk.tierIndex

                        val rangeText = if (i + 1 < tiers.size) {
                            String.format(Locale.US, "%,d–%,d", tier.low, tiers[i + 1].low - 1)
                        } else {
                            String.format(Locale.US, "%,d+", tier.low)
                        }

                        val rowBg = if (isNow) Dq.Raised else androidx.compose.ui.graphics.Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RowShape)
                                .background(rowBg)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dotModifier = when {
                                isNow -> Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Dq.Accent)
                                    .border(2.dp, Dq.Text, CircleShape)
                                isPast -> Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Dq.Accent)
                                else -> Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Dq.Track)
                            }

                            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                                Box(modifier = dotModifier)
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = tier.name,
                                fontFamily = Sans,
                                fontSize = 15.sp,
                                fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isFuture) Dq.Muted else Dq.Text,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = rangeText,
                                fontFamily = Sans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Dq.Muted
                            )
                        }
                    }
                }
            }
        }

        // My Rewards History
        if (state.earned.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PanelShape)
                        .background(Dq.Surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "My rewards",
                        fontFamily = Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Text
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Treats you've written and unlocked.",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        color = Dq.Muted
                    )

                    Spacer(Modifier.height(14.dp))

                    state.earned.forEach { (key, earned) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Dq.Raised)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_gift),
                                contentDescription = null,
                                tint = if (earned.enjoyed) Dq.Muted else Dq.Text,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = earned.text,
                                    fontFamily = Sans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (earned.enjoyed) Dq.Muted else Dq.Text
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${earned.kind.name} · ${earned.from}",
                                    fontFamily = Sans,
                                    fontSize = 12.sp,
                                    color = Dq.Muted
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            if (!earned.enjoyed) {
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(PillShape)
                                        .border(1.dp, Dq.Control, PillShape)
                                        .clickable(role = Role.Button) { onEnjoyReward(key) }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Enjoyed",
                                        fontFamily = Sans,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Dq.Text
                                    )
                                }
                            } else {
                                Text(
                                    text = "Enjoyed",
                                    fontFamily = Sans,
                                    fontSize = 12.sp,
                                    color = Dq.Muted
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // Milestone rewards (Section 4.7.1)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PanelShape)
                    .background(Dq.Surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Milestone rewards",
                        fontFamily = Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Text
                    )
                    Text(
                        text = "Medium and big",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        color = Dq.Muted
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Small rewards go on quests. Bigger ones wait for a streak, a level-up or a rank.",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    color = Dq.Muted,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(18.dp))

                // Group 1: Streaks
                Text(
                    text = "STREAKS",
                    fontFamily = Sans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Dq.Muted
                )
                Spacer(Modifier.height(8.dp))

                STREAK_GOALS.forEach { days ->
                    val isPast = days <= state.streak
                    val reward = state.goals.streak[days]
                    MilestoneRow(
                        title = "$days-day streak",
                        subtitle = if (isPast) "Milestone reached" else reward ?: "Treat yourself when you hit $days days",
                        actionLabel = if (isPast) "Reached" else if (reward != null) "Edit" else "Set",
                        enabled = !isPast,
                        onClick = { onSetGoal(GoalSlot.Streak(days)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(12.dp))

                // Group 2: Next level-up
                Text(
                    text = "NEXT LEVEL-UP",
                    fontFamily = Sans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Dq.Muted
                )
                Spacer(Modifier.height(8.dp))

                val levelReward = state.goals.level
                MilestoneRow(
                    title = "Level ${state.level + 1}",
                    subtitle = levelReward ?: "Treat yourself when you reach Level ${state.level + 1}",
                    actionLabel = if (levelReward != null) "Edit" else "Set",
                    enabled = true,
                    onClick = { onSetGoal(GoalSlot.Level) }
                )

                Spacer(Modifier.height(18.dp))

                // Group 3: Rank milestones
                val gameStyles = listOf(RankStyle.Dota, RankStyle.Mlbb)
                val activeGameStyle = if (state.rankStyle in gameStyles) state.rankStyle else RankStyle.Dota
                Text(
                    text = "${activeGameStyle.label.uppercase()} RANKS",
                    fontFamily = Sans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Dq.Muted
                )
                Spacer(Modifier.height(8.dp))

                RANK_REWARD[activeGameStyle]?.forEach { tierName ->
                    val key = "${activeGameStyle.key}:$tierName"
                    val isReached = rating(state) >= tierLow(activeGameStyle, tierName)
                    val rankReward = state.goals.rank[key]
                    MilestoneRow(
                        title = "${activeGameStyle.label} $tierName",
                        subtitle = if (isReached) "Tier reached" else rankReward ?: "Treat yourself when you reach $tierName",
                        actionLabel = if (isReached) "Reached" else if (rankReward != null) "Edit" else "Set",
                        enabled = !isReached,
                        onClick = { onSetGoal(GoalSlot.Rank(key)) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Backup Panel (Section 10.5)
        item {
            val backupStatus = state.lastBackupAt?.take(16)?.replace("T", ", ") ?: "Never backed up"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PanelShape)
                    .background(Dq.Surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup",
                        fontFamily = Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Text
                    )
                    Text(
                        text = if (state.lastBackupAt != null) "Last backup $backupStatus" else "Never backed up",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        color = Dq.Muted
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Saved on this device · ${state.visibleQuests.size} quests · Level ${state.level}",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    color = Dq.Muted
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Back up to keep a copy of your quests, XP and streak in one file, or to move to a new phone.",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    color = Dq.Muted,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(20.dp))

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
                            .clickable(role = Role.Button) { onBackupNow() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Back up now",
                            fontFamily = Sans,
                            fontSize = 14.sp,
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
                            .clickable(role = Role.Button) { onRestoreFromFile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Restore from file",
                            fontFamily = Sans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.Text
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(PillShape)
                        .border(1.dp, Dq.Control, PillShape)
                        .clickable(role = Role.Button) { onResetClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset demo",
                        fontFamily = Sans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Error
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MilestoneRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Dq.Raised)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Sans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Dq.Text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = Sans,
                fontSize = 12.sp,
                color = Dq.Muted,
                maxLines = 1
            )
        }

        Spacer(Modifier.width(8.dp))

        if (enabled) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(PillShape)
                    .border(1.dp, Dq.Control, PillShape)
                    .clickable(role = Role.Button) { onClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Dq.Text
                )
            }
        } else {
            Text(
                text = actionLabel,
                fontFamily = Sans,
                fontSize = 13.sp,
                color = Dq.Muted
            )
        }
    }
}
