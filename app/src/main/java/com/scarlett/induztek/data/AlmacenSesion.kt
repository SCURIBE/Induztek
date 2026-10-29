package com.scarlett.induztek.data

import kotlinx.coroutines.flow.Flow

/*
 * Lo que AuthViewModel necesita de la sesión guardada. Es una interfaz
 * para que los tests usen una sesión en memoria: SesionStore necesita un
 * Context de Android para abrir DataStore, y un test de JUnit no lo tiene.
 */
interface AlmacenSesion {

    val usuarioId: Flow<Int?>

    suspend fun guardar(usuarioId: Int)

    suspend fun cerrar()
}
