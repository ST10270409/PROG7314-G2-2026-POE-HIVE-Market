# 🐝 HiveMarket

## PROG7314 — Group 2 | Part 2 App Prototype

HiveMarket is a native Android marketplace application designed for
university and TVET college students in South Africa. The application
provides a student-focused environment for buying and selling second-hand
items such as textbooks, electronics, furniture and other everyday goods.

The application was developed as part of the **PROG7314 Portfolio of
Evidence (POE) Part 2 – App Prototype Development**.

### Group Members

| Student | Student Number |
|---|---|
| Nonjabulo Mathenjwa | ST10077892 |
| Luke Lutchmiah | ST10288560 |
| Lonwabo Gumede | ST10270409 |
| Ayabonga Nzuza | ST10400793 |

---

## Project Purpose

HiveMarket aims to provide students with a more organised and trusted
alternative to informal second-hand trading through platforms such as
WhatsApp groups and physical noticeboards.

The application allows students to:

- Register and sign in using Firebase Authentication.
- Browse available marketplace listings.
- Filter listings by category.
- View detailed information about individual listings.
- Make offers to sellers.
- Start conversations with sellers through in-app chat.
- Create listings while connected or offline.
- Store offline listing drafts locally.
- Synchronise pending listings with the backend.
- Manage application settings.
- Save favourite listings.

The application follows an **offline-first approach**, allowing selected
functionality to remain available when network connectivity is limited.
Local data is stored using Room Database and synchronisation is performed
when communication with the backend is available.

---

## Part 2 Scope

The Part 2 prototype focuses on implementing the core functionality
identified during the design phase. The implemented prototype includes
authentication, listing browsing, listing details, listing creation,
offline drafts, offers, conversations, settings and local caching.

The application also demonstrates integration with a custom REST API,
Firebase services and Android SDK functionality.

Features that were identified during the planning phase but were not
required or fully completed for this prototype are documented separately
in the **Known Limitations** section.


---

## Features and Functional Scope

The HiveMarket prototype implements the main marketplace functionality
identified for the Part 2 submission. The features are organised around
authentication, marketplace listings, communication between users and
offline-first functionality.

### Implemented Features

#### 1. User Authentication

Users can register and sign in to the application using Firebase
Authentication. Authentication provides the entry point into the
marketplace and prevents unauthenticated users from accessing the main
application areas.

#### 2. Browse Marketplace Listings

Users can browse available marketplace listings and view information about
items available for purchase. Listings can be filtered according to the
available categories.

#### 3. Listing Details

Users can open an individual listing to view additional information about
the item, including its title, description, price, condition and seller
information.

#### 4. Create Listing

Users can create marketplace listings by providing the required listing
information. Listing creation supports the application's offline-first
approach by saving listing information locally before attempting
synchronisation with the backend.

#### 5. Make an Offer

Users can submit an offer for an available listing. Offers are submitted
through the REST API and the application provides feedback based on the
success or failure of the request.

#### 6. Conversations and Chat

Users can access conversations associated with marketplace listings and
communicate with other users through the application's chat functionality.

#### 7. Favourite Listings

Users can save listings as favourites. Favourite information is stored
locally using Room and can also be synchronised with the backend.

#### 8. Offline Drafts

Listing information can be stored locally when immediate communication with
the backend is unavailable. Pending listings can subsequently be
synchronised when connectivity is restored.

#### 9. Settings

Users can access application settings and sign out of their account.

---

## Feature Scope

The following table summarises the main functionality included in the
Part 2 prototype.

| Feature | Part 2 Status | Description |
|---|---|---|
| User registration | Implemented | Users can create an account using Firebase Authentication. |
| User login | Implemented | Registered users can sign in to HiveMarket. |
| Browse listings | Implemented | Users can view available marketplace listings. |
| Listing categories | Implemented | Listings can be filtered using categories. |
| Listing details | Implemented | Users can view detailed listing and seller information. |
| Create listing | Implemented | Users can create and save marketplace listings. |
| Make an offer | Implemented | Users can submit offers through the REST API. |
| Conversations | Implemented | Users can access conversations associated with listings. |
| Chat | Implemented | Users can communicate through the application's chat functionality. |
| Favourite listings | Implemented | Users can save listings locally as favourites. |
| Offline drafts | Implemented | Listings can be stored locally before synchronisation. |
| Settings | Implemented | Users can manage available application settings and sign out. |
| Google Sign-In | Deferred | The Google sign-in option is present in the interface but is not implemented for this prototype milestone. |
| Messages/Profile bottom navigation | Deferred | These routes currently display a Coming Soon screen. |

---

## User-Defined Features

In addition to the core marketplace functionality, the prototype
incorporates user-defined features intended to improve the experience for
students.

### Offline-First Listing Creation

Listing information is saved locally before synchronisation with the
backend. This allows users to continue creating listings when network
connectivity is temporarily unavailable.

### Favourite Listings

Users can save listings locally so that items of interest can be accessed
more easily without repeatedly searching the marketplace.

### In-App Communication

The prototype includes conversations and chat functionality so that users
can communicate regarding marketplace listings within the application.

### Local Data Caching

Frequently accessed listing information is cached using Room. If the API
cannot be reached, the application can use locally stored information where
supported.

---

## Technology Stack

HiveMarket is implemented as a native Android application using Kotlin.
The prototype combines Android Jetpack components, Firebase services,
local persistence and REST API integration to support the application's
marketplace functionality.

### Core Technologies

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary programming language used to implement the Android application. |
| **Jetpack Compose** | Used to create the application's user interface using declarative UI components. |
| **Android SDK** | Provides the native Android platform and application development framework. |
| **Material 3** | Provides UI components and theming for the application's interface. |
| **Android Navigation** | Handles navigation between the application's screens. |
| **ViewModel** | Maintains UI-related state and separates presentation logic from the UI. |
| **Hilt** | Provides dependency injection throughout the application. |
| **Room** | Provides local SQLite-based persistence and caching. |
| **Retrofit** | Handles communication between the Android application and the REST API. |
| **Firebase Authentication** | Provides user registration and authentication functionality. |
| **Firebase services** | Supports authentication and the application's backend-related functionality. |
| **Kotlin Coroutines** | Supports asynchronous operations and background processing. |
| **WorkManager** | Supports deferred background synchronisation of pending offline data. |
| **JUnit** | Used to create and execute automated unit tests. |
| **MockK** | Used to mock dependencies when testing repository and ViewModel behaviour. |
| **Turbine** | Used to test Kotlin Flow emissions in ViewModel tests. |
| **Git & GitHub** | Used for version control, collaboration and repository management. |
| **GitHub Actions** | Provides continuous integration by automatically building and testing the project. |

---

## Application Development Technologies

### Kotlin

Kotlin is the primary programming language used throughout the HiveMarket
Android application. Kotlin provides support for null safety, coroutines,
data classes and concise Android development.

### Jetpack Compose

The user interface is implemented using Jetpack Compose. Compose allows the
application screens to be created using reusable composable functions
rather than traditional XML layout files.

The prototype uses Compose for screens including:

- Login
- Browse
- Listing Details
- Create Listing
- Chat
- Offline Drafts
- Settings
- Coming Soon screens

### Room Database

Room is used as the application's local persistence mechanism. Listing
information, pending offline data and favourite information can be stored
locally.

The local database supports the application's offline-first approach by
allowing selected data to remain available without an active network
connection.

### Retrofit

Retrofit is used as the HTTP client for communication between the Android
application and the HiveMarket REST API.

The API integration is accessed through the repository layer rather than
directly from the UI screens. This helps separate network communication
from presentation logic.

### Firebase Authentication

Firebase Authentication is used to provide user registration and login
functionality. The Login screen supports both registration and sign-in
states.

### Hilt

Hilt is used for dependency injection. It provides dependencies such as
the API service, Room database and repositories to the components that
require them.

### Kotlin Coroutines and Flow

Kotlin Coroutines are used for asynchronous operations such as API calls
and database operations. Kotlin Flow is used where application state and
locally stored data need to be observed reactively.

### WorkManager

WorkManager is used to support background processing for pending offline
listing synchronisation. When a listing cannot immediately be synchronised,
the application can retain the local data and attempt synchronisation
later.

---

## Testing Technologies

Automated unit testing is implemented using the following tools:

### JUnit

JUnit provides the testing framework used to define and execute individual
unit tests.

### MockK

MockK is used to create mock implementations of dependencies such as API
services and DAOs. This allows individual components to be tested without
requiring a live backend or database connection.

### Kotlin Coroutines Test

Coroutine testing utilities are used when testing suspend functions and
coroutine-based ViewModel or repository behaviour.

### Turbine

Turbine is used to test Kotlin Flow emissions and verify that ViewModels
produce the expected sequence of UI states.

---

## Development and Version Control

Git is used for source control and GitHub is used as the shared remote
repository. Changes are committed regularly during development using
descriptive commit messages.

GitHub Actions provides continuous integration. The CI workflow runs the
project's automated unit tests and builds a debug APK whenever changes are
pushed to the repository.


---

## System Architecture

HiveMarket follows a layered Android application architecture based on the
**Model–View–ViewModel (MVVM)** pattern and the **Repository Pattern**.

The architecture separates the user interface, application state,
business/data access logic, local persistence and remote API communication.
This separation makes the application easier to maintain and allows
individual components to be tested independently.

### Architectural Layers

The main layers of the HiveMarket application are:

1. **Presentation Layer**
2. **ViewModel Layer**
3. **Repository Layer**
4. **Data Sources**
5. **Backend and External Services**

---

### 1. Presentation Layer

The presentation layer contains the Jetpack Compose screens used by the
user.

Examples include:

- `LoginScreen`
- `BrowseScreen`
- `ListingDetailScreen`
- `CreateListingScreen`
- `ChatScreen`
- `OfflineDraftsScreen`
- `SettingsScreen`

The screens are responsible primarily for displaying application state and
collecting user input. They delegate application operations to their
corresponding ViewModels rather than directly communicating with the
database or REST API.

---

### 2. ViewModel Layer

ViewModels manage UI-related state and coordinate operations required by
the screens.

The project contains ViewModels for areas such as:

- Login
- Browse
- Create Listing
- Listing Detail
- Chat
- Offline Drafts
- Settings

ViewModels use Kotlin Coroutines and Flow where appropriate to perform
asynchronous operations and expose state to the Compose UI.

This separation prevents the UI from containing the application's main
business and data-access logic.

---

### 3. Repository Layer

The repository layer provides an abstraction between the ViewModels and the
underlying data sources.

The main repository used for marketplace functionality is:

```text
ListingRepository

---

## REST API Integration

HiveMarket communicates with a RESTful backend API to support marketplace
operations that require remote data. The Android application uses Retrofit
to define and execute HTTP requests while the repository layer controls how
the returned data is used by the application.

The API integration is separated from the Compose UI. Screens communicate
with ViewModels, ViewModels communicate with repositories, and the
repository communicates with the Retrofit API service.

### API Communication Flow

```text
User
  │
  ▼
Compose Screen
  │
  ▼
ViewModel
  │
  ▼
ListingRepository
  │
  ▼
HiveMarketApi
  │
  ▼
REST API

---

## Offline-First Design and Data Persistence

HiveMarket uses an offline-first approach for selected marketplace
functionality. The purpose of this approach is to reduce the application's
dependence on continuous network connectivity and allow important local
operations to continue when the REST API is temporarily unavailable.

The application uses **Room Database** for local persistence and the
`ListingRepository` to coordinate information between the local database
and the remote API.

### Offline-First Architecture

The offline-first data flow can be represented as:

```text
                    ┌──────────────────┐
                    │   Compose UI     │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │    ViewModel     │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │ ListingRepository│
                    └───────┬───┬──────┘
                            │   │
                  Local     │   │    Remote
                            │   │
                            ▼   ▼
                    ┌───────┐ ┌────────────┐
                    │ Room  │ │ Retrofit   │
                    │  DB   │ │    API     │
                    └───────┘ └────────────┘
