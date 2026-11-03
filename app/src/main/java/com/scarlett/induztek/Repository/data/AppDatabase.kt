package com.scarlett.induztek.Repository.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.scarlett.induztek.Repository.model.Equipo
import com.scarlett.induztek.Repository.model.Medicion
import com.scarlett.induztek.Repository.model.Prueba
import com.scarlett.induztek.Repository.model.TipoUsuario
import com.scarlett.induztek.Repository.model.Usuario
import com.scarlett.induztek.util.Hash
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Equipo::class,
        Prueba::class,
        Medicion::class,
        TipoUsuario::class,
        Usuario::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun equipoDao(): EquipoDao

    abstract fun pruebaDao(): PruebaDao

    abstract fun tipoUsuarioDao(): TipoUsuarioDao

    abstract fun usuarioDao(): UsuarioDao

    companion object {
        const val ADMIN_CORREO = "admin@induztek.com"
        const val ADMIN_CONTRASENA = "Induztek2026"

        internal const val SQL_PRECARGAR_TIPOS =
            "INSERT OR IGNORE INTO tipo_usuarios(id, tipo_usuario) VALUES " +
                "(1, 'Administrador'), (2, 'Técnico'), (3, 'Supervisor')"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "induztek_db"
                )
                    .addMigrations(*DatabaseProvider.MIGRACIONES)
                    .addCallback(seedCallback)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        internal val seedCallback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(SQL_PRECARGAR_TIPOS)

                val adminExiste = db.query(
                    "SELECT 1 FROM usuarios WHERE nombreUsuario = ?",
                    arrayOf(ADMIN_CORREO)
                ).use { it.moveToFirst() }

                if (!adminExiste) {
                    db.execSQL(
                        "INSERT INTO usuarios(nombreUsuario, nombreCompleto, " +
                            "credencial, creadoEn, tipoUsuarioId) " +
                            "VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>(
                            ADMIN_CORREO,
                            "Administrador",
                            Hash.generar(ADMIN_CONTRASENA),
                            System.currentTimeMillis(),
                            TipoUsuario.ID_ADMINISTRADOR
                        )
                    )
                }
            }
        }
    }
}
