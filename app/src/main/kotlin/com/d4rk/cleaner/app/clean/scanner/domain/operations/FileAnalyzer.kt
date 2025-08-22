package com.d4rk.cleaner.app.clean.scanner.domain.operations

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileTypesData
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetDuplicatesUseCase
import com.d4rk.cleaner.app.settings.cleaning.utils.constants.ExtensionsConstants
import kotlinx.coroutines.flow.asFlow
import java.io.File
import java.util.Locale

/**
 * Provides functions used during scanning to analyze files on disk.
 */
class FileAnalyzer(
    private val getDuplicatesUseCase: GetDuplicatesUseCase
) {
    suspend fun computeGroupedFiles(
        scannedFiles: List<File>,
        emptyFolders: List<File>,
        fileTypesData: FileTypesData,
        preferences: Map<String, Boolean>,
        includeDuplicates: Boolean
    ): Triple<Map<String, List<FileEntry>>, Set<FileEntry>, List<List<FileEntry>>> {

        val baseDefaultTitles = listOf(
            "Images",
            "Videos",
            "Audios",
            "Documents",
            "Archives",
            "APKs",
            "Fonts",
            "Windows Files",
            "Empty Folders",
            "Other Files"
        )

        val baseFinalTitles = baseDefaultTitles.mapIndexed { index, fallback ->
            fileTypesData.fileTypesTitles.getOrElse(index) { fallback }
        }

        val duplicatesTitle = if (includeDuplicates) {
            fileTypesData.fileTypesTitles.getOrElse(10) { "Duplicates" }
        } else null

        val filesMap: LinkedHashMap<String, MutableList<FileEntry>> = linkedMapOf()
        filesMap.putAll(baseFinalTitles.associateWith { mutableListOf() })
        duplicatesTitle?.let { filesMap[it] = mutableListOf() }

        val duplicateGroups: List<List<FileEntry>> = if (includeDuplicates) {
            getDuplicatesUseCase(scannedFiles.asFlow())
        } else emptyList()
        val duplicateFiles: Set<String> = if (includeDuplicates) {
            duplicateGroups.flatten().map { it.path }.toSet()
        } else emptySet()
        val duplicateOriginals: Set<FileEntry> = if (includeDuplicates) {
            duplicateGroups.mapNotNull { group -> group.minByOrNull { it.modified } }.toSet()
        } else emptySet()

        val prefKeys = listOf(
            ExtensionsConstants.IMAGE_EXTENSIONS,
            ExtensionsConstants.VIDEO_EXTENSIONS,
            ExtensionsConstants.AUDIO_EXTENSIONS,
            ExtensionsConstants.OFFICE_EXTENSIONS,
            ExtensionsConstants.ARCHIVE_EXTENSIONS,
            ExtensionsConstants.APK_EXTENSIONS,
            ExtensionsConstants.FONT_EXTENSIONS,
            ExtensionsConstants.WINDOWS_EXTENSIONS,
            ExtensionsConstants.EMPTY_FOLDERS,
            ExtensionsConstants.OTHER_EXTENSIONS,
        )

        scannedFiles.forEach { file ->
            val entry = FileEntry(
                path = file.absolutePath,
                size = if (file.isDirectory) 0 else file.length(),
                modified = file.lastModified()
            )
            if (includeDuplicates && entry.path in duplicateFiles) {
                duplicatesTitle?.let { title -> filesMap.getOrPut(title) { mutableListOf() }.add(entry) }
            } else {
                val extension = file.extension.lowercase(Locale.ROOT)
                val title = fileTypesData.extensionToCategory[extension] ?: baseFinalTitles[9]
                val index = baseFinalTitles.indexOf(title).takeIf { it >= 0 } ?: 9
                val prefKey = prefKeys.getOrElse(index) { ExtensionsConstants.OTHER_EXTENSIONS }
                if (preferences[prefKey] == true) {
                    filesMap.getOrPut(title) { mutableListOf() }.add(entry)
                }
            }
        }

        val emptyFoldersTitle = baseFinalTitles[8]
        if (preferences[ExtensionsConstants.EMPTY_FOLDERS] == true && emptyFolders.isNotEmpty()) {
            filesMap[emptyFoldersTitle] = emptyFolders
                .map { FileEntry(it.absolutePath, 0, it.lastModified()) }
                .toMutableList()
        }

        val filteredMap = filesMap.filterValues { it.isNotEmpty() }

        return Triple(filteredMap, duplicateOriginals, duplicateGroups)
    }

}
