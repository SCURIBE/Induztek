
package com.scarlett.induztek.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object AlmacenFotos {

    const val CARPETA = "evidencias"

    const val LADO_GUARDADO = 1920
    const val LADO_MINIATURA = 256
    const val LADO_PANTALLA = 1080
    const val LADO_PDF = 800

    // Obtener la carpeta de fotografías
    fun carpeta(context: Context): File {
        return File(context.filesDir, CARPETA)
    }

    // Crear un archivo para guardar la fotografía
    fun crearArchivo(context: Context): File {
        val carpeta = carpeta(context)

        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }

        val archivo = File(
            carpeta,
            "equipo_${System.currentTimeMillis()}.jpg"
        )

        archivo.createNewFile()

        return archivo
    }

    // Comprobar que la fotografía se haya guardado correctamente
    fun normalizar(archivo: File): Boolean {
        if (!archivo.exists() || archivo.length() == 0L) {
            return false
        }

        val opciones = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(archivo.absolutePath, opciones)

        return opciones.outWidth > 0 && opciones.outHeight > 0
    }

    // Importar una fotografía desde la galería
    fun importar(context: Context, uri: Uri): String {

        val archivo = crearArchivo(context)

        try {
            context.contentResolver.openInputStream(uri).use { entrada ->

                if (entrada == null) {
                    throw IllegalArgumentException("No se pudo abrir la imagen.")
                }

                FileOutputStream(archivo).use { salida ->
                    entrada.copyTo(salida)
                }
            }

            return archivo.absolutePath

        } catch (e: Exception) {
            archivo.delete()
            throw e
        }
    }

    // Leer una fotografía guardada
    fun leer(
        ruta: String?,
        ladoMaximo: Int = LADO_PANTALLA
    ): Bitmap? {

        if (ruta == null) {
            return null
        }

        val archivo = File(ruta)

        if (!archivo.exists()) {
            return null
        }

        return try {

            val opciones = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(
                archivo.absolutePath,
                opciones
            )

            opciones.inSampleSize = calcularMuestreo(
                opciones.outWidth,
                opciones.outHeight,
                ladoMaximo
            )

            opciones.inJustDecodeBounds = false

            BitmapFactory.decodeFile(
                archivo.absolutePath,
                opciones
            )

        } catch (e: Exception) {
            null
        }
    }

    // Calcular cuánto reducir la imagen al leerla
    fun calcularMuestreo(
        ancho: Int,
        alto: Int,
        ladoMaximo: Int
    ): Int {

        var muestra = 1
        val ladoMayor = maxOf(ancho, alto)

        while (ladoMayor / (muestra * 2) >= ladoMaximo) {
            muestra *= 2
        }

        return muestra
    }

    // Eliminar una fotografía
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

    // Eliminar fotografías que no están asociadas a equipos
    fun limpiarHuerfanas(
        carpeta: File,
        enUso: Collection<String>
    ): Int {

        val archivos = carpeta.listFiles() ?: return 0

        val usadas = enUso.map {
            File(it).absolutePath
        }.toSet()

        var eliminadas = 0

        for (archivo in archivos) {
            if (archivo.isFile &&
                archivo.absolutePath !in usadas
            ) {
                if (archivo.delete()) {
                    eliminadas++
                }
            }
        }

        return eliminadas
    }
}
