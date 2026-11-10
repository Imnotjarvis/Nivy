package com.nivy.onboarding.steps

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.nivy.base.model.Theme
import com.nivy.onboarding.OnboardingState
import com.nivy.ui.testing.PaparazziScreenshotTest
import com.nivy.ui.testing.PaparazziTheme
import com.nivy.wallet.NivyPreview
import org.junit.Test
import org.junit.runner.RunWith

/** Onboarding's first step after the splash: in light and dark, in both color modes. */
@RunWith(TestParameterInjector::class)
class OnboardingWelcomePaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
    @TestParameter
    private val colors: Colors,
) : PaparazziScreenshotTest() {
    enum class Colors { Default, Minimal }

    @Test
    fun `snapshot the welcome step`() {
        val dark = theme == PaparazziTheme.Dark
        snapshot(theme, minimal = colors == Colors.Minimal) {
            NivyPreview(theme = if (dark) Theme.DARK else Theme.LIGHT) {
                OnboardingSplashWelcome(onboardingState = OnboardingState.WELCOME, onStart = {})
            }
        }
    }
}
