package com.scarlett.induztek.navegation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scarlett.induztek.ui.theme.screens.EquiposScreen
import com.scarlett.induztek.ui.theme.screens.HistorialScreen
import com.scarlett.induztek.ui.theme.screens.NuevaPruebaScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    // NavController: el "capitán". Sabe en qué pantalla estamos y maneja la pila.
    val navController = rememberNavController()

    // Observamos la entrada actual de la pila. Cada vez que navegamos,
    // este valor cambia y Compose redibuja lo que dependa de él.
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    // Buscamos el título según la ruta actual; si no calza ninguna, usamos "Induztek"
    val tituloActual = Rutas.barraInferior.find { it.ruta == rutaActual }?.titulo ?: "Induztek"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(tituloActual) })
        },
        bottomBar = {
            NavigationBar {
                Rutas.barraInferior.forEach { destino ->
                    NavigationBarItem(
                        selected = rutaActual == destino.ruta,
                        onClick = {
                            navController.navigate(destino.ruta) {
                                // Vuelve al inicio del grafo para no apilar pantallas
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Si ya estás en esa pantalla, no crea otra copia
                                launchSingleTop = true
                                // Restaura el estado guardado al volver a una pestaña
                                restoreState = true
                            }
                        },
                        icon = { Icon(destino.icono, contentDescription = destino.titulo) },
                        label = { Text(destino.titulo) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // NavHost: el "escenario". Muestra la pantalla que corresponde a la ruta actual.
        // innerPadding evita que el contenido quede tapado por las barras.
        NavHost(
            navController = navController,
            startDestination = Rutas.Equipos.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Rutas.Equipos.ruta) { EquiposScreen() }
            composable(Rutas.NuevaPrueba.ruta) { NuevaPruebaScreen() }
            composable(Rutas.Historial.ruta) { HistorialScreen() }
        }
    }
}