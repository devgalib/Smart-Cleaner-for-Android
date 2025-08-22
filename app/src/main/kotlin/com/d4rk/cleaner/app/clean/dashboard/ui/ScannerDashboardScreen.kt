package com.d4rk.cleaner.app.clean.dashboard.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.animations.rememberAnimatedVisibilityState
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.Cleaner
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.apps.manager.ui.AppManagerViewModel
import com.d4rk.cleaner.app.clean.dashboard.ui.components.RenderDashboardItem
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.helpers.LogHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
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

    val promotedApp = uiState.data?.promotedApp
    val mrecAdsConfig: AdsConfig = koinInject(qualifier = named("banner_medium_rectangle"))
    val largeBannerAdsConfig: AdsConfig = koinInject(qualifier = named("large_banner"))
    val leaderboardAdsConfig: AdsConfig = koinInject(qualifier = named("leaderboard"))
    val bannerAdsConfig: AdsConfig = koinInject()

    val appManagerState: UiStateScreen<UiAppManagerModel> by appManagerViewModel.uiState.collectAsState()
    val whatsappSummary by viewModel.whatsAppMediaSummary.collectAsState()
    val whatsappLoaded by viewModel.whatsAppMediaLoaded.collectAsState()
    val whatsappInstalled by viewModel.isWhatsAppInstalled.collectAsState()
    val clipboardText by viewModel.clipboardPreview.collectAsState()
    val largeFilesState by viewModel.largestFilesState.collectAsState()
    val duplicateGroups = uiState.data?.analyzeState?.duplicateGroups?.map { group ->
        group.map { File(it.path) }
    } ?: emptyList()
    val showDuplicateCard by viewModel.showDuplicateCard.collectAsState()
    val emptyFolders by viewModel.emptyFolders.collectAsState()
    val emptyFoldersHideUntil by viewModel.emptyFoldersHideUntil.collectAsState()
    val streakDays by viewModel.cleanStreak.collectAsState()
    val streakRecord by viewModel.streakRecord.collectAsState()
    val showStreakCard by viewModel.showStreakCard.collectAsState()
    val cleaningApks by viewModel.cleaningApks.collectAsState()

    val dataStore: DataStore = koinInject()
    val adsState: Boolean by remember { dataStore.ads(default = true) }.collectAsState(initial = true)

    val showApkCard by remember(appManagerState) {
        derivedStateOf {
            appManagerState.data?.apkFilesLoading == false &&
                    appManagerState.data?.apkFiles?.isNotEmpty() == true
        }
    }
    val showWhatsAppCard by remember(whatsappLoaded, whatsappInstalled) {
        derivedStateOf { whatsappLoaded && whatsappInstalled }
    }
    LaunchedEffect(whatsappInstalled, whatsappSummary.hasData) {
        if (whatsappInstalled && !whatsappSummary.hasData) {
            Log.i(TAG, "WhatsApp installed but no media found")
        }
    }
    val showClipboardCard by remember(clipboardText) {
        derivedStateOf { !clipboardText.isNullOrBlank() }
    }
    val showLinkCleanerCard = true
    val showLargeFilesCard by remember(largeFilesState) {
        derivedStateOf {
            val state = largeFilesState
            if (state is DataState.Success) state.data.isNotEmpty() else true
        }
    }
    val showEmptyFoldersCard by remember(emptyFolders, emptyFoldersHideUntil) {
        derivedStateOf {
            emptyFolders.isNotEmpty() && emptyFoldersHideUntil <= System.currentTimeMillis()
        }
    }
    val showContactsCard = true
    val storageManagerIntent = remember {
        Intent("android.os.storage.action.MANAGE_STORAGE").setPackage("com.android.storagemanager")
    }
    val showSystemStorageManagerCard by remember {
        derivedStateOf {
            storageManagerIntent.resolveActivity(context.packageManager) != null
        }
    }

    val dataLoaded = appManagerState.data?.apkFilesLoading == false && whatsappLoaded
    val gmsAvailable = remember {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    val sessionDuration =
        remember { (System.currentTimeMillis() - Cleaner.sessionStartTime) / 1000L }

    val params = remember(
        showSystemStorageManagerCard,
        showStreakCard,
        showLargeFilesCard,
        showDuplicateCard,
        showWhatsAppCard,
        showApkCard,
        showEmptyFoldersCard,
        showClipboardCard,
        showContactsCard,
        showLinkCleanerCard,
        promotedApp,
        adsState,
        gmsAvailable,
        dataLoaded,
        sessionDuration
    ) {
        DashboardParams(
            showSystemStorageManagerCard = showSystemStorageManagerCard,
            showStreakCard = showStreakCard,
            showLargeFilesCard = showLargeFilesCard,
            showDuplicateCard = showDuplicateCard,
            showWhatsAppCard = showWhatsAppCard,
            showApkCard = showApkCard,
            showEmptyFoldersCard = showEmptyFoldersCard,
            showClipboardCard = showClipboardCard,
            showContactsCard = showContactsCard,
            showLinkCleanerCard = showLinkCleanerCard,
            promotedApp = promotedApp,
            adsState = adsState,
            gmsAvailable = gmsAvailable,
            dataLoaded = dataLoaded,
            sessionDurationSeconds = sessionDuration
        )
    }

    val result = remember(params) { DashboardItemBuilder.build(params) }
    val items = result.items
    val visibleCount = result.visibleCount

    val listState = rememberLazyListState()

    val (visibilityStates: SnapshotStateList<Boolean>) = rememberAnimatedVisibilityState(
        listState = listState,
        itemCount = items.size
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space = SizeConstants.LargeSize),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = listState,
        contentPadding = PaddingValues(SizeConstants.LargeSize)
    ) {
        items(items.size) { index ->
            val item = items[index]
            RenderDashboardItem(
                modifier = Modifier.animateItem(),
                item = item,
                index = index,
                visibilityStates = visibilityStates,
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
}