package com.scarlett.induztek.navegation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

// Cada destino de la barra inferior tiene una ruta (su identificador único),
// un título que se muestra en pantalla y un ícono.
// Usamos sealed class porque los destinos son un conjunto cerrado:
// solo existen estos tres y el compilador lo sabe.
sealed class Rutas(val ruta: String, val titulo: String, val icono: ImageVector) {
    data object Equipos : Rutas("equipos", "Equipos", Icons.Filled.Build)
    data object NuevaPrueba : Rutas("nueva_prueba", "Nueva prueba", Icons.Filled.AddCircle)
    data object Historial : Rutas("historial", "Historial", Icons.Filled.History)

    companion object {
        // Lista que recorrerá la NavigationBar para dibujar cada ítem
        val barraInferior = listOf(Equipos, NuevaPrueba, Historial)
    }
}
