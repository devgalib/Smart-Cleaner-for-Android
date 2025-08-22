package com.d4rk.cleaner.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun AppImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val request = remember(model) {
        if (model is ImageRequest) {
            model
        } else {
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build()
        }
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        imageLoader = context.imageLoader,
        contentScale = contentScale,
    )
}

