package com.github.kazaman97.sample_compose_d_and_d

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                    SampleList(
                        samples = (1..10).map {
                            Sample(
                                id = it.toLong(),
                                text = "id: $it"
                            )
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

@Composable
private fun SampleList(samples: List<Sample>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = samples,
            key = { it.id }
        ) { sample ->
            SampleItem(text = sample.text)
            Divider()
        }
    }
}

@Composable
private fun SampleItem(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .heightIn(min = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text)
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
