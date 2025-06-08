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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.metromultindo.pdam_app_v2.ui.navigation.NavGraph
import com.metromultindo.pdam_app_v2.services.FCMTokenManager
import com.metromultindo.pdam_app_v2.services.NotificationHelperFCM
import com.metromultindo.pdam_app_v2.services.SimplifiedNotificationManager
import com.metromultindo.pdam_app_v2.ui.components.LoadingDialog
import com.metromultindo.pdam_app_v2.ui.theme.PdamAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    private lateinit var navController: NavHostController
    private var hasProcessedIntent = false

    // UI States
    private var showPermissionDialog by mutableStateOf(false)
    private var showNavigationLoading by mutableStateOf(false)
    private var permissionDeniedCount by mutableStateOf(0)

    companion object {
        private const val TAG = "MainActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            showPermissionDialog = false
            initializeFCM()
        } else {
            Log.d(TAG, "Notification permission denied")
            permissionDeniedCount++
            if (permissionDeniedCount >= 2) {
                showPermissionDialog = true
            } else {
                showPermissionDialog = false
                initializeFCM()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity onCreate")
        logIntentDetails(intent, "onCreate")

        // Create notification channel
        NotificationHelperFCM.createNotificationChannel(this)

        setContent {
            PdamAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {
                        navController = rememberNavController()

                        // Setup navigation manager
                        LaunchedEffect(navController) {
                            SimplifiedNotificationManager.setNavController(navController)

                            // Setup loading state callback
                            SimplifiedNotificationManager.onLoadingStateChanged = { isLoading ->
                                showNavigationLoading = isLoading
                            }

                            // Check permission first
                            checkNotificationPermission()

                            // Short delay for UI stability
                            delay(200)

                            // Process notification intent if exists
                            if (!hasProcessedIntent) {
                                processNotificationIntentIfExists()
                                hasProcessedIntent = true
                            }
                        }

                        // Main navigation
                        NavGraph(navController = navController)

                        // Loading dialog for navigation
                        LoadingDialog(isShowing = showNavigationLoading)

                        // Permission dialog
                        if (showPermissionDialog) {
                            NotificationPermissionDialog(
                                onGrantPermission = { requestNotificationPermission() },
                                onDismiss = {
                                    if (permissionDeniedCount < 2) {
                                        showPermissionDialog = false
                                    }
                                },
                                onOpenSettings = { openNotificationSettings() },
                                showSettingsOption = permissionDeniedCount >= 2
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        intent?.let {
            lifecycleScope.launch {
                SimplifiedNotificationManager.processNotificationIntent(this@MainActivity, it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        // Process current intent on resume (untuk background to foreground)
        lifecycleScope.launch {
            delay(100) // Short delay for stability

            if (SimplifiedNotificationManager.hasPendingNavigation()) {
                Log.d(TAG, "Processing pending navigation on resume")
                SimplifiedNotificationManager.forceProcessPending()
            } else {
                // Try to process current intent
                SimplifiedNotificationManager.processNotificationIntent(this@MainActivity, intent)
            }
        }
    }

    /**
     * Process notification intent jika ada saat onCreate
     */
    private suspend fun processNotificationIntentIfExists() {
        try {
            Log.d(TAG, "Checking for notification intent")

            val processed = SimplifiedNotificationManager.processNotificationIntent(this, intent)
            if (processed) {
                Log.d(TAG, "Notification intent processed successfully")
            } else {
                Log.d(TAG, "No notification intent to process")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification intent", e)
        }
    }

    /**
     * Log intent details untuk debugging
     */
    private fun logIntentDetails(intent: Intent?, source: String) {
        Log.d(TAG, "=== Intent Details from $source ===")

        if (intent == null) {
            Log.d(TAG, "Intent is NULL")
            return
        }

        Log.d(TAG, "Action: ${intent.action}")
        Log.d(TAG, "Data: ${intent.data}")
        Log.d(TAG, "Flags: ${intent.flags}")

        intent.extras?.let { extras ->
            Log.d(TAG, "Extras:")
            for (key in extras.keySet()) {
                val value = extras.get(key)
                Log.d(TAG, "  $key: $value")
            }
        }

        Log.d(TAG, "=== End Intent Details ===")
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission granted")
                    initializeFCM()
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    showPermissionDialog = true
                }
            }
        } else {
            Log.d(TAG, "Notification permission not required")
            initializeFCM()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            showPermissionDialog = false
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification settings", e)
        }
    }

    private fun initializeFCM() {
        Log.d(TAG, "Initializing FCM")

        lifecycleScope.launch {
            try {
                fcmTokenManager.initializeFCM()
                fcmTokenManager.subscribeToNewsTopic()
                Log.d(TAG, "FCM initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FCM", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SimplifiedNotificationManager.reset()
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
                text = "Dapatkan Informasi Terbaru",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                Text(
                    text = "Aktifkan notifikasi untuk mendapatkan:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Izin notifikasi telah ditolak. Silakan buka Pengaturan untuk mengaktifkan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
                    Text("Aktifkan Notifikasi")
                }
            }
        },
        dismissButton = {
            if (!showSettingsOption) {
                TextButton(onClick = onDismiss) {
                    Text("Nanti Saja")
                }
            }
        }
    )
}