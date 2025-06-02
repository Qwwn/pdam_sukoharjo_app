package com.example.pdam_app_v2.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.pdam_app_v2.R
import java.net.URLDecoder
import androidx.compose.runtime.Composable
import java.io.File

@Composable
fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "Zoomable image",
    contentScale: ContentScale = ContentScale.Fit,
    onTap: (() -> Unit)? = null,
    initialScale: Float = 1f,
    enableSingleTap: Boolean = true
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(initialScale) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    scale = (scale * gestureZoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val maxOffset = size / scale.toInt()
                        offset = Offset(
                            x = (offset.x + pan.x / scale).coerceIn(
                                (-maxOffset.width).toFloat(),
                                maxOffset.width.toFloat()
                            ),
                            y = (offset.y + pan.y / scale).coerceIn(
                                (-maxOffset.height).toFloat(),
                                maxOffset.height.toFloat()
                            )
                        )
                    }
                    zoom = scale > 1f
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (enableSingleTap) {
                            onTap?.invoke()
                        }
                    },
                    onDoubleTap = {
                        zoom = !zoom
                        scale = if (zoom) 3f else 1f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            placeholder = painterResource(R.drawable.ic_loading_placeholder),
            error = painterResource(R.drawable.ic_error_placeholder)
        )

            // Tampilkan persentase zoom di bagian bawah
            Text(
                text = "${(scale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

