package com.scarlett.induztek.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Hash {
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"
    private const val ITERACIONES = 100_000
    private const val BITS = 256
    private const val BYTES_SALT = 16

    fun generar(contrasena: String): String {
        val salt = ByteArray(BYTES_SALT)
        SecureRandom().nextBytes(salt)

        val hash = derivar(contrasena, salt, ITERACIONES)

        return "pbkdf2sha1$$ITERACIONES$${aHex(salt)}$${aHex(hash)}"
    }

    fun verificar(contrasena: String, almacenado: String): Boolean {
        val partes = almacenado.split("$")
        if (partes.size != 4 || partes[0] != "pbkdf2sha1") {
            return false
        }

        val iteraciones = partes[1].toIntOrNull() ?: return false
        val salt = desdeHex(partes[2]) ?: return false
        val esperado = desdeHex(partes[3]) ?: return false
        val calculado = derivar(contrasena, salt, iteraciones)

        return MessageDigest.isEqual(calculado, esperado)
    }

    private fun derivar(
        contrasena: String,
        salt: ByteArray,
        iteraciones: Int
    ): ByteArray {
        val spec = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            iteraciones,
            BITS
        )
        return SecretKeyFactory
            .getInstance(ALGORITMO)
            .generateSecret(spec)
            .encoded
    }

    private fun aHex(bytes: ByteArray): String {
        return bytes.joinToString("") { byte ->
            val valor = byte.toInt() and 0xFF
            valor.toString(16).padStart(2, '0')
        }
    }

    private fun desdeHex(texto: String): ByteArray? {
        if (texto.length % 2 != 0) {
            return null
        }

        return try {
            texto.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        } catch (_: NumberFormatException) {
            null
        }
    }
}
