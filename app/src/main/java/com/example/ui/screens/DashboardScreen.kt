package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudyGroupEntity
import com.example.ui.components.AddEditGroupDialog
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.LedgerStatusColors
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.FinancialSummaryUi
import com.example.ui.viewmodel.GroupSummaryUi
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    snackbarHostState: SnackbarHostState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onSelectCenter: (Long?) -> Unit,
    onSelectStage: (Long?) -> Unit,
    onSelectGroupFinanceFilter: (Long?) -> Unit,
    onOpenGroup: (Long) -> Unit,
    onSaveGroup: (StudyGroupEntity) -> Unit,
    onDeleteGroup: (StudyGroupEntity) -> Unit,
    onOpenManageTaxonomy: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenPayments: () -> Unit
) {
    var showGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<StudyGroupEntity?>(null) }
    var deletingGroup by remember { mutableStateOf<GroupSummaryUi?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingGroup = null
                    showGroupDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_group)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_group_fab")
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                item {
                    DashboardTopHeader(
                        onOpenManageTaxonomy = onOpenManageTaxonomy,
                        onOpenBackupRestore = onOpenBackupRestore
                    )
                }

                // Month Selector
                item {
                    MonthSelectorBar(
                        selectedMonth = state.selectedMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onResetCurrent = onResetCurrentMonth
                    )
                }

                // Hero Financial Analytics Card
                item {
                    HeroFinancialCard(
                        summary = state.financialSummary,
                        hasGroupScopeFilter = state.selectedGroupFilterId != null,
                        onClearGroupScopeFilter = { onSelectGroupFinanceFilter(null) }
                    )
                }

                // Quick actions
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickActionTile("المدفوعات", Icons.Default.Payments, LedgerStatusColors.PaidText, Modifier.weight(1f), onOpenPayments)
                        QuickActionTile("السناتر والمراحل", Icons.Default.AccountBalance, MaterialTheme.colorScheme.primary, Modifier.weight(1f), onOpenManageTaxonomy)
                        QuickActionTile("نسخة احتياطية", Icons.Default.Backup, MaterialTheme.colorScheme.secondary, Modifier.weight(1f), onOpenBackupRestore)
                    }
                }

                // Center Filter Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تصفية حسب السنتر",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onOpenManageTaxonomy,
                                modifier = Modifier.testTag("quick_manage_centers_button")
                            ) {
                                Text("تعديل السناتر", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCenterId == null,
                                    onClick = { onSelectCenter(null) },
                                    label = { Text(stringResource(R.string.all_centers)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("center_chip_all")
                                )
                            }
                            items(state.centers, key = { it.id }) { center ->
                                FilterChip(
                                    selected = state.selectedCenterId == center.id,
                                    onClick = { onSelectCenter(center.id) },
                                    label = { Text(center.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("center_chip_${center.id}")
                                )
                            }
                        }
                    }
                }

                // Grade Stage Filter Row
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "المرحلة والصف الدراسي",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedStageId == null,
                                    onClick = { onSelectStage(null) },
                                    label = { Text(stringResource(R.string.all_stages)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier.testTag("stage_chip_all")
                                )
                            }
                            items(state.stages, key = { it.id }) { stage ->
                                FilterChip(
                                    selected = state.selectedStageId == stage.id,
                                    onClick = { onSelectStage(stage.id) },
                                    label = { Text(stage.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier.testTag("stage_chip_${stage.id}")
                                )
                            }
                        }
                    }
                }

                // Study Groups Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المجموعات الدراسية (${state.filteredGroups.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.filteredGroups.size > 1) {
                            Text(
                                text = "اضغط للدخول للحضور",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (state.filteredGroups.isEmpty()) {
                    item {
                        EmptyGroupsCard(
                            onAddGroup = {
                                editingGroup = null
                                showGroupDialog = true
                            }
                        )
                    }
                } else {
                    items(state.filteredGroups, key = { it.group.id }) { item ->
                        StudyGroupCard(
                            item = item,
                            isFinanceFocused = state.selectedGroupFilterId == item.group.id,
                            onClick = { onOpenGroup(item.group.id) },
                            onToggleFinanceFilter = { onSelectGroupFinanceFilter(item.group.id) },
                            onEdit = {
                                editingGroup = item.group
                                showGroupDialog = true
                            },
                            onDelete = { deletingGroup = item }
                        )
                    }
                }
            }
        }
    }

    if (showGroupDialog) {
        AddEditGroupDialog(
            centers = state.centers,
            stages = state.stages,
            initialGroup = editingGroup,
            defaultCenterId = state.selectedCenterId,
            defaultStageId = state.selectedStageId,
            onDismiss = { showGroupDialog = false },
            onSave = { group ->
                onSaveGroup(group)
                showGroupDialog = false
            }
        )
    }

    deletingGroup?.let { groupSummary ->
        AlertDialog(
            onDismissRequest = { deletingGroup = null },
            title = { Text("حذف المجموعة؟") },
            text = {
                Text(
                    "سيتم حذف مجموعة \"${groupSummary.group.groupName}\" وجميع الطلاب وسجلات الحضور المرتبطة بها نهائياً."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(groupSummary.group)
                        deletingGroup = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_group_button")
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroup = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DashboardTopHeader(
    onOpenManageTaxonomy: () -> Unit,
    onOpenBackupRestore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                IconButton(
                    onClick = onOpenManageTaxonomy,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manage_taxonomy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = stringResource(R.string.cd_manage_centers),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                IconButton(
                    onClick = onOpenBackupRestore,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("backup_restore_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = stringResource(R.string.cd_backup_restore),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroFinancialCard(
    summary: FinancialSummaryUi,
    hasGroupScopeFilter: Boolean,
    onClearGroupScopeFilter: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.collectionProgress,
        animationSpec = spring(stiffness = 280f),
        label = "collection_progress"
    )
    val percentInt = (summary.collectionProgress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_financial_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_dashboard_pattern),
                contentDescription = stringResource(R.string.cd_dashboard_pattern),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC063B33),
                                Color(0xE607493F)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Scope Tag & Progress %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = summary.scopeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasGroupScopeFilter) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إلغاء فلتر المجموعة",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable(onClick = onClearGroupScopeFilter)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF59E0B).copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "نسبة التحصيل $percentInt%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFE4A3),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // 3 Financial Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinancialMetricBox(
                        title = stringResource(R.string.expected_revenue),
                        value = Formatters.formatEgp(summary.totalExpected),
                        accent = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    FinancialMetricBox(
                        title = stringResource(R.string.collected_revenue),
                        value = Formatters.formatEgp(summary.totalCollected),
                        accent = Color(0xFF86EFAC),
                        modifier = Modifier.weight(1f)
                    )
                    FinancialMetricBox(
                        title = stringResource(R.string.remaining_revenue),
                        value = Formatters.formatEgp(summary.totalRemaining),
                        accent = Color(0xFFFDE68A),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFFF59E0B),
                    trackColor = Color.White.copy(alpha = 0.20f),
                    strokeCap = StrokeCap.Round
                )

                // Paid vs Unpaid Active Students Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudentCountPill(
                        label = "دفع: ${summary.paidStudentCount}",
                        bg = Color(0xFF22C55E).copy(alpha = 0.22f),
                        textColor = Color(0xFFBBF7D0),
                        modifier = Modifier.weight(1f)
                    )
                    StudentCountPill(
                        label = "لم يدفع: ${summary.unpaidStudentCount}",
                        bg = Color(0xFFFB7185).copy(alpha = 0.24f),
                        textColor = Color(0xFFFFE4E6),
                        modifier = Modifier.weight(1f)
                    )
                    StudentCountPill(
                        label = "نشط: ${summary.activeStudentCount}",
                        bg = Color.White.copy(alpha = 0.14f),
                        textColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialMetricBox(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.11f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StudentCountPill(
    label: String,
    bg: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun StudyGroupCard(
    item: GroupSummaryUi,
    isFinanceFocused: Boolean,
    onClick: () -> Unit,
    onToggleFinanceFilter: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val progress = if (item.expectedRevenue <= 0.0) 0f else {
        (item.collectedRevenue / item.expectedRevenue).toFloat().coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group_card_${item.group.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isFinanceFocused) 2.dp else 1.dp,
            color = if (isFinanceFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = item.centerName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = item.stageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = item.group.groupName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.group.scheduleDaysAndTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("group_menu_${item.group.id}")
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
                            text = {
                                Text(
                                    if (isFinanceFocused) "إلغاء تحديد الإحصائيات لهذه المجموعة"
                                    else "عرض إحصائيات هذه المجموعة فقط"
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleFinanceFilter()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تعديل المجموعة") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف المجموعة", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
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

            // Bottom stats row: Monthly fee, Active students, Paid/Unpaid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${item.activeStudentCount} طالب نشط",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (item.activeStudentCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (item.unpaidStudentCount == 0) {
                                LedgerStatusColors.PaidContainer
                            } else {
                                LedgerStatusColors.UnpaidContainer
                            }
                        ) {
                            Text(
                                text = "دفع ${item.paidStudentCount}/${item.activeStudentCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (item.unpaidStudentCount == 0) {
                                    LedgerStatusColors.PaidText
                                } else {
                                    LedgerStatusColors.UnpaidText
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Text(
                    text = Formatters.formatEgp(item.group.monthlyFee),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (item.activeStudentCount > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50)),
                    color = LedgerStatusColors.PaidText,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun EmptyGroupsCard(
    onAddGroup: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "لا توجد مجموعات في هذا التصنيف بعد",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ابدأ بإنشاء مجموعة جديدة وحدد السنتر والمرحلة وقيمة الاشتراك الشهري لتسجيل الطلاب والحضور بسهولة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAddGroup,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_add_group_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("إنشاء مجموعة الآن")
            }
        }
    }
}
