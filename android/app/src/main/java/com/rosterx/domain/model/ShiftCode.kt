package com.rosterx.domain.model

sealed class ShiftCode(
    val code: String,
    val name: String,
    val bgColorHex: String,
    val textColorHex: String,
    val minRestHours: Double
) {
    data object FS : ShiftCode("FS", "First Shift", "#BBF7D0", "#064E3B", 15.0)
    data object FG : ShiftCode("FG", "First General", "#166534", "#FFFFFF", 14.0)
    data object G : ShiftCode("G", "General Shift", "#BAE6FD", "#0C4A6E", 13.0)
    data object SG : ShiftCode("SG", "Second General", "#EA580C", "#FFFFFF", 11.5)
    data object SS : ShiftCode("SS", "Second Shift", "#FED7AA", "#7C2D12", 9.5)
    data object NS : ShiftCode("NS", "Night Shift", "#1E293B", "#FFFFFF", 24.0)
    data object OFF : ShiftCode("OFF", "Weekly Off", "#FEF08A", "#713F12", 24.0)
    data object L : ShiftCode("L", "Approved Leave", "#FECACA", "#991B1B", 24.0)
    
    data class SW(val peerName: String) : ShiftCode(
        code = if (peerName.startsWith("SW", ignoreCase = true)) peerName.uppercase() else "SW${peerName.uppercase()}",
        name = "Swap Shift",
        bgColorHex = "#E2E8F0",
        textColorHex = "#334155",
        minRestHours = 24.0
    )

    companion object {
        fun fromString(value: String?): ShiftCode {
            if (value.isNullOrBlank()) return OFF
            val clean = value.trim().uppercase()
            return when {
                clean == "FS" -> FS
                clean == "FG" -> FG
                clean == "G" -> G
                clean == "SG" -> SG
                clean == "SS" -> SS
                clean == "NS" -> NS
                clean == "OFF" -> OFF
                clean == "L" -> L
                clean.startsWith("SW") -> SW(clean.removePrefix("SW"))
                else -> OFF
            }
        }
    }
}
