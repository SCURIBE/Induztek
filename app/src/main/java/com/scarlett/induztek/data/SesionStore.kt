package com.scarlett.induztek.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sesionDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "sesion")

class SesionStore(context: Context) : AlmacenSesion {

    // applicationContext a proposito: el delegado preferencesDataStore
    // guarda una instancia por Context, y dos instancias sobre el mismo
    // archivo (al rotar la pantalla, por ejemplo) lanzan una excepcion.
    private val context = context.applicationContext
    private val claveUsuarioId = intPreferencesKey("usuario_id")

    override val usuarioId: Flow<Int?> = context.sesionDataStore.data
        .map { preferencias -> preferencias[claveUsuarioId] }

    override suspend fun guardar(usuarioId: Int) {
        context.sesionDataStore.edit { preferencias ->
            preferencias[claveUsuarioId] = usuarioId
        }
    }

    override suspend fun cerrar() {
        context.sesionDataStore.edit { preferencias ->
            preferencias.remove(claveUsuarioId)
        }
    }
}
