package dev.androidpowertoys.app

/** Exact stream/output combinations defined by AudioService and AudioSystem, not a prefix guess. */
internal fun audioDescriptions(): Map<Pair<Table,String>,Explanation> = buildMap {
    val streams=mapOf("voice" to "voice calls","system" to "system sounds","ring" to "ringing","music" to "media","alarm" to "alarms","notification" to "notifications","bluetooth_sco" to "Bluetooth call audio","assistant" to "assistant audio")
    val routes=mapOf("speaker" to "the speaker","earpiece" to "the earpiece","bt_a2dp" to "Bluetooth A2DP output","bt_sco" to "Bluetooth SCO output","bt_sco_hs" to "a Bluetooth SCO headset","bt_sco_carkit" to "a Bluetooth SCO car kit","ble_headset" to "a Bluetooth LE headset")
    for((stream,label) in streams) for((route,output) in routes) {
        put(Table.SYSTEM to "volume_${stream}_$route",Explanation(
            "Stored audio volume index associated with $label on $output. This is an internal persisted index, not a percentage. AudioService can alias streams and chooses the active output; changing the row alone does not establish a live volume change.",
            "Low or invalid values can affect audible alerts or playback when adopted. Valid ranges and stream aliases are device-specific. Prefer Android's volume controls; the audio service can overwrite direct writes.",
            "https://android.googlesource.com/platform/frameworks/base/+/94b4c163b7dfe5ce3607f7bb8456f9573f7de57d/services/core/java/com/android/server/audio/AudioService.java#10927",Risk.HIGH))
    }
}
