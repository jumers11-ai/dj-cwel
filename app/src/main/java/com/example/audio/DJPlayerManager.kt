package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import com.example.data.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class DJPlayerManager(private val context: Context) {

    private val tag = "DJPlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Media players for Deck A and Deck B
    private var playerA: MediaPlayer? = null
    private var playerB: MediaPlayer? = null

    // Track states
    val trackA = MutableStateFlow<Track?>(null)
    val trackB = MutableStateFlow<Track?>(null)

    // Playback states
    val isPlayingA = MutableStateFlow(false)
    val isPlayingB = MutableStateFlow(false)

    val progressA = MutableStateFlow(0f) // 0.0 to 1.0
    val progressB = MutableStateFlow(0f)

    val positionMsA = MutableStateFlow(0L)
    val positionMsB = MutableStateFlow(0L)

    val durationMsA = MutableStateFlow(0L)
    val durationMsB = MutableStateFlow(0L)

    // Mixer States
    val volumeA = MutableStateFlow(1.0f) // Channel fader A
    val volumeB = MutableStateFlow(1.0f) // Channel fader B
    val crossfader = MutableStateFlow(0.0f) // -1.0f (Left/Deck A only) to 1.0f (Right/Deck B only)

    // EQs (visually simulated and tied to gain reductions in MediaPlayer where possible)
    val eqLowA = MutableStateFlow(1.0f) // 0.0f to 2.0f
    val eqMidA = MutableStateFlow(1.0f)
    val eqHighA = MutableStateFlow(1.0f)

    val eqLowB = MutableStateFlow(1.0f)
    val eqMidB = MutableStateFlow(1.0f)
    val eqHighB = MutableStateFlow(1.0f)

    // Tempo/Pitch speeds
    val tempoA = MutableStateFlow(1.0f) // 0.8f to 1.2f
    val tempoB = MutableStateFlow(1.0f)

    // Sync state
    val isBpmSynced = MutableStateFlow(false)

    // Loops
    val isLoopActiveA = MutableStateFlow(false)
    val isLoopActiveB = MutableStateFlow(false)
    val loopBeatsA = MutableStateFlow(4) // 1, 2, 4, 8, 16 beats
    val loopBeatsB = MutableStateFlow(4)
    var loopStartMsA = 0L
    var loopStartMsB = 0L

    // Hot Cues: index (1-4) to position in ms
    val hotCuesA = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val hotCuesB = MutableStateFlow<Map<Int, Long>>(emptyMap())

    // Auto Mixing Status
    val isAutoMixing = MutableStateFlow(false)
    val autoMixMessage = MutableStateFlow("")

    private var progressJob: Job? = null

    init {
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(100)
                updatePlayerProgress()
            }
        }
    }

    private fun updatePlayerProgress() {
        // Track A progress
        playerA?.let { player ->
            if (isPlayingA.value) {
                try {
                    val pos = player.currentPosition.toLong()
                    val dur = player.duration.toLong()
                    positionMsA.value = pos
                    durationMsA.value = dur
                    if (dur > 0) {
                        progressA.value = pos.toFloat() / dur.toFloat()
                    }

                    // Check Loop A
                    if (isLoopActiveA.value && dur > 0) {
                        val bpm = trackA.value?.bpm ?: 120
                        val beatDurationMs = (60000f / bpm).toLong()
                        val loopLengthMs = beatDurationMs * loopBeatsA.value
                        if (pos >= loopStartMsA + loopLengthMs) {
                            player.seekTo(loopStartMsA.toInt())
                            positionMsA.value = loopStartMsA
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error updating player A progress: ${e.message}")
                }
            }
        }

        // Track B progress
        playerB?.let { player ->
            if (isPlayingB.value) {
                try {
                    val pos = player.currentPosition.toLong()
                    val dur = player.duration.toLong()
                    positionMsB.value = pos
                    durationMsB.value = dur
                    if (dur > 0) {
                        progressB.value = pos.toFloat() / dur.toFloat()
                    }

                    // Check Loop B
                    if (isLoopActiveB.value && dur > 0) {
                        val bpm = trackB.value?.bpm ?: 120
                        val beatDurationMs = (60000f / bpm).toLong()
                        val loopLengthMs = beatDurationMs * loopBeatsB.value
                        if (pos >= loopStartMsB + loopLengthMs) {
                            player.seekTo(loopStartMsB.toInt())
                            positionMsB.value = loopStartMsB
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error updating player B progress: ${e.message}")
                }
            }
        }
    }

    /**
     * Loads a track into Deck A or B.
     */
    fun loadTrack(deck: String, track: Track) {
        scope.launch {
            if (deck == "A") {
                trackA.value = track
                isLoopActiveA.value = false
                hotCuesA.value = emptyMap()
                tempoA.value = 1.0f
                preparePlayer("A", track.audioUrl)
            } else {
                trackB.value = track
                isLoopActiveB.value = false
                hotCuesB.value = emptyMap()
                tempoB.value = 1.0f
                preparePlayer("B", track.audioUrl)
            }
        }
    }

    private suspend fun preparePlayer(deck: String, url: String) = withContext(Dispatchers.IO) {
        try {
            if (deck == "A") {
                playerA?.release()
                playerA = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener { mp ->
                        durationMsA.value = mp.duration.toLong()
                        progressA.value = 0f
                        positionMsA.value = 0
                        applyMixerSettings()
                    }
                    setOnCompletionListener {
                        isPlayingA.value = false
                        progressA.value = 1f
                    }
                    prepare()
                }
            } else {
                playerB?.release()
                playerB = MediaPlayer().apply {
                    setDataSource(url)
                    setOnPreparedListener { mp ->
                        durationMsB.value = mp.duration.toLong()
                        progressB.value = 0f
                        positionMsB.value = 0
                        applyMixerSettings()
                    }
                    setOnCompletionListener {
                        isPlayingB.value = false
                        progressB.value = 1f
                    }
                    prepare()
                }
            }
        } catch (e: IOException) {
            Log.e(tag, "Failed to prepare player $deck: ${e.message}")
        }
    }

    /**
     * Play/Pause control.
     */
    fun togglePlay(deck: String) {
        if (deck == "A") {
            playerA?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    isPlayingA.value = false
                } else {
                    player.start()
                    isPlayingA.value = true
                    applyTempo("A", tempoA.value)
                }
            }
        } else {
            playerB?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    isPlayingB.value = false
                } else {
                    player.start()
                    isPlayingB.value = true
                    applyTempo("B", tempoB.value)
                }
            }
        }
    }

    fun seekTo(deck: String, progress: Float) {
        if (deck == "A") {
            playerA?.let { player ->
                val targetPos = (player.duration * progress).toInt()
                player.seekTo(targetPos)
                positionMsA.value = targetPos.toLong()
                progressA.value = progress
            }
        } else {
            playerB?.let { player ->
                val targetPos = (player.duration * progress).toInt()
                player.seekTo(targetPos)
                positionMsB.value = targetPos.toLong()
                progressB.value = progress
            }
        }
    }

    /**
     * Apply EQ & Volume settings dynamically.
     */
    fun applyMixerSettings() {
        // Crossfader calculation:
        // crossfader value: -1.0f (A 100%, B 0%), 0.0f (A 100%, B 100%), 1.0f (A 0%, B 100%)
        val xVal = crossfader.value
        val cfVolA = Math.min(1.0f, 1.0f - xVal)
        val cfVolB = Math.min(1.0f, 1.0f + xVal)

        // EQ reduction affects master player volume as a robust fallback
        // Bass cut creates a dramatic effect in modern electronic DJ mixes
        val eqFactorA = (eqLowA.value * 0.5f + eqMidA.value * 0.3f + eqHighA.value * 0.2f)
        val eqFactorB = (eqLowB.value * 0.5f + eqMidB.value * 0.3f + eqHighB.value * 0.2f)

        val finalVolA = volumeA.value * cfVolA * eqFactorA
        val finalVolB = volumeB.value * cfVolB * eqFactorB

        try {
            playerA?.setVolume(finalVolA, finalVolA)
        } catch (e: Exception) {
            Log.e(tag, "Error setting player A volume: ${e.message}")
        }

        try {
            playerB?.setVolume(finalVolB, finalVolB)
        } catch (e: Exception) {
            Log.e(tag, "Error setting player B volume: ${e.message}")
        }
    }

    fun setCrossfader(value: Float) {
        crossfader.value = value
        applyMixerSettings()
    }

    fun setVolume(deck: String, value: Float) {
        if (deck == "A") {
            volumeA.value = value
        } else {
            volumeB.value = value
        }
        applyMixerSettings()
    }

    fun setEQ(deck: String, band: String, value: Float) {
        if (deck == "A") {
            when (band) {
                "low" -> eqLowA.value = value
                "mid" -> eqMidA.value = value
                "high" -> eqHighA.value = value
            }
        } else {
            when (band) {
                "low" -> eqLowB.value = value
                "mid" -> eqMidB.value = value
                "high" -> eqHighB.value = value
            }
        }
        applyMixerSettings()
    }

    /**
     * Pitch/Tempo Speed controls.
     */
    fun setTempo(deck: String, value: Float) {
        if (deck == "A") {
            tempoA.value = value
            applyTempo("A", value)
        } else {
            tempoB.value = value
            applyTempo("B", value)
        }
    }

    private fun applyTempo(deck: String, speed: Float) {
        val player = if (deck == "A") playerA else playerB
        player?.let {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val params = PlaybackParams()
                    params.speed = speed
                    it.playbackParams = params
                }
            } catch (e: Exception) {
                Log.e(tag, "Error setting speed: ${e.message}")
            }
        }
    }

    /**
     * Sync BPM: adjusts target deck's tempo to perfectly match source deck's BPM!
     */
    fun syncBpm(sourceDeck: String) {
        val trA = trackA.value ?: return
        val trB = trackB.value ?: return

        if (sourceDeck == "A") {
            // Match Deck B's BPM to Deck A's BPM
            val ratio = trA.bpm.toFloat() / trB.bpm.toFloat()
            tempoB.value = ratio
            applyTempo("B", ratio)
            isBpmSynced.value = true
        } else {
            // Match Deck A's BPM to Deck B's BPM
            val ratio = trB.bpm.toFloat() / trA.bpm.toFloat()
            tempoA.value = ratio
            applyTempo("A", ratio)
            isBpmSynced.value = true
        }
    }

    /**
     * Hot Cues logic.
     */
    fun setHotCue(deck: String, index: Int) {
        if (deck == "A") {
            playerA?.let {
                val current = it.currentPosition.toLong()
                val updated = hotCuesA.value.toMutableMap()
                updated[index] = current
                hotCuesA.value = updated
            }
        } else {
            playerB?.let {
                val current = it.currentPosition.toLong()
                val updated = hotCuesB.value.toMutableMap()
                updated[index] = current
                hotCuesB.value = updated
            }
        }
    }

    fun triggerHotCue(deck: String, index: Int) {
        if (deck == "A") {
            hotCuesA.value[index]?.let { pos ->
                playerA?.seekTo(pos.toInt())
                positionMsA.value = pos
                if (playerA?.isPlaying == false) {
                    togglePlay("A")
                }
            }
        } else {
            hotCuesB.value[index]?.let { pos ->
                playerB?.seekTo(pos.toInt())
                positionMsB.value = pos
                if (playerB?.isPlaying == false) {
                    togglePlay("B")
                }
            }
        }
    }

    /**
     * Loops control.
     */
    fun toggleLoop(deck: String, beats: Int) {
        if (deck == "A") {
            val active = !isLoopActiveA.value
            isLoopActiveA.value = active
            loopBeatsA.value = beats
            if (active) {
                loopStartMsA = playerA?.currentPosition?.toLong() ?: 0L
            }
        } else {
            val active = !isLoopActiveB.value
            isLoopActiveB.value = active
            loopBeatsB.value = beats
            if (active) {
                loopStartMsB = playerB?.currentPosition?.toLong() ?: 0L
            }
        }
    }

    /**
     * ANIMATED AUTOMATIC DJ MIX ("Mix Maliny" AI Auto Mixer)
     * Performs a professional transition from Deck A to Deck B (or B to A).
     * Smoothly syncs BPMs, fades volume, crosses equalizers (swaps bass!), and shifts crossfader.
     */
    fun executeAIAutoMixTransition(fromDeck: String) {
        if (isAutoMixing.value) return

        scope.launch {
            isAutoMixing.value = true
            val toDeck = if (fromDeck == "A") "B" else "A"
            val fromTrack = if (fromDeck == "A") trackA.value else trackB.value
            val toTrack = if (fromDeck == "A") trackB.value else trackA.value

            if (fromTrack == null || toTrack == null) {
                autoMixMessage.value = "Bląd: Wymagane są 2 utwory do miksowania!"
                delay(2000)
                isAutoMixing.value = false
                return@launch
            }

            autoMixMessage.value = "Krok 1/4: Synchronizacja tempa (BPM) i klucza..."
            delay(1500)

            // 1. Sync BPM
            syncBpm(fromDeck)
            delay(1000)

            autoMixMessage.value = "Krok 2/4: Przygotowanie i start Decku $toDeck..."
            
            // 2. Start target deck playing
            val toPlayer = if (toDeck == "A") playerA else playerB
            if (toPlayer == null) {
                autoMixMessage.value = "Błąd: Deck docelowy nie jest gotowy."
                delay(2000)
                isAutoMixing.value = false
                return@launch
            }

            // Seek target to 0 or start point
            toPlayer.seekTo(0)
            toPlayer.start()
            if (toDeck == "A") isPlayingA.value = true else isPlayingB.value = true
            
            // Set initial volume for target deck
            if (toDeck == "A") {
                volumeA.value = 1.0f
                eqLowA.value = 0.0f // Start with cut bass for smooth transition
                eqMidA.value = 0.8f
                eqHighA.value = 0.9f
            } else {
                volumeB.value = 1.0f
                eqLowB.value = 0.0f // Cut bass on Deck B
                eqMidB.value = 0.8f
                eqHighB.value = 0.9f
            }
            applyMixerSettings()
            delay(1500)

            // 3. Smooth transition loop: 15 steps over 6 seconds
            autoMixMessage.value = "Krok 3/4: Automatyczne płynne przejście ('Mix Malina')..."
            val steps = 20
            val startCrossfader = if (fromDeck == "A") -1.0f else 1.0f
            val endCrossfader = if (fromDeck == "A") 1.0f else -1.0f

            for (i in 1..steps) {
                val t = i.toFloat() / steps.toFloat()
                
                // Animate crossfader
                val currentX = startCrossfader + t * (endCrossfader - startCrossfader)
                crossfader.value = currentX

                // Cross-swap EQ Bass (Swap low frequencies)
                if (fromDeck == "A") {
                    eqLowA.value = 1.0f - t      // Fading out Deck A bass
                    eqLowB.value = t             // Fading in Deck B bass
                    eqMidA.value = 1.0f - (t * 0.4f)
                    eqMidB.value = 0.8f + (t * 0.2f)
                } else {
                    eqLowB.value = 1.0f - t
                    eqLowA.value = t
                    eqMidB.value = 1.0f - (t * 0.4f)
                    eqMidA.value = 0.8f + (t * 0.2f)
                }

                applyMixerSettings()
                delay(300) // Total transition time: ~6 seconds
            }

            autoMixMessage.value = "Krok 4/4: Wyłączanie Decku $fromDeck i przywracanie EQ..."
            delay(1000)

            // Stop source player
            if (fromDeck == "A") {
                playerA?.pause()
                isPlayingA.value = false
                // Reset EQs for next use
                eqLowA.value = 1.0f
                eqMidA.value = 1.0f
                eqHighA.value = 1.0f
            } else {
                playerB?.pause()
                isPlayingB.value = false
                eqLowB.value = 1.0f
                eqMidB.value = 1.0f
                eqHighB.value = 1.0f
            }

            // Lock crossfader to final deck
            crossfader.value = endCrossfader
            applyMixerSettings()

            autoMixMessage.value = "Sukces! Płynne przejście ukończone perfekcyjnie!"
            delay(2000)
            isAutoMixing.value = false
        }
    }

    fun release() {
        progressJob?.cancel()
        playerA?.release()
        playerB?.release()
        playerA = null
        playerB = null
    }
}
