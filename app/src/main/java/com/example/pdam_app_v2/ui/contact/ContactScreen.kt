package com.example.pdam_app_v2.ui.contact

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pdam_app_v2.R
import com.example.pdam_app_v2.ui.theme.AppTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    navController: NavController
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Text(
                        "Hubungi Kami",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Contact Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.entity_name),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ContactItem(
                            icon = Icons.Default.LocationOn,
                            title = "Alamat",
                            content = "Jl. Abu Tholib Sastrotenoyo No.371, Gabusan, Jombor, Kec. Bendosari, Kabupaten Sukoharjo, Jawa Tengah 57521",
                            onClick = {
                                val mapUri = Uri.parse("https://maps.app.goo.gl/W1NFaf3nvc6yLYmp8")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                }
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        ContactItem(
                            icon = Icons.Default.Phone,
                            title = "WhatsApp",
                            content = "0821 1141 4111",
                            onClick = {
                                val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=6282111414111&text=Hallo%20PDAM%20Tirta%20Makmur%20Kab.%20Karanganyar")
                                val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri)
                                try {
                                    context.startActivity(whatsappIntent)
                                } catch (e: Exception) {
                                    // Fallback to regular dial
                                    val dialIntent = Intent(Intent.ACTION_DIAL)
                                    dialIntent.data = Uri.parse("tel:082111414111")
                                    context.startActivity(dialIntent)
                                }
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        ContactItem(
                            icon = Icons.Default.Email,
                            title = "Email",
                            content = "pdamtirtamakmur_skh@yahoo.co.id",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("mailto:pdamtirtamakmur_skh@yahoo.co.id")
                                context.startActivity(intent)
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )

                        ContactItem(
                            icon = Icons.Default.Info,
                            title = "Website",
                            content = "www.pdamsukoharjo.com",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("https://www.pdamsukoharjo.com")
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Jam Pelayanan",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Senin - Kamis
                        OperationalHoursItem(
                            day = "Senin - Kamis",
                            hours = "07:30 - 15:45 WIB"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Jumat
                        OperationalHoursItem(
                            day = "Jumat",
                            hours = "07:00 - 14:00 WIB"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sabtu - Minggu
                        OperationalHoursItem(
                            day = "Sabtu - Minggu",
                            hours = "Tutup",
                            isClosedDay = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    }
}

@Composable
fun ContactItem(
    icon: ImageVector,
    title: String,
    content: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

// Komponen OperationalHoursItem yang diperbaiki
@Composable
fun OperationalHoursItem(
    day: String,
    hours: String,
    isClosedDay: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = hours,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (isClosedDay) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                textAlign = TextAlign.End
            )
        )
    }
}