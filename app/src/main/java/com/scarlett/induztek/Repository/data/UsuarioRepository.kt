package com.scarlett.induztek.Repository.data

import com.scarlett.induztek.Repository.model.TipoUsuario
import com.scarlett.induztek.Repository.model.Usuario
import com.scarlett.induztek.util.Hash

class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {
    suspend fun autenticar(
        nombreUsuario: String,
        contrasena: String
    ): Usuario? {
        val usuario = usuarioDao
            .buscarPorNombre(nombreUsuario.trim().lowercase())
            ?: return null

        return if (Hash.verificar(contrasena, usuario.credencial)) {
            usuario
        } else {
            null
        }
    }

    suspend fun registrar(
        nombreUsuario: String,
        nombreCompleto: String,
        contrasena: String
    ): Usuario {
        val usuario = Usuario(
            nombreUsuario = nombreUsuario.trim().lowercase(),
            nombreCompleto = nombreCompleto.trim(),
            credencial = Hash.generar(contrasena),
            creadoEn = System.currentTimeMillis(),
            tipoUsuarioId = TipoUsuario.ID_TECNICO
        )

        val id = usuarioDao.insertar(usuario).toInt()
        return usuario.copy(id = id)
    }

    suspend fun existeNombre(nombreUsuario: String): Boolean =
        usuarioDao.existeNombre(nombreUsuario.trim().lowercase())

    suspend fun buscarPorId(id: Int): Usuario? =
        usuarioDao.buscarPorId(id)
}
