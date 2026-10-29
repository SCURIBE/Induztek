package com.scarlett.induztek.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.PruebaConMediciones
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.TipoEquipo
import com.scarlett.induztek.model.UnidadMedida
import com.scarlett.induztek.viewmodel.Protocolo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/*
 * Genera el PDF en un dispositivo o emulador. Es instrumentado y no de
 * Robolectric porque PdfDocument dibuja con código nativo que Robolectric
 * no simula: ahí el documento nace cerrado.
 *
 * No revisa cómo se ve (eso hay que mirarlo abriendo el archivo), pero
 * sí que sea un PDF válido y que el reparto en páginas funcione.
 *
 *     ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ProtocoloPdfTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val equipo = Equipo(id = 1, tipo = TipoEquipo.TRANSFORMADOR, codigo = "EQ-001")

    private fun prueba(observaciones: String = ""): PruebaConMediciones {
        return PruebaConMediciones(
            prueba = Prueba(
                id = 7,
                equipoId = 1,
                fechaHora = 1_790_857_800_000,
                ubicacion = "Planta A",
                tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
                unidadMedida = UnidadMedida.MEGAOHM,
                cliente = "Minera Norte",
                tecnicoResponsable = "Lu Pereira",
                observaciones = observaciones
            ),
            mediciones = listOf(
                Medicion(pruebaId = 7, nombre = "Fase A", valor = 1200.0, minimoAceptable = 1000.0),
                Medicion(pruebaId = 7, nombre = "Fase B", valor = 950.5, minimoAceptable = 1000.0),
                Medicion(pruebaId = 7, nombre = "Fase C", valor = 1100.0, minimoAceptable = 1000.0)
            )
        )
    }

    // Cuenta las páginas: cada una es un objeto "/Type /Page" en el PDF.
    private fun paginas(bytes: ByteArray): Int {
        return Regex("""/Type\s*/Page\b""")
            .findAll(String(bytes, Charsets.ISO_8859_1))
            .count()
    }

    @Test
    fun generaUnPdfDeUnaPagina() {
        val contenido = Protocolo.armar(prueba(), equipo, null, System.currentTimeMillis())
        val foto = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.GRAY)
        }

        val archivo = ProtocoloPdf.generar(context, contenido, foto)
        val bytes = archivo.readBytes()

        assertEquals(contenido.nombreArchivo, archivo.name)
        assertEquals("%PDF-", String(bytes, 0, 5, Charsets.ISO_8859_1))
        assertEquals(1, paginas(bytes))
    }

    @Test
    fun unasObservacionesLargasSiguenEnOtraPagina() {
        val largas = (1..120).joinToString(" ") {
            "Línea $it de observaciones del técnico sobre el estado del equipo."
        }
        val contenido = Protocolo.armar(prueba(largas), equipo, null, 0)

        val bytes = ProtocoloPdf.generar(context, contenido, null).readBytes()

        assertTrue(paginas(bytes) >= 2)
    }
}
