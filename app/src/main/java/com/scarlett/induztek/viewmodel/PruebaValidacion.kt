
package com.scarlett.induztek.viewmodel

object PruebaValidacion {

    fun validarEquipo(codigoEquipo: String): String? {
        return if (codigoEquipo.isBlank()) {
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

    fun validarMedicion(medicionTexto: String): String? {
        if (medicionTexto.isBlank()) {
            return "Ingresa el resultado de la medición."
        }

        val medicion = medicionTexto
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        return when {
            medicion == null ->
                "Ingresa un número válido."

            !medicion.isFinite() ->
                "La medición debe ser un número válido."

            else -> null
        }
    }

    fun validarFechaHora(fechaHora: String): String? {
        return if (fechaHora.isBlank()) {
            "La fecha y hora son obligatorias."
        } else {
            null
        }
    }
}
