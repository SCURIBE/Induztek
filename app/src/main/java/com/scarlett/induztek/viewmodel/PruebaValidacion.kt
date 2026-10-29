
package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida

// Una medición tal como la escribió el usuario, todavía como texto.
// El rango es opcional: si queda vacío, la medición no se evalúa.
data class MedicionIngresada(
    val nombre: String,
    val valorTexto: String,
    val minimoTexto: String = "",
    val maximoTexto: String = ""
)

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

    fun validarNombreMedicion(nombre: String): String? {
        return if (nombre.isBlank()) {
            "Indica el punto de medición."
        } else {
            null
        }
    }

    /*
     * El rango es opcional, pero si se escribe tiene que ser numérico y
     * coherente. Un rango al revés dejaría toda medición rechazada.
     */
    fun validarRango(
        minimoTexto: String,
        maximoTexto: String
    ): String? {
        val minimo = if (minimoTexto.isBlank()) {
            null
        } else {
            medicionDesdeTexto(minimoTexto)
                ?: return "El mínimo debe ser un número."
        }

        val maximo = if (maximoTexto.isBlank()) {
            null
        } else {
            medicionDesdeTexto(maximoTexto)
                ?: return "El máximo debe ser un número."
        }

        return if (minimo != null && maximo != null && minimo > maximo) {
            "El mínimo no puede ser mayor que el máximo."
        } else {
            null
        }
    }

    /*
     * Valida la lista completa. Los nombres no se pueden repetir porque
     * la comparación del historial empareja las mediciones de dos
     * pruebas por su nombre: con dos "Fase A" no se sabría cuál es cuál.
     */
    fun validarMediciones(
        mediciones: List<MedicionIngresada>
    ): String? {
        if (mediciones.isEmpty()) {
            return "Agrega al menos una medición."
        }

        mediciones.forEach { ingresada ->
            val error = validarNombreMedicion(ingresada.nombre)
                ?: validarMedicion(ingresada.valorTexto)
                ?: validarRango(
                    ingresada.minimoTexto,
                    ingresada.maximoTexto
                )

            if (error != null) {
                return error
            }
        }

        val nombres = mediciones.map { it.nombre.trim().lowercase() }

        return if (nombres.size != nombres.toSet().size) {
            "Hay dos mediciones con el mismo punto."
        } else {
            null
        }
    }

    // Convierte las filas escritas en entidades. Supone que ya pasaron
    // por validarMediciones; el pruebaId lo asigna el DAO al guardar.
    fun aMediciones(
        mediciones: List<MedicionIngresada>
    ): List<Medicion> {
        return mediciones.mapNotNull { ingresada ->
            val valor = medicionDesdeTexto(ingresada.valorTexto)
                ?: return@mapNotNull null

            Medicion(
                pruebaId = 0,
                nombre = ingresada.nombre.trim(),
                valor = valor,
                minimoAceptable =
                    medicionDesdeTexto(ingresada.minimoTexto),
                maximoAceptable =
                    medicionDesdeTexto(ingresada.maximoTexto)
            )
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
