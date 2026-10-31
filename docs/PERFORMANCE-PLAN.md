# Performance tab proposal

Planning only: this release adds no Performance tab, performance preset or automatic change. Target: Pixel 10 Pro XL / Android 17. Research checked 2026-10-31.

## Purpose and first version

A searchable, local catalogue of a few well-supported controls, with separate goals for faster-feeling interactions, smoother motion, and background efficiency. Each card should show current value, captured starting value, available AOSP reference, edit eligibility, evidence, tradeoffs and a review/restore action. Never label a lower animation duration as a measured CPU-speed improvement.

Start with animation controls and links to supported Android display controls. Include an educational “Claims to investigate” section, visibly separated from reviewed candidates. No one-tap “boost everything” button, numeric performance score or recommended universal preset. Existing tables remain the source of truth; the tab points to exact keys and the existing per-change review/history/undo flow.

## Candidate assessment

| Candidate | Published basis | Expected benefit and tradeoff | Android 17 / Pixel disposition |
|---|---|---|---|
| Window, transition and Animator duration scales | Global `window_animation_scale`, `transition_animation_scale`, `animator_duration_scale`; framework documentation and Developer options | Shorter visual transitions may feel quicker. Motion, perceived timing and some app behavior change; this is not evidence that computation is faster. | First editable candidate after confirming the current consumer/observer path on Android 17. Allow an individually reviewed trial of a selected scale, with original saved; no automatic 0.5× preset. Global/device-wide scope and actual permission shown. |
| Smooth Display / refresh-rate policy | System `peak_refresh_rate`, `min_refresh_rate`; display policy also considers app requests, supported modes and power saving | Higher permitted rates can improve motion smoothness. More sustained high-rate use can trade battery and heat for smoothness. | First version links to Android's supported display screen. Both raw keys are private System keys blocked by this app's modern-target access restrictions; WRITE_SECURE_SETTINGS does not make arbitrary System writes supported. No forced minimum, assumed 120 Hz value or claimed Pixel gain. |
| Cached-app freezer | Global `cached_apps_freezer`; AOSP CachedAppOptimizer and freezer documentation | Can reduce work by cached apps while retaining them in memory. Compatibility, lifecycle behavior and device configuration matter. | Research/information card initially. AOSP documents a platform feature, not evidence that overriding this Pixel's default improves it. Inspect the Android 17 consumer, restart requirements and device default before any editing UI. No automatic DeviceConfig changes. |
| “Don't keep activities” / background process limits | Developer options and Global `always_finish_activities` | These are development/testing controls; aggressively discarding activities can undermine multitasking. | Explain why they are not general speed-up recommendations. Do not recommend enabling them, invent a table key for process limits, or build a RAM-clearing action. |
| Disable HW overlays / force GPU / 4× MSAA | Developer options documents their rendering/debugging purposes; forum users report device-specific workarounds | Reported graphics fixes are not universal speed gains. Rendering workload, battery, heat and image quality may change. | Claims-to-investigate only, linked to Developer options when appropriate. Several controls use service state or properties rather than settings-table rows. Never create fake Global rows to simulate support. |

The animation and rendering-control descriptions above are grounded in [Android Developer options](https://developer.android.com/studio/debug/dev-options) and the pinned [Android 17 Settings declarations](https://android.googlesource.com/platform/frameworks/base/+/94b4c163b7dfe5ce3607f7bb8456f9573f7de57d/core/java/android/provider/Settings.java). Display-policy constraints are documented in [AOSP multiple refresh rates](https://source.android.com/docs/core/graphics/multiple-refresh-rate). The freezer is documented in [AOSP cached apps freezer](https://source.android.com/docs/core/perf/cached-apps-freezer); its [Android 17 key declaration](https://android.googlesource.com/platform/frameworks/base/+/94b4c163b7dfe5ce3607f7bb8456f9573f7de57d/core/java/android/provider/Settings.java#20317) names enabled/disabled/device_default states. These sources establish mechanisms; none establishes a measured gain on this Pixel.

## How forum evidence should appear

Use forums to discover hypotheses and conflicting reports, with device, OS version and post date visible. Popularity is not efficacy. For example, [Pixel owners discussing 0.5× animations](https://www.reddit.com/r/GooglePixel/comments/1gi8qn8/) describe subjective responsiveness and differing preferences. [Older Pixel reports](https://www.reddit.com/r/GooglePixel/comments/r03kk7/) describe fingerprint-related problems with reduced scales; these are historical anecdotes, not established Android 17 behavior. A [Pixel 10 graphics-workaround discussion](https://www.reddit.com/r/GooglePixel/comments/1n8tq21/) suggests GPU-related switches for glitches; this is a hypothesis about a particular issue, not a validated performance recommendation.

Each claim needs a primary implementation source before it can become an editable recommendation. Label evidence independently as “Platform documented,” “Community reported,” and “Measured on this build”; multiple labels may apply. Never collapse these into “safe” or “proven.” Store sources and review dates in the bundled catalogue; browse no forum or network service automatically in the app.

## Review and recovery

A detail screen should answer: what changes, which workload might benefit, what can worsen, whether the device supports it, how to measure it, and how to restore the exact pre-change state. No edit or elevated access simply from opening the tab. Unsupported paths show a Settings link or explanation, not a misleading Apply button.

Apply one change at a time using the existing journal-before-write, exact absent/empty handling, conflict-aware undo and read-back verification. A matching row proves storage read-back, not that a service adopted the policy or that performance improved. Keep baseline restore separate from returning to the immediate pre-experiment value. Avoid grouped animation presets until multi-key partial failures and conflict-aware rollback have an explicit design.

The existing 15-second timer is useful for screen usability, not for proving a performance benefit, detecting missed notifications or measuring battery life. Retain existing display trials where required; do not add a generic timer to every candidate. Defer any new raw display-policy edit until it has a supported access path and appropriate recovery design. A later optional experiment reminder can prompt a manual comparison, without automatically undoing long-running settings or adding background networking.

## Measuring an actual benefit

Before calling a change “Measured on this build,” record device/build, candidate and original state, permission path, workload, repeated before/after runs, spread/outliers, temperature/charging/power mode and display conditions. Re-run after restoring the original; record negative and neutral outcomes. Measure animation duration separately from app startup and frame jank. Don't compare a cold launch before with a warm launch after.

[Macrobenchmark metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics) include startup and frame timing; use a dedicated controlled benchmark/test target or supported tracing workflow. This app must not imply it can silently instrument arbitrary production apps or measure device-wide FPS from its own frame callbacks. Battery claims require longer controlled observations; a 15-second screen trial is insufficient. Traces/reports remain local unless the user explicitly exports them.

## Implementation sequence and acceptance criteria

1. Define a small versioned candidate catalogue with exact table/key or Settings action, evidence URLs, OS/build applicability, state semantics, expected benefit, tradeoffs and measurement recipe. Unit-test unsupported/inapplicable handling and evidence labels.
2. Ship the read-only Performance catalogue and Settings links first. Test on the Pixel that no candidate read, navigation or “learn more” action grants access or mutates a row. Preserve the settings-list viewport.
3. Validate Android 17 consumers and permission paths for animation changes. With separate user authorization, test one reversible change, service behavior, read-back, undo, failure handling and any OS override; only then expose the existing reviewed edit action there.
4. Add measurement-backed findings only after controlled experiments. Keep freezer/GPU/background-process hypotheses informational until their acceptance gates are met.

The proposed first slice is deliberately small: useful animation explanations, supported display controls and clear evidence. It does not need root, extra privileges, a persistent debugging connection or any cloud service. Open product choice for later: whether community-only hypotheses should live under a collapsed section or a separate research page; neither is an automatically recommended preset.
