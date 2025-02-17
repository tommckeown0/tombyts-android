package com.example.tombyts_android

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Classes {
    object ApiProvider {
        private const val BASE_URL = "https://${BuildConfig.HOME_PC_IP}:3001/" // Your backendURL
//        init {
//            Log.d("blah", "BASE_URL: $BASE_URL")
//        }

        val apiService: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}