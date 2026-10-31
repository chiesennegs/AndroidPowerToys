# Public-source release validation

Date: 2026-10-31.

- Exported exactly the reviewed Git index into a clean directory without local.properties, build outputs, device data or signing keys. With JDK 25 and Android SDK Platform 37.0 / Build Tools 36.0.0, unit tests, Android lint and the debug APK build completed successfully from that export. The workflow uses the same SDK/JDK choices and pinned action revisions.
- The clean export passed **36 app tests and 7 transport tests**. The corrected [hosted build](https://github.com/chiesennegs/AndroidPowerToys/actions/runs/41178053917) also passed. Its first attempt requested an obsolete SDK `tools` package through a setup default; the workflow now uses the runner's current SDK manager directly and installs the exact required platform/build-tools packages.
- Staged-source scan excluded local configuration, credentials, signing material, device identifiers, private paths, capture files and snapshot/history data. The release retains required source/artwork/library notices. Original application code has an Apache-2.0 license; bundled components retain their terms.
- The public README covers capabilities, installation, permissions, wireless/manual setup, reviewed edits, state semantics, restore/history, snapshots, privacy, limitations and source builds. Its Windows PowerToys comparison cites Microsoft's official overview and makes no feature-parity or affiliation claim.

---

# v1.6.1 pinned search and scrolling controls

Date: 2026-10-31 (America/Denver), Pixel 10 Pro XL / Android 17.

- Circular app icon/background, persistent title spacing, compact pinned search and a conditional Go to top action are installed in v1.6.1 (version code 11). The compact search keeps a full touch target, accessible label, search IME action and explicit clear button. Query changes reset the current list to its first matches; they do not change system settings.
- **Both read-only device regression checks passed.** The extended table-scroll check verifies search remains displayed deep in the list, the top action appears there and disappears after returning to index zero, other tables preserve their saved depths, and search/clear works from a scrolled state. An initial test assertion included the empty field's visible placeholder; reading EditableText instead verifies the actual cleared value correctly.
- The smoke test verifies search height **48 dp**, title/search separation **12 dp**, and normal browse/setup/defaults/About/history behavior, including existing permissions remaining unchanged. Dark/light branding and the expanded permission refresh still pass.
- Overall list/browsing region is **1821/2404 = 75.749%**. The pinned search plus its spacing occupies **187 px**; unobscured scrolling rows occupy **1634/2404 = 67.970%**. The fixed search deliberately consumes part of the browsing region; it is not represented as scrolling row space. Measurements are for the tested full-screen portrait/default-font configuration, keyboard closed.
- Local screenshots at the top, after scrolling and in light mode were visually inspected. Search has an opaque background so passing rows do not show through; extra bottom content padding lets the last row clear the floating control. Captures remain excluded from deliverables.
- Debug/test APK and lint builds passed: zero errors, 15 existing warnings. APK installed as an update preserving baseline, history, pairing and permissions. No permission grant/revoke or settings-table mutation occurred. Backend safety logic is unchanged.

---

# v1.6 interface refresh validation

Date: 2026-10-31 (America/Denver), Pixel 10 Pro XL / Android 17.

- Shared app-icon masthead and 26 sp Outfit title now appear on every main page, including Tables. The title/table area is a touch-sized table picker, retaining scroll-position restoration. Outfit is bundled locally and licensed under SIL OFL 1.1; source, copyright and SHA256 are recorded in docs/BRANDING.md and bundled notices.
- About replaces the former permissions tab. App/support and appearance lead the page; editing setup is available without exposing raw commands immediately. Privacy, recovery and manual permission information are expandable. Stable item keys preserve expanded sections across permission refresh/loading feedback. The retired helper-service evaluation and its references were removed from application and project documentation.
- Navigation uses original icon/label pairs, cards have softer corners and clearer spacing, setting names use the UI typeface, and actual values retain a distinct selectable technical typeface. Unassessed effects use neutral labels, while high-impact/recovery risks remain prominent and review requirements are unchanged. History and snapshot introductions are simpler; operation feedback and loading remain visible inside their scrolling pages.
- **Both read-only UI regression checks passed** on device: independent table depth/new-table top restoration and the expanded browsing/setup/defaults/About/history smoke check. The smoke check also inspects dark/light branding, verifies the app icon, opens manual permission details without executing commands, and refreshes detected access while retaining the expanded section. The smoke check was repeated after final feedback/disclosure refinements and passed.
- Settings-list viewport: **1821/2404 = 75.749%** in the tested full-screen portrait configuration. Custom font metrics make the shared header slightly taller than the former compact title, while retaining at least 75% scrollable space. Large user font scales and split-screen sizes are not covered by this ratio claim.
- Captured Tables and About in dark/light mode and visually inspected the app icon, title fit, custom font, readable values, labels, navigation and logo/contact artwork. Captures are local-only and excluded from deliverables. External website/email links were not opened.
- Debug APK v1.6 (version code 10), test APK and lint built successfully. Lint: zero errors and 15 existing dependency/KTX/library-bytecode warnings. Installed as an update preserving baseline, history, pairing identity and both permission states. No editing-mode enable, setting mutation, permission grant/revoke or automatic network action was performed. Backend logic is unchanged; its prior unit-test record remains applicable.

---

# v1.5.1 MooseAI branding validation

Date: 2026-10-30 (America/Denver), Pixel 10 Pro XL / Android 17.

- Reused OpenLifeSpan's MooseAI wordmark byte-for-byte (SHA256 recorded in docs/BRANDING.md), with its Apache-2.0 license and attribution. OpenLifeSpan files were read only. PowerToys keeps its app name, package and distinct sliders launcher icon, now in MooseAI colors.
- Debug APK v1.5.1 (version code 9), test APK and lint built successfully. Lint: zero errors, 15 warnings; the two additional warnings suggest the optional KTX URI convenience function for website/email intents.
- **Both read-only UI regression checks passed**: the existing smoke check now inspects the publisher/logo/contact card in dark and light themes without opening its external links, and the independent table-scroll check passes. The initial branding check stopped after scrolling only the About title into view; scrolling the complete card into view resolved that test issue. No product behavior was changed in response.
- Actual dark/light screen captures were visually inspected locally. The logo is readable on its consistent navy backplate; publisher text and support links are visible. Status/navigation icons follow the selected app theme. Captures are excluded from source/APK distribution artifacts.
- Core foreground/accent pairs were numerically checked: dark text 16.44:1; dark accent 13.40:1; light text 16.61:1; light accent 6.04:1; light button text 6.49:1. This is a focused contrast check, not a full accessibility audit.
- The settings-list viewport remains **1825/2404 = 75.915%** in the tested full-screen configuration. Branding uses the existing toolbar height; the larger logo is confined to the scrollable About page.
- Installed as an update preserving baseline, history, pairing identity and both existing permission states. No setting-value write, grant/revoke, email send or website visit was performed. Website/mail intents were inspected, not opened against external apps. The existing backend unit-test record from v1.5 remains applicable; backend logic was unchanged.

---

# v1.5 descriptions, navigation and Performance planning

Date: 2026-10-30 (America/Denver), Pixel 10 Pro XL / Android 17.

- **36 app unit tests passed**: existing 33 plus three catalogue/safety tests. New checks cover exact table/key lookup, pinned source links, absent unknown descriptions, no inferred defaults or risk downgrade for source excerpts, curated/default precedence and verified audio-family bounds. A known meaning with unassessed edit effects no longer displays a contradictory Description unavailable warning.
- **Four read-only device checks passed**: provider/snapshot round trip, actual description coverage, independent table scroll/navigation behavior, and the existing browsing/setup/defaults/access/history smoke check. The initial smoke run matched both a setting row and its search field; narrowing the selector to a non-input row resolved the test ambiguity. The other checks passed on that first run. After the last three SystemUI entries were added, coverage was measured again successfully on the installed final build.
- Coverage for this app's provider-visible rows: **System 66/73; Secure 132/258; Global 184/300**. The catalogue contains 1,306 exact-key Android 17 source excerpts plus curated descriptions, 56 explicitly supported audio stream/output combinations, and three SystemUI consumer descriptions. Counts are not all Android configuration and can change with build/user/permissions. Remaining private/unsupported meanings are not guessed.
- The device scroll test scrolls System, visits Secure and Global for the first time (each starts at index zero), gives each a different depth, and verifies restoring all three independent positions. Full-screen settings-list viewport remains **1825/2404 = 75.915%**; the ratio assertion applies outside split-screen mode.
- Diagnose, its supplied incident example, dedicated diagnostic state and special flag reads were removed. Filtering keys remain ordinary browsable settings with generic sourced explanations. The test asserts the Diagnose navigation entry is absent.
- Debug APK v1.5 (version code 8), test APK and lint build succeeded; lint reports zero errors and 13 existing warnings. Updated install preserved local baseline, history and pairing identity. Both existing permission states were preserved; no edit mode enable, grant/revoke or settings-table write occurred. Backend/wireless transport behavior is unchanged from v1.4.2.
- Generated source documentation is Apache-2.0 attributed and reproducible from the pinned, hashed source reference. Curated explanations override source fallbacks; raw source prose does not imply safe editing or a Pixel factory default. Full bounded descriptions and source links are in the detail view; list previews are capped at three lines.
- Performance is **planning only**, in docs/PERFORMANCE-PLAN.md. Research covers primary Android implementation/documentation plus explicitly labelled community hypotheses. No Performance tab, preset, background measurement, permission or automatic change was added.

---

# v1.4.2 paired-connection repair

- Inspected the user's split-screen setup on the Pixel. Pairing and the command TLS handshake succeeded; the bundled shell stream mishandled a normal remote close, causing the connection check to report IOException.
- Four new local transport regression tests first failed on the old code, reproducing EOF as an exception, an end-of-response wait, buffered-byte loss after remote close, and incorrect unsigned/zero-length InputStream behavior. All four pass after the repair. The 33 existing app unit tests pass as well.
- An explicit opt-in live connection test reproduced the same failure against the installed v1.4.1 app, then passed against v1.4.2 using the existing pairing identity. It performs three own-phone read-only connection checks, covering initial command-server pin capture and two pinned reconnects, and asserts the permission state is unchanged. The two isolated cryptography tests also passed again on the Pixel.
- Error messages now identify pairing, TLS connection, shell identity, current-user lookup or installed-UID validation as the failing stage. Connection failures no longer instruct the user to keep the pairing dialog open.
- Debug APK and test APK built; lint completed with zero errors and 13 warnings. Installed as an update, preserving the starting baseline, history and pairing identity. No grant/revoke or settings-table write was performed. The user successfully used split screen during pairing; no separate automated split-screen launch assertion was made.
- Run the opt-in connection test only with explicit authorization, an existing app pairing and the current connection port: `adb shell am instrument -w -r -e class dev.androidpowertoys.app.WirelessConnectionTest -e wirelessConnectionPort PORT dev.androidpowertoys.app.test/androidx.test.runner.AndroidJUnitRunner`. Without the port argument this test is skipped. It never pairs, grants, revokes or edits setting values.

---

# v1.4.1 split-screen setup helper

- Added **Open Settings in split screen** to wireless setup on Android 12L+, using the public LAUNCH_ADJACENT + NEW_TASK intent flags. MainActivity explicitly permits resizing. The existing normal Developer options button remains available.
- Added in-app Pixel recent-apps instructions and three-button navigation fallback. The helper only opens Developer options; it does not enable debugging, pair, connect or grant permissions.
- Debug APK built and lint completed with zero errors. Existing wireless/backend code is unchanged; v1.4 tests below remain applicable. Actual split-screen launch behavior has not yet been verified on the Pixel; Android controls whether the request is honored.

---

# v1.4 experimental wireless setup validation

Date: 2026-10-30 (America/Denver), Pixel 10 Pro XL / Android 17.

- Debug APK v1.4 (version code 5), native dependency modules and test APK built successfully. APK signature verification, ZIP 16 KB alignment, and 64-bit native ELF load alignment checks passed.
- **33 app unit tests passed**, including five new wireless policy tests: bounded numeric ports, exact six-digit code, own-device numeric addresses only, fixed own-package grant/revoke commands with nonnegative user IDs, and strict installed-package UID response parsing.
- **Four device instrumentation tests passed**. Existing table reads, baseline persistence, settings viewport >=75%, navigation and editing guide checks continue. Wireless UI checks verify grant/revoke stay disabled before connection verification, and returning to editing setup opens no network connection or permission grant.
- Native SPAKE2 produced matching keys for the same test password and different keys for a wrong password on the actual Pixel. Conscrypt TLS 1.3 native provider loaded. An isolated test RSA identity survived encrypted-storage reload and signed/verified correctly; pairing proof and command-server pin were checked as separate states; test-owned identity/Keystore entries were removed without touching the real pairing identity.
- System Modify system settings access was already enabled when testing started. Initial old tests failed their assumption that this permission was absent; they were updated to preserve and assert the existing state. Secure write permission remained absent. No System permission was revoked or used for a settings-table mutation.
- Lint: zero errors, 13 warnings. Warnings include dependency update suggestions, KTX convenience suggestions, and an unused general TLS trust-manager implementation inside Bouncy Castle's dependency bytecode. SPAKE2 authenticates pairing. Android uses a separate ephemeral pairing certificate, so the first own-device read-only connection captures the command-server pin using trust on first use; subsequent permission connections require that saved pin. This first connection is not cryptographically bound to the pairing certificate. No independent security audit is claimed.
- An upstream pure-Java SPAKE2 experiment failed four of its own shared-key tests and contained cryptographic debug output; it was removed from the project/APK. The Android native path is used instead. Its exact sources, prebuilt JNI hash, licenses and rebuild instructions are included under third_party/. Upstream Android-native test sources are retained as reference; actual key-agreement tests run on device rather than JVM JNI emulation.

**Pending interactive validation:** real Android debugging pairing, connection identity checks and the explicit WRITE_SECURE_SETTINGS grant/revoke have not yet been confirmed on-device. The user has been asked to try Pair and Check connection only, without a grant. A separate explicit grant confirmation is required; no settings-table writes were made during development. Root/other-device connections are intentionally rejected. Existing display-trial live-write limitations still apply.

Networking permission is a deliberate v1.4 change: INTERNET for sockets and explicit Android 17 ACCESS_LOCAL_NETWORK for the opt-in setup. No automatic discovery, launch-time connection, background reconnect, server upload, telemetry or general terminal was added. No pairing credentials, codes, device screenshots or local baseline/history are included in the deliverables.

---

# v1.3 guided editing validation

Date: 2026-10-30 (America/Denver), Pixel 10 Pro XL / Android 17.

- Debug APK v1.3 (version code 4) and test APK built; lint completed with zero errors.
- Both read-only device instrumentation tests passed. The expanded UI test opens Enable editing, checks System access instructions and the disabled enable button, selects Secure, copies the grant command and verifies the clipboard contains the exact package-specific command, selects Global, verifies its enable button is disabled, then cancels and verifies read-only mode and absent write permissions. It also continues the existing defaults/inspection/navigation/baseline checks.
- No permission screen approval, ADB grant/revoke or settings-table write was performed. Actual granted-permission continuation is not device-tested; enabling is gated by the existing live permission state and safety prerequisites.
- The existing >=75% viewport assertion still passes. The setup is a scrolling dialog and does not add fixed content to the settings-list screen.
- Updated with `adb install -r` to preserve the starting baseline. The guide capture was visually inspected and excluded from delivery archives. The test copied the displayed grant command to the phone clipboard but did not execute it.

---

# v1.2 compact-layout validation

Date: 2026-10-30 (America/Denver). Pixel 10 Pro XL, portrait, 1080 × 2404 pixels, current density override 440, keyboard closed.

- The measured scrollable settings viewport is **1825 pixels / 2404 pixels = 75.915%** of the full app window. A device UI assertion requires at least 75%. The first iteration measured 73%; compact 56 dp bottom navigation brought the final layout above the target while retaining 48 dp navigation touch targets.
- Both read-only instrumentation tests passed after updating selectors for the table dropdown and actions menu. Defaults, search, setting inspection, diagnostic/access/history navigation and baseline persistence remain exercised. No device settings or permissions were changed.
- Debug APK v1.2 (version code 3) and test APK built successfully; Android lint reports zero errors. Existing dependency/KTX suggestions remain. Core mutation and rollback logic were outside this layout change; the v1.1 unit test results below remain the relevant logic validation.
- Visually inspected the actual screen capture: compact toolbar/read-only indicator, table dropdown, action menu button, scrolling search/access summary, readable setting cards and compact navigation. Capture excluded from delivery archives. The ratio is measured for this portrait configuration, not for landscape, an open IME or arbitrary font/display scaling.
- APK updates used `adb install -r`, preserving the immutable first-capture baseline.

---

# v1.1 safety/defaults validation

Date: 2026-10-30 (America/Denver). Device: the same authorized Pixel 10 Pro XL, Android 17 / API 37.

- **28 JVM tests passed**: the original 11 core tests plus 17 safety tests. New tests cover baseline JSON persistence and unknown-vs-absent/empty/null/zero, evidence-based default comparison and numeric formatting, durable trial records before provider writes, exact timeout boundary, timely keep, expired keep refusal, concurrent-writer conflicts, failed rollback with retry, read failure, process-interruption recovery, interruption before mutation, failed trial writes, overlapping-trial refusal, bounded inputs and restorable originals, blocked/unknown risk classification, unknown filtering defaults, failed pending-record persistence preventing mutation, and explicit manual-recovery handoff without a new write.
- **Android lint: zero errors, 10 warnings**, still dependency update suggestions and optional KTX URI syntax.
- **Debug APK and test APK built successfully**, compile/target API 37, application v1.1 / version code 2.
- **Both device instrumentation tests passed** using `adb install -r` and `adb shell am instrument`, preserving app data and the first-capture baseline. They asserted WRITE_SECURE_SETTINGS and Modify system settings were absent; enumerated all three tables; checked exact/absent reads; round-tripped snapshots; verified baseline persistence; searched the new defaults screen; inspected brightness risk/trial explanations; navigated diagnostics/access/history; and confirmed both filtering flags were unchanged.
- **Foreground service readiness passed on Android 17**: the UI test starts the actual short foreground service with a probe action and no pending trial, verifies “No pending trial,” and makes no provider mutation. This validates foreground startup independently of write access, not actual display rollback.
- A screenshot of the actual detail dialog was captured during the passing test for visual inspection. It is not included in the source archive or uploaded.

**No settings-table mutations, write grants/revocations or actual display trials were performed on the Pixel.** Real timed rollback, notification permission approval, permission-loss recovery, process-death recovery and reboot behavior were not tested on-device. Those recovery state transitions were simulated with fake backends, deterministic clocks and durable-store interfaces in unit tests. Real rollback needs a separately authorized, controlled device test before treating it as device-validated. Force-stop/reboot remain documented limitations, not tested guarantees.

The earlier v1.0 test record below remains historical. Direct ADB tests are now documented to avoid uninstalling the app and deleting its immutable starting baseline. Two intermediate v1.1 UI attempts needed test-selector refinements (the query field and row shared text; the background row and dialog shared a risk label). Final assertions are scoped to the setting row and dialog, and the full suite passed.

---

# v1.0 validation record

Device: user's connected and authorized Pixel 10 Pro XL (`mustang`), Android 17 / API 37, foreground Android user 0. Date: 2026-10-30 (America/Denver).

## Passed

- Debug APK assembled with AGP 9.2.1, Gradle 9.4.1, Android SDK 37.0 revision 2 / Build Tools 36.0.0 and Android Studio JBR 25.
- 11 JUnit tests: absent/empty/zero/null separation and JSON state round-trip; added/removed/changed snapshot comparisons; snapshot format round-trip; rejecting different users, coverage and unreadable tables; original journal persistence before writing; undo restoring all four original states; duplicate undo refusal; conflicting newer value refusal; thrown/rejected/ignored/overridden writes; failed read-back; storage failure preventing mutation; stale originals preventing mutation.
- Android lint: zero errors. Ten warnings concern available dependency/Gradle updates and two optional KTX URI convenience substitutions. Pinned dependencies were retained after verification.
- Two instrumentation tests passed on the actual Pixel with both WRITE_SECURE_SETTINGS and Modify system settings absent. The provider test enumerates all three tables, reads a visible key exactly, reads the two diagnostic flags, verifies an absent key, and round-trips and self-compares a snapshot in memory. The Compose smoke test checks read-only start, Secure-table search, diagnostic/access/history navigation, empty local history, and unchanged flag values before/after.
- App installation and cold launcher activity start succeeded. A screenshot of the actual dark-mode read-only System screen was visually inspected: title, edit switch, tabs, permission label, search, wrapping values, copy buttons and bottom navigation were legible. Screenshot is not included in the source bundle.
- APK signature verification passed (v2), and manifest inspection confirmed the application name/package, min SDK 28, target SDK 37, WRITE_SETTINGS/WRITE_SECURE_SETTINGS declarations, and no INTERNET permission.

## Not performed

- No Android setting was added, edited, deleted, or undone on the Pixel. No write access was granted/revoked; no app-op or supervision service was changed. Provider mutation/undo and failures were verified using fake backends in unit tests, not real elevated device writes.
- The Android document-picker UI export/import flow was implemented but not exercised end-to-end with a user-chosen file. Snapshot parsing/comparison was tested on JVM and Android; no sensitive device snapshot was exported during development.
- Light theme, accessibility font scaling, rotation, secondary/work profiles, policy-managed devices, and OS backup/restore were not manually exercised. Backup exclusion is configured in manifest/XML and private history storage.

## Test setup findings

An initial UI attempt used transitive Espresso 3.5.0, which reflected on a removed Android 17 InputManager API. Explicit Espresso 3.7.0 resolves that failure (its authoritative release notes document the getSystemService fix). A later UI attempt could not see Compose while the device display was off/locked; both tests passed after the user unlocked the Pixel. Tests do not bypass the lock screen.

Read-back confirms the immediate provider-visible state. It cannot guarantee service behavior, future persistence, hidden/redacted state, or atomicity against concurrent writers. PENDING/UNVERIFIED outcomes are retained for manual inspection. Snapshot files describe only provider-visible subsets.
