package com.cid.bot.data

import com.cid.bot.NetManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class Message(val id: Int?, val sender: String?, val receiver: String?, val text: String, val created: String? = null)

class MessageRepository @Inject constructor(private val netManager: NetManager) {
    private val localSource = MessageLocalSource()
    private val remoteSource = MessageRemoteSource()

    fun getMessages(): Observable<List<Message>> {
        if (netManager.isConnectedToInternet == true) {
            return remoteSource.getMessages().flatMap {
                return@flatMap localSource.saveMessages(it)
                        .toSingleDefault(it)
                        .toObservable()
            }
        }
        return localSource.getMessages()
    }
}

class MessageLocalSource {
    fun getMessages(): Observable<List<Message>> {
        val messages = listOf(
                Message(1, null, null, "lm1"),
                Message(2, null, null, "lm2"),
                Message(3, null, null, "lm3")
        )

        return Observable.just(messages).delay(2, TimeUnit.SECONDS)
    }

    fun saveMessages(messages: List<Message>): Completable {
        return Single.just(1).delay(1, TimeUnit.SECONDS).toCompletable()
    }
}

class MessageRemoteSource {
    fun getMessages(): Observable<List<Message>> {
        val messages = listOf(
                Message(1, null, null, "rm1"),
                Message(2, null, null, "rm2"),
                Message(3, null, null, "rm3")
        )

        return Observable.just(messages).delay(2, TimeUnit.SECONDS)
    }
}
