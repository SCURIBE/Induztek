package com.scarlett.induztek.falsos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/*
 * viewModelScope corre en Dispatchers.Main, el hilo de la interfaz, que
 * en un test de JUnit no existe. Esta regla lo reemplaza por un
 * despachador de prueba durante cada test.
 *
 * UnconfinedTestDispatcher ejecuta las corrutinas en el acto: cuando
 * vuelve la llamada al ViewModel, lo que lanzó ya terminó, y el test
 * puede revisar el resultado en la línea siguiente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReglaDespachadorPrincipal(
    val despachador: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(despachador)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
