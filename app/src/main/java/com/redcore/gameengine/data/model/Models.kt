package com.redcore.gameengine.data.model

enum class AccentTheme(val id: String, val label: String) {
    CRIMSON("crimson", "Crimson Core"),
    EMBER("ember", "Ember"),
    CYAN("cyan", "Ion Cyan"),
    VIOLET("violet", "Ultraviolet"),
    TOXIC("toxic", "Toxic Lime");

    companion object {
        fun fromId(id: String?): AccentTheme = entries.firstOrNull { it.id == id } ?: CRIMSON
    }
}

enum class SidebarSide(val id: String) {
    LEFT("left"),
    RIGHT("right");

    companion object {
        fun fromId(id: String?): SidebarSide = entries.firstOrNull { it.id == id } ?: LEFT
    }
}

enum class SidebarTool(val id: String, val label: String, val hint: String) {
    FPS("fps", "FPS monitor", "Opens live frame-rate readout"),
    NETWORK("network", "Network", "Live up/down throughput"),
    BRIGHTNESS("brightness", "Brightness", "Adjusts system brightness"),
    VOLUME("volume", "Volume", "Media volume slider"),
    TIMER("timer", "Timer", "Session countdown"),
    SCREENSHOT("screenshot", "Screenshot", "Capture the screen"),
    RECORD("record", "Record", "Start/stop screen recording"),
    DND("dnd", "Do Not Disturb", "Toggle interruption filter"),
    PROFILE("profile", "Profile switch", "Cycle performance profiles"),
    QUICK_SETTINGS("settings", "Quick settings", "Open REDCORE settings");

    companion object {
        fun fromId(id: String?): SidebarTool? = entries.firstOrNull { it.id == id }
        fun parseCsv(csv: String?): List<SidebarTool> =
            csv?.split(',')?.mapNotNull { fromId(it.trim()) }?.distinct().orEmpty()
                .ifEmpty { listOf(FPS, NETWORK, BRIGHTNESS, VOLUME, TIMER, PROFILE) }
        fun toCsv(tools: Collection<SidebarTool>): String = tools.joinToString(",") { it.id }
    }
}

enum class CrosshairDesign(val id: String, val label: String) {
    DOT("dot", "Center dot"),
    CROSS("cross", "Cross"),
    RING("ring", "Ring"),
    RING_DOT("ring_dot", "Ring + dot"),
    T_CROSS("t_cross", "T cross"),
    CHEVRON("chevron", "Chevron"),
    X_MARK("x", "X mark");

    companion object {
        fun fromId(id: String?): CrosshairDesign =
            entries.firstOrNull { it.id == id } ?: RING_DOT
    }
}

enum class AudioPreset(val id: String, val label: String, val description: String, val gains: FloatArray) {
    FLAT("flat", "Flat", "No colouring", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
    COMPETITIVE("competitive", "Competitive", "Footsteps and reloads forward", floatArrayOf(-3f, 1f, 3f, 2f, -1f)),
    CINEMA("cinema", "Cinema", "Deep low end, wide mids", floatArrayOf(4f, 2f, 0f, 1f, 3f)),
    BASS("bass", "Bass boost", "Heavy low frequencies", floatArrayOf(6f, 4f, 0f, -1f, -2f)),
    VOICE("voice", "Voice clarity", "Mids for comms", floatArrayOf(-4f, 0f, 5f, 4f, 1f)),
    BRIGHT("bright", "Bright", "Treble detail", floatArrayOf(-2f, 0f, 1f, 3f, 5f));

    companion object {
        fun fromId(id: String?): AudioPreset = entries.firstOrNull { it.id == id } ?: FLAT
    }
}

enum class VoiceEffect(val id: String, val label: String, val description: String) {
    NONE("none", "None", "Clean passthrough"),
    DEEP("deep", "Deep", "Lower pitch, heavier body"),
    CHIPMUNK("chipmunk", "Chipmunk", "Raised pitch, faster"),
    ROBOT("robot", "Robot", "Ring modulation, metallic"),
    ECHO("echo", "Echo", "Repeating delay tail"),
    RADIO("radio", "Radio", "Band-limited walkie-talkie"),
    ALIEN("alien", "Alien", "Wobbling pitch modulation");

    companion object {
        fun fromId(id: String?): VoiceEffect = entries.firstOrNull { it.id == id } ?: NONE
    }
}
