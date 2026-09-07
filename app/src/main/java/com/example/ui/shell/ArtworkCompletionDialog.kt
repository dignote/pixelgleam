package com.example.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ArtworkCompletionDialog(
    artworkTitle: String,
    artworkId: String,
    onNextArtwork: () -> Unit,
    onBackToGallery: () -> Unit,
    onReplay: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("artwork_completion_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Golden Celebration Emblem
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3CD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Celebration",
                        tint = Color(0xFFD48806),
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Masterpiece Completed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "100% of $artworkTitle has been faceted with brilliant gemstones!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Artwork preview thumbnail
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ArtworkThumbnail(
                        artworkId = artworkId,
                        modifier = Modifier.size(140.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Next Artwork action button
                Button(
                    onClick = onNextArtwork,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("completion_next_button")
                ) {
                    Icon(imageVector = Icons.Default.NavigateNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Next Artwork", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Back to Gallery button
                OutlinedButton(
                    onClick = onBackToGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("completion_gallery_button")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Back to Gallery")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onReplay,
                        modifier = Modifier.testTag("completion_replay_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Replay")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("completion_admire_button")
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Admire Canvas")
                    }
                }
            }
        }
    }
}
