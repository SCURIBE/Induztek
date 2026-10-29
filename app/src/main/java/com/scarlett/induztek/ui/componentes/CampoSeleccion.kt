package com.scarlett.induztek.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/*
 * Campo de solo lectura que abre un menú para elegir una opción de una
 * lista fija. Se usa para el equipo, el tipo de ensayo, la unidad y el
 * tipo de equipo, que antes eran campos de texto libre.
 *
 * Es genérico porque las opciones son de tipos distintos (Equipo,
 * TipoEnsayo, UnidadMedida...); textoDe dice cómo mostrar cada una.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CampoSeleccion(
    etiqueta: String,
    textoVacio: String,
    opciones: List<T>,
    seleccion: T?,
    textoDe: (T) -> String,
    alSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
    esInvalido: Boolean = false,
    mensajeError: String = "",
    habilitado: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = {
            if (habilitado) {
                expandido = !expandido
            }
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccion?.let(textoDe) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(etiqueta) },
            placeholder = { Text(textoVacio) },
            isError = esInvalido,
            supportingText = {
                if (esInvalido && mensajeError.isNotBlank()) {
                    Text(mensajeError)
                }
            },
            trailingIcon = {
                if (esInvalido) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = etiqueta
                    )
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expandido
                    )
                }
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expandido && habilitado,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoDe(opcion)) },
                    onClick = {
                        alSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}
