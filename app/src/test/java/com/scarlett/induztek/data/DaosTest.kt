package com.scarlett.induztek.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.TipoEquipo
import com.scarlett.induztek.model.UnidadMedida
import com.scarlett.induztek.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/*
 * Los DAO contra una base Room real en memoria. Comprueba lo que los
 * DAO falsos de los otros tests solo imitan: las claves foráneas, la
 * cascada y el índice único.
 */
@RunWith(RobolectricTestRunner::class)
class DaosTest {

    private lateinit var db: AppDatabase

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun cerrar() {
        db.close()
    }

    private suspend fun crearEquipo(codigo: String, foto: String? = null): Equipo {
        db.equipoDao().insertar(
            Equipo(tipo = TipoEquipo.TRANSFORMADOR, codigo = codigo, fotoUri = foto)
        )
        return db.equipoDao().obtenerTodos().first().first { it.codigo == codigo }
    }

    private suspend fun registrarPrueba(equipoId: Int, fecha: Long, vararg valores: Double) {
        db.pruebaDao().insertarConMediciones(
            Prueba(
                equipoId = equipoId,
                fechaHora = fecha,
                ubicacion = "Planta A",
                tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
                unidadMedida = UnidadMedida.MEGAOHM
            ),
            valores.mapIndexed { i, valor ->
                Medicion(pruebaId = 0, nombre = "Fase ${'A' + i}", valor = valor)
            }
        )
    }

    @Test
    fun `cada medicion queda apuntando a su prueba`() = runBlocking {
        val equipo = crearEquipo("EQ-001")
        registrarPrueba(equipo.id, 1_000, 10.0, 20.0, 30.0)

        val guardada = db.pruebaDao().obtenerTodasConMediciones().first().single()

        assertEquals(listOf(10.0, 20.0, 30.0), guardada.mediciones.map { it.valor })
        assertTrue(guardada.mediciones.all { it.pruebaId == guardada.prueba.id })
    }

    @Test
    fun `borrar un equipo borra sus pruebas y mediciones y nada mas`() = runBlocking {
        val borrado = crearEquipo("EQ-001")
        val otro = crearEquipo("EQ-002")
        registrarPrueba(borrado.id, 1_000, 1.0, 2.0)
        registrarPrueba(borrado.id, 2_000, 3.0)
        registrarPrueba(otro.id, 3_000, 4.0)

        db.equipoDao().eliminarPorId(borrado.id)

        val quedan = db.pruebaDao().obtenerTodasConMediciones().first()
        assertEquals(1, quedan.size)
        assertEquals(otro.id, quedan[0].prueba.equipoId)
        assertEquals(listOf(4.0), quedan[0].mediciones.map { it.valor })
    }

    @Test
    fun `una prueba no puede apuntar a un equipo inexistente`() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { registrarPrueba(equipoId = 99, fecha = 1_000, 1.0) }
        }
    }

    @Test
    fun `el historial de un equipo va de la prueba mas nueva a la mas vieja`() = runBlocking {
        val equipo = crearEquipo("EQ-001")
        registrarPrueba(equipo.id, 1_000, 1.0)
        registrarPrueba(equipo.id, 3_000, 3.0)
        registrarPrueba(equipo.id, 2_000, 2.0)

        val historial = db.pruebaDao().obtenerPorEquipoConMediciones(equipo.id).first()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), historial.map { it.prueba.fechaHora })
    }

    @Test
    fun `solo devuelve las fotos que existen`() = runBlocking {
        crearEquipo("EQ-001", foto = "/evidencias/a.jpg")
        crearEquipo("EQ-002")

        assertEquals(listOf("/evidencias/a.jpg"), db.equipoDao().rutasDeFotos())
    }

    @Test
    fun `buscar un equipo borrado devuelve null`() = runBlocking {
        val equipo = crearEquipo("EQ-001")
        db.equipoDao().eliminarPorId(equipo.id)

        assertNull(db.equipoDao().buscarPorId(equipo.id))
    }

    @Test
    fun `el indice unico rechaza dos usuarios con el mismo nombre`() {
        val usuario = Usuario(
            nombreUsuario = "lu",
            nombreCompleto = "Lu",
            credencial = "x",
            creadoEn = 0
        )

        runBlocking { db.usuarioDao().insertar(usuario) }

        // Es la red de seguridad del registro: aunque dos registros pasen
        // la consulta existeNombre a la vez, la base no deja el segundo.
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { db.usuarioDao().insertar(usuario) }
        }
    }
}
