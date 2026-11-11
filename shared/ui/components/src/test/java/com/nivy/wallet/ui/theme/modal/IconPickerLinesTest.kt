package com.nivy.wallet.ui.theme.modal

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class IconPickerLinesTest {
    private val lines = iconPickerLines(perRow = 5)
    private val rows = lines.filterIsInstance<IconPickerLine.Icons>()

    @Test
    fun `every icon listed has its place in a row, once and in order`() {
        val listed = NivyIconSections.flatMap { it.icons }

        rows.flatMap { it.icons } shouldContainExactly listed
        listed.size shouldBe 379
    }

    @Test
    fun `rows hold five icons, the last of a section what's left`() {
        lines.zipWithNext().forEach { (line, next) ->
            if (line is IconPickerLine.Icons && next is IconPickerLine.Icons) line.icons.size shouldBe 5
        }
    }

    @Test
    fun `the loan dialog's own icon can be picked`() {
        rows.any { "loan" in it.icons } shouldBe true
    }
}
