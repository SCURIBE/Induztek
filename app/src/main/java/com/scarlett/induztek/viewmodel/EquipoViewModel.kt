
package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarlett.induztek.data.EquipoRepository
import com.scarlett.induztek.model.Equipo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EquipoViewModel(
    private val repository: EquipoRepository
) : ViewModel() {

    private val _equipos = MutableStateFlow<List<Equipo>>(emptyList())
    val equipos: StateFlow<List<Equipo>> = _equipos.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerTodos().collect { lista ->
                _equipos.value = lista
            }
        }
    }

    fun insertar(
        equipo: Equipo,
        alResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val codigo = equipo.codigo.trim()
            val tipo = equipo.tipo.trim()

            if (codigo.isBlank() || tipo.isBlank()) {
                _error.value = "Completa todos los campos."
                alResultado(false)
                return@launch
            }

            if (repository.existeCodigo(codigo)) {
                _error.value = "Ya existe un equipo con ese código."
                alResultado(false)
                return@launch
            }

            repository.insertar(
                equipo.copy(
                    codigo = codigo,
                    tipo = tipo
                )
            )

            _error.value = null
            alResultado(true)
        }
    }

    fun actualizar(
        equipo: Equipo,
        alResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val codigo = equipo.codigo.trim()
            val tipo = equipo.tipo.trim()

            if (codigo.isBlank() || tipo.isBlank()) {
                _error.value = "Completa todos los campos."
                alResultado(false)
                return@launch
            }

            if (
                repository.existeCodigoEnOtroEquipo(
                    codigo = codigo,
                    id = equipo.id
                )
            ) {
                _error.value = "Ese código ya pertenece a otro equipo."
                alResultado(false)
                return@launch
            }

            repository.actualizar(
                equipo.copy(
                    codigo = codigo,
                    tipo = tipo
                )
            )

            _error.value = null
            alResultado(true)
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            repository.eliminarPorId(id)
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}
