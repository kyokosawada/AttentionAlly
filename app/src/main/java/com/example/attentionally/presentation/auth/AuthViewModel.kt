package com.example.attentionally.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attentionally.data.AuthRepository
import com.example.attentionally.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for all authentication and onboarding UI; bridges Compose and backend AuthRepository.
 * - Holds all input/UI state for login/signup/guest onboarding.
 * - Exposes global navigation and error/success states.
 * - Manages session current user.
 * - All error, UX, and navigation logic is state-driven/reactive for Compose.
 * Suitable for testing/faking, orchestrates all account flows in app.
 */
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    // Public user Flow property for UI/navigation observation.
    /**
     * Current user observable state for UI, navigation, and other view models.
     */
    val user = repo.getCurrentUser()

    // Expose public signOut suspend function.
    /**
     * Signs out the current user from the app.
     * @return Unit
     */
    suspend fun signOut() = repo.signOut()

    /** UI state for authentication forms and flows */
    data class AuthUiState(
        val email: String = "",
        val password: String = "",
        val name: String = "",
        val role: String = "Student",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(AuthUiState())

    /**
     * Global UI state for authentication, including input fields and loading/error states.
     */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // --- Actions for Login ---
    /**
     * Updates the email input field in the UI state.
     * @param newEmail the new email to update the state with
     */
    fun onEmailChange(newEmail: String) {
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    /**
     * Updates the password input field in the UI state.
     * @param newPw the new password to update the state with
     */
    fun onPasswordChange(newPw: String) {
        _uiState.value = _uiState.value.copy(password = newPw)
    }

    /**
     * Updates the name input field in the UI state.
     * @param newName the new name to update the state with
     */
    fun onNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(name = newName)
    }

    /**
     * Updates the role selection in the UI state.
     * @param newRole the new role to update the state with
     */
    fun onRoleChange(newRole: String) {
        _uiState.value = _uiState.value.copy(role = newRole)
    }

    /**
     * Clears any error message in the UI state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Handles login using the AuthRepository and updates UI state.
    /**
     * Attempts to log in the user with the current email and password.
     */
    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (_uiState.value.email.isBlank() || _uiState.value.password.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Email/password required")
                return@launch
            }
            try {
                val result = repo.signIn(_uiState.value.email, _uiState.value.password)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Handles sign up using the AuthRepository and updates UI state, passing selected role.
    /**
     * Attempts to sign up the user with the current email, password, name, and role.
     */
    fun signUp() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (_uiState.value.name.isBlank() || _uiState.value.email.isBlank() || _uiState.value.password.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "All fields required")
                return@launch
            }
            try {
                val selectedRole = try {
                    UserRole.valueOf(_uiState.value.role.uppercase())
                } catch (e: Exception) {
                    UserRole.STUDENT
                }
                val result = repo.signUp(
                    _uiState.value.email,
                    _uiState.value.password,
                    _uiState.value.name,
                    selectedRole
                )
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Handles anonymous sign-in, requiring consent, and updates UI state.
    /**
     * Attempts to sign in the user anonymously, requiring parental consent.
     * @param consentGiven whether parental consent has been given
     */
    fun signInAnonymously(consentGiven: Boolean) {
        viewModelScope.launch {
            if (!consentGiven) {
                _uiState.value = _uiState.value.copy(error = "Parental consent required")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repo.signInAnonymously()
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Resets the UI state to its initial state.
     */
    fun reset() {
        _uiState.value = AuthUiState()
    }
}
