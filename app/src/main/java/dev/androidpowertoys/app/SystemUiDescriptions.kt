package dev.androidpowertoys.app

/** Consumer-verified keys outside Settings.java; no Pixel-private behavior inferred. */
internal fun systemUiDescriptions(): Map<Pair<Table,String>,Explanation> {
    val root="https://android.googlesource.com/platform/frameworks/base/+/94b4c163b7dfe5ce3607f7bb8456f9573f7de57d/packages/SystemUI/src/com/android/systemui/"
    return mapOf(
        (Table.SECURE to "clock_seconds") to Explanation(
            "Requests seconds in the SystemUI status-bar clock. Android's clock consumer reads this as an integer switch, with false as its fallback; the Pixel's UI may apply additional policy.",
            "Changes the visible clock format and requires more frequent clock updates when honored. It does not change the system time.",root+"statusbar/policy/Clock.java#276",Risk.CAUTION),
        (Table.SECURE to "icon_blacklist") to Explanation(
            "Comma-separated status-bar icon slot names to hide. A missing value lets SystemUI use its configured exclusion list. This is a display preference, not a way to disable the underlying service.",
            "Hiding icons can conceal useful connectivity or device-status indicators. Invalid or device-specific slot names may be ignored; a stored value does not prove that every icon disappeared.",root+"statusbar/phone/ui/StatusBarIconController.java#104",Risk.HIGH),
        (Table.SECURE to "sysui_tuner_version") to Explanation(
            "Internal SystemUI Tuner migration-version marker. TunerService compares it with its implementation version and upgrades stored tuner settings when needed. It is not a performance level.",
            "Changing or removing this marker may cause tuner migrations to run again and change other UI preferences. Leave migration bookkeeping to SystemUI.",root+"tuner/TunerServiceImpl.java#121",Risk.HIGH)
    )
}
