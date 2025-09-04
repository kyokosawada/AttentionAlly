# AttentionAlly Authentication System Documentation

## Overview

This document describes in depth the complete authentication stack, logic flow, and key
architectural patterns for the AttentionAlly Android app as of 2025. It covers all
submission/approval flows, user management, guest onboarding, error handling, and best practices for
Firebase and Jetpack Compose MVVM systems.

---

## Architecture

- **Pattern:** MVVM + Repository + Firestore (Cloud) + DataStore (Local) + DI (Koin)
- **Main Components:**
    - `AuthViewModel` (handles all UI <-> repository logic)
    - `FirebaseAuthRepository` (all backend auth and user profile CRUD)
    - `User` data class defines cloud and session user model
    - Composables: Screens for login, signup, guest flow, onboarding, and main UI
    - Navigation uses Compose Navigation

---

## Authentication Flows

### 1. Email/Password Signup & Login

- **Signup Flow:**
    - Calls `FirebaseAuth.createUserWithEmailAndPassword`
    - Updates profile info (displayName)
    - Creates `/users/{uid}` doc in Firestore via `setUserInFirestore()`
    - Loads the profile from Firestore (waits for success)
    - Sets local session and navigates to MainScreen with full profile info

- **Login Flow:**
    - Calls `FirebaseAuth.signInWithEmailAndPassword`
    - Immediately fetches `/users/{uid}` from Firestore (this is source of truth for all fields
      including role)
    - If missing, login fails and error surfaced

### 2. Anonymous/Guest Login & Onboarding

- **Anonymous Auth Flow:**
    - Calls `FirebaseAuth.signInAnonymously`
    - Redirects to onboarding (OnboardingStudentScreen)
    - User must enter a username (local only, role always STUDENT)
    - MainScreen receives a session User with guest name (not written to Firestore)

### 3. Upgrade Anonymous to Real Account

- **Upgrade Flow:**
    - Calls `linkWithCredential` on current user
    - Updates all Firestore/user profile info as on signup
    - Loads profile from Firestore for session

### 4. Logout

- Signs out in FirebaseAuth
- Clears DataStore (local session/role)
- Navigates back to login.

---

## Firestore Profile Management

- **Writes:**
    - All registered users have a `/users/{uid}` doc created on signup or upgrade.
    - Doc contains: id, name, email (may be blank for guests), role, avatarUrl, createdAt.
    - Writes are synchronous before session navigation; user always loaded back from Firestore doc.
- **Reads:**
    - On every login, app loads the latest `/users/{uid}` doc for profile, role, and all display
      info.
    - Guest/anonymous users do NOT have Firestore docs written until upgrade (MVP behavior).
- **Kotlin Data Class Caution:**
    - `User` must have all fields defaulted to allow `toObject(User::class.java)` mapping.

---

## Session Management

- **DataStore is only a transient cache:**
    - Stores role for UX (show/hide flows, fast local resume)
    - Always updated with whatever is loaded from Firestore
    - Never used as the primary source of truth for session/profile when user is authenticated
- **Current user/role for session:**
    - Always comes from live Firestore doc after login
    - Guest users: session contains only local onboarding info.

---

## Guest Onboarding Flow

- After anonymous sign-in, user is routed to `OnboardingStudentScreen`
- User enters their name (validated, required)
- App holds a session User object locally: id = FirebaseAuth.uid (or temp guest string), name =
  input, role = STUDENT
- On MainScreen, this info is displayed as if it were a full profile
- On logout, all session info including guest name is cleared

---

## Navigation Flows & Composables

- **Compose Navigation:**
    - Navigation graph dynamically changes start destinations:
        - If user session/profile exists, goes to Main
        - If not, shows login/signup/guest/onboarding as needed
- **MainScreen:**
    - Displays user name and role directly from session user (Firestore or, for guests, the
      onboarding User)
- **Guest MainScreen:**
    - Shows guest username, role=STUDENT, never "Unknown" after onboarding
- **Logout:**
    - Session and guest info cleared, navigates to login

---

## Error & Edge Case Handling

- **All errors:**
    - User-facing errors (form, network, missing data) are shown as Material 3 Snackbars, never as
      toast or inline red under field
    - Critical errors (profile missing, malformed, race) block session and surface an actionable
      message
    - All technical exceptions logged for diagnostics, not shown to end-user
- **Firestore profile races:**
    - Signup/upgrades wait for Firestore profile to be fully written and reloaded before continuing
    - MainScreen is never shown with partial/missing user data
- **Coroutine cancellation:**
    - Correctly handled so Firestore writes/reads respect cancellation and never log spurious errors

---

## Code Snippets – Key Flows

**User Data Class:**

```kotlin
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val avatarUrl: String? = null,
    val createdAt: Long = 0L
)
```

**Cloud-backed Login:**

```kotlin
val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
if (!userDoc.exists()) error
val user = userDoc.toObject(User::class.java)?.copy(id = firebaseUser.uid)
```

**Guest Onboarding:**

```kotlin
OnboardingStudentScreen(onContinue = { name ->
    val anonUid = FirebaseAuth.getInstance().currentUser?.uid ?: ...
    setGuestUser(User(id = anonUid, name = name, role = UserRole.STUDENT))
    // ...
})
```

---

## Best Practices & Extensibility

- Never use DataStore as primary session source—Firestore is always correct for cloud-backed users
- Update DataStore role _after_ loading a valid profile
- Always provide default values for all data class fields for Firestore `toObject()`
- Show user-friendly, actionable error feedback in Snackbar only
- Wait for profile write+read before session navigation after signup
- Guest sessions = local only, never written to Firestore until upgraded
- All navigation and error feedback must work correctly if Firebase or Firestore is temporarily
  unavailable
- Profile schema is future-proofed: new fields can be added safely

---

## FAQ

**Q: Why is role sometimes STUDENT by default?**
A: This only happens if there is a missing Firestore doc or the code attempts to map a FirebaseUser
without loading the latest user profile. Always ensure login/profile read flows come from Firestore,
not only DataStore or in-memory session objects.

**Q: Why doesn't Guest onboarding write to Firestore?**
A: By product requirements; privacy/MVP simplicity means Guest info stays only local until the
account is upgraded.

**Q: How do you extend session profile fields?**
A: Add new fields (nullable or default-valued) to the `User` class. Firestore will auto-inflate
these as set and be forward/backward compatible.

---

This documentation is up to date as of 2025. For upgrades, always reference the latest Firebase
Android SDK and Jetpack Compose Navigation/Material APIs.
