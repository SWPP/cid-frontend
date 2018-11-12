package com.cid.bot.data

import android.os.Handler
import com.cid.bot.NetManager

data class Message(val id: Int?, val sender: String?, val receiver: String?, val text: String, val created: String? = null)

class MessageRepository(private val netManager: NetManager) {
    private val localSource = MessageLocalSource()
    private val remoteSource = MessageRemoteSource()

    fun getMessages(onMessageReady: (List<Message>) -> Unit) {
        if (netManager.isConnectedToInternet == true) {
            remoteSource.getMessages {
                localSource.saveMessages(it)
                onMessageReady(it)
            }
        } else {
            localSource.getMessages(onMessageReady)
        }
    }
}

class MessageLocalSource {
    fun getMessages(onMessageReady: (List<Message>) -> Unit) {
        val messages = listOf(
                Message(1, null, null, "lm1"),
                Message(2, null, null, "lm2"),
                Message(3, null, null, "lm3")
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
                Message(1, null, null, "rm1"),
                Message(2, null, null, "rm2"),
                Message(3, null, null, "rm3")
        )

        Handler().postDelayed({ onMessageReady(messages) }, 2000)
    }
}
