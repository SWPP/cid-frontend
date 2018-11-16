package com.cid.bot

import android.content.Context
import android.net.ConnectivityManager
import android.text.TextUtils
import com.cid.bot.data.Message
import com.cid.bot.data.Muser
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton

private val interceptor = object : Interceptor {
    val authToken: String
        get() = if (NetworkManager.authToken == null) "" else "Token ${NetworkManager.authToken}"

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val builder = original.newBuilder().header("Authorization", authToken)
        val request = builder.build()
        return chain.proceed(request)
    }
}
val API: ChatBotAPI = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000")    /* development environment */
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().serializeNulls().create()
        ))
        .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
        .build().create(ChatBotAPI::class.java)

interface ChatBotAPI {
    @FormUrlEncoded
    @POST("/chatbot/auth/signup/")
    fun signUp(@Field("username") username: String,
               @Field("password") password: String
    ): Observable<Response<JsonObject>>

    @FormUrlEncoded
    @POST("/chatbot/auth/signin/")
    fun signIn(@Field("username") username: String,
               @Field("password") password: String,
               @Field("push_token") pushToken: String
    ): Observable<Response<JsonObject>>

    @POST("/chatbot/auth/signout/")
    fun signOut(): Observable<Response<JsonObject>>

    @FormUrlEncoded
    @POST("/chatbot/auth/withdraw/")
    fun withdraw(@Field("username") username: String,
                 @Field("password") password: String
    ): Observable<Response<JsonObject>>

    @FormUrlEncoded
    @PATCH("/chatbot/my-info/")
    fun changePassword(@Field("old_password") oldPassword: String,
                       @Field("new_password") newPassword: String
    ): Observable<Response<JsonObject>>

    @GET("/chatbot/my-info")
    fun loadMyInfo(): Observable<Response<Muser>>

    @PATCH("/chatbot/my-info/")
    fun saveMyInfo(@Body muser: Muser): Observable<Response<Muser>>

    @GET("/chatbot/chat/")
    fun loadAllMessages(): Observable<Response<List<Message>>>

    @GET("/chatbot/chat/{id}/")
    fun loadMessage(@Path("id") id: Int): Observable<Response<Message>>

    @FormUrlEncoded
    @POST("/chatbot/chat/")
    fun sendMessage(@Field("text") text: String): Observable<Response<Message>>
}

fun <T> Observable<Response<T>>.toHResult(): Observable<HResult<T>> {
    return map { response ->
        if (response.isSuccessful)
            HResult(response.body()!!)
        else
            HResult(HashMap<String, String>().apply {
                val errorString = response.errorBody()?.string() ?: ""
                val jsonObject = JSONObject(errorString)
                for (key in jsonObject.keys()) {
                    var list: JSONArray? = null
                    try {
                        list = jsonObject.getJSONArray(key)
                    } catch (e: JSONException) {}
                    if (list == null) list = JSONArray("[\"${jsonObject.getString(key)}\"")

                    val strings = mutableListOf<String>()
                    for (i in 0 until list.length()) {
                        strings += list.getString(i)
                    }

                    this[key] = TextUtils.join("\n", strings)
                }
            })
    }.onErrorResumeNext(Function {
        it.message?.let { Observable.just(HResult(mapOf("exception" to it))) }
    })
}

object NetworkManager {
    var authToken: String? = null

    /**
     * Helper function of API methods.
     * Provide simple abstraction of observations.
     *
     * @param observable Response object returned from API methods
     * @param onSuccess  Called on success response(200)
     * @param onError    Called on error response(not 200)
     * @param onFinish   Called on finish whichever success or error
     * @return A disposable object. By disposing it, you can cancel subscripting.
     */
    fun <T> call(observable: Observable<Response<T>>,
                 onSuccess: (T) -> Unit,
                 onError: (Map<String, String>) -> Unit,
                 onFinish: () -> Unit = {}
    ): Disposable {
        return observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    onFinish()
                    if (response.isSuccessful)
                        response.body()?.let { onSuccess(it) }
                    else {
                        onError(HashMap<String, String>().apply {
                            val errorString = response.errorBody()?.string() ?: ""
                            val jsonObject = JSONObject(errorString)
                            for (key in jsonObject.keys()) {
                                var list: JSONArray? = null
                                try {
                                    list = jsonObject.getJSONArray(key)
                                } catch (e: JSONException) {}
                                if (list == null) list = JSONArray("[\"${jsonObject.getString(key)}\"")

                                val strings = mutableListOf<String>()
                                for (i in 0 until list.length()) {
                                    strings += list.getString(i)
                                }

                                this[key] = TextUtils.join("\n", strings)
                            }
                        })
                    }
                }, { error ->
                    onFinish()
                    error.message?.let { onError(mapOf("exception??" to it)) }
                })
    }
}

@Singleton
class NetManager @Inject constructor(private val context: Context) {
    val isConnectedToInternet: Boolean?
        get() {
            val conManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = conManager.activeNetworkInfo
            return ni != null && ni.isConnected
        }

    fun <T> getNetworkError() = HResult<T>(mapOf("error" to "Network Error"))
}
