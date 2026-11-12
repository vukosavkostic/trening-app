package com.vukos.trening.ui.body

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.vukos.trening.data.BodyPhoto
import com.vukos.trening.data.SecurePhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Nova slika (jos nesifrovana, iz kamere/galerije) ili postojeca (desifruje se u memoriji). */
@Composable
fun PhotoContent(newUri: Uri?, existing: BodyPhoto?, scale: ContentScale, modifier: Modifier) {
    if (newUri != null) {
        AsyncImage(newUri, null, contentScale = scale, modifier = modifier)
        return
    }
    val path = existing?.localPath ?: return
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { SecurePhotoStore.load(path)?.asImageBitmap() }
    }
    bmp?.let { Image(it, null, contentScale = scale, modifier = modifier) }
}

/** Zabrani screenshot i pregled u "Recent apps" dok je ekran sa slikama otvoren. */
@Composable
fun SecureWindow() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

