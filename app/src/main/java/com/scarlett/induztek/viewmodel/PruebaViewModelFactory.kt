
package com.scarlett.induztek.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.scarlett.induztek.data.PruebaRepository

class PruebaViewModelFactory(
    private val repository: PruebaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(PruebaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PruebaViewModel(repository) as T
        }

        throw IllegalArgumentException("ViewModel desconocido")
    }
}
