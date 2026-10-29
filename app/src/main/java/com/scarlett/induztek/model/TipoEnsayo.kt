package com.scarlett.induztek.model

// Ensayos del protocolo de pruebas eléctricas.
//
// Cada ensayo declara las unidades que tienen sentido para él, así la
// pantalla de nueva prueba solo ofrece esas. Eso evita guardar, por
// ejemplo, una resistencia de aislamiento medida en milisegundos.
enum class TipoEnsayo(
    val etiqueta: String,
    val unidades: List<UnidadMedida>
) {
    RESISTENCIA_AISLAMIENTO(
        "Resistencia de aislamiento",
        listOf(UnidadMedida.MEGAOHM, UnidadMedida.OHM)
    ),
    RELACION_TRANSFORMACION(
        "Relación de transformación",
        listOf(UnidadMedida.RAZON, UnidadMedida.PORCENTAJE)
    ),
    RESISTENCIA_DEVANADOS(
        "Resistencia de devanados",
        listOf(UnidadMedida.OHM, UnidadMedida.MEGAOHM)
    ),
    TIEMPO_OPERACION_RELE(
        "Tiempo de operación de relé",
        listOf(UnidadMedida.MILISEGUNDO)
    ),
    OTRO(
        "Otro",
        listOf(
            UnidadMedida.MEGAOHM,
            UnidadMedida.OHM,
            UnidadMedida.MILISEGUNDO,
            UnidadMedida.KILOVOLT,
            UnidadMedida.VOLT,
            UnidadMedida.AMPERE,
            UnidadMedida.RAZON,
            UnidadMedida.PORCENTAJE
        )
    );

    // Unidad que queda preseleccionada al elegir el ensayo.
    val unidadPorDefecto: UnidadMedida
        get() = unidades.first()
}
