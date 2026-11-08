package com.nivy.wallet.ui.theme.modal.edit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.nivy.design.system.shapes.PillShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import arrow.core.raise.either
import com.nivy.data.model.Category
import com.nivy.data.model.primitive.ColorInt
import com.nivy.data.model.primitive.IconAsset
import com.nivy.data.model.primitive.NotBlankTrimmedString
import com.nivy.wallet.ui.NivyColorPicker
import com.nivy.wallet.NivyPreview
import com.nivy.design.utils.hideKeyboard
import com.nivy.wallet.utils.isNotNullOrBlank
import com.nivy.design.utils.selectEndTextFieldValue
import com.nivy.ui.R
import com.nivy.wallet.domain.logic.model.CreateCategoryData
import com.nivy.design.system.colors.Palette
import com.nivy.design.system.colors.shown
import com.nivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.nivy.wallet.ui.theme.components.NivyNameTextField
import com.nivy.design.system.colors.dynamicContrast
import com.nivy.wallet.ui.theme.modal.ChooseIconModal
import com.nivy.wallet.ui.theme.modal.NivyModal
import com.nivy.wallet.ui.theme.modal.dialogData
import com.nivy.wallet.ui.theme.modal.ModalAddSave
import com.nivy.wallet.ui.theme.modal.ModalTitle
import java.util.UUID
import androidx.compose.runtime.LaunchedEffect

data class CategoryModalData(
    val category: Category?,
    val id: UUID = UUID.randomUUID(),
    val autoFocusKeyboard: Boolean = true,
)

@Composable
fun BoxWithConstraintsScope.CategoryModal(
    modal: CategoryModalData?,
    onCreateCategory: (CreateCategoryData) -> Unit,
    onEditCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    dismiss: () -> Unit,
) {
    // Closing clears the data: the dialog keeps showing the last while it fades out.
    val shown = dialogData(modal)
    val initialCategory = shown?.category
    var nameTextFieldValue by remember(shown) {
        mutableStateOf(selectEndTextFieldValue(initialCategory?.name?.value))
    }
    var color by remember(shown) {
        mutableStateOf(initialCategory?.color?.let { Color(it.value) } ?: Palette.Nivy)
    }
    var icon by remember(shown) {
        mutableStateOf(initialCategory?.icon)
    }

    var chooseIconModalVisible by remember(shown) {
        mutableStateOf(false)
    }

    NivyModal(
        modifier = modifier,
        id = shown?.id,
        visible = modal != null,
        dismiss = dismiss,
        primaryAction = {
            ModalAddSave(
                item = shown?.category,
                enabled = nameTextFieldValue.text.isNotNullOrBlank()
            ) {
                if (initialCategory != null) {
                    val updatedCategory = either {
                        initialCategory.copy(
                            name = NotBlankTrimmedString.from(nameTextFieldValue.text.trim()).bind(),
                            color = ColorInt(color.toArgb()),
                            icon = icon
                        )
                    }.getOrNull()

                    if (updatedCategory != null) {
                        onEditCategory(updatedCategory)
                    }
                } else {
                    onCreateCategory(
                        CreateCategoryData(
                            name = nameTextFieldValue.text.trim(),
                            color = color,
                            icon = icon?.id
                        )
                    )
                }

                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = if (shown?.category != null) {
                stringResource(R.string.edit_category)
            } else {
                stringResource(
                    R.string.create_category
                )
            }
        )

        Spacer(Modifier.height(24.dp))

        IconNameRow(
            hint = stringResource(R.string.category_name),
            defaultIcon = R.drawable.ic_custom_category_m,
            color = color,
            icon = icon?.id,

            autoFocusKeyboard = shown?.autoFocusKeyboard ?: true,

            nameTextFieldValue = nameTextFieldValue,
            setNameTextFieldValue = { nameTextFieldValue = it },
            showChooseIconModal = {
                chooseIconModalVisible = true
            }
        )

        Spacer(Modifier.height(40.dp))

        NivyColorPicker(
            selectedColor = color,
            onColorSelected = { color = it }
        )

        Spacer(Modifier.height(48.dp))
    }

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon?.id ?: "category",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it?.let { iconId -> IconAsset.unsafe(iconId) }
    }
}

@Composable
fun IconNameRow(
    hint: String,
    @DrawableRes defaultIcon: Int,
    color: Color,
    icon: String?,

    autoFocusKeyboard: Boolean,

    nameTextFieldValue: TextFieldValue,
    setNameTextFieldValue: (TextFieldValue) -> Unit,

    modifier: Modifier = Modifier,
    showChooseIconModal: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val nameFocus = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            if (autoFocusKeyboard) {
                nameFocus.requestFocus()
            }
        }

        Spacer(Modifier.width(24.dp))

        ItemIconMDefaultIcon(
            modifier = Modifier
                .clip(PillShape)
                .background(color.shown(), PillShape)
                .clickable {
                    showChooseIconModal()
                }
                .testTag("modal_item_icon"),
            iconName = icon,
            tint = color.shown().dynamicContrast(),
            defaultIcon = defaultIcon
        )

        val view = LocalView.current
        NivyNameTextField(
            modifier = Modifier
                .padding(start = 28.dp, end = 36.dp)
                .focusRequester(nameFocus),
            underlineModifier = Modifier.padding(start = 24.dp, end = 32.dp),
            value = nameTextFieldValue,
            hint = hint,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = true
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    hideKeyboard(view)
                }
            ),
        ) { newValue ->
            setNameTextFieldValue(newValue)
        }
    }
}

@Preview
@Composable
private fun PreviewCategoryModal() {
    NivyPreview {
        CategoryModal(
            modal = CategoryModalData(null),
            onCreateCategory = { },
            onEditCategory = { }
        ) {
        }
    }
}
