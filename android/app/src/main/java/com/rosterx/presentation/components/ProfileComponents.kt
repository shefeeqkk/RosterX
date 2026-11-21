package com.rosterx.presentation.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.rosterx.data.remote.isExpiredCelebration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicAccentGlow
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicRaised
import com.rosterx.ui.neumorphic.neumorphicFloating
import com.rosterx.ui.neumorphic.neumorphicClickable
import com.rosterx.ui.theme.LocalIsDarkTheme
import com.rosterx.ui.theme.LocalThemeAccent
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

fun String.toProperCase(): String {
    val clean = this.trim()
    if (clean.isBlank()) return clean
    return clean.split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            if (word.length == 1) word.uppercase()
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
}

fun formatNotificationTime(rawCreatedAt: String?, timestampEpoch: Double?): String {
    try {
        if (timestampEpoch != null && timestampEpoch > 0) {
            val instant = Instant.ofEpochMilli((timestampEpoch * 1000).toLong())
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val today = LocalDate.now()
            val notifDate = zonedDateTime.toLocalDate()
            val timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            return when (notifDate) {
                today -> "Today, ${zonedDateTime.format(timeFmt)}"
                today.minusDays(1) -> "Yesterday, ${zonedDateTime.format(timeFmt)}"
                else -> {
                    val fullFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
                    zonedDateTime.format(fullFmt)
                }
            }
        }
    } catch (_: Exception) {}

    if (!rawCreatedAt.isNullOrBlank()) {
        try {
            val clean = rawCreatedAt.trim()
            val utcCandidate = if (clean.endsWith(" UTC")) clean.removeSuffix(" UTC").trim() else clean
            val utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
            val ldt = LocalDateTime.parse(utcCandidate, utcFormatter)
            val zonedDateTime = ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault())
            val today = LocalDate.now()
            val notifDate = zonedDateTime.toLocalDate()
            val timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            return when (notifDate) {
                today -> "Today, ${zonedDateTime.format(timeFmt)}"
                today.minusDays(1) -> "Yesterday, ${zonedDateTime.format(timeFmt)}"
                else -> {
                    val fullFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
                    zonedDateTime.format(fullFmt)
                }
            }
        } catch (_: Exception) {}
    }
    return rawCreatedAt ?: ""
}

data class TenureData(val years: Int, val months: Int, val days: Int)

fun calculateTenure(joinDateStr: String?): TenureData? {
    if (joinDateStr.isNullOrBlank()) return null
    return try {
        val clean = joinDateStr.trim().replace("/", "-")
        val parts = clean.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val d = parts[2].toInt()
        val joinDate = LocalDate.of(y, m, d)
        val today = LocalDate.now()
        if (joinDate.isAfter(today)) return null
        val period = Period.between(joinDate, today)
        TenureData(period.years, period.months, period.days)
    } catch (_: Exception) {
        null
    }
}

fun calculateDaysUntilBirthday(birthdayStr: String?): Int? {
    if (birthdayStr.isNullOrBlank()) return null
    return try {
        val clean = birthdayStr.trim().replace("/", "-")
        val parts = clean.split("-")
        if (parts.size != 3) return null
        val bMonth = parts[1].toInt()
        val bDay = parts[2].toInt()
        val today = LocalDate.now()
        var nextBday = LocalDate.of(today.year, bMonth, bDay)
        if (nextBday.isBefore(today)) {
            nextBday = nextBday.plusYears(1)
        }
        ChronoUnit.DAYS.between(today, nextBday).toInt()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun StandardProfileHeaderCard(
    name: String,
    roleTitle: String,
    profilePicture: String?,
    username: String = "",
    onEditClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null,
    onEditNameClick: (() -> Unit)? = null
) {
    val handleEdit: () -> Unit = {
        onEditClick?.invoke() ?: onEditNameClick?.invoke() ?: onAvatarClick?.invoke() ?: Unit
    }
    val hasEditAction = onEditClick != null || onEditNameClick != null || onAvatarClick != null

    val activeAccent = LocalThemeAccent.current
    val isDark = LocalIsDarkTheme.current
    val heroShape = RoundedCornerShape(24.dp)

    val gradientColors = remember(activeAccent, isDark) {
        if (isDark) {
            // Deep Midnight to Rich Accent — Starts from deep dark canvas tint (~80% darker) and smoothly transitions into deep rich theme tone (~40% darker)
            val deepMidnight = Color(
                red = (activeAccent.red * 0.18f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.18f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.18f).coerceIn(0f, 1f),
                alpha = 1f
            )
            val midDark = Color(
                red = (activeAccent.red * 0.40f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.40f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.40f).coerceIn(0f, 1f),
                alpha = 1f
            )
            val richAccent = Color(
                red = (activeAccent.red * 0.58f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.58f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.58f).coerceIn(0f, 1f),
                alpha = 1f
            )
            listOf(deepMidnight, midDark, richAccent)
        } else {
            // Calibrated Light Mode — Rich Theme Base (~25-30% darker) to Pure Theme Accent
            val richBase = Color(
                red = (activeAccent.red * 0.70f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.70f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.70f).coerceIn(0f, 1f),
                alpha = 1f
            )
            val midTone = Color(
                red = (activeAccent.red * 0.88f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.88f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.88f).coerceIn(0f, 1f),
                alpha = 1f
            )
            val vibrantAccent = activeAccent
            listOf(richBase, midTone, vibrantAccent)
        }
    }
    val gradStart = gradientColors.first()

    NeumorphicCard(
        shape = heroShape,
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = gradStart,
        contentPadding = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors),
                    shape = heroShape
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Circular Avatar with Vibrant Ring
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        name = name.toProperCase(),
                        profilePicture = profilePicture,
                        size = 72.dp,
                        border = BorderStroke(3.dp, Color.White.copy(alpha = 0.9f)),
                        backgroundColor = gradStart,
                        textColor = Color.White,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Middle: Name and Badges
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name.toProperCase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Role Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White
                        ) {
                            Text(
                                text = roleTitle.uppercase(),
                                color = activeAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Username Pill
                        if (username.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "@${username.lowercase()}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Right: Edit Pencil Button
                if (hasEditAction) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { handleEdit() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenureAndBirthdayRow(
    joinDate: String?,
    birthday: String?,
    modifier: Modifier = Modifier
) {
    val activeAccent = LocalThemeAccent.current
    val tenure = remember(joinDate) { calculateTenure(joinDate) }
    val daysUntilBday = remember(birthday) { calculateDaysUntilBirthday(birthday) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Tenure Card
        NeumorphicCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            style = NeumorphicElevationStyle.Raised,
            surfaceColor = NeumorphicTokens.Surface,
            contentPadding = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Tenure",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeumorphicTokens.TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (tenure != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (tenure.years > 0) {
                            Text(
                                text = "${tenure.years}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = " yr ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                            Text(
                                text = "${tenure.months}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = " mo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                        } else {
                            Text(
                                text = "${tenure.months}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = " mo ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                            Text(
                                text = "${tenure.days}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = " d",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                } else {
                    Text(
                        text = "--",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeumorphicTokens.TextMuted
                    )
                }
            }
        }

        // 2. Birthday Countdown Card
        NeumorphicCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            style = NeumorphicElevationStyle.Raised,
            surfaceColor = NeumorphicTokens.Surface,
            contentPadding = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                val bdayLabel = if (daysUntilBday != null && daysUntilBday >= 2) "BIRTHDAY IN" else "BIRTHDAY"
                Text(
                    text = bdayLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                when {
                    daysUntilBday == 0 -> {
                        Text(
                            text = "Today! 🎉",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                    }
                    daysUntilBday == 1 -> {
                        Text(
                            text = "Tomorrow! 🎂",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeAccent
                        )
                    }
                    daysUntilBday != null -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$daysUntilBday",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = " days",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeumorphicTokens.TextMuted
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "--",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyOffsCard(
    weeklyOffs: List<String>,
    modifier: Modifier = Modifier
) {
    val activeAccent = LocalThemeAccent.current

    NeumorphicCard(
        shape = RoundedCornerShape(20.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "WEEKLY OFFS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeAccent,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val days = listOf(
                "Monday" to "M",
                "Tuesday" to "T",
                "Wednesday" to "W",
                "Thursday" to "T",
                "Friday" to "F",
                "Saturday" to "S",
                "Sunday" to "S"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEach { (fullName, letter) ->
                    val isOff = weeklyOffs.any { it.equals(fullName, ignoreCase = true) }
                    if (isOff) {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(58.dp)
                                .neumorphicAccentGlow(glowColor = activeAccent.copy(alpha = 0.35f), radius = 8.dp)
                                .background(activeAccent, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = letter,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "OFF",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(58.dp)
                                .neumorphicInset(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = NeumorphicTokens.ElevationInsetSubtle
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                color = NeumorphicTokens.TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetailsCard(
    username: String,
    email: String,
    phone: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activeAccent = LocalThemeAccent.current

    NeumorphicCard(
        shape = RoundedCornerShape(20.dp),
        style = NeumorphicElevationStyle.Raised,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 16.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ACCOUNT DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeAccent,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Domain Username
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(activeAccent.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Domain username",
                        fontSize = 11.sp,
                        color = NeumorphicTokens.TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = username.ifBlank { "--" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMain
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )

            // 2. Registered Email
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(activeAccent.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Registered email",
                        fontSize = 11.sp,
                        color = NeumorphicTokens.TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = email.ifBlank { "--" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (email.isNotBlank()) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(email))
                            Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Email",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )

            // 3. Contact Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(activeAccent.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Contact phone",
                        fontSize = 11.sp,
                        color = NeumorphicTokens.TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = phone.ifBlank { "--" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeumorphicTokens.TextMain
                    )
                }
                if (phone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NorthEast,
                            contentDescription = "Call",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val activeAccent = LocalThemeAccent.current

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon in Tinted Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(activeAccent.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = activeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val fieldStyle = LocalTextStyle.current.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        readOnly = readOnly,
                        enabled = onClick == null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = singleLine,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        textStyle = fieldStyle,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
                            ) {
                                if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                                    Text(
                                        text = placeholder,
                                        style = fieldStyle.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Normal
                                        ),
                                        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (trailingIcon != null) {
                    trailingIcon()
                }
            }
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Unified Edit Profile & Security Modal Dialog
 * Consolidates photo, personal details, birthday, joining date, and password into one elegant popup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEditProfileDialog(
    show: Boolean,
    currentName: String,
    currentPhone: String,
    currentEmail: String,
    currentJoinDate: String,
    currentBirthday: String,
    currentProfilePicture: String?,
    onDismiss: () -> Unit,
    onRemovePhoto: ((onResult: (Boolean, String?) -> Unit) -> Unit)? = null,
    onSave: (
        name: String,
        phone: String,
        email: String,
        joinDate: String,
        birthday: String,
        newProfilePicture: String?, // null = keep unchanged, "" = remove, "data:..." = update
        currentPassword: String?,
        newPassword: String?
    ) -> Unit
) {
    if (!show) return

    val context = LocalContext.current
    val activeAccent = LocalThemeAccent.current
    val isDark = LocalIsDarkTheme.current
    val darker = remember(activeAccent, isDark) {
        if (isDark) {
            Color(
                red = (activeAccent.red * 0.70f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.70f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.70f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = (activeAccent.red * 0.65f).coerceIn(0f, 1f),
                green = (activeAccent.green * 0.65f).coerceIn(0f, 1f),
                blue = (activeAccent.blue * 0.65f).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
    }
    val lighter = remember(activeAccent, isDark) {
        Color(
            red = (activeAccent.red * 1.25f).coerceIn(0f, 1f),
            green = (activeAccent.green * 1.25f).coerceIn(0f, 1f),
            blue = (activeAccent.blue * 1.25f).coerceIn(0f, 1f),
            alpha = 1f
        )
    }

    var nameInput by remember(currentName, show) { mutableStateOf(currentName.toProperCase()) }
    var phoneInput by remember(currentPhone, show) { mutableStateOf(if (currentPhone.contains("*")) "" else currentPhone) }
    var emailInput by remember(currentEmail, show) { mutableStateOf(currentEmail) }
    var joinDateInput by remember(currentJoinDate, show) { mutableStateOf(currentJoinDate) }
    var birthdayInput by remember(currentBirthday, show) { mutableStateOf(currentBirthday) }

    // Photo state: null = unchanged, "" = remove, "data:..." = new image
    var pendingPhotoBase64 by remember(show) { mutableStateOf<String?>(null) }
    var showConfirmRemovePhotoDialog by remember { mutableStateOf(false) }
    var isRemovingPhoto by remember { mutableStateOf(false) }
    val effectivePhoto = when {
        pendingPhotoBase64 != null -> pendingPhotoBase64!!.takeIf { it.isNotBlank() }
        else -> currentProfilePicture
    }

    // Password fields
    var currentPassword by remember(show) { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPassword by remember(show) { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember(show) { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val b64 = compressImageUriToBase64(context, uri, maxDimension = 256, quality = 85)
            if (b64 != null) {
                pendingPhotoBase64 = b64
            } else {
                Toast.makeText(context, "Could not process selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDatePicker(initialDate: String, onSelect: (String) -> Unit) {
        val cal = Calendar.getInstance()
        if (initialDate.isNotBlank()) {
            try {
                val parts = initialDate.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onSelect(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val isNameValid = nameInput.trim().length in 2..60
    val isEmailValid = emailInput.isBlank() || (emailInput.contains("@") && emailInput.contains("."))
    val isPasswordChangeAttempted = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()
    val isCurrentPasswordOk = !isPasswordChangeAttempted || currentPassword.isNotBlank()
    val isPasswordLengthOk = !isPasswordChangeAttempted || newPassword.length >= 6
    val isPasswordMatch = !isPasswordChangeAttempted || (newPassword == confirmPassword && newPassword.isNotEmpty())
    val canSave = isNameValid && isEmailValid && isCurrentPasswordOk && isPasswordLengthOk && isPasswordMatch

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyDialogBlurBehind()
        val isDark = LocalIsDarkTheme.current
        val isDialogLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .then(if (isDialogLandscape) Modifier.widthIn(max = 540.dp) else Modifier)
                .padding(vertical = 12.dp)
                .neumorphicFloating(shape = RoundedCornerShape(24.dp))
                .background(NeumorphicTokens.Surface, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar with camera badge
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatar(
                            name = nameInput.ifBlank { "User" }.toProperCase(),
                            profilePicture = effectivePhoto,
                            size = 46.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(activeAccent)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Change photo",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Edit profile",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeumorphicTokens.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Personal details & security",
                            fontSize = 11.sp,
                            color = NeumorphicTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!effectivePhoto.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(enabled = !isRemovingPhoto) { showConfirmRemovePhotoDialog = true }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove photo",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isRemovingPhoto) "Removing photo..." else "Remove photo",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Close button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .neumorphicRaised(shape = RoundedCornerShape(10.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Row 1 (Dual-Column 50/50): Full name * | Contact phone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeumorphicFormInput(
                        value = nameInput,
                        onValueChange = { if (it.length <= 60) nameInput = it },
                        label = "Full name *",
                        placeholder = "Enter full name",
                        isError = !isNameValid,
                        errorMessage = if (!isNameValid) "Must be 2-60 chars" else null,
                        modifier = Modifier.weight(1f)
                    )

                    NeumorphicFormInput(
                        value = phoneInput,
                        onValueChange = { if (it.length <= 20) phoneInput = it },
                        label = "Phone",
                        placeholder = "Enter phone number",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2 (Single-Column 100%): Corporate email *
                NeumorphicFormInput(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = "Corporate email *",
                    placeholder = "Enter email address",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = !isEmailValid,
                    errorMessage = if (!isEmailValid) "Valid email required" else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Row 3 (Dual-Column 50/50): Join date | Birthday
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeumorphicFormInput(
                        value = joinDateInput,
                        onValueChange = { joinDateInput = it },
                        label = "Join date",
                        placeholder = "YYYY-MM-DD",
                        readOnly = true,
                        onClick = { openDatePicker(joinDateInput) { joinDateInput = it } },
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Pick Join Date",
                                tint = activeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    NeumorphicFormInput(
                        value = birthdayInput,
                        onValueChange = { birthdayInput = it },
                        label = "Birthday",
                        placeholder = "YYYY-MM-DD",
                        readOnly = true,
                        onClick = { openDatePicker(birthdayInput) { birthdayInput = it } },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Cake,
                                contentDescription = "Pick Birthday",
                                tint = activeAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Security Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECURITY & PASSWORD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activeAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "(Leave blank to keep current)",
                        fontSize = 10.5.sp,
                        color = NeumorphicTokens.TextMuted
                    )
                }

                // Row 4 (Single-Column 100%): Current password
                NeumorphicFormInput(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = "Current password",
                    placeholder = "Required only if changing password",
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Icon(
                            imageVector = if (currentPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            tint = NeumorphicTokens.TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { currentPasswordVisible = !currentPasswordVisible }
                        )
                    },
                    isError = isPasswordChangeAttempted && currentPassword.isBlank(),
                    errorMessage = if (isPasswordChangeAttempted && currentPassword.isBlank()) "Current password required" else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Row 5 (Dual-Column 50/50): New password | Confirm password
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeumorphicFormInput(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New password",
                        placeholder = "Min 6 chars",
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { newPasswordVisible = !newPasswordVisible }
                            )
                        },
                        isError = isPasswordChangeAttempted && !isPasswordLengthOk,
                        errorMessage = if (isPasswordChangeAttempted && !isPasswordLengthOk) "Min 6 characters" else null,
                        modifier = Modifier.weight(1f)
                    )

                    NeumorphicFormInput(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm password",
                        placeholder = "Confirm password",
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = NeumorphicTokens.TextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                            )
                        },
                        isError = isPasswordChangeAttempted && !isPasswordMatch,
                        errorMessage = if (isPasswordChangeAttempted && !isPasswordMatch) "Passwords mismatch" else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .neumorphicClickable(
                                shape = RoundedCornerShape(12.dp),
                                enabled = true
                            ) { onDismiss() }
                            .background(NeumorphicTokens.Surface, RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isDark) Color(0x1FFFFFFF) else Color(0x1F94A3B8),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeumorphicTokens.TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .neumorphicClickable(
                                shape = RoundedCornerShape(12.dp),
                                enabled = canSave
                            ) {
                                val trimmedName = nameInput.trim().toProperCase()
                                val trimmedPhone = phoneInput.trim()
                                val trimmedEmail = emailInput.trim().lowercase()
                                val trimmedJoinDate = joinDateInput.trim()
                                val trimmedBirthday = birthdayInput.trim()
                                val currPassToVerify = currentPassword.trim().takeIf { it.isNotBlank() }
                                val passToUpdate = newPassword.trim().takeIf { it.isNotBlank() }

                                onSave(
                                    trimmedName,
                                    trimmedPhone,
                                    trimmedEmail,
                                    trimmedJoinDate,
                                    trimmedBirthday,
                                    pendingPhotoBase64,
                                    currPassToVerify,
                                    passToUpdate
                                )
                                onDismiss()
                            }
                            .background(
                                if (canSave) activeAccent else activeAccent.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Save changes",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Immediate Remove Profile Photo Confirmation Dialog
    if (showConfirmRemovePhotoDialog) {
        Dialog(
            onDismissRequest = { if (!isRemovingPhoto) showConfirmRemovePhotoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ApplyDialogBlurBehind(blurRadiusPx = 50, dimAmount = 0.5f)
            val initials = nameInput.ifBlank { "User" }
                .trim()
                .split(" ")
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .take(2)
                .joinToString("")
                .ifBlank { "U" }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0x990A0F1A) else Color(0xB8F1F5F9))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isRemovingPhoto) showConfirmRemovePhotoDialog = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {}
                        .neumorphicFloating(shape = RoundedCornerShape(20.dp))
                        .background(NeumorphicTokens.Surface, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Remove profile photo?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeumorphicTokens.TextMain
                        )
                        Text(
                            text = "Are you sure you want to remove your profile photo right away? Your avatar will revert to initials ($initials).",
                            fontSize = 12.5.sp,
                            color = NeumorphicTokens.TextMuted,
                            lineHeight = 17.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neumorphicClickable(
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isRemovingPhoto
                                    ) {
                                        showConfirmRemovePhotoDialog = false
                                    }
                                    .background(NeumorphicTokens.Surface, RoundedCornerShape(10.dp))
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0x1FFFFFFF) else Color(0x1F94A3B8),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NeumorphicTokens.TextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .neumorphicClickable(
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isRemovingPhoto
                                    ) {
                                        if (onRemovePhoto != null) {
                                            isRemovingPhoto = true
                                            onRemovePhoto { success, errMsg ->
                                                isRemovingPhoto = false
                                                showConfirmRemovePhotoDialog = false
                                                if (success) {
                                                    pendingPhotoBase64 = ""
                                                    Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, errMsg ?: "Failed to remove photo", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            pendingPhotoBase64 = ""
                                            showConfirmRemovePhotoDialog = false
                                            Toast.makeText(context, "Photo removed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .background(Color(0xFFEF4444), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRemovingPhoto) "Removing..." else "Remove",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun resolveBroadcastMentions(
    rawText: String,
    recipientFullName: String,
    recipientUsername: String,
    engineers: List<com.rosterx.domain.model.Engineer>
): String {
    if (rawText.isBlank()) return rawText
    var text = rawText
    text = text.replace(Regex("@name\\b", RegexOption.IGNORE_CASE), recipientFullName)
    text = text.replace(Regex("\\{\\s*(?:Their\\s+name|their\\s+name|name)\\s*\\}", RegexOption.IGNORE_CASE), recipientFullName)
    text = text.replace(Regex("@\\s*\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), "@$recipientUsername")
    text = text.replace(Regex("\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), recipientUsername)

    val candidates = LinkedHashSet<String>()
    candidates.add("Everyone")
    candidates.add("everyone")
    candidates.add("all")
    candidates.add("team")
    candidates.add("name")

    engineers.forEach { eng ->
        eng.fullName.takeIf { it.isNotBlank() }?.let { candidates.add(it.trim()) }
        eng.name.takeIf { it.isNotBlank() }?.let { candidates.add(it.trim()) }
        eng.username.takeIf { it.isNotBlank() }?.let { candidates.add(it.trim()) }
    }

    // Dynamic extraction: if message contains @<FirstWord> <Initials/Words> followed by : / - / ( / [ / newline / comma
    // where FirstWord matches a known engineer first name/username, add the full multi-word token!
    val knownFirstWords = HashSet<String>()
    candidates.forEach { c ->
        val first = c.split("\\s+".toRegex()).firstOrNull()?.trim() ?: ""
        if (first.length >= 3) knownFirstWords.add(first.lowercase())
    }

    val colonMentionRegex = Regex("""@([A-Za-z0-9_.-]+(?:\s+[A-Za-z0-9_.-]+)+)(?=\s*[:\-\(\[\n,])""")
    colonMentionRegex.findAll(text).forEach { match ->
        val fullToken = match.groupValues[1].trim()
        val first = fullToken.split("\\s+".toRegex()).firstOrNull()?.trim()?.lowercase() ?: ""
        if (first in knownFirstWords) {
            candidates.add(fullToken)
        }
    }

    val sorted = candidates.filter { it.isNotBlank() }.sortedByDescending { it.length }
    val pattern = if (sorted.isNotEmpty()) {
        val branchList = sorted.map { Regex.escape(it) + "(?![a-zA-Z0-9_])" }
        "@(${branchList.joinToString("|")}|[a-zA-Z0-9_.-]+)"
    } else {
        "@([a-zA-Z0-9_.-]+)"
    }
    val mentionRegex = Regex(pattern, RegexOption.IGNORE_CASE)

    text = mentionRegex.replace(text) { matchResult ->
        val uname = matchResult.groupValues[1].trim()
        when {
            uname.equals("all", ignoreCase = true) || uname.equals("everyone", ignoreCase = true) || uname.equals("team", ignoreCase = true) -> "@Everyone"
            uname.equals("name", ignoreCase = true) -> recipientFullName
            else -> {
                val found = engineers.find {
                    it.username.equals(uname, ignoreCase = true) ||
                    it.fullName.equals(uname, ignoreCase = true) ||
                    it.name.equals(uname, ignoreCase = true)
                }
                val resolved = found?.fullName?.takeIf { it.isNotBlank() } ?: found?.name ?: uname
                "@$resolved"
            }
        }
    }
    return text
}

fun formatBroadcastMentions(
    resolvedMsg: String,
    engineers: List<com.rosterx.domain.model.Engineer>,
    activeAccent: Color
): AnnotatedString {
    if (resolvedMsg.isBlank()) return AnnotatedString("")

    val candidates = LinkedHashSet<String>()
    candidates.add("Everyone")
    candidates.add("everyone")
    candidates.add("all")
    candidates.add("team")

    engineers.forEach { eng ->
        eng.fullName.takeIf { it.isNotBlank() }?.let { candidates.add(it.trim()) }
        eng.name.takeIf { it.isNotBlank() }?.let { candidates.add(it.trim()) }
        eng.username.takeIf { it.isNotBlank() }?.let { candidates.add(it.trim()) }
    }

    // Dynamic extraction: if message contains @<FirstWord> <Initials/Words> followed by : / - / ( / [ / newline / comma
    // where FirstWord matches a known engineer first name/username, add the full multi-word token!
    val knownFirstWords = HashSet<String>()
    candidates.forEach { c ->
        val first = c.split("\\s+".toRegex()).firstOrNull()?.trim() ?: ""
        if (first.length >= 3) knownFirstWords.add(first.lowercase())
    }

    val colonMentionRegex = Regex("""@([A-Za-z0-9_.-]+(?:\s+[A-Za-z0-9_.-]+)+)(?=\s*[:\-\(\[\n,])""")
    colonMentionRegex.findAll(resolvedMsg).forEach { match ->
        val fullToken = match.groupValues[1].trim()
        val first = fullToken.split("\\s+".toRegex()).firstOrNull()?.trim()?.lowercase() ?: ""
        if (first in knownFirstWords) {
            candidates.add(fullToken)
        }
    }

    val sorted = candidates.filter { it.isNotBlank() }.sortedByDescending { it.length }
    val pattern = if (sorted.isNotEmpty()) {
        val branchList = sorted.map { Regex.escape(it) + "(?![a-zA-Z0-9_])" }
        "@(?:${branchList.joinToString("|")}|[a-zA-Z0-9_.-]+)"
    } else {
        "@[a-zA-Z0-9_.-]+"
    }

    val mentionRegex = Regex(pattern, RegexOption.IGNORE_CASE)

    return buildAnnotatedString {
        var lastIndex = 0
        mentionRegex.findAll(resolvedMsg).forEach { match ->
            if (match.range.first > lastIndex) {
                append(resolvedMsg.substring(lastIndex, match.range.first))
            }
            pushStyle(
                SpanStyle(
                    color = activeAccent,
                    fontWeight = FontWeight.Bold,
                    background = activeAccent.copy(alpha = 0.12f)
                )
            )
            append(match.value)
            pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < resolvedMsg.length) {
            append(resolvedMsg.substring(lastIndex))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickNotificationsBottomSheet(
    show: Boolean,
    notifications: List<com.rosterx.data.remote.NotificationDto>,
    dismissedNotificationIds: Set<String>,
    onDismiss: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    loggedInEngineer: com.rosterx.domain.model.Engineer? = null,
    engineers: List<com.rosterx.domain.model.Engineer> = emptyList(),
    onOpenCelebrationWishes: (
        celebrationType: CelebrationType,
        celebrantUser: String,
        celebrantName: String,
        anniversaryYears: Int,
        celebrantPhone: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onNavigateToCredentialTracker: (() -> Unit)? = null,
    onNavigateToMonthRoster: (() -> Unit)? = null
) {
    if (!show) return

    val activeAccent = LocalThemeAccent.current
    val coroutineScope = rememberCoroutineScope()
    var dismissingIds by remember { mutableStateOf(setOf<String>()) }

    val engName = loggedInEngineer?.fullName?.takeIf { it.isNotBlank() } ?: loggedInEngineer?.name ?: "Engineer"
    val engUsername = loggedInEngineer?.username ?: engName.lowercase().replace(" ", "")

    val activeNotifs = remember(notifications, dismissedNotificationIds, dismissingIds) {
        notifications.mapIndexed { idx, n ->
            val id = n.id ?: "idx_$idx"
            id to n
        }.filter { (id, n) ->
            if (n.isExpiredCelebration()) return@filter false
            id !in dismissedNotificationIds || id in dismissingIds
        }
    }

    val configuration = LocalConfiguration.current
    // Fixed height: sits strictly below the top bar (~92dp from top of screen), ensuring zero resizing/jumping when items are dismissed
    val sheetHeight = (configuration.screenHeightDp.dp - 92.dp).coerceAtLeast(360.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeumorphicTokens.Surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeumorphicTokens.SurfaceContrast.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = activeAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "NOTIFICATIONS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = NeumorphicTokens.TextMain
                        )
                        val unreadCount = activeNotifs.count { it.first !in dismissingIds }
                        Text(
                            text = if (unreadCount > 0) "$unreadCount active alert${if (unreadCount > 1) "s" else ""}" else "All caught up",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeumorphicTokens.TextMuted
                        )
                    }
                }

                if (activeNotifs.any { it.first !in dismissingIds }) {
                    TextButton(
                        onClick = {
                            val currentIds = activeNotifs.map { it.first }.filter { it !in dismissingIds }
                            coroutineScope.launch {
                                currentIds.forEach { id ->
                                    dismissingIds = dismissingIds + id
                                    delay(60)
                                }
                                delay(280)
                                onClearAllNotifications()
                                dismissingIds = dismissingIds - currentIds.toSet()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear All",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val isListEmpty = activeNotifs.isEmpty() || activeNotifs.all { it.first in dismissingIds }

            if (isListEmpty) {
                HorizontalDivider(
                    color = NeumorphicTokens.SurfaceContrast,
                    thickness = 1.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        style = NeumorphicElevationStyle.Raised,
                        surfaceColor = NeumorphicTokens.Surface,
                        contentPadding = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🎉", fontSize = 40.sp)
                            Text(
                                text = "No Active Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NeumorphicTokens.TextMain
                            )
                            Text(
                                text = "You're all caught up with your roster and operational briefings!",
                                fontSize = 12.sp,
                                color = NeumorphicTokens.TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = activeAccent),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Close",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer { clip = false },
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, start = 4.dp, end = 4.dp)
                ) {
                    item(key = "header_divider_spacer") {
                        Column {
                            HorizontalDivider(
                                color = NeumorphicTokens.SurfaceContrast,
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                    items(
                        items = activeNotifs,
                        key = { it.first }
                    ) { (notifId, notif) ->
                        val isCelebrationNotif = notif.celebrationType != null ||
                            notif.sender?.contains("Celebration", ignoreCase = true) == true ||
                            notif.title?.contains("🎂") == true || notif.title?.contains("🏆") == true

                        val isCredentialAlert = notif.category == "credential_expiry" ||
                            notif.title?.contains("Credential", ignoreCase = true) == true ||
                            notif.rawMessage?.contains("Credential", ignoreCase = true) == true

                        val isRosterAlert = notif.priority?.lowercase() == "shift_update" ||
                            notif.category == "shift_update" ||
                            notif.title?.contains("Roster", ignoreCase = true) == true ||
                            notif.title?.contains("Shift", ignoreCase = true) == true ||
                            notif.rawMessage?.contains("Roster", ignoreCase = true) == true ||
                            notif.rawMessage?.contains("Shift", ignoreCase = true) == true

                        val (badgeText, badgeIcon, badgeColor) = when {
                            isCelebrationNotif -> Triple("CELEBRATION 🎉", Icons.Default.Celebration, Color(0xFFF59E0B))
                            isCredentialAlert -> Triple("SECURITY", Icons.Default.Security, Color(0xFFE11D48))
                            isRosterAlert -> Triple("ROSTER UPDATE", Icons.Default.CalendarMonth, Color(0xFF0284C7))
                            notif.priority?.lowercase() == "urgent" -> Triple("URGENT", Icons.Default.Warning, Color(0xFFEF4444))
                            notif.priority?.lowercase() == "broadcast" -> Triple("ANNOUNCEMENT", Icons.Default.Campaign, Color(0xFF8B5CF6))
                            else -> Triple("OPERATIONS", Icons.Default.Info, Color(0xFF10B981))
                        }

                        val resolvedTitle = resolveBroadcastMentions(
                            rawText = notif.title ?: "Broadcast Announcement",
                            recipientFullName = engName,
                            recipientUsername = engUsername,
                            engineers = engineers
                        )

                        val rawResolved = resolveBroadcastMentions(
                            rawText = notif.rawMessage ?: notif.samplePersonalized ?: "",
                            recipientFullName = engName,
                            recipientUsername = engUsername,
                            engineers = engineers
                        )

                        val dateIsoRegex = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
                        val resolvedMsg = dateIsoRegex.replace(rawResolved) { m ->
                            try {
                                val y = m.groupValues[1].toInt()
                                val mo = m.groupValues[2].toInt()
                                val d = m.groupValues[3].toInt()
                                java.time.LocalDate.of(y, mo, d).format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy", java.util.Locale.US))
                            } catch (_: Throwable) {
                                m.value
                            }
                        }

                        val formattedMessage = remember(resolvedMsg, activeAccent, engineers) {
                            formatBroadcastMentions(
                                resolvedMsg = resolvedMsg,
                                engineers = engineers,
                                activeAccent = activeAccent
                            )
                        }

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                    dismissingIds = dismissingIds + notifId
                                    coroutineScope.launch {
                                        delay(280)
                                        onDismissNotification(notifId)
                                        dismissingIds = dismissingIds - notifId
                                    }
                                    true
                                } else false
                            }
                        )

                        AnimatedVisibility(
                            visible = notifId !in dismissingIds && notifId !in dismissedNotificationIds,
                            enter = fadeIn(tween(200)) + expandVertically(animationSpec = tween(200)),
                            exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280)) + shrinkVertically(animationSpec = tween(250)),
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            )
                        ) {
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val isSwiping = dismissState.dismissDirection != SwipeToDismissBoxValue.Settled
                                    val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                        Alignment.CenterStart
                                    } else {
                                        Alignment.CenterEnd
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isSwiping) Color(0xFFEF4444).copy(alpha = 0.18f) else Color.Transparent
                                            )
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        if (isSwiping) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Dismiss",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            ) {
                                NeumorphicCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            dismissingIds = dismissingIds + notifId
                                            coroutineScope.launch {
                                                delay(80)
                                                onDismissNotification(notifId)
                                            }
                                            onDismiss()
                                            if (isCelebrationNotif) {
                                                val cType = when (notif.celebrationType?.lowercase()) {
                                                    "both" -> CelebrationType.BOTH
                                                    "anniversary" -> CelebrationType.ANNIVERSARY
                                                    else -> {
                                                        if (notif.title?.contains("Anniversary", ignoreCase = true) == true ||
                                                            notif.rawMessage?.contains("Anniversary", ignoreCase = true) == true
                                                        ) CelebrationType.ANNIVERSARY else CelebrationType.BIRTHDAY
                                                    }
                                                }
                                                val cUser = notif.celebrationUser.orEmpty()
                                                val engObj = engineers.find { it.username.equals(cUser, ignoreCase = true) }
                                                val cName = notif.celebrationName?.takeIf { it.isNotBlank() }
                                                    ?: engObj?.fullName?.takeIf { it.isNotBlank() }
                                                    ?: engObj?.name?.takeIf { it.isNotBlank() }
                                                    ?: cUser
                                                val cYears = notif.celebrationYears ?: 1
                                                val cPhone = notif.celebrationPhone?.takeIf { it.isNotBlank() } ?: engObj?.phone
                                                onOpenCelebrationWishes(cType, cUser, cName, cYears, cPhone)
                                            } else if (isCredentialAlert && onNavigateToCredentialTracker != null) {
                                                onNavigateToCredentialTracker()
                                            } else if (isRosterAlert && onNavigateToMonthRoster != null) {
                                                onNavigateToMonthRoster()
                                            }
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    style = NeumorphicElevationStyle.Raised,
                                    surfaceColor = NeumorphicTokens.Surface,
                                    contentPadding = 16.dp
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(badgeColor.copy(alpha = 0.14f))
                                                        .border(
                                                            BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.3f)),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = badgeIcon,
                                                            contentDescription = null,
                                                            tint = badgeColor,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = badgeText,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = badgeColor
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = resolvedTitle,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeumorphicTokens.TextMain,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Neumorphic Inset Close Button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .neumorphicInset(
                                                        shape = CircleShape,
                                                        elevation = NeumorphicTokens.ElevationInset,
                                                        surfaceColor = NeumorphicTokens.Surface
                                                    )
                                                    .clickable {
                                                        dismissingIds = dismissingIds + notifId
                                                        coroutineScope.launch {
                                                            delay(280)
                                                            onDismissNotification(notifId)
                                                            dismissingIds = dismissingIds - notifId
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear Notification",
                                                    tint = NeumorphicTokens.TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (resolvedMsg.isNotBlank()) {
                                            Text(
                                                text = formattedMessage,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                color = NeumorphicTokens.TextMain.copy(alpha = 0.88f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "By ${notif.sender ?: "Operations Desk"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeumorphicTokens.TextMuted
                                            )
                                            val formattedTime = formatNotificationTime(notif.createdAt, notif.timestamp)
                                            if (formattedTime.isNotBlank()) {
                                                Text(
                                                    text = formattedTime,
                                                    fontSize = 10.sp,
                                                    color = NeumorphicTokens.TextMuted
                                                )
                                            }
                                        }

                                        if (isCredentialAlert || isRosterAlert || isCelebrationNotif) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        isCelebrationNotif -> "Tap to open wishes ✨"
                                                        isCredentialAlert -> "Tap to view Credential Tracker →"
                                                        isRosterAlert -> "Tap to view Month Roster →"
                                                        else -> "Tap to view →"
                                                    },
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

// Deprecated legacy dialogs kept for backwards compatibility
@Composable
fun ProfilePhotoActionDialog(
    show: Boolean,
    hasExistingPhoto: Boolean,
    onDismiss: () -> Unit,
    onChooseNewPhoto: () -> Unit,
    onRemovePhotoClick: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Photo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select an action for your profile picture:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        onDismiss()
                        onChooseNewPhoto()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasExistingPhoto) "Choose New Photo" else "Upload Photo")
                }
                if (hasExistingPhoto) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRemovePhotoClick()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Photo")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmRemovePhotoDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Remove Profile Photo?", fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to remove your profile picture? Your initials monogram will be displayed across the app and web portals instead.")
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditNameDialog(
    show: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!show) return
    var nameInput by remember(currentName) { mutableStateOf(currentName.toProperCase()) }
    val isValid = nameInput.trim().length in 2..60 && nameInput.trim() != currentName.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Full Name", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Update your display name across RosterX operations console, reports, and rosters:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 60) nameInput = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${nameInput.length} / 60 characters",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = nameInput.trim().toProperCase()
                    if (trimmed.length in 2..60) {
                        onDismiss()
                        onSave(trimmed)
                    }
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class AppUpdateInfo(
    val latestVersion: String,
    val installedVersion: String,
    val downloadUrl: String,
    val isMandatory: Boolean
)

/**
 * Numerically compares two semantic version strings (e.g. "1.34.0" vs "1.33.1").
 * Returns > 0 if v1 > v2, < 0 if v1 < v2, 0 if equal.
 */
fun compareSemanticVersions(v1: String?, v2: String?): Int {
    if (v1.isNullOrBlank() || v2.isNullOrBlank()) return 0
    val clean1 = v1.trim().removePrefix("v").removePrefix("V").substringBefore("-").substringBefore("+")
    val clean2 = v2.trim().removePrefix("v").removePrefix("V").substringBefore("-").substringBefore("+")
    val parts1 = clean1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = clean2.split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(parts1.size, parts2.size)
    for (i in 0 until maxLen) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) return p1.compareTo(p2)
    }
    return 0
}

fun resolveAppUpdateInfo(
    serverInfo: com.rosterx.data.remote.ServerInfoResponse?,
    installedVersion: String
): AppUpdateInfo? {
    if (serverInfo == null) return null
    val remoteVersion = serverInfo.latestAndroidVersion?.takeIf { it.isNotBlank() }
        ?: serverInfo.version?.takeIf { it.isNotBlank() }
        ?: return null

    if (compareSemanticVersions(remoteVersion, installedVersion) <= 0) {
        return null
    }

    val minSupported = serverInfo.minSupportedVersion?.takeIf { it.isNotBlank() }
    val isMandatory = minSupported != null && compareSemanticVersions(installedVersion, minSupported) < 0
    val downloadUrl = serverInfo.apkDownloadUrl?.takeIf { it.isNotBlank() }
        ?: "https://github.com/shefeeqkk/RosterX/releases/latest"

    return AppUpdateInfo(
        latestVersion = remoteVersion.trim().removePrefix("v").removePrefix("V"),
        installedVersion = installedVersion.trim().removePrefix("v").removePrefix("V"),
        downloadUrl = downloadUrl,
        isMandatory = isMandatory
    )
}

@Composable
fun AppUpdateBannerCard(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeAccent = LocalThemeAccent.current
    val highlightColor = if (updateInfo.isMandatory) Color(0xFFEF4444) else activeAccent

    NeumorphicCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        style = NeumorphicElevationStyle.RaisedSubtle,
        surfaceColor = NeumorphicTokens.Surface,
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = highlightColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(highlightColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (updateInfo.isMandatory) Icons.Default.SystemSecurityUpdateWarning else Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = highlightColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (updateInfo.isMandatory) {
                        "Required update • v${updateInfo.latestVersion}"
                    } else {
                        "Update available • v${updateInfo.latestVersion}"
                    },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeumorphicTokens.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Installed: v${updateInfo.installedVersion}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = NeumorphicTokens.TextMuted,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(highlightColor)
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to open download link", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Download",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            if (!updateInfo.isMandatory) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss update banner",
                        tint = NeumorphicTokens.TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

