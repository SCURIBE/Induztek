
package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarlett.induztek.data.PruebaRepository
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.PruebaConMediciones
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PruebaViewModel(
    private val repository: PruebaRepository
) : ViewModel() {

    private val _pruebas =
        MutableStateFlow<List<PruebaConMediciones>>(emptyList())
    val pruebas: StateFlow<List<PruebaConMediciones>> =
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

    /*
     * Registra una prueba con una medición. La pantalla entrega lo que
     * el usuario eligió y escribió; el ViewModel valida, arma la
     * entidad y guarda.
     *
     * La fecha se toma aquí, al guardar, y no al abrir la pantalla:
     * antes la pantalla la calculaba una sola vez con remember, así que
     * varias pruebas seguidas quedaban con la misma hora.
     */
    fun registrar(
        equipoId: Int?,
        ubicacion: String,
        tipoEnsayo: TipoEnsayo?,
        unidadMedida: UnidadMedida?,
        medicionTexto: String,
        alResultado: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val errorValidacion =
                PruebaValidacion.validarEquipo(equipoId)
                    ?: PruebaValidacion.validarUbicacion(ubicacion)
                    ?: PruebaValidacion.validarTipoEnsayo(tipoEnsayo)
                    ?: PruebaValidacion.validarUnidad(
                        tipoEnsayo,
                        unidadMedida
                    )
                    ?: PruebaValidacion.validarMedicion(medicionTexto)

            if (errorValidacion != null) {
                _error.value = errorValidacion
                _guardado.value = false
                alResultado(false)
                return@launch
            }

            // Las validaciones de arriba ya garantizan que ninguno es
            // null, pero el compilador no lo sabe: esto convierte los
            // tipos nullables en no nullables.
            val valor = PruebaValidacion.medicionDesdeTexto(medicionTexto)

            if (
                equipoId == null ||
                tipoEnsayo == null ||
                unidadMedida == null ||
                valor == null
            ) {
                _error.value = "Revisa los datos de la prueba."
                _guardado.value = false
                alResultado(false)
                return@launch
            }

            try {
                repository.registrar(
                    prueba = Prueba(
                        equipoId = equipoId,
                        fechaHora = System.currentTimeMillis(),
                        ubicacion = ubicacion.trim(),
                        tipoEnsayo = tipoEnsayo,
                        unidadMedida = unidadMedida
                    ),
                    mediciones = listOf(
                        Medicion(
                            pruebaId = 0,
                            nombre = "Medición",
                            valor = valor
                        )
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
