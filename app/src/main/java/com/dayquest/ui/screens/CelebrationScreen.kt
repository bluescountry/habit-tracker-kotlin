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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayquest.Celebration
import com.dayquest.domain.AppState
import com.dayquest.domain.RankStyle
import com.dayquest.domain.RewardKind
import com.dayquest.domain.rating
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif

@Composable
fun CelebrationDialog(
    celebration: Celebration,
    appState: AppState,
    onContinue: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val darkGreenSub = Color(0xFF2F3D00)
    val buttonDarkFill = Color(0xFF141A00)

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Dq.Accent)
                .padding(horizontal = 22.dp)
        ) {
            when (celebration) {
                is Celebration.RankUp -> {
                    val kicker = if (appState.rankStyle == RankStyle.Level) "Level up" else "Rank up"
                    val numeralFontSize = if (celebration.after.numeral.length >= 3) 176.sp else 250.sp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = kicker,
                            fontFamily = Sans,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.AccentInk
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = celebration.after.tier,
                            fontFamily = Serif,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 60.sp,
                            color = Dq.AccentInk
                        )

                        Text(
                            text = celebration.after.numeral,
                            fontFamily = Serif,
                            fontSize = numeralFontSize,
                            fontWeight = FontWeight.Normal,
                            lineHeight = numeralFontSize,
                            color = Dq.AccentInk
                        )

                        Text(
                            text = "From ${celebration.before.label} to ${celebration.after.label}.",
                            fontFamily = Sans,
                            fontSize = 16.sp,
                            color = darkGreenSub
                        )

                        Spacer(Modifier.weight(1f))

                        // Two facts split by a 1.5px accentInk rule
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = rating(appState).toString(),
                                    fontFamily = Serif,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 44.sp,
                                    color = Dq.AccentInk
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (celebration.gain > 0) "Rating, +${celebration.gain} from this quest" else "Rating",
                                    fontFamily = Sans,
                                    fontSize = 13.sp,
                                    color = darkGreenSub,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(48.dp)
                                    .background(Dq.AccentInk)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = appState.claimed.size.toString(),
                                    fontFamily = Serif,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 44.sp,
                                    color = Dq.AccentInk
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (appState.claimed.size == 1) "quest done today" else "quests done today",
                                    fontFamily = Sans,
                                    fontSize = 13.sp,
                                    color = darkGreenSub,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Action buttons
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(PillShape)
                                .background(buttonDarkFill)
                                .clickable(role = Role.Button) { onContinue() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue",
                                fontFamily = Sans,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Dq.Accent
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = "See my ladder",
                                fontFamily = Sans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Dq.AccentInk
                            )
                        }
                    }
                }

                is Celebration.Reward -> {
                    val kicker = if (celebration.reward.kind == RewardKind.Medium) "Medium reward" else "Big reward"
                    val numeralFontSize = if (celebration.isWord) {
                        104.sp
                    } else if (celebration.numeral.length >= 3) {
                        176.sp
                    } else {
                        250.sp
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = kicker,
                            fontFamily = Sans,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Dq.AccentInk
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = celebration.numeral,
                            fontFamily = Serif,
                            fontSize = numeralFontSize,
                            fontWeight = FontWeight.Normal,
                            lineHeight = numeralFontSize,
                            color = Dq.AccentInk
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "${celebration.reward.from}. You earned this one.",
                            fontFamily = Sans,
                            fontSize = 16.sp,
                            color = darkGreenSub,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.weight(1f))

                        // The reward text in serif 46, wraps up to three lines, anchored above buttons
                        Text(
                            text = celebration.reward.text,
                            fontFamily = Serif,
                            fontSize = 46.sp,
                            lineHeight = 50.sp,
                            fontWeight = FontWeight.Normal,
                            color = Dq.AccentInk,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(Modifier.height(36.dp))

                        // Action buttons
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(PillShape)
                                .background(buttonDarkFill)
                                .clickable(role = Role.Button) { onContinue() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue",
                                fontFamily = Sans,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Dq.Accent
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = onOpenProfile,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = "See my rewards",
                                fontFamily = Sans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Dq.AccentInk
                            )
                        }
                    }
                }
            }
        }
    }
}
