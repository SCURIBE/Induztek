
package com.scarlett.induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scarlett.induztek.Repository.model.Equipo
import com.scarlett.induztek.Repository.model.TipoEquipo
import com.scarlett.induztek.ui.componentes.CampoSeleccion
import com.scarlett.induztek.ui.componentes.SelectorFoto
import com.scarlett.induztek.util.AlmacenFotos

@Composable
fun EditarEquipoScreen(
    equipo: Equipo,
    errorExterno: String?,
    alGuardar: (Equipo, (Boolean) -> Unit) -> Unit,
    alCancelar: () -> Unit
) {
    var codigo by rememberSaveable(equipo.id) {
        mutableStateOf(equipo.codigo)
    }

    var tipo by rememberSaveable(equipo.id) {
        mutableStateOf(equipo.tipo)
    }

    var fotoUri by rememberSaveable(equipo.id) {
        mutableStateOf(equipo.fotoUri)
    }

    var errorLocal by remember {
        mutableStateOf<String?>(null)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    val mensajeError = errorExterno ?: errorLocal

    // Descarta una fotografía nueva si se cancela o se reemplaza.
    fun descartarFotoNueva() {
        if (fotoUri != equipo.fotoUri) {
            AlmacenFotos.borrar(fotoUri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Editar equipo",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Modifica los datos del equipo seleccionado.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = codigo,
            onValueChange = {
                codigo = it
                errorLocal = null
            },
            label = {
                Text("Código del equipo")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = mensajeError != null && codigo.isBlank(),
            enabled = !guardando
        )

        CampoSeleccion(
            etiqueta = "Tipo de equipo",
            textoVacio = "Selecciona el tipo de equipo",
            opciones = TipoEquipo.entries,
            seleccion = tipo,
            textoDe = { it.etiqueta },
            alSeleccionar = {
                tipo = it
                errorLocal = null
            },
            habilitado = !guardando
        )

        Text(
            text = "Fotografía del equipo",
            style = MaterialTheme.typography.titleMedium
        )

        SelectorFoto(
            fotoUri = fotoUri,
            alCambiar = { nueva ->
                descartarFotoNueva()
                fotoUri = nueva
                errorLocal = null
            },
            alError = {
                errorLocal = it
            },
            habilitado = !guardando,
            textoCamara = "Tomar nueva fotografía",
            textoGaleria = "Cambiar desde galería",
            textoSinFoto = "Este equipo no tiene fotografía."
        )

        mensajeError?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                errorLocal = null

                if (codigo.isBlank()) {
                    errorLocal = "Ingresa el código del equipo."
                } else {
                    guardando = true

                    val equipoActualizado = equipo.copy(
                        codigo = codigo.trim(),
                        tipo = tipo,
                        fotoUri = fotoUri
                    )

                    alGuardar(equipoActualizado) {
                        guardando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            Text(
                if (guardando) "Guardando..." else "Guardar cambios"
            )
        }

        OutlinedButton(
            onClick = {
                descartarFotoNueva()
                alCancelar()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !guardando
        ) {
            Text("Cancelar")
        }
    }
}
