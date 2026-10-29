
package com.scarlett.induztek.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Una prueba es una visita a terreno: un ensayo aplicado a un equipo
// en una fecha, con una o varias mediciones (ver Medicion).
//
// El vínculo con el equipo es por id y no por el texto del código:
// antes, al editar el código de un equipo, sus pruebas quedaban
// huérfanas y desaparecían del historial.
@Entity(
    tableName = "pruebas",
    foreignKeys = [
        ForeignKey(
            entity = Equipo::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            // Al eliminar un equipo se borran también sus pruebas.
            // La pantalla de detalle avisa cuántas son antes de borrar.
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("equipoId")]
)
data class Prueba(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val equipoId: Int,
    // Milisegundos desde la época. Se guarda como número para poder
    // ordenar y comparar; se formatea solo al mostrarlo.
    val fechaHora: Long,
    val ubicacion: String,
    val tipoEnsayo: TipoEnsayo,
    // La unidad vive en la prueba y no en cada medición porque todas
    // las mediciones de un mismo ensayo comparten unidad (las fases A,
    // B y C de un aislamiento se miden todas en MΩ).
    val unidadMedida: UnidadMedida,
    // Datos de la visita. Los llena la pantalla del paso 4.
    val cliente: String = "",
    val tecnicoResponsable: String = "",
    val observaciones: String = ""
)
