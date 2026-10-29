package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.falsos.medicion
import com.scarlett.induztek.falsos.pruebaCon
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HistorialComparacionTest {

    private val actual = pruebaCon(10, fechaHora = 5_000, medicion("Fase A", 12.0))

    @Test
    fun `elige la mas reciente de las anteriores comparables`() {
        val vieja = pruebaCon(1, fechaHora = 1_000, medicion("Fase A", 8.0))
        val reciente = pruebaCon(2, fechaHora = 3_000, medicion("Fase A", 10.0))

        val anterior = HistorialComparacion.anteriorComparable(
            actual,
            listOf(vieja, actual, reciente)
        )

        assertSame(reciente, anterior)
    }

    @Test
    fun `no compara con otro equipo otro ensayo u otra unidad`() {
        val todas = listOf(
            actual,
            pruebaCon(1, 1_000, medicion("Fase A", 1.0), equipoId = 2),
            pruebaCon(
                2, 2_000, medicion("Fase A", 1.0),
                tipoEnsayo = TipoEnsayo.RESISTENCIA_DEVANADOS
            ),
            pruebaCon(3, 3_000, medicion("Fase A", 1.0), unidadMedida = UnidadMedida.OHM)
        )

        assertNull(HistorialComparacion.anteriorComparable(actual, todas))
    }

    @Test
    fun `no se compara consigo misma ni con una posterior`() {
        val posterior = pruebaCon(11, fechaHora = 9_000, medicion("Fase A", 1.0))

        assertNull(
            HistorialComparacion.anteriorComparable(actual, listOf(actual, posterior))
        )
    }

    @Test
    fun `empareja por nombre de punto y no por posicion`() {
        // La visita anterior midió las tres fases; esta, solo la B. La
        // fase B tiene que compararse con la fase B, no con la primera
        // medición de la lista.
        val anterior = pruebaCon(
            1, 1_000,
            medicion("Fase A", 100.0),
            medicion("Fase B", 200.0),
            medicion("Fase C", 300.0)
        )
        val soloFaseB = pruebaCon(2, 2_000, medicion("Fase B", 210.0))

        val comparacion = HistorialComparacion.comparar(soloFaseB, anterior)

        assertEquals(1, comparacion.size)
        assertEquals(200.0, comparacion[0].anterior!!.valor, 0.0)
        assertEquals(10.0, comparacion[0].diferencia!!, 1e-9)
    }

    @Test
    fun `el nombre se compara sin mayusculas ni espacios`() {
        val anterior = pruebaCon(1, 1_000, medicion(" FASE A ", 100.0))
        val nueva = pruebaCon(2, 2_000, medicion("Fase A", 90.0))

        val comparacion = HistorialComparacion.comparar(nueva, anterior)

        assertEquals(-10.0, comparacion[0].diferencia!!, 1e-9)
    }

    @Test
    fun `un punto que no se midio antes queda sin anterior`() {
        val anterior = pruebaCon(1, 1_000, medicion("Fase A", 100.0))
        val nueva = pruebaCon(
            2, 2_000,
            medicion("Fase A", 90.0),
            medicion("Neutro", 5.0)
        )

        val comparacion = HistorialComparacion.comparar(nueva, anterior)

        assertEquals(2, comparacion.size)
        assertNull(comparacion[1].anterior)
        assertNull(comparacion[1].diferencia)
    }

    @Test
    fun `sin prueba anterior ninguna medicion tiene diferencia`() {
        val comparacion = HistorialComparacion.comparar(actual, null)

        assertEquals(1, comparacion.size)
        assertNull(comparacion[0].anterior)
    }
}
