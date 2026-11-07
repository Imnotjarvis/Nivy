package com.nivy.importdata.csvimport.flow.instructions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nivy.ui.R

@Composable
internal fun NivySteps(
    onUploadClick: () -> Unit
) {
    Spacer(Modifier.height(12.dp))

    StepTitle(
        number = 1,
        title = stringResource(R.string.export_data),
        description = """
            Settings > "Backup Data" (do NOT use "Export to CSV"!)
        """.trimIndent()
    )

    Spacer(Modifier.height(36.dp))

    UploadFileStep(
        stepNumber = 2,
        text = stringResource(R.string.upload_backup_file),
        btnTitle = stringResource(R.string.upload_backup_file_button),
        onUploadClick = onUploadClick
    )
}
