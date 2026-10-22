
package com.example.studentmanagementapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


// =========================================================
// COLORS
// =========================================================

private val AdmissionBlue2 = Color(0xFF1565C0)
private val AdmissionBackground2 = Color(0xFFF5F7FB)
private val AdmissionLightBlue2 = Color(0xFFE8EEF9)
private val AdmissionText2 = Color(0xFF172033)


// =========================================================
// CENTRALIZED OPTIONS
// =========================================================

private val admissionCategoryOptions2: List<String>
    get() = AdmissionCatalog.categories

private val allocatedCategoryOptions2: List<String>
    get() = AdmissionCatalog.allocatedCategories

private fun normalizedAcademicYear2(value: String): String {
    val text = value.trim()
    val match = Regex("^(\\d{4})\\s*[-/]\\s*(\\d{2,4})$").matchEntire(text) ?: return ""
    val start = match.groupValues[1].toIntOrNull() ?: return ""
    val rawEnd = match.groupValues[2]
    val end = if (rawEnd.length == 2) rawEnd else rawEnd.takeLast(2)
    return "$start-$end"
}


private val admissionStatusOptions2 = AdmissionCatalog.admissionStatuses

private fun admissionYearOptions2(): List<String> =
    AdmissionCatalog.admissionYears

private fun admissionRoundOptions2(): List<String> =
    AdmissionCatalog.admissionRounds

private fun admissionBranchesForYear2(year: String): List<String> {
    if (year.isBlank()) return emptyList()

    return BranchCatalog
        .optionsForAcademicYear(year)
        .map { BranchCatalog.canonicalBranch(it) }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun academicYearForAdmissionYear2(admissionYear: String): String {
    val value = admissionYear.trim()
    return if (value.matches(Regex("^\\d{4}$"))) {
        AdmissionCatalog.academicYearForAdmissionYear(value)
    } else {
        normalizedAcademicYear2(value)
    }
}

private fun admissionYearForAcademicYear2(academicYear: String): String =
    AdmissionCatalog.admissionYearForAcademicYear(normalizedAcademicYear2(academicYear))

private fun roundDisplay2(value: String): String =
    AdmissionCatalog.normalizeAdmissionRound(value).ifBlank { "Not Specified" }


// =========================================================
// VALIDATION HELPERS
// =========================================================

private fun isValidAdmissionStatus2(status: String): Boolean {
    return admissionStatusOptions2.any { it.equals(status.trim(), ignoreCase = true) }
}

private fun isValidAdmissionYear2(year: String): Boolean {
    return year.trim() in AdmissionCatalog.admissionYears
}

private fun isValidAdmissionBranch2(
    branch: String,
    admissionYear: String
): Boolean {
    val canonical = BranchCatalog.canonicalBranch(branch)
    return canonical.isNotBlank() && canonical in admissionBranchesForYear2(admissionYear)
}

// =========================================================
// MAIN ADMISSION SCREEN
// =========================================================

@Composable
fun AdmissionScreen(
    students: SnapshotStateList<Student>,
    onBack: () -> Unit
) {

    var currentAdmissionScreen by remember {
        mutableStateOf("menu")
    }
    val context = LocalContext.current

    LaunchedEffect(context) {
        try {
            val categories = ApiClient.getAllotedCategories(context)
            AdmissionCatalog.setAllocatedCategories(categories)
        } catch (_: Exception) {
            // Keep already-loaded values; do not invent fallback categories.
        }
        try {
            val years = ApiClient.getAcademicYears()
            if (years.isNotEmpty()) AdmissionCatalog.setAcademicYears(years)
        } catch (_: Exception) {
            // Preserve current options if the backend is temporarily unavailable.
        }
        try {
            val allStudents = ApiClient.getAllStudents(context)
            if (allStudents.isNotEmpty() || students.isEmpty()) {
                students.clear()
                students.addAll(allStudents)
            }
            if (AdmissionCatalog.allocatedCategories.isEmpty()) {
                AdmissionCatalog.setAllocatedCategories(allStudents.map { it.allocatedCategory })
            }
        } catch (_: Exception) {
            // Keep the currently available in-memory/cache records on network failure.
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AdmissionBackground2
    ) {

        when (currentAdmissionScreen) {

            "menu" -> {
                AdmissionMenu2(
                    onProcessing = {
                        currentAdmissionScreen = "processing"
                    },
                    onRecords = {
                        currentAdmissionScreen = "records"
                    },
                    onSummary = {
                        currentAdmissionScreen = "summary"
                    },
                    onBack = onBack
                )
            }

            "processing" -> {
                AdmissionProcessingScreen2(
                    students = students,
                    onBack = {
                        currentAdmissionScreen = "menu"
                    }
                )
            }

            "records" -> {
                AdmissionRecordsScreen2(
                    students = students,
                    onBack = {
                        currentAdmissionScreen = "menu"
                    }
                )
            }

            "summary" -> {
                AdmissionSummaryScreen2(
                    students = students,
                    onBack = {
                        currentAdmissionScreen = "menu"
                    }
                )
            }
        }
    }
}


// =========================================================
// HEADER
// =========================================================

@Composable
fun AdmissionHeader2(
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
            containerColor = AdmissionBlue2
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 22.dp
            )
        ) {

            Box(
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onBack()
                    }
                    .padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            ) {

                Text(
                    text = "←  Back",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }
    }
}


// =========================================================
// MENU
// =========================================================

@Composable
fun AdmissionMenu2(
    onProcessing: () -> Unit,
    onRecords: () -> Unit,
    onSummary: () -> Unit,
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        AdmissionHeader2(
            title = "Admissions",
            subtitle = "Manage and monitor student admissions",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            AdmissionMenuCard2(
                number = "01",
                title = "Admission Processing",
                description = "Select a student and enter or update admission details",
                onClick = onProcessing
            )

            Spacer(modifier = Modifier.height(14.dp))

            AdmissionMenuCard2(
                number = "02",
                title = "Admission Records",
                description = "View, search and filter individual admission records",
                onClick = onRecords
            )

            Spacer(modifier = Modifier.height(14.dp))

            AdmissionMenuCard2(
                number = "03",
                title = "Admission Summary",
                description = "View branch-wise, category-wise and round-wise admission analysis",
                onClick = onSummary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "BACK TO DASHBOARD",
                    color = AdmissionBlue2,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// =========================================================
// MENU CARD
// =========================================================

@Composable
fun AdmissionMenuCard2(
    number: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
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
                    .height(48.dp)
                    .fillMaxWidth(0.13f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AdmissionLightBlue2
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = number,
                        color = AdmissionBlue2,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdmissionText2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = AdmissionBlue2
            )
        }
    }
}


// =========================================================
// ADMISSION PROCESSING
// =========================================================

@Composable
fun AdmissionProcessingScreen2(
    students: SnapshotStateList<Student>,
    onBack: () -> Unit
) {

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var admissionYear by remember {
        mutableStateOf("")
    }

    var branch by remember {
        mutableStateOf("")
    }

    var eligibleCategory by remember {
        mutableStateOf("")
    }

    var allocatedCategory by remember {
        mutableStateOf("")
    }

    var offlineCategory by remember {
        mutableStateOf("")
    }

    var admissionRound by remember {
        mutableStateOf("")
    }

    var admissionStatus by remember {
        mutableStateOf("")
    }

    var remarks by remember {
        mutableStateOf("")
    }

    var savedMessage by remember {
        mutableStateOf(false)
    }

    var validationMessage by remember {
        mutableStateOf("")
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val searchResults = students
        .asSequence()
        .filter { student ->
            val query = searchText.trim()

            query.isNotBlank() &&
                    (
                            student.name.contains(query, true) ||
                                    student.enrollmentNo.contains(query, true) ||
                                    student.uniqueId.contains(query, true) ||
                                    student.classRollNo.contains(query, true) ||
                                    student.rollNo.contains(query, true)
                            )
        }
        .take(8)
        .toList()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AdmissionHeader2(
            title = "Admission Processing",
            subtitle = "Enter or update admission details",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Text(
                text = "Select Student",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AdmissionText2
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = if (selectedStudent != null) {
                    selectedStudent!!.name
                } else {
                    searchText
                },
                onValueChange = {

                    selectedStudent = null
                    searchText = it
                    savedMessage = false
                    validationMessage = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Search Name / Enrollment / Unique ID / Roll No")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (searchText.isNotBlank() && selectedStudent == null) {

                if (searchResults.isEmpty()) {

                    AdmissionEmptyCard2(
                        text = "No student found"
                    )

                } else {

                    searchResults.forEach { student ->

                        AdmissionStudentSearchCard2(
                            student = student,
                            onClick = {

                                selectedStudent = student
                                searchText = student.name

                                admissionYear =
                                    admissionYearForAcademicYear2(student.admissionYear)

                                branch =
                                    BranchCatalog.canonicalBranch(student.branch)

                                eligibleCategory =
                                    student.eligibleCategory.trim().let {
                                        if (it in admissionCategoryOptions2) it else ""
                                    }

                                allocatedCategory =
                                    student.allocatedCategory.trim().let {
                                        if (it in allocatedCategoryOptions2) it else ""
                                    }

                                offlineCategory =
                                    student.offlineAdmissionCategory.trim().let {
                                        if (it in admissionCategoryOptions2) it else ""
                                    }

                                admissionRound =
                                    student.round.trim()

                                admissionStatus =
                                    student.status.trim().let {
                                        when {
                                            it.equals("Admitted", ignoreCase = true) -> "Admitted"
                                            it.equals("Cancelled", ignoreCase = true) -> "Cancelled"
                                            else -> ""
                                        }
                                    }

                                remarks =
                                    student.remarks

                                savedMessage = false
                                validationMessage = ""
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (students.isEmpty()) {

                AdmissionEmptyCard2(
                    text = "No students available. Add students from Student Records first."
                )
            }

            selectedStudent?.let { student ->

                Spacer(modifier = Modifier.height(12.dp))

                AdmissionStudentCard2(student)

                Spacer(modifier = Modifier.height(18.dp))

                AdmissionSectionHeader2(
                    title = "Admission Details"
                )

                AdmissionDropdown2(
                    label = "Admission Year",
                    value = admissionYear,
                    options = admissionYearOptions2(),
                    onSelected = {
                        admissionYear = it
                        val validBranches = admissionBranchesForYear2(it)
                        if (BranchCatalog.canonicalBranch(branch) !in validBranches) {
                            branch = ""
                        }
                        validationMessage = ""
                    }
                )

                if (admissionYear.isNotBlank()) {
                    AdmissionInfoRow2(
                        "Academic Year",
                        academicYearForAdmissionYear2(admissionYear)
                    )
                }

                AdmissionDropdown2(
                    label = "Branch",
                    value = branch,
                    options = admissionBranchesForYear2(admissionYear),
                    onSelected = {
                        branch = BranchCatalog.canonicalBranch(it)
                        validationMessage = ""
                    }
                )

                AdmissionSectionHeader2(
                    title = "Category"
                )

                AdmissionDropdown2(
                    label = "Eligible Category",
                    value = eligibleCategory,
                    options = admissionCategoryOptions2,
                    onSelected = {
                        eligibleCategory = it
                    }
                )

                AdmissionDropdown2(
                    label = "Allocated Category",
                    value = allocatedCategory,
                    options = allocatedCategoryOptions2,
                    onSelected = {
                        allocatedCategory = it
                        validationMessage = ""
                    }
                )

                AdmissionDropdown2(
                    label = "Offline Admission Category",
                    value = offlineCategory,
                    options = admissionCategoryOptions2,
                    onSelected = {
                        offlineCategory = it
                    }
                )

                AdmissionSectionHeader2(
                    title = "Admission Processing"
                )

                AdmissionDropdown2(
                    label = "Admission Round",
                    value = roundDisplay2(admissionRound),
                    options = listOf("Not Specified") + admissionRoundOptions2(),
                    onSelected = {
                        admissionRound = if (it == "Not Specified") "" else it
                        validationMessage = ""
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Status",
                    value = admissionStatus,
                    options = admissionStatusOptions2,
                    onSelected = {
                        admissionStatus = it
                        validationMessage = ""
                    }
                )

                AdmissionField2(
                    label = "Remarks",
                    value = remarks,
                    onValueChange = {
                        remarks = it
                    },
                    singleLine = false
                )

                if (validationMessage.isNotBlank()) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = validationMessage,
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {

                        when {

                            selectedStudent == null -> {

                                validationMessage =
                                    "Please select a student first."

                                savedMessage = false
                            }

                            !isValidAdmissionYear2(admissionYear) -> {

                                validationMessage =
                                    "Please select a valid Admission Year."

                                savedMessage = false
                            }

                            !isValidAdmissionBranch2(branch, admissionYear) -> {

                                validationMessage =
                                    "Please select a valid Branch."

                                savedMessage = false
                            }

                            eligibleCategory !in admissionCategoryOptions2 -> {

                                validationMessage =
                                    "Please select a valid Eligible Category."

                                savedMessage = false
                            }

                            allocatedCategory.isBlank() || allocatedCategory !in allocatedCategoryOptions2 -> {

                                validationMessage =
                                    "Please select a valid Allocated Category."

                                savedMessage = false
                            }

                            admissionRound.isNotBlank() && admissionRoundOptions2().none { AdmissionCatalog.normalizeAdmissionRound(it).equals(AdmissionCatalog.normalizeAdmissionRound(admissionRound), ignoreCase = true) } -> {

                                validationMessage =
                                    "Please select a valid Admission Round."

                                savedMessage = false
                            }

                            !isValidAdmissionStatus2(admissionStatus) -> {

                                validationMessage =
                                    "Please select Admission Status: Admitted or Cancelled."

                                savedMessage = false
                            }

                            else -> {

                                val index = findStudentIndex2(
                                    students = students,
                                    student = student
                                )

                                if (index == -1) {
                                    validationMessage = "Student record could not be found."
                                    savedMessage = false
                                } else if (isSaving) {
                                    // Prevent duplicate submissions.
                                } else {
                                    isSaving = true
                                    savedMessage = false
                                    validationMessage = ""

                                    scope.launch {
                                        try {
                                            val key = student.enrollmentNo.trim()
                                                .ifBlank { student.studentId?.toString().orEmpty() }
                                                .ifBlank { student.uniqueId.trim() }

                                            if (key.isBlank()) {
                                                throw ApiClient.ApiException(
                                                    422,
                                                    "Student does not have an Enrollment No or Unique ID."
                                                )
                                            }

                                            ApiClient.updateAdmission(
                                                context = context,
                                                studentKey = key,
                                                admissionYear = admissionYear,
                                                branch = BranchCatalog.canonicalBranch(branch),
                                                eligibleCategory = eligibleCategory,
                                                allocatedCategory = allocatedCategory,
                                                offlineCategory = offlineCategory,
                                                round = admissionRound,
                                                status = admissionStatus,
                                                remarks = remarks
                                            )

                                            students[index] = students[index].copy(
                                                branch = BranchCatalog.canonicalBranch(branch),
                                                admissionYear = academicYearForAdmissionYear2(admissionYear),
                                                eligibleCategory = eligibleCategory,
                                                allocatedCategory = allocatedCategory,
                                                offlineAdmissionCategory = offlineCategory,
                                                round = admissionRound,
                                                status = admissionStatus,
                                                remarks = remarks
                                            )
                                            selectedStudent = students[index]
                                            savedMessage = true
                                            validationMessage = ""
                                        } catch (e: ApiClient.ApiException) {
                                            validationMessage = e.message
                                                ?: "Unable to save admission details."
                                            savedMessage = false
                                        } catch (_: Exception) {
                                            validationMessage = "Unable to save admission details."
                                            savedMessage = false
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdmissionBlue2
                    )
                ) {

                    Text(
                        text = if (isSaving) "SAVING..." else "SAVE ADMISSION",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (savedMessage) {

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = "Admission details saved successfully.",
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "BACK",
                    color = AdmissionBlue2
                )
            }
        }
    }
}


// =========================================================
// STUDENT SEARCH CARD
// =========================================================

@Composable
fun AdmissionStudentSearchCard2(
    student: Student,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Text(
                text = student.name.ifBlank { "-" },
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AdmissionText2
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Enrollment: ${
                    student.enrollmentNo.ifBlank { "-" }
                }",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = "Unique ID: ${
                    student.uniqueId.ifBlank { "-" }
                }",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = "Class Roll No: ${
                    student.classRollNo.ifBlank { "-" }
                }",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = "Roll No: ${
                    student.rollNo.ifBlank { "-" }
                }",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = "Branch: ${
                    student.branch.ifBlank { "-" }
                }",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}


// =========================================================
// SELECTED STUDENT CARD
// =========================================================

@Composable
fun AdmissionStudentCard2(
    student: Student
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = student.name.ifBlank { "-" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AdmissionText2
            )

            Spacer(modifier = Modifier.height(10.dp))

            AdmissionInfoRow2(
                "Enrollment No",
                student.enrollmentNo
            )

            AdmissionInfoRow2(
                "Unique ID",
                student.uniqueId
            )

            AdmissionInfoRow2(
                "Class Roll No",
                student.classRollNo
            )

            AdmissionInfoRow2(
                "Branch",
                BranchCatalog.canonicalBranch(student.branch)
            )
        }
    }
}


// =========================================================
// ADMISSION RECORDS
// =========================================================

@Composable
fun AdmissionRecordsScreen2(
    students: List<Student>,
    onBack: () -> Unit
) {

    var searchText by remember {
        mutableStateOf("")
    }

    var selectedYear by remember {
        mutableStateOf("All")
    }

    var selectedAdmissionYear by remember {
        mutableStateOf("All")
    }

    var selectedBranch by remember {
        mutableStateOf("All")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedRound by remember {
        mutableStateOf("All")
    }

    var selectedStatus by remember {
        mutableStateOf("All")
    }

    val admissionStudents =
        students.filter {
            hasAdmissionData2(it)
        }

    val filteredStudents =
        admissionStudents.filter { student ->

            val query = searchText.trim()

            val searchMatch =
                query.isBlank() ||
                        student.name.contains(query, true) ||
                        student.enrollmentNo.contains(query, true) ||
                        student.uniqueId.contains(query, true) ||
                        student.classRollNo.contains(query, true)

            val yearMatch =
                selectedYear == "All" ||
                        academicYearForAdmissionYear2(student.admissionYear) == normalizedAcademicYear2(selectedYear)

            val admissionYearMatch =
                selectedAdmissionYear == "All" ||
                        admissionYearForAcademicYear2(student.admissionYear) == selectedAdmissionYear

            val branchMatch =
                selectedBranch == "All" ||
                        BranchCatalog.canonicalBranch(student.branch) == selectedBranch

            val categoryMatch =
                selectedCategory == "All" ||
                        student.allocatedCategory.trim() == selectedCategory

            val roundMatch =
                selectedRound == "All" ||
                        (if (selectedRound == "Not Specified") student.round.isBlank() else AdmissionCatalog.normalizeAdmissionRound(student.round).equals(AdmissionCatalog.normalizeAdmissionRound(selectedRound), ignoreCase = true))

            val statusMatch =
                selectedStatus == "All" ||
                        student.status.trim().equals(selectedStatus, ignoreCase = true)

            searchMatch &&
                    yearMatch &&
                    admissionYearMatch &&
                    branchMatch &&
                    categoryMatch &&
                    roundMatch &&
                    statusMatch
        }

    val yearOptions =
        listOf("All") + AdmissionCatalog.academicYears.map { normalizedAcademicYear2(it) }.filter { it.isNotBlank() }.distinct().sorted()

    val admissionYearOptions =
        listOf("All") + AdmissionCatalog.academicYears.map { admissionYearForAcademicYear2(it) }.filter { it.isNotBlank() }.distinct().sorted()

    val branchOptions =
        listOf("All") + students
            .map { BranchCatalog.canonicalBranch(it.branch) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    val categoryOptions =
        listOf("All") + allocatedCategoryOptions2

    val roundOptions =
        listOf("All", "Not Specified") + admissionRoundOptions2()

    val statusOptions =
        listOf("All") + admissionStatusOptions2

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AdmissionHeader2(
            title = "Admission Records",
            subtitle = "View and filter individual processed admissions",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // -------------------------------------------------
            // FILTER SECTION
            // -------------------------------------------------

            item {

                AdmissionSectionHeader2(
                    title = "Search & Filters"
                )

                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            "Search Name / Enrollment / Unique ID / Roll No"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                AdmissionDropdown2(
                    label = "Academic Year",
                    value = selectedYear,
                    options = yearOptions,
                    onSelected = {
                        selectedYear = it
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Year",
                    value = selectedAdmissionYear,
                    options = admissionYearOptions,
                    onSelected = { selectedAdmissionYear = it }
                )

                AdmissionDropdown2(
                    label = "Branch",
                    value = selectedBranch,
                    options = branchOptions,
                    onSelected = {
                        selectedBranch = it
                    }
                )

                AdmissionDropdown2(
                    label = "Allocated Category",
                    value = selectedCategory,
                    options = categoryOptions,
                    onSelected = {
                        selectedCategory = it
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Round",
                    value = selectedRound,
                    options = roundOptions,
                    onSelected = {
                        selectedRound = it
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Status",
                    value = selectedStatus,
                    options = statusOptions,
                    onSelected = {
                        selectedStatus = it
                    }
                )

                OutlinedButton(
                    onClick = {

                        searchText = ""
                        selectedYear = "All"
                        selectedAdmissionYear = "All"
                        selectedBranch = "All"
                        selectedCategory = "All"
                        selectedRound = "All"
                        selectedStatus = "All"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = "RESET FILTERS",
                        color = AdmissionBlue2,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${filteredStudents.size} record(s) found",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            // -------------------------------------------------
            // RECORDS
            // -------------------------------------------------

            if (filteredStudents.isEmpty()) {

                item {

                    AdmissionEmptyCard2(
                        text = when {

                            students.isEmpty() ->
                                "No students available. Add students from Student Records first."

                            admissionStudents.isEmpty() ->
                                "No admission records available. Process an admission first."

                            else ->
                                "No admission records match the selected filters."
                        }
                    )
                }

            } else {

                items(
                    items = filteredStudents
                ) { student ->

                    AdmissionRecordCard2(
                        student = student
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 18.dp
                ),
            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text = "BACK",
                color = AdmissionBlue2
            )
        }
    }
}


// =========================================================
// RECORD CARD
// =========================================================

@Composable
fun AdmissionRecordCard2(
    student: Student
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = student.name.ifBlank { "-" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdmissionText2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enrollment: ${
                            student.enrollmentNo.ifBlank { "-" }
                        }",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            admissionStatusColor2(student.status),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                ) {

                    Text(
                        text = student.status.ifBlank { "-" },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AdmissionInfoRow2(
                "Unique ID",
                student.uniqueId
            )

            AdmissionInfoRow2(
                "Class Roll No",
                student.classRollNo
            )

            AdmissionInfoRow2(
                "Branch",
                BranchCatalog.canonicalBranch(student.branch)
            )

            AdmissionInfoRow2(
                "Admission Year",
                student.admissionYear
            )

            AdmissionInfoRow2(
                "Eligible Category",
                student.eligibleCategory
            )

            AdmissionInfoRow2(
                "Allocated Category",
                student.allocatedCategory
            )

            AdmissionInfoRow2(
                "Offline Category",
                student.offlineAdmissionCategory
            )

            AdmissionInfoRow2(
                "Admission Round",
                student.round
            )

            AdmissionInfoRow2(
                "Remarks",
                student.remarks
            )
        }
    }
}


// =========================================================
// ADMISSION SUMMARY
// =========================================================

@Composable
fun AdmissionSummaryScreen2(
    students: List<Student>,
    onBack: () -> Unit
) {

    var selectedYear by remember {
        mutableStateOf("All")
    }

    var selectedAdmissionYear by remember {
        mutableStateOf("All")
    }

    var selectedBranch by remember {
        mutableStateOf("All")
    }

    var selectedCategory by remember {
        mutableStateOf("All")
    }

    var selectedRound by remember {
        mutableStateOf("All")
    }

    var selectedStatus by remember {
        mutableStateOf("All")
    }

    val admissionStudents =
        students.filter {
            hasAdmissionData2(it)
        }

    val filteredStudents =
        admissionStudents.filter { student ->

            val yearMatch =
                selectedYear == "All" ||
                        academicYearForAdmissionYear2(student.admissionYear) == normalizedAcademicYear2(selectedYear)

            val admissionYearMatch =
                selectedAdmissionYear == "All" ||
                        admissionYearForAcademicYear2(student.admissionYear) == selectedAdmissionYear

            val branchMatch =
                selectedBranch == "All" ||
                        BranchCatalog.canonicalBranch(student.branch) == selectedBranch

            val categoryMatch =
                selectedCategory == "All" ||
                        student.allocatedCategory.trim() == selectedCategory

            val roundMatch =
                selectedRound == "All" ||
                        (if (selectedRound == "Not Specified") student.round.isBlank() else AdmissionCatalog.normalizeAdmissionRound(student.round).equals(AdmissionCatalog.normalizeAdmissionRound(selectedRound), ignoreCase = true))

            val statusMatch =
                selectedStatus == "All" ||
                        student.status.trim().equals(selectedStatus, ignoreCase = true)

            yearMatch &&
                    admissionYearMatch &&
                    branchMatch &&
                    categoryMatch &&
                    roundMatch &&
                    statusMatch
        }

    val admittedStudents =
        filteredStudents.filter {
            it.status.equals(
                "Admitted",
                ignoreCase = true
            )
        }

    val cancelledStudents =
        filteredStudents.filter {
            it.status.equals(
                "Cancelled",
                ignoreCase = true
            )
        }

    val branchGroups =
        filteredStudents
            .groupBy {
                it.branch.ifBlank {
                    "Branch Not Specified"
                }
            }
            .toSortedMap()

    val categoryCounts =
        filteredStudents
            .groupingBy {
                admissionCategoryForStudent2(it)
            }
            .eachCount()

    val roundCounts =
        filteredStudents
            .groupingBy {
                it.round.trim().ifBlank {
                    "Not Specified"
                }
            }
            .eachCount()

    val yearOptions =
        listOf("All") + AdmissionCatalog.academicYears.map { normalizedAcademicYear2(it) }.filter { it.isNotBlank() }.distinct().sorted()

    val admissionYearOptions =
        listOf("All") + AdmissionCatalog.academicYears.map { admissionYearForAcademicYear2(it) }.filter { it.isNotBlank() }.distinct().sorted()

    val branchOptions =
        listOf("All") + students
            .map { BranchCatalog.canonicalBranch(it.branch) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    val categoryOptions =
        listOf("All") + allocatedCategoryOptions2

    val roundOptions =
        listOf("All", "Not Specified") + admissionRoundOptions2()

    val statusOptions =
        listOf("All") + admissionStatusOptions2

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        AdmissionHeader2(
            title = "Admission Summary",
            subtitle = "Analyze admissions by year, branch, category and round",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // =================================================
            // FILTERS
            // =================================================

            item {

                AdmissionSectionHeader2(
                    title = "Summary Filters"
                )

                AdmissionDropdown2(
                    label = "Academic Year",
                    value = selectedYear,
                    options = yearOptions,
                    onSelected = {
                        selectedYear = it
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Year",
                    value = selectedAdmissionYear,
                    options = admissionYearOptions,
                    onSelected = { selectedAdmissionYear = it }
                )

                AdmissionDropdown2(
                    label = "Branch",
                    value = selectedBranch,
                    options = branchOptions,
                    onSelected = {
                        selectedBranch = it
                    }
                )

                AdmissionDropdown2(
                    label = "Allocated Category",
                    value = selectedCategory,
                    options = categoryOptions,
                    onSelected = {
                        selectedCategory = it
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Round",
                    value = selectedRound,
                    options = roundOptions,
                    onSelected = {
                        selectedRound = it
                    }
                )

                AdmissionDropdown2(
                    label = "Admission Status",
                    value = selectedStatus,
                    options = statusOptions,
                    onSelected = {
                        selectedStatus = it
                    }
                )

                OutlinedButton(
                    onClick = {

                        selectedYear = "All"
                        selectedAdmissionYear = "All"
                        selectedBranch = "All"
                        selectedCategory = "All"
                        selectedRound = "All"
                        selectedStatus = "All"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = "RESET FILTERS",
                        color = AdmissionBlue2,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // =================================================
            // MAIN STATISTICS
            // =================================================

            item {

                AdmissionSectionHeader2(
                    title = "Admission Statistics"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    AdmissionStatCard2(
                        title = "Processed",
                        value = filteredStudents.size.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    AdmissionStatCard2(
                        title = "Admitted",
                        value = admittedStudents.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    AdmissionStatCard2(
                        title = "Cancelled",
                        value = cancelledStudents.size.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    AdmissionStatCard2(
                        title = "Completion",
                        value = if (filteredStudents.isEmpty()) {
                            "0%"
                        } else {
                            "${(admittedStudents.size * 100) / filteredStudents.size}%"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // =================================================
            // BRANCH-WISE ADMITTED STUDENTS
            // =================================================

            item {

                AdmissionSectionHeader2(
                    title = "Branch-wise Admitted Students"
                )

                Text(
                    text = "This section shows only students whose admission status is Admitted.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (branchGroups.isEmpty()) {

                item {

                    AdmissionEmptyCard2(
                        text = "No admitted students found for the selected filters."
                    )
                }

            } else {

                branchGroups.forEach { (branchName, branchStudents) ->

                    item {

                        AdmissionBranchSummaryCard2(
                            branchName = branchName,
                            students = branchStudents
                        )
                    }
                }
            }

            // =================================================
            // CATEGORY SUMMARY
            // =================================================

            item {

                AdmissionSectionHeader2(
                    title = "Category-wise Summary"
                )

                AdmissionInfoCard2 {

                    if (categoryCounts.isEmpty()) {

                        AdmissionInfoRow2(
                            "Category",
                            "No data"
                        )

                    } else {

                        categoryCounts
                            .toList()
                            .sortedBy {
                                it.first
                            }
                            .forEach { (category, count) ->

                                AdmissionInfoRow2(
                                    category,
                                    count.toString()
                                )
                            }
                    }
                }
            }

            // =================================================
            // ROUND SUMMARY
            // =================================================

            item {

                AdmissionSectionHeader2(
                    title = "Round-wise Summary"
                )

                AdmissionInfoCard2 {

                    if (roundCounts.isEmpty()) {

                        AdmissionInfoRow2(
                            "Round",
                            "No data"
                        )

                    } else {

                        val preferredOrder =
                            admissionRoundOptions2()

                        preferredOrder.forEach { round ->

                            val count =
                                roundCounts[round] ?: 0

                            AdmissionInfoRow2(
                                round,
                                count.toString()
                            )
                        }

                        val unspecifiedCount =
                            roundCounts["Not Specified"] ?: 0

                        if (unspecifiedCount > 0) {

                            AdmissionInfoRow2(
                                "Not Specified",
                                unspecifiedCount.toString()
                            )
                        }
                    }
                }
            }

            // =================================================
            // FILTERED RECORD COUNT
            // =================================================

            item {

                AdmissionSectionHeader2(
                    title = "Selected Admission Records"
                )

                Text(
                    text = "${filteredStudents.size} admission record(s) match the selected filters.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            if (filteredStudents.isEmpty()) {

                item {

                    AdmissionEmptyCard2(
                        text = "No admission records found for the selected filters."
                    )
                }

            } else {

                items(
                    items = filteredStudents
                ) { student ->

                    AdmissionSummaryStudentCard2(
                        student = student
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    bottom = 18.dp
                ),
            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text = "BACK",
                color = AdmissionBlue2
            )
        }
    }
}


// =========================================================
// BRANCH SUMMARY CARD
// =========================================================

@Composable
fun AdmissionBranchSummaryCard2(
    branchName: String,
    students: List<Student>
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = branchName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdmissionBlue2,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${students.size} admitted",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            students.forEachIndexed { index, student ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AdmissionBackground2
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = "${index + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = AdmissionBlue2
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = student.name.ifBlank { "-" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AdmissionText2,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "Enrollment: ${
                                student.enrollmentNo.ifBlank { "-" }
                            }",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "Unique ID: ${
                                student.uniqueId.ifBlank { "-" }
                            }",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "Admission Year: ${
                                student.admissionYear.ifBlank { "-" }
                            }",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "Academic Year: ${
                                academicYearForAdmissionYear2(student.admissionYear).ifBlank { "-" }
                            }",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "Category: ${
                                admissionCategoryForStudent2(student)
                            }",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = "Round: ${
                                student.round.ifBlank { "-" }
                            }",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}


// =========================================================
// SUMMARY STUDENT CARD
// =========================================================

@Composable
fun AdmissionSummaryStudentCard2(
    student: Student
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = student.name.ifBlank { "-" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdmissionText2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enrollment: ${
                            student.enrollmentNo.ifBlank { "-" }
                        }",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            admissionStatusColor2(student.status),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                ) {

                    Text(
                        text = student.status.ifBlank { "-" },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AdmissionInfoRow2(
                "Unique ID",
                student.uniqueId
            )

            AdmissionInfoRow2(
                "Branch",
                BranchCatalog.canonicalBranch(student.branch)
            )

            AdmissionInfoRow2(
                "Admission Year",
                student.admissionYear
            )

            AdmissionInfoRow2(
                "Academic Year",
                academicYearForAdmissionYear2(student.admissionYear)
            )

            AdmissionInfoRow2(
                "Category",
                admissionCategoryForStudent2(student)
            )

            AdmissionInfoRow2(
                "Round",
                student.round
            )
        }
    }
}


// =========================================================
// ADMISSION DATA CHECK
// =========================================================

private fun hasAdmissionData2(
    student: Student
): Boolean {
    // Do not use Admission Status as the only admission-record test.
    // Legacy/imported rows may have admission year, branch, category or round
    // populated while status is blank/NULL. Filtering only by status caused
    // valid admission data to disappear from Records and Summary.
    return student.status.isNotBlank() ||
        student.admissionYear.isNotBlank() ||
        student.branch.isNotBlank() ||
        student.allocatedCategory.isNotBlank() ||
        student.eligibleCategory.isNotBlank() ||
        student.offlineAdmissionCategory.isNotBlank() ||
        student.round.isNotBlank()
}


// =========================================================
// CATEGORY HELPER
// =========================================================

private fun admissionCategoryForStudent2(
    student: Student
): String {
    // Admission summaries and filters must reflect Allocated Category only.
    // Do not substitute Eligible Category or invent an "Other" category.
    return student.allocatedCategory.trim().ifBlank { "Not Specified" }
}


// =========================================================
// FIND STUDENT INDEX
// =========================================================

private fun findStudentIndex2(
    students: List<Student>,
    student: Student
): Int {

    if (student.uniqueId.isNotBlank()) {

        val index =
            students.indexOfFirst {
                it.uniqueId == student.uniqueId
            }

        if (index != -1) {
            return index
        }
    }

    if (student.enrollmentNo.isNotBlank()) {

        val index =
            students.indexOfFirst {
                it.enrollmentNo == student.enrollmentNo
            }

        if (index != -1) {
            return index
        }
    }

    val objectIndex =
        students.indexOfFirst {
            it == student
        }

    return objectIndex
}


// =========================================================
// STATUS COLOR
// =========================================================

private fun admissionStatusColor2(
    status: String
): Color {

    return when {
        status.equals("Admitted", ignoreCase = true) ->
            Color(0xFF2E7D32)

        status.equals("Cancelled", ignoreCase = true) ->
            Color(0xFFC62828)

        else -> Color.Gray
    }
}


// =========================================================
// SECTION HEADER
// =========================================================

@Composable
fun AdmissionSectionHeader2(
    title: String
) {

    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AdmissionText2,
        modifier = Modifier.padding(
            top = 8.dp,
            bottom = 10.dp
        )
    )
}


// =========================================================
// INFO CARD
// =========================================================

@Composable
fun AdmissionInfoCard2(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}


// =========================================================
// INFO ROW
// =========================================================

@Composable
fun AdmissionInfoRow2(
    label: String,
    value: String
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.Top
        ) {

            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AdmissionText2,
                modifier = Modifier.width(155.dp)
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = value.ifBlank { "-" },
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E7EB))
        )
    }
}


// =========================================================
// STAT CARD
// =========================================================

@Composable
fun AdmissionStatCard2(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                color = Color.Gray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = AdmissionBlue2
            )
        }
    }
}


// =========================================================
// TEXT FIELD
// =========================================================

@Composable
fun AdmissionField2(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        label = {
            Text(label)
        },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text
        )
    )
}


// =========================================================
// DROPDOWN
// =========================================================

@Composable
fun AdmissionDropdown2(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {

        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray,
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
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = if (value.isBlank()) {
                            "Select $label"
                        } else {
                            value
                        },
                        fontSize = 14.sp,
                        color = AdmissionText2
                    )

                    Text(
                        text = "▼",
                        color = AdmissionBlue2,
                        fontSize = 11.sp
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },
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


// =========================================================
// EMPTY CARD
// =========================================================

@Composable
fun AdmissionEmptyCard2(
    text: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = text,
                color = Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
