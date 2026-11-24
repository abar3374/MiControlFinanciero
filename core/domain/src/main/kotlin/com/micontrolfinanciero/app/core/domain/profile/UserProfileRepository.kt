package com.micontrolfinanciero.app.core.domain.profile

import com.micontrolfinanciero.app.core.model.PersonalData
import com.micontrolfinanciero.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Acceso al perfil del usuario. La capa de datos (:core:data) lo implementa con la
 * base de datos cifrada; las pantallas solo conocen esta interfaz.
 */
interface UserProfileRepository {

    /** Emite el perfil cada vez que cambia. [UserProfile.EMPTY] si aún no hay datos. */
    fun observeProfile(): Flow<UserProfile>

    /** Guarda nombre, apellidos, celular y correo. Los datos deben venir validados. */
    suspend fun savePersonalData(data: PersonalData)

    /** Cambia la ruta de la foto (null = quitar la foto). */
    suspend fun updatePhoto(photoPath: String?)

    /**
     * Crea o cambia usuario y contraseña. La contraseña se guarda como hash PBKDF2,
     * nunca en texto plano.
     */
    suspend fun setCredentials(username: String, password: CharArray)

    /** true si usuario y contraseña coinciden con los guardados. */
    suspend fun verifyCredentials(username: String, password: CharArray): Boolean

    /** Activa o desactiva el desbloqueo con huella. */
    suspend fun setBiometricEnabled(enabled: Boolean)
}

/** Guarda la foto de perfil en el almacenamiento privado de la app. */
interface ProfilePhotoStorage {

    /**
     * Copia la imagen elegida ([sourceUri], un content:// de Android), la reduce a un
     * tamaño liviano y devuelve la ruta del archivo guardado.
     */
    suspend fun save(sourceUri: String): String

    /** Borra un archivo de foto anterior, si existe. */
    suspend fun delete(path: String)
}
