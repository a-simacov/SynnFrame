package com.synngate.synnframe.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Определение форм с учетом требований интерфейса
val Shapes = Shapes(
    // Для небольших элементов (чипсы, небольшие кнопки)
    small = RoundedCornerShape(4.dp),

    // Для средних элементов (карточки, списки)
    medium = RoundedCornerShape(8.dp),

    // Для больших элементов (диалоги, модальные окна)
    large = RoundedCornerShape(12.dp),

    // Для внешних контейнеров и экранных элементов
    extraLarge = RoundedCornerShape(16.dp)
)