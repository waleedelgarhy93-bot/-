package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GroupDetailScreen
import com.example.ui.screens.ManageTaxonomyScreen
import com.example.ui.screens.StudentFormScreen
import com.example.ui.theme.CenterLedgerTheme
import com.example.ui.viewmodel.LedgerViewModel

private object Routes {
    const val DASHBOARD = "dashboard"
    const val GROUP_DETAIL = "group/{groupId}"
    const val STUDENT_FORM = "student_form/{groupId}?studentId={studentId}"
    const val MANAGE_TAXONOMY = "manage_taxonomy"
    const val BACKUP_RESTORE = "backup_restore"

    fun groupDetail(groupId: Long) = "group/$groupId"
    fun studentForm(groupId: Long, studentId: Long? = null): String =
        if (studentId != null) "student_form/$groupId?studentId=$studentId"
        else "student_form/$groupId?studentId=-1"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CenterLedgerTheme {
                CenterLedgerApp()
            }
        }
    }
}

@Composable
private fun CenterLedgerApp() {
    val context = LocalContext.current
    val viewModel: LedgerViewModel = viewModel(
        factory = LedgerViewModel.Factory(context.applicationContext as android.app.Application)
    )
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val voiceUnavailableText = stringResource(R.string.voice_unavailable)

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val groupDetailState by viewModel.groupDetailState.collectAsStateWithLifecycle()
    val pendingRestore by viewModel.pendingRestorePayload.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                state = dashboardState,
                snackbarHostState = snackbarHostState,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onResetCurrentMonth = viewModel::resetToCurrentMonth,
                onSelectCenter = viewModel::selectCenterFilter,
                onSelectStage = viewModel::selectStageFilter,
                onSelectGroupFinanceFilter = viewModel::selectGroupFinanceFilter,
                onOpenGroup = { groupId ->
                    viewModel.openGroup(groupId)
                    navController.navigate(Routes.groupDetail(groupId))
                },
                onSaveGroup = viewModel::saveStudyGroup,
                onDeleteGroup = viewModel::deleteStudyGroup,
                onOpenManageTaxonomy = { navController.navigate(Routes.MANAGE_TAXONOMY) },
                onOpenBackupRestore = { navController.navigate(Routes.BACKUP_RESTORE) }
            )
        }

        composable(
            route = Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            LaunchedEffect(groupId) {
                viewModel.openGroup(groupId)
            }

            GroupDetailScreen(
                state = groupDetailState,
                centers = dashboardState.centers,
                stages = dashboardState.stages,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onResetCurrentMonth = viewModel::resetToCurrentMonth,
                onSearchQueryChange = viewModel::updateGroupSearchQuery,
                onSelectFilter = viewModel::selectGroupStudentFilter,
                onAddStudent = {
                    navController.navigate(Routes.studentForm(groupId))
                },
                onEditStudent = { studentId ->
                    navController.navigate(Routes.studentForm(groupId, studentId))
                },
                onSaveGroup = viewModel::saveStudyGroup,
                onToggleStudentSuspension = viewModel::toggleStudentSuspension,
                onDeleteStudent = viewModel::deleteStudent,
                onSetSingleStudentPaid = viewModel::setSingleStudentPaid,
                onMarkAllSiblingsPaid = viewModel::markAllSiblingsPaid,
                onCycleSession = viewModel::cycleSession,
                onVoiceUnavailable = { viewModel.emitMessage(voiceUnavailableText) }
            )
        }

        composable(
            route = Routes.STUDENT_FORM,
            arguments = listOf(
                navArgument("groupId") { type = NavType.LongType },
                navArgument("studentId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            val rawStudentId = backStackEntry.arguments?.getLong("studentId") ?: -1L
            val editingStudentId = rawStudentId.takeIf { it > 0L }

            StudentFormScreen(
                initialGroupId = groupId,
                editingStudentId = editingStudentId,
                allGroups = dashboardState.allGroups,
                allStudentsWithContext = dashboardState.allStudentsWithContext,
                loadStudent = viewModel::loadStudentForEdit,
                onNavigateBack = { navController.popBackStack() },
                onSave = { existingId, targetGroupId, name, stPhone, pPhone, notes, status, siblingId, discount, applyToSibling ->
                    viewModel.saveStudent(
                        existingStudentId = existingId,
                        groupId = targetGroupId,
                        fullName = name,
                        studentPhone = stPhone,
                        parentPhone = pPhone,
                        notes = notes,
                        status = status,
                        selectedSiblingId = siblingId,
                        siblingDiscountAmount = discount,
                        applyDiscountToSelectedSibling = applyToSibling,
                        onSaved = { navController.popBackStack() }
                    )
                },
                onVoiceUnavailable = { viewModel.emitMessage(voiceUnavailableText) }
            )
        }

        composable(Routes.MANAGE_TAXONOMY) {
            ManageTaxonomyScreen(
                centers = dashboardState.centers,
                stages = dashboardState.stages,
                allGroups = dashboardState.allGroups,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onAddCenter = viewModel::addCenter,
                onUpdateCenter = viewModel::updateCenter,
                onDeleteCenter = viewModel::deleteCenter,
                onAddStage = viewModel::addGradeStage,
                onUpdateStage = viewModel::updateGradeStage,
                onDeleteStage = viewModel::deleteGradeStage,
                onVoiceUnavailable = { viewModel.emitMessage(voiceUnavailableText) }
            )
        }

        composable(Routes.BACKUP_RESTORE) {
            BackupRestoreScreen(
                dashboardState = dashboardState,
                pendingRestore = pendingRestore,
                snackbarHostState = snackbarHostState,
                onNavigateBack = { navController.popBackStack() },
                onExportUri = viewModel::exportBackupToUri,
                onInspectImportUri = viewModel::inspectBackupFromUri,
                onConfirmRestore = viewModel::confirmRestoreBackup,
                onDismissRestore = viewModel::dismissPendingRestore
            )
        }
    }
}
