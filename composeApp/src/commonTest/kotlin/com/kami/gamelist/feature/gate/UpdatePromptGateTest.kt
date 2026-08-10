package com.kami.gamelist.feature.gate

import com.kami.gamelist.core.config.AppConfigState
import com.kami.gamelist.core.config.MaintenanceInfo
import com.kami.gamelist.core.config.UpdateInfo
import com.kami.gamelist.core.config.UpdateStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [shouldScheduleUpdatePrompt] is the fix for a race between `App.kt`'s two
 * startup `LaunchedEffect`s: the onboarding decision (local DB, but gated
 * behind an `observeLists().first()` plus up to three sequential
 * `createList()` writes on a fresh install) versus the app-config fetch (one
 * HTTP round trip). Either can finish first. These tests replay both
 * orderings directly against the decision function — no Compose runtime
 * needed, since the function takes both racing pieces of state as plain
 * arguments instead of reading them off `App.kt`'s snapshot state.
 */
class UpdatePromptGateTest {

    private fun recommendedConfig(latestVersion: String = "2.0.0") = AppConfigState(
        update = UpdateInfo(
            status = UpdateStatus.RECOMMENDED,
            latestVersion = latestVersion,
            storeUrl = "https://example.com",
            changelog = "Bug fixes",
        ),
        maintenance = MaintenanceInfo(active = false, message = null),
        flags = emptyMap(),
        issuer = "",
        clientId = "",
    )

    @Test
    fun `config-wins ordering on a fresh install does not schedule the prompt while onboarding is still undecided`() {
        // Config resolves first: onboarding hasn't run isOnboardingSeen() yet,
        // so the caller must not have flipped onboardingDecided yet either.
        val scheduled = shouldScheduleUpdatePrompt(
            onboardingDecided = false,
            onboardingShown = false,
            appConfig = recommendedConfig(),
            isVersionDismissed = { false },
        )

        assertFalse(scheduled)
    }

    @Test
    fun `config-wins ordering does not schedule the prompt once onboarding is decided to show`() {
        // Onboarding effect catches up next: fresh install, so it sets
        // showOnboarding = true before marking onboardingDecided = true.
        val scheduled = shouldScheduleUpdatePrompt(
            onboardingDecided = true,
            onboardingShown = true,
            appConfig = recommendedConfig(),
            isVersionDismissed = { false },
        )

        assertFalse(scheduled)
    }

    @Test
    fun `onboarding-wins ordering schedules the prompt once config also resolves`() {
        // Onboarding decides first (existing user, onboarding already seen),
        // config resolves after.
        val scheduled = shouldScheduleUpdatePrompt(
            onboardingDecided = true,
            onboardingShown = false,
            appConfig = recommendedConfig(),
            isVersionDismissed = { false },
        )

        assertTrue(scheduled)
    }

    @Test
    fun `onboarding-wins ordering does not schedule before config has resolved`() {
        val scheduled = shouldScheduleUpdatePrompt(
            onboardingDecided = true,
            onboardingShown = false,
            appConfig = null,
            isVersionDismissed = { false },
        )

        assertFalse(scheduled)
    }

    @Test
    fun `does not schedule when the latest version was already dismissed`() {
        val scheduled = shouldScheduleUpdatePrompt(
            onboardingDecided = true,
            onboardingShown = false,
            appConfig = recommendedConfig(latestVersion = "2.0.0"),
            isVersionDismissed = { version -> version == "2.0.0" },
        )

        assertFalse(scheduled)
    }

    @Test
    fun `does not schedule when status is not recommended`() {
        val scheduled = shouldScheduleUpdatePrompt(
            onboardingDecided = true,
            onboardingShown = false,
            appConfig = recommendedConfig().copy(
                update = recommendedConfig().update.copy(status = UpdateStatus.NONE),
            ),
            isVersionDismissed = { false },
        )

        assertFalse(scheduled)
    }
}
