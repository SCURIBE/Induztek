package com.scarlett.induztek.viewmodel

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CredencialValidacionTest {

    @Test
    fun `acepta un usuario valido`() {
        assertNull(CredencialValidacion.validarNombreUsuario("tecnico"))
        // Se recorta antes de medir, igual que al guardar.
        assertNull(CredencialValidacion.validarNombreUsuario("  tecnico  "))
    }

    @Test
    fun `rechaza un usuario vacio corto o con espacios`() {
        assertNotNull(CredencialValidacion.validarNombreUsuario(""))
        assertNotNull(CredencialValidacion.validarNombreUsuario("   "))
        assertNotNull(CredencialValidacion.validarNombreUsuario("lu"))
        assertNotNull(CredencialValidacion.validarNombreUsuario("lu perez"))
    }

    @Test
    fun `el login no exige formato de contrasena`() {
        // Una cuenta creada con reglas anteriores tiene que poder
        // entrar aunque hoy el minimo sea mayor.
        assertNull(CredencialValidacion.validarContrasenaLogin("corta"))
        assertNotNull(CredencialValidacion.validarContrasenaLogin(""))
    }

    @Test
    fun `el registro si exige formato`() {
        assertNull(CredencialValidacion.validarContrasenaNueva("Induztek2026"))
        // Corta, sin numero y sin letra.
        assertNotNull(CredencialValidacion.validarContrasenaNueva("Ind2026"))
        assertNotNull(CredencialValidacion.validarContrasenaNueva("Induztek"))
        assertNotNull(CredencialValidacion.validarContrasenaNueva("20262026"))
        assertNotNull(CredencialValidacion.validarContrasenaNueva(""))
    }

    @Test
    fun `la confirmacion tiene que coincidir exactamente`() {
        assertNull(
            CredencialValidacion.validarConfirmacion("Abc12345", "Abc12345")
        )
        assertNotNull(
            CredencialValidacion.validarConfirmacion("Abc12345", "abc12345")
        )
        assertNotNull(
            CredencialValidacion.validarConfirmacion("Abc12345", "Abc12345 ")
        )
    }

    @Test
    fun `el nombre completo no puede quedar en blanco`() {
        assertNull(CredencialValidacion.validarNombreCompleto("Lu Pereira"))
        assertNotNull(CredencialValidacion.validarNombreCompleto("   "))
    }
}
