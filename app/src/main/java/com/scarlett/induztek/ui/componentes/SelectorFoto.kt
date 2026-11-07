
package com.scarlett.induztek.ui.componentes

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
import androidx.compose.material.icons.filled.Image as ImageIcon
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    var mostrarCamara by rememberSaveable {
        mutableStateOf(false)
    }

    var procesando by remember {
        mutableStateOf(false)
    }

    val foto by produceState<Bitmap?>(
        initialValue = null,
        fotoUri
    ) {
        value = withContext(Dispatchers.IO) {
            AlmacenFotos.leer(fotoUri)
        }
    }

    val galeria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            procesando = true

            scope.launch {
                val ruta = withContext(Dispatchers.IO) {
                    try {
                        AlmacenFotos.importar(context, uri)
                    } catch (e: Exception) {
                        null
                    }
                }

                procesando = false

                if (ruta != null) {
                    alCambiar(ruta)
                } else {
                    alError("No se pudo seleccionar la fotografía.")
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mostrarCamara) {
            CamaraPreview(
                alFotoTomada = { ruta ->
                    mostrarCamara = false
                    alCambiar(ruta)
                },
                alCancelar = {
                    mostrarCamara = false
                },
                alError = { mensaje ->
                    alError(mensaje)
                }
            )
        } else {
            when {
                procesando -> {
                    CircularProgressIndicator()
                }

                foto != null -> {
                    Image(
                        bitmap = foto!!.asImageBitmap(),
                        contentDescription = "Fotografía del equipo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                textoSinFoto != null -> {
                    Text(
                        text = textoSinFoto,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    mostrarCamara = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = habilitado && !procesando
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null
                )
                Text(" $textoCamara")
            }

            OutlinedButton(
                onClick = {
                    galeria.launch("image/*")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = habilitado && !procesando
            ) {
                Icon(
                    imageVector = Icons.Default.ImageIcon,
                    contentDescription = null
                )
                Text(" $textoGaleria")
            }
        }
    }
}
