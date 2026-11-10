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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rosterx.ui.neumorphic.NeumorphicTokens
import com.rosterx.ui.neumorphic.neumorphicAccentGlow
import com.rosterx.ui.neumorphic.neumorphicInset
import com.rosterx.ui.neumorphic.neumorphicRaised
import com.rosterx.ui.theme.LocalIsDarkTheme
import com.rosterx.ui.theme.LocalThemeAccent
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
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
                Text(
                    text = "BIRTHDAY IN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeumorphicTokens.TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (daysUntilBday != null) {
                    if (daysUntilBday == 0) {
                        Text(
                            text = "TODAY! 🎉",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$daysUntilBday",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = if (daysUntilBday == 1) " day" else " days",
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

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        readOnly = readOnly,
                        enabled = onClick == null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = singleLine,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                                Text(
                                    text = placeholder,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            innerTextField()
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
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Header Row with [X], Title, and [Save]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Edit profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activeAccent
                    )

                    Button(
                        onClick = {
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
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeAccent,
                            disabledContainerColor = activeAccent.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Sleek Hero Card with Avatar & Actions
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = darker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(darker, activeAccent, lighter)
                                    )
                                )
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Avatar with white accent ring and camera badge
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(92.dp)
                                            .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                                            .padding(3.dp)
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UserAvatar(
                                            name = nameInput.ifBlank { "User" }.toProperCase(),
                                            profilePicture = effectivePhoto,
                                            size = 86.dp,
                                            backgroundColor = darker,
                                            textColor = Color.White,
                                            fontSize = 28.sp
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        shadowElevation = 3.dp,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .offset(x = 2.dp, y = 2.dp)
                                            .clickable {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Pick Photo",
                                                tint = activeAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                // Photo action pills
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = activeAccent
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Change photo",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = activeAccent
                                        )
                                    }

                                    if (!effectivePhoto.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = { showConfirmRemovePhotoDialog = true },
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Remove",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showConfirmRemovePhotoDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmRemovePhotoDialog = false },
                            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            title = { Text("Remove Profile Photo?", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("Are you sure you want to remove your profile photo? Your initials monogram will be displayed across the app and rosters instead.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        pendingPhotoBase64 = ""
                                        showConfirmRemovePhotoDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Remove")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmRemovePhotoDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // 2. Personal Information Section
                    Text(
                        text = "PERSONAL INFORMATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = activeAccent,
                        letterSpacing = 0.8.sp
                    )

                    EditProfileInputField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 60) nameInput = it },
                        label = "Full name *",
                        leadingIcon = Icons.Outlined.Person,
                        isError = !isNameValid,
                        errorMessage = if (!isNameValid) "Full name must be between 2 and 60 characters" else null
                    )

                    EditProfileInputField(
                        value = phoneInput,
                        onValueChange = { if (it.length <= 20) phoneInput = it },
                        label = "Contact phone",
                        leadingIcon = Icons.Outlined.Call,
                        keyboardType = KeyboardType.Phone
                    )

                    EditProfileInputField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = "Email address",
                        leadingIcon = Icons.Outlined.Mail,
                        keyboardType = KeyboardType.Email,
                        isError = !isEmailValid,
                        errorMessage = if (!isEmailValid) "Please enter a valid email address" else null
                    )

                    EditProfileInputField(
                        value = joinDateInput,
                        onValueChange = { joinDateInput = it },
                        label = "Joining date",
                        placeholder = "YYYY-MM-DD",
                        leadingIcon = Icons.Outlined.CalendarToday,
                        readOnly = true,
                        onClick = { openDatePicker(joinDateInput) { joinDateInput = it } },
                        trailingIcon = {
                            IconButton(onClick = { openDatePicker(joinDateInput) { joinDateInput = it } }) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Pick Joining Date",
                                    tint = activeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    EditProfileInputField(
                        value = birthdayInput,
                        onValueChange = { birthdayInput = it },
                        label = "Birthday",
                        placeholder = "YYYY-MM-DD",
                        leadingIcon = Icons.Outlined.Cake,
                        readOnly = true,
                        onClick = { openDatePicker(birthdayInput) { birthdayInput = it } },
                        trailingIcon = {
                            IconButton(onClick = { openDatePicker(birthdayInput) { birthdayInput = it } }) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = "Pick Birthday",
                                    tint = activeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    // 3. Security & Password Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY & PASSWORD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeAccent,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "· OPTIONAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "Leave the password fields blank to keep your current password.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Current password
                    EditProfileInputField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Current password",
                        placeholder = "••••••••",
                        leadingIcon = Icons.Outlined.Lock,
                        isPassword = true,
                        isPasswordVisible = currentPasswordVisible,
                        isError = isPasswordChangeAttempted && currentPassword.isBlank(),
                        errorMessage = if (isPasswordChangeAttempted && currentPassword.isBlank()) "Current password is required to change password" else null,
                        trailingIcon = {
                            IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                Icon(
                                    imageVector = if (currentPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    // New password
                    EditProfileInputField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = "New password",
                        placeholder = "Leave blank to keep current",
                        leadingIcon = Icons.Outlined.Key,
                        isPassword = true,
                        isPasswordVisible = newPasswordVisible,
                        isError = isPasswordChangeAttempted && !isPasswordLengthOk,
                        errorMessage = if (isPasswordChangeAttempted && !isPasswordLengthOk) "Password must be at least 6 characters" else null,
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    // Confirm new password
                    EditProfileInputField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm new password",
                        placeholder = "Repeat new password",
                        leadingIcon = Icons.Outlined.Key,
                        isPassword = true,
                        isPasswordVisible = confirmPasswordVisible,
                        isError = isPasswordChangeAttempted && !isPasswordMatch,
                        errorMessage = if (isPasswordChangeAttempted && !isPasswordMatch) "Passwords do not match" else null,
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotificationsBottomSheet(
    show: Boolean,
    notifications: List<com.rosterx.data.remote.NotificationDto>,
    dismissedNotificationIds: Set<String>,
    onDismiss: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    loggedInEngineer: com.rosterx.domain.model.Engineer? = null,
    onOpenCelebrationWishes: () -> Unit = {}
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
        }.filter { (id, _) -> id !in dismissedNotificationIds || id in dismissingIds }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeumorphicTokens.Surface,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            val allActiveIds = activeNotifs.map { it.first }.toSet()
                            dismissingIds = dismissingIds + allActiveIds
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(320)
                                onClearAllNotifications()
                                dismissingIds = dismissingIds - allActiveIds
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

            HorizontalDivider(
                color = NeumorphicTokens.SurfaceContrast,
                thickness = 1.dp
            )

            if (activeNotifs.isEmpty() || activeNotifs.all { it.first in dismissingIds }) {
                NeumorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    style = NeumorphicElevationStyle.Raised,
                    surfaceColor = NeumorphicTokens.Surface,
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎉", fontSize = 38.sp)
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
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    activeNotifs.forEach { (notifId, notif) ->
                        val isCelebrationNotif = notif.celebrationType != null ||
                            notif.sender?.contains("Celebration", ignoreCase = true) == true ||
                            notif.title?.contains("🎂") == true || notif.title?.contains("🏆") == true

                        val pColor = when {
                            isCelebrationNotif -> Color(0xFFF59E0B)
                            notif.priority?.lowercase() == "urgent" -> Color(0xFFEF4444)
                            notif.priority?.lowercase() == "shift_update" -> Color(0xFFF59E0B)
                            notif.priority?.lowercase() == "broadcast" -> Color(0xFF8B5CF6)
                            else -> Color(0xFF3B82F6)
                        }

                        val resolvedMsg = (notif.rawMessage ?: notif.samplePersonalized ?: "")
                            .replace(Regex("\\{\\s*(?:Their\\s+name|their\\s+name|name)\\s*\\}", RegexOption.IGNORE_CASE), engName)
                            .replace(Regex("\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), engUsername)
                            .replace(Regex("@\\s*\\{\\s*username\\s*\\}", RegexOption.IGNORE_CASE), "@$engUsername")

                        AnimatedVisibility(
                            visible = notifId !in dismissingIds && notifId !in dismissedNotificationIds,
                            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 220)
                            ) + shrinkVertically(
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            )
                        ) {
                            NeumorphicCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isCelebrationNotif) {
                                            Modifier.clickable { onOpenCelebrationWishes() }
                                        } else Modifier
                                    ),
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
                                                    .background(pColor.copy(alpha = 0.16f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (isCelebrationNotif) "CELEBRATION 🎉" else (notif.priority ?: "INFO").uppercase(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = pColor
                                                )
                                            }
                                            Text(
                                                text = notif.title ?: "Broadcast Announcement",
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
                                                        kotlinx.coroutines.delay(320)
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
                                            text = resolvedMsg,
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
                                        if (!notif.createdAt.isNullOrBlank()) {
                                            Text(
                                                text = notif.createdAt,
                                                fontSize = 10.sp,
                                                color = NeumorphicTokens.TextMuted
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

