package com.cid.bot

import androidx.lifecycle.MutableLiveData
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
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

fun View.applyErrors(errors: Map<String, String>): Map<String, String> {
    val list = mutableListOf<String>()
    val rest = mutableMapOf<String, String>()
    for ((key, value) in errors) {
        try {
            val view = findViewWithTag(key) as EditText
            view.error = value
            list.add(key)
        } catch (e: ClassCastException) {
            rest[key] = value
        }
    }
    tag = list
    return rest
}

fun <T> singleObservable(func: () -> T): Observable<T> {
    return Observable.create {
        it.onNext(func())
        it.onComplete()
    }
}

fun singleCompletable(func: () -> Unit): Completable {
    return Completable.create {
        func()
        it.onComplete()
    }
}
