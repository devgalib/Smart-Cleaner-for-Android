package com.d4rk.cleaner.app.clean.contacts.ui.components

import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerEvent
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.UiContactsCleanerModel
import com.d4rk.cleaner.app.clean.contacts.ui.ContactsCleanerViewModel

@Composable
fun ContactsCleanerList(
    data: UiContactsCleanerModel,
    viewModel: ContactsCleanerViewModel,
    paddingValues: PaddingValues,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(SizeConstants.LargeSize),
        contentPadding = PaddingValues(vertical = SizeConstants.LargeSize)
    ) {
        stickyHeader {
            val allSelected = data.duplicates.flatMap { it.contacts }.all { it.isSelected }
            val noneSelected = data.duplicates.flatMap { it.contacts }.none { it.isSelected }
            val toggleState = when {
                allSelected -> ToggleableState.On
                noneSelected -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
            val haptic = LocalHapticFeedback.current
            val view: View = LocalView.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(CircleShape)
                    .padding(horizontal = SizeConstants.LargeSize)
                    .clickable {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(ContactsCleanerEvent.ToggleSelectAll)
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TriStateCheckbox(
                    state = toggleState,
                    onClick = {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onEvent(ContactsCleanerEvent.ToggleSelectAll)
                    },
                )
                Text(
                    text = stringResource(id = R.string.select_all),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = SizeConstants.SmallSize),
                )
            }
        }
        items(
            items = data.duplicates,
            key = { group ->
                group.contacts.firstOrNull()?.rawContactId ?: group.hashCode()
            },
        ) { group ->
            ContactGroupItem(group = group, viewModel = viewModel)
        }
    }
}

