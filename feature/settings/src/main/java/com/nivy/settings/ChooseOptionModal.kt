package com.nivy.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.colors.readableTint
import com.nivy.wallet.ui.theme.modal.ModalTitle
import com.nivy.wallet.ui.theme.modal.NivyModal
import java.util.UUID

/**
 * A dialog to choose one of [options], each a row showing its [label] in [style]; the [selected]
 * one is filled. Choosing one closes it. [tag] names each row for the UI tests.
 */
@Composable
internal fun <T> BoxWithConstraintsScope.ChooseOptionModal(
    id: UUID,
    visible: Boolean,
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    style: TextStyle,
    tag: (T) -> String,
    dismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    NivyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        primaryAction = { },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = title)

        Spacer(Modifier.height(16.dp))

        for (option in options) {
            val isSelected = option == selected
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isSelected) Nivy else Color.Transparent)
                    .clickable {
                        onSelect(option)
                        dismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag(tag(option)),
                text = label(option),
                style = style.copy(
                    // Readable on the fill, also where minimal colors turn it into the text color.
                    color = if (isSelected) readableTint(Nivy, White) else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
