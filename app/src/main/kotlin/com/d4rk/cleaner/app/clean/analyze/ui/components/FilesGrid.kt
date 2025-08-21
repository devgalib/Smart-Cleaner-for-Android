package com.d4rk.cleaner.app.clean.analyze.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.ScreenHelper
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import java.io.File

@Composable
fun FilesGridLayout(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val columns: Int = if (ScreenHelper.isTablet(context = LocalContext.current)) 6 else 3

    LazyVerticalGrid(
        columns = GridCells.Fixed(count = columns),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SizeConstants.SmallSize),
        content = content,
    )
}

@Composable
fun FilesGrid(
    files: List<File>,
    fileSelectionStates: Map<File, Boolean>,
    onFileSelectionChange: (File, Boolean) -> Unit,
    originals: Set<File> = emptySet(),
    modifier: Modifier = Modifier,
    onFileLongPress: ((File) -> Unit)? = null,
) {
    FilesGridLayout(modifier = modifier) {
        items(files, key = { it.path }) { file ->
            val isProtected = file.isProtectedAndroidDir()
            FileCard(
                file = file,
                isChecked = fileSelectionStates[file] == true,
                onCheckedChange = { isChecked -> onFileSelectionChange(file, isChecked) },
                isOriginal = file in originals,
                isProtected = isProtected,
                sizeBytes = file.length(),
                modifier = Modifier.then(
                    if (onFileLongPress != null && !isProtected) {
                        Modifier.pointerInput(file) {
                            detectTapGestures(onLongPress = { onFileLongPress(file) })
                        }
                    } else {
                        Modifier
                    }
                )
            )
        }
    }
}
