package com.metromultindo.pdam_app_v2.ui.complaint

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.metromultindo.pdam_app_v2.R
import com.metromultindo.pdam_app_v2.ui.components.ErrorDialog
import com.metromultindo.pdam_app_v2.ui.components.LoadingDialog
import com.metromultindo.pdam_app_v2.ui.theme.AppTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintScreen(
    navController: NavController,
    customerName: String?,
    customerNumber: String?,
    viewModel: ComplaintViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Use FusedLocationProviderClient directly
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // State management
    var cameraFilePath by remember { mutableStateOf<String?>(null) }
    var cameraPhotoTaken by remember { mutableStateOf(false) }
    val isLoading = viewModel.isLoading.collectAsState()
    val errorState = viewModel.errorState.collectAsState()
    val complaintSubmitted = viewModel.complaintSubmitted.collectAsState()
    var addressValue by remember { mutableStateOf("") }
    var customerNo by remember { mutableStateOf(customerNumber ?: "") }
    var phoneNumber by remember { mutableStateOf("") }
    var complaintText by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf(customerName ?: "") }

    // Location states
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf("Belum diambil") }
    var isGettingLocation by remember { mutableStateOf(false) }

    // Dialog states
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // Image state
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // Customer status
    var isCustomer by remember { mutableStateOf(customerNumber != null && customerNumber.isNotEmpty()) }

    // Helper function to check location permission
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

    // Helper function to check if location services are enabled
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Helper function to open location settings
    fun openLocationSettings() {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings if location settings not available
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e("ComplaintScreen", "Cannot open settings", e2)
            }
        }
    }

    // Helper function to get current location
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
                        Log.d("ComplaintScreen", "Location obtained: ${location.latitude}, ${location.longitude}")
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else {
                        Log.d("ComplaintScreen", "Location is null")
                        continuation.resume(Pair(null, null))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ComplaintScreen", "Failed to get location", exception)
                    continuation.resume(Pair(null, null))
                }
        } catch (e: SecurityException) {
            Log.e("ComplaintScreen", "Security exception", e)
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
            // Permission granted, get location
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
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = context.getExternalFilesDir("Pictures")

            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            cameraFilePath = imageFile.absolutePath

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
        } catch (e: Exception) {
            Log.e("ComplaintScreen", "Error creating image file", e)
            null
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
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

    // Storage permission launcher
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            showPermissionDeniedDialog = true
        }
    }

    // Functions
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

    fun checkAndRequestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(context, permission)
                    == PackageManager.PERMISSION_GRANTED -> {
                galleryLauncher.launch("image/*")
            }
            else -> galleryPermissionLauncher.launch(permission)
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

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = { Text(stringResource(id = R.string.complaint_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, stringResource(id = R.string.back))
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
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Form Pengaduan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Silakan isi formulir pengaduan di bawah ini",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Customer Status Selection
                    Text(
                        text = "Status Pelanggan",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Customer option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    1.dp,
                                    if (isCustomer) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (isCustomer) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    isCustomer = true
                                    if (!customerName.isNullOrEmpty()) inputName = customerName
                                    if (!customerNumber.isNullOrEmpty()) customerNo = customerNumber
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pelanggan",
                                fontWeight = if (isCustomer) FontWeight.Medium else FontWeight.Normal,
                                color = if (isCustomer) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Non-customer option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(
                                    1.dp,
                                    if (!isCustomer) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (!isCustomer) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    isCustomer = false
                                    inputName = ""
                                    customerNo = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bukan\nPelanggan",
                                fontWeight = if (!isCustomer) FontWeight.Medium else FontWeight.Normal,
                                color = if (!isCustomer) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Form fields
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Person, "Nama") },
                        readOnly = isCustomer && !customerName.isNullOrEmpty()
                    )

                    if (isCustomer) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = customerNo,
                            onValueChange = { customerNo = it },
                            label = { Text("Nomor Sambungan") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Numbers, "Nomor Sambungan") },
                            readOnly = isCustomer && !customerNumber.isNullOrEmpty()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Nomor Telepon") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, "Telepon") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = addressValue,
                        onValueChange = { addressValue = it },
                        label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Outlined.Home, "Alamat") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = complaintText,
                        onValueChange = { complaintText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        label = { Text("Isi Pengaduan") },
                        minLines = 5,
                        maxLines = 8
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Section
                    Text(
                        text = "Lokasi Pengaduan",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Location Card with OutlinedTextField style
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
                                    } else {
                                        Text(
                                            text = "Tekan tombol untuk mengambil lokasi",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Image Upload Section
                    Text(
                        text = "Lampiran Gambar",
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
                                contentDescription = "Gambar Pengaduan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            IconButton(
                                onClick = {
                                    imageUri = null
                                    currentLatitude = null
                                    currentLongitude = null
                                    locationStatus = "Belum diambil"
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
                                    contentDescription = "Hapus Gambar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showImageSourceDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = "Upload Gambar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pilih Gambar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.submitComplaint(
                        name = inputName,
                        customerName = customerName ?: "",
                        isCustomer = isCustomer,
                        customerNumber = if (isCustomer) customerNo else null,
                        phoneNumber = phoneNumber,
                        complaintText = complaintText,
                        address = addressValue,
                        imageUri = imageUri,
                        cameraFilePath = if (cameraPhotoTaken) cameraFilePath else null,
                        latitude = currentLatitude,
                        longitude = currentLongitude
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Send, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kirim Pengaduan")
            }

            // Dialogs
            LoadingDialog(isLoading.value)

            errorState.value?.let { error ->
                ErrorDialog(
                    errorCode = error.first,
                    errorMessage = error.second,
                    onDismiss = { viewModel.clearError() }
                )
            }

            if (complaintSubmitted.value) {
                AlertDialog(
                    onDismissRequest = {
                        viewModel.resetSubmissionState()
                        navController.navigateUp()
                    },
                    title = { Text("Pengaduan Terkirim") },
                    text = { Text("Terima kasih atas pengaduan Anda.") },
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

            if (showImageSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showImageSourceDialog = false },
                    title = { Text("Pilih Sumber Gambar") },
                    text = { Text("Ambil foto akan otomatis mendeteksi lokasi Anda") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showImageSourceDialog = false
                                checkAndRequestCameraPermission()
                            }
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kamera")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showImageSourceDialog = false
                                checkAndRequestStoragePermission()
                            }
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galeri")
                        }
                    }
                )
            }

            if (showLocationPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationPermissionDialog = false },
                    title = { Text("Izin Lokasi Diperlukan") },
                    text = { Text("Aplikasi memerlukan izin lokasi untuk menandai posisi pengaduan Anda.") },
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
                    text = { Text("Aplikasi memerlukan izin untuk mengakses kamera dan penyimpanan.") },
                    confirmButton = {
                        Button(onClick = { showPermissionDeniedDialog = false }) { Text("OK") }
                    }
                )
            }
        }
    }
}