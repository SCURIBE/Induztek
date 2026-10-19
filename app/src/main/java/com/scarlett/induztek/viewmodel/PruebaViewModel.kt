
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

            if (codigo.isBlank()) {
                _error.value = "Selecciona un equipo."
                alResultado(false)
                return@launch
            }

            if (ubicacion.isBlank()) {
                _error.value = "Ingresa la ubicación del cliente."
                alResultado(false)
                return@launch
            }

            if (prueba.fechaHora.isBlank()) {
                _error.value = "Ingresa la fecha y hora."
                alResultado(false)
                return@launch
            }

            if (!prueba.medicion.isFinite()) {
                _error.value = "Ingresa una medición válida."
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
