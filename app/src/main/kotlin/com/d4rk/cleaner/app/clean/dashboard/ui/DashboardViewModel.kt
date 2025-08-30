package com.d4rk.cleaner.app.clean.dashboard.ui

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.clean.dashboard.domain.actions.DashboardAction
import com.d4rk.cleaner.app.clean.dashboard.domain.actions.DashboardEvent
import com.d4rk.cleaner.app.clean.dashboard.domain.repository.DashboardRepository
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.PromotedApp
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.WhatsAppMediaSummary
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import com.d4rk.cleaner.core.domain.model.network.Errors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class DashboardViewModel(
    private val repository: DashboardRepository,
    private val scannerViewModel: ScannerViewModel
) : ScreenViewModel<DashboardUiState, DashboardEvent, DashboardAction>(
    initialState = UiStateScreen(data = DashboardUiState(emptyList(), 0))
) {

    /** Exposed flows from [ScannerViewModel] to keep the dashboard screen independent. */
    val scannerUiState: StateFlow<UiStateScreen<UiScannerModel>> = scannerViewModel.uiState
    val clipboardPreview: StateFlow<String?> = scannerViewModel.clipboardPreview
    val cleanStreak: StateFlow<Int> = scannerViewModel.cleanStreak
    val streakRecord: StateFlow<Int> = scannerViewModel.streakRecord
    val showStreakCard: StateFlow<Boolean> = scannerViewModel.showStreakCard
    val largestFilesState: StateFlow<DataState<List<File>, Errors>> = scannerViewModel.largestFilesState
    val showDuplicateCard: StateFlow<Boolean> = scannerViewModel.showDuplicateCard
    val whatsAppMediaSummary: StateFlow<WhatsAppMediaSummary> = scannerViewModel.whatsAppMediaSummary
    val whatsAppMediaLoaded: StateFlow<Boolean> = scannerViewModel.whatsAppMediaLoaded
    val isWhatsAppInstalled: StateFlow<Boolean> = scannerViewModel.isWhatsAppInstalled
    val emptyFolders: StateFlow<List<File>> = scannerViewModel.emptyFolders
    val emptyFoldersHideUntil: StateFlow<Long> = scannerViewModel.emptyFoldersHideUntil
    val cleaningApks: StateFlow<Boolean> = scannerViewModel.cleaningApks

    private var observeJob: Job? = null

    override fun onEvent(event: DashboardEvent) = Unit

    fun observeDashboardUiState(
        appManagerStateFlow: StateFlow<UiStateScreen<UiAppManagerModel>>,
        sessionDurationSeconds: Long,
        promotedApp: PromotedApp?,
        showSystemStorageManagerCard: Boolean,
        gmsAvailable: Boolean
    ) {
        observeJob?.cancel()
        observeJob = launch {
            repository.dashboardUiState(
                appManagerStateFlow = appManagerStateFlow,
                showStreakCard = showStreakCard,
                largestFilesState = largestFilesState,
                showDuplicateCard = showDuplicateCard,
                whatsAppMediaLoaded = whatsAppMediaLoaded,
                isWhatsAppInstalled = isWhatsAppInstalled,
                clipboardPreview = clipboardPreview,
                emptyFolders = emptyFolders,
                emptyFoldersHideUntil = emptyFoldersHideUntil,
                sessionDurationSeconds = sessionDurationSeconds,
                promotedApp = promotedApp,
                showSystemStorageManagerCard = showSystemStorageManagerCard,
                gmsAvailable = gmsAvailable
            ).collect { state ->
                _uiState.update { current ->
                    current.copy(
                        data = state,
                        screenState = ScreenState.Success()
                    )
                }
            }
        }
    }

    fun onCleanApks(apkFiles: List<File>) {
        scannerViewModel.onCleanApks(apkFiles)
    }

    fun onCleanEmptyFolders(folders: List<File>) {
        scannerViewModel.onCleanEmptyFolders(folders)
    }

    fun onClipboardClear() {
        scannerViewModel.onClipboardClear()
    }

    fun onScannerEvent(event: ScannerEvent) {
        scannerViewModel.onEvent(event)
    }
}

