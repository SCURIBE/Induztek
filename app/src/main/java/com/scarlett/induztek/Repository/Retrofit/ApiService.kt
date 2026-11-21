package com.scarlett.induztek.Repository.Retrofit

import com.scarlett.induztek.Repository.model.Post
import retrofit2.http.GET

interface ApiService {

    @GET("posts")
    suspend fun obtenerPosts(): List<Post>
}
