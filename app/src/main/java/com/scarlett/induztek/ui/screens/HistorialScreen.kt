package com.scarlett.induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.scarlett.induztek.ui.componentes.TarjetaPrueba
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory

@Composable
fun HistorialScreen(
    alSeleccionarPrueba: (Int) -> Unit
) {
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

    // La prueba guarda el id del equipo, no su código. El código se
    // busca en la lista de equipos solo para mostrarlo y buscarlo.
    val pruebas by pruebaViewModel.pruebas.collectAsState()
    val equipos by equipoViewModel.equipos.collectAsState()

    var filtro by remember { mutableStateOf("") }

    val equipoPorId = equipos.associateBy { it.id }

    val pruebasFiltradas = pruebas.filter { conMediciones ->
        val codigo = equipoPorId[conMediciones.prueba.equipoId]?.codigo ?: ""

        codigo.contains(filtro, ignoreCase = true) ||
                conMediciones.prueba.ubicacion
                    .contains(filtro, ignoreCase = true) ||
                conMediciones.prueba.tipoEnsayo.etiqueta
                    .contains(filtro, ignoreCase = true)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Historial de pruebas",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = filtro,
                onValueChange = { filtro = it },
                label = {
                    Text("Buscar por equipo, ubicación o ensayo")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            if (pruebasFiltradas.isEmpty()) {
                Text(
                    if (pruebas.isEmpty()) {
                        "Todavía no hay pruebas registradas."
                    } else {
                        "No se encontraron pruebas."
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = pruebasFiltradas,
                        key = { it.prueba.id }
                    ) { conMediciones ->
                        val equipo =
                            equipoPorId[conMediciones.prueba.equipoId]

                        TarjetaPrueba(
                            conMediciones = conMediciones,
                            todas = pruebas,
                            textoEquipo = equipo
                                ?.let {
                                    "${it.codigo} — ${it.tipo.etiqueta}"
                                }
                                ?: "(equipo eliminado)",
                            alAbrir = {
                                alSeleccionarPrueba(
                                    conMediciones.prueba.id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
