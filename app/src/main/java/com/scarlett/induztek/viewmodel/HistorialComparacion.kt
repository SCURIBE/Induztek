
package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.PruebaConMediciones

// Una medición de la prueba actual junto con la del mismo punto en la
// prueba anterior comparable, si existe.
data class ComparacionMedicion(
    val medicion: Medicion,
    val anterior: Medicion?
) {
    val diferencia: Double?
        get() = anterior?.let { medicion.valor - it.valor }
}

/*
 * Búsqueda de la prueba anterior comparable y emparejamiento de sus
 * mediciones. Estaba dentro del LazyColumn del historial, donde no se
 * podía testear.
 *
 * Dos pruebas son comparables si son del mismo equipo, el mismo ensayo
 * y la misma unidad. Ahora que el ensayo y la unidad son enums, la
 * comparación es por identidad y no por texto: ya no hay forma de que
 * "Resistencia aislamiento" y "Resistencia de aislamiento" queden sin
 * compararse entre sí.
 */
object HistorialComparacion {

    fun anteriorComparable(
        prueba: PruebaConMediciones,
        todas: List<PruebaConMediciones>
    ): PruebaConMediciones? {
        return todas
            .filter {
                it.prueba.equipoId == prueba.prueba.equipoId &&
                        it.prueba.tipoEnsayo == prueba.prueba.tipoEnsayo &&
                        it.prueba.unidadMedida == prueba.prueba.unidadMedida &&
                        it.prueba.id != prueba.prueba.id &&
                        it.prueba.fechaHora < prueba.prueba.fechaHora
            }
            .maxByOrNull { it.prueba.fechaHora }
    }

    /*
     * Empareja cada medición de la prueba con la del mismo punto en la
     * prueba anterior. El emparejamiento es por nombre y no por
     * posición: si una visita midió solo la fase B, esa medición debe
     * compararse con la fase B anterior y no con la primera de la
     * lista.
     *
     * Devuelve una entrada por cada medición de la prueba actual, con
     * anterior en null cuando ese punto no se midió antes.
     */
    fun comparar(
        prueba: PruebaConMediciones,
        anterior: PruebaConMediciones?
    ): List<ComparacionMedicion> {
        val previas = anterior
            ?.mediciones
            ?.associateBy { it.nombre.trim().lowercase() }
            ?: emptyMap()

        return prueba.mediciones.map { medicion ->
            ComparacionMedicion(
                medicion = medicion,
                anterior = previas[medicion.nombre.trim().lowercase()]
            )
        }
    }
}
