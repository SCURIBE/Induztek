
package com.scarlett.induztek.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.Prueba

@Database(
    entities = [Equipo::class, Prueba::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun equipoDao(): EquipoDao

    abstract fun pruebaDao(): PruebaDao
}
