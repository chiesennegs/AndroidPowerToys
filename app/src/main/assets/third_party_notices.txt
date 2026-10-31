# Wireless protocol dependencies and local changes

The source bundle includes the application build inputs and library source so the libraries can be inspected, modified, replaced, rebuilt and relinked. No license restriction on reverse engineering needed to debug modifications to the LGPL-covered library is imposed by this distribution. Build replacement JNI binaries using the included sources, or replace `spake2/src/main/jniLibs/<ABI>/libspake2.so`, then rebuild/re-sign with your own Android signing key. The debug APK is not protected against modified/re-signed builds. Use update installs with the same signing key to preserve local data; a different signing key requires a separate package or reinstall. No device installation restriction beyond normal Android APK signing is added.

## LibADB Android 3.1.1

Upstream: https://github.com/MuntashirAkon/libadb-android/tree/3.1.1
Selected licensing: Apache-2.0 option, with retained BSD-3-Clause and other per-file notices. COPYING and LICENSES are included.

Local changes: API 37 build; corrected shell-stream EOF, pending-close draining, buffered reads and InputStream unsigned-byte/zero-length behavior (four local regression tests); sockets have connect/read timeouts; legacy non-TLS authentication is refused; only bundled Conscrypt APIs are used (no hidden Android TLS provider fallback); fresh TLS contexts avoid stale identity reuse; command-server public-key pinning after the first read-only own-device check (trust on first use); pairing streams/socket cleanup is null-safe; peer identity logging removed. Pairing bootstrap authenticates the explicitly entered code using SPAKE2. Android uses a separate ephemeral pairing certificate: the first read-only command connection accepts and captures its presented server key (TOFU), rather than deriving trust from that pairing certificate. Subsequent connections require the saved SHA-256 command-server public-key pin; permission commands cannot use an unpinned session. No PKI/hostname validation is claimed for Android's self-signed debugging server. App code restricts endpoints to numeric loopback or addresses assigned to this device.

## SPAKE2 Android 2.2.1

Upstream: https://github.com/MuntashirAkon/spake2-java/tree/2.2.1
Wrapper: LGPL-3.0; LICENSE is included along with GPL-3.0 terms incorporated by LGPL.
Native C source revision: 0d15933e5ba3e662cb01245a7ac0dc9fca3eac31 at https://github.com/MuntashirAkon/spake2-c (included under src/main/cpp/spake2-c with its licenses).

Java wrapper is built from source. JNI binaries are unmodified from the pinned JitPack AAR documented and hashed in spake2/UPSTREAM.txt. Full JNI/C sources are included. `./gradlew -PrebuildSpake=true :app:assembleDebug` switches to native source building (requires installed Android NDK/CMake). The local CMake configuration adds a 16 KB ELF page alignment flag. All four provided ABI binaries have 16 KB load alignment.

The upstream experimental pure-Java implementation failed its own shared-key tests and contained cryptographic debug output. It was rejected and is not included in this module or APK. The native path is checked with on-device matching-password and wrong-password key-agreement tests. This does not substitute for an independent protocol/security audit.

## Conscrypt Android 2.8.0 and Bouncy Castle 1.81

Pinned Maven Central dependencies. Conscrypt provides native TLS 1.3 and the public key-export API needed by Android pairing. Bouncy Castle provides certificate generation and pairing primitives. Their applicable licenses/notices are bundled under app/src/main/assets/licenses. No custom cryptographic primitive is implemented by the app. Conscrypt's 64-bit ELF load segments were checked for 16 KB alignment.

App-private RSA credentials are AES-GCM encrypted with an Android Keystore key and stored in no-backup storage; the paired server's public-key hash is also retained locally. No pairing code is persisted. Forgetting the local identity is distinct from forgetting trust in Android's Paired devices UI and from revoking WRITE_SECURE_SETTINGS.

## AOSP setting documentation

SourceDescriptions.kt contains cleaned and bounded Javadoc excerpts from Android 17 Settings.java. Copyright (C) 2006 The Android Open Source Project; Apache-2.0. The pinned unmodified reference, source hash and license are in aosp-settings/. Reproduce with `python3 tools/generate_descriptions.py`. Every entry links to its original declaration comment. These excerpts do not establish safe writes, device defaults or Pixel implementation behavior.

## MooseAI artwork

The unmodified MooseAI logo is from OpenLifeSpan, Copyright 2026 MooseAI, LLC, Apache-2.0. See NOTICE, docs/BRANDING.md and the bundled MooseAI-OpenLifeSpan-Apache-2.0.txt license. The artwork identifies the publisher; OpenLifeSpan's app-specific treadmill assets and behavior are not included.

## Outfit typeface

Outfit is bundled locally from the Google Fonts outfit directory, Copyright 2021 The Outfit Project Authors (https://github.com/Outfitio/Outfit-Fonts), under SIL Open Font License 1.1. The original variable font is unmodified; Compose selects weight axes. See app/src/main/assets/licenses/Outfit-OFL.txt and docs/BRANDING.md for the source and hash. No downloadable-font service is used.
