package com.rosterx.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ShiftCalendarExporter {

    private data class ShiftScheduleSpec(
        val title: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val rollsOverNextDay: Boolean = false
    )

    private val shiftSpecs = mapOf(
        "FS" to ShiftScheduleSpec("First Shift (FS)", 7, 30, 16, 30),
        "FG" to ShiftScheduleSpec("First General (FG)", 8, 30, 17, 30),
        "G"  to ShiftScheduleSpec("General Shift (G)", 9, 30, 18, 30),
        "SG" to ShiftScheduleSpec("Second General (SG)", 11, 0, 20, 0),
        "SS" to ShiftScheduleSpec("Second Shift (SS)", 13, 0, 22, 0),
        "NS" to ShiftScheduleSpec("Night Shift (NS)", 21, 0, 7, 30, rollsOverNextDay = true)
    )

    private val icsLocalFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)
    private val icsUtcFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)

    /**
     * Generates an RFC-5545 .ics calendar file for an engineer's working shifts in the given month
     * and launches the device Calendar app via FileProvider (zero runtime permissions required).
     *
     * @return Result containing the number of working shifts exported.
     */
    fun exportMonthlyShifts(
        context: Context,
        engineerName: String,
        username: String,
        year: Int,
        month: Int,
        dayShiftCodes: Map<Int, String>
    ): Result<Int> {
        return try {
            val cleanUser = username.trim().lowercase(Locale.US)
                .replace(Regex("[^a-z0-9._-]"), "_")
                .ifBlank { "member" }
            val safeName = escapeIcsText(engineerName.trim().ifBlank { "Team Member" })
            val dtStamp = ZonedDateTime.now(ZoneOffset.UTC).format(icsUtcFmt)

            val workingDays = dayShiftCodes.entries
                .sortedBy { it.key }
                .mapNotNull { (day, rawCode) ->
                    val code = rawCode.trim().uppercase(Locale.US)
                    if (code.isBlank() || code == "OFF" || code == "L" || code == "LEAVE" || code.startsWith("SW")) {
                        return@mapNotNull null
                    }
                    val spec = shiftSpecs[code] ?: return@mapNotNull null
                    val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return@mapNotNull null
                    Triple(date, code, spec)
                }

            if (workingDays.isEmpty()) {
                return Result.success(0)
            }

            val sb = StringBuilder()
            sb.append("BEGIN:VCALENDAR\r\n")
            sb.append("VERSION:2.0\r\n")
            sb.append("PRODID:-//RosterX//Operations Shift Calendar//EN\r\n")
            sb.append("CALSCALE:GREGORIAN\r\n")
            sb.append("METHOD:PUBLISH\r\n")
            sb.append("X-WR-CALNAME:RosterX Shifts - ")
                .append(String.format(Locale.US, "%04d-%02d", year, month))
                .append("\r\n")
            sb.append("X-WR-TIMEZONE:Asia/Kolkata\r\n")
            // Minimal VTIMEZONE block for Asia/Kolkata (UTC+05:30, no DST)
            sb.append("BEGIN:VTIMEZONE\r\n")
            sb.append("TZID:Asia/Kolkata\r\n")
            sb.append("BEGIN:STANDARD\r\n")
            sb.append("DTSTART:19700101T000000\r\n")
            sb.append("TZOFFSETFROM:+0530\r\n")
            sb.append("TZOFFSETTO:+0530\r\n")
            sb.append("TZNAME:IST\r\n")
            sb.append("END:STANDARD\r\n")
            sb.append("END:VTIMEZONE\r\n")

            workingDays.forEach { (date, code, spec) ->
                val startDt = LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, spec.startHour, spec.startMinute)
                val endDate = if (spec.rollsOverNextDay) date.plusDays(1) else date
                val endDt = LocalDateTime.of(endDate.year, endDate.monthValue, endDate.dayOfMonth, spec.endHour, spec.endMinute)

                val uid = String.format(
                    Locale.US,
                    "rosterx-%04d-%02d-%02d-%s@rosterx.online",
                    date.year,
                    date.monthValue,
                    date.dayOfMonth,
                    cleanUser
                )

                sb.append("BEGIN:VEVENT\r\n")
                sb.append("UID:").append(uid).append("\r\n")
                sb.append("DTSTAMP:").append(dtStamp).append("\r\n")
                sb.append("DTSTART;TZID=Asia/Kolkata:").append(startDt.format(icsLocalFmt)).append("\r\n")
                sb.append("DTEND;TZID=Asia/Kolkata:").append(endDt.format(icsLocalFmt)).append("\r\n")
                sb.append("SUMMARY:RosterX: ").append(escapeIcsText(spec.title)).append("\r\n")
                sb.append("DESCRIPTION:Scheduled ")
                    .append(escapeIcsText(spec.title))
                    .append(" duty for ")
                    .append(safeName)
                    .append(" (Shift Code: ")
                    .append(code)
                    .append("). Exported from RosterX.\r\n")
                sb.append("STATUS:CONFIRMED\r\n")
                sb.append("BEGIN:VALARM\r\n")
                sb.append("ACTION:DISPLAY\r\n")
                sb.append("DESCRIPTION:Upcoming RosterX ")
                    .append(escapeIcsText(spec.title))
                    .append(" in 30 minutes\r\n")
                sb.append("TRIGGER:-PT30M\r\n")
                sb.append("END:VALARM\r\n")
                sb.append("END:VEVENT\r\n")
            }

            sb.append("END:VCALENDAR\r\n")

            val exportDir = File(context.cacheDir, "calendar").apply {
                if (!exists()) mkdirs()
            }
            val fileName = String.format(Locale.US, "rosterx_shifts_%04d_%02d.ics", year, month)
            val icsFile = File(exportDir, fileName)
            icsFile.writeText(sb.toString(), Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                icsFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "text/calendar")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(viewIntent)
            } catch (_: ActivityNotFoundException) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/calendar"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "RosterX Shifts (${String.format(Locale.US, "%04d-%02d", year, month)})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Export Monthly Shifts to Calendar").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }

            Result.success(workingDays.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeIcsText(raw: String): String {
        return raw
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
    }
}
