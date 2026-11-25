package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceEntity
import com.example.data.model.BackupPayload
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.data.model.MonthlyRecordEntity
import com.example.data.model.SessionEntry
import com.example.data.model.StudentEntity
import com.example.data.model.StudentStatus
import com.example.data.model.StudyGroupEntity
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LedgerRepository(
    private val database: AppDatabase
) {
    private val dao = database.ledgerDao()
    private val moshi: Moshi = Moshi.Builder().build()
    private val backupAdapter = moshi.adapter(BackupPayload::class.java).indent("  ")

    val centersFlow: Flow<List<CenterEntity>> = dao.observeCenters()
    val gradeStagesFlow: Flow<List<GradeStageEntity>> = dao.observeGradeStages()
    val studyGroupsFlow: Flow<List<StudyGroupEntity>> = dao.observeStudyGroups()
    val allStudentsFlow: Flow<List<StudentEntity>> = dao.observeAllStudents()

    fun observeGroup(groupId: Long): Flow<StudyGroupEntity?> = dao.observeStudyGroupById(groupId)

    fun observeStudentsByGroup(groupId: Long): Flow<List<StudentEntity>> =
        dao.observeStudentsByGroup(groupId)

    fun observeMonthlyRecordsByMonth(monthYear: String): Flow<List<MonthlyRecordEntity>> =
        dao.observeMonthlyRecordsByMonth(monthYear)

    fun observeMonthlyRecordsByGroupAndMonth(
        groupId: Long,
        monthYear: String
    ): Flow<List<MonthlyRecordEntity>> =
        dao.observeMonthlyRecordsByGroupAndMonth(groupId, monthYear)

    // --- Centers CRUD ---
    suspend fun addCenter(name: String): Long =
        dao.insertCenter(CenterEntity(name = name.trim()))

    suspend fun updateCenter(center: CenterEntity) =
        dao.updateCenter(center.copy(name = center.name.trim()))

    suspend fun deleteCenter(center: CenterEntity) = dao.deleteCenter(center)

    // --- Grade Stages CRUD ---
    suspend fun addGradeStage(name: String): Long {
        val nextOrder = (dao.getAllGradeStages().maxOfOrNull { it.sortOrder } ?: 0) + 1
        return dao.insertGradeStage(GradeStageEntity(name = name.trim(), sortOrder = nextOrder))
    }

    suspend fun updateGradeStage(stage: GradeStageEntity) =
        dao.updateGradeStage(stage.copy(name = stage.name.trim()))

    suspend fun deleteGradeStage(stage: GradeStageEntity) = dao.deleteGradeStage(stage)

    // --- Study Groups CRUD ---
    suspend fun saveStudyGroup(group: StudyGroupEntity, currentMonthYear: String): Long {
        return database.withTransaction {
            val savedId = if (group.id == 0L) {
                dao.insertStudyGroup(group)
            } else {
                dao.updateStudyGroup(group)
                // Keep unpaid monthly records in sync when monthlyFee or sessionsPerMonth changes
                val existingRecords = dao.getMonthlyRecordsForMonth(currentMonthYear)
                    .filter { it.groupId == group.id }
                val groupStudents = dao.getAllStudents().associateBy { it.id }
                for (record in existingRecords) {
                    val student = groupStudents[record.studentId] ?: continue
                    val updatedExpected =
                        (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
                    val resizedSessions = resizeSessions(record.sessionStatuses, group.sessionsPerMonth)
                    val updatedRecord = record.copy(
                        expectedAmount = if (record.isPaid) record.expectedAmount else updatedExpected,
                        sessionStatuses = resizedSessions
                    )
                    if (updatedRecord != record) {
                        dao.updateMonthlyRecord(updatedRecord)
                    }
                }
                group.id
            }
            savedId
        }
    }

    suspend fun deleteStudyGroup(group: StudyGroupEntity) = dao.deleteStudyGroup(group)

    // --- Monthly Records Initialization (ACTIVE students only) ---
    suspend fun ensureMonthlyRecordsForActiveStudents(monthYear: String) {
        database.withTransaction {
            val activeStudents = dao.getStudentsByStatus(StudentStatus.ACTIVE)
            if (activeStudents.isEmpty()) return@withTransaction

            val groupsById = dao.getAllStudyGroups().associateBy { it.id }
            val existingStudentIds = dao.getMonthlyRecordsForMonth(monthYear)
                .map { it.studentId }
                .toHashSet()

            for (student in activeStudents) {
                if (student.id in existingStudentIds) continue
                val group = groupsById[student.groupId] ?: continue
                val expected = (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
                val record = MonthlyRecordEntity(
                    studentId = student.id,
                    groupId = student.groupId,
                    monthYear = monthYear,
                    isPaid = false,
                    expectedAmount = expected,
                    paidAmount = 0.0,
                    paymentDate = null,
                    sessionStatuses = List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }
                )
                dao.insertMonthlyRecordIgnore(record)
            }
        }
    }

    // --- Students & Sibling Linking ---
    suspend fun getStudentById(studentId: Long): StudentEntity? = dao.getStudentById(studentId)

    suspend fun getLinkedSiblings(student: StudentEntity): List<StudentEntity> {
        val groupId = student.siblingGroupId ?: return emptyList()
        return dao.getStudentsBySiblingGroup(groupId).filter { it.id != student.id }
    }

    suspend fun saveStudentWithSibling(
        existingStudentId: Long?,
        groupId: Long,
        fullName: String,
        studentPhone: String,
        parentPhone: String,
        notes: String,
        status: StudentStatus,
        selectedSiblingId: Long?,
        siblingDiscountAmount: Double,
        applyDiscountToSelectedSibling: Boolean,
        currentMonthYear: String
    ): Long {
        return database.withTransaction {
            val previousStudent = existingStudentId?.let { dao.getStudentById(it) }
            val group = dao.getStudyGroupById(groupId)
                ?: error("Study group not found")

            val cleanedDiscount = siblingDiscountAmount.coerceAtLeast(0.0)
            var resolvedSiblingGroupId: String? = null
            var targetSibling: StudentEntity? = null

            if (selectedSiblingId != null) {
                targetSibling = dao.getStudentById(selectedSiblingId)
                if (targetSibling != null) {
                    resolvedSiblingGroupId = targetSibling.siblingGroupId
                        ?: previousStudent?.siblingGroupId
                        ?: UUID.randomUUID().toString()

                    // Ensure target sibling also belongs to this shared siblingGroupId
                    val updatedSibling = targetSibling.copy(
                        siblingGroupId = resolvedSiblingGroupId,
                        siblingDiscountAmount = if (applyDiscountToSelectedSibling) {
                            cleanedDiscount
                        } else {
                            targetSibling.siblingDiscountAmount
                        }
                    )
                    dao.updateStudent(updatedSibling)
                    syncUnpaidExpectedAmount(updatedSibling, currentMonthYear)
                }
            } else {
                // If user explicitly unlinked an existing student from a sibling group,
                // check whether only 1 orphan sibling remains and clear its siblingGroupId
                val oldGroup = previousStudent?.siblingGroupId
                if (oldGroup != null) {
                    val remaining = dao.getStudentsBySiblingGroup(oldGroup)
                        .filter { it.id != previousStudent.id }
                    if (remaining.size == 1) {
                        val orphan = remaining.first().copy(siblingGroupId = null)
                        dao.updateStudent(orphan)
                    }
                }
            }

            val studentToSave = StudentEntity(
                id = existingStudentId ?: 0L,
                groupId = groupId,
                fullName = fullName.trim(),
                studentPhone = studentPhone.trim(),
                parentPhone = parentPhone.trim(),
                notes = notes.trim(),
                status = status,
                siblingGroupId = resolvedSiblingGroupId,
                siblingDiscountAmount = if (resolvedSiblingGroupId != null) cleanedDiscount else 0.0
            )

            val savedId = if (existingStudentId == null || existingStudentId == 0L) {
                dao.insertStudent(studentToSave)
            } else {
                dao.updateStudent(studentToSave)
                existingStudentId
            }

            val savedStudent = studentToSave.copy(id = savedId)

            // Sync or initialize current month record if ACTIVE
            if (savedStudent.status == StudentStatus.ACTIVE) {
                val existingRecord = dao.getMonthlyRecord(savedId, currentMonthYear)
                val expected = (group.monthlyFee - savedStudent.siblingDiscountAmount).coerceAtLeast(0.0)
                if (existingRecord == null) {
                    dao.insertMonthlyRecordIgnore(
                        MonthlyRecordEntity(
                            studentId = savedId,
                            groupId = groupId,
                            monthYear = currentMonthYear,
                            isPaid = false,
                            expectedAmount = expected,
                            paidAmount = 0.0,
                            paymentDate = null,
                            sessionStatuses = List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }
                        )
                    )
                } else {
                    val resizedSessions = resizeSessions(existingRecord.sessionStatuses, group.sessionsPerMonth)
                    dao.updateMonthlyRecord(
                        existingRecord.copy(
                            groupId = groupId,
                            expectedAmount = if (existingRecord.isPaid) existingRecord.expectedAmount else expected,
                            sessionStatuses = resizedSessions
                        )
                    )
                }
            }

            savedId
        }
    }

    suspend fun toggleStudentStatus(student: StudentEntity, currentMonthYear: String) {
        database.withTransaction {
            val newStatus = if (student.status == StudentStatus.ACTIVE) {
                StudentStatus.SUSPENDED
            } else {
                StudentStatus.ACTIVE
            }
            val updated = student.copy(status = newStatus)
            dao.updateStudent(updated)

            // When reactivating a student, ensure they have a record for the active month
            if (newStatus == StudentStatus.ACTIVE) {
                val group = dao.getStudyGroupById(student.groupId) ?: return@withTransaction
                val existing = dao.getMonthlyRecord(student.id, currentMonthYear)
                if (existing == null) {
                    val expected = (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
                    dao.insertMonthlyRecordIgnore(
                        MonthlyRecordEntity(
                            studentId = student.id,
                            groupId = student.groupId,
                            monthYear = currentMonthYear,
                            isPaid = false,
                            expectedAmount = expected,
                            paidAmount = 0.0,
                            paymentDate = null,
                            sessionStatuses = List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }
                        )
                    )
                }
            }
        }
    }

    suspend fun deleteStudent(student: StudentEntity) {
        database.withTransaction {
            val siblingGroup = student.siblingGroupId
            dao.deleteStudent(student)
            if (siblingGroup != null) {
                val remaining = dao.getStudentsBySiblingGroup(siblingGroup)
                if (remaining.size == 1) {
                    dao.updateStudent(remaining.first().copy(siblingGroupId = null))
                }
            }
        }
    }

    // --- Payments (Single & Linked Siblings Batch) ---
    suspend fun setStudentPaid(studentId: Long, monthYear: String, isPaid: Boolean) {
        database.withTransaction {
            val student = dao.getStudentById(studentId) ?: return@withTransaction
            val group = dao.getStudyGroupById(student.groupId) ?: return@withTransaction
            val expected = (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
            val existing = dao.getMonthlyRecord(studentId, monthYear)
            val now = System.currentTimeMillis()

            if (existing == null) {
                dao.insertMonthlyRecordIgnore(
                    MonthlyRecordEntity(
                        studentId = studentId,
                        groupId = student.groupId,
                        monthYear = monthYear,
                        isPaid = isPaid,
                        expectedAmount = expected,
                        paidAmount = if (isPaid) expected else 0.0,
                        paymentDate = if (isPaid) now else null,
                        sessionStatuses = List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }
                    )
                )
            } else {
                dao.updateMonthlyRecord(
                    existing.copy(
                        expectedAmount = expected,
                        isPaid = isPaid,
                        paidAmount = if (isPaid) expected else 0.0,
                        paymentDate = if (isPaid) now else null
                    )
                )
            }
        }
    }

    suspend fun markAllLinkedSiblingsPaid(siblingGroupId: String, monthYear: String): Int {
        return database.withTransaction {
            val siblings = dao.getStudentsBySiblingGroup(siblingGroupId)
                .filter { it.status == StudentStatus.ACTIVE }
            val groupsById = dao.getAllStudyGroups().associateBy { it.id }
            val now = System.currentTimeMillis()
            var markedCount = 0

            for (sibling in siblings) {
                val group = groupsById[sibling.groupId] ?: continue
                val expected = (group.monthlyFee - sibling.siblingDiscountAmount).coerceAtLeast(0.0)
                val existing = dao.getMonthlyRecord(sibling.id, monthYear)
                if (existing == null) {
                    dao.insertMonthlyRecordIgnore(
                        MonthlyRecordEntity(
                            studentId = sibling.id,
                            groupId = sibling.groupId,
                            monthYear = monthYear,
                            isPaid = true,
                            expectedAmount = expected,
                            paidAmount = expected,
                            paymentDate = now,
                            sessionStatuses = List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }
                        )
                    )
                } else {
                    dao.updateMonthlyRecord(
                        existing.copy(
                            expectedAmount = expected,
                            isPaid = true,
                            paidAmount = expected,
                            paymentDate = now
                        )
                    )
                }
                markedCount++
            }
            markedCount
        }
    }

    // --- Attendance Sessions ---
    suspend fun cycleSessionState(studentId: Long, monthYear: String, sessionIndex: Int) {
        database.withTransaction {
            val student = dao.getStudentById(studentId) ?: return@withTransaction
            val group = dao.getStudyGroupById(student.groupId) ?: return@withTransaction
            val count = group.sessionsPerMonth.coerceAtLeast(1)
            val existing = dao.getMonthlyRecord(studentId, monthYear)
                ?: run {
                    val expected = (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
                    val fresh = MonthlyRecordEntity(
                        studentId = studentId,
                        groupId = student.groupId,
                        monthYear = monthYear,
                        expectedAmount = expected,
                        sessionStatuses = List(count) { SessionEntry() }
                    )
                    val newId = dao.insertMonthlyRecordIgnore(fresh)
                    fresh.copy(id = newId)
                }

            val currentList = resizeSessions(existing.sessionStatuses, count).toMutableList()
            if (sessionIndex !in currentList.indices) return@withTransaction

            val nextState = currentList[sessionIndex].state.next()
            currentList[sessionIndex] = SessionEntry(
                state = nextState,
                dateMillis = if (nextState == com.example.data.model.SessionState.UNMARKED) {
                    null
                } else {
                    System.currentTimeMillis()
                }
            )
            dao.updateMonthlyRecord(existing.copy(sessionStatuses = currentList))
        }
    }

    // --- Daily attendance ---
    fun observeAttendanceBetween(fromKey: String, toKey: String): Flow<List<AttendanceEntity>> =
        dao.observeAttendanceBetween(fromKey, toKey)

    /** Sets PRESENT / ABSENT for one day; choosing the same state again clears it. */
    suspend fun toggleAttendance(studentId: Long, dateKey: String, state: String) {
        database.withTransaction {
            val student = dao.getStudentById(studentId) ?: return@withTransaction
            val existing = dao.getAttendance(studentId, dateKey)
            if (existing?.state == state) {
                dao.deleteAttendance(studentId, dateKey)
            } else {
                dao.upsertAttendance(
                    AttendanceEntity(
                        id = existing?.id ?: 0L,
                        studentId = studentId,
                        groupId = student.groupId,
                        dateKey = dateKey,
                        state = state
                    )
                )
            }
        }
    }

    /** Marks every active student of the group who is not marked yet as PRESENT. */
    suspend fun markAllPresent(groupId: Long, dateKey: String): Int {
        return database.withTransaction {
            var count = 0
            dao.getAllStudents()
                .filter { it.groupId == groupId && it.status == StudentStatus.ACTIVE }
                .forEach { st ->
                    if (dao.getAttendance(st.id, dateKey) == null) {
                        dao.upsertAttendance(AttendanceEntity(studentId = st.id, groupId = groupId, dateKey = dateKey, state = "PRESENT"))
                        count++
                    }
                }
            count
        }
    }

    /** Adds students by name; names already in the group are skipped. Returns how many were added. */
    suspend fun importStudents(groupId: Long, rows: List<ImportedStudent>, currentMonthYear: String): Int {
        return database.withTransaction {
            val group = dao.getStudyGroupById(groupId) ?: return@withTransaction 0
            val existing = dao.getAllStudents().filter { it.groupId == groupId }
                .map { normalizeName(it.fullName) }.toHashSet()
            var added = 0
            for (r in rows) {
                val name = normalizeName(r.name)
                if (name.isEmpty() || !existing.add(name)) continue
                val id = dao.insertStudent(
                    StudentEntity(groupId = groupId, fullName = name, studentPhone = r.studentPhone, parentPhone = r.parentPhone)
                )
                dao.insertMonthlyRecordIgnore(
                    MonthlyRecordEntity(
                        studentId = id,
                        groupId = groupId,
                        monthYear = currentMonthYear,
                        expectedAmount = group.monthlyFee,
                        sessionStatuses = List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }
                    )
                )
                added++
            }
            added
        }
    }

    private fun normalizeName(s: String) = s.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    // --- Backup & Restore (Local JSON) ---
    suspend fun exportBackupJson(): String {
        val payload = BackupPayload(
            version = 2,
            exportedAtMillis = System.currentTimeMillis(),
            centers = dao.getAllCenters(),
            gradeStages = dao.getAllGradeStages(),
            studyGroups = dao.getAllStudyGroups(),
            students = dao.getAllStudents(),
            monthlyRecords = dao.getAllMonthlyRecords(),
            attendance = dao.getAllAttendance()
        )
        return backupAdapter.toJson(payload)
    }

    fun parseBackupJson(json: String): Result<BackupPayload> = runCatching {
        backupAdapter.fromJson(json) ?: error("ملف النسخة الاحتياطية غير صالح أو فارغ")
    }

    suspend fun restoreBackup(payload: BackupPayload) {
        database.withTransaction {
            // Clear in reverse FK order
            dao.clearAttendance()
            dao.clearMonthlyRecords()
            dao.clearStudents()
            dao.clearStudyGroups()
            dao.clearGradeStages()
            dao.clearCenters()

            // Insert in FK parent-first order preserving original IDs
            dao.insertCenters(payload.centers)
            dao.insertGradeStages(payload.gradeStages)
            dao.insertStudyGroups(payload.studyGroups)
            dao.insertStudents(payload.students)
            dao.insertMonthlyRecordsReplace(payload.monthlyRecords)
            dao.insertAttendanceList(payload.attendance)
        }
    }

    private suspend fun syncUnpaidExpectedAmount(student: StudentEntity, monthYear: String) {
        val group = dao.getStudyGroupById(student.groupId) ?: return
        val record = dao.getMonthlyRecord(student.id, monthYear) ?: return
        if (!record.isPaid) {
            val expected = (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
            dao.updateMonthlyRecord(record.copy(expectedAmount = expected))
        }
    }

    private fun resizeSessions(current: List<SessionEntry>, targetSize: Int): List<SessionEntry> {
        val safeTarget = targetSize.coerceAtLeast(1)
        return when {
            current.size == safeTarget -> current
            current.size < safeTarget -> current + List(safeTarget - current.size) { SessionEntry() }
            else -> current.take(safeTarget)
        }
    }
}

/** A student row read from an Excel / CSV file. */
data class ImportedStudent(val name: String, val parentPhone: String = "", val studentPhone: String = "")
