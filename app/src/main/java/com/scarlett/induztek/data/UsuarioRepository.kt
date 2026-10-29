package com.scarlett.induztek.data

import com.scarlett.induztek.model.Usuario
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
            creadoEn = System.currentTimeMillis()
        )

        val id = usuarioDao.insertar(usuario).toInt()
        return usuario.copy(id = id)
    }

    suspend fun existeNombre(nombreUsuario: String): Boolean =
        usuarioDao.existeNombre(nombreUsuario.trim().lowercase())

    suspend fun buscarPorId(id: Int): Usuario? =
        usuarioDao.buscarPorId(id)

    /*
     * Usuario de demostracion. Se crea solo si la tabla esta vacia, para
     * que la app se pueda revisar sin tener que registrarse primero.
     *
     * La contrasena esta escrita en el codigo, asi que cualquiera que
     * abra el APK la puede leer: sirve para la entrega academica, no
     * para un despliegue real.
     */
    suspend fun sembrarSiEstaVacia() {
        if (usuarioDao.contar() > 0) {
            return
        }

        registrar(
            nombreUsuario = USUARIO_DEMO,
            nombreCompleto = "Tecnico de terreno",
            contrasena = CONTRASENA_DEMO
        )
    }

    companion object {
        const val USUARIO_DEMO = "tecnico"
        const val CONTRASENA_DEMO = "Induztek2026"
    }
}
