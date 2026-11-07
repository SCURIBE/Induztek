
package com.scarlett.induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scarlett.induztek.Repository.model.TipoEquipo
import com.scarlett.induztek.ui.componentes.CampoSeleccion
import com.scarlett.induztek.ui.componentes.SelectorFoto
import com.scarlett.induztek.util.AlmacenFotos

@Composable
fun NuevoEquipoScreen(
    errorExterno: String?,
    alGuardar: (String, TipoEquipo, String?, (Boolean) -> Unit) -> Unit,
    alCancelar: () -> Unit
) {
    var codigo by rememberSaveable {
        mutableStateOf("")
    }

    var tipo by rememberSaveable {
        mutableStateOf<TipoEquipo?>(null)
    }

    var fotoUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var errorLocal by remember {
        mutableStateOf<String?>(null)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    val mensajeError = errorExterno ?: errorLocal

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
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = codigo,
                    onValueChange = {
                        codigo = it
                        errorLocal = null
                    },
                    label = {
                        Text("Código del equipo")
                    },
                    placeholder = {
                        Text("Ej. EQ-001")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = mensajeError != null && codigo.isBlank(),
                    enabled = !guardando
                )

                CampoSeleccion(
                    etiqueta = "Tipo de equipo",
                    textoVacio = "Selecciona el tipo de equipo",
                    opciones = TipoEquipo.entries,
                    seleccion = tipo,
                    textoDe = { it.etiqueta },
                    alSeleccionar = {
                        tipo = it
                        errorLocal = null
                    },
                    esInvalido = mensajeError != null && tipo == null,
                    mensajeError = "Selecciona el tipo de equipo.",
                    habilitado = !guardando
                )

                Text(
                    text = "Evidencia fotográfica (opcional)",
                    style = MaterialTheme.typography.titleMedium
                )

                SelectorFoto(
                    fotoUri = fotoUri,
                    alCambiar = { nueva ->
                        AlmacenFotos.borrar(fotoUri)
                        fotoUri = nueva
                        errorLocal = null
                    },
                    alError = {
                        errorLocal = it
                    },
                    habilitado = !guardando
                )

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

                        val tipoElegido = tipo

                        if (codigo.isBlank() || tipoElegido == null) {
                            errorLocal = "Completa todos los campos."
                        } else {
                            guardando = true

                            alGuardar(
                                codigo.trim(),
                                tipoElegido,
                                fotoUri
                            ) { guardado ->
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
                    onClick = {
                        AlmacenFotos.borrar(fotoUri)
                        alCancelar()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}
