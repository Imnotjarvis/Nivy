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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.base.model.DateDisplayFormat
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.White
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeProvider
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.ModalTitle
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Today's date in this format, with its year, to show what the format looks like. */
@Composable
@ReadOnlyComposable
internal fun DateDisplayFormat.example(): String = LocalTimeProvider.current.localDateNow()
    .format(DateTimeFormatter.ofPattern(pattern(currentYear = false), Locale.ENGLISH))

@Composable
internal fun BoxWithConstraintsScope.ChooseDateFormatModal(
    visible: Boolean,
    selected: DateDisplayFormat,
    dismiss: () -> Unit,
    onSelect: (DateDisplayFormat) -> Unit,
) {
    NivyModal(
        id = ModalId,
        visible = visible,
        dismiss = dismiss,
        primaryAction = { },
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = stringResource(R.string.date_format))

        Spacer(Modifier.height(16.dp))

        for (format in DateDisplayFormat.entries) {
            val isSelected = format == selected
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (isSelected) Nivy else Color.Transparent)
                    .clickable {
                        onSelect(format)
                        dismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("date_format_${format.name}"),
                text = format.example(),
                style = MaterialTheme.typography.numberBodyLarge.copy(
                    color = if (isSelected) White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val ModalId = UUID.randomUUID()
