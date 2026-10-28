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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory
import java.util.Locale

@Composable
fun HistorialScreen() {
    val context = LocalContext.current

    val repository = remember {
        RepositoryProvider.obtenerPruebaRepository(context)
    }

    val pruebaViewModel: PruebaViewModel = viewModel(
        factory = PruebaViewModelFactory(repository)
    )

    val pruebas by pruebaViewModel.pruebas.collectAsState()
    var filtro by remember { mutableStateOf("") }

    val pruebasOrdenadas = pruebas.sortedByDescending {
        it.fechaHora
    }

    val pruebasFiltradas = pruebasOrdenadas.filter {
        it.codigoEquipo.contains(filtro, ignoreCase = true) ||
                it.ubicacion.contains(filtro, ignoreCase = true) ||
                it.tipoEnsayo.contains(filtro, ignoreCase = true)
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
                        key = { it.id }
                    ) { prueba ->

                        // Busca una prueba anterior compatible.
                        val anterior = pruebas
                            .filter {
                                it.codigoEquipo == prueba.codigoEquipo &&
                                        it.tipoEnsayo.equals(
                                            prueba.tipoEnsayo,
                                            ignoreCase = true
                                        ) &&
                                        it.unidadMedida.equals(
                                            prueba.unidadMedida,
                                            ignoreCase = true
                                        ) &&
                                        it.id != prueba.id &&
                                        it.fechaHora < prueba.fechaHora
                            }
                            .maxByOrNull { it.fechaHora }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Equipo: ${prueba.codigoEquipo}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text("Ubicación: ${prueba.ubicacion}")
                                Text("Tipo de ensayo: ${prueba.tipoEnsayo}")
                                Text("Unidad de medida: ${prueba.unidadMedida}")
                                Text("Fecha y hora: ${prueba.fechaHora}")

                                Text(
                                    "Medición actual: ${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            prueba.medicion
                                        )
                                    } ${prueba.unidadMedida}"
                                )

                                if (anterior != null) {
                                    val diferencia =
                                        prueba.medicion - anterior.medicion

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        "Medición anterior: ${
                                            String.format(
                                                Locale.US,
                                                "%.2f",
                                                anterior.medicion
                                            )
                                        } ${anterior.unidadMedida}"
                                    )

                                    Text(
                                        "Fecha anterior: ${anterior.fechaHora}"
                                    )

                                    Text(
                                        "Diferencia: ${
                                            String.format(
                                                Locale.US,
                                                "%+.2f",
                                                diferencia
                                            )
                                        } ${prueba.unidadMedida}"
                                    )
                                } else {
                                    Text(
                                        "Sin mediciones anteriores " +
                                                "del mismo equipo, ensayo y unidad."
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
