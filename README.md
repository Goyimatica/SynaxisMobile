<div align="center">

# ☦ Synaxis — Lives of the Saints

**An Orthodox reader for Android.**

Today's commemorations, the lives of the saints, the church calendar, and a
place to keep what moves you — all offline-first, beautifully typeset, and
yours.

</div>

---

## About

Synaxis is a private, ad-free Android app for reading the lives of the
saints and following the rhythm of the Orthodox liturgical year. It is
built around one idea: *every saint's life should open fast and read like a
book.*

- **Today** — the day's commemorations, feasts and fasts, and a daily
  saying that stays the same all day.
- **Lives** — the full index of the saints, browsable by era, jurisdiction,
  and tag, or by an A–Z list.
- **Calendar** — the church year on either calendar (Julian / Old and
  Revised Julian / New), with feasts, fasts, and the saints of each day.
- **Search** — the people *and* the subjects — feasts, fasts, and topics —
  so "dormition" finds the feast, not a blank page.
- **Library** — bookmarks, reading history with saved progress, highlights,
  and margin notes.
- **Reader** — a carefully typeset reading experience: your choice of
  palette, typeface, size, leading, weight, justification, and drop caps.
  Select any passage to highlight it, note it, or copy it. Where both wikis
  have an article, switch between them with a tap.

Article text is fetched from **both** OrthodoxWiki and Wikipedia and
portraits from Wikimedia Commons, then stored in **permanent app storage**
— not the OS-evictable cache — so everything you've synced is yours
offline. A short sync at launch tops up only what is missing or out of
date.

## Features

- 🗓 **Both calendars** — Julian (Old Style) and Revised Julian (New Style),
  switchable in one tap.
- 📖 **Real articles** — sourced from OrthodoxWiki and Wikipedia, cleaned of
  citation apparatus, and served as prose.
- 🖼 **Honest portraits** — every picture is verified against the subject's
  name before it is shown; a subject without a trustworthy image goes
  without one.
- 🎨 **Four palettes** — Night, Midnight, Sepia, and Parchment.
- 🔤 **Fonts your way** — three built-in families (Cormorant, Noto Serif,
  Outfit) plus any Google Font you install by name or link.
- 💾 **Offline-first** — every life and portrait you've synced is stored
  permanently on the device (not in the OS-wipeable cache).
- 📚 **Two sources, one reader** — OrthodoxWiki and Wikipedia articles are
  both kept; switch between them wherever both exist.
- 🔖 **Bookmarks, highlights & notes** — synced nowhere, stored locally.
- 🕯 **A daily saying for every day** — 366 sayings from the Fathers, keyed
  to your calendar reckoning, so each day of the church year has its own.
- ⚡ **Fast** — 120 Hz motion, cached layouts, one shared HTTP connection
  pool, a baseline profile, and parallel article fetches.
- 📲 **Background downloads** — the launch sync runs as a foreground
  service with a progress notification, so a stream finishes even when you
  leave the app.
- 🔒 **Hardened** — release builds require a real signing key (never the
  public debug key), R8 is on, cleartext is refused, and downloads are
  validated before they are saved.

## Tech stack

| Layer        | Choice                                                        |
| ------------ | ------------------------------------------------------------- |
| Language     | Kotlin 2.3                                                    |
| UI           | Jetpack Compose (Material 3, BOM 2025.11)                     |
| Architecture | Single-module, singleton repositories + Compose state         |
| Networking   | OkHttp 4 (shared client, one connection pool)                 |
| Images       | Coil 3 (memory + disk cache)                                  |
| Storage      | SharedPreferences + JSON files (atomic writes)                |
| Async        | Kotlin Coroutines                                             |
| Build        | Gradle (AGP 9) with the version catalog, configuration cache  |
| CI           | GitHub Actions — unit tests, debug APK, signed release APK    |
| Min SDK      | 24 · Target SDK 37                                            |

## Project structure

```
app/src/main/java/com/goyimatica/synaxismobile/
├── core/          # Church-calendar math: Pascha, feasts, fasts
├── data/          # Repositories: saints, quotes, wiki articles,
│                  #   images, fonts, library store, sync gate
├── ui/
│   ├── theme/     # Colors, type system, palettes
│   ├── components/# Shared chrome: cards, sheets, portrait, sync dialog
│   ├── screens/   # Today, Lives, Calendar, Search, Library, Saint,
│   │              #   Settings, Day facts, Font section
│   └── reader/    # The reader: text layout, selection, marks
├── App.kt         # Composition root
├── Nav.kt         # Routes & tab model
└── MainActivity.kt
```

The saint index lives in `app/src/main/assets/saints.json` (harvested and
verified by the scripts in `tools/`); the daily sayings live in
`app/src/main/assets/quotes.json`.

## Getting started

### Prerequisites

- **JDK 17**
- **Android SDK** — set it in `local.properties`:

  ```properties
  sdk.dir=/path/to/android-sdk
  ```

### Build

```bash
# Debug APK
./gradlew assembleDebug

# Unit tests
./gradlew test

# Install on a connected device
./gradlew installDebug
```

The debug APK is produced under `app/build/outputs/apk/debug/`.

### Release signing

Release builds are signed with your own key, loaded from a
`keystore.properties` file (never committed) or from environment variables:

```properties
storeFile=./synaxis-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

If no key is configured, the build falls back to the debug key — fine for
personal installs, but **never publish a store build signed with the debug
key**.

## CI/CD

`.github/workflows/android.yml` runs on every push and pull request:

- **Debug job** — unit tests + debug APK, uploaded as a build artifact.
- **Release job** (on `v*` tags) — decodes the signing key from repository
  secrets, builds a signed release APK **and AAB**, names them after the
  tag, deletes the key, uploads the artifacts, and publishes a GitHub
  release (the AAB is what you upload to Google Play).

Versioning is tag-driven: `VERSION_NAME` comes from the tag (`v1.0.1` →
`1.0.1`) and `VERSION_CODE` from the run number.

## Data & attribution

Synaxis reads freely available content from:

- [OrthodoxWiki](https://orthodoxwiki.org)
- [Wikipedia](https://en.wikipedia.org) and [Wikimedia Commons](https://commons.wikimedia.org)
- [Google Fonts](https://fonts.google.com) (fonts you choose to install)

All article text and images remain the property of their respective
authors and are reproduced under the terms of their licenses. Synaxis sets
a descriptive User-Agent on every request, as Wikimedia's API policy
requires, and respects the letter and spirit of it: modest parallelism,
on-device caching, and no hot-linking abuse.

## Privacy

- No accounts, no analytics, no ads, no trackers.
- Everything you read, bookmark, highlight, or note stays on your device
  (and in your device backup, unless you disable it in Android's settings).
- The only network traffic is fetching public articles, portraits, and
  fonts you choose to install.

See [SECURITY.md](SECURITY.md) for the security posture of the project.

## Version history

| Version | Highlights |
| ------- | ---------- |
| 1.0.1   | Both wikis stored permanently; source toggle; security hardening (signing, R8, cleartext, font validation, retries) |
| 9.1     | Lives is people; Search is everything; real articles and honest pictures for subjects |
| 9       | Harvest verified against categories; subjects reviewed |
| 8       | Readable feasts, fasts and subjects; one type system |
| 7       | Portraits everywhere; faster sync; a real homepage |
| 6       | Pictures, frame rate, signed releases |

## License

**Proprietary.** Copyright © 2026 GoyDevv, Goyimatica. All rights reserved.

This project is not open source. No part of it — code, assets, or
documentation — may be copied, modified, distributed, or used without the
prior written permission of the copyright holder. See [LICENSE](LICENSE)
for the full terms.
