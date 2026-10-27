package com.dayquest.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.domain.AppState
import com.dayquest.domain.Quest
import com.dayquest.domain.RankStyle
import com.dayquest.domain.rankInfo
import com.dayquest.ui.components.XpBar
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.RowShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import com.dayquest.ui.theme.fill
import com.dayquest.ui.theme.glyph
import com.dayquest.ui.theme.ink
import java.util.Locale

@Composable
fun CoverScreen(
    state: AppState,
    onQuestClick: (String) -> Unit,
    onBackToToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rk = rankInfo(state)
    val isLevel = state.rankStyle == RankStyle.Level
    val tierWord = if (isLevel || rk == null) "Level" else rk.tier
    val numeral = if (isLevel || rk == null) "${state.level}" else rk.numeral
    val isLong = numeral.length >= 3
    val numFontSize = if (isLong) 168.sp else 232.sp
    val numLineHeight = if (isLong) 150.sp else 190.sp

    val progress = if (isLevel || rk == null) {
        state.xp.toFloat() / state.cap.toFloat()
    } else {
        rk.progress
    }

    val capL = if (isLevel || rk == null) {
        String.format(Locale.US, "%,d XP", state.xp)
    } else {
        "${rk.figure} ${rk.unit}".trim()
    }

    val capR = if (isLevel || rk == null) {
        String.format(Locale.US, "%,d XP to Level %d", state.cap - state.xp, state.level + 1)
    } else {
        rk.foot
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Dq.Ground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top header with Back to Today button
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cover",
                    fontFamily = Serif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(Dq.Surface)
                        .clickable(role = Role.Button, onClick = onBackToToday)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Today",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Text
                    )
                }
            }
        }

        // Hero Numeral + Vertical Word
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Rotated word beside numeral
                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .width(28.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = tierWord,
                        fontFamily = Sans,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Dq.Muted,
                        maxLines = 1,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = -90f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = numeral,
                    fontFamily = Serif,
                    fontSize = numFontSize,
                    lineHeight = numLineHeight,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.04).sp,
                    color = Dq.Text,
                    modifier = Modifier.padding(bottom = 0.dp)
                )
            }
        }

        // XP Bar & captions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                XpBar(progress = progress)

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = capL,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                    Text(
                        text = capR,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                }
            }
        }

        // Six full-width colored quest rows
        items(state.quests, key = { it.id }) { quest ->
            CoverQuestRow(
                quest = quest,
                onClick = { onQuestClick(quest.id) }
            )
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun CoverQuestRow(
    quest: Quest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = quest.category

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RowShape)
            .background(category.fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(category.glyph),
            contentDescription = null,
            tint = category.ink,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(12.dp))

        val titleText = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(category.name)
            }
            append(" · ")
            append(quest.shortTitle)
        }

        Text(
            text = titleText,
            fontFamily = Sans,
            fontSize = 15.sp,
            color = category.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "+${quest.xp} XP",
            fontFamily = Sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = category.ink
        )
    }
}
