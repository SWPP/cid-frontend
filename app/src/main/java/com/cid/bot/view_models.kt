package com.cid.bot

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.databinding.ObservableField
import com.cid.bot.data.*
import dagger.MapKey
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Provider
import kotlin.math.abs
import kotlin.reflect.KClass
import androidx.databinding.BindingAdapter


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

class CObserver(
        val onError: (Map<String, String>) -> Unit = {},
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

fun Completable.subscribe(vararg observers: CObserver): Disposable {
    return subscribe({
        observers.forEach { it.onFinish() }
    }, { error ->
        error.message?.let { message ->
            observers.forEach { it.onError(mapOf("exception" to message)) }
        }
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

    fun call(completable: Completable, vararg observers: CObserver) {
        compositeDisposable += completable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(*observers)
    }

    val loadingAlpha = ObservableField<Float>()
    private var loadingAnim: ValueAnimator? = null
    fun setLoading(isLoading: Boolean) {
        if (loadingAnim?.isRunning == true) loadingAnim?.cancel()

        val valueFrom = loadingAlpha.get() ?: 0f
        val valueTo = if (isLoading) 1f else 0f

        loadingAnim = ValueAnimator.ofFloat(valueFrom, valueTo).apply {
            duration = (100 * abs(valueTo - valueFrom)).toLong()

            addUpdateListener {
                val value = it.animatedValue as Float
                loadingAlpha.set(value)
            }

            start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed)
            compositeDisposable.dispose()
    }
}

class ProfileViewModel @Inject constructor(private val muserRepo: MuserRepository, private val muserConfigRepo: MuserConfigRepository) : BaseViewModel() {
    val muser = ObservableField<Muser>()
    val muserConfig = ObservableField<MuserConfig>()

    fun loadMuser(vararg observers: HObserver<Muser>) {
        setLoading(true)
        call(muserRepo.getMuser(), HObserver(onSuccess = {
            muser.set(it)
        }, onFinish = {
            setLoading(false)
        }), *observers)
    }

    fun loadMuserConfig(vararg observers: HObserver<MuserConfig>) {
        call(muserConfigRepo.getMuserConfig(), HObserver(onSuccess = {
            muserConfig.set(it)
        }), *observers)
    }

    fun saveMuser(muser: Muser, vararg observers: HObserver<Muser>) {
        call(muserRepo.postMuser(muser), HObserver(onSuccess = {
            this.muser.set(it)
        }), *observers)
    }

    fun saveMuserConfig(muserConfig: MuserConfig, vararg observers: CObserver) {
        call(muserConfigRepo.saveMuserConfig(muserConfig), CObserver(onFinish = {
            this.muserConfig.set(muserConfig)
        }), *observers)
    }

    fun invalidateMuserConfig(vararg observers: HObserver<MuserConfig>) {
        call(muserConfigRepo.invalidateMuserConfig(), HObserver(onSuccess = {
            muserConfig.set(it)
        }), *observers)
    }
}

class ChatViewModel @Inject constructor(private val messageRepo: MessageRepository, private val muserConfigRepo: MuserConfigRepository) : BaseViewModel() {
    val muserConfig = ObservableField<MuserConfig>()
    val messages = MutableLiveData<MutableList<Message>>()

    companion object {
        @JvmStatic
        @BindingAdapter("imageBitmap")
        fun loadImageBitmap(view: ImageView, bitmap: Bitmap?) {
            view.setImageBitmap(bitmap)
        }
    }

    fun loadMessages(vararg observers: HObserver<List<Message>>) {
        setLoading(true)
        call(messageRepo.getMessages(), HObserver(onSuccess = {
            messages.value = it.toMutableList()
        }, onFinish = {
            setLoading(false)
        }), *observers)
    }

    fun loadMessage(id: Int, vararg observers: HObserver<Message>) {
        call(messageRepo.getMessage(id), HObserver(onSuccess = {
            messages.update { add(it) }
        }), *observers)
    }

    fun saveMessage(text: String, vararg observers: HObserver<Message>) {
        call(messageRepo.postMessage(text), HObserver(onSuccess = {
            messages.update { add(it) }
        }), *observers)
    }

    fun loadMuserConfig(vararg observers: HObserver<MuserConfig>) {
        call(muserConfigRepo.getMuserConfig(), HObserver(onSuccess = {
            muserConfig.set(it)
        }), *observers)
    }
}

class SignViewModel @Inject constructor(private val muserConfigRepo: MuserConfigRepository, private val muserRepo: MuserRepository) : BaseViewModel() {
    val muserConfig = ObservableField<MuserConfig>()

    fun loadMuserConfig(vararg observers: HObserver<MuserConfig>) {
        call(muserConfigRepo.getMuserConfig(), HObserver(onSuccess = {
            muserConfig.set(it)
        }), *observers)
    }

    fun saveMuserConfig(muserConfig: MuserConfig, vararg observers: CObserver) {
        call(muserConfigRepo.saveMuserConfig(muserConfig), CObserver(onFinish = {
            this.muserConfig.set(muserConfig)
        }), *observers)
    }

    fun invalidateMuserConfig(vararg observers: HObserver<MuserConfig>) {
        call(muserRepo.clearMuser().andThen(muserConfigRepo.invalidateMuserConfig()), HObserver(onSuccess = {
            muserConfig.set(it)
        }), *observers)
    }
}
