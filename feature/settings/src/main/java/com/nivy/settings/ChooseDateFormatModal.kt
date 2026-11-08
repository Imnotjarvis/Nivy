package com.nivy.settings

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nivy.base.model.DateDisplayFormat
import com.nivy.design.system.typography.numberBodyLarge
import com.nivy.ui.R
import com.nivy.ui.time.LocalTimeProvider
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
) = ChooseOptionModal(
    id = ModalId,
    visible = visible,
    title = stringResource(R.string.date_format),
    options = DateDisplayFormat.entries,
    selected = selected,
    label = { it.example() },
    style = MaterialTheme.typography.numberBodyLarge,
    tag = { "date_format_${it.name}" },
    dismiss = dismiss,
    onSelect = onSelect,
)

private val ModalId = UUID.randomUUID()
