package com.micontrolfinanciero.app.core.data.profile

import com.micontrolfinanciero.app.core.database.dao.UserProfileDao
import com.micontrolfinanciero.app.core.database.entity.UserProfileEntity
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import com.micontrolfinanciero.app.core.domain.security.PasswordHasher
import com.micontrolfinanciero.app.core.model.PersonalData
import com.micontrolfinanciero.app.core.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Perfil guardado en la tabla user_profile de la base cifrada. */
@Singleton
class RoomUserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
) : UserProfileRepository {

    /** Evita que dos guardados simultáneos se pisen (leer-modificar-escribir). */
    private val writeLock = Mutex()

    override fun observeProfile(): Flow<UserProfile> =
        dao.observe().map { it?.toModel() ?: UserProfile.EMPTY }

    override suspend fun savePersonalData(data: PersonalData) = update {
        it.copy(firstName = data.firstName, lastName = data.lastName, phone = data.phone, email = data.email)
    }

    override suspend fun updatePhoto(photoPath: String?) = update { it.copy(photoPath = photoPath) }

    override suspend fun setCredentials(username: String, password: CharArray) {
        // PBKDF2 es lento a propósito: se calcula fuera del hilo de la pantalla.
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
        update { it.copy(username = username, passwordHash = hash) }
    }

    override suspend fun verifyCredentials(username: String, password: CharArray): Boolean {
        val current = dao.get() ?: return false
        val storedHash = current.passwordHash ?: return false
        val sameUser = current.username == username.trim().lowercase()
        // Se calcula el hash aunque el usuario no coincida, para no revelar cuál de los dos falló.
        val passwordOk = withContext(Dispatchers.Default) { PasswordHasher.verify(password, storedHash) }
        return sameUser && passwordOk
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) = update { it.copy(biometricEnabled = enabled) }

    private suspend fun update(change: (UserProfileEntity) -> UserProfileEntity) {
        writeLock.withLock {
            val current = dao.get() ?: UserProfileEntity()
            dao.upsert(change(current).copy(updatedAt = System.currentTimeMillis()))
        }
    }

    private fun UserProfileEntity.toModel() = UserProfile(
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        email = email,
        photoPath = photoPath,
        username = username,
        hasPassword = passwordHash != null,
        biometricEnabled = biometricEnabled && passwordHash != null,
    )
}
