package com.commissioning.momrecorder.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MomReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val meetingTitle: String = "",
    val date: String = "",
    val time: String = "",
    val duration: String = "",
    val location: String = "",
    val attendees: List<String> = emptyList(),
    val facilitator: String = "",
    val summary: String = "",
    val keyDecisions: List<KeyDecision> = emptyList(),
    val actionItems: List<ActionItem> = emptyList(),
    val discussionPoints: List<String> = emptyList(),
    val nextSteps: List<String> = emptyList(),
    val nextMeetingDate: String = "",
    val remarks: String = "",
    val rawTranscript: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class KeyDecision(
    val decision: String = "",
    val context: String = ""
) : Parcelable

@Parcelize
data class ActionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val action: String = "",
    val assignedTo: String = "",
    val dueDate: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: ActionStatus = ActionStatus.PENDING,
    val remarks: String = ""
) : Parcelable

enum class Priority { HIGH, MEDIUM, LOW }
enum class ActionStatus { PENDING, IN_PROGRESS, COMPLETED, DEFERRED }
