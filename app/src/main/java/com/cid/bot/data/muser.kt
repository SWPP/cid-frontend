package com.cid.bot.data

import com.cid.bot.*
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class Muser(
        val id: Int? = null,
        val username: String? = null,
        val gender: Int? = null,
        val birthdate: String? = null,
        val autoSignIn: Boolean? = false,
        val token: String? = null
)

class MuserRepository @Inject constructor(private val netManager: NetManager) {
    @Inject lateinit var localSource : MuserLocalSource
    private val remoteSource = MuserRemoteSource()
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
        return Observable.zip(remoteSource.postMuser(muser), localSource.saveMuserConfig(muser), BiFunction(mergeMuser))
    }

    fun clearMuser(): Observable<HResult<Muser>> {
        return localSource.clearMuser()
    }
}

class MuserLocalSource @Inject constructor(prefManager: PrefManager) {
    private val pref = prefManager.getPreference(R.string.pref_name_sign)
    private val autoSignInKey = prefManager.getKey(R.string.pref_key_auto_sign_in)
    private val tokenKey = prefManager.getKey(R.string.pref_key_token)

    fun getMuser(): Observable<HResult<Muser>> {
        val muser = Muser(1, "user", 1, "1111-11-11")
        return Observable.just(muser).delay(2, TimeUnit.SECONDS).map {
            HResult(it)
        }
    }

    fun getMuserConfig(): Observable<HResult<Muser>> {
        return Observable.create {
            it.onNext(HResult(Muser(
                    autoSignIn = pref.getBoolean(autoSignInKey, false),
                    token = pref.getString(tokenKey, null)
            )))
        }
    }

    fun saveMuserConfig(muser: Muser): Observable<HResult<Muser>> {
        return Observable.create {
            pref.edit().apply {
                muser.autoSignIn?.let {
                    putBoolean(autoSignInKey, muser.autoSignIn)
                    putString(tokenKey, if (muser.autoSignIn) muser.token else null)
                }
            }.apply()
            it.onNext(HResult(muser))
        }
        // TODO: save on database rest parts
    }

    fun clearMuser(): Observable<HResult<Muser>> {
        return Observable.create {
            pref.edit().apply {
                putBoolean(autoSignInKey, false)
                putString(tokenKey, null)
            }
            it.onNext(HResult(Muser()))
        }
        // TODO: delete muser from database
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
