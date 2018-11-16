package com.cid.bot

import android.arch.lifecycle.MutableLiveData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

fun <T> MutableLiveData<T>.update(block: T.() -> Unit) {
    value?.apply(block)
    value = value
}
