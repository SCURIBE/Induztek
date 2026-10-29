package com.scarlett.induztek.ui.screens

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scarlett.induztek.data.RepositoryProvider
import com.scarlett.induztek.model.ResultadoMedicion
import com.scarlett.induztek.util.AlmacenFotos
import com.scarlett.induztek.util.FechaFormato
import com.scarlett.induztek.util.ProtocoloPdf
import com.scarlett.induztek.viewmodel.ComparacionMedicion
import com.scarlett.induztek.viewmodel.EquipoViewModel
import com.scarlett.induztek.viewmodel.EquipoViewModelFactory
import com.scarlett.induztek.viewmodel.HistorialComparacion
import com.scarlett.induztek.viewmodel.Protocolo
import com.scarlett.induztek.viewmodel.PruebaViewModel
import com.scarlett.induztek.viewmodel.PruebaViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/*
 * El protocolo de una prueba: todos sus datos, sus mediciones con el
 * resultado frente al rango, y la comparación con la prueba anterior
 * comparable del mismo equipo.
 *
 * De esta pantalla sale el PDF que recibe el cliente, con el mismo
 * contenido: lo arma viewmodel/Protocolo y lo dibuja util/ProtocoloPdf.
 */
@Composable
fun PruebaDetalleScreen(
    pruebaId: Int,
    alVolver: () -> Unit
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

    val conMediciones by remember(pruebaId) {
        pruebaRepository.obtenerPorId(pruebaId)
    }.collectAsState(initial = null)

    val todas by pruebaViewModel.pruebas.collectAsState()
    val equipos by equipoViewModel.equipos.collectAsState()

    val scope = rememberCoroutineScope()
    var generandoPdf by remember { mutableStateOf(false) }
    var errorPdf by remember { mutableStateOf<String?>(null) }

    fun formatear(valor: Double): String {
        return String.format(Locale.US, "%.2f", valor)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = alVolver) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
            )
            Text(" Volver")
        }

        val actual = conMediciones

        if (actual == null) {
            // Pasa mientras llega la consulta, y también si la prueba
            // fue eliminada en cascada al borrar su equipo.
            Text(
                text = "Esta prueba ya no está disponible.",
                style = MaterialTheme.typography.bodyLarge
            )
            return@Column
        }

        val prueba = actual.prueba
        val unidad = prueba.unidadMedida.simbolo
        val equipo = equipos.find { it.id == prueba.equipoId }

        val anterior = HistorialComparacion.anteriorComparable(
            actual,
            todas
        )

        val comparaciones = HistorialComparacion.comparar(actual, anterior)

        Text(
            text = "Protocolo de prueba",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = prueba.tipoEnsayo.etiqueta,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Lo que se entrega al cliente en terreno. Generar el PDF lee la
        // foto y escribe un archivo: va fuera del hilo principal.
        Button(
            onClick = {
                generandoPdf = true
                errorPdf = null

                scope.launch {
                    val generado = withContext(Dispatchers.IO) {
                        try {
                            val contenido = Protocolo.armar(
                                actual = actual,
                                equipo = equipo,
                                anterior = anterior,
                                generadoEn = System.currentTimeMillis()
                            )

                            val foto = AlmacenFotos.leer(
                                equipo?.fotoUri,
                                AlmacenFotos.LADO_PDF
                            )

                            ProtocoloPdf.generar(context, contenido, foto) to
                                    contenido.asunto
                        } catch (_: Exception) {
                            null
                        }
                    }

                    generandoPdf = false

                    if (generado == null) {
                        errorPdf = "No se pudo generar el PDF."
                    } else {
                        try {
                            ProtocoloPdf.compartir(
                                context,
                                generado.first,
                                generado.second
                            )
                        } catch (_: ActivityNotFoundException) {
                            errorPdf = "No hay apps instaladas para " +
                                    "compartir el PDF."
                        }
                    }
                }
            },
            enabled = !generandoPdf,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (generandoPdf) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null
                )
                Text("  Compartir protocolo en PDF")
            }
        }

        errorPdf?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Dato(
                    "Equipo",
                    equipo?.let { "${it.codigo} — ${it.tipo.etiqueta}" }
                        ?: "(equipo eliminado)"
                )
                Dato("Ubicación", prueba.ubicacion)
                Dato(
                    "Fecha y hora",
                    FechaFormato.formatear(prueba.fechaHora)
                )
                Dato("Unidad de medida", unidad)

                if (prueba.cliente.isNotBlank()) {
                    Dato("Cliente", prueba.cliente)
                }

                if (prueba.tecnicoResponsable.isNotBlank()) {
                    Dato("Técnico responsable", prueba.tecnicoResponsable)
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Mediciones",
            style = MaterialTheme.typography.titleLarge
        )

        if (anterior != null) {
            Text(
                text = "Se comparan con la prueba del " +
                        FechaFormato.formatear(anterior.prueba.fechaHora) +
                        ", el mismo ensayo en la misma unidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "No hay una prueba anterior de este equipo con " +
                        "el mismo ensayo y la misma unidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        comparaciones.forEach { comparacion ->
            FilaComparacion(
                comparacion = comparacion,
                unidad = unidad,
                formatear = ::formatear
            )
        }

        if (prueba.observaciones.isNotBlank()) {
            HorizontalDivider()

            Text(
                text = "Observaciones",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = prueba.observaciones,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun FilaComparacion(
    comparacion: ComparacionMedicion,
    unidad: String,
    formatear: (Double) -> String
) {
    val medicion = comparacion.medicion
    val resultado = medicion.resultado
    val diferencia = comparacion.diferencia

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = medicion.nombre,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "${formatear(medicion.valor)} $unidad",
                style = MaterialTheme.typography.headlineSmall
            )

            // El resultado no está guardado: se deduce del rango cada
            // vez, así no puede contradecir al valor.
            if (resultado != ResultadoMedicion.NO_EVALUADO) {
                Text(
                    text = resultado.etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (
                        resultado == ResultadoMedicion.RECHAZADO
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Text(
                    text = "Rango aceptable: " +
                            (medicion.minimoAceptable
                                ?.let { formatear(it) } ?: "sin mínimo") +
                            " a " +
                            (medicion.maximoAceptable
                                ?.let { formatear(it) } ?: "sin máximo"),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (comparacion.anterior != null && diferencia != null) {
                Text(
                    text = "Anterior: " +
                            formatear(comparacion.anterior.valor) +
                            " $unidad  ·  Diferencia: " +
                            String.format(
                                Locale.US,
                                "%+.2f",
                                diferencia
                            ) + " $unidad",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Sin medición anterior de este punto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
