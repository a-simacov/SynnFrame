package com.synngate.synnframe.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal для хранения высоты кнопки навигации
 * По умолчанию используется значение 72f (соответствует 72dp)
 */
val LocalNavigationButtonHeight = compositionLocalOf { 72f }