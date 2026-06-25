package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Track
import kotlinx.coroutines.launch

// Custom Theme Colors for DJ Console Vibe
val ThemeDarkBg = Color(0xFF0C0E12)
val ThemeSurfaceDark = Color(0xFF141822)
val ThemeDeckABlue = Color(0xFF00E5FF)
val ThemeDeckBMagenta = Color(0xFFE040FB)
val ThemeMixerOrange = Color(0xFFFFAB40)
val ThemeTerminalBg = Color(0xFF070B11)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DJScreen(
    viewModel: DJViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val playlist by viewModel.playlist.collectAsStateWithLifecycle()
    val inputUrl by viewModel.inputUrl.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()

    val adviceText by viewModel.adviceText.collectAsStateWithLifecycle()
    val adviceLoading by viewModel.adviceLoading.collectAsStateWithLifecycle()
    val playlistCommentary by viewModel.playlistCommentary.collectAsStateWithLifecycle()

    // Player States
    val playerManager = viewModel.playerManager
    val trackA by playerManager.trackA.collectAsStateWithLifecycle()
    val trackB by playerManager.trackB.collectAsStateWithLifecycle()

    val isPlayingA by playerManager.isPlayingA.collectAsStateWithLifecycle()
    val isPlayingB by playerManager.isPlayingB.collectAsStateWithLifecycle()

    val progressA by playerManager.progressA.collectAsStateWithLifecycle()
    val progressB by playerManager.progressB.collectAsStateWithLifecycle()

    val posMsA by playerManager.positionMsA.collectAsStateWithLifecycle()
    val posMsB by playerManager.positionMsB.collectAsStateWithLifecycle()

    val durMsA by playerManager.durationMsA.collectAsStateWithLifecycle()
    val durMsB by playerManager.durationMsB.collectAsStateWithLifecycle()

    val volumeA by playerManager.volumeA.collectAsStateWithLifecycle()
    val volumeB by playerManager.volumeB.collectAsStateWithLifecycle()
    val crossfader by playerManager.crossfader.collectAsStateWithLifecycle()

    val tempoA by playerManager.tempoA.collectAsStateWithLifecycle()
    val tempoB by playerManager.tempoB.collectAsStateWithLifecycle()

    val eqLowA by playerManager.eqLowA.collectAsStateWithLifecycle()
    val eqMidA by playerManager.eqMidA.collectAsStateWithLifecycle()
    val eqHighA by playerManager.eqHighA.collectAsStateWithLifecycle()

    val eqLowB by playerManager.eqLowB.collectAsStateWithLifecycle()
    val eqMidB by playerManager.eqMidB.collectAsStateWithLifecycle()
    val eqHighB by playerManager.eqHighB.collectAsStateWithLifecycle()

    val loopActiveA by playerManager.isLoopActiveA.collectAsStateWithLifecycle()
    val loopActiveB by playerManager.isLoopActiveB.collectAsStateWithLifecycle()
    val loopBeatsA by playerManager.loopBeatsA.collectAsStateWithLifecycle()
    val loopBeatsB by playerManager.loopBeatsB.collectAsStateWithLifecycle()

    val hotCuesA by playerManager.hotCuesA.collectAsStateWithLifecycle()
    val hotCuesB by playerManager.hotCuesB.collectAsStateWithLifecycle()

    val isAutoMixing by playerManager.isAutoMixing.collectAsStateWithLifecycle()
    val autoMixMessage by playerManager.autoMixMessage.collectAsStateWithLifecycle()

    // Toast feedback for import status
    LaunchedEffect(importStatus) {
        importStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearImportStatus()
        }
    }

    // Auto load first tracks if empty
    LaunchedEffect(playlist) {
        if (playlist.isNotEmpty()) {
            if (trackA == null) {
                viewModel.loadTrackToDeck("A", playlist[0])
            }
            if (trackB == null && playlist.size > 1) {
                viewModel.loadTrackToDeck("B", playlist[1])
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicVideo,
                            contentDescription = null,
                            tint = ThemeDeckABlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "DJ AI AutoMixer",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(ThemeMixerOrange.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, ThemeMixerOrange.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AI ACTIVE",
                                fontSize = 10.sp,
                                color = ThemeMixerOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeDarkBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = ThemeDarkBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // AI AUTOMIX PULSING STATUS BANNER
            item {
                AnimatedVisibility(
                    visible = isAutoMixing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0A22)),
                        border = BorderStroke(1.dp, ThemeDeckBMagenta),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, spotColor = ThemeDeckBMagenta)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = ThemeDeckBMagenta,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Column {
                                Text(
                                    text = "TRANSICJA AI W TOKU 🎧",
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeDeckBMagenta,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = autoMixMessage,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // DUAL DECKS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // DECK A (LEFT - CYAN Theme)
                    Box(modifier = Modifier.weight(1f)) {
                        DeckComponent(
                            deckName = "A",
                            track = trackA,
                            isPlaying = isPlayingA,
                            progress = progressA,
                            positionMs = posMsA,
                            durationMs = durMsA,
                            tempo = tempoA,
                            loopActive = loopActiveA,
                            loopBeats = loopBeatsA,
                            hotCues = hotCuesA,
                            themeColor = ThemeDeckABlue,
                            onPlayPause = { playerManager.togglePlay("A") },
                            onSeek = { progress -> playerManager.seekTo("A", progress) },
                            onSync = { playerManager.syncBpm("B") }, // Sync with B
                            onLoop = { beats -> playerManager.toggleLoop("A", beats) },
                            onSetCue = { idx -> playerManager.setHotCue("A", idx) },
                            onTriggerCue = { idx -> playerManager.triggerHotCue("A", idx) },
                            onPitchChange = { pitch -> playerManager.setTempo("A", pitch) }
                        )
                    }

                    // DECK B (RIGHT - MAGENTA Theme)
                    Box(modifier = Modifier.weight(1f)) {
                        DeckComponent(
                            deckName = "B",
                            track = trackB,
                            isPlaying = isPlayingB,
                            progress = progressB,
                            positionMs = posMsB,
                            durationMs = durMsB,
                            tempo = tempoB,
                            loopActive = loopActiveB,
                            loopBeats = loopBeatsB,
                            hotCues = hotCuesB,
                            themeColor = ThemeDeckBMagenta,
                            onPlayPause = { playerManager.togglePlay("B") },
                            onSeek = { progress -> playerManager.seekTo("B", progress) },
                            onSync = { playerManager.syncBpm("A") }, // Sync with A
                            onLoop = { beats -> playerManager.toggleLoop("B", beats) },
                            onSetCue = { idx -> playerManager.setHotCue("B", idx) },
                            onTriggerCue = { idx -> playerManager.triggerHotCue("B", idx) },
                            onPitchChange = { pitch -> playerManager.setTempo("B", pitch) }
                        )
                    }
                }
            }

            // CENTRAL MIXER PANEL (EQ, Volume, Crossfader)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "KONSOLA MIKSERA",
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // EQ Deck A
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("EQ DECK A", color = ThemeDeckABlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                EqSlider(label = "HI", value = eqHighA, color = ThemeDeckABlue) { playerManager.setEQ("A", "high", it) }
                                EqSlider(label = "MID", value = eqMidA, color = ThemeDeckABlue) { playerManager.setEQ("A", "mid", it) }
                                EqSlider(label = "LOW", value = eqLowA, color = ThemeDeckABlue) { playerManager.setEQ("A", "low", it) }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("VOLUME A", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Slider(
                                    value = volumeA,
                                    onValueChange = { playerManager.setVolume("A", it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = ThemeDeckABlue,
                                        activeTrackColor = ThemeDeckABlue.copy(alpha = 0.8f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }

                            Divider(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .padding(vertical = 12.dp)
                            )

                            // EQ Deck B
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("EQ DECK B", color = ThemeDeckBMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                EqSlider(label = "HI", value = eqHighB, color = ThemeDeckBMagenta) { playerManager.setEQ("B", "high", it) }
                                EqSlider(label = "MID", value = eqMidB, color = ThemeDeckBMagenta) { playerManager.setEQ("B", "mid", it) }
                                EqSlider(label = "LOW", value = eqLowB, color = ThemeDeckBMagenta) { playerManager.setEQ("B", "low", it) }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("VOLUME B", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                Slider(
                                    value = volumeB,
                                    onValueChange = { playerManager.setVolume("B", it) },
                                    colors = SliderDefaults.colors(
                                        thumbColor = ThemeDeckBMagenta,
                                        activeTrackColor = ThemeDeckBMagenta.copy(alpha = 0.8f),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // CROSSFADER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DECK A", color = ThemeDeckABlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("CROSSFADER", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                Text("DECK B", color = ThemeDeckBMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = crossfader,
                                onValueChange = { playerManager.setCrossfader(it) },
                                valueRange = -1.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ThemeMixerOrange,
                                    activeTrackColor = ThemeMixerOrange.copy(alpha = 0.8f),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("crossfader")
                            )
                        }
                    }
                }
            }

            // AI DJ CO-PILOT ADVICE & AUTOMIX BUTTON
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeTerminalBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ThemeMixerOrange.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = ThemeMixerOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "LIVE AI DJ CO-PILOT",
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeMixerOrange,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = { viewModel.generateAITransitionAdvice() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                if (adviceLoading) {
                                    CircularProgressIndicator(color = ThemeMixerOrange, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Advice", tint = ThemeMixerOrange, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = adviceText.ifEmpty { "Wybierz dwa utwory z biblioteki i kliknij przycisk odświeżenia powyżej, aby wygenerować rekomendację miksu!" },
                            color = Color(0xFF90A4AE),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // CORE AUTO MIX TRIGGER BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { playerManager.executeAIAutoMixTransition("A") },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeDeckABlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("automix_a_to_b_button"),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isAutoMixing && trackA != null && trackB != null
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Mix: A ➔ B", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { playerManager.executeAIAutoMixTransition("B") },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeDeckBMagenta),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("automix_b_to_a_button"),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isAutoMixing && trackA != null && trackB != null
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Mix: B ➔ A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // PLAYLIST IMPORT & MUSIC LIBRARY PANEL
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemeSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "IMPORTOWANIE PLAYLISTY (SPOTIFY / YOUTUBE)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { viewModel.setInputUrl(it) },
                                placeholder = { Text("Wklej link do playlisty Spotify / YT...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemeMixerOrange,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("playlist_url_input")
                            )

                            Button(
                                onClick = { viewModel.importPlaylistUrl(inputUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeMixerOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                enabled = inputUrl.isNotEmpty() && !isLoading,
                                modifier = Modifier.testTag("playlist_import_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Import")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BIBLIOTEKA UTWORÓW (${playlist.size})",
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )

                            Button(
                                onClick = { viewModel.sortPlaylistHarmonically() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                border = BorderStroke(1.dp, ThemeMixerOrange),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                enabled = !isLoading,
                                modifier = Modifier.testTag("harmonic_sort_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ThemeMixerOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Harmonijny AI", color = ThemeMixerOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (playlistCommentary.isNotEmpty()) {
                            Text(
                                text = playlistCommentary,
                                fontSize = 11.sp,
                                color = ThemeMixerOrange,
                                style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Medium),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // TRACKS LIST
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            playlist.forEach { track ->
                                TrackRow(
                                    track = track,
                                    currentAId = trackA?.id,
                                    currentBId = trackB?.id,
                                    onLoadA = { viewModel.loadTrackToDeck("A", track) },
                                    onLoadB = { viewModel.loadTrackToDeck("B", track) }
                                )
                            }
                        }
                    }
                }
            }

            // FOOTER / SAFETY PROTOCOL EXPLANATION
            item {
                Text(
                    text = "Gwarancja wyszukiwania idealnych przejść: Mikser wykonuje matematyczne zestrojenie fazy, automatyczne wyrównanie BPM (Pitch speed-matching) oraz harmoniczne wyciszenie basu. DJ AI zasilany przez Gemini 3.5 Flash.",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun EqSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.8f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%.1f", value),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
fun DeckComponent(
    deckName: String,
    track: Track?,
    isPlaying: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    tempo: Float,
    loopActive: Boolean,
    loopBeats: Int,
    hotCues: Map<Int, Long>,
    themeColor: Color,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSync: () -> Unit,
    onLoop: (Int) -> Unit,
    onSetCue: (Int) -> Unit,
    onTriggerCue: (Int) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    // Vinyl Spinning Animation Angle
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpinning")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val currentAngle = if (isPlaying) angle else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = ThemeSurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, themeColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, spotColor = themeColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Deck Indicator & Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DECK $deckName",
                    fontWeight = FontWeight.Bold,
                    color = themeColor,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )

                // Sync button
                Box(
                    modifier = Modifier
                        .background(themeColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, themeColor.copy(alpha = 0.6f), CircleShape)
                        .clickable { onSync() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SYNC",
                        fontSize = 9.sp,
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vinyl Record Art Drawing
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .rotate(currentAngle),
                contentAlignment = Alignment.Center
            ) {
                // Draws a realistic shiny vinyl record groove
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Base black vinyl disc
                    drawCircle(color = Color(0xFF111111), radius = radius)

                    // Concentric record grooves
                    for (i in 1..8) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = radius * (0.3f + 0.08f * i),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Colored label center
                    drawCircle(color = themeColor, radius = radius * 0.3f)
                    drawCircle(color = Color.Black, radius = radius * 0.2f)

                    // Spindle hole
                    drawCircle(color = Color.White, radius = radius * 0.06f)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Track Details
            Text(
                text = track?.title ?: "Brak utworu",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = track?.artist ?: "Załaduj z biblioteki",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // BPM & KEY BADGES
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val actualBpm = if (track != null) (track.bpm * tempo).toInt() else 0
                    Text(
                        text = "$actualBpm BPM",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, themeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track?.key ?: "--",
                        color = themeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Seek Slider and Time Display
            Slider(
                value = progress,
                onValueChange = onSeek,
                colors = SliderDefaults.colors(
                    thumbColor = themeColor,
                    activeTrackColor = themeColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val remSec = if (durationMs > 0) (durationMs - positionMs) / 1000 else 0
                val totalMin = remSec / 60
                val totalSec = remSec % 60
                val elapsedSec = positionMs / 1000
                val elapsedMin = elapsedSec / 60
                val elapsedSecRem = elapsedSec % 60

                Text(
                    text = String.format("%02d:%02d", elapsedMin, elapsedSecRem),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format("-%02d:%02d", totalMin, totalSec),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback and Loop Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button (Large Neon Filled)
                IconButton(
                    onClick = { onPlayPause() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isPlaying) themeColor else Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                        .testTag("deck_${deckName.lowercase()}_play_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isPlaying) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Loop Toggle Button
                IconButton(
                    onClick = { onLoop(4) }, // Standard 4-beat loop
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (loopActive) ThemeMixerOrange else Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = "Toggle Loop",
                        tint = if (loopActive) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // PITCH / SPEED ADJUSTMENT SLIDER (-20% to +20%)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PITCH", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = tempo,
                    onValueChange = onPitchChange,
                    valueRange = 0.8f..1.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = themeColor,
                        activeTrackColor = themeColor.copy(alpha = 0.6f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .padding(horizontal = 6.dp)
                )
                Text(
                    text = String.format("%+.1f%%", (tempo - 1f) * 100f),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // HOT CUES (1-4 Grid)
            Text(
                text = "HOT CUES (Hold to set, click to recall)",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (cueIndex in 1..4) {
                    val cueSaved = hotCues.containsKey(cueIndex)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(
                                if (cueSaved) themeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (cueSaved) themeColor else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { onSetCue(cueIndex) },
                                    onTap = { onTriggerCue(cueIndex) }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C$cueIndex",
                            fontSize = 10.sp,
                            color = if (cueSaved) themeColor else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    currentAId: String?,
    currentBId: String?,
    onLoadA: () -> Unit,
    onLoadB: () -> Unit
) {
    val isLoadedOnA = currentAId == track.id
    val isLoadedOnB = currentBId == track.id

    Card(
        colors = CardDefaults.cardColors(containerColor = ThemeSurfaceDark.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isLoadedOnA) ThemeDeckABlue else if (isLoadedOnB) ThemeDeckBMagenta else Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Meta
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = track.genre,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            // BPM, Key, Time
            Column(
                modifier = Modifier.weight(0.7f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${track.bpm} BPM",
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.key,
                        fontWeight = FontWeight.Bold,
                        color = ThemeMixerOrange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = track.durationString,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Load buttons
            Row(
                modifier = Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Load A
                Button(
                    onClick = onLoadA,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoadedOnA) ThemeDeckABlue else Color.Black
                    ),
                    border = BorderStroke(1.dp, ThemeDeckABlue),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Text(
                        text = "DECK A",
                        color = if (isLoadedOnA) Color.Black else ThemeDeckABlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Load B
                Button(
                    onClick = onLoadB,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoadedOnB) ThemeDeckBMagenta else Color.Black
                    ),
                    border = BorderStroke(1.dp, ThemeDeckBMagenta),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Text(
                        text = "DECK B",
                        color = if (isLoadedOnB) Color.White else ThemeDeckBMagenta,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
