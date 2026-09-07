package com.example.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val defaultCategoriesList = listOf("All")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    artworks: List<ArtworkSummaryItem>,
    dailyInfo: DailyChallengeInfo?,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onSelectArtwork: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesList = androidx.compose.runtime.remember(artworks) {
        val dynamicCategories = artworks
            .map { Pair(it.categoryId, it.category) }
            .distinctBy { pair -> pair.first.ifEmpty { pair.second } }
            .sortedBy { pair -> com.example.data.repository.CategoryTaxonomyContract.getCategoryOrderIndex(pair.first) }
            .map { pair -> pair.second }
        listOf("All") + dynamicCategories
    }

    val filteredArtworks = if (selectedCategory == "All") {
        artworks
    } else {
        artworks.filter {
            it.category.equals(selectedCategory, ignoreCase = true) ||
            it.categoryId.equals(selectedCategory, ignoreCase = true)
        }
    }

    // In-progress item for quick resume section
    val inProgressArtwork = artworks.firstOrNull { it.status == ArtworkStatus.IN_PROGRESS }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PixelGleam",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Faceted Gem Mosaic Atelier",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenTutorial,
                        modifier = Modifier.testTag("home_tutorial_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How to play",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("home_gallery_screen")
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("gallery_artwork_grid")
        ) {
            // 1. Daily Challenge Hero Banner
            if (dailyInfo != null) {
                item(span = { GridItemSpan(2) }) {
                    DailyMosaicHeroBanner(
                        dailyInfo = dailyInfo,
                        onClick = { onSelectArtwork(dailyInfo.featuredArtwork.id) }
                    )
                }
            }

            // 2. In-Progress Quick Resume Section (if any)
            if (inProgressArtwork != null && inProgressArtwork.id != dailyInfo?.featuredArtwork?.id) {
                item(span = { GridItemSpan(2) }) {
                    QuickResumeCard(
                        artwork = inProgressArtwork,
                        onClick = { onSelectArtwork(inProgressArtwork.id) }
                    )
                }
            }

            // 3. Category Filter Chips
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriesList.forEach { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = { Text(text = category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // 4. Section Header: Mosaics Catalog
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = if (selectedCategory == "All") "Gem Mosaics (${filteredArtworks.size})" else "$selectedCategory (${filteredArtworks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 5. Grid of Artworks
            items(filteredArtworks, key = { it.id }) { artwork ->
                GalleryArtworkCard(
                    artwork = artwork,
                    onClick = { onSelectArtwork(artwork.id) }
                )
            }
        }
    }
}

@Composable
fun DailyMosaicHeroBanner(
    dailyInfo: DailyChallengeInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("daily_hero_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ArtworkThumbnail(
                    artworkId = dailyInfo.featuredArtwork.id,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Today's Daily • ${dailyInfo.monthName} ${dailyInfo.dayNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dailyInfo.featuredArtwork.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${dailyInfo.featuredArtwork.category} • ${dailyInfo.featuredArtwork.totalRegions} Regions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { dailyInfo.featuredArtwork.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play Icon Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play daily",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun QuickResumeCard(
    artwork: ArtworkSummaryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("quick_resume_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ArtworkThumbnail(
                    artworkId = artwork.id,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue Coloring",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = artwork.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { artwork.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${artwork.progressPercent.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun GalleryArtworkCard(
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
            .testTag("artwork_card_${artwork.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ArtworkThumbnail(
                    artworkId = artwork.id,
                    modifier = Modifier.fillMaxSize()
                )

                // Status chip overlay
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
                text = "${artwork.category} • ${artwork.totalRegions} Regions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
