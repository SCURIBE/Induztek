package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.data.EquipoRepository
import com.scarlett.induztek.falsos.EquipoDaoFalso
import com.scarlett.induztek.falsos.ReglaDespachadorPrincipal
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.TipoEquipo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EquipoViewModelTest {

    @get:Rule
    val regla = ReglaDespachadorPrincipal()

    // Las fotos que el ViewModel mandó a borrar, en vez de borrar
    // archivos de verdad.
    private val fotosBorradas = mutableListOf<String?>()

    private val transformador = Equipo(
        id = 1,
        tipo = TipoEquipo.TRANSFORMADOR,
        codigo = "EQ-001",
        fotoUri = "/evidencias/vieja.jpg"
    )

    private fun crear(dao: EquipoDaoFalso): EquipoViewModel {
        return EquipoViewModel(EquipoRepository(dao)) { fotosBorradas += it }
    }

    @Test
    fun `expone los equipos ordenados por codigo`() = runTest {
        val dao = EquipoDaoFalso(
            listOf(
                transformador.copy(id = 2, codigo = "EQ-002"),
                transformador
            )
        )

        val viewModel = crear(dao)

        assertEquals(
            listOf("EQ-001", "EQ-002"),
            viewModel.equipos.value.map { it.codigo }
        )
    }

    @Test
    fun `no guarda un equipo sin codigo`() = runTest {
        val dao = EquipoDaoFalso()
        val viewModel = crear(dao)
        var resultado: Boolean? = null

        viewModel.insertar(Equipo(tipo = TipoEquipo.CABLE, codigo = "   ")) {
            resultado = it
        }

        assertEquals(false, resultado)
        assertEquals("Ingresa el código del equipo.", viewModel.error.value)
        assertTrue(dao.equipos.value.isEmpty())
    }

    @Test
    fun `no guarda un codigo repetido`() = runTest {
        val dao = EquipoDaoFalso(listOf(transformador))
        val viewModel = crear(dao)
        var resultado: Boolean? = null

        viewModel.insertar(Equipo(tipo = TipoEquipo.CABLE, codigo = " EQ-001 ")) {
            resultado = it
        }

        assertEquals(false, resultado)
        assertEquals("Ya existe un equipo con ese código.", viewModel.error.value)
        assertEquals(1, dao.equipos.value.size)
    }

    @Test
    fun `guarda el codigo sin espacios alrededor`() = runTest {
        val dao = EquipoDaoFalso()
        val viewModel = crear(dao)
        var resultado: Boolean? = null

        viewModel.insertar(Equipo(tipo = TipoEquipo.CELDA, codigo = "  EQ-010 ")) {
            resultado = it
        }

        assertEquals(true, resultado)
        assertNull(viewModel.error.value)
        assertEquals("EQ-010", dao.equipos.value.single().codigo)
    }

    @Test
    fun `al editar no acepta el codigo de otro equipo`() = runTest {
        val otro = transformador.copy(id = 2, codigo = "EQ-002")
        val dao = EquipoDaoFalso(listOf(transformador, otro))
        val viewModel = crear(dao)
        var resultado: Boolean? = null

        viewModel.actualizar(otro.copy(codigo = "EQ-001")) { resultado = it }

        assertEquals(false, resultado)
        assertEquals("Ese código ya pertenece a otro equipo.", viewModel.error.value)
    }

    @Test
    fun `al editar puede conservar su propio codigo`() = runTest {
        val dao = EquipoDaoFalso(listOf(transformador))
        val viewModel = crear(dao)
        var resultado: Boolean? = null

        viewModel.actualizar(transformador.copy(tipo = TipoEquipo.OTRO)) {
            resultado = it
        }

        assertEquals(true, resultado)
        assertEquals(TipoEquipo.OTRO, dao.equipos.value.single().tipo)
    }

    @Test
    fun `al cambiar la foto borra la anterior`() = runTest {
        val dao = EquipoDaoFalso(listOf(transformador))
        val viewModel = crear(dao)

        viewModel.actualizar(transformador.copy(fotoUri = "/evidencias/nueva.jpg")) {}

        assertEquals(listOf<String?>("/evidencias/vieja.jpg"), fotosBorradas)
        assertEquals("/evidencias/nueva.jpg", dao.equipos.value.single().fotoUri)
    }

    @Test
    fun `si la foto no cambia no se borra nada`() = runTest {
        val dao = EquipoDaoFalso(listOf(transformador))
        val viewModel = crear(dao)

        viewModel.actualizar(transformador.copy(codigo = "EQ-001-B")) {}

        assertTrue(fotosBorradas.isEmpty())
    }

    @Test
    fun `si el cambio no se guarda la foto anterior se conserva`() = runTest {
        val otro = transformador.copy(id = 2, codigo = "EQ-002", fotoUri = null)
        val dao = EquipoDaoFalso(listOf(transformador, otro))
        val viewModel = crear(dao)

        // Código repetido: el update no ocurre, la foto vieja sigue en uso.
        viewModel.actualizar(
            transformador.copy(codigo = "EQ-002", fotoUri = "/evidencias/nueva.jpg")
        ) {}

        assertTrue(fotosBorradas.isEmpty())
    }

    @Test
    fun `al eliminar el equipo borra tambien su foto`() = runTest {
        val dao = EquipoDaoFalso(listOf(transformador))
        val viewModel = crear(dao)

        viewModel.eliminar(transformador.id)

        assertTrue(dao.equipos.value.isEmpty())
        assertEquals(listOf<String?>("/evidencias/vieja.jpg"), fotosBorradas)
    }
}
