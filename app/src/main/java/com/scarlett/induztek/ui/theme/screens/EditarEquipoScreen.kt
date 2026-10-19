
package com.scarlett.induztek.ui.theme.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scarlett.induztek.model.Equipo
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditarEquipoScreen(
    equipo: Equipo,
    errorExterno: String?,
    alGuardar: (Equipo, (Boolean) -> Unit) -> Unit,
    alCancelar: () -> Unit
) {
    val context = LocalContext.current

    var codigo by remember(equipo.id) {
        mutableStateOf(equipo.codigo)
    }

    var tipo by remember(equipo.id) {
        mutableStateOf(equipo.tipo)
    }

    var fotoUri by remember(equipo.id) {
        mutableStateOf(equipo.fotoUri)
    }

    var foto by remember(equipo.id) {
        mutableStateOf(
            equipo.fotoUri?.let { ruta ->
                try {
                    BitmapFactory.decodeFile(ruta)
                } catch (_: Exception) {
                    null
                }
            }
        )
    }

    var errorLocal by remember {
        mutableStateOf<String?>(null)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    val mensajeError = errorExterno ?: errorLocal

    fun guardarBitmap(bitmap: Bitmap): String {
        val carpeta = File(context.filesDir, "evidencias")

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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, salida)
        }

        return archivo.absolutePath
    }

    val galeria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = context.contentResolver
                    .openInputStream(uri)
                    ?.use { BitmapFactory.decodeStream(it) }

                if (bitmap != null) {
                    fotoUri = guardarBitmap(bitmap)
                    foto = bitmap
                    errorLocal = null
                } else {
                    errorLocal = "No se pudo cargar la imagen."
                }
            } catch (_: Exception) {
                errorLocal = "No se pudo abrir la imagen seleccionada."
            }
        }
    }

    val camara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                fotoUri = guardarBitmap(bitmap)
                foto = bitmap
                errorLocal = null
            } catch (_: Exception) {
                errorLocal = "No se pudo guardar la fotografía."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Editar equipo",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Modifica los datos del equipo seleccionado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = codigo,
            onValueChange = {
                codigo = it
                errorLocal = null
            },
            label = { Text("Código del equipo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = mensajeError != null && codigo.isBlank(),
            enabled = !guardando
        )

        OutlinedTextField(
            value = tipo,
            onValueChange = {
                tipo = it
                errorLocal = null
            },
            label = { Text("Tipo de equipo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = mensajeError != null && tipo.isBlank(),
            enabled = !guardando
        )

        Text(
            text = "Fotografía del equipo",
            style = MaterialTheme.typography.titleMedium
        )

        if (foto != null) {
            Image(
                bitmap = foto!!.asImageBitmap(),
                contentDescription = "Fotografía del equipo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        } else {
            Text("Este equipo no tiene fotografía.")
        }

        OutlinedButton(
            onClick = { camara.launch(null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.CameraAlt,
                contentDescription = null
            )
            Text("  Tomar nueva fotografía")
        }

        OutlinedButton(
            onClick = { galeria.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.Image,
                contentDescription = null
            )
            Text("  Cambiar desde galería")
        }

        mensajeError?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                errorLocal = null

                if (codigo.isBlank() || tipo.isBlank()) {
                    errorLocal = "Completa todos los campos."
                } else {
                    guardando = true

                    val equipoActualizado = equipo.copy(
                        codigo = codigo.trim(),
                        tipo = tipo.trim(),
                        fotoUri = fotoUri
                    )

                    alGuardar(equipoActualizado) { guardado ->
                        guardando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            Text(
                if (guardando) "Guardando..." else "Guardar cambios"
            )
        }

        OutlinedButton(
            onClick = alCancelar,
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Cancelar")
        }
    }
}
