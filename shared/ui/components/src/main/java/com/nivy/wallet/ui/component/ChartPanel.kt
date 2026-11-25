package com.nivy.wallet.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nivy.design.system.colors.Gray
import com.nivy.design.system.shapes.PillShape
import com.nivy.ui.R
import com.nivy.wallet.ui.theme.components.NivyIcon
import com.nivy.wallet.utils.springBounce

/**
 * A chart's panel: its [title], a [legend] beside it, and the chart below. With [onExpandedChange],
 * its title's row folds it down to itself, and opens it again, as the arrow at its end shows; the
 * legend then goes under the title.
 */
@Composable
fun ChartPanel(
    title: String,
    legend: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .then(
                    if (onExpandedChange != null) {
                        Modifier.clickable(interactionSource, indication = null, role = Role.Button) {
                            onExpandedChange(!expanded)
                        }
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                ),
            )
            Spacer(Modifier.weight(1f))
            if (onExpandedChange == null) legend() else FoldArrow(expanded)
        }
        if (expanded) {
            if (onExpandedChange != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = legend,
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

// Down while folded, up while open, as on the lists' sections.
@Composable
private fun FoldArrow(expanded: Boolean) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) OpenRotation else 0f,
        animationSpec = springBounce(),
    )
    NivyIcon(modifier = Modifier.rotate(rotation), icon = R.drawable.ic_expandarrow)
}

private const val OpenRotation = -180f

/** A series' color in a chart's legend, and its name. */
@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(color, PillShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(color = Gray))
    }
}
