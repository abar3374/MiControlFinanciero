package com.micontrolfinanciero.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.profile.ProfileField
import com.micontrolfinanciero.app.core.domain.profile.ProfilePhotoStorage
import com.micontrolfinanciero.app.core.domain.profile.ProfileValidator
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import com.micontrolfinanciero.app.core.model.PersonalData
import com.micontrolfinanciero.app.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonalForm(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
)

data class CredentialsForm(
    val username: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

data class ProfileUiState(
    val profile: UserProfile = UserProfile.EMPTY,
    val loading: Boolean = true,
    val editingPersonal: Boolean = false,
    val personalForm: PersonalForm = PersonalForm(),
    val personalErrors: Map<ProfileField, String> = emptyMap(),
    val editingCredentials: Boolean = false,
    val credentialsForm: CredentialsForm = CredentialsForm(),
    val credentialsErrors: Map<ProfileField, String> = emptyMap(),
    val saving: Boolean = false,
    val photoBusy: Boolean = false,
    /** Mensaje corto de confirmación o error para mostrar una vez. */
    val message: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val photoStorage: ProfilePhotoStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeProfile().collect { profile ->
                _uiState.update { state ->
                    // La primera vez, si el perfil está vacío, se abre directo el formulario.
                    val openForm = state.loading && !profile.hasPersonalData
                    state.copy(
                        profile = profile,
                        loading = false,
                        editingPersonal = state.editingPersonal || openForm,
                    )
                }
            }
        }
    }

    // ---------- Datos personales ----------

    fun startEditPersonal() {
        val p = _uiState.value.profile
        _uiState.update {
            it.copy(
                editingPersonal = true,
                personalErrors = emptyMap(),
                personalForm = PersonalForm(
                    firstName = p.firstName,
                    lastName = p.lastName,
                    phone = if (p.phone.isNotEmpty()) ProfileValidator.formatPhone(p.phone) else "",
                    email = p.email,
                ),
            )
        }
    }

    fun onPersonalFormChange(form: PersonalForm) = _uiState.update {
        it.copy(personalForm = form, personalErrors = emptyMap())
    }

    fun cancelEditPersonal() = _uiState.update {
        it.copy(editingPersonal = false, personalErrors = emptyMap())
    }

    fun savePersonal() {
        val form = _uiState.value.personalForm
        val result = ProfileValidator.validatePersonalData(
            PersonalData(form.firstName, form.lastName, form.phone, form.email),
        )
        val clean = result.value
        if (!result.isValid || clean == null) {
            _uiState.update { it.copy(personalErrors = result.errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching { repository.savePersonalData(clean) }
                .onSuccess {
                    _uiState.update { it.copy(saving = false, editingPersonal = false, message = "Datos guardados.") }
                }
                .onFailure {
                    _uiState.update { it.copy(saving = false, message = "No se pudieron guardar los datos. Intenta de nuevo.") }
                }
        }
    }

    // ---------- Usuario y contraseña ----------

    fun startEditCredentials() = _uiState.update {
        it.copy(
            editingCredentials = true,
            credentialsErrors = emptyMap(),
            credentialsForm = CredentialsForm(username = it.profile.username.orEmpty()),
        )
    }

    fun onCredentialsFormChange(form: CredentialsForm) = _uiState.update {
        it.copy(credentialsForm = form, credentialsErrors = emptyMap())
    }

    fun cancelEditCredentials() = _uiState.update {
        it.copy(editingCredentials = false, credentialsForm = CredentialsForm(), credentialsErrors = emptyMap())
    }

    fun saveCredentials() {
        val state = _uiState.value
        val form = state.credentialsForm
        val newPass = form.newPassword.toCharArray()
        val confirm = form.confirmPassword.toCharArray()
        val result = ProfileValidator.validateCredentials(form.username, newPass, confirm)
        confirm.fill('\u0000')
        val username = result.value
        if (!result.isValid || username == null) {
            newPass.fill('\u0000')
            _uiState.update { it.copy(credentialsErrors = result.errors) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                // Para cambiar una contraseña existente, primero se confirma la actual.
                if (state.profile.hasPassword) {
                    val current = form.currentPassword.toCharArray()
                    val ok = try {
                        repository.verifyCredentials(state.profile.username.orEmpty(), current)
                    } finally {
                        current.fill('\u0000')
                    }
                    if (!ok) {
                        _uiState.update {
                            it.copy(
                                saving = false,
                                credentialsErrors = mapOf(ProfileField.CURRENT_PASSWORD to "La contraseña actual no es correcta."),
                            )
                        }
                        return@launch
                    }
                }
                repository.setCredentials(username, newPass)
                _uiState.update {
                    it.copy(
                        saving = false,
                        editingCredentials = false,
                        credentialsForm = CredentialsForm(),
                        message = if (state.profile.hasPassword) "Contraseña actualizada." else "Listo: desde ahora Finance pedirá usuario y contraseña al abrir.",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(saving = false, message = "No se pudo guardar. Intenta de nuevo.") }
            } finally {
                newPass.fill('\u0000')
            }
        }
    }

    // ---------- Foto ----------

    fun onPhotoPicked(uri: String) {
        val oldPath = _uiState.value.profile.photoPath
        _uiState.update { it.copy(photoBusy = true) }
        viewModelScope.launch {
            runCatching {
                val newPath = photoStorage.save(uri)
                repository.updatePhoto(newPath)
                if (oldPath != null) photoStorage.delete(oldPath)
            }.onSuccess {
                _uiState.update { it.copy(photoBusy = false, message = "Foto actualizada.") }
            }.onFailure {
                _uiState.update { it.copy(photoBusy = false, message = "No se pudo usar esa imagen. Prueba con otra.") }
            }
        }
    }

    fun removePhoto() {
        val oldPath = _uiState.value.profile.photoPath ?: return
        viewModelScope.launch {
            repository.updatePhoto(null)
            photoStorage.delete(oldPath)
            _uiState.update { it.copy(message = "Foto eliminada.") }
        }
    }

    // ---------- Huella ----------

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
            _uiState.update {
                it.copy(message = if (enabled) "Huella activada." else "Huella desactivada.")
            }
        }
    }

    fun showMessage(message: String) = _uiState.update { it.copy(message = message) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}
