package com.ivy.wallet.utils

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract

fun ComponentActivity.simpleActivityForResultLauncher(
    intent: Intent,
    onActivityResult: (resultCode: Int, data: Intent?) -> Unit
): ActivityResultLauncher<Unit> {
    return activityForResultLauncher(
        createIntent = { _, _ -> intent },
        onActivityResult = onActivityResult
    )
}

fun <I> ComponentActivity.activityForResultLauncher(
    createIntent: (context: Context, input: I) -> Intent,
    onActivityResult: (resultCode: Int, data: Intent?) -> Unit
): ActivityResultLauncher<I> {
    return registerForActivityResult(
        activityResultContract(
            createIntent = createIntent,
            onActivityResult = onActivityResult
        )
    ) {
    }
}

private fun <I> activityResultContract(
    createIntent: (context: Context, input: I) -> Intent,
    onActivityResult: (resultCode: Int, data: Intent?) -> Unit
): ActivityResultContract<I, Unit> {
    return object : ActivityResultContract<I, Unit>() {
        override fun createIntent(
            context: Context,
            input: I
        ): Intent {
            return createIntent(context, input)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ) {
            onActivityResult(resultCode, intent)
        }
    }
}
