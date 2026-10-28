
package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarlett.induztek.data.PruebaRepository
import com.scarlett.induztek.model.Prueba
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PruebaViewModel(
    private val repository: PruebaRepository
) : ViewModel() {

    private val _pruebas =
        MutableStateFlow<List<Prueba>>(emptyList())
    val pruebas: StateFlow<List<Prueba>> =
        _pruebas.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerTodas().collect { lista ->
                _pruebas.value = lista
            }
        }
    }

    fun insertar(
        prueba: Prueba,
        alResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val codigo = prueba.codigoEquipo.trim()
            val ubicacion = prueba.ubicacion.trim()

            val errorValidacion =
                PruebaValidacion.validarEquipo(codigo)
                    ?: PruebaValidacion.validarUbicacion(ubicacion)
                    ?: PruebaValidacion.validarFechaHora(prueba.fechaHora)
                    ?: if (!prueba.medicion.isFinite()) {
                        "La medición debe ser un número válido."
                    } else {
                        null
                    }

            if (errorValidacion != null) {
                _error.value = errorValidacion
                _guardado.value = false
                alResultado(false)
                return@launch
            }

            try {
                repository.insertar(
                    prueba.copy(
                        id = 0,
                        codigoEquipo = codigo,
                        ubicacion = ubicacion
                    )
                )

                _error.value = null
                _guardado.value = true
                alResultado(true)

            } catch (e: Exception) {
                _error.value = "No se pudo guardar la prueba."
                _guardado.value = false
                alResultado(false)
            }
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    fun limpiarGuardado() {
        _guardado.value = false
    }
}
