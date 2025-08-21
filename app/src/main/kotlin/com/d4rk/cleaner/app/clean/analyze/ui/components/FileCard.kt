package com.d4rk.cleaner.app.clean.analyze.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.ColorUtils
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.bounceClick
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import com.d4rk.cleaner.core.utils.helpers.FileManagerHelper
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCard(
    modifier: Modifier = Modifier,
    file: File,
    onCheckedChange: (Boolean) -> Unit,
    isChecked: Boolean,
    isOriginal: Boolean = false,
    isProtected: Boolean = false,
    sizeBytes: Long = 0L,
) {
    val context: Context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view: View = LocalView.current
    val defaultColor = MaterialTheme.colorScheme.onSurface
    val textColor by produceState(initialValue = defaultColor, file.path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeFile(file.path, options)?.let { bmp ->
                    val pixels = IntArray(bmp.width * bmp.height)
                    bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                    bmp.recycle()
                    var r = 0L
                    var g = 0L
                    var b = 0L
                    for (p in pixels) {
                        r += android.graphics.Color.red(p)
                        g += android.graphics.Color.green(p)
                        b += android.graphics.Color.blue(p)
                    }
                    val count = pixels.size
                    val avg = android.graphics.Color.rgb(
                        (r / count).toInt(),
                        (g / count).toInt(),
                        (b / count).toInt()
                    )
                    val luminance = ColorUtils.calculateLuminance(avg)
                    if (luminance > 0.5) Color.Black else Color.White
                } ?: defaultColor
            }.getOrDefault(defaultColor)
        }
    }
    val cardContent: @Composable () -> Unit = {
        Card(
            modifier = modifier
                .aspectRatio(ratio = 1f)
                .then(if (!isProtected) Modifier.bounceClick() else Modifier)
                .clickable(enabled = !isProtected) {
                    if (!file.isDirectory) {
                        FileManagerHelper.openFile(context, file)
                    }
                },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FilePreviewHelper.Preview(file = file, modifier = Modifier.align(Alignment.Center))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        if (isOriginal) {
                            Text(
                                text = stringResource(id = R.string.original),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(bottomEnd = SizeConstants.MediumSize)
                                    )
                                    .padding(all = SizeConstants.SmallSize)
                            )
                        }

                        if (sizeBytes > 0L) {
                            Text(
                                text = FileSizeFormatter.format(sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(bottomEnd = SizeConstants.MediumSize)
                                    )
                                    .padding(all = SizeConstants.SmallSize)
                            )
                        }
                    }

                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            haptic.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                            onCheckedChange(checked)
                        },
                        enabled = !isProtected,
                    )
                }

                if (isProtected) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(id = R.string.protected_icon_description),
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Text(
                    text = file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(all = SizeConstants.SmallSize),
                )
            }
        }
    }

    if (isProtected) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { Text(text = stringResource(id = R.string.protected_system_folder_message)) },
            state = rememberTooltipState(),
        ) {
            cardContent()
        }
    } else {
        cardContent()
    }
}

