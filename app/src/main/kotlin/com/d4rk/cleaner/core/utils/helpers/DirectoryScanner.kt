package com.d4rk.cleaner.core.utils.helpers

import java.io.File

/**
 * Recursively scans directories starting at [root]. Hidden files are skipped
 * unless [showHidden] is true. Directories for which [skipDir] returns true or
 * protected Android directories will not be entered. When a protected directory
 * is encountered, [onLockedDir] is invoked.
 */
object DirectoryScanner {
    fun scan(
        root: File,
        showHidden: Boolean = false,
        skipDir: (File) -> Boolean = { false },
        onLockedDir: ((File) -> Unit)? = null,
        onFile: (File) -> Unit
    ) {
        if (!root.exists()) return
        root.walkTopDown()
            .onEnter { dir ->
                if ((!showHidden && dir.isHidden) || skipDir(dir)) {
                    if (dir.isProtectedAndroidDir()) onLockedDir?.invoke(dir)
                    return@onEnter false
                }
                if (dir.isProtectedAndroidDir()) {
                    onLockedDir?.invoke(dir)
                    return@onEnter false
                }
                true
            }
            .forEach { file ->
                if (file.isFile && (showHidden || !file.isHidden)) {
                    onFile(file)
                }
            }
    }
}
