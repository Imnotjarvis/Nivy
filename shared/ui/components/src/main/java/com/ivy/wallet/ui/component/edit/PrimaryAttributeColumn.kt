package com.ivy.wallet.ui.component.edit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.wallet.IvyWalletComponentPreview
import com.ivy.design.utils.clickableNoIndication
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyIcon
import androidx.compose.material3.MaterialTheme

@Composable
internal fun PrimaryAttributeColumn(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    titleRowExtra: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickableNoIndication(rememberInteractionSource(), onClick = onClick),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IvyIcon(icon = icon)

            Spacer(Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            titleRowExtra?.invoke(this)
        }

        content()
    }
}

@Preview
@Composable
private fun PreviewPrimaryAttributeColumn() {
    IvyWalletComponentPreview {
        PrimaryAttributeColumn(
            icon = R.drawable.ic_description,
            title = stringResource(R.string.description),
            onClick = { }
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "This mode is not recommended for production use,\n" +
                        "as no stability/compatibility guarantees are given on\n" +
                        "compiler or generated code. Use it at your own risk!\n" +
                        "\n" +
                        "\n" +
                        "Deprecated Gradle features were used in this build, " +
                        "making it incompatible with Gradle 8.0.\n" +
                        "Use '--warning-mode all' to show the individual deprecation warnings.\n" +
                        "See the Gradle command line interface docs.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
