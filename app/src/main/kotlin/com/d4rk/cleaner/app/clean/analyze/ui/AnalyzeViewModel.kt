package com.d4rk.cleaner.app.clean.analyze.ui

import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import kotlinx.coroutines.flow.collectLatest
import java.io.File

/**
 * ViewModel for the Analyze screen.
 *
 * This ViewModel mirrors the state from [ScannerViewModel] and exposes the
 * necessary operations for the Analyze UI. It relies on Kotlin [flows]
 * to keep the UI updated following Google's recommended architecture.
 */
class AnalyzeViewModel(
    private val scannerViewModel: ScannerViewModel
) : ScreenViewModel<UiScannerModel, AnalyzeEvent, AnalyzeAction>(
    initialState = UiStateScreen(data = UiScannerModel())
) {

    init {
        // Observe scanner state and update local screen state
        launch {
            scannerViewModel.uiState.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Delegates [ScannerEvent]s to the underlying [ScannerViewModel].
     */
    fun onScannerEvent(event: ScannerEvent) {
        scannerViewModel.onEvent(event)
    }

    fun onFileSelectionChange(file: File, isChecked: Boolean) {
        scannerViewModel.onFileSelectionChange(file, isChecked)
    }

    fun toggleSelectFilesForCategory(category: String) {
        scannerViewModel.toggleSelectFilesForCategory(category)
    }

    fun onCloseAnalyzeComposable() {
        scannerViewModel.onCloseAnalyzeComposable()
    }

    fun setMoveToTrashConfirmationDialogVisibility(isVisible: Boolean) {
        scannerViewModel.setMoveToTrashConfirmationDialogVisibility(isVisible)
    }

    fun setDeleteForeverConfirmationDialogVisibility(isVisible: Boolean) {
        scannerViewModel.setDeleteForeverConfirmationDialogVisibility(isVisible)
    }

    fun resetAfterError() {
        scannerViewModel.resetAfterError()
    }

    override fun onEvent(event: AnalyzeEvent) {
        when (event) {
            is AnalyzeEvent.ScannerEventWrapper -> scannerViewModel.onEvent(event.event)
            AnalyzeEvent.DismissSnackbar -> screenState.dismissSnackbar()
        }
    }
}

sealed interface AnalyzeEvent {
    data class ScannerEventWrapper(val event: ScannerEvent) : AnalyzeEvent
    data object DismissSnackbar : AnalyzeEvent
}

sealed interface AnalyzeAction
