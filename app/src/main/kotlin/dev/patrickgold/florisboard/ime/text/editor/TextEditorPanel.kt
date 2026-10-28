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

package dev.patrickgold.florisboard.ime.text.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.compose.florisVerticalScroll
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

/**
 * A small text editor which is shown above the keys of the keyboard.
 *
 * The text is not edited with a text field, but with the keys of the keyboard itself, which is why the keys are
 * routed to the [TextEditorState] while the editor is visible.
 */
@Composable
fun TextEditorPanel(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()
    val editorInstance by context.editorInstance()

    val isClipboardMode = TextEditorState.mode == TextEditorState.Mode.CLIPBOARD_ITEM
    val text = TextEditorState.text
    val cursor = TextEditorState.cursor

    SnyggColumn(
        elementName = FlorisImeUi.TextEditorPanel.elementName,
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight * 2),
    ) {
        SnyggRow(
            elementName = "text-editor-header",
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIconButton(
                elementName = "text-editor-header-button",
                onClick = { TextEditorState.close() },
            ) {
                SnyggIcon(imageVector = Icons.AutoMirrored.Filled.ArrowBack)
            }
            SnyggText(
                elementName = "text-editor-header-text",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                text = stringRes(R.string.text_editor__title),
            )
            SnyggIconButton(
                elementName = "text-editor-header-button",
                onClick = {
                    val currentText = TextEditorState.text
                    when (TextEditorState.mode) {
                        TextEditorState.Mode.FREE -> {
                            if (currentText.isNotEmpty()) {
                                editorInstance.commitText(currentText)
                            }
                        }
                        TextEditorState.Mode.CLIPBOARD_ITEM -> {
                            val item = TextEditorState.targetItem
                            if (item != null) {
                                clipboardManager.updateClipText(item, currentText)
                            }
                        }
                    }
                    TextEditorState.close()
                },
                enabled = text.isNotEmpty() || isClipboardMode,
            ) {
                SnyggIcon(imageVector = Icons.Default.Check)
            }
        }
        SnyggBox(
            elementName = "text-editor-text",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .florisVerticalScroll(),
        ) {
            if (text.isEmpty()) {
                SnyggText(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    text = stringRes(R.string.text_editor__hint),
                )
            } else {
                SnyggText(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    text = text.substring(0, cursor.coerceIn(0, text.length)) + "|" +
                        text.substring(cursor.coerceIn(0, text.length)),
                )
            }
        }
        SnyggRow(
            elementName = "text-editor-toolbar",
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIconButton(
                elementName = "text-editor-toolbar-button",
                onClick = {
                    if (TextEditorState.text.isNotEmpty()) {
                        clipboardManager.addNewPlaintext(TextEditorState.text)
                    }
                },
                enabled = text.isNotEmpty(),
            ) {
                SnyggIcon(imageVector = Icons.Default.ContentCopy)
            }
            SnyggIconButton(
                elementName = "text-editor-toolbar-button",
                onClick = {
                    if (TextEditorState.text.isNotEmpty()) {
                        clipboardManager.addNewPlaintext(TextEditorState.text)
                        TextEditorState.clear()
                    }
                },
                enabled = text.isNotEmpty(),
            ) {
                SnyggIcon(imageVector = Icons.Default.ContentCut)
            }
            SnyggIconButton(
                elementName = "text-editor-toolbar-button",
                onClick = {
                    val clipText = clipboardManager.primaryClip?.text
                    if (!clipText.isNullOrEmpty()) {
                        TextEditorState.insert(clipText)
                    }
                },
            ) {
                SnyggIcon(imageVector = Icons.Default.ContentPaste)
            }
            SnyggIconButton(
                elementName = "text-editor-toolbar-button",
                onClick = { TextEditorState.clear() },
                enabled = text.isNotEmpty(),
            ) {
                SnyggIcon(imageVector = Icons.Default.DeleteSweep)
            }
        }
    }
}
