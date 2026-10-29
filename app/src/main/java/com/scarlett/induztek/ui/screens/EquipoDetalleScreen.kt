package com.scarlett.induztek.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.PruebaConMediciones
import com.scarlett.induztek.ui.componentes.TarjetaPrueba
import com.scarlett.induztek.util.AlmacenFotos

@Composable
fun EquipoDetalleScreen(
    equipo: Equipo,
    // Las pruebas de este equipo, ya filtradas. Sirven para el
    // historial de abajo y para avisar en el diálogo de eliminación
    // cuántas se van a borrar en cascada.
    pruebas: List<PruebaConMediciones>,
    alVolver: () -> Unit,
    alEditar: (Equipo) -> Unit,
    alEliminar: (Equipo) -> Unit,
    alVerPrueba: (Int) -> Unit
) {
    val cantidadPruebas = pruebas.size
    var confirmarEliminacion by remember {
        mutableStateOf(false)
    }

    val bitmap = remember(equipo.fotoUri) {
        AlmacenFotos.leer(equipo.fotoUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = alVolver) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
            )
            Text(" Volver a equipos")
        }

        Text(
            text = "Detalle del equipo",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Fotografía del equipo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Sin fotografía disponible",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Código del equipo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = equipo.codigo,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Tipo de equipo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = equipo.tipo.etiqueta,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Identificador interno: ${equipo.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = { alEditar(equipo) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null
            )
            Text("  Editar equipo")
        }

        OutlinedButton(
            onClick = { confirmarEliminacion = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "  Eliminar equipo",
                color = MaterialTheme.colorScheme.error
            )
        }

        HorizontalDivider()

        Text(
            text = "Historial de este equipo",
            style = MaterialTheme.typography.titleLarge
        )

        if (pruebas.isEmpty()) {
            Text(
                text = "Todavía no hay pruebas registradas para este " +
                        "equipo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // forEach y no LazyColumn: esta columna ya tiene scroll, y
            // anidar una lista perezosa dentro de un scroll no está
            // permitido en Compose.
            pruebas.forEach { conMediciones ->
                TarjetaPrueba(
                    conMediciones = conMediciones,
                    todas = pruebas,
                    alAbrir = {
                        alVerPrueba(conMediciones.prueba.id)
                    }
                )
            }
        }
    }

    if (confirmarEliminacion) {
        AlertDialog(
            onDismissRequest = {
                confirmarEliminacion = false
            },
            title = {
                Text("Eliminar equipo")
            },
            text = {
                Text(
                    buildString {
                        append("¿Seguro que deseas eliminar el equipo ")
                        append(equipo.codigo)
                        append("? ")

                        if (cantidadPruebas > 0) {
                            append("Este equipo tiene ")
                            append(cantidadPruebas)
                            append(
                                if (cantidadPruebas == 1) {
                                    " prueba registrada, que también se "
                                } else {
                                    " pruebas registradas, que también se "
                                }
                            )
                            append("eliminará")
                            if (cantidadPruebas > 1) append("n")
                            append(". ")
                        }

                        append("Esta acción no se puede deshacer.")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                        alEliminar(equipo)
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmarEliminacion = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
