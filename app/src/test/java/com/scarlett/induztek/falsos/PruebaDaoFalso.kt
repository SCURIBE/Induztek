package com.scarlett.induztek.falsos

import com.scarlett.induztek.data.PruebaDao
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.PruebaConMediciones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

// PruebaDao en memoria. insertarConMediciones no se reescribe: se usa la
// del DAO real, que inserta la prueba y luego sus mediciones con el id.
class PruebaDaoFalso : PruebaDao {

    val pruebas = MutableStateFlow<List<Prueba>>(emptyList())
    val mediciones = MutableStateFlow<List<Medicion>>(emptyList())

    private var siguientePrueba = 1
    private var siguienteMedicion = 1

    override suspend fun insertarPrueba(prueba: Prueba): Long {
        val id = siguientePrueba++
        pruebas.value = pruebas.value + prueba.copy(id = id)
        return id.toLong()
    }

    override suspend fun insertarMediciones(mediciones: List<Medicion>) {
        this.mediciones.value = this.mediciones.value +
                mediciones.map { it.copy(id = siguienteMedicion++) }
    }

    override fun obtenerTodasConMediciones(): Flow<List<PruebaConMediciones>> {
        return combine(pruebas, mediciones) { lista, todas ->
            lista.sortedByDescending { it.fechaHora }
                .map { prueba -> conMediciones(prueba, todas) }
        }
    }

    override fun obtenerPorIdConMediciones(
        id: Int
    ): Flow<PruebaConMediciones?> {
        return combine(pruebas, mediciones) { lista, todas ->
            lista.find { it.id == id }?.let { conMediciones(it, todas) }
        }
    }

    override fun obtenerPorEquipoConMediciones(
        equipoId: Int
    ): Flow<List<PruebaConMediciones>> {
        return combine(pruebas, mediciones) { lista, todas ->
            lista.filter { it.equipoId == equipoId }
                .sortedByDescending { it.fechaHora }
                .map { prueba -> conMediciones(prueba, todas) }
        }
    }

    private fun conMediciones(
        prueba: Prueba,
        todas: List<Medicion>
    ): PruebaConMediciones {
        return PruebaConMediciones(
            prueba = prueba,
            mediciones = todas.filter { it.pruebaId == prueba.id }
        )
    }
}
