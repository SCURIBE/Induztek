package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.data.PruebaRepository
import com.scarlett.induztek.falsos.PruebaDaoFalso
import com.scarlett.induztek.falsos.ReglaDespachadorPrincipal
import com.scarlett.induztek.model.TipoEnsayo
import com.scarlett.induztek.model.UnidadMedida
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PruebaViewModelTest {

    @get:Rule
    val regla = ReglaDespachadorPrincipal()

    private val dao = PruebaDaoFalso()

    // by lazy: las propiedades se crean antes de que la regla reemplace
    // Dispatchers.Main, y el ViewModel arranca su init en ese hilo.
    private val viewModel by lazy { PruebaViewModel(PruebaRepository(dao)) }

    private val dosFases = listOf(
        MedicionIngresada("Fase A", "1200", minimoTexto = "1000"),
        MedicionIngresada("Fase B", "950,5", minimoTexto = "1000")
    )

    // Registra con datos válidos salvo lo que el test cambie.
    private fun registrar(
        equipoId: Int? = 1,
        ubicacion: String = "  Planta A ",
        unidad: UnidadMedida? = UnidadMedida.MEGAOHM,
        mediciones: List<MedicionIngresada> = dosFases
    ): Boolean? {
        var resultado: Boolean? = null

        viewModel.registrar(
            equipoId = equipoId,
            ubicacion = ubicacion,
            tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
            unidadMedida = unidad,
            mediciones = mediciones,
            cliente = " Minera Norte ",
            tecnicoResponsable = "Lu Pereira",
            observaciones = ""
        ) { resultado = it }

        return resultado
    }

    @Test
    fun `guarda la prueba con todas sus mediciones`() = runTest {
        assertEquals(true, registrar())

        val prueba = dao.pruebas.value.single()
        assertEquals("Planta A", prueba.ubicacion)
        assertEquals("Minera Norte", prueba.cliente)

        val mediciones = dao.mediciones.value
        assertEquals(listOf("Fase A", "Fase B"), mediciones.map { it.nombre })
        assertEquals(950.5, mediciones[1].valor, 0.0)
        // Cada medición quedó apuntando a la prueba recién creada.
        assertTrue(mediciones.all { it.pruebaId == prueba.id })

        assertNull(viewModel.error.value)
        assertTrue(viewModel.guardado.value)
    }

    @Test
    fun `la fecha se toma al guardar`() = runTest {
        val antes = System.currentTimeMillis()
        registrar()
        val despues = System.currentTimeMillis()

        val fecha = dao.pruebas.value.single().fechaHora
        assertTrue(fecha in antes..despues)
    }

    @Test
    fun `dos pruebas seguidas no comparten la hora de la primera`() = runTest {
        // Antes la pantalla calculaba la fecha una vez con remember, y
        // todas las pruebas de la sesión quedaban con la misma hora.
        registrar()
        Thread.sleep(5)
        registrar()

        val fechas = dao.pruebas.value.map { it.fechaHora }
        assertTrue(fechas[1] > fechas[0])
    }

    @Test
    fun `no guarda nada si falta un dato`() = runTest {
        assertEquals(false, registrar(equipoId = null))
        assertEquals("Selecciona un equipo.", viewModel.error.value)

        assertEquals(false, registrar(ubicacion = " "))
        assertEquals(false, registrar(unidad = UnidadMedida.MILISEGUNDO))
        assertEquals(false, registrar(mediciones = emptyList()))

        assertTrue(dao.pruebas.value.isEmpty())
        assertFalse(viewModel.guardado.value)
    }

    @Test
    fun `expone las pruebas guardadas de la mas nueva a la mas vieja`() = runTest {
        registrar()
        Thread.sleep(5)
        registrar(ubicacion = "Planta B")

        assertEquals(
            listOf("Planta B", "Planta A"),
            viewModel.pruebas.value.map { it.prueba.ubicacion }
        )
        assertEquals(2, viewModel.pruebas.value[0].mediciones.size)
    }
}
