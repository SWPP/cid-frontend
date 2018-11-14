package com.cid.bot.data

import com.cid.bot.NetManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class Muser(val id: Int?, val username: String, val gender: Int, val birthdate: String?)

class MuserRepository @Inject constructor(private val netManager: NetManager) {
    private val localSource = MuserLocalSource()
    private val remoteSource = MuserRemoteSource()

    fun getMuser(): Observable<Muser> {
        if (netManager.isConnectedToInternet == true) {
            return remoteSource.getMuser().flatMap {
                return@flatMap localSource.saveMuser(it)
                        .toSingleDefault(it)
                        .toObservable()
            }
        }
        return localSource.getMuser()
    }
}

class MuserLocalSource {
    fun getMuser(): Observable<Muser> {
        val muser = Muser(1, "user", 1, "1111-11-11")
        return Observable.just(muser).delay(2, TimeUnit.SECONDS)
    }

    fun saveMuser(muser: Muser): Completable {
        return Single.just(1).delay(1, TimeUnit.SECONDS).toCompletable()
    }
}

class MuserRemoteSource {
    fun getMuser(): Observable<Muser> {
        val muser = Muser(1, "user", 1, "2222-22-22")
        return Observable.just(muser).delay(2, TimeUnit.SECONDS)
    }
}
