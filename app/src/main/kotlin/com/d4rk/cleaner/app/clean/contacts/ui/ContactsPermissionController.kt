package com.d4rk.cleaner.app.clean.contacts.ui

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import com.d4rk.android.libs.apptoolkit.core.ui.effects.LifecycleEventsEffect
import com.d4rk.cleaner.core.utils.helpers.PermissionsHelper

enum class ContactsPermissionState { CHECKING, GRANTED, RATIONALE, DENIED }

@Composable
fun ContactsPermissionController(activity: Activity): MutableState<ContactsPermissionState> {
    val context = LocalContext.current
    val permissionState = remember { mutableStateOf(ContactsPermissionState.CHECKING) }

    LaunchedEffect(Unit) {
        when {
            PermissionsHelper.hasContactsPermissions(context) -> {
                permissionState.value = ContactsPermissionState.GRANTED
            }
            PermissionsHelper.shouldShowContactsPermissionRationale(activity) -> {
                permissionState.value = ContactsPermissionState.RATIONALE
            }
            else -> {
                PermissionsHelper.requestContactsPermissions(activity)
            }
        }
    }

    LifecycleEventsEffect(Lifecycle.Event.ON_RESUME) {
        permissionState.value = when {
            PermissionsHelper.hasContactsPermissions(context) -> ContactsPermissionState.GRANTED
            PermissionsHelper.shouldShowContactsPermissionRationale(activity) -> ContactsPermissionState.RATIONALE
            else -> ContactsPermissionState.DENIED
        }
    }

    return permissionState
}

