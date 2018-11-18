package com.cid.bot

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}

fun <T> MutableLiveData<T>.update(block: T.() -> Unit) {
    value?.apply(block)
    value = value
}

fun Map<String, String>.zip(): String {
    return TextUtils.join("\n", keys.map { key ->
        if (key == "_" || key == "error") this[key] else "$key: ${this[key]}"
    })
}

fun Map<String, String>.simple(message: String = "Error Occurred."): String {
    val s = zip()
    return if (s.isBlank()) message else "$message\n$s"
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

fun <T> Observable<HResult<T>>.andSave(
        func: (T) -> Completable
): Observable<HResult<T>> {
    return doOnNext {
        it.data?.also { func(it).subscribeOn(Schedulers.io()).subscribe() }
    }
}

fun SharedPreferences.commit(func: SharedPreferences.Editor.() -> Unit) {
    edit().apply(func).apply()
}
