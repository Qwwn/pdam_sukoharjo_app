package com.metromultindo.tirtamakmur.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metromultindo.tirtamakmur.ui.components.ErrorDialog
import com.metromultindo.tirtamakmur.ui.components.LoadingDialog
import com.metromultindo.tirtamakmur.ui.theme.AppTheme
import java.util.Calendar
import com.metromultindo.tirtamakmur.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    destination: String = "home",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val customerNumber = viewModel.customerNumber.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val errorState = viewModel.errorState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val navigateDestination = remember { mutableStateOf(destination) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Improved top app bar
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = getTimeBasedGreeting(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }

            // Main content - scrollable to handle different screen sizes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.mipmap.logo),
                    contentDescription = stringResource(id = R.string.app_logo),
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp)
                )


                Text(
                    text = stringResource(id = R.string.welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(id = R.string.entity_name),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Card for login form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.customer_number),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.Start),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customerNumber.value,
                            onValueChange = { viewModel.updateCustomerNumber(it) },
                            placeholder = { Text(stringResource(id = R.string.customer_number_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (customerNumber.value.isNotEmpty()) {
                                        viewModel.login()
                                    }
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.login() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = customerNumber.value.isNotEmpty() && !isLoading.value,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.check_bill),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { navController.navigate("home") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.back),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(id = R.string.bill_info_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Loading indicator
    LoadingDialog(isLoading.value)

    // Error dialog
    if (errorState.value != null) {
        ErrorDialog(
            errorCode = errorState.value!!.first,
            errorMessage = errorState.value!!.second,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Observe login success
    LaunchedEffect(viewModel.loginSuccess.collectAsState().value) {
        if (viewModel.loginSuccess.value) {
            // Navigate to the specified destination after login
            navController.navigate(navigateDestination.value) {
                popUpTo("login") { inclusive = true }
                popUpTo("login/{destination}") { inclusive = true }
            }
            viewModel.resetLoginSuccess()
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