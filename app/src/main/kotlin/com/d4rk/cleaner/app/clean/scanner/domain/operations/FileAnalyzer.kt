package com.d4rk.cleaner.app.clean.scanner.domain.operations

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileTypesData
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetDuplicatesUseCase
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FileTypeResolver
import com.d4rk.cleaner.app.settings.cleaning.utils.constants.ExtensionsConstants
import java.io.File
import kotlinx.coroutines.flow.asFlow

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

        scannedFiles.forEach { file ->
            val entry = FileEntry(
                path = file.absolutePath,
                size = if (file.isDirectory) 0 else file.length(),
                modified = file.lastModified()
            )
            if (includeDuplicates && entry.path in duplicateFiles) {
                duplicatesTitle?.let { title -> filesMap.getOrPut(title) { mutableListOf() }.add(entry) }
            } else {
                val type = FileTypeResolver.resolve(file)
                val (index, prefKey) = when (type) {
                    FileTypeResolver.Category.IMAGE -> 0 to ExtensionsConstants.IMAGE_EXTENSIONS
                    FileTypeResolver.Category.VIDEO -> 1 to ExtensionsConstants.VIDEO_EXTENSIONS
                    FileTypeResolver.Category.AUDIO -> 2 to ExtensionsConstants.AUDIO_EXTENSIONS
                    FileTypeResolver.Category.DOCUMENT -> 3 to ExtensionsConstants.OFFICE_EXTENSIONS
                    FileTypeResolver.Category.ARCHIVE -> 4 to ExtensionsConstants.ARCHIVE_EXTENSIONS
                    FileTypeResolver.Category.APK -> 5 to ExtensionsConstants.APK_EXTENSIONS
                    FileTypeResolver.Category.FONT -> 6 to ExtensionsConstants.FONT_EXTENSIONS
                    FileTypeResolver.Category.WINDOWS -> 7 to ExtensionsConstants.WINDOWS_EXTENSIONS
                    FileTypeResolver.Category.OTHER -> 9 to ExtensionsConstants.OTHER_EXTENSIONS
                }
                if (preferences[prefKey] == true) {
                    val title = baseFinalTitles[index]
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
