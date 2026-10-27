package com.dayquest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PanelShape = RoundedCornerShape(22.dp)
val CardShape = RoundedCornerShape(22.dp)
val TileShape = RoundedCornerShape(22.dp)
val RowShape = RoundedCornerShape(16.dp)
val InputShape = RoundedCornerShape(14.dp)
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val ToastShape = RoundedCornerShape(14.dp)
val PillShape = RoundedCornerShape(percent = 50)
val SegmentShape = RoundedCornerShape(2.dp)
val BarShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

val DayquestShapes = Shapes(
    small = RowShape,
    medium = PanelShape,
    large = SheetShape
)
