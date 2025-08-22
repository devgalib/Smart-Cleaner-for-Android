package com.d4rk.cleaner.core.utils.extensions

import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import net.jpountz.xxhash.XXHashFactory

private const val PARTIAL_HASH_SIZE = 1 * 1024 * 1024 // 1MB
private val bufferPool = ThreadLocal.withInitial { ByteArray(DEFAULT_BUFFER_SIZE) }
private val xxFactory = XXHashFactory.fastestInstance()

fun File.partialHash(): String? = runCatching {
    val hasher = xxFactory.newStreamingHash64(0)
    val buffer = bufferPool.get()

    if (length() <= PARTIAL_HASH_SIZE * 2) {
        FileInputStream(this).use { stream ->
            var read = stream.read(buffer)
            while (read > 0) {
                hasher.update(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
    } else {
        FileInputStream(this).use { stream ->
            var remaining = PARTIAL_HASH_SIZE
            while (remaining > 0) {
                val read = stream.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                hasher.update(buffer, 0, read)
                remaining -= read
            }
        }

        RandomAccessFile(this, "r").use { raf ->
            raf.seek(length() - PARTIAL_HASH_SIZE)
            var remaining = PARTIAL_HASH_SIZE
            while (remaining > 0) {
                val read = raf.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                hasher.update(buffer, 0, read)
                remaining -= read
            }
        }
    }

    java.lang.Long.toHexString(hasher.value)
}.getOrNull()

