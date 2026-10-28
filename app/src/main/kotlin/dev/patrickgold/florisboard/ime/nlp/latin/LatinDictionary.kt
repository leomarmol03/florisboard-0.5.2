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

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.min

/**
 * An immutable and memory friendly word list, which is used to provide word suggestions and spelling corrections.
 *
 * The dictionary keeps two parallel arrays: [words] holds the words in the way they should be displayed to the
 * user, while [keys] holds a normalized variant of each word (lowercase and without diacritics) which is used for
 * all lookups. Both arrays are sorted by the normalized key, which allows for fast binary search based lookups of
 * words and prefixes.
 *
 * All lookup methods are non-blocking and allocation friendly, so that they can be safely called on every keystroke.
 */
class LatinDictionary private constructor(
    private val words: Array<String>,
    private val keys: Array<String>,
    private val frequencies: IntArray,
) {
    /**
     * A single word of this dictionary, together with its frequency weight.
     *
     * @property word The word as it should be displayed to the user.
     * @property frequency The frequency weight of this word, a value between 0 and 255.
     */
    data class Entry(
        val word: String,
        val frequency: Int,
    )

    /**
     * A word of this dictionary which is similar to a given input word.
     *
     * @property word The word as it should be displayed to the user.
     * @property distance The edit distance between the input word and this word.
     * @property frequency The frequency weight of this word, a value between 0 and 255.
     */
    data class NearMiss(
        val word: String,
        val distance: Int,
        val frequency: Int,
    )

    /** The number of words in this dictionary. */
    val size: Int get() = words.size

    /** Returns true if this dictionary contains the given [word]. */
    fun contains(word: String): Boolean = indexOf(word) >= 0

    /** Returns the frequency weight of the given [word] or 0 if the word is not part of this dictionary. */
    fun frequencyOf(word: String): Int {
        val index = indexOf(word)
        return if (index >= 0) frequencies[index] else 0
    }

    /** Returns all words of this dictionary. The returned list is a read-only view on the internal data. */
    fun wordList(): List<String> = words.asList()

    /**
     * Returns up to [limit] words which start with the given [prefix], ordered by descending frequency.
     *
     * @param prefix The prefix the returned words should start with. Is matched case-insensitively and ignoring
     *  diacritics.
     * @param limit The maximum number of words to return.
     */
    fun suggestionsFor(prefix: String, limit: Int): List<Entry> {
        if (limit <= 0) return emptyList()
        val key = normalize(prefix)
        if (key.isEmpty()) return emptyList()

        val start = lowerBound(key)
        if (start >= keys.size || !keys[start].startsWith(key)) return emptyList()

        val result = ArrayList<Entry>(min(limit, 16))
        var index = start
        while (index < keys.size && keys[index].startsWith(key)) {
            val frequency = frequencies[index]
            if (result.size < limit || frequency > result[result.size - 1].frequency) {
                insertByFrequency(result, Entry(words[index], frequency), limit)
            }
            index++
        }
        return result
    }

    /**
     * Returns up to [limit] words which are similar to the given [word], ordered by ascending edit distance and
     * descending frequency.
     *
     * @param word The word to find similar words for.
     * @param limit The maximum number of words to return.
     */
    fun nearMisses(word: String, limit: Int): List<NearMiss> {
        if (limit <= 0) return emptyList()
        val key = normalize(word)
        if (key.length < 2) return emptyList()

        val prefix = key.substring(0, min(2, key.length))
        val start = lowerBound(prefix)
        if (start >= keys.size || !keys[start].startsWith(prefix)) return emptyList()

        val result = ArrayList<NearMiss>(min(limit, 16))
        var index = start
        while (index < keys.size && keys[index].startsWith(prefix)) {
            val candidate = keys[index]
            if (abs(candidate.length - key.length) <= MaxEditDistance) {
                val distance = editDistance(key, candidate, MaxEditDistance)
                if (distance in 1..MaxEditDistance) {
                    result.add(NearMiss(words[index], distance, frequencies[index]))
                }
            }
            index++
        }
        result.sortWith(compareBy({ it.distance }, { -it.frequency }))
        return if (result.size > limit) result.subList(0, limit) else result
    }

    private fun indexOf(word: String): Int {
        val key = normalize(word)
        val index = lowerBound(key)
        return if (index < keys.size && keys[index] == key) index else -1
    }

    private fun lowerBound(key: String): Int {
        var low = 0
        var high = keys.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (keys[mid] < key) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    private fun insertByFrequency(list: MutableList<Entry>, entry: Entry, limit: Int) {
        var index = list.size - 1
        while (index >= 0 && list[index].frequency < entry.frequency) {
            index--
        }
        list.add(index + 1, entry)
        if (list.size > limit) {
            list.removeAt(list.size - 1)
        }
    }

    companion object {
        /** The maximum edit distance which is considered to be a near miss. */
        const val MaxEditDistance = 2

        /** An empty dictionary, which is used as a fallback if no dictionary is available for a language. */
        val EMPTY = LatinDictionary(arrayOf(), arrayOf(), IntArray(0))

        /**
         * Creates a dictionary from a list of words which is ordered by descending frequency. The frequency weight of
         * each word is derived from its position within the given list.
         *
         * @param orderedWords The words of this dictionary, ordered by descending frequency.
         */
        fun fromOrderedWords(orderedWords: List<String>): LatinDictionary {
            val total = orderedWords.size
            val freqs = IntArray(total) { index -> frequencyForRank(index, total) }
            return build(orderedWords.toTypedArray(), freqs)
        }

        /**
         * Creates a dictionary from a map of words and their frequency weights.
         *
         * @param wordFrequencies The words of this dictionary and their frequency weights.
         */
        fun fromWordFrequencies(wordFrequencies: Map<String, Int>): LatinDictionary {
            val words = wordFrequencies.keys.toTypedArray()
            val freqs = IntArray(words.size) { index -> wordFrequencies[words[index]] ?: 0 }
            return build(words, freqs)
        }

        /**
         * Derives a frequency weight for a word at the given [rank] within a dictionary of the given [total] size.
         *
         * The weights are distributed on a square root scale, which mirrors the distribution of the word lists this
         * dictionary is based on and keeps the difference between common and rare words noticeable.
         */
        private fun frequencyForRank(rank: Int, total: Int): Int {
            if (total <= 1) return MaxFrequency
            val scaled = MaxFrequency - (MaxFrequency - MinFrequency) * Math.sqrt(rank.toDouble() / total)
            return scaled.toInt().coerceIn(MinFrequency, MaxFrequency)
        }

        private fun build(words: Array<String>, freqs: IntArray): LatinDictionary {
            if (words.isEmpty()) return EMPTY

            val keys = Array(words.size) { index -> normalize(words[index]) }
            val order = Array(words.size) { index -> index }
            order.sortWith { a, b -> keys[a].compareTo(keys[b]) }

            val sortedWords = ArrayList<String>(words.size)
            val sortedKeys = ArrayList<String>(words.size)
            val sortedFreqs = IntArray(words.size)
            var count = 0
            for (index in order) {
                val key = keys[index]
                if (count > 0 && sortedKeys[count - 1] == key) {
                    // The same normalized key is already present, keep the more frequent variant only
                    if (freqs[index] > sortedFreqs[count - 1]) {
                        sortedWords[count - 1] = words[index]
                        sortedFreqs[count - 1] = freqs[index]
                    }
                    continue
                }
                sortedWords.add(words[index])
                sortedKeys.add(key)
                sortedFreqs[count] = freqs[index]
                count++
            }
            return LatinDictionary(
                words = sortedWords.toTypedArray(),
                keys = sortedKeys.toTypedArray(),
                frequencies = sortedFreqs.copyOf(count),
            )
        }

        private const val MaxFrequency = 255
        private const val MinFrequency = 128

        /**
         * Normalizes the given [text] by converting it to lowercase and by removing all diacritics. This allows for
         * a lookup of e.g. "cafe" to also match "café".
         */
        fun normalize(text: String): String {
            val lowercased = text.lowercase()
            if (lowercased.all { it.code < 128 }) return lowercased
            val decomposed = Normalizer.normalize(lowercased, Normalizer.Form.NFD)
            return buildString(lowercased.length) {
                for (char in decomposed) {
                    if (Character.getType(char) != Character.NON_SPACING_MARK.toInt()) {
                        append(char)
                    }
                }
            }
        }

        /**
         * Computes the Levenshtein distance between [a] and [b], but stops early if the distance exceeds the given
         * [maxDistance].
         */
        private fun editDistance(a: String, b: String, maxDistance: Int): Int {
            if (a == b) return 0
            if (abs(a.length - b.length) > maxDistance) return maxDistance + 1

            var previous = IntArray(b.length + 1) { it }
            var current = IntArray(b.length + 1)

            for (i in 1..a.length) {
                current[0] = i
                var rowMin = current[0]
                for (j in 1..b.length) {
                    val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
                    current[j] = min(
                        min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + substitutionCost,
                    )
                    if (current[j] < rowMin) rowMin = current[j]
                }
                if (rowMin > maxDistance) return maxDistance + 1
                val swap = previous
                previous = current
                current = swap
            }
            return previous[b.length]
        }
    }
}
