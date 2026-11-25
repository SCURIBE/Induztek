package com.scarlett.induztek.Repository.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.scarlett.induztek.Repository.model.Medicion
import com.scarlett.induztek.Repository.model.Prueba
import com.scarlett.induztek.Repository.model.PruebaConMediciones
import kotlinx.coroutines.flow.Flow

@Dao
interface PruebaDao {

    @Insert
    suspend fun insertarPrueba(prueba: Prueba): Long

    @Insert
    suspend fun insertarMediciones(mediciones: List<Medicion>)

    //actualizar una prueba existente
    @Update
    suspend fun actualizarPrueba(prueba: Prueba)

    //eliminar las mediciones anteriores
    @Query("DELETE FROM mediciones WHERE pruebaId = :pruebaId")
    suspend fun eliminarMedicionesDePrueba(pruebaId: Int)

    //Eliminar prueba existente
    @Query("DELETE FROM pruebas WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    // actualizar prueba y sus mediciones
    @Transaction
    suspend fun actualizarConMediciones(
        prueba: Prueba,
        mediciones: List<Medicion>
    ) {
        actualizarPrueba(prueba)
        eliminarMedicionesDePrueba(prueba.id)

        if (mediciones.isNotEmpty()) {
            insertarMediciones(
                mediciones.map {
                    it.copy(id = 0, pruebaId = prueba.id)
                }
            )
        }
    }

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
