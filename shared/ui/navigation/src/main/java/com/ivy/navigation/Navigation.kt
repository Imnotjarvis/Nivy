package com.ivy.navigation

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

  fun navigateTo(screen: Screen) {
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
      switchScreen(backStack.pop())
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
