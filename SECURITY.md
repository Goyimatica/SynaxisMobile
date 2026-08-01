# Security Policy

## Reporting a Vulnerability

This is a personal, single-owner project. If you believe you have found a
security issue in Synaxis, please report it privately by opening an issue
on the repository or contacting the maintainer directly — **do not** open a
public issue that describes a live exploit.

We aim to acknowledge reports within 7 days.

---

## Security Audit — Findings & Recommendations

The audit below was conducted on **v9.1** (August 2026). All findings were
addressed in **1.0.1** (August 2026); each row notes how.

| # | Severity | Finding | V10 status |
|---|----------|---------|------------|
| 1 | **High** | Release builds silently fell back to the **debug signing key** when no release key was configured. The default Android debug keystore is a well-known, publicly documented key (password `android`); anyone holding it could sign a malicious "update" that installs over the app on sideloaded devices. | **Fixed.** `packageRelease` / `bundleRelease` / `assembleRelease` now fail fast with a clear message when no key is configured (`keystore.properties` or `SYNAXIS_STORE_*` / `SYNAXIS_KEY_*` env vars). The debug key can no longer sign a release. |
| 2 | Medium | R8 minification and obfuscation were disabled. The release APK could be decompiled trivially. | **Fixed.** `isMinifyEnabled` and `isShrinkResources` are now `true` for release, with explicit keep rules in `proguard-rules.pro` for Coil 3's service-loader network fetcher (the reason R8 was previously avoided). |
| 3 | Medium | Privacy / logging: `WikiRepo` and `Fonts` logged full request URLs, article titles, and search terms at `Log.i`. Anything readable on logcat is exposed to anyone with USB debugging access — this leaked the reading/search history. | **Fixed.** All URL and title logging removed from `WikiRepo` and `Fonts`. |
| 4 | Low–Med | Fonts (input validation): `Fonts.install()` derived folder names from arbitrary user input and URL filenames without rejecting path separators or `..`; a name like `..` could resolve outside `filesDir/fonts`. Downloaded files were validated only by size, not content. | **Fixed.** `safeName()` rejects separators, dots, `..`, control characters, and overlong names; direct downloads are **https-only**; downloaded bytes must match TrueType / OpenType / TTC magic numbers before saving. |
| 5 | Low | Transport security: HTTPS everywhere, but no explicit network security config or certificate pinning. | **Fixed.** Added `res/xml/network_security_config.xml` refusing cleartext in every build type, referenced from the manifest. Certificate pinning remains deliberately absent (public wikis rotate certificates; pinning would break the app). |
| 6 | Low | Backup / data at rest: broad cloud-backup rules backed up the whole library as plaintext. | **Partially mitigated.** Downloaded lives and icons are now excluded from cloud backup (they can be re-fetched); the user-facing library (bookmarks, highlights, notes, settings) remains backed up by design. Data at rest is not encrypted — the library is low-sensitivity reading history, and full encryption would break cross-device restore. |
| 7 | Low | Randomness: highlight keys used `Math.random()`, which is not cryptographically secure. | **Fixed.** `Mark.newKey()` now derives keys from `UUID.randomUUID()`. |
| 8 | Low | CI supply chain: actions pinned to version tags rather than commit SHAs; the release keystore was deleted with `rm -f`. | **Fixed.** All GitHub Actions pinned to verified commit SHAs (checked against upstream repos on 2026-08-01); the keystore is now removed with `shred -u` (with `rm -f` fallback). |
| 9 | Low | Release checks: `checkReleaseBuilds = false` meant fatal-severity lint findings never blocked a release. | **Fixed.** `checkReleaseBuilds = true`; findings are reported on every build while `abortOnError = false` keeps the personal-app workflow intact. |
| 10 | Info | Rate limiting: the sync fetched up to 12 articles in parallel with no backoff; sustained use could get the device/IP throttled by Wikimedia. | **Fixed.** `WikiRepo.get()` now retries 429 and 5xx responses (and network errors) up to three times with a short pause, so a single refused request no longer costs a saint its picture. |

### What is done well (verified)

- **No WebView and no JavaScript** — wiki content is rendered as plain text,
  so there is no script-injection or `addJavascriptInterface` attack surface.
- **Minimal component surface** — only the launcher `MainActivity` is
  exported; there are no exported services, receivers, or providers.
- **No secrets in the repository** — `keystore.properties`, `*.jks` and
  `keystore.b64` are gitignored and absent; the CI signing key arrives via
  GitHub secrets and is shredded after the build.
- **HTTPS everywhere** — every hardcoded endpoint (`orthodoxwiki.org`,
  `en.wikipedia.org`, `commons.wikimedia.org`, `fonts.googleapis.com`) is
  `https://`; cleartext is now also refused by explicit policy.
- **Proper User-Agent** — a descriptive agent is set on every request,
  satisfying Wikimedia's API policy.
- **Defensive storage** — the library JSON is written atomically
  (tmp + rename), and a corrupt library file is quarantined rather than
  crashing the app.
- **No dangerous permissions** — only `INTERNET` and `ACCESS_NETWORK_STATE`.

---

## 1.0.1 offline change

Since 1.0.1, every life is fetched from **both** OrthodoxWiki and Wikipedia
and kept in **permanent app storage** (`filesDir`, not the OS-evictable
cache). The reader offers a source toggle wherever both wikis have an
article, and everything downloaded is available offline. The downloaded
lives and icons are excluded from cloud backup (they can always be fetched
again); device-to-device transfer still carries them.

---

## Scope

This policy applies to the `SynaxisMobile` repository and all artifacts it
produces. Third-party content rendered by the app (articles, images, fonts)
is governed by its own licenses, not by this policy.
