package com.cid.bot

import android.support.v7.app.AppCompatActivity
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseDaggerActivity: DaggerAppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    fun register(disposable: Disposable) {
        compositeDisposable += disposable
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}

abstract class BaseActivity: AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    fun register(disposable: Disposable) {
        compositeDisposable += disposable
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}
