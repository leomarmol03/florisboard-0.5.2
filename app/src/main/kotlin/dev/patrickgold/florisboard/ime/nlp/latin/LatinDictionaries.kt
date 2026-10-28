/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.nlp.latin

import android.content.Context
import dev.patrickgold.florisboard.R
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.florisboard.lib.android.readText

/**
 * A word list which is bundled with the app and which can be selected by the user.
 *
 * @property language The language code of the word list, e.g. "en".
 * @property labelRes The string resource which holds the name of the language.
 */
data class AvailableDictionary(
    val language: String,
    val labelRes: Int,
)

/**
 * Loads and caches the word lists which are used by the [LatinLanguageProvider].
 *
 * Currently, word lists for the english and the spanish language are bundled with the app. Dictionaries are loaded
 * lazily and are kept in a small cache, so that only the languages which are actually in use occupy memory.
 */
object LatinDictionaries {
    /**
     * The word lists which are bundled with the app. The user can choose which of them are used for the word
     * suggestions and the spell corrections.
     */
    val AvailableDictionaries = listOf(
        AvailableDictionary("en", R.string.settings__suggestion__language__english),
        AvailableDictionary("es", R.string.settings__suggestion__language__spanish),
    )

    /**
     * The asset which contains the english word list as a map of words and their frequency weights.
     */
    private const val EnglishAsset = "ime/dict/data.json"

    /**
     * The asset which contains the spanish word list, ordered by descending frequency.
     */
    private const val SpanishAsset = "ime/dict/es.txt"

    /**
     * The language which is used as a fallback if no dictionary is available for the active language.
     */
    const val FallbackLanguage = "en"

    private val cache = Cache.Builder().build<String, LatinDictionary>()

    /**
     * Returns the dictionary for the given [language] or [LatinDictionary.EMPTY] if no dictionary is available.
     *
     * @param context The context used to access the application assets.
     * @param language The language code of the requested dictionary, e.g. "en" or "es".
     */
    suspend fun get(context: Context, language: String): LatinDictionary {
        val key = language.lowercase()
        return cache.get(key) {
            withContext(Dispatchers.IO) { load(context, key) }
        }
    }

    /**
     * Returns the dictionary for the given [language], if it has already been loaded, or null otherwise.
     *
     * In contrast to [get], this method does not load a word list and can therefore be called from a thread which
     * must not block for a long time.
     *
     * @param language The language code of the requested dictionary, e.g. "en" or "es".
     */
    fun getIfLoaded(language: String): LatinDictionary? {
        return cache.get(language.lowercase())
    }

    /**
     * Parses the given [value] of the preference which holds the enabled languages.
     *
     * @param value The raw value of the preference, e.g. "en,es".
     * @return The set of the enabled language codes.
     */
    fun parseLanguages(value: String): Set<String> {
        return value.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun load(context: Context, language: String): LatinDictionary {
        return try {
            when (language) {
                "en" -> loadEnglish(context)
                "es" -> loadSpanish(context)
                else -> LatinDictionary.EMPTY
            }
        } catch (_: Throwable) {
            LatinDictionary.EMPTY
        }
    }

    private fun loadEnglish(context: Context): LatinDictionary {
        val rawData = context.assets.readText(EnglishAsset)
        val jsonData = Json.decodeFromString(
            MapSerializer(String.serializer(), Int.serializer()),
            rawData,
        )
        return LatinDictionary.fromWordFrequencies(jsonData)
    }

    private fun loadSpanish(context: Context): LatinDictionary {
        val words = context.assets.readText(SpanishAsset)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        return LatinDictionary.fromOrderedWords(words)
    }
}
