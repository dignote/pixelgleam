package com.example.ui.shell

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory bitmap cache for decoded thumbnails to ensure scrolling back to viewed cards
 * does not trigger redundant I/O or decoding.
 */
private object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 16).coerceIn(4096, 32768) // 4MB - 32MB cache
    val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
}

@Composable
fun ArtworkThumbnail(
    artworkId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = ThumbnailCache.cache.get(artworkId), key1 = artworkId) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                loadThumbnailBitmap(context, artworkId)
            }
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Mosaic thumbnail for $artworkId",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder box
        }
    }
}

private fun loadThumbnailBitmap(context: Context, artworkId: String): Bitmap? {
    ThumbnailCache.cache.get(artworkId)?.let { return it }

    val devDirs = listOf(
        "app/src/main/assets",
        "src/main/assets",
        "app/build/intermediates/assets/debug/mergeDebugAssets",
        "build/intermediates/assets/debug/mergeDebugAssets"
    )

    fun openAssetStream(relPath: String): java.io.InputStream? {
        for (dir in devDirs) {
            val f = java.io.File(dir, relPath)
            if (f.exists() && f.isFile) {
                return java.io.FileInputStream(f)
            }
        }
        return try {
            context.assets.open(relPath)
        } catch (_: Exception) {
            null
        }
    }

    // Tier 1: Dedicated lightweight thumbnail.webp (primary fast path)
    val thumbStream = openAssetStream("artworks/$artworkId/thumbnail.webp")
    if (thumbStream != null) {
        try {
            thumbStream.use { stream ->
                val bm = BitmapFactory.decodeStream(stream)
                if (bm != null) {
                    ThumbnailCache.cache.put(artworkId, bm)
                    return bm
                }
            }
        } catch (_: Throwable) {
            // Fall through to defensive path
        }
    }

    // Defensive/debug fallback only if thumbnail.webp missing
    val textureStream = openAssetStream("artworks/$artworkId/settled_texture.png")
    if (textureStream != null) {
        try {
            textureStream.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                val bm = BitmapFactory.decodeStream(stream, null, options)
                if (bm != null) {
                    ThumbnailCache.cache.put(artworkId, bm)
                    return bm
                }
            }
        } catch (_: Throwable) {
            // Return null
        }
    }

    return null
}
