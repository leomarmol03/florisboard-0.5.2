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
import java.io.DataInputStream
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** The magic bytes of a ttf file. */
    private val TTF_MAGIC = byteArrayOf(0x00, 0x01, 0x00, 0x00)

    /** The magic bytes of an otf file. */
    private val OTF_MAGIC = byteArrayOf(0x4F, 0x54, 0x54, 0x4F)

    /** The magic bytes of a font collection. */
    private val TTC_MAGIC = byteArrayOf(0x74, 0x74, 0x63, 0x66)

    /** The tables which every usable font has to provide. */
    private val REQUIRED_TABLES = setOf("cmap", "head", "hhea", "hmtx", "maxp", "name")

    /** The tables which hold the outlines of a font. */
    private val OUTLINE_TABLES = setOf("glyf", "CFF ", "CFF2")

    /**
     * A counter which is increased whenever the fonts of the app change.
     *
     * The theme of the keyboard is compiled with the font of the user, therefore the theme has to be recompiled
     * whenever a font is added, removed or restored. As the keyboard is not able to observe the files dir directly,
     * this counter is used as a trigger for the recompilation.
     */
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    /** Notifies the observers that the fonts of the app have changed. */
    fun notifyChanged() {
        _revision.value = _revision.value + 1
    }

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
            notifyChanged()
            fileName
        }.onFailure { error ->
            target?.delete()
            flogError { "Unable to import the font: ${error.message}" }
        }.getOrNull()
    }

    /** Deletes the font with the given [name] from the font directory of the app. */
    fun deleteFont(context: Context, name: String) {
        runCatching { fontFile(context, name)?.delete() }
        notifyChanged()
    }

    /**
     * Checks whether the given [file] is a valid ttf, otf or ttc font file.
     *
     * The header of the font is checked and, for a ttf or otf file, the table directory is validated as well. The
     * android system is not able to handle invalid font files and would crash the app while rendering text with
     * them, therefore only files which look like a usable font are accepted here.
     */
    fun isFontFile(file: File): Boolean {
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { stream ->
                val header = ByteArray(12)
                stream.readFully(header)
                val magic = header.copyOfRange(0, 4)
                when {
                    // 0x00010000 is the sfnt version of a ttf file and 'OTTO' is the magic of an otf file. Both of
                    // them use the same table directory layout.
                    magic.contentEquals(TTF_MAGIC) || magic.contentEquals(OTF_MAGIC) -> {
                        val numTables = readUInt16(header, 4)
                        if (numTables <= 0 || numTables > 512) return@use false
                        val records = ByteArray(numTables * 16)
                        stream.readFully(records)
                        val tags = HashSet<String>(numTables)
                        for (index in 0 until numTables) {
                            tags.add(String(records, index * 16, 4, Charsets.ISO_8859_1))
                        }
                        REQUIRED_TABLES.all { it in tags } && tags.any { it in OUTLINE_TABLES }
                    }
                    // 'ttcf' is the magic of a font collection, which holds multiple fonts in a single file.
                    magic.contentEquals(TTC_MAGIC) -> true
                    else -> false
                }
            }
        }.getOrDefault(false)
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
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
