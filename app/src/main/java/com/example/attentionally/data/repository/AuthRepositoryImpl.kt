package com.example.attentionally.data.repository

import com.example.attentionally.domain.repository.AuthRepository
import com.example.attentionally.domain.model.User
import com.example.attentionally.domain.model.UserRole
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

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
class AuthRepositoryImpl(
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
            Timber.d("[AuthRepositoryImpl] signIn called: email=%s", email)
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                Timber.d("[AuthRepositoryImpl] Firebase returned: %s", result.user)
                val firebaseUser =
                    result.user ?: return@withContext Result.failure(Exception("User not found"))
                Timber.d(
                    "[AuthRepositoryImpl] User found, checking Firestore profile for uid=%s",
                    firebaseUser.uid
                )
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                if (!userDoc.exists()) {
                    Timber.e(
                        "[AuthRepositoryImpl] Firestore missing profile, uid=%s",
                        firebaseUser.uid
                    )
                    return@withContext Result.failure(Exception("User profile missing in Firestore."))
                }
                val user = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
                Timber.d("[AuthRepositoryImpl] Firestore loaded user profile: %s", user)
                if (user == null) {
                    Timber.e(
                        "[AuthRepositoryImpl] Firestore profile malformed, uid=%s",
                        firebaseUser.uid
                    )
                    return@withContext Result.failure(Exception("Malformed user profile."))
                }
                Timber.d("[AuthRepositoryImpl] signIn succeeded for %s", user.email)
                Result.success(user)
            } catch (e: Exception) {
                Timber.e(e, "[AuthRepositoryImpl] signIn exception")
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
        Timber.d(
            "[AuthRepositoryImpl] signUp called: email=%s, name=%s, role=%s",
            email,
            name,
            role
        )
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Timber.d("[AuthRepositoryImpl] Firebase created account: %s", result.user)
            val firebaseUser =
                result.user ?: return@withContext Result.failure(Exception("User not created"))
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            Timber.d("[AuthRepositoryImpl] Firebase profile updated")
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = name,
                role = role,
                avatarUrl = firebaseUser.photoUrl?.toString(),
                createdAt = System.currentTimeMillis()
            )
            Timber.d("[AuthRepositoryImpl] Created business user: %s", user)
            setUserInFirestore(user, merge = false)
            Timber.d(
                "[AuthRepositoryImpl] Attempting to load created user from Firestore... uid=%s",
                firebaseUser.uid
            )
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val loadedUser = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
            Timber.d("[AuthRepositoryImpl] Firestore post-create result: %s", loadedUser)
            if (loadedUser == null) {
                Timber.e(
                    "[AuthRepositoryImpl] Firestore failed to load created profile, uid=%s",
                    firebaseUser.uid
                )
                return@withContext Result.failure(Exception("Failed to load created user profile from Firestore."))
            }
            Timber.d("[AuthRepositoryImpl] signUp succeeded for %s", user.email)
            Result.success(loadedUser)
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] signUp exception")
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Creates an anonymous (guest) session. Profile/role are local until user upgrades.
     */
    override suspend fun signInAnonymously(): Result<User> = withContext(ioDispatcher) {
        Timber.d("[AuthRepositoryImpl] signInAnonymously called")
        try {
            val result = auth.signInAnonymously().await()
            Timber.d("[AuthRepositoryImpl] Firebase created anonymous session: %s", result.user)
            val firebaseUser = result.user
                ?: return@withContext Result.failure(Exception("Anonymous user not created"))
            Timber.d("[AuthRepositoryImpl] Anonymous user: %s", firebaseUser.uid)
            Result.success(firebaseUser.toUser(role = null))
        } catch (e: Exception) {
            Timber.e(e, "[AuthRepositoryImpl] signInAnonymously exception")
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Logs out user globally and wipes session state (including DataStore role).
     */
    override suspend fun signOut() = withContext(ioDispatcher) {
        Timber.d("[AuthRepositoryImpl] signOut called")
        auth.signOut()
        Timber.d("[AuthRepositoryImpl] Firebase signOut called - session cleared")
    }

    /**
     * Exposes real-time User (from Firestore) as a Flow, bound to the current session.
     */
    override fun getCurrentUser(): Flow<User?> =
        callbackFlow<FirebaseUser?> {
            Timber.d("[AuthRepositoryImpl] getCurrentUser() started - listening for auth state changes")
            val authListener = FirebaseAuth.AuthStateListener { fauth ->
                val fUser = fauth.currentUser
                Timber.d("[AuthRepositoryImpl] AuthStateListener triggered: %s", fUser)
                trySend(fUser)
            }
            auth.addAuthStateListener(authListener)
            awaitClose {
                Timber.d("[AuthRepositoryImpl] getCurrentUser() closed - removed auth listener")
                auth.removeAuthStateListener(authListener)
            }
        }.flatMapLatest { user ->
            if (user == null) flowOf(null)
            else flow {
                Timber.d(
                    "[AuthRepositoryImpl] getCurrentUser() - Reading Firestore profile for uid=%s",
                    user.uid
                )
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                if (!userDoc.exists()) {
                    Timber.e(
                        "[AuthRepositoryImpl] Firestore missing profile in getCurrentUser, uid=%s",
                        user.uid
                    )
                    emit(null)
                } else {
                    val userProfile = userDoc.toObject(User::class.java)?.copy(id = user.uid)
                    Timber.d(
                        "[AuthRepositoryImpl] Firestore profile loaded in getCurrentUser: %s",
                        userProfile
                    )
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
            Timber.d("[AuthRepositoryImpl] upgradeAnonymousUser called: email=%s", email)
            val user = auth.currentUser
            if (user == null) {
                Timber.e("[AuthRepositoryImpl] upgradeAnonymousUser failed - no anonymous user")
                return@withContext Result.failure(Exception("User must be signed in anonymously to upgrade."))
            }
            try {
                val credential = EmailAuthProvider.getCredential(email, password)
                val result = user.linkWithCredential(credential).await()
                val linkedUser =
                    result.user ?: throw IllegalStateException("Failed to upgrade anon user.")
                Timber.d("[AuthRepositoryImpl] Anonymous user upgraded: %s", linkedUser.uid)
                val userModel = linkedUser.toUser(role = UserRole.STUDENT)
                setUserInFirestore(userModel, merge = false)
                Timber.d("[AuthRepositoryImpl] Firestore updated after upgrade, reading profile")
                val userDoc = firestore.collection("users").document(linkedUser.uid).get().await()
                val loadedUser = userDoc.toObject(User::class.java)?.copy(id = linkedUser.uid)
                Timber.d("[AuthRepositoryImpl] Post-upgrade Firestore result: %s", loadedUser)
                if (loadedUser == null) {
                    Timber.e(
                        "[AuthRepositoryImpl] Failed to load created profile after upgrade, uid=%s",
                        linkedUser.uid
                    )
                    return@withContext Result.failure(Exception("Failed to load created user profile from Firestore."))
                }
                Timber.d("[AuthRepositoryImpl] upgradeAnonymousUser succeeded for %s", email)
                Result.success(loadedUser)
            } catch (e: Exception) {
                Timber.e(e, "[AuthRepositoryImpl] upgradeAnonymousUser exception")
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
                Timber.d("[AuthRepositoryImpl] setUserInFirestore: uid=%s merge=%s", user.id, merge)
                val doc = firestore.collection("users").document(user.id)
                if (merge) {
                    doc.set(user, SetOptions.merge()).await()
                    Timber.d("[AuthRepositoryImpl] setUserInFirestore merged profile")
                } else {
                    doc.set(user).await()
                    Timber.d("[AuthRepositoryImpl] setUserInFirestore replaced profile")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e // Respect coroutine cancellation
                Timber.e(e, "[AuthRepositoryImpl] Failed to update user in Firestore")
            }
        }
    }

    /**
     * Maps FirebaseUser to our business User model, providing safe defaults.
     */
    private fun FirebaseUser.toUser(role: UserRole?): User {
        Timber.d("[AuthRepositoryImpl] Mapping FirebaseUser to business User, role=%s", role)
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
        val code = (e as? FirebaseAuthException)?.errorCode
        Timber.e(e, "[AuthRepositoryImpl] Mapping Firebase Auth error: code=%s", code)
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
