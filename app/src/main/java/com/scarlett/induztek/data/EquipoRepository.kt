
package com.scarlett.induztek.data

import com.scarlett.induztek.model.Equipo
import kotlinx.coroutines.flow.Flow

class EquipoRepository(
    private val equipoDao: EquipoDao
) {
    fun obtenerTodos(): Flow<List<Equipo>> {
        return equipoDao.obtenerTodos()
    }

    suspend fun insertar(equipo: Equipo) {
        equipoDao.insertar(equipo)
    }
}
