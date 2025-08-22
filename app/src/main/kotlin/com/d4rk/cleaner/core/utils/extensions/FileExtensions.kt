package com.d4rk.cleaner.core.utils.extensions

import com.google.common.hash.Hashing
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

private const val PARTIAL_HASH_SIZE = 1 * 1024 * 1024 // 1MB
private val bufferPool = object : ThreadLocal<ByteArray>() {
    override fun initialValue(): ByteArray = ByteArray(DEFAULT_BUFFER_SIZE)
}
private val hashFunction = Hashing.murmur3_128()

fun File.partialHash(): String? = runCatching {
    val hasher = hashFunction.newHasher()
    val buffer = bufferPool.get()

    if (length() <= PARTIAL_HASH_SIZE * 2) {
        FileInputStream(this).use { stream ->
            var read = stream.read(buffer)
            while (read > 0) {
                hasher.putBytes(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
    } else {
        FileInputStream(this).use { stream ->
            var remaining = PARTIAL_HASH_SIZE
            while (remaining > 0) {
                val read = stream.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                hasher.putBytes(buffer, 0, read)
                remaining -= read
            }
        }

        RandomAccessFile(this, "r").use { raf ->
            raf.seek(length() - PARTIAL_HASH_SIZE)
            var remaining = PARTIAL_HASH_SIZE
            while (remaining > 0) {
                val read = raf.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                hasher.putBytes(buffer, 0, read)
                remaining -= read
            }
        }
    }

    hasher.hash().toString()
}.getOrNull()

