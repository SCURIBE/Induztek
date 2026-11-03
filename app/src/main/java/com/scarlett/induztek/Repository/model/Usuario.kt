package com.scarlett.induztek.Repository.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    foreignKeys = [
        ForeignKey(
            entity = TipoUsuario::class,
            parentColumns = ["id"],
            childColumns = ["tipoUsuarioId"]
        )
    ],
    indices = [
        Index(value = ["nombreUsuario"], unique = true),
        Index(value = ["tipoUsuarioId"])
    ]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombreUsuario: String,
    val nombreCompleto: String,
    val credencial: String,
    val creadoEn: Long,
    val tipoUsuarioId: Int
)
