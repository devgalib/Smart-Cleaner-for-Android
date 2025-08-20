package com.d4rk.cleaner.app.clean.duplicates.ui

import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.OutlinedIconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.LoadingScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.NoDataScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.ScreenStateHandler
import com.d4rk.android.libs.apptoolkit.core.ui.components.navigation.LargeTopAppBarWithScaffold
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.analyze.ui.components.CleaningAnimationScreen
import com.d4rk.cleaner.app.clean.duplicates.domain.actions.DuplicatesEvent
import com.d4rk.cleaner.app.clean.duplicates.domain.data.model.ui.UiDuplicatesModel
import com.d4rk.cleaner.app.clean.duplicates.ui.components.DuplicateGroupsSection
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

private const val TAG = "DuplicatesScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(activity: DuplicateCleanerActivity) {
    val viewModel: DuplicatesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val view: View = LocalView.current

    LargeTopAppBarWithScaffold(
        title = stringResource(id = R.string.duplicates),
        onBackClicked = { activity.finish() }) { padding ->
        ScreenStateHandler(
            screenState = uiState,
            onLoading = {
                if (uiState.data?.cleaningState == CleaningState.Cleaning) {
                    CleaningAnimationScreen()
                } else {
                    LoadingScreen()
                    Log.d(TAG, "LoadingScreen")
                }
            },
            onEmpty = {
                Log.d(TAG, "onEmpty")
                NoDataScreen(textMessage = R.string.no_duplicates, icon = Icons.Outlined.Folder)
            },
            onSuccess = { data: UiDuplicatesModel ->
                Log.d(TAG, "onSuccess")
                ConstraintLayout(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                ) {
                    val (list, buttons) = createRefs()
                    val enabled = data.selectedFileCount > 0 &&
                        data.cleaningState != CleaningState.Cleaning &&
                        data.cleaningState != CleaningState.Error
                    DuplicateGroupsSection(
                        modifier = Modifier.constrainAs(list) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(buttons.top)
                            width = Dimension.fillToConstraints
                            height = Dimension.fillToConstraints
                        },
                        filesByDate = data.duplicatesByDate,
                        fileSelectionStates = data.fileSelectionStates.mapKeys { File(it.key) },
                        onFileSelectionChange = { file, checked ->
                            viewModel.onEvent(DuplicatesEvent.OnFileSelectionChange(file, checked))
                        },
                        onDateSelectionChange = { files, checked ->
                            files.forEach {
                                viewModel.onEvent(DuplicatesEvent.OnFileSelectionChange(it, checked))
                            }
                        },
                        view = view
                    )

                    OutlinedIconButtonWithText(
                        modifier = Modifier
                            .padding(all = SizeConstants.LargeSize)
                            .constrainAs(buttons) {
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                width = Dimension.fillToConstraints
                            },
                        onClick = {
                            viewModel.onEvent(event = DuplicatesEvent.DeleteSelectedFiles)
                        },
                        enabled = enabled,
                        icon = Icons.Outlined.DeleteForever,
                        iconContentDescription = stringResource(id = R.string.move_to_trash_icon_description),
                        label = stringResource(id = R.string.delete_forever)
                    )
                }
            })
    }
}

