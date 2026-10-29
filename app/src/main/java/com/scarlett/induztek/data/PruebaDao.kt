
package com.scarlett.induztek.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.PruebaConMediciones
import kotlinx.coroutines.flow.Flow

@Dao
interface PruebaDao {

    @Insert
    suspend fun insertarPrueba(prueba: Prueba): Long

    @Insert
    suspend fun insertarMediciones(mediciones: List<Medicion>)

    // Guardar la prueba y sus mediciones es una sola operación: si algo
    // falla a mitad de camino no debe quedar una prueba sin mediciones.
    // La prueba se inserta primero porque las mediciones necesitan su id.
    @Transaction
    suspend fun insertarConMediciones(
        prueba: Prueba,
        mediciones: List<Medicion>
    ) {
        val pruebaId = insertarPrueba(prueba).toInt()

        if (mediciones.isNotEmpty()) {
            insertarMediciones(
                mediciones.map { it.copy(pruebaId = pruebaId) }
            )
        }
    }

    @Transaction
    @Query("SELECT * FROM pruebas ORDER BY fechaHora DESC")
    fun obtenerTodasConMediciones(): Flow<List<PruebaConMediciones>>

    // Devuelve null si la prueba ya no existe, por ejemplo si se
    // elimino su equipo mientras la pantalla de detalle estaba abierta.
    @Transaction
    @Query("SELECT * FROM pruebas WHERE id = :id")
    fun obtenerPorIdConMediciones(id: Int): Flow<PruebaConMediciones?>

    @Transaction
    @Query(
        "SELECT * FROM pruebas " +
                "WHERE equipoId = :equipoId " +
                "ORDER BY fechaHora DESC"
    )
    fun obtenerPorEquipoConMediciones(
        equipoId: Int
    ): Flow<List<PruebaConMediciones>>
}
