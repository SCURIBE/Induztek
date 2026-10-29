package com.scarlett.induztek.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// La fecha se guarda como número (milisegundos) y se formatea solo al
// mostrarla. Este objeto concentra el formato para que el historial, el
// detalle y el futuro PDF muestren la fecha igual.
object FechaFormato {

    private const val PATRON = "yyyy-MM-dd HH:mm"

    fun formatear(fechaHora: Long): String {
        return SimpleDateFormat(PATRON, Locale.getDefault())
            .format(Date(fechaHora))
    }
}
