<<<<<<< HEAD
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
=======
# HiveMarket — Android Prototype

A native Android prototype of **HiveMarket**, a campus marketplace app for verified students to buy, sell, and negotiate over second-hand goods within their own institution. This is a personal working branch built from the group's Part 1 design documents (Research Report + Planning and Design), to demonstrate a working milestone ahead of the group's shared Part 2 submission.

## Status

This milestone implements: **Login (Firebase Authentication)** and **Browse listings** with category filtering, backed by an offline-first Room cache. It is a foundation to build the rest of Part 2 on top of, not the final submission.

## Architecture

- **UI**: Jetpack Compose, single-Activity, `NavHost`-based navigation.
- **Pattern**: MVVM. Screens hold no logic — they read `StateFlow` from a `ViewModel` and call its functions.
- **DI**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Inject`).
- **Data layer**: Repository pattern. `ListingRepository` is the single source of truth, combining:
  - **Room** (`HiveMarketDatabase`) — local cache, and the offline-draft queue for FR4/FR9.
  - **Retrofit + kotlinx.serialization** (`HiveMarketApi`) — talks to the group's ASP.NET Core API on Render.
- **Auth**: Firebase Authentication. An `OkHttp` interceptor attaches the current user's Firebase ID token to every API call automatically (see `NetworkModule`) — screens and view models never handle tokens directly.
- **Push**: Firebase Cloud Messaging, received via `HiveMarketMessagingService` (stubbed — logs the payload, doesn't yet render a system notification).

Every field name in the Kotlin data classes (`domain/Models.kt`) intentionally matches the Data Models and Schema Definitions table in the group's Planning and Design document (e.g. `listingID`, `categoryID`, `price`, not `id`/`amount`) — that consistency is what makes the request/response payloads actually line up with the group's REST API spec.

```
app/src/main/java/com/hivemarket/app/
├── MainActivity.kt
├── HiveMarketApp.kt              # Application class, Hilt entry point
├── navigation/NavGraph.kt
├── ui/
│   ├── theme/                    # Compose theme, matches the brand colours from Part 1
│   └── screens/
│       ├── login/                 # LoginScreen + LoginViewModel
│       └── browse/                # BrowseScreen + BrowseViewModel
├── domain/Models.kt               # Shared data classes (User, Listing, Offer, Conversation, Message)
├── data/
│   ├── local/                     # Room database, DAO, entity
│   ├── remote/                    # Retrofit API interface, FCM service
│   └── repository/                # ListingRepository — the source of truth
└── di/                             # Hilt modules (Network, Database)
```

## Setup

You need a Firebase project and this app's client registered in it before it will run against real auth.

1. Create/open a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name `com.hivemarket.app`.
3. Download the resulting `google-services.json` and place it at `app/google-services.json` (this exact path — it's git-ignored, so it won't be committed).
4. Enable **Email/Password** sign-in under Authentication → Sign-in method.
5. Open the project in Android Studio (Koala or newer recommended) and let it sync.

**Gradle/AGP/Kotlin versions — read this if the build fails on first sync.** This project targets **Gradle 9.6.0, AGP 9.3.0, and Kotlin 2.1.0**. These are recent as of September 2026 and move fast — if your Android Studio auto-generates a different Gradle wrapper version than what's committed here (check `gradle/wrapper/gradle-wrapper.properties`), or if sync fails with a version-compatibility error, run **Tools → AGP Upgrade Assistant** in Android Studio rather than hand-editing versions — it resolves the whole chain (AGP, Kotlin, Gradle) as a matched set automatically, capped to what your specific Android Studio release actually supports. Do not try to downgrade Gradle to fix an AGP mismatch if you're on a very recent JDK (26+) — Gradle 8.x cannot run its daemon on JDK 25/26 at all, so downgrading trades one broken build for another.

**"Cannot add extension with name 'kotlin'" or a `ClassCastException` mentioning `ApplicationExtensionImpl` / `BaseExtension`.** AGP 9.0+ changed enough internally that the classic `org.jetbrains.kotlin.android` plugin this project uses can't apply cleanly on its own. `gradle.properties` sets two opt-outs to buy time before a real migration:
- `android.builtInKotlin=false` — stops AGP's new built-in Kotlin support from registering its own `kotlin` extension and colliding with the explicit plugin.
- `android.newDsl=false` — restores the old `BaseExtension`-based DSL types the classic Kotlin plugin expects internally, fixing the `ApplicationExtensionImpl ... cannot be cast to ... BaseExtension` error.

If you still hit either error, confirm both lines are present in `gradle.properties`. **Both opt-outs are removed entirely in AGP 10** (originally targeted mid-2026, not yet shipped as of this writing) — this project will need a proper migration to AGP's built-in Kotlin and new DSL before upgrading past AGP 9.x. See the [built-in Kotlin migration guide](https://developer.android.com/build/migrate-to-built-in-kotlin) when there's time to do it properly; this isn't urgent for the current milestone.

**Gradle wrapper jar**: the wrapper's binary jar (`gradle/wrapper/gradle-wrapper.jar`) isn't committed to this repo. Android Studio will offer to regenerate it automatically the first time you open the project — accept that prompt, or run `gradle wrapper --gradle-version 9.5.0` once if you have a local Gradle install.

**Student email domain**: `LoginViewModel` currently checks for `@student.iie.ac.za` as a placeholder — update `ALLOWED_EMAIL_DOMAIN` to your actual institution's domain.

**API base URL**: `NetworkModule.BASE_URL` points at a placeholder Render URL. Point it at `http://10.0.2.2:5000/` (the emulator's alias for your machine's `localhost`) while testing against a locally-run API, and swap to the real Render URL once it's deployed.

## Running tests locally

```
./gradlew testDebugUnitTest
```

(or `gradle testDebugUnitTest` if you haven't generated the wrapper jar yet — see above.)

## Continuous Integration

`.github/workflows/android-ci.yml` runs on every push and pull request:
1. Checks out the repo and sets up JDK 17 + Gradle 8.9.
2. Copies the committed placeholder `google-services.json.example` into place — real Firebase credentials are never committed, but the Google Services Gradle plugin still needs *a* file at that path to let the build proceed.
3. Runs `testDebugUnitTest`.
4. Builds a debug APK and uploads it as a workflow artifact.

This uses `gradle/actions/setup-gradle` rather than the committed `./gradlew` script, since the wrapper jar itself isn't checked in (see "Gradle wrapper" above) — CI provisions its own Gradle instead of depending on that file.

## What's deliberately not built yet

This is a milestone, not the finished Part 2 submission:
- Listing Detail, Chat, Messages, Profile, and Settings screens all have designs and a matching data/API layer already in place, but no UI yet — each follows the same pattern as `BrowseScreen`.
- The three user-defined features the group actually commits to for grading (see the Part 2 group plan) aren't implemented yet — Favourites and offline-draft creation have partial backing in the repository/database layer already (`createListingOfflineFirst`), but no screen calls them yet.
- Biometric re-entry, FCM notification rendering, and full isiZulu/Afrikaans string coverage exist as stubs or partial resources, not finished features.
>>>>>>> 3dbdbed (Initial commit: Login + Browse, Firebase Auth, offline-first repository, unit tests, CI)
