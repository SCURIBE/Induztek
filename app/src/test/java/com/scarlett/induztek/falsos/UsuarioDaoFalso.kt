package com.scarlett.induztek.falsos

import com.scarlett.induztek.data.UsuarioDao
import com.scarlett.induztek.model.Usuario

// UsuarioDao en memoria.
class UsuarioDaoFalso(iniciales: List<Usuario> = emptyList()) : UsuarioDao {

    val usuarios = iniciales.toMutableList()
    private var siguienteId = (iniciales.maxOfOrNull { it.id } ?: 0) + 1

    override suspend fun insertar(usuario: Usuario): Long {
        val id = siguienteId++
        usuarios += usuario.copy(id = id)
        return id.toLong()
    }

    override suspend fun buscarPorNombre(nombreUsuario: String): Usuario? {
        return usuarios.find { it.nombreUsuario == nombreUsuario }
    }

    override suspend fun buscarPorId(id: Int): Usuario? {
        return usuarios.find { it.id == id }
    }

    override suspend fun existeNombre(nombreUsuario: String): Boolean {
        return usuarios.any { it.nombreUsuario == nombreUsuario }
    }

    override suspend fun contar(): Int {
        return usuarios.size
    }
}
