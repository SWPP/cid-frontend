package com.cid.bot

import android.os.Handler

class Repository {
    fun refreshData(onDataReadyCallback: OnDataReadyCallback) {
        Handler().postDelayed({ onDataReadyCallback.onDataReady("new data") }, 2000)
    }

    fun getMessages(onMessageReadyCallback: OnMessageReadyCallback) {
        val list = listOf(
                Message(1, null, null, "m1"),
                Message(2, null, null, "m2"),
                Message(3, null, null, "m3")
        )

        Handler().postDelayed({ onMessageReadyCallback.onMessageReady(list) }, 2000)
    }
}

interface OnDataReadyCallback {
    fun onDataReady(data: String)
}

interface OnMessageReadyCallback {
    fun onMessageReady(data: List<Message>)
}
