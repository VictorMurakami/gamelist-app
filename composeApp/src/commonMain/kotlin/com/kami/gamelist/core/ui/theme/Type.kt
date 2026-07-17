package com.kami.gamelist.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import gamelist.composeapp.generated.resources.Res
import gamelist.composeapp.generated.resources.jetbrainsmono_variable
import gamelist.composeapp.generated.resources.orbitron_variable
import org.jetbrains.compose.resources.Font

val OrbitronFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.orbitron_variable, FontWeight.Normal),
        Font(Res.font.orbitron_variable, FontWeight.Medium),
        Font(Res.font.orbitron_variable, FontWeight.SemiBold),
        Font(Res.font.orbitron_variable, FontWeight.Bold),
        Font(Res.font.orbitron_variable, FontWeight.ExtraBold),
    )

val JetBrainsMonoFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.jetbrainsmono_variable, FontWeight.Normal),
        Font(Res.font.jetbrainsmono_variable, FontWeight.Medium),
        Font(Res.font.jetbrainsmono_variable, FontWeight.SemiBold),
        Font(Res.font.jetbrainsmono_variable, FontWeight.Bold),
    )

@Composable
fun gameListTypography(): Typography {
    val orbitron = OrbitronFamily
    val jetbrainsMono = JetBrainsMonoFamily

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = orbitron,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = orbitron,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 1.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = orbitron,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = orbitron,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        titleSmall = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = jetbrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp
        )
    )
}
