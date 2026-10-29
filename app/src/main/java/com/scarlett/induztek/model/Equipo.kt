
package com.scarlett.induztek.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipos")
data class Equipo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tipo: TipoEquipo,
    val codigo: String,
    val fotoUri: String? = null
)
