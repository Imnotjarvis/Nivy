package com.ivy.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

@Composable
fun BackButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  IconButton(
    modifier = modifier,
    onClick = onClick,
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
      contentDescription = stringResource(R.string.back)
    )
  }
}
