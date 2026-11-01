package com.ivy.navigation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

private val LocalNavigation = compositionLocalOf<Navigation> { error("No LocalNavigation") }

@Composable
fun NavigationRoot(
    navigation: Navigation,
    navGraph: @Composable (screen: Screen?) -> Unit
) {
    CompositionLocalProvider(
        LocalNavigation provides navigation,
    ) {
        val screen = navigation.currentScreen
        if (screen == null || screen.isLegacy) {
            // Legacy screens keep their view models in the activity's store, for its lifetime.
            navGraph(screen)
        } else {
            ScreenScope(screen) { navGraph(screen) }
        }
    }
}

/**
 * A ViewModelStore of the screen's own, cleared when the screen is left: [screenScopedViewModel]
 * reads it through [LocalViewModelStoreOwner]. The activity's store, where RootActivity's and the
 * legacy screens' view models live, is never touched.
 */
@Composable
private fun ScreenScope(screen: Screen, content: @Composable () -> Unit) {
    val activity = requireNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner provided" }
    val owner = remember(screen) { ScreenViewModelStoreOwner(activity) }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

private class ScreenViewModelStoreOwner(
    private val activity: ViewModelStoreOwner,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val viewModelStore = ViewModelStore()

    // The activity's factory (Hilt's) and saved state build the screen's view models.
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = (activity as HasDefaultViewModelProviderFactory).defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = (activity as HasDefaultViewModelProviderFactory).defaultViewModelCreationExtras
}

@Composable
@ReadOnlyComposable
fun navigation(): Navigation {
    return LocalNavigation.current
}

/**
 * Provides a [ViewModel] instance scoped to the screen's life: when the user leaves the screen,
 * its view models are destroyed. Legacy screens have no scope of their own, so there it is the
 * activity's view model.
 */
@Composable
inline fun <reified T : ViewModel> screenScopedViewModel(
    factory: ViewModelProvider.Factory? = null
): T {
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    requireNotNull(viewModelStoreOwner) { "No ViewModelStoreOwner provided" }
    val viewModelProvider = remember(viewModelStoreOwner, factory) {
        factory?.let { ViewModelProvider(viewModelStoreOwner, it) } ?: ViewModelProvider(viewModelStoreOwner)
    }
    return viewModelProvider[T::class.java]
}

/**
 * Goes back as the system back button does: open dialogs and the current screen's BackHandlers
 * get it first, then the back stack. For in-app back buttons.
 */
@Composable
fun rememberOnBack(): () -> Unit {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    return remember(dispatcher) { { dispatcher?.onBackPressed() } }
}
