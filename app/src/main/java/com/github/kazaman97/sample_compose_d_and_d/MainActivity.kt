package com.github.kazaman97.sample_compose_d_and_d

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kazaman97.sample_compose_d_and_d.ui.theme.SampleComposeDandDTheme

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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SampleList(
    samples: List<Sample>,
    onDelete: (id: Long) -> Unit
) {
    val lazyListState = rememberLazyListState()
    var currentIndexOfDraggedItem: Int? by remember { mutableStateOf(null) }
    var initiallyDraggedElement: LazyListItemInfo? by remember { mutableStateOf(null) }
    var draggedDistance by remember { mutableStateOf(0f) }

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
                    },
                    onDragStart = { offset ->
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
                    },
                    onDragCancel = {
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
                modifier = Modifier.graphicsLayer {
                    translationY = draggedDistance.takeIf {
                        index == currentIndexOfDraggedItem
                    } ?: 0f
                },
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
