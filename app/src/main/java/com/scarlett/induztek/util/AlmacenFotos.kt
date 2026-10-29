package com.scarlett.induztek.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/*
 * Acceso a los archivos de las fotografías de equipos.
 *
 * Guardar y leer estaba copiado en varias pantallas: guardarBitmap en
 * NuevoEquipoScreen y EditarEquipoScreen, y decodeFile con su try/catch
 * en esas dos más EquiposScreen y EquipoDetalleScreen. Ahora la ruta,
 * el nombre del archivo y la calidad del JPEG se deciden en un solo
 * lugar.
 *
 * Es un object y no un repositorio porque no toca la base de datos: la
 * entidad Equipo solo guarda la ruta del archivo.
 */
object AlmacenFotos {

    private const val CARPETA = "evidencias"
    private const val CALIDAD_JPEG = 90

    /*
     * Guarda el bitmap como JPEG en el almacenamiento interno de la app
     * y devuelve la ruta absoluta, que es lo que se guarda en
     * Equipo.fotoUri.
     *
     * Lanza IllegalStateException si no puede crear la carpeta, igual
     * que antes: quien llama ya atrapa la excepción para mostrar un
     * mensaje.
     */
    fun guardar(context: Context, bitmap: Bitmap): String {
        val carpeta = File(context.filesDir, CARPETA)

        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw IllegalStateException(
                "No se pudo crear la carpeta de evidencias."
            )
        }

        val archivo = File(
            carpeta,
            "equipo_${System.currentTimeMillis()}.jpg"
        )

        FileOutputStream(archivo).use { salida ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                CALIDAD_JPEG,
                salida
            )
        }

        return archivo.absolutePath
    }

    /*
     * Lee la foto de una ruta guardada. Devuelve null si la ruta es
     * null, si el archivo ya no está o si no se puede decodificar, para
     * que la pantalla muestre el ícono por defecto en vez de caerse.
     */
    fun leer(ruta: String?): Bitmap? {
        if (ruta == null) {
            return null
        }

        return try {
            BitmapFactory.decodeFile(ruta)
        } catch (_: Exception) {
            null
        }
    }
}
