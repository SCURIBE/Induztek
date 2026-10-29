
package com.scarlett.induztek.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Una medición concreta dentro de una prueba: por ejemplo la fase A de
// un ensayo de aislamiento. Una prueba puede tener varias.
@Entity(
    tableName = "mediciones",
    foreignKeys = [
        ForeignKey(
            entity = Prueba::class,
            parentColumns = ["id"],
            childColumns = ["pruebaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pruebaId")]
)
data class Medicion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pruebaId: Int,
    // "Fase A", "Fase B", "Medición"... según el ensayo.
    val nombre: String,
    val valor: Double,
    val minimoAceptable: Double? = null,
    val maximoAceptable: Double? = null
) {
    // Room ignora esta propiedad porque no tiene campo propio: es un
    // getter calculado. El resultado se deduce del rango en vez de
    // guardarse, así no puede contradecir al valor.
    val resultado: ResultadoMedicion
        get() = when {
            minimoAceptable == null && maximoAceptable == null ->
                ResultadoMedicion.NO_EVALUADO

            minimoAceptable != null && valor < minimoAceptable ->
                ResultadoMedicion.RECHAZADO

            maximoAceptable != null && valor > maximoAceptable ->
                ResultadoMedicion.RECHAZADO

            else -> ResultadoMedicion.APROBADO
        }
}
