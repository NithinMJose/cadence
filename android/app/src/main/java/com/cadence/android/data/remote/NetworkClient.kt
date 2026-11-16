package com.cadence.android.data.remote

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val PREFS_NAME = "cadence_network_prefs"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"
    private const val API_KEY = "cadence-dev-secret-key-change-me"

    @Volatile
    private var apiService: CadenceApiService? = null

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, url.trimEnd('/')).apply()
        synchronized(this) { apiService = null }
    }

    fun getApiService(context: Context): CadenceApiService {
        return apiService ?: synchronized(this) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("X-Cadence-Api-Key", API_KEY)
                        .addHeader("Accept", "application/json")
                        .build()
                    chain.proceed(req)
                }
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(getBaseUrl(context) + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(CadenceApiService::class.java)
            apiService = service
            service
        }
    }
}
