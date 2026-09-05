package cz.janek.vineyardlog.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Shows a JPEG from app storage, decoded off the main thread and downsampled to roughly [targetPx]
 * on the longer side. Photos are stored at 1600 px, so this is cheap enough without an image library.
 */
@Composable
fun PhotoImage(
    file: File,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    targetPx: Int = 400,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = file.path, key2 = targetPx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / sample > targetPx * 2) sample *= 2
                BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = contentDescription, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

/** Same as [PhotoImage] but for a bundled asset such as "guide/oidium.jpg". */
@Composable
fun AssetImage(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    targetPx: Int = 900,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / sample > targetPx * 2) sample *= 2
                context.assets.open(path).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = contentDescription, contentScale = contentScale, modifier = modifier)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
