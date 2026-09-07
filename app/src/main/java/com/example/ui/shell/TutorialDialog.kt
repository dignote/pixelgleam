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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private data class TutorialStepData(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val tutorialSteps = listOf(
    TutorialStepData(
        title = "1. Choose Numbered Color",
        description = "Select a numbered color chip from the bottom palette to activate that gemstone color.",
        icon = Icons.Default.Palette
    ),
    TutorialStepData(
        title = "2. Tap Matching Cells",
        description = "Tap any cell on the canvas marked with your selected number to place your gems.",
        icon = Icons.Default.TouchApp
    ),
    TutorialStepData(
        title = "3. Instant Region Fill",
        description = "A single tap places glistening gemstones across the entire connected region with gleaming sparkle.",
        icon = Icons.Default.AutoAwesome
    ),
    TutorialStepData(
        title = "4. Zoom & Pan",
        description = "Pinch with two fingers to zoom in for delicate details or pan freely to navigate the mosaic canvas.",
        icon = Icons.Default.ZoomIn
    ),
    TutorialStepData(
        title = "5. Guided Hints",
        description = "Tap the Lightbulb button anytime to reveal and center an uncolored region matching your palette.",
        icon = Icons.Default.Lightbulb
    )
)

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = tutorialSteps[currentStepIndex]
    val isLastStep = currentStepIndex == tutorialSteps.size - 1

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("tutorial_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with step number and Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "How to Play • Step ${currentStepIndex + 1}/${tutorialSteps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_tutorial_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close tutorial")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Step Icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = step.title,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Step dots indicator
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tutorialSteps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == currentStepIndex) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentStepIndex)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("tutorial_prev_button")
                        ) {
                            Text("Previous")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Button(
                        onClick = {
                            if (isLastStep) {
                                onDismiss()
                            } else {
                                currentStepIndex++
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("tutorial_next_button")
                    ) {
                        Text(if (isLastStep) "Start Playing!" else "Next", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
