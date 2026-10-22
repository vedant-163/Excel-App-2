package com.example.studentmanagementapp

data class Student(
    // Stable database identity. Used for update/delete; never inferred from a display ID.
    val studentId: Int? = null,

    // Basic Information
    val uniqueId: String = "",
    val name: String = "",
    val classRollNo: String = "",
    val enrollmentNo: String = "",
    val rollNo: String = "",
    val dateOfBirth: String = "",

    // Personal & Contact Information
    val father: String = "",
    val mother: String = "",
    val gender: String = "",
    val studentPhone: String = "",
    val email: String = "",
    val parentsPhone: String = "",
    val parentsEmail: String = "",
    val religion: String = "",
    val domicile: String = "",
    val ews: String = "",
    val permanentAddress: String = "",
    val presentAddress: String = "",

    // Academic Information
    val branch: String = "",
    val feeStructureCode: String = "",
    val rank: String = "",
    val marks: String = "",

    // Admission Information
    val eligibleCategory: String = "",
    val allocatedCategory: String = "",
    val offlineAdmissionCategory: String = "",
    val status: String = "",
    val round: String = "",
    val admissionYear: String = "",
    val remarks: String = "",

    // Document Information
    val aadhaarNo: String = "",
    val aadhaarLinkedMobile: String = ""
)