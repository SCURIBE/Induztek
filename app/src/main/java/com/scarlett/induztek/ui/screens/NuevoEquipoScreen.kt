package com.scarlett.induztek.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@Composable
fun NuevoEquipoScreen(
    errorExterno: String?,
    alGuardar: (String, String, String?, (Boolean) -> Unit) -> Unit,
    alCancelar: () -> Unit
) {
    val context = LocalContext.current

    var codigo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var fotoUri by remember { mutableStateOf<String?>(null) }
    var foto by remember { mutableStateOf<Bitmap?>(null) }

    val mensajeError = errorExterno ?: errorLocal

    fun guardarBitmap(bitmap: Bitmap): String {
        val carpeta = File(context.filesDir, "evidencias")

        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw IllegalStateException("No se pudo crear la carpeta de evidencias.")
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
            } catch (e: Exception) {
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
            } catch (e: Exception) {
                errorLocal = "No se pudo guardar la fotografía."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Registrar equipo",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "Ingresa los datos del equipo eléctrico.",
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
                    placeholder = { Text("Ej. EQ-001") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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
                    placeholder = { Text("Ej. Transformador") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    isError = mensajeError != null && tipo.isBlank(),
                    enabled = !guardando
                )

                Text(
                    text = "Evidencia fotográfica (opcional)",
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

                    Text(
                        text = "Fotografía seleccionada",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedButton(
                    onClick = { camara.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null
                    )
                    Text("  Tomar fotografía")
                }

                OutlinedButton(
                    onClick = { galeria.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null
                    )
                    Text("  Elegir de galería")
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

                            alGuardar(codigo, tipo, fotoUri) { guardado ->
                                guardando = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando
                ) {
                    Text(
                        if (guardando) "Guardando..." else "Guardar equipo"
                    )
                }

                OutlinedButton(
                    onClick = alCancelar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}
