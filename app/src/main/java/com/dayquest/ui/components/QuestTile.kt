package com.dayquest.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayquest.R
import com.dayquest.domain.Quest
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.SegmentShape
import com.dayquest.ui.theme.TileShape
import com.dayquest.ui.theme.fill
import com.dayquest.ui.theme.glyph
import com.dayquest.ui.theme.ink
import com.dayquest.ui.theme.secondary

@Composable
fun QuestTile(
    quest: Quest,
    done: Boolean,
    stepsDone: List<Boolean>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Int = 136
) {
    val c = quest.category
    val bg = if (done) Dq.Surface else c.fill
    val ink = if (done) Dq.Muted else c.ink
    val secInk = if (done) Dq.Muted else c.secondary

    val borderModifier = if (selected) {
        Modifier.border(2.dp, Dq.Text, TileShape)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .heightIn(min = minHeight.dp)
            .clip(TileShape)
            .background(bg)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(14.dp)
            .semantics {
                contentDescription = "${c.name}, ${quest.shortTitle}, ${if (done) "completed" else "${quest.xp} XP"}"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (done) R.drawable.ic_check else c.glyph),
                contentDescription = null,
                tint = if (done) Dq.Accent else ink,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = if (done) "Done" else "+${quest.xp} XP",
                color = ink,
                fontFamily = Sans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = c.name,
            color = secInk,
            fontFamily = Sans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = quest.shortTitle,
            color = ink,
            fontFamily = Sans,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (done) TextDecoration.LineThrough else null
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            stepsDone.forEach { on ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(SegmentShape)
                        .background(if (on) ink else ink.copy(alpha = 0.22f))
                )
            }
        }
    }
}
