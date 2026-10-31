# Phone-only wireless setup (v1.4.2, experimental)

This feature uses Android's explicit wireless **ADB** authorization. It removes the need for a computer or typed shell commands; it does not bypass debugging or Android permission checks. No root service or hidden API exemption is used.

## Phone workflow

1. Tables → ⋮ → Enable editing → Secure or Global → **Set up wirelessly on this phone**.
2. Explicitly allow Android 17 local-network access. On the phone, enable Developer options and Wireless debugging and accept Android's network trust prompt if shown.
3. Tap **Open Settings in split screen** (Android 12L+), then open Wireless debugging in the Settings half. Android controls whether the split request is honored. If it opens full screen, swipe up and hold to show recent apps, tap the PowerToys icon above its preview → Split screen, then select Settings. With three-button navigation, use Overview instead. Keep Android's **Pair device with pairing code** dialog open. Enter its pairing port and six-digit code in the app and tap **Pair with this phone**. The code is masked and cleared from the field when submitted; it is not saved or logged.
4. Once pairing reports completion, close Android's pairing-code dialog. Enter the separate connection port from the main Wireless debugging screen's **IP address & Port**. Tap **Check connection only**. Pairing and checking do not grant settings access.
5. After a successful connection/identity check, **Review wireless grant…** shows exactly what privilege is being granted. Only the separate **Grant write access** confirmation runs a fixed permission command. Success is reported only if this app's local permission check confirms WRITE_SECURE_SETTINGS. No settings-table values are changed by setup.
6. Return to the main guide and explicitly enable editing. Turn Wireless debugging off after setup. A successful permission grant normally remains until revoked or the app is uninstalled; ordinary value edits continue through the app's normal ContentResolver backend and safety/history checks.

The default host is `127.0.0.1` (this phone). If a device requires the address shown by Android, only a numeric IP actually assigned to this phone is accepted. Other devices and hostnames are rejected. Pairing and connection ports are different and can change after debugging restarts. An expired code, a dismissed pairing dialog, permission denial, policy restrictions or changed server identity can require another attempt. There is no automatic fallback to legacy non-TLS ADB.

## Permission and trust removal

- **Review wireless revoke…** requires a fresh connection check and a separate confirmation. It executes only this app's fixed revoke command and verifies the resulting local permission. It does not restore edited settings.
- Android Wireless debugging → Paired devices → Android PowerToys → Forget removes the daemon's trust in the pairing identity.
- **Delete local pairing identity…** removes the app's encrypted RSA credential and server pin after confirmation. This is independent of revoking an already granted WRITE_SECURE_SETTINGS permission.
- **Switch to read-only** stops new session edits; it does not revoke Android permissions or undo values.

## Boundaries

- INTERNET is declared for sockets, and ACCESS_LOCAL_NETWORK is explicitly requested on Android 17 only inside wireless setup. The app restricts destinations to this phone's numeric loopback/interface addresses. It does not scan the LAN, call internet services, upload snapshots, or add telemetry. This replaces the older build's “no INTERNET permission” property while preserving no cloud uploads.
- Every action opens a short connection and closes it. There is no auto-connect on launch, background reconnect service, discovery scan, arbitrary terminal, user-supplied command or root mode.
- TLS command sessions require the command-server public-key pin captured after the first own-device read-only identity check (trust on first use). Self-signed debugging certificates use this pairing/pinning model rather than public-web PKI. The first read-only command connection accepts and records the presented server key; it is not cryptographically bound to the separate pairing certificate. Permission commands require the saved pin. A changed key requires re-pairing and another read-only check.
- Read-only probes are `id -u`, `am get-current-user`, and an exact-package UID listing. The shell must be unrooted UID 2000; the installed package UID must match this app's UID in the current Android user. Only then can a reviewed grant/revoke target that same user. Other profiles are not supported.
- RSA credentials are encrypted using AES-GCM with a non-exportable Android Keystore wrapping key. Encrypted credentials and public server-key pin live in noBackupFilesDir, excluded from cloud backup/device transfer. Pairing codes are memory-only. Native SPAKE2 and Conscrypt have 16 KB-aligned binaries. Source, licenses and rebuild instructions are in third_party/ and **About → Open-source licenses**.
- Connect and socket reads have bounded timeouts. A stalled operation may still take multiple timeout intervals. Pairing and read-only connections (including pinned reconnects) have now been verified on the target Pixel. Grant/revoke and actual settings writes through granted access have not been device-tested. Library inspection/tests are not an independent security audit.

## Research

- [Android 17 local-network permission](https://developer.android.com/privacy-and-security/local-network-permission): target 37 direct local networking requires runtime ACCESS_LOCAL_NETWORK (or a mediated picker). Manual phone-only pairing uses an explicit permission rather than network discovery.
- [Android wireless debugging](https://developer.android.com/tools/adb#wireless): pairing-code authorization and distinct pairing/connection steps.
- [LibADB Android 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1): paired TLS transport. Local modifications add timeouts, server-key pinning and reject legacy non-TLS connections; Android hidden TLS-provider fallback is removed.
- [SPAKE2 Android 2.2.1](https://github.com/MuntashirAkon/spake2-java/tree/2.2.1): native Android wrapper, with pinned [C source](https://github.com/MuntashirAkon/spake2-c/tree/0d15933e5ba3e662cb01245a7ac0dc9fca3eac31). The experimental pure-Java implementation failed its own tests and logged cryptographic material; it is not shipped.

Split-screen launch uses the public [LAUNCH_ADJACENT + NEW_TASK API](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode#launch-adjacent). It opens Developer options without enabling debugging, pairing or granting permissions.
