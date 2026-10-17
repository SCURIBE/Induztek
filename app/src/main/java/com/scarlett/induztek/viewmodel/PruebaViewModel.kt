
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

    private val _pruebas = MutableStateFlow<List<Prueba>>(emptyList())
    val pruebas: StateFlow<List<Prueba>> = _pruebas.asStateFlow()

    init {
        viewModelScope.launch {
            repository.obtenerTodas().collect { lista ->
                _pruebas.value = lista
            }
        }
    }

    fun insertar(prueba: Prueba) {
        viewModelScope.launch {
            repository.insertar(prueba)
        }
    }
}
