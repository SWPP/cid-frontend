package com.cid.bot.data

import android.arch.persistence.room.*
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

class MessageRepository @Inject constructor(private val netManager: NetManager) {
    private val remoteSource = MessageRemoteSource()
    @Inject lateinit var localSource: MessageLocalSource

    fun getMessages(): Observable<HResult<List<Message>>> {
        if (netManager.isConnectedToInternet) {
            return remoteSource.getMessages().map {
                it.data!!.let { localSource.saveMessages(it) }
                it
            }
        }
        return localSource.getMessages()
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        if (netManager.isConnectedToInternet) {
            return remoteSource.getMessage(id).map {
                it.data!!.let { localSource.saveMessage(it) }
                it
            }
        }
        return localSource.getMessage(id)
    }

    fun postMessage(text: String): Observable<HResult<Message>> {
        if (!netManager.isConnectedToInternet) return Observable.just(netManager.getNetworkError())
        return remoteSource.postMessage(text).map {
            it.data!!.let { localSource.saveMessage(it) }
            it
        }
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

class MessageLocalSource @Inject constructor(db: AppDatabase) {
    private val dao = db.messageDao()

    fun getMessages(): Observable<HResult<List<Message>>> {
        return singleObservable {
            HResult(dao.getAll())
        }
    }

    fun getMessage(id: Int): Observable<HResult<Message>> {
        return singleObservable { HResult(dao.getById(id)) }
    }

    fun saveMessage(message: Message): Completable {
        return singleCompletable {
            dao.insert(message)
        }
    }

    fun saveMessages(messages: List<Message>): Completable {
//        return dao.deleteAll().andThen {
//            dao.insertAll(messages)
//        }
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(message: Message)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(message: List<Message>)

    @Query("DELETE FROM Message")
    fun deleteAll()
}
