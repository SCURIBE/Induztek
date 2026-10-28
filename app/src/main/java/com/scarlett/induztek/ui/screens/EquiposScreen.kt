package com.scarlett.induztek.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory

@Composable
fun EquiposScreen(
    alSeleccionarEquipo: (Equipo) -> Unit
) {
    val context = LocalContext.current

    val repository = remember {
        RepositoryProvider.obtenerEquipoRepository(context)
    }

    val factory = remember {
        EquipoViewModelFactory(repository)
    }

    val viewModel: EquipoViewModel = viewModel(
        factory = factory
    )

    val equipos by viewModel.equipos.collectAsState()
    val error by viewModel.error.collectAsState()

    var mostrarFormulario by remember {
        mutableStateOf(false)
    }

    if (mostrarFormulario) {
        NuevoEquipoScreen(
            errorExterno = error,
            alGuardar = { codigo, tipo, fotoUri, resultado ->
                val nuevoEquipo = Equipo(
                    codigo = codigo,
                    tipo = tipo,
                    fotoUri = fotoUri
                )

                viewModel.insertar(nuevoEquipo) { guardado ->
                    resultado(guardado)

                    if (guardado) {
                        mostrarFormulario = false
                    }
                }
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
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar equipo"
                    )
                }
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    )
            ) {
                Text(
                    text = "Mis equipos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Equipos eléctricos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 4.dp,
                        bottom = 20.dp
                    )
                )

                if (equipos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "No hay equipos registrados",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Presiona + para agregar tu primer equipo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = equipos,
                            key = { equipo -> equipo.id }
                        ) { equipo ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        alSeleccionarEquipo(equipo)
                                    },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 2.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment =
                                        Alignment.CenterVertically,
                                    horizontalArrangement =
                                        Arrangement.spacedBy(14.dp)
                                ) {
                                    val miniatura = remember(equipo.fotoUri) {
                                        equipo.fotoUri?.let { ruta ->
                                            try {
                                                BitmapFactory.decodeFile(ruta)
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                    }

                                    if (miniatura != null) {
                                        Image(
                                            bitmap = miniatura.asImageBitmap(),
                                            contentDescription =
                                                "Fotografía de ${equipo.codigo}",
                                            modifier = Modifier.size(64.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement =
                                            Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = equipo.codigo,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = equipo.tipo,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = "Ver detalles  →",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
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
