package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BackupPayload
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.data.model.MonthlyRecordEntity
import com.example.data.model.SessionEntry
import com.example.data.model.SessionState
import com.example.data.model.StudentEntity
import com.example.data.model.StudentStatus
import com.example.data.model.StudyGroupEntity
import com.example.data.repository.LedgerRepository
import com.example.util.MonthYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GroupStudentFilter {
    ACTIVE_ALL,
    PAID,
    UNPAID,
    SUSPENDED
}

data class FinancialSummaryUi(
    val totalExpected: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val activeStudentCount: Int = 0,
    val paidStudentCount: Int = 0,
    val unpaidStudentCount: Int = 0,
    val suspendedStudentCount: Int = 0,
    val scopeLabel: String = "كل السناتر"
) {
    val collectionProgress: Float
        get() = if (totalExpected <= 0.0) 0f else (totalCollected / totalExpected).toFloat().coerceIn(0f, 1f)
}

data class GroupSummaryUi(
    val group: StudyGroupEntity,
    val centerName: String,
    val stageName: String,
    val activeStudentCount: Int,
    val suspendedStudentCount: Int,
    val paidStudentCount: Int,
    val unpaidStudentCount: Int,
    val expectedRevenue: Double,
    val collectedRevenue: Double
)

data class StudentWithContextUi(
    val student: StudentEntity,
    val groupName: String,
    val centerName: String,
    val stageName: String,
    val groupMonthlyFee: Double
)

data class LinkedSiblingPreview(
    val student: StudentEntity,
    val groupName: String,
    val stageName: String,
    val expectedAmount: Double,
    val isPaid: Boolean
)

data class StudentAttendanceRowUi(
    val student: StudentEntity,
    val record: MonthlyRecordEntity?,
    val expectedAmount: Double,
    val paidAmount: Double,
    val isPaid: Boolean,
    val paymentDate: Long?,
    val sessionStatuses: List<SessionEntry>,
    val linkedActiveSiblings: List<LinkedSiblingPreview>
) {
    val hasSiblings: Boolean get() = student.siblingGroupId != null
    val presentCount: Int get() = sessionStatuses.count { it.state == SessionState.PRESENT }
    val absentCount: Int get() = sessionStatuses.count { it.state == SessionState.ABSENT }
}

data class GroupFilterCounts(
    val activeAll: Int = 0,
    val paid: Int = 0,
    val unpaid: Int = 0,
    val suspended: Int = 0
)

data class DashboardUiState(
    val selectedMonth: MonthYear = MonthYear.current(),
    val centers: List<CenterEntity> = emptyList(),
    val stages: List<GradeStageEntity> = emptyList(),
    val selectedCenterId: Long? = null,
    val selectedStageId: Long? = null,
    val selectedGroupFilterId: Long? = null,
    val filteredGroups: List<GroupSummaryUi> = emptyList(),
    val allGroups: List<GroupSummaryUi> = emptyList(),
    val allStudentsWithContext: List<StudentWithContextUi> = emptyList(),
    val financialSummary: FinancialSummaryUi = FinancialSummaryUi(),
    val monthlyRecordsCount: Int = 0
)

data class GroupDetailUiState(
    val group: StudyGroupEntity? = null,
    val centerName: String = "",
    val stageName: String = "",
    val selectedMonth: MonthYear = MonthYear.current(),
    val searchQuery: String = "",
    val selectedFilter: GroupStudentFilter = GroupStudentFilter.ACTIVE_ALL,
    val financialSummary: FinancialSummaryUi = FinancialSummaryUi(),
    val filterCounts: GroupFilterCounts = GroupFilterCounts(),
    val students: List<StudentAttendanceRowUi> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LedgerRepository(AppDatabase.getInstance(application))

    private val _selectedMonth = MutableStateFlow(MonthYear.current())
    val selectedMonth: StateFlow<MonthYear> = _selectedMonth.asStateFlow()

    private val _selectedCenterId = MutableStateFlow<Long?>(null)
    private val _selectedStageId = MutableStateFlow<Long?>(null)
    private val _selectedGroupFilterId = MutableStateFlow<Long?>(null)

    private val _activeGroupId = MutableStateFlow<Long?>(null)
    private val _groupSearchQuery = MutableStateFlow("")
    private val _groupStudentFilter = MutableStateFlow(GroupStudentFilter.ACTIVE_ALL)

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val _pendingRestorePayload = MutableStateFlow<BackupPayload?>(null)
    val pendingRestorePayload: StateFlow<BackupPayload?> = _pendingRestorePayload.asStateFlow()

    private val currentMonthRecordsFlow = _selectedMonth.flatMapLatest { monthYear ->
        repository.observeMonthlyRecordsByMonth(monthYear.toStorageKey())
    }

    init {
        viewModelScope.launch {
            _selectedMonth.collect { month ->
                repository.ensureMonthlyRecordsForActiveStudents(month.toStorageKey())
            }
        }
    }

    private data class CoreSnapshot(
        val centers: List<CenterEntity>,
        val stages: List<GradeStageEntity>,
        val groups: List<StudyGroupEntity>,
        val students: List<StudentEntity>,
        val records: List<MonthlyRecordEntity>
    )

    private val coreSnapshotFlow = combine(
        repository.centersFlow,
        repository.gradeStagesFlow,
        repository.studyGroupsFlow,
        repository.allStudentsFlow,
        currentMonthRecordsFlow
    ) { centers, stages, groups, students, records ->
        CoreSnapshot(centers, stages, groups, students, records)
    }

    val dashboardState: StateFlow<DashboardUiState> = combine(
        coreSnapshotFlow,
        _selectedMonth,
        _selectedCenterId,
        _selectedStageId,
        _selectedGroupFilterId
    ) { snapshot, month, centerId, stageId, groupFilterId ->
        val centersById = snapshot.centers.associateBy { it.id }
        val stagesById = snapshot.stages.associateBy { it.id }
        val recordsByStudentId = snapshot.records.associateBy { it.studentId }
        val studentsByGroupId = snapshot.students.groupBy { it.groupId }

        val allGroupSummaries = snapshot.groups.map { group ->
            val groupStudents = studentsByGroupId[group.id].orEmpty()
            val activeStudents = groupStudents.filter { it.status == StudentStatus.ACTIVE }
            val suspendedCount = groupStudents.count { it.status == StudentStatus.SUSPENDED }

            var expected = 0.0
            var collected = 0.0
            var paidCount = 0
            for (student in activeStudents) {
                val rec = recordsByStudentId[student.id]
                val studentExpected = rec?.expectedAmount
                    ?: (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
                expected += studentExpected
                if (rec?.isPaid == true) {
                    collected += rec.paidAmount
                    paidCount++
                }
            }

            GroupSummaryUi(
                group = group,
                centerName = centersById[group.centerId]?.name ?: "سنتر",
                stageName = stagesById[group.gradeStageId]?.name ?: "مرحلة",
                activeStudentCount = activeStudents.size,
                suspendedStudentCount = suspendedCount,
                paidStudentCount = paidCount,
                unpaidStudentCount = (activeStudents.size - paidCount).coerceAtLeast(0),
                expectedRevenue = expected,
                collectedRevenue = collected
            )
        }

        val filteredGroups = allGroupSummaries.filter { item ->
            (centerId == null || item.group.centerId == centerId) &&
                (stageId == null || item.group.gradeStageId == stageId)
        }

        val effectiveGroupFilterId = groupFilterId?.takeIf { id ->
            filteredGroups.any { it.group.id == id }
        }

        val financeTargetGroups = if (effectiveGroupFilterId != null) {
            filteredGroups.filter { it.group.id == effectiveGroupFilterId }
        } else {
            filteredGroups
        }

        val totalExpected = financeTargetGroups.sumOf { it.expectedRevenue }
        val totalCollected = financeTargetGroups.sumOf { it.collectedRevenue }
        val totalRemaining = (totalExpected - totalCollected).coerceAtLeast(0.0)
        val activeCount = financeTargetGroups.sumOf { it.activeStudentCount }
        val paidCount = financeTargetGroups.sumOf { it.paidStudentCount }
        val unpaidCount = financeTargetGroups.sumOf { it.unpaidStudentCount }
        val suspendedCount = financeTargetGroups.sumOf { it.suspendedStudentCount }

        val scopeParts = buildList {
            if (centerId != null) centersById[centerId]?.name?.let { add(it) }
            if (stageId != null) stagesById[stageId]?.name?.let { add(it) }
            if (effectiveGroupFilterId != null) {
                filteredGroups.firstOrNull { it.group.id == effectiveGroupFilterId }
                    ?.group?.groupName?.let { add(it) }
            }
        }
        val scopeLabel = if (scopeParts.isEmpty()) "كل السناتر" else scopeParts.joinToString(" • ")

        val groupsById = snapshot.groups.associateBy { it.id }
        val studentsWithContext = snapshot.students.mapNotNull { st ->
            val grp = groupsById[st.groupId] ?: return@mapNotNull null
            StudentWithContextUi(
                student = st,
                groupName = grp.groupName,
                centerName = centersById[grp.centerId]?.name ?: "",
                stageName = stagesById[grp.gradeStageId]?.name ?: "",
                groupMonthlyFee = grp.monthlyFee
            )
        }

        DashboardUiState(
            selectedMonth = month,
            centers = snapshot.centers,
            stages = snapshot.stages,
            selectedCenterId = centerId,
            selectedStageId = stageId,
            selectedGroupFilterId = effectiveGroupFilterId,
            filteredGroups = filteredGroups,
            allGroups = allGroupSummaries,
            allStudentsWithContext = studentsWithContext,
            financialSummary = FinancialSummaryUi(
                totalExpected = totalExpected,
                totalCollected = totalCollected,
                totalRemaining = totalRemaining,
                activeStudentCount = activeCount,
                paidStudentCount = paidCount,
                unpaidStudentCount = unpaidCount,
                suspendedStudentCount = suspendedCount,
                scopeLabel = scopeLabel
            ),
            monthlyRecordsCount = snapshot.records.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    val groupDetailState: StateFlow<GroupDetailUiState> = combine(
        coreSnapshotFlow,
        _selectedMonth,
        _activeGroupId,
        _groupSearchQuery,
        _groupStudentFilter
    ) { snapshot, month, groupId, query, filter ->
        val group = snapshot.groups.firstOrNull { it.id == groupId }
            ?: return@combine GroupDetailUiState(selectedMonth = month)

        val centersById = snapshot.centers.associateBy { it.id }
        val stagesById = snapshot.stages.associateBy { it.id }
        val groupsById = snapshot.groups.associateBy { it.id }
        val recordsByStudentId = snapshot.records.associateBy { it.studentId }
        val siblingsByGroupKey = snapshot.students
            .filter { it.siblingGroupId != null }
            .groupBy { it.siblingGroupId!! }

        val groupStudents = snapshot.students.filter { it.groupId == group.id }
        val allRows = groupStudents.map { student ->
            val rec = recordsByStudentId[student.id]
            val expected = rec?.expectedAmount
                ?: (group.monthlyFee - student.siblingDiscountAmount).coerceAtLeast(0.0)
            val sessions = rec?.sessionStatuses
                ?.let { list ->
                    val target = group.sessionsPerMonth.coerceAtLeast(1)
                    when {
                        list.size == target -> list
                        list.size < target -> list + List(target - list.size) { SessionEntry() }
                        else -> list.take(target)
                    }
                }
                ?: List(group.sessionsPerMonth.coerceAtLeast(1)) { SessionEntry() }

            val linkedActiveSiblings = student.siblingGroupId?.let { key ->
                siblingsByGroupKey[key].orEmpty()
                    .filter { it.id != student.id && it.status == StudentStatus.ACTIVE }
                    .map { sib ->
                        val sibGroup = groupsById[sib.groupId]
                        val sibRec = recordsByStudentId[sib.id]
                        val sibExpected = sibRec?.expectedAmount
                            ?: ((sibGroup?.monthlyFee ?: 0.0) - sib.siblingDiscountAmount).coerceAtLeast(0.0)
                        LinkedSiblingPreview(
                            student = sib,
                            groupName = sibGroup?.groupName ?: "",
                            stageName = stagesById[sibGroup?.gradeStageId]?.name ?: "",
                            expectedAmount = sibExpected,
                            isPaid = sibRec?.isPaid == true
                        )
                    }
            }.orEmpty()

            StudentAttendanceRowUi(
                student = student,
                record = rec,
                expectedAmount = expected,
                paidAmount = rec?.paidAmount ?: 0.0,
                isPaid = rec?.isPaid == true,
                paymentDate = rec?.paymentDate,
                sessionStatuses = sessions,
                linkedActiveSiblings = linkedActiveSiblings
            )
        }

        val activeRows = allRows.filter { it.student.status == StudentStatus.ACTIVE }
        val suspendedRows = allRows.filter { it.student.status == StudentStatus.SUSPENDED }
        val paidActiveRows = activeRows.filter { it.isPaid }
        val unpaidActiveRows = activeRows.filter { !it.isPaid }

        val expectedTotal = activeRows.sumOf { it.expectedAmount }
        val collectedTotal = paidActiveRows.sumOf { it.paidAmount }
        val remainingTotal = (expectedTotal - collectedTotal).coerceAtLeast(0.0)

        val tabFiltered = when (filter) {
            GroupStudentFilter.ACTIVE_ALL -> activeRows
            GroupStudentFilter.PAID -> paidActiveRows
            GroupStudentFilter.UNPAID -> unpaidActiveRows
            GroupStudentFilter.SUSPENDED -> suspendedRows
        }

        val normalizedQuery = query.trim()
        val searchFiltered = if (normalizedQuery.isEmpty()) {
            tabFiltered
        } else {
            tabFiltered.filter { row ->
                row.student.fullName.contains(normalizedQuery, ignoreCase = true) ||
                    row.student.studentPhone.contains(normalizedQuery) ||
                    row.student.parentPhone.contains(normalizedQuery)
            }
        }

        GroupDetailUiState(
            group = group,
            centerName = centersById[group.centerId]?.name ?: "",
            stageName = stagesById[group.gradeStageId]?.name ?: "",
            selectedMonth = month,
            searchQuery = query,
            selectedFilter = filter,
            financialSummary = FinancialSummaryUi(
                totalExpected = expectedTotal,
                totalCollected = collectedTotal,
                totalRemaining = remainingTotal,
                activeStudentCount = activeRows.size,
                paidStudentCount = paidActiveRows.size,
                unpaidStudentCount = unpaidActiveRows.size,
                suspendedStudentCount = suspendedRows.size,
                scopeLabel = group.groupName
            ),
            filterCounts = GroupFilterCounts(
                activeAll = activeRows.size,
                paid = paidActiveRows.size,
                unpaid = unpaidActiveRows.size,
                suspended = suspendedRows.size
            ),
            students = searchFiltered
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupDetailUiState()
    )

    // --- Month & Filter Controls ---
    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.previous()
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.next()
    }

    fun resetToCurrentMonth() {
        _selectedMonth.value = MonthYear.current()
    }

    fun selectCenterFilter(centerId: Long?) {
        _selectedCenterId.value = centerId
        _selectedGroupFilterId.value = null
    }

    fun selectStageFilter(stageId: Long?) {
        _selectedStageId.value = stageId
        _selectedGroupFilterId.value = null
    }

    fun selectGroupFinanceFilter(groupId: Long?) {
        _selectedGroupFilterId.value = if (_selectedGroupFilterId.value == groupId) null else groupId
    }

    fun openGroup(groupId: Long) {
        if (_activeGroupId.value != groupId) {
            _activeGroupId.value = groupId
            _groupSearchQuery.value = ""
            _groupStudentFilter.value = GroupStudentFilter.ACTIVE_ALL
        }
        viewModelScope.launch {
            repository.ensureMonthlyRecordsForActiveStudents(_selectedMonth.value.toStorageKey())
        }
    }

    fun updateGroupSearchQuery(query: String) {
        _groupSearchQuery.value = query
    }

    fun selectGroupStudentFilter(filter: GroupStudentFilter) {
        _groupStudentFilter.value = filter
    }

    // --- Centers & Grade Stages Actions ---
    fun addCenter(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCenter(name)
            _snackbarMessages.tryEmit("تم إضافة السنتر بنجاح")
        }
    }

    fun updateCenter(center: CenterEntity) {
        if (center.name.isBlank()) return
        viewModelScope.launch {
            repository.updateCenter(center)
            _snackbarMessages.tryEmit("تم تعديل اسم السنتر")
        }
    }

    fun deleteCenter(center: CenterEntity) {
        viewModelScope.launch {
            repository.deleteCenter(center)
            if (_selectedCenterId.value == center.id) _selectedCenterId.value = null
            _snackbarMessages.tryEmit("تم حذف السنتر")
        }
    }

    fun addGradeStage(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addGradeStage(name)
            _snackbarMessages.tryEmit("تم إضافة المرحلة الدراسية")
        }
    }

    fun updateGradeStage(stage: GradeStageEntity) {
        if (stage.name.isBlank()) return
        viewModelScope.launch {
            repository.updateGradeStage(stage)
            _snackbarMessages.tryEmit("تم تعديل المرحلة الدراسية")
        }
    }

    fun deleteGradeStage(stage: GradeStageEntity) {
        viewModelScope.launch {
            repository.deleteGradeStage(stage)
            if (_selectedStageId.value == stage.id) _selectedStageId.value = null
            _snackbarMessages.tryEmit("تم حذف المرحلة الدراسية")
        }
    }

    // --- Study Groups Actions ---
    fun saveStudyGroup(group: StudyGroupEntity) {
        viewModelScope.launch {
            repository.saveStudyGroup(group, _selectedMonth.value.toStorageKey())
            _snackbarMessages.tryEmit(
                if (group.id == 0L) "تم إنشاء المجموعة بنجاح" else "تم تحديث بيانات المجموعة"
            )
        }
    }

    fun deleteStudyGroup(group: StudyGroupEntity) {
        viewModelScope.launch {
            repository.deleteStudyGroup(group)
            _snackbarMessages.tryEmit("تم حذف المجموعة")
        }
    }

    // --- Students & Siblings Actions ---
    suspend fun loadStudentForEdit(studentId: Long): Pair<StudentEntity?, List<StudentEntity>> {
        val student = repository.getStudentById(studentId) ?: return null to emptyList()
        val siblings = repository.getLinkedSiblings(student)
        return student to siblings
    }

    fun saveStudent(
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
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveStudentWithSibling(
                existingStudentId = existingStudentId,
                groupId = groupId,
                fullName = fullName,
                studentPhone = studentPhone,
                parentPhone = parentPhone,
                notes = notes,
                status = status,
                selectedSiblingId = selectedSiblingId,
                siblingDiscountAmount = siblingDiscountAmount,
                applyDiscountToSelectedSibling = applyDiscountToSelectedSibling,
                currentMonthYear = _selectedMonth.value.toStorageKey()
            )
            _snackbarMessages.tryEmit(
                if (existingStudentId == null) "تم إضافة الطالب بنجاح" else "تم حفظ تعديلات الطالب"
            )
            onSaved()
        }
    }

    fun toggleStudentSuspension(student: StudentEntity) {
        viewModelScope.launch {
            val wasActive = student.status == StudentStatus.ACTIVE
            repository.toggleStudentStatus(student, _selectedMonth.value.toStorageKey())
            _snackbarMessages.tryEmit(
                if (wasActive) "تم تعليق الطالب ${student.fullName}"
                else "تم إعادة تفعيل الطالب ${student.fullName}"
            )
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _snackbarMessages.tryEmit("تم حذف الطالب")
        }
    }

    // --- Payment & Attendance Actions ---
    fun setSingleStudentPaid(studentId: Long, isPaid: Boolean, studentName: String) {
        viewModelScope.launch {
            repository.setStudentPaid(studentId, _selectedMonth.value.toStorageKey(), isPaid)
            _snackbarMessages.tryEmit(
                if (isPaid) "تم تسجيل الدفع للطالب $studentName"
                else "تم إلغاء حالة الدفع للطالب $studentName"
            )
        }
    }

    fun markAllSiblingsPaid(siblingGroupId: String) {
        viewModelScope.launch {
            val count = repository.markAllLinkedSiblingsPaid(
                siblingGroupId = siblingGroupId,
                monthYear = _selectedMonth.value.toStorageKey()
            )
            _snackbarMessages.tryEmit("تم تسجيل الدفع للأخوات معاً ($count طلاب)")
        }
    }

    fun cycleSession(studentId: Long, sessionIndex: Int) {
        viewModelScope.launch {
            repository.cycleSessionState(studentId, _selectedMonth.value.toStorageKey(), sessionIndex)
        }
    }

    // --- Local JSON Backup & Restore (SAF) ---
    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = repository.exportBackupJson()
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } ?: error("تعذّر فتح ملف التصدير")
                }
            }
            if (result.isSuccess) {
                _snackbarMessages.tryEmit("تم تصدير النسخة الاحتياطية بنجاح")
            } else {
                _snackbarMessages.tryEmit("فشل تصدير النسخة الاحتياطية")
            }
        }
    }

    fun inspectBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val json = getApplication<Application>().contentResolver
                        .openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("تعذّر قراءة الملف")
                    repository.parseBackupJson(json).getOrThrow()
                }
            }
            parsed.onSuccess { payload ->
                _pendingRestorePayload.value = payload
            }.onFailure {
                _snackbarMessages.tryEmit("ملف النسخة الاحتياطية غير صالح")
            }
        }
    }

    fun dismissPendingRestore() {
        _pendingRestorePayload.value = null
    }

    fun confirmRestoreBackup() {
        val payload = _pendingRestorePayload.value ?: return
        _pendingRestorePayload.value = null
        viewModelScope.launch {
            val result = runCatching {
                repository.restoreBackup(payload)
                repository.ensureMonthlyRecordsForActiveStudents(_selectedMonth.value.toStorageKey())
            }
            if (result.isSuccess) {
                _selectedCenterId.value = null
                _selectedStageId.value = null
                _selectedGroupFilterId.value = null
                _snackbarMessages.tryEmit("تمت استعادة البيانات بنجاح")
            } else {
                _snackbarMessages.tryEmit("حدث خطأ أثناء استعادة النسخة الاحتياطية")
            }
        }
    }

    fun emitMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LedgerViewModel(application) as T
        }
    }
}
