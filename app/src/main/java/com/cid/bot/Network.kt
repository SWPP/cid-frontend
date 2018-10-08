package com.cid.bot

import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

val API: ChatBotAPI = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000")    /* development environment */
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(
                GsonBuilder().serializeNulls().create()
        ))
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder().header("Authorization", NetworkManager.authToken)
            val request = builder.build()
            chain.proceed(request)
        }.build())
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
               @Field("password") password: String
    ): Observable<Response<JsonObject>>
}

object NetworkManager {
    var authToken = ""
        set(value) {
            field = "Token $value"
        }

    fun <T> call(observable: Observable<Response<T>>, onSuccess: (T) -> Unit, onError: (Map<String, String>) -> Unit): Disposable {
        return observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
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
                    error.message?.let { onError(mapOf("exception" to it)) }
                })
    }
}
