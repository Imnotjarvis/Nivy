package com.nivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Red
import java.util.UUID
import com.nivy.design.system.shapes.PillShape
import androidx.compose.material3.MaterialTheme

@Composable
fun BoxWithConstraintsScope.ProgressModal(
    title: String,
    description: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    NivyModal(
        modifier = modifier,
        id = remember { UUID.randomUUID() },
        visible = visible,
        dismiss = {},
        primaryAction = {}
    ) {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Red,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(24.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(8.dp)
                .clip(
                    PillShape
                ),
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(Modifier.height(48.dp))
    }
}
