package com.d4rk.cleaner.app.clean.scanner.utils.helpers

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaFormat.KEY_MAX_INPUT_SIZE
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import coil3.video.VideoFrameDecoder
import coil3.video.videoFramePercent
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.apps.manager.ui.components.IconLoadingPlaceholder
import com.d4rk.cleaner.app.core.ui.components.AppImage
import com.google.common.io.Files.getFileExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.zip.ZipFile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Helper used for displaying file previews. All composables that need to render
 * a file preview should delegate to [FilePreviewHelper.Preview] so that the
 * logic for determining how a file should be presented lives in a single place.
 *
 * Extend [PreviewType] when supporting new file formats.
 */
object FilePreviewHelper {

    /**
     * Maximum bytes to allocate when generating audio waveforms. This cap
     * prevents OutOfMemory errors that can be triggered by corrupted or
     * malicious files reporting extremely large input sizes.
     */
    private const val MAX_WAVEFORM_BYTES = 1 * 1024 * 1024 // 1 MB
    private const val MAX_ALBUM_ART_DIMENSION = 256
    private const val MAX_WAVEFORM_FILE_SIZE = 20L * 1024 * 1024 // 20 MB
    private val recycleHandler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    private val bitmapCache: LruCache<String, Bitmap> by lazy {
        val cacheSizeKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024

            override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
                if (evicted) {
                    if (!oldValue.isRecycled) {
                        recycleHandler.postDelayed({
                            if (!oldValue.isRecycled) {
                                oldValue.recycle()
                            }
                        }, 2000)
                    }
                }
            }
        }
    }

    private fun albumArtKey(filePath: String) = "art:$filePath"
    private fun waveformKey(filePath: String) = "waveform:$filePath"


    /**
     * Respond to system memory pressure. On Android 14 (API 34) and above,
     * only the UI hidden callback is dispatched, so we clear the entire cache
     * when the UI is no longer visible. On older versions we retain the
     * existing behaviour of halving the cache size under background pressure
     * and evicting everything under moderate pressure.
     */
    fun onTrimMemory(level: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                bitmapCache.evictAll()
            }
        } else {
            @Suppress("DEPRECATION")
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                    bitmapCache.evictAll()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    val newSize = bitmapCache.maxSize() / 2
                    bitmapCache.resize(newSize)
                }
            }
        }
    }

    private suspend fun loadAlbumArt(file: File): Bitmap? = withContext(Dispatchers.IO) {
        val key = albumArtKey(file.path)
        bitmapCache.get(key)?.let { cached ->
            return@withContext cached.config?.let { cached.copy(it, true) } ?: cached
        }
        val bmp = runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.path)
            val art = retriever.embeddedPicture
            retriever.release()
            art?.let { bytes ->
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
                    val maxDim = max(outWidth, outHeight)
                    inSampleSize = if (maxDim > MAX_ALBUM_ART_DIMENSION) {
                        max(1, maxDim / MAX_ALBUM_ART_DIMENSION)
                    } else {
                        1
                    }
                    inJustDecodeBounds = false
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.let { decoded ->
                    if (decoded.width > MAX_ALBUM_ART_DIMENSION || decoded.height > MAX_ALBUM_ART_DIMENSION) {
                        val scale = min(
                            MAX_ALBUM_ART_DIMENSION.toFloat() / decoded.width,
                            MAX_ALBUM_ART_DIMENSION.toFloat() / decoded.height
                        )
                        val w = (decoded.width * scale).toInt()
                        val h = (decoded.height * scale).toInt()
                        decoded.scale(w, h, true).also { scaled ->
                            if (scaled != decoded) decoded.recycle()
                            scaled
                        }
                    } else decoded
                }
            }
        }.getOrNull()
        bmp?.let {
            bitmapCache.put(key, it)
            return@withContext it.config?.let { config -> it.copy(config, true) } ?: it
        }
        null
    }

    private suspend fun generateWaveform(file: File, width: Int = 64, height: Int = 32): Bitmap? = withContext(Dispatchers.IO) {
        val key = waveformKey(file.path)
        bitmapCache.get(key)?.let { cached ->
            return@withContext cached.config?.let { cached.copy(it, true) } ?: cached
        }
        if (file.length() > MAX_WAVEFORM_FILE_SIZE) return@withContext null
        runCatching {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(file.path)
                var trackIndex = 0
                while (trackIndex < extractor.trackCount) {
                    val format = extractor.getTrackFormat(trackIndex)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) break else trackIndex++
                }
                if (trackIndex >= extractor.trackCount) {
                    return@runCatching null
                }
                extractor.selectTrack(trackIndex)
                val format = extractor.getTrackFormat(trackIndex)
                val reported = if (format.containsKey(KEY_MAX_INPUT_SIZE)) {
                    format.getInteger(KEY_MAX_INPUT_SIZE)
                } else {
                    64 * 1024
                }
                val maxInput = min(reported, MAX_WAVEFORM_BYTES)
                val buffer = ByteBuffer.allocate(maxInput)
                val data = IntArray(width)
                var i = 0
                while (i < width) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    var sum = 0
                    for (b in 0 until size step 2) {
                        sum += abs(buffer.getShort(b).toInt())
                    }
                    data[i] = if (size > 0) sum / (size / 2) else 0
                    extractor.advance()
                    i++
                }
                val max = data.maxOrNull() ?: 0
                if (max == 0) return@runCatching null
                val bmp = createBitmap(width, height)
                val canvas = Canvas(bmp)
                val paint = Paint().apply { color = Color.WHITE }
                data.forEachIndexed { idx, amp ->
                    val ratio = amp.toFloat() / max
                    val barHeight = ratio * height
                    val x = idx.toFloat()
                    canvas.drawLine(x, height - barHeight, x, height.toFloat(), paint)
                }
                bmp
            } finally {
                extractor.release()
            }
        }.getOrNull()?.also { bitmapCache.put(key, it) }?.let { it.config?.let { config -> it.copy(config, true) } ?: it }
    }

    private fun isProbablyBinary(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        var nonPrintable = 0
        for (b in data) {
            val c = b.toInt() and 0xFF
            if (c in 9..13 || c in 32..126) continue else nonPrintable++
        }
        return nonPrintable.toFloat() / data.size > 0.3f
    }

    private suspend fun loadTextSnippet(file: File, lines: Int = 3, maxBytes: Int = 2048): String? = withContext(Dispatchers.IO) {
        if (file.length() > 1024 * 1024) return@withContext null
        runCatching {
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(maxBytes)
                val read = fis.read(buffer)
                if (read <= 0) return@runCatching null
                if (isProbablyBinary(buffer.copyOf(read))) return@runCatching null
            }
            file.bufferedReader().useLines { seq ->
                seq.take(lines).joinToString("\n")
            }
        }.getOrNull()
    }

    private suspend fun getArchiveEntryCount(file: File, maxEntries: Int = 10_000): Int? = withContext(Dispatchers.IO) {
        val ext = getFileExtension(file.name).lowercase()
        runCatching {
            when (ext) {
                "zip" -> ZipFile(file).use { zip ->
                    var count = 0
                    val entries = zip.entries()
                    while (entries.hasMoreElements() && count <= maxEntries) {
                        entries.nextElement()
                        count++
                    }
                    if (count > maxEntries) null else count
                }

                "rar" -> FileInputStream(file).use { fis: FileInputStream ->
                    ArchiveStreamFactory().createArchiveInputStream<ArchiveInputStream<out ArchiveEntry>>("rar", BufferedInputStream(fis))
                        .use { ais ->
                            var count = 0
                            while (ais.nextEntry != null && count <= maxEntries) {
                                count++
                            }
                            if (count > maxEntries) null else count
                        }
                }

                "7z", "7zip" -> SevenZFile.Builder().setFile(file).get().use { sevenZ ->
                    var count = 0
                    while (sevenZ.nextEntry != null && count <= maxEntries) {
                        count++
                    }
                    if (count > maxEntries) null else count
                }

                else -> null
            }
        }.getOrNull()
    }

    /** Represents the available preview strategies for files. */
    sealed class PreviewType {
        object Directory : PreviewType()
        object Image : PreviewType()
        object Video : PreviewType()
        object Apk : PreviewType()
        object Pdf : PreviewType()
        object Office : PreviewType()
        object Audio : PreviewType()
        object Text : PreviewType()
        object Archive : PreviewType()
        object Unknown : PreviewType()
    }

    fun getPreviewType(file: File): PreviewType {
        if (file.isDirectory) return PreviewType.Directory
        val mime = FileTypeResolver.getMimeType(file) ?: ""
        val category = FileTypeResolver.resolve(file)
        return when (category) {
            FileTypeResolver.Category.IMAGE -> PreviewType.Image
            FileTypeResolver.Category.VIDEO -> PreviewType.Video
            FileTypeResolver.Category.APK -> PreviewType.Apk
            FileTypeResolver.Category.AUDIO -> PreviewType.Audio
            FileTypeResolver.Category.ARCHIVE -> PreviewType.Archive
            FileTypeResolver.Category.DOCUMENT -> when {
                mime == "application/pdf" -> PreviewType.Pdf
                mime.startsWith("text/") -> PreviewType.Text
                else -> PreviewType.Office
            }
            FileTypeResolver.Category.FONT,
            FileTypeResolver.Category.WINDOWS,
            FileTypeResolver.Category.OTHER -> PreviewType.Unknown
        }
    }

    /**
     * Displays a preview of the provided [file]. Call this from any UI where a
     * preview icon or thumbnail is required.
     */
    @Composable
    fun Preview(file: File, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val type = remember(file) { getPreviewType(file) }
        when (type) {
            PreviewType.Directory -> {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Image -> {
                AppImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.FillWidth,
                    modifier = modifier.fillMaxWidth(),
                    size = 64,
                )
            }

            PreviewType.Video -> {
                AppImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = modifier.fillMaxSize(),
                    size = 64,
                    requestBuilder = {
                        decoderFactory { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                        videoFramePercent(0.5)
                    },
                )
            }

            PreviewType.Apk -> {
                val icon by produceState<android.graphics.drawable.Drawable?>(initialValue = null, file.path) {
                    value = withContext(Dispatchers.IO) {
                        context.packageManager.getPackageArchiveInfo(file.path, 0)?.applicationInfo?.apply {
                            sourceDir = file.path
                            publicSourceDir = file.path
                        }?.loadIcon(context.packageManager)
                    }
                }
                Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (icon == null) {
                        IconLoadingPlaceholder(
                            iconRes = R.drawable.ic_apk_document,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AppImage(
                            model = icon,
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            PreviewType.Pdf -> {
                val pdfBitmap by produceState<Bitmap?>(initialValue = null, file.path) {
                    value = loadPdfThumbnail(file)
                }
                val safe = pdfBitmap?.takeUnless { it.isRecycled }
                if (safe != null) {
                    DisposableEffect(safe) {
                        onDispose {
                            if (!safe.isRecycled) {
                                safe.recycle()
                            }
                        }
                    }
                    Image(
                        bitmap = safe.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.FillWidth,
                        modifier = modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Office -> {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Audio -> {
                val bitmap by produceState<Bitmap?>(initialValue = null, file.path) {
                    value = loadAlbumArt(file) ?: generateWaveform(file)
                }
                val safe = bitmap?.takeUnless { it.isRecycled }
                if (safe != null) {
                    DisposableEffect(safe) {
                        onDispose {
                            if (!safe.isRecycled) {
                                safe.recycle()
                            }
                        }
                    }
                    Image(
                        bitmap = safe.asImageBitmap(),
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = modifier.fillMaxWidth()
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_audio_file),
                        contentDescription = null,
                        modifier = modifier.size(24.dp)
                    )
                }
            }

            PreviewType.Text -> {
                val snippet by produceState<String?>(initialValue = null, file.path) {
                    value = loadTextSnippet(file)
                }
                snippet?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = modifier
                    )
                } ?: Icon(
                    painter = painterResource(id = R.drawable.ic_unknown_document),
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }

            PreviewType.Archive -> {
                val count by produceState<Int?>(initialValue = null, file.path) {
                    value = getArchiveEntryCount(file)
                }
                val extLabel = remember(file.name) { getFileExtension(file.name).uppercase() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = modifier
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_archive_filter),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    count?.let {
                        Text(
                            text = "$extLabel (${it} files)",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            PreviewType.Unknown -> {
                val icon = remember(getFileExtension(file.name)) {
                    getFileIcon(getFileExtension(file.name), context)
                }
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = modifier.size(24.dp)
                )
            }
        }
    }
}