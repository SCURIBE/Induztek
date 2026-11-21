package com.scarlett.induztek.Repository.Retrofit

import com.scarlett.induztek.Repository.model.Post

class PostRepository {

    suspend fun obtenerPosts(): List<Post> {
        return RetrofitClient.apiService.obtenerPosts()
    }
}