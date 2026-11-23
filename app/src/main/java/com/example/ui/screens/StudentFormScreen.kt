package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudentEntity
import com.example.data.model.StudentStatus
import com.example.ui.components.VoiceInputTextField
import com.example.ui.theme.LedgerStatusColors
import com.example.ui.viewmodel.GroupSummaryUi
import com.example.ui.viewmodel.StudentWithContextUi
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormScreen(
    initialGroupId: Long,
    editingStudentId: Long?,
    allGroups: List<GroupSummaryUi>,
    allStudentsWithContext: List<StudentWithContextUi>,
    loadStudent: suspend (Long) -> Pair<StudentEntity?, List<StudentEntity>>,
    onNavigateBack: () -> Unit,
    onSave: (
        existingStudentId: Long?,
        groupId: Long,
        fullName: String,
        studentPhone: String,
        parentPhone: String,
        notes: String,
        status: StudentStatus,
        selectedSiblingId: Long?,
        siblingDiscountAmount: Double,
        applyDiscountToSelectedSibling: Boolean
    ) -> Unit,
    onVoiceUnavailable: () -> Unit
) {
    var selectedGroupId by remember { mutableStateOf(initialGroupId) }
    var fullName by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(StudentStatus.ACTIVE) }
    var selectedSiblingId by remember { mutableStateOf<Long?>(null) }
    var discountText by remember { mutableStateOf("") }
    var applyDiscountToSibling by remember { mutableStateOf(true) }
    var existingLinkedSiblings by remember { mutableStateOf<List<StudentEntity>>(emptyList()) }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var showSiblingPicker by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(editingStudentId == null) }

    LaunchedEffect(editingStudentId) {
        if (editingStudentId != null) {
            val (student, siblings) = loadStudent(editingStudentId)
            if (student != null) {
                selectedGroupId = student.groupId
                fullName = student.fullName
                studentPhone = student.studentPhone
                parentPhone = student.parentPhone
                notes = student.notes
                status = student.status
                existingLinkedSiblings = siblings
                selectedSiblingId = siblings.firstOrNull()?.id
                discountText = if (student.siblingDiscountAmount > 0.0) {
                    Formatters.formatAmount(student.siblingDiscountAmount).replace(",", "")
                } else ""
            }
            isLoaded = true
        }
    }

    val selectedGroupSummary = allGroups.firstOrNull { it.group.id == selectedGroupId }
    val groupMonthlyFee = selectedGroupSummary?.group?.monthlyFee ?: 0.0
    val parsedDiscount = discountText.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val effectiveDiscount = if (selectedSiblingId != null) parsedDiscount else 0.0
    val netMonthlyFee = (groupMonthlyFee - effectiveDiscount).coerceAtLeast(0.0)

    val selectedSiblingContext = selectedSiblingId?.let { id ->
        allStudentsWithContext.firstOrNull { it.student.id == id }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("student_form_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (editingStudentId == null) "إضافة طالب جديد" else "تعديل بيانات الطالب",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            showErrors = true
                            if (fullName.trim().isNotEmpty() && selectedGroupId != 0L) {
                                onSave(
                                    editingStudentId,
                                    selectedGroupId,
                                    fullName.trim(),
                                    studentPhone.trim(),
                                    parentPhone.trim(),
                                    notes.trim(),
                                    status,
                                    selectedSiblingId,
                                    effectiveDiscount,
                                    applyDiscountToSibling
                                )
                            }
                        },
                        enabled = isLoaded,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .height(54.dp)
                            .testTag("save_student_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (editingStudentId == null) "حفظ الطالب" else "حفظ التعديلات",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Voice-enabled Student Name
                VoiceInputTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "اسم الطالب رباعي *",
                    placeholder = "مثال: يوسف محمد أحمد علي",
                    voicePrompt = "انطق اسم الطالب",
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    isError = showErrors && fullName.trim().isEmpty(),
                    supportingText = if (showErrors && fullName.trim().isEmpty()) {
                        { Text("يرجى إدخال اسم الطالب") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    fieldTestTag = "student_name_input",
                    micTestTag = "student_name_voice_mic",
                    onVoiceUnavailable = onVoiceUnavailable
                )

                // Study Group Selector
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = it }
                ) {
                    val labelText = selectedGroupSummary?.let {
                        "${it.group.groupName} (${it.centerName} • ${it.stageName})"
                    } ?: "اختر المجموعة"
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المجموعة") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("student_group_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        allGroups.forEach { grp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(grp.group.groupName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${grp.centerName} • ${grp.stageName} • ${Formatters.formatEgp(grp.group.monthlyFee)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedGroupId = grp.group.id
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Phones Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = studentPhone,
                        onValueChange = { studentPhone = it },
                        label = { Text("رقم الطالب") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("student_phone_input")
                    )
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("رقم ولي الأمر") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("parent_phone_input")
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_notes_input")
                )

                // Student Status Toggle (Active / Suspended) when editing
                if (editingStudentId != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حالة الطالب", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = status == StudentStatus.ACTIVE,
                                    onClick = { status = StudentStatus.ACTIVE },
                                    label = { Text("نشط") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LedgerStatusColors.PaidText,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("status_active_chip")
                                )
                                FilterChip(
                                    selected = status == StudentStatus.SUSPENDED,
                                    onClick = { status = StudentStatus.SUSPENDED },
                                    label = { Text("مُعلّق") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LedgerStatusColors.SuspendedText,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("status_suspended_chip")
                                )
                            }
                        }
                    }
                }

                // Sibling Linking & Custom Discount Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selectedSiblingId != null) {
                            LedgerStatusColors.SiblingText.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LedgerStatusColors.SiblingContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = LedgerStatusColors.SiblingText
                                )
                            }
                            Column {
                                Text(
                                    text = "نظام الأخوات والخصم المخصص",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "اربط الطالب بأخيه/أخته في أي مجموعة أو صف وحدد قيمة الخصم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedSiblingContext != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = LedgerStatusColors.SiblingContainer.copy(alpha = 0.65f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "مرتبط مع: ${selectedSiblingContext.student.fullName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LedgerStatusColors.SiblingText
                                        )
                                        Text(
                                            text = "${selectedSiblingContext.centerName} • ${selectedSiblingContext.stageName} • ${selectedSiblingContext.groupName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedSiblingId = null
                                            discountText = ""
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("unlink_sibling_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LinkOff,
                                            contentDescription = "فك ربط الأخ",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            if (existingLinkedSiblings.size > 1) {
                                Text(
                                    text = "يوجد ${existingLinkedSiblings.size} إخوة مرتبطين بنفس العائلة.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerStatusColors.SiblingText
                                )
                            }

                            OutlinedTextField(
                                value = discountText,
                                onValueChange = { input ->
                                    discountText = input.filter { it.isDigit() || it == '.' }
                                },
                                label = { Text("قيمة الخصم بالجنيه (ج.م)") },
                                placeholder = { Text("مثال: 50") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sibling_discount_input")
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { applyDiscountToSibling = !applyDiscountToSibling },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = applyDiscountToSibling,
                                    onCheckedChange = { applyDiscountToSibling = it }
                                )
                                Text(
                                    text = "تطبيق نفس قيمة الخصم على الأخ/الأخت المختار أيضاً",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showSiblingPicker = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("link_sibling_button")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (selectedSiblingContext == null) {
                                    stringResource(R.string.link_sibling)
                                } else {
                                    "تغيير الأخ/الأخت المرتبط"
                                }
                            )
                        }

                        // Live Monthly Fee Breakdown
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "صافي الاشتراك الشهري للطالب",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (effectiveDiscount > 0.0) {
                                    Text(
                                        text = "${Formatters.formatEgp(groupMonthlyFee)} - خصم ${Formatters.formatEgp(effectiveDiscount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = Formatters.formatEgp(netMonthlyFee),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showSiblingPicker) {
        SiblingPickerDialog(
            candidates = allStudentsWithContext.filter { it.student.id != editingStudentId },
            selectedId = selectedSiblingId,
            onDismiss = { showSiblingPicker = false },
            onSelect = { chosen ->
                selectedSiblingId = chosen.student.id
                showSiblingPicker = false
            },
            onVoiceUnavailable = onVoiceUnavailable
        )
    }
}

@Composable
private fun SiblingPickerDialog(
    candidates: List<StudentWithContextUi>,
    selectedId: Long?,
    onDismiss: () -> Unit,
    onSelect: (StudentWithContextUi) -> Unit,
    onVoiceUnavailable: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(candidates, query) {
        val q = query.trim()
        if (q.isEmpty()) candidates
        else candidates.filter { item ->
            item.student.fullName.contains(q, ignoreCase = true) ||
                item.groupName.contains(q, ignoreCase = true) ||
                item.stageName.contains(q, ignoreCase = true) ||
                item.centerName.contains(q, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر الأخ/الأخت من السنتر",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VoiceInputTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "ابحث بالاسم أو المجموعة",
                    voicePrompt = "انطق اسم الأخ أو الأخت",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    fieldTestTag = "sibling_search_input",
                    micTestTag = "sibling_search_voice_mic",
                    onVoiceUnavailable = onVoiceUnavailable
                )

                if (filtered.isEmpty()) {
                    Text(
                        text = if (candidates.isEmpty()) {
                            "لا يوجد طلاب آخرون مسجلون في التطبيق بعد."
                        } else {
                            "لا توجد نتائج مطابقة للبحث."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.student.id }) { item ->
                            val isSelected = item.student.id == selectedId
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    LedgerStatusColors.SiblingContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) LedgerStatusColors.SiblingText else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelect(item) }
                                    .testTag("sibling_candidate_${item.student.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.student.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.centerName} • ${item.stageName} • ${item.groupName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "محدد",
                                            tint = LedgerStatusColors.SiblingText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
