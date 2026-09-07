package com.example.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyArtScreen(
    artworks: List<ArtworkSummaryItem>,
    currentFilter: MyArtFilter,
    onFilterChanged: (MyArtFilter) -> Unit,
    onSelectArtwork: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val inProgressCount = artworks.count { it.status == ArtworkStatus.IN_PROGRESS }
    val completedCount = artworks.count { it.status == ArtworkStatus.COMPLETED }
    val newCount = artworks.count { it.status == ArtworkStatus.NEW }

    val filteredArtworks = when (currentFilter) {
        MyArtFilter.ALL -> artworks
        MyArtFilter.IN_PROGRESS -> artworks.filter { it.status == ArtworkStatus.IN_PROGRESS }
        MyArtFilter.COMPLETED -> artworks.filter { it.status == ArtworkStatus.COMPLETED }
        MyArtFilter.NEW -> artworks.filter { it.status == ArtworkStatus.NEW }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "My Art",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("my_art_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary Metrics Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // In Progress
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).testTag("stat_in_progress")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$inProgressCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "In Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Completed
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).testTag("stat_completed")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$completedCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD48806)
                        )
                        Text(
                            text = "Completed ✨",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).testTag("stat_total")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${artworks.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Mosaics",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MyArtFilter.values().forEach { filter ->
                    val selected = currentFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterChanged(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    MyArtFilter.ALL -> "All (${artworks.size})"
                                    MyArtFilter.IN_PROGRESS -> "In Progress ($inProgressCount)"
                                    MyArtFilter.COMPLETED -> "Completed ($completedCount)"
                                    MyArtFilter.NEW -> "New ($newCount)"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("my_art_filter_${filter.name.lowercase()}")
                    )
                }
            }

            // Content Grid or Empty State
            if (filteredArtworks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentFilter) {
                                    MyArtFilter.COMPLETED -> Icons.Default.CheckCircle
                                    MyArtFilter.IN_PROGRESS -> Icons.Default.Timeline
                                    else -> Icons.Default.Palette
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when (currentFilter) {
                                MyArtFilter.COMPLETED -> "No Completed Mosaics Yet"
                                MyArtFilter.IN_PROGRESS -> "No Mosaics In Progress"
                                MyArtFilter.NEW -> "All Mosaics Have Progress"
                                else -> "No Mosaics Found"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (currentFilter) {
                                MyArtFilter.COMPLETED -> "Complete your first gemstone mosaic from the Gallery to see it shining here!"
                                MyArtFilter.IN_PROGRESS -> "Start coloring any artwork to pick up where you left off."
                                else -> "Select an artwork to start coloring."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag("my_art_grid")
                ) {
                    items(filteredArtworks, key = { it.id }) { artwork ->
                        MyArtCard(
                            artwork = artwork,
                            onClick = { onSelectArtwork(artwork.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyArtCard(
    artwork: ArtworkSummaryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("my_art_item_${artwork.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ArtworkThumbnail(
                    artworkId = artwork.id,
                    modifier = Modifier.fillMaxSize()
                )

                // Status Badge Overlay
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                    color = when (artwork.status) {
                        ArtworkStatus.NEW -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ArtworkStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ArtworkStatus.COMPLETED -> Color(0xFFFFF3CD).copy(alpha = 0.95f)
                    },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = when (artwork.status) {
                            ArtworkStatus.NEW -> "New"
                            ArtworkStatus.IN_PROGRESS -> "${artwork.progressPercent.toInt()}%"
                            ArtworkStatus.COMPLETED -> "Done ✨"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (artwork.status) {
                            ArtworkStatus.NEW -> MaterialTheme.colorScheme.onSurface
                            ArtworkStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
                            ArtworkStatus.COMPLETED -> Color(0xFFD48806)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = artwork.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${artwork.totalRegions} Regions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { artwork.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}
