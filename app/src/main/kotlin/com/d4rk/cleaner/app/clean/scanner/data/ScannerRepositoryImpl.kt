package com.d4rk.cleaner.app.clean.scanner.data


import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.memory.domain.data.model.StorageInfo
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileTypesData
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.cleaner.app.clean.scanner.domain.`interface`.ScannerRepositoryInterface
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FileTypeResolver
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.StorageUtils
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.extensions.clearClipboardCompat
import com.d4rk.cleaner.core.utils.extensions.partialMd5
import com.d4rk.cleaner.core.utils.helpers.DirectoryScanner
import com.d4rk.cleaner.core.utils.helpers.FileDeletionHelper
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import com.d4rk.cleaner.core.utils.helpers.shouldSkip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ScannerRepositoryImpl(
    private val application: Application, private val dataStore: DataStore
) : ScannerRepositoryInterface {

    private val trashDir: File =
        File(application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Trash")

    override suspend fun getStorageInfo(): UiScannerModel {
        return suspendCoroutine { continuation ->
            StorageUtils.getStorageInfo(context = application) { _, _, _, usageProgress, freeSpacePercentage ->
                continuation.resume(
                    UiScannerModel(
                        storageInfo = StorageInfo(
                            storageUsageProgress = usageProgress,
                            freeSpacePercentage = freeSpacePercentage
                        )
                    )
                )
            }
        }
    }

    override suspend fun getFileTypes(): FileTypesData {
        return suspendCoroutine { continuation ->
            val fileTypesTitles: List<String> =
                application.resources.getStringArray(R.array.file_types_titles).toList()

            val fileTypesData = FileTypesData(
                fileTypesTitles = fileTypesTitles,
            )
            continuation.resume(value = fileTypesData)
        }
    }

    override fun getAllFiles(onLockedDir: ((File) -> Unit)?): Flow<File> = flow {
        val stack: ArrayDeque<File> = ArrayDeque()

        val root = Environment.getExternalStorageDirectory()
        stack.addFirst(root)

        val trashDir =
            File(application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Trash")

        val showHidden = dataStore.showHiddenFiles.first()

        while (stack.isNotEmpty()) {
            val currentFile: File = stack.removeFirst()
            if (currentFile.shouldSkip(showHidden)) continue
            if (currentFile.isDirectory) {
                if (!currentFile.absolutePath.startsWith(trashDir.absolutePath)) {
                    currentFile.listFiles()?.forEach { child ->
                        if (child.shouldSkip(showHidden)) {
                            if (child.isDirectory && child.isProtectedAndroidDir()) {
                                onLockedDir?.invoke(child)
                            }
                        } else if (child.isDirectory) {
                            stack.addLast(child)
                        } else {
                            emit(child)
                        }
                    }
                }
            } else {
                emit(currentFile)
            }
        }
    }

    override suspend fun getTrashFiles(): List<File> {
        return withContext(context = Dispatchers.IO) {
            val trashDir =
                File(application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Trash")
            if (trashDir.exists()) trashDir.listFiles()?.toList() ?: emptyList()
            else emptyList()
        }
    }

    override suspend fun deleteFiles(files: Collection<File>) {
        val results = FileDeletionHelper.deleteFiles(files, application.contentResolver)
        val totalSize = results.filter { it.success }.sumOf { it.file.length() }
        val failed = results.filter { !it.success }
        if (failed.isNotEmpty()) {
            val paths = failed.joinToString { it.file.absolutePath }
            throw RuntimeException("FILE_DELETE_FAILED: $paths")
        }
        if (totalSize > 0) {
            dataStore.addCleanedSpace(space = totalSize)
        }
        clearClipboardImplementation()
    }

    private fun clearClipboardImplementation() {
        val clipboardManager: ClipboardManager =
            application.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        runCatching {
            clipboardManager.clearClipboardCompat()
        }
    }

    override suspend fun moveToTrash(files: Collection<File>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = files.filter { it.exists() }.map { file ->
                dataStore.addTrashFileOriginalPath(originalPath = file.absolutePath)
                dataStore.addTrashSize(size = file.length())
                FileProvider.getUriForFile(
                    application,
                    "${application.packageName}.fileprovider",
                    file
                )
            }
            if (uris.isNotEmpty()) {
                MediaStore.createTrashRequest(application.contentResolver, uris, true).send()
            }
        } else {
            if (!trashDir.exists()) {
                trashDir.mkdirs()
            }

            files.forEach { file ->
                if (file.exists()) {
                    val destination = File(trashDir, file.name)

                    if (file.renameTo(destination)) {
                        dataStore.addTrashFileOriginalPath(originalPath = file.absolutePath)
                        dataStore.addTrashSize(size = file.length())
                        MediaScannerConnection.scanFile(
                            application,
                            arrayOf(destination.absolutePath, file.absolutePath),
                            null,
                            null
                        )
                    }
                }
            }
        }
    }

    override suspend fun restoreFromTrash(filesToRestore: Set<File>) {
        val originalPaths: Set<String> = dataStore.trashFileOriginalPaths.first()
        val pathMap = originalPaths.associateBy { File(it).name }
        filesToRestore.forEach { file ->
            if (file.exists()) {
                val originalPath: String? = pathMap[file.name]
                if (originalPath != null) {
                    val destinationFile = File(originalPath)
                    val destinationParent: File? = destinationFile.parentFile

                    if (destinationParent?.exists() == false) {
                        destinationParent.mkdirs()
                    }

                    if (file.renameTo(destinationFile)) {
                        dataStore.removeTrashFileOriginalPath(originalPath)
                        dataStore.subtractTrashSize(size = file.length())
                        MediaScannerConnection.scanFile(
                            application,
                            arrayOf(destinationFile.absolutePath, file.absolutePath),
                            null,
                            null
                        )
                    }
                } else {
                    val downloadsDir: File =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val destinationFile = File(downloadsDir, file.name)

                    if (file.renameTo(destinationFile)) {
                        dataStore.removeTrashFileOriginalPath(file.absolutePath)
                        dataStore.subtractTrashSize(size = file.length())
                        MediaScannerConnection.scanFile(
                            application,
                            arrayOf(destinationFile.absolutePath, file.absolutePath),
                            null,
                            null
                        )
                    }
                }
            }
        }
    }

    override suspend fun addTrashSize(size: Long) {
        dataStore.addTrashSize(size = size)
    }

    override suspend fun subtractTrashSize(size: Long) {
        dataStore.subtractTrashSize(size = size)
    }

    override suspend fun getLargestFiles(limit: Int?): List<File> {
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val root = Environment.getExternalStorageDirectory()
            val minSize = 100L * 1024 * 1024 // 100MB threshold
            val trashed = dataStore.trashFileOriginalPaths.first()
            val showHidden = dataStore.showHiddenFiles.first()

            fun fileType(file: File): String = when (FileTypeResolver.resolve(file)) {
                FileTypeResolver.Category.VIDEO -> "video"
                FileTypeResolver.Category.ARCHIVE -> "archive"
                FileTypeResolver.Category.APK -> "apk"
                FileTypeResolver.Category.IMAGE -> "image"
                FileTypeResolver.Category.AUDIO -> "audio"
                FileTypeResolver.Category.DOCUMENT -> "doc"
                else -> "other"
            }

            val groups = mutableMapOf<String, MutableList<File>>()
            val seenHashes = mutableSetOf<String>()

            DirectoryScanner.scan(
                root = root,
                skipDir = { dir ->
                    dir.absolutePath.startsWith(trashDir.absolutePath) ||
                            dir.absolutePath.startsWith(File(root, "Android").absolutePath) ||
                            (!showHidden && dir.isHidden)
                }
            ) { file ->
                if ((!showHidden && file.isHidden) || file.absolutePath in trashed) return@scan
                if (file.length() >= minSize) {
                    val hash = file.partialMd5() ?: return@scan
                    if (seenHashes.add(hash)) {
                        val type = fileType(file)
                        groups.getOrPut(type) { mutableListOf() }.add(file)
                    }
                }
            }

            groups.values.forEach { list ->
                list.sortWith(compareByDescending<File> { it.length() }.thenByDescending { it.lastModified() })
            }

            if (limit == null) {
                groups.values.flatten().sortedWith(
                    compareByDescending<File> { it.length() }.thenByDescending { it.lastModified() }
                )
            } else {
                val result = mutableListOf<File>()
                var index = 0
                while (result.size < limit) {
                    var added = false
                    for (list in groups.values) {
                        if (index < list.size && result.size < limit) {
                            result.add(list[index])
                            added = true
                        }
                    }
                    if (!added) break
                    index++
                }
                result
            }
        }
    }

    override fun getEmptyFolders(): Flow<File> = flow {
        val stack: ArrayDeque<File> = ArrayDeque()
        @Suppress("DEPRECATION")
        val root = Environment.getExternalStorageDirectory()
        stack.addFirst(root)
        val showHidden = dataStore.showHiddenFiles.first()
        while (stack.isNotEmpty()) {
            val dir = stack.removeFirst()
            if (dir.shouldSkip(showHidden) || dir.absolutePath.startsWith(trashDir.absolutePath)) continue
            if (dir.isDirectory) {
                val children = dir.listFiles()?.filterNot { it.shouldSkip(showHidden) }
                if (children == null || children.isEmpty()) {
                    emit(dir)
                } else {
                    children.filter { it.isDirectory }.forEach { stack.addLast(it) }
                }
            }
        }
    }
}
