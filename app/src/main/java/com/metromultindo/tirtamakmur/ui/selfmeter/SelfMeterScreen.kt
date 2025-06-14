package com.metromultindo.tirtamakmur.ui.selfmeter

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
import com.metromultindo.tirtamakmur.ui.components.ErrorDialog
import com.metromultindo.tirtamakmur.ui.components.LoadingDialog
import com.metromultindo.tirtamakmur.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.metromultindo.tirtamakmur.ui.components.ErrorDialog2
import com.metromultindo.tirtamakmur.ui.customer.InfoRow

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

    val savedCustomerNumber = viewModel.savedCustomerNumber.collectAsState()

    // Local state
    var customerNumber by remember { mutableStateOf("") }
    var editPhoneNumber by remember { mutableStateOf("") } // For phone editing
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

    var showWhatsAppErrorDialog by remember { mutableStateOf(false) }

    // Phone edit states
    var isEditingPhone by remember { mutableStateOf(false) }
    var tempPhoneNumber by remember { mutableStateOf("") }

    LaunchedEffect(savedCustomerNumber.value) {
        if (isLoggedIn.value && customerNumber.isEmpty() && !savedCustomerNumber.value.isNullOrEmpty()) {
            customerNumber = savedCustomerNumber.value!!
        }
    }

    LaunchedEffect(customerInfo.value) {
        customerInfo.value?.let { info ->
            editPhoneNumber = info.phone
        }
    }

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

    fun isValidWhatsAppNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
        return when {
            cleanNumber.startsWith("0") && cleanNumber.length >= 9 -> true
            cleanNumber.startsWith("62") && cleanNumber.length >= 10 -> true
            cleanNumber.startsWith("+62") && cleanNumber.length >= 11 -> true
            else -> false
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

            // Show intro card only if customer info is not loaded yet
            if (customerInfo.value == null) {
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Customer Number Input (if not logged in and customer info not loaded)
            if (customerInfo.value == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Cari Data Pelanggan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customerNumber,
                            onValueChange = { customerNumber = it },
                            label = { Text("ID Pel / No Samb") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Numbers, "ID Pel / No Samb") },
                            placeholder = { Text("Ketik ID Pel / No Samb") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.loadCustomerInfo(customerNumber)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customerNumber.isNotEmpty() && !isLoading.value
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

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Customer Info and Forms (when customer info is available)
            customerInfo.value?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)

                ) {
                    // Customer Info - moved to top
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Column {

                            BillDetailRow(
                                label = "ID Pel / No Samb",
                                value = info.custCode,
                                icon = Icons.Default.Numbers
                            )

                            BillDetailRow(
                                label = "Nama",
                                value = info.name,
                                icon = Icons.Default.Person
                            )

                            BillDetailRow(
                                label = "Alamat",
                                value = info.address,
                                icon = Icons.Default.Home
                            )

                            BillDetailRow(
                                label = "Tarif",
                                value = info.tariffClass,
                                icon = Icons.Outlined.Receipt
                            )
                            BillDetailRow(
                                label = "Status",
                                value = info.status,
                                icon = if (info.status.uppercase() == "AKTIF") Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                iconTint = if (info.status.uppercase() == "AKTIF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phone Number Section - moved below customer info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        if (isEditingPhone) {
                            OutlinedTextField(
                                value = tempPhoneNumber,
                                onValueChange = { tempPhoneNumber = it },
                                label = { Text("No. WhatsApp") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Phone, "WhatsApp") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = tempPhoneNumber.isNotEmpty() && !isValidWhatsAppNumber(tempPhoneNumber), // Tambah validasi visual
                                supportingText = if (tempPhoneNumber.isNotEmpty() && !isValidWhatsAppNumber(tempPhoneNumber)) {
                                    { Text("Format nomor tidak valid. Harus diawali 0 atau 62 dan minimal 9 digit", color = MaterialTheme.colorScheme.error) }
                                } else null                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isEditingPhone = false
                                        tempPhoneNumber = ""
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Batal")
                                }

                                Button(
                                    onClick = {
                                        viewModel.updateCustomerPhone(info.custCode, tempPhoneNumber)
                                        editPhoneNumber = tempPhoneNumber
                                        isEditingPhone = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = tempPhoneNumber.isNotEmpty() &&
                                            isValidWhatsAppNumber(tempPhoneNumber) && // Tambah validasi
                                            !phoneUpdateLoading.value
                                ) {
                                    if (phoneUpdateLoading.value) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Simpan Nomor")
                                }
                            }
                        } else {
                            // Display current phone number
                            OutlinedTextField(
                                value = if (editPhoneNumber.isNotEmpty()) editPhoneNumber else "",
                                onValueChange = { },
                                label = { Text("No. WhatsApp") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Phone, "WhatsApp") },
                                readOnly = true,
                                enabled = false,
                                placeholder = { Text("Belum diatur") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            isEditingPhone = true
                                            tempPhoneNumber = editPhoneNumber
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Nomor WhatsApp",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))
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

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Pastikan no whatsapp sudah sesuai, akan digunakan untuk komunikasi jika ada pemakaian atau tagihan melonjak.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meter Reading Form - removed stand meter input, only photo and location
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

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

                        Spacer(modifier = Modifier.height(8.dp))

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
                                                text = "Lokasi: $locationStatus",
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tambahkan state untuk error WhatsApp
                        var showWhatsAppErrorDialog by remember { mutableStateOf(false) }

// Modifikasi Submit Button
                        Button(
                            onClick = {
                                // Validasi nomor WhatsApp terlebih dahulu

                                if (!isValidWhatsAppNumber(editPhoneNumber)) {
                                    viewModel.setError(400, "Format nomor WhatsApp tidak valid. Harus diawali 0 atau 62 dan minimal 9 digit")
                                    return@Button
                                }

                                // Validasi foto dan lokasi
                                if (imageUri == null) {
                                    viewModel.setError(400, "Foto meter wajib disertakan")
                                    return@Button
                                }

                                if (currentLatitude == null || currentLongitude == null) {
                                    viewModel.setError(400, "Lokasi wajib disertakan")
                                    return@Button
                                }

                                if (editPhoneNumber.isEmpty()) {
                                    showWhatsAppErrorDialog = true
                                    return@Button
                                }

                                val customerCode = if (isLoggedIn.value) {
                                    info.custCode
                                } else {
                                    customerNumber
                                }

                                viewModel.submitMeterReading(
                                    customerNumber = customerCode,
                                    standMeter = 0, // Not using stand meter anymore
                                    imageUri = imageUri,
                                    cameraFilePath = if (cameraPhotoTaken) cameraFilePath else null,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = imageUri != null && currentLatitude != null && currentLongitude != null && !isLoading.value
                        ) {
                            Icon(Icons.Outlined.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Data Meter")
                        }

// Tambahkan dialog error WhatsApp di bagian dialog (setelah dialog lainnya)
                        if (showWhatsAppErrorDialog) {
                            AlertDialog(
                                onDismissRequest = { showWhatsAppErrorDialog = false },
                                icon = {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        "No. WhatsApp Belum Diisi",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = {
                                    Text("No wathsapp belum di isi, silahkan isi terlebih dahulu.")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { showWhatsAppErrorDialog = false }
                                    ) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        description = "Baca meter mandiri membantu mencegah kesalahan perhitungan tagihan yang mungkin terjadi karena kesalahan petugas atau faktor lain. Data photo Meter Air yang dikirimkan akan menjadi bukti pendukung jika terjadi perbedaan dalam perhitungan tagihan."
                    )

                    BenefitItem(
                        title = "Pencatatan Transparan",
                        description = "Dengan fitur ini, pencatatan jadi lebih transparan, cepat, dan tanpa perlu menunggu petugas datang."
                    )
                    BenefitItem(
                        title = "Tagihan Melonjak",
                        description = "Melalui fitur catat meter mandiri, ini akan menjadi solusi bersama untuk petugas catat meter PDAM Tirta Jati dan pelanggan di saat pembaca meter mendapati rumah yang terkunci atau ditinggal pergi oleh pemiliknya, kondisi tersebut tentunya menyulitkan petugas untuk mendapatkan photo/angka stand meter, Sehingga petugas tidak memiliki informasi untuk menentukan besar tagihan rekening pelanggan tersebut, dan tagihan pun diambil dari angka rata-rata pemakaian dalam beberapa bulan sebelumnya, dan kejadian tersebut akan menjadi potensi pemakaian/tagihan melonjak.",
                    )
                    BenefitItem(
                        title = "Lebih praktis dan efisien",
                        description = "Melalui fitur ini, pelanggan dapat mencatat secara mudah hanya dengan mengirim photo meter air, data yang dikirimkan akan diverifikasi dan selanjutnya digunakan untuk menghitung tagihan.",
                        isLast = true
                    )

                    // Call to Action
                    Text(
                        text = "Yuk, gunakan catat meter mandiri setiap bulan",
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
                ErrorDialog2(
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
                    text = { Text("Data meter berhasil dikirim.") },
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
                    title = { Text("Nomor WhatsApp Berhasil Diperbarui") },
                    text = {
                        Text("Nomor WhatsApp Anda telah berhasil diperbarui.")
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

@Composable
fun BillDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBold: Boolean = false,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),

            )
    }
}