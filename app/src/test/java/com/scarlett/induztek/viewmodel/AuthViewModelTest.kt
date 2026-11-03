package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.Repository.data.UsuarioRepository
import com.scarlett.induztek.Repository.model.TipoUsuario
import com.scarlett.induztek.falsos.ReglaDespachadorPrincipal
import com.scarlett.induztek.falsos.SesionFalsa
import com.scarlett.induztek.falsos.UsuarioDaoFalso
import com.scarlett.induztek.Repository.model.Usuario
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

    private val correo = "tecnico@induztek.com"
    private val clave = "Abc12345"

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
            creadoEn = 0,
            tipoUsuarioId = TipoUsuario.ID_TECNICO
        )
    }

    private fun daoConTecnico(): UsuarioDaoFalso {
        return UsuarioDaoFalso(listOf(usuario(1, correo, clave)))
    }

    private fun autenticado(viewModel: AuthViewModel): Usuario? {
        return (viewModel.estado.value as? EstadoSesion.Autenticado)?.usuario
    }

    @Test
    fun `no crea usuarios al iniciar`() = runTest {
        val dao = UsuarioDaoFalso()

        val viewModel = crear(dao)

        assertTrue(dao.usuarios.isEmpty())
        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
    }

    @Test
    fun `entra directo si habia una sesion guardada`() = runTest {
        val dao = UsuarioDaoFalso(listOf(usuario(7, "lu@induztek.com", "Abc12345")))

        val viewModel = crear(dao, SesionFalsa(inicial = 7))

        assertEquals("lu@induztek.com", autenticado(viewModel)?.nombreUsuario)
    }

    @Test
    fun `limpia una sesion que apunta a un usuario borrado`() = runTest {
        val dao = UsuarioDaoFalso(listOf(usuario(1, "lu@induztek.com", "Abc12345")))
        val sesion = SesionFalsa(inicial = 99)

        val viewModel = crear(dao, sesion)

        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
        assertNull(sesion.actual)
    }

    @Test
    fun `entra con la contrasena correcta`() = runTest {
        val sesion = SesionFalsa()
        val viewModel = crear(daoConTecnico(), sesion)

        // El usuario no distingue mayúsculas ni espacios alrededor.
        viewModel.iniciarSesion("  TECNICO@INDUZTEK.COM ", clave)

        assertEquals(correo, autenticado(viewModel)?.nombreUsuario)
        assertEquals(autenticado(viewModel)?.id, sesion.actual)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `una cuenta antigua sin correo puede entrar`() = runTest {
        val dao = UsuarioDaoFalso(listOf(usuario(1, "tecnico", clave)))
        val viewModel = crear(dao)

        viewModel.iniciarSesion("tecnico", clave)

        assertEquals("tecnico", autenticado(viewModel)?.nombreUsuario)
    }

    @Test
    fun `contrasena incorrecta y usuario inexistente dan el mismo mensaje`() = runTest {
        // Si los mensajes fueran distintos, quien prueba sabría qué
        // usuarios existen.
        val viewModel = crear(daoConTecnico())

        viewModel.iniciarSesion(correo, "otraClave1")
        val conClaveMala = viewModel.error.value

        viewModel.iniciarSesion("nadie@induztek.com", "otraClave1")
        val sinUsuario = viewModel.error.value

        assertEquals("Correo o contraseña incorrectos.", conClaveMala)
        assertEquals(conClaveMala, sinUsuario)
        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
    }

    @Test
    fun `valida los campos antes de buscar al usuario`() = runTest {
        val viewModel = crear(UsuarioDaoFalso())

        viewModel.iniciarSesion("", "")

        assertEquals("Ingresa tu correo.", viewModel.error.value)
        assertFalse(viewModel.ocupado.value)
    }

    @Test
    fun `registra un usuario como tecnico y abre su sesion`() = runTest {
        val dao = UsuarioDaoFalso()
        val sesion = SesionFalsa()
        val viewModel = crear(dao, sesion)

        viewModel.registrar("Lu@Induztek.com", " Lu Pereira ", "Abc12345", "Abc12345")

        val nuevo = dao.usuarios.single { it.nombreUsuario == "lu@induztek.com" }
        assertEquals("Lu Pereira", nuevo.nombreCompleto)
        assertEquals(TipoUsuario.ID_TECNICO, nuevo.tipoUsuarioId)
        assertFalse(nuevo.credencial.contains("Abc12345"))
        assertEquals(nuevo.id, sesion.actual)
        assertEquals("lu@induztek.com", autenticado(viewModel)?.nombreUsuario)
    }

    @Test
    fun `no registra un usuario sin formato de correo`() = runTest {
        val dao = UsuarioDaoFalso()
        val viewModel = crear(dao)

        viewModel.registrar("lupereira", "Lu Pereira", "Abc12345", "Abc12345")

        assertEquals("Ingresa un correo válido.", viewModel.error.value)
        assertTrue(dao.usuarios.isEmpty())
    }

    @Test
    fun `no registra un usuario repetido`() = runTest {
        val dao = daoConTecnico()
        val viewModel = crear(dao)

        viewModel.registrar("TECNICO@induztek.com", "Otra persona", "Abc12345", "Abc12345")

        assertEquals("Ese correo ya está registrado.", viewModel.error.value)
        assertEquals(1, dao.usuarios.size)
    }

    @Test
    fun `no registra si la confirmacion no coincide`() = runTest {
        val dao = daoConTecnico()
        val viewModel = crear(dao)

        viewModel.registrar("lu@induztek.com", "Lu Pereira", "Abc12345", "Abc12346")

        assertEquals("Las contraseñas no coinciden.", viewModel.error.value)
        assertEquals(1, dao.usuarios.size)
    }

    @Test
    fun `cerrar sesion vuelve al login`() = runTest {
        val sesion = SesionFalsa()
        val viewModel = crear(daoConTecnico(), sesion)
        viewModel.iniciarSesion(correo, clave)

        viewModel.cerrarSesion()

        assertEquals(EstadoSesion.SinSesion, viewModel.estado.value)
        assertNull(sesion.actual)
    }
}
