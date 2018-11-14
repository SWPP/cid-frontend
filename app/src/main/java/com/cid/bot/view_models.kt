package com.cid.bot

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.databinding.ObservableField
import com.cid.bot.data.Message
import com.cid.bot.data.MessageRepository
import com.cid.bot.data.Muser
import com.cid.bot.data.MuserRepository
import dagger.MapKey
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
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

class ProfileViewModel @Inject constructor(private val repo: MuserRepository) : ViewModel() {
    val muser = ObservableField<Muser>()
    val isLoading = ObservableField<Boolean>()

    private val compositeDisposable = CompositeDisposable()

    init {
        refresh()
    }

    fun refresh() {
        isLoading.set(true)
        compositeDisposable += repo
                .getMuser()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Muser>() {
                    override fun onNext(t: Muser) {
                        muser.set(t)
                    }

                    override fun onError(e: Throwable) {
                        // TODO
                    }

                    override fun onComplete() {
                        isLoading.set(false)
                    }
                })
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}

class ChatViewModel @Inject constructor(private val repo: MessageRepository) : ViewModel() {
    val messages = MutableLiveData<List<Message>>()
    val isLoading = ObservableField<Boolean>()

    private val compositeDisposable = CompositeDisposable()

    init {
        loadMessages()
    }

    fun loadMessages() {
        isLoading.set(true)
        compositeDisposable += repo
                .getMessages()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<List<Message>>() {
                    override fun onNext(t: List<Message>) {
                        messages.value = t
                    }

                    override fun onError(e: Throwable) {
                        // TODO
                    }

                    override fun onComplete() {
                        isLoading.set(false)
                    }
                })
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}
