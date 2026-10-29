
package com.scarlett.induztek.data

import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.PruebaConMediciones
import kotlinx.coroutines.flow.Flow

class PruebaRepository(
    private val pruebaDao: PruebaDao
) {

    fun obtenerTodas(): Flow<List<PruebaConMediciones>> {
        return pruebaDao.obtenerTodasConMediciones()
    }

    fun obtenerPorEquipo(
        equipoId: Int
    ): Flow<List<PruebaConMediciones>> {
        return pruebaDao.obtenerPorEquipoConMediciones(equipoId)
    }

    suspend fun registrar(
        prueba: Prueba,
        mediciones: List<Medicion>
    ) {
        pruebaDao.insertarConMediciones(prueba, mediciones)
    }
}
