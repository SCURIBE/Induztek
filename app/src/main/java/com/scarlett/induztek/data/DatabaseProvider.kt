
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

    /*
     * Migración 4 → 5. Es la más grande de todas porque cierra el modelo
     * de datos de una sola vez:
     *
     *  - equipos.tipo pasa de texto libre a lista fija (TipoEquipo).
     *  - pruebas.codigoEquipo (texto) pasa a pruebas.equipoId con clave
     *    foránea y borrado en cascada.
     *  - pruebas.fechaHora pasa de texto "yyyy-MM-dd HH:mm" a número
     *    (milisegundos).
     *  - pruebas.tipoEnsayo y unidadMedida pasan a listas fijas.
     *  - se agregan los datos de la visita (cliente, técnico,
     *    observaciones).
     *  - la medición única de cada prueba se traslada a la nueva tabla
     *    mediciones, que permite varias por prueba.
     *
     * SQLite no sabe cambiar el tipo de una columna ni agregar una clave
     * foránea, así que las dos tablas se recrean y se copian los datos.
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {

            // 1. La medición de cada prueba se guarda aparte antes de
            // rehacer la tabla, porque la tabla mediciones todavía no
            // puede existir: su clave foránea apunta a la tabla pruebas
            // definitiva, que se crea más abajo.
            database.execSQL(
                """
                CREATE TABLE medicion_origen (
                    pruebaId INTEGER NOT NULL,
                    valor REAL NOT NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO medicion_origen (pruebaId, valor)
                SELECT id, medicion FROM pruebas
                """.trimIndent()
            )

            // 2. Equipos: el tipo escrito a mano se normaliza al nombre
            // de la constante del enum. LIKE en SQLite no distingue
            // mayúsculas, así que "Transformador" y "TRANSFORMADOR"
            // caen en el mismo caso.
            database.execSQL(
                """
                CREATE TABLE equipos_nueva (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    tipo TEXT NOT NULL,
                    codigo TEXT NOT NULL,
                    fotoUri TEXT
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO equipos_nueva (id, tipo, codigo, fotoUri)
                SELECT
                    id,
                    CASE
                        WHEN trim(tipo) LIKE '%transformador%'
                            THEN 'TRANSFORMADOR'
                        WHEN trim(tipo) LIKE '%rel%'
                            THEN 'RELE_PROTECCION'
                        WHEN trim(tipo) LIKE '%celda%'
                            THEN 'CELDA'
                        WHEN trim(tipo) LIKE '%cable%'
                            THEN 'CABLE'
                        ELSE 'OTRO'
                    END,
                    codigo,
                    fotoUri
                FROM equipos
                """.trimIndent()
            )

            database.execSQL("DROP TABLE equipos")
            database.execSQL(
                "ALTER TABLE equipos_nueva RENAME TO equipos"
            )

            // 3. Pruebas: tabla nueva con la clave foránea al equipo.
            // Las columnas de la visita no llevan DEFAULT en SQL porque
            // la entidad tampoco lo declara con @ColumnInfo; si no
            // coincidieran, Room rechazaría la migración al abrir.
            database.execSQL(
                """
                CREATE TABLE pruebas_nueva (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    equipoId INTEGER NOT NULL,
                    fechaHora INTEGER NOT NULL,
                    ubicacion TEXT NOT NULL,
                    tipoEnsayo TEXT NOT NULL,
                    unidadMedida TEXT NOT NULL,
                    cliente TEXT NOT NULL,
                    tecnicoResponsable TEXT NOT NULL,
                    observaciones TEXT NOT NULL,
                    FOREIGN KEY(equipoId) REFERENCES equipos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // El JOIN resuelve el código de texto al id del equipo. Las
            // pruebas cuyo código ya no corresponde a ningún equipo se
            // descartan: equipoId no admite nulos y no hay forma de
            // adivinar a qué equipo pertenecían.
            //
            // strftime con el modificador 'utc' interpreta el texto como
            // hora local del dispositivo y devuelve segundos desde la
            // época; por eso se multiplica por 1000.
            database.execSQL(
                """
                INSERT INTO pruebas_nueva (
                    id, equipoId, fechaHora, ubicacion,
                    tipoEnsayo, unidadMedida,
                    cliente, tecnicoResponsable, observaciones
                )
                SELECT
                    p.id,
                    e.id,
                    CAST(
                        COALESCE(strftime('%s', p.fechaHora, 'utc'), 0)
                        AS INTEGER
                    ) * 1000,
                    p.ubicacion,
                    CASE
                        WHEN trim(p.tipoEnsayo) LIKE '%aisl%'
                            THEN 'RESISTENCIA_AISLAMIENTO'
                        WHEN trim(p.tipoEnsayo) LIKE '%devanad%'
                            THEN 'RESISTENCIA_DEVANADOS'
                        WHEN trim(p.tipoEnsayo) LIKE '%transformaci%'
                            THEN 'RELACION_TRANSFORMACION'
                        WHEN trim(p.tipoEnsayo) LIKE '%tiempo%'
                            THEN 'TIEMPO_OPERACION_RELE'
                        ELSE 'OTRO'
                    END,
                    CASE
                        WHEN trim(p.unidadMedida) IN ('MΩ', 'mΩ')
                            OR trim(p.unidadMedida) LIKE 'mohm%'
                            OR trim(p.unidadMedida) LIKE 'megaohm%'
                            THEN 'MEGAOHM'
                        WHEN trim(p.unidadMedida) = 'Ω'
                            OR trim(p.unidadMedida) LIKE 'ohm%'
                            THEN 'OHM'
                        WHEN trim(p.unidadMedida) LIKE 'ms'
                            OR trim(p.unidadMedida) LIKE 'milisegundo%'
                            THEN 'MILISEGUNDO'
                        WHEN trim(p.unidadMedida) LIKE 'kv%'
                            THEN 'KILOVOLT'
                        WHEN trim(p.unidadMedida) LIKE 'v'
                            OR trim(p.unidadMedida) LIKE 'volt%'
                            THEN 'VOLT'
                        WHEN trim(p.unidadMedida) LIKE 'a'
                            OR trim(p.unidadMedida) LIKE 'amper%'
                            THEN 'AMPERE'
                        WHEN trim(p.unidadMedida) = '%'
                            OR trim(p.unidadMedida) LIKE 'porcentaje%'
                            THEN 'PORCENTAJE'
                        WHEN trim(p.unidadMedida) LIKE 'raz%'
                            THEN 'RAZON'
                        ELSE 'SIN_ESPECIFICAR'
                    END,
                    '',
                    '',
                    ''
                FROM pruebas p
                JOIN equipos e ON e.codigo = p.codigoEquipo
                """.trimIndent()
            )

            database.execSQL("DROP TABLE pruebas")
            database.execSQL(
                "ALTER TABLE pruebas_nueva RENAME TO pruebas"
            )

            // El nombre del índice tiene que ser el que genera Room a
            // partir de @Index("equipoId"), o la validación falla.
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_pruebas_equipoId
                ON pruebas (equipoId)
                """.trimIndent()
            )

            // 4. Mediciones: una fila por cada medición que antes era
            // una columna de la prueba.
            database.execSQL(
                """
                CREATE TABLE mediciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    pruebaId INTEGER NOT NULL,
                    nombre TEXT NOT NULL,
                    valor REAL NOT NULL,
                    minimoAceptable REAL,
                    maximoAceptable REAL,
                    FOREIGN KEY(pruebaId) REFERENCES pruebas(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_mediciones_pruebaId
                ON mediciones (pruebaId)
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO mediciones (pruebaId, nombre, valor)
                SELECT o.pruebaId, 'Medición', o.valor
                FROM medicion_origen o
                WHERE EXISTS (
                    SELECT 1 FROM pruebas p WHERE p.id = o.pruebaId
                )
                """.trimIndent()
            )

            database.execSQL("DROP TABLE medicion_origen")
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
                    MIGRATION_3_4,
                    MIGRATION_4_5
                )
                .build()
                .also { INSTANCE = it }
        }
    }
}
