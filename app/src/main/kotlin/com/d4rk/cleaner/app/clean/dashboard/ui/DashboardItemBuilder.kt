package com.d4rk.cleaner.app.clean.dashboard.ui

import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig

enum class AdSlot { A, B, C, D, E, F }

enum class ContentCard {
    QUICK_SCAN,
    SYSTEM_STORAGE,
    WEEKLY_STREAK,
    LARGE_FILES,
    DUPLICATES,
    WHATSAPP,
    APK,
    EMPTY_FOLDERS,
    CLIPBOARD,
    CONTACTS,
    LINK_CLEANER,
    IMAGE_OPTIMIZER,
    PROMOTED_APP
}

sealed interface HomeItem {
    data class Card(val type: ContentCard) : HomeItem
    data class Ad(val slot: AdSlot) : HomeItem
}

data class DashboardParams(
    val showSystemStorageManagerCard: Boolean,
    val showStreakCard: Boolean,
    val showLargeFilesCard: Boolean,
    val showDuplicateCard: Boolean,
    val showWhatsAppCard: Boolean,
    val showApkCard: Boolean,
    val showEmptyFoldersCard: Boolean,
    val showClipboardCard: Boolean,
    val showContactsCard: Boolean,
    val showLinkCleanerCard: Boolean,
    val promotedApp: Any?,
    val adsState: Boolean,
    val gmsAvailable: Boolean,
    val dataLoaded: Boolean,
    val sessionDurationSeconds: Long
)

data class DashboardItemsResult(
    val items: List<HomeItem>,
    val visibleCount: Int
)

object DashboardItemBuilder {
    fun build(params: DashboardParams): DashboardItemsResult {
        val content = mutableListOf<Pair<Int, ContentCard>>().apply {
            add(1 to ContentCard.QUICK_SCAN)
            if (params.showSystemStorageManagerCard) add(2 to ContentCard.SYSTEM_STORAGE)
            if (params.showStreakCard) add(3 to ContentCard.WEEKLY_STREAK)
            if (params.showLargeFilesCard) add(4 to ContentCard.LARGE_FILES)
            if (params.showDuplicateCard) add(5 to ContentCard.DUPLICATES)
            if (params.showWhatsAppCard) add(6 to ContentCard.WHATSAPP)
            if (params.showApkCard) add(7 to ContentCard.APK)
            if (params.showEmptyFoldersCard) add(8 to ContentCard.EMPTY_FOLDERS)
            if (params.showClipboardCard) add(9 to ContentCard.CLIPBOARD)
            if (params.showContactsCard) add(10 to ContentCard.CONTACTS)
            if (params.showLinkCleanerCard) add(11 to ContentCard.LINK_CLEANER)
            add(12 to ContentCard.IMAGE_OPTIMIZER)
            if (params.promotedApp != null) add(13 to ContentCard.PROMOTED_APP)
        }
        val visibleCount = content.size

        val sessionSlots = when {
            params.sessionDurationSeconds < 60 -> listOf(AdSlot.A, AdSlot.C, AdSlot.F)
            params.sessionDurationSeconds < 180 -> listOf(AdSlot.A, AdSlot.B, AdSlot.C, AdSlot.F)
            else -> listOf(AdSlot.A, AdSlot.B, AdSlot.C, AdSlot.D, AdSlot.E, AdSlot.F)
        }
        val maxAds = when {
            params.sessionDurationSeconds < 60 -> 3
            params.sessionDurationSeconds < 180 -> 4
            else -> 5
        }
        val slotsAfterCount = when {
            visibleCount <= 6 -> listOf(AdSlot.C, AdSlot.F)
            visibleCount <= 9 -> {
                val firstAB = listOf(AdSlot.A, AdSlot.B).firstOrNull { sessionSlots.contains(it) }
                listOfNotNull(firstAB, AdSlot.C, AdSlot.F)
            }
            else -> sessionSlots
        }
        val allowedSlots = if (params.adsState && params.gmsAvailable && params.dataLoaded) {
            slotsAfterCount.filter { sessionSlots.contains(it) }.take(maxAds)
        } else emptyList()

        val slotAnchors = mapOf(
            AdSlot.A to 1,
            AdSlot.B to 3,
            AdSlot.C to 5,
            AdSlot.D to 9,
            AdSlot.E to 10,
            AdSlot.F to 11
        )

        val adQueue = allowedSlots
            .filter { slotAnchors[it] != null }
            .sortedBy { slotAnchors.getValue(it) }
            .toMutableList()
        val items = mutableListOf<HomeItem>()
        var lastWasAd = false
        for ((index, card) in content) {
            items.add(HomeItem.Card(card))
            lastWasAd = false
            while (adQueue.isNotEmpty()) {
                val slot = adQueue.first()
                val anchor = slotAnchors[slot] ?: run {
                    adQueue.removeAt(0)
                    continue
                }
                if (anchor <= index) {
                    if (!lastWasAd) {
                        items.add(HomeItem.Ad(adQueue.removeAt(0)))
                        lastWasAd = true
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
        }
        if (adQueue.isNotEmpty() && !lastWasAd) {
            items.add(HomeItem.Ad(adQueue.removeAt(0)))
        }

        return DashboardItemsResult(items, visibleCount)
    }

    fun chooseAdsConfigFor(
        slot: AdSlot,
        visibleContent: Int,
        hasPromoted: Boolean,
        mrecAdsConfig: AdsConfig,
        largeBannerAdsConfig: AdsConfig,
        leaderboardAdsConfig: AdsConfig,
        bannerAdsConfig: AdsConfig
    ): AdsConfig {
        val density = when {
            visibleContent >= 9 -> 2 // dense
            visibleContent >= 7 -> 1 // medium
            else -> 0 // sparse
        }
        val selected: AdsConfig? = when (slot) {
            AdSlot.A, AdSlot.B, AdSlot.C -> if (density == 0) largeBannerAdsConfig else mrecAdsConfig
            AdSlot.D, AdSlot.E -> if (density == 2) mrecAdsConfig else largeBannerAdsConfig
            AdSlot.F -> if (hasPromoted) leaderboardAdsConfig else largeBannerAdsConfig
        }
        return selected ?: bannerAdsConfig
    }
}

