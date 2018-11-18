package com.cid.bot.data

import androidx.room.*
import com.cid.bot.*
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

@Entity
data class Message(
        @PrimaryKey val id: Int?,
        val sender: String?,
        val receiver: String?,
        val text: String,
        val created: String? = null
)

class MessageRepository @Inject constructor(private val networkManager: NetworkManager) {
    @Inject lateinit var remoteSource: MessageRemoteSource
    @Inject lateinit var localSource: MessageLocalSource

    fun getMessages(): Observable<HResult<List<Message>>> {
        if (networkManager.isConnectedToInternet) {
            return remoteSource.getMessages().andSave(localSource::saveMessages)
        }
        return localSource.getMessages()
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        if (networkManager.isConnectedToInternet) {
            return remoteSource.getMessage(id).andSave(localSource::saveMessage)
        }
        return localSource.getMessage(id)
    }

    fun postMessage(text: String): Observable<HResult<Message>> {
        if (!networkManager.isConnectedToInternet) return Observable.just(networkManager.getNetworkError())
        return remoteSource.postMessage(text).andSave(localSource::saveMessage)
    }
}

class MessageRemoteSource @Inject constructor(private val net: NetworkManager) {
    fun getMessages(): Observable<HResult<List<Message>>> {
        return net.api.loadAllMessages().toHResult()
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        return net.api.loadMessage(id).toHResult()
    }

    fun postMessage(text: String): Observable<HResult<Message>> {
        return net.api.sendMessage(text).toHResult()
    }
}

class MessageLocalSource @Inject constructor(db: AppDatabase) {
    private val dao = db.messageDao()

    fun getMessages(): Observable<HResult<List<Message>>> {
        return singleObservable { HResult(dao.getAll()) }
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        return singleObservable { HResult(dao.getById(id)) }
    }

    fun saveMessage(message: Message): Completable {
        return singleCompletable { dao.insert(message) }
    }

    fun saveMessages(messages: List<Message>): Completable {
        return singleCompletable {
            dao.deleteAll()
            dao.insertAll(messages)
        }
    }
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM Message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM Message WHERE id = :id")
    fun getById(id: Int): Message

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(messages: List<Message>)

    @Query("DELETE FROM Message")
    fun deleteAll()
}
