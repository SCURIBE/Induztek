package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.data.UsuarioRepository
import com.scarlett.induztek.falsos.ReglaDespachadorPrincipal
import com.scarlett.induztek.falsos.SesionFalsa
import com.scarlett.induztek.falsos.UsuarioDaoFalso
import com.scarlett.induztek.model.Usuario
import com.scarlett.induztek.util.Hash
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val regla = ReglaDespachadorPrincipal()

    private val demo = UsuarioRepository.USUARIO_DEMO
    private val claveDemo = UsuarioRepository.CONTRASENA_DEMO

    private fun crear(
        dao: UsuarioDaoFalso,
        sesion: SesionFalsa = SesionFalsa()
    ): AuthViewModel {
        // El mismo despachador de la regla también para PBKDF2: el hash
        // se calcula en el acto y el test no espera otro hilo.
        return AuthViewModel(UsuarioRepository(dao), sesion, regla.despachador)
    }

    private fun usuario(id: Int, nombre: String, clave: String): Usuario {
        return Usuario(
            id = id,
            nombreUsuario = nombre,
            nombreCompleto = "Nombre de $nombre",
            credencial = Hash.generar(clave),
            creadoEn = 0
        )
    }

    private fun autenticado(viewModel: AuthViewModel): Usuario? {
        return (viewModel.estado.value as? EstadoSesion.Autenticado)?.usuario
    }

    @Test
    fun `crea el usuario de demostracion si no hay ninguno`() = runTest {
        val dao = UsuarioDaoFalso()

        val viewModel = crear(dao)

        val creado = dao.usuarios.single()
        assertEquals(demo, creado.nombreUsuario)
        // Se guarda el hash, nunca la contraseña.
        assertFalse(creado.credencial.contains(claveDemo))
        assertTrue(Hash.verificar(claveDemo, creado.credencial))
        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
    }

    @Test
    fun `no crea el usuario de demostracion si ya hay usuarios`() = runTest {
        val dao = UsuarioDaoFalso(listOf(usuario(1, "lu", "Abc12345")))

        crear(dao)

        assertEquals(listOf("lu"), dao.usuarios.map { it.nombreUsuario })
    }

    @Test
    fun `entra directo si habia una sesion guardada`() = runTest {
        val dao = UsuarioDaoFalso(listOf(usuario(7, "lu", "Abc12345")))

        val viewModel = crear(dao, SesionFalsa(inicial = 7))

        assertEquals("lu", autenticado(viewModel)?.nombreUsuario)
    }

    @Test
    fun `limpia una sesion que apunta a un usuario borrado`() = runTest {
        val dao = UsuarioDaoFalso(listOf(usuario(1, "lu", "Abc12345")))
        val sesion = SesionFalsa(inicial = 99)

        val viewModel = crear(dao, sesion)

        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
        assertNull(sesion.actual)
    }

    @Test
    fun `entra con la contrasena correcta`() = runTest {
        val sesion = SesionFalsa()
        val viewModel = crear(UsuarioDaoFalso(), sesion)

        // El usuario no distingue mayúsculas ni espacios alrededor.
        viewModel.iniciarSesion("  TECNICO ", claveDemo)

        assertEquals(demo, autenticado(viewModel)?.nombreUsuario)
        assertEquals(autenticado(viewModel)?.id, sesion.actual)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `contrasena incorrecta y usuario inexistente dan el mismo mensaje`() = runTest {
        // Si los mensajes fueran distintos, quien prueba sabría qué
        // usuarios existen.
        val viewModel = crear(UsuarioDaoFalso())

        viewModel.iniciarSesion(demo, "otraClave1")
        val conClaveMala = viewModel.error.value

        viewModel.iniciarSesion("nadie", "otraClave1")
        val sinUsuario = viewModel.error.value

        assertEquals("Usuario o contraseña incorrectos.", conClaveMala)
        assertEquals(conClaveMala, sinUsuario)
        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
    }

    @Test
    fun `valida los campos antes de buscar al usuario`() = runTest {
        val viewModel = crear(UsuarioDaoFalso())

        viewModel.iniciarSesion("", "")

        assertEquals("Ingresa tu usuario.", viewModel.error.value)
        assertFalse(viewModel.ocupado.value)
    }

    @Test
    fun `registra un usuario y abre su sesion`() = runTest {
        val dao = UsuarioDaoFalso()
        val sesion = SesionFalsa()
        val viewModel = crear(dao, sesion)

        viewModel.registrar("LuPereira", " Lu Pereira ", "Abc12345", "Abc12345")

        val nuevo = dao.usuarios.single { it.nombreUsuario == "lupereira" }
        assertEquals("Lu Pereira", nuevo.nombreCompleto)
        assertFalse(nuevo.credencial.contains("Abc12345"))
        assertEquals(nuevo.id, sesion.actual)
        assertEquals("lupereira", autenticado(viewModel)?.nombreUsuario)
    }

    @Test
    fun `no registra un usuario repetido`() = runTest {
        val dao = UsuarioDaoFalso()
        val viewModel = crear(dao)

        viewModel.registrar("TECNICO", "Otra persona", "Abc12345", "Abc12345")

        assertEquals("Ese usuario ya está registrado.", viewModel.error.value)
        assertEquals(1, dao.usuarios.size)
    }

    @Test
    fun `no registra si la confirmacion no coincide`() = runTest {
        val dao = UsuarioDaoFalso()
        val viewModel = crear(dao)

        viewModel.registrar("lupereira", "Lu Pereira", "Abc12345", "Abc12346")

        assertEquals("Las contraseñas no coinciden.", viewModel.error.value)
        assertEquals(1, dao.usuarios.size)
    }

    @Test
    fun `cerrar sesion vuelve al login`() = runTest {
        val sesion = SesionFalsa()
        val viewModel = crear(UsuarioDaoFalso(), sesion)
        viewModel.iniciarSesion(demo, claveDemo)

        viewModel.cerrarSesion()

        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
        assertNull(sesion.actual)
    }
}
