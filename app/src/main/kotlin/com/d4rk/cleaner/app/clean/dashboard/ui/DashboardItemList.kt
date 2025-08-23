package com.d4rk.cleaner.app.clean.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants

@Composable
fun DashboardItemList(
    items: List<HomeItem>,
    visibilityStates: SnapshotStateList<Boolean>,
    listState: LazyListState,
    renderItem: @Composable LazyItemScope.(HomeItem, Int, SnapshotStateList<Boolean>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space = SizeConstants.LargeSize),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = listState,
        contentPadding = PaddingValues(SizeConstants.LargeSize)
    ) {
        itemsIndexed(items) { index, item ->
            renderItem(item, index, visibilityStates)
        }
    }
}
