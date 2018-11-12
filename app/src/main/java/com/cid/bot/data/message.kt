package com.cid.bot.data

import android.os.Handler

data class Message(val id: Int?, val sender: String?, val receiver: String?, val text: String, val created: String? = null)

class MessageRepository {
    private val localSource = MessageLocalSource()
    private val remoteSource = MessageRemoteSource()

    fun getMessages(onMessageReady: (List<Message>) -> Unit) {
        remoteSource.getMessages {
            localSource.saveMessages(it)
            onMessageReady(it)
        }
    }
}

class MessageLocalSource {
    fun getMessages(onMessageReady: (List<Message>) -> Unit) {
        val messages = listOf(
                Message(1, null, null, "m1"),
                Message(2, null, null, "m2"),
                Message(3, null, null, "m3")
        )

        Handler().postDelayed({ onMessageReady(messages) }, 2000)
    }

    fun saveMessages(messages: List<Message>) {
        // TODO: save messages in DB
    }
}

class MessageRemoteSource {
    fun getMessages(onMessageReady: (List<Message>) -> Unit) {
        val messages = listOf(
                Message(1, null, null, "m1"),
                Message(2, null, null, "m2"),
                Message(3, null, null, "m3")
        )

        Handler().postDelayed({ onMessageReady(messages) }, 2000)
    }
}
