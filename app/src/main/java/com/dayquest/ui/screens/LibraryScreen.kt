package com.dayquest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dayquest.data.SeedData
import com.dayquest.domain.AppState
import com.dayquest.domain.Category
import com.dayquest.domain.LibraryItem
import com.dayquest.ui.components.DqChip
import com.dayquest.ui.theme.Dq
import com.dayquest.ui.theme.PillShape
import com.dayquest.ui.theme.RowShape
import com.dayquest.ui.theme.Sans
import com.dayquest.ui.theme.Serif
import com.dayquest.ui.theme.glyph
import com.dayquest.ui.theme.libraryTint

@Composable
fun LibraryScreen(
    state: AppState,
    onCategorySelect: (Category?) -> Unit = {},
    onAddHabit: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val filteredItems = if (selectedCategory == null) {
        SeedData.LIBRARY
    } else {
        SeedData.LIBRARY.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Dq.Ground),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Library",
                    fontFamily = Serif,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Habits worth starting. Add one and it lands in today's quests.",
                    fontFamily = Sans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Dq.Muted
                )
            }
        }

        // Horizontal Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    DqChip(
                        text = "All",
                        selected = selectedCategory == null,
                        onClick = {
                            selectedCategory = null
                            onCategorySelect(null)
                        }
                    )
                }
                items(Category.entries) { cat ->
                    DqChip(
                        text = cat.name,
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            onCategorySelect(cat)
                        }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
        }

        // Habits List
        items(filteredItems, key = { it.id }) { item ->
            val isAdded = state.visibleQuests.any { it.libraryId == item.id || it.id == item.id }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 68.dp)
                    .clip(RowShape)
                    .background(Dq.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(item.category.glyph),
                    contentDescription = null,
                    tint = item.category.libraryTint,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontFamily = Sans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Dq.Text
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${item.category.name} · ${item.meta} · +${item.xp} XP",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Dq.Muted
                    )
                }

                Spacer(Modifier.width(8.dp))

                val buttonBg = if (isAdded) Color.Transparent else Color.Transparent
                val borderModifier = if (isAdded) Modifier else Modifier.border(1.dp, Dq.Control, PillShape)
                val textColor = if (isAdded) Dq.Muted else Dq.Text

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .widthIn(min = 78.dp)
                        .clip(PillShape)
                        .background(buttonBg)
                        .then(borderModifier)
                        .clickable(
                            enabled = !isAdded,
                            role = Role.Button,
                            onClick = { onAddHabit(item) }
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isAdded) "Added" else "Add",
                        fontFamily = Sans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}
