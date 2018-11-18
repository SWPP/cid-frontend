package com.cid.bot

import androidx.appcompat.app.AppCompatActivity
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.Observable
import retrofit2.Response
import javax.inject.Inject

abstract class BaseDaggerActivity : DaggerAppCompatActivity() {
    @Inject lateinit var net: NetworkManager
    private val compositeDisposable = CompositeDisposable()

    fun <T> register(observable: Observable<Response<T>>,
                     onSuccess: (T) -> Unit,
                     onError: (Map<String, String>) -> Unit,
                     onFinish: () -> Unit = {}
    ) {
        compositeDisposable += net.call(observable, {
            onSuccess(it)
        }, {
            onError(it)
        }, {
            onFinish()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}

abstract class BaseActivity: AppCompatActivity() {
    @Inject lateinit var net: NetworkManager
    private val compositeDisposable = CompositeDisposable()

    fun <T> register(observable: Observable<Response<T>>,
                     onSuccess: (T) -> Unit,
                     onError: (Map<String, String>) -> Unit,
                     onFinish: () -> Unit = {}
    ) {
        compositeDisposable += net.call(observable, {
            onSuccess(it)
        }, {
            onError(it)
        }, {
            onFinish()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}
