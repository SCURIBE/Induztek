
package com.scarlett.induztek.model

import androidx.room.Embedded
import androidx.room.Relation

// Resultado de consultar una prueba junto con sus mediciones.
// No es una tabla: Room arma el objeto a partir de las dos consultas
// cuando el método del DAO está marcado con @Transaction.
data class PruebaConMediciones(
    @Embedded
    val prueba: Prueba,

    @Relation(
        parentColumn = "id",
        entityColumn = "pruebaId"
    )
    val mediciones: List<Medicion>
)
