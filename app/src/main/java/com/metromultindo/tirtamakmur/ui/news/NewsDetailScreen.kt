package com.metromultindo.tirtamakmur.ui.news

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metromultindo.tirtamakmur.data.model.News
import com.metromultindo.tirtamakmur.ui.components.ErrorDialog
import com.metromultindo.tirtamakmur.ui.components.LoadingDialog
import com.metromultindo.tirtamakmur.utils.ZoomableImage
import com.metromultindo.tirtamakmur.ui.theme.AppTheme
import java.io.File
import java.net.URLDecoder
import com.metromultindo.tirtamakmur.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    navController: NavController,
    newsId: Int,
    viewModel: NewsDetailViewModel = hiltViewModel()
) {
    val newsDetail = viewModel.newsDetail.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val errorState = viewModel.errorState.collectAsState()

    LaunchedEffect(newsId) {
        viewModel.loadNewsDetail(newsId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Text("Detail Berita")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            newsDetail.value?.let { news ->
                NewsDetailContent(news = news)
            }

            // Show loading indicator
            LoadingDialog(isLoading.value)

            // Show error dialog
            errorState.value?.let { error ->
                ErrorDialog(
                    errorCode = error.first,
                    errorMessage = error.second,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
fun NewsDetailContent(news: News) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Image with enhanced zoom capability
                news.imageUrl?.let { imageUrl ->
                    var showFullImage by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppTheme.colors.cardBackground)
                    ) {
                        ZoomableImage(
                            imageUrl = imageUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = news.title,
                            contentScale = ContentScale.Fit,
                            initialScale = 1f,
                            onTap = { showFullImage = true }
                        )

                        // Overlay hint for tap to zoom
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Tap untuk fullscreen",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    // Enhanced full screen zoom dialog
                    if (showFullImage) {
                        Dialog(
                            onDismissRequest = { showFullImage = false },
                            properties = DialogProperties(
                                usePlatformDefaultWidth = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { showFullImage = false }  // Tap anywhere to close
                                        )
                                    }
                            ) {
                                // Zoomable image that fills the screen
                                ZoomableImage(
                                    imageUrl = imageUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    contentDescription = "Full screen ${news.title}",
                                    contentScale = ContentScale.Fit,
                                    enableSingleTap = false  // Disable single tap di fullscreen
                                )

                                // Top bar with download and close buttons
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Download button
                                    IconButton(
                                        onClick = {
                                            downloadImage(context, imageUrl)
                                        },
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.6f),
                                                CircleShape
                                            )
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download image",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Close button
                                    IconButton(
                                        onClick = { showFullImage = false },
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.6f),
                                                CircleShape
                                            )
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Title overlay
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                        .widthIn(max = 250.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Text(
                                        text = news.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(12.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Instructions text
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Black.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Text(
                                        text = "Pinch untuk zoom • Drag untuk geser • Double tap untuk zoom • Tap area kosong untuk keluar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Category chip (if available)
                news.category?.takeIf { it.isNotBlank() && it != "0" }?.let { category ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = news.getDisplayCategory(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Title - Centered
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Date, time and author - Left aligned
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Date and time in one row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "${news.getFormattedDate()} • ${news.getFormattedTime()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Author
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = news.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Content
                Text(
                    text = news.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


private fun downloadImage(context: Context, imageUrl: String) {
    try {
        val fileName = getFileNameFromUrl(imageUrl) ?: "image_${System.currentTimeMillis()}.jpg"
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(imageUrl)

        val request = DownloadManager.Request(uri)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(fileName)
            .setDescription("Downloading $fileName")

        // Solusi universal yang bekerja di semua versi Android
        request.setDestinationUri(
            Uri.fromFile(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "PDAM_Images/$fileName"
                )
            )
        )

        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getFileNameFromUrl(url: String): String? {
    return try {
        val decodedUrl = URLDecoder.decode(url, "UTF-8")
        decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}