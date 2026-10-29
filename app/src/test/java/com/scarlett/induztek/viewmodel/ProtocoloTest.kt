package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.falsos.medicion
import com.scarlett.induztek.falsos.pruebaCon
import com.scarlett.induztek.model.Equipo
import com.scarlett.induztek.model.ResultadoMedicion
import com.scarlett.induztek.model.TipoEquipo
import com.scarlett.induztek.util.FechaFormato
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocoloTest {

    private val equipo = Equipo(id = 1, tipo = TipoEquipo.TRANSFORMADOR, codigo = "EQ-001")

    private val anterior = pruebaCon(
        1, 1_000,
        medicion("Fase A", 100.0),
        medicion("Fase B", 200.0)
    )

    private val actual = pruebaCon(
        42, 5_000,
        medicion("Fase A", 120.0, minimo = 100.0),
        medicion("Fase B", 50.0, minimo = 100.0),
        medicion("Neutro", 3.0),
        cliente = "Minera Norte",
        observaciones = "  Bornes con óxido.  "
    )

    private fun armar() = Protocolo.armar(actual, equipo, anterior, generadoEn = 9_000)

    @Test
    fun `incluye los datos de la prueba`() {
        val datos = armar().datos.toMap()

        assertEquals("P-000042", datos["N.º de protocolo"])
        assertEquals("EQ-001 — Transformador", datos["Equipo"])
        assertEquals(FechaFormato.formatear(5_000), datos["Fecha y hora"])
        assertEquals("Minera Norte", datos["Cliente"])
        assertEquals("MΩ", datos["Unidad de medida"])
    }

    @Test
    fun `un dato vacio se informa en vez de omitirse`() {
        // En un documento formal, un campo en blanco no se distingue de
        // uno que se olvidó imprimir.
        assertEquals(
            Protocolo.NO_INFORMADO,
            armar().datos.toMap()["Técnico responsable"]
        )
    }

    @Test
    fun `aclara cuando el equipo ya no existe`() {
        val contenido = Protocolo.armar(actual, null, anterior, 9_000)

        assertEquals("(equipo eliminado)", contenido.datos.toMap()["Equipo"])
        assertTrue(contenido.nombreArchivo.startsWith("Protocolo_equipo_"))
    }

    @Test
    fun `cada medicion es una fila con su resultado y su comparacion`() {
        val filas = armar().filas

        assertEquals(listOf("Fase A", "Fase B", "Neutro"), filas.map { it.punto })

        assertEquals("120.00 MΩ", filas[0].valor)
        assertEquals("≥ 100.00", filas[0].rango)
        assertEquals(ResultadoMedicion.APROBADO, filas[0].resultado)
        assertEquals("100.00 MΩ", filas[0].anterior)
        assertEquals("+20.00", filas[0].diferencia)

        assertEquals("Rechazado", filas[1].resultadoTexto)
        assertEquals("-150.00", filas[1].diferencia)

        // Neutro no tiene rango ni se midió en la visita anterior.
        assertEquals("No evaluado", filas[2].resultadoTexto)
        assertEquals("Sin rango", filas[2].rango)
        assertEquals(Protocolo.SIN_DATO, filas[2].anterior)
        assertEquals(Protocolo.SIN_DATO, filas[2].diferencia)
    }

    @Test
    fun `el resumen cuenta solo las mediciones evaluadas`() {
        val contenido = armar()

        assertEquals(1, contenido.fueraDeRango)
        assertEquals(
            "1 de 2 mediciones evaluadas fuera del rango aceptable.",
            contenido.resumen
        )
    }

    @Test
    fun `resumen para cada caso`() {
        assertEquals("La prueba no tiene mediciones.", Protocolo.resumen(0, 0, 0))
        assertTrue(Protocolo.resumen(3, 0, 0).startsWith("Ninguna medición"))
        assertEquals(
            "Todas las mediciones dentro del rango aceptable.",
            Protocolo.resumen(3, 3, 0)
        )
        assertTrue(Protocolo.resumen(3, 2, 0).contains("1 sin rango definido"))
    }

    @Test
    fun `los rangos se escriben segun los limites que tengan`() {
        assertEquals("10.00 a 20.00", Protocolo.rango(medicion("A", 1.0, 10.0, 20.0)))
        assertEquals("≤ 20.00", Protocolo.rango(medicion("A", 1.0, maximo = 20.0)))
    }

    @Test
    fun `explica con que prueba se compara`() {
        assertTrue(armar().comparacion.contains(FechaFormato.formatear(1_000)))

        val sinAnterior = Protocolo.armar(actual, equipo, null, 9_000)
        assertTrue(sinAnterior.comparacion.startsWith("Sin prueba anterior"))
    }

    @Test
    fun `las observaciones se recortan`() {
        assertEquals("Bornes con óxido.", armar().observaciones)
    }

    @Test
    fun `el nombre del archivo no lleva espacios barras ni tildes`() {
        assertEquals(
            "Protocolo_EQ_01_Subestacion_2026-10-29_14_30.pdf",
            Protocolo.nombreArchivo("EQ 01/Subestación", "2026-10-29 14:30")
        )
    }
}
