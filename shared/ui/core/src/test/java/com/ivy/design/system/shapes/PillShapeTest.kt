package com.ivy.design.system.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test

class PillShapeTest {
    private val density = Density(1f)
    private val phoneRadius = AppCornerRadius.matchPhone

    @After
    fun tearDown() {
        AppCornerRadius.setting = null
        AppCornerRadius.matchPhone = phoneRadius
    }

    private fun cornerOf(width: Float, height: Float): Float {
        val outline = PillShape.createOutline(Size(width, height), LayoutDirection.Ltr, density)
        return (outline as Outline.Rounded).roundRect.topLeftCornerRadius.x
    }

    @Test
    fun `a square element stays a circle whatever the radius`() {
        AppCornerRadius.setting = 4.dp

        cornerOf(48f, 48f) shouldBe 24f
    }

    @Test
    fun `a nearly square element stays a circle`() {
        AppCornerRadius.setting = 4.dp

        cornerOf(50f, 48f) shouldBe 24f
    }

    @Test
    fun `a pill takes the app's corner radius`() {
        AppCornerRadius.setting = 8.dp

        cornerOf(160f, 48f) shouldBe 8f
    }

    @Test
    fun `a radius bigger than the pill keeps it fully round`() {
        AppCornerRadius.setting = 40.dp

        cornerOf(160f, 48f) shouldBe 24f
    }

    @Test
    fun `without a setting the radius matches the phone`() {
        AppCornerRadius.matchPhone = 12.dp

        cornerOf(160f, 48f) shouldBe 12f
    }
}
