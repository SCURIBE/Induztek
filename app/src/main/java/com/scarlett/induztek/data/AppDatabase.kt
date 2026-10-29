
package com.scarlett.induztek.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.Usuario

@Database(
    entities = [Equipo::class, Prueba::class, Medicion::class, Usuario::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun equipoDao(): EquipoDao

    abstract fun pruebaDao(): PruebaDao

    abstract fun usuarioDao(): UsuarioDao
}
