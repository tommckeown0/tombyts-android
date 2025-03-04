package com.example.tombyts_android

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import okhttp3.OkHttpClient

class Classes {
    object ApiProvider {
        private const val BASE_URL = "https://${BuildConfig.HOME_PC_IP}:3001/" // Your backendURL

        val apiService: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }

        private val subtitleRetrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build()) // Create OkHttpClient for subtitle API
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
        }

        fun getSubtitleApiService(): ApiService {
            return subtitleRetrofit.create(ApiService::class.java)
        }
    }
}