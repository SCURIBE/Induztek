
package com.scarlett.induztek.ui.componentes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.scarlett.induztek.util.AlmacenFotos
import java.util.concurrent.Executor

@Composable
fun CamaraPreview(
    alFotoTomada: (String) -> Unit,
    alCancelar: () -> Unit,
    alError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor: Executor = ContextCompat.getMainExecutor(context)

    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var usarFrontal by remember {
        mutableStateOf(false)
    }

    var imageCapture by remember {
        mutableStateOf<ImageCapture?>(null)
    }

    var cameraProvider by remember {
        mutableStateOf<ProcessCameraProvider?>(null)
    }

    var camaraPreparada by remember {
        mutableStateOf(false)
    }

    val vistaCamara = remember {
        PreviewView(context)
    }

    val permisoCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        tienePermiso = concedido

        if (!concedido) {
            alError("Se necesita permiso para utilizar la cámara.")
        }
    }

    // Solicitar permiso automáticamente al abrir la cámara.
    LaunchedEffect(Unit) {
        if (!tienePermiso) {
            permisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    // Preparar o cambiar la cámara.
    LaunchedEffect(tienePermiso, usarFrontal) {
        if (tienePermiso) {
            camaraPreparada = false
            imageCapture = null

            try {
                val futuro = ProcessCameraProvider.getInstance(context)

                futuro.addListener({
                    try {
                        val proveedor = futuro.get()
                        cameraProvider = proveedor

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider =
                                    vistaCamara.surfaceProvider
                            }

                        val captura = ImageCapture.Builder()
                            .setCaptureMode(
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            .build()

                        val selector = if (usarFrontal) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        proveedor.unbindAll()

                        proveedor.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            captura
                        )

                        imageCapture = captura
                        camaraPreparada = true

                    } catch (e: Exception) {
                        camaraPreparada = false
                        alError("No se pudo iniciar la cámara.")
                    }
                }, executor)

            } catch (e: Exception) {
                alError("No se pudo preparar la cámara.")
            }
        }
    }

    // Liberar la cámara al cerrar la pantalla.
    DisposableEffect(cameraProvider) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Dialog(
        onDismissRequest = alCancelar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (tienePermiso) {
                AndroidView(
                    factory = { vistaCamara },
                    modifier = Modifier.fillMaxSize()
                )

                // Controles superiores.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = alCancelar
                    ) {
                        Text("Cancelar")
                    }

                    OutlinedButton(
                        onClick = {
                            usarFrontal = !usarFrontal
                        },
                        enabled = camaraPreparada
                    ) {
                        Text("Voltear cámara")
                    }
                }

                // Botón de captura.
                Button(
                    onClick = {
                        val captura = imageCapture

                        if (captura == null) {
                            alError("La cámara todavía no está preparada.")
                        } else {
                            try {
                                val archivo =
                                    AlmacenFotos.crearArchivo(context)

                                val opciones =
                                    ImageCapture.OutputFileOptions
                                        .Builder(archivo)
                                        .build()

                                captura.takePicture(
                                    opciones,
                                    executor,
                                    object : ImageCapture.OnImageSavedCallback {

                                        override fun onImageSaved(
                                            resultado: ImageCapture.OutputFileResults
                                        ) {
                                            alFotoTomada(
                                                archivo.absolutePath
                                            )
                                        }

                                        override fun onError(
                                            excepcion: ImageCaptureException
                                        ) {
                                            AlmacenFotos.borrar(
                                                archivo.absolutePath
                                            )

                                            alError(
                                                "No se pudo tomar la fotografía."
                                            )
                                        }
                                    }
                                )

                            } catch (e: Exception) {
                                alError(
                                    "No se pudo guardar la fotografía."
                                )
                            }
                        }
                    },
                    enabled = camaraPreparada,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 35.dp)
                ) {
                    Text("Tomar fotografía")
                }

                if (!camaraPreparada) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Se necesita permiso para utilizar la cámara.",
                        color = Color.White
                    )

                    Button(
                        onClick = {
                            permisoCamara.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text("Intentar de nuevo")
                    }

                    OutlinedButton(onClick = alCancelar) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}
