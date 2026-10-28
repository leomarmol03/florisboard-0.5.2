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

package dev.patrickgold.florisboard.ime.clipboard

import androidx.annotation.StringRes
import dev.patrickgold.florisboard.R

/**
 * The tabs which are available in the clipboard view.
 *
 * The currently selected tab is remembered, so that the user always ends up in the same tab as before.
 */
enum class ClipboardTab(
    @StringRes val labelRes: Int,
) {
    /** The tab which shows all items which are pinned by the user. */
    PINNED(R.string.clipboard__tab_pinned),

    /** The tab which shows all items which are not pinned, sorted by their creation date. */
    RECENT(R.string.clipboard__tab_recent),
}
