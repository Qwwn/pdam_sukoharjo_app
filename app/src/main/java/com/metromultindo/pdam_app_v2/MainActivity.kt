// ===================================================================
// IMPROVED NOTIFICATION PERMISSION UX
// Update MainActivity.kt dengan UX yang lebih baik
// ===================================================================

package com.metromultindo.pdam_app_v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.metromultindo.pdam_app_v2.navigation.NavGraph
import com.metromultindo.pdam_app_v2.service.FCMTokenManager
import com.metromultindo.pdam_app_v2.service.NotificationHelper
import com.metromultindo.pdam_app_v2.ui.theme.PdamAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    // State untuk permission
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionDeniedCount by mutableStateOf(0)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showPermissionDialog = false
            initializeFCM()
            showSuccessMessage()
        } else {
            permissionDeniedCount++
            handlePermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        setContent {
            PdamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        val navController = rememberNavController()

                        // Handle notification navigation
                        LaunchedEffect(Unit) {
                            handleNotificationIntent(navController)
                            checkNotificationPermission()
                        }

                        // Main content
                        NavGraph(navController = navController)

                        // Permission dialog overlay
                        if (showPermissionDialog) {
                            NotificationPermissionDialog(
                                onGrantPermission = {
                                    requestNotificationPermission()
                                },
                                onDismiss = {
                                    if (permissionDeniedCount < 2) {
                                        showPermissionDialog = false
                                        // Proceed without notification (graceful degradation)
                                        proceedWithoutNotification()
                                    }
                                },
                                onOpenSettings = {
                                    openNotificationSettings()
                                },
                                showSettingsOption = permissionDeniedCount >= 2
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    initializeFCM()
                }
                else -> {
                    // Show dialog explaining why we need permission
                    showPermissionDialog = true
                }
            }
        } else {
            initializeFCM()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handlePermissionDenied() {
        if (permissionDeniedCount >= 2) {
            // User denied multiple times, show settings option
            showPermissionDialog = true
        } else {
            // First denial, proceed without notification but show info
            showPermissionDialog = false
            proceedWithoutNotification()
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            showPermissionDialog = false
        } catch (e: Exception) {
        }
    }

    private fun proceedWithoutNotification() {
        // App tetap bisa digunakan, tapi tanpa push notification
        // User bisa enable notification nanti via app settings
    }

    private fun showSuccessMessage() {
        // Optional: Show toast or snackbar
    }

    private fun initializeFCM() {

        lifecycleScope.launch {
            try {
                // Initialize FCM and register token
                fcmTokenManager.initializeFCM()

                // Subscribe to news topic for general news notifications
                fcmTokenManager.subscribeToNewsTopic()

            } catch (e: Exception) {
            }
        }
    }

    private fun handleNotificationIntent(navController: androidx.navigation.NavController) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        val newsId = intent?.getStringExtra("news_id")

        Log.d("MainActivity", "Handling notification intent - navigateTo: $navigateTo, newsId: $newsId")

        when (navigateTo) {
            "news_detail" -> {
                newsId?.let {
                    Log.d("MainActivity", "Navigating to news detail: $it")
                    navController.navigate("newsDetail/$it")
                }
            }
            "news" -> {
                navController.navigate("news")
            }
        }

        // Clear the intent extras to prevent re-navigation
        intent?.removeExtra("navigate_to")
        intent?.removeExtra("news_id")
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new notification when app is already running
        intent?.let {
            val navigateTo = it.getStringExtra("navigate_to")
            val newsId = it.getStringExtra("news_id")

            Log.d("MainActivity", "New intent received - navigateTo: $navigateTo, newsId: $newsId")
        }
    }
}

@Composable
fun NotificationPermissionDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    showSettingsOption: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Dapatkan Update Berita Terbaru",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = "Izinkan notifikasi untuk mendapatkan:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val benefits = listOf(
                    "📰 Berita terbaru PDAM",
                    "🚰 Info gangguan layanan",
                    "💧 Pengumuman penting",
                    "🔧 Update maintenance"
                )

                benefits.forEach { benefit ->
                    Text(
                        text = "• $benefit",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                    )
                }

                if (showSettingsOption) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Izin ditolak. Untuk mengaktifkan notifikasi, buka Pengaturan → Aplikasi → PDAM → Notifikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            if (showSettingsOption) {
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka Pengaturan")
                }
            } else {
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Izinkan Notifikasi")
                }
            }
        },
        dismissButton = {
            if (!showSettingsOption) {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Nanti Saja")
                }
            }
        }
    )
}

@Composable
fun NotificationSettingsCard(
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = if (isPermissionGranted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Notifikasi Push",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isPermissionGranted)
                            "Aktif - Anda akan menerima update berita terbaru"
                        else
                            "Nonaktif - Aktifkan untuk mendapatkan update berita",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isPermissionGranted,
                    onCheckedChange = {
                        if (!isPermissionGranted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                onRequestPermission()
                            } else {
                                onOpenSettings()
                            }
                        } else {
                            onOpenSettings()
                        }
                    }
                )
            }

            if (!isPermissionGranted) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            onRequestPermission()
                        } else {
                            onOpenSettings()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aktifkan Notifikasi")
                }
            }
        }
    }
}