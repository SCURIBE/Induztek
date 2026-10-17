package com.scarlett.induztek.model

import androidx.room.Entity
import androidx.room.PrimaryKey


// se le agrega anotaciones room que sirven para poder tratarlas como tablas
@Entity(tableName = "equipos") //<- esto de aqui, anotacion pa tabla
data class Equipo(

    //la llave pa representar de forma unica cada registro
    @PrimaryKey

    val id: Int,
    val tipo: String,
    val codigo: String
)