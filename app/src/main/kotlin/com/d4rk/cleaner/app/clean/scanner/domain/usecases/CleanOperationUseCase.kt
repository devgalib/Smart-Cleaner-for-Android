package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel

interface CleanOperationUseCase {
    fun analyzeFiles()
    fun cleanFiles(screenData: UiScannerModel?)
    fun moveToTrash(files: List<FileEntry>)
    fun onCleaningFailed()
    fun resetAfterError()
}
