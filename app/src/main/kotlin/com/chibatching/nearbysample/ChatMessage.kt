package com.chibatching.nearbysample

import org.json.JSONObject
import java.util.*


public data class ChatMessage(
        val text: String,
        val timestamp: Long,
        val id: String = UUID.randomUUID().toString(),
        val type: String = ChatMessage.TYPE_USER_CHAT) : Comparable<ChatMessage> {

    companion object {
        val TYPE_USER_CHAT = "user-chat"
        val TYPE_BEACON = "beacon-message"

        fun fromJson(jsonString: String) : ChatMessage {
            val json = JSONObject(jsonString)
            return if (json.getString("type").equals(TYPE_USER_CHAT)) {
                ChatMessage(json.getString("text"), json.getLong("timestamp"), json.getString("id"), TYPE_USER_CHAT)
            } else {
                ChatMessage(json.getString("text"), System.currentTimeMillis(), json.getString("id"), TYPE_BEACON)
            }
        }
    }

    override fun toString(): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("text", text)
        json.put("timestamp", timestamp)
        json.put("type", type)
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