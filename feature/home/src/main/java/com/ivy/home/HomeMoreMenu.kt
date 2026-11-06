package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.base.model.Theme
import com.ivy.design.utils.LocalScreenSize
import com.ivy.design.utils.thenIf
import com.ivy.design.utils.clickableNoIndication
import com.ivy.design.utils.colorLerp
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.design.utils.statusBarInset
import com.ivy.design.utils.toDensityPx
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.TrendsScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.CircleButtonFilled
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import com.ivy.design.system.motion.AppearRiseFraction
import com.ivy.design.system.motion.CoveredDimAlpha
import androidx.compose.ui.graphics.drawOutline
import com.ivy.design.system.motion.appearTween
import com.ivy.design.system.shapes.AppCornerRadius
import com.ivy.design.utils.consumeClicks
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

/**
 * Home's menu under the arrow at its top end: a panel from the top, as tall as its content, over
 * Home, which dims. Like a dialog, it comes and goes in a fifth of a second, dropping a little as it
 * comes in: solid first, then its content fades in on it, so that Home never shows through it. The
 * arrow stays where it is and turns to point up: the menu closes from where it opened, as from the
 * dim and Back.
 */
@Composable
fun BoxWithConstraintsScope.MoreMenu(
    expanded: Boolean,
    theme: Theme,

    setExpanded: (Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenSize = LocalScreenSize.current

    val progress = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = appearTween(),
        label = "more_menu",
    )
    val shown by remember { derivedStateOf { progress.value > 0f } }

    val nav = navigation()
    val searchLabel = stringResource(R.string.search_transactions)

    val xButton = screenSize.width - 24.dp.toDensityPx() - 40.dp.toDensityPx()
    val yButton = 20.dp.toDensityPx() + statusBarInset()

    if (shown) {
        // The dim and the panel read the animated value while drawing, not in composition.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = CoveredDimAlpha * progress.value) }
                // No ripple: on the dim it would wash over the whole screen as the menu closes.
                .clickable(interactionSource = null, indication = null, enabled = expanded) {
                    setExpanded(false)
                }
                .zIndex(500f)
        )

        val panelColor = MaterialTheme.colorScheme.surfaceVariant
        Column(
            modifier = modifier
                .fillMaxWidth()
                // Drops a little as it comes in, as dialogs rise. The panel turns solid in its first
                // frames and only then does its content fade in on it: fading in together, Home's
                // colors showed through both, as a flash.
                .graphicsLayer {
                    translationY = -(1f - progress.value) * AppearRiseFraction * screenSize.height
                }
                .drawBehind {
                    drawOutline(
                        outline = ShapeBottom.createOutline(size, layoutDirection, this),
                        color = panelColor,
                        alpha = (progress.value / SolidAt).coerceIn(0f, 1f),
                    )
                }
                .consumeClicks(rememberInteractionSource())
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .graphicsLayer { alpha = ((progress.value - SolidAt) / (1f - SolidAt)).coerceIn(0f, 1f) }
                .zIndex(510f)
        ) {
            val modalId = remember {
                UUID.randomUUID()
            }

            AddModalBackHandling(
                modalId = modalId,
                visible = expanded
            ) {
                setExpanded(false)
            }

            Content(
                theme = theme,
                onSwitchTheme = onSwitchTheme,
            )
        }
    }

    CircleButtonFilled(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(x = xButton.roundToInt(), y = yButton.roundToInt())
                }
            }
            .graphicsLayer { rotationZ = -180f * progress.value }
            // Over the panel also while it leaves.
            .thenIf(shown) {
                zIndex(520f)
            }
            // Search is on Home, pulled down from the list's top: screen readers also reach it here.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(searchLabel) {
                        nav.navigateTo(SearchScreen)
                        true
                    }
                )
            }
            .testTag("home_more_menu_arrow"),
        backgroundColor = colorLerp(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface,
            progress.value
        ),
        icon = R.drawable.ic_expandarrow
    ) {
        setExpanded(!expanded)
    }
}

// How far into its coming the panel is solid, before its content fades in on it: a quarter, about
// 50 ms.
private const val SolidAt = 0.25f

/** A panel from the top rounds only its bottom corners, on the app's radius. */
private val ShapeBottom: Shape
    get() = RoundedCornerShape(bottomStart = AppCornerRadius.value, bottomEnd = AppCornerRadius.value)

@Composable
private fun ColumnScope.Content(
    theme: Theme,
    onSwitchTheme: () -> Unit,
) {
    // The title beside the arrow, where Home's header row is: as tall as the arrow, so that both
    // center alike.
    Box(
        modifier = Modifier
            .padding(top = 20.dp, start = 24.dp)
            .height(48.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
    }

    Spacer(Modifier.height(8.dp))

    QuickAccess(
        theme = theme,
        onSwitchTheme = onSwitchTheme
    )

    Spacer(Modifier.height(24.dp))
}

@Composable
private fun QuickAccess(
    theme: Theme,
    onSwitchTheme: () -> Unit
) {
    Column {
        val nav = navigation()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.home_more_menu_settings,
                label = stringResource(R.string.settings)
            ) {
                nav.navigateTo(SettingsScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.home_more_menu_categories,
                label = stringResource(R.string.categories)
            ) {
                nav.navigateTo(CategoriesScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = when (theme) {
                    Theme.LIGHT -> R.drawable.home_more_menu_light_mode
                    Theme.DARK -> R.drawable.home_more_menu_dark_mode
                    Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
                    Theme.AUTO -> R.drawable.home_more_menu_auto_mode
                },
                label = when (theme) {
                    Theme.LIGHT -> stringResource(R.string.light_mode)
                    Theme.DARK -> stringResource(R.string.dark_mode)
                    Theme.AMOLED_DARK -> stringResource(R.string.amoled_mode)
                    Theme.AUTO -> stringResource(R.string.auto_mode)
                },
                backgroundColor = when (theme) {
                    Theme.LIGHT -> MaterialTheme.colorScheme.surface
                    Theme.DARK -> MaterialTheme.colorScheme.onSurface
                    Theme.AMOLED_DARK -> MaterialTheme.colorScheme.onSurface
                    Theme.AUTO -> MaterialTheme.colorScheme.surface
                },
                tint = when (theme) {
                    Theme.LIGHT -> MaterialTheme.colorScheme.onSurface
                    Theme.DARK -> MaterialTheme.colorScheme.surface
                    Theme.AMOLED_DARK -> MaterialTheme.colorScheme.surface
                    Theme.AUTO -> MaterialTheme.colorScheme.onSurface
                }
            ) {
                onSwitchTheme()
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.home_more_menu_planned_payments,
                label = stringResource(R.string.planned_payments)
            ) {
                nav.navigateTo(PlannedPaymentsScreen)
            }

            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Second Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.home_more_menu_reports,
                label = stringResource(R.string.reports),
            ) {
                nav.navigateTo(ReportScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.home_more_menu_budgets,
                label = stringResource(R.string.budgets),
            ) {
                nav.navigateTo(BudgetScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.home_more_menu_loans,
                label = stringResource(R.string.loans),
            ) {
                nav.navigateTo(LoansScreen)
            }

            Spacer(Modifier.weight(1f))

            MoreMenuButton(
                icon = R.drawable.ic_vue_chart_trend_up,
                label = stringResource(R.string.trends),
            ) {
                nav.navigateTo(TrendsScreen)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoreMenuButton(
    @DrawableRes icon: Int,
    label: String,

    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    expandPadding: Dp = 14.dp,

    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircleButtonFilled(
            icon = icon,
            backgroundColor = backgroundColor,
            tint = tint,
            clickAreaPadding = expandPadding,
            onClick = onClick
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier
                .defaultMinSize(minWidth = 92.dp)
                .clickableNoIndication(rememberInteractionSource()) {
                    onClick()
                },
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_Expanded() {
    IvyPreview {
        MoreMenu(
            expanded = true,
            theme = Theme.LIGHT,
            setExpanded = {
            },
            onSwitchTheme = {},
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview() {
    IvyPreview {
        var expanded by remember { mutableStateOf(false) }

        MoreMenu(
            expanded = expanded,
            theme = Theme.LIGHT,
            setExpanded = {
                expanded = it
            },
            onSwitchTheme = {},
        )
    }
}
