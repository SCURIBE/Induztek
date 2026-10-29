
package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida

// Validaciones de una prueba como funciones puras: no dependen de
// Android ni de Room, así que se pueden testear con JUnit normal.
// Devuelven el mensaje de error, o null si el dato es válido.
object PruebaValidacion {

    fun validarEquipo(equipoId: Int?): String? {
        return if (equipoId == null) {
            "Selecciona un equipo."
        } else {
            null
        }
    }

    fun validarUbicacion(ubicacion: String): String? {
        return if (ubicacion.isBlank()) {
            "Ingresa la ubicación del cliente."
        } else {
            null
        }
    }

    fun validarTipoEnsayo(tipoEnsayo: TipoEnsayo?): String? {
        return if (tipoEnsayo == null) {
            "Selecciona el tipo de ensayo."
        } else {
            null
        }
    }

    fun validarUnidad(
        tipoEnsayo: TipoEnsayo?,
        unidadMedida: UnidadMedida?
    ): String? {
        return when {
            unidadMedida == null ->
                "Selecciona la unidad de medida."

            tipoEnsayo != null &&
                    unidadMedida !in tipoEnsayo.unidades ->
                "Esa unidad no corresponde al tipo de ensayo."

            else -> null
        }
    }

    fun validarMedicion(medicionTexto: String): String? {
        if (medicionTexto.isBlank()) {
            return "Ingresa el resultado de la medición."
        }

        val medicion = medicionDesdeTexto(medicionTexto)

        return when {
            medicion == null ->
                "Ingresa un número válido."

            !medicion.isFinite() ->
                "La medición debe ser un número válido."

            else -> null
        }
    }

    // Acepta coma o punto como separador decimal, porque en terreno se
    // escribe de las dos formas. Devuelve null si el texto no es un
    // número.
    fun medicionDesdeTexto(medicionTexto: String): Double? {
        return medicionTexto
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
    }
}
