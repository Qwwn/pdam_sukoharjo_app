package com.metromultindo.pdam_app_v2.ui.selfmeter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.metromultindo.pdam_app_v2.ui.components.ErrorDialog
import com.metromultindo.pdam_app_v2.ui.components.LoadingDialog
import com.metromultindo.pdam_app_v2.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfMeterScreen(
    navController: NavController,
    viewModel: SelfMeterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Use FusedLocationProviderClient directly
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // State management from ViewModel
    val isLoading = viewModel.isLoading.collectAsState()
    val errorState = viewModel.errorState.collectAsState()
    val customerInfo = viewModel.customerInfo.collectAsState()
    val isLoggedIn = viewModel.isLoggedIn.collectAsState()
    val submissionSuccess = viewModel.submissionSuccess.collectAsState()

    // Phone update states
    val phoneUpdateSuccess = viewModel.phoneUpdateSuccess.collectAsState()
    val phoneUpdateLoading = viewModel.phoneUpdateLoading.collectAsState()

    // Local state
    var customerNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") } // NEW: Added phone input
    var standMeter by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    var cameraFilePath by remember { mutableStateOf<String?>(null) }
    var cameraPhotoTaken by remember { mutableStateOf(false) }

    // Location states
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf("Belum diambil") }
    var isGettingLocation by remember { mutableStateOf(false) }

    // Dialog states
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    // Helper functions (same as before)
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("SelfMeterScreen", "Cannot open settings", e2)
            }
        }
    }

    suspend fun getCurrentLocation(): Pair<Double?, Double?> = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(Pair(null, null))
            return@suspendCancellableCoroutine
        }

        try {
            @Suppress("MissingPermission")
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d("SelfMeterScreen", "Location obtained: ${location.latitude}, ${location.longitude}")
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else {
                        Log.d("SelfMeterScreen", "Location is null")
                        continuation.resume(Pair(null, null))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("SelfMeterScreen", "Failed to get location", exception)
                    continuation.resume(Pair(null, null))
                }
        } catch (e: SecurityException) {
            Log.e("SelfMeterScreen", "Security exception", e)
            continuation.resume(Pair(null, null))
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            isGettingLocation = true
            coroutineScope.launch {
                val (lat, lng) = getCurrentLocation()
                currentLatitude = lat
                currentLongitude = lng
                locationStatus = if (lat != null && lng != null) {
                    "Berhasil"
                } else {
                    "Gagal mendapatkan lokasi"
                }
                isGettingLocation = false
            }
        } else {
            showLocationPermissionDialog = true
            isGettingLocation = false
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            imageUri = cameraUri
            cameraPhotoTaken = true

            // Automatically get location after taking photo
            if (hasLocationPermission()) {
                if (isLocationEnabled()) {
                    isGettingLocation = true
                    coroutineScope.launch {
                        val (lat, lng) = getCurrentLocation()
                        currentLatitude = lat
                        currentLongitude = lng
                        locationStatus = if (lat != null && lng != null) {
                            "Berhasil"
                        } else {
                            "Gagal mendapatkan lokasi"
                        }
                        isGettingLocation = false
                    }
                } else {
                    locationStatus = "GPS tidak aktif"
                    showLocationDialog = true
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        } else {
            cameraFilePath = null
            cameraPhotoTaken = false
        }
    }

    val createImageFile = {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "METER_${timeStamp}_"
            val storageDir = context.getExternalFilesDir("Pictures")

            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            cameraFilePath = imageFile.absolutePath

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
        } catch (e: Exception) {
            Log.e("SelfMeterScreen", "Error creating image file", e)
            null
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraUri = createImageFile()
            cameraUri?.let { cameraLauncher.launch(it) }
        } else {
            showPermissionDeniedDialog = true
        }
    }

    fun requestLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (!isLocationEnabled()) {
            showLocationDialog = true
            return
        }

        isGettingLocation = true
        coroutineScope.launch {
            val (lat, lng) = getCurrentLocation()
            currentLatitude = lat
            currentLongitude = lng
            locationStatus = if (lat != null && lng != null) {
                "Berhasil"
            } else {
                "Gagal mendapatkan lokasi"
            }
            isGettingLocation = false
        }
    }

    fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                cameraUri = createImageFile()
                cameraUri?.let { cameraLauncher.launch(it) }
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // NEW: Function to load customer and update phone
    fun loadCustomerAndUpdatePhone() {
        if (customerNumber.isNotEmpty() && phoneNumber.isNotEmpty()) {
            viewModel.loadCustomerInfoWithPhoneUpdate(customerNumber, phoneNumber)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = { Text("Catat Meter Mandiri") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Catat Meter Mandiri",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Laporkan angka meter air Anda beserta foto sebagai bukti",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Customer Number Input (if not logged in) - UPDATED
            if (!isLoggedIn.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Data Pelanggan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customerNumber,
                            onValueChange = { customerNumber = it },
                            label = { Text("Nomor Sambung") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Numbers, "Nomor Sambungan") },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("No. WhatsApp") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, "WhatsApp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Information card about phone number
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Pastikan no whatsapp sudah sesuai, akan digunakan untuk komunikasi jika ada pemakaian atau tagihan melonjak.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // UPDATED: Button now requires both fields
                        Button(
                            onClick = { loadCustomerAndUpdatePhone() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customerNumber.isNotEmpty() && phoneNumber.isNotEmpty() && !isLoading.value
                        ) {
                            if (isLoading.value) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Cari Data Pelanggan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Customer Info (if available) - UPDATED WITHOUT EDIT BUTTON
            customerInfo.value?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Informasi Pelanggan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        InfoRow(
                            label = "Nomor Sambungan",
                            value = info.custCode,
                            icon = Icons.Default.Numbers
                        )

                        InfoRow(
                            label = "Nama",
                            value = info.name,
                            icon = Icons.Default.Person
                        )

                        InfoRow(
                            label = "Alamat",
                            value = info.address,
                            icon = Icons.Default.Home
                        )

                        InfoRow(
                            label = "Tarif",
                            value = info.tariffClass,
                            icon = Icons.Outlined.Receipt
                        )

                        InfoRow(
                            label = "Stand Awal",
                            value = info.startMeter,
                            icon = Icons.Outlined.Speed
                        )

                        // UPDATED: Phone info without edit (read-only)
                        InfoRow(
                            label = "No. WhatsApp",
                            value = info.phone.ifEmpty { phoneNumber },
                            icon = Icons.Default.Phone
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Meter Reading Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Input Data Meter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = standMeter,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    standMeter = it
                                }
                            },
                            label = { Text("Angka Stand Meter") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Outlined.Speed, "Stand Meter") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Contoh: 12345") }
                        )

                        // Estimasi Pemakaian
                        if (standMeter.isNotEmpty() && info.startMeter != "Tidak diketahui") {
                            val currentMeter = standMeter.toIntOrNull() ?: 0
                            val startMeterInt = info.startMeter.toIntOrNull() ?: 0
                            val estimatedUsage = currentMeter - startMeterInt

                            if (estimatedUsage >= 0) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Calculate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = "Estimasi Pemakaian Air Juni 2025",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${estimatedUsage}m³",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            } else if (estimatedUsage < 0) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "Angka stand meter tidak boleh lebih kecil dari stand awal",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Camera Section
                        Text(
                            text = "Foto Meter",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (imageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(context).data(imageUri).build()
                                    ),
                                    contentDescription = "Foto Meter",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )

                                IconButton(
                                    onClick = {
                                        imageUri = null
                                        currentLatitude = null
                                        currentLongitude = null
                                        locationStatus = "Belum diambil"
                                        cameraFilePath = null
                                        cameraPhotoTaken = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Hapus Foto",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { checkAndRequestCameraPermission() },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = "Ambil Foto")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ambil Foto Meter")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Location Section
                        Text(
                            text = "Lokasi",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .background(Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = "Lokasi",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Status: $locationStatus",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = when {
                                                    locationStatus.contains("Berhasil") -> MaterialTheme.colorScheme.primary
                                                    locationStatus.contains("Gagal") || locationStatus.contains("tidak aktif") -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }

                                        if (currentLatitude != null && currentLongitude != null) {
                                            Text(
                                                text = String.format("%.6f, %.6f", currentLatitude, currentLongitude),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 28.dp)
                                            )
                                        }
                                    }

                                    if (isGettingLocation) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        IconButton(
                                            onClick = { requestLocation() },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = "Ambil Lokasi",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val customerCode = if (isLoggedIn.value) {
                                    info.custCode
                                } else {
                                    customerNumber
                                }

                                viewModel.submitMeterReading(
                                    customerNumber = customerCode,
                                    standMeter = standMeter.toIntOrNull() ?: 0,
                                    imageUri = imageUri,
                                    cameraFilePath = if (cameraPhotoTaken) cameraFilePath else null,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = standMeter.isNotEmpty() && imageUri != null && !isLoading.value
                        ) {
                            Icon(Icons.Outlined.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Data Meter")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card (same as before)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Manfaat Catat Meter Mandiri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Benefit Items
                    BenefitItem(
                        title = "Tagihan yang Sesuai",
                        description = "Dengan mencatat meter sendiri, pelanggan memiliki kontrol atas tagihan rekening air dan dapat memastikan tagihan sesuai dengan pemakaian air."
                    )

                    BenefitItem(
                        title = "Bukti Pendukung",
                        description = "Data yang dikirimkan menjadi bukti pendukung jika terjadi perbedaan dalam perhitungan tagihan."
                    )

                    BenefitItem(
                        title = "Pencatatan Transparan",
                        description = "Dengan fitur ini, pencatatan jadi lebih transparan, cepat, dan tanpa perlu menunggu petugas."
                    )

                    BenefitItem(
                        title = "Komunikasi Layanan",
                        description = "Dengan nomor WhatsApp yang sudah diperbarui, Anda akan menerima notifikasi tagihan, informasi gangguan layanan, dan komunikasi penting lainnya dari PDAM Tirta Makmur.",
                        isLast = true
                    )

                    // Call to Action
                    Text(
                        text = "Yuk, gunakan Catat Meter Mandiri secara rutin..",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Dialogs
            LoadingDialog(isLoading.value || phoneUpdateLoading.value)

            errorState.value?.let { error ->
                ErrorDialog(
                    errorCode = error.first,
                    errorMessage = error.second,
                    onDismiss = { viewModel.clearError() }
                )
            }

            if (submissionSuccess.value) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.resetSubmissionState()
                        navController.navigateUp()
                    },
                    title = { Text("Data Terkirim") },
                    text = { Text("Data meter berhasil dikirim. Terima kasih atas partisipasi Anda.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetSubmissionState()
                                navController.navigateUp()
                            }
                        ) { Text("OK") }
                    }
                )
            }

            // Phone update success dialog
            if (phoneUpdateSuccess.value) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.resetPhoneUpdateState()
                    },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = { Text("Data Pelanggan Berhasil Dimuat") },
                    text = {
                        Text("Data pelanggan telah dimuat dan nomor WhatsApp berhasil diperbarui. Sekarang Anda dapat melanjutkan untuk input data meter.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetPhoneUpdateState()
                            }
                        ) { Text("OK") }
                    }
                )
            }

            if (showLocationPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationPermissionDialog = false },
                    title = { Text("Izin Lokasi Diperlukan") },
                    text = { Text("Aplikasi memerlukan izin lokasi untuk menandai posisi meter Anda.") },
                    confirmButton = {
                        Button(onClick = {
                            showLocationPermissionDialog = false
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) { Text("Izinkan") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLocationPermissionDialog = false }) {
                            Text("Nanti Saja")
                        }
                    }
                )
            }

            if (showLocationDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationDialog = false },
                    icon = {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = { Text("GPS Tidak Aktif") },
                    text = {
                        Text("Untuk mendapatkan lokasi yang akurat, silakan aktifkan GPS/Location Services di pengaturan perangkat Anda.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLocationDialog = false
                                openLocationSettings()
                            }
                        ) {
                            Text("Buka Pengaturan")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLocationDialog = false }
                        ) {
                            Text("Nanti Saja")
                        }
                    }
                )
            }

            if (showPermissionDeniedDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDeniedDialog = false },
                    title = { Text("Izin Diperlukan") },
                    text = { Text("Aplikasi memerlukan izin untuk mengakses kamera.") },
                    confirmButton = {
                        Button(onClick = { showPermissionDeniedDialog = false }) { Text("OK") }
                    }
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        // Add a subtle divider for visual separation
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    }
}

@Composable
fun BenefitItem(
    title: String,
    description: String,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
            }
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}