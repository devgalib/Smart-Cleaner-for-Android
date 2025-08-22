package com.d4rk.cleaner.app.clean.dashboard.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.ads.AdBanner
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.animateVisibility
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.apps.manager.domain.data.model.ui.UiAppManagerModel
import com.d4rk.cleaner.app.clean.contacts.ui.ContactsCleanerActivity
import com.d4rk.cleaner.app.clean.contacts.ui.components.ContactsCleanerCard
import com.d4rk.cleaner.app.clean.dashboard.ui.ContentCard
import com.d4rk.cleaner.app.clean.dashboard.ui.DashboardItemBuilder
import com.d4rk.cleaner.app.clean.dashboard.ui.HomeItem
import com.d4rk.cleaner.app.clean.duplicates.ui.DuplicateCleanerActivity
import com.d4rk.cleaner.app.clean.largefiles.ui.LargeFilesActivity
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.PromotedApp
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.ui.ScannerViewModel
import com.d4rk.cleaner.app.clean.scanner.ui.components.ApkCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.ClipboardCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.DuplicateFilesCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.EmptyFolderCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.ImageOptimizerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.LargeFilesCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.LinkCleanerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.PromotedAppCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.QuickScanSummaryCard
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.WhatsAppMediaSummary
import com.d4rk.cleaner.app.clean.scanner.ui.components.SystemStorageManagerCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.WeeklyCleanStreakCard
import com.d4rk.cleaner.app.clean.scanner.ui.components.WhatsAppCleanerCard
import com.d4rk.cleaner.app.clean.whatsapp.summary.ui.WhatsAppCleanerActivity
import com.d4rk.cleaner.app.images.picker.ui.ImagePickerActivity
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.IntentsHelper
import com.d4rk.cleaner.core.domain.model.network.Errors
import java.io.File

@Composable
fun RenderDashboardItem(
    modifier: Modifier = Modifier,
    item: HomeItem,
    index: Int,
    visibilityStates: SnapshotStateList<Boolean>,
    uiState: UiStateScreen<UiScannerModel>,
    viewModel: ScannerViewModel,
    context: Context,
    storageManagerIntent: Intent,
    streakDays: Int,
    streakRecord: Int,
    largeFilesState: DataState<List<File>, Errors>,
    duplicateGroups: List<List<File>>,
    whatsappSummary: WhatsAppMediaSummary,
    clipboardText: String?,
    appManagerState: UiStateScreen<UiAppManagerModel>,
    cleaningApks: Boolean,
    emptyFolders: List<File>,
    mrecAdsConfig: AdsConfig,
    largeBannerAdsConfig: AdsConfig,
    leaderboardAdsConfig: AdsConfig,
    bannerAdsConfig: AdsConfig,
    visibleCount: Int,
    promotedApp: PromotedApp?
) {
    when (item) {
        is HomeItem.Card -> RenderContentCard(
            modifier = modifier,
            card = item.type,
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
            promotedApp = promotedApp
        )
        is HomeItem.Ad -> {
            val config = DashboardItemBuilder.chooseAdsConfigFor(
                slot = item.slot,
                visibleContent = visibleCount,
                hasPromoted = promotedApp != null,
                mrecAdsConfig = mrecAdsConfig,
                largeBannerAdsConfig = largeBannerAdsConfig,
                leaderboardAdsConfig = leaderboardAdsConfig,
                bannerAdsConfig = bannerAdsConfig
            )
            AdBanner(
                modifier = Modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                adsConfig = config
            )
        }
    }
}

@Composable
private fun RenderContentCard(
    modifier : Modifier = Modifier,
    card: ContentCard,
    index: Int,
    visibilityStates: SnapshotStateList<Boolean>,
    uiState: UiStateScreen<UiScannerModel>,
    viewModel: ScannerViewModel,
    context: Context,
    storageManagerIntent: Intent,
    streakDays: Int,
    streakRecord: Int,
    largeFilesState: DataState<List<File>, Errors>,
    duplicateGroups: List<List<File>>,
    whatsappSummary: WhatsAppMediaSummary,
    clipboardText: String?,
    appManagerState: UiStateScreen<UiAppManagerModel>,
    cleaningApks: Boolean,
    emptyFolders: List<File>,
    promotedApp: PromotedApp?
) {
    when (card) {
        ContentCard.QUICK_SCAN -> {
            QuickScanSummaryCard(
                modifier = modifier
                    .animateVisibility(
                        visible = visibilityStates.getOrElse(index) { false },
                        index = index
                    )
                    .animateContentSize(),
                cleanedSize = uiState.data?.storageInfo?.cleanedSpace ?: "",
                freePercent = uiState.data?.storageInfo?.freeSpacePercentage ?: 0,
                usedPercent = ((uiState.data?.storageInfo?.storageUsageProgress ?: 0f) * 100).toInt(),
                progress = uiState.data?.storageInfo?.storageUsageProgress ?: 0f,
                onQuickScanClick = {
                    viewModel.onEvent(
                        event = ScannerEvent.ToggleAnalyzeScreen(true)
                    )
                }
            )
        }

        ContentCard.SYSTEM_STORAGE -> {
            SystemStorageManagerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                onOpen = {
                    if (storageManagerIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(storageManagerIntent)
                    } else {
                        Toast.makeText(
                            context,
                            R.string.no_application_found,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        ContentCard.WEEKLY_STREAK -> {
            WeeklyCleanStreakCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                streakDays = streakDays,
                streakRecord = streakRecord,
                onDismiss = {
                    viewModel.onEvent(
                        ScannerEvent.SetHideStreakDialogVisibility(true)
                    )
                }
            )
        }

        ContentCard.LARGE_FILES -> {
            LargeFilesCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                state = largeFilesState,
                onOpenClick = {
                    IntentsHelper.openActivity(
                        context = context,
                        activityClass = LargeFilesActivity::class.java
                    )
                }
            )
        }

        ContentCard.DUPLICATES -> {
            DuplicateFilesCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                duplicateGroups = duplicateGroups,
                onOpenClick = {
                    IntentsHelper.openActivity(
                        context = context,
                        activityClass = DuplicateCleanerActivity::class.java
                    )
                }
            )
        }

        ContentCard.WHATSAPP -> {
            if (whatsappSummary.hasData) {
                WhatsAppCleanerCard(
                    modifier = modifier.animateVisibility(
                        visible = visibilityStates.getOrElse(index) { false },
                        index = index
                    ),
                    mediaSummary = whatsappSummary,
                    onCleanClick = {
                        IntentsHelper.openActivity(
                            context = context,
                            activityClass = WhatsAppCleanerActivity::class.java
                        )
                    }
                )
            } else {
                DashboardActionCard(
                    modifier = modifier.animateVisibility(
                        visible = visibilityStates.getOrElse(index) { false },
                        index = index
                    ),
                    icon = Icons.Filled.Whatsapp,
                    title = stringResource(id = R.string.whatsapp_card_title),
                    subtitle = stringResource(id = R.string.whatsapp_cleaner_empty_message),
                    actionLabel = stringResource(id = R.string.open_whatsapp_cleaner),
                    actionPainter = painterResource(id = R.drawable.ic_folder_search),
                    onActionClick = {
                        IntentsHelper.openActivity(
                            context = context,
                            activityClass = WhatsAppCleanerActivity::class.java
                        )
                    },
                    onHeaderClick = {
                        IntentsHelper.openActivity(
                            context = context,
                            activityClass = WhatsAppCleanerActivity::class.java
                        )
                    }
                )
            }
        }

        ContentCard.APK -> {
            val isCleaningApks = cleaningApks && uiState.data?.analyzeState?.state == CleaningState.Cleaning
            ApkCleanerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                apkFiles = appManagerState.data?.apkFiles ?: emptyList(),
                isLoading = isCleaningApks,
                onCleanClick = { selected ->
                    val files = selected.map { File(it.path) }
                    viewModel.onCleanApks(files)
                }
            )
        }

        ContentCard.EMPTY_FOLDERS -> {
            EmptyFolderCleanerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                folders = emptyFolders,
                onCleanClick = { viewModel.onCleanEmptyFolders(it) }
            )
        }

        ContentCard.CLIPBOARD -> {
            ClipboardCleanerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                clipboardText = clipboardText,
                onCleanClick = { viewModel.onClipboardClear() }
            )
        }

        ContentCard.CONTACTS -> {
            ContactsCleanerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                onOpen = {
                    IntentsHelper.openActivity(
                        context = context,
                        activityClass = ContactsCleanerActivity::class.java
                    )
                }
            )
        }

        ContentCard.LINK_CLEANER -> {
            LinkCleanerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                )
            )
        }

        ContentCard.IMAGE_OPTIMIZER -> {
            ImageOptimizerCard(
                modifier = modifier.animateVisibility(
                    visible = visibilityStates.getOrElse(index) { false },
                    index = index
                ),
                onOptimizeClick = {
                    IntentsHelper.openActivity(
                        context = context,
                        activityClass = ImagePickerActivity::class.java
                    )
                }
            )
        }

        ContentCard.PROMOTED_APP -> {
            promotedApp?.let { app ->
                PromotedAppCard(
                    modifier = modifier.animateVisibility(
                        visible = visibilityStates.getOrElse(index) { false },
                        index = index
                    ),
                    app = app
                )
            }
        }
    }
}

