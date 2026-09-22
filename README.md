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
