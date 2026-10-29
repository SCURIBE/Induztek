package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.PruebaConMediciones
import com.scarlett.induztek.model.ResultadoMedicion
import com.scarlett.induztek.util.FechaFormato
import java.text.Normalizer
import java.util.Locale

// Una fila de la tabla de mediciones del PDF, con todo ya convertido a
// texto. El resultado se guarda también como enum para elegir el color.
data class FilaProtocolo(
    val punto: String,
    val valor: String,
    val rango: String,
    val resultado: ResultadoMedicion,
    val resultadoTexto: String,
    val anterior: String,
    val diferencia: String
)

// Todo lo que va escrito en el protocolo, sin nada de dibujo.
data class ProtocoloContenido(
    val numero: String,
    val tipoEnsayo: String,
    val unidad: String,
    val datos: List<Pair<String, String>>,
    val filas: List<FilaProtocolo>,
    val comparacion: String,
    val resumen: String,
    val fueraDeRango: Int,
    val observaciones: String,
    val tecnico: String,
    val generado: String,
    val nombreArchivo: String,
    val asunto: String
)

/*
 * Arma el contenido del protocolo a partir de la prueba. Está separado
 * del dibujo del PDF (util/ProtocoloPdf) por la misma razón que
 * PruebaValidacion: así se puede testear qué dice el documento sin
 * Android, y el dibujo solo se ocupa de dónde va cada cosa.
 *
 * Usa HistorialComparacion para la columna "Anterior": el PDF compara
 * exactamente igual que la pantalla de detalle.
 */
object Protocolo {

    const val SIN_DATO = "—"
    const val NO_INFORMADO = "No informado"

    fun armar(
        actual: PruebaConMediciones,
        equipo: Equipo?,
        anterior: PruebaConMediciones?,
        generadoEn: Long
    ): ProtocoloContenido {
        val prueba = actual.prueba
        val unidad = prueba.unidadMedida.simbolo
        val fecha = FechaFormato.formatear(prueba.fechaHora)

        val filas = HistorialComparacion.comparar(actual, anterior)
            .map { comparacion ->
                val medicion = comparacion.medicion

                FilaProtocolo(
                    punto = medicion.nombre,
                    valor = "${formatear(medicion.valor)} $unidad",
                    rango = rango(medicion),
                    resultado = medicion.resultado,
                    resultadoTexto = textoResultado(medicion.resultado),
                    anterior = comparacion.anterior
                        ?.let { "${formatear(it.valor)} $unidad" }
                        ?: SIN_DATO,
                    diferencia = comparacion.diferencia
                        ?.let { String.format(Locale.US, "%+.2f", it) }
                        ?: SIN_DATO
                )
            }

        val evaluadas = actual.mediciones.count {
            it.resultado != ResultadoMedicion.NO_EVALUADO
        }
        val rechazadas = actual.mediciones.count {
            it.resultado == ResultadoMedicion.RECHAZADO
        }

        val textoEquipo = equipo
            ?.let { "${it.codigo} — ${it.tipo.etiqueta}" }
            ?: "(equipo eliminado)"

        val numero = numero(prueba.id)

        return ProtocoloContenido(
            numero = numero,
            tipoEnsayo = prueba.tipoEnsayo.etiqueta,
            unidad = unidad,
            datos = listOf(
                "N.º de protocolo" to numero,
                "Equipo" to textoEquipo,
                "Fecha y hora" to fecha,
                "Ubicación" to prueba.ubicacion,
                "Cliente" to prueba.cliente.ifBlank { NO_INFORMADO },
                "Técnico responsable" to
                        prueba.tecnicoResponsable.ifBlank { NO_INFORMADO },
                "Unidad de medida" to unidad
            ),
            filas = filas,
            comparacion = if (anterior != null) {
                "Comparado con la prueba del " +
                        FechaFormato.formatear(anterior.prueba.fechaHora) +
                        ": mismo equipo, mismo ensayo y misma unidad."
            } else {
                "Sin prueba anterior comparable: es la primera de este " +
                        "equipo con este ensayo y esta unidad."
            },
            resumen = resumen(
                total = actual.mediciones.size,
                evaluadas = evaluadas,
                rechazadas = rechazadas
            ),
            fueraDeRango = rechazadas,
            observaciones = prueba.observaciones.trim(),
            tecnico = prueba.tecnicoResponsable.trim(),
            generado = "Generado el " + FechaFormato.formatear(generadoEn) +
                    " con Induztek",
            nombreArchivo = nombreArchivo(equipo?.codigo, fecha),
            asunto = "Protocolo $numero — ${prueba.tipoEnsayo.etiqueta}" +
                    (equipo?.let { " — ${it.codigo}" } ?: "")
        )
    }

    // El mismo formato que la pantalla de detalle, para que el cliente
    // lea en el PDF exactamente lo que vio el técnico.
    fun formatear(valor: Double): String {
        return String.format(Locale.US, "%.2f", valor)
    }

    fun rango(medicion: Medicion): String {
        val minimo = medicion.minimoAceptable
        val maximo = medicion.maximoAceptable

        return when {
            minimo != null && maximo != null ->
                "${formatear(minimo)} a ${formatear(maximo)}"

            minimo != null -> "≥ ${formatear(minimo)}"
            maximo != null -> "≤ ${formatear(maximo)}"
            else -> "Sin rango"
        }
    }

    fun textoResultado(resultado: ResultadoMedicion): String {
        return if (resultado == ResultadoMedicion.NO_EVALUADO) {
            "No evaluado"
        } else {
            resultado.etiqueta
        }
    }

    fun resumen(total: Int, evaluadas: Int, rechazadas: Int): String {
        return when {
            total == 0 ->
                "La prueba no tiene mediciones."

            evaluadas == 0 ->
                "Ninguna medición tiene rango aceptable definido: " +
                        "no se evalúan."

            rechazadas > 0 ->
                "$rechazadas de $evaluadas mediciones evaluadas " +
                        "fuera del rango aceptable."

            evaluadas == total ->
                "Todas las mediciones dentro del rango aceptable."

            else ->
                "Las $evaluadas mediciones evaluadas están dentro del " +
                        "rango aceptable (${total - evaluadas} sin rango " +
                        "definido)."
        }
    }

    // Número correlativo a partir del id de la prueba, con ceros a la
    // izquierda para que se lea como número de documento.
    fun numero(pruebaId: Int): String {
        return "P-" + pruebaId.toString().padStart(6, '0')
    }

    /*
     * Nombre del PDF. Solo letras sin tilde, números, punto, guion y
     * guion bajo: el archivo viaja por correo y WhatsApp, y algunas apps
     * cortan o rechazan nombres con espacios, barras o tildes.
     *
     * Las tildes se quitan en vez de reemplazar la letra: NFD separa la
     * "ó" en "o" más el acento, y el acento se descarta. Así
     * "Subestación" queda "Subestacion" y no "Subestaci_n".
     */
    fun nombreArchivo(codigoEquipo: String?, fecha: String): String {
        val base = "Protocolo_${codigoEquipo ?: "equipo"}_$fecha"

        val limpio = Normalizer.normalize(base, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

        return "$limpio.pdf"
    }
}
