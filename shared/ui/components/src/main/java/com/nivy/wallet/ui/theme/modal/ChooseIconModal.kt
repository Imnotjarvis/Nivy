package com.nivy.wallet.ui.theme.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import com.nivy.design.system.shapes.PillShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.hideKeyboard
import com.nivy.design.utils.thenIf
import com.nivy.ui.R
import com.nivy.design.system.colors.asItemColor
import com.nivy.design.system.colors.Nivy
import com.nivy.wallet.ui.theme.components.ItemIconS
import com.nivy.design.system.colors.dynamicContrast
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect

private const val ICON_PICKER_ICONS_PER_ROW = 5

@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.ChooseIconModal(
    visible: Boolean,
    initialIcon: String?,
    color: Color,

    dismiss: () -> Unit,

    modifier: Modifier = Modifier,
    id: UUID = UUID.randomUUID(),
    onIconChosen: (String?) -> Unit
) {
    var selectedIcon by remember(id) {
        mutableStateOf(initialIcon)
    }

    NivyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = dismiss,
        scrollState = null,
        includeActionsRowPadding = false,
        primaryAction = {
            ModalSave(
                modifier = Modifier.testTag("choose_icon_save")
            ) {
                onIconChosen(selectedIcon)
                dismiss()
            }
        }
    ) {
        val view = LocalView.current
        LaunchedEffect(Unit) {
            hideKeyboard(view)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(Modifier.height(32.dp))

                ModalTitle(text = stringResource(R.string.choose_icon))

                Spacer(Modifier.height(4.dp))
            }

            icons(selectedIcon = selectedIcon, color = color.asItemColor()) {
                selectedIcon = it
            }

            item {
                Spacer(Modifier.height(160.dp))
            }
        }
    }
}

private fun LazyListScope.icons(
    selectedIcon: String?,
    color: Color,

    onIconSelected: (String) -> Unit
) {
    for (line in iconPickerLines()) {
        when (line) {
            is IconPickerLine.Title -> item {
                Section(title = line.title)
            }

            is IconPickerLine.Icons -> item {
                IconsRow(
                    icons = line.icons,
                    selectedIcon = selectedIcon,
                    color = color
                ) {
                    onIconSelected(it)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** A line of the icon picker: a section's title, or a row of its icons. */
internal sealed interface IconPickerLine {
    data class Title(val title: String) : IconPickerLine
    data class Icons(val icons: ImmutableList<String>) : IconPickerLine
}

/** The icon picker's lines: each section's title, then its icons, [perRow] to a row. */
internal fun iconPickerLines(perRow: Int = ICON_PICKER_ICONS_PER_ROW): List<IconPickerLine> =
    NivyIconSections.flatMap { section ->
        listOf(IconPickerLine.Title(section.title)) +
            section.icons.chunked(perRow).map { IconPickerLine.Icons(it.toImmutableList()) }
    }

@Composable
@Suppress("ParameterNaming")
private fun IconsRow(
    icons: ImmutableList<String>,
    selectedIcon: String?,
    color: Color,

    onIconSelected: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        for ((index, icon) in icons.withIndex()) {
            Icon(
                icon = icon,
                selected = selectedIcon == icon,
                color = color
            ) {
                onIconSelected(icon)
            }

            if (index < icons.lastIndex && icons.size >= 5) {
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.width(20.dp))
            }
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun Icon(
    icon: String,
    selected: Boolean,
    color: Color,

    onClick: () -> Unit,
) {
    ItemIconS(
        modifier = Modifier
            .clip(PillShape)
            .border(2.dp, if (selected) color else MaterialTheme.colorScheme.surfaceVariant, PillShape)
            .thenIf(selected) {
                background(color, PillShape)
            }
            .clickable {
                onClick()
            }
            .padding(all = 8.dp)
            .testTag(icon),
        iconName = icon,
        tint = if (selected) color.dynamicContrast() else MaterialTheme.colorScheme.inverseSurface
    )
}

@Composable
private fun Section(
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.width(16.dp))

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun ChooseIconModal() {
    NivyPreview {
        ChooseIconModal(
            visible = true,
            initialIcon = "gift",
            color = Nivy,
            dismiss = {}
        ) {
        }
    }
}
