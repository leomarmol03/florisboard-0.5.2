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

package dev.patrickgold.florisboard.app.font

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.patrickgold.florisboard.lib.devtools.flogError
import java.io.File
import org.florisboard.lib.kotlin.io.subDir

/**
 * Manages the fonts which the user has added to the app.
 *
 * The fonts are stored in the `fonts` directory of the files dir of the app. Only the file name of the selected
 * font is stored in the preferences, so that a backup can be restored on another device, even if the absolute path
 * of the font file is different there.
 */
object CustomFontStorage {
    /** The name of the directory which holds all user provided fonts. */
    const val FONT_DIR_NAME = "fonts"

    /** The file name which is used when no font is selected. */
    const val NO_FONT = ""

    /** The mime types which are used to select a font file from the storage of the device. */
    val FONT_MIME_TYPES = arrayOf(
        "font/ttf",
        "font/otf",
        "application/x-font-ttf",
        "application/x-font-truetype",
        "application/vnd.ms-opentype",
        "application/octet-stream",
    )

    /** Returns the directory which holds all user provided fonts. */
    fun fontDir(context: Context): File = context.filesDir.subDir(FONT_DIR_NAME).apply { mkdirs() }

    /** Returns the file of the font with the given [name] or null if no such font exists. */
    fun fontFile(context: Context, name: String): File? {
        if (name.isBlank()) return null
        val file = File(fontDir(context), name)
        return file.takeIf { it.exists() && it.isFile }
    }

    /** Returns all fonts which the user has added to the app. */
    fun availableFonts(context: Context): List<File> {
        val dir = fontDir(context)
        return dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
    }

    /**
     * Copies the font at the given [uri] into the font directory of the app.
     *
     * @param context The context used to access the files dir of the app.
     * @param uri The uri of the font file, which is usually provided by a file picker.
     * @return The name of the newly added font file or null if the font could not be added.
     */
    fun importFont(context: Context, uri: Uri): String? {
        var target: File? = null
        return runCatching {
            val displayName = queryDisplayName(context, uri) ?: "font.ttf"
            val fileName = uniqueFileName(context, sanitizeFileName(displayName))
            val file = File(fontDir(context), fileName)
            target = file
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Unable to open the font file")
            check(isFontFile(file)) { "The selected file is not a valid ttf or otf font file" }
            fileName
        }.onFailure { error ->
            target?.delete()
            flogError { "Unable to import the font: ${error.message}" }
        }.getOrNull()
    }

    /** Deletes the font with the given [name] from the font directory of the app. */
    fun deleteFont(context: Context, name: String) {
        runCatching { fontFile(context, name)?.delete() }
    }

    /**
     * Checks whether the given [file] is a valid ttf, otf or ttc font file.
     *
     * This is a simple sanity check of the sfnt header, as the android system is not able to handle invalid font
     * files and would crash the app while rendering text with them.
     */
    fun isFontFile(file: File): Boolean {
        return runCatching {
            val header = ByteArray(4)
            file.inputStream().use { stream ->
                if (stream.read(header) != 4) return false
            }
            val tag = header.map { it.toInt() and 0xFF }
            // 0x00010000 is the sfnt version of a ttf file, 'OTTO' is the magic of an otf file and 'ttcf' is
            // the magic of a font collection.
            (tag[0] == 0x00 && tag[1] == 0x01 && tag[2] == 0x00 && tag[3] == 0x00) ||
                (tag[0] == 0x4F && tag[1] == 0x54 && tag[2] == 0x54 && tag[3] == 0x4F) ||
                (tag[0] == 0x74 && tag[1] == 0x74 && tag[2] == 0x63 && tag[3] == 0x66)
        }.getOrDefault(false)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) it.getString(index) else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "font.ttf" }
        return if (sanitized.contains('.')) sanitized else "$sanitized.ttf"
    }

    private fun uniqueFileName(context: Context, fileName: String): String {
        val dir = fontDir(context)
        if (!File(dir, fileName).exists()) return fileName
        val baseName = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "ttf")
        var counter = 1
        var candidate = "$baseName ($counter).$extension"
        while (File(dir, candidate).exists()) {
            counter++
            candidate = "$baseName ($counter).$extension"
        }
        return candidate
    }
}
