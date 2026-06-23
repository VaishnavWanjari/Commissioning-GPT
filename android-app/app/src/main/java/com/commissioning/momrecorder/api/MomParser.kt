package com.commissioning.momrecorder.api

import com.commissioning.momrecorder.model.*
import com.google.gson.JsonParser
import com.google.gson.JsonObject

object MomParser {

    fun parse(jsonString: String, rawTranscript: String): MomReport {
        // Strip markdown code fences if present
        val cleaned = jsonString
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JsonParser.parseString(cleaned).asJsonObject
        return MomReport(
            meetingTitle = json.str("meetingTitle"),
            date = json.str("date"),
            time = json.str("time"),
            duration = json.str("duration"),
            location = json.str("location"),
            attendees = json.strList("attendees"),
            facilitator = json.str("facilitator"),
            summary = json.str("summary"),
            keyDecisions = parseDecisions(json),
            actionItems = parseActions(json),
            discussionPoints = json.strList("discussionPoints"),
            nextSteps = json.strList("nextSteps"),
            nextMeetingDate = json.str("nextMeetingDate"),
            remarks = json.str("remarks"),
            rawTranscript = rawTranscript
        )
    }

    private fun parseDecisions(json: JsonObject): List<KeyDecision> {
        return try {
            json.getAsJsonArray("keyDecisions")?.map { el ->
                val obj = el.asJsonObject
                KeyDecision(
                    decision = obj.str("decision"),
                    context = obj.str("context")
                )
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun parseActions(json: JsonObject): List<ActionItem> {
        return try {
            json.getAsJsonArray("actionItems")?.map { el ->
                val obj = el.asJsonObject
                ActionItem(
                    action = obj.str("action"),
                    assignedTo = obj.str("assignedTo").ifBlank { "Team" },
                    dueDate = obj.str("dueDate"),
                    priority = parsePriority(obj.str("priority")),
                    status = ActionStatus.PENDING,
                    remarks = obj.str("remarks")
                )
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun parsePriority(value: String): Priority = when (value.uppercase()) {
        "HIGH" -> Priority.HIGH
        "LOW" -> Priority.LOW
        else -> Priority.MEDIUM
    }

    private fun JsonObject.str(key: String): String =
        try { get(key)?.takeIf { !it.isJsonNull }?.asString ?: "" } catch (e: Exception) { "" }

    private fun JsonObject.strList(key: String): List<String> =
        try {
            getAsJsonArray(key)?.mapNotNull {
                it?.takeIf { !it.isJsonNull }?.asString?.takeIf { s -> s.isNotBlank() }
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
}
