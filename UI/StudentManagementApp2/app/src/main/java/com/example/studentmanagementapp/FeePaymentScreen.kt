package com.example.studentmanagementapp

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// =====================================================
// DATA
// =====================================================

data class FeePayment(
    val studentId: String = "",
    val studentName: String = "",
    val enrollmentNo: String = "",
    val branch: String = "",
    val category: String = "",
    val feeCategory: String = "",
    val academicYear: String = "",
    val semester: String = "",
    val totalFee: String = "",
    val paidAmount: String = "",
    val pendingAmount: String = "",
    val paymentDate: String = "",
    val paymentMode: String = "",
    val transactionId: String = "",
    val remarks: String = "",
    val paymentStatus: String = ""
)

// =====================================================
// FEE CONFIG
// FeeCatalog is centralized in FeeStructureScreen.kt so the Fee module
// uses one source for academic years, semesters and payment modes.
// =====================================================

// =====================================================
// COLORS
// =====================================================

private val FeeBlue = Color(0xFF1565C0)
private val FeeBackground = Color(0xFFF5F7FB)
private val FeeLightBlue = Color(0xFFEAF2FF)
private val FeeText = Color(0xFF172033)
private val FeeGray = Color(0xFF6B7280)

// =====================================================
// SHARED FRONTEND STATE
// Fee structures and payments are kept in the shared in-memory store.
// This is intentionally temporary until the repository/backend is connected.
// =====================================================

// =====================================================
// MAIN
// =====================================================

@Composable
fun FeePaymentScreen(
    students: SnapshotStateList<Student>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val feeStructures = FeeDataStore.feeStructures
    val payments = FeeDataStore.payments
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var paymentLoadError by remember { mutableStateOf("") }
    var feeStudents by remember {
        mutableStateOf(FeeDataStore.students.toList().ifEmpty { students.toList() })
    }

    var currentScreen by remember {
        mutableStateOf("menu")
    }

    LaunchedEffect(Unit) {
        loading = true
        error = ""
        paymentLoadError = ""
        val loadErrors = mutableListOf<String>()
        val remotePayments = try { ApiClient.getPayments(context) }
            catch (e: Exception) {
                paymentLoadError = e.message ?: "Unable to load payments"
                loadErrors += "payments: $paymentLoadError"
                emptyList()
            }
        val remoteFeeStructures = try { ApiClient.getFeeStructures(context) }
            catch (e: Exception) { loadErrors += "fee structures: ${e.message ?: "unavailable"}"; emptyList() }
        payments.clear()
        payments.addAll(remotePayments)
        feeStructures.clear()
        feeStructures.addAll(remoteFeeStructures)
        // The Pending Fee screen needs the full candidate set; this is a single
        // bounded API fetch, not the source of truth for interactive student search.
        if (FeeDataStore.students.isNotEmpty()) {
            feeStudents = FeeDataStore.students.toList()
        } else {
            feeStudents = try {
                ApiClient.getAllStudents(context).also { all ->
                    FeeDataStore.students.clear()
                    FeeDataStore.students.addAll(all)
                }
            } catch (e: Exception) {
                loadErrors += "student list: ${e.message ?: "unavailable"}"
                students.toList()
            }
        }
        FeeDataStore.feeCategories.clear()
        FeeDataStore.feeCategories.addAll(
            try { ApiClient.getFeeCategories(context) }
            catch (_: Exception) { remoteFeeStructures.map { it.feeCategory }.filter { it.isNotBlank() }.distinct().sorted() }
        )
        FeeDataStore.paymentModes.clear()
        FeeDataStore.paymentModes.addAll(
            try { ApiClient.getPaymentModes(context) }
            catch (e: Exception) { loadErrors += "payment modes: ${e.message ?: "unavailable"}"; emptyList() }
        )
        error = loadErrors.joinToString("; ")
        loading = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FeeBackground
    ) {
        if (loading) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = FeeBlue)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Preparing fee workspace", fontWeight = FontWeight.SemiBold, color = FeeText)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Connecting to college records", fontSize = 13.sp, color = FeeGray)
            }
        } else when (currentScreen) {

            "menu" -> FeePaymentMenu(
                onStudentFeeInformation = {
                    currentScreen = "studentFee"
                },
                onNewPayment = {
                    currentScreen = "newPayment"
                },
                onPaymentRecords = {
                    currentScreen = "records"
                },
                onPendingFees = {
                    currentScreen = "pending"
                },
                loadError = error,
                onBack = onBack
            )

            "studentFee" -> StudentFeeInformationScreen(
                students = students,
                payments = payments,
                feeStructures = feeStructures,
                onBack = {
                    currentScreen = "menu"
                }
            )

            "newPayment" -> NewPaymentScreen(
                students = students,
                payments = payments,
                feeStructures = feeStructures,
                onSavePayment = { payment, onComplete ->
                    scope.launch {
                        try {
                            ApiClient.savePayment(context, payment)
                            val fresh = ApiClient.getPayments(context)
                            payments.clear()
                            payments.addAll(fresh)
                            Toast.makeText(context, "Payment saved successfully", Toast.LENGTH_SHORT).show()
                            onComplete(true)
                            currentScreen = "records"
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Unable to save payment", Toast.LENGTH_LONG).show()
                            onComplete(false)
                        }
                    }
                },
                onBack = {
                    currentScreen = "menu"
                }
            )

            "records" -> PaymentRecordsScreen(
                payments = payments,
                feeStructures = feeStructures,
                loadError = paymentLoadError,
                onBack = {
                    currentScreen = "menu"
                }
            )

            "pending" -> PendingFeesScreen(
                students = feeStudents,
                payments = payments,
                feeStructures = feeStructures,
                onBack = {
                    currentScreen = "menu"
                }
            )
        }
    }
}

// =====================================================
// HEADER
// =====================================================

@Composable
fun FeeProfessionalHeader(
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
            containerColor = FeeBlue
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 20.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onBack)
                        .padding(
                            horizontal = 13.dp,
                            vertical = 9.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// =====================================================
// MENU
// =====================================================

@Composable
fun FeePaymentMenu(
    onStudentFeeInformation: () -> Unit,
    onNewPayment: () -> Unit,
    onPaymentRecords: () -> Unit,
    onPendingFees: () -> Unit,
    loadError: String = "",
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(FeeBackground)
    ) {
        FeeProfessionalHeader(
            title = "Fee & Payment",
            subtitle = "Manage student fees and payment information",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (loadError.isNotBlank()) {
            EmptyCard("Some fee data could not be loaded: $loadError")
            Spacer(modifier = Modifier.height(12.dp))
        }

        FeeMenuCard(
            number = "01",
            title = "Student Fee Information",
            description = "View applicable fee, paid amount, pending amount and payment history",
            onClick = onStudentFeeInformation
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeMenuCard(
            number = "02",
            title = "New Payment",
            description = "Record a new student fee payment safely",
            onClick = onNewPayment
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeMenuCard(
            number = "03",
            title = "Payment Records",
            description = "Search and review saved payment transactions",
            onClick = onPaymentRecords
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeMenuCard(
            number = "04",
            title = "Pending Fees",
            description = "Find students who still have an outstanding fee",
            onClick = onPendingFees
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FeeMenuCard(
    number: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = FeeLightBlue
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        fontWeight = FontWeight.Bold,
                        color = FeeBlue
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeeText
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = FeeGray
                )
            }
        }
    }
}

// =====================================================
// STUDENT FEE INFORMATION
// =====================================================

@Composable
fun StudentFeeInformationScreen(
    students: List<Student>,
    payments: List<FeePayment>,
    feeStructures: List<FeeStructure>,
    onBack: () -> Unit
) {

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }
    val studentForFeeInfo = selectedStudent;

    var selectedYear by remember {
        mutableStateOf("")
    }

    var selectedFeeCategory by remember {
        mutableStateOf("")
    }

    var selectedSemester by remember {
        mutableStateOf("")
    }

    // Fee structures are already fetched by the parent Fee module. Use that shared data
    // instead of making another request every time a filter changes.
    val queriedFeeStructures = feeStructures

    val assignedStudentFeeStructure = feeStructures.firstOrNull { record ->
        selectedStudent?.feeStructureCode.orEmpty().trim().let { code ->
            code.isNotBlank() && record.feeMastCode.trim().equals(code, ignoreCase = true)
        }
    }

    val selectedFeeRecords = queriedFeeStructures.filter { record ->
        val assignedCode = selectedStudent?.feeStructureCode.orEmpty().trim()
        val useAssignedStructure = assignedCode.isNotBlank() &&
            (selectedYear.isBlank() || selectedYear.equals(assignedStudentFeeStructure?.academicYear, ignoreCase = true)) &&
            (selectedFeeCategory.isBlank() || selectedFeeCategory.equals(assignedStudentFeeStructure?.feeCategory, ignoreCase = true))
        selectedStudent != null &&
            (if (useAssignedStructure) {
                record.feeMastCode.trim().equals(assignedCode, ignoreCase = true)
            } else {
                BranchCatalog.canonicalBranch(record.branch).equals(
                    BranchCatalog.canonicalBranch(studentForFeeInfo?.branch.orEmpty()), ignoreCase = true
                )
            }) &&
            (selectedYear.isBlank() || sameAcademicYear(record.academicYear, selectedYear)) &&
            (selectedFeeCategory.isBlank() || record.feeCategory.equals(selectedFeeCategory, ignoreCase = true)) &&
            (selectedSemester.isBlank() || record.semester.equals(selectedSemester, ignoreCase = true))
    }
    val hasCompleteFeeContext = selectedYear.isNotBlank() && selectedFeeCategory.isNotBlank() && selectedSemester.isNotBlank()
    val applicableFee = if (hasCompleteFeeContext) {
        selectedFeeRecords.firstOrNull()?.feeAmount?.toDoubleOrNull() ?: 0.0
    } else 0.0

    val matchingPayments = payments.filter { payment ->
        selectedStudent != null &&
            (payment.enrollmentNo.trim().equals(studentForFeeInfo?.enrollmentNo.orEmpty().trim(), ignoreCase = true) ||
                (studentForFeeInfo?.uniqueId.orEmpty().isNotBlank() && payment.studentId.trim().equals(studentForFeeInfo?.uniqueId.orEmpty().trim(), ignoreCase = true))) &&
            (selectedYear.isBlank() || sameAcademicYear(payment.academicYear, selectedYear)) &&
            (selectedFeeCategory.isBlank() || payment.feeCategory.isBlank() || payment.feeCategory.equals(selectedFeeCategory, ignoreCase = true)) &&
            (selectedSemester.isBlank() || payment.semester.isBlank() || payment.semester.filter { it.isDigit() } == selectedSemester.filter { it.isDigit() } || payment.semester.equals(selectedSemester, ignoreCase = true)) &&
            (payment.branch.isBlank() || studentForFeeInfo?.branch.isNullOrBlank() || BranchCatalog.canonicalBranch(payment.branch).equals(
                BranchCatalog.canonicalBranch(studentForFeeInfo?.branch.orEmpty()), ignoreCase = true
            ))
    }
    val paidAmount = matchingPayments.sumOf { it.paidAmount.toDoubleOrNull() ?: 0.0 }
    val pendingAmount = calculatePending(applicableFee = applicableFee, paidAmount = paidAmount)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(FeeBackground)
    ) {
        FeeProfessionalHeader(
            title = "Student Fee Information",
            subtitle = "View complete fee information of a student",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        StudentSearchSelector(
            students = students,
            selectedStudent = selectedStudent,
            onStudentSelected = { student ->
                selectedStudent = student
                selectedSemester = ""
                val assigned = feeStructures.firstOrNull {
                    !student?.feeStructureCode.isNullOrBlank() &&
                            it.feeMastCode.trim().equals(
                                student?.feeStructureCode?.trim(),
                                ignoreCase = true
                            )
                }
                selectedYear = assigned?.academicYear.orEmpty()
                selectedFeeCategory = assigned?.feeCategory.orEmpty()
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        selectedStudent?.let {
            StudentBasicCard(it)
            Spacer(modifier = Modifier.height(18.dp))
        }

        FeeDropdown(
            label = "Academic Year",
            value = selectedYear,
            options = listOf("") + (feeAcademicYears(feeStructures) + payments.map { it.academicYear })
                .filter { it.isNotBlank() }.distinct().sortedDescending(),
            enabled = true,
            onSelected = {
                if (selectedYear != it) {
                    selectedYear = it
                    selectedSemester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeDropdown(
            label = "Fee Category",
            value = selectedFeeCategory,
            options = listOf("") + feeCategoriesFor(
                feeStructures = feeStructures,
                academicYear = selectedYear,
                student = selectedStudent
            ),
            enabled = true,
            onSelected = {
                if (selectedFeeCategory != it) {
                    selectedFeeCategory = it
                    selectedSemester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeDropdown(
            label = "Semester",
            value = selectedSemester,
            options = listOf("") + semesterOptionsFor(
                feeStructures = feeStructures,
                academicYear = selectedYear,
                feeCategory = selectedFeeCategory,
                student = selectedStudent
            ),
            enabled = true,
            onSelected = {
                selectedSemester = it
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedStudent != null && !hasCompleteFeeContext) {
            SearchHintCard(
                when {
                    selectedYear.isBlank() -> "Select Academic Year, Fee Category and Semester to view the applicable fee breakdown."
                    selectedFeeCategory.isBlank() -> "Select Fee Category and Semester to calculate applicable fees."
                    selectedSemester.isBlank() -> "Select Semester to view the semester applicable fee and pending balance."
                    else -> "Select academic year, category and semester to view fee breakdown."
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            FeeSummaryCard(
                applicableFee = applicableFee,
                paidAmount = paidAmount,
                pendingAmount = if (applicableFee > 0.0) pendingAmount else 0.0
            )
        } else {
            FeeSummaryCard(
                applicableFee = applicableFee,
                paidAmount = paidAmount,
                pendingAmount = pendingAmount
            )
        }

        if (selectedStudent != null) {
            Spacer(modifier = Modifier.height(20.dp))

            SectionTitle("Payment History")

            Spacer(modifier = Modifier.height(10.dp))

            if (matchingPayments.isEmpty()) {
                EmptyCard("No payment made for this fee category and semester")
            } else {
                matchingPayments.asReversed().forEach { payment ->
                    PaymentSmallCard(payment)

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        FeeBackButton(onBack)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// =====================================================
// NEW PAYMENT
// =====================================================

@Composable
fun NewPaymentScreen(
    students: List<Student>,
    payments: List<FeePayment>,
    feeStructures: List<FeeStructure>,
    onSavePayment: (FeePayment, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }
    val currentStudent = selectedStudent;

    var academicYear by remember {
        mutableStateOf("")
    }

    var semester by remember {
        mutableStateOf("")
    }

    var feeCategory by remember {
        mutableStateOf("")
    }

    var paidAmount by remember {
        mutableStateOf("")
    }

    var paymentDate by remember {
        mutableStateOf("")
    }

    var paymentMode by remember {
        mutableStateOf("")
    }

    var transactionId by remember {
        mutableStateOf("")
    }

    var remarks by remember {
        mutableStateOf("")
    }
    var savingPayment by remember { mutableStateOf(false) }
    var queriedFeeStructures by remember { mutableStateOf(feeStructures) }

    LaunchedEffect(academicYear, feeCategory, semester, selectedStudent?.branch) {
        queriedFeeStructures = if (
            academicYear.isBlank() && feeCategory.isBlank() && semester.isBlank()
        ) feeStructures else try {
            ApiClient.getFeeStructures(
                context = context,
                academicYear = academicYear,
                feeCategory = feeCategory,
                semester = semester,
                branch = selectedStudent?.branch.orEmpty()
            )
        } catch (_: Exception) {
            feeStructures.filter { record ->
                (academicYear.isBlank() || record.academicYear.equals(academicYear, true)) &&
                    (feeCategory.isBlank() || record.feeCategory.equals(feeCategory, true)) &&
                    (semester.isBlank() || record.semester.equals(semester, true)) &&
                    (selectedStudent == null || BranchCatalog.canonicalBranch(record.branch).equals(
                        BranchCatalog.canonicalBranch(currentStudent?.branch.orEmpty()), true
                    ))
            }
        }
    }

    val applicableFee = getApplicableFee(
        feeStructures = queriedFeeStructures,
        student = selectedStudent,
        academicYear = academicYear,
        feeCategory = feeCategory,
        semester = semester
    )

    val previousPaid = payments
        .filter {
            paymentMatches(
                payment = it,
                student = selectedStudent,
                academicYear = academicYear,
                feeCategory = feeCategory,
                semester = semester
            )
        }
        .sumOf {
            it.paidAmount.toDoubleOrNull() ?: 0.0
        }

    val currentPaid = paidAmount.toDoubleOrNull() ?: 0.0

    val totalPaidAfterPayment = previousPaid + currentPaid

    val pending = calculatePending(
        applicableFee = applicableFee,
        paidAmount = totalPaidAfterPayment
    )

    val transactionRequired = paymentMode.trim().uppercase(Locale.ROOT) in setOf(
        "ONLINE", "DD", "NEFT", "RTGS", "CARD", "UPI", "CHEQUE",
        "DEMAND DRAFT", "DIMAND DRAFT", "DIMAND DRAFT (DD)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(FeeBackground)
    ) {
        FeeProfessionalHeader(
            title = "New Payment",
            subtitle = "Add a new student fee payment",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        StudentSearchSelector(
            students = students,
            selectedStudent = selectedStudent,
            onStudentSelected = {
                selectedStudent = it
                academicYear = ""
                semester = ""
                feeCategory = ""
                paidAmount = ""
                paymentDate = ""
                paymentMode = ""
                transactionId = ""
                remarks = ""
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        selectedStudent?.let {
            StudentBasicCard(it)
            Spacer(modifier = Modifier.height(18.dp))
        }

        FeeDropdown(
            label = "Academic Year",
            value = academicYear,
            options = listOf("") + feeAcademicYears(feeStructures),
            enabled = true,
            onSelected = {
                if (academicYear != it) {
                    academicYear = it
                    feeCategory = ""
                    semester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeDropdown(
            label = "Fee Category",
            value = feeCategory,
            options = listOf("") + feeCategoriesFor(feeStructures, academicYear, selectedStudent),
            enabled = true,
            onSelected = {
                if (feeCategory != it) {
                    feeCategory = it
                    semester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeDropdown(
            label = "Semester",
            value = semester,
options = listOf("") + semesterOptionsFor(
                feeStructures = feeStructures,
                academicYear = academicYear,
                feeCategory = feeCategory,
                student = selectedStudent
            ),
            enabled = true,
            onSelected = {
                semester = it
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        FeeSummaryCard(
            applicableFee = applicableFee,
            paidAmount = totalPaidAfterPayment,
            pendingAmount = pending
        )

        if (previousPaid > 0.0) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Previous payments: ${formatMoney(previousPaid)}",
                fontSize = 13.sp,
                color = FeeGray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        FeeTextField(
            value = paidAmount,
            label = "Current Paid Amount",
            enabled = selectedStudent != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            onValueChange = {
                if (
                    it.isEmpty() ||
                    it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))
                ) {
                    paidAmount = it
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DatePickerField(
            value = paymentDate,
            label = "Payment Date",
            enabled = true,
            onDateSelected = {
                paymentDate = it
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Payment Mode",
            value = paymentMode,
            options = (FeeDataStore.paymentModes.toList() + FeeCatalog.paymentModes)
                .filter { it.isNotBlank() }.distinct(),
            enabled = selectedStudent != null,
            onSelected = {
                paymentMode = it

                if (it.trim().uppercase(Locale.ROOT) in setOf("CASH", "OTHER")) {
                    transactionId = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeTextField(
            value = transactionId,
            label = if (transactionRequired) {
                "Transaction / Reference ID *"
            } else {
                "Transaction / Reference ID (Optional)"
            },
            enabled = selectedStudent != null,
            onValueChange = {
                transactionId = it
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeTextField(
            value = remarks,
            label = "Remarks (Optional)",
            enabled = true,
            onValueChange = {
                remarks = it
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                val student = selectedStudent

                when {
                    student == null -> {
                        showFeeToast(context, "Please select a student")
                    }

                    academicYear.isBlank() -> {
                        showFeeToast(context, "Please select academic year")
                    }

                    feeCategory.isBlank() -> {
                        showFeeToast(context, "Please select fee category")
                    }

                    semester.isBlank() -> {
                        showFeeToast(context, "Please select semester")
                    }

                    applicableFee <= 0.0 -> {
                        showFeeToast(
                            context,
                            "Applicable fee is not configured for this selection"
                        )
                    }

                    paidAmount.isBlank() -> {
                        showFeeToast(context, "Please enter paid amount")
                    }

                    currentPaid <= 0.0 -> {
                        showFeeToast(
                            context,
                            "Paid amount must be greater than zero"
                        )
                    }

                    currentPaid > (applicableFee - previousPaid) -> {
                        showFeeToast(
                            context,
                            "Payment exceeds remaining fee by ${
                                formatMoney(
                                    currentPaid - (applicableFee - previousPaid)
                                )
                            }"
                        )
                    }

                    paymentDate.isBlank() -> {
                        showFeeToast(context, "Please select payment date")
                    }

                    paymentMode.isBlank() -> {
                        showFeeToast(context, "Please select payment mode")
                    }

                    transactionRequired && transactionId.isBlank() -> {
                        showFeeToast(
                            context,
                            "Please enter transaction/reference ID"
                        )
                    }

                    else -> {
                        val category = getStudentCategory(student)

                        savingPayment = true
                        onSavePayment(
                            FeePayment(
                                studentId = student.uniqueId.ifBlank {
                                    student.enrollmentNo
                                },
                                studentName = student.name.trim(),
                                enrollmentNo = student.enrollmentNo.trim(),
                                branch = BranchCatalog.canonicalBranch(student.branch),
                                category = category,
                                feeCategory = feeCategory.trim(),
                                academicYear = academicYear.trim(),
                                semester = semester.trim(),
                                totalFee = moneyValue(applicableFee),
                                paidAmount = moneyValue(currentPaid),
                                pendingAmount = moneyValue(calculatePending(
                                    applicableFee, previousPaid + currentPaid
                                )),
                                paymentDate = paymentDate.trim(),
                                paymentMode = paymentMode.trim(),
                                transactionId = transactionId.trim(),
                                remarks = remarks.trim()
                            )
                        ) { success ->
                            savingPayment = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
            enabled = selectedStudent != null && !savingPayment,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FeeBlue
            )
        ) {
            Text(
                text = if (savingPayment) "SAVING…" else "SAVE PAYMENT",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FeeBackButton(onBack)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// =====================================================
// PAYMENT RECORDS
// =====================================================

@Composable
fun PaymentRecordsScreen(
    payments: List<FeePayment>,
    feeStructures: List<FeeStructure>,
    loadError: String = "",
    onBack: () -> Unit
) {
    var searchText by remember {
        mutableStateOf("")
    }

    var selectedYear by remember {
        mutableStateOf("")
    }

    var selectedSemester by remember {
        mutableStateOf("")
    }

    var selectedFeeCategory by remember {
        mutableStateOf("")
    }

    var selectedBranch by remember {
        mutableStateOf("")
    }
    val context = LocalContext.current
    var queriedPayments by remember { mutableStateOf(payments) }
    var recordsLoading by remember { mutableStateOf(false) }
    var recordsError by remember { mutableStateOf("") }

    val recordBranchOptions = (payments.map { it.branch } + feeStructures.map { it.branch })
        .map { BranchCatalog.canonicalBranch(it) }.filter { it.isNotBlank() }.distinct().sorted()

    LaunchedEffect(selectedYear, selectedSemester, selectedFeeCategory, selectedBranch) {
        if (selectedYear.isBlank() && selectedSemester.isBlank() && selectedFeeCategory.isBlank() && selectedBranch.isBlank()) {
            queriedPayments = payments
            recordsError = ""
            recordsLoading = false
        } else {
            recordsLoading = true
            recordsError = ""
            try {
                queriedPayments = ApiClient.getPayments(
                    context = context,
                    academicYear = selectedYear,
                    semester = selectedSemester,
                    feeCategory = selectedFeeCategory,
                    branch = selectedBranch
                )
            } catch (e: Exception) {
                recordsError = e.message ?: "Unable to load filtered payment records."
                queriedPayments = payments.filter { payment ->
                    (selectedYear.isBlank() || sameAcademicYear(payment.academicYear, selectedYear)) &&
                        (selectedSemester.isBlank() || payment.semester.equals(selectedSemester, true)) &&
                        (selectedFeeCategory.isBlank() || payment.feeCategory.equals(selectedFeeCategory, true)) &&
                        (selectedBranch.isBlank() || BranchCatalog.canonicalBranch(payment.branch).equals(selectedBranch, true))
                }
            } finally {
                recordsLoading = false
            }
        }
    }

    val query = searchText.trim()

    val filteredPayments = queriedPayments
        .filter { payment ->
            val searchMatch =
                query.isBlank() ||
                        payment.studentName.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        payment.enrollmentNo.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        payment.studentId.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        payment.transactionId.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        payment.branch.contains(
                            query,
                            ignoreCase = true
                        )

            val yearMatch =
                selectedYear.isBlank() ||
                        sameAcademicYear(payment.academicYear, selectedYear)

            val semesterMatch =
                selectedSemester.isBlank() ||
                        payment.semester.equals(selectedSemester, ignoreCase = true) ||
                        (selectedSemester.filter { it.isDigit() }.isNotBlank() &&
                            payment.semester.filter { it.isDigit() } == selectedSemester.filter { it.isDigit() })

            val categoryMatch =
                selectedFeeCategory.isBlank() ||
                        payment.feeCategory.equals(selectedFeeCategory, true)

            val branchMatch =
                selectedBranch.isBlank() ||
                        BranchCatalog.canonicalBranch(payment.branch).equals(selectedBranch, true) ||
                        payment.branch.equals(selectedBranch, true)

            searchMatch &&
                    yearMatch &&
                    semesterMatch &&
                    categoryMatch &&
                    branchMatch
        }
        .sortedByDescending { payment ->
            parsePaymentDateForSort(payment.paymentDate)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(FeeBackground)
    ) {
        FeeProfessionalHeader(
            title = "Payment Records",
            subtitle = "View saved fee payment transactions",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeeTextField(
            value = searchText,
            label = "Search Student / Enrollment / ID / Transaction / Branch",
            enabled = true,
            onValueChange = {
                searchText = it
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeeDropdown(
            label = "Academic Year",
            value = selectedYear,
            options = listOf("") + (feeAcademicYears(feeStructures) + payments.map { it.academicYear })
                .filter { it.isNotBlank() }.distinct().sortedDescending(),
            enabled = true,
            onSelected = {
                if (selectedYear != it) {
                    selectedYear = it
                    selectedSemester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Semester",
            value = selectedSemester,
            options = listOf("") + (
                semesterOptionsFor(feeStructures, "", "") + payments.map { it.semester }
            ).filter { it.isNotBlank() }.distinct().sortedBy { it.substringAfterLast(' ').toIntOrNull() ?: Int.MAX_VALUE },
            enabled = true,
            onSelected = {
                selectedSemester = it
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Fee Category",
            value = selectedFeeCategory,
            options = listOf("") + (feeStructures.map { it.feeCategory } + payments.map { it.feeCategory })
                .filter { it.isNotBlank() }.distinct().sorted(),
            enabled = true,
            onSelected = { selectedFeeCategory = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Branch",
            value = selectedBranch,
            options = listOf("") + recordBranchOptions,
            enabled = true,
            onSelected = { selectedBranch = BranchCatalog.canonicalBranch(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (recordsError.isNotBlank()) {
            SearchHintCard("Showing cached payment records because refresh failed: $recordsError")
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (payments.any { it.academicYear.isBlank() || it.semester.isBlank() || it.feeCategory.isBlank() }) {
            SearchHintCard(
                "Some existing payment records have no saved academic year, semester or fee category. " +
                    "Those records can be filtered by branch, but cannot be reliably assigned to a semester/year/category without verified source data."
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Records: ${filteredPayments.size}",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = FeeText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (recordsLoading) {
            SearchHintCard("Loading filtered payment records…")
        } else if (filteredPayments.isEmpty()) {
            EmptyCard(
                when {
                    recordsError.isNotBlank() -> "Unable to load payment records: $recordsError"
                    payments.isEmpty() && loadError.isNotBlank() -> "Unable to load payment records: $loadError"
                    payments.isEmpty() -> "No payment records yet"
                    else -> "No matching payment records"
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                userScrollEnabled = true
            ) {
                itemsIndexed(filteredPayments) { _, payment ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PaymentRecordCard(payment)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        FeeBackButton(onBack)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// =====================================================
// PENDING FEES
// =====================================================

@Composable
fun PendingFeesScreen(
    students: List<Student>,
    payments: List<FeePayment>,
    feeStructures: List<FeeStructure>,
    onBack: () -> Unit
) {
    var selectedYear by remember {
        mutableStateOf("")
    }

    var selectedSemester by remember {
        mutableStateOf("")
    }

    var selectedFeeCategory by remember {
        mutableStateOf("")
    }
    var selectedBranch by remember { mutableStateOf("") }
    val context = LocalContext.current
    var queriedFeeStructures by remember { mutableStateOf(feeStructures) }
    var pendingLoadError by remember { mutableStateOf("") }
    var pendingLoading by remember { mutableStateOf(false) }
    val pendingBranchOptions = (students.map { it.branch } + feeStructures.map { it.branch })
        .map { BranchCatalog.canonicalBranch(it) }.filter { it.isNotBlank() }.distinct().sorted()

    LaunchedEffect(selectedYear, selectedFeeCategory, selectedSemester, selectedBranch) {
        pendingLoading = true
        pendingLoadError = ""
        queriedFeeStructures = try {
            ApiClient.getFeeStructures(
                context = context,
                academicYear = selectedYear,
                feeCategory = selectedFeeCategory,
                semester = selectedSemester,
                branch = selectedBranch
            )
        } catch (e: Exception) {
            pendingLoadError = e.message ?: "Unable to load fee structures"
            feeStructures.filter { record ->
                (selectedYear.isBlank() || sameAcademicYear(record.academicYear, selectedYear)) &&
                    (selectedFeeCategory.isBlank() || record.feeCategory.equals(selectedFeeCategory, true)) &&
                    (selectedSemester.isBlank() || record.semester.equals(selectedSemester, true)) &&
                    (selectedBranch.isBlank() || BranchCatalog.canonicalBranch(record.branch).equals(selectedBranch, true))
            }
        }
        pendingLoading = false
    }

    val pendingStudents = remember(
        students, payments, queriedFeeStructures, selectedYear, selectedFeeCategory, selectedSemester, selectedBranch
    ) {
        val filteredStructuresByBranch = queriedFeeStructures
            .filter { record ->
                (selectedYear.isBlank() || sameAcademicYear(record.academicYear, selectedYear)) &&
                    (selectedFeeCategory.isBlank() || record.feeCategory.equals(selectedFeeCategory, ignoreCase = true)) &&
                    (selectedSemester.isBlank() || record.semester.equals(selectedSemester, ignoreCase = true)) &&
                    (selectedBranch.isBlank() || BranchCatalog.canonicalBranch(record.branch).equals(selectedBranch, true))
            }
            .groupBy { BranchCatalog.canonicalBranch(it.branch) }
        val paymentsByEnrollment = payments.groupBy { it.enrollmentNo.trim().lowercase(Locale.ROOT) }

        students.mapNotNull { student ->
            val branch = BranchCatalog.canonicalBranch(student.branch)
            val branchRecords = filteredStructuresByBranch[branch].orEmpty()
            val generalRecords = filteredStructuresByBranch[""].orEmpty()
            val applicableRecords = if (branchRecords.isNotEmpty()) branchRecords else generalRecords
            // When filters are broad, emit a separate pending row for each fee context rather than
            // combining semesters/categories/years into one misleading total.
            val feeContexts = applicableRecords.groupBy { Triple(it.academicYear, it.feeCategory, it.semester) }
            val matchingPayments = (paymentsByEnrollment[student.enrollmentNo.trim().lowercase(Locale.ROOT)].orEmpty() +
                (if (student.uniqueId.isNotBlank()) paymentsByEnrollment[student.uniqueId.trim().lowercase(Locale.ROOT)].orEmpty() else emptyList()))
                .distinctBy { it.transactionId.ifBlank { "${it.paymentDate}_${it.paidAmount}" } }
                .filter { payment ->
                    (payment.branch.isBlank() || BranchCatalog.canonicalBranch(payment.branch).equals(branch, ignoreCase = true)) &&
                        (selectedYear.isBlank() || sameAcademicYear(payment.academicYear, selectedYear)) &&
                        (selectedFeeCategory.isBlank() || payment.feeCategory.isBlank() || payment.feeCategory.equals(selectedFeeCategory, ignoreCase = true)) &&
                        (selectedSemester.isBlank() || payment.semester.isBlank() || payment.semester.filter { it.isDigit() } == selectedSemester.filter { it.isDigit() } || payment.semester.equals(selectedSemester, ignoreCase = true))
                }
                        (selectedFeeCategory.isBlank() || payment.feeCategory.equals(selectedFeeCategory, ignoreCase = true)) &&
                        (selectedSemester.isBlank() || payment.semester.equals(selectedSemester, ignoreCase = true))
                }
            feeContexts.values.mapNotNull { contextRecords ->
                val applicableFee = contextRecords.firstOrNull()?.feeAmount?.toDoubleOrNull() ?: 0.0
                if (applicableFee <= 0.0) {
                    null
                } else {
                    val contextYear = contextRecords.first().academicYear
                    val contextCategory = contextRecords.first().feeCategory
                    val contextSemester = contextRecords.first().semester
                    val paid = matchingPayments.filter { payment ->
                        sameAcademicYear(payment.academicYear, contextYear) &&
                            payment.feeCategory.equals(contextCategory, true) &&
                            payment.semester.equals(contextSemester, true)
                    }.sumOf { it.paidAmount.toDoubleOrNull() ?: 0.0 }
                    val pending = calculatePending(applicableFee, paid)
                    if (pending > 0.0) PendingStudentData(student, contextYear, contextCategory, contextSemester, applicableFee, paid, pending) else null
                }
            }
        }.flatten()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(FeeBackground)
    ) {
        FeeProfessionalHeader(
            title = "Pending Fees",
            subtitle = "Students with outstanding fee amounts",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(20.dp))

        FeeDropdown(
            label = "Academic Year",
            value = selectedYear,
            options = listOf("") + feeAcademicYears(feeStructures),
            enabled = true,
            onSelected = {
                if (selectedYear != it) {
                    selectedYear = it
                    selectedFeeCategory = ""
                    selectedSemester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Fee Category",
            value = selectedFeeCategory,
            options = listOf("") + feeStructures.filter { record ->
                (selectedYear.isBlank() || sameAcademicYear(record.academicYear, selectedYear)) &&
                    (selectedBranch.isBlank() || BranchCatalog.canonicalBranch(record.branch).equals(selectedBranch, true))
            }.map { it.feeCategory }.filter { it.isNotBlank() }.distinct().sorted(),
            enabled = true,
            onSelected = {
                if (selectedFeeCategory != it) {
                    selectedFeeCategory = it
                    selectedSemester = ""
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Semester",
            value = selectedSemester,
            options = listOf("") + feeStructures.filter { record ->
                (selectedYear.isBlank() || sameAcademicYear(record.academicYear, selectedYear)) &&
                    (selectedFeeCategory.isBlank() || record.feeCategory.equals(selectedFeeCategory, true)) &&
                    (selectedBranch.isBlank() || BranchCatalog.canonicalBranch(record.branch).equals(selectedBranch, true))
            }.map { it.semester }.filter { it.isNotBlank() }.distinct().sortedBy {
                it.substringAfterLast(' ').toIntOrNull() ?: Int.MAX_VALUE
            },
            enabled = true,
            onSelected = {
                selectedSemester = it
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeeDropdown(
            label = "Branch",
            value = selectedBranch,
            options = listOf("") + pendingBranchOptions,
            enabled = true,
            onSelected = { selectedBranch = BranchCatalog.canonicalBranch(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (pendingLoading) {
            SearchHintCard("Loading pending fee data…")
        } else if (pendingLoadError.isNotBlank() && queriedFeeStructures.isEmpty()) {
            EmptyCard("Unable to load pending fee data: $pendingLoadError")
        } else if (students.isEmpty()) {
            EmptyCard("No students available")
        } else if (pendingStudents.isEmpty()) {
            EmptyCard(
                if (pendingLoadError.isNotBlank()) "Showing available cached data; refresh failed: $pendingLoadError" else "No pending fees for the selected year, category, semester and branch"
            )
        } else {
            if (pendingLoadError.isNotBlank()) {
                SearchHintCard("Showing cached fee data because refresh failed: $pendingLoadError")
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "Pending Students: ${pendingStudents.size}",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
            ) {
                itemsIndexed(pendingStudents) { _, item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PendingStudentCard(
                            student = item.student,
                            applicableFee = item.applicableFee,
                            paidAmount = item.paidAmount,
                            pendingAmount = item.pendingAmount,
                            academicYear = item.academicYear,
                            feeCategory = item.feeCategory,
                            semester = item.semester
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        FeeBackButton(onBack)

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private data class PendingStudentData(
    val student: Student,
    val academicYear: String,
    val feeCategory: String,
    val semester: String,
    val applicableFee: Double,
    val paidAmount: Double,
    val pendingAmount: Double
)

// =====================================================
// STUDENT SEARCH
// =====================================================

@Composable
fun StudentSearchSelector(
    students: List<Student>,
    selectedStudent: Student?,
    onStudentSelected: (Student?) -> Unit
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var filteredStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf("") }
    val query = searchText.trim()

    LaunchedEffect(query) {
        if (query.isBlank()) {
            filteredStudents = emptyList()
            searchError = ""
            searchLoading = false
        } else {
            searchLoading = true
            searchError = ""
            delay(250)
            try {
                val result = ApiClient.searchStudents(
                    context = context,
                    query = query,
                    limit = 100,
                    offset = 0,
                    sort = "name"
                )
                filteredStudents = result.students
            } catch (e: Exception) {
                // Keep the local cache as a network-failure fallback, but never treat
                // it as the authoritative complete student list.
                filteredStudents = students.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.enrollmentNo.contains(query, ignoreCase = true) ||
                        it.uniqueId.contains(query, ignoreCase = true) ||
                        it.classRollNo.contains(query, ignoreCase = true) ||
                        it.rollNo.contains(query, ignoreCase = true)
                }.take(20)
                searchError = e.message ?: "Unable to search students on the server."
            } finally {
                searchLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Student",
            fontSize = 13.sp,
            color = FeeGray,
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 5.dp
            )
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it

                if (selectedStudent != null) {
                    onStudentSelected(null)
                }
            },
            label = {
                Text("Search by name / enrollment / ID")
            },
            placeholder = {
                Text("Type at least part of the student's details")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (selectedStudent != null) {
            Spacer(modifier = Modifier.height(8.dp))

            SelectedStudentCard(
                student = selectedStudent,
                onChangeStudent = {
                    onStudentSelected(null)
                    searchText = ""
                }
            )
        } else if (query.isBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchHintCard(
                "Search by name, enrollment number, unique ID, class roll or roll number."
            )
        } else if (searchLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            SearchHintCard("Searching all students…")
        } else if (filteredStudents.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            EmptyCard(if (searchError.isNotBlank()) searchError else "No matching student found")
        } else {
            Spacer(modifier = Modifier.height(8.dp))

            filteredStudents.forEach { student ->
                StudentSearchResultCard(
                    student = student,
                    onClick = {
                        onStudentSelected(student)
                        searchText = ""
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SelectedStudentCard(
    student: Student,
    onChangeStudent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = FeeLightBlue
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = "Selected Student",
                fontSize = 12.sp,
                color = FeeGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = student.name.ifBlank { "Student" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )

            Text(
                text = "${student.enrollmentNo.ifBlank { "Enrollment N/A" }} • ${
                    student.uniqueId.ifBlank { "ID N/A" }
                }",
                fontSize = 13.sp,
                color = FeeGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onChangeStudent,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Change Student")
            }
        }
    }
}

@Composable
private fun StudentSearchResultCard(
    student: Student,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = student.name.ifBlank { "Student" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enrollment: ${
                    student.enrollmentNo.ifBlank { "N/A" }
                }",
                fontSize = 13.sp,
                color = FeeGray
            )

            Text(
                text = "ID: ${
                    student.uniqueId.ifBlank { "N/A" }
                } • Roll No: ${
                    student.classRollNo.ifBlank { "N/A" }
                }",
                fontSize = 13.sp,
                color = FeeGray
            )

            Text(
                text = "Branch: ${
                    BranchCatalog.canonicalBranch(student.branch)
                        .ifBlank { "N/A" }
                }",
                fontSize = 13.sp,
                color = FeeGray
            )
        }
    }
}

@Composable
private fun SearchHintCard(
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = FeeGray,
            modifier = Modifier.padding(14.dp)
        )
    }
}

// =====================================================
// STUDENT BASIC CARD
// =====================================================

@Composable
fun StudentBasicCard(
    student: Student
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = student.name.ifBlank { "Student" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoRow("Unique ID", student.uniqueId)
            InfoRow("Enrollment No", student.enrollmentNo)
            InfoRow("Class Roll No", student.classRollNo)
            InfoRow(
                "Branch",
                BranchCatalog.canonicalBranch(student.branch)
            )
            InfoRow("Category", getStudentCategory(student))
        }
    }
}

// =====================================================
// SUMMARY
// =====================================================

@Composable
fun FeeSummaryCard(
    applicableFee: Double,
    paidAmount: Double,
    pendingAmount: Double
) {
    val status = when {
        applicableFee <= 0.0 -> "NOT CONFIGURED"
        pendingAmount <= 0.0 && paidAmount > 0.0 -> "PAID"
        paidAmount > 0.0 -> "PARTIALLY PAID"
        else -> "PENDING"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Fee Summary",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )

            Spacer(modifier = Modifier.height(14.dp))

            InfoRow("Applicable Fee", formatMoney(applicableFee))
            InfoRow("Amount Paid", formatMoney(paidAmount))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp)
            )

            InfoRow("Pending Amount", formatMoney(pendingAmount))

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Payment Status: $status",
                fontWeight = FontWeight.Bold,
                color = FeeBlue
            )
        }
    }
}

// =====================================================
// PAYMENT CARDS
// =====================================================

@Composable
fun PaymentRecordCard(
    payment: FeePayment
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = payment.studentName.ifBlank { "Student" },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )

            Text(
                text = payment.enrollmentNo.ifBlank { "Enrollment not available" },
                fontSize = 13.sp,
                color = FeeGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("Unique ID", payment.studentId)
            InfoRow("Branch", payment.branch)
            InfoRow("Student Category", payment.category)
            InfoRow("Fee Category", payment.feeCategory)
            InfoRow("Academic Year", payment.academicYear)
            InfoRow("Semester", payment.semester)
            InfoRow(
                "Applicable Fee",
                formatMoney(payment.totalFee.toDoubleOrNull() ?: 0.0)
            )
            InfoRow(
                "Paid Amount",
                formatMoney(payment.paidAmount.toDoubleOrNull() ?: 0.0)
            )
            InfoRow(
                "Pending After Payment",
                formatMoney(payment.pendingAmount.toDoubleOrNull() ?: 0.0)
            )
            InfoRow("Payment Mode", payment.paymentMode)
            if (payment.paymentStatus.isNotBlank()) InfoRow("Payment Status", payment.paymentStatus)
            InfoRow("Payment Date", payment.paymentDate)

            if (payment.transactionId.isNotBlank()) {
                InfoRow("Transaction / Reference ID", payment.transactionId)
            }

            if (payment.remarks.isNotBlank()) {
                InfoRow("Remarks", payment.remarks)
            }
        }
    }
}

@Composable
fun PendingStudentCard(
    student: Student,
    applicableFee: Double,
    paidAmount: Double,
    pendingAmount: Double,
    academicYear: String = "",
    feeCategory: String = "",
    semester: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = student.name.ifBlank { "Student" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )

            Text(
                text = student.enrollmentNo.ifBlank { "Enrollment not available" },
                fontSize = 13.sp,
                color = FeeGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoRow(
                "Branch",
                BranchCatalog.canonicalBranch(student.branch)
            )
            InfoRow("Category", getStudentCategory(student))
            if (academicYear.isNotBlank()) InfoRow("Academic Year", academicYear)
            if (feeCategory.isNotBlank()) InfoRow("Fee Category", feeCategory)
            if (semester.isNotBlank()) InfoRow("Semester", semester)
            InfoRow("Applicable Fee", formatMoney(applicableFee))
            InfoRow("Paid", formatMoney(paidAmount))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp)
            )

            InfoRow("Status", if (paidAmount <= 0.0) "UNPAID" else "PARTIAL")
            InfoRow("Pending", formatMoney(pendingAmount))
        }
    }
}

@Composable
fun PaymentSmallCard(
    payment: FeePayment
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            if (payment.academicYear.isNotBlank()) InfoRow("Academic Year", payment.academicYear)
            if (payment.feeCategory.isNotBlank()) InfoRow("Fee Category", payment.feeCategory)
            if (payment.semester.isNotBlank()) InfoRow("Semester", payment.semester)
            InfoRow(
                "Paid",
                formatMoney(
                    payment.paidAmount.toDoubleOrNull() ?: 0.0
                )
            )
            InfoRow("Payment Mode", payment.paymentMode)
            InfoRow(
                "Date",
                payment.paymentDate.ifBlank { "Not entered" }
            )
            InfoRow(
                "Transaction ID",
                payment.transactionId.ifBlank { "Not entered" }
            )
        }
    }
}

// =====================================================
// DATE PICKER
// =====================================================

@Composable
fun DatePickerField(
    value: String,
    label: String,
    enabled: Boolean = true,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            if (!enabled) return@OutlinedButton

            val calendar = Calendar.getInstance()

            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(
                            year,
                            month,
                            dayOfMonth
                        )
                    }

                    val formatter = SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    )

                    onDateSelected(
                        formatter.format(selectedCalendar.time)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = FeeGray
                )

                Text(
                    text = value.ifBlank {
                        "Select payment date"
                    },
                    fontSize = 15.sp,
                    color = if (value.isBlank()) {
                        FeeGray
                    } else {
                        FeeText
                    },
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Text(
                text = "📅",
                fontSize = 18.sp
            )
        }
    }
}

// =====================================================
// DROPDOWN
// =====================================================

@Composable
fun FeeDropdown(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val cleanOptions = options
        .map { it.trim() }
        .distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = FeeGray,
            modifier = Modifier.padding(
                start = 4.dp,
                bottom = 5.dp
            )
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    if (enabled && cleanOptions.isNotEmpty()) {
                        expanded = true
                    }
                },
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
                        text = when {
                            value.isNotBlank() -> value
                            cleanOptions.isEmpty() -> "No options available"
                            else -> "Select $label"
                        },
                        color = if (
                            value.isBlank() ||
                            cleanOptions.isEmpty()
                        ) {
                            FeeGray
                        } else {
                            FeeText
                        }
                    )

                    Text(
                        text = "▼",
                        fontSize = 11.sp,
                        color = FeeBlue
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                cleanOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (option.isBlank()) {
                                    "All"
                                } else {
                                    option
                                }
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (
            enabled &&
            cleanOptions.isEmpty()
        ) {
            Text(
                text = "No configured options available",
                fontSize = 12.sp,
                color = FeeGray,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 5.dp
                )
            )
        }
    }
}

// =====================================================
// TEXT FIELD
// =====================================================

@Composable
fun FeeTextField(
    value: String,
    label: String,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = {
            Text(label)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions
    )
}

// =====================================================
// COMMON UI
// =====================================================

@Composable
fun FeeBackButton(
    onBack: () -> Unit
) {
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("BACK TO FEE & PAYMENT")
    }
}

@Composable
fun SectionTitle(
    text: String
) {
    Text(
        text = text,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = FeeText,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = FeeGray,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value.ifBlank { "N/A" },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun EmptyCard(
    text: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = FeeText
            )
        }
    }
}

private fun feeAcademicYears(feeStructures: List<FeeStructure>): List<String> =
    feeStructures.map { it.academicYear.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedDescending()

// =====================================================
// SEMESTER OPTIONS
// =====================================================

private fun semesterOptionsFor(
    feeStructures: List<FeeStructure>,
    academicYear: String,
    feeCategory: String,
    student: Student? = null
): List<String> {
    val studentBranch = student?.let {
        BranchCatalog.canonicalBranch(it.branch)
    }.orEmpty()

    val configured = feeStructures
        .filter { record ->
            (academicYear.isBlank() || sameAcademicYear(record.academicYear, academicYear)) &&
                    (feeCategory.isBlank() || record.feeCategory.trim().equals(feeCategory.trim(), ignoreCase = true))
        }
        .filter { record ->
            studentBranch.isBlank() ||
                    BranchCatalog.canonicalBranch(record.branch).equals(
                        studentBranch,
                        ignoreCase = true
                    )
        }
        .map { it.semester.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    val available = if (configured.isNotEmpty()) configured else FeeCatalog.semesters
    return available.sortedBy { semester ->
        semester.substringAfterLast(' ').toIntOrNull() ?: Int.MAX_VALUE
    }
}

// =====================================================
// BUSINESS LOGIC
// =====================================================

fun getStudentCategory(
    student: Student
): String {
    val allowedCategories = AdmissionCatalog.categories

    return listOf(
        student.allocatedCategory,
        student.eligibleCategory,
        student.offlineAdmissionCategory
    )
        .map { it.trim() }
        .firstOrNull { category ->
            category.isNotBlank() &&
                    !category.equals("EWS", ignoreCase = true) &&
                    category in allowedCategories
        }
        ?: "Other"
}

private fun feeCategoriesFor(
    feeStructures: List<FeeStructure>,
    academicYear: String,
    student: Student? = null
): List<String> {
    val studentBranch = student?.let {
        BranchCatalog.canonicalBranch(it.branch)
    }.orEmpty()

    val branchMatched = feeStructures
        .filter { record ->
            (academicYear.isBlank() || sameAcademicYear(record.academicYear, academicYear))
        }
        .filter { record ->
            studentBranch.isBlank() ||
                    BranchCatalog.canonicalBranch(record.branch).equals(
                        studentBranch,
                        ignoreCase = true
                    )
        }
        .map { it.feeCategory.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    // If the selected student's branch has no configured fee record yet,
    // still allow the category to be selected. The final fee calculation
    // will correctly show 0 until that branch/category configuration exists.
    return if (branchMatched.isNotEmpty()) {
        branchMatched
    } else {
        FeeDataStore.feeCategories.toList()
    }
}

/** Compares academic-year labels without rewriting persisted database values.
 * Accepts equivalent labels such as 2025-26 and 2025-2026.
 */
private fun sameAcademicYear(first: String, second: String): Boolean {
    val a = first.trim()
    val b = second.trim()
    if (a.isBlank() || b.isBlank()) return a.equals(b, ignoreCase = true)
    fun key(value: String): String {
        val match = Regex("^(\\d{4})[-/](?:\\d{2}|\\d{4})$").matchEntire(value)
        return match?.groupValues?.get(1) ?: value.lowercase(Locale.ROOT)
    }
    return key(a) == key(b)
}

fun getApplicableFee(
    feeStructures: List<FeeStructure>,
    student: Student?,
    academicYear: String,
    feeCategory: String,
    semester: String
): Double {
    if (
        student == null ||
        academicYear.isBlank() ||
        feeCategory.isBlank() ||
        semester.isBlank()
    ) {
        return 0.0
    }

    val studentBranch = BranchCatalog.canonicalBranch(student.branch)

    return feeStructures
        .firstOrNull { record ->
            sameAcademicYear(record.academicYear, academicYear) &&
                    BranchCatalog.canonicalBranch(record.branch).equals(
                        studentBranch,
                        ignoreCase = true
                    ) &&
                    record.feeCategory.trim().equals(
                        feeCategory.trim(),
                        ignoreCase = true
                    ) &&
                    record.semester.trim().equals(
                        semester.trim(),
                        ignoreCase = true
                    )
        }
        ?.feeAmount
        ?.toDoubleOrNull()
        ?.coerceAtLeast(0.0)
        ?: 0.0
}

private fun paymentMatches(
    payment: FeePayment,
    student: Student?,
    academicYear: String,
    feeCategory: String,
    semester: String
): Boolean {
    if (student == null) return false

    // Enrollment number is the stable business identifier exposed by the payment API.
    // Do not compare it with the numeric database student_id.
    val paymentMatchesStudent = payment.enrollmentNo.isNotBlank() &&
            payment.enrollmentNo.equals(student.enrollmentNo, ignoreCase = true)

    return paymentMatchesStudent &&
            sameAcademicYear(payment.academicYear, academicYear) &&
            payment.feeCategory.equals(
                feeCategory,
                ignoreCase = true
            ) &&
            payment.semester.equals(
                semester,
                ignoreCase = true
            ) &&
            BranchCatalog.canonicalBranch(payment.branch).equals(
                BranchCatalog.canonicalBranch(student.branch),
                ignoreCase = true
            )
}

private fun calculatePending(
    applicableFee: Double,
    paidAmount: Double
): Double {
    return (
            applicableFee - paidAmount
            ).coerceAtLeast(0.0)
}

private fun moneyValue(
    amount: Double
): String {
    return String.format(
        Locale.US,
        "%.2f",
        amount.coerceAtLeast(0.0)
    )
}

private fun parsePaymentDateForSort(
    value: String
): Long {
    if (value.isBlank()) return 0L

    return try {
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).parse(value)?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
}

fun formatMoney(
    amount: Double
): String {
    return "₹ %.2f".format(
        Locale.getDefault(),
        amount.coerceAtLeast(0.0)
    )
}

private fun showFeeToast(
    context: Context,
    message: String
) {
    Toast.makeText(
        context,
        message,
        Toast.LENGTH_SHORT
    ).show()
}
