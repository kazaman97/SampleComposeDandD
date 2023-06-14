package com.github.kazaman97.sample_compose_d_and_d

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kazaman97.sample_compose_d_and_d.ui.theme.SampleComposeDandDTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SampleComposeDandDTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    var samples by remember {
                        mutableStateOf(
                            (1..100).map {
                                Sample(
                                    id = it.toLong(),
                                    text = "id: $it"
                                )
                            }
                        )
                    }

                    SampleList(
                        samples = samples,
                        onDelete = { id ->
                            samples = samples.toMutableList().apply {
                                removeIf { it.id == id }
                            }
                        },
                        onMove = { fromIndex, toIndex ->
                            samples = samples.toMutableList().apply {
                                val removeItem = removeAt(fromIndex)
                                add(toIndex, removeItem)
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class Sample(
    val id: Long,
    val text: String
)

// visibleItemsInfoに入っている配列から取得する必要があるため、絶対位置から相対位置indexに変換して取得する
fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? =
    this.layoutInfo.visibleItemsInfo.getOrNull(
        absoluteIndex - this.layoutInfo.visibleItemsInfo.first().index
    )

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SampleList(
    samples: List<Sample>,
    onDelete: (id: Long) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var currentIndexOfDraggedItem: Int? by remember { mutableStateOf(null) }
    var initiallyDraggedElement: LazyListItemInfo? by remember { mutableStateOf(null) }
    var draggedDistance by remember { mutableStateOf(0f) }
    var overScrollJob: Job? by remember { mutableStateOf(null) }

    fun checkForOverScroll(): Float = initiallyDraggedElement?.let {
        val startOffset = it.offset + draggedDistance
        val endOffset = it.offset + it.size + draggedDistance
        val viewPortStart = lazyListState.layoutInfo.viewportStartOffset
        val viewPortEnd = lazyListState.layoutInfo.viewportEndOffset

        when {
            draggedDistance > 0 -> (endOffset - viewPortEnd).takeIf { diff -> diff > 0 }
            draggedDistance < 0 -> (startOffset - viewPortStart).takeIf { diff -> diff < 0 }
            else -> null
        }
    } ?: 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        // 変更イベントを消費して他のHandlerで再び変更イベントが流れないようにする
                        change.consume()
                        // y軸の移動距離を保持する
                        draggedDistance += dragAmount.y

                        initiallyDraggedElement?.also {
                            val startOffset = it.offset + draggedDistance
                            val endOffset = (it.offset + it.size) + draggedDistance

                            val currentElement = currentIndexOfDraggedItem?.let { index ->
                                lazyListState.getVisibleItemInfoFor(absoluteIndex = index)
                            }
                            currentElement?.also { hovered ->
                                // DragしているItemと重なっているItemを探す(これは複数取得できる可能性がある)
                                lazyListState.layoutInfo.visibleItemsInfo
                                    .filterNot { item ->
                                        (item.offset + item.size) < startOffset || item.offset > endOffset
                                    }
                                    .firstOrNull { item ->
                                        // Drag方向に応じて取得するItemを判定する
                                        // 正の値ならincrementしたitem
                                        // 負の値ならdecrementしたitem
                                        val delta = startOffset - hovered.offset
                                        if (delta > 0) {
                                            endOffset > (item.offset + item.size)
                                        } else {
                                            startOffset < item.offset
                                        }
                                    }
                                    ?.also { item ->
                                        // itemの位置が変わっていることを伝える
                                        currentIndexOfDraggedItem?.also { currentIndex ->
                                            onMove(currentIndex, item.index)
                                        }
                                        // DragしているItemのIndexを変更する
                                        currentIndexOfDraggedItem = item.index
                                    }
                            }
                        }

                        if (overScrollJob?.isActive == true) return@detectDragGesturesAfterLongPress

                        checkForOverScroll()
                            .takeIf { offset -> offset != 0f }
                            ?.let { offset ->
                                overScrollJob = coroutineScope.launch {
                                    lazyListState.scrollBy(offset)
                                }
                            }
                            ?: run { overScrollJob?.cancel() }
                    },
                    onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // LongPressしているItemを探す
                        val findItem =
                            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                offset.y.toInt() in item.offset..(item.offset + item.size)
                            } ?: return@detectDragGesturesAfterLongPress
                        // LongPressしているItemを見つけたら index & item自体を保持する
                        // itemにindex情報が入っているため、item自体を保持するならいらない気がする
                        currentIndexOfDraggedItem = findItem.index
                        initiallyDraggedElement = findItem
                    },
                    onDragEnd = {
                        // Dragが完了したら、y軸の移動距離を初期化する
                        draggedDistance = 0f
                        currentIndexOfDraggedItem = null
                        initiallyDraggedElement = null
                    },
                    onDragCancel = {
                        // Dragがキャンセルされたら、y軸の移動距離を初期化する
                        draggedDistance = 0f
                        currentIndexOfDraggedItem = null
                        initiallyDraggedElement = null
                    }
                )
            },
        state = lazyListState
    ) {
        itemsIndexed(
            items = samples,
            key = { _, item -> item.id }
        ) { index, sample ->
            val dismissState = rememberDismissState(
                confirmStateChange = {
                    if (it == DismissValue.DismissedToStart) onDelete(sample.id)
                    true
                }
            )

            SwipeToDismiss(
                // graphicsLayerを使ってdrag距離に応じ、DragしているItemの位置を変更する
                modifier = Modifier
                    .semantics {
                        customActions = buildList {
                            val prevIndex = index.dec()
                            val nextIndex = index.inc()

                            if (0 <= prevIndex) {
                                add(
                                    CustomAccessibilityAction(
                                        label = "id:${samples[prevIndex].id}と入れ替える",
                                        action = {
                                            onMove(index, prevIndex)
                                            true
                                        }
                                    )
                                )
                            }

                            if (nextIndex <= (samples.size - 1)) {
                                add(
                                    CustomAccessibilityAction(
                                        label = "id:${samples[nextIndex].id}と入れ替える",
                                        action = {
                                            onMove(index, nextIndex)
                                            true
                                        }
                                    )
                                )
                            }

                            add(
                                CustomAccessibilityAction(
                                    label = "削除",
                                    action = {
                                        onDelete(sample.id)
                                        true
                                    }
                                )
                            )
                        }
                    }
                    .graphicsLayer {
                        // DragしているItemのときだけItemのpositionを変更しDrag移動できていることを表現する
                        if (index != currentIndexOfDraggedItem) return@graphicsLayer
                        translationY = currentIndexOfDraggedItem
                            ?.let { lazyListState.getVisibleItemInfoFor(absoluteIndex = it) }
                            ?.let { item ->
                                // 初期位置からどれだけ移動しかつDragの結果Itemの位置に変動があればその分も差し引いて現在のDrag位置を算出する
                                (initiallyDraggedElement?.offset ?: 0f)
                                    .toFloat() + draggedDistance - item.offset
                            } ?: 0f
                    }
                    .clickable { },
                state = dismissState,
                directions = setOf(DismissDirection.EndToStart),
                background = {
                    val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                    val color by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            DismissValue.Default -> Color.LightGray
                            DismissValue.DismissedToEnd -> Color.Green
                            DismissValue.DismissedToStart -> Color.Red
                        }
                    )
                    val alignment = when (direction) {
                        DismissDirection.StartToEnd -> Alignment.CenterStart
                        DismissDirection.EndToStart -> Alignment.CenterEnd
                    }
                    val icon = when (direction) {
                        DismissDirection.StartToEnd -> Icons.Default.Done
                        DismissDirection.EndToStart -> Icons.Default.Delete
                    }
                    val scale by animateFloatAsState(
                        targetValue = if (dismissState.targetValue == DismissValue.Default) {
                            0.75f
                        } else {
                            1f
                        }
                    )

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Localized description",
                            modifier = Modifier.scale(scale)
                        )
                    }
                },
                dismissContent = {
                    SampleItem(text = sample.text)
                }
            )
            Divider()
        }
    }
}

@Composable
private fun SampleItem(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 4.dp)
            .heightIn(min = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            color = if (isSystemInDarkTheme()) {
                Color.White
            } else {
                Color.Black
            }
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SampleItemPreview() {
    SampleComposeDandDTheme {
        SampleItem("id: 1")
    }
}
