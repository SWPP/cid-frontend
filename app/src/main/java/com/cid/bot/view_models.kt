package com.cid.bot

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.databinding.ObservableField
import com.cid.bot.data.*
import dagger.MapKey
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MapKey
annotation class ViewModelKey(val value: KClass<out ViewModel>)

class DaggerAwareViewModelFactory @Inject constructor(private val creators: @JvmSuppressWildcards Map<Class<out ViewModel>, Provider<ViewModel>>) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        var creator: Provider<out ViewModel>? = creators[modelClass]
        if (creator == null) {
            for ((key, value) in creators) {
                if (modelClass.isAssignableFrom(key)) {
                    creator = value
                    break
                }
            }
        }
        if (creator == null) {
            throw IllegalArgumentException("unknown model class $modelClass")
        }
        try {
            @Suppress("UNCHECKED_CAST")
            return creator.get() as T
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}

class HResult<T> {
    val data: T?
    val error: Map<String, String>?
    constructor(data: T) {
        this.data = data
        this.error = null
    }
    constructor(error: Map<String, String>) {
        this.data = null
        this.error = error
    }
}

class HObserver<T>(
        val onError: (Map<String, String>) -> Unit = {},
        val onSuccess: (T) -> Unit = {},
        val onFinish: () -> Unit = {}
)

fun <T> Observable<HResult<T>>.subscribe(vararg observers: HObserver<T>): Disposable {
    return subscribe({ result ->
        result.data?.let { data ->
            observers.forEach { it.onSuccess(data) }
        }
        result.error?.let { error ->
            observers.forEach { it.onError(error) }
        }
    }, { error ->
        error.message?.let { message ->
            observers.forEach { it.onError(mapOf("exception" to message)) }
        }
    }, {
        observers.forEach { it.onFinish() }
    })
}

open class BaseViewModel : ViewModel() {
    private val compositeDisposable = CompositeDisposable()

    fun <T> call(observable: Observable<HResult<T>>, vararg observers: HObserver<T>) {
        compositeDisposable += observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(*observers)
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}

class ProfileViewModel @Inject constructor(private val repo: MuserRepository) : BaseViewModel() {
    val muser = ObservableField<Muser>()
    val isLoading = ObservableField<Boolean>()

    init {
        loadMuser()
    }

    fun loadMuser(vararg observers: HObserver<Muser>) {
        isLoading.set(true)
        call(repo.getMuser(), HObserver(onSuccess = {
            muser.set(it)
        }, onFinish = {
            isLoading.set(false)
        }), *observers)
    }

    fun saveMuser(muser: Muser, vararg observers: HObserver<Muser>) {
        call(repo.postMuser(muser), HObserver(onSuccess = {
            this.muser.set(it)
        }), *observers)
    }
}

class ChatViewModel @Inject constructor(private val repo: MessageRepository) : BaseViewModel() {
    val messages = MutableLiveData<MutableList<Message>>()
    val isLoading = ObservableField<Boolean>()

    init {
        loadMessages()
    }

    fun loadMessages(vararg observers: HObserver<List<Message>>) {
        isLoading.set(true)
        call(repo.getMessages(), HObserver(onSuccess = {
            messages.value = it.toMutableList()
        }, onFinish = {
            isLoading.set(false)
        }), *observers)
    }

    fun loadMessage(id: Int, vararg observers: HObserver<Message>) {
        call(repo.getMessage(id), HObserver(onSuccess = {
            messages.update { add(it) }
        }), *observers)
    }

    fun saveMessage(text: String, vararg observers: HObserver<Message>) {
        call(repo.postMessage(text), HObserver(onSuccess = {
            messages.update { add(it) }
        }), *observers)
    }
}
