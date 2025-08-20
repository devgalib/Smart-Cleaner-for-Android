package com.d4rk.cleaner.app.clean.duplicates.domain.actions

import com.d4rk.android.libs.apptoolkit.core.ui.base.handling.UiEvent
import java.io.File

sealed interface DuplicatesEvent : UiEvent {
    data object LoadDuplicates : DuplicatesEvent
    data class OnFileSelectionChange(val file: File, val isChecked: Boolean) : DuplicatesEvent
    data object DeleteSelectedFiles : DuplicatesEvent
}

