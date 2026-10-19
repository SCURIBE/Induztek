
package com.scarlett.induztek.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.scarlett.induztek.model.Prueba
import kotlinx.coroutines.flow.Flow

@Dao
interface PruebaDao {

    @Insert
    suspend fun insertar(prueba: Prueba)

    @Query("SELECT * FROM pruebas ORDER BY fechaHora DESC")
    fun obtenerTodas(): Flow<List<Prueba>>

    @Query(
        "SELECT * FROM pruebas " +
                "WHERE codigoEquipo = :codigoEquipo " +
                "ORDER BY fechaHora DESC"
    )
    fun obtenerPorEquipo(codigoEquipo: String): Flow<List<Prueba>>
}
