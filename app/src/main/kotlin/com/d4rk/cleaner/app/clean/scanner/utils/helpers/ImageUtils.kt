package com.d4rk.cleaner.app.clean.scanner.utils.helpers

import android.content.Context
import com.d4rk.cleaner.R
import java.io.File

fun getFileIcon(extension: String, context: Context): Int {
    val category = FileTypeResolver.resolve(File("dummy.${'$'}{extension.lowercase()}"))
    return when (category) {
        FileTypeResolver.Category.APK -> R.drawable.ic_apk_document
        FileTypeResolver.Category.IMAGE -> R.drawable.ic_image
        FileTypeResolver.Category.VIDEO -> R.drawable.ic_video_file
        FileTypeResolver.Category.AUDIO -> R.drawable.ic_audio_file
        FileTypeResolver.Category.ARCHIVE -> R.drawable.ic_archive_filter
        else -> R.drawable.ic_unknown_document
    }
}