package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.data.model.StudyGroupEntity
import com.example.ui.theme.LedgerStatusColors
import com.example.ui.viewmodel.StudentAttendanceRowUi
import com.example.util.Formatters
import com.example.util.MonthYear

@Composable
fun MonthSelectorBar(
    selectedMonth: MonthYear,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentMonth = selectedMonth == MonthYear.current()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_prev_month),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = selectedMonth.toArabicDisplay(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isCurrentMonth) {
                    TextButton(
                        onClick = onResetCurrent,
                        modifier = Modifier.testTag("reset_current_month_button")
                    ) {
                        Text(
                            text = "الحالي",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditGroupDialog(
    centers: List<CenterEntity>,
    stages: List<GradeStageEntity>,
    initialGroup: StudyGroupEntity? = null,
    defaultCenterId: Long? = null,
    defaultStageId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (StudyGroupEntity) -> Unit
) {
    var selectedCenterId by remember {
        mutableStateOf(
            initialGroup?.centerId
                ?: defaultCenterId
                ?: centers.firstOrNull()?.id
                ?: 0L
        )
    }
    var selectedStageId by remember {
        mutableStateOf(
            initialGroup?.gradeStageId
                ?: defaultStageId
                ?: stages.firstOrNull()?.id
                ?: 0L
        )
    }
    var groupName by remember { mutableStateOf(initialGroup?.groupName ?: "") }
    var scheduleText by remember { mutableStateOf(initialGroup?.scheduleDaysAndTime ?: "") }
    var monthlyFeeText by remember {
        mutableStateOf(
            initialGroup?.monthlyFee?.let { Formatters.formatAmount(it).replace(",", "") } ?: ""
        )
    }
    var sessionsPerMonth by remember { mutableIntStateOf(initialGroup?.sessionsPerMonth ?: 8) }

    var centerExpanded by remember { mutableStateOf(false) }
    var stageExpanded by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val parsedFee = monthlyFeeText.trim().toDoubleOrNull()
    val isNameValid = groupName.trim().isNotEmpty()
    val isFeeValid = parsedFee != null && parsedFee > 0.0

    val quickSchedulePresets = listOf(
        "السبت والثلاثاء - 4:00 عصراً",
        "الأحد والأربعاء - 5:00 مساءً",
        "الاثنين والخميس - 6:00 مساءً"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialGroup == null) "إضافة مجموعة جديدة" else "تعديل بيانات المجموعة",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Center selector
                ExposedDropdownMenuBox(
                    expanded = centerExpanded,
                    onExpandedChange = { centerExpanded = it }
                ) {
                    val selectedCenterName = centers.firstOrNull { it.id == selectedCenterId }?.name ?: "اختر السنتر"
                    OutlinedTextField(
                        value = selectedCenterName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("السنتر") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = centerExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("group_center_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = centerExpanded,
                        onDismissRequest = { centerExpanded = false }
                    ) {
                        centers.forEach { center ->
                            DropdownMenuItem(
                                text = { Text(center.name) },
                                onClick = {
                                    selectedCenterId = center.id
                                    centerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Stage selector
                ExposedDropdownMenuBox(
                    expanded = stageExpanded,
                    onExpandedChange = { stageExpanded = it }
                ) {
                    val selectedStageName = stages.firstOrNull { it.id == selectedStageId }?.name ?: "اختر المرحلة"
                    OutlinedTextField(
                        value = selectedStageName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المرحلة والصف") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stageExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("group_stage_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = stageExpanded,
                        onDismissRequest = { stageExpanded = false }
                    ) {
                        stages.forEach { stage ->
                            DropdownMenuItem(
                                text = { Text(stage.name) },
                                onClick = {
                                    selectedStageId = stage.id
                                    stageExpanded = false
                                }
                            )
                        }
                    }
                }

                // Voice-enabled Group Name
                VoiceInputTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = "اسم المجموعة *",
                    placeholder = "مثال: مجموعة السبت والثلاثاء",
                    voicePrompt = "قل اسم المجموعة",
                    isError = showErrors && !isNameValid,
                    supportingText = if (showErrors && !isNameValid) {
                        { Text("يرجى إدخال اسم المجموعة") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    fieldTestTag = "group_name_input",
                    micTestTag = "group_name_voice_mic"
                )

                // Schedule Days & Time
                OutlinedTextField(
                    value = scheduleText,
                    onValueChange = { scheduleText = it },
                    label = { Text("المواعيد والأيام") },
                    placeholder = { Text("السبت والثلاثاء - 4:00 عصراً") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_schedule_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickSchedulePresets.forEach { preset ->
                        FilterChip(
                            selected = scheduleText == preset,
                            onClick = { scheduleText = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Monthly Fee (Required)
                OutlinedTextField(
                    value = monthlyFeeText,
                    onValueChange = { input ->
                        monthlyFeeText = input.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("الاشتراك الشهري (ج.م) *") },
                    placeholder = { Text("مثال: 300") },
                    singleLine = true,
                    isError = showErrors && !isFeeValid,
                    supportingText = if (showErrors && !isFeeValid) {
                        { Text("يرجى إدخال قيمة اشتراك صحيحة أكبر من صفر") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_fee_input")
                )

                // Sessions Per Month Stepper
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "عدد الحصص شهرياً",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "الافتراضي 8 حصص",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (sessionsPerMonth > 4) sessionsPerMonth-- },
                                modifier = Modifier.testTag("decrease_sessions_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "تقليل الحصص")
                            }
                            Text(
                                text = "$sessionsPerMonth",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalIconButton(
                                onClick = { if (sessionsPerMonth < 16) sessionsPerMonth++ },
                                modifier = Modifier.testTag("increase_sessions_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زيادة الحصص")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showErrors = true
                    if (isNameValid && isFeeValid && selectedCenterId != 0L && selectedStageId != 0L) {
                        onSave(
                            StudyGroupEntity(
                                id = initialGroup?.id ?: 0L,
                                centerId = selectedCenterId,
                                gradeStageId = selectedStageId,
                                groupName = groupName.trim(),
                                scheduleDaysAndTime = scheduleText.trim().ifEmpty { "غير محدد" },
                                sessionsPerMonth = sessionsPerMonth,
                                monthlyFee = parsedFee!!
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_group_button")
            ) {
                Text(if (initialGroup == null) "إنشاء المجموعة" else "حفظ التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiblingPaymentBottomSheet(
    row: StudentAttendanceRowUi,
    monthDisplay: String,
    onDismiss: () -> Unit,
    onPaySingle: () -> Unit,
    onPayAllSiblings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allActiveSiblingsTotal = row.expectedAmount + row.linkedActiveSiblings.sumOf { it.expectedAmount }
    val unpaidFamilyTotal = (if (!row.isPaid) row.expectedAmount else 0.0) +
        row.linkedActiveSiblings.filter { !it.isPaid }.sumOf { it.expectedAmount }
    val totalSiblingsCount = 1 + row.linkedActiveSiblings.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        text = "تسجيل دفع الأخوات • $monthDisplay",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "هذا الطالب مرتبط بأخ/أخت في السنتر بخصم مخصص",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Summary list of the student + linked siblings
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current student row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.student.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (row.student.siblingDiscountAmount > 0.0) {
                                Text(
                                    text = "خصم أخوات: ${Formatters.formatEgp(row.student.siblingDiscountAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LedgerStatusColors.SiblingText
                                )
                            }
                        }
                        Text(
                            text = Formatters.formatEgp(row.expectedAmount),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    row.linkedActiveSiblings.forEach { sibling ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sibling.student.fullName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${sibling.stageName} • ${sibling.groupName}" +
                                        if (sibling.isPaid) " (دفع بالفعل)" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = Formatters.formatEgp(sibling.expectedAmount),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (sibling.isPaid) LedgerStatusColors.PaidText else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Button 1: Pay all linked siblings together
            Button(
                onClick = onPayAllSiblings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("pay_all_siblings_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "تسجيل الدفع للأخوات معاً ($totalSiblingsCount) • ${Formatters.formatEgp(if (unpaidFamilyTotal > 0) unpaidFamilyTotal else allActiveSiblingsTotal)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Button 2: Pay for this student only
            OutlinedButton(
                onClick = onPaySingle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("pay_single_student_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "تسجيل الدفع لهذا الطالب فقط • ${Formatters.formatEgp(row.expectedAmount)}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
