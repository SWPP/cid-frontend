package com.cid.bot.data

import android.arch.persistence.room.*
import android.util.Log
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

class MuserRepository @Inject constructor(private val netManager: NetManager) {
    private val remoteSource = MuserRemoteSource()
    @Inject lateinit var localSource: MuserLocalSource

    fun getMuser(): Observable<HResult<Muser>> {
        if (netManager.isConnectedToInternet)
            return remoteSource.getMuser()
        return localSource.getMuser()
    }

    fun postMuser(muser: Muser): Observable<HResult<Muser>> {
        Log.e("postMuser", "network ${netManager.isConnectedToInternet}")
        if (!netManager.isConnectedToInternet) return Observable.just(netManager.getNetworkError())
        return remoteSource.postMuser(muser)
                .map {
                    it.data!!.let {
                        localSource.saveMuser(it)
                        HResult(it)
                    }
                }
    }

    fun clearMuser(): Completable {
        return localSource.clearMuser()
    }
}

class MuserRemoteSource {
    fun getMuser(): Observable<HResult<Muser>> {
        return API.loadMyInfo().toHResult()
    }

    fun postMuser(muser: Muser): Observable<HResult<Muser>> {
        return API.saveMyInfo(muser).toHResult()
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
