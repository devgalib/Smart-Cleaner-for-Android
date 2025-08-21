package com.d4rk.cleaner.app.clean.analyze.ui.components

import android.view.View
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FilesByDateSection(
    modifier: Modifier,
    filesByDate: Map<String, List<File>>,
    fileSelectionStates: Map<File, Boolean>,
    onFileSelectionChange: (File, Boolean) -> Unit,
    onDateSelectionChange: (List<File>, Boolean) -> Unit,
    originals: Set<File> = emptySet(),
    view: View,
) {
    FilesGridLayout(modifier = modifier) {
        val sortedDates: List<String> = filesByDate.keys.sortedByDescending { dateString ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        }

        sortedDates.forEach { date ->
            val files: List<File> = filesByDate[date] ?: emptyList()
            if (files.isNotEmpty()) {
                item(key = date, span = { GridItemSpan(maxLineSpan) }) {
                    DateHeader(
                        files = files,
                        fileSelectionStates = fileSelectionStates,
                        onDateSelectionChange = { list, checked ->
                            onDateSelectionChange(list.filterNot { it.isProtectedAndroidDir() }, checked)
                        },
                        view = view
                    )
                }

                items(files, key = { it.path }) { file ->
                    FileCard(
                        file = file,
                        isChecked = fileSelectionStates[file] == true,
                        onCheckedChange = { checked -> onFileSelectionChange(file, checked) },
                        isOriginal = file in originals,
                        isProtected = file.isProtectedAndroidDir(),
                        sizeBytes = file.length(),
                        modifier = Modifier
                    )
                }
            }
        }
    }
}