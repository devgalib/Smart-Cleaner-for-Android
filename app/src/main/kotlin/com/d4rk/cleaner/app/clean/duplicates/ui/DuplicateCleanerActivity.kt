package com.d4rk.cleaner.app.clean.duplicates.ui

import androidx.compose.runtime.Composable
import com.d4rk.cleaner.core.ui.BaseCleanupActivity

class DuplicateCleanerActivity : BaseCleanupActivity() {
    @Composable
    override fun ScreenContent() {
        DuplicatesScreen(activity = this@DuplicateCleanerActivity)
    }
}

