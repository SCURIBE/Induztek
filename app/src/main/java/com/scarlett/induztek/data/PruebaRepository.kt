
package com.scarlett.induztek.data

import com.scarlett.induztek.model.Prueba
import kotlinx.coroutines.flow.Flow

class PruebaRepository(
    private val pruebaDao: PruebaDao
) {

    fun obtenerTodas(): Flow<List<Prueba>> {
        return pruebaDao.obtenerTodas()
    }

    fun obtenerPorEquipo(
        codigoEquipo: String
    ): Flow<List<Prueba>> {
        return pruebaDao.obtenerPorEquipo(codigoEquipo)
    }

    suspend fun insertar(prueba: Prueba) {
        pruebaDao.insertar(prueba)
    }
}
