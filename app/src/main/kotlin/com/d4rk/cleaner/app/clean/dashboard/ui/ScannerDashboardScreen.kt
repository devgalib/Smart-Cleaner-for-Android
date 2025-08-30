package com.d4rk.cleaner.app.clean.dashboard.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.animations.rememberAnimatedVisibilityState
import com.d4rk.cleaner.Cleaner
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.apps.manager.ui.AppManagerViewModel
import com.d4rk.cleaner.app.clean.dashboard.ui.components.RenderDashboardItem
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import com.d4rk.cleaner.core.utils.helpers.LogHelper
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import java.io.File

private const val TAG = LogHelper.SCANNER_DASHBOARD_SCREEN

@Composable
fun ScannerDashboardScreen(
    uiState: UiStateScreen<UiScannerModel>,
    viewModel: ScannerViewModel,
) {
    val appManagerViewModel: AppManagerViewModel = koinViewModel()
    val context: Context = LocalContext.current

    val mrecAdsConfig: AdsConfig = koinInject(qualifier = named("banner_medium_rectangle"))
    val largeBannerAdsConfig: AdsConfig = koinInject(qualifier = named("large_banner"))
    val leaderboardAdsConfig: AdsConfig = koinInject(qualifier = named("leaderboard"))
    val bannerAdsConfig: AdsConfig = koinInject()

    val appManagerState: UiStateScreen<UiAppManagerModel> by appManagerViewModel.uiState.collectAsState()
    val whatsappSummary by viewModel.whatsAppMediaSummary.collectAsState()
    val whatsappInstalled by viewModel.isWhatsAppInstalled.collectAsState()
    val clipboardText by viewModel.clipboardPreview.collectAsState()
    val largeFilesState by viewModel.largestFilesState.collectAsState()
    val duplicateGroups = uiState.data?.analyzeState?.duplicateGroups?.map { group ->
        group.map { File(it.path) }
    } ?: emptyList()
    val emptyFolders by viewModel.emptyFolders.collectAsState()
    val streakDays by viewModel.cleanStreak.collectAsState()
    val streakRecord by viewModel.streakRecord.collectAsState()
    val cleaningApks by viewModel.cleaningApks.collectAsState()

    LaunchedEffect(whatsappInstalled, whatsappSummary.hasData) {
        if (whatsappInstalled && !whatsappSummary.hasData) {
            Log.i(TAG, "WhatsApp installed but no media found")
        }
    }

    val storageManagerIntent = remember {
        Intent("android.os.storage.action.MANAGE_STORAGE").setPackage("com.android.storagemanager")
    }
    val sessionDuration = remember { (System.currentTimeMillis() - Cleaner.sessionStartTime) / 1000L }

    val dashboardItemsResult by viewModel.dashboardItems(
        context = context,
        appManagerStateFlow = appManagerViewModel.uiState,
        sessionDurationSeconds = sessionDuration
    ).collectAsState(initial = DashboardItemsResult(emptyList(), 0))
    val promotedApp = uiState.data?.promotedApp
    val items = dashboardItemsResult.items
    val visibleCount = dashboardItemsResult.visibleCount

    val listState = rememberLazyListState()

    val (visibilityStates: SnapshotStateList<Boolean>) = rememberAnimatedVisibilityState(
        listState = listState,
        itemCount = items.size
    )

    DashboardItemList(
        items = items,
        visibilityStates = visibilityStates,
        listState = listState
    ) { item, index, visibility ->
        RenderDashboardItem(
            modifier = Modifier.animateItem(),
            item = item,
            index = index,
            visibilityStates = visibility,
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            storageManagerIntent = storageManagerIntent,
            streakDays = streakDays,
            streakRecord = streakRecord,
            largeFilesState = largeFilesState,
            duplicateGroups = duplicateGroups,
            whatsappSummary = whatsappSummary,
            clipboardText = clipboardText,
            appManagerState = appManagerState,
            cleaningApks = cleaningApks,
            emptyFolders = emptyFolders,
            mrecAdsConfig = mrecAdsConfig,
            largeBannerAdsConfig = largeBannerAdsConfig,
            leaderboardAdsConfig = leaderboardAdsConfig,
            bannerAdsConfig = bannerAdsConfig,
            visibleCount = visibleCount,
            promotedApp = promotedApp
        )
    }
}
