package dev.voicejournal.ui.notedetail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.voicejournal.ui.theme.AppTheme
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageMosaic(
    images: List<String>,
    isSelectionMode: Boolean = false,
    selectedImages: Set<String> = emptySet(),
    onImageClick: (Int) -> Unit,
    onImageLongClick: (Int) -> Unit = {}
) {
    if (images.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        when (images.size) {
            1 -> {
                MosaicImageItem(
                    imagePath = images[0],
                    index = 0,
                    isSelectionMode = isSelectionMode,
                    isSelected = images[0] in selectedImages,
                    onImageClick = onImageClick,
                    onImageLongClick = onImageLongClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MosaicImageItem(
                        imagePath = images[0],
                        index = 0,
                        isSelectionMode = isSelectionMode,
                        isSelected = images[0] in selectedImages,
                        onImageClick = onImageClick,
                        onImageLongClick = onImageLongClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    MosaicImageItem(
                        imagePath = images[1],
                        index = 1,
                        isSelectionMode = isSelectionMode,
                        isSelected = images[1] in selectedImages,
                        onImageClick = onImageClick,
                        onImageLongClick = onImageLongClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            3 -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MosaicImageItem(
                        imagePath = images[0],
                        index = 0,
                        isSelectionMode = isSelectionMode,
                        isSelected = images[0] in selectedImages,
                        onImageClick = onImageClick,
                        onImageLongClick = onImageLongClick,
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MosaicImageItem(
                            imagePath = images[1],
                            index = 1,
                            isSelectionMode = isSelectionMode,
                            isSelected = images[1] in selectedImages,
                            onImageClick = onImageClick,
                            onImageLongClick = onImageLongClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        MosaicImageItem(
                            imagePath = images[2],
                            index = 2,
                            isSelectionMode = isSelectionMode,
                            isSelected = images[2] in selectedImages,
                            onImageClick = onImageClick,
                            onImageLongClick = onImageLongClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MosaicImageItem(
                            imagePath = images[0],
                            index = 0,
                            isSelectionMode = isSelectionMode,
                            isSelected = images[0] in selectedImages,
                            onImageClick = onImageClick,
                            onImageLongClick = onImageLongClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        MosaicImageItem(
                            imagePath = images[1],
                            index = 1,
                            isSelectionMode = isSelectionMode,
                            isSelected = images[1] in selectedImages,
                            onImageClick = onImageClick,
                            onImageLongClick = onImageLongClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MosaicImageItem(
                            imagePath = images[2],
                            index = 2,
                            isSelectionMode = isSelectionMode,
                            isSelected = images[2] in selectedImages,
                            onImageClick = onImageClick,
                            onImageLongClick = onImageLongClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        MosaicImageItem(
                            imagePath = images[3],
                            index = 3,
                            isSelectionMode = isSelectionMode,
                            isSelected = images[3] in selectedImages,
                            onImageClick = onImageClick,
                            onImageLongClick = onImageLongClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MosaicImageItem(
    imagePath: String,
    index: Int,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onImageClick: (Int) -> Unit,
    onImageLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val modelObj: Any = if (imagePath.startsWith("/")) File(imagePath) else imagePath
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) colors.primary else Color.Transparent,
                shape = shape
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onImageLongClick(index) // Toggle selection when mode active
                    } else {
                        onImageClick(index)
                    }
                },
                onLongClick = {
                    onImageLongClick(index)
                }
            )
    ) {
        AsyncImage(
            model = modelObj,
            contentDescription = "Note image ${index + 1}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.primary.copy(alpha = 0.25f))
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .background(colors.primary, CircleShape)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Image",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
