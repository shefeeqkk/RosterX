package com.rosterx.domain.model

enum class EngineerRole {
    ADMIN, ENGINEER, APPRENTICE
}

enum class EngineerStatus {
    ACTIVE, NOTICE_PERIOD, ONBOARDING, RESIGNED
}

data class Engineer(
    val id: String,
    val name: String,
    val fullName: String,
    val email: String,
    val username: String, // Extracted domain id
    val phone: String,
    val role: EngineerRole,
    val status: EngineerStatus,
    val defaultWeeklyOffs: List<String>, // e.g., ["MONDAY", "TUESDAY"]
    val joinDate: String,
    val resignationDate: String? = null
)
