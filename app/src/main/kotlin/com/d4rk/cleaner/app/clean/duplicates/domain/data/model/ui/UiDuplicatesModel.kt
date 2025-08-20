package com.d4rk.cleaner.app.clean.duplicates.domain.data.model.ui

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import java.io.File

data class UiDuplicatesModel(
    val duplicateGroups: List<List<File>> = emptyList(),
    val duplicatesByDate: Map<String, List<List<File>>> = emptyMap(),
    /** Map of file paths to selection state */
    val fileSelectionStates: Map<String, Boolean> = emptyMap(),
    val selectedFileCount: Int = 0,
    val cleaningState: CleaningState = CleaningState.Idle,
)

