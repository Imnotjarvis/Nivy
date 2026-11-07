package com.ivy.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.utils.thenIf
import com.ivy.ui.R
import com.ivy.design.system.colors.Gray
import com.ivy.design.system.colors.White
import com.ivy.wallet.ui.theme.components.IvySwitch
import androidx.compose.material3.MaterialTheme
import com.ivy.design.system.typography.numberBodyLarge
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.ivy.design.system.colors.readableTint

@Composable
internal fun StartDateOfMonth(
    startDateOfMonth: Int,
    onClick: () -> Unit
) {
    SettingsButtonRow(
        onClick = onClick
    ) {
        Spacer(Modifier.width(12.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(2.dp),
            painter = painterResource(id = R.drawable.ic_custom_calendar_m),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp),
            text = stringResource(R.string.start_date_of_month),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = startDateOfMonth.toString(),
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(32.dp))
    }
}

// A setting showing its current value on the right.
@Composable
internal fun SettingsValueRow(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    SettingsButtonRow(
        onClick = onClick
    ) {
        Spacer(Modifier.width(12.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(12.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp),
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.numberBodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
internal fun CustomFeatures(
    onClick: () -> Unit
) {
    SettingsButtonRow(
        onClick = onClick
    ) {
        Spacer(Modifier.width(12.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(0.dp),
            painter = painterResource(id = R.drawable.ic_custom_programming_m),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            modifier = Modifier.padding(vertical = 20.dp),
            text = stringResource(R.string.advanced_features),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
internal fun AppThemeButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit
) {
    SettingsPrimaryButton(
        icon = icon,
        text = label,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        textColor = MaterialTheme.colorScheme.onSurface,
        iconPadding = 6.dp,
        description = stringResource(R.string.tap_to_switch_theme),
        onClick = onClick
    )
}

@Composable
internal fun AppSwitch(
    lockApp: Boolean,
    onSetLockApp: (Boolean) -> Unit,
    text: String,
    icon: Int,
    description: String = "",
) {
    SettingsButtonRow(
        onClick = {
            onSetLockApp(!lockApp)
        }
    ) {
        Spacer(Modifier.width(12.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(0.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(8.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(top = 20.dp, bottom = 20.dp, end = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )
            if (description.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Gray,
                        fontWeight = FontWeight.Normal
                    ).copy(fontSize = 14.sp)
                )
            }
        }

        // Spacer(Modifier.weight(1f))

        IvySwitch(enabled = lockApp) {
            onSetLockApp(it)
        }

        Spacer(Modifier.width(16.dp))
    }
}

@Composable
internal fun AccountCard(
    nameLocalAccount: String?,
    onCardClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge)
            .clickable {
                onCardClick()
            }
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_profile_card"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(24.dp))

            Text(
                text = stringResource(R.string.account_uppercase),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Black
                )
            )
        }

        Spacer(Modifier.height(4.dp))

        AccountCardLocalAccount(
            name = nameLocalAccount,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AccountCardLocalAccount(
    name: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(20.dp))
        Icon(
            modifier = Modifier.size(48.dp).padding(4.dp),
            painter = painterResource(id = R.drawable.ic_local_account),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(12.dp))

        Text(
            modifier = Modifier
                .weight(1f)
                .testTag("local_account_name"),
            text = if (!name.isNullOrBlank()) name else stringResource(R.string.anonymous),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.width(12.dp))
    }
}

@Composable
internal fun ExportCSV(
    onExportToCSV: () -> Unit
) {
    SettingsDefaultButton(
        icon = R.drawable.ic_vue_pc_printer,
        text = stringResource(R.string.export_to_csv),
        iconPadding = 6.dp,
        description = stringResource(R.string.do_not_use_for_backup_purposes)
    ) {
        onExportToCSV()
    }
}

@Composable
internal fun SettingsPrimaryButton(
    @DrawableRes icon: Int,
    text: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = White,
    iconPadding: Dp = 0.dp,
    description: String? = null,
    onClick: () -> Unit
) {
    // Readable on the button, also where minimal colors turn it into the text color.
    val content = readableTint(backgroundColor, textColor)
    SettingsButtonRow(
        backgroundColor = backgroundColor,
        onClick = onClick
    ) {
        Spacer(Modifier.width(12.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(iconPadding),
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = content
        )

        Spacer(Modifier.width(8.dp))

        Column(
            Modifier
                .weight(1f)
                .padding(top = 20.dp, bottom = 20.dp, end = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = content,
                    fontWeight = FontWeight.Bold
                )
            )
            if (!description.isNullOrEmpty()) {
                Text(
                    modifier = Modifier.padding(end = 8.dp),
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Gray,
                        fontWeight = FontWeight.Normal
                    ).copy(fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
private fun SettingsButtonRow(
    onClick: (() -> Unit)?,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .thenIf(onClick != null) {
                clickable {
                    onClick?.invoke()
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
internal fun CurrencyButton(
    currency: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(0.dp),
            painter = painterResource(id = R.drawable.ic_currency),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(8.dp))

        Text(
            modifier = Modifier.padding(vertical = 20.dp),
            text = stringResource(R.string.set_currency),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = currency,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(4.dp))

        Icon(
            modifier = Modifier.size(48.dp).padding(4.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
internal fun SettingsSectionDivider(
    text: String,
    color: Color = Gray
) {
    Column {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(start = 32.dp),
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
internal fun SettingsDefaultButton(
    @DrawableRes icon: Int,
    text: String,
    iconPadding: Dp = 0.dp,
    description: String? = null,
    onClick: () -> Unit,
) {
    SettingsPrimaryButton(
        icon = icon,
        text = text,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        textColor = MaterialTheme.colorScheme.onSurface,
        iconPadding = iconPadding,
        description = description
    ) {
        onClick()
    }
}
