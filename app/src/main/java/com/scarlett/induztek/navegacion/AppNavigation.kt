
package com.scarlett.induztek.navegacion

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.model.Usuario
import com.scarlett.induztek.ui.screens.EditarEquipoScreen
import com.scarlett.induztek.ui.screens.EquipoDetalleScreen
import com.scarlett.induztek.ui.screens.EquiposScreen
import com.scarlett.induztek.ui.screens.HistorialScreen
import com.scarlett.induztek.ui.screens.NuevaPruebaScreen
import com.scarlett.induztek.ui.screens.PruebaDetalleScreen
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    usuario: Usuario,
    alCerrarSesion: () -> Unit
) {
    val navController = rememberNavController()

    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    val tituloActual = when (rutaActual) {
        Rutas.EQUIPO_DETALLE -> "Detalle del equipo"
        Rutas.EQUIPO_EDITAR -> "Editar Equipo"
        Rutas.PRUEBA_DETALLE -> "Protocolo de prueba"
        else -> Rutas.barraInferior
            .find { it.ruta == rutaActual }
            ?.titulo ?: "Induztek"
    }

    val mostrarBarrasInferiores =
        rutaActual != Rutas.EQUIPO_DETALLE &&
                rutaActual != Rutas.EQUIPO_EDITAR &&
                rutaActual != Rutas.PRUEBA_DETALLE

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tituloActual) },
                actions = {
                    IconButton(onClick = alCerrarSesion) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
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

        val pruebaRepository = remember {
            RepositoryProvider.obtenerPruebaRepository(context)
        }

        val pruebaViewModel: PruebaViewModel = viewModel(
            factory = PruebaViewModelFactory(pruebaRepository)
        )

        val equipos by equipoViewModel.equipos.collectAsState()
        val pruebas by pruebaViewModel.pruebas.collectAsState()

        NavHost(
            navController = navController,
            startDestination = Rutas.Equipos.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Rutas.Equipos.ruta) {
                EquiposScreen(
                    alSeleccionarEquipo = { equipo ->
                        navController.navigate(Rutas.equipoDetalle(equipo.id))
                    }
                )
            }

            composable(
                route = Rutas.EQUIPO_DETALLE,
                arguments = listOf(
                    navArgument(Rutas.ARG_ID) { type = NavType.IntType }
                )
            ) { entrada ->
                val id = entrada.arguments?.getInt(Rutas.ARG_ID)
                val equipo = equipos.find { it.id == id }

                if (equipo != null) {
                    val pruebasDelEquipo = pruebas.filter {
                        it.prueba.equipoId == equipo.id
                    }

                    EquipoDetalleScreen(
                        equipo = equipo,
                        pruebas = pruebasDelEquipo,
                        alVerPrueba = { pruebaId ->
                            navController.navigate(
                                Rutas.pruebaDetalle(pruebaId)
                            )
                        },
                        alVolver = {
                            navController.popBackStack()
                        },
                        alEditar = { seleccionado ->
                            navController.navigate(
                                Rutas.equipoEditar(seleccionado.id)
                            )
                        },
                        alEliminar = { seleccionado ->
                            equipoViewModel.eliminar(seleccionado.id)
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(
                route = Rutas.EQUIPO_EDITAR,
                arguments = listOf(
                    navArgument(Rutas.ARG_ID) { type = NavType.IntType }
                )
            ) { entrada ->
                val id = entrada.arguments?.getInt(Rutas.ARG_ID)
                val equipo = equipos.find { it.id == id }

                if (equipo != null) {
                    val error by equipoViewModel.error.collectAsState()
                    EditarEquipoScreen(
                        equipo = equipo,
                        errorExterno = error,
                        alGuardar = { actualizado, resultado ->
                            equipoViewModel.actualizar(actualizado) { guardado ->
                                resultado(guardado)

                                if (guardado) {
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

            composable(
                route = Rutas.PRUEBA_DETALLE,
                arguments = listOf(
                    navArgument(Rutas.ARG_ID) { type = NavType.IntType }
                )
            ) { entrada ->
                val id = entrada.arguments?.getInt(Rutas.ARG_ID)

                if (id != null) {
                    PruebaDetalleScreen(
                        pruebaId = id,
                        alVolver = { navController.popBackStack() }
                    )
                }
            }

            composable(Rutas.NuevaPrueba.ruta) {
                NuevaPruebaScreen(
                    tecnicoInicial = usuario.nombreCompleto
                )
            }

            composable(Rutas.Historial.ruta) {
                HistorialScreen(
                    alSeleccionarPrueba = { pruebaId ->
                        navController.navigate(
                            Rutas.pruebaDetalle(pruebaId)
                        )
                    }
                )
            }
        }
    }
}
