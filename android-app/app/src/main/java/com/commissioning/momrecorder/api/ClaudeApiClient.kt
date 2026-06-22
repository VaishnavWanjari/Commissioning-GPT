package com.commissioning.momrecorder.api

import android.util.Log
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ClaudeApiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ClaudeApiClient"
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val MAX_TOKENS = 4096
    }

    suspend fun generateMom(transcript: String, meetingContext: MeetingContext): String {
        val prompt = buildMomPrompt(transcript, meetingContext)
        return callClaude(prompt)
    }

    private fun buildMomPrompt(transcript: String, ctx: MeetingContext): String {
        val today = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        val contextInfo = buildString {
            if (ctx.title.isNotBlank()) append("Meeting Title: ${ctx.title}\n")
            if (ctx.platform.isNotBlank()) append("Platform: ${ctx.platform}\n")
            if (ctx.date.isNotBlank()) append("Date: ${ctx.date}\n") else append("Date: $today\n")
        }

        return """You are an expert professional meeting minutes (MOM) writer. Analyze the following meeting transcript and generate a comprehensive, professional MOM report.

MEETING CONTEXT:
$contextInfo

TRANSCRIPT:
$transcript

Generate a detailed MOM as a JSON object with this exact structure. Be thorough, professional and extract every important detail:

{
  "meetingTitle": "infer a professional meeting title from context",
  "date": "meeting date (use today $today if not mentioned)",
  "time": "meeting time if mentioned, otherwise empty string",
  "duration": "estimated duration based on content",
  "location": "WhatsApp/Instagram Video Call or as mentioned",
  "attendees": ["list of all speakers/participants identified from transcript"],
  "facilitator": "meeting host/facilitator name if identifiable",
  "summary": "Professional 2-3 paragraph executive summary covering what was discussed and key outcomes",
  "keyDecisions": [
    {
      "decision": "specific decision made",
      "context": "why this decision was made"
    }
  ],
  "actionItems": [
    {
      "action": "specific actionable task",
      "assignedTo": "person responsible (use 'Team' if unclear)",
      "dueDate": "specific date or timeline (e.g. 'by Friday', '2 weeks', 'EOD')",
      "priority": "HIGH or MEDIUM or LOW based on urgency discussed",
      "status": "PENDING",
      "remarks": "any additional notes about this action"
    }
  ],
  "discussionPoints": [
    "key topic discussed 1",
    "key topic discussed 2"
  ],
  "nextSteps": [
    "immediate next step 1",
    "immediate next step 2"
  ],
  "nextMeetingDate": "date/time of next meeting if mentioned, otherwise empty string",
  "remarks": "any other important notes, concerns, or observations"
}

Rules:
- Extract ALL action items mentioned - these are critical
- Identify who is responsible for each action (name or role)
- Extract any dates, deadlines, or timelines mentioned
- Prioritize HIGH for urgent/critical items, MEDIUM for regular items, LOW for optional
- Be professional and formal in language
- If a piece of information is not available, use empty string or empty array
- Return ONLY valid JSON, no markdown, no extra text"""
    }

    private fun callClaude(prompt: String): String {
        val requestJson = """
            {
                "model": "$MODEL",
                "max_tokens": $MAX_TOKENS,
                "messages": [
                    {
                        "role": "user",
                        "content": ${escapeJsonString(prompt)}
                    }
                ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(BASE_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from API")

        if (!response.isSuccessful) {
            Log.e(TAG, "API error: ${response.code} - $responseBody")
            throw Exception("API error ${response.code}: ${extractErrorMessage(responseBody)}")
        }

        return extractContent(responseBody)
    }

    private fun extractContent(responseBody: String): String {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.getAsJsonArray("content")
                .get(0).asJsonObject
                .get("text").asString
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response: $responseBody", e)
            throw Exception("Failed to parse API response: ${e.message}")
        }
    }

    private fun extractErrorMessage(responseBody: String): String {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            json.getAsJsonObject("error")?.get("message")?.asString ?: responseBody
        } catch (e: Exception) {
            responseBody
        }
    }

    private fun escapeJsonString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}

data class MeetingContext(
    val title: String = "",
    val platform: String = "Video Call",
    val date: String = ""
)
