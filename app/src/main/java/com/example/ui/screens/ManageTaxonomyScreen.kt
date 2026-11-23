package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.ui.components.VoiceInputTextField
import com.example.ui.viewmodel.GroupSummaryUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTaxonomyScreen(
    centers: List<CenterEntity>,
    stages: List<GradeStageEntity>,
    allGroups: List<GroupSummaryUi>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onAddCenter: (String) -> Unit,
    onUpdateCenter: (CenterEntity) -> Unit,
    onDeleteCenter: (CenterEntity) -> Unit,
    onAddStage: (String) -> Unit,
    onUpdateStage: (GradeStageEntity) -> Unit,
    onDeleteStage: (GradeStageEntity) -> Unit,
    onVoiceUnavailable: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingCenter by remember { mutableStateOf<CenterEntity?>(null) }
    var editingStage by remember { mutableStateOf<GradeStageEntity?>(null) }
    var deletingCenter by remember { mutableStateOf<CenterEntity?>(null) }
    var deletingStage by remember { mutableStateOf<GradeStageEntity?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            .testTag("taxonomy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = "إدارة السناتر والمراحل",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingCenter = null
                    editingStage = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (selectedTab == 0) "إضافة سنتر" else "إضافة مرحلة") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_taxonomy_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("السناتر (${centers.size})") },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    modifier = Modifier.testTag("tab_centers")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("المراحل والصفوف (${stages.size})") },
                    icon = { Icon(Icons.Default.School, contentDescription = null) },
                    modifier = Modifier.testTag("tab_stages")
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 14.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 0) {
                        items(centers, key = { it.id }) { center ->
                            val groupCount = allGroups.count { it.group.centerId == center.id }
                            TaxonomyItemCard(
                                title = center.name,
                                subtitle = "$groupCount مجموعات مسجلة",
                                onEdit = {
                                    editingCenter = center
                                    editingStage = null
                                    showAddEditDialog = true
                                },
                                onDelete = { deletingCenter = center },
                                itemTestTag = "center_item_${center.id}"
                            )
                        }
                    } else {
                        items(stages, key = { it.id }) { stage ->
                            val groupCount = allGroups.count { it.group.gradeStageId == stage.id }
                            TaxonomyItemCard(
                                title = stage.name,
                                subtitle = "$groupCount مجموعات مسجلة",
                                onEdit = {
                                    editingStage = stage
                                    editingCenter = null
                                    showAddEditDialog = true
                                },
                                onDelete = { deletingStage = stage },
                                itemTestTag = "stage_item_${stage.id}"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        val isCenterMode = selectedTab == 0
        val initialText = if (isCenterMode) editingCenter?.name ?: "" else editingStage?.name ?: ""
        var text by remember(editingCenter, editingStage, selectedTab) { mutableStateOf(initialText) }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = when {
                        isCenterMode && editingCenter == null -> "إضافة سنتر جديد"
                        isCenterMode -> "تعديل اسم السنتر"
                        editingStage == null -> "إضافة مرحلة دراسية جديدة"
                        else -> "تعديل اسم المرحلة"
                    }
                )
            },
            text = {
                VoiceInputTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = if (isCenterMode) "اسم السنتر" else "اسم المرحلة والصف",
                    placeholder = if (isCenterMode) "مثال: سنتر النور" else "مثال: سادسة ابتدائي",
                    voicePrompt = if (isCenterMode) "انطق اسم السنتر" else "انطق اسم المرحلة",
                    modifier = Modifier.fillMaxWidth(),
                    fieldTestTag = "taxonomy_name_input",
                    micTestTag = "taxonomy_name_voice_mic",
                    onVoiceUnavailable = onVoiceUnavailable
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (text.trim().isNotEmpty()) {
                            if (isCenterMode) {
                                val existing = editingCenter
                                if (existing == null) onAddCenter(text)
                                else onUpdateCenter(existing.copy(name = text))
                            } else {
                                val existing = editingStage
                                if (existing == null) onAddStage(text)
                                else onUpdateStage(existing.copy(name = text))
                            }
                            showAddEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_taxonomy_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    deletingCenter?.let { center ->
        val linkedGroups = allGroups.count { it.group.centerId == center.id }
        AlertDialog(
            onDismissRequest = { deletingCenter = null },
            title = { Text("حذف السنتر؟") },
            text = {
                Text(
                    if (linkedGroups > 0) {
                        "تحذير: يوجد $linkedGroups مجموعات داخل \"${center.name}\". حذفه سيؤدي لحذف هذه المجموعات وطلابها نهائياً."
                    } else {
                        "هل تريد حذف \"${center.name}\"؟"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCenter(center)
                        deletingCenter = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCenter = null }) { Text("إلغاء") }
            }
        )
    }

    deletingStage?.let { stage ->
        val linkedGroups = allGroups.count { it.group.gradeStageId == stage.id }
        AlertDialog(
            onDismissRequest = { deletingStage = null },
            title = { Text("حذف المرحلة الدراسية؟") },
            text = {
                Text(
                    if (linkedGroups > 0) {
                        "تحذير: يوجد $linkedGroups مجموعات في مرحلة \"${stage.name}\". حذفها سيؤدي لحذف هذه المجموعات وطلابها نهائياً."
                    } else {
                        "هل تريد حذف مرحلة \"${stage.name}\"؟"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStage(stage)
                        deletingStage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingStage = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun TaxonomyItemCard(
    title: String,
    subtitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    itemTestTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(itemTestTag),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل $title",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف $title",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
