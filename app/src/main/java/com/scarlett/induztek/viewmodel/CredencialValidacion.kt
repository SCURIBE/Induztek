package com.scarlett.induztek.viewmodel

object CredencialValidacion {

    private const val MINIMO_USUARIO = 3
    private const val MINIMO_CONTRASENA = 8

    fun validarNombreUsuario(nombreUsuario: String): String? {
        val limpio = nombreUsuario.trim()

        return when {
            limpio.isBlank() -> "Ingresa tu usuario."
            limpio.length < MINIMO_USUARIO ->
                "El usuario debe tener al menos $MINIMO_USUARIO caracteres."

            limpio.contains(" ") -> "El usuario no puede tener espacios."
            else -> null
        }
    }

    fun validarContrasenaLogin(contrasena: String): String? {
        return if (contrasena.isBlank()) {
            "Ingresa tu contraseña."
        } else {
            null
        }
    }

    fun validarContrasenaNueva(contrasena: String): String? {
        return when {
            contrasena.isBlank() -> "Ingresa una contraseña."
            contrasena.length < MINIMO_CONTRASENA ->
                "Debe tener al menos $MINIMO_CONTRASENA caracteres."

            contrasena.none { it.isDigit() } ->
                "Debe incluir al menos un número."

            contrasena.none { it.isLetter() } ->
                "Debe incluir al menos una letra."

            else -> null
        }
    }
    fun validarConfirmacion(
        contrasena: String,
        confirmacion: String
    ): String? {
        return if (contrasena != confirmacion) {
            "Las contraseñas no coinciden."
        } else {
            null
        }
    }

    fun validarNombreCompleto(nombre: String): String? {
        return if (nombre.isBlank()) {
            "Ingresa tu nombre."
        } else {
            null
        }
    }
}