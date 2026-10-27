package com.rosterx.domain.service

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.rosterx.domain.model.Engineer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RosterConstraints(
    val rules: List<String>
)

data class AiProposal(
    val explanation: String,
    val modifiedShifts: Map<String, Map<String, String>> // engineerId -> (dayString -> shiftCode)
)

class GeminiRosterService(private val apiKey: String, private val modelName: String = "gemini-1.5-flash") {

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    suspend fun generateMonthlyRoster(
        monthYear: String,
        engineers: List<Engineer>,
        constraints: RosterConstraints,
        customDirectives: String
    ): Result<Map<String, Map<String, String>>> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are an expert NOC roster scheduler. Generate a monthly shift roster for $monthYear.
                Strictly use these shift codes: FS, FG, G, SG, SS, NS, OFF, L, SW[PeerName].
                
                Engineers: ${engineers.joinToString { it.username + " (Off: " + it.defaultWeeklyOffs + ")" }}
                Constraints: ${constraints.rules.joinToString()}
                Custom Directives: $customDirectives
                
                Return ONLY a valid JSON object matching this schema:
                {
                   "engineerUsername1": {
                       "1": "FS",
                       "2": "OFF"
                   }
                }
            """.trimIndent()

            val response = generativeModel.generateContent(
                content { text(prompt) }
            )
            
            // In a real app, parse the JSON response here using Gson or Kotlinx Serialization
            // For now, returning an empty map as a placeholder for the parsed result
            Result.success(emptyMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseConversationalCommand(
        userMessage: String,
        currentRosterContext: String
    ): Result<AiProposal> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a smart NOC AI assistant. The user says: "$userMessage"
                Current Context: $currentRosterContext
                
                Propose the changes in valid JSON adhering to:
                {
                   "explanation": "I have cleared Shefeeq's shifts for next week...",
                   "modifiedShifts": {
                       "shefeeqkk": {
                           "15": "OFF",
                           "16": "OFF"
                       }
                   }
                }
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            // Parse response...
            Result.success(AiProposal("Proposal generated", emptyMap()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
