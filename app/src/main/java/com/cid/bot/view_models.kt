package com.cid.bot

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField

class ProfileViewModel : ViewModel() {
    val repo = Repository()
    val text = ObservableField<String>()
    val isLoading = ObservableField<Boolean>()

    init {
        refresh()
    }

    fun refresh() {
        isLoading.set(true)
        repo.refreshData(object : OnDataReadyCallback {
            override fun onDataReady(data: String) {
                isLoading.set(false)
                text.set(data)
            }
        })
    }
}

class ChatViewModel : ViewModel() {
    val repo = Repository()
    val messages = MutableLiveData<List<Message>>()
    val isLoading = ObservableField<Boolean>()

    init {
        loadMessages()
    }

    fun loadMessages() {
        isLoading.set(true)
        repo.getMessages(object : OnMessageReadyCallback {
            override fun onMessageReady(data: List<Message>) {
                isLoading.set(false)
                messages.value = data
            }
        })
    }
}
