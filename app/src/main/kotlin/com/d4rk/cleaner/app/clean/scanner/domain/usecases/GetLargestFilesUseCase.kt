package com.d4rk.cleaner.app.clean.scanner.domain.usecases

import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.cleaner.app.clean.scanner.domain.`interface`.ScannerRepositoryInterface
import com.d4rk.cleaner.core.domain.model.network.Errors
import com.d4rk.cleaner.core.utils.extensions.toError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class GetLargestFilesUseCase(private val repository: ScannerRepositoryInterface) {

    private var cache: List<File>? = null
    private var lastFetch: Long = 0L
    private val cacheDuration = 5 * 60 * 1000L // 5 minutes

    operator fun invoke(limit: Int? = null): Flow<DataState<List<File>, Errors>> = flow {
        emit(DataState.Loading())
        val now = System.currentTimeMillis()
        val cached = cache
        if (cached != null && now - lastFetch < cacheDuration) {
            val data = limit?.let { cached.take(it) } ?: cached
            emit(DataState.Success(data))
            return@flow
        }

        runCatching {
            repository.getLargestFiles(limit)
        }.onSuccess { files ->
            if (limit == null) {
                cache = files
                lastFetch = now
            }
            emit(DataState.Success(files))
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(DataState.Error(error = throwable.toError()))
        }
    }
}
