package com.example.data

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Int,
    val key: String, // Camelot Key system, e.g., "8A", "9A", "5B"
    val durationSec: Int,
    val audioUrl: String,
    val imageUrl: String,
    val genre: String,
    val isUserImported: Boolean = false
) {
    val durationString: String
        get() {
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}

object DefaultTracks {
    val list = listOf(
        Track(
            id = "track_1",
            title = "Neon Horizon",
            artist = "Aether Shift",
            bpm = 108,
            key = "8A", // A Minor
            durationSec = 372,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
            genre = "Synthwave"
        ),
        Track(
            id = "track_2",
            title = "Cyberpunk Pulse",
            artist = "Vortex 9",
            bpm = 115,
            key = "9A", // E Minor
            durationSec = 423,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            imageUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=400",
            genre = "Electro House"
        ),
        Track(
            id = "track_3",
            title = "Digital Dream",
            artist = "Lofi Core",
            bpm = 122,
            key = "10A", // B Minor
            durationSec = 344,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400",
            genre = "Deep Tech"
        ),
        Track(
            id = "track_4",
            title = "Sublime Velvet",
            artist = "Silk Resonance",
            bpm = 98,
            key = "8B", // C Major (harmonically compatible with 8A/9B)
            durationSec = 302,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
            genre = "Chillout / Lounge"
        ),
        Track(
            id = "track_5",
            title = "Solar Flare",
            artist = "Nova Sector",
            bpm = 128,
            key = "11A", // F# Minor
            durationSec = 361,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
            genre = "Future Techno"
        ),
        Track(
            id = "track_6",
            title = "Velocity Shifter",
            artist = "Pulse Driver",
            bpm = 135,
            key = "12A", // C# Minor
            durationSec = 385,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            imageUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=400",
            genre = "PsyTrance / Uplifting"
        )
    )

    /**
     * Determines whether two keys are harmonically compatible using the Camelot Wheel.
     * Compatible keys are:
     * - The same key (e.g. 8A -> 8A)
     * - Adjecent hours on the wheel (+1 or -1, e.g. 8A -> 9A or 8A -> 7A)
     * - Inner/outer circle swap (A -> B or B -> A of same number, e.g. 8A -> 8B)
     */
    fun areKeysCompatible(key1: String, key2: String): Boolean {
        if (key1 == key2) return true
        
        val num1 = key1.dropLast(1).toIntOrNull() ?: return false
        val letter1 = key1.last()
        val num2 = key2.dropLast(1).toIntOrNull() ?: return false
        val letter2 = key2.last()

        if (letter1 == letter2) {
            val diff = Math.abs(num1 - num2)
            return diff == 1 || diff == 11 // 12-hour wrap around (12 <-> 1)
        } else if (num1 == num2) {
            return true // A <-> B swap of same number
        }
        return false
    }

    /**
     * Suggests a pitch transition (how much to adjust speed of Track B to match Track A).
     */
    fun calculateBpmShiftPercent(bpmSource: Int, bpmTarget: Int): Float {
        return ((bpmTarget - bpmSource).toFloat() / bpmSource) * 100f
    }
}
