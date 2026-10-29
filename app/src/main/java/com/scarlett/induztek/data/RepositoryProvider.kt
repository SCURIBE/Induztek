
package com.scarlett.induztek.data

import android.content.Context

object RepositoryProvider {

    fun obtenerPruebaRepository(context: Context): PruebaRepository {
        val baseDatos = DatabaseProvider.obtenerBaseDatos(context)
        return PruebaRepository(baseDatos.pruebaDao())
    }

    fun obtenerEquipoRepository(context: Context): EquipoRepository {
        val baseDatos = DatabaseProvider.obtenerBaseDatos(context)
        return EquipoRepository(baseDatos.equipoDao())
    }

    fun obtenerUsuarioRepository(context: Context): UsuarioRepository {
        val baseDatos = DatabaseProvider.obtenerBaseDatos(context)
        return UsuarioRepository(baseDatos.usuarioDao())
    }
}
