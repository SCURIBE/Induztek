
package com.scarlett.induztek.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pruebas")
data class Prueba(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val codigoEquipo: String,
    val fechaHora: String,
    val ubicacion: String,
    val medicion: Double,
    val tipoEnsayo: String = "General",
    val unidadMedida: String = "Sin especificar"
)
