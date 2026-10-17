
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

    init {
        viewModelScope.launch {
            repository.obtenerTodos().collect { lista ->
                _equipos.value = lista
            }
        }
    }

    fun insertar(equipo: Equipo) {
        viewModelScope.launch {
            repository.insertar(equipo)
        }
    }
}
