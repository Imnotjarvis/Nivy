package com.ivy.wallet.ui.component.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.repository.mapper.TagMapper.Companion.createNewTagId
import com.ivy.design.system.colors.Blue2Dark
import com.ivy.design.utils.thenIf
import com.ivy.wallet.ui.SearchInput
import com.ivy.design.utils.drawColoredShadow
import com.ivy.design.utils.hideKeyboard
import com.ivy.design.utils.selectEndTextFieldValue
import com.ivy.design.system.colors.Gradient
import com.ivy.wallet.ui.theme.components.IvyBorderButton
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.design.system.colors.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.DeleteModal
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalPositiveButton
import com.ivy.wallet.ui.theme.modal.ModalTitle
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.WrapContentRow
import kotlinx.collections.immutable.ImmutableList
import java.util.UUID
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect

@ExperimentalFoundationApi
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.ShowTagModal(
    onDismiss: () -> Unit,
    allTagList: ImmutableList<Tag>,
    selectedTagList: ImmutableList<TagId>,
    onTagAdd: (String) -> Unit,
    onTagEdit: (oldTag: Tag, newTag: Tag) -> Unit,
    onTagDelete: (Tag) -> Unit,
    onTagSelected: (Tag) -> Unit,
    onTagDeSelected: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    id: UUID = UUID.randomUUID(),
    visible: Boolean = false,
    selectOnlyMode: Boolean = false,
    onTagSearch: (String) -> Unit
) {
    var showTagAddModal by remember {
        mutableStateOf(false)
    }

    var deleteTagModalVisible by remember {
        mutableStateOf(false)
    }

    var selectedTag by remember {
        mutableStateOf<Tag?>(null)
    }

    var selectedTagId by remember(selectedTag) {
        mutableStateOf(selectedTag?.id ?: createNewTagId())
    }

    var searchQueryTextFieldValue by remember(visible) {
        mutableStateOf(selectEndTextFieldValue(""))
    }

    IvyModal(
        modifier = modifier,
        id = id,
        visible = visible,
        dismiss = onDismiss,
        primaryAction = {
            ModalPositiveButton(
                onClick = onDismiss,
                text = stringResource(R.string.done),
                iconStart = R.drawable.ic_custom_document_s
            )
        },
        scrollState = null
    ) {
        HideKeyboard()

        Spacer(Modifier.height(32.dp))

        ModalTitle(text = stringResource(R.string.tags))

        Spacer(Modifier.height(24.dp))

        SearchInput(
            searchQueryTextFieldValue = searchQueryTextFieldValue,
            hint = stringResource(id = R.string.search_tags),
            onSetSearchQueryTextField = {
                searchQueryTextFieldValue = it
                onTagSearch(it.text)
            }
        )

        Spacer(Modifier.height(24.dp))

        TagList(
            transactionTags = allTagList,
            selectedTagList = selectedTagList,
            selectOnlyMode = selectOnlyMode,
            onAddNewTag = {
                showTagAddModal = true
            },
            onTagSelected = {
                onTagSelected(it)
            },
            onTagDeSelected = {
                onTagDeSelected(it)
            },
            onTagLongClick = {
                if (!selectOnlyMode) {
                    selectedTag = it
                    showTagAddModal = true
                }
            }
        )
    }

    val view = LocalView.current

    AddOrEditTagModal(
        id = selectedTagId,
        initialTag = selectedTag,
        visible = showTagAddModal,
        onDismiss = {
            showTagAddModal = false
            selectedTag = null
        },
        onTagAdd = {
            onTagAdd(it)
            selectedTag = null
            selectedTagId = createNewTagId()
        },
        onTagDelete = {
            deleteTagModalVisible = true
            hideKeyboard(view)
        },
        onTagEdit = { oldTag, newTag ->
            onTagEdit(oldTag, newTag)
        }
    )

    DeleteModal(
        visible = deleteTagModalVisible,
        title = stringResource(R.string.confirm_deletion),
        description = "Are you sure you want to delete the following tag:\t'${selectedTag?.name?.value}' ?",
        dismiss = { deleteTagModalVisible = false }
    ) {
        if (selectedTag != null) {
            deleteTagModalVisible = false
            onTagDelete(selectedTag!!)
            showTagAddModal = false
            selectedTag = null
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("ParameterNaming")
@Composable
private fun ColumnScope.TagList(
    transactionTags: ImmutableList<Tag>,
    onAddNewTag: () -> Unit,
    selectedTagList: ImmutableList<TagId>,
    selectOnlyMode: Boolean,
    onTagSelected: (Tag) -> Unit = {},
    onTagDeSelected: (Tag) -> Unit = {},
    onTagLongClick: (Tag) -> Unit = {}
) {
    val tagListWithAddNewTag: List<Any> by remember(transactionTags) {
        if (selectOnlyMode) {
            mutableStateOf(transactionTags)
        } else {
            mutableStateOf(listOf(AddNewTag()) + transactionTags)
        }
    }

    WrapContentRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalMarginBetweenItems = 12.dp,
        verticalMarginBetweenRows = 12.dp,
        items = tagListWithAddNewTag
    ) {
        when (it) {
            is Tag -> {
                ExistingTag(
                    tag = it,
                    selected = selectedTagList.contains(it.id),
                    onClick = { onTagSelected(it) },
                    onLongClick = { onTagLongClick(it) },
                    onDeselect = {
                        onTagDeSelected(it)
                    }
                )
            }

            is AddNewTag -> {
                AddNewTagButton(onClick = onAddNewTag)
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ExistingTag(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDeselect: () -> Unit,
) {
    val tagColor = Blue2Dark
    val rFull = CircleShape

    Row(
        modifier = Modifier
            .thenIf(selected) {
                drawColoredShadow(tagColor)
            }
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .thenIf(selected) {
                background(tagColor, rFull)
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("choose_category_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(if (selected) 12.dp else 8.dp))

        Text(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .padding(
                    start = if (selected) 12.dp else 12.dp,
                    end = if (selected) 20.dp else 24.dp
                )
                .weight(1f, fill = false),
            text = "#${tag.name.value}",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (selected) findContrastTextColor(tagColor) else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (selected) {
            val deselectBtnBackground = findContrastTextColor(tagColor)
            IvyCircleButton(
                modifier = Modifier.size(32.dp),
                icon = R.drawable.ic_remove,
                backgroundGradient = Gradient.solid(deselectBtnBackground),
                tint = findContrastTextColor(deselectBtnBackground)
            ) {
                onDeselect()
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun HideKeyboard() {
    val view = LocalView.current
    LaunchedEffect(Unit) { hideKeyboard(view) }
}

@Composable
private fun AddNewTagButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IvyBorderButton(
        modifier = modifier,
        text = stringResource(R.string.add_new),
        backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.inverseSurface),
        iconStart = R.drawable.ic_plus,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        ),
        iconTint = MaterialTheme.colorScheme.onSurface,
        padding = 10.dp,
        onClick = onClick
    )
}

private class AddNewTag
