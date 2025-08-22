package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import android.os.Environment
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.cleaner.core.utils.extensions.partialHash
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
        val seen = HashMap<Long, File>()
        val hashCache = HashMap<File, String>()

        for (dir in searchDirs) {
            if (!dir.exists()) continue
            for (file in dir.walkTopDown()) {
                if (!file.isFile) continue
                val size = file.length()
                val other = seen[size]
                if (other != null) {
                    val hash1 = hashCache[other] ?: other.partialHash()?.also { hashCache[other] = it }
                    val hash2 = hashCache[file] ?: file.partialHash()?.also { hashCache[file] = it }
                    if (hash1 != null && hash1 == hash2) return@withContext true
                } else {
                    seen[size] = file
                }
            }
        }
        false
    }
}

