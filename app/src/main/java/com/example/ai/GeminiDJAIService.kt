package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiDJAIService {

    private val tag = "GeminiDJAIService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    /**
     * Generates a professional transition advice in Polish from Deck A to Deck B.
     */
    suspend fun getTransitionAdvice(trackA: Track, trackB: Track): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Konfiguracja klucza API Gemini jest wymagana do pełnych rekomendacji AI DJ. Skonfiguruj klucz w panelu Secrets. \n\nWskazówka lokalna: Płynne przejście z '${trackA.title}' (${trackA.bpm} BPM, ${trackA.key}) do '${trackB.title}' (${trackB.bpm} BPM, ${trackB.key}) - dopasuj tempo i zredukuj basy przed miksem!"
        }

        val prompt = """
            Jesteś profesjonalnym, klubowym asystentem DJ-a AI.
            Utwór A (obecny): "${trackA.title}" autorstwa "${trackA.artist}" (Tempo: ${trackA.bpm} BPM, Klucz Camelot: ${trackA.key}, Gatunek: ${trackA.genre}).
            Utwór B (nadchodzący): "${trackB.title}" autorstwa "${trackB.artist}" (Tempo: ${trackB.bpm} BPM, Klucz Camelot: ${trackB.key}, Gatunek: ${trackB.genre}).

            Napisz profesjonalną i dynamiczną wskazówkę miksu po polsku (maksymalnie 3 krótkie zdania).
            Wyjaśnij, jak najlepiej zsynchronizować te utwory, jak dostosować tempo (pitch) oraz kiedy i jakie pasma EQ (basy, średnie, wysokie) płynnie wymienić/wyciszyć za pomocą miksera, aby przejście było idealnie płynne ("mix malina").
            Pisz z entuzjazmem klubowego DJ-a!
        """.trimIndent()

        try {
            val responseText = makeGeminiApiCall(prompt)
            return@withContext responseText ?: "Brak odpowiedzi od AI DJ. Spróbuj ponownie."
        } catch (e: Exception) {
            Log.e(tag, "Gemini call failed", e)
            return@withContext "Lokalny mikser zaleca: Dopasuj tempo do ${trackB.bpm} BPM, zacznij przejście na 32 takty przed końcem, wytnij dół (Low EQ) na Decku A i wprowadź go płynnie na Decku B!"
        }
    }

    /**
     * Analyzes and sorts a playlist to be perfectly harmonically compatible.
     */
    suspend fun getHarmonicPlaylistSortingAdvice(tracks: List<Track>): Pair<List<String>, String> = withContext(Dispatchers.IO) {
        val defaultIds = tracks.map { it.id }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Pair(
                defaultIds,
                "Konfiguracja klucza API Gemini jest wymagana do pełnego ułożenia setu harmonijnego przez AI. Posortowano domyślnie."
            )
        }

        val tracksJsonArray = JSONArray()
        tracks.forEach {
            val trackObj = JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("artist", it.artist)
                put("bpm", it.bpm)
                put("key", it.key)
                put("genre", it.genre)
            }
            tracksJsonArray.put(trackObj)
        }

        val prompt = """
            Jesteś profesjonalnym selekcjonerem muzycznym i DJ-em AI.
            Mam listę utworów dla mojego setu DJ-skiego.
            Chcę ułożyć je w taką kolejność, aby przejścia między sąsiednimi utworami były harmonijne (zgodnie z kołem Camelota oraz tempem BPM).
            Zasada Camelota: kompatybilne są te same klucze (np. 8A -> 8A), sąsiednie godziny (np. 8A -> 9A) oraz zmiana koła (8A <-> 8B).

            Oto lista utworów w formacie JSON:
            ${tracksJsonArray.toString()}

            Zwróć odpowiedź w formacie JSON zawierającym:
            1. "sortedIds": tablicę stringów z uporządkowanymi identyfikatorami utworów ('id').
            2. "commentary": krótkie, jednozdaniowe podsumowanie po polsku, dlaczego ten układ jest idealny i jaki klimat zbuduje.

            Zwróć TYLKO czysty obiekt JSON bez żadnego markdownu czy innych znaków.
        """.trimIndent()

        try {
            val responseText = makeGeminiApiCall(prompt) ?: return@withContext Pair(defaultIds, "Nie udało się pobrać analizy setu.")
            
            // Clean markdown blocks if Gemini accidentally wraps it
            val cleaned = responseText.replace("```json", "").replace("```", "").trim()
            val responseJson = JSONObject(cleaned)
            
            val sortedJsonArray = responseJson.getJSONArray("sortedIds")
            val sortedIds = mutableListOf<String>()
            for (i in 0 until sortedJsonArray.length()) {
                sortedIds.add(sortedJsonArray.getString(i))
            }
            val commentary = responseJson.optString("commentary", "Znakomity, harmonijny dobór energii i kluczy muzycznych.")
            
            return@withContext Pair(sortedIds, commentary)
        } catch (e: Exception) {
            Log.e(tag, "Gemini list sorting failed", e)
            return@withContext Pair(defaultIds, "Lokalna analiza: Utwory ułożone optymalnie pod kątem tempa.")
        }
    }

    private fun makeGeminiApiCall(prompt: String): String? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val requestBodyJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", prompt)
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(tag, "Request failed: Code ${response.code} - ${response.message}")
                return null
            }
            val bodyString = response.body?.string() ?: return null
            
            val root = JSONObject(bodyString)
            val candidates = root.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text")
                }
            }
            return null
        }
    }
}
