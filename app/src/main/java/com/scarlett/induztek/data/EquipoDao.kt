
package com.scarlett.induztek.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.scarlett.induztek.model.Equipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {

    @Insert
    suspend fun insertar(equipo: Equipo)

    @Query("SELECT * FROM equipos ORDER BY codigo ASC")
    fun obtenerTodos(): Flow<List<Equipo>>
}
