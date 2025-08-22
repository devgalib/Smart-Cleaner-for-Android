package com.d4rk.cleaner.app.clean.contacts.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import com.d4rk.cleaner.app.clean.contacts.domain.data.model.RawContactInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepositoryImpl(context: Context) : ContactsRepository {
    private val resolver = context.contentResolver

    private fun normalizePhone(number: String): String =
        number.filterIndexed { index, c -> c.isDigit() || (index == 0 && c == '+') }

    private fun normalizeName(name: String): String =
        name.filter { it.isLetterOrDigit() }.lowercase()

    private fun mergeDisplayName(group: List<RawContactInfo>): String {
        val parts = linkedSetOf<String>()
        group.forEach { info ->
            info.displayName.split(" ").forEach { word ->
                val trimmed = word.trim()
                if (trimmed.isNotEmpty() &&
                    parts.none { it.equals(trimmed, ignoreCase = true) }
                ) {
                    parts.add(trimmed)
                }
            }
        }
        return parts.joinToString(" ")
    }

    override suspend fun findDuplicates(): List<List<RawContactInfo>> {
        val contacts = withContext(Dispatchers.IO) {
            val list = mutableListOf<RawContactInfo>()
            val rawIdToIndex = mutableMapOf<Long, Int>()

            val contactTimestamps = mutableMapOf<Long, Long>()
            val contactsProjection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
            )
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                contactsProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val updatedIdx = cursor.getColumnIndexOrThrow(
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
                )
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val updated = cursor.getLong(updatedIdx)
                    contactTimestamps[id] = updated
                }
            }

            val projection = arrayOf(
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME
            )

            resolver.query(ContactsContract.RawContacts.CONTENT_URI, projection, null, null, null)
                ?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
                    val contactIdIdx =
                        cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID)
                    val nameIdx =
                        cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY)
                    val typeIdx =
                        cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    val accIdx =
                        cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)

                    while (cursor.moveToNext()) {
                        val rawId = cursor.getLong(idIdx)
                        val contactId = cursor.getLong(contactIdIdx)
                        val name = cursor.getString(nameIdx) ?: ""
                        val updated = contactTimestamps[contactId] ?: 0L
                        val accountType = cursor.getString(typeIdx)
                        val accountName = cursor.getString(accIdx)
                        val phones = mutableListOf<String>()
                        val emails = mutableListOf<String>()
                        list.add(
                            RawContactInfo(
                                contactId,
                                rawId,
                                name,
                                accountType,
                                accountName,
                                updated,
                                phones,
                                emails
                            )
                        )
                        rawIdToIndex[rawId] = list.lastIndex
                    }
                }

            val phoneProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val rawIdx =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
                val numIdx =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val rawId = cursor.getLong(rawIdx)
                    val number = cursor.getString(numIdx)
                    val index = rawIdToIndex[rawId] ?: continue
                    val normalized = normalizePhone(number)
                    if (normalized.isNotEmpty()) {
                        (list[index].phones as MutableList).add(normalized)
                    }
                }
            }

            val emailProjection = arrayOf(
                ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )
            resolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                emailProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val rawIdx =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.RAW_CONTACT_ID)
                val addrIdx =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                while (cursor.moveToNext()) {
                    val rawId = cursor.getLong(rawIdx)
                    val address = cursor.getString(addrIdx)?.lowercase() ?: continue
                    val index = rawIdToIndex[rawId] ?: continue
                    (list[index].emails as MutableList).add(address)
                }
            }

            list
        }

        if (contacts.isEmpty()) return emptyList()

        return withContext(Dispatchers.Default) {
            val parent = IntArray(contacts.size) { it }

            fun find(x: Int): Int {
                var r = x
                while (parent[r] != r) r = parent[r]
                var i = x
                while (parent[i] != r) {
                    val p = parent[i]
                    parent[i] = r
                    i = p
                }
                return r
            }

            fun union(a: Int, b: Int) {
                val ra = find(a)
                val rb = find(b)
                if (ra != rb) parent[rb] = ra
            }

            val phoneGroups = mutableMapOf<String, MutableList<Int>>()
            val emailGroups = mutableMapOf<String, MutableList<Int>>()
            contacts.forEachIndexed { index, info ->
                info.phones.forEach { phone ->
                    phoneGroups.getOrPut(phone) { mutableListOf() }.add(index)
                }
                info.emails.forEach { email ->
                    emailGroups.getOrPut(email) { mutableListOf() }.add(index)
                }
            }

            phoneGroups.values.forEach { group ->
                for (i in 1 until group.size) union(group[0], group[i])
            }
            emailGroups.values.forEach { group ->
                for (i in 1 until group.size) union(group[0], group[i])
            }

            val nameGroups = mutableMapOf<String, MutableList<Int>>()
            contacts.forEachIndexed { index, info ->
                val norm = normalizeName(info.displayName)
                if (norm.isNotEmpty()) nameGroups.getOrPut(norm) { mutableListOf() }.add(index)
            }
            nameGroups.values.forEach { group ->
                if (group.size > 1) {
                    for (i in 0 until group.size) {
                        val aIdx = group[i]
                        val a = contacts[aIdx]
                        for (j in i + 1 until group.size) {
                            val bIdx = group[j]
                            val b = contacts[bIdx]
                            if (a.phones.isEmpty() && b.phones.isEmpty() && a.emails.isEmpty() && b.emails.isEmpty()) {
                                union(aIdx, bIdx)
                            } else {
                                loop@ for (pa in a.phones) {
                                    val da = normalizePhone(pa).trimStart('+')
                                    for (pb in b.phones) {
                                        val db = normalizePhone(pb).trimStart('+')
                                        if (da.endsWith(db) || db.endsWith(da)) {
                                            union(aIdx, bIdx)
                                            break@loop
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val groupsMap = mutableMapOf<Int, MutableList<RawContactInfo>>()
            contacts.forEachIndexed { index, info ->
                val root = find(index)
                groupsMap.getOrPut(root) { mutableListOf() }.add(info)
            }
            groupsMap.values.filter { it.size > 1 }
        }
    }

    override suspend fun deleteOlder(group: List<RawContactInfo>) = withContext(Dispatchers.IO) {
        val keep = group.maxByOrNull { it.lastUpdated } ?: return@withContext
        group.filter { it != keep }.forEach { info ->
            val uri = ContentUris.withAppendedId(
                ContactsContract.RawContacts.CONTENT_URI,
                info.rawContactId
            )
            resolver.delete(uri, null, null)
        }
    }

    override suspend fun deleteContacts(contacts: List<RawContactInfo>) =
        withContext(Dispatchers.IO) {
            contacts.forEach { info ->
                val uri = ContentUris.withAppendedId(
                    ContactsContract.RawContacts.CONTENT_URI,
                    info.rawContactId
                )
                resolver.delete(uri, null, null)
            }
        }

    override suspend fun mergeContacts(group: List<RawContactInfo>) = withContext(Dispatchers.IO) {
        val keep = group.maxByOrNull { it.lastUpdated } ?: return@withContext

        val newName = mergeDisplayName(group)
        if (newName.isNotBlank() && !newName.equals(keep.displayName, ignoreCase = true)) {
            val where =
                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?"
            val args = arrayOf(
                keep.rawContactId.toString(),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
            )
            val values = ContentValues().apply {
                put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
            }
            val updated = resolver.update(ContactsContract.Data.CONTENT_URI, values, where, args)
            if (updated == 0) {
                val insertValues = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, keep.rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                }
                resolver.insert(ContactsContract.Data.CONTENT_URI, insertValues)
            }
        }

        group.filter { it != keep }.forEach { source ->
            source.phones.forEach { phone ->
                val values = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, keep.rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    put(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                }
                resolver.insert(ContactsContract.Data.CONTENT_URI, values)
            }
            source.emails.forEach { email ->
                val values = ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, keep.rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                    )
                    put(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    put(
                        ContactsContract.CommonDataKinds.Email.TYPE,
                        ContactsContract.CommonDataKinds.Email.TYPE_OTHER
                    )
                }
                resolver.insert(ContactsContract.Data.CONTENT_URI, values)
            }
            val uri = ContentUris.withAppendedId(
                ContactsContract.RawContacts.CONTENT_URI,
                source.rawContactId
            )
            resolver.delete(uri, null, null)
        }
    }
}
