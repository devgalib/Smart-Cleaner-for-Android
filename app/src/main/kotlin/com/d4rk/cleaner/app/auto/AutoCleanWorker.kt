package com.d4rk.cleaner.app.auto

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.app.clean.scanner.domain.`interface`.ScannerRepositoryInterface
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.AnalyzeFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.DeleteFilesUseCase
import com.d4rk.cleaner.app.clean.scanner.domain.usecases.GetEmptyFoldersUseCase
import com.d4rk.cleaner.app.images.utils.ImageHashUtils
import com.d4rk.cleaner.app.settings.cleaning.utils.constants.ExtensionsConstants
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.extensions.partialMd5
import com.d4rk.cleaner.core.utils.helpers.CleaningEventBus
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FileTypeResolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.time.Duration.Companion.days

class AutoCleanWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val analyzeFiles: AnalyzeFilesUseCase by inject()
    private val deleteFiles: DeleteFilesUseCase by inject()
    private val getEmptyFolders: GetEmptyFoldersUseCase by inject()
    private val dataStore: DataStore by inject()
    private val repository: ScannerRepositoryInterface by inject()

    override suspend fun doWork(): Result {
        val autoCleanEnabled = dataStore.autoCleanEnabled.first()
        if (!autoCleanEnabled) return Result.success()

        val frequency = dataStore.autoCleanFrequencyDays.first()
        val lastScan = dataStore.lastScanTimestamp.first() ?: 0L
        val now = System.currentTimeMillis()
        if (frequency <= 0 || now - lastScan < frequency.days.inWholeMilliseconds) {
            return Result.success()
        }

        val files = mutableListOf<File>()
        val errorDuringScan = analyzeFiles()
            .onEach { state ->
                if (state is DataState.Success) files.add(state.data)
            }
            .firstOrNull { it is DataState.Error } != null
        if (errorDuringScan) return Result.retry()

        val emptyFolders = mutableListOf<File>()
        val errorDuringEmpty = getEmptyFolders()
            .onEach { state ->
                if (state is DataState.Success) emptyFolders.add(state.data)
            }
            .firstOrNull { it is DataState.Error } != null
        if (errorDuringEmpty) return Result.retry()

        val prefs = mapOf(
            ExtensionsConstants.IMAGE_EXTENSIONS to dataStore.deleteImageFiles.first(),
            ExtensionsConstants.VIDEO_EXTENSIONS to dataStore.deleteVideoFiles.first(),
            ExtensionsConstants.AUDIO_EXTENSIONS to dataStore.deleteAudioFiles.first(),
            ExtensionsConstants.OFFICE_EXTENSIONS to dataStore.deleteOfficeFiles.first(),
            ExtensionsConstants.ARCHIVE_EXTENSIONS to dataStore.deleteArchives.first(),
            ExtensionsConstants.APK_EXTENSIONS to dataStore.deleteApkFiles.first(),
            ExtensionsConstants.FONT_EXTENSIONS to dataStore.deleteFontFiles.first(),
            ExtensionsConstants.WINDOWS_EXTENSIONS to dataStore.deleteWindowsFiles.first(),
            ExtensionsConstants.EMPTY_FOLDERS to dataStore.deleteEmptyFolders.first(),
            ExtensionsConstants.OTHER_EXTENSIONS to dataStore.deleteOtherFiles.first()
        )
        val includeDuplicates =
            dataStore.deleteDuplicateFiles.first() && dataStore.duplicateScanEnabled.first()
        val deepDuplicateSearch = dataStore.deepDuplicateSearch.first()
        val toDelete = computeFilesToClean(
            files,
            emptyFolders,
            prefs,
            includeDuplicates,
            deepDuplicateSearch
        )
        if (toDelete.isEmpty()) return Result.success()

        val deleteState = deleteFiles(
            repository = repository,
            files = toDelete,
            mode = DeleteFilesUseCase.Mode.PERMANENT
        ).first()
        if (deleteState is DataState.Error) {
            CleaningEventBus.notifyCleaned(success = false)
            return Result.retry()
        }
        dataStore.saveLastScanTimestamp(now)
        CleaningEventBus.notifyCleaned(success = true)
        return Result.success()
    }

    private fun computeFilesToClean(
        scannedFiles: List<File>,
        emptyFolders: List<File>,
        preferences: Map<String, Boolean>,
        includeDuplicates: Boolean,
        deepDuplicateSearch: Boolean
    ): List<File> {
        val result = mutableListOf<File>()

        val duplicateGroups = if (includeDuplicates) findDuplicateGroups(
            scannedFiles,
            deepDuplicateSearch
        ) else emptyList()
        val duplicateFiles =
            if (includeDuplicates) duplicateGroups.flatten().toSet() else emptySet()

        scannedFiles.forEach { file ->
            if (includeDuplicates && file in duplicateFiles) {
                result.add(file)
            } else {
                val type = FileTypeResolver.resolve(file)
                val pref = when (type) {
                    FileTypeResolver.Category.IMAGE -> ExtensionsConstants.IMAGE_EXTENSIONS
                    FileTypeResolver.Category.VIDEO -> ExtensionsConstants.VIDEO_EXTENSIONS
                    FileTypeResolver.Category.AUDIO -> ExtensionsConstants.AUDIO_EXTENSIONS
                    FileTypeResolver.Category.DOCUMENT -> ExtensionsConstants.OFFICE_EXTENSIONS
                    FileTypeResolver.Category.ARCHIVE -> ExtensionsConstants.ARCHIVE_EXTENSIONS
                    FileTypeResolver.Category.APK -> ExtensionsConstants.APK_EXTENSIONS
                    FileTypeResolver.Category.FONT -> ExtensionsConstants.FONT_EXTENSIONS
                    FileTypeResolver.Category.WINDOWS -> ExtensionsConstants.WINDOWS_EXTENSIONS
                    FileTypeResolver.Category.OTHER -> ExtensionsConstants.OTHER_EXTENSIONS
                }
                if (preferences[pref] == true) result.add(file)
            }
        }
        if (preferences[ExtensionsConstants.EMPTY_FOLDERS] == true) {
            result.addAll(emptyFolders)
        }
        return result
    }

    private fun findDuplicateGroups(
        files: List<File>,
        deepSearch: Boolean
    ): List<List<File>> {
        val hashMap = mutableMapOf<String, MutableList<File>>()
        files.filter { it.isFile }.forEach { file ->
            val hash = if (deepSearch && FileTypeResolver.resolve(file) == FileTypeResolver.Category.IMAGE) {
                ImageHashUtils.perceptualHash(file)
            } else {
                file.partialMd5()
            } ?: return@forEach
            hashMap.getOrPut(hash) { mutableListOf() }.add(file)
        }
        return hashMap.values.filter { it.size > 1 }
    }
}
