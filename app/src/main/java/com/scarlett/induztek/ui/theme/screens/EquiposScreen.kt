
package com.scarlett.induztek.ui.theme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory

@Composable
fun EquiposScreen() {
    val context = LocalContext.current
    val repository = RepositoryProvider.obtenerEquipoRepository(context)
    val factory = EquipoViewModelFactory(repository)

    val viewModel: EquipoViewModel = viewModel(factory = factory)
    val equipos by viewModel.equipos.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Equipos registrados",
            style = MaterialTheme.typography.headlineSmall
        )

        if (equipos.isEmpty()) {
            Text("Todavía no hay equipos registrados.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(equipos, key = { it.id }) { equipo ->
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Código: ${equipo.codigo}")
                            Text("Tipo: ${equipo.tipo}")
                        }
                    }
                }
            }
        }
    }
}
