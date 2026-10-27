package com.dayquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.R
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.RowShape
import com.dayquest.ui.theme.Sans

@Composable
fun StepRow(
    text: String,
    done: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkOutlineColor = Color(0xFF8C8C88)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RowShape)
            .background(Dq.Surface)
            .border(1.dp, Dq.Border, RowShape)
            .toggleable(
                value = done,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val checkModifier = if (done) {
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Dq.Accent)
        } else {
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(1.5.dp, checkOutlineColor, CircleShape)
        }

        Box(
            modifier = checkModifier,
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Dq.AccentInk,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = text,
            fontFamily = Sans,
            fontSize = 15.sp,
            fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
            color = if (done) Dq.Muted else Dq.Text,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
    }
}
