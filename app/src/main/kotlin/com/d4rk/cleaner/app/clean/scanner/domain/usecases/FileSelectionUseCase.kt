package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import java.io.File

/**
 * Handles file selection logic for the scanner feature.
 */
interface FileSelectionUseCase {
    fun onFileSelectionChange(currentData: UiScannerModel, file: File, isChecked: Boolean): UiScannerModel
    fun toggleSelectAllFiles(currentData: UiScannerModel): UiScannerModel
    fun toggleSelectFilesForCategory(currentData: UiScannerModel, category: String): UiScannerModel
    fun toggleSelectFilesForDate(currentData: UiScannerModel, files: List<File>, isChecked: Boolean): UiScannerModel
}
