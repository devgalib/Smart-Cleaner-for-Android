package com.d4rk.cleaner.app.clean.contacts.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.ui.components.ads.AdBanner
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.TonalIconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallHorizontalSpacer
import com.d4rk.cleaner.R

@Composable
fun ContactsCleanerBottomBar(
    selectedCount: Int,
    selectionState: SelectionState,
    canMerge: Boolean,
    onMerge: () -> Unit,
    onDelete: () -> Unit,
    bottomBarAdConfig: AdsConfig,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Column {
            AdBanner(modifier = Modifier.fillMaxWidth(), adsConfig = bottomBarAdConfig)

            BottomAppBar(
                actions = {
                    Text(
                        modifier = Modifier.weight(1f, fill = true),
                        text = pluralStringResource(
                            R.plurals.items_selected,
                            selectedCount,
                            selectedCount,
                        ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    SmallHorizontalSpacer()
                    when (selectionState) {
                        SelectionState.SINGLE -> {
                            TonalIconButtonWithText(
                                onClick = onDelete,
                                label = stringResource(id = R.string.delete),
                            )
                        }
                        SelectionState.SAME_GROUP -> {
                            TonalIconButtonWithText(
                                onClick = onMerge,
                                enabled = selectedCount >= 2,
                                label = stringResource(id = R.string.merge),
                            )
                            SmallHorizontalSpacer()
                            TonalIconButtonWithText(
                                onClick = onDelete,
                                icon = Icons.Filled.AutoAwesome,
                                iconContentDescription = null,
                                label = stringResource(id = R.string.smart_clean),
                            )
                        }
                        SelectionState.MULTIPLE_GROUPS -> {
                            TonalIconButtonWithText(
                                onClick = onMerge,
                                enabled = canMerge,
                                label = stringResource(id = R.string.merge_groups),
                            )
                            SmallHorizontalSpacer()
                            TonalIconButtonWithText(
                                onClick = onDelete,
                                icon = Icons.Filled.AutoAwesome,
                                iconContentDescription = null,
                                label = stringResource(id = R.string.smart_clean_all),
                            )
                        }
                    }
                },
            )
        }
    }
}

