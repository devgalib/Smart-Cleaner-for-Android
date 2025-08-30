package com.d4rk.cleaner.app.clean.dashboard.ui

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.clean.dashboard.domain.actions.DashboardAction
import com.d4rk.cleaner.app.clean.dashboard.domain.actions.DashboardEvent
import com.d4rk.cleaner.app.clean.dashboard.domain.repository.DashboardRepository
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.PromotedApp
import com.d4rk.cleaner.core.domain.model.network.Errors
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class DashboardViewModel(
    private val repository: DashboardRepository
) : ScreenViewModel<DashboardUiState, DashboardEvent, DashboardAction>(
    initialState = UiStateScreen(data = DashboardUiState(emptyList(), 0))
) {

    private var observeJob: Job? = null

    override fun onEvent(event: DashboardEvent) = Unit

    fun observeDashboardUiState(
        appManagerStateFlow: StateFlow<UiStateScreen<UiAppManagerModel>>,
        showStreakCard: Flow<Boolean>,
        largestFilesState: Flow<DataState<List<File>, Errors>>,
        showDuplicateCard: Flow<Boolean>,
        whatsAppMediaLoaded: Flow<Boolean>,
        isWhatsAppInstalled: Flow<Boolean>,
        clipboardPreview: Flow<String?>,
        emptyFolders: Flow<List<File>>,
        emptyFoldersHideUntil: Flow<Long>,
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

    fun prepareDashboardParams(
        appManagerState: UiStateScreen<UiAppManagerModel>,
        adsState: Boolean,
        sessionDurationSeconds: Long,
        showStreakCard: Boolean,
        largestFilesState: DataState<List<File>, Errors>,
        showDuplicateCard: Boolean,
        whatsAppMediaLoaded: Boolean,
        isWhatsAppInstalled: Boolean,
        clipboardPreview: String?,
        emptyFolders: List<File>,
        emptyFoldersHideUntil: Long,
        promotedApp: PromotedApp?,
        showSystemStorageManagerCard: Boolean,
        gmsAvailable: Boolean
    ): DashboardParams {
        val showApkCard = appManagerState.data?.apkFilesLoading == false &&
            appManagerState.data?.apkFiles?.isNotEmpty() == true
        val showWhatsAppCard = whatsAppMediaLoaded && isWhatsAppInstalled
        val showClipboardCard = !clipboardPreview.isNullOrBlank()
        val showLargeFilesCard =
            when (largestFilesState) {
                is DataState.Success -> largestFilesState.data.isNotEmpty()
                else -> true
            }
        val showEmptyFoldersCard = emptyFolders.isNotEmpty() &&
            emptyFoldersHideUntil <= System.currentTimeMillis()
        val dataLoaded = appManagerState.data?.apkFilesLoading == false && whatsAppMediaLoaded
        return DashboardParams(
            showSystemStorageManagerCard = showSystemStorageManagerCard,
            showStreakCard = showStreakCard,
            showLargeFilesCard = showLargeFilesCard,
            showDuplicateCard = showDuplicateCard,
            showWhatsAppCard = showWhatsAppCard,
            showApkCard = showApkCard,
            showEmptyFoldersCard = showEmptyFoldersCard,
            showClipboardCard = showClipboardCard,
            showContactsCard = true,
            showLinkCleanerCard = true,
            promotedApp = promotedApp,
            adsState = adsState,
            gmsAvailable = gmsAvailable,
            dataLoaded = dataLoaded,
            sessionDurationSeconds = sessionDurationSeconds
        )
    }


}

