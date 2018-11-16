package com.cid.bot

import android.arch.lifecycle.MutableLiveData
import android.text.TextUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

fun <T> MutableLiveData<T>.update(block: T.() -> Unit) {
    value?.apply(block)
    value = value
}

fun Map<String, String>.zip(): String {
    return TextUtils.join("\n", keys.map { key ->
        "$key: ${this[key]}"
    })
}
