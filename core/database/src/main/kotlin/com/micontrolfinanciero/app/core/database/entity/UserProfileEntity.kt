package com.micontrolfinanciero.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla user_profile: una sola fila (id = 1) con el perfil del dueño del teléfono.
 * password_hash guarda el hash PBKDF2, nunca la contraseña.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "first_name") val firstName: String = "",
    @ColumnInfo(name = "last_name") val lastName: String = "",
    @ColumnInfo(name = "phone") val phone: String = "",
    @ColumnInfo(name = "email") val email: String = "",
    @ColumnInfo(name = "photo_path") val photoPath: String? = null,
    @ColumnInfo(name = "username") val username: String? = null,
    @ColumnInfo(name = "password_hash") val passwordHash: String? = null,
    @ColumnInfo(name = "biometric_enabled") val biometricEnabled: Boolean = false,
    /** Momento del último cambio, en milisegundos desde 1970 (UTC). */
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
