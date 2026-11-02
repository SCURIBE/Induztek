package com.scarlett.induztek.Repository.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.scarlett.induztek.Repository.model.Equipo
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipoDao {
    //CREAR EQUIPO
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(equipo: Equipo): Long

    //ACTUALIZAR EQUIPO
    @Update
    suspend fun actualizar(equipo: Equipo)

    //OBTENER TODOS LOS EQUIPOS
    @Query("SELECT * FROM equipos ORDER BY codigo ASC")
    fun obtenerTodos(): Flow<List<Equipo>>

    //BUSCAR EQUIPO POR CODIGO
    @Query("SELECT EXISTS(SELECT 1 FROM equipos WHERE codigo = :codigo)")
    suspend fun existeCodigo(codigo: String): Boolean


    @Query(
        "SELECT EXISTS(" +
                "SELECT 1 FROM equipos " +
                "WHERE codigo = :codigo AND id != :id" +
                ")"
    )
    suspend fun existeCodigoEnOtroEquipo(
        codigo: String,
        id: Int
    ): Boolean

    //ELIMINAR EQUIPO POR ID
    @Query("DELETE FROM equipos WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    //BUSCAR EQUIPO POR ID
    @Query("SELECT * FROM equipos WHERE id = :id")
    suspend fun buscarPorId(id: Int): Equipo?

    // Las fotos que algún equipo usa. Sirve para borrar las huérfanas.
    @Query("SELECT fotoUri FROM equipos WHERE fotoUri IS NOT NULL")
    suspend fun rutasDeFotos(): List<String>
}
