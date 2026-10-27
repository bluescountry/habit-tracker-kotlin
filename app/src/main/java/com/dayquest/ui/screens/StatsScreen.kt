package com.dayquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.data.SeedData
import com.dayquest.domain.AppState
import com.dayquest.ui.components.CategoryBreakdownPanel
import com.dayquest.ui.components.FiveWeeksHeatmapPanel
import com.dayquest.ui.components.KpiStrip
import com.dayquest.ui.components.XpChartPanel
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import java.util.Locale

@Composable
fun StatsScreen(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val isDemo = state.streakBase >= 12
    val claimedQuests = state.quests.filter { state.claimed.contains(it.id) }
    val todayXp = claimedQuests.sumOf { it.xp }
    val dow = java.time.LocalDate.now().dayOfWeek.value
    val todayIdx = (dow - 1).coerceIn(0, 6)
    val pastWeekXp = if (isDemo) {
        (0 until todayIdx).sumOf { SeedData.WEEK_XP.getOrElse(it) { 0 } }
    } else {
        0
    }
    val weekTotal = pastWeekXp + todayXp

    val todayDate = java.time.LocalDate.now()
    val mondayDate = todayDate.minusDays(todayIdx.toLong())
    val monthName = mondayDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val subtitleText = if (isDemo) {
        String.format(Locale.US, "Week of %d %s · %,d XP so far", mondayDate.dayOfMonth, monthName, weekTotal)
    } else {
        String.format(Locale.US, "This week · %,d XP so far", weekTotal)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Dq.Ground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "Stats",
                    fontFamily = Serif,
                    fontSize = 34.sp,
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
        }

        item {
            KpiStrip(state = state)
        }

        item {
            XpChartPanel(state = state)
        }

        item {
            CategoryBreakdownPanel(state = state)
        }

        item {
            FiveWeeksHeatmapPanel(state = state)
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}
