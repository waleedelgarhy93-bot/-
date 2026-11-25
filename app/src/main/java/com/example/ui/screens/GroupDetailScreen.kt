package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDatePickerState
import com.example.data.repository.ImportedStudent
import com.example.util.DayDate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.data.model.SessionEntry
import com.example.data.model.SessionState
import com.example.data.model.StudentEntity
import com.example.data.model.StudentStatus
import com.example.data.model.StudyGroupEntity
import com.example.ui.components.AddEditGroupDialog
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SiblingPaymentBottomSheet
import com.example.ui.components.VoiceInputTextField
import com.example.ui.theme.LedgerStatusColors
import com.example.ui.viewmodel.GroupDetailUiState
import com.example.ui.viewmodel.GroupStudentFilter
import com.example.ui.viewmodel.StudentAttendanceRowUi
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    state: GroupDetailUiState,
    centers: List<CenterEntity>,
    stages: List<GradeStageEntity>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (GroupStudentFilter) -> Unit,
    onAddStudent: () -> Unit,
    onEditStudent: (Long) -> Unit,
    onSaveGroup: (StudyGroupEntity) -> Unit,
    onToggleStudentSuspension: (StudentEntity) -> Unit,
    onDeleteStudent: (StudentEntity) -> Unit,
    onSetSingleStudentPaid: (Long, Boolean, String) -> Unit,
    onMarkAllSiblingsPaid: (String) -> Unit,
    onShiftDay: (Int) -> Unit,
    onPickDate: (DayDate) -> Unit,
    onToday: () -> Unit,
    onMarkAttendance: (Long, String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onDeleteGroup: (StudyGroupEntity) -> Unit,
    pendingImport: List<ImportedStudent>?,
    onImportUri: (Uri) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImport: () -> Unit,
    onExport: (Uri, Boolean) -> Unit,
    onVoiceUnavailable: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmDeleteGroup by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportUri)
    }
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let { onExport(it, false) } }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { onExport(it, true) }
    }
    val exportName = "${state.group?.groupName ?: "مجموعة"} - ${state.selectedMonth.toArabicDisplay()}"
    var siblingPromptRow by remember { mutableStateOf<StudentAttendanceRowUi?>(null) }
    var undoPaymentRow by remember { mutableStateOf<StudentAttendanceRowUi?>(null) }
    var deletingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var showEditGroupDialog by remember { mutableStateOf(false) }

    val group = state.group

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("group_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = group?.groupName ?: "المجموعة",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (group != null) {
                            Text(
                                text = "${state.centerName} • ${state.stageName} • ${Formatters.formatEgp(group.monthlyFee)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (group != null) {
                        IconButton(
                            onClick = { showEditGroupDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("edit_current_group_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل المجموعة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("تحضير الكل (${state.selectedDate.shortLabel()})") },
                                    leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                                    onClick = { showMenu = false; onMarkAllPresent() }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("استيراد أسماء من Excel") },
                                    leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        importLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "text/plain",
                                                "application/octet-stream"
                                            )
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تصدير Excel") },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                                    onClick = { showMenu = false; excelLauncher.launch("$exportName.xlsx") }
                                )
                                DropdownMenuItem(
                                    text = { Text("تصدير PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                    onClick = { showMenu = false; pdfLauncher.launch("$exportName.pdf") }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("حذف المجموعة", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; confirmDeleteGroup = true }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddStudent,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(stringResource(R.string.add_student)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_student_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Day picker: attendance is recorded for this day
                item {
                    DaySelectorBar(
                        date = state.selectedDate,
                        present = state.dayPresent,
                        absent = state.dayAbsent,
                        total = state.filterCounts.activeAll,
                        onPrevious = { onShiftDay(-1) },
                        onNext = { onShiftDay(1) },
                        onOpenPicker = { showDatePicker = true },
                        onToday = onToday
                    )
                }

                // Group Mini Financial Summary Strip
                item {
                    GroupFinancialStrip(
                        expected = state.financialSummary.totalExpected,
                        collected = state.financialSummary.totalCollected,
                        remaining = state.financialSummary.totalRemaining
                    )
                }

                // Voice-Enabled Student Search Bar
                item {
                    VoiceInputTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = "بحث عن طالب",
                        placeholder = "اكتب أو انطق اسم الطالب…",
                        voicePrompt = "انطق اسم الطالب للبحث",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fieldTestTag = "student_search_input",
                        micTestTag = "student_search_voice_mic",
                        onVoiceUnavailable = onVoiceUnavailable
                    )
                }

                // Quick Filter Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedFilter == GroupStudentFilter.ACTIVE_ALL,
                                onClick = { onSelectFilter(GroupStudentFilter.ACTIVE_ALL) },
                                label = { Text("الكل (${state.filterCounts.activeAll})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("filter_chip_active_all")
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedFilter == GroupStudentFilter.PAID,
                                onClick = { onSelectFilter(GroupStudentFilter.PAID) },
                                label = { Text("دفع الشهر (${state.filterCounts.paid})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LedgerStatusColors.PaidText,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_chip_paid")
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedFilter == GroupStudentFilter.UNPAID,
                                onClick = { onSelectFilter(GroupStudentFilter.UNPAID) },
                                label = { Text("لم يدفع (${state.filterCounts.unpaid})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LedgerStatusColors.UnpaidText,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_chip_unpaid")
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedFilter == GroupStudentFilter.SUSPENDED,
                                onClick = { onSelectFilter(GroupStudentFilter.SUSPENDED) },
                                label = { Text("المُعلّقون (${state.filterCounts.suspended})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LedgerStatusColors.SuspendedText,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_chip_suspended")
                            )
                        }
                    }
                }

                // Student Cards or Empty State
                if (state.students.isEmpty()) {
                    item {
                        EmptyStudentsCard(
                            filter = state.selectedFilter,
                            hasSearch = state.searchQuery.isNotBlank(),
                            onAddStudent = onAddStudent
                        )
                    }
                } else {
                    itemsIndexed(state.students, key = { _, r -> r.student.id }) { index, row ->
                        StudentAttendanceCard(
                            number = index + 1,
                            row = row,
                            onPaymentClick = {
                                if (row.isPaid) {
                                    undoPaymentRow = row
                                } else if (row.linkedActiveSiblings.isNotEmpty() && row.student.siblingGroupId != null) {
                                    siblingPromptRow = row
                                } else {
                                    onSetSingleStudentPaid(row.student.id, true, row.student.fullName)
                                }
                            },
                            onPresent = { onMarkAttendance(row.student.id, "PRESENT") },
                            onAbsent = { onMarkAttendance(row.student.id, "ABSENT") },
                            onEdit = { onEditStudent(row.student.id) },
                            onToggleSuspension = { onToggleStudentSuspension(row.student) },
                            onDelete = { deletingStudent = row.student }
                        )
                    }
                }
            }
        }
    }

    // Smart Sibling Payment Bottom Sheet
    siblingPromptRow?.let { row ->
        val siblingKey = row.student.siblingGroupId
        if (siblingKey != null) {
            SiblingPaymentBottomSheet(
                row = row,
                monthDisplay = state.selectedMonth.toArabicDisplay(),
                onDismiss = { siblingPromptRow = null },
                onPaySingle = {
                    onSetSingleStudentPaid(row.student.id, true, row.student.fullName)
                    siblingPromptRow = null
                },
                onPayAllSiblings = {
                    onMarkAllSiblingsPaid(siblingKey)
                    siblingPromptRow = null
                }
            )
        }
    }

    // Confirm Undo Payment Dialog
    undoPaymentRow?.let { row ->
        AlertDialog(
            onDismissRequest = { undoPaymentRow = null },
            title = { Text("إلغاء تسجيل الدفع؟") },
            text = {
                Text("هل تريد تغيير حالة الطالب \"${row.student.fullName}\" إلى لم يدفع لهذا الشهر؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetSingleStudentPaid(row.student.id, false, row.student.fullName)
                        undoPaymentRow = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_undo_payment_button")
                ) {
                    Text("نعم، إلغاء الدفع")
                }
            },
            dismissButton = {
                TextButton(onClick = { undoPaymentRow = null }) {
                    Text("تراجع")
                }
            }
        )
    }

    // Confirm Delete Student Dialog
    deletingStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { deletingStudent = null },
            title = { Text("حذف الطالب نهائياً؟") },
            text = {
                Text(
                    "سيتم حذف الطالب \"${student.fullName}\" وجميع سجلاته. يُفضّل استخدام \"تعليق الطالب\" للاحتفاظ بسجل حضوره ومدفوعاته السابقة."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStudent(student)
                        deletingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStudent = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.selectedDate.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onPickDate(DayDate.fromUtcMillis(it)) }
                    showDatePicker = false
                }) { Text("تمام") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    pendingImport?.let { list ->
        AlertDialog(
            onDismissRequest = onDismissImport,
            title = { Text("استيراد ${list.size} اسم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("هيتضافوا لمجموعة \"${group?.groupName.orEmpty()}\". الأسماء الموجودة قبل كده مش هتتكرر.")
                    Spacer(Modifier.height(6.dp))
                    list.take(8).forEachIndexed { i, st ->
                        Text(
                            "${i + 1}. ${st.name}" + if (st.parentPhone.isNotBlank()) "  •  ${st.parentPhone}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (list.size > 8) Text("… و${list.size - 8} كمان", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = onConfirmImport) { Text("إضافة") } },
            dismissButton = { TextButton(onClick = onDismissImport) { Text("إلغاء") } }
        )
    }

    if (confirmDeleteGroup && group != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteGroup = false },
            title = { Text("حذف المجموعة؟") },
            text = { Text("سيتم حذف مجموعة \"${group.groupName}\" وكل طلابها وسجلات الحضور والدفع نهائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteGroup = false
                        onDeleteGroup(group)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف نهائي") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteGroup = false }) { Text("إلغاء") } }
        )
    }

    // Edit Current Group Dialog
    if (showEditGroupDialog && group != null) {
        AddEditGroupDialog(
            centers = centers,
            stages = stages,
            initialGroup = group,
            onDismiss = { showEditGroupDialog = false },
            onSave = { updated ->
                onSaveGroup(updated)
                showEditGroupDialog = false
            }
        )
    }
}

@Composable
private fun GroupFinancialStrip(
    expected: Double,
    collected: Double,
    remaining: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniFinanceColumn(
                label = "المستهدف",
                amount = Formatters.formatEgp(expected),
                color = MaterialTheme.colorScheme.onSurface
            )
            MiniFinanceColumn(
                label = "المُحصَّل",
                amount = Formatters.formatEgp(collected),
                color = LedgerStatusColors.PaidText
            )
            MiniFinanceColumn(
                label = "المتبقي",
                amount = Formatters.formatEgp(remaining),
                color = if (remaining > 0) LedgerStatusColors.UnpaidText else LedgerStatusColors.PaidText
            )
        }
    }
}

@Composable
private fun MiniFinanceColumn(
    label: String,
    amount: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StudentAttendanceCard(
    number: Int,
    row: StudentAttendanceRowUi,
    onPaymentClick: () -> Unit,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onEdit: () -> Unit,
    onToggleSuspension: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isSuspended = row.student.status == StudentStatus.SUSPENDED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${row.student.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isSuspended -> LedgerStatusColors.ExcusedBorder
                row.isPaid -> LedgerStatusColors.PaidBorder
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Name + Sibling Badge + Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$number. ${row.student.fullName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (row.hasSiblings) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LedgerStatusColors.SiblingContainer,
                                modifier = Modifier.testTag("sibling_badge_${row.student.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = LedgerStatusColors.SiblingText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (row.student.siblingDiscountAmount > 0.0) {
                                            "أخوات (-${Formatters.formatAmount(row.student.siblingDiscountAmount)})"
                                        } else {
                                            stringResource(R.string.siblings_badge)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LedgerStatusColors.SiblingText
                                    )
                                }
                            }
                        }
                    }

                    val phoneSubtitle = buildList {
                        if (row.student.parentPhone.isNotBlank()) add("ولي الأمر: ${row.student.parentPhone}")
                        if (row.student.studentPhone.isNotBlank()) add("الطالب: ${row.student.studentPhone}")
                    }.joinToString(" • ")

                    if (phoneSubtitle.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = phoneSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("student_menu_${row.student.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل بيانات الطالب") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (isSuspended) "إعادة تفعيل الطالب" else "تعليق الطالب مؤقتاً")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isSuspended) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                                    contentDescription = null,
                                    tint = if (isSuspended) LedgerStatusColors.PaidText else LedgerStatusColors.SuspendedText
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleSuspension()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف الطالب", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Payment Action Row OR Reactivate Row for Suspended students
            if (isSuspended) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LedgerStatusColors.SuspendedContainer
                    ) {
                        Text(
                            text = "طالب مُعلّق (مستبعد من إحصائيات الشهر)",
                            style = MaterialTheme.typography.labelMedium,
                            color = LedgerStatusColors.SuspendedText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Button(
                        onClick = onToggleSuspension,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LedgerStatusColors.PaidText),
                        modifier = Modifier.testTag("reactivate_student_${row.student.id}")
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إعادة تفعيل")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionToggleButton(
                        label = "حضر",
                        icon = Icons.Default.Check,
                        selected = row.dayState == "PRESENT",
                        tint = LedgerStatusColors.PresentFill,
                        onClick = onPresent,
                        modifier = Modifier.weight(1f).testTag("present_${row.student.id}")
                    )
                    ActionToggleButton(
                        label = "غاب",
                        icon = Icons.Default.Close,
                        selected = row.dayState == "ABSENT",
                        tint = LedgerStatusColors.AbsentFill,
                        onClick = onAbsent,
                        modifier = Modifier.weight(1f).testTag("absent_${row.student.id}")
                    )
                    ActionToggleButton(
                        label = if (row.isPaid) "دفع ✓" else "دفع",
                        icon = Icons.Default.Payments,
                        selected = row.isPaid,
                        tint = LedgerStatusColors.PaidText,
                        onClick = onPaymentClick,
                        modifier = Modifier.weight(1f).testTag("payment_toggle_${row.student.id}")
                    )
                }
                Text(
                    text = "الشهر ده: حضر ${row.monthPresent} • غاب ${row.monthAbsent} • " +
                        if (row.isPaid) "دفع ${Formatters.formatEgp(row.paidAmount)}" else "مطلوب ${Formatters.formatEgp(row.expectedAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionToggleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(if (selected) tint else tint.copy(alpha = 0.08f), spring(), label = "btn_bg")
    val fg by animateColorAsState(if (selected) Color.White else tint, spring(), label = "btn_fg")
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, tint.copy(alpha = if (selected) 1f else 0.45f))
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun DaySelectorBar(
    date: DayDate,
    present: Int,
    absent: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPicker: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "اليوم اللي قبله", tint = MaterialTheme.colorScheme.primary)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenPicker)
                    .padding(vertical = 4.dp)
            ) {
                Text(date.toArabicDisplay(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "حضر $present • غاب $absent • من $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (date != DayDate.today()) {
                    TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(30.dp)) {
                        Text("ارجع للنهاردة", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "اليوم اللي بعده", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyStudentsCard(
    filter: GroupStudentFilter,
    hasSearch: Boolean,
    onAddStudent: () -> Unit
) {
    val (title, subtitle) = when {
        hasSearch -> "لا توجد نتائج مطابقة للبحث" to "جرب البحث باسم آخر أو استخدم الإدخال الصوتي."
        filter == GroupStudentFilter.SUSPENDED -> "لا يوجد طلاب مُعلّقون" to "الطلاب الذين يتم تعليقهم سيظهرون هنا ويمكن إعادة تفعيلهم بضغطة واحدة."
        filter == GroupStudentFilter.PAID -> "لا يوجد طلاب مسجل لهم دفع بعد" to "اضغط على زر \"لم يدفع\" في بطاقة أي طالب لتسجيل دفعه."
        filter == GroupStudentFilter.UNPAID -> "ممتاز! جميع الطلاب النشطين دفعوا هذا الشهر" to "تم تحصيل مصروفات جميع طلاب هذه المجموعة بالكامل."
        else -> "لا يوجد طلاب في هذه المجموعة بعد" to "أضف أول طالب الآن باستخدام زر \"إضافة طالب\" مع دعم الإدخال الصوتي ونظام الأخوات."
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (filter == GroupStudentFilter.ACTIVE_ALL && !hasSearch) {
                Button(
                    onClick = onAddStudent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("empty_add_student_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("إضافة أول طالب")
                }
            }
        }
    }
}
