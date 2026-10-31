<p align="center"><img src="docs/app-icon.svg" width="88" height="88" alt="Android PowerToys circular settings icon"></p>

# Android PowerToys

**Explore Android settings. Understand changes. Keep a way back.**

Created by [MooseAI, LLC](https://mooseaillc.com) · [Support](mailto:support@mooseaillc.com) · [Download preview](https://github.com/chiesennegs/AndroidPowerToys/releases/tag/v1.6.1)

Android PowerToys is a local settings workbench for Android's **System**, **Secure**, and **Global** tables. Browse and search the values Android exposes, read sourced descriptions, compare snapshots, and make explicitly authorized changes with saved originals and verified read-back.

The app starts **read-only**. It has no ads, analytics, accounts, or cloud upload service. Its optional phone-only wireless setup uses Android's debugging authorization and needs a separate confirmation before granting editing access.

> **Preview:** v1.6.1 is a development-signed build, initially validated on a Pixel 10 Pro XL running Android 17. Real permission grant/revoke and live settings writes have not been validated in the recorded device tests. Review [testing and limitations](#testing-and-known-limits) before using elevated access.

[Capabilities](#capabilities) · [Install](#install) · [Use](#browse-and-inspect) · [Enable editing](#enable-editing) · [History and restore](#history-and-restore) · [Snapshots](#snapshots-and-comparison) · [Build](#build-from-source)

## Capabilities

| Area | What you can do |
| --- | --- |
| Browse | Switch among System, Secure and Global; see each visible setting's exact name and current value. |
| Search and sort | Search names and values with a compact pinned search field; sort by name in either direction or by value. Clear a query with ×. |
| Navigate | Each table starts at the top on first visit and remembers its own depth. A floating up-arrow returns the current table to the top. |
| Inspect | Open a setting for its full available description, source link, permission eligibility, risk explanation and value references. Text is selectable and values can be copied. |
| Understand | 1,306 exact-key Android 17 source-documentation entries, curated explanations, 56 documented audio stream/output combinations and three SystemUI consumer descriptions. Unknown meanings stay **Description unavailable**. |
| Edit | Review eligible updates, additions and deletions after granting the required Android access and explicitly enabling the editing session. Platform restrictions still apply. |
| Record and undo | Record the original existence/value before attempting a write; report verification only after a matching read-back. Review an undo without overwriting a newer conflicting value. |
| Starting values | Compare current values with the immutable, locally captured starting baseline; review a per-key restore. |
| Reference values | Show a sourced AOSP reference default where separately verified, distinguish it from the starting value, and indicate whether they match. |
| Snapshots | Export a JSON snapshot through Android's file picker; compare two files for added, removed and changed visible settings. |
| Usability | Light, dark or device-following appearance, bundled Outfit typography, readable wrapping values, circular app icon and a consistent header. |
| Setup | Guided System special-access setup, optional phone-only wireless pairing, or copyable package-specific ADB commands. |

Description availability is not a safety rating. Source-only descriptions remain **effects not reviewed**, with no inferred Pixel factory default. On the tested device, descriptions covered 66/73 visible System keys, 132/258 Secure keys and 184/300 Global keys; other builds and permission states can expose different rows. See [description evidence](docs/DESCRIPTIONS.md).

## How this compares with Windows PowerToys

Both share the idea of making more control available to power users. Their current scope is different:

| | Windows PowerToys | Android PowerToys |
| --- | --- | --- |
| Platform | Windows desktop | Android; initial target is Pixel / Android 17 |
| Main scope | A broad collection of productivity and customization utilities | A focused workbench for Android's three settings tables |
| Typical tools | Window layouts, keyboard remapping, launching, file operations and other desktop utilities | Settings search, descriptions, reviewed edits, history, starting-value restore and JSON snapshot comparison |
| Configuration access | Depends on the individual desktop utility | Governed by Android table permissions, user scope, validators and device policy |
| Feature parity | A suite with many separate tools | Does not implement the desktop suite's window, keyboard, launcher or file-management features |

The Windows overview is documented by [Microsoft](https://learn.microsoft.com/en-us/windows/powertoys/). Android PowerToys is an independent MooseAI project, not a Microsoft product, Android port, or promise of feature parity.

## Requirements and table scope

- **Minimum installation:** Android 9 / API 28. Compile and target SDK: 37.
- **Initial device validation:** Pixel 10 Pro XL, Android 17 / API 37. Other devices and Android versions are not covered by the current device-test record.
- **Phone-only wireless pairing:** Android 11 or newer, Wi-Fi, Developer options, and Wireless debugging. Android 17 additionally requires explicit local-network permission inside this setup.
- **System / Secure:** generally scoped to the Android user running the app.
- **Global:** device-wide; an edit can affect other users.

The app uses its current Android user and offers no cross-profile access. Android can omit keys, deny exact reads, or redact values. An unreadable value is never treated as an absent setting.

These tables do **not** contain all Android configuration. App-private preferences, device policy, DeviceConfig, properties and service-managed state can live elsewhere. A row may also be legacy, ignored, validated, or later overwritten by its consumer.

## Install

### On the phone

1. Open the [v1.6.1 preview release](https://github.com/chiesennegs/AndroidPowerToys/releases/tag/v1.6.1) and download **AndroidPowerToys-debug.apk**.
2. Open the APK in your phone's file manager. Approve installation from that source when Android asks.
3. Launch **Android PowerToys**. It starts read-only and does not grant access or change a setting automatically.

The release includes SHA-256 checksums and a complete source archive. Preview APKs use a development certificate. Builds you produce locally or download from CI can have a different signing identity; Android may refuse to install them as an update. Uninstalling to change signing identities deletes local baseline/history, so preserve any records you need first.

### With Android Platform Tools

After explicitly enabling and authorizing USB or wireless debugging:

```sh
adb devices
adb install -r AndroidPowerToys-debug.apk
```

`-r` updates an installation with the same signing identity while preserving app data. If multiple devices are connected, select the intended device with `adb -s SERIAL ...`. Installing the APK does not grant settings editing access.

## Browse and inspect

1. Open **Tables**. Tap the title/table area and select **System**, **Secure**, or **Global**.
2. Type a name or value in the search field. Search remains visible while rows scroll; changing the query returns the current list to its first matches.
3. Use **⋮ → Sort** to cycle through name ascending, name descending and value ascending. Use the floating up-arrow when you want to return to the top.
4. Tap a row to inspect it. The detail shows current state, the full available description, source documentation, consequences, starting value and any verified AOSP reference.
5. Use **Copy value** or select text when you need to inspect it elsewhere.

Use **⋮ → Look up / Add** for an exact key. The app reads its state before offering a creation operation; it does not infer absence from a failed read.

A row's documented meaning does not guarantee that a direct write is supported or that a service will act on it. The app marks eligibility and risks, and it can still receive a platform rejection after permission is granted.

## Enable editing

Open **Tables → ⋮ → Enable editing**. Select the intended table and complete the relevant setup below. The final enable button stays unavailable until detected access and local safety prerequisites are present. Cancelling leaves the session read-only.

### System

Choose **Open Modify system settings**, approve the Android special-access screen, and return. The guide checks access again automatically and also offers **Check access again**.

This access supports eligible public System edits. Android 17 still prevents this modern-target app from editing arbitrary private System keys, creating arbitrary System rows, or deleting platform-defined System keys. A secure-settings grant does not remove those restrictions.

The same Android special-access screen is available under **About → Advanced permission details**.

### Secure and Global: phone-only wireless setup

No computer is required, but this path **does use wireless ADB authorization**. It does not bypass Android's permission model or require root.

1. In the editing guide, choose Secure or Global, then **Set up wirelessly on this phone**.
2. Explicitly allow local-network access if requested. Enable Developer options and Wireless debugging in Android Settings, accepting Android's network prompt if shown.
3. Tap **Open Settings in split screen**. In the Settings half, open **Wireless debugging → Pair device with pairing code**. Keep that code dialog open.
4. Enter its **pairing port** and six-digit code in Android PowerToys, then tap **Pair**. The code is masked, cleared on submission, and never saved or logged.
5. Close Android's pairing-code dialog. Enter the separate **connection port** from the main Wireless debugging screen, then choose **Check connection only**. Pairing and checking do not grant settings access.
6. After a successful identity check, choose **Review wireless grant** and explicitly confirm **Grant write access**. Success requires this app's actual permission check to confirm the grant.
7. Return to the editing guide and explicitly enable the session. Wireless debugging can be turned off after a verified grant; normal edits use Android's content-provider APIs.

The pairing and connection ports differ and can change when debugging restarts. Split screen is a request to Android; if it is not honored, open recent apps, tap the PowerToys icon above its preview, choose Split screen and select Settings. Keep the pairing dialog visible until pairing completes.

Connections are restricted to numeric addresses assigned to this phone or loopback. There is no LAN discovery, general terminal, background reconnect or root mode. Each operation opens and closes its own connection. The shell must be unrooted and the installed app UID must match the current Android user. Credentials are encrypted in no-backup storage. The first read-only command connection records a server-key pin using trust on first use; later permission connections require that saved pin. See [wireless setup and trust boundaries](docs/WIRELESS-SETUP.md).

### Secure and Global: one-time computer command

With the intended Android user active, explicitly authorize a debugging connection and run:

```sh
adb shell pm grant --user current dev.androidpowertoys.app android.permission.WRITE_SECURE_SETTINGS
```

This grants a broad development permission to this package. Device policy or a platform build may refuse it. To inspect the foreground user first:

```sh
adb shell am get-current-user
```

Return to the app and check detected access. Copying an on-screen command does not execute it. The optional wireless flow executes only its fixed grant/revoke command after a separate explicit confirmation.

### Revoke access or stop editing

- **⋮ → Switch to read-only** stops new session edits. It does not revoke Android permissions or undo values.
- Turn off **Modify system settings** in Android's special app access screen to revoke that access.
- Revoke the development permission with:

```sh
adb shell pm revoke --user current dev.androidpowertoys.app android.permission.WRITE_SECURE_SETTINGS
```

The wireless setup also offers a reviewed revoke action after a fresh connection check. It verifies the resulting permission state. Forgetting a paired device in Android and deleting the app's local pairing identity remove pairing trust; neither action revokes an already granted settings permission or restores edited values.

## Review and apply a change

Once editing is enabled, open a setting, enter a value or choose Delete, then review the exact target and consequences before applying.

| State | Meaning |
| --- | --- |
| Absent | The row did not exist in a successful exact read. |
| Empty string | The row exists and stores `""`. This is not deletion. |
| Zero | The row exists and stores `"0"`; its meaning depends on the key. |
| Present null | A present row exposes a null value; distinct from an absent row. |
| Unreadable | The read failed or is restricted; no absence is assumed. |

Before attempting a write, the app records the original value **and whether the row existed**. A provider-reported success is insufficient: the app reads the setting back and marks it verified only when it matches. A rejection or mismatch remains visible and is not reported as a verified change.

High-impact and unassessed edits require explicit acknowledgement. Recovery-critical input, accessibility, setup and debugging controls are blocked; use Android's supported Settings UI for those changes.

### 15-second display trials

Six System keys use a confirmation trial: `screen_brightness`, `screen_brightness_mode`, `font_scale`, `screen_off_timeout`, `accelerometer_rotation`, and `user_rotation`.

The app requires recovery notifications, records the original, starts a short foreground recovery service, applies the change, and asks you to confirm that the screen is usable within 15 seconds. Otherwise it attempts restoration and verifies read-back. Input bounds reduce the chance of an unusable value.

This is best-effort recovery, not an OS failsafe. Force-stop, reboot, lost permission or system failure can prevent restoration. Pending originals are retained for recovery on the next start. Conflicting newer values are not silently overwritten. A screen-visibility check cannot establish privacy, battery, connectivity or future alarm behavior; it is not applied as a generic timer to every edit. See [safety behavior](docs/SAFETY.md).

## History and restore

**History** keeps original and requested states with the outcome of each attempt. Verified means the immediate read-back matched; pending or unverified records need inspection. Review an undo only when the current value still matches the value the recorded change wrote. Undo itself is journaled and verified.

**Tables → ⋮ → Defaults & starting-value restore** provides a separate view:

- **Starting value:** captured locally once when baseline support first opens. It describes that device at capture time, not an untouched factory image. Updates and Refresh preserve it; uninstall/clear-data removes it.
- **AOSP reference:** a separately verified Android source reference where available. It is not a Pixel factory default and is never guessed for unknown keys.
- **Current value:** what the provider exposes now. The app shows how it compares with the recorded/reference state.

Choose a key and review a starting-value restore. Restoration remains per key, needs the usual access, records history and verifies read-back. A changed OS build is called out because historical values may no longer suit it. An omitted baseline key is not inferred absent. Snapshot imports do not bulk-restore settings.

## Snapshots and comparison

1. Open **Snapshots → Export snapshot** and select a location in Android's file picker.
2. Later, choose **A** and **B** and run **Compare snapshots**.
3. Review added, removed and changed visible rows, including exact existence/value differences.

Snapshots are versioned JSON containing the provider-visible tables for this app's user. They are captured sequentially rather than atomically. Compare files from the same device, user and permission level. The app rejects incompatible user serials, coverage and table errors, but cannot independently prove device identity. Imports are bounded to 8 MiB and never execute a restore.

Snapshots can contain sensitive identifiers or configuration. Keep them private. A cloud document provider can upload an export that you explicitly save there; this app has no upload service.

## Privacy and local storage

- No ads, analytics SDK, account requirement, telemetry or automatic cloud uploads.
- History, starting baseline, pending recovery state and encrypted pairing identity use app-private no-backup storage. Automatic backup/device transfer are excluded.
- Network permissions exist for the explicit wireless setup. The app connects only to this phone's accepted numeric addresses and does not scan the LAN or call an internet backend.
- Documentation, website and email links open only when tapped. Email opens a composer; nothing is sent automatically.
- Uninstalling or clearing app data deletes local records. Permission revocation does not restore previously edited settings.

## Testing and known limits

The recorded validation includes 36 app unit tests, four shell-stream regression tests, and device checks for provider reads/snapshot round trips, baseline persistence, navigation, description coverage, display-service readiness without a write, encrypted identity/native cryptography, actual wireless pairing/read-only pinned reconnects, compact pinned search and return-to-top behavior. The latest interface checks passed in light and dark modes with lint reporting zero errors.

**Not device-validated:** live settings writes/undo, the explicit permission grant/revoke, actual timed display rollback, force-stop/reboot recovery, the document picker's complete file workflow, work profiles, managed devices, or arbitrary Android versions. Core mutation/failure/undo and recovery states are tested with fake backends and deterministic clocks. Read-back proves immediate visible storage state, not consumer behavior, future persistence, performance improvement or hidden underlying state.

The overall browsing region measured 75.7% of the tested full-screen Pixel window. Pinned search and its spacing occupy part of that region, leaving 68.0% for unobscured scrolling rows. Those measurements do not apply to arbitrary font scales, an open keyboard or split screen.

See the detailed [validation record](docs/TESTING.md), [platform decisions](docs/PLATFORM-RESEARCH.md), [wireless constraints](docs/WIRELESS-SETUP.md), and [description evidence](docs/DESCRIPTIONS.md).

## Planned, not shipped

A [Performance tab proposal](docs/PERFORMANCE-PLAN.md) explores a small evidence-based catalogue. It separates documented mechanisms, community reports and measurements on a specific build. No Performance tab, speed preset, automatic optimization or background benchmarking ships in this release.

## Build from source

The project uses Kotlin, Jetpack Compose and Material 3, with compile/target SDK 37. Install Android SDK Platform **37.0** and Build Tools **36.0.0**. Gradle 9.4.1 and Android Gradle Plugin 9.2.1 are pinned. Builds have been verified with JDK 25; configure Android Studio's Gradle JDK or `JAVA_HOME` accordingly.

```sh
git clone https://github.com/chiesennegs/AndroidPowerToys.git
cd AndroidPowerToys
# Set ANDROID_HOME to your SDK directory, or configure local.properties.
./gradlew :app:testDebugUnitTest :libadb:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. JDK/SDK paths and signing keys are not in the repository. The supplied source includes the locally modified transport modules, native sources, pinned JNI binaries, documentation sources, fonts and licenses. [Third-party build/relink instructions](third_party/README.md) explain replacing or rebuilding the native component with `-PrebuildSpake=true` and an installed NDK/CMake. That optional native rebuild path is not part of the recorded device validation.

### Read-only device checks

Keep the device unlocked, preserve any baseline you want to retain, and use update installs with a matching signing identity:

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r dev.androidpowertoys.app.test/androidx.test.runner.AndroidJUnitRunner
```

The normal suite preserves existing permissions and performs no settings writes or grants. An opt-in live wireless connection test is skipped without an explicit current-port argument; see [the test record](docs/TESTING.md) before running it. Never uninstall a baseline-bearing installation merely to run UI checks.

## Contributing and support

[Report an issue](https://github.com/chiesennegs/AndroidPowerToys/issues) or email [support@mooseaillc.com](mailto:support@mooseaillc.com). Include the Android version, device family, app version, affected key name and a redacted error. Do not attach unredacted snapshots, pairing codes, private identifiers or credentials to public issues.

Changes should preserve read-only startup, explicit authorization, original-state recording, conflict-aware recovery, verified writes and no automatic elevation. Add authoritative sources for new descriptions; keep unknown meanings unknown. Separate a documented mechanism from an unverified performance claim.

## License

Original Android PowerToys code is licensed under [Apache License 2.0](LICENSE). Bundled components retain their own terms, including LGPL-3.0, BSD and SIL OFL notices. See [NOTICE](NOTICE), [third-party provenance and modifications](third_party/README.md), and the bundled license texts available under **About → Open-source licenses**. The complete library sources and replacement/relink instructions are included.
