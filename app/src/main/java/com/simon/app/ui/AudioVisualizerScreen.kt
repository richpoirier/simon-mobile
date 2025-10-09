package com.simon.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.app.ui.components.AudioVisualizer
import com.simon.app.ui.theme.DarkBackground
import com.simon.app.ui.theme.MediumBackground
import com.simon.app.ui.theme.WhiteAlpha70

@Composable
fun AudioVisualizerScreen(
    viewModel: VoiceSessionViewModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(MediumBackground, DarkBackground, Color.Black),
                    radius = 800f
                )
            )
    ) {
        AudioVisualizer(
            isConnected = uiState.isConnected,
            isUserSpeaking = uiState.isUserSpeaking,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .size(280.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(color = DarkBackground.copy(alpha = 0.6f))
        ) {
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = WhiteAlpha70,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
