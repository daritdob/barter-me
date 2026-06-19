package com.example.data

import com.example.BuildConfig
import com.example.data.model.ListingEntity
import com.example.data.model.ProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SmartMatchResult(
    val listings: List<ListingEntity>,
    val usedGemini: Boolean
)

class GeminiMatchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean {
        val key = geminiApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun findSmartMatches(
        profile: ProfileEntity,
        listings: List<ListingEntity>
    ): SmartMatchResult = withContext(Dispatchers.IO) {
        val localMatches = MatchEngine.findComplementaryMatches(profile, listings)
        if (!isConfigured() || localMatches.isEmpty()) {
            return@withContext SmartMatchResult(localMatches, usedGemini = false)
        }

        try {
            val ranked = rankWithGemini(profile, localMatches)
            SmartMatchResult(ranked.ifEmpty { localMatches }, usedGemini = ranked.isNotEmpty())
        } catch (_: Exception) {
            SmartMatchResult(localMatches, usedGemini = false)
        }
    }

    private fun rankWithGemini(profile: ProfileEntity, candidates: List<ListingEntity>): List<ListingEntity> {
        val listingSummary = JSONArray().apply {
            candidates.forEach { listing ->
                put(
                    JSONObject()
                        .put("id", listing.id)
                        .put("owner", listing.ownerName)
                        .put("offers", listing.haveItem)
                        .put("wants", listing.needItem)
                        .put("categoryOffer", listing.categoryHave)
                        .put("categoryWant", listing.categoryNeed)
                )
            }
        }

        val prompt = """
            You are a barter marketplace matchmaker. Given a user profile and candidate listings,
            return ONLY a JSON array of listing ids ordered from best to worst complementary trade match.
            Example: [103, 101, 104]

            User offers: ${profile.skillsOffered}
            User wants: ${profile.skillsNeeded}
            User location: ${profile.locationName}

            Candidates: $listingSummary
        """.trimIndent()

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${geminiApiKey()}"
            )
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val payload = response.body?.string() ?: return emptyList()
            val text = JSONObject(payload)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                .orEmpty()

            val ids = parseIdArray(text)
            if (ids.isEmpty()) return emptyList()

            val byId = candidates.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }
    }

    private fun parseIdArray(text: String): List<Int> {
        val start = text.indexOf('[')
        val end = text.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return text.substring(start, end + 1)
            .removePrefix("[")
            .removeSuffix("]")
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
    }

    private fun geminiApiKey(): String = try {
        BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
        ""
    }
}
