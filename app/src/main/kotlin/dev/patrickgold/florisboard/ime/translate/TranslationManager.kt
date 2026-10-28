/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.translate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Translates the text of the user with the translate api of yandex or google.
 *
 * The api of yandex requires an api key, which the user can set in the settings of the app. The api of google can be
 * used without an api key.
 */
object TranslationManager {
    /** The endpoint of the translate api of yandex. */
    private const val YANDEX_ENDPOINT = "https://translate.yandex.net/api/v1.5/tr.json/translate"

    /** The endpoint of the translate api of google. */
    private const val GOOGLE_ENDPOINT = "https://translate.googleapis.com/translate_a/single"

    /** The languages which the user can choose as the target language of a translation. */
    val SupportedLanguages = listOf(
        "en", "es", "fr", "de", "it", "pt", "ru", "uk", "pl", "nl", "tr", "ar",
        "hi", "zh", "ja", "ko", "vi", "id", "th", "sv", "cs", "el", "he", "fi",
        "da", "no", "ro", "hu", "bg", "hr", "sr", "sk", "sl", "lt", "lv", "et",
    )

    /** Whether the panel of the translator is currently shown. */
    var isPanelVisible by mutableStateOf(false)

    /** The text which is translated. */
    var sourceText by mutableStateOf("")

    /** The result of the last translation. */
    var translatedText by mutableStateOf("")

    /** Whether a translation is currently running. */
    var isTranslating by mutableStateOf(false)

    /** The error message of the last translation, if it failed. */
    var errorMessage by mutableStateOf<String?>(null)

    /** The language into which the text is translated. */
    var targetLanguage by mutableStateOf("en")

    private val prefs by FlorisPreferenceStore

    /**
     * Opens the panel of the translator with the given [text].
     *
     * @param text The text which is translated.
     */
    fun open(text: String) {
        sourceText = text
        translatedText = ""
        errorMessage = null
        isTranslating = false
        targetLanguage = prefs.translate.targetLanguage.get()
        isPanelVisible = true
    }

    /** Closes the panel of the translator. */
    fun close() {
        isPanelVisible = false
        isTranslating = false
    }

    /**
     * Sets the language into which the text is translated and stores it in the preferences.
     *
     * @param language The language code of the target language, e.g. "es".
     */
    suspend fun setTargetLanguage(language: String) {
        targetLanguage = language
        prefs.translate.targetLanguage.set(language)
    }

    /**
     * Translates the current [sourceText] into the [targetLanguage].
     */
    suspend fun translate() {
        val text = sourceText
        if (text.isBlank()) {
            errorMessage = null
            translatedText = ""
            return
        }
        isTranslating = true
        errorMessage = null
        val result = translateText(text, targetLanguage)
        isTranslating = false
        result.onSuccess { translatedText = it }
        result.onFailure { errorMessage = it.message }
    }

    /**
     * Translates the given [text] into the given [targetLanguage] with the configured provider.
     *
     * @param text The text which is translated.
     * @param targetLanguage The language code of the target language, e.g. "es".
     *
     * @return The translated text or the error which occurred during the translation.
     */
    suspend fun translateText(text: String, targetLanguage: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val provider = prefs.translate.provider.get()
            val url = when (provider) {
                TranslationProvider.YANDEX -> {
                    val apiKey = prefs.translate.yandexApiKey.get()
                    check(apiKey.isNotBlank()) { "No api key for the yandex translate api has been set" }
                    "$YANDEX_ENDPOINT?key=${apiKey.encode()}&text=${text.encode()}&lang=${targetLanguage.encode()}"
                }
                TranslationProvider.GOOGLE -> {
                    "$GOOGLE_ENDPOINT?client=gtx&sl=auto&tl=${targetLanguage.encode()}&dt=t&q=${text.encode()}"
                }
            }
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
                useCaches = false
                setRequestProperty("User-Agent", "Mozilla/5.0 (FlorisBoard)")
                setRequestProperty("Accept", "application/json")
            }
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    error("The translate api returned the status code $responseCode: $errorText")
                }
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                parseResponse(provider, responseText)
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Parses the response of the translate api.
     *
     * @param provider The provider which was used for the translation.
     * @param responseText The raw response of the api.
     *
     * @return The translated text.
     */
    private fun parseResponse(provider: TranslationProvider, responseText: String): String {
        val json = Json.parseToJsonElement(responseText)
        return when (provider) {
            TranslationProvider.YANDEX -> {
                val array = json.jsonObject["text"]?.jsonArray
                    ?: error("The yandex translate api returned an unexpected response")
                array.joinToString("\n") { it.jsonPrimitive.content }
            }
            TranslationProvider.GOOGLE -> {
                val array = json.jsonArray
                val chunks = array.firstOrNull()?.jsonArray?.mapNotNull { chunk ->
                    chunk.jsonArray.firstOrNull()?.jsonPrimitive?.content
                }
                if (chunks.isNullOrEmpty()) {
                    error("The google translate api returned an unexpected response")
                }
                chunks.joinToString("")
            }
        }
    }

    private fun String.encode(): String = URLEncoder.encode(this, "UTF-8")
}
