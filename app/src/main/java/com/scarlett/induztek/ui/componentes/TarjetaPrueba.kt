package com.scarlett.induztek.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scarlett.induztek.model.PruebaConMediciones
import com.scarlett.induztek.model.ResultadoMedicion
import com.scarlett.induztek.util.FechaFormato
import com.scarlett.induztek.viewmodel.HistorialComparacion
import java.util.Locale

/*
 * Resumen de una prueba en una lista. Se usa en el historial completo y
 * en el historial por equipo dentro del detalle del equipo.
 *
 * textoEquipo se deja en null cuando la tarjeta ya está dentro de la
 * pantalla de ese equipo y repetir el código sobra.
 */
@Composable
fun TarjetaPrueba(
    conMediciones: PruebaConMediciones,
    todas: List<PruebaConMediciones>,
    alAbrir: () -> Unit,
    textoEquipo: String? = null
) {
    val prueba = conMediciones.prueba
    val unidad = prueba.unidadMedida.simbolo

    val anterior = HistorialComparacion.anteriorComparable(
        conMediciones,
        todas
    )

    val comparaciones = HistorialComparacion.comparar(
        conMediciones,
        anterior
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alAbrir),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (textoEquipo != null) {
                Text(
                    text = textoEquipo,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = prueba.tipoEnsayo.etiqueta,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = FechaFormato.formatear(prueba.fechaHora) +
                        "  ·  " + prueba.ubicacion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            comparaciones.forEach { comparacion ->
                val medicion = comparacion.medicion
                val diferencia = comparacion.diferencia

                val valor = String.format(
                    Locale.US,
                    "%.2f",
                    medicion.valor
                )

                val sufijo = if (diferencia != null) {
                    "  (" + String.format(
                        Locale.US,
                        "%+.2f",
                        diferencia
                    ) + ")"
                } else {
                    ""
                }

                Text(
                    text = "${medicion.nombre}: $valor $unidad$sufijo",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val rechazadas = conMediciones.mediciones.count {
                it.resultado == ResultadoMedicion.RECHAZADO
            }

            if (rechazadas > 0) {
                Text(
                    text = if (rechazadas == 1) {
                        "1 medición fuera de rango"
                    } else {
                        "$rechazadas mediciones fuera de rango"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (anterior == null) {
                Text(
                    text = "Sin prueba anterior comparable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Ver protocolo  →",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
