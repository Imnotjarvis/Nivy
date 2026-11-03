package com.ivy.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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

  /** The block just pressed, if it opens a screen: see [opensScreen]. */
  internal var pressedBlock: Block? = null

  /** How the latest screen change animates; null when it doesn't. */
  internal var animation: ScreenAnimation? by mutableStateOf(null)
    private set

  // The block each screen was opened from, if any, to shrink back into it: the current screen's,
  // and below it those of the screens on the back stack.
  private var currentBlock: Block? = null
  private val blockStack: Stack<Block?> = Stack()

  fun navigateTo(screen: Screen) {
    // A screen of the same kind would share its view model with the one it covers.
    val from = lastScreen
    val block = pressedBlock?.takeIf { from != null && from::class != screen::class }
    pressedBlock = null
    if (lastScreen != null) {
      backStack.push(lastScreen)
      blockStack.push(currentBlock)
    }
    currentBlock = block
    animation = block?.let { ScreenAnimation(block = it, opening = true) }
    switchScreen(screen)
  }

  fun backStackEmpty() = backStack.empty()

  private fun popBackStack() {
    backStack.pop()
    blockStack.pop()
  }

  fun back(): Boolean {
    if (!backStack.empty()) {
      animation = currentBlock?.let { ScreenAnimation(block = it, opening = false) }
      currentBlock = blockStack.pop()
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
    currentBlock = null
  }
}

/** A block that opens a screen: where it is, and its color, which the card growing from it has. */
internal data class Block(val bounds: Rect, val color: Color)

/** A screen growing out of the [block] it was opened from, or shrinking back into it. */
internal data class ScreenAnimation(val block: Block, val opening: Boolean)
