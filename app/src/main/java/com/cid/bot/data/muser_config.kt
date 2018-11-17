package com.cid.bot.data

import com.cid.bot.*
import io.reactivex.Completable
import io.reactivex.Observable
import javax.inject.Inject

data class MuserConfig(
        val autoSignIn: Boolean? = false,
        val token: String? = null,
        val saveUsername: Boolean? = false,
        val username: String? = null
)

class MuserConfigRepository @Inject constructor() {
    @Inject lateinit var localSource: LocalSource

    fun getMuserConfig(): Observable<HResult<MuserConfig>> {
        return localSource.getMuserConfig()
    }

    fun saveMuserConfig(muserConfig: MuserConfig): Completable {
        return localSource.saveMuserConfig(muserConfig)
    }

    fun clearMuserConfig(): Completable {
        return localSource.clearMuserConfig()
    }
}

class LocalSource @Inject constructor(prefManager: PrefManager) {
    private val pref = prefManager.getPreference(R.string.pref_name_sign)
    private val autoSignInKey = prefManager.getKey(R.string.pref_key_auto_sign_in)
    private val tokenKey = prefManager.getKey(R.string.pref_key_token)
    private val saveUsernameKey = prefManager.getKey(R.string.pref_key_save_username)
    private val usernameKey = prefManager.getKey(R.string.pref_key_username)

    fun getMuserConfig(): Observable<HResult<MuserConfig>> {
        return singleObservable {
            HResult(MuserConfig(
                    autoSignIn = pref.getBoolean(autoSignInKey, false),
                    token = pref.getString(tokenKey, null),
                    saveUsername = pref.getBoolean(saveUsernameKey, false),
                    username = pref.getString(usernameKey, null)
            ).also { NetworkManager.authToken = it.token })
        }
    }

    fun saveMuserConfig(muserConfig: MuserConfig): Completable {
        return singleCompletable {
            pref.edit().apply {
                muserConfig.autoSignIn?.let {
                    putBoolean(autoSignInKey, it)
                    putString(tokenKey, if (it) muserConfig.token else null)
                }
                muserConfig.saveUsername?.let {
                    putBoolean(saveUsernameKey, it)
                    putString(usernameKey, if (it) muserConfig.username else null)
                }
            }.apply()
        }
    }

    fun clearMuserConfig(): Completable {
        return singleCompletable {
            pref.edit().apply {
                putBoolean(autoSignInKey, false)
                putString(tokenKey, null)
                putBoolean(saveUsernameKey, false)
                putString(usernameKey, null)
            }
        }
    }
}
