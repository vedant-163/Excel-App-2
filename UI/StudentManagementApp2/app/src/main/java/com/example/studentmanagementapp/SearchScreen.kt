package com.example.studentmanagementapp

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject


@Composable
fun SearchScreen(
    students: SnapshotStateList<Student>,
    onBack: () -> Unit
) {

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var uniqueId by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var enrollmentNo by remember { mutableStateOf("") }
    var classRollNo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }

    var searchClicked by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resultStudents by remember { mutableStateOf<List<Student>>(emptyList()) }
    var resultCount by remember { mutableStateOf(0) }
    var selectedDetail by remember { mutableStateOf<JSONObject?>(null) }
    var selectedTermFees by remember { mutableStateOf<JSONArray?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }
    var selectedStudentKey by remember { mutableStateOf("") }

    val hasSearchCriteria =
        uniqueId.isNotBlank() ||
                studentName.isNotBlank() ||
                enrollmentNo.isNotBlank() ||
                classRollNo.isNotBlank() ||
                branch.isNotBlank()

    fun performSearch() {
        if (!hasSearchCriteria) return

        searchClicked = true
        loading = true
        errorMessage = null

        scope.launch {
            try {
                val response = ApiClient.searchStudents(
                    context = context,
                    uniqueId = uniqueId,
                    name = studentName,
                    enrollmentNo = enrollmentNo,
                    classRollNo = classRollNo,
                    branch = branch
                )
                resultStudents = response.students
                resultCount = response.totalCount

            } catch (e: ApiClient.ApiException) {
                resultStudents = emptyList()
                resultCount = 0
                errorMessage = e.message
            } catch (e: Exception) {
                resultStudents = emptyList()
                resultCount = 0
                errorMessage = e.message ?: "Unable to search students."
            } finally {
                loading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F7FB)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onBack() }
                                .padding(horizontal = 13.dp, vertical = 9.dp)
                        ) {
                            Text("←", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Search Students", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Search the college database securely",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SearchInfoCard(totalStudents = students.size)

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Search Student", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "Search is performed on the server, so it is not limited to the first 100 records.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    SearchFieldLabel("Unique ID")
                    SearchInputField(
                        value = uniqueId,
                        onValueChange = { uniqueId = it; searchClicked = false },
                        label = "Unique ID",
                        placeholder = "Enter unique ID"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SearchFieldLabel("Student Name")
                    SearchInputField(
                        value = studentName,
                        onValueChange = { studentName = it; searchClicked = false },
                        label = "Student Name",
                        placeholder = "Enter student name"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SearchFieldLabel("Enrollment Number")
                    SearchInputField(
                        value = enrollmentNo,
                        onValueChange = { enrollmentNo = it; searchClicked = false },
                        label = "Enrollment No.",
                        placeholder = "Enter enrollment number"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SearchFieldLabel("Class Roll Number")
                    SearchInputField(
                        value = classRollNo,
                        onValueChange = { classRollNo = it; searchClicked = false },
                        label = "Class Roll No.",
                        placeholder = "Enter class roll number"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SearchFieldLabel("Course / Branch")
                    SearchInputField(
                        value = branch,
                        onValueChange = { branch = it; searchClicked = false },
                        label = "Branch",
                        placeholder = "Enter course / branch"
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { performSearch() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            enabled = hasSearchCriteria && !loading
                        ) {
                            if (loading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Search", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                uniqueId = ""
                                studentName = ""
                                enrollmentNo = ""
                                classRollNo = ""
                                branch = ""
                                searchClicked = false
                                resultStudents = emptyList()
                                resultCount = 0
                                errorMessage = null
                                selectedDetail = null
                                selectedTermFees = null
                                detailError = null
                                selectedStudentKey = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE9EEF5),
                                contentColor = Color(0xFF374151)
                            )
                        ) {
                            Text("Clear", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                !searchClicked -> SearchEmptyState(totalStudents = students.size)
                loading -> SearchLoadingCard()
                errorMessage != null -> SearchErrorCard(errorMessage!!)
                !hasSearchCriteria -> SearchNoCriteriaCard()
                resultStudents.isEmpty() -> NoSearchResultCard()
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Search Results",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF172033),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$resultCount found",
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        resultStudents.forEach { student ->
                            SearchStudentResultCard(
                                student = student,
                                onViewFullDetails = {
                                    val key = student.enrollmentNo.ifBlank { student.studentId?.toString().orEmpty() }
                                    if (key.isNotBlank()) {
                                        selectedStudentKey = key
                                        selectedDetail = null
                                        selectedTermFees = null
                                        detailError = null
                                        detailLoading = true
                                        scope.launch {
                                            try {
                                                selectedDetail = ApiClient.getStudentFullDetails(context, key)
                                                selectedTermFees = ApiClient.getStudentTermFees(context, key)
                                            } catch (e: Exception) {
                                                detailError = e.message ?: "Unable to load full student details."
                                            } finally {
                                                detailLoading = false
                                            }
                                        }
                                    } else {
                                        detailError = "This search result does not include a student ID or enrollment number."
                                    }
                                }
                            )
                        }
                    }
                    if (detailLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SearchLoadingCard()
                    }
                    detailError?.let { SearchErrorCard(it) }
                    selectedDetail?.let { detail ->
                        Spacer(modifier = Modifier.height(16.dp))
                        FullStudentDetailsCard(detail = detail, termFees = selectedTermFees)
                    }
                }
            }

            Spacer(modifier = Modifier.height(25.dp))
        }
    }
}

@Composable
private fun SearchLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF1565C0))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Searching student records...", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun SearchErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Search failed", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB42318))
            Spacer(modifier = Modifier.height(7.dp))
            Text(message, fontSize = 13.sp, color = Color.Gray)
        }
    }
}


// =====================================================
// FIELD LABEL
// =====================================================

@Composable
fun SearchFieldLabel(
    text: String
) {

    Column {

        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151)
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )
    }
}


// =====================================================
// INFO CARD
// =====================================================

@Composable
fun SearchInfoCard(
    totalStudents: Int
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFFEAF2FF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(
                        horizontal = 15.dp,
                        vertical = 11.dp
                    )
            ) {

                Text(
                    text = "$totalStudents",
                    color = Color(0xFF1565C0),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            Column {

                Text(
                    text = "Student Records",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF172033)
                )

                Text(
                    text = "Records currently available for search",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// =====================================================
// SEARCH INPUT
// =====================================================

@Composable
fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(text = label)
        },
        placeholder = {
            Text(text = placeholder)
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}


// =====================================================
// EMPTY SEARCH STATE
// =====================================================

@Composable
fun SearchEmptyState(
    totalStudents: Int
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Search Student Records",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172033)
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            Text(
                text = if (totalStudents == 0) {
                    "No student records have been added yet."
                } else {
                    "Enter any student detail above and press Search."
                },
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}


// =====================================================
// NO SEARCH CRITERIA
// =====================================================

@Composable
fun SearchNoCriteriaCard() {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Enter Search Details",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172033)
            )


            Spacer(
                modifier = Modifier.height(7.dp)
            )


            Text(
                text = "Please enter at least one field before searching.",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}


// =====================================================
// NO RESULT
// =====================================================

@Composable
fun NoSearchResultCard() {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "No Record Found",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172033)
            )


            Spacer(
                modifier = Modifier.height(7.dp)
            )


            Text(
                text = "No student matches the entered search details.",
                fontSize = 13.sp,
                color = Color.Gray
            )


            Spacer(
                modifier = Modifier.height(5.dp)
            )


            Text(
                text = "Try checking the spelling or search using another field.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}


// =====================================================
// STUDENT RESULT CARD
// =====================================================

@Composable
fun SearchStudentResultCard(
    student: Student,
    onViewFullDetails: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            // =================================================
            // STUDENT HEADER
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFEAF2FF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(
                            horizontal = 13.dp,
                            vertical = 11.dp
                        )
                ) {

                    Text(
                        text = "ST",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }


                Spacer(
                    modifier = Modifier.width(12.dp)
                )


                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = student.name.ifBlank {
                            "Unnamed Student"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF172033)
                    )


                    Text(
                        text = student.branch.ifBlank {
                            "Branch not available"
                        },
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =================================================
            // BASIC INFORMATION
            // =================================================

            SearchSectionTitle("Basic Information")

            SearchDetailRow(
                "Unique ID",
                student.uniqueId
            )

            SearchDetailRow(
                "Student Name",
                student.name
            )

            SearchDetailRow(
                "Class Roll No.",
                student.classRollNo
            )

            SearchDetailRow(
                "Enrollment No.",
                student.enrollmentNo
            )

            SearchDetailRow(
                "Date of Birth",
                student.dateOfBirth
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // PERSONAL & CONTACT
            // =================================================

            SearchSectionTitle(
                "Personal & Contact Information"
            )

            SearchDetailRow(
                "Father's Name",
                student.father
            )

            SearchDetailRow(
                "Mother's Name",
                student.mother
            )

            SearchDetailRow(
                "Gender",
                student.gender
            )

            SearchDetailRow(
                "Student Phone",
                student.studentPhone
            )

            SearchDetailRow(
                "Student Email",
                student.email
            )

            SearchDetailRow(
                "Parents Phone",
                student.parentsPhone
            )

            SearchDetailRow(
                "Parents Email",
                student.parentsEmail
            )

            SearchDetailRow(
                "Religion",
                student.religion
            )

            SearchDetailRow(
                "Domicile",
                student.domicile
            )

            SearchDetailRow(
                "EWS",
                student.ews
            )

            SearchDetailRow(
                "Permanent Address",
                student.permanentAddress
            )

            SearchDetailRow(
                "Present Address",
                student.presentAddress
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // ACADEMIC
            // =================================================

            SearchSectionTitle(
                "Academic Information"
            )

            SearchDetailRow(
                "Branch / Course",
                student.branch
            )

            SearchDetailRow(
                "Rank",
                student.rank
            )

            SearchDetailRow(
                "Marks",
                student.marks
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // ADMISSION
            // =================================================

            SearchSectionTitle(
                "Admission Information"
            )

            SearchDetailRow(
                "Eligible Category",
                student.eligibleCategory
            )

            SearchDetailRow(
                "Allocated Category",
                student.allocatedCategory
            )

            SearchDetailRow(
                "Offline Admission Category",
                student.offlineAdmissionCategory
            )

            SearchDetailRow(
                "Admission Status",
                student.status
            )

            SearchDetailRow(
                "Admission Round",
                student.round
            )

            SearchDetailRow(
                "Admission Year",
                student.admissionYear
            )

            SearchDetailRow(
                "Academic Year",
                AdmissionCatalog.academicYearForAdmissionYear(
                    student.admissionYear
                )
            )

            SearchDetailRow(
                "Remarks",
                student.remarks
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            // =================================================
            // DOCUMENTS
            // =================================================

            SearchSectionTitle(
                "Document Information"
            )

            SearchDetailRow(
                "Aadhaar No.",
                student.aadhaarNo
            )

            SearchDetailRow(
                "Aadhaar Linked Mobile",
                student.aadhaarLinkedMobile
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onViewFullDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("Load Complete Database Details", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun FullStudentDetailsCard(detail: JSONObject, termFees: JSONArray?) {
    fun value(key: String): String {
        val raw = detail.opt(key)
        return if (raw == null || raw == JSONObject.NULL) "" else raw.toString().takeUnless { it == "null" }.orEmpty()
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Complete Student Profile", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Text("Full details loaded from the college database", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            SearchSectionTitle("Identification & Academic Details")
            SearchDetailRow("Serial No.", value("sr_no"))
            SearchDetailRow("Student ID", value("student_id"))
            SearchDetailRow("Unique ID", value("college_unique_id"))
            SearchDetailRow("Class Roll No.", value("class_roll_no"))
            SearchDetailRow("Enrollment No.", value("enrolment_no"))
            SearchDetailRow("Roll No.", value("exam_roll_no"))
            SearchDetailRow("Student Name", value("name"))
            SearchDetailRow("Father Name", value("father_name"))
            SearchDetailRow("Mother Name", value("mother_name"))
            SearchDetailRow("Branch Code", value("branch_code"))
            SearchDetailRow("Branch Name", value("branch_name"))
            SearchDetailRow("Rank", value("rank_text"))
            SearchDetailRow("Marks", value("marks"))
            SearchDetailRow("Date of Birth", value("date_of_birth"))

            Spacer(modifier = Modifier.height(10.dp))
            SearchSectionTitle("Admission Details")
            SearchDetailRow("Eligible Category", value("eligible_category"))
            SearchDetailRow("Allotted Category", value("alloted_category"))
            SearchDetailRow("Offline Category", value("offline_category"))
            SearchDetailRow("Domicile", value("domicile"))
            SearchDetailRow("Gender", value("gender"))
            SearchDetailRow("EWS", value("ews"))
            SearchDetailRow("Religion", value("religion"))
            SearchDetailRow("Admission Year", value("admission_year"))
            SearchDetailRow("Academic Year Start", value("admission_year_start"))
            SearchDetailRow("Admission No.", value("admission_no"))
            SearchDetailRow("Status", value("admission_status"))
            SearchDetailRow("Round", value("admission_round"))
            SearchDetailRow("Remarks", value("remarks"))
            SearchDetailRow("Remarks Details", value("remarks_raw"))

            Spacer(modifier = Modifier.height(10.dp))
            SearchSectionTitle("Contact & Address")
            SearchDetailRow("Student Phone", value("student_phone"))
            SearchDetailRow("Student Email", value("student_email"))
            SearchDetailRow("Parent Phone", value("parents_phone"))
            SearchDetailRow("Parent Email", value("parents_email"))
            SearchDetailRow("Present Address", value("present_address"))
            SearchDetailRow("Permanent Address", value("permanent_address"))
            SearchDetailRow("Aadhaar No.", value("aadhaar_no"))
            SearchDetailRow("Aadhaar Linked Mobile", value("aadhaar_linked_mobile"))

            Spacer(modifier = Modifier.height(10.dp))
            SearchSectionTitle("Fee Summary")
            SearchDetailRow("Fee Structure / MPDTE Fee Code", value("fee_structure"))
            SearchDetailRow("Total Amount", value("total_amount"))
            SearchDetailRow("Total Submitted Amount", value("student_submitted_amount"))
            SearchDetailRow("Paid Amount", value("paid_amount"))
            SearchDetailRow("Pending Amount", value("pending_amount"))

            val payments = detail.optJSONArray("payments") ?: JSONArray()
            SearchSectionTitle("Payment Records (${payments.length()})")
            if (payments.length() == 0) {
                Text("No payment records returned for this student.", fontSize = 13.sp, color = Color.Gray)
            } else {
                for (i in 0 until payments.length()) {
                    val payment = payments.optJSONObject(i) ?: continue
                    Text("Payment ${i + 1}", fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0), modifier = Modifier.padding(top = 8.dp))
                    SearchDetailRow("Academic Year", payment.optString("academic_year", ""))
                    SearchDetailRow("Fee Category", payment.optString("fee_category", ""))
                    SearchDetailRow("Semester", payment.optString("semester_no", ""))
                    SearchDetailRow("Total Amount", payment.optString("total_amount", ""))
                    SearchDetailRow("Submitted Amount", payment.optString("student_submitted_amount", ""))
                    SearchDetailRow("MPDTE / DTE Fee", payment.optString("dte_fee", ""))
                    SearchDetailRow("Transaction ID", payment.optString("online_application_txn_id", ""))
                    SearchDetailRow("Payment Mode", payment.optString("payment_mode", "").ifBlank { payment.optString("payment_mode_raw", "") })
                    SearchDetailRow("Payment Date", payment.optString("payment_date", "").ifBlank { payment.optString("payment_date_raw", "") })
                    SearchDetailRow("Payment Status", payment.optString("payment_status", ""))
                    SearchDetailRow("Payment Reference", payment.optString("payment_reference_no", ""))
                    SearchDetailRow("Pending Amount", payment.optString("pending_amount", ""))
                }
            }

            val fees = termFees ?: JSONArray()
            Spacer(modifier = Modifier.height(10.dp))
            SearchSectionTitle("Semester-wise Scheduled Fee Records (${fees.length()})")
            if (fees.length() == 0) {
                Text("No semester-wise scheduled fee records returned for this student.", fontSize = 13.sp, color = Color.Gray)
            } else {
                for (i in 0 until fees.length()) {
                    val fee = fees.optJSONObject(i) ?: continue
                    Text("${fee.optString("term_code", "Term ${i + 1}")} — ${fee.optString("scheduled_fee", "0")}", fontSize = 13.sp, color = Color(0xFF172033), modifier = Modifier.padding(vertical = 4.dp))
                    SearchDetailRow("Calendar Year", fee.optString("calendar_year", ""))
                    SearchDetailRow("Term Half", fee.optString("term_half", ""))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sensitive credentials are intentionally not displayed here.", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// =====================================================
// SECTION TITLE
// =====================================================

@Composable
fun SearchSectionTitle(
    text: String
) {

    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1565C0),
        modifier = Modifier.padding(
            top = 2.dp,
            bottom = 6.dp
        )
    )
}


// =====================================================
// DETAIL ROW
// =====================================================

@Composable
fun SearchDetailRow(
    label: String,
    value: String
) {

    if (value.isNotBlank()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.Top
        ) {

            Text(
                text = label,
                modifier = Modifier.width(125.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )


            Text(
                text = value,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = Color(0xFF172033)
            )
        }
    }
}
