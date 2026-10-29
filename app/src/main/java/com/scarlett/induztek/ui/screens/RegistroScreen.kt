package com.scarlett.induztek.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.scarlett.induztek.viewmodel.AuthViewModel
import com.scarlett.induztek.viewmodel.CredencialValidacion

@Composable
fun RegistroScreen(
    authViewModel: AuthViewModel,
    alVolver: () -> Unit
) {
    val error by authViewModel.error.collectAsState()
    val ocupado by authViewModel.ocupado.collectAsState()

    var nombreCompleto by remember { mutableStateOf("") }
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var verContrasena by remember { mutableStateOf(false) }
    var intentoRegistrar by remember { mutableStateOf(false) }

    // Los avisos por campo salen de las mismas funciones que revisa el
    // ViewModel al guardar, y solo aparecen tras el primer intento.
    val avisoNombre = CredencialValidacion.validarNombreCompleto(nombreCompleto)
    val avisoUsuario = CredencialValidacion.validarNombreUsuario(usuario)
    val avisoContrasena = CredencialValidacion.validarContrasenaNueva(contrasena)
    val avisoConfirmacion =
        CredencialValidacion.validarConfirmacion(contrasena, confirmacion)

    val registrar = {
        intentoRegistrar = true

        authViewModel.registrar(
            nombreUsuario = usuario,
            nombreCompleto = nombreCompleto,
            contrasena = contrasena,
            confirmacion = confirmacion
        )
    }

    val limpiarAvisos = {
        intentoRegistrar = false
        authViewModel.limpiarError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "El nombre queda registrado en cada protocolo que levantes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombreCompleto,
            onValueChange = {
                nombreCompleto = it
                limpiarAvisos()
            },
            label = { Text("Nombre y apellido") },
            singleLine = true,
            enabled = !ocupado,
            isError = intentoRegistrar && avisoNombre != null,
            supportingText = {
                if (intentoRegistrar && avisoNombre != null) {
                    Text(avisoNombre)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = usuario,
            onValueChange = {
                usuario = it
                limpiarAvisos()
            },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !ocupado,
            isError = intentoRegistrar && avisoUsuario != null,
            supportingText = {
                if (intentoRegistrar && avisoUsuario != null) {
                    Text(avisoUsuario)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contrasena,
            onValueChange = {
                contrasena = it
                limpiarAvisos()
            },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !ocupado,
            isError = intentoRegistrar && avisoContrasena != null,
            supportingText = {
                Text(
                    if (intentoRegistrar && avisoContrasena != null) {
                        avisoContrasena
                    } else {
                        "Mínimo 8 caracteres, con una letra y un número."
                    }
                )
            },
            visualTransformation = if (verContrasena) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton(onClick = { verContrasena = !verContrasena }) {
                    Icon(
                        imageVector = if (verContrasena) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (verContrasena) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmacion,
            onValueChange = {
                confirmacion = it
                limpiarAvisos()
            },
            label = { Text("Repite la contraseña") },
            singleLine = true,
            enabled = !ocupado,
            isError = intentoRegistrar && avisoConfirmacion != null,
            supportingText = {
                if (intentoRegistrar && avisoConfirmacion != null) {
                    Text(avisoConfirmacion)
                }
            },
            visualTransformation = if (verContrasena) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { registrar() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = registrar,
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ocupado) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp)
                )
            } else {
                Text("Crear cuenta y entrar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = alVolver,
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ya tengo cuenta")
        }
    }
}
