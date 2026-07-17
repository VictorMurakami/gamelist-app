package com.kami.gamelist.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kami.gamelist.core.ui.model.PlatformPreference
import com.kami.gamelist.core.ui.theme.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSheet(
    onDismiss: () -> Unit,
) {
    val colors = GameTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStep by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        dragHandle = null
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it } + fadeOut())
                    .using(SizeTransform(clip = false))
            },
            label = "onboarding_step"
        ) { step ->
            when (step) {
                0 -> WelcomeStep(onContinue = { currentStep = 1 })
                else -> PreferencesStep(onDone = onDismiss)
            }
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    val colors = GameTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GAMELIST",
            style = GameTheme.typography.headlineLarge,
            color = colors.accent
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your free-to-play game catalog",
            style = GameTheme.typography.bodyMedium,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.height(28.dp))

        FeatureRow(Icons.Outlined.SportsEsports, "Browse", "Explore hundreds of free-to-play games")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(Icons.Outlined.FilterList, "Filter", "Find games by genre and platform")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(Icons.Outlined.FavoriteBorder, "Favorite", "Tap the heart on any card to save it")
        Spacer(modifier = Modifier.height(16.dp))
        FeatureRow(Icons.Outlined.Search, "Search", "Quickly find any game by name")

        Spacer(modifier = Modifier.height(28.dp))

        StepIndicator(currentStep = 0, totalSteps = 2)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.backgroundDark
            )
        ) {
            Text(
                text = "Continue",
                style = GameTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesStep(onDone: () -> Unit) {
    val colors = GameTheme.colors
    val preferences = LocalUserPreferences.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Customize Your Feed",
            style = GameTheme.typography.headlineSmall,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Pick genres you enjoy",
            style = GameTheme.typography.bodyMedium,
            color = colors.textMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserPreferencesState.AVAILABLE_GENRES.forEach { genre ->
                GenreChip(
                    label = genre,
                    isSelected = genre in preferences.selectedGenres,
                    onClick = { preferences.toggleGenre(genre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Preferred Platform",
            style = GameTheme.typography.titleSmall,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlatformPreference.entries.forEach { platform ->
                val isSelected = platform == preferences.platformPreference

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surfaceBase,
                    border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { preferences.updatePlatform(platform) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = platform.label,
                            style = GameTheme.typography.labelSmall,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StepIndicator(currentStep = 1, totalSteps = 2)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.backgroundDark
            )
        ) {
            Text(
                text = "Get Started",
                style = GameTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "You can change these in Settings",
            style = GameTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GenreChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = GameTheme.colors

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surfaceBase,
        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.borderSubtle),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = GameTheme.typography.labelMedium,
            color = if (isSelected) colors.accent else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    val colors = GameTheme.colors

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index == currentStep) colors.accent else colors.borderSubtle,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    val colors = GameTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = title, style = GameTheme.typography.titleSmall, color = colors.textPrimary)
            Text(text = description, style = GameTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
