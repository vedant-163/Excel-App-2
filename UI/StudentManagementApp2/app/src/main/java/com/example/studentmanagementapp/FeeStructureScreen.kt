package com.example.studentmanagementapp

import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// DATA
// ============================================================

/**
 * Fee Structure is configuration/business data.
 * It is intentionally kept out of SharedPreferences.
 *
 * Branch is part of the key because applicable fees may differ by branch.
 */
data class FeeStructure(
    val academicYear: String = "",
    val branch: String = "",
    val feeCategory: String = "",
    val semester: String = "",
    val feeAmount: String = "",
    val structureId: Int = 0,
    val feeMastCode: String = ""
)

/**
 * Shared frontend state for the Fee module.
 *
 * Temporary stage:
 *   Fee UI -> in-memory state
 *
 * Final stage:
 *   Fee UI -> Repository -> API -> Database
 *
 * Keeping one state source prevents Fee Structure and Fee Payment from
 * maintaining separate copies of the same data.
 */
object FeeDataStore {
    val feeStructures = mutableStateListOf<FeeStructure>()
    val payments = mutableStateListOf<FeePayment>()
    val students = mutableStateListOf<Student>()
    val feeCategories = mutableStateListOf<String>()
    val paymentModes = mutableStateListOf<String>()
}

// ============================================================
// UI CONFIG
// ============================================================

private val FeeStructureBlue = Color(0xFF1565C0)
private val FeeStructureBackground = Color(0xFFF5F7FB)
private val FeeStructureLightBlue = Color(0xFFEAF2FF)
private val FeeStructureText = Color(0xFF172033)
private val FeeStructureGray = Color(0xFF6B7280)

object FeeCatalog {
    val academicYears: List<String>
        get() = AdmissionCatalog.academicYears

    val semesters: List<String> = listOf(
        "Semester 1",
        "Semester 2",
        "Semester 3",
        "Semester 4",
        "Semester 5",
        "Semester 6",
        "Semester 7",
        "Semester 8"
    )

    // These are the fee-plan categories present in the existing database's fee_category table.
    // They are not admission reservation categories (UR/OBC/SC/ST).
    val feeCategories: List<String> = listOf(
        "BTECH/BPH",
        "BTECH SF&GF",
        "BTECH/BPH FW",
        "BTECH/BPH EW 25%",
        "BTECH/BPH SF EW 25%",
        "BTECH/BPH EW 80%",
        "BTECH/BPH FW EW 80%",
        "BTECH/BPH NRI OCI FN"
    )

    val paymentModes: List<String> = listOf(
        "Online",
        "Cash",
        "DD",
        "Cheque",
        "Other"
    )
}

// ============================================================
// MAIN
// ============================================================

@Composable
fun FeeStructureScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showForm by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<FeeStructure?>(null) }
    var deleteRecord by remember { mutableStateOf<FeeStructure?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        loading = true
        errorMessage = ""
        scope.launch {
            try {
                val remote = ApiClient.getFeeStructures(context)
                FeeDataStore.feeStructures.clear()
                FeeDataStore.feeStructures.addAll(remote)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unable to load fee structures."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (FeeDataStore.feeStructures.isNotEmpty()) {
            // Data already loaded by the Fee Payment module – reuse without another network call.
            loading = false
            return@LaunchedEffect
        }
        try {
            val remote = ApiClient.getFeeStructures(context)
            FeeDataStore.feeStructures.clear()
            FeeDataStore.feeStructures.addAll(remote)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unable to load fee structures."
        } finally {
            loading = false
        }
    }

    if (showForm) {
        FeeStructureForm(
            initialRecord = editingRecord,
            onSaved = { editingRecord = null; showForm = false; reload() },
            onBack = { editingRecord = null; showForm = false }
        )
        return
    }

    FeeStructureRecords(
        records = FeeDataStore.feeStructures,
        loading = loading,
        errorMessage = errorMessage,
        onRetry = { reload() },
        onAdd = { editingRecord = null; showForm = true },
        onEdit = { record -> editingRecord = record; showForm = true },
        onDelete = { record -> deleteRecord = record },
        onBack = onBack
    )

    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            title = { Text("Delete Fee Structure?") },
            text = { Text("Remove ${record.academicYear} • ${BranchCatalog.canonicalBranch(record.branch)} • ${record.feeCategory} • ${record.semester}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            ApiClient.deleteFeeSemester(context, record.structureId, record.semester)
                            deleteRecord = null
                            reload()
                            Toast.makeText(context, "Fee structure deleted", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Delete failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("DELETE", color = Color(0xFFC62828)) }
            },
            dismissButton = { TextButton(onClick = { deleteRecord = null }) { Text("CANCEL") } }
        )
    }
}

// ============================================================
// RECORDS
// ============================================================

@Composable
private fun FeeStructureRecords(
    records: List<FeeStructure>,
    loading: Boolean,
    errorMessage: String,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (FeeStructure) -> Unit,
    onDelete: (FeeStructure) -> Unit,
    onBack: () -> Unit
) {
    var selectedYear by remember { mutableStateOf("All Academic Years") }
    var selectedBranch by remember { mutableStateOf("All Branches") }
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var selectedSemester by remember { mutableStateOf("All Semesters") }

    val semesterOptions = listOf("All Semesters") + FeeCatalog.semesters

    val yearOptions = listOf("All Academic Years") +
            (AdmissionCatalog.academicYears.toList() + records.map { it.academicYear })
                .map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    val branchOptions = listOf("All Branches") +
            (records.map { BranchCatalog.canonicalBranch(it.branch) } + BranchCatalog.standardBranches + BranchCatalog.AI_ROBOTICS)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

    val categoryOptions = listOf("All Categories") + FeeCatalog.feeCategories

    val filtered = records.filter { record ->
        val branch = BranchCatalog.canonicalBranch(record.branch)
        val selectedBranchCode = BranchCatalog.branchCode(selectedBranch)
        val recordBranchCode = BranchCatalog.branchCode(record.branch)

        (selectedYear == "All Academic Years" ||
                normalizeFeeYear(record.academicYear) == normalizeFeeYear(selectedYear)) &&
                (selectedBranch == "All Branches" ||
                        branch.equals(selectedBranch, ignoreCase = true) ||
                        recordBranchCode.equals(selectedBranchCode, ignoreCase = true)) &&
                (selectedCategory == "All Categories" ||
                        record.feeCategory.equals(selectedCategory, ignoreCase = true)) &&
                (selectedSemester == "All Semesters" ||
                        record.semester.filter { it.isDigit() } == selectedSemester.filter { it.isDigit() })
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FeeStructureBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            FeeStructureHeader(
                title = "Fee Structure",
                subtitle = "Configure applicable fees by academic year, branch and category",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                Text("Loading fee structures from database...", modifier = Modifier.padding(horizontal = 16.dp), color = FeeStructureGray)
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (errorMessage.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(errorMessage, color = Color(0xFFC62828))
                        TextButton(onClick = onRetry) { Text("RETRY") }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FeeStructureBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "ADD FEE STRUCTURE",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FeeStructureDropdown(
                label = "Academic Year",
                value = selectedYear,
                options = yearOptions,
                onSelected = { selectedYear = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeeStructureDropdown(
                label = "Branch",
                value = selectedBranch,
                options = branchOptions,
                onSelected = { selectedBranch = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeeStructureDropdown(
                label = "Category",
                value = selectedCategory,
                options = categoryOptions,
                onSelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FeeStructureDropdown(
                label = "Semester",
                value = selectedSemester,
                options = semesterOptions,
                onSelected = { selectedSemester = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Fee Records: ${filtered.size}",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = FeeStructureText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                FeeStructureEmptyCard(
                    if (records.isEmpty()) {
                        "No fee structure configured yet"
                    } else {
                        "No fee structure matches the selected filters"
                    }
                )
            } else {
                filtered.forEach { record ->
                    FeeStructureCard(
                        record = record,
                        onEdit = { onEdit(record) },
                        onDelete = { onDelete(record) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "BACK TO DASHBOARD",
                    color = FeeStructureBlue
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============================================================
// FORM
// ============================================================

@Composable
private fun FeeStructureForm(
    initialRecord: FeeStructure?,
    onSaved: (FeeStructure) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var academicYear by remember(initialRecord) {
        mutableStateOf(initialRecord?.academicYear.orEmpty())
    }
    var branch by remember(initialRecord) {
        mutableStateOf(initialRecord?.let { BranchCatalog.canonicalBranch(it.branch) }.orEmpty())
    }
    var feeCategory by remember(initialRecord) {
        mutableStateOf(initialRecord?.feeCategory.orEmpty())
    }
    var semester by remember(initialRecord) {
        mutableStateOf(initialRecord?.semester.orEmpty())
    }
    var feeAmount by remember(initialRecord) {
        mutableStateOf(initialRecord?.feeAmount.orEmpty())
    }

    val branchOptions = if (academicYear.isBlank()) {
        emptyList()
    } else {
        (BranchCatalog.optionsForAcademicYear(academicYear) +
                FeeDataStore.feeStructures
                    .filter { it.academicYear.equals(academicYear, ignoreCase = true) }
                    .map { it.branch })
            .map { BranchCatalog.canonicalBranch(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun clearBranchIfInvalid() {
        if (branch.isNotBlank() && branch !in branchOptions) {
            branch = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FeeStructureBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            FeeStructureHeader(
                title = if (initialRecord == null) "Add Fee Structure" else "Edit Fee Structure",
                subtitle = "Set the applicable fee for a specific year, branch, category and semester",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(18.dp))

            FeeStructureSectionTitle(
                "01",
                "Fee Configuration",
                "Choose the exact combination to avoid incorrect fee calculation"
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeeStructureDropdown(
                label = "Academic Year",
                value = academicYear,
                options = AdmissionCatalog.academicYears,
                onSelected = {
                    academicYear = it
                    clearBranchIfInvalid()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeeStructureDropdown(
                label = "Branch",
                value = branch,
                options = branchOptions,
                enabled = academicYear.isNotBlank(),
                onSelected = { branch = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeeStructureDropdown(
                label = "Fee Category",
                value = feeCategory,
                options = FeeCatalog.feeCategories,
                onSelected = { feeCategory = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeeStructureDropdown(
                label = "Semester",
                value = semester,
                options = FeeCatalog.semesters,
                onSelected = { semester = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = feeAmount,
                onValueChange = {
                    if (
                        it.isEmpty() ||
                        it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))
                    ) {
                        feeAmount = it
                    }
                },
                label = { Text("Applicable Fee Amount") },
                placeholder = { Text("Enter amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FeeStructureLightBlue
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Configuration key",
                        fontWeight = FontWeight.Bold,
                        color = FeeStructureBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = listOf(
                            academicYear,
                            branch,
                            feeCategory,
                            semester
                        ).filter { it.isNotBlank() }.joinToString(" • ")
                            .ifBlank { "Select all fields" },
                        color = FeeStructureText,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val amount = feeAmount.trim().toDoubleOrNull()

                    when {
                        academicYear.isBlank() ->
                            showFeeStructureToast(context, "Please select academic year")

                        branch.isBlank() ->
                            showFeeStructureToast(context, "Please select branch")

                        feeCategory.isBlank() ->
                            showFeeStructureToast(context, "Please select fee category")

                        semester.isBlank() ->
                            showFeeStructureToast(context, "Please select semester")

                        amount == null || amount <= 0.0 ->
                            showFeeStructureToast(context, "Enter a valid fee amount")

                        else -> {
                            val normalized = FeeStructure(
                                academicYear = academicYear.trim(),
                                branch = BranchCatalog.canonicalBranch(branch),
                                feeCategory = feeCategory.trim(),
                                semester = semester.trim(),
                                feeAmount = "%.2f".format(java.util.Locale.US, amount),
                                structureId = initialRecord?.structureId ?: 0,
                                feeMastCode = initialRecord?.feeMastCode.orEmpty()
                            )

                            scope.launch {
                                try {
                                    ApiClient.saveFeeSemester(context, normalized)
                                    Toast.makeText(context, if (initialRecord == null) "Fee structure saved" else "Fee structure updated", Toast.LENGTH_SHORT).show()
                                    onSaved(normalized)
                                } catch (e: Exception) {
                                    showFeeStructureToast(context, e.message ?: "Unable to save fee structure")
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FeeStructureBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (initialRecord == null) "SAVE FEE STRUCTURE" else "UPDATE FEE STRUCTURE",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CANCEL", color = FeeStructureBlue)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============================================================
// HEADER
// ============================================================

@Composable
private fun FeeStructureHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = FeeStructureBlue
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 13.dp, vertical = 9.dp)
                    .then(
                        Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        "←",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ============================================================
// COMMON UI
// ============================================================

@Composable
private fun FeeStructureSectionTitle(
    number: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .width(46.dp)
                .height(46.dp),
            shape = RoundedCornerShape(13.dp),
            colors = CardDefaults.cardColors(
                containerColor = FeeStructureLightBlue
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number,
                    color = FeeStructureBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FeeStructureText
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = FeeStructureGray
            )
        }
    }
}

@Composable
private fun FeeStructureDropdown(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val cleanOptions = options.distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = FeeStructureGray,
            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (enabled && cleanOptions.isNotEmpty()) expanded = true },
                enabled = enabled && cleanOptions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value.ifBlank {
                            if (cleanOptions.isEmpty()) "No options available" else "Select $label"
                        },
                        color = if (value.isBlank()) FeeStructureGray else FeeStructureText
                    )
                    Text("▼", fontSize = 11.sp, color = FeeStructureBlue)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                cleanOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeeStructureCard(
    record: FeeStructure,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${record.academicYear} • ${BranchCatalog.canonicalBranch(record.branch)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FeeStructureText
            )

            Spacer(modifier = Modifier.height(8.dp))

            FeeStructureInfoRow("Category", record.feeCategory)
            FeeStructureInfoRow("Semester", record.semester)
            FeeStructureInfoRow("Applicable Fee", "₹ ${record.feeAmount}")

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("EDIT", color = FeeStructureBlue)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DELETE", color = Color(0xFFC62828))
                }
            }
        }
    }
}

@Composable
private fun FeeStructureInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            color = FeeStructureGray,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            value.ifBlank { "N/A" },
            color = FeeStructureText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeeStructureEmptyCard(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = FeeStructureText,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun showFeeStructureToast(
    context: android.content.Context,
    message: String
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}


private fun normalizeFeeYear(value: String): String {
    val cleaned = value.trim()
    val match = Regex("^(\\d{4})\\s*[-/]\\s*(\\d{2}|\\d{4})$").matchEntire(cleaned)
    if (match != null) {
        val start = match.groupValues[1]
        val endRaw = match.groupValues[2]
        val end = if (endRaw.length == 2) start.take(2) + endRaw else endRaw
        return "$start-$end"
    }
    return cleaned.replace(" ", "").lowercase()
}
