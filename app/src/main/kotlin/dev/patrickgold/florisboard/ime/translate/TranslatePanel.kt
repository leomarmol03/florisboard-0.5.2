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

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.FlorisLocale
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.florisVerticalScroll
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

/**
 * The panel of the translator, which is shown instead of the keys of the keyboard.
 *
 * The text of the user is translated with the translate api of yandex or google and the result can be inserted into
 * the input field of the user, copied to the clipboard or shared with another app.
 */
@Composable
fun TranslatePanel(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardManager by context.keyboardManager()
    val clipboardManager by context.clipboardManager()
    val editorInstance by context.editorInstance()

    val sourceText = TranslationManager.sourceText
    val translatedText = TranslationManager.translatedText
    val errorMessage = TranslationManager.errorMessage
    val isTranslating = TranslationManager.isTranslating
    val targetLanguage = TranslationManager.targetLanguage
    var isLanguageMenuShown by remember { mutableStateOf(false) }

    fun close() {
        TranslationManager.close()
        keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT
    }

    SnyggColumn(
        elementName = FlorisImeUi.TranslatePanel.elementName,
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.imeUiHeight()),
    ) {
        SnyggRow(
            elementName = "translate-panel-header",
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIconButton(
                elementName = "translate-panel-header-button",
                onClick = { close() },
            ) {
                SnyggIcon(imageVector = Icons.AutoMirrored.Filled.ArrowBack)
            }
            SnyggText(
                elementName = "translate-panel-header-text",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                text = stringRes(R.string.translate__title),
            )
            SnyggBox(
                elementName = "translate-panel-language-button",
                modifier = Modifier.padding(end = 8.dp),
            ) {
                SnyggIconButton(
                    elementName = "translate-panel-language-icon",
                    onClick = { isLanguageMenuShown = true },
                ) {
                    SnyggIcon(imageVector = Icons.Default.Language)
                }
                DropdownMenu(
                    expanded = isLanguageMenuShown,
                    onDismissRequest = { isLanguageMenuShown = false },
                ) {
                    for (language in TranslationManager.SupportedLanguages) {
                        DropdownMenuItem(
                            text = {
                                androidx.compose.material3.Text(
                                    text = FlorisLocale.from(language).base.displayLanguage,
                                )
                            },
                            onClick = {
                                isLanguageMenuShown = false
                                scope.launch {
                                    TranslationManager.setTargetLanguage(language)
                                }
                            },
                        )
                    }
                }
            }
        }
        SnyggColumn(
            elementName = "translate-panel-content",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .florisVerticalScroll(),
        ) {
            SnyggText(
                elementName = "translate-panel-label",
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                text = stringRes(R.string.translate__source),
            )
            SnyggText(
                elementName = "translate-panel-text",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                text = sourceText.ifBlank { stringRes(R.string.translate__no_text) },
            )
            SnyggText(
                elementName = "translate-panel-label",
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                text = stringRes(R.string.translate__result),
            )
            SnyggText(
                elementName = "translate-panel-text",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                text = when {
                    isTranslating -> stringRes(R.string.translate__loading)
                    !errorMessage.isNullOrBlank() -> stringRes(R.string.translate__error, "error" to errorMessage)
                    translatedText.isBlank() -> stringRes(R.string.translate__result_empty)
                    else -> translatedText
                },
            )
        }
        SnyggRow(
            elementName = "translate-panel-actions",
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIconButton(
                elementName = "translate-panel-action-button",
                onClick = {
                    scope.launch { TranslationManager.translate() }
                },
                enabled = sourceText.isNotBlank() && !isTranslating,
            ) {
                SnyggIcon(imageVector = Icons.Default.Language)
            }
            SnyggIconButton(
                elementName = "translate-panel-action-button",
                onClick = {
                    if (translatedText.isNotEmpty()) {
                        editorInstance.commitText(translatedText)
                    }
                },
                enabled = translatedText.isNotEmpty(),
            ) {
                SnyggIcon(imageVector = Icons.Default.ContentPaste)
            }
            SnyggIconButton(
                elementName = "translate-panel-action-button",
                onClick = {
                    if (translatedText.isNotEmpty()) {
                        clipboardManager.addNewPlaintext(translatedText)
                    }
                },
                enabled = translatedText.isNotEmpty(),
            ) {
                SnyggIcon(imageVector = Icons.Default.ContentCopy)
            }
            SnyggIconButton(
                elementName = "translate-panel-action-button",
                onClick = {
                    val text = translatedText.ifBlank { sourceText }
                    if (text.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        val chooser = Intent.createChooser(intent, null).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(chooser) }
                    }
                },
                enabled = translatedText.isNotEmpty() || sourceText.isNotEmpty(),
            ) {
                SnyggIcon(imageVector = Icons.Default.Share)
            }
        }
    }
}
