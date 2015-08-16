package com.chibatching.nearbysample

import org.json.JSONObject
import java.util.*


public data class ChatMessage(
        val text: String,
        val timestamp: Long,
        val self: Boolean,
        val id: String = UUID.randomUUID().toString()) : Comparable<ChatMessage> {

    companion object {
        fun fromJson(jsonString: String) : ChatMessage {
            val json = JSONObject(jsonString)
            return ChatMessage(json.getString("text"), json.getLong("timestamp"), json.getBoolean("self"), json.getString("id"))
        }
    }

    override fun toString(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("text", text)
        json.put("timestamp", timestamp)
        json.put("self", self)
        return json.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ChatMessage) {
            return false
        }
        return id.equals(other.id)
    }

    override fun compareTo(other: ChatMessage): Int {
        return if (this.timestamp < other.timestamp) {
            -1
        } else if (this.timestamp > other.timestamp) {
            1
        } else {
            0
        }
    }
}