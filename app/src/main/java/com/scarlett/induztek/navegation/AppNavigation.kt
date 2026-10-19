
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.ui.theme.screens.EditarEquipoScreen
import com.scarlett.induztek.ui.theme.screens.EquipoDetalleScreen
import com.scarlett.induztek.ui.theme.screens.EquiposScreen
import com.scarlett.induztek.ui.theme.screens.HistorialScreen
import com.scarlett.induztek.ui.theme.screens.NuevaPruebaScreen
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    val tituloActual = when (rutaActual) {
        "equipo_detalle" -> "Detalle del equipo"
        "equipo_editar" -> "Editar equipo"
        else -> Rutas.barraInferior
            .find { it.ruta == rutaActual }
            ?.titulo ?: "Induztek"
    }

    val mostrarBarrasInferiores =
        rutaActual != "equipo_detalle" &&
                rutaActual != "equipo_editar"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tituloActual) }
            )
        },
        bottomBar = {
            if (mostrarBarrasInferiores) {
                NavigationBar {
                    Rutas.barraInferior.forEach { destino ->
                        NavigationBarItem(
                            selected = rutaActual == destino.ruta,
                            onClick = {
                                navController.navigate(destino.ruta) {
                                    popUpTo(
                                        navController.graph
                                            .findStartDestination().id
                                    ) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    destino.icono,
                                    contentDescription = destino.titulo
                                )
                            },
                            label = { Text(destino.titulo) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        val context = androidx.compose.ui.platform.LocalContext.current

        val repository = remember {
            RepositoryProvider.obtenerEquipoRepository(context)
        }

        val factory = remember {
            EquipoViewModelFactory(repository)
        }

        val equipoViewModel: EquipoViewModel = viewModel(
            factory = factory
        )

        val equipos by equipoViewModel.equipos.collectAsState()

        var equipoSeleccionado by remember {
            mutableStateOf<Equipo?>(null)
        }

        NavHost(
            navController = navController,
            startDestination = Rutas.Equipos.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Rutas.Equipos.ruta) {
                EquiposScreen(
                    alSeleccionarEquipo = { equipo ->
                        equipoSeleccionado = equipo
                        navController.navigate("equipo_detalle")
                    }
                )
            }

            composable("equipo_detalle") {
                val equipo = equipos.find {
                    it.id == equipoSeleccionado?.id
                }

                if (equipo != null) {
                    EquipoDetalleScreen(
                        equipo = equipo,
                        alVolver = {
                            navController.popBackStack()
                        },
                        alEditar = { seleccionado ->
                            equipoSeleccionado = seleccionado
                            navController.navigate("equipo_editar")
                        },
                        alEliminar = { seleccionado ->
                            equipoViewModel.eliminar(seleccionado.id)
                            equipoSeleccionado = null
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable("equipo_editar") {
                val equipo = equipos.find {
                    it.id == equipoSeleccionado?.id
                }

                if (equipo != null) {
                    val error by equipoViewModel.error.collectAsState()
                    EditarEquipoScreen(
                        equipo = equipo,
                        errorExterno = error,
                        alGuardar = { actualizado, resultado ->
                            equipoViewModel.actualizar(actualizado) { guardado ->
                                resultado(guardado)

                                if (guardado) {
                                    equipoSeleccionado = actualizado
                                    navController.popBackStack()
                                }
                            }
                        },
                        alCancelar = {
                            equipoViewModel.limpiarError()
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Rutas.NuevaPrueba.ruta) {
                NuevaPruebaScreen()
            }

            composable(Rutas.Historial.ruta) {
                HistorialScreen()
            }
        }
    }
}

/*
 * Adaptador para collectAsState sin cambiar el resto de la navegación.
 */

