package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.ui.components.spacers.SmallVerticalSpacer
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard
import com.d4rk.cleaner.core.domain.model.network.Errors
import java.io.File

@Composable
fun LargeFilesCard(
    state: DataState<List<File>, Errors>,
    modifier: Modifier = Modifier,
    onOpenClick: () -> Unit
) {
    val files = (state as? DataState.Success)?.data ?: emptyList()
    val preview = files.take(4)

    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.DonutLarge,
        title = stringResource(id = R.string.large_files_card_title),
        subtitle = stringResource(id = R.string.large_files_card_subtitle),
        actionLabel = stringResource(id = R.string.open_large_files),
        actionPainter = painterResource(id = R.drawable.ic_folder_search),
        onActionClick = onOpenClick,
        onHeaderClick = onOpenClick
    ) {
        SmallVerticalSpacer()
        when (state) {
            is DataState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            is DataState.Error -> {
                Text(text = state.error.toString(), style = MaterialTheme.typography.bodySmall)
            }

            is DataState.Success -> {
                AnimatedVisibility(visible = preview.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .animateContentSize(),
                        horizontalArrangement = Arrangement.spacedBy(SizeConstants.SmallSize),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        preview.forEach { file ->
                            FilePreviewCard(
                                file = file,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        if (files.size > preview.size) {
                            Text(
                                text = pluralStringResource(
                                    id = R.plurals.large_files_card_more_format,
                                    count = files.size - preview.size,
                                    files.size - preview.size
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
