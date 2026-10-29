package com.scarlett.induztek.falsos

import com.scarlett.induztek.data.EquipoDao
import com.scarlett.induztek.model.Equipo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// EquipoDao en memoria. Se comporta como la consulta real: ordena por
// código y compara el código distinguiendo mayúsculas, igual que el
// "=" de SQLite.
class EquipoDaoFalso(iniciales: List<Equipo> = emptyList()) : EquipoDao {

    val equipos = MutableStateFlow(iniciales)
    private var siguienteId = (iniciales.maxOfOrNull { it.id } ?: 0) + 1

    override suspend fun insertar(equipo: Equipo) {
        equipos.value = equipos.value + equipo.copy(id = siguienteId++)
    }

    override suspend fun actualizar(equipo: Equipo) {
        equipos.value = equipos.value.map {
            if (it.id == equipo.id) equipo else it
        }
    }

    override fun obtenerTodos(): Flow<List<Equipo>> {
        return equipos.map { lista -> lista.sortedBy { it.codigo } }
    }

    override suspend fun existeCodigo(codigo: String): Boolean {
        return equipos.value.any { it.codigo == codigo }
    }

    override suspend fun existeCodigoEnOtroEquipo(
        codigo: String,
        id: Int
    ): Boolean {
        return equipos.value.any { it.codigo == codigo && it.id != id }
    }

    override suspend fun eliminarPorId(id: Int) {
        equipos.value = equipos.value.filterNot { it.id == id }
    }

    override suspend fun buscarPorId(id: Int): Equipo? {
        return equipos.value.find { it.id == id }
    }

    override suspend fun rutasDeFotos(): List<String> {
        return equipos.value.mapNotNull { it.fotoUri }
    }
}
