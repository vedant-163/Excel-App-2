
package com.example.studentmanagementapp
import androidx.compose.runtime.LaunchedEffect
import android.app.DatePickerDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.School

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val StudentBlue = Color(0xFF1565C0)
private val StudentDarkBlue = Color(0xFF173B8F)
private val StudentBackground = Color(0xFFF5F7FB)
private val StudentLightBlue = Color(0xFFE8EEF9)
private val StudentText = Color(0xFF172033)


// ============================================================
// COMMON OPTIONS
// ============================================================

// Branch names are maintained centrally in BranchCatalog.kt.
// AI & Robotics is available only from academic year 2026-27.

// Admission year means the year in which admission was taken.
// Example: 2024, not 2024-25.
//
// Admission Status is intentionally restricted to:
// Admitted / Cancelled.

val admissionYearOptions: List<String>
    get() = AdmissionCatalog.admissionYears

val genderOptions = listOf(
    "Male",
    "Female",
    "Other"
)

val categoryOptions: List<String>
    get() = AdmissionCatalog.categories

// Backend stores domicile as Y/N; keep the UI values aligned with that contract.
val domicileOptions = listOf("Yes", "No")

val ewsOptions = listOf(
    "Yes",
    "No"
)

val statusOptions: List<String>
    get() = AdmissionCatalog.admissionStatuses

val roundOptions: List<String>
    get() = AdmissionCatalog.admissionRounds

private fun isValidAdmissionStatus(status: String): Boolean {
    return status.trim() in statusOptions
}

private fun studentAcademicYear(admissionYear: String): String =
    AdmissionCatalog.academicYearForAdmissionYear(admissionYear)



// ============================================================
// MAIN STUDENT SCREEN
// ============================================================

@Composable
fun StudentScreen(
    students: SnapshotStateList<Student>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var screen by remember {
        mutableStateOf("menu")
    }

    when (screen) {

        "menu" -> {
            StudentMenu(
                onAddStudent = {
                    screen = "add"
                },
                onViewStudents = {
                    screen = "view"
                },
                onEditStudent = {
                    screen = "edit"
                },
                onDeleteStudent = {
                    screen = "delete"
                },
                onBack = onBackClick
            )
        }

        "add" -> {
            AddStudentForm(
                onSave = { student ->
                    val duplicateEnrollment = students.any {
                        it.enrollmentNo.trim().equals(student.enrollmentNo.trim(), ignoreCase = true)
                    }
                    val duplicateUniqueId = student.uniqueId.isNotBlank() && students.any {
                        it.uniqueId.trim().equals(student.uniqueId.trim(), ignoreCase = true)
                    }
                    if (duplicateEnrollment || duplicateUniqueId) {
                        false
                    } else {
                        ApiClient.createStudent(context, student)
                        students.clear()
                        students.addAll(StudentStorage.syncWithBackend(context))
                        true
                    }
                },
                onBack = {
                    screen = "menu"
                }
            )
        }

        "view" -> {
            ViewStudentRecords(
                students = students,
                onBack = {
                    screen = "menu"
                }
            )
        }

        "edit" -> {
            EditStudentForm(
                students = students,
                onUpdate = { originalId, updatedStudent ->
                    ApiClient.updateStudent(context, originalId, updatedStudent)
                    val refreshed = StudentStorage.syncWithBackend(context)
                    students.clear()
                    students.addAll(refreshed)
                },
                onBack = {
                    screen = "menu"
                }
            )
        }

        "delete" -> {
            DeleteStudentForm(
                students = students,
                onDelete = { student ->
                    val key = student.studentId?.toString() ?: student.enrollmentNo
                    ApiClient.deleteStudent(context, key)
                    val refreshed = StudentStorage.syncWithBackend(context)
                    students.clear()
                    students.addAll(refreshed)
                },
                onBack = {
                    screen = "menu"
                }
            )
        }
    }
}


// ============================================================
// COMMON HEADER
// ============================================================

@Composable
fun StudentScreenHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = StudentBlue,
                shape = RoundedCornerShape(
                    bottomStart = 26.dp,
                    bottomEnd = 26.dp
                )
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

            if (onBack != null) {

                Row(
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BACK",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }
    }
}


// ============================================================
// STUDENT MENU
// ============================================================

@Composable
fun StudentMenu(
    onAddStudent: () -> Unit,
    onViewStudents: () -> Unit,
    onEditStudent: () -> Unit,
    onDeleteStudent: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudentBackground
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            StudentScreenHeader(
                title = "Student Records",
                subtitle = "Manage student information",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Student Management",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudentText
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "Add, view, update and manage student records.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                Text(
                    text = "Quick Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudentText
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    StudentActionCard(
                        title = "Add Student",
                        subtitle = "Create new record",
                        symbol = "+",
                        modifier = Modifier.weight(1f),
                        onClick = onAddStudent
                    )

                    StudentActionCard(
                        title = "View Records",
                        subtitle = "View all students",
                        symbol = "VR",
                        modifier = Modifier.weight(1f),
                        onClick = onViewStudents
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    StudentActionCard(
                        title = "Edit Student",
                        subtitle = "Update information",
                        symbol = "ED",
                        modifier = Modifier.weight(1f),
                        onClick = onEditStudent
                    )

                    StudentActionCard(
                        title = "Delete Student",
                        subtitle = "Remove a record",
                        symbol = "DL",
                        modifier = Modifier.weight(1f),
                        onClick = onDeleteStudent
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudentBlue
                    )
                ) {
                    Text(
                        text = "BACK TO DASHBOARD",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}


// ============================================================
// ACTION CARD
// ============================================================

@Composable
fun StudentActionCard(
    title: String,
    subtitle: String,
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(155.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = StudentLightBlue,
                        shape = RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val actionIcon = when (symbol) {
                    "+" -> Icons.Filled.Add
                    "VR" -> Icons.Filled.School
                    "ED" -> Icons.Filled.Edit
                    "DL" -> Icons.Filled.Delete
                    else -> Icons.Filled.Person
                }
                Icon(
                    imageVector = actionIcon,
                    contentDescription = title,
                    tint = StudentBlue,
                    modifier = Modifier.size(23.dp)
                )
            }

            Column {

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudentText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// ============================================================
// ADD STUDENT
// ============================================================

@Composable
fun AddStudentForm(
    onSave: suspend (Student) -> Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    var uniqueId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var classRollNo by remember { mutableStateOf("") }
    var enrollmentNo by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }

    var father by remember { mutableStateOf("") }
    var mother by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var parentsPhone by remember { mutableStateOf("") }
    var parentsEmail by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }

    var domicile by remember { mutableStateOf("") }
    var ews by remember { mutableStateOf("") }

    var permanentAddress by remember {
        mutableStateOf("")
    }

    var presentAddress by remember {
        mutableStateOf("")
    }

    var branch by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("") }

    var eligibleCategory by remember {
        mutableStateOf("")
    }

    var allocatedCategory by remember {
        mutableStateOf("")
    }

    var offlineAdmissionCategory by remember {
        mutableStateOf("")
    }

    var status by remember { mutableStateOf("") }
    var round by remember { mutableStateOf("") }
    var admissionYear by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val branchOptionsForYear =
        BranchCatalog.optionsForAcademicYear(admissionYear)

    var aadhaarNo by remember { mutableStateOf("") }

    var aadhaarLinkedMobile by remember {
        mutableStateOf("")
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudentBackground
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            StudentScreenHeader(
                title = "Add Student",
                subtitle = "Create a new student record",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                FormSectionHeader(
                    "01",
                    "Student Identity",
                    "Basic student identification"
                )

                AppTextField(
                    label = "Unique ID",
                    value = uniqueId,
                    onValueChange = {
                        uniqueId = it
                    }
                )

                AppTextField(
                    label = "Student Name *",
                    value = name,
                    onValueChange = {
                        name = it
                    }
                )

                AppTextField(
                    label = "Enrollment No *",
                    value = enrollmentNo,
                    onValueChange = {
                        enrollmentNo = it
                    }
                )

                AppTextField(
                    label = "Class Roll No",
                    value = classRollNo,
                    onValueChange = {
                        classRollNo = it
                    }
                )

                AppTextField(
                    label = "Roll No",
                    value = rollNo,
                    onValueChange = {
                        rollNo = it
                    }
                )

                DateField(
                    label = "Date of Birth",
                    value = dateOfBirth,
                    onClick = {
                        showDatePicker = true
                    }
                )

                DropdownField(
                    label = "Gender",
                    value = gender,
                    options = genderOptions,
                    onValueChange = {
                        gender = it
                    }
                )

                FormSectionHeader(
                    "02",
                    "Academic Details",
                    "Course and academic information"
                )

                DropdownField(
                    label = "Branch *",
                    value = branch,
                    options = branchOptionsForYear,
                    onValueChange = {
                        branch = it
                    }
                )

                DropdownField(
                    label = "Admission Year",
                    value = admissionYear,
                    options = admissionYearOptions,
                    onValueChange = {
                        admissionYear = it
                        if (branch.isNotBlank() &&
                            branch !in BranchCatalog.optionsForAcademicYear(it)
                        ) {
                            branch = ""
                        }
                    }
                )

                if (admissionYear.isNotBlank()) {
                    Text(
                        text = "Academic Year: ${studentAcademicYear(admissionYear)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                AppTextField(
                    label = "Rank",
                    value = rank,
                    onValueChange = {
                        rank = it
                    }
                )

                AppTextField(
                    label = "Marks",
                    value = marks,
                    onValueChange = {
                        marks = it
                    }
                )

                FormSectionHeader(
                    "03",
                    "Family & Contact",
                    "Student and parent contact information"
                )

                AppTextField(
                    label = "Father Name",
                    value = father,
                    onValueChange = {
                        father = it
                    }
                )

                AppTextField(
                    label = "Mother Name",
                    value = mother,
                    onValueChange = {
                        mother = it
                    }
                )

                AppTextField(
                    label = "Student Phone",
                    value = studentPhone,
                    placeholder = "10 digit mobile number",
                    onValueChange = { value ->

                        if (
                            value.all { it.isDigit() } &&
                            value.length <= 10
                        ) {
                            studentPhone = value
                        }
                    }
                )

                AppTextField(
                    label = "Student Email",
                    value = email,
                    placeholder = "example@email.com",
                    onValueChange = {
                        email = it
                    }
                )

                AppTextField(
                    label = "Parents Phone",
                    value = parentsPhone,
                    placeholder = "10 digit mobile number",
                    onValueChange = { value ->

                        if (
                            value.all { it.isDigit() } &&
                            value.length <= 10
                        ) {
                            parentsPhone = value
                        }
                    }
                )

                AppTextField(
                    label = "Parents Email",
                    value = parentsEmail,
                    placeholder = "example@email.com",
                    onValueChange = {
                        parentsEmail = it
                    }
                )

                AppTextField(
                    label = "Religion",
                    value = religion,
                    onValueChange = {
                        religion = it
                    }
                )

                FormSectionHeader(
                    "04",
                    "Admission Details",
                    "Category and admission information"
                )

                DropdownField(
                    label = "Eligible Category",
                    value = eligibleCategory,
                    options = categoryOptions,
                    onValueChange = {
                        eligibleCategory = it
                    }
                )

                DropdownField(
                    label = "Allocated Category",
                    value = allocatedCategory,
                    options = categoryOptions,
                    onValueChange = {
                        allocatedCategory = it
                    }
                )

                AppTextField(
                    label = "Offline Admission Category",
                    value = offlineAdmissionCategory,
                    onValueChange = {
                        offlineAdmissionCategory = it
                    }
                )

                DropdownField(
                    label = "Domicile",
                    value = domicile,
                    options = domicileOptions,
                    onValueChange = {
                        domicile = it
                    }
                )

                DropdownField(
                    label = "EWS",
                    value = ews,
                    options = ewsOptions,
                    onValueChange = {
                        ews = it
                    }
                )

                DropdownField(
                    label = "Admission Round",
                    value = round,
                    options = roundOptions,
                    onValueChange = {
                        round = it
                    }
                )

                DropdownField(
                    label = "Status",
                    value = status,
                    options = statusOptions,
                    onValueChange = {
                        status = it
                    }
                )

                FormSectionHeader(
                    "05",
                    "Address & Documents",
                    "Address and basic document details"
                )

                AppTextField(
                    label = "Permanent Address",
                    value = permanentAddress,
                    onValueChange = {
                        permanentAddress = it
                    }
                )

                AppTextField(
                    label = "Present Address",
                    value = presentAddress,
                    onValueChange = {
                        presentAddress = it
                    }
                )

                AppTextField(
                    label = "Aadhaar No",
                    value = aadhaarNo,
                    onValueChange = { value ->

                        if (
                            value.all { it.isDigit() } &&
                            value.length <= 12
                        ) {
                            aadhaarNo = value
                        }
                    }
                )

                AppTextField(
                    label = "Aadhaar Linked Mobile",
                    value = aadhaarLinkedMobile,
                    placeholder = "10 digit mobile number",
                    onValueChange = { value ->

                        if (
                            value.all { it.isDigit() } &&
                            value.length <= 10
                        ) {
                            aadhaarLinkedMobile = value
                        }
                    }
                )

                AppTextField(
                    label = "Remarks",
                    value = remarks,
                    onValueChange = {
                        remarks = it
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {

                        when {

                            name.trim().isBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Please enter Student Name",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            enrollmentNo.trim().isBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Please enter Enrollment No",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            classRollNo.trim().isBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Please enter Class Roll No (required by the database)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            branch.isBlank() -> {
                                Toast.makeText(
                                    context,
                                    "Please select Branch",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            gender.isBlank() -> {
                                Toast.makeText(context, "Please select Gender", Toast.LENGTH_SHORT).show()
                            }

                            eligibleCategory.isBlank() -> {
                                Toast.makeText(context, "Please select Eligible Category", Toast.LENGTH_SHORT).show()
                            }

                            allocatedCategory.isBlank() -> {
                                Toast.makeText(context, "Please select Allocated Category", Toast.LENGTH_SHORT).show()
                            }

                            religion.isBlank() -> {
                                Toast.makeText(context, "Please select Religion", Toast.LENGTH_SHORT).show()
                            }

                            !isValidAdmissionStatus(status) -> {
                                Toast.makeText(
                                    context,
                                    "Please select Admission Status: Admitted or Cancelled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            studentPhone.isNotBlank() &&
                                    studentPhone.length != 10 -> {
                                Toast.makeText(
                                    context,
                                    "Student phone must be 10 digits",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            parentsPhone.isNotBlank() &&
                                    parentsPhone.length != 10 -> {
                                Toast.makeText(
                                    context,
                                    "Parents phone must be 10 digits",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            aadhaarNo.isNotBlank() &&
                                    aadhaarNo.length != 12 -> {
                                Toast.makeText(
                                    context,
                                    "Aadhaar No must be 12 digits",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            aadhaarLinkedMobile.isNotBlank() &&
                                    aadhaarLinkedMobile.length != 10 -> {
                                Toast.makeText(
                                    context,
                                    "Aadhaar linked mobile must be 10 digits",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {

                                val student = Student(
                                    uniqueId = uniqueId.trim(),
                                    name = name.trim(),
                                    classRollNo = classRollNo.trim(),
                                    enrollmentNo = enrollmentNo.trim(),
                                    rollNo = rollNo.trim(),
                                    dateOfBirth = dateOfBirth.trim(),
                                    father = father.trim(),
                                    mother = mother.trim(),
                                    gender = gender,
                                    studentPhone = studentPhone.trim(),
                                    email = email.trim(),
                                    parentsPhone = parentsPhone.trim(),
                                    parentsEmail = parentsEmail.trim(),
                                    religion = religion.trim(),
                                    domicile = domicile,
                                    ews = ews,
                                    permanentAddress =
                                        permanentAddress.trim(),
                                    presentAddress =
                                        presentAddress.trim(),
                                    branch = BranchCatalog.canonicalBranch(branch),
                                    rank = rank.trim(),
                                    marks = marks.trim(),
                                    eligibleCategory =
                                        eligibleCategory,
                                    allocatedCategory =
                                        allocatedCategory,
                                    offlineAdmissionCategory =
                                        offlineAdmissionCategory.trim(),
                                    status = status,
                                    round = round,
                                    admissionYear =
                                        admissionYear,
                                    remarks = remarks.trim(),
                                    aadhaarNo = aadhaarNo.trim(),
                                    aadhaarLinkedMobile =
                                        aadhaarLinkedMobile.trim()
                                )

                                scope.launch {
                                    saving = true
                                    try {
                                        val saved = onSave(student)
                                        Toast.makeText(
                                            context,
                                            if (saved) "Student saved successfully" else "Unable to save student",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (saved) onBack()
                                    } catch (e: ApiClient.ApiException) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                    } finally {
                                        saving = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudentBlue
                    )
                ) {
                    Text(
                        text = "SAVE STUDENT",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        uniqueId = ""
                        name = ""
                        classRollNo = ""
                        enrollmentNo = ""
                        rollNo = ""
                        dateOfBirth = ""
                        father = ""
                        mother = ""
                        gender = ""
                        studentPhone = ""
                        email = ""
                        parentsPhone = ""
                        parentsEmail = ""
                        religion = ""
                        domicile = ""
                        ews = ""
                        permanentAddress = ""
                        presentAddress = ""
                        branch = ""
                        rank = ""
                        marks = ""
                        eligibleCategory = ""
                        allocatedCategory = ""
                        offlineAdmissionCategory = ""
                        status = ""
                        round = ""
                        admissionYear = ""
                        remarks = ""
                        aadhaarNo = ""
                        aadhaarLinkedMobile = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CLEAR FORM",
                        color = StudentBlue
                    )
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BACK TO STUDENT RECORDS",
                        color = StudentBlue
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }

    if (showDatePicker) {

        val datePickerDialog = remember {
            DatePickerDialog(
                context,
                { _, year, month, day ->

                    dateOfBirth = String.format(
                        "%02d/%02d/%04d",
                        day,
                        month + 1,
                        year
                    )

                    showDatePicker = false
                },
                2000,
                0,
                1
            )
        }

        DisposableEffect(Unit) {

            datePickerDialog.setOnDismissListener {
                showDatePicker = false
            }

            datePickerDialog.show()

            onDispose {
                datePickerDialog.dismiss()
            }
        }
    }
}



// ============================================================
// VIEW STUDENT RECORDS
// ============================================================

@Composable
fun ViewStudentRecords(
    students: List<Student>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchText by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("All Branches") }
    var selectedYear by remember { mutableStateOf("All Years") }
    var selectedStatus by remember { mutableStateOf("All Status") }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    // Independent API-backed state — does NOT depend on the shared students list
    var apiStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var totalCount by remember { mutableStateOf(0) }
    var currentOffset by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val pageSize = 30

    // Fetch from API whenever filters or offset change
    fun fetchPage(offset: Int, append: Boolean = false) {
        scope.launch {
            isLoading = true
            errorMsg = null
            try {
                val branchParam = if (selectedBranch == "All Branches") null
                    else BranchCatalog.branchCode(selectedBranch)
                val yearParam = if (selectedYear == "All Years") null else selectedYear
                val statusParam = if (selectedStatus == "All Status") null else selectedStatus
                val result = ApiClient.searchStudents(
                    context = context,
                    query = searchText.trim(),
                    // q searches name, enrollment, class roll, exam roll and unique ID together.
                    // Do not also pass name, because API filters are combined with AND.
                    name = "",
                    branch = branchParam ?: "",
                    admissionYear = yearParam ?: "",
                    status = statusParam ?: "",
                    limit = pageSize,
                    offset = offset
                )
                if (append) {
                    apiStudents = apiStudents + result.students
                } else {
                    apiStudents = result.students
                }
                totalCount = result.totalCount
                currentOffset = offset
            } catch (e: ApiClient.ApiException) {
                errorMsg = e.message ?: "Unable to load student records."
            } catch (_: Exception) {
                errorMsg = "Unable to connect to the server."
            } finally {
                isLoading = false
            }
        }
    }

    // Load first page when screen opens or filters change
    LaunchedEffect(searchText, selectedBranch, selectedYear, selectedStatus) {
        fetchPage(offset = 0)
    }

    if (selectedStudent != null) {
        StudentDetailsScreen(
            student = selectedStudent!!,
            onBack = { selectedStudent = null }
        )
        return
    }

    val branchFilterOptions = (listOf("All Branches") + (BranchCatalog.standardBranches + BranchCatalog.AI_ROBOTICS).distinct()).distinct()
    val yearFilterOptions = listOf("All Years") + AdmissionCatalog.academicYears
    val statusFilterOptions = listOf("All Status") + statusOptions

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudentBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            StudentScreenHeader(
                title = "View Student Records",
                subtitle = if (isLoading) "Loading…" else "$totalCount student record(s) found",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Search & Filters",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudentText
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AppTextField(
                            label = "Search Student",
                            value = searchText,
                            placeholder = "Name / Enrollment / ID / Roll No",
                            onValueChange = { searchText = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DropdownField(
                            label = "Branch",
                            value = selectedBranch,
                            options = branchFilterOptions,
                            onValueChange = { selectedBranch = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DropdownField(
                            label = "Academic Year",
                            value = selectedYear,
                            options = yearFilterOptions,
                            onValueChange = { selectedYear = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DropdownField(
                            label = "Status",
                            value = selectedStatus,
                            options = statusFilterOptions,
                            onValueChange = { selectedStatus = it }
                        )

                        if (searchText.isNotBlank() || selectedBranch != "All Branches" ||
                            selectedYear != "All Years" || selectedStatus != "All Status"
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    searchText = ""
                                    selectedBranch = "All Branches"
                                    selectedYear = "All Years"
                                    selectedStatus = "All Status"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "CLEAR SEARCH & FILTERS", color = StudentBlue)
                            }
                        }
                    }
                }

                when {
                    isLoading && apiStudents.isEmpty() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(color = StudentBlue)
                            }
                        }
                    }
                    errorMsg != null -> {
                        StudentEmptyCard(
                            title = "Unable to load records",
                            message = errorMsg ?: "Check your network and try again."
                        )
                        Button(
                            onClick = { fetchPage(0) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudentBlue)
                        ) { Text("RETRY", fontWeight = FontWeight.Bold) }
                    }
                    apiStudents.isEmpty() -> {
                        StudentEmptyCard(
                            title = "No matching students found",
                            message = "Try changing your search or filters."
                        )
                    }
                    else -> {
                        apiStudents.forEach { student ->
                            StudentRecordCard(
                                student = student,
                                onClick = { selectedStudent = student }
                            )
                        }

                        // Load more button
                        if (apiStudents.size < totalCount) {
                            if (isLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = androidx.compose.ui.Modifier.then(Modifier.size(24.dp)),
                                        color = StudentBlue
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { fetchPage(currentOffset + pageSize, append = true) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "LOAD MORE (${apiStudents.size} of $totalCount)",
                                        color = StudentBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "BACK TO STUDENT RECORDS MENU", color = StudentBlue)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}



// ============================================================
// STUDENT RECORD CARD
// ============================================================

@Composable
fun StudentRecordCard(
    student: Student,
    onClick: () -> Unit
) {
    val initials = student.name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "S" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(StudentLightBlue, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudentBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name.ifBlank { "Unnamed Student" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudentText
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = student.enrollmentNo.ifBlank { "Enrollment not available" },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "View details",
                    tint = StudentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudentMetaChip(
                    label = BranchCatalog.canonicalBranch(student.branch)
                        .ifBlank { "Branch N/A" },
                    modifier = Modifier.weight(1f)
                )
                StudentMetaChip(
                    label = student.admissionYear.ifBlank { "Year N/A" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${student.uniqueId.ifBlank { "N/A" }}",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Box(
                    modifier = Modifier
                        .background(StudentLightBlue, RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = student.status.ifBlank { "N/A" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudentBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentMetaChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(StudentBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = StudentText,
            maxLines = 1
        )
    }
}

// ============================================================
// STUDENT DETAILS
// ============================================================

@Composable
fun StudentDetailsScreen(
    student: Student,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudentBackground
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            StudentScreenHeader(
                title = "Student Details",
                subtitle = "Complete student information",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudentBlue),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    Color.White.copy(alpha = 0.16f),
                                    RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Student",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.name.ifBlank { "Student" },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = student.enrollmentNo.ifBlank { "Enrollment not available" },
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = BranchCatalog.canonicalBranch(student.branch)
                                    .ifBlank { "Branch not available" },
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = student.status.ifBlank { "N/A" },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                DetailSection(
                    title = "Basic Information",
                    details = listOf(
                        "Unique ID" to student.uniqueId,
                        "Student Name" to student.name,
                        "Class Roll No" to student.classRollNo,
                        "Enrollment No" to student.enrollmentNo,
                        "Roll No" to student.rollNo,
                        "Date of Birth" to student.dateOfBirth
                    )
                )

                DetailSection(
                    title = "Personal & Contact",
                    details = listOf(
                        "Father Name" to student.father,
                        "Mother Name" to student.mother,
                        "Gender" to student.gender,
                        "Student Phone" to student.studentPhone,
                        "Student Email" to student.email,
                        "Parents Phone" to student.parentsPhone,
                        "Parents Email" to student.parentsEmail,
                        "Religion" to student.religion,
                        "Domicile" to student.domicile,
                        "EWS" to student.ews,
                        "Permanent Address" to
                                student.permanentAddress,
                        "Present Address" to
                                student.presentAddress
                    )
                )

                DetailSection(
                    title = "Academic Information",
                    details = listOf(
                        "Branch" to BranchCatalog.canonicalBranch(student.branch),
                        "Admission Year" to student.admissionYear,
                        "Academic Year" to studentAcademicYear(student.admissionYear),
                        "Rank" to student.rank,
                        "Marks" to student.marks
                    )
                )

                DetailSection(
                    title = "Admission Information",
                    details = listOf(
                        "Eligible Category" to
                                student.eligibleCategory,
                        "Allocated Category" to
                                student.allocatedCategory,
                        "Offline Admission Category" to
                                student.offlineAdmissionCategory,
                        "Admission Round" to student.round,
                        "Admission Year" to
                                student.admissionYear,
                        "Academic Year" to
                                studentAcademicYear(student.admissionYear),
                        "Status" to student.status,
                        "Remarks" to student.remarks
                    )
                )

                DetailSection(
                    title = "Document Information",
                    details = listOf(
                        "Aadhaar No" to student.aadhaarNo,
                        "Aadhaar Linked Mobile" to
                                student.aadhaarLinkedMobile
                    )
                )

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BACK TO STUDENT RECORDS",
                        color = StudentBlue
                    )
                }
            }
        }
    }
}


// ============================================================
// DETAIL SECTION
// ============================================================

@Composable
fun DetailSection(
    title: String,
    details: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(StudentBlue, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudentText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            details.filter { it.second.isNotBlank() }.forEachIndexed { index, item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.second,
                        fontSize = 14.sp,
                        color = StudentText
                    )
                    if (index < details.count { it.second.isNotBlank() } - 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ============================================================
// EDIT STUDENT
// ============================================================

@Composable
fun EditStudentForm(
    students: List<Student>,
    onUpdate: suspend (String, Student) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updating by remember { mutableStateOf(false) }

    var searchId by remember {
        mutableStateOf("")
    }

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var name by remember { mutableStateOf("") }
    var classRollNo by remember { mutableStateOf("") }
    var enrollmentNo by remember { mutableStateOf("") }
    var rollNo by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var showEditDatePicker by remember { mutableStateOf(false) }

    var father by remember { mutableStateOf("") }
    var mother by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var studentPhone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var parentsPhone by remember { mutableStateOf("") }
    var parentsEmail by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }
    var domicile by remember { mutableStateOf("") }
    var ews by remember { mutableStateOf("") }

    var permanentAddress by remember {
        mutableStateOf("")
    }

    var presentAddress by remember {
        mutableStateOf("")
    }

    var branch by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("") }

    var eligibleCategory by remember {
        mutableStateOf("")
    }

    var allocatedCategory by remember {
        mutableStateOf("")
    }

    var offlineAdmissionCategory by remember {
        mutableStateOf("")
    }

    var status by remember { mutableStateOf("") }
    var round by remember { mutableStateOf("") }
    var admissionYear by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val branchOptionsForYear =
        BranchCatalog.optionsForAcademicYear(admissionYear)

    var aadhaarNo by remember {
        mutableStateOf("")
    }

    var aadhaarLinkedMobile by remember {
        mutableStateOf("")
    }


    fun loadStudent(student: Student) {

        selectedStudent = student

        name = student.name
        classRollNo = student.classRollNo
        enrollmentNo = student.enrollmentNo
        rollNo = student.rollNo
        dateOfBirth = student.dateOfBirth

        father = student.father
        mother = student.mother
        gender = student.gender
        studentPhone = student.studentPhone
        email = student.email
        parentsPhone = student.parentsPhone
        parentsEmail = student.parentsEmail
        religion = student.religion
        domicile = when (student.domicile.trim().uppercase()) {
            "Y", "YES", "TRUE", "1" -> "Yes"
            "N", "NO", "FALSE", "0" -> "No"
            else -> ""
        }
        ews = when (student.ews.trim().uppercase()) {
            "Y", "YES", "TRUE", "1" -> "Yes"
            "N", "NO", "FALSE", "0" -> "No"
            else -> student.ews
        }

        permanentAddress = student.permanentAddress
        presentAddress = student.presentAddress

        branch = BranchCatalog.canonicalBranch(student.branch)
        rank = student.rank
        marks = student.marks

        eligibleCategory = student.eligibleCategory.trim().let {
            if (it in categoryOptions) it else ""
        }
        allocatedCategory = student.allocatedCategory.trim().let {
            if (it in categoryOptions) it else ""
        }
        offlineAdmissionCategory =
            student.offlineAdmissionCategory.trim().let {
                if (it in categoryOptions) it else ""
            }

        status = student.status.trim().let {
            if (it in statusOptions) it else ""
        }
        round = student.round.ifBlank { "Not specified" }
        admissionYear = student.admissionYear
        remarks = student.remarks

        aadhaarNo = student.aadhaarNo
        aadhaarLinkedMobile =
            student.aadhaarLinkedMobile
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudentBackground
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            StudentScreenHeader(
                title = "Edit Student",
                subtitle = "Find and update student information",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                FormSectionHeader(
                    "01",
                    "Find Student",
                    "Search using Name, Enrollment No or Unique ID"
                )

                AppTextField(
                    label = "Name / Enrollment No / Unique ID",
                    value = searchId,
                    placeholder = "Enter student name or identifier",
                    onValueChange = {
                        searchId = it
                    }
                )

                Button(
                    onClick = {
                        val query = searchId.trim()
                        if (query.isBlank()) {
                            Toast.makeText(context, "Please enter Student Name, Enrollment No or Unique ID", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                updating = true
                                try {
                                    // Search the server first; the passed-in list may be stale or only a partial page.
                                    val byId = ApiClient.searchStudents(context, uniqueId = query, limit = 100, offset = 0).students
                                    val byEnrollment = if (byId.isEmpty()) ApiClient.searchStudents(context, enrollmentNo = query, limit = 100, offset = 0).students else emptyList()
                                    val byName = if (byId.isEmpty() && byEnrollment.isEmpty()) ApiClient.searchStudents(context, name = query, limit = 100, offset = 0).students else emptyList()
                                    val serverMatches = byId + byEnrollment + byName
                                    val found = serverMatches.firstOrNull { it.uniqueId.equals(query, true) || it.enrollmentNo.equals(query, true) }
                                        ?: serverMatches.firstOrNull { it.name.contains(query, true) }
                                        ?: students.firstOrNull { it.uniqueId.equals(query, true) || it.enrollmentNo.equals(query, true) }
                                        ?: students.firstOrNull { it.name.contains(query, true) }
                                    if (found == null) {
                                        selectedStudent = null
                                        Toast.makeText(context, "Student not found. Check the name/enrollment number and server connection.", Toast.LENGTH_LONG).show()
                                    } else {
                                        loadStudent(found)
                                        Toast.makeText(context, "Student loaded. Please verify details before saving.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    val localFound = students.firstOrNull { it.uniqueId.equals(query, true) || it.enrollmentNo.equals(query, true) }
                                        ?: students.firstOrNull { it.name.contains(query, true) }
                                    if (localFound != null) {
                                        loadStudent(localFound)
                                        Toast.makeText(context, "Loaded from current list; server search failed: ${e.message ?: "connection error"}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Could not search the database: ${e.message ?: "connection error"}", Toast.LENGTH_LONG).show()
                                    }
                                } finally {
                                    updating = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudentBlue
                    )
                ) {
                    Text(
                        text = "SEARCH STUDENT",
                        fontWeight = FontWeight.Bold
                    )
                }


                if (selectedStudent != null) {

                    StudentSelectedCard(
                        student = selectedStudent!!
                    )

                    FormSectionHeader(
                        "02",
                        "Student Identity",
                        "Basic student identification"
                    )

                    AppTextField(
                        label = "Student Name *",
                        value = name,
                        onValueChange = {
                            name = it
                        }
                    )

                    AppTextField(
                        label = "Enrollment No *",
                        value = enrollmentNo,
                        onValueChange = {
                            enrollmentNo = it
                        }
                    )

                    AppTextField(
                        label = "Class Roll No",
                        value = classRollNo,
                        onValueChange = {
                            classRollNo = it
                        }
                    )

                    AppTextField(
                        label = "Roll No",
                        value = rollNo,
                        onValueChange = {
                            rollNo = it
                        }
                    )

                    DateField(
                        label = "Date of Birth",
                        value = dateOfBirth,
                        onClick = { showEditDatePicker = true }
                    )


                    FormSectionHeader(
                        "03",
                        "Personal & Contact",
                        "Student and family information"
                    )

                    AppTextField(
                        label = "Father Name",
                        value = father,
                        onValueChange = {
                            father = it
                        }
                    )

                    AppTextField(
                        label = "Mother Name",
                        value = mother,
                        onValueChange = {
                            mother = it
                        }
                    )

                    DropdownField(
                        label = "Gender",
                        value = gender,
                        options = genderOptions,
                        onValueChange = {
                            gender = it
                        }
                    )

                    AppTextField(
                        label = "Student Phone",
                        value = studentPhone,
                        placeholder = "10 digit mobile number",
                        onValueChange = { value ->

                            if (
                                value.all { it.isDigit() } &&
                                value.length <= 10
                            ) {
                                studentPhone = value
                            }
                        }
                    )

                    AppTextField(
                        label = "Student Email",
                        value = email,
                        placeholder = "example@email.com",
                        onValueChange = {
                            email = it
                        }
                    )

                    AppTextField(
                        label = "Parents Phone",
                        value = parentsPhone,
                        placeholder = "10 digit mobile number",
                        onValueChange = { value ->

                            if (
                                value.all { it.isDigit() } &&
                                value.length <= 10
                            ) {
                                parentsPhone = value
                            }
                        }
                    )

                    AppTextField(
                        label = "Parents Email",
                        value = parentsEmail,
                        placeholder = "example@email.com",
                        onValueChange = {
                            parentsEmail = it
                        }
                    )

                    AppTextField(
                        label = "Religion",
                        value = religion,
                        onValueChange = {
                            religion = it
                        }
                    )

                    DropdownField(
                        label = "Domicile",
                        value = domicile,
                        options = domicileOptions,
                        onValueChange = {
                            domicile = it
                        }
                    )

                    DropdownField(
                        label = "EWS",
                        value = ews,
                        options = ewsOptions,
                        onValueChange = {
                            ews = it
                        }
                    )


                    FormSectionHeader(
                        "04",
                        "Academic Information",
                        "Course and academic details"
                    )

                    DropdownField(
                        label = "Branch *",
                        value = branch,
                        options = branchOptionsForYear,
                        onValueChange = {
                            branch = it
                        }
                    )

                    DropdownField(
                        label = "Admission Year",
                        value = admissionYear,
                        options = admissionYearOptions,
                        onValueChange = {
                            admissionYear = it
                            if (branch.isNotBlank() &&
                                branch !in BranchCatalog.optionsForAcademicYear(it)
                            ) {
                                branch = ""
                            }
                        }
                    )

                    if (admissionYear.isNotBlank()) {
                        Text(
                            text = "Academic Year: ${studentAcademicYear(admissionYear)}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    AppTextField(
                        label = "Rank",
                        value = rank,
                        onValueChange = {
                            rank = it
                        }
                    )

                    AppTextField(
                        label = "Marks",
                        value = marks,
                        onValueChange = {
                            marks = it
                        }
                    )


                    FormSectionHeader(
                        "05",
                        "Admission Information",
                        "Category and admission details"
                    )

                    DropdownField(
                        label = "Eligible Category",
                        value = eligibleCategory,
                        options = categoryOptions,
                        onValueChange = {
                            eligibleCategory = it
                        }
                    )

                    DropdownField(
                        label = "Allocated Category",
                        value = allocatedCategory,
                        options = categoryOptions,
                        onValueChange = {
                            allocatedCategory = it
                        }
                    )

                    AppTextField(
                        label = "Offline Admission Category",
                        value = offlineAdmissionCategory,
                        onValueChange = {
                            offlineAdmissionCategory = it
                        }
                    )

                    DropdownField(
                        label = "Admission Round",
                        value = round,
                        options = listOf("Not specified") + roundOptions,
                        onValueChange = {
                            round = if (it == "Not specified") "" else it
                        }
                    )

                    DropdownField(
                        label = "Status",
                        value = status,
                        options = statusOptions,
                        onValueChange = {
                            status = it
                        }
                    )

                    AppTextField(
                        label = "Remarks",
                        value = remarks,
                        onValueChange = {
                            remarks = it
                        }
                    )


                    FormSectionHeader(
                        "06",
                        "Address & Documents",
                        "Additional student information"
                    )

                    AppTextField(
                        label = "Permanent Address",
                        value = permanentAddress,
                        onValueChange = {
                            permanentAddress = it
                        }
                    )

                    AppTextField(
                        label = "Present Address",
                        value = presentAddress,
                        onValueChange = {
                            presentAddress = it
                        }
                    )

                    AppTextField(
                        label = "Aadhaar No",
                        value = aadhaarNo,
                        onValueChange = { value ->

                            if (
                                value.all { it.isDigit() } &&
                                value.length <= 12
                            ) {
                                aadhaarNo = value
                            }
                        }
                    )

                    AppTextField(
                        label = "Aadhaar Linked Mobile",
                        value = aadhaarLinkedMobile,
                        placeholder = "10 digit mobile number",
                        onValueChange = { value ->

                            if (
                                value.all { it.isDigit() } &&
                                value.length <= 10
                            ) {
                                aadhaarLinkedMobile = value
                            }
                        }
                    )


                    Button(
                        onClick = {

                            val original =
                                selectedStudent

                            when {

                                original == null -> {

                                    Toast.makeText(
                                        context,
                                        "Please search and select a student first",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                name.trim().isBlank() -> {

                                    Toast.makeText(
                                        context,
                                        "Please enter Student Name",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                enrollmentNo.trim().isBlank() -> {

                                    Toast.makeText(
                                        context,
                                        "Please enter Enrollment No",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                branch.isBlank() -> {

                                    Toast.makeText(
                                        context,
                                        "Please select Branch",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                !isValidAdmissionStatus(status) -> {

                                    Toast.makeText(
                                        context,
                                        "Please select Admission Status: Admitted or Cancelled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                studentPhone.isNotBlank() &&
                                        studentPhone.length != 10 -> {

                                    Toast.makeText(
                                        context,
                                        "Student phone must be 10 digits",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                parentsPhone.isNotBlank() &&
                                        parentsPhone.length != 10 -> {

                                    Toast.makeText(
                                        context,
                                        "Parents phone must be 10 digits",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                aadhaarNo.isNotBlank() &&
                                        aadhaarNo.length != 12 -> {

                                    Toast.makeText(
                                        context,
                                        "Aadhaar No must be 12 digits",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                aadhaarLinkedMobile.isNotBlank() &&
                                        aadhaarLinkedMobile.length != 10 -> {

                                    Toast.makeText(
                                        context,
                                        "Aadhaar linked mobile must be 10 digits",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                else -> {

                                    val duplicateEnrollment =
                                        students.any { candidate ->

                                            val sameStudent =
                                                if (
                                                    original.uniqueId.isNotBlank()
                                                ) {
                                                    candidate.uniqueId.trim()
                                                        .equals(
                                                            original.uniqueId.trim(),
                                                            ignoreCase = true
                                                        )
                                                } else {
                                                    candidate.enrollmentNo.trim()
                                                        .equals(
                                                            original.enrollmentNo.trim(),
                                                            ignoreCase = true
                                                        )
                                                }

                                            candidate.enrollmentNo.trim()
                                                .equals(
                                                    enrollmentNo.trim(),
                                                    ignoreCase = true
                                                ) &&
                                                    !sameStudent
                                        }

                                    if (duplicateEnrollment) {

                                        Toast.makeText(
                                            context,
                                            "Another student already has this Enrollment No",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } else {

                                        val updatedStudent =
                                            original.copy(
                                                // Keep the stable student identity unchanged during edit.
                                                uniqueId = original.uniqueId.trim(),

                                                name = name.trim(),
                                                classRollNo =
                                                    classRollNo.trim(),
                                                enrollmentNo =
                                                    enrollmentNo.trim(),
                                                rollNo =
                                                    rollNo.trim(),
                                                dateOfBirth =
                                                    dateOfBirth.trim(),

                                                father =
                                                    father.trim(),
                                                mother =
                                                    mother.trim(),
                                                gender = gender,

                                                studentPhone =
                                                    studentPhone.trim(),

                                                email =
                                                    email.trim(),

                                                parentsPhone =
                                                    parentsPhone.trim(),

                                                parentsEmail =
                                                    parentsEmail.trim(),

                                                religion =
                                                    religion.trim(),

                                                domicile =
                                                    domicile,

                                                ews = ews,

                                                permanentAddress =
                                                    permanentAddress.trim(),

                                                presentAddress =
                                                    presentAddress.trim(),

                                                branch = BranchCatalog.canonicalBranch(branch),

                                                rank =
                                                    rank.trim(),

                                                marks =
                                                    marks.trim(),

                                                eligibleCategory =
                                                    eligibleCategory,

                                                allocatedCategory =
                                                    allocatedCategory,

                                                offlineAdmissionCategory =
                                                    offlineAdmissionCategory.trim(),

                                                status = status,

                                                round = if (round == "Not specified") "" else round,

                                                admissionYear =
                                                    admissionYear,

                                                remarks =
                                                    remarks.trim(),

                                                aadhaarNo =
                                                    aadhaarNo.trim(),

                                                aadhaarLinkedMobile =
                                                    aadhaarLinkedMobile.trim()
                                            )

                                        scope.launch {
                                            updating = true
                                            try {
                                                onUpdate(
                                                    original.studentId?.toString()
                                                        ?: original.enrollmentNo,
                                                    updatedStudent
                                                )
                                                selectedStudent = updatedStudent
                                                Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show()
                                            } catch (e: ApiClient.ApiException) {
                                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                            } finally {
                                                updating = false
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudentBlue
                        )
                    ) {
                        Text(
                            text = "UPDATE STUDENT",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {

                            selectedStudent = null
                            searchId = ""

                            name = ""
                            classRollNo = ""
                            enrollmentNo = ""
                            rollNo = ""
                            dateOfBirth = ""

                            father = ""
                            mother = ""
                            gender = ""
                            studentPhone = ""
                            email = ""
                            parentsPhone = ""
                            parentsEmail = ""

                            religion = ""
                            domicile = ""
                            ews = ""

                            permanentAddress = ""
                            presentAddress = ""

                            branch = ""
                            rank = ""
                            marks = ""

                            eligibleCategory = ""
                            allocatedCategory = ""
                            offlineAdmissionCategory = ""

                            status = ""
                            round = ""
                            admissionYear = ""
                            remarks = ""

                            aadhaarNo = ""
                            aadhaarLinkedMobile = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CLEAR",
                            color = StudentBlue
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BACK TO STUDENT RECORDS",
                        color = StudentBlue
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }

    if (showEditDatePicker) {
        val initialParts = dateOfBirth.split("/")
        val initialDay = initialParts.getOrNull(0)?.toIntOrNull() ?: 1
        val initialMonth = (initialParts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val initialYear = initialParts.getOrNull(2)?.toIntOrNull() ?: 2000
        val editDatePicker = remember(dateOfBirth) {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    dateOfBirth = String.format("%02d/%02d/%04d", day, month + 1, year)
                    showEditDatePicker = false
                },
                initialYear,
                initialMonth.coerceIn(0, 11),
                initialDay.coerceIn(1, 31)
            )
        }
        DisposableEffect(showEditDatePicker) {
            if (showEditDatePicker) editDatePicker.show()
            onDispose { editDatePicker.dismiss() }
        }
    }
}


// ============================================================
// DELETE STUDENT
// ============================================================

@Composable
fun DeleteStudentForm(
    students: List<Student>,
    onDelete: suspend (Student) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }

    var searchId by remember {
        mutableStateOf("")
    }

    var foundStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var showDialog by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = StudentBackground
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            StudentScreenHeader(
                title = "Delete Student",
                subtitle = "Safely remove an existing record",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3F3)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "⚠ Delete carefully",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB3261E)
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Deleting a student will remove the record from the current student list. Verify the student before confirming.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                FormSectionHeader(
                    "01",
                    "Find Student",
                    "Search using Name, Enrollment No or Unique ID"
                )

                AppTextField(
                    label = "Name / Enrollment No / Unique ID",
                    value = searchId,
                    placeholder = "Enter student name or identifier",
                    onValueChange = {
                        searchId = it
                        foundStudent = null
                        showDialog = false
                    }
                )

                Button(
                    onClick = {

                        val query = searchId.trim()

                        if (query.isBlank()) {

                            foundStudent = null

                            Toast.makeText(
                                context,
                                "Please enter Student Name, Enrollment No or Unique ID",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            scope.launch {
                                deleting = true
                                try {
                                    // Search the database, not only the small in-memory cache.
                                    val byUniqueId = ApiClient.searchStudents(
                                        context, uniqueId = query, limit = 100, offset = 0
                                    ).students
                                    val byEnrollment = if (byUniqueId.isEmpty()) {
                                        ApiClient.searchStudents(
                                            context, enrollmentNo = query, limit = 100, offset = 0
                                        ).students
                                    } else emptyList()
                                    val byRoll = if (byUniqueId.isEmpty() && byEnrollment.isEmpty()) {
                                        ApiClient.searchStudents(
                                            context, classRollNo = query, limit = 100, offset = 0
                                        ).students
                                    } else emptyList()
                                    val byName = if (byUniqueId.isEmpty() && byEnrollment.isEmpty() && byRoll.isEmpty()) {
                                        ApiClient.searchStudents(
                                            context, query = query, limit = 100, offset = 0
                                        ).students
                                    } else emptyList()
                                    val allMatches = byUniqueId + byEnrollment + byRoll + byName
                                    foundStudent = allMatches.firstOrNull {
                                        it.uniqueId.trim().equals(query, true) ||
                                            it.enrollmentNo.trim().equals(query, true) ||
                                            it.classRollNo.trim().equals(query, true) ||
                                            it.rollNo.trim().equals(query, true)
                                    } ?: allMatches.firstOrNull {
                                        it.name.trim().contains(query, ignoreCase = true)
                                    } ?: students.firstOrNull {
                                        it.uniqueId.trim().equals(query, true) ||
                                            it.enrollmentNo.trim().equals(query, true) ||
                                            it.classRollNo.trim().equals(query, true) ||
                                            it.rollNo.trim().equals(query, true) ||
                                            it.name.trim().contains(query, ignoreCase = true)
                                    }

                                    if (foundStudent == null) {
                                        Toast.makeText(context, "Student not found", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Student found successfully", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    foundStudent = null
                                    Toast.makeText(
                                        context,
                                        e.message ?: "Unable to search students. Check server connection.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    deleting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudentBlue
                    )
                ) {
                    Text(
                        text = "SEARCH STUDENT",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (foundStudent != null) {

                    FormSectionHeader(
                        "02",
                        "Student Found",
                        "Verify the student before deletion"
                    )

                    StudentDeleteInfoCard(
                        student = foundStudent!!
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFFFF8E1)
                        )
                    ) {

                        Text(
                            text =
                                "Please verify the Enrollment No and student name before deleting.",
                            modifier =
                                Modifier.padding(15.dp),
                            fontSize = 13.sp,
                            color = Color(0xFF6D5200)
                        )
                    }

                    Button(
                        onClick = {
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFFB3261E)
                        )
                    ) {
                        Text(
                            text = "DELETE STUDENT",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (
                    searchId.isNotBlank() ||
                    foundStudent != null
                ) {

                    OutlinedButton(
                        onClick = {
                            searchId = ""
                            foundStudent = null
                            showDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CLEAR SEARCH",
                            color = StudentBlue
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BACK TO STUDENT RECORDS",
                        color = StudentBlue
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }

    if (
        showDialog &&
        foundStudent != null
    ) {

        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },

            title = {
                Text("Delete Student?")
            },

            text = {

                Column {

                    Text(
                        text =
                            "You are about to permanently remove this student record."
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "Student: ${foundStudent!!.name}",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "Enrollment No: ${foundStudent!!.enrollmentNo}"
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "This action cannot be undone.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB3261E)
                    )
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val studentToDelete =
                            foundStudent

                        if (studentToDelete != null) {

                            scope.launch {
                                deleting = true
                                try {
                                    onDelete(studentToDelete)
                                    showDialog = false
                                    foundStudent = null
                                    searchId = ""
                                    Toast.makeText(context, "Student deleted successfully", Toast.LENGTH_SHORT).show()
                                } catch (e: ApiClient.ApiException) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                                } finally {
                                    deleting = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = "DELETE",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB3261E)
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}


// ============================================================
// SELECTED STUDENT CARD
// ============================================================

@Composable
fun StudentSelectedCard(
    student: Student
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = StudentLightBlue
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = student.name.ifBlank {
                    "Student"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = StudentText
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "Enrollment No: ${
                        student.enrollmentNo.ifBlank {
                            "N/A"
                        }
                    }",
                fontSize = 13.sp,
                color = Color.DarkGray
            )

            Text(
                text =
                    "Unique ID: ${
                        student.uniqueId.ifBlank {
                            "N/A"
                        }
                    }",
                fontSize = 13.sp,
                color = Color.DarkGray
            )

            Text(
                text =
                    "Branch: ${
                        BranchCatalog.canonicalBranch(student.branch).ifBlank {
                            "N/A"
                        }
                    }",
                fontSize = 13.sp,
                color = Color.DarkGray
            )

            Text(
                text =
                    "Admission Year: ${
                        student.admissionYear.ifBlank {
                            "N/A"
                        }
                    }",
                fontSize = 13.sp,
                color = Color.DarkGray
            )
        }
    }
}


// ============================================================
// DELETE INFO CARD
// ============================================================

@Composable
fun StudentDeleteInfoCard(
    student: Student
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = student.name.ifBlank {
                    "Unnamed Student"
                },
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = StudentText
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            StudentInfoRow(
                "Enrollment No",
                student.enrollmentNo
            )

            StudentInfoRow(
                "Unique ID",
                student.uniqueId
            )

            StudentInfoRow(
                "Branch",
                BranchCatalog.canonicalBranch(student.branch)
            )

            StudentInfoRow(
                "Admission Year",
                student.admissionYear
            )

            StudentInfoRow(
                "Status",
                student.status
            )
        }
    }
}


// ============================================================
// EMPTY CARD
// ============================================================

@Composable
fun StudentEmptyCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        StudentLightBlue,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = "No students",
                    tint = StudentBlue,
                    modifier = Modifier.size(27.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StudentText
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}


// ============================================================
// STUDENT INFO ROW
// ============================================================

@Composable
fun StudentInfoRow(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = StudentText
            )
        }
    }
}

// ============================================================
// FORM TEXT FIELD
// ============================================================

@Composable
fun AppTextField(
    label: String,
    value: String,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(placeholder)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = StudentBlue,
            unfocusedBorderColor = Color(0xFFD5DAE5),
            focusedLabelColor = StudentBlue
        )
    )
}


// ============================================================
// DATE FIELD
// ============================================================

@Composable
fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(label)
            },
            placeholder = {
                Text("Select date")
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = "Select date",
                    tint = StudentBlue,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable {
                    onClick()
                }
        )
    }
}


// ============================================================
// FORM SECTION HEADER
// ============================================================

@Composable
fun FormSectionHeader(
    number: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = StudentLightBlue,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = number,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudentBlue
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudentText
                )

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// ============================================================
// DROPDOWN FIELD
// ============================================================

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(label)
            },
            placeholder = {
                Text("Select $label")
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Open $label",
                    tint = StudentBlue,
                    modifier = Modifier.size(22.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable {
                    expanded = true
                }
        )

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

                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}