
package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.model.PruebaConMediciones

// Búsqueda de la prueba anterior comparable. Estaba dentro del
// LazyColumn del historial, donde no se podía testear.
//
// Dos pruebas son comparables si son del mismo equipo, el mismo ensayo
// y la misma unidad. Ahora que el ensayo y la unidad son enums, la
// comparación es por identidad y no por texto: ya no hay forma de que
// "Resistencia aislamiento" y "Resistencia de aislamiento" queden sin
// compararse entre sí.
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

    // Diferencia entre el valor principal de una prueba y el de la
    // anterior comparable. null si falta alguno de los dos.
    fun diferencia(
        prueba: PruebaConMediciones,
        anterior: PruebaConMediciones?
    ): Double? {
        val actual = prueba.valorPrincipal ?: return null
        val previo = anterior?.valorPrincipal ?: return null
        return actual - previo
    }
}
