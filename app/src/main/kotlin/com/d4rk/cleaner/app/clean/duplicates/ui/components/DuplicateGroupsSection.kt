package com.d4rk.cleaner.app.clean.duplicates.ui.components

import android.view.View
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.app.clean.analyze.ui.components.FilesGridLayout
import com.d4rk.cleaner.app.clean.analyze.ui.components.DateHeader
import com.d4rk.cleaner.app.clean.analyze.ui.components.FileCard
import com.d4rk.cleaner.core.utils.helpers.isProtectedAndroidDir
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DuplicateGroupsSection(
    modifier: Modifier,
    filesByDate: Map<String, List<List<File>>>,
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
            val groups: List<List<File>> = filesByDate[date] ?: emptyList()
            val allFiles: List<File> = groups.flatten()
            if (allFiles.isNotEmpty()) {
                item(key = date, span = { GridItemSpan(maxLineSpan) }) {
                    DateHeader(
                        files = allFiles,
                        fileSelectionStates = fileSelectionStates,
                        onDateSelectionChange = { list, checked ->
                            onDateSelectionChange(list.filterNot { it.isProtectedAndroidDir() }, checked)
                        },
                        view = view
                    )
                }

                groups.forEachIndexed { index, group ->
                    items(group, key = { it.path }) { file ->
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
                    if (index < groups.lastIndex) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(SizeConstants.MediumSize))
                        }
                    }
                }
            }
        }
    }
}