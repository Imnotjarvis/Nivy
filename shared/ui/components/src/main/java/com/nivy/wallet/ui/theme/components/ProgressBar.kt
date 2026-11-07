package com.nivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nivy.wallet.NivyComponentPreview
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.colors.Green
import com.nivy.design.system.colors.Nivy
import com.nivy.design.system.colors.Orange
import androidx.compose.material3.MaterialTheme
import com.nivy.design.system.colors.Palette

@Composable
fun ProgressBar(
    percent: Double,
    modifier: Modifier = Modifier,
    notFilledColor: Color = MaterialTheme.colorScheme.surface,
    positiveProgress: Boolean = true
) {
    Spacer(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(notFilledColor)
            .drawBehind {
                drawRect(
                    color = when {
                        percent <= 0.25 -> {
                            if (positiveProgress) Palette.Red else Green
                        }
                        percent <= 0.50 -> {
                            if (positiveProgress) Orange else Nivy
                        }
                        percent <= 0.75 -> {
                            if (positiveProgress) Nivy else Orange
                        }
                        else -> if (positiveProgress) Green else Palette.Red
                    },
                    size = size.copy(
                        width = (size.width * percent).toFloat()
                    )
                )
            },
    )
}

@Preview
@Composable
private fun Preview() {
    NivyComponentPreview {
        ProgressBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 16.dp),
            notFilledColor = Gray,
            percent = 0.6
        )
    }
}
