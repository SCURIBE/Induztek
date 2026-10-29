package com.scarlett.induztek.model

// Lista fija de tipos de equipo. Antes era texto libre, así que dos
// equipos podían quedar como "Transformador" y "transformador " y no
// se agrupaban entre sí.
//
// Room guarda el enum como TEXT usando el nombre de la constante
// (TRANSFORMADOR, RELE_PROTECCION...), no la etiqueta. Así se puede
// cambiar el texto que ve el usuario sin migrar la base de datos.
enum class TipoEquipo(val etiqueta: String) {
    TRANSFORMADOR("Transformador"),
    RELE_PROTECCION("Relé de protección"),
    CELDA("Celda"),
    CABLE("Cable"),
    OTRO("Otro")
}
