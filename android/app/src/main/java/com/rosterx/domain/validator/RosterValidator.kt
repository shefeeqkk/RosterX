package com.rosterx.domain.validator

import com.rosterx.domain.model.ShiftCode

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

class RosterValidator {

    /**
     * Validates an entire month's roster for a single engineer.
     * @param shifts A list of ShiftCodes ordered by day of the month.
     */
    fun validateEngineerMonth(shifts: List<ShiftCode>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        var consecutiveWorkDays = 0
        var consecutiveNightShifts = 0

        for (i in shifts.indices) {
            val currentShift = shifts[i]
            val nextShift = if (i + 1 < shifts.size) shifts[i + 1] else null

            // 1. Consecutive Work Cap (Max 6 days)
            if (isWorkingShift(currentShift)) {
                consecutiveWorkDays++
                if (consecutiveWorkDays > 6) {
                    errors.add("Day ${i + 1}: Exceeded maximum of 6 consecutive work days.")
                }
            } else {
                consecutiveWorkDays = 0 // Reset on OFF, L, or SW
            }

            // 2. Post-Night Rest Guard (NS must be followed by OFF/L/SW before taking any Day shift; continuous NS is allowed)
            if (currentShift is ShiftCode.NS) {
                consecutiveNightShifts++
                val isNextRest = nextShift == null || nextShift is ShiftCode.NS || nextShift is ShiftCode.OFF || nextShift is ShiftCode.L || nextShift is ShiftCode.SW || nextShift.code.startsWith("SW")
                if (!isNextRest) {
                    errors.add("Day ${i + 2}: Shift ${nextShift!!.code} immediately follows Night Shift (NS) without mandatory OFF/L/SW rest day.")
                }
            } else if (currentShift is ShiftCode.OFF || currentShift is ShiftCode.L || currentShift is ShiftCode.SW || currentShift.code.startsWith("SW")) {
                // Check if the block of night shifts was valid (3 to 5 nights)
                if (consecutiveNightShifts > 0) {
                    if (consecutiveNightShifts !in 3..5) {
                        warnings.add("Day ${i}: Night shifts should ideally be grouped in blocks of 3 to 5. Found $consecutiveNightShifts.")
                    }
                    consecutiveNightShifts = 0
                }
            }

            // Quick Turnaround (SS to FS) is fine as per NOC operational guidelines
        }
        
        // Handle night shift count at the end of the month
        if (consecutiveNightShifts > 0 && consecutiveNightShifts !in 3..5) {
            warnings.add("End of month: Night shifts should be grouped in blocks of 3 to 5. Found $consecutiveNightShifts.")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validates the daily headcount minimum quotas for the entire team.
     * @param dayShifts A list of all shifts assigned to engineers on a specific day.
     */
    fun validateDailyHeadcount(dayShifts: List<ShiftCode>): ValidationResult {
        val errors = mutableListOf<String>()

        val nsCount = dayShifts.count { it is ShiftCode.NS }
        val fsCount = dayShifts.count { it is ShiftCode.FS }
        val ssCount = dayShifts.count { it is ShiftCode.SS }
        val daytimeOverlapCount = dayShifts.count { it is ShiftCode.FG || it is ShiftCode.G || it is ShiftCode.SG }

        if (nsCount != 1) {
            errors.add("Night Shift (NS) headcount must be exactly 1. Found $nsCount.")
        }
        if (fsCount < 1) {
            errors.add("First Shift (FS) headcount must be at least 1. Found $fsCount.")
        }
        if (ssCount < 1) {
            errors.add("Second Shift (SS) headcount must be at least 1. Found $ssCount.")
        }
        if (daytimeOverlapCount < 3) {
            errors.add("Daytime Overlap (FG, G, SG) headcount must be at least 3. Found $daytimeOverlapCount.")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun isWorkingShift(shift: ShiftCode): Boolean {
        return shift !is ShiftCode.OFF && shift !is ShiftCode.L && shift !is ShiftCode.SW && !shift.code.startsWith("SW")
    }
}
