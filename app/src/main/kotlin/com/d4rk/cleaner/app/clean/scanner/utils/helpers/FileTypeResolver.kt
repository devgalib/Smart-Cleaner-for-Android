package com.d4rk.cleaner.app.clean.scanner.utils.helpers

import android.os.Build
import android.webkit.MimeTypeMap
import java.io.File
import java.nio.file.Files
import java.util.Locale

/**
 * Resolves [File] instances to high level categories using MIME types.
 * Falls back to file extensions when necessary and provides a generic
 * "Other" category for unknown or unsupported types.
 */
object FileTypeResolver {

    enum class Category {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        ARCHIVE,
        APK,
        FONT,
        WINDOWS,
        OTHER
    }

    private val archiveMimeTypes = setOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/x-7z-compressed",
        "application/x-rar-compressed",
        "application/x-tar",
        "application/gzip",
        "application/x-gzip",
        "application/x-bzip2",
        "application/x-xz"
    )

    private val archiveExtensions = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
    )

    private val fontExtensions = setOf("ttf", "otf", "woff", "woff2")
    private val windowsExtensions = setOf("exe", "msi", "ini")

    fun getMimeType(file: File): String? {
        var type: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = runCatching { Files.probeContentType(file.toPath()) }.getOrNull()
        }
        if (type.isNullOrBlank()) {
            val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
                .lowercase(Locale.ROOT)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
        return type
    }

    fun resolve(file: File): Category {
        val mime = getMimeType(file) ?: ""
        val ext = file.extension.lowercase(Locale.ROOT)
        return when {
            mime.startsWith("image/") -> Category.IMAGE
            mime.startsWith("video/") -> Category.VIDEO
            mime.startsWith("audio/") -> Category.AUDIO
            mime == "application/vnd.android.package-archive" || ext == "apk" -> Category.APK
            mime == "application/pdf" || mime.contains("msword") || mime.contains("officedocument") ||
                mime.contains("ms-powerpoint") || mime.contains("ms-excel") || mime.startsWith("text/") -> Category.DOCUMENT
            mime in archiveMimeTypes || ext in archiveExtensions -> Category.ARCHIVE
            mime.startsWith("font/") || mime.contains("font") || ext in fontExtensions -> Category.FONT
            mime == "application/x-msdownload" || mime == "application/x-msi" || ext in windowsExtensions -> Category.WINDOWS
            else -> Category.OTHER
        }
    }
}

