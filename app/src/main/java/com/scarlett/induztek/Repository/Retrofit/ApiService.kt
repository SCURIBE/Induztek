package com.scarlett.induztek.Repository.Retrofit

import com.scarlett.induztek.Repository.model.Equipo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path

interface ApiService {

    @GET("/api/equipos")
    suspend fun obtenerEquipos(): Response<List<Equipo>>

    @GET("/api/equipos/{id}")
    suspend fun obtenerEquipo(@Path("id")id: Int): Response<Equipo>

    @POST("/api/equipos")
    suspend fun crearEquipo(@Body equipo: Equipo): Response<Equipo>

    @PUT("/api/equipos/{id}")
    suspend fun actualizarEquipo(
        @Path("id") id: Int,
        @Body equipo: Equipo): Response<Equipo>

    @DELETE("/api/equipos/{id}")
    suspend fun eliminarEquipo(
        @Path("id")id: Int): Response<Unit>
}
