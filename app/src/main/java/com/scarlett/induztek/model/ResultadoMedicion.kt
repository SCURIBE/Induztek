package com.scarlett.induztek.model

// Resultado de comparar una medición con su rango aceptable.
// No se guarda en la base de datos: se deduce del valor y del rango
// cada vez que se necesita, así nunca queda desincronizado.
enum class ResultadoMedicion(val etiqueta: String) {
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado"),
    NO_EVALUADO("Sin rango definido")
}
