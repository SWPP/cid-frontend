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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers

class ProfileViewModel : ViewModel() {
    private val repo = MuserRepository()
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
    private val repo = MessageRepository(NetManager(application))
    val messages = MutableLiveData<List<Message>>()
    val isLoading = ObservableField<Boolean>()

    private val compositeDisposable = CompositeDisposable()

    init {
        loadMessages()
    }

    fun loadMessages() {
        isLoading.set(true)
        compositeDisposable += repo
                .getMessages()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Message>>() {
            override fun onNext(t: List<Message>) {
                messages.value = t
            }

            override fun onError(e: Throwable) {
                // TODO
            }

            override fun onComplete() {
                isLoading.set(false)
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}
