package com.cid.bot.data

import android.os.Handler

data class Message(val id: Int?, val sender: String?, val receiver: String?, val text: String, val created: String? = null)

class MessageRepository {
    fun getMessages(onMessageReady: (List<Message>) -> Unit) {
        val messages = listOf(
                Message(1, null, null, "m1"),
                Message(2, null, null, "m2"),
                Message(3, null, null, "m3")
        )

        Handler().postDelayed({ onMessageReady(messages) }, 2000)
    }
}
