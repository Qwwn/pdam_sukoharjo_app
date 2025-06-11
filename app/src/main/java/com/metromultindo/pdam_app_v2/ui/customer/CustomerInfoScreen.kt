package com.metromultindo.pdam_app_v2.ui.customer

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metromultindo.pdam_app_v2.data.model.CustomerResponse
import com.metromultindo.pdam_app_v2.ui.components.ErrorDialog
import com.metromultindo.pdam_app_v2.ui.components.LoadingDialog
import com.metromultindo.pdam_app_v2.ui.theme.AppTheme
import com.metromultindo.pdam_app_v2.utils.formatCurrency
import java.text.NumberFormat
import java.util.Locale
import com.metromultindo.pdam_app_v2.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerInfoScreen(
    navController: NavController,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    val customerData = viewModel.customerData.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val errorState = viewModel.errorState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                title = {
                    Text(stringResource(id = R.string.customer_info))
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val screenWidth = maxWidth
            val contentWidth = if (screenWidth > 600.dp) 600.dp else screenWidth

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                customerData.value?.let { customer ->
                    // Pass both customer data and navController to CustomerInfoContent
                    CustomerInfoContent(
                        customer = customer,
                        navController = navController
                    )
                }

                // Show loading indicator
                LoadingDialog(isLoading.value)

                // Show error dialog
                errorState.value?.let { error ->
                    ErrorDialog(
                        errorCode = error.first,
                        errorMessage = error.second,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerInfoContent(customer: CustomerResponse, navController: NavController) {
    // Get the latest bill if available, for fallback data
    val latestBill = if (customer.cust_data.isNotEmpty()) customer.cust_data.first() else null

    // Determine the actual values to display with fallback logic
    val custName = if (customer.cust_name == "-" && latestBill != null) latestBill.name else customer.cust_name
    val custCode = if (customer.cust_code == "-" && latestBill != null) latestBill.customerNumber else customer.cust_code
    val custAddress = if (customer.cust_address == "-" && latestBill != null) latestBill.address else customer.cust_address

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Customer info
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.customer_info),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoRow(
                    label = stringResource(id = R.string.customer_number),
                    value = custCode,
                    icon = Icons.Default.Numbers
                )

                InfoRow(
                    label = stringResource(id = R.string.name),
                    value = custName,
                    icon = Icons.Default.Person
                )

                InfoRow(
                    label = stringResource(id = R.string.address),
                    value = custAddress,
                    icon = Icons.Default.Home
                )

                if (latestBill != null) {
                    InfoRow(
                        label = stringResource(id = R.string.tariff),
                        value = latestBill.tariffClass,
                        icon = Icons.Outlined.Receipt
                    )

                    InfoRow(
                        label = stringResource(id = R.string.customer_status),
                        value = latestBill.status,
                        icon = if (latestBill.status.uppercase() == "AKTIF") Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                        iconTint = if (latestBill.status.uppercase() == "AKTIF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        // Latest bill if available
        if (latestBill != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tagihan Terbaru",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))


                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            BillDetailRow(
                                label = stringResource(id = R.string.period),
                                value = latestBill.period.toString(),
                                icon = Icons.Outlined.DateRange
                            )

                            BillDetailRow(
                                label = stringResource(id = R.string.meter_stand),
                                value = "${latestBill.startMeter} - ${latestBill.endMeter}",
                                icon = Icons.Outlined.Speed
                            )

                            BillDetailRow(
                                label = stringResource(id = R.string.usage),
                                value = "${latestBill.usage} m³",
                                icon = Icons.Outlined.WaterDrop
                            )

                            BillDetailRow(
                                label = stringResource(id = R.string.tariff),
                                value = latestBill.tariffClass,
                                icon = Icons.Outlined.Receipt
                            )

                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )

                            BillDetailRow(
                                label = stringResource(id = R.string.water_bill),
                                value = formatCurrency(latestBill.block1_RP + latestBill.block2_RP + latestBill.block3_RP + latestBill.block4_RP + latestBill.adm_rp + latestBill.dmw_rp + latestBill.abonemen_rp + latestBill.ppn_rp - latestBill.discount_rp),
                                icon = Icons.Outlined.Water
                            )

                            if (latestBill.denda_rp > 0) {
                                BillDetailRow(
                                    label = stringResource(id = R.string.amercement),
                                    value = formatCurrency(latestBill.denda_rp),
                                    icon = Icons.Outlined.MoneyOff
                                )
                            }

                            if (latestBill.angsuran_rp > 0) {
                                BillDetailRow(
                                    label = "${stringResource(id = R.string.installment)} (${latestBill.angsuran_ke})",
                                    value = formatCurrency(latestBill.angsuran_rp),
                                    icon = Icons.Outlined.Payments
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )

                            BillDetailRow(
                                label = stringResource(id = R.string.total_bill),
                                value = formatCurrency(latestBill.billAmount),
                                icon = Icons.Outlined.ReceiptLong,
                                isBold = true
                            )


                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Billing summary
        if (customer.unpaid_sheets.toIntOrNull() ?: 0 > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.unpaid_bill),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Receipt,
                            contentDescription = "Unpaid Bills",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jumlah lembar: ${customer.unpaid_sheets}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoneyOff,
                            contentDescription = "Total Bill",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Total tagihan: ${formatCurrency(customer.total_tagihan)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(id = R.string.total_bill_extend),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center

                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.no_bill),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center

                        )

                    }
                    // Thank you text in italic
                    Spacer(modifier = Modifier.width(8.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.thank_you),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }
        }

        // Add some space before the thank you text
        Spacer(modifier = Modifier.height(24.dp))


        // Riwayat Tagihan button
        Button(
            onClick = { navController.navigate("billHistory") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.AccountBox,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(id = R.string.bill_history_title),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
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
                tint = iconTint,
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
fun BillDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBold: Boolean = false
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
                tint = MaterialTheme.colorScheme.primary
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

fun Int.toRupiahFormat(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(this.toLong()).replace("Rp", "Rp. ")
}