package com.d4rk.cleaner.app.clean.analyze.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.d4rk.android.libs.apptoolkit.core.ui.components.dialogs.BasicAlertDialog
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.analyze.ui.AnalyzeViewModel
import com.d4rk.cleaner.app.clean.scanner.domain.actions.ScannerEvent
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel

@Composable
fun DeleteOrTrashConfirmation(data: UiScannerModel, viewModel: AnalyzeViewModel) {
    val isDeleteDialog = data.analyzeState.isDeleteForeverConfirmationDialogVisible

    val titleRes =
        if (isDeleteDialog) R.string.delete_forever_title else R.string.move_to_trash_title
    val messageRes =
        if (isDeleteDialog) R.string.delete_forever_message else R.string.move_to_trash_message
    val confirmButtonText = if (isDeleteDialog) R.string.clean_all else R.string.moving

    BasicAlertDialog(
        title = stringResource(id = titleRes), content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.delete_anim))
                LottieAnimation(
                    modifier = Modifier.fillMaxWidth(),
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    contentScale = ContentScale.Crop
                )
                Text(text = stringResource(id = messageRes))
            }
        }, onConfirm = {
            if (isDeleteDialog) {
                viewModel.onScannerEvent(ScannerEvent.CleanFiles)
                viewModel.onScannerEvent(ScannerEvent.SetDeleteForeverConfirmationDialogVisibility(false))
            } else {
                viewModel.onScannerEvent(ScannerEvent.MoveSelectedToTrash)
                viewModel.onScannerEvent(ScannerEvent.SetMoveToTrashConfirmationDialogVisibility(false))
            }
        },

        confirmButtonText = stringResource(id = confirmButtonText),
        onDismiss = {
            if (isDeleteDialog) {
                viewModel.onScannerEvent(ScannerEvent.SetDeleteForeverConfirmationDialogVisibility(false))
            } else {
                viewModel.onScannerEvent(ScannerEvent.SetMoveToTrashConfirmationDialogVisibility(false))
            }
        })
}