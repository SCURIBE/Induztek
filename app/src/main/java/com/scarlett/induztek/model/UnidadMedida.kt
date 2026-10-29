package com.scarlett.induztek.model

// Unidades en que se puede expresar una medición.
// El símbolo es lo que se muestra; el nombre de la constante es lo que
// se guarda en la base de datos.
enum class UnidadMedida(val simbolo: String) {
    MEGAOHM("MΩ"),
    OHM("Ω"),
    MILISEGUNDO("ms"),
    KILOVOLT("kV"),
    VOLT("V"),
    AMPERE("A"),
    RAZON("razón"),
    PORCENTAJE("%"),

    // Solo para las pruebas que ya existían cuando la unidad era texto
    // libre y no se pudo deducir cuál era. No se ofrece al registrar
    // una prueba nueva.
    SIN_ESPECIFICAR("sin especificar")
}
