package com.example.studentmanagementapp

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small encrypted cache used only to keep the existing UI responsive while
 * the server is temporarily unavailable.
 *
 * MySQL/FastAPI remains the source of truth. The cache is never used to
 * silently replace a successful server response.
 */
object StudentStorage {

    private const val PREF_NAME = "student_management_data"
    private const val STUDENTS_KEY = "students_encrypted"
    private const val KEY_ALIAS = "student_management_student_cache_key"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val combined = cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? {
    return try {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        if (combined.size <= 12) return null

        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, iv)
        )
        String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    } catch (_: Exception) {
        null
    }
}

    fun saveStudents(context: Context, students: List<Student>) {
        val array = JSONArray()

        students.forEach { student ->
            val obj = JSONObject().apply {
                put("studentId", student.studentId)
                put("uniqueId", student.uniqueId)
                put("name", student.name)
                put("classRollNo", student.classRollNo)
                put("enrollmentNo", student.enrollmentNo)
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
                put("feeStructureCode", student.feeStructureCode)
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
            array.put(obj)
        }

        prefs(context).edit()
            .putString(STUDENTS_KEY, encrypt(array.toString()))
            .remove("students") // Remove the old plaintext cache if it exists.
            .apply()
    }

    fun loadStudents(context: Context): List<Student> {
        val encrypted = prefs(context).getString(STUDENTS_KEY, null) ?: return emptyList()
        val data = decrypt(encrypted) ?: return emptyList()

        return try {
            val array = JSONArray(data)
            val students = mutableListOf<Student>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                students.add(
                    Student(
                        studentId = obj.optInt("studentId", 0).takeIf { it > 0 },
                        uniqueId = obj.optString("uniqueId"),
                        name = obj.optString("name"),
                        classRollNo = obj.optString("classRollNo"),
                        enrollmentNo = obj.optString("enrollmentNo"),
                        rollNo = obj.optString("rollNo"),
                        dateOfBirth = obj.optString("dateOfBirth"),
                        father = obj.optString("father"),
                        mother = obj.optString("mother"),
                        gender = obj.optString("gender"),
                        studentPhone = obj.optString("studentPhone"),
                        email = obj.optString("email"),
                        parentsPhone = obj.optString("parentsPhone"),
                        parentsEmail = obj.optString("parentsEmail"),
                        religion = obj.optString("religion"),
                        domicile = obj.optString("domicile"),
                        ews = obj.optString("ews"),
                        permanentAddress = obj.optString("permanentAddress"),
                        presentAddress = obj.optString("presentAddress"),
                        branch = obj.optString("branch"),
                        feeStructureCode = obj.optString("feeStructureCode"),
                        rank = obj.optString("rank"),
                        marks = obj.optString("marks"),
                        eligibleCategory = obj.optString("eligibleCategory"),
                        allocatedCategory = obj.optString("allocatedCategory"),
                        offlineAdmissionCategory = obj.optString("offlineAdmissionCategory"),
                        status = obj.optString("status"),
                        round = obj.optString("round"),
                        admissionYear = obj.optString("admissionYear"),
                        remarks = obj.optString("remarks"),
                        aadhaarNo = obj.optString("aadhaarNo"),
                        aadhaarLinkedMobile = obj.optString("aadhaarLinkedMobile")
                    )
                )
            }
            students
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun syncWithBackend(context: Context): List<Student> {
        return try {
            // Fetch only the first page. The dashboard count is retrieved separately
            // via /dashboard which gives the real DB total without loading all rows.
            // Student Records, Search, Admissions, and Reports all use server-side
            // API calls with pagination and filtering — they do not depend on this list.
            val page = ApiClient.getStudents(
                context = context,
                limit = 50,
                offset = 0
            )
            saveStudents(context, page)
            page
        } catch (e: ApiClient.ApiException) {
            // A temporary network outage may use the encrypted cache. Auth
            // failures are propagated so the caller can force re-login.
            if (e.statusCode == 0) loadStudents(context) else throw e
        }
    }
}

