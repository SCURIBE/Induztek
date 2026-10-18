
package com.scarlett.induztek.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory

@Composable
fun EquiposScreen() {
    val context = LocalContext.current
    val repository = RepositoryProvider.obtenerEquipoRepository(context)
    val factory = EquipoViewModelFactory(repository)
    val viewModel: EquipoViewModel = viewModel(factory = factory)

    val equipos by viewModel.equipos.collectAsState()
    val error by viewModel.error.collectAsState()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var equipoAEliminar by remember { mutableStateOf<Equipo?>(null) }

    if (equipoAEliminar != null) {
        AlertDialog(
            onDismissRequest = {
                equipoAEliminar = null
            },
            title = {
                Text("Eliminar equipo")
            },
            text = {
                Text(
                    "¿Seguro que deseas eliminar el equipo " +
                            "${equipoAEliminar?.codigo}? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        equipoAEliminar?.let { equipo ->
                            viewModel.eliminar(equipo.id)
                        }
                        equipoAEliminar = null
                    }
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        equipoAEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarFormulario) {
        NuevoEquipoScreen(
            errorExterno = error,
            alGuardar = { codigo, tipo, resultado ->
                viewModel.insertar(
                    Equipo(
                        codigo = codigo,
                        tipo = tipo
                    ),
                    alResultado = { guardado ->
                        resultado(guardado)

                        if (guardado) {
                            mostrarFormulario = false
                        }
                    }
                )
            },
            alCancelar = {
                viewModel.limpiarError()
                mostrarFormulario = false
            }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.limpiarError()
                        mostrarFormulario = true
                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir equipo"
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mis equipos",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Gestiona tus equipos registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (equipos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Text(
                                text = "Aún no tienes equipos",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Pulsa + para registrar tu primer equipo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = equipos,
                            key = { it.id }
                        ) { equipo ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Memory,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(27.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = equipo.codigo,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Text(
                                            text = equipo.tipo,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            equipoAEliminar = equipo
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar ${equipo.codigo}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
