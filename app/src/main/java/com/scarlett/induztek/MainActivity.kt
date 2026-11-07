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
import com.scarlett.induztek.Repository.data.RepositoryProvider
import com.scarlett.induztek.Repository.data.SesionStore
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

                AnimatedContent(
                    targetState = estado,
                    contentKey = { it::class },
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut()
                    },
                    label = "transicionSesion"
                ) { actual ->
                    when (actual) {
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
    }

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

            }
        }
    }
}
