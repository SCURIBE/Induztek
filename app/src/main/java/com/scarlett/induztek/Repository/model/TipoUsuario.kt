package com.scarlett.induztek.Repository.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipo_usuarios")
data class TipoUsuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "tipo_usuario")
    val tipoUsuario: String
) {
    companion object {
        const val ID_ADMINISTRADOR = 1
        const val ID_TECNICO = 2
        const val ID_SUPERVISOR = 3
    }
}
