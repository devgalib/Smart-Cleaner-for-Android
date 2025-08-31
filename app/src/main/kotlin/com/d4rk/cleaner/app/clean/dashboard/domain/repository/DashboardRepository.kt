package com.d4rk.cleaner.app.clean.dashboard.domain.repository

import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.clean.dashboard.ui.DashboardItemBuilder
import com.d4rk.cleaner.app.clean.dashboard.ui.DashboardParams
import com.d4rk.cleaner.app.clean.dashboard.ui.DashboardUiState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.PromotedApp
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.domain.model.network.Errors
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class DashboardRepository(
    private val dataStore: DataStore,
    private val dispatchers: DispatcherProvider
) {
    fun dashboardUiState(
        appManagerStateFlow: Flow<UiStateScreen<UiAppManagerModel>>,
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
    ): Flow<DashboardUiState> {
        val adsStateFlow = dataStore.ads(default = true)
        return combine(
            appManagerStateFlow,
            showStreakCard,
            largestFilesState,
            showDuplicateCard,
            whatsAppMediaLoaded,
            isWhatsAppInstalled,
            clipboardPreview,
            emptyFolders,
            emptyFoldersHideUntil,
            adsStateFlow
        ) { values ->
            val appManagerState = values[0] as UiStateScreen<UiAppManagerModel> // FIXME: Unchecked cast of 'Any?' to 'UiStateScreen<UiAppManagerModel>'.
            val showStreak = values[1] as Boolean
            val largeFilesState = values[2] as DataState<List<File>, *> // FIXME: Unchecked cast of 'Any?' to 'DataState<List<File>, *>'.
            val showDuplicate = values[3] as Boolean
            val whatsAppLoaded = values[4] as Boolean
            val whatsAppInstalled = values[5] as Boolean
            val clipboard = values[6] as String?
            val emptyFoldersList = values[7] as List<File> // FIXME: Unchecked cast of 'Any?' to 'List<File>'.
            val hideUntil = values[8] as Long
            val adsState = values[9] as Boolean

            val showApkCard = appManagerState.data?.apkFilesLoading == false &&
                appManagerState.data?.apkFiles?.isNotEmpty() == true
            val showWhatsAppCard = whatsAppLoaded && whatsAppInstalled
            val showClipboardCard = !clipboard.isNullOrBlank()
            val showLargeFilesCard = when (largeFilesState) {
                is DataState.Success -> largeFilesState.data.isNotEmpty()
                else -> true
            }
            val showEmptyFoldersCard = emptyFoldersList.isNotEmpty() &&
                hideUntil <= System.currentTimeMillis()
            val dataLoaded = appManagerState.data?.apkFilesLoading == false && whatsAppLoaded
            val params = DashboardParams(
                showSystemStorageManagerCard = showSystemStorageManagerCard,
                showStreakCard = showStreak,
                showLargeFilesCard = showLargeFilesCard,
                showDuplicateCard = showDuplicate,
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
            DashboardItemBuilder.build(params, dispatchers.default)
        }
            .catch { emit(DashboardUiState(emptyList(), 0)) }
            .flowOn(dispatchers.default)
    }
}

