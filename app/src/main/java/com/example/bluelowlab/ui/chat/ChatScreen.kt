package com.example.bluelowlab.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

private const val TAG = "ChatScreen"

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        CurrentDeviceData(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.1f)
        )
        FoundDevicesList(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.4f)
        )
        ChatUserInput(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.1f)
        )
        ChatMessagesList(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.4f)
        )
    }
}

@Composable
fun CurrentDeviceData(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row() {
            Text(text = "I'm a Phone")
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Scan")
            }
        }
    }
}

@Composable
fun FoundDevicesList(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(text = "I'm a List of Devices")
    }
}

@Composable
fun ChatUserInput(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row {
            Text(text = "I'm Chat UI")
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Button")
            }
        }
    }
}

@Composable
fun ChatMessagesList(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(text = "I'm a List of Messages")
    }
}

val prevMod: Modifier = Modifier.fillMaxSize()
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ChatScreenPreview() {
    ChatScreen(
        modifier = prevMod
    )
}