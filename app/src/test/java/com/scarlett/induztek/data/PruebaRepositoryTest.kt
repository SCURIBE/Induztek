
package com.scarlett.induztek.data

import com.scarlett.induztek.Repository.data.PruebaRepository
import com.scarlett.induztek.Repository.model.Medicion
import com.scarlett.induztek.Repository.model.Prueba
import com.scarlett.induztek.Repository.model.TipoEnsayo
import com.scarlett.induztek.Repository.model.UnidadMedida
import com.scarlett.induztek.falsos.PruebaDaoFalso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PruebaRepositoryTest {

    private val dao = PruebaDaoFalso()
    private val repository = PruebaRepository(dao)

    private fun crearPrueba(
        equipoId: Int = 1,
        fechaHora: Long = 1000L
    ) = Prueba(
        equipoId = equipoId,
        fechaHora = fechaHora,
        ubicacion = "Sala eléctrica",
        tipoEnsayo = TipoEnsayo.RESISTENCIA_AISLAMIENTO,
        unidadMedida = UnidadMedida.MEGAOHM
    )

    private fun crearMedicion(
        nombre: String = "Fase A",
        valor: Double = 50.0
    ) = Medicion(
        pruebaId = 0,
        nombre = nombre,
        valor = valor
    )

    @Test
    fun obtenerTodas_devuelveLasPruebasRegistradas() = runTest {
        repository.registrar(
            crearPrueba(),
            listOf(crearMedicion())
        )

        val resultado = repository.obtenerTodas().first()

        assertEquals(1, resultado.size)
        assertEquals(
            "Sala eléctrica",
            resultado.first().prueba.ubicacion
        )
    }

    @Test
    fun obtenerPorId_devuelveLaPruebaCorrespondiente() = runTest {
        repository.registrar(
            crearPrueba(),
            listOf(crearMedicion())
        )

        val resultado = repository.obtenerPorId(1).first()

        assertEquals(1, resultado?.prueba?.id)
        assertEquals(
            "Sala eléctrica",
            resultado?.prueba?.ubicacion
        )
    }

    @Test
    fun obtenerPorId_devuelveNullSiNoExiste() = runTest {
        val resultado = repository.obtenerPorId(999).first()

        assertNull(resultado)
    }

    @Test
    fun obtenerPorEquipo_devuelveSoloLasPruebasDelEquipo() = runTest {
        repository.registrar(
            crearPrueba(equipoId = 1),
            listOf(crearMedicion())
        )

        repository.registrar(
            crearPrueba(equipoId = 2, fechaHora = 2000L),
            listOf(crearMedicion("Fase B", 60.0))
        )

        val resultado = repository.obtenerPorEquipo(1).first()

        assertEquals(1, resultado.size)
        assertEquals(1, resultado.first().prueba.equipoId)
    }

    @Test
    fun registrar_guardaLaPruebaYLasMediciones() = runTest {
        val prueba = crearPrueba()

        val mediciones = listOf(
            crearMedicion("Fase A", 50.0),
            crearMedicion("Fase B", 60.0)
        )

        repository.registrar(prueba, mediciones)

        assertEquals(1, dao.pruebas.value.size)
        assertEquals(2, dao.mediciones.value.size)

        assertEquals(
            1,
            dao.mediciones.value[0].pruebaId
        )

        assertEquals(
            1,
            dao.mediciones.value[1].pruebaId
        )
    }
}
