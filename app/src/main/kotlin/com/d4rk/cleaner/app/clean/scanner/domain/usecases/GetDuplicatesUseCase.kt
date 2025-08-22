package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.FileEntry
import com.d4rk.cleaner.core.utils.extensions.partialHash
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class GetDuplicatesUseCase(
    private val dispatchers: DispatcherProvider
) {
    private val hashCache = ConcurrentHashMap<String, String>()
    private val chunkSize = 100

    suspend operator fun invoke(files: Flow<File>): List<List<FileEntry>> {
        val groupsBySize = HashMap<Long, MutableList<FileEntry>>()

        files.collect { file ->
            if (file.isFile) {
                val entry = FileEntry(file.absolutePath, file.length(), file.lastModified())
                groupsBySize.getOrPut(entry.size) { mutableListOf() }.add(entry)
            }
        }

        val candidates = groupsBySize.values.filter { it.size > 1 }
        if (candidates.isEmpty()) return emptyList()

        val hashed = coroutineScope {
            candidates.flatMap { group ->
                group.chunked(chunkSize).map { chunk ->
                    async {
                        withContext(dispatchers.io) {
                            chunk.mapNotNull { entry ->
                                val file = File(entry.path)
                                val key = "${entry.path}:${entry.modified}"
                                val hash = hashCache[key] ?: file.partialHash()?.also { hashCache[key] = it }
                                hash?.let { it to entry }
                            }
                        }
                    }
                }
            }.awaitAll().flatten()
        }

        return hashed.groupBy({ it.first }, { it.second })
            .values
            .filter { it.size > 1 }
            .toList()
    }
}

