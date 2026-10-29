package com.scarlett.induztek.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// Solo las partes de AlmacenFotos que no dependen de Android: los
// archivos y el cálculo del muestreo. Decodificar imágenes necesita
// BitmapFactory, que en un test de JUnit no existe.
class AlmacenFotosTest {

    @get:Rule
    val temporal = TemporaryFolder()

    private fun carpetaEvidencias(): File {
        return temporal.newFolder(AlmacenFotos.CARPETA)
    }

    private fun foto(carpeta: File, nombre: String): File {
        return File(carpeta, nombre).apply { writeText("jpg") }
    }

    @Test
    fun `una foto de 12 MP se decodifica a la mitad`() {
        // 4032 / 2 = 2016, todavía sobre 1920: alcanza sin perder nitidez.
        assertEquals(
            2,
            AlmacenFotos.calcularMuestreo(4032, 3024, AlmacenFotos.LADO_GUARDADO)
        )
    }

    @Test
    fun `la miniatura de la lista se reduce bastante mas`() {
        // 1920 / 4 = 480 sobre 256, pero 1920 / 8 = 240 ya quedaría bajo.
        assertEquals(
            4,
            AlmacenFotos.calcularMuestreo(1920, 1440, AlmacenFotos.LADO_MINIATURA)
        )
    }

    @Test
    fun `una foto chica no se reduce`() {
        assertEquals(1, AlmacenFotos.calcularMuestreo(640, 480, 1080))
        assertEquals(1, AlmacenFotos.calcularMuestreo(1080, 1080, 1080))
    }

    @Test
    fun `el muestreo mira el lado mayor aunque la foto este vertical`() {
        assertEquals(
            AlmacenFotos.calcularMuestreo(4032, 3024, 1000),
            AlmacenFotos.calcularMuestreo(3024, 4032, 1000)
        )
    }

    @Test
    fun `borra una foto de la carpeta de evidencias`() {
        val archivo = foto(carpetaEvidencias(), "equipo_1.jpg")

        assertTrue(AlmacenFotos.borrar(archivo.absolutePath))
        assertFalse(archivo.exists())
    }

    @Test
    fun `no borra archivos fuera de la carpeta de evidencias`() {
        val ajeno = foto(temporal.newFolder("otra"), "importante.jpg")

        assertFalse(AlmacenFotos.borrar(ajeno.absolutePath))
        assertTrue(ajeno.exists())
    }

    @Test
    fun `borrar sin foto o una foto que ya no esta no falla`() {
        assertFalse(AlmacenFotos.borrar(null))
        assertFalse(AlmacenFotos.borrar(""))
        assertFalse(
            AlmacenFotos.borrar(File(carpetaEvidencias(), "no_existe.jpg").absolutePath)
        )
    }

    @Test
    fun `limpia solo las fotos que ningun equipo usa`() {
        val carpeta = carpetaEvidencias()
        val enUso = foto(carpeta, "equipo_1.jpg")
        val huerfana = foto(carpeta, "equipo_2.jpg")
        val otraHuerfana = foto(carpeta, "equipo_3.jpg")

        val borradas = AlmacenFotos.limpiarHuerfanas(
            carpeta,
            listOf(enUso.absolutePath)
        )

        assertEquals(2, borradas)
        assertTrue(enUso.exists())
        assertFalse(huerfana.exists())
        assertFalse(otraHuerfana.exists())
    }

    @Test
    fun `reconoce una foto en uso aunque la ruta este escrita distinto`() {
        val carpeta = carpetaEvidencias()
        val enUso = foto(carpeta, "equipo_1.jpg")

        // La misma foto, nombrada pasando por una carpeta y volviendo.
        val rutaRodeada = File(carpeta, "../${AlmacenFotos.CARPETA}/equipo_1.jpg").path

        assertEquals(0, AlmacenFotos.limpiarHuerfanas(carpeta, listOf(rutaRodeada)))
        assertTrue(enUso.exists())
    }

    @Test
    fun `sin carpeta de evidencias no hay nada que limpiar`() {
        val inexistente = File(temporal.root, "nunca_creada")

        assertEquals(0, AlmacenFotos.limpiarHuerfanas(inexistente, emptyList()))
    }
}
