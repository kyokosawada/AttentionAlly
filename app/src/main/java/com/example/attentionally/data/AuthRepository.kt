package com.example.attentionally.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all authentication operations using Firebase Auth or fakes for testing.
 *
 * This contract supports sign-up, login, anonymous (guest) sessions, anonymous-to-account upgrade, and real-time user/session flows.
 *
 * Used in MVVM architecture to abstract underlying implementation (Firebase, fake, or otherwise).
 * - `suspend` functions are for operations that require network or asynchronous work.
 * - `Flow` is used for real-time user/session state changes (e.g., login, logout, upgrade).
 *
 * To implement or test:
 * - See method documentation for intended behaviors and result handling.
 * - Fake/test implementations should be easy, using matching method contracts.
 */
interface AuthRepository {
    /**
     * Sign in with email and password.
     * @return [Result] containing authenticated [User] or failure.
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Register new account with email, password, name, and role.
     * @return [Result] containing created [User] or failure info.
     */
    suspend fun signUp(email: String, password: String, name: String, role: UserRole): Result<User>

    /**
     * Sign in as an anonymous/guest user.
     * @return [Result] containing anonymous [User] or failure.
     */
    suspend fun signInAnonymously(): Result<User>

    /**
     * Sign out currently authenticated user (if any).
     */
    suspend fun signOut()

    /**
     * Observe the current user/session in real time.
     * Emits user changes or null if signed out.
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Upgrade an anonymous user to a permanent account.
     * @param email New email for upgrading account.
     * @param password New password for upgrading account.
     * @return [Result] with upgraded [User] or error.
     */
    suspend fun upgradeAnonymousUser(email: String, password: String): Result<User>
}
