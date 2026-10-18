
package com.scarlett.induztek.ui.theme.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NuevoEquipoScreen(
    errorExterno: String?,
    alGuardar: (String, String, (Boolean) -> Unit) -> Unit,
    alCancelar: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

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

                mensajeError?.let {
                    Text(
                        text = it,
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
                            alGuardar(codigo, tipo) { guardado ->
                                guardando = false
                                if (!guardado) {
                                    // El error lo proporciona el ViewModel.
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando
                ) {
                    Text(if (guardando) "Guardando..." else "Guardar equipo")
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
