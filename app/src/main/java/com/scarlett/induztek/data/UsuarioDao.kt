package com.scarlett.induztek.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.scarlett.induztek.model.Usuario

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insertar(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario")
    suspend fun buscarPorNombre(nombreUsuario: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Int): Usuario?

    @Query(
        "SELECT EXISTS(SELECT 1 FROM usuarios " +
                "WHERE nombreUsuario = :nombreUsuario)"
    )
    suspend fun existeNombre(nombreUsuario: String): Boolean

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int
}
