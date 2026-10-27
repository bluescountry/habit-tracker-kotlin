package com.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.data.SeedData
import com.dayquest.domain.AppState
import com.dayquest.domain.Category
import com.dayquest.ui.theme.BarShape
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PanelShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans
import java.util.Locale

@Composable
fun KpiStrip(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val isDemo = state.streakBase >= 12
    val streak = state.streak
    val longest = if (isDemo) maxOf(12, streak) else streak
    val questsThisWeek = (if (isDemo) SeedData.WEEK_QUESTS_BASE else 0) + state.claimed.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Dq.Surface)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        KpiColumn(label = "Streak", value = "$streak", unit = if (streak == 1) "day" else "days", modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(Dq.Border)
        )
        KpiColumn(label = "Longest", value = "$longest", unit = if (longest == 1) "day" else "days", modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(48.dp)
                .background(Dq.Border)
        )
        KpiColumn(label = "Quests", value = "$questsThisWeek", unit = "this week", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KpiColumn(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontFamily = Sans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Dq.Muted
        )
        Text(
            text = value,
            fontFamily = Sans,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            color = Dq.Text
        )
        Text(
            text = unit,
            fontFamily = Sans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Dq.Muted
        )
    }
}

@Composable
fun XpChartPanel(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val isDemo = state.streakBase >= 12
    val claimedQuests = state.quests.filter { state.claimed.contains(it.id) }
    val todayXp = claimedQuests.sumOf { it.xp }
    val vals = if (isDemo) {
        SeedData.WEEK_XP + todayXp
    } else {
        val dow = java.time.LocalDate.now().dayOfWeek.value
        val todayIdx = (dow - 1).coerceIn(0, 6)
        List(7) { idx -> if (idx == todayIdx) todayXp else 0 }
    }
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxVal = 500f

    val accessibleDescription = "XP per day this week: " +
            vals.mapIndexed { i, v -> "${dayNames[i]} $v" }.joinToString(", ")

    Column(
        modifier = modifier
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
                text = "XP per day",
                fontFamily = Sans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Dq.Text
            )
            Text(
                text = String.format(Locale.US, "Today %,d XP", todayXp),
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Dq.Muted
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics {
                    contentDescription = accessibleDescription
                }
        ) {
            // Gridlines at 0, 250, 500 (top, middle, bottom)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                HorizontalDivider(color = Dq.Border, thickness = 1.dp)
                HorizontalDivider(color = Dq.Border, thickness = 1.dp)
                HorizontalDivider(color = Dq.Border, thickness = 1.dp)
            }

            // Y labels
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("500", fontFamily = Sans, fontSize = 10.sp, color = Dq.Muted)
                Text("250", fontFamily = Sans, fontSize = 10.sp, color = Dq.Muted)
                Text("0", fontFamily = Sans, fontSize = 10.sp, color = Dq.Muted)
            }

            // Columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                vals.forEachIndexed { index, v ->
                    val isToday = index == 6
                    val barHeightFraction = (v / maxVal).coerceIn(0f, 1f)
                    val barColor = if (isToday) Dq.Accent else Dq.BarPast

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "$v",
                            fontFamily = Sans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isToday) Dq.Text else Dq.Muted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(barHeightFraction * 0.78f)
                                .clip(BarShape)
                                .background(barColor)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // X labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.forEachIndexed { index, name ->
                val isToday = index == 6
                Text(
                    text = name,
                    fontFamily = Sans,
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isToday) Dq.Text else Dq.Muted,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownPanel(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val isDemo = state.streakBase >= 12
    val claimedQuests = state.quests.filter { state.claimed.contains(it.id) }
    val catTotals = if (isDemo) {
        SeedData.WEEK_CAT.toMutableMap()
    } else {
        Category.entries.associateWith { 0 }.toMutableMap()
    }
    claimedQuests.forEach { quest ->
        catTotals[quest.category] = (catTotals[quest.category] ?: 0) + quest.xp
    }
    val rows = catTotals.entries.sortedByDescending { it.value }
    val maxVal = (rows.firstOrNull()?.value ?: 0).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Dq.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Where the XP came from",
            fontFamily = Sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Dq.Text
        )

        Spacer(Modifier.height(14.dp))

        rows.forEachIndexed { index, (cat, xp) ->
            val isTop = index == 0
            val fraction = (xp.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = cat.name,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Dq.Text
                    )
                    Text(
                        text = String.format(Locale.US, "%,d XP", xp),
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(PillShape)
                        .background(Dq.Track)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(PillShape)
                            .background(if (isTop) Dq.Accent else Dq.CatOther)
                    )
                }
            }
        }
    }
}

@Composable
fun FiveWeeksHeatmapPanel(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val isDemo = state.streakBase >= 12
    val totalDays = if (isDemo) {
        if (state.securedToday) 35 else 34
    } else {
        1
    }
    val missed = if (isDemo) SeedData.MISSED_INDICES else (0 until 34).toSet()

    val doneDays = if (isDemo) {
        (0 until 35).count { i ->
            if (i == 34) state.securedToday else !missed.contains(i)
        }
    } else {
        if (state.securedToday) 1 else 0
    }

    Column(
        modifier = modifier
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
                text = "Last 5 weeks",
                fontFamily = Sans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Dq.Text
            )
            Text(
                text = if (isDemo) "$doneDays of $totalDays days" else "$doneDays of 1 day",
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Dq.Muted
            )
        }

        Spacer(Modifier.height(14.dp))

        // 7 columns header (M T W T F S S)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { letter ->
                Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        fontFamily = Sans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Dq.Muted
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 5 weeks of 7 days
        for (row in 0 until 5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val isToday = idx == 34
                    val isOn = if (isToday) state.securedToday else !missed.contains(idx)

                    val dotModifier = when {
                        isOn -> Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Dq.Accent)
                        isToday -> Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Dq.Surface)
                            .border(1.5.dp, Dq.Text, CircleShape)
                        else -> Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Dq.Border)
                    }

                    Box(modifier = dotModifier)
                }
            }
        }
    }
}
