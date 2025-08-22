package com.d4rk.cleaner.app.clean.memory.data

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.memory.domain.data.model.InternalStorageInfo
import com.d4rk.cleaner.app.clean.memory.domain.data.model.RamInfo
import com.d4rk.cleaner.app.clean.memory.domain.data.model.StorageInfo
import com.d4rk.cleaner.app.clean.memory.domain.interfaces.MemoryRepository
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.StorageUtils
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.pow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryRepositoryImpl(private val application: Application) : MemoryRepository {

    override suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val storageBreakdown: Map<String, Long> = getStorageBreakdown(application)
        suspendCoroutine { continuation ->
            StorageUtils.getStorageInfo(application) { used, total, _, _, _ ->
                val usedStorageBytes: Double =
                    (used.toDoubleOrNull() ?: 0.0) * 1024.0.pow(n = 3)
                val totalStorageBytes: Double =
                    (total.toDoubleOrNull() ?: 0.0) * 1024.0.pow(n = 3)

                val storageInfo = StorageInfo(
                    storageUsageProgress = totalStorageBytes.toFloat(),
                    freeStorage = (totalStorageBytes - usedStorageBytes).toLong(),
                    usedStorage = usedStorageBytes.toLong(),
                    storageBreakdown = storageBreakdown
                )
                continuation.resume(storageInfo)
            }
        }
    }

    override fun getRamInfo(): RamInfo {
        val activityManager: ActivityManager =
            application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return RamInfo(
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            usedRam = memoryInfo.totalMem - memoryInfo.availMem
        )
    }

    private suspend fun getStorageBreakdown(context: Context): Map<String, Long> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val breakdown: MutableMap<String, Long> = mutableMapOf()
                val externalStoragePath: String =
                    context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile?.absolutePath
                        ?: "/"

                val installedAppsDeferred = async { getInstalledAppsSize(context) }
                val systemDeferred = async { getDirectorySize(Environment.getRootDirectory()) }
                val musicDeferred = async { getDirectorySize(File(externalStoragePath, "Music")) }
                val dcimDeferred = async { getDirectorySize(File(externalStoragePath, "DCIM")) }
                val picturesDeferred = async {
                    getDirectorySize(File(externalStoragePath, "Pictures"))
                }
                val documentsDeferred =
                    async { getDirectorySize(File(externalStoragePath, "Documents")) }
                val downloadsDeferred =
                    async { getDirectorySize(File(externalStoragePath, "Download")) }

                val sizes = awaitAll(
                    installedAppsDeferred,
                    systemDeferred,
                    musicDeferred,
                    dcimDeferred,
                    picturesDeferred,
                    documentsDeferred,
                    downloadsDeferred
                )

                val installedAppsSize = sizes[0]
                val systemSize = sizes[1]
                val musicSize = sizes[2]
                val dcimSize = sizes[3]
                val picturesSize = sizes[4]
                val documentsSize = sizes[5]
                val downloadsSize = sizes[6]

                breakdown[context.getString(R.string.installed_apps)] = installedAppsSize
                breakdown[context.getString(R.string.system)] = systemSize
                breakdown[context.getString(R.string.music)] = musicSize
                breakdown[context.getString(R.string.images)] = dcimSize + picturesSize
                breakdown[context.getString(R.string.documents)] = documentsSize
                breakdown[context.getString(R.string.downloads)] = downloadsSize
                breakdown[context.getString(R.string.other_files)] = getOtherFilesSize(breakdown)

                breakdown
            }
        }

    private suspend fun getInstalledAppsSize(context: Context): Long {
        val packageManager: PackageManager = context.packageManager
        val installedApps: MutableList<ApplicationInfo> = packageManager.getInstalledApplications(0)
        return coroutineScope {
            installedApps.map { async { getApkSize(context, it.packageName) } }.awaitAll().sum()
        }
    }

    private fun getApkSize(context: Context, packageName: String): Long {
        return runCatching {
            context.packageManager.getApplicationInfo(
                packageName, 0
            ).sourceDir.let { File(it).length() }
        }.getOrDefault(0L)
    }

    private fun getDirectorySize(directory: File?): Long {
        if (directory == null || !directory.exists() || !directory.isDirectory) return 0
        var size = 0L
        val files: Array<out File>? = directory.listFiles()
        if (files != null) {
            for (file: File in files) {
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    private fun getOtherFilesSize(breakdown: MutableMap<String, Long>): Long {
        val totalUsedStorage: Long = getInternalStorageInfo().usedStorage
        val calculatedSize: Long = breakdown.values.sum()
        return totalUsedStorage - calculatedSize
    }

    private fun getInternalStorageInfo(): InternalStorageInfo {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val blockSizeBytes: Long = statFs.blockSizeLong
        val totalBlocks: Long = statFs.blockCountLong
        val availableBlocks: Long = statFs.availableBlocksLong

        val totalStorage: Long = totalBlocks * blockSizeBytes
        val freeStorage: Long = availableBlocks * blockSizeBytes
        val usedStorage: Long = totalStorage - freeStorage

        return InternalStorageInfo(totalStorage, freeStorage, usedStorage)
    }
}