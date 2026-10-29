package com.scarlett.induztek.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashTest {

    @Test
    fun `verifica la contrasena con la que se genero`() {
        val almacenado = Hash.generar("Induztek2026")

        assertTrue(Hash.verificar("Induztek2026", almacenado))
    }

    @Test
    fun `rechaza una contrasena distinta`() {
        val almacenado = Hash.generar("Induztek2026")

        assertFalse(Hash.verificar("induztek2026", almacenado))
        assertFalse(Hash.verificar("", almacenado))
    }

    @Test
    fun `el mismo texto genera credenciales distintas`() {
        // El salt es aleatorio por usuario: dos personas con la misma
        // contrasena no pueden quedar con el mismo hash guardado.
        val primera = Hash.generar("Induztek2026")
        val segunda = Hash.generar("Induztek2026")

        assertFalse(primera == segunda)
        assertTrue(Hash.verificar("Induztek2026", primera))
        assertTrue(Hash.verificar("Induztek2026", segunda))
    }

    @Test
    fun `el formato almacenado se puede leer`() {
        val partes = Hash.generar("Induztek2026").split("$")

        assertEquals(4, partes.size)
        assertEquals("pbkdf2sha1", partes[0])
        assertEquals(100_000, partes[1].toInt())
        // 16 bytes de salt y 256 bits de hash, en hexadecimal.
        assertEquals(32, partes[2].length)
        assertEquals(64, partes[3].length)
    }

    @Test
    fun `un texto guardado invalido no revienta`() {
        assertFalse(Hash.verificar("x", ""))
        assertFalse(Hash.verificar("x", "solo-texto"))
        assertFalse(Hash.verificar("x", "pbkdf2sha1\$abc\$aa\$bb"))
        assertFalse(Hash.verificar("x", "bcrypt\$100000\$aa\$bb"))
        // Hexadecimal impar y con caracteres que no son digitos.
        assertFalse(Hash.verificar("x", "pbkdf2sha1\$100000\$aaa\$bb"))
        assertFalse(Hash.verificar("x", "pbkdf2sha1\$100000\$zz\$bb"))
    }
}
