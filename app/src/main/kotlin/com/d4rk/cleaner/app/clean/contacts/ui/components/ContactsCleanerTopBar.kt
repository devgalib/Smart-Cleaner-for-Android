package com.d4rk.cleaner.app.clean.contacts.ui.components

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.AnimatedIconButtonDirection
import com.d4rk.cleaner.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsCleanerTopBar(
    activity: Activity,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
        title = {
            Text(
                modifier = Modifier.animateContentSize(),
                text = stringResource(id = R.string.contacts_cleaner_title),
            )
        },
        navigationIcon = {
            AnimatedIconButtonDirection(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = com.d4rk.android.libs.apptoolkit.R.string.go_back),
                onClick = { activity.finish() }
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

