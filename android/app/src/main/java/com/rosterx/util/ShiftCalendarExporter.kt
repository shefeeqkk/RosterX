package com.rosterx.util

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean
)

data class ShiftSyncPreview(
    val workingCount: Int,
    val offCount: Int,
    val leaveCount: Int,
    val swapCount: Int
) {
    val nonWorkingCount: Int get() = offCount + leaveCount + swapCount
    val totalCount: Int get() = workingCount + nonWorkingCount
}

data class CalendarSyncResult(
    val inserted: Int,
    val updated: Int,
    val deleted: Int,
    val totalSynced: Int
)

object ShiftCalendarExporter {

    private const val PREFS_NAME = "rosterx_calendar_sync_prefs"
    private const val KEY_CALENDAR_ID = "selected_calendar_id"
    private const val KEY_REMINDER_MINUTES = "reminder_minutes"
    private const val REMINDER_NONE_SENTINEL = -1
    private const val IST_ZONE_ID = "Asia/Kolkata"

    private data class ShiftScheduleSpec(
        val title: String,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
        val rollsOverNextDay: Boolean = false
    )

    private data class ResolvedDayEvent(
        val date: LocalDate,
        val dayKey: String,
        val code: String,
        val title: String,
        val description: String,
        val isAllDay: Boolean,
        val isWorkingShift: Boolean,
        val startMillis: Long,
        val endMillis: Long,
        val timezone: String,
        val availability: Int,
        val icsStartLocal: LocalDateTime? = null,
        val icsEndLocal: LocalDateTime? = null
    )

    private data class ExistingCalendarEvent(
        val eventId: Long,
        val dayKey: String,
        val isAllDay: Boolean
    )

    private val shiftSpecs = mapOf(
        "FS" to ShiftScheduleSpec("First Shift (FS)", 7, 30, 16, 30),
        "FG" to ShiftScheduleSpec("First General (FG)", 8, 30, 17, 30),
        "G"  to ShiftScheduleSpec("General Shift (G)", 9, 30, 18, 30),
        "SG" to ShiftScheduleSpec("Second General (SG)", 11, 0, 20, 0),
        "SS" to ShiftScheduleSpec("Second Shift (SS)", 13, 0, 22, 0),
        "NS" to ShiftScheduleSpec("Night Shift (NS)", 21, 0, 7, 30, rollsOverNextDay = true)
    )

    private val istZone: ZoneId = ZoneId.of(IST_ZONE_ID)
    private val icsLocalFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US)
    private val icsDateFmt = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
    private val icsUtcFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US)

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun getSavedCalendarId(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getLong(KEY_CALENDAR_ID, -1L)
        return if (saved > 0L) saved else null
    }

    fun saveCalendarId(context: Context, calendarId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_CALENDAR_ID, calendarId)
            .apply()
    }

    /**
     * Returns saved reminder lead time in minutes for working shifts (default 30),
     * or null when reminders are disabled ("None").
     */
    fun getSavedReminderMinutes(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_REMINDER_MINUTES)) return 30
        val value = prefs.getInt(KEY_REMINDER_MINUTES, 30)
        return if (value == REMINDER_NONE_SENTINEL || value < 0) null else value
    }

    fun saveReminderMinutes(context: Context, minutes: Int?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REMINDER_MINUTES, minutes ?: REMINDER_NONE_SENTINEL)
            .apply()
    }

    /**
     * Computes a quick breakdown of exportable days for a single user's monthly roster.
     */
    fun computePreview(dayShiftCodes: Map<Int, String>): ShiftSyncPreview {
        var working = 0
        var off = 0
        var leave = 0
        var swap = 0
        dayShiftCodes.values.forEach { raw ->
            val code = raw.trim().uppercase(Locale.US)
            when {
                code in shiftSpecs -> working++
                code == "OFF" -> off++
                code == "L" || code == "LEAVE" -> leave++
                code.startsWith("SW") -> swap++
            }
        }
        return ShiftSyncPreview(
            workingCount = working,
            offCount = off,
            leaveCount = leave,
            swapCount = swap
        )
    }

    /**
     * Queries writable calendars on the user's device (Google Calendar, Outlook, local, etc.).
     * Requires READ_CALENDAR permission.
     */
    fun getWritableCalendars(context: Context): List<DeviceCalendar> {
        if (!hasCalendarPermissions(context)) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND " +
            "(${CalendarContract.Calendars.DELETED} IS NULL OR ${CalendarContract.Calendars.DELETED} = 0)"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        val calendars = mutableListOf<DeviceCalendar>()
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val accountName = if (accNameIdx >= 0) cursor.getString(accNameIdx).orEmpty() else ""
                    val accountType = if (accTypeIdx >= 0) cursor.getString(accTypeIdx).orEmpty() else ""
                    val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0xFF2563EB.toInt()
                    val isPrimary = if (primaryIdx >= 0) cursor.getInt(primaryIdx) == 1 else false

                    calendars.add(
                        DeviceCalendar(
                            id = id,
                            displayName = displayName.ifBlank { accountName.ifBlank { "Device Calendar" } },
                            accountName = accountName,
                            accountType = accountType,
                            color = color,
                            isPrimary = isPrimary
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }

        return calendars.sortedWith(
            compareByDescending<DeviceCalendar> { it.isPrimary }
                .thenByDescending { it.accountType.contains("google", ignoreCase = true) }
                .thenBy { it.displayName.lowercase(Locale.US) }
        )
    }

    /**
     * Counts how many RosterX-tagged events currently exist in [calendarId] for [username] in [year]/[month].
     */
    fun countSyncedEventsForMonth(
        context: Context,
        calendarId: Long,
        username: String,
        year: Int,
        month: Int
    ): Int {
        if (!hasCalendarPermissions(context) || calendarId <= 0L) return 0
        val cleanUser = sanitizeUsername(username)
        val monthPrefix = buildMonthTagPrefix(year, month, cleanUser)
        return queryExistingMonthEvents(context, calendarId, monthPrefix).size
    }

    /**
     * Performs a full-month smart diff sync directly into Android's CalendarContract for ONLY this user:
     * - Working shifts (FS/FG/G/SG/SS/NS) -> Timed IST events, Busy, optional reminder.
     * - Non-working days (OFF/L/SW*) -> All-Day events (UTC midnight boundaries, no clock time), Free, no reminder.
     * - Updates modified days, removes stale RosterX days, and NEVER touches personal calendar events.
     */
    fun syncMonthToCalendar(
        context: Context,
        calendarId: Long,
        engineerName: String,
        username: String,
        year: Int,
        month: Int,
        dayShiftCodes: Map<Int, String>,
        reminderMinutes: Int?
    ): Result<CalendarSyncResult> {
        if (!hasCalendarPermissions(context)) {
            return Result.failure(SecurityException("Calendar permission is required for direct sync"))
        }
        return try {
            val cleanUser = sanitizeUsername(username)
            val safeName = engineerName.trim().ifBlank { "Team Member" }
            val monthPrefix = buildMonthTagPrefix(year, month, cleanUser)
            val desiredEvents = resolveMonthlyEvents(
                engineerName = safeName,
                cleanUser = cleanUser,
                year = year,
                month = month,
                dayShiftCodes = dayShiftCodes
            )

            val existingList = queryExistingMonthEvents(context, calendarId, monthPrefix)
            val existingByDay = mutableMapOf<String, ExistingCalendarEvent>()
            var deletedCount = 0

            // Deduplicate any accidental duplicate RosterX events for the same day
            existingList.forEach { existing ->
                if (existing.dayKey.isBlank() || existingByDay.containsKey(existing.dayKey)) {
                    if (deleteEvent(context, existing.eventId)) deletedCount++
                } else {
                    existingByDay[existing.dayKey] = existing
                }
            }

            var insertedCount = 0
            var updatedCount = 0
            val desiredDayKeys = desiredEvents.map { it.dayKey }.toSet()

            // Remove RosterX events for days no longer present in the month's roster
            existingByDay.entries
                .filter { it.key !in desiredDayKeys }
                .forEach { (_, stale) ->
                    if (deleteEvent(context, stale.eventId)) deletedCount++
                }

            // Upsert every resolved day cleanly
            desiredEvents.forEach { target ->
                val existing = existingByDay[target.dayKey]
                val effectiveReminder = if (target.isWorkingShift) reminderMinutes else null

                if (existing == null) {
                    val newId = insertEvent(context, calendarId, target)
                    if (newId != null) {
                        replaceEventReminder(context, newId, effectiveReminder)
                        insertedCount++
                    }
                } else if (existing.isAllDay != target.isAllDay) {
                    // Switching between timed (Asia/Kolkata) and all-day (UTC) is cleanest via replace
                    deleteEvent(context, existing.eventId)
                    val newId = insertEvent(context, calendarId, target)
                    if (newId != null) {
                        replaceEventReminder(context, newId, effectiveReminder)
                        updatedCount++
                    }
                } else {
                    updateEvent(context, existing.eventId, target)
                    replaceEventReminder(context, existing.eventId, effectiveReminder)
                    updatedCount++
                }
            }

            saveCalendarId(context, calendarId)
            saveReminderMinutes(context, reminderMinutes)

            Result.success(
                CalendarSyncResult(
                    inserted = insertedCount,
                    updated = updatedCount,
                    deleted = deletedCount,
                    totalSynced = desiredEvents.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes ONLY RosterX-synced events for [username] in [year]/[month] from [calendarId].
     * Never touches personal or third-party calendar events.
     */
    fun clearMonthFromCalendar(
        context: Context,
        calendarId: Long,
        username: String,
        year: Int,
        month: Int
    ): Result<Int> {
        if (!hasCalendarPermissions(context)) {
            return Result.failure(SecurityException("Calendar permission is required"))
        }
        return try {
            val cleanUser = sanitizeUsername(username)
            val monthPrefix = buildMonthTagPrefix(year, month, cleanUser)
            val existing = queryExistingMonthEvents(context, calendarId, monthPrefix)
            var deleted = 0
            existing.forEach { ev ->
                if (deleteEvent(context, ev.eventId)) deleted++
            }
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a zero-permission RFC-5545 .ics calendar file mirroring the exact same full-month rules
     * (timed Busy working shifts with selected reminder + All-Day Free OFF/Leave/Swap Off with no reminder)
     * and shares/opens it via FileProvider.
     */
    fun exportMonthlyShifts(
        context: Context,
        engineerName: String,
        username: String,
        year: Int,
        month: Int,
        dayShiftCodes: Map<Int, String>,
        reminderMinutes: Int? = 30,
        preferShareSheet: Boolean = false
    ): Result<Int> {
        return try {
            val cleanUser = sanitizeUsername(username)
            val safeName = engineerName.trim().ifBlank { "Team Member" }
            val dtStamp = ZonedDateTime.now(ZoneOffset.UTC).format(icsUtcFmt)
            val resolvedEvents = resolveMonthlyEvents(
                engineerName = safeName,
                cleanUser = cleanUser,
                year = year,
                month = month,
                dayShiftCodes = dayShiftCodes
            )

            if (resolvedEvents.isEmpty()) {
                return Result.success(0)
            }

            val sb = StringBuilder()
            sb.append("BEGIN:VCALENDAR\r\n")
            sb.append("VERSION:2.0\r\n")
            sb.append("PRODID:-//RosterX//Personal Shift Calendar//EN\r\n")
            sb.append("CALSCALE:GREGORIAN\r\n")
            sb.append("METHOD:PUBLISH\r\n")
            sb.append("X-WR-CALNAME:RosterX - ")
                .append(escapeIcsText(safeName))
                .append(" (")
                .append(String.format(Locale.US, "%04d-%02d", year, month))
                .append(")\r\n")
            sb.append("X-WR-TIMEZONE:Asia/Kolkata\r\n")
            sb.append("BEGIN:VTIMEZONE\r\n")
            sb.append("TZID:Asia/Kolkata\r\n")
            sb.append("BEGIN:STANDARD\r\n")
            sb.append("DTSTART:19700101T000000\r\n")
            sb.append("TZOFFSETFROM:+0530\r\n")
            sb.append("TZOFFSETTO:+0530\r\n")
            sb.append("TZNAME:IST\r\n")
            sb.append("END:STANDARD\r\n")
            sb.append("END:VTIMEZONE\r\n")

            resolvedEvents.forEach { ev ->
                val uid = String.format(
                    Locale.US,
                    "rosterx-%04d-%02d-%02d-%s@rosterx.online",
                    ev.date.year,
                    ev.date.monthValue,
                    ev.date.dayOfMonth,
                    cleanUser
                )
                sb.append("BEGIN:VEVENT\r\n")
                sb.append("UID:").append(uid).append("\r\n")
                sb.append("DTSTAMP:").append(dtStamp).append("\r\n")
                if (ev.isAllDay) {
                    sb.append("DTSTART;VALUE=DATE:").append(ev.date.format(icsDateFmt)).append("\r\n")
                    sb.append("DTEND;VALUE=DATE:").append(ev.date.plusDays(1).format(icsDateFmt)).append("\r\n")
                    sb.append("TRANSP:TRANSPARENT\r\n")
                } else {
                    val startDt = ev.icsStartLocal!!
                    val endDt = ev.icsEndLocal!!
                    sb.append("DTSTART;TZID=Asia/Kolkata:").append(startDt.format(icsLocalFmt)).append("\r\n")
                    sb.append("DTEND;TZID=Asia/Kolkata:").append(endDt.format(icsLocalFmt)).append("\r\n")
                    sb.append("TRANSP:OPAQUE\r\n")
                }
                sb.append("SUMMARY:").append(escapeIcsText(ev.title)).append("\r\n")
                sb.append("DESCRIPTION:").append(escapeIcsText(ev.description)).append("\r\n")
                sb.append("STATUS:CONFIRMED\r\n")

                if (ev.isWorkingShift && reminderMinutes != null && reminderMinutes > 0) {
                    sb.append("BEGIN:VALARM\r\n")
                    sb.append("ACTION:DISPLAY\r\n")
                    sb.append("DESCRIPTION:Upcoming ")
                        .append(escapeIcsText(ev.title))
                        .append(" in ")
                        .append(reminderMinutes)
                        .append(" minutes\r\n")
                    sb.append("TRIGGER:-PT").append(reminderMinutes).append("M\r\n")
                    sb.append("END:VALARM\r\n")
                }
                sb.append("END:VEVENT\r\n")
            }

            sb.append("END:VCALENDAR\r\n")

            val exportDir = File(context.cacheDir, "calendar").apply {
                if (!exists()) mkdirs()
            }
            val fileName = String.format(Locale.US, "rosterx_%s_%04d_%02d.ics", cleanUser, year, month)
            val icsFile = File(exportDir, fileName)
            icsFile.writeText(sb.toString(), Charsets.UTF_8)

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                icsFile
            )

            if (preferShareSheet) {
                launchShareSheet(context, contentUri, year, month)
            } else {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "text/calendar")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(viewIntent)
                } catch (_: ActivityNotFoundException) {
                    launchShareSheet(context, contentUri, year, month)
                }
            }

            Result.success(resolvedEvents.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun launchShareSheet(context: Context, contentUri: android.net.Uri, year: Int, month: Int) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "RosterX Personal Shifts (${String.format(Locale.US, "%04d-%02d", year, month)})"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Personal Roster (.ics)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun resolveMonthlyEvents(
        engineerName: String,
        cleanUser: String,
        year: Int,
        month: Int,
        dayShiftCodes: Map<Int, String>
    ): List<ResolvedDayEvent> {
        val monthPrefix = buildMonthTagPrefix(year, month, cleanUser)
        return dayShiftCodes.entries
            .sortedBy { it.key }
            .mapNotNull { (day, rawCode) ->
                val code = rawCode.trim().uppercase(Locale.US)
                if (code.isBlank()) return@mapNotNull null
                val date = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return@mapNotNull null
                val dayKey = String.format(Locale.US, "%02d", day)
                val syncTag = "$monthPrefix$dayKey]"

                val workingSpec = shiftSpecs[code]
                if (workingSpec != null) {
                    val startLocal = LocalDateTime.of(
                        date.year,
                        date.monthValue,
                        date.dayOfMonth,
                        workingSpec.startHour,
                        workingSpec.startMinute
                    )
                    val endDate = if (workingSpec.rollsOverNextDay) date.plusDays(1) else date
                    val endLocal = LocalDateTime.of(
                        endDate.year,
                        endDate.monthValue,
                        endDate.dayOfMonth,
                        workingSpec.endHour,
                        workingSpec.endMinute
                    )
                    val startMillis = startLocal.atZone(istZone).toInstant().toEpochMilli()
                    val endMillis = endLocal.atZone(istZone).toInstant().toEpochMilli()
                    ResolvedDayEvent(
                        date = date,
                        dayKey = dayKey,
                        code = code,
                        title = "RosterX: ${workingSpec.title}",
                        description = "Scheduled ${workingSpec.title} duty for $engineerName (Shift Code: $code).\n$syncTag",
                        isAllDay = false,
                        isWorkingShift = true,
                        startMillis = startMillis,
                        endMillis = endMillis,
                        timezone = IST_ZONE_ID,
                        availability = CalendarContract.Events.AVAILABILITY_BUSY,
                        icsStartLocal = startLocal,
                        icsEndLocal = endLocal
                    )
                } else {
                    val nonWorkingTitleAndDesc = when {
                        code == "OFF" -> "RosterX: Weekly Off (OFF)" to "Weekly Off day for $engineerName.\n$syncTag"
                        code == "L" || code == "LEAVE" -> "RosterX: Approved Leave (L)" to "Approved Leave day for $engineerName.\n$syncTag"
                        code.startsWith("SW") -> "RosterX: Swap Off ($code)" to "Swap Off day for $engineerName (Code: $code).\n$syncTag"
                        else -> null
                    } ?: return@mapNotNull null

                    // Android CalendarContract requires ALL_DAY events to use UTC midnight boundaries.
                    val startUtcMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    val endUtcMillis = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                    ResolvedDayEvent(
                        date = date,
                        dayKey = dayKey,
                        code = code,
                        title = nonWorkingTitleAndDesc.first,
                        description = nonWorkingTitleAndDesc.second,
                        isAllDay = true,
                        isWorkingShift = false,
                        startMillis = startUtcMillis,
                        endMillis = endUtcMillis,
                        timezone = "UTC",
                        availability = CalendarContract.Events.AVAILABILITY_FREE
                    )
                }
            }
    }

    private fun queryExistingMonthEvents(
        context: Context,
        calendarId: Long,
        monthPrefix: String
    ): List<ExistingCalendarEvent> {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.ALL_DAY
        )
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
            "${CalendarContract.Events.DESCRIPTION} LIKE ? AND " +
            "(${CalendarContract.Events.DELETED} IS NULL OR ${CalendarContract.Events.DELETED} = 0)"
        val selectionArgs = arrayOf(calendarId.toString(), "%$monthPrefix%")

        val results = mutableListOf<ExistingCalendarEvent>()
        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val desc = if (descIdx >= 0) cursor.getString(descIdx).orEmpty() else ""
                    val allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                    val dayKey = extractDayKey(desc, monthPrefix)
                    results.add(ExistingCalendarEvent(eventId = id, dayKey = dayKey, isAllDay = allDay))
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return results
    }

    private fun insertEvent(
        context: Context,
        calendarId: Long,
        event: ResolvedDayEvent
    ): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startMillis)
            put(CalendarContract.Events.DTEND, event.endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timezone)
            put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
            put(CalendarContract.Events.AVAILABILITY, event.availability)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
        return runCatching { ContentUris.parseId(uri) }.getOrNull()
    }

    private fun updateEvent(
        context: Context,
        eventId: Long,
        event: ResolvedDayEvent
    ) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startMillis)
            put(CalendarContract.Events.DTEND, event.endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, event.timezone)
            put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
            put(CalendarContract.Events.AVAILABILITY, event.availability)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
        }
        val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.update(updateUri, values, null, null)
    }

    private fun deleteEvent(context: Context, eventId: Long): Boolean {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return context.contentResolver.delete(deleteUri, null, null) > 0
    }

    private fun replaceEventReminder(context: Context, eventId: Long, reminderMinutes: Int?) {
        try {
            context.contentResolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString())
            )
            if (reminderMinutes != null && reminderMinutes > 0) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
        } catch (_: Exception) {
            // Non-fatal: event itself is already safely synced
        }
    }

    private fun sanitizeUsername(username: String): String {
        return username.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9._-]"), "_")
            .ifBlank { "member" }
    }

    private fun buildMonthTagPrefix(year: Int, month: Int, cleanUser: String): String {
        return String.format(Locale.US, "[RosterX-Sync:%04d-%02d:%s:", year, month, cleanUser)
    }

    private fun extractDayKey(description: String, monthPrefix: String): String {
        val idx = description.indexOf(monthPrefix)
        if (idx < 0) return ""
        val start = idx + monthPrefix.length
        val end = description.indexOf(']', start)
        return if (end > start) description.substring(start, end).trim() else ""
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
