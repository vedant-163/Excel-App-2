package com.example.studentmanagementapp

/**
 * Single source of truth for branch names used throughout the app.
 *
 * Artificial Intelligence and Robotics is available only
 * from academic year 2026-27.
 */
object BranchCatalog {

    const val AI_ROBOTICS = "Artificial Intelligence and Robotics"

    val standardBranches = listOf(
        "Computer Science and Engineering",
        "Information Technology",
        "Electronics and Telecommunication Engineering",
        "Electrical Engineering",
        "Electronics and Instrumentation Engineering",
        "Mechanical Engineering",
        "Civil Engineering",
        "Industrial and Production Engineering",
        "Biomedical Engineering",
        "Bachelor of Pharmacy"
    )

    fun optionsForAcademicYear(admissionYear: String): List<String> {
        val year = admissionYear.trim()

        return if (year == "2026" || year == "2026-27") {
            standardBranches + AI_ROBOTICS
        } else {
            standardBranches
        }
    }

    /** Converts canonical/display branch names to the codes used by backend filters. */
    fun branchCode(branch: String): String {
        return when (canonicalBranch(branch).lowercase()) {
            "computer science and engineering" -> "CSE"
            "information technology" -> "IT"
            "electronics and telecommunication engineering" -> "EC"
            "electrical engineering" -> "EE"
            "electronics and instrumentation engineering" -> "EI"
            "mechanical engineering" -> "ME"
            "civil engineering" -> "CE"
            "industrial and production engineering" -> "IPE"
            "biomedical engineering" -> "BME"
            "bachelor of pharmacy" -> "B.PHARMACY"
            "artificial intelligence and robotics" -> "AIR"
            else -> branch.trim().uppercase()
        }
    }

    fun canonicalBranch(branch: String): String {

        return when (branch.trim().lowercase()) {

            "cse",
            "computer science",
            "computer science & engineering",
            "computer science and engineering" ->
                "Computer Science and Engineering"

            "it",
            "information technology" ->
                "Information Technology"

            "etc",
            "e&tc",
            "e&telecom",
            "electronics & telecommunication engineering",
            "ec",
            "electronics and communication engineering",
            "electronics and telecommunication engineering" ->
                "Electronics and Telecommunication Engineering"

            "ee",
            "electrical",
            "electrical engineering" ->
                "Electrical Engineering"

            "e&i",
            "ei",
            "electronics & instrumentation engineering",
            "electronics and instrumentation engineering" ->
                "Electronics and Instrumentation Engineering"

            "me",
            "mechanical",
            "mechanical engineering" ->
                "Mechanical Engineering"

            "ce",
            "civil",
            "civil engineering" ->
                "Civil Engineering"

            "i&p",
            "industrial & production engineering",
            "industrial and production engineering" ->
                "Industrial and Production Engineering"

            "bme",
            "biomedical",
            "biomedical engineering" ->
                "Biomedical Engineering"

            "b.pharmacy",
            "b pharmacy",
            "bpharm",
            "b.pharm",
            "bachelor of pharmacy",
            "pharmacy" ->
                "Bachelor of Pharmacy"

            "ai & robotics",
            "ai and robotics",
            "artificial intelligence & robotics",
            "artificial intelligence and robotics" ->
                AI_ROBOTICS

            else -> branch.trim()
        }
    }
}