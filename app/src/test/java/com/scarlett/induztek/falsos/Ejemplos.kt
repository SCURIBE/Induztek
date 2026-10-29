package com.scarlett.induztek.falsos

import com.scarlett.induztek.model.Medicion
import com.scarlett.induztek.model.Prueba
import com.scarlett.induztek.model.PruebaConMediciones
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida

// Atajos para armar datos de prueba sin repetir todos los campos en
// cada test. Solo se escribe lo que importa para el caso.

fun medicion(
    nombre: String,
    valor: Double,
    minimo: Double? = null,
    maximo: Double? = null,
    pruebaId: Int = 0
): Medicion {
    return Medicion(
        pruebaId = pruebaId,
        nombre = nombre,
        valor = valor,
        minimoAceptable = minimo,
        maximoAceptable = maximo
    )
}

fun pruebaCon(
    id: Int,
    fechaHora: Long,
    vararg mediciones: Medicion,
    equipoId: Int = 1,
    tipoEnsayo: TipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
    unidadMedida: UnidadMedida = UnidadMedida.MEGAOHM,
    cliente: String = "",
    tecnico: String = "",
    observaciones: String = ""
): PruebaConMediciones {
    return PruebaConMediciones(
        prueba = Prueba(
            id = id,
            equipoId = equipoId,
            fechaHora = fechaHora,
            ubicacion = "Planta A",
            tipoEnsayo = tipoEnsayo,
            unidadMedida = unidadMedida,
            cliente = cliente,
            tecnicoResponsable = tecnico,
            observaciones = observaciones
        ),
        mediciones = mediciones.map { it.copy(pruebaId = id) }
    )
}
