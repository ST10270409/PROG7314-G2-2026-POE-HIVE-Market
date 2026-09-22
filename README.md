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
