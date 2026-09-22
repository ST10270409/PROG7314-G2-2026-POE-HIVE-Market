# AI Sub-Report

Generative AI (Claude, Anthropic) was used throughout Part 2 development as a
pair-programming and debugging tool, not as an autonomous code generator.
Specific uses:

**Debugging build/environment errors.** The bulk of AI assistance was
resolving a chain of Gradle/AGP/Kotlin version incompatibilities (AGP 8.x vs
Gradle 9.x, Kotlin metadata format mismatches in Dagger/Hilt and Room,
missing `testOptions` configuration causing unit tests to crash on unmocked
`android.util.Log` calls). Each fix was diagnosed from the actual compiler
or CI error text, not guessed.

**REST API implementation.** The ASP.NET Core Web API (Controllers, EF Core
DbContext, entity models, Firebase JWT validation) was scaffolded with AI
assistance to match the Android client's existing Retrofit interface
field-for-field, then debugged through several real deployment issues on
Render: a missing `[Key]` attribute on EF Core entities, and Npgsql
rejecting Render's URL-style `DATABASE_URL` (which needed conversion to
keyword-value format).

**Structuring and fixing Kotlin source files.** AI helped scaffold new
screens (Listing Detail, Create Listing, Chat, Offline Drafts, Settings)
following the existing MVVM + Repository pattern already established in the
codebase, and diagnosed several real bugs introduced during merges between
group members' branches: missing imports in `ListingRepository.kt`, an
incorrect import path for `PasswordVisualTransformation`, and a duplicate-
listing bug caused by the local Room cache not rewriting its primary key
after a listing synced to the server.

**Google Sign-In integration.** AI identified a functional bug in an
early implementation (a missing `.requestIdToken(...)` call that would have
made sign-in silently fail after account selection) and walked through the
Firebase Console configuration steps (enabling the Google provider,
registering a SHA-1 debug fingerprint) required to make it work.

**CI/CD troubleshooting.** When GitHub Actions began failing after the
Google Sign-In changes, AI traced the failure to the CI workflow using a
placeholder `google-services.json` (real credentials are git-ignored) that
lacked the OAuth client entry the new code required, and fixed the
placeholder file to include the same structure with dummy values.

**What AI did not do:** design decisions (which 3 features to build, UI
layout, database schema), writing the app from scratch, or making
architectural choices — those were made by the group, based on the Part 1
design documents. AI's role was implementation support and error diagnosis
once decisions were already made.

All AI-assisted code was reviewed, tested against real device/emulator
behaviour, and verified via a passing CI pipeline before being merged.
