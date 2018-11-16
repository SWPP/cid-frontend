package com.cid.bot.data

import com.cid.bot.API
import com.cid.bot.NetManager
import com.cid.bot.toHResult
import com.cid.bot.HResult
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class Message(val id: Int?, val sender: String?, val receiver: String?, val text: String, val created: String? = null)

class MessageRepository @Inject constructor(private val netManager: NetManager) {
    private val localSource = MessageLocalSource()
    private val remoteSource = MessageRemoteSource()

    fun getMessages(): Observable<HResult<List<Message>>> {
        if (netManager.isConnectedToInternet == true) {
            return remoteSource.getMessages()
        }
        return localSource.getMessages()
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        if (netManager.isConnectedToInternet == true) {
            return remoteSource.getMessage(id)
        }
        return localSource.getMessage(id)
    }

    fun postMessage(text: String): Observable<HResult<Message>> {
        if (netManager.isConnectedToInternet == true) {
            return remoteSource.postMessage(text)
        }
        return Observable.just(netManager.getNetworkError())
    }
}

class MessageLocalSource {
    fun getMessages(): Observable<HResult<List<Message>>> {
        val messages = listOf(
                Message(1, null, null, "lm1"),
                Message(2, null, null, "lm2"),
                Message(3, null, null, "lm3")
        )

        return Observable.just(messages).delay(2, TimeUnit.SECONDS).map {
            HResult(it)
        }
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        return Observable.just(Message(4, null, null, "lm4")).delay(2, TimeUnit.SECONDS).map {
            HResult(it)
        }
    }

    fun saveMessages(messages: List<Message>): Completable {
        return Single.just(1).delay(1, TimeUnit.SECONDS).toCompletable()
    }
}

class MessageRemoteSource {
    fun getMessages(): Observable<HResult<List<Message>>> {
        return API.loadAllMessages().toHResult()
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        return API.loadMessage(id).toHResult()
    }

    fun postMessage(text: String): Observable<HResult<Message>> {
        return API.sendMessage(text).toHResult()
    }
}
