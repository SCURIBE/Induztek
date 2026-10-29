package com.scarlett.induztek.falsos

import com.scarlett.induztek.data.AlmacenSesion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// La sesión en memoria, en vez de DataStore.
class SesionFalsa(inicial: Int? = null) : AlmacenSesion {

    private val id = MutableStateFlow(inicial)

    override val usuarioId: Flow<Int?> = id

    // Lo que quedó guardado, para revisarlo en el test.
    val actual: Int?
        get() = id.value

    override suspend fun guardar(usuarioId: Int) {
        id.value = usuarioId
    }

    override suspend fun cerrar() {
        id.value = null
    }
}
