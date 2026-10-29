package com.scarlett.induztek.viewmodel

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarlett.induztek.data.AlmacenSesion
import com.scarlett.induztek.data.UsuarioRepository
import com.scarlett.induztek.model.Usuario
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Los tres estados posibles de la sesión. Cargando existe porque leer
// DataStore toma un instante: sin él la app mostraría el login por un
// momento aunque la sesión estuviera abierta.
sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Autenticado(val usuario: Usuario) : EstadoSesion
}

class AuthViewModel(
    private val repository: UsuarioRepository,
    private val sesionStore: AlmacenSesion,
    // Dónde corre PBKDF2. Es un parámetro para que los tests lo corran
    // en su propio despachador y no tengan que esperar otro hilo.
    private val despachadorCalculo: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Deshabilita los botones mientras PBKDF2 trabaja, que tarda lo
    // suficiente para que alguien toque dos veces.
    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(despachadorCalculo) {
                repository.sembrarSiEstaVacia()
            }

            sesionStore.usuarioId.collect { id ->
                _estado.value = if (id == null) {
                    EstadoSesion.SinSesion
                } else {
                    // Si el usuario ya no existe, la sesión guardada no
                    // sirve: hay que limpiarla, no dejarla colgada.
                    repository.buscarPorId(id)
                        ?.let { EstadoSesion.Autenticado(it) }
                        ?: EstadoSesion.SinSesion.also { sesionStore.cerrar() }
                }
            }
        }
    }

    fun iniciarSesion(nombreUsuario: String, contrasena: String) {
        viewModelScope.launch {
            // Las mismas funciones que usan las pantallas para avisar
            // antes de tocar el botón, en vez de repetir las reglas.
            val aviso =
                CredencialValidacion.validarNombreUsuario(nombreUsuario)
                    ?: CredencialValidacion.validarContrasenaLogin(contrasena)

            if (aviso != null) {
                _error.value = aviso
                return@launch
            }

            _error.value = null
            _ocupado.value = true

            // PBKDF2 tarda cientos de milisegundos: fuera del hilo
            // principal o la pantalla se congela al tocar Entrar.
            val usuario = withContext(despachadorCalculo) {
                repository.autenticar(nombreUsuario, contrasena)
            }

            _ocupado.value = false

            if (usuario == null) {
                // Mensaje genérico a propósito: "ese usuario no existe"
                // le confirma a quien prueba cuáles son válidos.
                _error.value = "Usuario o contraseña incorrectos."
                return@launch
            }

            // No hace falta navegar: al guardar la sesión, el Flow de
            // usuarioId emite y el estado pasa a Autenticado.
            sesionStore.guardar(usuario.id)
        }
    }

    fun registrar(
        nombreUsuario: String,
        nombreCompleto: String,
        contrasena: String,
        confirmacion: String
    ) {
        viewModelScope.launch {
            val aviso =
                CredencialValidacion.validarNombreCompleto(nombreCompleto)
                    ?: CredencialValidacion.validarNombreUsuario(nombreUsuario)
                    ?: CredencialValidacion.validarContrasenaNueva(contrasena)
                    ?: CredencialValidacion.validarConfirmacion(
                        contrasena,
                        confirmacion
                    )

            if (aviso != null) {
                _error.value = aviso
                return@launch
            }

            _error.value = null
            _ocupado.value = true

            if (repository.existeNombre(nombreUsuario)) {
                _ocupado.value = false
                _error.value = "Ese usuario ya está registrado."
                return@launch
            }

            val usuario = try {
                withContext(despachadorCalculo) {
                    repository.registrar(
                        nombreUsuario = nombreUsuario,
                        nombreCompleto = nombreCompleto,
                        contrasena = contrasena
                    )
                }
            } catch (_: SQLiteConstraintException) {
                // El índice único de la tabla es la red de seguridad
                // real: entre la consulta de arriba y este insert cabe
                // otro registro con el mismo nombre.
                _ocupado.value = false
                _error.value = "Ese usuario ya está registrado."
                return@launch
            }

            _ocupado.value = false
            sesionStore.guardar(usuario.id)
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            _error.value = null
            sesionStore.cerrar()
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}
