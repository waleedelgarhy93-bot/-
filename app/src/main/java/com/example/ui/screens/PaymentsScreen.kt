package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.MonthSelectorBar
import com.example.ui.theme.LedgerStatusColors
import com.example.ui.viewmodel.PaymentRowUi
import com.example.ui.viewmodel.PaymentStatusFilter
import com.example.ui.viewmodel.PaymentsUiState
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    state: PaymentsUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onSelectCenter: (Long?) -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onSelectStatus: (PaymentStatusFilter) -> Unit,
    onSetPaid: (Long, Boolean, String) -> Unit,
    onExport: (Uri, Boolean) -> Unit
) {
    var undoRow by remember { mutableStateOf<PaymentRowUi?>(null) }
    val name = "المدفوعات - ${state.selectedMonth.toArabicDisplay()}"
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> uri?.let { onExport(it, false) } }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { onExport(it, true) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                title = { Text("المدفوعات", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { excelLauncher.launch("$name.xlsx") }) {
                        Icon(Icons.Default.TableChart, contentDescription = "تصدير Excel", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { pdfLauncher.launch("$name.pdf") }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "تصدير PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    MonthSelectorBar(
                        selectedMonth = state.selectedMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onResetCurrent = onResetCurrentMonth
                    )
                }
                item { PaymentsSummary(state) }

                item { SectionLabel("السنتر") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Chip("كل السناتر", state.selectedCenterId == null) { onSelectCenter(null) } }
                        items(state.centers, key = { it.id }) { c ->
                            Chip(c.name, state.selectedCenterId == c.id) { onSelectCenter(c.id) }
                        }
                    }
                }
                item { SectionLabel("المجموعة") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Chip("كل المجموعات", state.selectedGroupId == null) { onSelectGroup(null) } }
                        items(state.groups, key = { it.id }) { g ->
                            Chip(g.groupName, state.selectedGroupId == g.id) { onSelectGroup(g.id) }
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Chip("الكل (${state.paidCount + state.unpaidCount})", state.status == PaymentStatusFilter.ALL) {
                                onSelectStatus(PaymentStatusFilter.ALL)
                            }
                        }
                        item {
                            Chip("دفعوا (${state.paidCount})", state.status == PaymentStatusFilter.PAID, LedgerStatusColors.PaidText) {
                                onSelectStatus(PaymentStatusFilter.PAID)
                            }
                        }
                        item {
                            Chip("لسه ما دفعوش (${state.unpaidCount})", state.status == PaymentStatusFilter.UNPAID, LedgerStatusColors.UnpaidText) {
                                onSelectStatus(PaymentStatusFilter.UNPAID)
                            }
                        }
                    }
                }

                if (state.rows.isEmpty()) {
                    item {
                        Text(
                            "مافيش طلاب هنا",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                } else {
                    itemsIndexed(state.rows, key = { _, r -> r.student.id }) { i, r ->
                        PaymentRowCard(i + 1, r) {
                            if (r.isPaid) undoRow = r else onSetPaid(r.student.id, true, r.student.fullName)
                        }
                    }
                }
            }
        }
    }

    undoRow?.let { r ->
        AlertDialog(
            onDismissRequest = { undoRow = null },
            title = { Text("إلغاء تسجيل الدفع؟") },
            text = { Text("\"${r.student.fullName}\" هيرجع لم يدفع لشهر ${state.selectedMonth.toArabicDisplay()}.") },
            confirmButton = {
                Button(
                    onClick = { onSetPaid(r.student.id, false, r.student.fullName); undoRow = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("نعم، إلغاء الدفع") }
            },
            dismissButton = { TextButton(onClick = { undoRow = null }) { Text("تراجع") } }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Chip(label: String, selected: Boolean, color: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color ?: MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun PaymentsSummary(state: PaymentsUiState) {
    val remaining = (state.totalExpected - state.totalCollected).coerceAtLeast(0.0)
    val progress = if (state.totalExpected <= 0.0) 0f else (state.totalCollected / state.totalExpected).toFloat().coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Metric("المطلوب", Formatters.formatEgp(state.totalExpected), MaterialTheme.colorScheme.onSurface)
                Metric("المُحصَّل", Formatters.formatEgp(state.totalCollected), LedgerStatusColors.PaidText)
                Metric("المتبقي", Formatters.formatEgp(remaining), if (remaining > 0) LedgerStatusColors.UnpaidText else LedgerStatusColors.PaidText)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                "دفع ${state.paidCount} • لسه ${state.unpaidCount} • نسبة التحصيل ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PaymentRowCard(number: Int, r: PaymentRowUi, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (r.isPaid) LedgerStatusColors.PaidBorder else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$number. ${r.student.fullName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${r.centerName} • ${r.groupName}" + (r.paymentDate?.let { " • ${Formatters.formatShortDate(it)}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (r.isPaid) LedgerStatusColors.PaidContainer else LedgerStatusColors.UnpaidContainer,
                border = BorderStroke(1.dp, if (r.isPaid) LedgerStatusColors.PaidBorder else LedgerStatusColors.UnpaidBorder),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle)
            ) {
                Text(
                    if (r.isPaid) "دفع ✓ ${Formatters.formatAmount(r.paid)}" else "لم يدفع ${Formatters.formatAmount(r.expected)}",
                    color = if (r.isPaid) LedgerStatusColors.PaidText else LedgerStatusColors.UnpaidText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
