package com.d4rk.cleaner.app.clean.analyze.ui.components

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.bounceClick
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import com.d4rk.cleaner.core.utils.helpers.FileManagerHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCard(
    modifier: Modifier = Modifier,
    file: File,
    onCheckedChange: (Boolean) -> Unit,
    isChecked: Boolean,
    isOriginal: Boolean = false,
    isProtected: Boolean = false,
    fileSize : Int = 0,
) {
    val context: Context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view: View = LocalView.current
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

                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        haptic.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                        onCheckedChange(checked)
                    },
                    enabled = !isProtected,
                    modifier = Modifier.align(alignment = Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                ) {
                    if (isOriginal) {
                        Text(
                            text = stringResource(id = R.string.original),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(bottomEnd = SizeConstants.MediumSize)
                                )
                                .padding(all = SizeConstants.SmallSize)
                        )
                    }

                    if (fileSize > 0) {
                        Text(
                            text = "23 KB", // TODO: add file size with format KB MB GB
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(bottomEnd = SizeConstants.MediumSize)
                                )
                                .padding(all = SizeConstants.SmallSize)
                        )
                    }
                }

                if (isProtected) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(id = R.string.protected_icon_description),
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                        )
                        .align(alignment = Alignment.BottomCenter),
                ) {
                    Text(
                        text = file.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .basicMarquee()
                            .padding(all = SizeConstants.SmallSize),
                    )
                }
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

