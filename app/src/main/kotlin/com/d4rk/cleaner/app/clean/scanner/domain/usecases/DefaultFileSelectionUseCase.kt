package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import kotlinx.coroutines.yield
import java.io.File

/**
 * Default implementation of [FileSelectionUseCase].
 */
class DefaultFileSelectionUseCase : FileSelectionUseCase {
    override fun onFileSelectionChange(
        currentData: UiScannerModel,
        file: File,
        isChecked: Boolean
    ): UiScannerModel {
        val path = file.absolutePath
        val updatedSet = currentData.analyzeState.selectedFiles.toMutableSet().apply {
            if (isChecked) add(path) else remove(path)
        }
        val visibleFiles = currentData.analyzeState.groupedFiles.values.flatten()
        val visiblePaths = visibleFiles.map { it.path }
        val selectedVisibleCount =
            updatedSet.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }
        val selectedSize = calculateSelectedFilesSize(
            currentData.analyzeState.scannedFileList,
            updatedSet
        )
        return currentData.copy(
            analyzeState = currentData.analyzeState.copy(
                selectedFiles = updatedSet,
                selectedFilesCount = selectedVisibleCount,
                selectedFilesSize = selectedSize,
                areAllFilesSelected = selectedVisibleCount == visibleFiles.size && visibleFiles.isNotEmpty()
            )
        )
    }

    override fun toggleSelectAllFiles(currentData: UiScannerModel): UiScannerModel {
        val visibleFiles = currentData.analyzeState.groupedFiles.values.flatten()
        val visiblePaths = visibleFiles.map { it.path }
        val hasFiles = visibleFiles.isNotEmpty()
        val newState = if (hasFiles) !currentData.analyzeState.areAllFilesSelected else false
        val duplicateOriginals = currentData.analyzeState.duplicateOriginals.map { it.path }.toSet()
        val updatedSet: MutableSet<String> = if (newState) {
            val base = currentData.analyzeState.selectedFiles.toMutableSet()
            visibleFiles.chunked(200).forEach { chunk ->
                chunk.forEach { file ->
                    val path = file.path
                    if (path !in duplicateOriginals) base.add(path)
                }
                yield()
            }
            base
        } else mutableSetOf()
        val selectedVisibleCount = if (newState) {
            updatedSet.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }
        } else 0
        val selectedSize = calculateSelectedFilesSize(
            currentData.analyzeState.scannedFileList,
            updatedSet
        )
        return currentData.copy(
            analyzeState = currentData.analyzeState.copy(
                areAllFilesSelected = newState,
                selectedFiles = updatedSet,
                selectedFilesCount = selectedVisibleCount,
                selectedFilesSize = selectedSize
            )
        )
    }

    override fun toggleSelectFilesForCategory(
        currentData: UiScannerModel,
        category: String
    ): UiScannerModel {
        val filesInCategory = currentData.analyzeState.groupedFiles[category] ?: emptyList()
        if (filesInCategory.isEmpty()) return currentData
        val duplicateOriginals = currentData.analyzeState.duplicateOriginals.map { it.path }.toSet()
        val currentSelection = currentData.analyzeState.selectedFiles
        val allSelected =
            filesInCategory.filterNot { it.path in duplicateOriginals }
                .all { it.path in currentSelection }
        val updatedSelection = currentSelection.toMutableSet().apply {
            filesInCategory.forEach { file ->
                val path = file.path
                if (path in duplicateOriginals) {
                    if (allSelected) remove(path)
                } else {
                    if (allSelected) remove(path) else add(path)
                }
            }
        }
        val visibleFiles = currentData.analyzeState.groupedFiles.values.flatten()
        val visiblePaths = visibleFiles.map { it.path }
        val selectedVisibleCount =
            updatedSelection.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }
        val selectedSize = calculateSelectedFilesSize(
            currentData.analyzeState.scannedFileList,
            updatedSelection
        )
        return currentData.copy(
            analyzeState = currentData.analyzeState.copy(
                selectedFiles = updatedSelection,
                selectedFilesCount = selectedVisibleCount,
                selectedFilesSize = selectedSize,
                areAllFilesSelected = selectedVisibleCount == visibleFiles.size && visibleFiles.isNotEmpty()
            )
        )
    }

    override fun toggleSelectFilesForDate(
        currentData: UiScannerModel,
        files: List<File>,
        isChecked: Boolean
    ): UiScannerModel {
        val duplicateOriginals = currentData.analyzeState.duplicateOriginals.map { it.path }.toSet()
        val updatedSelectionSet =
            currentData.analyzeState.selectedFiles.toMutableSet().apply {
                files.forEach { file ->
                    val path = file.absolutePath
                    if (isChecked) {
                        if (path in duplicateOriginals && path !in this) {
                            // keep unchecked
                        } else {
                            add(path)
                        }
                    } else {
                        remove(path)
                    }
                }
            }
        val visibleFiles = currentData.analyzeState.groupedFiles.values.flatten()
        val visiblePaths = visibleFiles.map { it.path }
        val selectedVisibleCount =
            updatedSelectionSet.count { it in visiblePaths && !File(it).isProtectedAndroidDir() }
        val selectedSize = calculateSelectedFilesSize(
            currentData.analyzeState.scannedFileList,
            updatedSelectionSet
        )
        return currentData.copy(
            analyzeState = currentData.analyzeState.copy(
                selectedFiles = updatedSelectionSet,
                selectedFilesCount = selectedVisibleCount,
                selectedFilesSize = selectedSize,
                areAllFilesSelected = selectedVisibleCount == visibleFiles.size && visibleFiles.isNotEmpty()
            )
        )
    }

    private fun calculateSelectedFilesSize(
        scannedFiles: List<FileEntry>,
        selectedPaths: Set<String>
    ): Long {
        val sizeMap = scannedFiles.associate { it.path to it.size }
        return selectedPaths.sumOf { path ->
            if (!File(path).isProtectedAndroidDir()) sizeMap[path] ?: 0L else 0L
        }
    }
}
