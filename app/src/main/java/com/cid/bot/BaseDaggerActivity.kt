package com.cid.bot

import android.widget.Toast
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

    fun toastShort(content: Any?) {
        Toast.makeText(this, content.toString(), Toast.LENGTH_SHORT).show()
    }

    fun toastLong(content: Any?) {
        Toast.makeText(this, content.toString(), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}
