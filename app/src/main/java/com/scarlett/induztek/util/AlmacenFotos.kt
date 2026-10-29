package com.scarlett.induztek.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/*
 * Acceso a los archivos de las fotografías de equipos.
 *
 * Guardar y leer estaba copiado en varias pantallas; ahora la carpeta,
 * el nombre del archivo, el tamaño y la calidad del JPEG se deciden en
 * un solo lugar.
 *
 * Es un object y no un repositorio porque no toca la base de datos: la
 * entidad Equipo solo guarda la ruta del archivo.
 */
object AlmacenFotos {

    const val CARPETA = "evidencias"
    private const val CALIDAD_JPEG = 90

    // Lado mayor de la foto guardada. Full HD alcanza para leer la placa
    // de características de un equipo. Además, una foto de 12 MP (4032
    // px) se decodifica a la mitad, sin pasar por los 48 MB que ocupa
    // completa en memoria.
    const val LADO_GUARDADO = 1920

    // Tamaños para mostrar. La lista dibuja miniaturas de 64 dp: no
    // tiene sentido decodificar la foto completa para cada fila.
    const val LADO_MINIATURA = 256
    const val LADO_PANTALLA = 1080
    const val LADO_PDF = 800

    fun carpeta(context: Context): File {
        return File(context.filesDir, CARPETA)
    }

    /*
     * Crea el archivo vacío donde la app de cámara escribirá la foto.
     * TakePicture no devuelve la imagen: la escribe en la Uri que se le
     * entrega, y esa Uri sale de este archivo vía ProveedorArchivos.
     */
    fun crearArchivo(context: Context): File {
        val carpeta = carpeta(context)

        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw IllegalStateException(
                "No se pudo crear la carpeta de evidencias."
            )
        }

        val archivo = File(
            carpeta,
            "equipo_${System.currentTimeMillis()}.jpg"
        )

        archivo.createNewFile()
        return archivo
    }

    /*
     * Deja la foto que escribió la cámara lista para guardar: reducida
     * a LADO_GUARDADO y girada según su EXIF.
     *
     * Muchas cámaras no giran los píxeles al sacar la foto en vertical:
     * la guardan acostada y anotan la orientación en el EXIF.
     * BitmapFactory ignora ese dato, así que sin este paso la foto se
     * vería de lado. Al girar los píxeles y reescribir el archivo, el
     * resto de la app (la lista, el detalle, el PDF) la lee derecha sin
     * saber nada de EXIF.
     *
     * Devuelve false si el archivo está vacío o no es una imagen, por
     * ejemplo si la cámara se cerró sin guardar.
     */
    fun normalizar(archivo: File): Boolean {
        val rotacion = rotacionExif { archivo.inputStream() }

        val bitmap = decodificar(LADO_GUARDADO) { archivo.inputStream() }
            ?: return false

        escribir(ajustar(bitmap, LADO_GUARDADO, rotacion), archivo)
        return true
    }

    /*
     * Copia una imagen de la galería a la carpeta de evidencias, con el
     * mismo tratamiento que una foto de la cámara. Devuelve la ruta, que
     * es lo que se guarda en Equipo.fotoUri.
     */
    fun importar(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val rotacion = rotacionExif { resolver.openInputStream(uri) }

        val bitmap = decodificar(LADO_GUARDADO) {
            resolver.openInputStream(uri)
        } ?: throw IllegalArgumentException(
            "El archivo elegido no es una imagen."
        )

        val archivo = crearArchivo(context)

        try {
            escribir(ajustar(bitmap, LADO_GUARDADO, rotacion), archivo)
        } catch (e: Exception) {
            archivo.delete()
            throw e
        }

        return archivo.absolutePath
    }

    /*
     * Lee la foto de una ruta guardada, reducida a ladoMaximo. Devuelve
     * null si la ruta es null, si el archivo ya no está o si no se puede
     * decodificar, para que la pantalla muestre el ícono por defecto en
     * vez de caerse.
     */
    fun leer(ruta: String?, ladoMaximo: Int = LADO_PANTALLA): Bitmap? {
        if (ruta == null) {
            return null
        }

        val archivo = File(ruta)

        if (!archivo.exists()) {
            return null
        }

        return try {
            decodificar(ladoMaximo) { archivo.inputStream() }
        } catch (_: Exception) {
            null
        }
    }

    /*
     * Borra una foto guardada. Solo toca archivos de la carpeta de
     * evidencias: si fotoUri trajera otra cosa (un dato viejo o
     * corrupto), no se borra nada. Devuelve true si borró el archivo.
     */
    fun borrar(ruta: String?): Boolean {
        if (ruta.isNullOrBlank()) {
            return false
        }

        val archivo = File(ruta)

        if (archivo.parentFile?.name != CARPETA) {
            return false
        }

        return archivo.delete()
    }

    /*
     * Borra las fotos de la carpeta que ningún equipo usa, y devuelve
     * cuántas borró.
     *
     * Quedan huérfanas cuando se sale del formulario con el botón atrás
     * en vez de Cancelar, o cuando la app se cierra con una foto tomada
     * y el equipo sin guardar. Por eso se llama al abrir la app, antes
     * de que haya un formulario abierto: si se llamara con un formulario
     * en pantalla, borraría la foto que todavía no se guardó.
     */
    fun limpiarHuerfanas(carpeta: File, enUso: Collection<String>): Int {
        val usadas = enUso.map { rutaComparable(File(it)) }.toSet()
        val archivos = carpeta.listFiles() ?: return 0

        return archivos.count { archivo ->
            archivo.isFile &&
                    rutaComparable(archivo) !in usadas &&
                    archivo.delete()
        }
    }

    /*
     * Cuánto dividir la imagen al decodificarla. BitmapFactory solo
     * divide por potencias de dos; se elige la mayor que deja el lado
     * mayor en ladoMaximo o más, para no perder nitidez.
     */
    fun calcularMuestreo(ancho: Int, alto: Int, ladoMaximo: Int): Int {
        val ladoMayor = maxOf(ancho, alto)
        var muestreo = 1

        while (ladoMayor / (muestreo * 2) >= ladoMaximo) {
            muestreo *= 2
        }

        return muestreo
    }

    // Dos lecturas: la primera solo mide la imagen, sin cargarla, para
    // decidir cuánto reducirla en la segunda.
    private fun decodificar(
        ladoMaximo: Int,
        abrir: () -> InputStream?
    ): Bitmap? {
        val limites = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        abrir()?.use { BitmapFactory.decodeStream(it, null, limites) }

        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            return null
        }

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularMuestreo(
                limites.outWidth,
                limites.outHeight,
                ladoMaximo
            )
        }

        return abrir()?.use { BitmapFactory.decodeStream(it, null, opciones) }
    }

    private fun rotacionExif(abrir: () -> InputStream?): Int {
        return try {
            abrir()?.use { ExifInterface(it).rotationDegrees } ?: 0
        } catch (_: IOException) {
            0
        }
    }

    // Lleva el lado mayor exacto a ladoMaximo (inSampleSize solo llega
    // a una potencia de dos) y gira los píxeles según el EXIF.
    private fun ajustar(
        bitmap: Bitmap,
        ladoMaximo: Int,
        rotacion: Int
    ): Bitmap {
        val ladoMayor = maxOf(bitmap.width, bitmap.height)
        val escala = if (ladoMayor > ladoMaximo) {
            ladoMaximo.toFloat() / ladoMayor
        } else {
            1f
        }

        if (escala == 1f && rotacion == 0) {
            return bitmap
        }

        val matriz = Matrix().apply {
            postScale(escala, escala)
            postRotate(rotacion.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matriz,
            true
        )
    }

    private fun escribir(bitmap: Bitmap, archivo: File) {
        FileOutputStream(archivo).use { salida ->
            val escrito = bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                CALIDAD_JPEG,
                salida
            )

            if (!escrito) {
                throw IllegalStateException(
                    "No se pudo guardar la fotografía."
                )
            }
        }
    }

    // La misma carpeta puede aparecer como /data/data/... o como
    // /data/user/0/... según la versión de Android: se comparan las
    // rutas resueltas para no tomar una foto en uso por huérfana.
    private fun rutaComparable(archivo: File): String {
        return try {
            archivo.canonicalPath
        } catch (_: IOException) {
            archivo.absolutePath
        }
    }
}
