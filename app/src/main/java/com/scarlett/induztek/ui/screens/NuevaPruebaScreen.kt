package com.scarlett.induztek.ui.screens

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
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida
import com.scarlett.induztek.ui.componentes.CampoSeleccion
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.PruebaValidacion
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory

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

    var equipoSeleccionadoId by remember { mutableStateOf<Int?>(null) }
    var ubicacion by remember { mutableStateOf("") }
    var tipoEnsayo by remember { mutableStateOf<TipoEnsayo?>(null) }
    var unidadMedida by remember { mutableStateOf<UnidadMedida?>(null) }
    var medicionTexto by remember { mutableStateOf("") }
    var intentoGuardar by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val equipoSeleccionado = equipos.find {
        it.id == equipoSeleccionadoId
    }

    val valorMedicion = PruebaValidacion.medicionDesdeTexto(medicionTexto)

    // Las unidades que se ofrecen dependen del ensayo elegido, para no
    // guardar por ejemplo un aislamiento medido en milisegundos.
    val unidadesDisponibles = tipoEnsayo?.unidades ?: emptyList()

    // Los mensajes vienen de PruebaValidacion, las mismas funciones que
    // usa el ViewModel al guardar, en vez de repetir las reglas aquí.
    val errorEquipo = PruebaValidacion.validarEquipo(equipoSeleccionadoId)
    val errorUbicacion = PruebaValidacion.validarUbicacion(ubicacion)
    val errorTipoEnsayo = PruebaValidacion.validarTipoEnsayo(tipoEnsayo)
    val errorUnidad = PruebaValidacion.validarUnidad(
        tipoEnsayo,
        unidadMedida
    )
    val errorMedicion = PruebaValidacion.validarMedicion(medicionTexto)

    fun limpiarAvisos() {
        mensaje = null
        pruebaViewModel.limpiarError()
    }

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

            CampoSeleccion(
                etiqueta = "Equipo",
                textoVacio = "Selecciona un equipo",
                opciones = equipos,
                seleccion = equipoSeleccionado,
                textoDe = { "${it.codigo} — ${it.tipo.etiqueta}" },
                alSeleccionar = {
                    equipoSeleccionadoId = it.id
                    limpiarAvisos()
                },
                esInvalido = intentoGuardar && errorEquipo != null,
                mensajeError = errorEquipo ?: "",
                habilitado = !guardando
            )

            if (equipos.isEmpty()) {
                Text(
                    text = "Primero debes registrar un equipo.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = ubicacion,
                onValueChange = {
                    ubicacion = it
                    limpiarAvisos()
                },
                label = { Text("Ubicación / instalación del cliente") },
                placeholder = { Text("Ej.: Planta industrial") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !guardando,
                isError = intentoGuardar && errorUbicacion != null,
                supportingText = {
                    if (intentoGuardar && errorUbicacion != null) {
                        Text(errorUbicacion)
                    }
                },
                trailingIcon = {
                    when {
                        intentoGuardar && errorUbicacion != null -> Icon(
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

            CampoSeleccion(
                etiqueta = "Tipo de ensayo",
                textoVacio = "Selecciona el ensayo realizado",
                opciones = TipoEnsayo.entries,
                seleccion = tipoEnsayo,
                textoDe = { it.etiqueta },
                alSeleccionar = { elegido ->
                    tipoEnsayo = elegido

                    // Si la unidad elegida no sirve para el ensayo
                    // nuevo, se reemplaza por la que corresponde.
                    if (unidadMedida !in elegido.unidades) {
                        unidadMedida = elegido.unidadPorDefecto
                    }

                    limpiarAvisos()
                },
                esInvalido = intentoGuardar && errorTipoEnsayo != null,
                mensajeError = errorTipoEnsayo ?: "",
                habilitado = !guardando
            )

            CampoSeleccion(
                etiqueta = "Unidad de medida",
                textoVacio = if (tipoEnsayo == null) {
                    "Elige primero el tipo de ensayo"
                } else {
                    "Selecciona la unidad"
                },
                opciones = unidadesDisponibles,
                seleccion = unidadMedida,
                textoDe = { it.simbolo },
                alSeleccionar = {
                    unidadMedida = it
                    limpiarAvisos()
                },
                esInvalido = intentoGuardar && errorUnidad != null,
                mensajeError = errorUnidad ?: "",
                habilitado = !guardando && tipoEnsayo != null
            )

            OutlinedTextField(
                value = medicionTexto,
                onValueChange = {
                    medicionTexto = it
                    limpiarAvisos()
                },
                label = { Text("Valor de medición") },
                placeholder = { Text("Ej.: 12.5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !guardando,
                isError = intentoGuardar && errorMedicion != null,
                supportingText = {
                    val unidad = unidadMedida

                    if (intentoGuardar && errorMedicion != null) {
                        Text(errorMedicion)
                    } else if (unidad != null) {
                        Text("Se registrará en ${unidad.simbolo}.")
                    }
                },
                trailingIcon = {
                    when {
                        intentoGuardar && errorMedicion != null -> Icon(
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fecha y hora",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Se registra automáticamente al guardar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    guardando = true

                    pruebaViewModel.registrar(
                        equipoId = equipoSeleccionadoId,
                        ubicacion = ubicacion,
                        tipoEnsayo = tipoEnsayo,
                        unidadMedida = unidadMedida,
                        medicionTexto = medicionTexto
                    ) { correcto ->
                        guardando = false

                        if (correcto) {
                            mensaje = "Prueba guardada correctamente."
                            intentoGuardar = false
                            equipoSeleccionadoId = null
                            ubicacion = ""
                            tipoEnsayo = null
                            unidadMedida = null
                            medicionTexto = ""
                            pruebaViewModel.limpiarGuardado()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando && equipos.isNotEmpty()
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp)
                    )
                } else {
                    Text("Guardar prueba")
                }
            }
        }
    }
}
