package com.scarlett.induztek.model

import com.scarlett.induztek.falsos.medicion
import org.junit.Assert.assertEquals
import org.junit.Test

// El resultado no se guarda: se deduce del valor y del rango cada vez.
class MedicionTest {

    @Test
    fun `sin rango no se evalua`() {
        assertEquals(
            ResultadoMedicion.NO_EVALUADO,
            medicion("Fase A", 5.0).resultado
        )
    }

    @Test
    fun `fuera del rango se rechaza`() {
        assertEquals(
            ResultadoMedicion.RECHAZADO,
            medicion("Fase A", 5.0, minimo = 10.0, maximo = 20.0).resultado
        )
        assertEquals(
            ResultadoMedicion.RECHAZADO,
            medicion("Fase A", 25.0, minimo = 10.0, maximo = 20.0).resultado
        )
    }

    @Test
    fun `los bordes del rango se aprueban`() {
        assertEquals(
            ResultadoMedicion.APROBADO,
            medicion("Fase A", 10.0, minimo = 10.0, maximo = 20.0).resultado
        )
        assertEquals(
            ResultadoMedicion.APROBADO,
            medicion("Fase A", 20.0, minimo = 10.0, maximo = 20.0).resultado
        )
    }

    @Test
    fun `basta con uno de los dos limites`() {
        assertEquals(
            ResultadoMedicion.APROBADO,
            medicion("Fase A", 1000.0, minimo = 100.0).resultado
        )
        assertEquals(
            ResultadoMedicion.RECHAZADO,
            medicion("Fase A", 50.0, minimo = 100.0).resultado
        )
        assertEquals(
            ResultadoMedicion.RECHAZADO,
            medicion("Fase A", 50.0, maximo = 40.0).resultado
        )
    }
}
