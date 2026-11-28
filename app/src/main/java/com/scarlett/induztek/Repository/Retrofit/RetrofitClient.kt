package com.scarlett.induztek.Repository.Retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BackendRetroFitClient {
    private const val BASE_URL = "http://host"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
