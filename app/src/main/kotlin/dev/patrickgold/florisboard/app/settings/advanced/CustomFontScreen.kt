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

package dev.patrickgold.florisboard.app.settings.advanced

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.font.CustomFontStorage
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.Preference
import kotlinx.coroutines.launch
import org.florisboard.lib.android.showShortToastSync
import org.florisboard.lib.compose.FlorisOutlinedBox
import org.florisboard.lib.compose.defaultFlorisOutlinedBox
import org.florisboard.lib.compose.stringRes

/**
 * The screen which allows the user to add a custom font from the storage of the device.
 *
 * The selected font is used for the keyboard, the clipboard and the settings app.
 */
@Composable
fun CustomFontScreen() = FlorisScreen {
    title = stringRes(R.string.settings__custom_font__title)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by FlorisPreferenceStore
    val selectedFontName by prefs.other.customFont.observeAsState()
    var availableFontNames by remember {
        mutableStateOf(CustomFontStorage.availableFonts(context).map { it.name })
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val fontName = CustomFontStorage.importFont(context, uri)
            if (fontName != null) {
                prefs.other.customFont.set(fontName)
                availableFontNames = CustomFontStorage.availableFonts(context).map { it.name }
                context.showShortToastSync(R.string.settings__custom_font__add__success)
            } else {
                context.showShortToastSync(R.string.settings__custom_font__add__failure)
            }
        }
    }

    content {
        Preference(
            icon = Icons.Default.FontDownload,
            title = stringRes(R.string.settings__custom_font__add__label),
            summary = stringRes(R.string.settings__custom_font__add__summary),
            onClick = {
                fontPickerLauncher.launch(CustomFontStorage.FONT_MIME_TYPES)
            },
        )

        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
            title = stringRes(R.string.settings__custom_font__available__title),
        ) {
            RadioListItem(
                onClick = {
                    scope.launch { prefs.other.customFont.set(CustomFontStorage.NO_FONT) }
                },
                selected = selectedFontName.isBlank(),
                text = stringRes(R.string.settings__custom_font__system_default),
            )
            for (fontName in availableFontNames) {
                RadioListItem(
                    onClick = {
                        scope.launch { prefs.other.customFont.set(fontName) }
                    },
                    selected = selectedFontName == fontName,
                    text = fontName,
                )
            }
        }

        Preference(
            icon = Icons.Default.Delete,
            title = stringRes(R.string.settings__custom_font__remove__label),
            summary = stringRes(R.string.settings__custom_font__remove__summary),
            enabledIf = { selectedFontName.isNotBlank() },
            onClick = {
                scope.launch {
                    CustomFontStorage.deleteFont(context, selectedFontName)
                    prefs.other.customFont.set(CustomFontStorage.NO_FONT)
                    availableFontNames = CustomFontStorage.availableFonts(context).map { it.name }
                    context.showShortToastSync(R.string.settings__custom_font__remove__success)
                }
            },
        )
    }
}
