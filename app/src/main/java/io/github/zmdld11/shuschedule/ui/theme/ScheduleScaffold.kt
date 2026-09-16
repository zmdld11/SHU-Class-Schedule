package io.github.zmdld11.shuschedule.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.zmdld11.shuschedule.R
import io.github.zmdld11.shuschedule.data.settings.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local, decorative artwork. A dark scrim keeps even empty grid cells readable. */
@Composable
fun ScheduleScaffold(
    backgroundPath: String? = null,
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val customBackground by produceState<Bitmap?>(null, backgroundPath) {
        value = null
        value = withContext(Dispatchers.IO) {
            backgroundPath?.let { path ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val options = BitmapFactory.Options().apply {
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
                    inSampleSize = sample
                }
                BitmapFactory.decodeFile(path, options)
            }
        }
    }
    Scaffold(topBar = topBar) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // A chosen photo takes precedence; clearing it restores the theme's built-in backdrop.
            Crossfade(targetState = customBackground, label = "scheduleBg") { bitmap ->
                Box(Modifier.fillMaxSize()) {
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)))
                    } else if (LocalScheduleStyle.current.theme == AppTheme.ARKNIGHTS) {
                        Image(
                            painter = painterResource(R.drawable.arknights_background),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.86f)))
                    }
                }
            }
            content()
        }
    }
}
