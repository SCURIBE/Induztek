package com.scarlett.induztek.navegacion

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scarlett.induztek.ui.screens.LoginScreen
import com.scarlett.induztek.ui.screens.RegistroScreen
import com.scarlett.induztek.viewmodel.AuthViewModel

/*
 * Grafo propio para el login, separado del de la app. No lleva Scaffold
 * ni barra inferior, y cuando la sesión se abre desaparece completo: así
 * el botón atrás desde Equipos no puede volver al login.
 */
@Composable
fun AutenticacionNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                alCrearCuenta = {
                    // El error del login no tiene nada que ver con el
                    // registro, así que no debe viajar a la otra pantalla.
                    authViewModel.limpiarError()
                    navController.navigate(Rutas.REGISTRO)
                }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                authViewModel = authViewModel,
                alVolver = {
                    authViewModel.limpiarError()
                    navController.popBackStack()
                }
            )
        }
    }
}
