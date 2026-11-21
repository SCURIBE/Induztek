package com.scarlett.induztek.util

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AlmacenFotosTest {

    @get:Rule
    val temporal = TemporaryFolder()

    private val context =
        ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun carpetaEvidencias(): File {
        return temporal.newFolder(AlmacenFotos.CARPETA)
    }

    private fun foto(carpeta: File, nombre: String): File {
        return File(carpeta, nombre).apply {
            writeText("jpg")
        }
    }

    private fun crearImagenValida(archivo: File) {
        val bitmap = Bitmap.createBitmap(
            100,
            100,
            Bitmap.Config.ARGB_8888
        )

        archivo.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        bitmap.recycle()
    }

    // ---------------- CARPETA ----------------

    @Test
    fun carpetaObtieneRutaDeEvidencias() {
        val carpeta = AlmacenFotos.carpeta(context)

        assertEquals(
            File(context.filesDir, AlmacenFotos.CARPETA),
            carpeta
        )
    }

    // ---------------- CREAR ARCHIVO ----------------

    @Test
    fun crearArchivoCreaArchivoDentroDeEvidencias() {
        val archivo = AlmacenFotos.crearArchivo(context)

        assertTrue(archivo.exists())
        assertTrue(archivo.isFile)
        assertEquals(AlmacenFotos.CARPETA, archivo.parentFile?.name)
    }

    // ---------------- NORMALIZAR ----------------

    @Test
    fun normalizarReconoceImagenValida() {
        val archivo = File(temporal.root, "foto.jpg")
        crearImagenValida(archivo)

        assertTrue(AlmacenFotos.normalizar(archivo))
    }

    @Test
    fun normalizarRechazaArchivoInexistente() {
        val archivo = File(temporal.root, "inexistente.jpg")

        assertFalse(AlmacenFotos.normalizar(archivo))
    }

    @Test
    fun normalizarRechazaArchivoVacio() {
        val archivo = File(temporal.root, "vacio.jpg")
        archivo.createNewFile()

        assertFalse(AlmacenFotos.normalizar(archivo))
    }

    // ---------------- LEER ----------------

    @Test
    fun leerRutaNullDevuelveNull() {
        assertNull(AlmacenFotos.leer(null))
    }

    @Test
    fun leerArchivoInexistenteDevuelveNull() {
        assertNull(
            AlmacenFotos.leer(
                File(temporal.root, "no_existe.jpg").absolutePath
            )
        )
    }

    @Test
    fun leerImagenValidaDevuelveBitmap() {
        val archivo = File(temporal.root, "foto.jpg")
        crearImagenValida(archivo)

        val bitmap = AlmacenFotos.leer(archivo.absolutePath)

        assertNotNull(bitmap)
        bitmap?.recycle()
    }

    // ---------------- CALCULAR MUESTREO ----------------

    @Test
    fun unaFotoDe12MPSeDecodificaALaMitad() {
        assertEquals(
            2,
            AlmacenFotos.calcularMuestreo(
                4032,
                3024,
                AlmacenFotos.LADO_GUARDADO
            )
        )
    }

    @Test
    fun miniaturaSeReduceBastanteMas() {
        assertEquals(
            4,
            AlmacenFotos.calcularMuestreo(
                1920,
                1440,
                AlmacenFotos.LADO_MINIATURA
            )
        )
    }

    @Test
    fun unaFotoChicaNoSeReduce() {
        assertEquals(
            1,
            AlmacenFotos.calcularMuestreo(640, 480, 1080)
        )

        assertEquals(
            1,
            AlmacenFotos.calcularMuestreo(1080, 1080, 1080)
        )
    }

    @Test
    fun muestreoConsideraElLadoMayor() {
        assertEquals(
            AlmacenFotos.calcularMuestreo(4032, 3024, 1000),
            AlmacenFotos.calcularMuestreo(3024, 4032, 1000)
        )
    }

    // ---------------- BORRAR ----------------

    @Test
    fun borraFotoDeCarpetaEvidencias() {
        val archivo = foto(carpetaEvidencias(), "equipo_1.jpg")

        assertTrue(AlmacenFotos.borrar(archivo.absolutePath))
        assertFalse(archivo.exists())
    }

    @Test
    fun noBorraArchivosFueraDeEvidencias() {
        val ajeno = foto(temporal.newFolder("otra"), "importante.jpg")

        assertFalse(AlmacenFotos.borrar(ajeno.absolutePath))
        assertTrue(ajeno.exists())
    }

    @Test
    fun borrarSinRutaNoFunciona() {
        assertFalse(AlmacenFotos.borrar(null))
        assertFalse(AlmacenFotos.borrar(""))
    }

    @Test
    fun borrarArchivoInexistenteDevuelveFalse() {
        val archivo = File(
            carpetaEvidencias(),
            "no_existe.jpg"
        )

        assertFalse(AlmacenFotos.borrar(archivo.absolutePath))
    }

    // ---------------- LIMPIAR HUERFANAS ----------------

    @Test
    fun limpiaFotosQueNingunEquipoUsa() {
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
    fun reconoceFotoEnUsoConRutaDiferente() {
        val carpeta = carpetaEvidencias()
        val enUso = foto(carpeta, "equipo_1.jpg")

        val rutaRodeada = File(
            carpeta,
            "../${AlmacenFotos.CARPETA}/equipo_1.jpg"
        ).path

        assertEquals(
            0,
            AlmacenFotos.limpiarHuerfanas(
                carpeta,
                listOf(rutaRodeada)
            )
        )

        assertTrue(enUso.exists())
    }

    @Test
    fun carpetaInexistenteNoEliminaNada() {
        val inexistente = File(
            temporal.root,
            "nunca_creada"
        )

        assertEquals(
            0,
            AlmacenFotos.limpiarHuerfanas(
                inexistente,
                emptyList()
            )
        )
    }

    @Test
    fun limpiarHuerfanasNoEliminaSubcarpetas() {
        val carpeta = carpetaEvidencias()

        val subcarpeta = File(carpeta, "subcarpeta")
        subcarpeta.mkdir()

        val fotoHuerfana = foto(carpeta, "equipo_2.jpg")

        val borradas = AlmacenFotos.limpiarHuerfanas(
            carpeta,
            emptyList()
        )

        assertEquals(1, borradas)
        assertTrue(subcarpeta.exists())
        assertFalse(fotoHuerfana.exists())
    }
}
