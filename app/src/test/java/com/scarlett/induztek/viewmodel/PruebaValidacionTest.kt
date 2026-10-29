package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PruebaValidacionTest {

    @Test
    fun `exige equipo ubicacion y tipo de ensayo`() {
        assertNotNull(PruebaValidacion.validarEquipo(null))
        assertNull(PruebaValidacion.validarEquipo(3))

        assertNotNull(PruebaValidacion.validarUbicacion("   "))
        assertNull(PruebaValidacion.validarUbicacion("Planta A"))

        assertNotNull(PruebaValidacion.validarTipoEnsayo(null))
        assertNull(
            PruebaValidacion.validarTipoEnsayo(TipoEnsayo.RESISTENCIA_AISLAMIENTO)
        )
    }

    @Test
    fun `la unidad tiene que corresponder al tipo de ensayo`() {
        val aislamiento = TipoEnsayo.RESISTENCIA_AISLAMIENTO

        assertNull(PruebaValidacion.validarUnidad(aislamiento, UnidadMedida.MEGAOHM))
        assertEquals(
            "Esa unidad no corresponde al tipo de ensayo.",
            PruebaValidacion.validarUnidad(aislamiento, UnidadMedida.MILISEGUNDO)
        )
        assertEquals(
            "Selecciona la unidad de medida.",
            PruebaValidacion.validarUnidad(aislamiento, null)
        )
    }

    @Test
    fun `acepta la medicion con coma o con punto`() {
        assertNull(PruebaValidacion.validarMedicion("12,5"))
        assertNull(PruebaValidacion.validarMedicion("12.5"))
        assertNull(PruebaValidacion.validarMedicion("  -3 "))

        assertEquals(12.5, PruebaValidacion.medicionDesdeTexto("12,5")!!, 0.0)
        assertEquals(12.5, PruebaValidacion.medicionDesdeTexto(" 12.5 ")!!, 0.0)
    }

    @Test
    fun `rechaza una medicion vacia o que no es numero`() {
        assertEquals(
            "Ingresa el resultado de la medición.",
            PruebaValidacion.validarMedicion("")
        )
        assertEquals(
            "Ingresa un número válido.",
            PruebaValidacion.validarMedicion("doce")
        )
        // toDouble acepta estas dos palabras, pero no son una medición.
        assertNotNull(PruebaValidacion.validarMedicion("NaN"))
        assertNotNull(PruebaValidacion.validarMedicion("Infinity"))
    }

    @Test
    fun `el rango es opcional`() {
        assertNull(PruebaValidacion.validarRango("", ""))
        assertNull(PruebaValidacion.validarRango("10", ""))
        assertNull(PruebaValidacion.validarRango("", "20,5"))
        // Un rango de un solo valor es válido: exige ese valor exacto.
        assertNull(PruebaValidacion.validarRango("10", "10"))
    }

    @Test
    fun `rechaza un rango al reves o no numerico`() {
        assertEquals(
            "El mínimo no puede ser mayor que el máximo.",
            PruebaValidacion.validarRango("20", "10")
        )
        assertEquals(
            "El mínimo debe ser un número.",
            PruebaValidacion.validarRango("diez", "20")
        )
        assertEquals(
            "El máximo debe ser un número.",
            PruebaValidacion.validarRango("10", "veinte")
        )
    }

    @Test
    fun `exige al menos una medicion`() {
        assertEquals(
            "Agrega al menos una medición.",
            PruebaValidacion.validarMediciones(emptyList())
        )
    }

    @Test
    fun `no acepta dos mediciones del mismo punto`() {
        // Distinto en mayúsculas y espacios, pero es el mismo punto: la
        // comparación del historial no sabría cuál emparejar.
        val mediciones = listOf(
            MedicionIngresada("Fase A", "10"),
            MedicionIngresada(" fase a ", "11")
        )

        assertEquals(
            "Hay dos mediciones con el mismo punto.",
            PruebaValidacion.validarMediciones(mediciones)
        )
    }

    @Test
    fun `informa el error de una fila aunque las otras esten bien`() {
        val mediciones = listOf(
            MedicionIngresada("Fase A", "10"),
            MedicionIngresada("Fase B", "10", minimoTexto = "20", maximoTexto = "5")
        )

        assertEquals(
            "El mínimo no puede ser mayor que el máximo.",
            PruebaValidacion.validarMediciones(mediciones)
        )
    }

    @Test
    fun `convierte las filas en mediciones`() {
        val mediciones = PruebaValidacion.aMediciones(
            listOf(
                MedicionIngresada(" Fase A ", "12,5", minimoTexto = "10"),
                MedicionIngresada("Fase B", "8")
            )
        )

        assertEquals(2, mediciones.size)
        assertEquals("Fase A", mediciones[0].nombre)
        assertEquals(12.5, mediciones[0].valor, 0.0)
        assertEquals(10.0, mediciones[0].minimoAceptable!!, 0.0)
        assertNull(mediciones[0].maximoAceptable)
        assertNull(mediciones[1].minimoAceptable)
        // El id de la prueba lo asigna el DAO al guardar.
        assertEquals(0, mediciones[0].pruebaId)
    }
}
