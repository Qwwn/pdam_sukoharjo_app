package com.metromultindo.tirtapanrannuangku

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.metromultindo.tirtapanrannuangku.ui.navigation.NavGraph
import com.metromultindo.tirtapanrannuangku.services.FCMTokenManager
import com.metromultindo.tirtapanrannuangku.services.NotificationHelperFCM
import com.metromultindo.tirtapanrannuangku.services.SimplifiedNotificationManager
import com.metromultindo.tirtapanrannuangku.ui.components.LoadingDialog
import com.metromultindo.tirtapanrannuangku.ui.theme.PdamAppTheme
import com.metromultindo.tirtapanrannuangku.utils.AppUpdateManagerUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var appUpdateManager: AppUpdateManagerUtil

    private lateinit var navController: NavHostController
    private var hasProcessedIntent = false
    private val updateRequestCode = 100

    // UI States
    private var showPermissionDialog by mutableStateOf(false)
    private var showNavigationLoading by mutableStateOf(false)
    private var permissionDeniedCount by mutableStateOf(0)
    private var showUpdateRequiredDialog by mutableStateOf(false)
    private var lastUpdateDate by mutableStateOf("")

    companion object {
        private const val TAG = "MainActivity"
        // DEBUG MODE - Set ke true untuk testing design dialog
        private const val DEBUG_SHOW_UPDATE_DIALOG = false // Ubah ke false untuk production
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

                            // DEBUG MODE - Force show dialog untuk testing
                            if (DEBUG_SHOW_UPDATE_DIALOG) {
                                Log.d(TAG, "DEBUG MODE: Showing update dialog for testing")
                                lastUpdateDate = getCurrentFormattedDate() // Mock date
                                delay(1000) // Delay 1 detik agar UI stable
                                showUpdateRequiredDialog = true
                            } else {
                                // Normal mode - check for real updates
                                checkForAppUpdate()
                            }

                            // Check permission
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

                        // Update required dialog dengan design baru
                        if (showUpdateRequiredDialog) {
                            UpdateRequiredDialog(
                                onUpdateNow = {
                                    if (DEBUG_SHOW_UPDATE_DIALOG) {
                                        // DEBUG MODE - Just hide dialog
                                        Log.d(TAG, "DEBUG MODE: Update button clicked")
                                        showUpdateRequiredDialog = false
                                        Toast.makeText(this@MainActivity, "DEBUG: Update dialog dismissed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Normal mode - start real update
                                        startImmediateUpdate()
                                    }
                                },
                                lastUpdateDate = lastUpdateDate
                            )
                        }

                        // DEBUG MODE - Floating Action Button untuk toggle dialog
                        if (DEBUG_SHOW_UPDATE_DIALOG) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                FloatingActionButton(
                                    onClick = {
                                        showUpdateRequiredDialog = !showUpdateRequiredDialog
                                        if (showUpdateRequiredDialog) {
                                            lastUpdateDate = getCurrentFormattedDate()
                                        }
                                    },
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text("Test")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get current formatted date untuk testing
     */
    private fun getCurrentFormattedDate(): String {
        return try {
            val dateFormat = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("id", "ID"))
            dateFormat.format(java.util.Date())
        } catch (e: Exception) {
            "19 Juni 2025"
        }
    }

    private fun checkForAppUpdate() {
        Log.d(TAG, "Checking for app updates using utility...")

        appUpdateManager.checkForUpdate { isUpdateAvailable, updateDate ->
            if (isUpdateAvailable) {
                Log.d(TAG, "Update available - showing required dialog")
                lastUpdateDate = updateDate ?: ""
                showUpdateRequiredDialog = true
            } else {
                Log.d(TAG, "No update available")
            }
        }
    }

    private fun startImmediateUpdate() {
        Log.d(TAG, "Starting immediate update...")
        showUpdateRequiredDialog = false

        appUpdateManager.startImmediateUpdate(this, updateRequestCode) { success ->
            if (!success) {
                Log.e(TAG, "Failed to start immediate update")
                Toast.makeText(this, "Gagal memulai update. Silakan coba lagi.", Toast.LENGTH_LONG).show()
                // Show dialog again if update fails
                showUpdateRequiredDialog = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == updateRequestCode) {
            when (resultCode) {
                RESULT_OK -> {
                    Log.d(TAG, "Update completed successfully")
                    Toast.makeText(this, "Update berhasil", Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED -> {
                    Log.d(TAG, "Update canceled by user")
                    Toast.makeText(this, "Update diperlukan untuk melanjutkan", Toast.LENGTH_LONG).show()
                    // User canceled update - close app
                    finish()
                }
                else -> {
                    Log.e(TAG, "Update failed with result code: $resultCode")
                    Toast.makeText(this, "Update gagal. Aplikasi akan ditutup.", Toast.LENGTH_LONG).show()
                    finish()
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

        // Skip checking interrupted updates in debug mode
        if (!DEBUG_SHOW_UPDATE_DIALOG) {
            // Check for any interrupted updates on resume
            appUpdateManager.checkForInterruptedUpdate { isInterrupted ->
                if (isInterrupted) {
                    Log.d(TAG, "Resuming interrupted update")
                    startImmediateUpdate()
                }
            }
        }

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

@Composable
fun UpdateRequiredDialog(
    onUpdateNow: () -> Unit,
    lastUpdateDate: String
) {
    // Menggunakan Dialog biasa untuk custom layout yang lebih fleksibel
    Dialog(
        onDismissRequest = { /* Cannot be dismissed */ }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD) // Light blue background
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title - warna hitam sesuai request
                Text(
                    text = "Update Tersedia",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Untuk menggunakan aplikasi ini,\n" +
                            "download versi terbaru.\n" +
                            "Anda dapat terus menggunakan\n" +
                            "aplikasi ini setelah mendownload update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp // Kurangi line height agar lebih compact
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Last update date - tidak hardcode
                if (lastUpdateDate.isNotEmpty()) {
                    Text(
                        text = "Terakhir diperbarui $lastUpdateDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Update button - warna biru sesuai request
                Button(
                    onClick = onUpdateNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2) // Blue color
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "Update Sekarang",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}