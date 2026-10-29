package com.scarlett.induztek.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.TipoEquipo
import com.scarlett.induztek.model.UnidadMedida
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.text.SimpleDateFormat
import java.util.Locale

/*
 * Abre una base en la versión 4, con datos escritos a mano como los que
 * había en el teléfono, usando exactamente las migraciones de la app.
 *
 * Es lo que la verificación anterior no cubría: aquella ejecutaba el SQL
 * en Python, pero no pasaba por Room. Al abrir, Room compara el esquema
 * que dejó la migración (tablas, columnas, claves foráneas, índices) con
 * el que esperan las entidades, y si no coincide lanza
 * IllegalStateException. Si este test pasa, la app no se cae al abrir
 * una base vieja.
 */
@RunWith(RobolectricTestRunner::class)
class MigracionesTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val nombre = "migracion_test.db"

    @After
    fun borrar() {
        context.deleteDatabase(nombre)
    }

    @Test
    fun `una base version 4 llega a la 6 y Room acepta el esquema`() {
        crearBaseVersion4()

        val db = abrirConRoom()

        try {
            // La primera consulta abre la base: migra y valida.
            runBlocking { db.equipoDao().obtenerTodos().first() }

            assertEquals(6, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun `los datos escritos a mano quedan normalizados`() {
        crearBaseVersion4()
        val db = abrirConRoom()

        try {
            val equipos = runBlocking { db.equipoDao().obtenerTodos().first() }
                .associateBy { it.codigo }

            assertEquals(TipoEquipo.TRANSFORMADOR, equipos["EQ-001"]?.tipo)
            assertEquals(TipoEquipo.RELE_PROTECCION, equipos["EQ-002"]?.tipo)
            assertEquals(TipoEquipo.CELDA, equipos["EQ-003"]?.tipo)
            assertEquals(TipoEquipo.OTRO, equipos["EQ-004"]?.tipo)

            val pruebas = runBlocking {
                db.pruebaDao().obtenerTodasConMediciones().first()
            }.associateBy { it.prueba.id }

            // La prueba 6 era de un equipo que ya no existe: se descarta.
            assertEquals(setOf(1, 2, 3, 4, 5), pruebas.keys)

            // Las dos formas de escribir "aislamiento" y "megaohm" quedan
            // iguales: ahora esas dos pruebas sí se comparan.
            val p1 = pruebas.getValue(1).prueba
            val p2 = pruebas.getValue(2).prueba
            assertEquals(TipoEnsayo.RESISTENCIA_AISLAMIENTO, p1.tipoEnsayo)
            assertEquals(p1.tipoEnsayo, p2.tipoEnsayo)
            assertEquals(UnidadMedida.MEGAOHM, p1.unidadMedida)
            assertEquals(p1.unidadMedida, p2.unidadMedida)
            assertEquals(p1.equipoId, equipos.getValue("EQ-001").id)

            val esperada = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                .parse("2026-10-01 09:30")!!.time
            assertEquals(esperada, p1.fechaHora)

            // La medición única de cada prueba pasó a la tabla nueva.
            val mediciones = pruebas.getValue(1).mediciones
            assertEquals(1, mediciones.size)
            assertEquals("Medición", mediciones[0].nombre)
            assertEquals(1200.0, mediciones[0].valor, 0.0)
        } finally {
            db.close()
        }
    }

    @Test
    fun `despues de migrar borrar un equipo borra en cascada`() {
        crearBaseVersion4()
        val db = abrirConRoom()

        try {
            runBlocking {
                val eq1 = db.equipoDao().obtenerTodos().first()
                    .first { it.codigo == "EQ-001" }

                db.equipoDao().eliminarPorId(eq1.id)

                val pruebas = db.pruebaDao().obtenerTodasConMediciones().first()
                assertEquals(3, pruebas.size)
                assertTrue(pruebas.none { it.prueba.equipoId == eq1.id })
            }

            val mediciones = db.openHelper.readableDatabase
                .query("SELECT COUNT(*) FROM mediciones")
                .use { it.moveToFirst(); it.getInt(0) }
            assertEquals(3, mediciones)
        } finally {
            db.close()
        }
    }

    @Test
    fun `la tabla de usuarios queda creada y vacia`() {
        crearBaseVersion4()
        val db = abrirConRoom()

        try {
            assertEquals(0, runBlocking { db.usuarioDao().contar() })
        } finally {
            db.close()
        }
    }

    private fun abrirConRoom(): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, nombre)
            .addMigrations(*DatabaseProvider.MIGRACIONES)
            .build()
    }

    /*
     * El esquema de la versión 4, tal como lo dejó MIGRATION_3_4, con
     * los mismos datos sucios de la verificación del paso 2: tipos y
     * unidades escritos a mano, la fecha como texto y una prueba de un
     * equipo que ya no existe.
     */
    private fun crearBaseVersion4() {
        val archivo = context.getDatabasePath(nombre)
        archivo.parentFile?.mkdirs()

        val db = SQLiteDatabase.openOrCreateDatabase(archivo, null)

        db.execSQL(
            """
            CREATE TABLE equipos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tipo TEXT NOT NULL,
                codigo TEXT NOT NULL,
                fotoUri TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE pruebas (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                codigoEquipo TEXT NOT NULL,
                fechaHora TEXT NOT NULL,
                ubicacion TEXT NOT NULL,
                medicion REAL NOT NULL,
                tipoEnsayo TEXT NOT NULL DEFAULT 'General',
                unidadMedida TEXT NOT NULL DEFAULT 'Sin especificar'
            )
            """.trimIndent()
        )

        listOf(
            "(1, 'Transformador', 'EQ-001', NULL)",
            "(2, '  relé de proteccion ', 'EQ-002', '/data/foto.jpg')",
            "(3, 'CELDA', 'EQ-003', NULL)",
            "(4, 'chatarra varia', 'EQ-004', NULL)"
        ).forEach { fila ->
            db.execSQL("INSERT INTO equipos VALUES $fila")
        }

        listOf(
            "(1, 'EQ-001', '2026-10-01 09:30', 'Planta A', 1200.0, " +
                    "'Resistencia aislamiento', 'MΩ')",
            "(2, 'EQ-001', '2026-10-15 14:05', 'Planta A', 1150.5, " +
                    "'Resistencia de aislamiento', 'Mohm')",
            "(3, 'EQ-002', '2026-10-20 08:00', 'Subestación', 42.0, " +
                    "'Tiempo de operación de relé', 'ms')",
            "(4, 'EQ-003', '2026-10-21 11:11', 'Planta B', 0.98, " +
                    "'Relación de transformación', 'razón')",
            "(5, 'EQ-004', '2026-10-22 16:45', 'Taller', 7.5, " +
                    "'General', 'Sin especificar')",
            "(6, 'EQ-999', '2026-10-23 10:00', 'Planta C', 99.9, " +
                    "'Resistencia de devanados', 'Ω')"
        ).forEach { fila ->
            db.execSQL("INSERT INTO pruebas VALUES $fila")
        }

        db.version = 4
        db.close()
    }
}
