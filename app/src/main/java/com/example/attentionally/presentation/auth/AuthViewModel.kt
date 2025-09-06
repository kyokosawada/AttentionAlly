package com.example.attentionally.presentation.auth

import timber.log.Timber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attentionally.domain.repository.AuthRepository
import com.example.attentionally.domain.model.UserRole
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

    // Track when initial auth state is loaded
    init {
        viewModelScope.launch {
            Timber.d("[AuthViewModel] Initializing - Listening for auth state")
            user.collect { userValue ->
                Timber.d("[AuthViewModel] Auth state emission: %s", userValue)
                // Once we get any value (null or user), auth state is loaded
                _uiState.value = _uiState.value.copy(isAuthStateLoading = false)
            }
        }
    }

    // Expose public signOut suspend function.
    /**
     * Signs out the current user from the app.
     * @return Unit
     */
    suspend fun signOut() {
        Timber.d("[AuthViewModel] Attempting sign out")
        repo.signOut()
        Timber.d("[AuthViewModel] Signed out - current user should be null")
    }

    /** UI state for authentication forms and flows */
    data class AuthUiState(
        val email: String = "",
        val password: String = "",
        val name: String = "",
        val role: String = "Student",
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSuccess: Boolean = false,
        val isAuthStateLoading: Boolean = true // Track initial auth state loading
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
        Timber.d("[AuthViewModel] Email input changed: %s", newEmail)
        _uiState.value = _uiState.value.copy(email = newEmail)
    }

    /**
     * Updates the password input field in the UI state.
     * @param newPw the new password to update the state with
     */
    fun onPasswordChange(newPw: String) {
        Timber.d("[AuthViewModel] Password input changed (length): %d", newPw.length)
        _uiState.value = _uiState.value.copy(password = newPw)
    }

    /**
     * Updates the name input field in the UI state.
     * @param newName the new name to update the state with
     */
    fun onNameChange(newName: String) {
        Timber.d("[AuthViewModel] Name input changed: %s", newName)
        _uiState.value = _uiState.value.copy(name = newName)
    }

    /**
     * Updates the role selection in the UI state.
     * @param newRole the new role to update the state with
     */
    fun onRoleChange(newRole: String) {
        Timber.d("[AuthViewModel] Role selection changed: %s", newRole)
        _uiState.value = _uiState.value.copy(role = newRole)
    }

    /**
     * Clears any error message in the UI state.
     */
    fun clearError() {
        Timber.d("[AuthViewModel] Clearing error message from UI state")
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Handles login using the AuthRepository and updates UI state.
    /**
     * Attempts to log in the user with the current email and password.
     */
    fun login() {
        Timber.d(
            "[AuthViewModel] Attempting login: email=%s passwordLen=%d",
            _uiState.value.email,
            _uiState.value.password.length
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (_uiState.value.email.isBlank() || _uiState.value.password.isBlank()) {
                Timber.w("[AuthViewModel] Login failed - missing email or password")
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "Email/password required")
                return@launch
            }
            try {
                val result = repo.signIn(_uiState.value.email, _uiState.value.password)
                result.fold(
                    onSuccess = {
                        Timber.d("[AuthViewModel] Login succeeded: %s", it)
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { e ->
                        Timber.e(e, "[AuthViewModel] Login failed")
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "[AuthViewModel] Login exception")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Handles sign up using the AuthRepository and updates UI state, passing selected role.
    /**
     * Attempts to sign up the user with the current email, password, name, and role.
     */
    fun signUp() {
        Timber.d(
            "[AuthViewModel] Attempting sign up: name=%s email=%s role=%s passwordLen=%d",
            _uiState.value.name,
            _uiState.value.email,
            _uiState.value.role,
            _uiState.value.password.length
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            if (_uiState.value.name.isBlank() || _uiState.value.email.isBlank() || _uiState.value.password.isBlank()) {
                Timber.w("[AuthViewModel] Sign up failed - missing fields")
                _uiState.value =
                    _uiState.value.copy(isLoading = false, error = "All fields required")
                return@launch
            }
            try {
                val selectedRole = try {
                    UserRole.valueOf(_uiState.value.role.uppercase())
                } catch (e: Exception) {
                    Timber.w(
                        e,
                        "[AuthViewModel] Sign up role parsing failed - defaulting to STUDENT"
                    )
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
                        Timber.d("[AuthViewModel] Sign up succeeded: %s", it)
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { e ->
                        Timber.e(e, "[AuthViewModel] Sign up failed")
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "[AuthViewModel] Sign up exception")
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
        Timber.d("[AuthViewModel] Attempting anonymous sign-in, consentGiven=%s", consentGiven)
        viewModelScope.launch {
            if (!consentGiven) {
                Timber.w("[AuthViewModel] Anonymous sign-in refused: parental consent not given")
                _uiState.value = _uiState.value.copy(error = "Parental consent required")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repo.signInAnonymously()
                result.fold(
                    onSuccess = {
                        Timber.d("[AuthViewModel] Anonymous sign-in succeeded: %s", it)
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = { e ->
                        Timber.e(e, "[AuthViewModel] Anonymous sign-in failed")
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "[AuthViewModel] Anonymous sign-in exception")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Resets the UI state to its initial state.
     */
    fun reset() {
        Timber.d("[AuthViewModel] Resetting UI state to initial values")
        _uiState.value = AuthUiState()
    }
}
