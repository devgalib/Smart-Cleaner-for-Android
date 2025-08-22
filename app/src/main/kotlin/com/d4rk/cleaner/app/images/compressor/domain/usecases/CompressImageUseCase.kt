package com.d4rk.cleaner.app.images.compressor.domain.usecases

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class CompressImageUseCase(private val context: Context) {
    suspend operator fun invoke(
        originalFile: File,
        quality: Int,
        targetWidth: Int,
        targetHeight: Int,
        desiredSizeBytes: Long? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): File = withContext(Dispatchers.IO) {
        if (desiredSizeBytes != null) {
            compressImageToTargetSize(
                originalFile,
                targetWidth,
                targetHeight,
                desiredSizeBytes,
                format
            )
        } else {
            compressImageUsingNative(originalFile, quality, targetWidth, targetHeight, format)
        }
    }

    private fun compressImageToTargetSize(
        originalFile: File,
        targetWidth: Int,
        targetHeight: Int,
        desiredSizeBytes: Long,
        format: Bitmap.CompressFormat
    ): File {
        if (originalFile.length() <= desiredSizeBytes) {
            return originalFile
        }

        val scaledBitmap = decodeSampledBitmap(originalFile, targetWidth, targetHeight)
            .scale(targetWidth, targetHeight)

        var minQuality = 10
        var maxQuality = 100
        var finalQuality: Int
        val stream = ByteArrayOutputStream()
        var compressedBytes: ByteArray = byteArrayOf()

        while (minQuality <= maxQuality) {
            finalQuality = (minQuality + maxQuality) / 2
            stream.reset()
            scaledBitmap.compress(format, finalQuality, stream)
            val byteArray = stream.toByteArray()
            val size = byteArray.size.toLong()
            compressedBytes = byteArray
            if (size in (desiredSizeBytes * 95 / 100)..(desiredSizeBytes * 105 / 100)) {
                break
            }
            if (size > desiredSizeBytes) {
                maxQuality = finalQuality - 1
            } else {
                minQuality = finalQuality + 1
            }
        }
        val outputFile = File(context.cacheDir, "native_compressed_${System.currentTimeMillis()}.jpg")
        outputFile.outputStream().use { it.write(compressedBytes) }
        return outputFile
    }

    private fun compressImageUsingNative(
        originalFile: File,
        quality: Int,
        targetWidth: Int,
        targetHeight: Int,
        format: Bitmap.CompressFormat
    ): File {
        val scaledBitmap = decodeSampledBitmap(originalFile, targetWidth, targetHeight)
            .scale(targetWidth, targetHeight)
        val outputFile =
            File(context.cacheDir, "native_compressed_${System.currentTimeMillis()}.jpg")
        outputFile.outputStream().use { out ->
            scaledBitmap.compress(format, quality, out)
        }
        return outputFile
    }

    private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        val (height, width) = options.outHeight to options.outWidth
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
