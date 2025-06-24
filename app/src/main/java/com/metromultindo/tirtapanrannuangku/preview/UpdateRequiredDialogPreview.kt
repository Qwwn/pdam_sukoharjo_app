package com.metromultindo.tirtapanrannuangku.preview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.metromultindo.tirtapanrannuangku.ui.theme.PdamAppTheme
import kotlin.collections.forEach
import kotlin.text.isNotEmpty

/**
 * Preview untuk testing Update Required Dialog
 * Bisa dilihat langsung di Android Studio Design tab
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun UpdateRequiredDialogPreview() {
    PdamAppTheme {
        UpdateRequiredDialog(
            onUpdateNow = { },
            lastUpdateDate = "19 Juni 2025"
        )
    }
}

/**
 * Preview dengan tanggal kosong
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun UpdateRequiredDialogPreviewNoDate() {
    PdamAppTheme {
        UpdateRequiredDialog(
            onUpdateNow = { },
            lastUpdateDate = ""
        )
    }
}

/**
 * Preview untuk testing Notification Permission Dialog
 */
@Preview(showBackground = true)
@Composable
fun NotificationPermissionDialogPreview() {
    PdamAppTheme {
        NotificationPermissionDialog(
            onGrantPermission = { },
            onDismiss = { },
            onOpenSettings = { },
            showSettingsOption = false
        )
    }
}

/**
 * Preview dengan settings option
 */
@Preview(showBackground = true)
@Composable
fun NotificationPermissionDialogWithSettingsPreview() {
    PdamAppTheme {
        NotificationPermissionDialog(
            onGrantPermission = { },
            onDismiss = { },
            onOpenSettings = { },
            showSettingsOption = true
        )
    }
}

/**
 * Component terpisah untuk Update Dialog
 * Dapat digunakan dalam preview atau testing
 */
@Composable
fun UpdateRequiredDialog(
    onUpdateNow: () -> Unit,
    lastUpdateDate: String
) {
    Dialog(
        onDismissRequest = { /* Cannot be dismissed */ }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD) // Light blue background
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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
                    text = "Update tersedia",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Untuk menggunakan aplikasi ini,\ndownload versi terbaru.\nAnda dapat terus menggunakan\naplikasi ini setelah mendownload update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp)
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

/**
 * Preview untuk testing berbagai variant warna background card
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun UpdateDialogColorVariants() {
    PdamAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Color Variants Testing:", color = Color.White)

            // Original light blue
            UpdateDialogCardOnly(
                title = "Light Blue (Original)",
                backgroundColor = Color(0xFFE3F2FD)
            )

            // Alternative colors
            UpdateDialogCardOnly(
                title = "Light Gray",
                backgroundColor = Color(0xFFF5F5F5)
            )

            UpdateDialogCardOnly(
                title = "White",
                backgroundColor = Color.White
            )
        }
    }
}

@Composable
private fun UpdateDialogCardOnly(
    title: String,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = "Update tersedia",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sample description text",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}