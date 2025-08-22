package com.d4rk.cleaner.app.clean.contacts.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.d4rk.android.libs.apptoolkit.core.di.DispatcherProvider
import com.d4rk.android.libs.apptoolkit.core.domain.model.network.DataState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.ScreenState
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiSnackbar
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import com.d4rk.android.libs.apptoolkit.core.ui.base.ScreenViewModel
import com.d4rk.android.libs.apptoolkit.core.utils.helpers.UiTextHelper
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerAction
import com.d4rk.cleaner.app.clean.contacts.domain.actions.ContactsCleanerEvent
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.DuplicateContactGroup
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.RawContactInfo
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.UiContactsCleanerModel
import com.d4rk.cleaner.app.clean.contacts.domain.usecases.DeleteContactsUseCase
import com.d4rk.cleaner.app.clean.contacts.domain.usecases.DeleteOlderContactsUseCase
import com.d4rk.cleaner.app.clean.contacts.domain.usecases.GetDuplicateContactsUseCase
import com.d4rk.cleaner.app.clean.contacts.domain.usecases.MergeContactsUseCase
import com.d4rk.cleaner.app.clean.scanner.work.FileCleanupWorker
import com.d4rk.cleaner.core.data.datastore.DataStore
import com.d4rk.cleaner.core.utils.extensions.asUiText
import com.d4rk.cleaner.core.utils.helpers.LogHelper
import com.d4rk.cleaner.core.work.FileCleanWorkEnqueuer
import com.d4rk.cleaner.core.work.FileCleaner
import com.d4rk.cleaner.core.work.observeFileCleanWork
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.util.UUID

class ContactsCleanerViewModel(
    private val application: Application,
    private val getDuplicatesUseCase: GetDuplicateContactsUseCase,
    private val deleteOlderUseCase: DeleteOlderContactsUseCase,
    private val deleteContactsUseCase: DeleteContactsUseCase,
    private val mergeContactsUseCase: MergeContactsUseCase,
    private val dataStore: DataStore,
    private val dispatchers: DispatcherProvider,
    private val fileCleanWorkEnqueuer: FileCleanWorkEnqueuer,
) : ScreenViewModel<UiContactsCleanerModel, ContactsCleanerEvent, ContactsCleanerAction>(
    initialState = UiStateScreen(data = UiContactsCleanerModel())
) {

    companion object {
        private const val TAG = LogHelper.CONTACTS_CLEANER_VIEW_MODEL
    }

    private var activeWorkObserver: Job? = null

    init {
        onEvent(ContactsCleanerEvent.LoadDuplicates)
        launch(dispatchers.io) {
            dataStore.contactsCleanWorkId.first()?.let { id ->
                val uuid = UUID.fromString(id)
                val info = WorkManager.getInstance(application)
                    .getWorkInfoByIdFlow(uuid)
                    .first()
                if (info == null || info.state.isFinished) {
                    dataStore.clearContactsCleanWorkId()
                } else {
                    observeWork(uuid) {}
                }
            }
        }
    }

    override fun onEvent(event: ContactsCleanerEvent) {
        when (event) {
            ContactsCleanerEvent.LoadDuplicates -> loadDuplicates()
            is ContactsCleanerEvent.DeleteOlder ->
                enqueueContactClean(event.group) { performDeleteOlder(event.group) }

            is ContactsCleanerEvent.MergeAll ->
                enqueueContactClean(event.group) { performMerge(event.group) }

            is ContactsCleanerEvent.ToggleGroupSelection -> handleToggleGroupSelection(event.group)
            is ContactsCleanerEvent.ToggleContactSelection -> handleToggleContactSelection(event.contact)
            ContactsCleanerEvent.MergeSelectedContacts -> handleMergeSelectedContacts()
            ContactsCleanerEvent.DeleteSelectedContacts -> handleDeleteSelectedContacts()
            ContactsCleanerEvent.ToggleSelectAll -> handleToggleSelectAll()
        }
    }

    private fun loadDuplicates() {
        launch(context = dispatchers.default) {
            getDuplicatesUseCase().collectLatest { result ->
                _uiState.update { current ->

                    Log.d(TAG, "result: $result")
                    Log.d(TAG, "error: ${(result as? DataState.Error)?.error}")
                    Log.d(TAG, "message error: ${current.errors}")


                    when (result) {
                        is DataState.Loading -> current.copy(screenState = ScreenState.IsLoading())
                        is DataState.Success -> current.copy(
                            screenState = if (result.data.isEmpty()) ScreenState.NoData() else ScreenState.Success(),
                            data = UiContactsCleanerModel(
                                result.data.map { group ->
                                    DuplicateContactGroup(
                                        contacts = group.map { it.copy(isSelected = false) },
                                        isSelected = false
                                    )
                                }
                            )
                        )

                        is DataState.Error -> current.copy(
                            screenState = ScreenState.Error(),
                            errors = current.errors + UiSnackbar(
                                message = result.error.asUiText(),
                                isError = true
                            )
                        )
                    }
                }
            }
        }
    }

    private fun enqueueContactClean(
        contacts: List<RawContactInfo>,
        onSuccess: suspend () -> Unit,
    ) {
        launch(dispatchers.io) {
            FileCleaner.enqueue(
                enqueuer = fileCleanWorkEnqueuer,
                paths = contacts.map { it.rawContactId.toString() },
                action = FileCleanupWorker.ACTION_DELETE,
                getWorkId = { dataStore.contactsCleanWorkId.first() },
                saveWorkId = { dataStore.saveContactsCleanWorkId(it) },
                clearWorkId = { dataStore.clearContactsCleanWorkId() },
                showSnackbar = { snackbar ->
                    _uiState.update { current ->
                        current.copy(errors = current.errors + snackbar)
                    }
                },
                onEnqueued = { id -> observeWork(id, onSuccess) },
                onError = {
                    _uiState.update { current ->
                        current.copy(
                            errors = current.errors + UiSnackbar(
                                message = UiTextHelper.StringResource(R.string.failed_to_delete_files),
                                isError = true,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun observeWork(id: UUID, onSuccess: suspend () -> Unit) {
        activeWorkObserver = observeFileCleanWork(
            previousObserver = activeWorkObserver,
            scope = viewModelScope,
            application = application,
            dispatcher = dispatchers.io,
            workId = id,
            clearWorkId = { dataStore.clearContactsCleanWorkId() },
            onRunning = {
                _uiState.update { it.copy(screenState = ScreenState.IsLoading()) }
            },
            onSuccess = {
                onSuccess()
                _uiState.update { it.copy(screenState = ScreenState.Success()) }
                onEvent(ContactsCleanerEvent.LoadDuplicates)
            },
            onFailed = {
                _uiState.update {
                    it.copy(
                        screenState = ScreenState.Error(),
                        errors = it.errors + UiSnackbar(
                            message = UiTextHelper.StringResource(R.string.failed_to_delete_files),
                            isError = true,
                        ),
                    )
                }
            },
            onCancelled = {
                _uiState.update { it.copy(screenState = ScreenState.Success()) }
            },
        )
    }

    private suspend fun performDeleteOlder(group: List<RawContactInfo>) {
        deleteOlderUseCase(group).collectLatest { result ->
            if (result is DataState.Error) {
                _uiState.update { current ->
                    current.copy(
                        errors = current.errors + UiSnackbar(
                            message = result.error.asUiText(),
                            isError = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun performMerge(group: List<RawContactInfo>) {
        mergeContactsUseCase(group).collectLatest { result ->
            if (result is DataState.Error) {
                _uiState.update { current ->
                    current.copy(
                        errors = current.errors + UiSnackbar(
                            message = result.error.asUiText(),
                            isError = true
                        )
                    )
                }
            }
        }
    }

    private suspend fun performDeleteContacts(contacts: List<RawContactInfo>) {
        deleteContactsUseCase(contacts).collectLatest { result ->
            if (result is DataState.Error) {
                _uiState.update { current ->
                    current.copy(
                        errors = current.errors + UiSnackbar(
                            message = result.error.asUiText(),
                            isError = true
                        )
                    )
                }
            }
        }
    }

    private fun handleToggleGroupSelection(group: List<RawContactInfo>) {
        launch(context = dispatchers.default) {
            _uiState.update { current ->
                val updated = current.data?.duplicates?.map { duplicateGroup ->
                    if (duplicateGroup.contacts == group) {
                        val shouldSelect = duplicateGroup.contacts.any { !it.isSelected }
                        val updatedContacts =
                            duplicateGroup.contacts.map { it.copy(isSelected = shouldSelect) }
                        duplicateGroup.copy(contacts = updatedContacts, isSelected = shouldSelect)
                    } else {
                        duplicateGroup
                    }
                }
                current.copy(data = current.data?.copy(duplicates = updated ?: emptyList()))
            }
        }
    }

    private fun handleToggleContactSelection(contact: RawContactInfo) {
        launch(context = dispatchers.default) {
            _uiState.update { current ->
                val updatedGroups = current.data?.duplicates?.map { group ->
                    val updatedContacts = group.contacts.map { raw ->
                        if (raw.rawContactId == contact.rawContactId) raw.copy(isSelected = !raw.isSelected) else raw
                    }
                    val groupSelected = updatedContacts.all { it.isSelected }
                    group.copy(contacts = updatedContacts, isSelected = groupSelected)
                }
                current.copy(data = current.data?.copy(duplicates = updatedGroups ?: emptyList()))
            }
        }
    }

    private fun handleMergeSelectedContacts() {
        launch(context = dispatchers.default) {
            val groups = _uiState.value.data?.duplicates ?: emptyList()
            val selectedGroups = groups.mapNotNull { group ->
                val contacts = group.contacts.filter { it.isSelected }
                if (contacts.size >= 2) contacts else null
            }
            if (selectedGroups.isEmpty()) return@launch
            val allContacts = selectedGroups.flatten()
            enqueueContactClean(allContacts) {
                selectedGroups.forEach { performMerge(it) }
            }
        }
    }

    private fun handleDeleteSelectedContacts() {
        launch(context = dispatchers.default) {
            val groups = _uiState.value.data?.duplicates ?: emptyList()
            val selectedGroups = groups.mapNotNull { group ->
                val contacts = group.contacts.filter { it.isSelected }
                contacts.ifEmpty { null }
            }
            if (selectedGroups.isEmpty()) return@launch
            val totalSelected = selectedGroups.sumOf { it.size }
            if (totalSelected == 1) {
                val toDelete = selectedGroups.first()
                enqueueContactClean(toDelete) { performDeleteContacts(toDelete) }
            } else {
                val groupsToDelete = selectedGroups.filter { it.size > 1 }
                if (groupsToDelete.isEmpty()) return@launch
                val allContacts = groupsToDelete.flatten()
                enqueueContactClean(allContacts) {
                    groupsToDelete.forEach { performDeleteOlder(it) }
                }
            }
        }
    }

    private fun handleToggleSelectAll() {
        launch(context = dispatchers.default) {
            _uiState.update { current ->
                val shouldSelect = current.data?.duplicates
                    ?.flatMap { it.contacts }
                    ?.any { !it.isSelected } ?: false
                val updatedGroups = current.data?.duplicates?.map { group ->
                    val updatedContacts = group.contacts.map { it.copy(isSelected = shouldSelect) }
                    group.copy(contacts = updatedContacts, isSelected = shouldSelect)
                }
                current.copy(data = current.data?.copy(duplicates = updatedGroups ?: emptyList()))
            }
        }
    }
}
