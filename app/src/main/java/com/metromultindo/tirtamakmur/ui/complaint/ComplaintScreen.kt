package com.metromultindo.tirtamakmur.ui.complaint

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
import com.metromultindo.tirtamakmur.ui.components.ErrorDialog
import com.metromultindo.tirtamakmur.ui.components.LoadingDialog
import com.metromultindo.tirtamakmur.ui.theme.AppTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import com.metromultindo.tirtamakmur.R
import com.metromultindo.tirtamakmur.data.model.CustomerResponse
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.input.KeyboardType

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

    // State management from ViewModel
    val isLoading = viewModel.isLoading.collectAsState()
    val errorState = viewModel.errorState.collectAsState()
    val customerInfo = viewModel.customerInfo.collectAsState()
    val complaintSubmitted = viewModel.complaintSubmitted.collectAsState()

    // Local state
    var customerSearchNumber by remember { mutableStateOf("") }
    var editPhoneNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFilePath by remember { mutableStateOf<String?>(null) }
    var cameraPhotoTaken by remember { mutableStateOf(false) }
    var complaintText by remember { mutableStateOf("") }
    var addressValue by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf(customerName ?: "") }
    val phoneUpdateSuccess = viewModel.phoneUpdateSuccess.collectAsState()
    val phoneUpdateLoading = viewModel.phoneUpdateLoading.collectAsState()

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
    var showWhatsAppErrorDialog by remember { mutableStateOf(false) }

    // Phone edit states
    var isEditingPhone by remember { mutableStateOf(false) }
    var tempPhoneNumber by remember { mutableStateOf("") }

    // Customer status
    var isCustomer by remember { mutableStateOf(customerNumber != null && customerNumber.isNotEmpty()) }

    // Initialize customer search number if provided
    LaunchedEffect(customerNumber) {
        if (!customerNumber.isNullOrEmpty()) {
            customerSearchNumber = customerNumber
            viewModel.loadCustomerInfo(customerNumber)
        }
    }

    // Initialize phone number when customer info is loaded
    LaunchedEffect(customerInfo.value) {
        customerInfo.value?.let { customer ->
            editPhoneNumber = customer.cust_phone ?: ""
        }
    }

    // Helper functions (same as SelfMeterScreen)
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
                Log.e("ComplaintScreen", "Cannot open settings", e2)
            }
        }
    }

    fun isValidWhatsAppNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
        return when {
            cleanNumber.startsWith("0") -> cleanNumber.length > 7
            cleanNumber.startsWith("62") -> cleanNumber.length > 10 // +62 + minimal 8 digit
            cleanNumber.startsWith("+62") -> cleanNumber.length > 10 // +62 + minimal 8 digit
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
            val imageFileName = "COMPLAINT_${timeStamp}_"
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
                title = { Text("Form Pengaduan") },
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
            Spacer(modifier = Modifier.height(4.dp))
            // Customer Status Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status Pelanggan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    if (!customerNumber.isNullOrEmpty()) customerSearchNumber = customerNumber
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
                                    customerSearchNumber = ""
                                    editPhoneNumber = ""
                                    viewModel.clearCustomerInfo()
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Search (if customer and no customer info)
            if (isCustomer && customerInfo.value == null) {
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
                            value = customerSearchNumber,
                            onValueChange = { customerSearchNumber = it },
                            label = { Text("No Sambung") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Numbers, "No Sambung") },
                            placeholder = { Text("Masukkan No Sambung") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.loadCustomerInfo(customerSearchNumber)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = customerSearchNumber.isNotEmpty() && !isLoading.value
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

            // Customer Info Display (if customer and has customer info)
            if (isCustomer && customerInfo.value != null) {
                customerInfo.value?.let { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                BillDetailRow(
                                    label = "No Sambung",
                                    value = customer.cust_code ?: "",
                                    icon = Icons.Default.Numbers
                                )

                                BillDetailRow(
                                    label = "Nama",
                                    value = customer.cust_name ?: "",
                                    icon = Icons.Default.Person
                                )

                                BillDetailRow(
                                    label = "Alamat",
                                    value = customer.cust_address ?: "",
                                    icon = Icons.Default.Home
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phone Number Section - UPDATED
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
                                    } else null
                                )

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
                                            viewModel.updateCustomerPhone(customer.cust_code ?: "", tempPhoneNumber)
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
                                        Text("Simpan")
                                    }
                                }
                            } else {
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
                                        text = "Pastikan no whatsapp sudah sesuai, akan digunakan untuk komunikasi terkait pengaduan.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Complaint Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = complaintText,
                                onValueChange = { complaintText = it },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                label = { Text("Isi Pengaduan") },
                                minLines = 5,
                                maxLines = 8,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Photo section
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
                                        contentDescription = "Foto Pengaduan",
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
                                    onClick = { showImageSourceDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Ambil Foto")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ambil Foto Bukti")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Location section
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

                            Button(
                                onClick = {
                                    // Validasi nomor WhatsApp terlebih dahulu
                                    if (editPhoneNumber.isEmpty()) {
                                        showWhatsAppErrorDialog = true
                                        return@Button
                                    }

                                    if (!isValidWhatsAppNumber(editPhoneNumber)) {
                                        viewModel.setError(400, "Format nomor WhatsApp tidak valid. Harus diawali 0 atau 62 dan minimal 9 digit")
                                        return@Button
                                    }

                                    // Validasi foto dan lokasi
                                    if (imageUri == null) {
                                        viewModel.setError(400, "Foto bukti wajib disertakan")
                                        return@Button
                                    }

                                    if (currentLatitude == null || currentLongitude == null) {
                                        viewModel.setError(400, "Lokasi wajib disertakan")
                                        return@Button
                                    }

                                    Log.d("ComplaintScreen", "Customer form data:")
                                    Log.d("ComplaintScreen", "Name: '${customer.cust_name ?: ""}'")
                                    Log.d("ComplaintScreen", "Customer Number: '${customer.cust_code ?: ""}'")
                                    Log.d("ComplaintScreen", "Phone: '$editPhoneNumber'")
                                    Log.d("ComplaintScreen", "Address: '${customer.cust_address ?: ""}'")
                                    Log.d("ComplaintScreen", "Complaint: '$complaintText'")

                                    viewModel.submitComplaint(
                                        name = customer.cust_name ?: "",
                                        customerName = customer.cust_name ?: "",
                                        isCustomer = true,
                                        customerNumber = customer.cust_code ?: "",
                                        phoneNumber = editPhoneNumber,
                                        complaintText = complaintText,
                                        address = customer.cust_address ?: "",
                                        imageUri = imageUri,
                                        cameraFilePath = if (cameraPhotoTaken) cameraFilePath else null,
                                        latitude = currentLatitude,
                                        longitude = currentLongitude
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = complaintText.isNotEmpty() &&
                                        editPhoneNumber.isNotEmpty() &&
                                        isValidWhatsAppNumber(editPhoneNumber) && // Validasi WhatsApp
                                        imageUri != null && // Validasi foto wajib
                                        currentLatitude != null && currentLongitude != null && // Validasi lokasi wajib
                                        !isLoading.value
                            ) {
                                Icon(Icons.Outlined.Send, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kirim Pengaduan")
                            }
                        }
                    }
                }
            }

            // Non-Customer Form dengan layout terpisah
            if (!isCustomer) {
                // Card 1: Nama dan Alamat
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Nama") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Outlined.Person, "Nama") }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = addressValue,
                            onValueChange = { addressValue = it },
                            label = { Text("Alamat") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Outlined.Home, "Alamat") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card 2: No WhatsApp
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("No. WhatsApp") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, "WhatsApp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneNumber.isNotEmpty() && !isValidWhatsAppNumber(phoneNumber), // Tambah validasi visual
                            supportingText = if (phoneNumber.isNotEmpty() && !isValidWhatsAppNumber(phoneNumber)) {
                                { Text("Format nomor tidak valid. Harus diawali 0 atau 62 dan minimal 9 digit", color = MaterialTheme.colorScheme.error) }
                            } else null
                        )


                        Spacer(modifier = Modifier.height(4.dp))

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
                                text = "Pastikan no whatsapp sudah sesuai, akan digunakan untuk komunikasi terkait pengaduan.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card 3: Isi Pengaduan, Gambar, dan Lokasi
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = complaintText,
                            onValueChange = { complaintText = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            label = { Text("Isi Pengaduan") },
                            minLines = 2,
                            maxLines = 8,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Photo section
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
                                    contentDescription = "Foto Pengaduan",
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
                                onClick = { showImageSourceDialog = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = "Ambil Foto")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ambil Foto Bukti")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Location section
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

                        Button(
                            onClick = {
                                // Validasi format nomor WhatsApp
                                if (!isValidWhatsAppNumber(phoneNumber)) {
                                    viewModel.setError(400, "Format nomor WhatsApp tidak valid. Harus diawali 0 atau 62 dan minimal 9 digit")
                                    return@Button
                                }

                                // Validasi foto dan lokasi
                                if (imageUri == null) {
                                    viewModel.setError(400, "Foto bukti wajib disertakan")
                                    return@Button
                                }

                                if (currentLatitude == null || currentLongitude == null) {
                                    viewModel.setError(400, "Lokasi wajib disertakan")
                                    return@Button
                                }

                                Log.d("ComplaintScreen", "Non-customer form data:")
                                Log.d("ComplaintScreen", "Name: '$inputName'")
                                Log.d("ComplaintScreen", "Phone: '$phoneNumber'")
                                Log.d("ComplaintScreen", "Address: '$addressValue'")
                                Log.d("ComplaintScreen", "Complaint: '$complaintText'")

                                viewModel.submitComplaint(
                                    name = inputName,
                                    customerName = "",
                                    isCustomer = false,
                                    customerNumber = null,
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
                            enabled = inputName.isNotEmpty() &&
                                    phoneNumber.isNotEmpty() &&
                                    isValidWhatsAppNumber(phoneNumber) && // Validasi WhatsApp
                                    addressValue.isNotEmpty() &&
                                    complaintText.isNotEmpty() &&
                                    imageUri != null && // Validasi foto wajib
                                    currentLatitude != null && currentLongitude != null && // Validasi lokasi wajib
                                    !isLoading.value
                        ) {
                            Icon(Icons.Outlined.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Pengaduan")
                        }
                    }
                }
            }

            LoadingDialog(isLoading.value || phoneUpdateLoading.value)

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

            // TAMBAHAN BARU - Phone update success dialog
            if (phoneUpdateSuccess.value) {
                AlertDialog(
                    onDismissRequest = {
                        // Reset phone update state dan editing state
                        viewModel.resetPhoneUpdateState()
                        editPhoneNumber = tempPhoneNumber
                        isEditingPhone = false
                        tempPhoneNumber = ""
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
                                // Reset phone update state dan editing state
                                viewModel.resetPhoneUpdateState()
                                editPhoneNumber = tempPhoneNumber
                                isEditingPhone = false
                                tempPhoneNumber = ""
                            }
                        ) { Text("OK") }
                    }
                )
            }
            // WhatsApp error dialog
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

            // Other dialogs (showImageSourceDialog, showLocationPermissionDialog, etc.)
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

// Tambahkan ini di bagian bawah ComplaintScreen.kt
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