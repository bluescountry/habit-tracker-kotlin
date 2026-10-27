package com.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.Sans

@Composable
fun DqChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Button
) {
    val bg = if (selected) Dq.Text else Color.Transparent
    val textColor = if (selected) Dq.Ground else Dq.Text
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
    val borderModifier = if (selected) Modifier else Modifier.border(1.dp, Dq.Control, PillShape)

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(PillShape)
            .background(bg)
            .then(borderModifier)
            .clickable(role = role, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = Sans,
            fontSize = 14.sp,
            fontWeight = fontWeight,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}
