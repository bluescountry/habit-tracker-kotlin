package com.dayquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.ui.theme.CardShape
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.InputShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif

@Composable
fun OnboardingScreen(
    onStartFirstDay: (String) -> Unit,
    onRestoreBackup: () -> Unit,
    onStartDemo: (String) -> Unit = {},
    restoreError: String? = null
) {
    var rawName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasBeenFocused by remember { mutableStateOf(false) }

    fun submit() {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Tell us what to call you."
        } else if (trimmed.length > 24) {
            errorMessage = "Name must be 24 characters or less."
        } else {
            errorMessage = null
            onStartFirstDay(trimmed)
        }
    }

    val previewName = rawName.trim().ifEmpty { "…" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Dq.Ground)
            .padding(horizontal = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 120.dp)
        ) {
            // Brand
            Text(
                text = "Dayquest",
                fontFamily = Serif,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                color = Dq.Text
            )

            Spacer(Modifier.height(32.dp))

            // Title
            Text(
                text = "What should we\ncall you?",
                fontFamily = Serif,
                fontSize = 46.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = (46 * 1.02).sp,
                color = Dq.Text
            )

            Spacer(Modifier.height(14.dp))

            // Lede
            Text(
                text = "Your name goes on your daily quests and into your backups. Everything stays on this device.",
                fontFamily = Sans,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                color = Dq.Muted,
                modifier = Modifier.width(280.dp)
            )

            Spacer(Modifier.height(36.dp))

            // Field
            Text(
                text = "Your name",
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Dq.Text
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = rawName,
                onValueChange = {
                    if (it.length <= 24) {
                        rawName = it
                        if (errorMessage != null && it.trim().isNotEmpty()) {
                            errorMessage = null
                        }
                    }
                },
                placeholder = {
                    Text(
                        text = "e.g. Sam",
                        fontFamily = Sans,
                        fontSize = 18.sp,
                        color = Dq.Muted
                    )
                },
                isError = errorMessage != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                shape = InputShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Dq.Surface,
                    unfocusedContainerColor = Dq.Surface,
                    errorContainerColor = Dq.Surface,
                    focusedTextColor = Dq.Text,
                    unfocusedTextColor = Dq.Text,
                    cursorColor = Dq.Text,
                    focusedBorderColor = Dq.Text,
                    unfocusedBorderColor = Dq.Control,
                    errorBorderColor = Dq.Error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasBeenFocused = true
                        } else if (hasBeenFocused) {
                            if (rawName.trim().isEmpty()) {
                                errorMessage = "Tell us what to call you."
                            }
                        }
                    }
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    color = Dq.Error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            Spacer(Modifier.height(32.dp))

            // Live preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Dq.Surface)
                    .padding(20.dp)
            ) {
                Text(
                    text = "YOUR FIRST SCREEN",
                    fontFamily = Sans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Dq.Muted
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Morning, $previewName.",
                    fontFamily = Serif,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    color = Dq.Text
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "6 quests are waiting for you.",
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    color = Dq.Muted
                )
            }
        }

        // Bottom Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (restoreError != null) {
                Text(
                    text = "Couldn't restore from that file. $restoreError",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    color = Dq.Error,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            // Primary action: Start my first day
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(PillShape)
                    .background(Dq.Text)
                    .clickable(role = Role.Button) { submit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start my first day",
                    fontFamily = Sans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Dq.Ground
                )
            }

            Spacer(Modifier.height(8.dp))

            // Secondary action: I have a backup file
            TextButton(
                onClick = onRestoreBackup,
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "I have a backup file",
                    fontFamily = Sans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Dq.Muted
                )
            }

            // Demo option: Explore demo state
            TextButton(
                onClick = {
                    val name = rawName.trim().ifEmpty { "Sam" }
                    onStartDemo(name)
                },
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "Explore demo (Level 14 · 13-day streak)",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted
                )
            }
        }
    }
}
