package com.scarlett.induztek.Repository.data

import com.scarlett.induztek.Repository.model.Medicion
import com.scarlett.induztek.Repository.model.Prueba
import com.scarlett.induztek.Repository.model.PruebaConMediciones
import kotlinx.coroutines.flow.Flow

class PruebaRepository(
    private val pruebaDao: PruebaDao
) {

    fun obtenerTodas(): Flow<List<PruebaConMediciones>> {
        return pruebaDao.obtenerTodasConMediciones()
    }

    fun obtenerPorId(id: Int): Flow<PruebaConMediciones?> {
        return pruebaDao.obtenerPorIdConMediciones(id)
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
