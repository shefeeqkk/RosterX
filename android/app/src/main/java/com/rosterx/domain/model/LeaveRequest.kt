package com.rosterx.domain.model

import com.google.gson.annotations.SerializedName

data class LeaveRequest(
    val requestId: String = "",
    val engineerId: String = "",
    val engineerName: String = "",
    val dates: List<String> = emptyList(),
    val monthKeys: List<String> = emptyList(),
    val reason: String = "",
    val preferredPartner: String = "",
    val annualYear: Int = 2026,
    val daysCount: Int = 0,
    val status: String = "pending", // "pending" | "approved_swap" | "approved_leave" | "rejected"
    val createdAt: String = "",
    val approval: LeaveApprovalInfo? = null
)

data class LeaveApprovalInfo(
    val approvedBy: String = "",
    val approvedByName: String = "",
    val approvedAt: String = "",
    val actionType: String = "", // "swap" | "leave" | "reject"
    val swapPartner: String = "",
    val swapShiftCode: String = "",
    val adminNote: String = ""
)

data class EligibleSwapPartner(
    val username: String = "",
    val name: String = "",
    @SerializedName("profile_picture") val profilePicture: String? = null,
    val shift: String = "",
    @SerializedName("is_off") val isOff: Boolean = false
)

data class LeaveStats(
    val approvedLeaveDays: Int = 0,
    val remainingLeaveBalance: Int = 14,
    val annualQuota: Int = 14,
    val activeCount: Int = 0
)
