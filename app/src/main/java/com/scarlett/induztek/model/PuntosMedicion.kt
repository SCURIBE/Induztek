package com.scarlett.induztek.model

/*
 * Puntos donde se toma una medición dentro de un mismo ensayo: las tres
 * fases de un transformador, el neutro, la tierra...
 *
 * Es una lista fija por la misma razón que el tipo de ensayo y la
 * unidad: la comparación del historial empareja las mediciones de dos
 * pruebas por su nombre, así que "Fase A" y "fase a" no se compararían
 * entre sí.
 *
 * Se guarda como texto en Medicion.nombre, no como enum, para no tener
 * que migrar la base cada vez que se agregue un punto a la lista.
 * "Medición" es el nombre que la migración 4→5 le puso a las pruebas
 * que tenían un solo valor.
 */
object PuntosMedicion {

    const val UNICA = "Medición"

    val sugeridos = listOf(
        UNICA,
        "Fase A",
        "Fase B",
        "Fase C",
        "Neutro",
        "Tierra",
        "Núcleo"
    )
}
