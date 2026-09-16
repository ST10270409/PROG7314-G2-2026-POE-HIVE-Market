package com.hivemarket.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HiveBlue = Color(0xFF185FA5)
val HiveBlueDark = Color(0xFF0C447C)
val HiveBlueLight = Color(0xFFE6F1FB)
val HiveAmber = Color(0xFFEF9F27)
val HiveGreen = Color(0xFF3B6D11)

private val LightColors = lightColorScheme(
    primary = HiveBlue,
    onPrimary = Color.White,
    primaryContainer = HiveBlueLight,
    onPrimaryContainer = HiveBlueDark,
    secondary = HiveAmber
)

@Composable
fun HiveMarketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
