package com.rosterx.domain.model

import com.google.gson.annotations.SerializedName

data class OperationalConstraints(
    @SerializedName("nightHeadcount")
    val nightHeadcount: Int = 1,

    @SerializedName("fsMin")
    val fsMin: Int = 1,

    @SerializedName("ssMin")
    val ssMin: Int = 1,

    @SerializedName("daytimeOverlapMin")
    val daytimeOverlapMin: Int = 3,

    @SerializedName("postNightRestDays")
    val postNightRestDays: Int = 2,

    @SerializedName("maxConsecutiveDays")
    val maxConsecutiveDays: Int = 6,

    @SerializedName("nightBlockMin")
    val nightBlockMin: Int = 3,

    @SerializedName("nightBlockMax")
    val nightBlockMax: Int = 5,

    @SerializedName("swapFirstPolicy")
    val swapFirstPolicy: Boolean = true,

    @SerializedName("customDirectives")
    val customDirectives: String = "",

    @SerializedName("aiModel")
    val aiModel: String = "Gemini 1.5 Pro"
)

data class ConstraintsResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("constraints")
    val constraints: OperationalConstraints? = null,

    @SerializedName("message")
    val message: String? = null
)
