
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

    suspend fun actualizar(equipo: Equipo) {
        equipoDao.actualizar(equipo)
    }

    suspend fun existeCodigo(codigo: String): Boolean {
        return equipoDao.existeCodigo(codigo)
    }

    suspend fun existeCodigoEnOtroEquipo(
        codigo: String,
        id: Int
    ): Boolean {
        return equipoDao.existeCodigoEnOtroEquipo(codigo, id)
    }

    suspend fun eliminarPorId(id: Int) {
        equipoDao.eliminarPorId(id)
    }
}
