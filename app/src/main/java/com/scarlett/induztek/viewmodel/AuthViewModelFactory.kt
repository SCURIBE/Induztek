package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.scarlett.induztek.data.AlmacenSesion
import com.scarlett.induztek.data.UsuarioRepository

class AuthViewModelFactory(
    private val repository: UsuarioRepository,
    private val sesionStore: AlmacenSesion
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, sesionStore) as T
        }

        throw IllegalArgumentException("ViewModel desconocido")
    }
}
