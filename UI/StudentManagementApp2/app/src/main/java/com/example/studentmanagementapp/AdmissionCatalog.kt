package com.example.studentmanagementapp

import androidx.compose.runtime.mutableStateListOf

/**
 * Central catalog for admission-related values used across the app.
 * Academic years are loaded from the existing MySQL database through the
 * backend. No academic-year list is hardcoded in the UI.
 */
object AdmissionCatalog {

    // Admission Year = calendar year in which admission is taken.
    // Kept only as an offline fallback if the backend is temporarily unavailable.
    val admissionYears = mutableStateListOf(
        "2022", "2023", "2024", "2025", "2026", "2027"
    )

    // Academic years returned by the database, e.g. 2020-21, 2021-22.
    // Compose observes this list, so every screen using it updates automatically.
    // Fallback only: API/database values replace this list when available.
    // Includes upcoming years so the form does not become unusable offline.
    val academicYears = mutableStateListOf<String>().apply {
        val currentStart = java.time.LocalDate.now().year
        addAll((currentStart - 5..currentStart + 5).map { year ->
            "$year-${((year + 1) % 100).toString().padStart(2, '0')}"
        })
    }

    // Allocated Category options are populated from GET /lookups/alloted-categories.
    // This list intentionally has no hardcoded category fallback.
    val allocatedCategories = mutableStateListOf<String>()

    fun setAllocatedCategories(values: List<String>) {
        val cleaned = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        allocatedCategories.clear()
        allocatedCategories.addAll(cleaned)
    }

    fun setAcademicYears(years: List<String>) {
        val cleaned = years
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        if (cleaned.isNotEmpty()) {
            academicYears.clear()
            academicYears.addAll(cleaned)

            // Keep admission-year options consistent with the DB academic years.
            val derivedAdmissionYears = cleaned.mapNotNull { admissionYearForAcademicYear(it).ifBlank { null } }
            if (derivedAdmissionYears.isNotEmpty()) {
                admissionYears.clear()
                admissionYears.addAll(derivedAdmissionYears.distinct().sorted())
            }
        }
    }

    fun academicYearForAdmissionYear(admissionYear: String): String {
        val year = admissionYear.trim().toIntOrNull() ?: return ""
        val end = ((year + 1) % 100).toString().padStart(2, '0')
        return "$year-$end"
    }

    fun admissionYearForAcademicYear(academicYear: String): String {
        val normalized = academicYear.trim()
        val startYear = normalized.substringBefore('-', "").toIntOrNull()
            ?: return ""
        return startYear.toString()
    }

    // Database-provided round labels replace the fallback when available.
    val admissionRounds = mutableStateListOf(
        "CLC ROUND",
        "FIRST ROUND",
        "FIRST ROUND UPGRADE",
        "SECOND ROUND",
        "CSAB ROUND",
        "NRI ROUND",
        "GULF ROUND"
    )

    fun setAdmissionRounds(values: List<String>) {
        val cleaned = values.map { normalizeAdmissionRound(it) }
            .filter { it.isNotBlank() }
            .distinct()
        if (cleaned.isNotEmpty()) {
            admissionRounds.clear()
            admissionRounds.addAll(cleaned)
        }
    }

    fun normalizeAdmissionRound(value: String): String =
        value.trim().removePrefix("(").removeSuffix(")").trim()

    val admissionStatuses: List<String> = listOf(
        "Admitted",
        "Cancelled"
    )

    val categories: List<String> = listOf(
        "UR",
        "OBC",
        "SC",
        "ST",
        "Other"
    )
}
