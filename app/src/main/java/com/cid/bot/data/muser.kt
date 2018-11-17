package com.cid.bot.data

import android.arch.persistence.room.*
import com.cid.bot.*
import com.cid.bot.R
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import javax.inject.Inject

@Entity
data class Muser(
        @PrimaryKey var id: Int? = null,
        var username: String? = null,
        var gender: Int? = null,
        var birthdate: String? = null,
        @Ignore var autoSignIn: Boolean? = false,
        @Ignore var token: String? = null
)

class MuserRepository @Inject constructor(private val netManager: NetManager) {
    private val remoteSource = MuserRemoteSource()
    @Inject lateinit var localSource : MuserLocalSource
    private val mergeMuser = { mr: HResult<Muser>, ml: HResult<Muser> ->
        when {
            mr.data == null -> mr
            ml.data == null -> ml
            else -> HResult(mr.data.copy(autoSignIn = ml.data.autoSignIn, token = ml.data.token))
        }
    }

    fun getMuser(): Observable<HResult<Muser>> {
        return Observable.zip(if (netManager.isConnectedToInternet == true) {
            remoteSource.getMuser()
        } else {
            localSource.getMuser()
        }, localSource.getMuserConfig(), BiFunction(mergeMuser))
    }

    fun postMuser(muser: Muser): Observable<HResult<Muser>> {
        if (netManager.isConnectedToInternet != true) return Observable.just(netManager.getNetworkError())
        // TODO: try remote post muser and try local save muser sequentially.
        return Observable.zip(remoteSource.postMuser(muser), localSource.saveMuser(muser), BiFunction(mergeMuser))
    }

    fun clearMuser(): Observable<HResult<Muser>> {
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

class MuserLocalSource @Inject constructor(prefManager: PrefManager, db: AppDatabase) {
    private val dao = db.muserDao()
    private val pref = prefManager.getPreference(R.string.pref_name_sign)
    private val autoSignInKey = prefManager.getKey(R.string.pref_key_auto_sign_in)
    private val tokenKey = prefManager.getKey(R.string.pref_key_token)

    fun getMuser(): Observable<HResult<Muser>> {
        return createSingle {
            it.onNext(HResult(dao.get().let {
                list -> if (list.isEmpty()) Muser() else list[0]
            }))
        }
    }

    fun getMuserConfig(): Observable<HResult<Muser>> {
        return createSingle {
            it.onNext(HResult(Muser(
                    autoSignIn = pref.getBoolean(autoSignInKey, false),
                    token = pref.getString(tokenKey, null)
            )))
        }
    }

    fun saveMuser(muser: Muser): Observable<HResult<Muser>> {
        return createSingle {
            dao.insert(muser)
            pref.edit().apply {
                muser.autoSignIn?.let {
                    putBoolean(autoSignInKey, it)
                    putString(tokenKey, if (it) muser.token else null)
                }
            }.apply()
            it.onNext(HResult(muser))
        }
    }

    fun clearMuser(): Observable<HResult<Muser>> {
        return createSingle {
            dao.delete()
            pref.edit().apply {
                putBoolean(autoSignInKey, false)
                putString(tokenKey, null)
            }
            it.onNext(HResult(Muser()))
        }
    }
}

@Dao
interface MuserDao {
    @Query("SELECT * FROM Muser LIMIT 1")
    fun get(): List<Muser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(muser: Muser)

    @Query("DELETE FROM Muser")
    fun delete()
}
