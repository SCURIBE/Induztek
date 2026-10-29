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
import com.scarlett.induztek.util.FechaFormato
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.HistorialComparacion
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory
import java.util.Locale

@Composable
fun HistorialScreen() {
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

    val codigoPorEquipo = equipos.associate { it.id to it.codigo }

    val pruebasFiltradas = pruebas.filter { conMediciones ->
        val codigo = codigoPorEquipo[conMediciones.prueba.equipoId] ?: ""

        codigo.contains(filtro, ignoreCase = true) ||
                conMediciones.prueba.ubicacion
                    .contains(filtro, ignoreCase = true) ||
                conMediciones.prueba.tipoEnsayo.etiqueta
                    .contains(filtro, ignoreCase = true)
    }

    fun formatear(valor: Double): String {
        return String.format(Locale.US, "%.2f", valor)
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

                        val prueba = conMediciones.prueba
                        val unidad = prueba.unidadMedida.simbolo

                        // La búsqueda de la prueba anterior comparable
                        // vive en HistorialComparacion, para poder
                        // testearla sin levantar la pantalla.
                        val anterior =
                            HistorialComparacion.anteriorComparable(
                                conMediciones,
                                pruebas
                            )

                        val diferencia = HistorialComparacion.diferencia(
                            conMediciones,
                            anterior
                        )

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
                                    text = "Equipo: " + (
                                            codigoPorEquipo[prueba.equipoId]
                                                ?: "(equipo eliminado)"
                                            ),
                                    style =
                                        MaterialTheme.typography.titleMedium
                                )

                                Text("Ubicación: ${prueba.ubicacion}")
                                Text(
                                    "Tipo de ensayo: " +
                                            prueba.tipoEnsayo.etiqueta
                                )
                                Text("Unidad de medida: $unidad")
                                Text(
                                    "Fecha y hora: " +
                                            FechaFormato.formatear(
                                                prueba.fechaHora
                                            )
                                )

                                conMediciones.mediciones.forEach { medicion ->
                                    Text(
                                        "${medicion.nombre}: " +
                                                formatear(medicion.valor) +
                                                " $unidad"
                                    )
                                }

                                if (anterior != null && diferencia != null) {
                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        "Medición anterior: " +
                                                formatear(
                                                    anterior.valorPrincipal
                                                        ?: 0.0
                                                ) + " $unidad"
                                    )

                                    Text(
                                        "Fecha anterior: " +
                                                FechaFormato.formatear(
                                                    anterior.prueba.fechaHora
                                                )
                                    )

                                    Text(
                                        "Diferencia: " + String.format(
                                            Locale.US,
                                            "%+.2f",
                                            diferencia
                                        ) + " $unidad"
                                    )
                                } else {
                                    Text(
                                        "Sin mediciones anteriores " +
                                                "del mismo equipo, ensayo " +
                                                "y unidad."
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
