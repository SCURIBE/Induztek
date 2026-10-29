package com.scarlett.induztek.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/*
 * Convierte un archivo propio en una Uri content:// que otra app puede
 * abrir. Otra app no puede leer una ruta de filesDir o cacheDir: no
 * tiene permiso. La Uri sí, mientras dure el permiso que se le otorga.
 *
 * La usan la cámara (para escribir la foto) y el botón de compartir el
 * protocolo (para leer el PDF).
 */
object ProveedorArchivos {

    // Tiene que coincidir con android:authorities del <provider> en el
    // manifest, que se arma con el applicationId.
    private fun autoridad(context: Context): String {
        return "${context.packageName}.fileprovider"
    }

    fun uriPara(context: Context, archivo: File): Uri {
        return FileProvider.getUriForFile(
            context,
            autoridad(context),
            archivo
        )
    }
}
