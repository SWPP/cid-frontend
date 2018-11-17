package com.cid.bot

import android.support.v7.app.AppCompatActivity
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.Observable
import retrofit2.Response

abstract class BaseDaggerActivity: DaggerAppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    fun <T> register(observable: Observable<Response<T>>,
                     onSuccess: (T) -> Unit,
                     onError: (Map<String, String>) -> Unit,
                     onFinish: () -> Unit = {}
    ) {
        compositeDisposable += NetworkManager.call(observable, {
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
    private val compositeDisposable = CompositeDisposable()

    fun <T> register(observable: Observable<Response<T>>,
                     onSuccess: (T) -> Unit,
                     onError: (Map<String, String>) -> Unit,
                     onFinish: () -> Unit = {}
    ) {
        compositeDisposable += NetworkManager.call(observable, {
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
