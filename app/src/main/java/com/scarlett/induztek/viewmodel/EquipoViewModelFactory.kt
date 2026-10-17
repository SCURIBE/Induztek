
package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.scarlett.induztek.data.EquipoRepository

class EquipoViewModelFactory(
    private val repository: EquipoRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(EquipoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EquipoViewModel(repository) as T
        }

        throw IllegalArgumentException("ViewModel desconocido")
    }
}
