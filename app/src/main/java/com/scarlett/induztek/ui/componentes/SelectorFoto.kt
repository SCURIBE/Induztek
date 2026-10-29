package com.scarlett.induztek.ui.componentes

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scarlett.induztek.util.AlmacenFotos
import com.scarlett.induztek.util.ProveedorArchivos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/*
 * Foto del equipo con sus dos botones, cámara y galería. Estaba copiado
 * en NuevoEquipoScreen y EditarEquipoScreen.
 *
 * Entrega la ruta de la foto ya guardada en evidencias; qué hacer con
 * la foto anterior lo decide la pantalla, porque solo ella sabe si esa
 * foto era la original del equipo o una tomada en este mismo formulario.
 */
@Composable
fun SelectorFoto(
    fotoUri: String?,
    alCambiar: (String) -> Unit,
    alError: (String) -> Unit,
    habilitado: Boolean,
    textoCamara: String = "Tomar fotografía",
    textoGaleria: String = "Elegir de galería",
    textoSinFoto: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Ruta del archivo que la cámara está llenando. rememberSaveable y
    // no remember: mientras la cámara está abierta, Android puede
    // destruir esta pantalla (por memoria, o al girar el teléfono), y al
    // volver hay que saber dónde quedó la foto.
    var rutaCaptura by rememberSaveable { mutableStateOf<String?>(null) }
    var procesando by remember { mutableStateOf(false) }

    // La foto se decodifica fuera del hilo principal: una de 1920 px
    // tarda lo suficiente para trabar la pantalla.
    val foto by produceState<Bitmap?>(initialValue = null, fotoUri) {
        value = withContext(Dispatchers.IO) {
            AlmacenFotos.leer(fotoUri, AlmacenFotos.LADO_PANTALLA)
        }
    }

    // TakePicture escribe la foto en resolución completa en la Uri que
    // recibe. TakePicturePreview, el que se usaba antes, solo devolvía
    // una miniatura de pocos cientos de píxeles: no servía como
    // evidencia del estado del equipo.
    val camara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { exito ->
        val ruta = rutaCaptura ?: return@rememberLauncherForActivityResult
        rutaCaptura = null

        scope.launch {
            procesando = true

            val lista = withContext(Dispatchers.IO) {
                try {
                    exito && AlmacenFotos.normalizar(File(ruta))
                } catch (_: Exception) {
                    false
                }
            }

            procesando = false

            if (lista) {
                alCambiar(ruta)
            } else {
                // Si se canceló la cámara, el archivo quedó vacío.
                AlmacenFotos.borrar(ruta)

                if (exito) {
                    alError("No se pudo guardar la fotografía.")
                }
            }
        }
    }

    val galeria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            procesando = true

            val ruta = withContext(Dispatchers.IO) {
                try {
                    AlmacenFotos.importar(context, uri)
                } catch (_: Exception) {
                    null
                }
            }

            procesando = false

            if (ruta != null) {
                alCambiar(ruta)
            } else {
                alError("No se pudo abrir la imagen seleccionada.")
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val actual = foto

        when {
            procesando -> CircularProgressIndicator()

            actual != null -> Image(
                bitmap = actual.asImageBitmap(),
                contentDescription = "Fotografía del equipo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            textoSinFoto != null -> Text(
                text = textoSinFoto,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        OutlinedButton(
            onClick = {
                var archivo: File? = null

                try {
                    archivo = AlmacenFotos.crearArchivo(context)
                    rutaCaptura = archivo.absolutePath
                    camara.launch(ProveedorArchivos.uriPara(context, archivo))
                } catch (_: ActivityNotFoundException) {
                    AlmacenFotos.borrar(archivo?.absolutePath)
                    rutaCaptura = null
                    alError("No hay una app de cámara disponible.")
                } catch (_: Exception) {
                    AlmacenFotos.borrar(archivo?.absolutePath)
                    rutaCaptura = null
                    alError("No se pudo preparar la cámara.")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado && !procesando
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null
            )
            Text("  $textoCamara")
        }

        OutlinedButton(
            onClick = { galeria.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = habilitado && !procesando
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null
            )
            Text("  $textoGaleria")
        }
    }
}
