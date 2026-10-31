# Descriptions, defaults and recovery (v1.1)

## What “default” means here

Android's normal settings query exposes readable current values, not a complete, authoritative per-key Pixel factory-default catalog. AOSP resources can be overridden by device overlays, setup, carriers, policy and upgrades. The app therefore presents three separate values:

1. **Current**: the exact provider-exposed value/existence observed for this Android user, subject to Android's read restrictions/redaction.
2. **Starting device value**: the immutable local baseline captured when v1.1 first opens successfully. It is not a factory default or the value at first installation of v1.0. Refresh and APK updates do not overwrite it. Build fingerprint, user serial and capture time are retained; a different OS build produces a historical-baseline notice. Uninstall/clear-data removes it.
3. **AOSP Android 17 reference default**: a curated unoverlaid resource value with a verified initialization path. Unknown references say unavailable. In particular, neither filtering flag is assigned an invented factory default of zero.

The Defaults screen compares starting vs AOSP, current vs starting, and current vs AOSP where evidence exists. Numeric formatting differences such as `1` and `1.0` are equivalent for the curated numeric AOSP comparisons; restoration always preserves the exact captured string/state. Missing baseline entries are **unknown**, not absent. For catalog keys missing from enumeration, an exact successful read can record actual absence. Unreadable keys are not assigned baseline values.

Starting-value restore is per key: **Tables → ⋮ → Defaults & starting-value restore → find the key → Review starting-value restore**. “Changed or not currently visible” narrows the list; omitted rows still require exact inspection, not an assumption of deletion. The review shows the target and effects and uses the existing permission, history, conflict and read-back checks. Null, empty, zero and absent are distinct. Android's System deletion restrictions still apply. There is no bulk reset or automatic application of AOSP reference values.

## Risk decisions

| Category | Examples | Behavior and reason |
| --- | --- | --- |
| Display/usability trial | System `screen_brightness`, `screen_brightness_mode`, `font_scale`, `screen_off_timeout`, `accelerometer_rotation`, `user_rotation` | Mandatory 15-second visibility/usability confirmation. A person can directly assess whether controls remain usable. Values are bounded by the app; original must be present and readable. |
| Recovery risk / blocked | Input-method selection, accessibility controls, forced density/size, system locales, debugging switches, provisioning/setup and hidden-API controls | Prefer Android's supported screens. Losing essential input, assistance or external recovery can outlast an app timer; no claim that a countdown makes these safe. Blocking is enforced in the mutation path as well as UI. |
| High impact | Filtering/SafeSearch flags, time/time-zone detection, airplane-mode row, sound URIs and stored volumes | Specific effects plus explicit acknowledgement at edit review. A 15-second screen check cannot validate privacy, future alarms, connectivity or correct service behavior. These do not receive a misleading automatic trial. |
| Caution | UI sounds/haptics, animation timing and clock formatting | Explain concrete effects at review. No blanket “safe” badge. |
| Unassessed | Vendor/private/unrecognized keys | “Description unavailable,” no guessed default, explicit acknowledgement that effects are unknown and may be adverse. No automatic timer: the app cannot reliably decide how to assess or reverse an unknown operation. |

The catalog is curated, not an exhaustive risk assessment of every Android/vendor key. A setting absent from the blocked list is not a promise of safety. Direct table writes do not replace the dedicated service/API for radios, audio, languages, input or security controls.

## Timed trial implementation and limits

An explicit foreground edit starts a non-exported `shortService` foreground service. It must successfully enter foreground mode before any new provider mutation. Display-trial notifications must be enabled; Android's POST_NOTIFICATIONS approval is requested only through the explicit notification control. The service uses a partial CPU wake lock, capped at 30 seconds, to support the 15-second deadline when the display sleeps; it does not force the display on or bypass the lock screen. There is no exact-alarm access, root, battery-optimization exemption, or hidden elevation.

Before the provider write, an atomic no-backup pending record stores the original state, requested state and monotonic deadline. The normal journal independently records the original before mutation. The foreground service attempts rollback after the deadline; only an in-app “I can use it — Keep” confirmation before expiry keeps the trial. The notification offers **Restore original**, never a blind keep action. Activity recreation does not cancel the service timer. Manual restore and timeout both verify read-back. Other edits are blocked during a pending trial.

If a write is rejected or cannot be verified, the guard attempts to end the trial safely. If rollback fails, its original is retained and the UI reports failure. If another writer has replaced the trial value, rollback refuses to overwrite it. The user can retry, or explicitly choose manual recovery; that writes a RECOVERY_REQUIRED history record before clearing the pending trial and makes no settings write. A fresh process that finds a persisted pending trial attempts recovery immediately instead of silently accepting it.

**This is best-effort application recovery, not an Android display-mode transaction.** OS scheduling delays, force-stop, reboot, a killed/frozen process, permission revocation, provider restrictions or failed storage can prevent timely rollback. shortService is non-sticky; no reboot/background receiver is installed. Recovery after an interrupted process is attempted when the app process next starts. The service has no authority beyond the permissions already granted. Read-back covers exposed state only. There remains a race between an optimistic conflict check and another process's write; no cross-process compare-and-swap API is claimed.

Read-only startup prevents new edits. An explicitly authorized trial's pending restoration may still run on reopening, as described before the user starts that trial. Filtering flags never use automatic changes.

## Sources checked 2026-10-30

- [Android 17 Settings.java](https://android.googlesource.com/platform/frameworks/base/+/android17-release/core/java/android/provider/Settings.java): names, units, semantics and SDK read restrictions.
- [Android 17 unoverlaid default resources](https://android.googlesource.com/platform/frameworks/base/+/android17-release/packages/SettingsProvider/res/values/defaults.xml): reference values, including 60000 ms screen timeout, brightness 102, manual brightness mode, rotation preferences, sounds/haptics and font scale 1.0.
- [DatabaseHelper](https://android.googlesource.com/platform/frameworks/base/+/android17-release/packages/SettingsProvider/src/com/android/providers/settings/DatabaseHelper.java): initialization mappings, boolean-to-integer and fraction values. These mappings and resources are reference data, not a device factory-reset API.
- [SettingsProvider](https://android.googlesource.com/platform/frameworks/base/+/android17-release/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java): modern `handleDefaultFontScale` initializes `font_scale` from the overridable `def_device_font_scale` resource only when no prior value exists; System mutation limits remain in force.
- [Foreground service types / shortService](https://developer.android.com/develop/background-work/services/fgs/service-types#short-service): brief critical work, roughly three-minute upper timeout, non-sticky behavior and timeout callback. The app's 15-second deadline is much shorter.
- [Foreground start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start): service starts from an explicit visible interaction; failure must abort before mutation.
