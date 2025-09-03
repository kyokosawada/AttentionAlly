# Product Requirements Document (PRD)

## Project: Attention Ally (Next-Gen)

---

## 12. Development Implementation Plan

This section provides a step-by-step roadmap for implementing the Attention Ally application
according to PRD specifications.

---

### **Phase 1: Foundation Setup (Days 1-3)**

#### **Step 1.1: Firebase Project Setup** ⚡ CRITICAL

```bash
# Prerequisites before coding
1. Go to https://console.firebase.google.com
2. Create new project: "AttentionAlly"
3. Enable services:
   - Authentication (Email/Password, Google)
   - Cloud Firestore
   - Cloud Storage
   - Analytics
   - Cloud Messaging
4. Download google-services.json → app/google-services.json
5. Test build: ./gradlew build
```

#### **Step 1.2: Application Class & Core Initialization**

```kotlin
// Create: app/src/main/java/com/example/attentionally/AttentionAllyApplication.kt
class AttentionAllyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        // Initialize Timber logging
        // Initialize Koin DI
    }
}
```

#### **Step 1.3: Basic App Structure**

```
app/src/main/java/com/example/attentionally/
├── di/                    # Koin dependency injection modules
├── data/                  # Repositories, data sources
├── domain/                # Business logic, use cases
├── presentation/          # UI screens, ViewModels
│   ├── auth/             # Authentication screens
│   ├── dashboard/        # Main dashboard
│   ├── profile/          # User profile
│   ├── rooms/            # Classroom functionality
│   └── tracking/         # Attention tracking
└── util/                 # Helper classes, extensions
```

---

### **Phase 2: Authentication System (Days 4-7)**

#### **Step 2.1: Data Models & Repository**

```kotlin
// Create core data classes
data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole { STUDENT, TEACHER }

// Create AuthRepository with Firebase Auth
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, name: String, role: UserRole): Result<User>
  suspend fun signInAnonymously(): Result<User> // <--- Added for anonymous auth
    suspend fun signOut()
    fun getCurrentUser(): Flow<User?>
  suspend fun upgradeAnonymousUser(
    email: String,
    password: String
  ): Result<User> // <--- For upgrade-to-account
}
```

#### **Step 2.2: Authentication Screens**

```kotlin
// Priority order for implementation:
1. SplashScreen.kt           # App startup, check login state
2. AuthenticationScreen.kt   # Login/Signup/Guest toggle
3. LoginScreen.kt            # Email/password login form
4. SignUpScreen.kt           # Registration with role selection
5. GuestScreen.kt            # Anonymous auth option and UX info
```

#### **Step 2.3: Session Management**

```kotlin
// Implement persistent login using DataStore
class SessionManager {
    suspend fun saveUserRole(role: UserRole)
    suspend fun getUserRole(): UserRole?
    suspend fun clearSession()
    suspend fun isAnonymousSession(): Boolean // <--- Anonymous session tracking
}
```

---

### **Phase 3: Core Navigation & Dashboard (Days 8-12)**

#### **Step 3.1: Navigation Setup**

```kotlin
// Create main navigation structure
@Composable
fun AttentionAllyNavigation() {
    NavHost(
        startDestination = if (isLoggedIn) "dashboard" else "auth"
    ) {
        // Auth flow
        composable("auth") { AuthenticationScreen() }
        composable("login") { LoginScreen() }
        composable("signup") { SignUpScreen() }
        
        // Main app flow
        composable("dashboard") { DashboardScreen() }
        composable("profile") { ProfileScreen() }
        composable("rooms") { RoomsScreen() }
        composable("market") { MarketScreen() }
    }
}
```

#### **Step 3.2: Bottom Navigation**

```kotlin
// Implement Material 3 bottom navigation
sealed class BottomNavItem {
    object Dashboard : BottomNavItem()
    object Rooms : BottomNavItem()
    object Market : BottomNavItem()
    object Profile : BottomNavItem()
}
```

#### **Step 3.3: Role-Based Dashboard**

```kotlin
// Different dashboards for different user roles
@Composable
fun DashboardScreen(userRole: UserRole) {
    when (userRole) {
        UserRole.STUDENT -> StudentDashboard()
        UserRole.TEACHER -> TeacherDashboard()
    }
}
```

---

### **Phase 4: User Profile & Avatar System (Days 13-16)**

#### **Step 4.1: Profile Data Structure**

```kotlin
// Create profile management system
data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val stats: UserStats = UserStats(),
    val preferences: UserPreferences = UserPreferences()
)

data class UserStats(
    val totalSessions: Int = 0,
    val totalTime: Long = 0,
    val coinsEarned: Int = 0,
    val averageAttention: Float = 0f
)
```

#### **Step 4.2: Avatar & Image Management**

```kotlin
// Implement avatar selection and upload
@Composable
fun AvatarSelectionScreen() {
    // Grid of default avatars
    // Camera option for custom photo
    // Firebase Storage upload
    // Coil image loading and caching
}
```

#### **Step 4.3: Profile Screen Implementation**

```kotlin
// Create comprehensive profile management
@Composable
fun ProfileScreen() {
    // User info display/edit
    // Avatar management
    // Statistics overview
    // Settings and preferences
    // Logout functionality
}
```

---

### **Phase 5: Room System & Real-time Features (Days 17-23)**

#### **Step 5.1: Room Data Models**

```kotlin
data class Room(
    val id: String,
    val name: String,
    val description: String,
    val teacherId: String,
    val participants: List<String> = emptyList(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val roomCode: String? = null
)

data class RoomActivity(
    val id: String,
    val roomId: String,
    val type: ActivityType,
    val title: String,
    val description: String,
    val createdAt: Long,
    val isActive: Boolean = true
)

enum class ActivityType { QUIZ, ATTENTION_CHECK, SURVEY, BREAK }
```

#### **Step 5.2: Firestore Integration**

```kotlin
// Real-time room management
class RoomRepository {
    fun getRoomsForUser(userId: String): Flow<List<Room>>
    suspend fun createRoom(room: Room): Result<String>
    suspend fun joinRoom(roomId: String, userId: String): Result<Unit>
    suspend fun leaveRoom(roomId: String, userId: String): Result<Unit>
    fun getRoomActivities(roomId: String): Flow<List<RoomActivity>>
}
```

#### **Step 5.3: Room Screens Implementation**

```kotlin
// Implementation priority:
1. RoomsListScreen.kt        # Browse available rooms
2. CreateRoomScreen.kt       # Teachers create new rooms
3. JoinRoomScreen.kt         # Students join by code/selection
4. RoomDetailScreen.kt       # Inside room interface
5. TeacherRoomControls.kt    # Teacher dashboard for room management
```

---

### **Phase 6: Camera & Attention Tracking (Days 24-30)**

#### **Step 6.1: Camera Permission & Setup**

```kotlin
// Implement camera access with proper permissions
@Composable
fun CameraPermissionScreen() {
    // Use Accompanist permissions
    // Request camera permission
    // Handle permission denied states
    // Educational content about tracking purpose
}
```

#### **Step 6.2: ML Kit Integration**

```kotlin
// Face detection and attention analysis
class AttentionTracker {
    fun startTracking(
        onFaceDetected: (List<Face>) -> Unit,
        onAttentionChange: (AttentionState) -> Unit
    )
    
    fun stopTracking()
    
    fun analyzeAttention(faces: List<Face>): AttentionMetrics
}

data class AttentionMetrics(
    val isLookingAtScreen: Boolean,
    val facePosition: PointF,
    val eyeOpenProbability: Float,
    val smileProbability: Float,
    val timestamp: Long
)
```

#### **Step 6.3: Tracking Data Storage**

```kotlin
// Local and cloud storage for research data
@Entity(tableName = "attention_sessions")
data class AttentionSession(
    @PrimaryKey val id: String,
    val userId: String,
    val roomId: String?,
    val startTime: Long,
    val endTime: Long?,
    val metrics: List<AttentionMetrics>,
    val isSynced: Boolean = false
)

// Background sync with WorkManager
class SyncAttentionDataWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        // Upload local data to Firebase
        // Handle offline scenarios
    }
}
```

---

### **Phase 7: Market & Gamification (Days 31-35)**

#### **Step 7.1: Market Data Models**

```kotlin
data class MarketItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val category: ItemCategory,
    val imageUrl: String,
    val isAvailable: Boolean = true
)

enum class ItemCategory { AVATAR_ACCESSORY, BACKGROUND, BADGE, BOOST }

data class UserInventory(
    val userId: String,
    val items: List<String>,
    val coins: Int = 0
)
```

#### **Step 7.2: Market Implementation**

```kotlin
// Market screens and functionality
1. MarketScreen.kt           # Browse items by category
2. ItemDetailScreen.kt       # View item details, purchase
3. InventoryScreen.kt        # User's owned items
4. CoinEarningSystem.kt      # Award coins for engagement
```

---

### **Phase 8: Advanced Features (Days 36-42)**

#### **Step 8.1: Real-time Activities**

```kotlin
// Teacher-triggered activities for students
@Composable
fun QuizActivityScreen(activity: RoomActivity) {
    // Real-time quiz interface
    // Submit answers to Firestore
    // Show results and feedback
}

@Composable
fun AttentionCheckScreen() {
    // Brief attention measurement
    // Camera-based engagement check
    // Immediate feedback to teacher
}
```

#### **Step 8.2: Analytics & Reporting**

```kotlin
// Research data aggregation
class AnalyticsRepository {
    suspend fun getSessionAnalytics(userId: String): SessionAnalytics
    suspend fun getRoomAnalytics(roomId: String): RoomAnalytics
    suspend fun exportResearchData(studyId: String): ByteArray
}
```

#### **Step 8.3: Notifications**

```kotlin
// Firebase Cloud Messaging integration
class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle room invites
        // Activity notifications
        // Achievement alerts
    }
}
```

---

### **Phase 9: Testing & Polish (Days 43-47)**

#### **Step 9.1: Unit Testing**

```kotlin
// Test critical business logic
class AuthRepositoryTest {
    @Test
    fun `signIn with valid credentials returns success`()
    
    @Test
    fun `signUp creates user with correct role`()
}

class AttentionTrackerTest {
    @Test
    fun `analyzeAttention correctly identifies looking away`()
}
```

#### **Step 9.2: UI Testing**

```kotlin
// Test user flows with Compose Testing
@Test
fun loginFlow_validCredentials_navigatesToDashboard()

@Test
fun roomCreation_teacherRole_createsRoomSuccessfully()
```

#### **Step 9.3: Performance & Accessibility**

```
- Test app performance with large datasets
- Verify accessibility compliance (TalkBack, screen readers)
- Test offline functionality
- Memory leak detection
- Battery usage optimization
```

---

### **Phase 10: Deployment Preparation (Days 48-50)**

#### **Step 10.1: Production Configuration**

```kotlin
// Environment-specific configurations
buildTypes {
    debug {
        buildConfigField("String", "API_ENDPOINT", "\"https://dev-api.attentionally.com\"")
        debuggable = true
    }
    release {
        buildConfigField("String", "API_ENDPOINT", "\"https://api.attentionally.com\"")
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
}
```

#### **Step 10.2: Security & Privacy**

```
- Implement data encryption for sensitive research data
- Add privacy policy and terms of service
- Configure Firebase security rules
- Set up user consent flows for data collection
- Implement data export/deletion (GDPR compliance)
```

#### **Step 10.3: Release Preparation**

```
- Generate signed APK
- Configure Play Store listing
- Prepare app screenshots and descriptions
- Set up crash reporting (Firebase Crashlytics)
- Configure analytics tracking
```

---

### **🎯 Implementation Priority Matrix**

#### **HIGH PRIORITY (Must Have for MVP)**

1. ✅ Firebase setup & authentication
2. ✅ Basic navigation & role management
3. ✅ Simple room creation/joining
4. ✅ Basic attention tracking
5. ✅ Profile management

#### **MEDIUM PRIORITY (Important for User Experience)**

1. 🔄 Real-time room activities
2. 🔄 Market & gamification
3. 🔄 Advanced analytics
4. 🔄 Notifications
5. 🔄 Offline functionality

#### **LOW PRIORITY (Nice to Have)**

1. ⏳ Advanced avatar customization
2. ⏳ Social features
3. ⏳ Advanced reporting
4. ⏳ Multi-language support
5. ⏳ Web dashboard

---

### **📅 Estimated Timeline: 50 Days**

- **Weeks 1-2**: Foundation & Authentication
- **Weeks 3-4**: Core UI & Navigation
- **Weeks 5-6**: Room System & Real-time Features
- **Weeks 7-8**: Camera & Tracking Implementation
- **Weeks 9-10**: Polish, Testing & Deployment

**Total Estimated Effort**: ~400 hours for complete implementation

---

## 1. Executive Summary

Build a modern, scalable Android application for small-scale research studies targeting children
with Autism Spectrum Disorder (ASD)—with a focus on classroom scenarios involving up to ~10 students
per study and their teachers. The developer is the researcher. Using Jetpack Compose, MVVM
architecture, and Material Design 3 expressive guidelines, the app will integrate Firebase as the
backend for authentication, data collection, and session management. Core research functionality
will include tracking, analyzing, and reporting on students' facial expressions and attention states
during app interaction, supporting studies and interventions for ASD.

---

## 2. Goals and Success Metrics

- **Goal:** Deliver an MVP that’s production-ready for user authentication, backend services, and
  compliance with the latest Android UI/UX standards.
- **Success Metrics:**
    - App boots with Firebase Auth fully functional (email/password, potentially Google).
    - MVVM and Compose enforced everywhere for logic separation and testability.
    - Smooth onboarding/signup/login, error handling, and secure session state.
    - Material Design 3 applied to all user-facing screens.
    - Flexible for rapid iteration and future features (marketplace, rooms, avatars, teacher/student
      roles).

---

## 3. Core Features

- **Attention & Facial Expression Tracking:** Use device camera and ML/vision models to track and
  analyze children's facial expressions, gaze, and attention changes in real time, providing
  quantitative data for research on ASD.
- **Authentication:** Firebase Auth (email/password, Google sign-in, etc.)
- **User Registration & Login:** Compose forms, validation, error flows, password hiding/toggle,
  password strength feedback.
- **Role Selection:** Teacher/Student selectable during registration.
- **Session Handling:** Persistent login across app restarts, secure state, sign-out.
- **Material 3 UI:** All screens, navigation, forms, buttons, chips, etc. to follow Material 3
  guidelines and expressive layouts.
- **MVVM Pattern:** Strict ViewModel usage for business logic, Composable for UI, Repository for
  Firebase/other data sources.
- **Firebase Integration:** Ability to extend to Firestore, Realtime DB, Storage, Messaging, etc.
  with minimal friction.
- **Scalability:** Simple code, minimal hard-coded variables to allow future growth/features.
- **Platform Awareness:** Designed for Android, mindful of future cross-platform moves (iOS,
  Desktop, etc).

---

## 4. Planned Features

### MVP Features

- **User Authentication:** Email/password sign-up, login, secure session persistence, and *
  *Anonymous guest sessions**.
- **Role Management:** Select and save user type (Teacher or Student) at registration; reflect role
  throughout experience (no researcher role, as researcher is developer).
- **Profile:** User profile for managing name, email, avatar (future-ready for avatars/images).
- **Dashboard (Landing Page):** Context-sensitive for Teacher or Student, summarizing key
  activities (tasks, market, rooms).
- **Market:** View and buy/sell app items, coins/points, and featured assets (MVP: static or demo
  items).
- **Rooms:** List of study/classrooms, ability to join or create (MVP: basic join/create logic,
  possibly with mock data).
- **Navigation:** Bottom navigation bar for swift access to Dashboard, Market, Avatar, Rooms.
- **Material 3 UI/UX:** All primary screens built with Compose and Material 3 guidelines, including
  error/success feedback and proper theming (light/dark).
- **Logout:** User can securely sign out from anywhere via menu/navigation.

### Advanced & Future Features

- **Avatar Builder:** Custom avatar/profile image creation and item equipping.
- **Marketplace Expansion:** Real-time selling, trading, and dynamic inventory with Firestore
  backend.
- **Classroom Management:** Advanced room/class features for Teachers: add/remove students,
  moderate, assign tasks or content.
- **Messaging/Chat:** Real-time or asynchronous messaging between users, leveraging Firebase
  Messaging or Firestore.
- **Notifications:** Push notifications (Firebase Cloud Messaging), reminders for tasks/room
  activities/events.
- **Tasks & Achievements:** Assignments, checklists, gamified progress tracking, and achievement
  badges for users.
- **Leaderboard/Stats:** Rankings and stats, visible to all users (or filtered by class).
- **Analytics:** Firebase Analytics for event/state tracking, user engagement, and insights for
  teachers/admin.
- **Admin/Dashboard Panel:** Web or in-app panel for teachers/school admins to manage
  users/content/data.
- **Payments Integration:** Future: add in-app purchases or rewards (Google Play Billing).
- **Multiplatform Expansion:** Codebase prep for iOS and desktop using Compose Multiplatform.
- **Localization:** Multi-language support for broader global access.
- **Accessibility:** WCAG-compliant color/font/interaction standards.

## 5. Technical Stack

- **Language:** Kotlin (primary, idiomatic usage)
- **UI Layer:** Jetpack Compose (Material3)
- **Architecture:** MVVM + Repository Pattern
- **Auth/Backend:** Firebase Authentication—supports Email/Password, Google login, and **Anonymous
  Auth**; Firestore for future features.
- **Build Tools:** Gradle, using BOM for dependency management of Firebase and Compose
- **Testing:** JUnit for unit tests, Espresso for UI tests, Compose Testing Library
- **Dependency Injection:** (Recommend Koin or Hilt if project is non-trivial)
- **Version Control:** Git, with relevant .gitignore and workflow hooks

---

## 5a. Dependency Management & Justification

This project uses Gradle Version Catalogs (`libs.versions.toml`) to centrally manage all
dependencies, ensuring version consistency and upgrade safety. The following dependencies are
essential for the goals and research features of this application:

```toml
[versions]
kotlin = "1.9.23"
compose-bom = "2024.05.01"
koin = "3.5.3"
firebase-bom = "33.0.0"
camera = "1.3.1"
mlkit-face = "16.1.7"
mlkit-vision = "17.3.0"
lifecycle = "2.7.0"
coroutines = "1.7.3"
accompanist-perms = "0.32.0"
navigation = "2.7.6"

[libraries]
# --- Kotlin Stdlib (App core language) ---
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }

# --- Compose UI Stack (Modern Android UI, Material 3 compliance) ---
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" } # Aligns all Compose library versions
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" } # Material Design 3 components
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }

# --- Navigation ---
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" } # For in-app screen flows

# --- Lifecycle & ViewModel (for proper MVVM and state management) ---
lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# --- Koin (Dependency Injection, modular/rescalable application structure) ---
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-androidx-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

# --- Firebase BOM & Components ---
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" } # Version alignment for all Firebase
firebase-auth-ktx = { module = "com.google.firebase:firebase-auth-ktx" } # Secure user sign-up and login
firebase-firestore-ktx = { module = "com.google.firebase:firebase-firestore-ktx" } # Realtime data for rooms, quizzes, and tracking
firebase-storage-ktx = { module = "com.google.firebase:firebase-storage-ktx" } # For avatars/recorded media
firebase-analytics-ktx = { module = "com.google.firebase:firebase-analytics-ktx" } # Usage analytics and research data
firebase-messaging-ktx = { module = "com.google.firebase:firebase-messaging-ktx" } # For notifications and future real-time events

# --- CameraX (Device camera integration for ML and attention tracking) ---
camera-core = { module = "androidx.camera:camera-core", version.ref = "camera" }
camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camera" }
camera-view = { module = "androidx.camera:camera-view", version.ref = "camera" }
camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camera" }

# --- ML Kit (On-device facial expression/attention detection) ---
mlkit-face-detection = { module = "com.google.mlkit:face-detection", version.ref = "mlkit-face" }
mlkit-vision-common = { module = "com.google.mlkit:vision-common", version.ref = "mlkit-vision" }

# --- Coroutines (for performant async data flows and listeners) ---
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

# --- Accompanist (easy permission management, especially for camera/microphone) ---
accompanist-permissions = { module = "com.google.accompanist:accompanist-permissions", version.ref = "accompanist-perms" }
```

### **Dependency Rationale**

- **Kotlin & Coroutines:** Core Android language and necessary for asynchronous task management,
  e.g. live data, Room/Firestore listeners, and ML/Camera pipeline.
- **Compose, Material3, Navigation:** Modern UI, responsive and accessible; foundation for scalable,
  maintainable MVVM app.
- **Lifecycle, ViewModel:** Enables separation of business/UI logic, making the app robust,
  testable, and research-grade.
- **Koin:** Lightweight dependency injection so complex features (tracking, ML, quizzes, analytics)
  are easy to modularize and swap in/out for studies.
- **Firebase Auth & Firestore:** Real-time user, room, and experiment coordination; enables
  real-time quizzes/activities, session logs, and secure research data.
- **Firebase Storage & Analytics:** Profile avatar uploads, storage for recorded media, and
  analytics for both product improvement and research.
- **Firebase Messaging:** Preps for remote notifications or “live events” communications between
  researchers, students, teachers.
- **CameraX & ML Kit:** Absolutely required for facial expression analysis, attention metrics, and
  in-app research measurements.
- **Accompanist:** Easiest and safest way to manage user permissions (camera, possibly microphone)
  in production apps.

All of the above compose a robust foundational stack—flexible for both MVP and research
extensibility, while following best practices in modern Android development.

---

## 6. Firebase Integration Requirements

- **Immediate:**
    - Connect project to Firebase via console.
    - Download and integrate google-services.json.
    - Add Firebase BOM and core libraries to Gradle (auth, optionally Firestore and Storage).
    - Initialize Firebase in Application class.
  - Replace dummy logic with Firebase SDK calls for login/signup/**anonymous/guest access**/session
    management.
    - Handle all auth result/errors in the UI following Material guidelines.
  - Provide **guest flow** with anonymous sign-in accessible from AuthenticationScreen.
- **Extensible:**
  - Linking anonymous account to a permanent user account (email/password or Google), preserving
    session data.
    - Firestore or Realtime Database ready for later Marketplace, Room, or Avatar features.
    - Storage for profile images or other media.
    - Analytics, Messaging, Cloud Functions as needed.

---

## 7. User Journey

### 1. Registration, Onboarding, and Login

- User opens app (child, parent, teacher, or researcher)
- If new: chooses between registration (Student, Teacher, Researcher) **or guest mode (Anonymous
  Auth) with limited features**
  - Enters required info (email, password, name, etc.) or proceeds as guest
    - For students/children: parent/guardian consent UI (if required by study/regulations)
- If returning: logs in via email/password (or Google, if enabled), **or resumes as guest (if not
  explicitly logged out or upgraded)**
- On first login, onboarding/tutorial is presented
- **Users in guest mode may upgrade to full account, preserving UID/data, at any time**

### 2. Profile and Avatar

- User can view/edit profile (name, email, role, basic details)
- Customize or select avatar (from default images or custom editor)
- Avatar used throughout UI, including in rooms and on leaderboards

### 3. Marketplace: Browsing and Buying

- User enters marketplace via bottom navigation
- Browses categories of items (wearables, badges, backgrounds, boosters, etc.)
- Views item details, affordability (based on user's coins/points)
- "Buy" button enabled if user has enough coins
- On purchase:
    - Coins deducted, item added to user's inventory/profile
    - Confirmation and positive feedback UI
    - Purchased avatar items shown on profile/avatar

### 4. Rooms: Create, Join, Leave, Interact

- Any user can view list of available "Rooms" (classrooms or study rooms)
- Student can join "open" rooms or by room code (invite-only or public)
- Teacher can create a new room (set name, codes, visibility)
- On joining:
    - User is added to room's participants; presence tracked
    - Student sees list of other students/teacher present
    - Room context menu for leaving or switching rooms
- On leaving room, user is removed from participants (tracked for session data)

### 5. Teacher Dashboard / Room Actions

- Teacher sees real-time list of participants in their room
- Can trigger quizzes/activities (e.g. "Attention Check Quiz") with a button
- Triggers are broadcast (via Firestore) to all room participants
- View real-time student responses and attention analytics in dashboard panel
- Can clear or post new activities at any time
- Can award coins/items to students for engagement

### 6. Student Flow in Room (Quizzes, Activities)

- Students in room immediately see quiz/activity modal/dialog when teacher triggers one
- UI updates in real time as soon as activity appears in Firestore
- Student interacts with the quiz (selects answer, submits, timer if needed)
- Answers are posted to student's record (for analytics and for teacher view)
- Positive/neutral feedback after submission
- Returns to typical room interface after activity

### 7. Attention & Facial Expression Tracking

- Device camera (with consent) monitors student during session (background or at intervals)
- ML/vision tracks attention, engagement, and facial affect, logging quantitative session data to
  Firebase (per room or per activity)
- Teacher/researcher can view summary analytics (individual or group attention, e.g. times looked
  away, smiles, etc.)
- Alerts or encouragements may be triggered by system or teacher (e.g. "Please pay attention!" if
  disengaged)

### 8. Logging Out/Session Management

- User can log out via navigation or menu
- Session state is cleared; user returned to login
- Persistent login on next launch (if desired, app uses secure session tokens)


---

---

## 8. UX/UI Standards

- Use Material Design 3 Expressive guidelines for:
    - Card, Button, Chip, NavigationBar, TextField, etc.
    - Responsive layouts, rounded corners, padding consistent across screens
    - Error display and state changes
    - Color schemes and dark mode

---

## 9. Development Best Practices

- Follow Kotlin coding conventions.
- Use nullable types sparingly.
- Reuse Composable functions and ViewModels instead of duplicating logic.
- Comment code clearly and concisely.
- Proactively add code for environment variables if needed (.env or local.properties).
- Ensure all important logic is unit tested before release.

---

## 10. Project Setup Checklist

```
- [ ] Create blank Jetpack Compose project with MVVM skeleton
- [ ] Connect to Firebase Console and download google-services.json
- [ ] Add Firebase BOM and required dependencies to Gradle
- [ ] Initialize Firebase in Application class
- [ ] Implement Authentication screens (login/signup/guest) with direct Firebase integration
- [ ] Enforce role selection and session management via Firebase user custom claims if needed
- [ ] Apply Material Design 3 Expressive guidelines to all UI
- [ ] Set up basic ViewModel and Repository pattern for future extensibility
- [ ] Ensure scalability: keep variable/general types for future features
- [ ] Test login/sign-up/guest flows, error states, and user persistence thoroughly
```

---

## 11. Additional Considerations

- Ensure privacy compliance: clearly display when in anonymous/guest mode, limit data collection
  unless consent is provided, and provide easy upgrade or delete for session/researcher compliance.
- Recommended: Use automatic deletion of anonymous accounts older than 30 days if configured in
  Firebase project.
- Prepare for iOS/Android multiplatform if future plans demand.
- Ensure build is CI-friendly (GitHub Actions, etc.).
- Use latest official Firebase and Compose documentation for reference.

---

### Example: Essential Gradle configuration

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android { /* ... */ }

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    // ... other Firebase and Compose dependencies
}
```

---

This markdown PRD gives you a clear, actionable guide for your next project or refactor—ensuring
Firebase is correctly integrated from day one, with full support for MVVM and Material3 best
practices.
