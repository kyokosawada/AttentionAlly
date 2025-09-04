package com.example.attentionally.data

/**
 * Cloud-backed user profile for AttentionAlly. Used for Firestore docs and in-app session.
 * Defaults for all fields required to enable Firestore toObject() mapping via reflection.
 *
 * @property id Unique user ID (matches Auth/Firestore doc id)
 * @property email User's registered email; blank for guests
 * @property name User's display name (registration or onboarding)
 * @property role Current user role for features/reporting. See UserRole
 * @property avatarUrl Optional uploaded avatar uri or external image url
 * @property createdAt Profile creation date (millis since epoch)
 */
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val avatarUrl: String? = null,
    val createdAt: Long = 0L
)

/**
 * User roles for the app, for dashboard visibility/routing/permissions.
 * Extendable for future roles (admin/researcher).
 */
enum class UserRole {
    STUDENT, TEACHER
}
