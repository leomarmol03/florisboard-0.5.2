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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem

/**
 * Holds the state of the text editor of the keyboard.
 *
 * The text editor is a small editor which is shown above the keys of the keyboard. The text is not edited with a
 * text field, but with the keys of the keyboard itself, therefore the state of the editor has to be shared between
 * the panel and the keyboard manager.
 */
object TextEditorState {
    /**
     * The mode of the text editor.
     *
     * In the mode [FREE] the text of the editor can be inserted into the input field of the user, while the mode
     * [CLIPBOARD_ITEM] is used to edit an item of the clipboard history.
     */
    enum class Mode {
        FREE,
        CLIPBOARD_ITEM,
    }

    /** Whether the text editor is currently shown. */
    var isVisible by mutableStateOf(false)

    /** The mode of the text editor. */
    var mode by mutableStateOf(Mode.FREE)

    /** The text which is currently edited. */
    var text by mutableStateOf("")

    /** The position of the cursor within the text. */
    var cursor by mutableStateOf(0)

    /** The item of the clipboard history which is edited, if the mode is [Mode.CLIPBOARD_ITEM]. */
    var targetItem by mutableStateOf<ClipboardItem?>(null)

    /**
     * Opens the text editor in the mode [FREE], so that the text can be inserted into the input field of the user.
     *
     * @param initialText The text which is shown when the editor is opened.
     */
    fun openFree(initialText: String = "") {
        mode = Mode.FREE
        targetItem = null
        text = initialText
        cursor = initialText.length
        isVisible = true
    }

    /**
     * Opens the text editor in the mode [CLIPBOARD_ITEM], so that an item of the clipboard history can be edited.
     *
     * @param item The item of the clipboard history which is edited.
     * @param initialText The text of the item.
     */
    fun openForItem(item: ClipboardItem, initialText: String) {
        mode = Mode.CLIPBOARD_ITEM
        targetItem = item
        text = initialText
        cursor = initialText.length
        isVisible = true
    }

    /** Closes the text editor. */
    fun close() {
        isVisible = false
        targetItem = null
    }

    /** Inserts the given [str] at the current position of the cursor. */
    fun insert(str: String) {
        if (str.isEmpty()) return
        val safeCursor = cursor.coerceIn(0, text.length)
        text = text.substring(0, safeCursor) + str + text.substring(safeCursor)
        cursor = safeCursor + str.length
    }

    /** Deletes the character in front of the cursor. */
    fun deleteBackward() {
        val safeCursor = cursor.coerceIn(0, text.length)
        if (safeCursor <= 0) return
        text = text.removeRange(safeCursor - 1, safeCursor)
        cursor = safeCursor - 1
    }

    /** Deletes the character behind the cursor. */
    fun deleteForward() {
        val safeCursor = cursor.coerceIn(0, text.length)
        if (safeCursor >= text.length) return
        text = text.removeRange(safeCursor, safeCursor + 1)
    }

    /** Moves the cursor by the given [delta]. */
    fun moveCursor(delta: Int) {
        cursor = (cursor + delta).coerceIn(0, text.length)
    }

    /** Removes the whole text. */
    fun clear() {
        text = ""
        cursor = 0
    }
}
