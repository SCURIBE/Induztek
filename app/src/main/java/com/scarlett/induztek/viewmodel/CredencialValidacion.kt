package com.scarlett.induztek.viewmodel

object CredencialValidacion {

    private const val MINIMO_CONTRASENA = 8
    private val FORMATO_CORREO =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun validarCorreoLogin(correo: String): String? {
        return if (correo.isBlank()) {
            "Ingresa tu correo."
        } else {
            null
        }
    }

    fun validarCorreoNuevo(correo: String): String? {
        val limpio = correo.trim()

        return when {
            limpio.isBlank() -> "Ingresa tu correo."
            !FORMATO_CORREO.matches(limpio) -> "Ingresa un correo válido."
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