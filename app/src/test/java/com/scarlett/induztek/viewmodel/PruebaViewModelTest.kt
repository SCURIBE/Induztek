
package com.scarlett.induztek.viewmodel

import com.scarlett.induztek.Repository.data.PruebaRepository
import com.scarlett.induztek.falsos.PruebaDaoFalso
import com.scarlett.induztek.falsos.ReglaDespachadorPrincipal
import com.scarlett.induztek.Repository.model.TipoEnsayo
import com.scarlett.induztek.Repository.model.UnidadMedida
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

    private val viewModel by lazy {
        PruebaViewModel(PruebaRepository(dao))
    }

    private val dosFases = listOf(
        MedicionIngresada("Fase A", "1200", minimoTexto = "1000"),
        MedicionIngresada("Fase B", "950,5", minimoTexto = "1000")
    )

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
        ) {
            resultado = it
        }

        return resultado
    }

    private fun editar(
        pruebaId: Int = 1,
        equipoId: Int? = 1,
        ubicacion: String = "Planta B",
        mediciones: List<MedicionIngresada> = listOf(
            MedicionIngresada(
                "Fase C",
                "1500",
                minimoTexto = "1000"
            )
        )
    ): Boolean? {
        var resultado: Boolean? = null

        val prueba = dao.pruebas.value.first {
            it.id == pruebaId
        }

        viewModel.editar(
            prueba = prueba,
            equipoId = equipoId,
            ubicacion = ubicacion,
            tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
            unidadMedida = UnidadMedida.MEGAOHM,
            mediciones = mediciones,
            cliente = "Cliente actualizado",
            tecnicoResponsable = "Tecnico actualizado",
            observaciones = "Observacion actualizada"
        ) {
            resultado = it
        }

        return resultado
    }

    // PRUEBAS DE REGISTRO

    @Test
    fun `guarda la prueba con todas sus mediciones`() = runTest {
        assertEquals(true, registrar())

        val prueba = dao.pruebas.value.single()

        assertEquals("Planta A", prueba.ubicacion)
        assertEquals("Minera Norte", prueba.cliente)

        val mediciones = dao.mediciones.value

        assertEquals(
            listOf("Fase A", "Fase B"),
            mediciones.map { it.nombre }
        )

        assertEquals(950.5, mediciones[1].valor, 0.0)

        assertTrue(
            mediciones.all { it.pruebaId == prueba.id }
        )

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

    // PRUEBAS DE EDICIÓN

    @Test
    fun `actualiza una prueba existente sin duplicarla`() = runTest {
        registrar()

        assertEquals(1, dao.pruebas.value.size)

        assertEquals(true, editar())

        assertEquals(1, dao.pruebas.value.size)
        assertEquals("Planta B", dao.pruebas.value.single().ubicacion)
    }

    @Test
    fun `al editar conserva el id y la fecha original`() = runTest {
        registrar()

        val original = dao.pruebas.value.single()
        val idOriginal = original.id
        val fechaOriginal = original.fechaHora

        assertEquals(true, editar())

        val actualizada = dao.pruebas.value.single()

        assertEquals(idOriginal, actualizada.id)
        assertEquals(fechaOriginal, actualizada.fechaHora)
    }

    @Test
    fun `al editar reemplaza las mediciones anteriores`() = runTest {
        registrar()

        assertEquals(2, dao.mediciones.value.size)

        assertEquals(true, editar())

        val prueba = dao.pruebas.value.single()
        val medicionesActualizadas = dao.mediciones.value

        assertEquals(1, medicionesActualizadas.size)
        assertEquals("Fase C", medicionesActualizadas.single().nombre)
        assertEquals(1500.0, medicionesActualizadas.single().valor, 0.0)

        assertTrue(
            medicionesActualizadas.all {
                it.pruebaId == prueba.id
            }
        )
    }

    @Test
    fun `una edicion invalida no modifica la prueba existente`() = runTest {
        registrar()

        val original = dao.pruebas.value.single()
        val medicionesOriginales = dao.mediciones.value.toList()

        assertEquals(false, editar(equipoId = null))

        assertEquals(original, dao.pruebas.value.single())
        assertEquals(medicionesOriginales, dao.mediciones.value)
        assertFalse(viewModel.guardado.value)
    }

    // PRUEBAS DE LIMPIEZA

    @Test
    fun `limpiarError elimina el mensaje de error`() = runTest {
        registrar(equipoId = null)

        assertEquals(
            "Selecciona un equipo.",
            viewModel.error.value
        )

        viewModel.limpiarError()

        assertNull(viewModel.error.value)
    }

    @Test
    fun `limpiarGuardado cambia el estado a falso`() = runTest {
        registrar()

        assertTrue(viewModel.guardado.value)

        viewModel.limpiarGuardado()

        assertFalse(viewModel.guardado.value)
    }
}
