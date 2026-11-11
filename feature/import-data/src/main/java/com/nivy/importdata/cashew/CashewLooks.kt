package com.nivy.importdata.cashew

import androidx.compose.ui.graphics.toArgb
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.design.system.colors.NIVY_COLOR_PICKER_HUES

/** A Cashew colour, "0xffRRGGBB", as Nivy's; none when there's none or it can't be read. */
internal fun cashewColor(colour: String?): ColorInt? =
    colour?.trim()?.lowercase()?.removePrefix("0x")?.toLongOrNull(radix = 16)?.let { ColorInt(it.toInt()) }

/** The color picker's [index]th color, from the start again past its end: for what Cashew left without. */
internal fun pickerColor(index: Int): ColorInt =
    ColorInt(NIVY_COLOR_PICKER_HUES[index % NIVY_COLOR_PICKER_HUES.size].toArgb())

/**
 * Nivy's icon for a Cashew one ("cutlery.png"): the icon of the same name, or the one that shows
 * the same thing, which every icon of Cashew's default categories has. None otherwise: the item
 * shows the default icon. An emoji, which Cashew can show instead, has no icon.
 */
internal fun cashewIcon(iconName: String?): IconAsset? {
    val name = iconName?.substringBeforeLast('.')?.trim()?.lowercase() ?: return null
    val nivy = Synonyms[name] ?: name.takeIf { it in SameNames }
    return nivy?.let { IconAsset.from(it).getOrNull() }
}

// Cashew's icons that Nivy has under the same name.
private val SameNames = setOf(
    "bank", "bills", "calculator", "calendar", "camera", "coffee", "diamond", "furniture", "gears",
    "gift", "groceries", "house", "loan", "makeup", "music", "plant", "shopping", "sports",
)

// Cashew's icons that Nivy has under another name: its default categories' first.
private val Synonyms = mapOf(
    "cutlery" to "restaurant",
    "tram" to "transport",
    "popcorn" to "ic_vue_media_film",
    "graduation" to "education",
    "briefcase" to "work",
    "coin" to "cash",
    "flower" to "plant",
    "plane" to "travel",
    "make-up" to "makeup",
    "makeup(1)" to "makeup",
    "coffee-cup" to "coffee",
)
