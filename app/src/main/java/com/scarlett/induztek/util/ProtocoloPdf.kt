package com.scarlett.induztek.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import com.scarlett.induztek.model.ResultadoMedicion
import com.scarlett.induztek.viewmodel.FilaProtocolo
import com.scarlett.induztek.viewmodel.ProtocoloContenido
import java.io.File
import java.io.FileOutputStream

/*
 * Dibuja el protocolo en un PDF A4 y lo entrega a la hoja de compartir.
 * Qué dice el documento lo decide viewmodel/Protocolo; aquí solo se
 * decide dónde va cada cosa.
 *
 * El documento se arma en tres pasos:
 *  1. Se mide cada bloque (el título, cada dato, cada fila de la
 *     tabla). StaticLayout calcula cuánto alto ocupa un texto con sus
 *     saltos de línea sin necesidad de dibujarlo.
 *  2. Se reparten los bloques en páginas. Como ya se sabe cuántas son,
 *     el pie puede decir "Página 1 de 2".
 *  3. Se dibuja cada página.
 */
object ProtocoloPdf {

    // A4 en puntos (1/72 de pulgada), la unidad de PdfDocument.
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 48f
    private const val ANCHO_UTIL = ANCHO - 2 * MARGEN

    // La cabecera y el pie se repiten en todas las páginas; los bloques
    // van entre los dos.
    private const val INICIO = MARGEN + 30f
    private const val LIMITE = ALTO - MARGEN - 26f

    private const val RELLENO = 4f
    private const val CARPETA = "protocolos"

    // Ancho de cada columna de la tabla de mediciones: suman ANCHO_UTIL.
    private val COLUMNAS = floatArrayOf(88f, 82f, 92f, 70f, 82f, 85f)

    private val TEXTO = 0xFF1F1F1F.toInt()
    private val SECUNDARIO = 0xFF5F6368.toInt()
    private val ACENTO = 0xFF1F3A5F.toInt()
    private val APROBADO = 0xFF1E6B32.toInt()
    private val RECHAZADO = 0xFFB3261E.toInt()
    private val LINEA = 0xFFC4C7CC.toInt()
    private val FONDO_ENCABEZADO = 0xFFEEF1F5.toInt()

    private val pinturaLinea = Paint().apply {
        color = LINEA
        strokeWidth = 0.75f
    }

    // Un trozo del documento que no se parte entre dos páginas.
    private class Bloque(
        val alto: Float,
        // Alto que tiene que caber después en la misma página, para no
        // dejar un título solo al pie, sin nada de lo que titula.
        val reservaSiguiente: Float = 0f,
        // Si el bloque cae al principio de una página nueva, antes se
        // dibuja este otro: el encabezado de la tabla, para que la tabla
        // que continúa se entienda.
        val repetirAntes: Bloque? = null,
        val dibujar: (Canvas, Float) -> Unit
    )

    /*
     * Genera el PDF en la caché de la app y devuelve el archivo. En la
     * caché porque siempre se puede volver a generar desde la base: si
     * el sistema necesita espacio y lo borra, no se pierde nada.
     */
    fun generar(
        context: Context,
        contenido: ProtocoloContenido,
        foto: Bitmap?
    ): File {
        val paginas = repartir(bloques(contenido, foto))
        val documento = PdfDocument()

        try {
            paginas.forEachIndexed { indice, colocados ->
                val pagina = documento.startPage(
                    PdfDocument.PageInfo.Builder(ANCHO, ALTO, indice + 1)
                        .create()
                )

                val canvas = pagina.canvas
                dibujarCabecera(canvas, contenido)

                colocados.forEach { (bloque, y) ->
                    bloque.dibujar(canvas, y)
                }

                dibujarPie(canvas, contenido, indice + 1, paginas.size)
                documento.finishPage(pagina)
            }

            val carpeta = File(context.cacheDir, CARPETA)

            if (!carpeta.exists() && !carpeta.mkdirs()) {
                throw IllegalStateException(
                    "No se pudo crear la carpeta de protocolos."
                )
            }

            val archivo = File(carpeta, contenido.nombreArchivo)
            FileOutputStream(archivo).use { documento.writeTo(it) }
            return archivo
        } finally {
            documento.close()
        }
    }

    fun compartir(context: Context, archivo: File, asunto: String) {
        val uri = ProveedorArchivos.uriPara(context, archivo)

        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, asunto)
            // ClipData además de EXTRA_STREAM: es lo que hace llegar el
            // permiso de lectura a la app elegida a través del selector,
            // que es otra actividad entre medio.
            clipData = ClipData.newRawUri(archivo.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val selector = Intent.createChooser(envio, "Compartir protocolo")

        if (context !is Activity) {
            selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(selector)
    }

    private fun repartir(
        bloques: List<Bloque>
    ): List<List<Pair<Bloque, Float>>> {
        val paginas = mutableListOf(mutableListOf<Pair<Bloque, Float>>())
        var y = INICIO

        bloques.forEach { bloque ->
            val necesario = bloque.alto + bloque.reservaSiguiente

            // y > INICIO evita una página en blanco infinita si un bloque
            // no cabe ni en una página vacía: se dibuja igual y se corta.
            if (y + necesario > LIMITE && y > INICIO) {
                paginas += mutableListOf<Pair<Bloque, Float>>()
                y = INICIO

                bloque.repetirAntes?.let { encabezado ->
                    paginas.last() += encabezado to y
                    y += encabezado.alto
                }
            }

            paginas.last() += bloque to y
            y += bloque.alto
        }

        return paginas
    }

    private fun bloques(
        contenido: ProtocoloContenido,
        foto: Bitmap?
    ): List<Bloque> {
        val bloques = mutableListOf<Bloque>()

        // Título.
        val titulo = texto(
            "Protocolo de prueba eléctrica",
            pintura(18f, ACENTO, negrita = true)
        )
        val ensayo = texto(
            contenido.tipoEnsayo,
            pintura(13f, negrita = true)
        )

        bloques += Bloque(titulo.height + 4f + ensayo.height + 4f) { c, y ->
            c.dibujar(titulo, MARGEN, y)
            c.dibujar(ensayo, MARGEN, y + titulo.height + 4f)
        }

        // Datos de la prueba, en dos columnas: etiqueta y valor.
        val datos = contenido.datos.map { (etiqueta, valor) ->
            dato(etiqueta, valor)
        }

        bloques += tituloSeccion(
            "Datos de la prueba",
            datos.firstOrNull()?.alto ?: 0f
        )
        bloques += datos

        if (foto != null) {
            bloques += bloqueFoto(foto)
        }

        // Mediciones.
        val unidad = contenido.unidad
        val encabezado = filaTabla(
            celdas = listOf(
                "Punto",
                "Valor",
                "Rango aceptable ($unidad)",
                "Resultado",
                "Anterior",
                "Diferencia ($unidad)"
            ),
            pinturas = List(6) { pintura(8.5f, ACENTO, negrita = true) },
            fondo = FONDO_ENCABEZADO
        )

        val filas = contenido.filas.map { fila ->
            filaMedicion(fila, encabezado)
        }

        val comparacion = parrafo(
            contenido.comparacion,
            pintura(9f, SECUNDARIO),
            espacioDespues = 8f
        )

        val primeraFila = filas.firstOrNull()?.alto ?: 0f

        bloques += tituloSeccion(
            "Mediciones",
            comparacion.alto + encabezado.alto + primeraFila
        )
        bloques += Bloque(
            alto = comparacion.alto,
            reservaSiguiente = encabezado.alto + primeraFila,
            dibujar = comparacion.dibujar
        )

        if (filas.isEmpty()) {
            bloques += parrafo(
                "Sin mediciones registradas.",
                pintura(10f, SECUNDARIO)
            )
        } else {
            bloques += Bloque(
                alto = encabezado.alto,
                reservaSiguiente = primeraFila,
                dibujar = encabezado.dibujar
            )
            bloques += filas
        }

        bloques += parrafo(
            contenido.resumen,
            pintura(
                10.5f,
                if (contenido.fueraDeRango > 0) RECHAZADO else TEXTO,
                negrita = true
            ),
            espacioAntes = 10f
        )

        // Observaciones: una línea por bloque, para que un texto largo
        // pueda seguir en la página siguiente en vez de salirse.
        if (contenido.observaciones.isNotBlank()) {
            val lineas = lineas(contenido.observaciones, pintura(10f))

            bloques += tituloSeccion(
                "Observaciones",
                lineas.firstOrNull()?.alto ?: 0f
            )
            bloques += lineas
        }

        bloques += bloqueFirmas(contenido.tecnico)

        return bloques
    }

    private fun tituloSeccion(titulo: String, reserva: Float): Bloque {
        val layout = texto(titulo, pintura(12f, ACENTO, negrita = true))
        val alto = 16f + layout.height + 4f + 8f

        return Bloque(alto, reservaSiguiente = reserva) { c, y ->
            c.dibujar(layout, MARGEN, y + 16f)

            val yLinea = y + 16f + layout.height + 4f
            c.drawLine(MARGEN, yLinea, MARGEN + ANCHO_UTIL, yLinea, pinturaLinea)
        }
    }

    private fun dato(etiqueta: String, valor: String): Bloque {
        val anchoEtiqueta = 130f
        val e = texto(etiqueta, pintura(9.5f, SECUNDARIO), anchoEtiqueta - 8f)
        val v = texto(valor, pintura(10.5f), ANCHO_UTIL - anchoEtiqueta)

        return Bloque(maxOf(e.height, v.height) + 5f) { c, y ->
            c.dibujar(e, MARGEN, y + 1f)
            c.dibujar(v, MARGEN + anchoEtiqueta, y)
        }
    }

    // La foto entra completa en un recuadro de 240 x 180 puntos, sin
    // deformarse. Con 800 px de ancho queda a unos 240 ppp impresa.
    private fun bloqueFoto(foto: Bitmap): Bloque {
        val escala = minOf(240f / foto.width, 180f / foto.height)
        val ancho = foto.width * escala
        val alto = foto.height * escala
        val pie = texto("Fotografía del equipo", pintura(8.5f, SECUNDARIO))

        val borde = Paint().apply {
            color = LINEA
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
        }

        return Bloque(10f + alto + 4f + pie.height + 4f) { c, y ->
            val recuadro = RectF(MARGEN, y + 10f, MARGEN + ancho, y + 10f + alto)
            c.drawBitmap(foto, null, recuadro, Paint(Paint.FILTER_BITMAP_FLAG))
            c.drawRect(recuadro, borde)
            c.dibujar(pie, MARGEN, y + 10f + alto + 4f)
        }
    }

    private fun filaMedicion(fila: FilaProtocolo, encabezado: Bloque): Bloque {
        val colorResultado = when (fila.resultado) {
            ResultadoMedicion.APROBADO -> APROBADO
            ResultadoMedicion.RECHAZADO -> RECHAZADO
            ResultadoMedicion.NO_EVALUADO -> SECUNDARIO
        }

        return filaTabla(
            celdas = listOf(
                fila.punto,
                fila.valor,
                fila.rango,
                fila.resultadoTexto,
                fila.anterior,
                fila.diferencia
            ),
            pinturas = listOf(
                pintura(9f, negrita = true),
                pintura(9f),
                pintura(9f, SECUNDARIO),
                pintura(9f, colorResultado, negrita = true),
                pintura(9f),
                pintura(9f)
            ),
            repetirAntes = encabezado
        )
    }

    private fun filaTabla(
        celdas: List<String>,
        pinturas: List<TextPaint>,
        fondo: Int? = null,
        repetirAntes: Bloque? = null
    ): Bloque {
        val layouts = celdas.mapIndexed { i, celda ->
            texto(celda, pinturas[i], COLUMNAS[i] - 2 * RELLENO)
        }
        val alto = layouts.maxOf { it.height } + 2 * RELLENO

        val pinturaFondo = fondo?.let { Paint().apply { color = it } }

        return Bloque(alto, repetirAntes = repetirAntes) { c, y ->
            if (pinturaFondo != null) {
                c.drawRect(MARGEN, y, MARGEN + ANCHO_UTIL, y + alto, pinturaFondo)
            }

            var x = MARGEN
            layouts.forEachIndexed { i, layout ->
                c.dibujar(layout, x + RELLENO, y + RELLENO)
                x += COLUMNAS[i]
            }

            c.drawLine(MARGEN, y + alto, MARGEN + ANCHO_UTIL, y + alto, pinturaLinea)
        }
    }

    private fun parrafo(
        contenido: String,
        pintura: TextPaint,
        espacioAntes: Float = 0f,
        espacioDespues: Float = 0f
    ): Bloque {
        val layout = texto(contenido, pintura)

        return Bloque(espacioAntes + layout.height + espacioDespues) { c, y ->
            c.dibujar(layout, MARGEN, y + espacioAntes)
        }
    }

    // Parte el texto en un bloque por línea. Cada bloque dibuja el
    // párrafo completo, pero recortado a su línea.
    private fun lineas(contenido: String, pintura: TextPaint): List<Bloque> {
        val layout = texto(contenido, pintura)

        return (0 until layout.lineCount).map { linea ->
            val arriba = layout.getLineTop(linea).toFloat()
            val alto = layout.getLineBottom(linea) - arriba

            Bloque(alto) { c, y ->
                c.withClip(MARGEN, y, MARGEN + ANCHO_UTIL, y + alto) {
                    translate(MARGEN, y - arriba)
                    layout.draw(this)
                }
            }
        }
    }

    private fun bloqueFirmas(tecnico: String): Bloque {
        val anchoFirma = (ANCHO_UTIL - 40f) / 2
        val pinturaEtiqueta = pintura(9f, SECUNDARIO)

        val izquierda = texto(
            if (tecnico.isNotBlank()) {
                "Técnico responsable\n$tecnico"
            } else {
                "Técnico responsable"
            },
            pinturaEtiqueta,
            anchoFirma
        )
        val derecha = texto(
            "Recepción del cliente\nNombre y firma",
            pinturaEtiqueta,
            anchoFirma
        )

        // Espacio en blanco sobre la línea para firmar a mano.
        val yLinea = 56f
        val alto = yLinea + 6f + maxOf(izquierda.height, derecha.height)

        return Bloque(alto) { c, y ->
            val xDerecha = MARGEN + anchoFirma + 40f

            c.drawLine(MARGEN, y + yLinea, MARGEN + anchoFirma, y + yLinea, pinturaLinea)
            c.drawLine(xDerecha, y + yLinea, xDerecha + anchoFirma, y + yLinea, pinturaLinea)

            c.dibujar(izquierda, MARGEN, y + yLinea + 6f)
            c.dibujar(derecha, xDerecha, y + yLinea + 6f)
        }
    }

    private fun dibujarCabecera(c: Canvas, contenido: ProtocoloContenido) {
        val empresa = pintura(9f, ACENTO, negrita = true)
        val numero = pintura(9f, SECUNDARIO)
        val base = MARGEN + 10f

        c.drawText("INDUZTEK INGENIERÍA SpA", MARGEN, base, empresa)

        val textoNumero = "Protocolo ${contenido.numero}"
        c.drawText(
            textoNumero,
            MARGEN + ANCHO_UTIL - numero.measureText(textoNumero),
            base,
            numero
        )

        c.drawLine(MARGEN, base + 8f, MARGEN + ANCHO_UTIL, base + 8f, pinturaLinea)
    }

    private fun dibujarPie(
        c: Canvas,
        contenido: ProtocoloContenido,
        pagina: Int,
        total: Int
    ) {
        val pie = pintura(8f, SECUNDARIO)
        val base = ALTO - MARGEN

        c.drawLine(MARGEN, base - 14f, MARGEN + ANCHO_UTIL, base - 14f, pinturaLinea)
        c.drawText(contenido.generado, MARGEN, base, pie)

        val textoPagina = "Página $pagina de $total"
        c.drawText(
            textoPagina,
            MARGEN + ANCHO_UTIL - pie.measureText(textoPagina),
            base,
            pie
        )
    }

    private fun pintura(
        tamano: Float,
        color: Int = TEXTO,
        negrita: Boolean = false
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = tamano
            this.color = color
            typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    // Texto con saltos de línea automáticos dentro del ancho dado.
    private fun texto(
        contenido: String,
        pintura: TextPaint,
        ancho: Float = ANCHO_UTIL
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(contenido, 0, contenido.length, pintura, ancho.toInt())
            .setIncludePad(false)
            .build()
    }

    private fun Canvas.dibujar(layout: StaticLayout, x: Float, y: Float) {
        withTranslation(x, y) {
            layout.draw(this)
        }
    }
}
