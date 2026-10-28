
package com.scarlett.induztek.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaPruebaScreen() {
    val context = LocalContext.current

    val pruebaRepository = remember {
        RepositoryProvider.obtenerPruebaRepository(context)
    }

    val equipoRepository = remember {
        RepositoryProvider.obtenerEquipoRepository(context)
    }

    val pruebaViewModel: PruebaViewModel = viewModel(
        factory = PruebaViewModelFactory(pruebaRepository)
    )

    val equipoViewModel: EquipoViewModel = viewModel(
        factory = EquipoViewModelFactory(equipoRepository)
    )

    val equipos by equipoViewModel.equipos.collectAsState()
    val error by pruebaViewModel.error.collectAsState()

    var equipoSeleccionadoId by remember {
        mutableStateOf<Int?>(null)
    }
    var menuExpandido by remember { mutableStateOf(false) }
    var ubicacion by remember { mutableStateOf("") }
    var medicionTexto by remember { mutableStateOf("") }
    var intentoGuardar by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val fechaHora = remember {
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).format(Date())
    }

    val equipoSeleccionado = equipos.find {
        it.id == equipoSeleccionadoId
    }

    val valorMedicion = medicionTexto
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()

    val equipoInvalido = intentoGuardar && equipoSeleccionado == null
    val ubicacionInvalida = intentoGuardar && ubicacion.isBlank()
    val medicionInvalida = intentoGuardar &&
            (valorMedicion == null || !valorMedicion.isFinite())

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nueva prueba",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Registra los resultados de una prueba eléctrica.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Selección del equipo
            ExposedDropdownMenuBox(
                expanded = menuExpandido,
                onExpandedChange = {
                    menuExpandido = !menuExpandido
                }
            ) {
                OutlinedTextField(
                    value = equipoSeleccionado?.let {
                        "${it.codigo} — ${it.tipo}"
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Equipo") },
                    placeholder = {
                        Text("Selecciona un equipo")
                    },
                    isError = equipoInvalido,
                    supportingText = {
                        if (equipoInvalido) {
                            Text("Debes seleccionar un equipo.")
                        }
                    },
                    trailingIcon = {
                        if (equipoInvalido) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Equipo obligatorio"
                            )
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = menuExpandido
                            )
                        }
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                DropdownMenu(
                    expanded = menuExpandido,
                    onDismissRequest = {
                        menuExpandido = false
                    }
                ) {
                    equipos.forEach { equipo ->
                        DropdownMenuItem(
                            text = {
                                Text("${equipo.codigo} — ${equipo.tipo}")
                            },
                            onClick = {
                                equipoSeleccionadoId = equipo.id
                                menuExpandido = false
                                mensaje = null
                                pruebaViewModel.limpiarError()
                            }
                        )
                    }
                }
            }

            if (equipos.isEmpty()) {
                Text(
                    text = "Primero debes registrar un equipo.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Ubicación
            OutlinedTextField(
                value = ubicacion,
                onValueChange = {
                    ubicacion = it
                    mensaje = null
                    pruebaViewModel.limpiarError()
                },
                label = {
                    Text("Ubicación / instalación del cliente")
                },
                placeholder = {
                    Text("Ej.: Planta industrial")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = ubicacionInvalida,
                supportingText = {
                    if (ubicacionInvalida) {
                        Text("Ingresa la ubicación del cliente.")
                    }
                },
                trailingIcon = {
                    when {
                        ubicacionInvalida -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Ubicación obligatoria"
                        )

                        ubicacion.isNotBlank() -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Ubicación completada",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Medición
            OutlinedTextField(
                value = medicionTexto,
                onValueChange = {
                    medicionTexto = it
                    mensaje = null
                    pruebaViewModel.limpiarError()
                },
                label = { Text("Valor de medición") },
                placeholder = { Text("Ej.: 12.5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = medicionInvalida,
                supportingText = {
                    if (medicionInvalida) {
                        Text("Ingresa una medición numérica válida.")
                    }
                },
                trailingIcon = {
                    when {
                        medicionInvalida -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Medición inválida"
                        )

                        valorMedicion != null &&
                                valorMedicion.isFinite() -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Medición válida",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Fecha y hora
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Fecha y hora",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(fechaHora)
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (mensaje != null) {
                Text(
                    text = mensaje!!,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = {
                    intentoGuardar = true
                    mensaje = null

                    val equipo = equipoSeleccionado
                    val valor = medicionTexto
                        .trim()
                        .replace(',', '.')
                        .toDoubleOrNull()

                    when {
                        equipo == null -> {
                            mensaje = "Selecciona un equipo."
                        }

                        ubicacion.isBlank() -> {
                            mensaje = "Ingresa la ubicación del cliente."
                        }

                        valor == null || !valor.isFinite() -> {
                            mensaje = "Ingresa una medición numérica válida."
                        }

                        else -> {
                            guardando = true

                            pruebaViewModel.insertar(
                                Prueba(
                                    codigoEquipo = equipo.codigo,
                                    fechaHora = fechaHora,
                                    ubicacion = ubicacion.trim(),
                                    medicion = valor
                                )
                            ) { correcto ->
                                guardando = false

                                if (correcto) {
                                    mensaje = "Prueba guardada correctamente."
                                    intentoGuardar = false
                                    equipoSeleccionadoId = null
                                    ubicacion = ""
                                    medicionTexto = ""
                                    pruebaViewModel.limpiarGuardado()
                                } else {
                                    mensaje = null
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando && equipos.isNotEmpty()
            ) {
                if (guardando) {
                    CircularProgressIndicator()
                } else {
                    Text("Guardar prueba")
                }
            }
        }
    }
}
