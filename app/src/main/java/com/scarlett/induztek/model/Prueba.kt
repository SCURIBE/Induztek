package com.scarlett.induztek.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pruebas")

data class Prueba(

    @PrimaryKey

    val id: Int,
    val codigoEquipo: String,
    val fechaHora: String,
    val ubicacion: String,
    val medicion: Double
)