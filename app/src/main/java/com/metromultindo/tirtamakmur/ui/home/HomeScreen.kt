package com.metromultindo.tirtamakmur.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metromultindo.tirtamakmur.ui.theme.AppTheme
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.Calendar
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.metromultindo.tirtamakmur.R

data class MenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color,
    val requiresLogin: Boolean = false
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val customerName = viewModel.customerName.collectAsState()
    val customerNumber = viewModel.customerNumber.collectAsState()
    val isLoggedOut = viewModel.isLoggedOut.collectAsState()
    val isLoggedIn = !customerNumber.value.isNullOrEmpty()

    // Show logout dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserInfo()
    }

    // Stay on home screen when logged out
    LaunchedEffect(isLoggedOut.value) {
        if (isLoggedOut.value) {
            // Just reset the logout state without navigation
            viewModel.resetLogoutState()
        }
    }

    fun handleMenuNavigation(route: String, requiresLogin: Boolean) {
        when {
            route == "complaint" -> navigateToComplaint(navController, customerName.value, customerNumber.value)
            route == "customerInfo" -> {
                // Selalu arahkan ke login screen terlebih dahulu untuk cek tagihan
                // meskipun user sudah login sebelumnya
                navController.navigate("login/customerInfo")
            }
            requiresLogin && !isLoggedIn -> {
                // Go directly to login screen and pass the destination route
                navController.navigate("login/$route")
            }
            else -> navController.navigate(route)
        }
    }

    val menuItems = listOf(
        MenuItem(
            title = "Cek Tagihan",
            icon = Icons.Outlined.Receipt,
            route = "customerInfo",
            color = Color(0xFF2196F3),
            requiresLogin = true
        ),
        MenuItem(
            title = "Baca Meter",
            icon = Icons.Outlined.Speed,
            route = "selfMeter",
            color = Color(0xFF2196F3),
            requiresLogin = false
        ),
        MenuItem(
            title = "Pengaduan",
            icon = Icons.Outlined.ReportProblem,
            route = "complaint",
            color = Color(0xFF2196F3),
            requiresLogin = false
        ),
        MenuItem(
            title = "Berita",
            icon = Icons.Outlined.Newspaper,
            route = "news",
            color = Color(0xFF2196F3),
            requiresLogin = false
        ),
        MenuItem(
            title = "Kontak",
            icon = Icons.Outlined.ContactPhone,
            route = "contact",
            color = Color(0xFF2196F3),
            requiresLogin = false
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Text(stringResource(id = R.string.app_name))
                },
                actions = {
                    // Logout button only shown when logged in
                    if (isLoggedIn) {
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.navigate("login") }) {
                            Icon(
                                imageVector = Icons.Filled.Login,
                                contentDescription = "Login",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // TAMBAHKAN BARIS INI
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with logo and welcome message
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.welcome_titleV2),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${getTimeBasedGreeting()}, ${customerName.value ?: "Pelanggan Yth"}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Two menu items per row
                for (i in menuItems.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // First item in the row
                        MenuItemCard(
                            menuItem = menuItems[i],
                            modifier = Modifier.weight(1f),
                            onClick = { handleMenuNavigation(menuItems[i].route, menuItems[i].requiresLogin) }
                        )

                        // Second item in the row (if exists)
                        if (i + 1 < menuItems.size) {
                            MenuItemCard(
                                menuItem = menuItems[i + 1],
                                modifier = Modifier.weight(1f),
                                onClick = { handleMenuNavigation(menuItems[i + 1].route, menuItems[i + 1].requiresLogin) }
                            )
                        } else {
                            // Empty placeholder to maintain grid layout
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Apakah Anda yakin ingin keluar dari aplikasi?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                        }
                    ) {
                        Text("Ya, Keluar")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

// Fungsi extension untuk navigasi ke complaint
private fun navigateToComplaint(
    navController: NavController,
    name: String?,
    number: String?
) {
    val encodedName = name?.let { encodeURIComponent(it) } ?: ""
    val encodedNumber = number?.let { encodeURIComponent(it) } ?: ""
    navController.navigate("complaint?name=$encodedName&number=$encodedNumber")
}

// Fungsi untuk encoding string
private fun encodeURIComponent(s: String): String {
    return try {
        URLEncoder.encode(s, "UTF-8")
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
    } catch (e: UnsupportedEncodingException) {
        s
    }
}

@Composable
fun MenuItemCard(
    menuItem: MenuItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = menuItem.icon,
                    contentDescription = menuItem.title,
                    tint = menuItem.color,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getTimeBasedGreeting(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    return when (hour) {
        in 0..10 -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..18 -> "Selamat Sore"
        else -> "Selamat Malam"
    }
}