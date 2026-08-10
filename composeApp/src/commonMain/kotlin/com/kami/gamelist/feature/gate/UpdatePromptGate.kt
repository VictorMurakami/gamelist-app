package com.kami.gamelist.feature.gate

import com.kami.gamelist.core.config.AppConfigState
import com.kami.gamelist.core.config.UpdateStatus

/**
 * Decides whether `App.kt` should schedule the [UpdateAvailableSheet] for the
 * current launch.
 *
 * This exists because "has onboarding been shown this launch" is not
 * something `App.kt` can safely read off a single snapshot of `showOnboarding`
 * — it is decided by one `LaunchedEffect` and raced against another that
 * loads [AppConfigState] over the network. On a fresh install the onboarding
 * decision needs an `observeLists().first()` plus three sequential
 * `createList()` writes before it ever reaches `isOnboardingSeen()`, while
 * the config effect only needs one HTTP round trip; against a fast backend
 * the config effect can resolve first, and reading `showOnboarding` at that
 * instant would see `false` even though onboarding is about to open.
 *
 * [onboardingDecided] is the fix: it must flip to `true` exactly once per
 * launch, and only *after* the onboarding effect has already set
 * [onboardingShown] to its final value for this launch (i.e. after
 * `cacheManager.isOnboardingSeen()` has been read). Callers must re-run this
 * function whenever [onboardingDecided] or [appConfig] changes — but *not*
 * merely because [onboardingShown] changed on its own (e.g. the user
 * dismissing onboarding). Reacting to that would flip this decision from
 * "wrong in one direction" (prompt shown together with onboarding) to
 * "wrong in the other" (prompt appears the instant onboarding closes, in the
 * same session, instead of waiting for the next launch as the spec requires).
 *
 * Whichever of the two `LaunchedEffect`s finishes first, the caller keeps
 * re-evaluating this function until both [onboardingDecided] is `true` and
 * [appConfig] is non-null, at which point [onboardingShown] is guaranteed to
 * already hold its correct, final value for the launch.
 */
fun shouldScheduleUpdatePrompt(
    onboardingDecided: Boolean,
    onboardingShown: Boolean,
    appConfig: AppConfigState?,
    isVersionDismissed: (String) -> Boolean,
): Boolean {
    if (!onboardingDecided || onboardingShown || appConfig == null) return false
    val latestVersion = appConfig.update.latestVersion ?: return false
    return appConfig.update.status == UpdateStatus.RECOMMENDED && !isVersionDismissed(latestVersion)
}
