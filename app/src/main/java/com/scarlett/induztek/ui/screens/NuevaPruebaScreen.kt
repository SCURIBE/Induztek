package com.scarlett.induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.scarlett.induztek.model.PuntosMedicion
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida
import com.scarlett.induztek.ui.componentes.CampoSeleccion
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.MedicionIngresada
import com.scarlett.induztek.viewmodel.PruebaValidacion
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory

/*
 * Una fila del formulario de mediciones. La clave es un número que no
 * cambia aunque se borren filas de arriba, para que Compose no pierda
 * el estado de los campos al reordenarse la lista.
 */
private data class FilaMedicion(
    val clave: Int,
    val nombre: String,
    val valorTexto: String = "",
    val minimoTexto: String = "",
    val maximoTexto: String = ""
) {
    fun aIngresada(): MedicionIngresada {
        return MedicionIngresada(
            nombre = nombre,
            valorTexto = valorTexto,
            minimoTexto = minimoTexto,
            maximoTexto = maximoTexto
        )
    }
}

@Composable
fun NuevaPruebaScreen(tecnicoInicial: String = "") {
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
    var cliente by remember { mutableStateOf("") }
    var tecnico by remember { mutableStateOf(tecnicoInicial) }
    var observaciones by remember { mutableStateOf("") }
    var intentoGuardar by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    var filas by remember {
        mutableStateOf(
            listOf(FilaMedicion(clave = 0, nombre = PuntosMedicion.UNICA))
        )
    }
    var siguienteClave by remember { mutableStateOf(1) }

    val equipoSeleccionado = equipos.find {
        it.id == equipoSeleccionadoId
    }

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

    fun limpiarAvisos() {
        mensaje = null
        pruebaViewModel.limpiarError()
    }

    fun cambiarFila(clave: Int, cambio: (FilaMedicion) -> FilaMedicion) {
        filas = filas.map { fila ->
            if (fila.clave == clave) cambio(fila) else fila
        }
        limpiarAvisos()
    }

    // Puntos que todavía no se usaron: los nombres no se pueden repetir
    // porque la comparación del historial empareja por nombre.
    fun puntosLibres(fila: FilaMedicion): List<String> {
        val usados = filas
            .filter { it.clave != fila.clave }
            .map { it.nombre }

        return PuntosMedicion.sugeridos.filter { it !in usados }
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
                text = "Registra el protocolo de una prueba eléctrica.",
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Mediciones",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Una fila por cada punto medido. El rango es " +
                        "opcional: si lo dejas vacío, la medición no " +
                        "se evalúa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            filas.forEach { fila ->
                val errorNombre =
                    PruebaValidacion.validarNombreMedicion(fila.nombre)
                val errorValor =
                    PruebaValidacion.validarMedicion(fila.valorTexto)
                val errorRango = PruebaValidacion.validarRango(
                    fila.minimoTexto,
                    fila.maximoTexto
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CampoSeleccion(
                            etiqueta = "Punto de medición",
                            textoVacio = "Elige el punto",
                            opciones = puntosLibres(fila),
                            seleccion = fila.nombre.ifBlank { null },
                            textoDe = { it },
                            alSeleccionar = { punto ->
                                cambiarFila(fila.clave) {
                                    it.copy(nombre = punto)
                                }
                            },
                            esInvalido =
                                intentoGuardar && errorNombre != null,
                            mensajeError = errorNombre ?: "",
                            habilitado = !guardando
                        )

                        OutlinedTextField(
                            value = fila.valorTexto,
                            onValueChange = { texto ->
                                cambiarFila(fila.clave) {
                                    it.copy(valorTexto = texto)
                                }
                            },
                            label = { Text("Valor medido") },
                            placeholder = { Text("Ej.: 12.5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !guardando,
                            isError =
                                intentoGuardar && errorValor != null,
                            supportingText = {
                                val unidad = unidadMedida

                                if (intentoGuardar && errorValor != null) {
                                    Text(errorValor)
                                } else if (unidad != null) {
                                    Text("Se registra en ${unidad.simbolo}.")
                                }
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = fila.minimoTexto,
                                onValueChange = { texto ->
                                    cambiarFila(fila.clave) {
                                        it.copy(minimoTexto = texto)
                                    }
                                },
                                label = { Text("Mínimo") },
                                placeholder = { Text("opcional") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !guardando,
                                isError =
                                    intentoGuardar && errorRango != null
                            )

                            OutlinedTextField(
                                value = fila.maximoTexto,
                                onValueChange = { texto ->
                                    cambiarFila(fila.clave) {
                                        it.copy(maximoTexto = texto)
                                    }
                                },
                                label = { Text("Máximo") },
                                placeholder = { Text("opcional") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                enabled = !guardando,
                                isError =
                                    intentoGuardar && errorRango != null
                            )
                        }

                        if (intentoGuardar && errorRango != null) {
                            Text(
                                text = errorRango,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (filas.size > 1) {
                            TextButton(
                                onClick = {
                                    filas = filas.filter {
                                        it.clave != fila.clave
                                    }
                                    limpiarAvisos()
                                },
                                enabled = !guardando
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                                Text("  Quitar esta medición")
                            }
                        }
                    }
                }
            }

            val puntoDisponible = PuntosMedicion.sugeridos
                .firstOrNull { punto ->
                    punto !in filas.map { it.nombre }
                }

            OutlinedButton(
                onClick = {
                    filas = filas + FilaMedicion(
                        clave = siguienteClave,
                        nombre = puntoDisponible ?: ""
                    )
                    siguienteClave += 1
                    limpiarAvisos()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando && puntoDisponible != null
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Text(
                    if (puntoDisponible != null) {
                        "  Agregar otra medición"
                    } else {
                        "  Ya usaste todos los puntos de la lista"
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Datos de la visita",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = cliente,
                onValueChange = {
                    cliente = it
                    limpiarAvisos()
                },
                label = { Text("Cliente") },
                placeholder = { Text("Opcional") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !guardando
            )

            OutlinedTextField(
                value = tecnico,
                onValueChange = {
                    tecnico = it
                    limpiarAvisos()
                },
                label = { Text("Técnico responsable") },
                placeholder = { Text("Opcional") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !guardando
            )

            OutlinedTextField(
                value = observaciones,
                onValueChange = {
                    observaciones = it
                    limpiarAvisos()
                },
                label = { Text("Observaciones") },
                placeholder = { Text("Opcional") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !guardando
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
                        mediciones = filas.map { it.aIngresada() },
                        cliente = cliente,
                        tecnicoResponsable = tecnico,
                        observaciones = observaciones
                    ) { correcto ->
                        guardando = false

                        if (correcto) {
                            mensaje = "Prueba guardada correctamente."
                            intentoGuardar = false
                            equipoSeleccionadoId = null
                            ubicacion = ""
                            tipoEnsayo = null
                            unidadMedida = null
                            cliente = ""
                            tecnico = tecnicoInicial
                            observaciones = ""
                            filas = listOf(
                                FilaMedicion(
                                    clave = siguienteClave,
                                    nombre = PuntosMedicion.UNICA
                                )
                            )
                            siguienteClave += 1
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
