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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A dropdown which shows the selected option and which opens a menu with all the available options.
 *
 * This dropdown is implemented in the app itself, because the dropdown of the preference library is not compatible
 * with the material3 version which is used by this app and would therefore crash at runtime.
 *
 * @param options The available options.
 * @param selectedOptionIndex The index of the option which is currently selected.
 * @param onSelectOption The callback which is invoked with the index of the selected option.
 * @param modifier The modifier of this composable.
 * @param expanded The state which holds whether the menu of the options is shown.
 * @param isError Whether the selection should be marked as invalid.
 * @param enabled Whether the dropdown can be opened.
 * @param labelText The label which is shown in the dropdown.
 * @param optionsLabelProvider The provider of the labels of the options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FlorisDropdown(
    options: List<T>,
    selectedOptionIndex: Int,
    onSelectOption: (Int) -> Unit,
    modifier: Modifier = Modifier,
    expanded: MutableState<Boolean> = remember { mutableStateOf(false) },
    isError: Boolean = false,
    enabled: Boolean = true,
    labelText: String? = null,
    optionsLabelProvider: (T) -> String = { it.toString() },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed && enabled) {
            expanded.value = true
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = options.getOrNull(selectedOptionIndex)?.let(optionsLabelProvider).orEmpty(),
            onValueChange = {},
            enabled = enabled,
            readOnly = true,
            isError = isError,
            singleLine = true,
            label = labelText?.let { text -> { Text(text = text) } },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
            },
            interactionSource = interactionSource,
        )
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(text = optionsLabelProvider(option)) },
                    onClick = {
                        expanded.value = false
                        onSelectOption(index)
                    },
                )
            }
        }
    }
}
