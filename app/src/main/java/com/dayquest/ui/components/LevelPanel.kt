package com.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.domain.AppState
import com.dayquest.domain.RankStyle
import com.dayquest.domain.rankInfo
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PanelShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import java.util.Locale

@Composable
fun LevelPanel(
    state: AppState,
    modifier: Modifier = Modifier
) {
    val rk = rankInfo(state)
    val isLevel = state.rankStyle == RankStyle.Level

    val title = if (isLevel || rk == null) "Level ${state.level}" else rk.label
    val subtitle = if (isLevel || rk == null) "Experience" else "${state.rankStyle.label} rank"
    val figure = if (isLevel || rk == null) {
        String.format(Locale.US, "%,d / %,d XP", state.xp, state.cap)
    } else {
        "${rk.figure} ${rk.unit}".trim()
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
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(Dq.Surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    fontFamily = Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )
                Text(
                    text = subtitle,
                    fontFamily = Sans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Dq.Muted
                )
            }

            Text(
                text = figure,
                fontFamily = Sans,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Dq.Text
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
