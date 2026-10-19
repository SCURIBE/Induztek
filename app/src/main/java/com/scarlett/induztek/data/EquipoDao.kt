
package com.scarlett.induztek.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.scarlett.induztek.model.Equipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {

    @Insert
    suspend fun insertar(equipo: Equipo)

    @Update
    suspend fun actualizar(equipo: Equipo)

    @Query("SELECT * FROM equipos ORDER BY codigo ASC")
    fun obtenerTodos(): Flow<List<Equipo>>

    @Query("SELECT EXISTS(SELECT 1 FROM equipos WHERE codigo = :codigo)")
    suspend fun existeCodigo(codigo: String): Boolean

    @Query(
        "SELECT EXISTS(" +
                "SELECT 1 FROM equipos " +
                "WHERE codigo = :codigo AND id != :id" +
                ")"
    )
    suspend fun existeCodigoEnOtroEquipo(
        codigo: String,
        id: Int
    ): Boolean

    @Query("DELETE FROM equipos WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}
