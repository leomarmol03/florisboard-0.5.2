/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.apptheme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.patrickgold.florisboard.app.font.CustomFontStorage

/**
 * Creates the font family of the custom font which the user has added from the storage of the device.
 *
 * If no font has been selected by the user, or if the selected font can no longer be found, `null` is returned and
 * the default system font is used instead.
 */
@Composable
fun rememberCustomFontFamily(customFontName: String): FontFamily? {
    val context = LocalContext.current
    return remember(customFontName) {
        if (customFontName.isBlank()) {
            null
        } else {
            CustomFontStorage.fontFile(context, customFontName)?.let { fontFile ->
                FontFamily(
                    Font(fontFile, weight = FontWeight.Normal),
                    Font(fontFile, weight = FontWeight.Bold),
                )
            }
        }
    }
}

private fun TextStyle.withFontFamily(fontFamily: FontFamily?): TextStyle =
    if (fontFamily == null) this else copy(fontFamily = fontFamily)

/**
 * Creates the typography of the settings app. The custom font of the user is used throughout the settings app, so
 * that it matches the look and feel of the keyboard UI.
 */
@Composable
fun rememberFlorisTypography(customFontName: String): Typography {
    val fontFamily = rememberCustomFontFamily(customFontName)
    return remember(fontFamily) {
        Typography().run {
            copy(
                displayLarge = displayLarge.withFontFamily(fontFamily),
                displayMedium = displayMedium.withFontFamily(fontFamily),
                displaySmall = displaySmall.withFontFamily(fontFamily),
                headlineLarge = headlineLarge.withFontFamily(fontFamily),
                headlineMedium = headlineMedium.withFontFamily(fontFamily),
                headlineSmall = headlineSmall.withFontFamily(fontFamily),
                titleLarge = titleLarge.withFontFamily(fontFamily),
                titleMedium = titleMedium.withFontFamily(fontFamily),
                titleSmall = titleSmall.withFontFamily(fontFamily),
                bodyLarge = bodyLarge.withFontFamily(fontFamily),
                bodyMedium = bodyMedium.withFontFamily(fontFamily),
                bodySmall = bodySmall.withFontFamily(fontFamily),
                labelLarge = labelLarge.withFontFamily(fontFamily),
                labelMedium = labelMedium.withFontFamily(fontFamily),
                labelSmall = labelSmall.withFontFamily(fontFamily),
            )
        }
    }
}
