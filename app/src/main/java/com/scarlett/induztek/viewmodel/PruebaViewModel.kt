package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarlett.induztek.Repository.data.PruebaRepository
import com.scarlett.induztek.Repository.model.Prueba
import com.scarlett.induztek.Repository.model.PruebaConMediciones
import com.scarlett.induztek.Repository.model.TipoEnsayo
import com.scarlett.induztek.Repository.model.UnidadMedida
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

    // REGISTRAR UNA PRUEBA
    fun registrar(
        equipoId: Int?,
        ubicacion: String,
        tipoEnsayo: TipoEnsayo?,
        unidadMedida: UnidadMedida?,
        mediciones: List<MedicionIngresada>,
        cliente: String = "",
        tecnicoResponsable: String = "",
        observaciones: String = "",
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
                    ?: PruebaValidacion.validarMediciones(mediciones)

            if (errorValidacion != null) {
                _error.value = errorValidacion
                _guardado.value = false
                alResultado(false)
                return@launch
            }

            if (
                equipoId == null ||
                tipoEnsayo == null ||
                unidadMedida == null
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
                        unidadMedida = unidadMedida,
                        cliente = cliente.trim(),
                        tecnicoResponsable = tecnicoResponsable.trim(),
                        observaciones = observaciones.trim()
                    ),
                    mediciones = PruebaValidacion.aMediciones(mediciones)
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

    // NUEVO: EDITAR UNA PRUEBA EXISTENTE
    fun editar(
        prueba: Prueba,
        equipoId: Int?,
        ubicacion: String,
        tipoEnsayo: TipoEnsayo?,
        unidadMedida: UnidadMedida?,
        mediciones: List<MedicionIngresada>,
        cliente: String = "",
        tecnicoResponsable: String = "",
        observaciones: String = "",
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
                    ?: PruebaValidacion.validarMediciones(mediciones)

            if (errorValidacion != null) {
                _error.value = errorValidacion
                _guardado.value = false
                alResultado(false)
                return@launch
            }

            if (
                equipoId == null ||
                tipoEnsayo == null ||
                unidadMedida == null
            ) {
                _error.value = "Revisa los datos de la prueba."
                _guardado.value = false
                alResultado(false)
                return@launch
            }

            try {
                repository.actualizar(
                    prueba = prueba.copy(
                        equipoId = equipoId,
                        ubicacion = ubicacion.trim(),
                        tipoEnsayo = tipoEnsayo,
                        unidadMedida = unidadMedida,
                        cliente = cliente.trim(),
                        tecnicoResponsable = tecnicoResponsable.trim(),
                        observaciones = observaciones.trim()
                    ),
                    mediciones = PruebaValidacion.aMediciones(mediciones)
                )

                _error.value = null
                _guardado.value = true
                alResultado(true)

            } catch (e: Exception) {
                _error.value = "No se pudo actualizar la prueba."
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
