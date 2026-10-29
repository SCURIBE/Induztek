package com.scarlett.induztek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.data.SesionStore
import com.scarlett.induztek.navegacion.AppNavigation
import com.scarlett.induztek.navegacion.AutenticacionNavigation
import com.scarlett.induztek.ui.componentes.PantallaCargando
import com.scarlett.induztek.ui.theme.InduztekTheme
import com.scarlett.induztek.util.AlmacenFotos
import com.scarlett.induztek.viewmodel.AuthViewModel
import com.scarlett.induztek.viewmodel.AuthViewModelFactory
import com.scarlett.induztek.viewmodel.EstadoSesion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solo en un arranque nuevo. Al girar la pantalla savedInstanceState
        // no es null, y podría haber un formulario abierto con una foto
        // tomada y todavía sin guardar.
        if (savedInstanceState == null) {
            limpiarFotosHuerfanas()
        }

        setContent {
            InduztekTheme {
                val context = LocalContext.current

                val authViewModel: AuthViewModel = viewModel(
                    factory = remember {
                        AuthViewModelFactory(
                            RepositoryProvider.obtenerUsuarioRepository(context),
                            SesionStore(context)
                        )
                    }
                )

                val estado by authViewModel.estado.collectAsState()

                // El portón de la sesión está aquí, por encima del
                // NavHost de la app: así el login no necesita ocultar la
                // barra inferior ni dejar rastro en la pila de retroceso.
                when (val actual = estado) {
                    EstadoSesion.Cargando -> PantallaCargando()

                    EstadoSesion.SinSesion -> AutenticacionNavigation(
                        authViewModel = authViewModel
                    )

                    is EstadoSesion.Autenticado -> AppNavigation(
                        usuario = actual.usuario,
                        alCerrarSesion = { authViewModel.cerrarSesion() }
                    )
                }
            }
        }
    }

    // Ver AlmacenFotos.limpiarHuerfanas: borra las fotos que quedaron
    // sin equipo, por ejemplo al salir de un formulario con el botón atrás.
    private fun limpiarFotosHuerfanas() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val enUso = RepositoryProvider
                    .obtenerEquipoRepository(applicationContext)
                    .rutasDeFotos()

                AlmacenFotos.limpiarHuerfanas(
                    AlmacenFotos.carpeta(applicationContext),
                    enUso
                )
            } catch (_: Exception) {
                // Si la base no se puede abrir, mejor no borrar nada. La
                // pantalla mostrará el problema al intentar leerla.
            }
        }
    }
}
