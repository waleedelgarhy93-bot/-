package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.SessionEntry
import com.example.data.model.StudentStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class Converters {

    private val moshi: Moshi = Moshi.Builder().build()
    private val sessionListType = Types.newParameterizedType(List::class.java, SessionEntry::class.java)
    private val sessionListAdapter = moshi.adapter<List<SessionEntry>>(sessionListType)

    @TypeConverter
    fun fromStudentStatus(status: StudentStatus): String = status.name

    @TypeConverter
    fun toStudentStatus(value: String): StudentStatus =
        runCatching { StudentStatus.valueOf(value) }.getOrDefault(StudentStatus.ACTIVE)

    @TypeConverter
    fun fromSessionEntries(entries: List<SessionEntry>): String =
        sessionListAdapter.toJson(entries)

    @TypeConverter
    fun toSessionEntries(json: String): List<SessionEntry> {
        if (json.isBlank()) return List(8) { SessionEntry() }
        return runCatching { sessionListAdapter.fromJson(json) }.getOrNull()
            ?: List(8) { SessionEntry() }
    }
}
