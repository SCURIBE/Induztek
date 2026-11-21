package com.scarlett.induztek.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.scarlett.induztek.Repository.data.AppDatabase
import com.scarlett.induztek.Repository.data.UsuarioRepository
import com.scarlett.induztek.Repository.model.Equipo
import com.scarlett.induztek.Repository.model.Medicion
import com.scarlett.induztek.Repository.model.Prueba
import com.scarlett.induztek.Repository.model.TipoEnsayo
import com.scarlett.induztek.Repository.model.TipoEquipo
import com.scarlett.induztek.Repository.model.TipoUsuario
import com.scarlett.induztek.Repository.model.UnidadMedida
import com.scarlett.induztek.Repository.model.Usuario
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
 * Los DAO contra una base Room real en memoria.
 * Comprueba claves foráneas, cascada, índice único
 * y actualización de pruebas y mediciones.
 */
@RunWith(RobolectricTestRunner::class)
class DaosTest {

    private lateinit var db: AppDatabase

    @Before
    fun abrir() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        )
            .addCallback(AppDatabase.seedCallback)
            .build()
    }

    @After
    fun cerrar() {
        db.close()
    }

    private suspend fun crearEquipo(
        codigo: String,
        foto: String? = null
    ): Equipo {
        db.equipoDao().insertar(
            Equipo(
                tipo = TipoEquipo.TRANSFORMADOR,
                codigo = codigo,
                fotoUri = foto
            )
        )

        return db.equipoDao()
            .obtenerTodos()
            .first()
            .first { it.codigo == codigo }
    }

    private suspend fun registrarPrueba(
        equipoId: Int,
        fecha: Long,
        vararg valores: Double
    ) {
        db.pruebaDao().insertarConMediciones(
            Prueba(
                equipoId = equipoId,
                fechaHora = fecha,
                ubicacion = "Planta A",
                tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
                unidadMedida = UnidadMedida.MEGAOHM
            ),
            valores.mapIndexed { i, valor ->
                Medicion(
                    pruebaId = 0,
                    nombre = "Fase ${'A' + i}",
                    valor = valor
                )
            }
        )
    }

    // ---------------------------------------------------------
    // EDICIÓN DE PRUEBAS
    // ---------------------------------------------------------

    @Test
    fun `editar prueba conserva su id y actualiza sus datos`() = runBlocking {
        val equipo = crearEquipo("EQ-EDIT")

        registrarPrueba(equipo.id, 1_000, 10.0, 20.0)

        val original = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()
            .prueba

        val editada = original.copy(
            ubicacion = "Planta B"
        )

        db.pruebaDao().actualizarConMediciones(
            editada,
            emptyList()
        )

        val resultado = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()

        assertEquals(original.id, resultado.prueba.id)
        assertEquals(1, db.pruebaDao().obtenerTodasConMediciones().first().size)
        assertEquals("Planta B", resultado.prueba.ubicacion)
        assertEquals(original.fechaHora, resultado.prueba.fechaHora)
        assertTrue(resultado.mediciones.isEmpty())
    }

    @Test
    fun `editar prueba reemplaza las mediciones anteriores`() = runBlocking {
        val equipo = crearEquipo("EQ-EDIT-MED")

        registrarPrueba(equipo.id, 1_000, 10.0, 20.0)

        val original = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()

        val nuevasMediciones = listOf(
            Medicion(
                pruebaId = original.prueba.id,
                nombre = "Fase A",
                valor = 50.0
            ),
            Medicion(
                pruebaId = original.prueba.id,
                nombre = "Fase B",
                valor = 60.0
            )
        )

        db.pruebaDao().actualizarConMediciones(
            original.prueba.copy(
                ubicacion = "Planta C"
            ),
            nuevasMediciones
        )

        val resultado = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()

        assertEquals(1, db.pruebaDao().obtenerTodasConMediciones().first().size)

        assertEquals(
            listOf(50.0, 60.0),
            resultado.mediciones.map { it.valor }
        )

        assertTrue(
            resultado.mediciones.all {
                it.pruebaId == resultado.prueba.id
            }
        )
    }

    @Test
    fun `editar prueba sin mediciones elimina las anteriores`() = runBlocking {
        val equipo = crearEquipo("EQ-EDIT-VACIO")

        registrarPrueba(equipo.id, 1_000, 10.0, 20.0)

        val original = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()

        db.pruebaDao().actualizarConMediciones(
            original.prueba.copy(
                ubicacion = "Planta D"
            ),
            emptyList()
        )

        val resultado = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()

        assertEquals("Planta D", resultado.prueba.ubicacion)
        assertTrue(resultado.mediciones.isEmpty())

        assertEquals(
            1,
            db.pruebaDao().obtenerTodasConMediciones().first().size
        )
    }

    // ---------------------------------------------------------
    // MEDICIONES
    // ---------------------------------------------------------

    @Test
    fun `cada medicion queda apuntando a su prueba`() = runBlocking {
        val equipo = crearEquipo("EQ-001")

        registrarPrueba(
            equipo.id,
            1_000,
            10.0,
            20.0,
            30.0
        )

        val guardada = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()

        assertEquals(
            listOf(10.0, 20.0, 30.0),
            guardada.mediciones.map { it.valor }
        )

        assertTrue(
            guardada.mediciones.all {
                it.pruebaId == guardada.prueba.id
            }
        )
    }

    @Test
    fun `insertar prueba sin mediciones guarda solo la prueba`() = runBlocking {
        val equipo = crearEquipo("EQ-SIN-MEDICIONES")

        db.pruebaDao().insertarConMediciones(
            Prueba(
                equipoId = equipo.id,
                fechaHora = 1_000,
                ubicacion = "Planta A",
                tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
                unidadMedida = UnidadMedida.MEGAOHM
            ),
            emptyList()
        )

        val resultado = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()

        assertEquals(1, resultado.size)
        assertTrue(resultado.first().mediciones.isEmpty())
    }

    // ---------------------------------------------------------
    // ELIMINACIÓN EN CASCADA
    // ---------------------------------------------------------

    @Test
    fun `borrar un equipo borra sus pruebas y mediciones y nada mas`() = runBlocking {
        val borrado = crearEquipo("EQ-001")
        val otro = crearEquipo("EQ-002")

        registrarPrueba(borrado.id, 1_000, 1.0, 2.0)
        registrarPrueba(borrado.id, 2_000, 3.0)
        registrarPrueba(otro.id, 3_000, 4.0)

        db.equipoDao().eliminarPorId(borrado.id)

        val quedan = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()

        assertEquals(1, quedan.size)
        assertEquals(otro.id, quedan[0].prueba.equipoId)
        assertEquals(
            listOf(4.0),
            quedan[0].mediciones.map { it.valor }
        )
    }

    // ---------------------------------------------------------
    // CLAVE FORÁNEA
    // ---------------------------------------------------------

    @Test
    fun `una prueba no puede apuntar a un equipo inexistente`() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                registrarPrueba(
                    equipoId = 99,
                    fecha = 1_000,
                    1.0
                )
            }
        }
    }

    // ---------------------------------------------------------
    // HISTORIAL
    // ---------------------------------------------------------

    @Test
    fun `el historial de un equipo va de la prueba mas nueva a la mas vieja`() = runBlocking {
        val equipo = crearEquipo("EQ-001")

        registrarPrueba(equipo.id, 1_000, 1.0)
        registrarPrueba(equipo.id, 3_000, 3.0)
        registrarPrueba(equipo.id, 2_000, 2.0)

        val historial = db.pruebaDao()
            .obtenerPorEquipoConMediciones(equipo.id)
            .first()

        assertEquals(
            listOf(3_000L, 2_000L, 1_000L),
            historial.map { it.prueba.fechaHora }
        )
    }

    @Test
    fun `buscar prueba por id devuelve sus mediciones`() = runBlocking {
        val equipo = crearEquipo("EQ-BUSCAR")

        registrarPrueba(equipo.id, 1_000, 10.0, 20.0)

        val prueba = db.pruebaDao()
            .obtenerTodasConMediciones()
            .first()
            .single()
            .prueba

        val resultado = db.pruebaDao()
            .obtenerPorIdConMediciones(prueba.id)
            .first()

        assertEquals(prueba.id, resultado?.prueba?.id)
        assertEquals(
            listOf(10.0, 20.0),
            resultado?.mediciones?.map { it.valor }
        )
    }

    // ---------------------------------------------------------
    // FOTOGRAFÍAS
    // ---------------------------------------------------------

    @Test
    fun `solo devuelve las fotos que existen`() = runBlocking {
        crearEquipo("EQ-001", foto = "/evidencias/a.jpg")
        crearEquipo("EQ-002")

        assertEquals(
            listOf("/evidencias/a.jpg"),
            db.equipoDao().rutasDeFotos()
        )
    }

    // ---------------------------------------------------------
    // BÚSQUEDA DE EQUIPO
    // ---------------------------------------------------------

    @Test
    fun `buscar un equipo borrado devuelve null`() = runBlocking {
        val equipo = crearEquipo("EQ-001")

        db.equipoDao().eliminarPorId(equipo.id)

        assertNull(
            db.equipoDao().buscarPorId(equipo.id)
        )
    }

    // ---------------------------------------------------------
    // USUARIOS
    // ---------------------------------------------------------

    @Test
    fun `el indice unico rechaza dos usuarios con el mismo nombre`() {
        val usuario = Usuario(
            nombreUsuario = "lu@induztek.com",
            nombreCompleto = "Lu",
            credencial = "x",
            creadoEn = 0,
            tipoUsuarioId = TipoUsuario.ID_TECNICO
        )

        runBlocking {
            db.usuarioDao().insertar(usuario)
        }

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                db.usuarioDao().insertar(usuario)
            }
        }
    }

    // ---------------------------------------------------------
    // PRECARGA DE TIPOS DE USUARIO
    // ---------------------------------------------------------

    @Test
    fun `precarga los tres tipos de usuario`() = runBlocking {
        val tipos = db.tipoUsuarioDao()
            .obtenerTodos()
            .first()

        assertEquals(
            listOf(
                TipoUsuario(
                    TipoUsuario.ID_ADMINISTRADOR,
                    "Administrador"
                ),
                TipoUsuario(
                    TipoUsuario.ID_TECNICO,
                    "Técnico"
                ),
                TipoUsuario(
                    TipoUsuario.ID_SUPERVISOR,
                    "Supervisor"
                )
            ),
            tipos
        )
    }

    // ---------------------------------------------------------
    // ADMINISTRADOR PRE-CARGADO
    // ---------------------------------------------------------

    @Test
    fun `precarga el administrador y puede autenticarse`() = runBlocking {
        val repositorio = UsuarioRepository(
            db.usuarioDao()
        )

        val admin = repositorio.autenticar(
            AppDatabase.ADMIN_CORREO,
            AppDatabase.ADMIN_CONTRASENA
        )

        assertEquals(
            TipoUsuario.ID_ADMINISTRADOR,
            admin?.tipoUsuarioId
        )

        assertEquals(
            1,
            db.usuarioDao().contar()
        )
    }

    // ---------------------------------------------------------
    // TIPO DE USUARIO INVÁLIDO
    // ---------------------------------------------------------

    @Test
    fun `la clave foranea rechaza un tipo de usuario inexistente`() {
        val usuario = Usuario(
            nombreUsuario = "lu@induztek.com",
            nombreCompleto = "Lu",
            credencial = "x",
            creadoEn = 0,
            tipoUsuarioId = 99
        )

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                db.usuarioDao().insertar(usuario)
            }
        }
    }
}
