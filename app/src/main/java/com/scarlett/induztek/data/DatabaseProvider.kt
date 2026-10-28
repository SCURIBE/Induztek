
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

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE pruebas
                ADD COLUMN tipoEnsayo TEXT NOT NULL DEFAULT 'General'
                """.trimIndent()
            )

            database.execSQL(
                """
                ALTER TABLE pruebas
                ADD COLUMN unidadMedida TEXT NOT NULL DEFAULT 'Sin especificar'
                """.trimIndent()
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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4
                )
                .build()
                .also { INSTANCE = it }
        }
    }
}
