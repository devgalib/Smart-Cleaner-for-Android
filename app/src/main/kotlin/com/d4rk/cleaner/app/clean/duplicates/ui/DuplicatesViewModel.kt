package com.d4rk.cleaner.app.clean.duplicates.ui

import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.updateData
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.duplicates.domain.actions.DuplicatesAction
import com.d4rk.cleaner.app.clean.duplicates.domain.actions.DuplicatesEvent
import com.d4rk.cleaner.app.clean.duplicates.domain.data.model.ui.UiDuplicatesModel
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.`interface`.ScannerRepositoryInterface
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetDuplicatesUseCase
import com.d4rk.cleaner.core.utils.extensions.selectedFiles
import com.d4rk.cleaner.core.utils.helpers.FileGroupingHelper
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import kotlinx.coroutines.flow.update
import java.io.File

class DuplicatesViewModel(
    private val repository: ScannerRepositoryInterface,
    private val getDuplicatesUseCase: GetDuplicatesUseCase,
    private val dispatchers: DispatcherProvider,
) : ScreenViewModel<UiDuplicatesModel, DuplicatesEvent, DuplicatesAction>(
    initialState = UiStateScreen(data = UiDuplicatesModel())
) {
    init {
        onEvent(DuplicatesEvent.LoadDuplicates)
    }

    override fun onEvent(event: DuplicatesEvent) {
        when (event) {
            DuplicatesEvent.LoadDuplicates -> loadDuplicates()
            is DuplicatesEvent.OnFileSelectionChange -> onFileSelectionChange(event.file, event.isChecked)
            DuplicatesEvent.DeleteSelectedFiles -> deleteSelected()
        }
    }

    private fun loadDuplicates() {
        launch(dispatchers.io) {
            val grouped = getDuplicatesUseCase(repository.getAllFiles())
            val sortedGroups = grouped.map { it.sortedBy { entry -> entry.modified } }
            val duplicates = sortedGroups.map { it.map { File(it.path) } }
            val originals = duplicates.map { it.first() }.toSet()
            val groupedByDate = FileGroupingHelper.groupDuplicateGroupsByDate(sortedGroups)
            _uiState.update { current ->
                current.copy(
                    screenState = if (duplicates.isEmpty()) ScreenState.NoData() else ScreenState.Success(),
                    data = UiDuplicatesModel(
                        duplicateGroups = duplicates,
                        duplicatesByDate = groupedByDate,
                        originals = originals,
                        fileSelectionStates = emptyMap(),
                        selectedFileCount = 0,
                        cleaningState = CleaningState.Idle,
                    )
                )
            }
        }
    }

    private fun onFileSelectionChange(file: File, isChecked: Boolean) {
        _uiState.updateData(newState = _uiState.value.screenState) { current ->
            val updated = current.fileSelectionStates.toMutableMap().apply { this[file.absolutePath] = isChecked }
            current.copy(
                fileSelectionStates = updated,
                selectedFileCount = updated.count { it.value && !File(it.key).isProtectedAndroidDir() }
            )
        }
    }

    private fun deleteSelected() {
        launch(dispatchers.io) {
            val selection = _uiState.value.data?.fileSelectionStates ?: emptyMap()
            val files = selection.selectedFiles()
            if (files.isEmpty()) {
                val resId = if (selection.any { it.value }) {
                    R.string.protected_android_folder
                } else {
                    R.string.no_files_selected_to_delete
                }
                sendAction(
                    DuplicatesAction.ShowSnackbar(
                        UiSnackbar(
                            message = UiTextHelper.StringResource(resId)
                        )
                    )
                )
                return@launch
            }
            try {
                _uiState.updateData(ScreenState.IsLoading()) { current ->
                    current.copy(cleaningState = CleaningState.Cleaning)
                }
                repository.deleteFiles(files)
                _uiState.updateData(ScreenState.Success()) { current ->
                    current.copy(
                        fileSelectionStates = emptyMap(),
                        selectedFileCount = 0,
                        cleaningState = CleaningState.Result
                    )
                }
                loadDuplicates()
                sendAction(
                    DuplicatesAction.ShowSnackbar(
                        UiSnackbar(message = UiTextHelper.StringResource(R.string.all_clean))
                    )
                )
            } catch (e: Exception) {
                _uiState.updateData(ScreenState.Error()) { current ->
                    current.copy(cleaningState = CleaningState.Error)
                }
                sendAction(
                    DuplicatesAction.ShowSnackbar(
                        UiSnackbar(
                            message = UiTextHelper.DynamicString("${e.message}"),
                            isError = true
                        )
                    )
                )
            }
        }
    }
}

