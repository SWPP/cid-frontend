package com.cid.bot

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import com.cid.bot.data.Message
import com.cid.bot.data.MessageRepository
import com.cid.bot.data.Muser
import com.cid.bot.data.MuserRepository

class ProfileViewModel : ViewModel() {
    val repo = MuserRepository()
    val muser = ObservableField<Muser>()
    val isLoading = ObservableField<Boolean>()

    init {
        refresh()
    }

    fun refresh() {
        isLoading.set(true)
        repo.getMuser {
            isLoading.set(false)
            muser.set(it)
        }
    }
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val repo = MessageRepository(NetManager(application))
    val messages = MutableLiveData<List<Message>>()
    val isLoading = ObservableField<Boolean>()

    init {
        loadMessages()
    }

    fun loadMessages() {
        isLoading.set(true)
        repo.getMessages {
            isLoading.set(false)
            messages.value = it
        }
    }
}
