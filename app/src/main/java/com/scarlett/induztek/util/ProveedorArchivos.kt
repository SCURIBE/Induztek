package com.scarlett.induztek.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ProveedorArchivos {

    private fun autoridad(context: Context): String {
        return "${context.packageName}.fileprovider"
    }

    fun uriPara(context: Context, archivo: File): Uri {
        return FileProvider.getUriForFile(
            context,
            autoridad(context),
            archivo
        )
    }
}