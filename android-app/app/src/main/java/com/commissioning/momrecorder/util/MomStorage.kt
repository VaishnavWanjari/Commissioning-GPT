package com.commissioning.momrecorder.util

import android.content.Context
import com.commissioning.momrecorder.model.MomReport
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object MomStorage {

    private const val FILE_NAME = "saved_moms.json"
    private val gson = Gson()

    fun saveMom(context: Context, report: MomReport) {
        val list = loadAll(context).toMutableList()
        val existing = list.indexOfFirst { it.id == report.id }
        if (existing >= 0) list[existing] = report else list.add(0, report)
        writeAll(context, list)
    }

    fun loadAll(context: Context): List<MomReport> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<MomReport>>() {}.type
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun delete(context: Context, id: String) {
        val list = loadAll(context).filter { it.id != id }
        writeAll(context, list)
    }

    fun updateActionStatus(
        context: Context,
        meetingId: String,
        actionId: String,
        newStatus: com.commissioning.momrecorder.model.ActionStatus
    ) {
        val list = loadAll(context).toMutableList()
        val meetingIndex = list.indexOfFirst { it.id == meetingId }
        if (meetingIndex < 0) return
        val meeting = list[meetingIndex]
        val updatedActions = meeting.actionItems.map { item ->
            if (item.id == actionId) item.copy(status = newStatus) else item
        }
        list[meetingIndex] = meeting.copy(actionItems = updatedActions)
        writeAll(context, list)
    }

    private fun writeAll(context: Context, list: List<MomReport>) {
        File(context.filesDir, FILE_NAME).writeText(gson.toJson(list))
    }
}
