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

package dev.patrickgold.florisboard.app.settings.translation

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.translate.TranslationManager
import dev.patrickgold.florisboard.ime.translate.TranslationProvider
import dev.patrickgold.florisboard.lib.FlorisLocale
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.TextFieldPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun TranslationScreen() = FlorisScreen {
    title = stringRes(R.string.settings__translate__title)
    previewFieldVisible = true

    content {
        SwitchPreference(
            prefs.translate.enabled,
            title = stringRes(R.string.pref__translate__enabled__label),
            summary = stringRes(R.string.pref__translate__enabled__summary),
        )
        PreferenceGroup(title = stringRes(R.string.pref__translate__group__label)) {
            ListPreference(
                prefs.translate.provider,
                title = stringRes(R.string.pref__translate__provider__label),
                entries = enumDisplayEntriesOf(TranslationProvider::class),
                enabledIf = { prefs.translate.enabled isEqualTo true },
            )
            TextFieldPreference(
                prefs.translate.yandexApiKey,
                title = stringRes(R.string.pref__translate__yandex_api_key__label),
                summaryIfBlank = stringRes(R.string.pref__translate__yandex_api_key__summary),
                enabledIf = {
                    prefs.translate.enabled isEqualTo true &&
                        prefs.translate.provider isEqualTo TranslationProvider.YANDEX
                },
            )
            ListPreference(
                prefs.translate.sourceLanguage,
                title = stringRes(R.string.pref__translate__source_language__label),
                entries = listPrefEntries {
                    entry(
                        key = TranslationManager.AutoLanguage,
                        label = stringRes(R.string.translate__auto_language),
                    )
                    for (language in TranslationManager.SupportedLanguages) {
                        entry(
                            key = language,
                            label = FlorisLocale.from(language).base.displayLanguage,
                        )
                    }
                },
                enabledIf = { prefs.translate.enabled isEqualTo true },
            )
            ListPreference(
                prefs.translate.targetLanguage,
                title = stringRes(R.string.pref__translate__target_language__label),
                entries = listPrefEntries {
                    for (language in TranslationManager.SupportedLanguages) {
                        entry(
                            key = language,
                            label = FlorisLocale.from(language).base.displayLanguage,
                        )
                    }
                },
                enabledIf = { prefs.translate.enabled isEqualTo true },
            )
        }
    }
}
