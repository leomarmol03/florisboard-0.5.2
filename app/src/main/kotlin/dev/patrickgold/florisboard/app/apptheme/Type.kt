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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.patrickgold.florisboard.R

/**
 * The font family which is used throughout the settings app. This is the same font which is used for the keyboard
 * UI, so that both look and feel the same.
 *
 * Note that the bundled "Google Sans" font is a freely redistributable stand-in for the proprietary font of Google.
 */
val GoogleSansFontFamily = FontFamily(
    Font(R.font.google_sans, weight = FontWeight.Normal),
    Font(R.font.google_sans, weight = FontWeight.Bold),
)

private fun TextStyle.withGoogleSans(): TextStyle = copy(fontFamily = GoogleSansFontFamily)

// Set of Material typography styles to start with
val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.withGoogleSans(),
        displayMedium = displayMedium.withGoogleSans(),
        displaySmall = displaySmall.withGoogleSans(),
        headlineLarge = headlineLarge.withGoogleSans(),
        headlineMedium = headlineMedium.withGoogleSans(),
        headlineSmall = headlineSmall.withGoogleSans(),
        titleLarge = titleLarge.withGoogleSans(),
        titleMedium = titleMedium.withGoogleSans(),
        titleSmall = titleSmall.withGoogleSans(),
        bodyLarge = bodyLarge.withGoogleSans(),
        bodyMedium = bodyMedium.withGoogleSans(),
        bodySmall = bodySmall.withGoogleSans(),
        labelLarge = labelLarge.withGoogleSans(),
        labelMedium = labelMedium.withGoogleSans(),
        labelSmall = labelSmall.withGoogleSans(),
    )
}
