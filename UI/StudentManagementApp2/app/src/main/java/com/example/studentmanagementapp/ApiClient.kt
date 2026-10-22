package com.example.studentmanagementapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {

    // Emulator: 10.0.2.2 points to the development PC.
    // Physical device: replace with the PC's LAN IP.
    // Production: use an HTTPS URL.
    var BASE_URL = "http://192.168.1.77:8000"

    class ApiException(
        val statusCode: Int,
        override val message: String
    ) : Exception(message)

    data class LoginResult(
        val accessToken: String,
        val expiresIn: Int,
        val username: String,
        val role: String
    )

    private fun openConnection(
        context: Context,
        path: String,
        method: String,
        authenticated: Boolean = true
    ): HttpURLConnection {
        val conn = URL("$BASE_URL$path").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 8000
        conn.readTimeout = 30000
        conn.setRequestProperty("Accept", "application/json")

        if (authenticated) {
            val token = AuthStorage.getToken(context)
                ?: throw ApiException(401, "Sign in required")
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        return conn
    }

    private fun responseText(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream ?: conn.inputStream
        }
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }

    private fun serverMessage(response: String, fallback: String): String {
        return runCatching {
            JSONObject(response).optString("message").ifBlank { fallback }
        }.getOrDefault(fallback)
    }

    suspend fun login(
        context: Context,
        username: String,
        password: String
    ): LoginResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/auth/login", "POST", authenticated = false)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            val body = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body.toString())
            }

            val code = conn.responseCode
            val response = responseText(conn)

            if (code !in 200..299) {
                throw ApiException(
                    code,
                    serverMessage(response, "Login failed.")
                )
            }

            val json = JSONObject(response)
            LoginResult(
                accessToken = json.getString("access_token"),
                expiresIn = json.getInt("expires_in"),
                username = json.getString("username"),
                role = json.getString("role")
            )
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun requestPasswordReset(context: Context, email: String): String = withContext(Dispatchers.IO) {
        postPublicAuthRequest(
            context = context,
            path = "/auth/forgot-password",
            body = JSONObject().put("email", email.trim())
        )
    }

    suspend fun resetPassword(
        context: Context,
        email: String,
        code: String,
        newPassword: String
    ): String = withContext(Dispatchers.IO) {
        postPublicAuthRequest(
            context = context,
            path = "/auth/reset-password",
            body = JSONObject().put("email", email.trim())
                .put("code", code.trim())
                .put("new_password", newPassword)
        )
    }

    private fun postPublicAuthRequest(
        context: Context,
        path: String,
        body: JSONObject
    ): String {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, path, "POST", authenticated = false)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val code = conn.responseCode
            val response = responseText(conn)
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Password recovery failed."))
            }
            return serverMessage(response, "Request completed.")
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getDashboardStudentCount(context: Context): Int = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/dashboard", "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to load dashboard."))
            }
            JSONObject(response)
                .optJSONObject("data")
                ?.optJSONObject("totals")
                ?.optInt("student_count", 0) ?: 0
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getStudents(
        context: Context,
        limit: Int = 100,
        offset: Int = 0
    ): List<Student> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(
                context,
                "/students?limit=$limit&offset=$offset",
                "GET"
            )

            val code = conn.responseCode
            val response = responseText(conn)

            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(
                    code,
                    serverMessage(response, "Unable to load students.")
                )
            }

            val studentList = mutableListOf<Student>()
            val resultsArray = JSONObject(response).optJSONArray("results") ?: JSONArray()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                studentList.add(
                    Student(
                        studentId = item.optInt("student_id", 0).takeIf { it > 0 },
                        uniqueId = item.optString("college_unique_id", ""),
                        name = item.optString("name", ""),
                        classRollNo = item.optString("class_roll_no", ""),
                        enrollmentNo = item.optString("enrolment_no", ""),
                        rollNo = item.optString("exam_roll_no", ""),
                        dateOfBirth = item.optString("date_of_birth", ""),
                        gender = item.optString("gender", ""),
                        studentPhone = item.optString("student_phone", ""),
                        religion = item.optString("religion", ""),
                        domicile = item.optString("domicile", ""),
                        ews = item.optString("ews", ""),
                        branch = item.optString("branch_code", ""),
                        eligibleCategory = item.optString("eligible_category", ""),
                        allocatedCategory = item.optString("alloted_category", "").takeUnless { it == "null" } ?: "",
                        status = item.optString("admission_status", "").takeUnless { it == "null" } ?: "",
                        round = AdmissionCatalog.normalizeAdmissionRound(item.optString("admission_round", "").takeUnless { it == "null" } ?: ""),
                        remarks = item.optString("remarks", ""),
                        admissionYear = item.optString("admission_year", "").takeUnless { it == "null" } ?: ""
                    )
                )
            }
            studentList
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun searchStudents(
        context: Context,
        uniqueId: String = "",
        name: String = "",
        enrollmentNo: String = "",
        classRollNo: String = "",
        branch: String = "",
        limit: Int = 30,
        offset: Int = 0,
        query: String = "",
        admissionYear: String = "",
        eligibleCategory: String = "",
        allottedCategory: String = "",
        gender: String = "",
        ews: String = "",
        domicile: String = "",
        religion: String = "",
        remarks: String = "",
        status: String = "",
        sort: String = ""
    ): SearchResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val params = mutableListOf<String>()
            fun add(name: String, value: String) {
                if (value.isNotBlank()) {
                    params += "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value.trim(), "UTF-8")}"
                }
            }
            add("unique_id", uniqueId)
            add("name", name)
            add("enrolment_no", enrollmentNo)
            add("class_roll_no", classRollNo)
            add("branch", if (branch.isBlank()) "" else BranchCatalog.branchCode(branch))
            add("q", query)
            add("admission_year", admissionYear)
            add("eligible_category", eligibleCategory)
            add("alloted_category", allottedCategory)
            add("gender", gender)
            add("ews", ews)
            add("domicile", domicile)
            add("religion", religion)
            add("remarks", remarks)
            add("status", status)
            add("sort", sort)
            params += "limit=$limit"
            params += "offset=$offset"

            conn = openConnection(
                context,
                "/students?${params.joinToString("&")}",
                "GET"
            )

            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to search students."))
            }

            val json = JSONObject(response)
            val array = json.optJSONArray("results") ?: JSONArray()
            val result = mutableListOf<Student>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                result += Student(
                    studentId = item.optInt("student_id", 0).takeIf { it > 0 },
                    uniqueId = item.optString("college_unique_id", ""),
                    name = item.optString("name", ""),
                    classRollNo = item.optString("class_roll_no", ""),
                    enrollmentNo = item.optString("enrolment_no", ""),
                    rollNo = item.optString("exam_roll_no", ""),
                    branch = item.optString("branch_code", ""),
                    feeStructureCode = item.optString("fee_structure", "").takeUnless { it == "null" } ?: "",
                    eligibleCategory = item.optString("eligible_category", ""),
                    allocatedCategory = item.optString("alloted_category", "").takeUnless { it == "null" } ?: "",
                    gender = item.optString("gender", ""),
                    ews = item.optString("ews", ""),
                    domicile = item.optString("domicile", ""),
                    religion = item.optString("religion", ""),
                    admissionYear = item.optString("admission_year", "").takeUnless { it == "null" } ?: "",
                    status = item.optString("admission_status", "").takeUnless { it == "null" } ?: "",
                    round = AdmissionCatalog.normalizeAdmissionRound(item.optString("admission_round", "").takeUnless { it == "null" } ?: ""),
                    remarks = item.optString("remarks", ""),
                    studentPhone = item.optString("student_phone", "")
                )
            }
            SearchResult(
                totalCount = json.optInt("count", result.size),
                students = result
            )
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getStudentFullDetails(context: Context, studentKey: String): JSONObject = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encodedKey = URLEncoder.encode(studentKey, "UTF-8")
            conn = openConnection(context, "/students/$encodedKey", "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to load student details."))
            JSONObject(response).optJSONObject("data") ?: throw ApiException(code, "Student details were empty.")
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server while loading student details.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getStudentTermFees(context: Context, studentKey: String): JSONArray = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encodedKey = URLEncoder.encode(studentKey, "UTF-8")
            conn = openConnection(context, "/students/$encodedKey/fees", "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to load semester-wise fee records."))
            JSONObject(response).optJSONArray("data") ?: JSONArray()
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server while loading fee records.")
        } finally {
            conn?.disconnect()
        }
    }

    data class SearchResult(
        val totalCount: Int,
        val students: List<Student>
    )

    data class ReportGroupCount(val group: String, val count: Int)

    data class ReportGroupResult(val total: Int, val groups: List<ReportGroupCount>)

    /**
     * Server-side aggregate for the Reports screen (GET /reports/group-counts).
     * groupBy: branch | category | status | ews | domicile | religion.
     * Counts are computed by MySQL, so no student rows are downloaded.
     */
    suspend fun getReportGroupCounts(
        context: Context,
        groupBy: String,
        admissionYear: String? = null,
        branch: String? = null,
        eligibleCategory: String? = null
    ): ReportGroupResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val params = mutableListOf("group_by=${URLEncoder.encode(groupBy, "UTF-8")}")
            fun add(name: String, value: String?) {
                if (!value.isNullOrBlank()) {
                    params += "$name=${URLEncoder.encode(value.trim(), "UTF-8")}"
                }
            }
            add("year", admissionYear)
            add("branch", branch)
            add("eligible_category", eligibleCategory)

            conn = openConnection(context, "/reports/group-counts?${params.joinToString("&")}", "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to load report counts."))
            }
            val json = JSONObject(response)
            val array = json.optJSONArray("data") ?: JSONArray()
            val groups = mutableListOf<ReportGroupCount>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                groups += ReportGroupCount(
                    group = if (item.isNull("group")) "" else item.optString("group", ""),
                    count = item.optInt("student_count", 0)
                )
            }
            ReportGroupResult(json.optInt("total", groups.sumOf { it.count }), groups)
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getReportStudents(
        context: Context,
        admissionYear: String? = null,
        branch: String? = null,
        eligibleCategory: String? = null
    ): List<Student> = withContext(Dispatchers.IO) {
        val all = mutableListOf<Student>()
        // Reports previously downloaded every 100 rows in a serial loop. Each
        // page repeated the expensive report view/count query, causing long waits.
        // The reports endpoint now supports a single bounded request for the
        // complete current student dataset (up to 10,000 rows).
        val pageSize = 10000
        var offset = 0
        var total = Int.MAX_VALUE
        while (offset < total) {
            val params = mutableListOf<String>()
            fun add(name: String, value: String?) {
                if (!value.isNullOrBlank()) {
                    params += "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value.trim(), "UTF-8")}"
                }
            }
            add("year", admissionYear)
            add("branch", branch)
            add("category", eligibleCategory)
            params += "limit=$pageSize"
            params += "offset=$offset"

            var conn: HttpURLConnection? = null
            try {
                conn = openConnection(context, "/reports/students?${params.joinToString("&")}", "GET")
                val code = conn.responseCode
                val response = responseText(conn)
                if (code == 401) {
                    AuthStorage.clear(context)
                    throw ApiException(401, "Session expired. Please sign in again.")
                }
                if (code !in 200..299) {
                    throw ApiException(code, serverMessage(response, "Unable to load report data."))
                }
                val json = JSONObject(response)
                val array = json.optJSONArray("results") ?: JSONArray()
                total = json.optInt("count", all.size + array.length())
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    all += Student(
                        studentId = item.optInt("student_id", 0).takeIf { it > 0 },
                        uniqueId = item.optString("college_unique_id", ""),
                        name = item.optString("name", ""),
                        classRollNo = item.optString("class_roll_no", ""),
                        enrollmentNo = item.optString("enrolment_no", ""),
                        rollNo = item.optString("class_roll_no", ""),
                        branch = item.optString("branch_code", ""),
                        eligibleCategory = item.optString("eligible_category", ""),
                        allocatedCategory = item.optString("alloted_category", "").takeUnless { it == "null" } ?: "",
                        gender = item.optString("gender", ""),
                        ews = item.optString("ews", ""),
                        domicile = item.optString("domicile", ""),
                        religion = item.optString("religion", ""),
                        admissionYear = item.optString("admission_year", "").takeUnless { it == "null" } ?: "",
                        status = item.optString("admission_status", "").takeUnless { it == "null" } ?: "",
                        round = AdmissionCatalog.normalizeAdmissionRound(item.optString("admission_round", "").takeUnless { it == "null" } ?: ""),
                        remarks = item.optString("remarks", ""),
                        studentPhone = item.optString("student_phone", "")
                    )
                }
                if (array.length() == 0) break
                offset += array.length()
            } finally {
                conn?.disconnect()
            }
        }
        all
    }

    /** Reads Allocated Category options from the alloted_category reference table. */
    suspend fun getAllotedCategories(context: Context): List<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/lookups/alloted-categories", "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to load Allocated Category options."))
            }
            val array = JSONObject(response).optJSONArray("data") ?: JSONArray()
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val codeValue = item.optString("code", "").trim()
                    if (codeValue.isNotBlank()) add(codeValue)
                }
            }.distinct()
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to load Allocated Category options from server.")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Loads all students for Admission Records/Summary. The current backend accepts
     * a large bounded page, avoiding dozens of serial requests. If a server/proxy
     * still caps responses, continue by the actual returned row count; do not treat
     * a short non-empty page as the end. A no-progress guard prevents infinite loops.
     */
    suspend fun getAllStudents(context: Context, pageSize: Int = 10000): List<Student> = withContext(Dispatchers.IO) {
        val all = mutableListOf<Student>()
        val seenKeys = mutableSetOf<String>()
        var offset = 0

        while (true) {
            val page = getStudents(context, limit = pageSize, offset = offset)
            if (page.isEmpty()) break

            var addedThisPage = 0
            for (student in page) {
                val key = when {
                    student.studentId != null -> "id:${student.studentId}"
                    student.enrollmentNo.isNotBlank() -> "enrol:${student.enrollmentNo.trim().lowercase()}"
                    student.uniqueId.isNotBlank() -> "uid:${student.uniqueId.trim().lowercase()}"
                    else -> "row:${offset + addedThisPage}"
                }
                if (seenKeys.add(key)) {
                    all.add(student)
                    addedThisPage++
                }
            }

            // Avoid an infinite loop if a backend ignores offset and repeats a page.
            if (addedThisPage == 0) break
            offset += page.size
        }

        all
    }

    suspend fun getAdmissionRounds(context: Context): List<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/lookups/admission-rounds", "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to load admission rounds."))
            }
            val array = JSONObject(response).optJSONArray("data") ?: JSONArray()
            val rounds = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = item.optString("name", "").trim()
                val codeValue = item.optString("code", "").trim()
                val label = AdmissionCatalog.normalizeAdmissionRound(name.ifBlank { codeValue })
                if (label.isNotBlank()) rounds += label
            }
            rounds.distinct()
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to load admission rounds from server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getAcademicYears(): List<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL("$BASE_URL/public/lookups/academic-years").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 30000
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            val response = responseText(conn)
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to load academic years."))
            }

            val array = JSONObject(response).optJSONArray("data") ?: JSONArray()
            val years = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val year = array.optString(i, "").trim()
                if (year.isNotBlank()) years.add(year)
            }
            years
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to load academic years from server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun createStudent(
        context: Context,
        student: Student
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/students", "POST")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            val json = studentJson(student)

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(json.toString())
            }

            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(
                    code,
                    serverMessage(response, "Unable to create student.")
                )
            }

            // Do not report success merely because the HTTP request returned 2xx.
            // The FastAPI create endpoint commits the MySQL transaction and returns
            // the persisted student in `data`; verify that response before the UI
            // displays “Student saved successfully”.
            val result = try {
                JSONObject(response)
            } catch (_: Exception) {
                throw ApiException(code, "Server response was invalid; student save could not be verified.")
            }
            val savedStudent = result.optJSONObject("data")
            val savedId = savedStudent?.optInt("student_id", 0) ?: 0
            if (!result.optBoolean("success", false) || savedId <= 0) {
                throw ApiException(code, serverMessage(response, "Server did not confirm that the student was saved to the database."))
            }
            true
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }
    suspend fun updateStudent(
        context: Context,
        studentKey: String,
        student: Student
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(
                context,
                "/students/${URLEncoder.encode(studentKey, "UTF-8")}",
                "PUT"
            )
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val json = studentJson(student)
            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to update student."))
            }
            // Confirm the backend returned the persisted student before the UI
            // reports success or refreshes its local list.
            val result = try {
                JSONObject(response)
            } catch (_: Exception) {
                throw ApiException(code, "Server response was invalid; student update could not be verified.")
            }
            val savedStudent = result.optJSONObject("data")
            val savedId = savedStudent?.optInt("student_id", 0) ?: 0
            if (!result.optBoolean("success", false) || savedId <= 0) {
                throw ApiException(code, serverMessage(response, "Server did not confirm that the student was updated."))
            }
            true
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun updateAdmission(
        context: Context,
        studentKey: String,
        admissionYear: String,
        branch: String,
        eligibleCategory: String,
        allocatedCategory: String,
        offlineCategory: String,
        round: String,
        status: String,
        remarks: String
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(
                context,
                "/admissions/${URLEncoder.encode(studentKey, "UTF-8")}",
                "PUT"
            )
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            val json = JSONObject().apply {
                put("admission_year", admissionYear)
                put("branch", branch)
                put("eligible_category", eligibleCategory)
                put("allocated_category", allocatedCategory)
                put("offline_category", offlineCategory)
                put("round", round.trim().takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                put("status", status)
                put("remarks", remarks)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }

            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(
                    code,
                    serverMessage(response, "Unable to save admission details.")
                )
            }
            true
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun deleteStudent(
        context: Context,
        studentKey: String
    ): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(
                context,
                "/students/${URLEncoder.encode(studentKey, "UTF-8")}",
                "DELETE"
            )
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to delete student."))
            }
            val result = try {
                JSONObject(response)
            } catch (_: Exception) {
                throw ApiException(code, "Server response was invalid; student deletion could not be verified.")
            }
            if (!result.optBoolean("success", false)) {
                throw ApiException(code, serverMessage(response, "Server did not confirm that the student was deleted."))
            }
            true
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to connect to the server.")
        } finally {
            conn?.disconnect()
        }
    }

    private fun studentJson(student: Student): JSONObject = JSONObject().apply {
        put("name", student.name)
        put("enrollmentNo", student.enrollmentNo)
        put("classRollNo", student.classRollNo)
        put("uniqueId", student.uniqueId)
        put("rollNo", student.rollNo)
        put("dateOfBirth", student.dateOfBirth)
        put("father", student.father)
        put("mother", student.mother)
        put("gender", student.gender)
        put("studentPhone", student.studentPhone)
        put("email", student.email)
        put("parentsPhone", student.parentsPhone)
        put("parentsEmail", student.parentsEmail)
        put("religion", student.religion)
        put("domicile", student.domicile)
        put("ews", student.ews)
        put("permanentAddress", student.permanentAddress)
        put("presentAddress", student.presentAddress)
        put("branch", student.branch)
        put("rank", student.rank)
        put("marks", student.marks)
        put("eligibleCategory", student.eligibleCategory)
        put("allocatedCategory", student.allocatedCategory)
        put("offlineAdmissionCategory", student.offlineAdmissionCategory)
        put("status", student.status)
        put("round", student.round)
        put("admissionYear", student.admissionYear)
        put("remarks", student.remarks)
        put("aadhaarNo", student.aadhaarNo)
        put("aadhaarLinkedMobile", student.aadhaarLinkedMobile)
    }

    suspend fun getFeeStructures(
        context: Context,
        academicYear: String = "",
        feeCategory: String = "",
        semester: String = "",
        branch: String = ""
    ): List<FeeStructure> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val params = mutableListOf<String>()
            fun addParam(key: String, value: String) {
                if (value.isNotBlank()) params += "$key=${URLEncoder.encode(value.trim(), "UTF-8")}"
            }
            addParam("academic_year", academicYear)
            addParam("fee_category", feeCategory)
            addParam("semester_no", semester.filter { it.isDigit() })
            addParam("branch", BranchCatalog.canonicalBranch(branch))
            val path = "/fee-structures" + if (params.isEmpty()) "" else "?${params.joinToString("&")}"
            conn = openConnection(context, path, "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) { AuthStorage.clear(context); throw ApiException(401, "Session expired. Please sign in again.") }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to load fee structures."))
            val array = JSONObject(response).optJSONArray("data") ?: JSONArray()
            buildList {
                for (i in 0 until array.length()) {
                    val x = array.getJSONObject(i)
                    val structureYear = x.optInt("structure_year", 0)
                    val year = if (structureYear > 0) {
                        "${structureYear}-${((structureYear + 1) % 100).toString().padStart(2, '0')}"
                    } else x.optString("structure_year", "")
                    val category = x.optString("fee_category", "")
                    val branch = x.optString("branch", "")
                    val structureId = x.optInt("fee_structure_id", 0)
                    val feeMastCode = x.optString("fee_mast_code", "")
                    val semesters = x.optJSONArray("semesters") ?: JSONArray()
                    for (j in 0 until semesters.length()) {
                        val sem = semesters.getJSONObject(j)
                        val no = sem.optInt("semester_no", 0)
                        if (no > 0) add(FeeStructure(
                            academicYear = year,
                            branch = branch,
                            feeCategory = category,
                            semester = "Semester $no",
                            feeAmount = sem.optString("amount", ""),
                            structureId = structureId,
                            feeMastCode = feeMastCode
                        ))
                    }
                }
            }
        } catch (e: ApiException) { throw e }
        catch (_: Exception) { throw ApiException(0, "Unable to connect to the server.") }
        finally { conn?.disconnect() }
    }

    suspend fun saveFeeSemester(context: Context, fee: FeeStructure): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/fee-structures/semester", "POST")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val yearStart = fee.academicYear.trim().take(4)
            val semesterNo = fee.semester.filter { it.isDigit() }.toIntOrNull()
                ?: throw ApiException(422, "Invalid semester")
            val body = JSONObject().apply {
                put("academic_year", fee.academicYear.trim())
                put("branch", BranchCatalog.canonicalBranch(fee.branch))
                put("fee_category", fee.feeCategory.trim())
                put("semester_no", semesterNo)
                put("amount", fee.feeAmount.toDoubleOrNull() ?: 0.0)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) { AuthStorage.clear(context); throw ApiException(401, "Session expired. Please sign in again.") }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to save fee structure."))
            true
        } catch (e: ApiException) { throw e }
        catch (_: Exception) { throw ApiException(0, "Unable to connect to the server.") }
        finally { conn?.disconnect() }
    }

    suspend fun deleteFeeSemester(context: Context, structureId: Int, semester: String): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val semesterNo = semester.filter { it.isDigit() }.toIntOrNull()
                ?: throw ApiException(422, "Invalid semester")
            conn = openConnection(context, "/fee-structures/$structureId/semester/$semesterNo", "DELETE")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) { AuthStorage.clear(context); throw ApiException(401, "Session expired. Please sign in again.") }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to delete fee structure."))
            true
        } catch (e: ApiException) { throw e }
        catch (_: Exception) { throw ApiException(0, "Unable to connect to the server.") }
        finally { conn?.disconnect() }
    }

    suspend fun getFeeCategories(context: Context): List<String> = withContext(Dispatchers.IO) {
        getLookupValues(context, "/lookups/fee-categories")
    }

    suspend fun getPaymentModes(context: Context): List<String> = withContext(Dispatchers.IO) {
        getLookupValues(context, "/lookups/payment-modes")
    }

    private fun getLookupValues(context: Context, path: String): List<String> {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, path, "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) {
                throw ApiException(code, serverMessage(response, "Unable to load lookup options."))
            }
            val array = JSONObject(response).optJSONArray("data") ?: JSONArray()
            return buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val value = item.optString("code", "").trim()
                    val name = item.optString("name", "").trim()
                    (value.ifBlank { name }).takeIf { it.isNotBlank() }?.let(::add)
                }
            }.distinct()
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "Unable to load lookup options from server.")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun getPayments(
        context: Context,
        academicYear: String = "",
        feeCategory: String = "",
        semester: String = "",
        paymentMode: String = "",
        branch: String = ""
    ): List<FeePayment> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val params = mutableListOf<String>()
            fun addParam(key: String, value: String) {
                if (value.isNotBlank()) params += "$key=${URLEncoder.encode(value.trim(), "UTF-8")}"
            }
            addParam("academic_year", academicYear)
            addParam("fee_category", feeCategory)
            addParam("semester_no", semester.filter { it.isDigit() })
            addParam("payment_mode", paymentMode)
            addParam("branch", BranchCatalog.canonicalBranch(branch))
            val path = "/payments" + if (params.isEmpty()) "" else "?${params.joinToString("&")}"
            conn = openConnection(context, path, "GET")
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) {
                AuthStorage.clear(context)
                throw ApiException(401, "Session expired. Please sign in again.")
            }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to load payments."))
            val array = JSONObject(response).optJSONArray("data") ?: JSONArray()
            buildList {
                for (i in 0 until array.length()) {
                    val x = array.getJSONObject(i)
                    val sem = x.optInt("semester_no", 0)
                    add(FeePayment(
                        studentId = x.optString("student_id", ""),
                        studentName = x.optString("name", ""),
                        enrollmentNo = x.optString("enrolment_no", ""),
                        branch = x.optString("branch", ""),
                        academicYear = x.optString("academic_year", ""),
                        feeCategory = x.optString("fee_category", ""),
                        semester = if (sem > 0) "Semester $sem" else "",
                        totalFee = x.optString("total_amount", ""),
                        paidAmount = x.optString("paid_amount", ""),
                        pendingAmount = x.optString("pending_amount", ""),
                        paymentDate = x.optString("payment_date", ""),
                        paymentMode = x.optString("payment_mode", ""),
                        transactionId = x.optString("transaction_id", ""),
                        paymentStatus = x.optString("payment_status", "")
                    ))
                }
            }
        } catch (e: ApiException) { throw e }
        catch (_: Exception) { throw ApiException(0, "Unable to connect to the server.") }
        finally { conn?.disconnect() }
    }

    suspend fun savePayment(context: Context, payment: FeePayment): Boolean = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(context, "/payments", "POST")
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val semesterNo = payment.semester.filter { it.isDigit() }.toIntOrNull()
            val body = JSONObject().apply {
                put("enrolment_no", payment.enrollmentNo)
                put("academic_year", payment.academicYear)
                put("fee_category", payment.feeCategory)
                if (semesterNo != null) put("semester_no", semesterNo)
                put("total_amount", payment.totalFee.toDoubleOrNull() ?: 0.0)
                put("student_submitted_amount", payment.paidAmount.toDoubleOrNull() ?: 0.0)
                put("payment_mode", payment.paymentMode)
                put("payment_date", payment.paymentDate)
                if (payment.transactionId.isNotBlank()) put("transactionId", payment.transactionId)
                put("payment_status", if ((payment.pendingAmount.toDoubleOrNull() ?: 0.0) <= 0.0) "PAID" else "PARTIAL")
                put("create_new", true)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val code = conn.responseCode
            val response = responseText(conn)
            if (code == 401) { AuthStorage.clear(context); throw ApiException(401, "Session expired. Please sign in again.") }
            if (code !in 200..299) throw ApiException(code, serverMessage(response, "Unable to save payment."))
            true
        } catch (e: ApiException) { throw e }
        catch (_: Exception) { throw ApiException(0, "Unable to connect to the server.") }
        finally { conn?.disconnect() }
    }

}
