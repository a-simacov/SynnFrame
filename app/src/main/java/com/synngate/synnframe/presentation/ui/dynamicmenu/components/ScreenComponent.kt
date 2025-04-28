package com.synngate.synnframe.presentation.ui.dynamicmenu.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ScreenComponent {

    @Composable
    fun Render(modifier: Modifier)

    fun usesWeight(): Boolean = false

    fun getWeight(): Float = 1f
}