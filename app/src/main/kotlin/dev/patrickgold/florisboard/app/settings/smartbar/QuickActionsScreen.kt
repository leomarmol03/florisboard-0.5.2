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

package dev.patrickgold.florisboard.app.settings.smartbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickAction
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionArrangement
import dev.patrickgold.florisboard.ime.smartbar.quickaction.computeActionName
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

/**
 * The screen which allows the user to add the actions of the smart bar, e.g. the translator or the text editor.
 */
@Composable
fun QuickActionsScreen() = FlorisScreen {
    title = stringRes(R.string.settings__smartbar__quick_actions__title)
    previewFieldVisible = true

    content {
        val scope = rememberCoroutineScope()
        // The arrangement is read once and kept in a local state, so that the changes of the user are shown
        // immediately, without the need to wait for the next change of the preferences.
        var arrangement by remember { mutableStateOf(prefs.smartbar.actionArrangement.get()) }
        val availableActions = remember {
            (QuickActionArrangement.Default.dynamicActions +
                listOfNotNull(QuickActionArrangement.Default.stickyAction)).distinct()
        }

        fun save(newArrangement: QuickActionArrangement) {
            arrangement = newArrangement.distinct()
            scope.launch {
                prefs.smartbar.actionArrangement.set(arrangement)
            }
        }

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__quick_actions__group_sticky__label)) {
            for (action in availableActions) {
                val isSticky = arrangement.stickyAction == action
                Preference(
                    title = action.computeActionName(),
                    summary = if (isSticky) {
                        stringRes(R.string.pref__smartbar__quick_actions__selected__summary)
                    } else {
                        null
                    },
                    onClick = { save(arrangement.copy(stickyAction = action)) },
                )
            }
        }

        PreferenceGroup(title = stringRes(R.string.pref__smartbar__quick_actions__group_dynamic__label)) {
            for (action in availableActions) {
                val isDynamic = arrangement.dynamicActions.contains(action)
                Preference(
                    title = action.computeActionName(),
                    onClick = {
                        val newActions = if (isDynamic) {
                            arrangement.dynamicActions.filter { it != action }
                        } else {
                            arrangement.dynamicActions + action
                        }
                        save(arrangement.copy(dynamicActions = newActions))
                    },
                    trailing = {
                        Row {
                            IconButton(onClick = { save(arrangement.copy(stickyAction = action)) }) {
                                Icon(
                                    imageVector = if (arrangement.stickyAction == action) {
                                        Icons.Default.PushPin
                                    } else {
                                        Icons.Outlined.PushPin
                                    },
                                    contentDescription = stringRes(
                                        R.string.pref__smartbar__quick_actions__set_sticky__summary,
                                    ),
                                )
                            }
                            Icon(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                imageVector = if (isDynamic) {
                                    Icons.Default.CheckBox
                                } else {
                                    Icons.Default.CheckBoxOutlineBlank
                                },
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        }
    }
}
