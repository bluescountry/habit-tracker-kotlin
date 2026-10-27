package com.dayquest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.Toast
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.ToastShape
import kotlinx.coroutines.delay

@Composable
fun DqToast(
    toast: Toast?,
    onDismiss: () -> Unit,
    onUndo: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(toast) {
        if (toast != null) {
            val timeout = if (toast.undoQuestId != null) 8000L else 3200L
            delay(timeout)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (toast != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(ToastShape)
                    .background(Dq.Text)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = toast.message,
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Dq.Ground,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (toast.undoQuestId != null && onUndo != null) {
                        Text(
                            text = "Undo",
                            fontFamily = Sans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Dq.Ground,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable(role = Role.Button) {
                                    onUndo(toast.undoQuestId)
                                }
                        )
                    }
                }
            }
        }
    }
}
