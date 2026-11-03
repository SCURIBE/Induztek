package com.scarlett.induztek.Repository.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.scarlett.induztek.Repository.model.TipoUsuario
import kotlinx.coroutines.flow.Flow

@Dao
interface TipoUsuarioDao {
    @Insert
    suspend fun insertar(tipoUsuario: TipoUsuario): Long

    @Query("SELECT * FROM tipo_usuarios ORDER BY id ASC")
    fun obtenerTodos(): Flow<List<TipoUsuario>>

    @Query("SELECT * FROM tipo_usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Int): TipoUsuario?
}
