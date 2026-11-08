package com.rosterx.domain.model

enum class EngineerRole {
    ADMIN, MANAGER, ENGINEER, APPRENTICE
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
    val resignationDate: String? = null,
    val password: String? = null,
    val originalUsername: String? = null,
    val profilePicture: String? = null
)

/**
 * Returns true if this account is a dedicated management or system administrator account
 * that exists solely for login and operations management, and must never be considered in
 * shift calculations, team shift rosters, or team management directories.
 */
val Engineer.isManagementAccount: Boolean
    get() {
        val uname = username.trim().lowercase()
        val n = name.trim().lowercase()
        val fn = fullName.trim().lowercase()
        // Never treat actual team engineers (like Shefeeq or Atul) as pure management accounts even if they hold admin roles
        if (uname in setOf("shefeeqkk", "shefeeq", "athulka")) {
            return false
        }
        // Managers are strictly management accounts - never on desk shift duty or roster grids
        if (role == EngineerRole.MANAGER) {
            return true
        }
        return uname in setOf("admin", "manager", "sysadmin", "system_admin", "administrator") ||
               n in setOf("admin", "system administrator", "system admin") ||
               n.contains("system admin") ||
               n.contains("administrator") ||
               fn.contains("system admin") ||
               fn.contains("administrator") ||
               (role == EngineerRole.ADMIN && (uname.contains("admin") || n.contains("admin") || n.contains("system")))
    }

private val flexibleDateFormats = listOf(
    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
    java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
    java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy"),
    java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy")
)

fun parseResignationDate(dateStr: String?): java.time.LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    val clean = dateStr.trim().split("T")[0].split(" ")[0].trim()
    for (fmt in flexibleDateFormats) {
        try {
            return java.time.LocalDate.parse(clean, fmt)
        } catch (_: Exception) {}
    }
    return null
}

fun Engineer.isEligibleForShiftRoster(date: java.time.LocalDate): Boolean {
    if (isManagementAccount) return false
    if (status == EngineerStatus.RESIGNED) {
        val rDate = parseResignationDate(resignationDate) ?: return false
        val rYearMonth = java.time.YearMonth.from(rDate)
        val targetYearMonth = java.time.YearMonth.from(date)
        if (targetYearMonth.isAfter(rYearMonth)) return false
        if (targetYearMonth == rYearMonth && date.isAfter(rDate)) return false
    }
    return true
}

fun Engineer.isEligibleForMonthRoster(year: Int, month: Int): Boolean {
    if (isManagementAccount) return false
    if (status == EngineerStatus.RESIGNED) {
        val rDate = parseResignationDate(resignationDate) ?: return false
        val rYearMonth = java.time.YearMonth.from(rDate)
        val targetYearMonth = java.time.YearMonth.of(year, month)
        if (targetYearMonth.isAfter(rYearMonth)) return false
    }
    return true
}
