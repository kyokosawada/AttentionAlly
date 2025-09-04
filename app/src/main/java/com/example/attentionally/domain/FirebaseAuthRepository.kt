package com.example.attentionally.domain

import com.example.attentionally.data.AuthRepository
import com.example.attentionally.data.User
import com.example.attentionally.data.UserRole
import com.example.attentionally.data.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import timber.log.Timber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

/**
 * Repository for all authentication and user-related flows.
 * Handles:
 * - Firebase email/password login
 * - Signup and user profile creation in Firestore
 * - Anonymous (guest) auth and onboarding
 * - Profile and session reading (cloud as truth)
 * - Logout
 * - All backend error management best-practices
 *
 * Kotlin idiomatic, MVVM, coroutine/Flow compliant, fully DI-ready.
 */
class FirebaseAuthRepository(
    private val sessionManager: SessionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Signs in user with email/password.
     * Loads their latest profile from Firestore for fully authoritative session (role, etc.).
     * @return Result with full User object on success; error otherwise
     */
    override suspend fun signIn(email: String, password: String): Result<User> =
        withContext(ioDispatcher) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser =
                    result.user ?: return@withContext Result.failure(Exception("User not found"))
                // Always load from Firestore
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                if (!userDoc.exists()) {
                    // Optionally: Create or recover? For now, block.
                    return@withContext Result.failure(Exception("User profile missing in Firestore."))
                }
                val user = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
                if (user == null) {
                    return@withContext Result.failure(Exception("Malformed user profile."))
                }
                // Only now update local session with Firestore profile role
                sessionManager.saveUserRole(user.role)
                sessionManager.setAnonymousSession(false)
                Result.success(user)
            } catch (e: Exception) {
                Result.failure(mapAuthException(e))
            }
    }

    /**
     * Registers new account via Auth and creates user profile in Firestore.
     * Waits for profile to sync. Returns usable User for UI/session.
     */
    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Result<User> = withContext(ioDispatcher) {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser =
                result.user ?: return@withContext Result.failure(Exception("User not created"))
            // Update profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            sessionManager.saveUserRole(role)
            sessionManager.setAnonymousSession(false)
            val user = firebaseUser.toUser(role = role)
            setUserInFirestore(user, merge = false)
            // Now load the just-created user from Firestore to ensure consistency
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val loadedUser = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
            if (loadedUser == null) {
                return@withContext Result.failure(Exception("Failed to load created user profile from Firestore."))
            }
            Result.success(loadedUser)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Creates an anonymous (guest) session. Profile/role are local until user upgrades.
     */
    override suspend fun signInAnonymously(): Result<User> = withContext(ioDispatcher) {
        try {
            val result = auth.signInAnonymously().await()
            val firebaseUser = result.user
                ?: return@withContext Result.failure(Exception("Anonymous user not created"))
            sessionManager.setAnonymousSession(true)
            Result.success(firebaseUser.toUser(role = null))
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Logs out user globally and wipes session state (including DataStore role).
     */
    override suspend fun signOut() = withContext(ioDispatcher) {
        auth.signOut()
        sessionManager.clearSession()
    }

    /**
     * Exposes real-time User (from Firestore) as a Flow, bound to the current session.
     */
    override fun getCurrentUser(): Flow<User?> =
        callbackFlow<com.google.firebase.auth.FirebaseUser?> {
            val authListener = FirebaseAuth.AuthStateListener { fauth ->
                val fUser = fauth.currentUser
                trySend(fUser)
            }
            auth.addAuthStateListener(authListener)
            awaitClose { auth.removeAuthStateListener(authListener) }
        }.flatMapLatest { user ->
            if (user == null) flowOf(null)
            else flow {
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                if (!userDoc.exists()) {
                    emit(null)
                } else {
                    val userProfile = userDoc.toObject(User::class.java)?.copy(id = user.uid)
                    userProfile?.let { sessionManager.saveUserRole(it.role) }
                    emit(userProfile)
                }
            }
        }

    /**
     * Upgrades an anonymous account to a real (email/password) account and profile in Firestore.
     * Returns the fully loaded new User.
     */
    override suspend fun upgradeAnonymousUser(email: String, password: String): Result<User> =
        withContext(ioDispatcher) {
            val user = auth.currentUser
            if (user == null)
                return@withContext Result.failure(Exception("User must be signed in anonymously to upgrade."))
            try {
                val credential = EmailAuthProvider.getCredential(email, password)
                val result = user.linkWithCredential(credential).await()
                val linkedUser =
                    result.user ?: throw IllegalStateException("Failed to upgrade anon user.")
                sessionManager.setAnonymousSession(false)
                sessionManager.saveUserRole(UserRole.STUDENT) // Consider role selection!
                val userModel = linkedUser.toUser(role = UserRole.STUDENT)
                setUserInFirestore(userModel, merge = false)
                // Reload from Firestore for consistency
                val userDoc = firestore.collection("users").document(linkedUser.uid).get().await()
                val loadedUser = userDoc.toObject(User::class.java)?.copy(id = linkedUser.uid)
                if (loadedUser == null) {
                    return@withContext Result.failure(Exception("Failed to load created user profile from Firestore."))
                }
                Result.success(loadedUser)
            } catch (e: Exception) {
                Result.failure(mapAuthException(e))
            }
    }

    /**
     * Writes or updates user profile in Firestore at /users/{uid}.
     * @param merge Whether to merge fields (default false; true on login).
     */
    private suspend fun setUserInFirestore(user: User, merge: Boolean = false) {
        withContext(ioDispatcher) {
            try {
                val doc = firestore.collection("users").document(user.id)
                if (merge) {
                    doc.set(user, SetOptions.merge()).await()
                } else {
                    doc.set(user).await()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e // Respect coroutine cancellation
                // Log only real Firestore errors
                Timber.e(e, "Failed to update user in Firestore")
            }
        }
    }

    /**
     * Maps FirebaseUser to our business User model, providing safe defaults.
     */
    private fun com.google.firebase.auth.FirebaseUser.toUser(role: UserRole?): User {
        return User(
            id = uid,
            email = email ?: "",
            name = displayName ?: "Anonymous",
            role = role ?: UserRole.STUDENT,
            avatarUrl = photoUrl?.toString(),
            createdAt = metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }

    /**
     * Maps all backend and crash errors to user-friendly auth errors with logging.
     */
    private fun mapAuthException(e: Exception): Exception {
        // Map FirebaseAuthException error codes to friendly messages
        val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
        val message = when (code) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email address already in use."
            "ERROR_USER_NOT_FOUND" -> "No user found for that email."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password."
            "ERROR_WEAK_PASSWORD" -> "Password is too weak."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network connection error."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Operation not allowed. Please contact support."
            else -> e.localizedMessage ?: "Authentication failed."
        }
        return Exception(message, e)
    }
}
