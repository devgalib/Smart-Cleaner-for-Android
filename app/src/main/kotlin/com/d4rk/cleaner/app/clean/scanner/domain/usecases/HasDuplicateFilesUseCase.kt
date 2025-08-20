package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import android.os.Environment
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.cleaner.core.utils.extensions.partialMd5
import kotlinx.coroutines.withContext
import java.io.File

class HasDuplicateFilesUseCase(
    private val dispatchers: DispatcherProvider
) {
    private val searchDirs = listOf(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    )

    suspend operator fun invoke(): Boolean = withContext(dispatchers.io) {
        val seen = HashMap<Pair<Long, Long>, File>()
        val hashCache = HashMap<File, String>()

        for (dir in searchDirs) {
            if (!dir.exists()) continue
            for (file in dir.walkTopDown()) {
                if (!file.isFile) continue
                val key = file.length() to file.lastModified()
                val other = seen[key]
                if (other != null) {
                    val hash1 = hashCache[other] ?: other.partialMd5()?.also { hashCache[other] = it }
                    val hash2 = hashCache[file] ?: file.partialMd5()?.also { hashCache[file] = it }
                    if (hash1 != null && hash1 == hash2) return@withContext true
                } else {
                    seen[key] = file
                }
            }
        }
        false
    }
}

