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

package dev.patrickgold.florisboard.ime.theme

/**
 * Holds the name and the uri of the font which the user has provided.
 *
 * The font is not bundled with the app, but can be added by the user from the storage of the device. If no font has
 * been selected, the default system font is used instead.
 */
object FlorisImeFont {
    /** The name of the font which the user has provided. */
    const val Custom = "Custom"

    /** The uri of the font which the user has provided. */
    const val CustomUri = "font:/custom"
}
