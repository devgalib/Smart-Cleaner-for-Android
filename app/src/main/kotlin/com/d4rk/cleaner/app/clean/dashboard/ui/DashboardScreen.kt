package com.d4rk.cleaner.app.clean.dashboard.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.animations.rememberAnimatedVisibilityState
import com.d4rk.cleaner.Cleaner
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.apps.manager.ui.AppManagerViewModel
import com.d4rk.cleaner.app.clean.dashboard.ui.components.RenderDashboardItem
import com.d4rk.cleaner.core.utils.helpers.LogHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import java.io.File

private const val TAG = LogHelper.DASHBOARD_SCREEN

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
) {
    val appManagerViewModel: AppManagerViewModel = koinViewModel()
    val context: Context = LocalContext.current

    val mrecAdsConfig: AdsConfig = koinInject(qualifier = named("banner_medium_rectangle"))
    val largeBannerAdsConfig: AdsConfig = koinInject(qualifier = named("large_banner"))
    val leaderboardAdsConfig: AdsConfig = koinInject(qualifier = named("leaderboard"))
    val bannerAdsConfig: AdsConfig = koinInject()

    val appManagerState: UiStateScreen<UiAppManagerModel> by appManagerViewModel.uiState.collectAsStateWithLifecycle()
    val scannerUiState by dashboardViewModel.scannerUiState.collectAsStateWithLifecycle()
    val whatsappSummary by dashboardViewModel.whatsAppMediaSummary.collectAsStateWithLifecycle()
    val whatsappInstalled by dashboardViewModel.isWhatsAppInstalled.collectAsStateWithLifecycle()
    val clipboardText by dashboardViewModel.clipboardPreview.collectAsStateWithLifecycle()
    val largeFilesState by dashboardViewModel.largestFilesState.collectAsStateWithLifecycle()
    val duplicateGroupsState = scannerUiState.data?.analyzeState?.duplicateGroups
    val duplicateGroups = remember(duplicateGroupsState) {
        duplicateGroupsState?.map { group ->
            group.map { File(it.path) }
        } ?: emptyList()
    }
    val emptyFolders by dashboardViewModel.emptyFolders.collectAsStateWithLifecycle()
    val streakDays by dashboardViewModel.cleanStreak.collectAsStateWithLifecycle()
    val streakRecord by dashboardViewModel.streakRecord.collectAsStateWithLifecycle()
    val cleaningApks by dashboardViewModel.cleaningApks.collectAsStateWithLifecycle()

    LaunchedEffect(whatsappInstalled, whatsappSummary.hasData) {
        if (whatsappInstalled && !whatsappSummary.hasData) {
            Log.i(TAG, "WhatsApp installed but no media found")
        }
    }

    val storageManagerIntent = remember {
        Intent("android.os.storage.action.MANAGE_STORAGE").setPackage("com.android.storagemanager")
    }
    val showSystemStorageManagerCard = remember {
        storageManagerIntent.resolveActivity(context.packageManager) != null
    }
    val gmsAvailable = remember {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    val sessionDuration = remember { (System.currentTimeMillis() - Cleaner.sessionStartTime) / 1000L }

    LaunchedEffect(Unit) {
        dashboardViewModel.observeDashboardUiState(
            appManagerStateFlow = appManagerViewModel.uiState,
            sessionDurationSeconds = sessionDuration,
            promotedApp = scannerUiState.data?.promotedApp,
            showSystemStorageManagerCard = showSystemStorageManagerCard,
            gmsAvailable = gmsAvailable
        )
    }
    val dashboardState: UiStateScreen<DashboardUiState> by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val promotedApp = scannerUiState.data?.promotedApp
    val items = dashboardState.data?.items ?: emptyList()
    val visibleCount = dashboardState.data?.visibleCount ?: 0

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
            uiState = scannerUiState,
            viewModel = dashboardViewModel,
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
