package com.d4rk.cleaner.core.utils.extensions

import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.security.MessageDigest

private const val PARTIAL_HASH_SIZE = 1 * 1024 * 1024 // 1MB

fun File.partialHash(): String? = runCatching {
    val digest = MessageDigest.getInstance("MD5")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    if (length() <= PARTIAL_HASH_SIZE * 2) {
        FileInputStream(this).use { stream ->
            var read = stream.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = stream.read(buffer)
            }
        }
    } else {
        FileInputStream(this).use { stream ->
            var remaining = PARTIAL_HASH_SIZE
            while (remaining > 0) {
                val read = stream.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
        }

        RandomAccessFile(this, "r").use { raf ->
            raf.seek(length() - PARTIAL_HASH_SIZE)
            var remaining = PARTIAL_HASH_SIZE
            while (remaining > 0) {
                val read = raf.read(buffer, 0, minOf(buffer.size, remaining))
                if (read <= 0) break
                digest.update(buffer, 0, read)
                remaining -= read
            }
        }
    }

    digest.digest().joinToString("") { "%02x".format(it) }
}.getOrNull()

