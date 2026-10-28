/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The language provider for all languages which are based on the latin alphabet.
 *
 * Word suggestions and spelling corrections are provided by the word lists which are bundled with the app. Currently,
 * word lists for the english and the spanish language are available. For all other languages the english word list is
 * used as a fallback, so that the glide typing feature keeps working as before.
 */
class LatinLanguageProvider(context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        // Default user ID used for all subtypes, unless otherwise specified.
        // See `ime/core/Subtype.kt` Line 210 and 211 for the default usage
        const val ProviderId = "org.florisboard.nlp.providers.latin"

        /**
         * The maximum length of a word which is considered for a suggestion. Longer words are ignored, as they are
         * most likely not a word but a sentence or an url.
         */
        private const val MaxSuggestionWordLength = 32
    }

    private val appContext by context.appContext()
    private val prefs by FlorisPreferenceStore

    override val providerId = ProviderId

    override suspend fun create() {
        // Here we initialize our provider, set up all things which are not language dependent.
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        // Here we have the chance to preload dictionaries and prepare a neural network for a specific language.
        // Is kept in sync with the active keyboard subtype of the user, however a new preload does not necessary mean
        // the previous language is not needed anymore (e.g. if the user constantly switches between two subtypes)

        for (locale in subtype.locales()) {
            val dictionary = LatinDictionaries.get(appContext, locale.language)
            if (dictionary.size > 0) {
                flogInfo { "Preloaded ${dictionary.size} words for language '${locale.language}'" }
            }
        }
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): SpellingResult {
        val trimmedWord = word.trim()
        if (trimmedWord.isEmpty()) {
            return SpellingResult.unspecified()
        }
        val dictionary = dictionaryFor(subtype, allowFallback = false) ?: return SpellingResult.unspecified()
        if (dictionary.contains(trimmedWord)) {
            return SpellingResult.validWord()
        }
        val corrections = dictionary.nearMisses(trimmedWord, maxSuggestionCount)
            .map { it.word }
            .toTypedArray()
        return SpellingResult.typo(corrections)
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        val word = content.composingText.trim()
        if (word.isEmpty() || word.length > MaxSuggestionWordLength) {
            return emptyList()
        }
        val dictionary = dictionaryFor(subtype, allowFallback = false) ?: return emptyList()

        val exactFrequency = dictionary.frequencyOf(word)
        val suggestions = dictionary.suggestionsFor(word, maxCandidateCount)
        if (exactFrequency <= 0 && suggestions.isEmpty()) {
            // The typed word is not part of the dictionary, offer similar words instead
            return dictionary.nearMisses(word, maxCandidateCount).map { nearMiss ->
                WordSuggestionCandidate(
                    text = matchCapitalization(word, nearMiss.word),
                    confidence = confidenceOf(nearMiss.frequency, nearMiss.distance),
                    isEligibleForAutoCommit = false,
                    isEligibleForUserRemoval = false,
                    sourceProvider = this,
                )
            }
        }

        val candidates = ArrayList<SuggestionCandidate>(suggestions.size + 1)
        if (exactFrequency > 0) {
            // The word which was typed is a valid word, so it should be the first suggestion
            candidates.add(
                WordSuggestionCandidate(
                    text = word,
                    confidence = confidenceOf(exactFrequency, 0),
                    isEligibleForAutoCommit = false,
                    isEligibleForUserRemoval = false,
                    sourceProvider = this,
                )
            )
        }
        for (suggestion in suggestions) {
            if (exactFrequency > 0 && suggestion.word.equals(word, ignoreCase = true)) {
                continue
            }
            candidates.add(
                WordSuggestionCandidate(
                    text = matchCapitalization(word, suggestion.word),
                    confidence = confidenceOf(suggestion.frequency, 0),
                    isEligibleForAutoCommit = false,
                    isEligibleForUserRemoval = false,
                    sourceProvider = this,
                )
            )
        }
        return candidates.take(maxCandidateCount)
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        // We can use flogDebug, flogInfo, flogWarning and flogError for debug logging, which is a wrapper for Logcat
        flogDebug { candidate.toString() }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { candidate.toString() }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        flogDebug { candidate.toString() }
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        val dictionary = dictionaryFor(subtype, allowFallback = true) ?: return emptyList()
        return dictionary.wordList()
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        val dictionary = dictionaryFor(subtype, allowFallback = true) ?: return 0.0
        return dictionary.frequencyOf(word) / 255.0
    }

    override suspend fun destroy() {
        // Here we have the chance to de-allocate memory and finish our work. However this might never be called if
        // the app process is killed (which will most likely always be the case).
    }

    /**
     * Returns the dictionary which matches the language of the given [subtype] or null if no dictionary is available.
     *
     * @param subtype The subtype to get the dictionary for.
     * @param allowFallback If true, the english dictionary is used if no dictionary is available for the language of
     *  the given subtype. This is required for the glide typing feature, which relies on a word list being present.
     */
    private suspend fun dictionaryFor(subtype: Subtype, allowFallback: Boolean): LatinDictionary? {
        val enabledLanguages = enabledLanguages()
        for (locale in subtype.locales()) {
            val language = locale.language.lowercase()
            // Only the languages which the user has enabled are used for the suggestions.
            if (language !in enabledLanguages) continue
            val dictionary = LatinDictionaries.get(appContext, language)
            if (dictionary.size > 0) {
                return dictionary
            }
        }
        if (!allowFallback) return null
        val fallbackLanguage = LatinDictionaries.FallbackLanguage
        if (fallbackLanguage !in enabledLanguages) return null
        val fallback = LatinDictionaries.get(appContext, fallbackLanguage)
        return fallback.takeIf { it.size > 0 }
    }

    /**
     * Returns the languages for which the word suggestions and the spell corrections are provided.
     *
     * The user can choose the languages in the settings of the app.
     */
    private fun enabledLanguages(): Set<String> {
        return LatinDictionaries.parseLanguages(prefs.suggestion.enabledLanguages.get())
    }

    /**
     * Matches the capitalization of the typed [word] with the given [suggestion], so that a word which was typed with
     * an uppercase first letter is also suggested with an uppercase first letter.
     */
    private fun matchCapitalization(word: String, suggestion: String): String {
        if (word.isEmpty() || suggestion.isEmpty()) return suggestion
        if (!word[0].isUpperCase()) return suggestion
        return suggestion.replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * Computes the confidence of a suggestion candidate based on the frequency of the suggested word and the edit
     * distance to the typed word.
     */
    private fun confidenceOf(frequency: Int, distance: Int): Double {
        val frequencyConfidence = (frequency / 255.0).coerceIn(0.0, 1.0)
        val distancePenalty = distance * 0.2
        return (frequencyConfidence - distancePenalty).coerceIn(0.05, 1.0)
    }
}
