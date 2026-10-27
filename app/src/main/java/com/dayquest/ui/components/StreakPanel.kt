package com.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.R
import com.dayquest.domain.AppState
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PanelShape
import com.dayquest.ui.theme.Sans

@Composable
fun StreakPanel(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val currentStreak = state.streak
    val streakTitle = "$currentStreak-day streak"
    val streakNote = if (state.securedToday) "Secured for today" else "Today still open"
    val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

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
                text = streakTitle,
                fontFamily = Sans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Dq.Text
            )
            Text(
                text = streakNote,
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Dq.Muted
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val daysBeforeToday = state.streakBase.coerceAtMost(6)
            dayLetters.forEachIndexed { index, letter ->
                val isToday = index == 6
                val isOn = if (isToday) state.securedToday else (index < daysBeforeToday)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotModifier = if (isOn) {
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Dq.Accent)
                    } else if (isToday) {
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Dq.Surface)
                            .border(1.5.dp, Dq.Text, CircleShape)
                    } else {
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Dq.Track)
                    }

                    Box(
                        modifier = dotModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOn) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = Dq.AccentInk,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = letter,
                        fontFamily = Sans,
                        fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isToday) Dq.Text else Dq.Muted
                    )
                }
            }
        }
    }
}
