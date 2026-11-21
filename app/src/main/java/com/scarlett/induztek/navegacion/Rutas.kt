package com.scarlett.induztek.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.ui.graphics.vector.ImageVector

// Cada destino de la barra inferior tiene una ruta,
// un título y un ícono.
sealed class Rutas(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
) {
    data object Equipos :
        Rutas("equipos", "Equipos", Icons.Filled.Build)

    data object NuevaPrueba :
        Rutas("nueva_prueba", "Nueva prueba", Icons.Filled.AddCircle)

    data object Historial :
        Rutas("historial", "Historial", Icons.Filled.History)

    data object Publicaciones :
        Rutas("publicaciones", "Publicaciones", Icons.Filled.Cloud)

    companion object {
        val barraInferior = listOf(
            Equipos,
            NuevaPrueba,
            Historial,
            Publicaciones
        )

        const val LOGIN = "login"
        const val REGISTRO = "registro"

        const val ARG_ID = "id"

        const val EQUIPO_DETALLE = "equipo_detalle/{$ARG_ID}"
        const val EQUIPO_EDITAR = "equipo_editar/{$ARG_ID}"

        const val PRUEBA_DETALLE = "prueba_detalle/{$ARG_ID}"
        const val PRUEBA_EDITAR = "prueba_editar/{$ARG_ID}"

        fun equipoDetalle(id: Int) = "equipo_detalle/$id"
        fun equipoEditar(id: Int) = "equipo_editar/$id"

        fun pruebaDetalle(id: Int) = "prueba_detalle/$id"
        fun pruebaEditar(id: Int) = "prueba_editar/$id"
    }
}
