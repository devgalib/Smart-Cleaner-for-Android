package com.d4rk.cleaner.app.clean.contacts.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.d4rk.android.libs.apptoolkit.core.domain.model.ads.AdsConfig
import com.d4rk.android.libs.apptoolkit.core.ui.components.buttons.IconButtonWithText
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.LoadingScreen
import com.d4rk.android.libs.apptoolkit.core.ui.components.layouts.ScreenStateHandler
import com.d4rk.android.libs.apptoolkit.core.ui.effects.LifecycleEventsEffect
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerEvent
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.UiContactsCleanerModel
import com.d4rk.cleaner.app.clean.contacts.ui.components.ContactsCleanerBottomBar
import com.d4rk.cleaner.app.clean.contacts.ui.components.ContactsCleanerList
import com.d4rk.cleaner.app.clean.contacts.ui.components.ContactsCleanerTopBar
import com.d4rk.cleaner.app.clean.contacts.ui.components.SelectionState
import com.d4rk.cleaner.app.clean.contacts.ui.components.dialogs.PermissionRationaleDialog
import com.d4rk.cleaner.app.clean.contacts.ui.components.states.ContactsEmptyState
import com.d4rk.cleaner.app.clean.contacts.ui.components.states.ContactsErrorState
import com.d4rk.cleaner.core.utils.helpers.PermissionsHelper
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsCleanerScreen(activity: Activity) {
    val viewModel: ContactsCleanerViewModel = koinViewModel()
    val state = viewModel.uiState.collectAsState().value
    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val permissionState = ContactsPermissionController(activity)

    val bottomBarAdConfig: AdsConfig = koinInject(qualifier = named(name = "full_banner"))

    val selectedCount by remember(state.data) {
        derivedStateOf {
            state.data?.duplicates
                ?.flatMap { it.contacts }
                ?.count { it.isSelected } ?: 0
        }
    }

    val selectionState by remember(state.data) {
        derivedStateOf {
            val groups = state.data?.duplicates ?: emptyList()
            val selectedGroups = groups.filter { group -> group.contacts.any { it.isSelected } }
            val total =
                selectedGroups.sumOf { group -> group.contacts.count { contact -> contact.isSelected } }
            when {
                total == 1 -> SelectionState.SINGLE
                selectedGroups.size == 1 -> SelectionState.SAME_GROUP
                else -> SelectionState.MULTIPLE_GROUPS
            }
        }
    }

    val canMerge by remember(state.data) {
        derivedStateOf {
            state.data?.duplicates?.any { group ->
                group.contacts.count { it.isSelected } >= 2
            } ?: false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ContactsCleanerTopBar(activity = activity, scrollBehavior = scrollBehavior)
        },
        bottomBar = {
            ContactsCleanerBottomBar(
                selectedCount = selectedCount,
                selectionState = selectionState,
                canMerge = canMerge,
                onMerge = { viewModel.onEvent(ContactsCleanerEvent.MergeSelectedContacts) },
                onDelete = { viewModel.onEvent(ContactsCleanerEvent.DeleteSelectedContacts) },
                bottomBarAdConfig = bottomBarAdConfig,
            )
        },
    ) { paddingValues ->
        when (permissionState.value) {
            ContactsPermissionState.GRANTED -> {
                ScreenStateHandler(
                    screenState = state,
                    onLoading = { LoadingScreen() },
                    onEmpty = { ContactsEmptyState(paddingValues = paddingValues) },
                    onSuccess = { data: UiContactsCleanerModel ->
                        ContactsCleanerList(
                            data = data,
                            viewModel = viewModel,
                            paddingValues = paddingValues,
                        )
                    },
                    onError = {
                        ContactsErrorState(paddingValues = paddingValues) {
                            viewModel.onEvent(ContactsCleanerEvent.LoadDuplicates)
                        }
                    },
                )

                LifecycleEventsEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.onEvent(ContactsCleanerEvent.LoadDuplicates)
                }
            }

            ContactsPermissionState.RATIONALE -> {
                PermissionRationaleDialog(
                    onRequest = { PermissionsHelper.requestContactsPermissions(activity) },
                ) {
                    permissionState.value = ContactsPermissionState.DENIED
                }
            }

            ContactsPermissionState.DENIED -> {
                PermissionDeniedScreen(
                    onOpenSettings = { PermissionsHelper.openAppSettings(activity) },
                )
            }

            ContactsPermissionState.CHECKING -> Unit
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SizeConstants.LargeSize),
        verticalArrangement = Arrangement.spacedBy(SizeConstants.LargeSize),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Contacts,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(id = R.string.contacts_permission_denied),
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButtonWithText(
            onClick = onOpenSettings,
            label = stringResource(id = R.string.open_settings),
        )
    }
}

