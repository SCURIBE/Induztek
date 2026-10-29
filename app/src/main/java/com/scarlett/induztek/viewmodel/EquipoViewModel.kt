
package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarlett.induztek.data.EquipoRepository
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.util.AlmacenFotos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EquipoViewModel(
    private val repository: EquipoRepository,
    // Borrar la foto se recibe como parámetro para que los tests puedan
    // comprobar qué archivo se borra sin tocar archivos de verdad.
    private val borrarFoto: (String?) -> Unit = { AlmacenFotos.borrar(it) }
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

            // El tipo ya no se valida: es un enum elegido de una lista,
            // así que no puede venir vacío ni mal escrito.
            if (codigo.isBlank()) {
                _error.value = "Ingresa el código del equipo."
                alResultado(false)
                return@launch
            }

            if (repository.existeCodigo(codigo)) {
                _error.value = "Ya existe un equipo con ese código."
                alResultado(false)
                return@launch
            }

            repository.insertar(equipo.copy(codigo = codigo))

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

            if (codigo.isBlank()) {
                _error.value = "Ingresa el código del equipo."
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

            val anterior = repository.buscarPorId(equipo.id)

            repository.actualizar(equipo.copy(codigo = codigo))

            // La foto reemplazada se borra solo después de guardar: si el
            // update fallara, el equipo seguiría apuntando a ella.
            if (anterior != null && anterior.fotoUri != equipo.fotoUri) {
                borrarFoto(anterior.fotoUri)
            }

            _error.value = null
            alResultado(true)
        }
    }

    fun eliminar(id: Int) {
        viewModelScope.launch {
            // Se lee antes de borrar, porque después ya no se sabe cuál
            // era su foto. La cascada de la base borra sus pruebas, pero
            // el archivo de la foto no está en la base: hay que borrarlo
            // aparte.
            val equipo = repository.buscarPorId(id)

            repository.eliminarPorId(id)
            borrarFoto(equipo?.fotoUri)
        }
    }

    fun limpiarError() {
        _error.value = null
    }
}
