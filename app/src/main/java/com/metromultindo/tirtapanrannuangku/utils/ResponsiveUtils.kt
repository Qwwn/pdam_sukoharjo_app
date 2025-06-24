package com.metromultindo.tirtapanrannuangku.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ScreenClass {
    COMPACT,   // Phone portrait
    MEDIUM,    // Phone landscape, tablet portrait
    EXPANDED   // Tablet landscape, desktop
}

/**
 * BoxWithConstraints that determines screen size class and provides content with this information
 */
@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(screenClass: ScreenClass) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val screenClass = when {
            maxWidth < 600.dp -> ScreenClass.COMPACT
            maxWidth < 840.dp -> ScreenClass.MEDIUM
            else -> ScreenClass.EXPANDED
        }

        content(screenClass)
    }
}

/**
 * Returns appropriate maximum width for UI container based on screen size
 */
fun BoxWithConstraintsScope.getContentMaxWidth(screenClass: ScreenClass): Dp {
    return when (screenClass) {
        ScreenClass.COMPACT -> maxWidth
        ScreenClass.MEDIUM -> 600.dp
        ScreenClass.EXPANDED -> 840.dp
    }
}

/**
 * Returns appropriate number of grid columns based on screen size
 */
fun getGridColumns(screenClass: ScreenClass): Int {
    return when (screenClass) {
        ScreenClass.COMPACT -> 2
        ScreenClass.MEDIUM -> 3
        ScreenClass.EXPANDED -> 4
    }
}

/**
 * Returns appropriate padding based on screen size
 */
fun getPadding(screenClass: ScreenClass): Dp {
    return when (screenClass) {
        ScreenClass.COMPACT -> 16.dp
        ScreenClass.MEDIUM -> 24.dp
        ScreenClass.EXPANDED -> 32.dp
    }
}