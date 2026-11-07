package com.nivy.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Stack
import javax.inject.Inject
import javax.inject.Singleton

// The app's screen back stack. Screens and dialogs that handle the back button themselves do it
// with Compose's BackHandler; what none of them consumes reaches RootActivity, which calls back().
@Stable
@Singleton
class Navigation @Inject constructor() {
  var currentScreen: Screen? by mutableStateOf(null)
    private set

  private val backStack: Stack<Screen> = Stack()
  var lastScreen: Screen? = null
    private set

  /** How the latest screen change animates; null when it doesn't. */
  internal var animation: ScreenAnimation? by mutableStateOf(null)
    private set

  fun navigateTo(screen: Screen) {
    animation = ScreenAnimation.Opening.takeIf { animates(from = lastScreen, to = screen) }
    if (lastScreen != null) {
      backStack.push(lastScreen)
    }
    switchScreen(screen)
  }

  fun backStackEmpty() = backStack.empty()

  private fun popBackStack() {
    backStack.pop()
  }

  fun back(): Boolean {
    if (!backStack.empty()) {
      val screen = backStack.pop()
      animation = ScreenAnimation.Closing.takeIf { animates(from = lastScreen, to = screen) }
      switchScreen(screen)
      return true
    }
    return false
  }

  private fun switchScreen(screen: Screen) {
    this.currentScreen = screen
    lastScreen = screen
  }

  fun resetBackStack() {
    while (!backStackEmpty()) {
      popBackStack()
    }
    lastScreen = null
  }
}

// The first screen just shows. A screen of the same kind would share its view model with the one
// it covers, both on screen while it animates.
private fun animates(from: Screen?, to: Screen) = from != null && from::class != to::class

/** A screen rising over the one it covers, or sinking off the one it uncovers. */
internal enum class ScreenAnimation { Opening, Closing }
