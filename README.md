# HiveMarket

PROG7314 Group 2 — Portfolio of Evidence, Part 2 (App Prototype)

Group: Nonjabulo Mathenjwa (ST10077892), Luke Lutchmiah (ST10288560), Lonwabo Gumede (ST10270409), Ayabonga Nzuza (ST10400793)

---

## Part 2 status

This prototype implements: **Login + Register (Firebase Authentication)**, **Browse listings** with category filtering on an offline-first Room cache, **Settings** (FR2 — language with real runtime locale switching, notifications, biometric toggle, log out), **Listing Detail** (view + Make Offer + start/open a conversation), **Create Listing** (FR4/FR9, fully offline-first), **Offline Drafts** (view + manually retry unsynced listings), and **Chat** (real message history + sending). A persistent bottom navigation bar appears on every top-level screen.

**The group's 3 chosen user-defined features for Part 2 grading**: Verified Seller / Trust Score badge, in-app secure chat, and offline listing drafts with auto-sync (manual retry — see "What's deliberately not built yet" below for the one honest gap in this).

---

Activity 1: App Concept and API Proposal
PROG7314 | Group Worksheet | Maximum 4 students

1. Group and Concept
Group members and student numbers:

Nonjabulo Mathenjwa — ST10077892
Luke Lutchmiah — ST10288560
Lonwabo Gumede — ST10270409
Ayabonga Nzuza — ST10400793

Working app name: HiveMarket

Problem to be solved: Students currently buy and sell second-hand goods (textbooks, electronics, furniture, appliances) through informal, unsafe, and fragmented channels such as WhatsApp groups and physical noticeboards, with no student-only verification, no organised search, and no reliable way to negotiate or get notified while offline or on poor campus wifi.
Target users: University and TVET college students across multiple South African campuses, particularly students looking to buy or sell textbooks and everyday items within their own trusted student community.

2. Proposed Mobile Solution

Core solution

HiveMarket is a native Android marketplace app restricted to verified students. The main user journey is: a student signs in with Firebase Authentication using their student email, browses or searches a categorised feed of listings from their campus, opens a listing to view details and photos, messages the seller in-app to negotiate, and receives a push notification when they get a reply or offer. Students can also create their own listing — including while offline, with the draft syncing automatically once connectivity returns — and manage preferences such as language and notifications from a settings screen.

Why Android?

A native Android app is appropriate because the required feature set leans heavily on device-level capabilities that are best accessed natively: BiometricPrompt for fingerprint/face authentication, RoomDB for a local offline cache and sync queue, the device camera for listing photos, and Firebase Cloud Messaging for real-time background push notifications even when the app is closed. These capabilities are more reliable and better supported through native Android APIs than through a cross-platform or web wrapper.

Positive impact

HiveMarket reduces the cost of studying by making second-hand textbooks and equipment easier to find and trust within a student-only community, encourages reuse over waste by keeping still-useful items circulating on campus, and improves safety relative to informal channels by verifying student identity and keeping communication inside the app. Offline listing support also makes the app usable for students in residences or areas with limited data access.

3. Custom REST API

The API is the central component of HiveMarket: it owns the authoritative database of users, campuses, listings, images, conversations, and notifications, and enforces all business rules that the mobile client cannot be trusted to enforce itself — such as verifying that a user's email domain matches a known campus before granting access, ensuring only a listing's owner can edit or delete it, resolving conflicts when an offline-created listing syncs, and triggering push notifications when relevant events occur. The mobile app is intentionally kept as a presentation and offline-cache layer; almost all state changes and validation logic live server-side.

API responsibilities and business logic

Validating Firebase ID tokens and mapping verified student email domains to campuses; enforcing ownership and permission checks on listings and messages; managing listing lifecycle status (active, sold, removed); storing and serving image references from blob storage; orchestrating conversation and message delivery; and triggering Firebase Cloud Messaging notifications on relevant events.

Proposed endpoints (minimum five)

#	Method and route	Purpose
1	POST /api/auth/firebase -	Verify a Firebase ID token (via the Firebase Admin SDK) and return/provision the matching HiveMarket user profile
2	GET /api/listings -	Browse, search and filter active listings by campus, category, price range and keyword
3	POST /api/listings -	Create a new listing (accepts client-generated GUID so offline-created drafts sync idempotently)
4	GET /api/listings/{id} -	Retrieve full listing detail, including seller info and image URLs, for the listing detail screen
5	POST /api/conversations/{id}/messages -	Send a chat message inside an existing buyer-seller conversation about a listing
6	PATCH /api/users/me/settings -	Update the current user's settings (language, notification and biometric preferences)

4. POE Feature Fit

Requirement	How it will fit the proposed app
Single Sign-On	Students sign in via Firebase Authentication using their student email (email/password, with Google as an optional federated provider). Sign-up is restricted to recognised student email domains, and the API validates the Firebase ID token on every request via the Firebase Admin SDK before matching the user to a Campus record.
Settings (3+)	1) Preferred language (English / isiZulu / Afrikaans). 2) Push notification toggle (new messages, new offers). 3) Biometric login toggle. 4) Default campus/location for the home feed.
Biometric authentication	Android BiometricPrompt gates re-entry to the app after the first Firebase sign-in and confirms sensitive actions such as marking an item as sold, without requiring the user to re-authenticate via Firebase each time.
Offline action + synchronisation	Listings can be drafted and saved locally in RoomDB while offline. A WorkManager background job detects reconnection and pushes any queued listing creations/edits to the REST API, resolving conflicts by timestamp.
Real-time notification	The API triggers Firebase Cloud Messaging pushes when a new message or offer is created on a listing the user owns or is chatting about, delivering real-time alerts even when the app is closed.
Two South African languages	isiZulu and Afrikaans are implemented via Android string resources and a runtime locale switch exposed in Settings, in addition to the default English UI.

5. Five User-Defined Features

#	Feature	Purpose and user value
1	Verified Seller / Trust Score badge	Gives buyers a quick, visible signal of a seller's reliability based on completed transactions and account verification, increasing confidence in a stranger-to-stranger trade.
2	Category-based smart search & filters	Lets students narrow a large, mixed-goods catalogue by category, price range and campus in a few taps, reducing time-to-find for a specific item.
3	Offline listing drafts with auto-sync	Allows a student to create or edit a listing in areas with poor campus wifi/data coverage (e.g. residence rooms) without losing their work, syncing automatically once reconnected.
4	Save / Favourite listings	Lets a buyer bookmark items of interest while browsing and revisit them later without having to search again.
5	In-app secure chat tied to a listing	Keeps all negotiation inside the trusted platform, so buyers and sellers do not need to exchange personal phone numbers before they are ready to.

6. Comparable Android Apps
This is not the formal research report. These three apps are identified here as suitable candidates to be compared in detail in the formal Research Report.
App	Google Play Store link	Why it is comparable
Campus Trade	play.google.com/.../com.campustrade.app	South African student marketplace for textbooks and study essentials — directly comparable regional audience and core buy/sell loop.
Campora	play.google.com/.../com.campora.app	Broader campus super-app with marketplace, chat, verified accounts and payments — shows the ceiling of feature scope to compare against.
Vezzy	hypepotamus.com/companies/vezzy	Student-built, university-email-verified, geolocated second-hand marketplace — comparable trust model and single-campus MVP scope.

7. Feasibility and Approval
Proposed technology
Android client: Kotlin, Jetpack Compose, MVVM, RoomDB (offline cache/sync), BiometricPrompt, Firebase Cloud Messaging SDK.
API: ASP.NET Core Web API with Entity Framework Core.
Database: PostgreSQL (or SQLite for lightweight deployment).
Hosting: Render (Dockerised Web Service deployment from the same container used in Docker coursework), with image assets stored via Firebase Storage. Render's free tier supports the project's budget; a low-cost starter tier removes cold starts if continuous uptime is needed for demos.
Authentication: Firebase Authentication (email/password, with Google as an optional federated sign-in), with sign-up restricted to recognised student email domains. The API validates the resulting Firebase ID token on each request using the Firebase Admin SDK, rather than operating a first-party OAuth2/OIDC SSO server.

Three main risks and how they will be reduced

1) Offline sync conflicts, where the same listing is edited on two devices — reduced with client-generated GUIDs, timestamp-based versioning, and a simple last-write-wins resolution strategy in the sync worker.
2) Scope creep across the many POE-required features — reduced by following a strict, task-level Gantt chart and treating non-essential features as the first to be cut.
3) Firebase Authentication integration combined with Play Store release requirements — reduced by using the well-documented Firebase Authentication and Admin SDKs, and keeping a simple email/password path as the primary flow with Google sign-in as a secondary option.

Scope check

If the project becomes too large, the Trust Score/Verified Seller badge feature will be removed first, followed by the Save/Favourites feature, while the core buy-sell loop, in-app chat, push notifications, offline sync, Firebase Authentication, and biometric authentication are preserved as non-negotiable POE requirements.
Lecturer Decision
☐ Approved     ☐ Approved with changes     ☐ Revise and resubmit
Conditions or comments:

---

## Architecture

- **UI**: Jetpack Compose, single-Activity, `NavHost`-based navigation, with a persistent bottom navigation bar (Home/Search → Browse, Sell → Create Listing, Messages/Profile → placeholders — see below).
- **Pattern**: MVVM. Screens hold no logic — they read `StateFlow` from a `ViewModel` and call its functions.
- **DI**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Inject`).
- **Data layer**: Repository pattern. `ListingRepository` is the single source of truth, combining:
  - **Room** (`HiveMarketDatabase`) — local cache, and the offline-draft queue for FR4/FR9.
  - **Retrofit + kotlinx.serialization** (`HiveMarketApi`) — talks to the group's ASP.NET Core API.
- **Auth**: Firebase Authentication. An `OkHttp` interceptor attaches the current user's Firebase ID token to every API call automatically (see `NetworkModule`) — screens and ViewModels never handle tokens directly.
- **Push**: Firebase Cloud Messaging, received via `HiveMarketMessagingService` (stubbed — logs the payload, doesn't yet render a system notification).
- **Settings/locale**: `SettingsStore` (SharedPreferences) is the offline-first local cache for FR2; language changes are applied at runtime via `LocaleSwitcher` (`AppCompatDelegate`), injected behind an interface specifically so it stays unit-testable.

Every field name in the Kotlin data classes (`domain/Models.kt`) intentionally matches the Data Models and Schema Definitions table in the group's Planning and Design document (e.g. `listingID`, `categoryID`, `price`, not `id`/`amount`) — that consistency is what makes the request/response payloads actually line up with the REST API.

```
app/src/main/java/com/hivemarket/app/
├── MainActivity.kt
├── HiveMarketApp.kt              # Application class, Hilt entry point
├── navigation/NavGraph.kt
├── ui/
│   ├── theme/
│   └── screens/
│       ├── login/                 # Login + Register
│       ├── browse/
│       ├── settings/
│       ├── listingdetail/         # View + Make Offer + start Chat
│       ├── createlisting/         # Offline-first (FR4/FR9)
│       ├── offlinedrafts/         # View + manually retry unsynced listings
│       ├── chat/
│       └── common/                # BottomNavBar, ComingSoonScreen
├── domain/Models.kt               # Shared data classes (User, Listing, Offer, Conversation, Message)
├── data/
│   ├── local/                     # Room database/DAO, SettingsStore, LocaleSwitcher
│   ├── remote/                    # Retrofit API interface, FCM service
│   └── repository/                # ListingRepository — the source of truth
└── di/                             # Hilt modules (Network, Database, App)
```

## Setup

You need a Firebase project and this app's client registered in it before it will run against real auth.

1. Create/open a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name `com.hivemarket.app`.
3. Download the resulting `google-services.json` and place it at `app/google-services.json` (this exact path — it's git-ignored, so it won't be committed).
4. Enable **Email/Password** sign-in under Authentication → Sign-in method.
5. Open the project in Android Studio and let it sync.

**Gradle/AGP/Kotlin versions — read this if the build fails on first sync.** This project targets **Gradle 9.6.0, AGP 9.3.0, and Kotlin 2.1.0**. If your Android Studio auto-generates a different Gradle wrapper version, or sync fails with a version-compatibility error, run **Tools → AGP Upgrade Assistant** rather than hand-editing versions — it resolves the whole chain automatically, capped to what your specific Android Studio release supports. Do not downgrade Gradle to fix an AGP mismatch if you're on a recent JDK (25+) — older Gradle cannot run its daemon on it at all.

**"Cannot add extension with name 'kotlin'" or a `ClassCastException` mentioning `ApplicationExtensionImpl` / `BaseExtension`.** `gradle.properties` sets two opt-outs (`android.builtInKotlin=false`, `android.newDsl=false`) to keep the classic Kotlin Android plugin working under AGP 9. Both are explained in comments right there in the file, and are removed in AGP 10 — a real migration will be needed eventually, not urgent now.

**"[Hilt] Provided Metadata instance has version..." or the same error mentioning `androidx.room.jarjarred`.** Two separate libraries (Dagger and Room) each bundle their own reader for Kotlin's metadata format, and each can fall behind a Kotlin version bump. `app/build.gradle.kts` already pins Hilt 2.57+ with an explicit `kotlin-metadata-jvm` override, and Room to 2.8.4 — see the comments at each dependency for what to do if this recurs after a future Kotlin upgrade.

**Gradle wrapper jar**: not committed (binary files don't belong in source control without Git LFS). Android Studio regenerates it automatically on first open.

**Student email domain**: `LoginViewModel.ALLOWED_EMAIL_DOMAIN` is set to `@student.iie.ac.za` — update if your actual student email domain differs.

**API base URL**: `NetworkModule.BASE_URL` needs to point at wherever the group's ASP.NET Core API is actually deployed. Update this once it's live.

## Running tests locally

```
./gradlew testDebugUnitTest
```

## Continuous Integration

`.github/workflows/android-ci.yml` runs on every push and pull request: sets up JDK 17 + Gradle 9.6.0, copies the committed placeholder `google-services.json.example` into place (real Firebase credentials are never committed), runs the unit tests, builds a debug APK, and uploads it as a workflow artifact.

## What's deliberately not built yet

- **Messages (inbox) and Profile are placeholders** — the bottom nav routes to them, but there's no real screen behind either. Chat itself (reached from Listing Detail's "Message" button) is fully built.
- **Offline Drafts sync is manual-retry only, not automatic.** `WorkManager` is a dependency and initializes at startup, but no `Worker` class has been implemented to retry pending listings automatically when connectivity returns. Since offline drafts is one of the group's 3 chosen features, finishing this (a real `CoroutineWorker` with a `NetworkType.CONNECTED` constraint) is worth prioritising.
- **Listing Detail, Create Listing, and Chat use hardcoded English strings**, not string resources — Login/Browse/Settings are properly localized (en/zu/af); these three aren't yet.
- Favourites and a dedicated search bar (beyond category chips) were not chosen as one of the 3 features and remain unbuilt.
- No real biometric re-entry gate yet — the Settings toggle exists, nothing enforces it.
- Photo upload is a UI stub — Firebase Storage isn't wired in.
