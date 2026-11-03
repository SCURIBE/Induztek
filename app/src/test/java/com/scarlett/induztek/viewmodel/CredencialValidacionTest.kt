package com.scarlett.induztek.viewmodel

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CredencialValidacionTest {

    @Test
    fun `acepta un correo valido`() {
        assertNull(CredencialValidacion.validarCorreoNuevo("tecnico@induztek.com"))
        assertNull(CredencialValidacion.validarCorreoNuevo("  lu.perez+1@planta.induztek.cl  "))
    }

    @Test
    fun `rechaza un correo vacio o mal formado`() {
        assertNotNull(CredencialValidacion.validarCorreoNuevo(""))
        assertNotNull(CredencialValidacion.validarCorreoNuevo("   "))
        assertNotNull(CredencialValidacion.validarCorreoNuevo("tecnico"))
        assertNotNull(CredencialValidacion.validarCorreoNuevo("tecnico@induztek"))
        assertNotNull(CredencialValidacion.validarCorreoNuevo("lu perez@induztek.com"))
        assertNotNull(CredencialValidacion.validarCorreoNuevo("@induztek.com"))
    }

    @Test
    fun `el login no exige formato de correo`() {
        assertNull(CredencialValidacion.validarCorreoLogin("tecnico"))
        assertNotNull(CredencialValidacion.validarCorreoLogin("  "))
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
