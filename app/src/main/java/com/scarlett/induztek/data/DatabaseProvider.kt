
package com.scarlett.induztek.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    @Volatile
    private var INSTANCE: AppDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE equipos ADD COLUMN fotoUri TEXT"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {

            // Crear la nueva tabla con identificador autogenerado.
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pruebas_nueva (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    codigoEquipo TEXT NOT NULL,
                    fechaHora TEXT NOT NULL,
                    ubicacion TEXT NOT NULL,
                    medicion REAL NOT NULL
                )
                """.trimIndent()
            )

            // Copiar los registros existentes conservando sus identificadores.
            database.execSQL(
                """
                INSERT INTO pruebas_nueva (
                    id, codigoEquipo, fechaHora, ubicacion, medicion
                )
                SELECT
                    id, codigoEquipo, fechaHora, ubicacion, medicion
                FROM pruebas
                """.trimIndent()
            )

            database.execSQL("DROP TABLE pruebas")
            database.execSQL(
                "ALTER TABLE pruebas_nueva RENAME TO pruebas"
            )
        }
    }

    fun obtenerBaseDatos(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "induztek_db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
        }
    }
}
