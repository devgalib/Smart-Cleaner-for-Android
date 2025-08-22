package com.d4rk.cleaner.app.clean.dashboard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardItemBuilderTest {

    @Test
    fun `content cards are ordered correctly`() {
        val params = DashboardParams(
            showSystemStorageManagerCard = true,
            showStreakCard = true,
            showLargeFilesCard = true,
            showDuplicateCard = true,
            showWhatsAppCard = true,
            showApkCard = true,
            showEmptyFoldersCard = true,
            showClipboardCard = true,
            showContactsCard = true,
            showLinkCleanerCard = true,
            promotedApp = Any(),
            adsState = false,
            gmsAvailable = false,
            dataLoaded = false,
            sessionDurationSeconds = 0
        )
        val result = DashboardItemBuilder.build(params)
        val cards = result.items.filterIsInstance<HomeItem.Card>().map { it.type }
        assertEquals(
            listOf(
                ContentCard.QUICK_SCAN,
                ContentCard.SYSTEM_STORAGE,
                ContentCard.WEEKLY_STREAK,
                ContentCard.LARGE_FILES,
                ContentCard.DUPLICATES,
                ContentCard.WHATSAPP,
                ContentCard.APK,
                ContentCard.EMPTY_FOLDERS,
                ContentCard.CLIPBOARD,
                ContentCard.CONTACTS,
                ContentCard.LINK_CLEANER,
                ContentCard.IMAGE_OPTIMIZER,
                ContentCard.PROMOTED_APP
            ),
            cards
        )
    }

    @Test
    fun `ads inserted after content with minimal configuration`() {
        val params = DashboardParams(
            showSystemStorageManagerCard = false,
            showStreakCard = false,
            showLargeFilesCard = false,
            showDuplicateCard = false,
            showWhatsAppCard = false,
            showApkCard = false,
            showEmptyFoldersCard = false,
            showClipboardCard = false,
            showContactsCard = false,
            showLinkCleanerCard = false,
            promotedApp = null,
            adsState = true,
            gmsAvailable = true,
            dataLoaded = true,
            sessionDurationSeconds = 30
        )
        val result = DashboardItemBuilder.build(params)
        assertEquals(2, result.items.size)
        val first = result.items[0] as HomeItem.Card
        val second = result.items[1] as HomeItem.Ad
        assertEquals(ContentCard.QUICK_SCAN, first.type)
        assertEquals(AdSlot.C, second.slot)
    }
}

