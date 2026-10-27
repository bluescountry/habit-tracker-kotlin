package com.dayquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.domain.Category
import com.dayquest.ui.components.DqChip
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.InputShape
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.RowShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import com.dayquest.ui.theme.SheetShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewQuestSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCreateQuest: (String, Category, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.Body) }
    var xp by remember { mutableIntStateOf(60) }
    var isError by remember { mutableStateOf(false) }
    var hasBeenFocused by remember { mutableStateOf(false) }

    fun submit() {
        if (name.trim().isEmpty()) {
            isError = true
        } else {
            isError = false
            onCreateQuest(name.trim().take(40), category, xp)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = Dq.Surface,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Dq.Control)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "New quest",
                fontFamily = Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = Dq.Text
            )

            Spacer(Modifier.height(16.dp))

            // Quest Name
            Text(
                text = "Quest name",
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Dq.Muted
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    if (it.length <= 40) {
                        name = it
                        if (it.isNotEmpty()) isError = false
                    }
                },
                singleLine = true,
                placeholder = {
                    Text("e.g. Read 20 pages", color = Dq.Muted, fontSize = 14.sp)
                },
                isError = isError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = InputShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Dq.Text,
                    unfocusedBorderColor = Dq.Control,
                    errorBorderColor = Dq.Error,
                    focusedTextColor = Dq.Text,
                    unfocusedTextColor = Dq.Text,
                    cursorColor = Dq.Text,
                    focusedContainerColor = Dq.Surface,
                    unfocusedContainerColor = Dq.Surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            hasBeenFocused = true
                        } else if (hasBeenFocused && name.trim().isEmpty()) {
                            isError = true
                        }
                    }
            )

            if (isError) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Give your quest a name.",
                    fontFamily = Sans,
                    fontSize = 12.sp,
                    color = Dq.Error,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                    }
                )
            }

            Spacer(Modifier.height(18.dp))

            // Category Chips
            Text(
                text = "Category",
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Dq.Muted
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Category.entries.take(3).forEach { cat ->
                    DqChip(
                        text = cat.name,
                        selected = category == cat,
                        onClick = { category = cat },
                        role = Role.RadioButton,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Category.entries.drop(3).forEach { cat ->
                    DqChip(
                        text = cat.name,
                        selected = category == cat,
                        onClick = { category = cat },
                        role = Role.RadioButton,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Reward Radio Group (Easy 30 XP, Medium 60 XP, Hard 90 XP)
            Text(
                text = "Reward",
                fontFamily = Sans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Dq.Muted
            )
            Spacer(Modifier.height(8.dp))

            val rewardOptions = listOf("Easy" to 30, "Medium" to 60, "Hard" to 90)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rewardOptions.forEach { (label, rewardXp) ->
                    val isSelected = xp == rewardXp
                    val bg = if (isSelected) Dq.Text else Color.Transparent
                    val labelColor = if (isSelected) Dq.Ground else Dq.Text
                    val xpColor = if (isSelected) Dq.Ground else Dq.Muted
                    val borderMod = if (isSelected) Modifier else Modifier.border(1.dp, Dq.Control, RowShape)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RowShape)
                            .background(bg)
                            .then(borderMod)
                            .clickable(role = Role.RadioButton) { xp = rewardXp },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = Sans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor
                        )
                        Text(
                            text = "$rewardXp XP",
                            fontFamily = Sans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = xpColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Create Quest Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(PillShape)
                    .background(Dq.Text)
                    .clickable(role = Role.Button, onClick = { submit() }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create quest",
                    fontFamily = Sans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Dq.Ground
                )
            }

            Spacer(Modifier.height(8.dp))

            // Cancel Button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontFamily = Sans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Dq.Muted
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
