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

package dev.patrickgold.florisboard.ime.clipboard

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardFileStorage
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.media.KeyboardLikeButton
import dev.patrickgold.florisboard.ime.smartbar.AnimationDuration
import dev.patrickgold.florisboard.ime.smartbar.VerticalEnterTransition
import dev.patrickgold.florisboard.ime.smartbar.VerticalExitTransition
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.observeAsTransformingState
import dev.patrickgold.florisboard.lib.util.NetworkUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import org.florisboard.lib.android.AndroidKeyguardManager
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.showShortToastSync
import org.florisboard.lib.android.systemService
import org.florisboard.lib.compose.LocalLocalizedDateTimeFormatter
import org.florisboard.lib.compose.autoMirrorForRtl
import org.florisboard.lib.compose.florisHorizontalScroll
import org.florisboard.lib.compose.florisVerticalScroll
import org.florisboard.lib.compose.rippleClickable
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggChip
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

private val ItemWidth = 200.dp
private val DialogWidth = 240.dp

/** The duration of the swipe animation in milliseconds. */
private const val SwipeAnimationDuration = 180

/** The factor of the item width which needs to be swiped to delete the item. */
private const val DeleteSwipeThresholdFactor = 0.3f

/** The pixel distance which is used to fade in the delete indicator. */
private const val DeleteSwipeThresholdPx = 64f

const val CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO: Int = 0

@Composable
fun ClipboardInputLayout(
    modifier: Modifier = Modifier,
) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()
    val keyboardManager by context.keyboardManager()
    val androidKeyguardManager = remember { context.systemService(AndroidKeyguardManager::class) }

    val deviceLocked = androidKeyguardManager.let { it.isDeviceLocked || it.isKeyguardLocked }
    val historyEnabled by prefs.clipboard.historyEnabled.observeAsState()
    val persistedActiveTab by prefs.clipboard.activeTab.observeAsState()

    var isFilterRowShown by remember { mutableStateOf(false) }
    val activeFilterTypes = remember { mutableStateSetOf<ItemType>() }

    /**
     * The active tab is kept in a local state, so that the ui reacts immediately on a tab change. The preference
     * is only used to remember the last active tab across sessions.
     */
    var activeTabOverride by remember { mutableStateOf<ClipboardTab?>(null) }
    val activeTab = activeTabOverride ?: persistedActiveTab
    fun selectTab(tab: ClipboardTab) {
        if (tab != activeTab) {
            activeTabOverride = tab
            scope.launch { prefs.clipboard.activeTab.set(tab) }
        }
    }

    /**
     * Whether the user is currently selecting items in order to delete them in bulk.
     */
    var selectionMode by remember { mutableStateOf(false) }
    val selectedItemIds = remember { mutableStateSetOf<Long>() }
    fun enterSelectionMode(item: ClipboardItem? = null) {
        selectionMode = true
        selectedItemIds.clear()
        if (item != null) {
            selectedItemIds.add(item.id)
        }
    }
    fun leaveSelectionMode() {
        selectionMode = false
        selectedItemIds.clear()
    }

    val unfilteredHistory by clipboardManager.historyFlow.collectAsState()
    val filteredHistory = remember(unfilteredHistory, activeFilterTypes.toSet()) {
        if (activeFilterTypes.isEmpty()) {
            unfilteredHistory
        } else {
            unfilteredHistory.all
                .filter { activeFilterTypes.contains(it.type) }
                .let { ClipboardHistory(it) }
        }
    }

    /**
     * The items which are currently visible, based on the active tab.
     */
    val visibleItems = remember(filteredHistory, activeTab) {
        when (activeTab) {
            ClipboardTab.PINNED -> filteredHistory.pinned
            ClipboardTab.RECENT -> filteredHistory.recent + filteredHistory.other
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    var popupItem by remember(filteredHistory) { mutableStateOf<ClipboardItem?>(null) }
    var showClearAllHistory by remember { mutableStateOf(false) }

    fun isPopupSurfaceActive() = popupItem != null || showClearAllHistory

    LaunchedEffect(isFilterRowShown) {
        delay(AnimationDuration.toLong())
        if (!isFilterRowShown) {
            activeFilterTypes.clear()
        }
    }

    LaunchedEffect(activeFilterTypes.toSet(), activeTab) {
        // Wait until the grid actually has items, otherwise scrolling to the first item would fail. This is
        // important for tabs which do not contain any item at all.
        snapshotFlow { gridState.layoutInfo.totalItemsCount }.first { it > 0 }
        // The grid may be empty again in the meantime, for example if the items of the previous tab are still
        // counted while the new tab is empty. Therefore the scroll is guarded against failures.
        runCatching { gridState.scrollToItem(0) }
    }

    @Composable
    fun HeaderRow() {
        SnyggRow(FlorisImeUi.ClipboardHeader.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sizeModifier = Modifier
                .sizeIn(maxHeight = FlorisImeSizing.smartbarHeight)
                .aspectRatio(1f)
            if (selectionMode) {
                // In the selection mode the back button leaves the selection mode instead of the clipboard
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = { leaveSelectionMode() },
                    modifier = sizeModifier,
                ) {
                    SnyggIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    )
                }
                SnyggText(
                    elementName = FlorisImeUi.ClipboardHeaderText.elementName,
                    modifier = Modifier.weight(1f),
                    text = stringRes(R.string.clipboard__selection__count, "count" to selectedItemIds.size),
                )
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = {
                        if (selectedItemIds.size == visibleItems.size) {
                            selectedItemIds.clear()
                        } else {
                            selectedItemIds.clear()
                            visibleItems.forEach { selectedItemIds.add(it.id) }
                        }
                    },
                    modifier = sizeModifier,
                    enabled = visibleItems.isNotEmpty() && !isPopupSurfaceActive(),
                ) {
                    SnyggIcon(
                        imageVector = Icons.Default.SelectAll,
                    )
                }
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = {
                        val itemsToDelete = filteredHistory.all.filter { selectedItemIds.contains(it.id) }
                        clipboardManager.deleteClips(itemsToDelete)
                        leaveSelectionMode()
                    },
                    modifier = sizeModifier,
                    enabled = selectedItemIds.isNotEmpty() && !isPopupSurfaceActive(),
                ) {
                    SnyggIcon(
                        imageVector = Icons.Default.Delete,
                    )
                }
            } else {
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = { keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT },
                    modifier = sizeModifier,
                ) {
                    SnyggIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    )
                }
                SnyggText(
                    elementName = FlorisImeUi.ClipboardHeaderText.elementName,
                    modifier = Modifier.weight(1f),
                    text = stringRes(R.string.clipboard__header_title),
                )
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = { enterSelectionMode() },
                    modifier = sizeModifier,
                    enabled = !deviceLocked && historyEnabled && filteredHistory.all.isNotEmpty() && !isPopupSurfaceActive(),
                ) {
                    SnyggIcon(
                        imageVector = Icons.Default.Checklist,
                    )
                }
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = { scope.launch { prefs.clipboard.historyEnabled.set(!historyEnabled) } },
                    modifier = sizeModifier.autoMirrorForRtl(),
                    enabled = !deviceLocked && !isPopupSurfaceActive(),
                ) {
                    SnyggIcon(
                        imageVector = if (historyEnabled) {
                            Icons.Default.ToggleOn
                        } else {
                            Icons.Default.ToggleOff
                        },
                    )
                }
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = { showClearAllHistory = true },
                    modifier = sizeModifier.autoMirrorForRtl(),
                    enabled = !deviceLocked && historyEnabled && filteredHistory.all.isNotEmpty() && !isPopupSurfaceActive(),
                ) {
                    SnyggIcon(
                        imageVector = Icons.Default.DeleteSweep,
                    )
                }
                SnyggIconButton(
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                    onClick = { isFilterRowShown = !isFilterRowShown },
                    modifier = sizeModifier,
                    enabled = !deviceLocked && historyEnabled && unfilteredHistory.all.isNotEmpty() && !isPopupSurfaceActive(),
                ) {
                    SnyggIcon(
                        imageVector = if (!isFilterRowShown) {
                            Icons.Default.FilterList
                        } else {
                            Icons.Default.FilterListOff
                        },
                    )
                }
                KeyboardLikeButton(
                    modifier = sizeModifier,
                    inputEventDispatcher = keyboardManager.inputEventDispatcher,
                    keyData = TextKeyData.DELETE,
                    elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                ) {
                    SnyggIcon(imageVector = Icons.AutoMirrored.Outlined.Backspace)
                }
            }
        }
    }

    @Composable
    fun ClipItemView(
        elementName: String,
        item: ClipboardItem,
        contentScrollInsteadOfClip: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val attributes = remember(item) {
            mapOf("type" to item.type.toString().lowercase())
        }
        val isSelected = selectedItemIds.contains(item.id)
        SnyggBox(
            elementName = elementName,
            attributes = attributes,
            modifier = modifier.fillMaxWidth(),
            clickAndSemanticsModifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = popupItem == null,
                onLongClick = {
                    if (selectionMode) {
                        if (!selectedItemIds.remove(item.id)) {
                            selectedItemIds.add(item.id)
                        }
                    } else {
                        enterSelectionMode(item)
                    }
                },
                onClick = {
                    if (selectionMode) {
                        if (!selectedItemIds.remove(item.id)) {
                            selectedItemIds.add(item.id)
                        }
                    } else {
                        clipboardManager.pasteItem(item)
                    }
                },
            ),
        ) {
            if (selectionMode) {
                // Show a checkbox on the item, so that the user can see which items are selected
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                ) {
                    SnyggIcon(
                        elementName = FlorisImeUi.ClipboardItemCheckbox.elementName,
                        imageVector = if (isSelected) {
                            Icons.Outlined.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                    )
                }
            }
            if (item.type == ItemType.IMAGE) {
                val id = ContentUris.parseId(item.uri!!)
                val file = ClipboardFileStorage.getFileForId(context, id)
                val bitmap = remember(id) {
                    runCatching {
                        check(file.exists()) { "Unable to resolve image at ${file.absolutePath}" }
                        val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
                        checkNotNull(rawBitmap) { "Unable to decode image at ${file.absolutePath}" }
                        rawBitmap.asImageBitmap()
                    }
                }
                if (bitmap.isSuccess) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        bitmap = bitmap.getOrThrow(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                    )
                } else {
                    SnyggText(
                        modifier = Modifier.fillMaxWidth(),
                        text = bitmap.exceptionOrNull()?.message ?: "Unknown error",
                    )
                }
            } else if (item.type == ItemType.VIDEO) {
                val id = ContentUris.parseId(item.uri!!)
                val file = ClipboardFileStorage.getFileForId(context, id)
                val bitmap = remember(id) {
                    runCatching {
                        check(file.exists()) { "Unable to resolve video at ${file.absolutePath}" }
                        val rawBitmap = if (AndroidVersion.ATLEAST_API29_Q) {
                            val dataRetriever = MediaMetadataRetriever()
                            dataRetriever.setDataSource(file.absolutePath)
                            val width = dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            val height = dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ThumbnailUtils.createVideoThumbnail(file, Size(width!!.toInt(), height!!.toInt()), null)
                        } else {
                            @Suppress("DEPRECATION")
                            ThumbnailUtils.createVideoThumbnail(file.absolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
                        }
                        checkNotNull(rawBitmap) { "Unable to decode video at ${file.absolutePath}" }
                        rawBitmap.asImageBitmap()
                    }
                }
                if (bitmap.isSuccess) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        bitmap = bitmap.getOrThrow(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                    )
                    Icon(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 4.dp, bottom = 4.dp)
                            .background(Color.White, CircleShape),
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.Black,
                    )
                } else {
                    SnyggText(
                        modifier = Modifier.fillMaxWidth(),
                        text = bitmap.exceptionOrNull()?.message ?: "Unknown error",
                    )
                }
            } else {
                val text = item.stringRepresentation()
                Column {
                    ClipTextItemDescription(
                        elementName = FlorisImeUi.ClipboardItemDescription.elementName,
                        attributes = attributes,
                        text = text,
                    )
                    SnyggText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .run { if (contentScrollInsteadOfClip) this.florisVerticalScroll() else this },
                        text = item.displayText(),
                    )
                }
            }
        }
    }

    @Composable
    fun ClipboardTabRow() {
        SnyggRow(
            elementName = FlorisImeUi.ClipboardTabRow.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (tab in ClipboardTab.entries) {
                val isActive = tab == activeTab
                val attributes = remember(isActive) {
                    mapOf("state" to if (isActive) "active" else "inactive")
                }
                SnyggBox(
                    elementName = FlorisImeUi.ClipboardTab.elementName,
                    attributes = attributes,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .rippleClickable {
                            selectTab(tab)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.ClipboardTabText.elementName,
                        attributes = attributes,
                        text = stringRes(tab.labelRes),
                    )
                }
            }
        }
    }

    @Composable
    fun HistoryMainView() {
        SnyggBox(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
        ) {
            val historyAlpha by animateFloatAsState(targetValue = if (isPopupSurfaceActive()) 0.12f else 1f)
            val staggeredGridCells by prefs.clipboard.historyNumGridColumns()
                .observeAsTransformingState { numGridColumns ->
                    if (numGridColumns == CLIPBOARD_HISTORY_NUM_GRID_COLUMNS_AUTO) {
                        StaggeredGridCells.Adaptive(160.dp)
                    } else {
                        StaggeredGridCells.Fixed(numGridColumns)
                    }
                }

            fun LazyStaggeredGridScope.clipboardItems(
                items: List<ClipboardItem>,
                key: String,
                @StringRes title: Int,
            ) {
                if (items.isNotEmpty()) {
                    item(key, span = StaggeredGridItemSpan.FullLine) {
                        ClipCategoryTitle(text = stringRes(title))
                    }
                    items(items) { item ->
                        if (selectionMode) {
                            ClipItemView(
                                elementName = FlorisImeUi.ClipboardItem.elementName,
                                item = item,
                                contentScrollInsteadOfClip = false,
                            )
                        } else {
                            SwipeToDeleteItem(
                                onDelete = {
                                    clipboardManager.deleteClip(item, onlyIfUnpinned = false)
                                },
                            ) {
                                ClipItemView(
                                    elementName = FlorisImeUi.ClipboardItem.elementName,
                                    item = item,
                                    contentScrollInsteadOfClip = false,
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(historyAlpha),
            ) {
                AnimatedVisibility(
                    visible = isFilterRowShown,
                    enter = VerticalEnterTransition,
                    exit = VerticalExitTransition,
                ) {
                    SnyggRow(
                        elementName = FlorisImeUi.ClipboardFilterRow.elementName,
                        modifier = Modifier.fillMaxWidth(),
                        clickAndSemanticsModifier = Modifier.florisHorizontalScroll(),
                    ) {
                        @Composable
                        fun FilterChip(
                            imageVector: ImageVector,
                            text: String,
                            itemType: ItemType,
                        ) {
                            val active = activeFilterTypes.contains(itemType)
                            val attributes = remember(active) {
                                mapOf(
                                    "state" to if (active) "active" else "inactive",
                                    "type" to itemType.toString().lowercase(),
                                )
                            }
                            SnyggChip(
                                elementName = FlorisImeUi.ClipboardFilterChip.elementName,
                                attributes = attributes,
                                onClick = {
                                    if (!activeFilterTypes.add(itemType)) {
                                        activeFilterTypes.remove(itemType)
                                    }
                                },
                                imageVector = imageVector,
                                text = text,
                            )
                        }

                        FilterChip(
                            imageVector = Icons.Default.TextFields,
                            text = "Text",
                            itemType = ItemType.TEXT,
                        )
                        FilterChip(
                            imageVector = Icons.Default.Image,
                            text = "Images",
                            itemType = ItemType.IMAGE,
                        )
                        FilterChip(
                            imageVector = Icons.Default.Movie,
                            text = "Videos",
                            itemType = ItemType.VIDEO,
                        )
                    }
                }
                SnyggBox(FlorisImeUi.ClipboardGrid.elementName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    LazyVerticalStaggeredGrid(
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        columns = staggeredGridCells,
                    ) {
                        when (activeTab) {
                            ClipboardTab.PINNED -> clipboardItems(
                                items = filteredHistory.pinned,
                                key = "pinned-header",
                                title = R.string.clipboard__group_pinned,
                            )
                            ClipboardTab.RECENT -> {
                                clipboardItems(
                                    items = filteredHistory.recent,
                                    key = "recent-header",
                                    title = R.string.clipboard__group_recent,
                                )
                                clipboardItems(
                                    items = filteredHistory.other,
                                    key = "other-header",
                                    title = R.string.clipboard__group_other,
                                )
                            }
                        }
                    }
                }
            }

            if (popupItem != null) {
                SnyggRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { popupItem = null }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    SnyggColumn(modifier = Modifier.weight(0.5f)) {
                        ClipItemView(
                            elementName = FlorisImeUi.ClipboardItemPopup.elementName,
                            modifier = Modifier
                                .widthIn(max = ItemWidth)
                                .weight(1f, fill = false),
                            item = popupItem!!,
                            contentScrollInsteadOfClip = true,
                        )
                        SnyggBox(FlorisImeUi.ClipboardItemTimestamp.elementName) {
                            val formatter = LocalLocalizedDateTimeFormatter.current
                            SnyggText(
                                modifier = Modifier.fillMaxWidth(),
                                text = formatter.format(Instant.ofEpochMilli(popupItem!!.creationTimestampMs)),
                            )
                        }
                    }
                    SnyggColumn(modifier = Modifier.weight(0.5f)) {
                        SnyggColumn(FlorisImeUi.ClipboardItemActions.elementName) {
                            PopupAction(
                                icon = Icons.Outlined.PushPin,
                                text = stringRes(if (popupItem!!.isPinned) {
                                    R.string.clip__unpin_item
                                } else {
                                    R.string.clip__pin_item
                                }),
                            ) {
                                if (popupItem!!.isPinned) {
                                    clipboardManager.unpinClip(popupItem!!)
                                } else {
                                    clipboardManager.pinClip(popupItem!!)
                                }
                                popupItem = null
                            }
                            PopupAction(
                                icon = Icons.Default.Delete,
                                text = stringRes(R.string.clip__delete_item),
                            ) {
                                clipboardManager.deleteClip(popupItem!!, onlyIfUnpinned = false)
                                popupItem = null
                            }
                            PopupAction(
                                icon = Icons.Outlined.ContentPasteGo,
                                text = stringRes(R.string.clip__paste_item),
                            ) {
                                clipboardManager.pasteItem(popupItem!!)
                                popupItem = null
                            }
                        }
                    }
                }
            }

            if (showClearAllHistory) {
                SnyggRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showClearAllHistory = false }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    SnyggColumn(
                        elementName = FlorisImeUi.ClipboardClearAllDialog.elementName,
                        modifier = Modifier
                            .width(DialogWidth)
                            .pointerInput(Unit) {
                                detectTapGestures { /* Do nothing */ }
                            },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.ClipboardClearAllDialogMessage.elementName,
                            text = stringRes(
                                if (isFilterRowShown) {
                                    R.string.clipboard__confirm_clear_filtered_history__message
                                } else {
                                    R.string.clipboard__confirm_clear_unfiltered_history__message
                                }
                            ),
                        )
                        SnyggRow(FlorisImeUi.ClipboardClearAllDialogButtons.elementName) {
                            Spacer(modifier = Modifier.weight(1f))
                            SnyggButton(
                                elementName = FlorisImeUi.ClipboardClearAllDialogButton.elementName,
                                attributes = mapOf("action" to "no"),
                                onClick = {
                                    showClearAllHistory = false
                                },
                            ) {
                                SnyggText(
                                    text = stringRes(R.string.action__no),
                                )
                            }
                            SnyggButton(
                                elementName = FlorisImeUi.ClipboardClearAllDialogButton.elementName,
                                attributes = mapOf("action" to "yes"),
                                onClick = {
                                    val itemsToClear = when (activeTab) {
                                        ClipboardTab.PINNED -> filteredHistory.pinned
                                        ClipboardTab.RECENT -> filteredHistory.unpinned
                                    }
                                    clipboardManager.clearExactHistory(itemsToClear)
                                    context.showShortToastSync(R.string.clipboard__cleared_history)
                                    showClearAllHistory = false
                                },
                            ) {
                                SnyggText(
                                    text = stringRes(R.string.action__yes),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HistoryEmptyView() {
        SnyggColumn(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                text = stringRes(R.string.clipboard__empty__title),
            )
            SnyggText(
                text = stringRes(R.string.clipboard__empty__message),
            )
            if (filteredHistory.all.isNotEmpty()) {
                // The current tab is empty, but there are items in the other tab. Hint the user at the
                // swipe gesture, so that they know how to delete items.
                SnyggText(
                    text = stringRes(R.string.clipboard__swipe_to_delete__hint),
                )
            }
        }
    }

    @Composable
    fun HistoryDisabledView() {
        SnyggColumn(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
        ) {
            SnyggText(
                elementName = FlorisImeUi.ClipboardHistoryDisabledTitle.elementName,
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringRes(R.string.clipboard__disabled__title),
            )
            SnyggText(
                elementName = FlorisImeUi.ClipboardHistoryDisabledMessage.elementName,
                text = stringRes(R.string.clipboard__disabled__message),
            )
            SnyggButton(FlorisImeUi.ClipboardHistoryDisabledButton.elementName,
                onClick = { scope.launch { prefs.clipboard.historyEnabled.set(true) } },
                modifier = Modifier.align(Alignment.End),
            ) {
                SnyggText(
                    text = stringRes(R.string.clipboard__disabled__enable_button),
                )
            }
        }
    }

    @Composable
    fun HistoryLockedView() {
        SnyggColumn(FlorisImeUi.ClipboardContent.elementName,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                elementName = FlorisImeUi.ClipboardHistoryLockedTitle.elementName,
                text = stringRes(R.string.clipboard__locked__title),
            )
            SnyggText(
                elementName = FlorisImeUi.ClipboardHistoryLockedMessage.elementName,
                text = stringRes(R.string.clipboard__locked__message),
            )
        }
    }

    SnyggColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.imeUiHeight()),
    ) {
        HeaderRow()
        if (deviceLocked) {
            HistoryLockedView()
        } else {
            if (historyEnabled) {
                // The tab row is always shown, so that the user is able to switch between the tabs at any time
                ClipboardTabRow()
                if (visibleItems.isNotEmpty() || !activeFilterTypes.isEmpty()) {
                    HistoryMainView()
                } else {
                    HistoryEmptyView()
                }
            } else {
                HistoryDisabledView()
            }
        }
    }
}

@Composable
private fun ClipCategoryTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    SnyggText(FlorisImeUi.ClipboardSubheader.elementName,
        modifier = modifier.fillMaxWidth(),
        text = text.uppercase(),
    )
}

/**
 * A composable which wraps its content and allows the user to delete it by swiping it to one side.
 *
 * If the swipe distance exceeds a certain threshold, the item is swiped out of the view and the [onDelete]
 * callback is invoked. Otherwise, the item snaps back to its original position.
 *
 * @param onDelete The callback which is invoked if the item is swiped far enough to be deleted.
 * @param modifier The modifier of this composable.
 * @param content The content which should be swipeable.
 */
@Composable
private fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    Box(modifier = modifier) {
        // The background which becomes visible while the item is swiped away
        SnyggBox(
            elementName = FlorisImeUi.ClipboardItemDeleteBackground.elementName,
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            SnyggIcon(
                elementName = FlorisImeUi.ClipboardItemDeleteIcon.elementName,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .graphicsLayer {
                        alpha = (abs(offsetX.value) / DeleteSwipeThresholdPx).coerceIn(0f, 1f)
                    },
                imageVector = Icons.Default.Delete,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scope.launch { offsetX.stop() }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, tween(SwipeAnimationDuration)) }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (abs(offsetX.value) >= size.width * DeleteSwipeThresholdFactor) {
                                    val target = if (offsetX.value > 0) {
                                        size.width.toFloat()
                                    } else {
                                        -size.width.toFloat()
                                    }
                                    offsetX.animateTo(target, tween(SwipeAnimationDuration))
                                    onDelete()
                                } else {
                                    offsetX.animateTo(0f, tween(SwipeAnimationDuration))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        },
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun ClipTextItemDescription(
    elementName: String,
    attributes: SnyggQueryAttributes,
    text: String,
    modifier: Modifier = Modifier,
): Unit = with(LocalDensity.current) {
    val icon: ImageVector?
    val description: String?
    when {
        NetworkUtils.isEmailAddress(text) -> {
            icon = Icons.Outlined.Email
            description = stringRes(R.string.clipboard__item_description_email)
        }
        NetworkUtils.isUrl(text) -> {
            icon = Icons.Default.Link
            description = stringRes(R.string.clipboard__item_description_url)
        }
        NetworkUtils.isPhoneNumber(text) -> {
            icon = Icons.Default.Phone
            description = stringRes(R.string.clipboard__item_description_phone)
        }
        else -> {
            icon = null
            description = null
        }
    }
    if (icon != null && description != null) {
        SnyggRow(
            elementName = elementName,
            attributes = attributes,
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggIcon(
                imageVector = icon,
            )
            SnyggText(
                modifier = Modifier.weight(1f),
                text = description,
            )
        }
    }
}

@Composable
private fun PopupAction(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SnyggRow(FlorisImeUi.ClipboardItemAction.elementName,
        modifier = modifier.rippleClickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SnyggIcon(FlorisImeUi.ClipboardItemActionIcon.elementName,
            imageVector = icon,
        )
        SnyggText(FlorisImeUi.ClipboardItemActionText.elementName,
            modifier = Modifier.weight(1f),
            text = text,
        )
    }
}
