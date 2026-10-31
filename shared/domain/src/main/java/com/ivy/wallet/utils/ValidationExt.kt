package com.ivy.wallet.utils

fun String?.isNotNullOrBlank(): Boolean {
    return this != null && this.isNotBlank()
}
