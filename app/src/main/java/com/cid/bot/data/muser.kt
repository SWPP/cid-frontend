package com.cid.bot.data

import androidx.room.*
import com.cid.bot.*
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

@Entity
data class Muser(
        @PrimaryKey var id: Int? = null,
        var username: String? = null,
        var gender: Int? = null,
        var birthdate: String? = null
)

class MuserRepository @Inject constructor(private val networkManager: NetworkManager) {
    @Inject lateinit var remoteSource: MuserRemoteSource
    @Inject lateinit var localSource: MuserLocalSource

    fun getMuser(): Observable<HResult<Muser>> {
        if (networkManager.isConnectedToInternet)
            return remoteSource.getMuser().andSave(localSource::saveMuser)
        return localSource.getMuser()
    }

    fun postMuser(muser: Muser): Observable<HResult<Muser>> {
        if (!networkManager.isConnectedToInternet) return Observable.just(networkManager.getNetworkError())
        return remoteSource.postMuser(muser).andSave(localSource::saveMuser)
    }

    fun clearMuser(): Completable {
        return localSource.clearMuser()
    }
}

class MuserRemoteSource @Inject constructor(private val net: NetworkManager) {
    fun getMuser(): Observable<HResult<Muser>> {
        return net.api.loadMyInfo().toHResult()
    }

    fun postMuser(muser: Muser): Observable<HResult<Muser>> {
        return net.api.saveMyInfo(muser).toHResult()
    }
}

class MuserLocalSource @Inject constructor(db: AppDatabase) {
    private val dao = db.muserDao()

    fun getMuser(): Observable<HResult<Muser>> {
        return singleObservable {
            HResult(dao.get())
        }
    }

    fun saveMuser(muser: Muser): Completable {
        return singleCompletable {
            dao.insert(muser)
        }
    }

    fun clearMuser(): Completable {
        return singleCompletable {
            dao.delete()
        }
    }
}

@Dao
interface MuserDao {
    @Query("SELECT * FROM Muser")
    fun get(): Muser

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(muser: Muser)

    @Query("DELETE FROM Muser")
    fun delete()
}
