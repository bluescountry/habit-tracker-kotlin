package com.dayquest.ui.theme

import androidx.compose.ui.graphics.Color
import com.dayquest.R
import com.dayquest.domain.Category

object Dq {
    val Ground = Color(0xFF111111)
    val Surface = Color(0xFF1B1B1B)
    val Raised = Color(0xFF242424)
    val Border = Color(0xFF2E2E2E)
    val Control = Color(0xFF444444)
    val Track = Color(0xFF3A3A37)
    val Text = Color(0xFFF5F5F2)
    val Muted = Color(0xFFA9A9A4)
    val Accent = Color(0xFFBAED3F)
    val AccentInk = Color(0xFF141A00)
    val Error = Color(0xFFFF9D8F)
    val BarPast = Color(0xFF6A6A65)
    val CatOther = Color(0xFFD9D9D4)
}

val Category.fill: Color
    get() = when (this) {
        Category.Body -> Color(0xFFFF7E76)
        Category.Read -> Color(0xFFFAC131)
        Category.Listen -> Color(0xFF0ABFFF)
        Category.Nourish -> Color(0xFF66DA85)
        Category.Mind -> Color(0xFFAD8DFD)
        Category.Watch -> Color(0xFFFB83BF)
    }

val Category.ink: Color
    get() = when (this) {
        Category.Body -> Color(0xFF240706)
        Category.Read -> Color(0xFF1B0F00)
        Category.Listen -> Color(0xFF001424)
        Category.Nourish -> Color(0xFF001705)
        Category.Mind -> Color(0xFF140C25)
        Category.Watch -> Color(0xFF210715)
    }

val Category.secondary: Color
    get() = when (this) {
        Category.Body -> Color(0xFF3C1715)
        Category.Read -> Color(0xFF312100)
        Category.Listen -> Color(0xFF00283D)
        Category.Nourish -> Color(0xFF092C13)
        Category.Mind -> Color(0xFF261D3E)
        Category.Watch -> Color(0xFF391628)
    }

val Category.pressed: Color
    get() = when (this) {
        Category.Body -> Color(0xFFFF8E85)
        Category.Read -> Color(0xFFFFD147)
        Category.Listen -> Color(0xFF32D0FF)
        Category.Nourish -> Color(0xFF77EB94)
        Category.Mind -> Color(0xFFBC9DFF)
        Category.Watch -> Color(0xFFFF93CF)
    }

val Category.libraryTint: Color
    get() = when (this) {
        Category.Body -> Color(0xFFFF8E86)
        Category.Read -> Color(0xFFE3AE28)
        Category.Listen -> Color(0xFF38C5FF)
        Category.Nourish -> Color(0xFF67D283)
        Category.Mind -> Color(0xFFBFA2FF)
        Category.Watch -> Color(0xFFFE8DC5)
    }

val Category.glyph: Int
    get() = when (this) {
        Category.Body -> R.drawable.ic_body
        Category.Read -> R.drawable.ic_read
        Category.Listen -> R.drawable.ic_listen
        Category.Nourish -> R.drawable.ic_nourish
        Category.Mind -> R.drawable.ic_mind
        Category.Watch -> R.drawable.ic_watch
    }
