package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.RentPayment
import com.example.data.Tenant
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.RentPayViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

// Formatters
val rupeesFormat = DecimalFormat("##,##,##0.00")

fun safeStartActivity(context: Context, intent: Intent) {
    try {
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Desired action or application is not available on this device", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainAppContainer(viewModel: RentPayViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            is AppScreen.Splash -> SplashScreen {
                if (activeUser != null) {
                    viewModel.navigateTo(AppScreen.Dashboard)
                } else {
                    viewModel.navigateTo(AppScreen.Login)
                }
            }
            is AppScreen.Login -> LoginScreen(viewModel)
            is AppScreen.Signup -> SignupScreen(viewModel)
            is AppScreen.Dashboard -> AuthenticatedLayout(viewModel, AppScreen.Dashboard) {
                DashboardScreen(viewModel)
            }
            is AppScreen.RentCollection -> AuthenticatedLayout(viewModel, AppScreen.RentCollection) {
                RentCollectionScreen(viewModel)
            }
            is AppScreen.PaymentHistory -> AuthenticatedLayout(viewModel, AppScreen.PaymentHistory) {
                PaymentHistoryScreen(viewModel)
            }
            is AppScreen.AddTenant -> AddTenantScreen(viewModel)
            is AppScreen.TenantDetail -> TenantDetailScreen(viewModel)
        }
    }
}

// -------------------------------------------------------------
// NAVIGATION LAYOUT
// -------------------------------------------------------------
@Composable
fun AuthenticatedLayout(
    viewModel: RentPayViewModel,
    activeTab: AppScreen,
    content: @Composable () -> Unit
) {
    val syncing by viewModel.repository.syncingState.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val selectedProperty by viewModel.selectedProperty.collectAsState()
    var showPropertyMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                // Top Custom Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .drawBehind {
                            val r = this.size.width * 0.45f
                            if (r > 0f) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(PrimaryColor.copy(alpha = 0.35f), Color.Transparent),
                                        center = Offset(x = this.size.width, y = 0f),
                                        radius = r
                                    ),
                                    radius = r,
                                    center = Offset(x = this.size.width, y = 0f)
                                )
                            }
                        }
                        .background(Color(0xFF1A1A1A))
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom App Identity Label
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryColor)
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(
                                        text = "R",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RentPay",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Welcome, ${activeUser?.name ?: "Owner"}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        // Property Sector Button (Multi-property support)
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF33312D))
                                    .clickable { showPropertyMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = "Property",
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedProperty,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showPropertyMenu,
                                onDismissRequest = { showPropertyMenu = false }
                            ) {
                                viewModel.propertiesList.forEach { prop ->
                                    DropdownMenuItem(
                                        text = { Text(prop) },
                                        onClick = {
                                            viewModel.selectProperty(prop)
                                            showPropertyMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Sync Indicator & Logout
                        IconButton(
                            onClick = { viewModel.triggerSync() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (syncing) {
                                CircularProgressIndicator(
                                    color = PrimaryColor,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = ColorSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.handleLogout() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == AppScreen.Dashboard,
                    onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == AppScreen.RentCollection,
                    onClick = { viewModel.navigateTo(AppScreen.RentCollection) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Rent Collection") },
                    label = { Text("Collect") },
                    modifier = Modifier.testTag("nav_tab_collect")
                )
                NavigationBarItem(
                    selected = activeTab == AppScreen.PaymentHistory,
                    onClick = { viewModel.navigateTo(AppScreen.PaymentHistory) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_tab_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

// -------------------------------------------------------------
// SPLASH & AUTHENTICATION SCREENS
// -------------------------------------------------------------
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1B)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated logo icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RP",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "RentPay",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "PG & Hostel Management",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LoginScreen(viewModel: RentPayViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage your PG properties with ease",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Forgot Password Banner
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Password reset link sent to your registered email!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?", color = PrimaryColor, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.handleLogin(email.trim(), password) {
                            Toast.makeText(context, "Successfully Logged In!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Login Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.navigateTo(AppScreen.Signup) }
                ) {
                    Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Standard Google Sign In simulator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable {
                            Toast.makeText(context, "Google Sign-In Connected!", Toast.LENGTH_SHORT).show()
                            // Mock login
                            viewModel.handleLogin("owner@rentpay.com", "password123") {}
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SignupScreen(viewModel: RentPayViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sign up as a PG property manager",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.handleSignup(name, email.trim(), password) {
                            Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("signup_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Register & Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.navigateTo(AppScreen.Login) }
                ) {
                    Text("Already registered? Login", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DASHBOARD SCREEN
// -------------------------------------------------------------
@Composable
fun DashboardScreen(viewModel: RentPayViewModel) {
    val stats by viewModel.monthlyStatsState.collectAsState()
    val tenantList by viewModel.tenants.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val paymentMap = remember(payments, selectedMonth) {
        payments.filter { it.month == selectedMonth }.associateBy { it.tenantId }
    }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rent Collection Statistics Banner (Header text cards)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rent Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = selectedMonth,
                        color = PrimaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Collected",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${rupeesFormat.format(stats.totalCollected)}",
                                    color = ColorSuccess,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Pending",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${rupeesFormat.format(stats.pendingAmount)}",
                                    color = ColorGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val collectedRatio = if (stats.expectedAmount > 0) {
                            (stats.totalCollected / stats.expectedAmount).toFloat()
                        } else 0.0f
                        
                        val pendingRatio = if (stats.expectedAmount > 0) {
                            (stats.pendingAmount / stats.expectedAmount).toFloat()
                        } else 0.0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            if (collectedRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(collectedRatio.coerceAtLeast(0.01f))
                                        .background(ColorSuccess)
                                )
                            }
                            if (pendingRatio > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(pendingRatio.coerceAtLeast(0.01f))
                                        .background(ColorGold)
                                )
                            }
                            val remaining = (1.0f - collectedRatio - pendingRatio).coerceAtLeast(0f)
                            if (remaining > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(remaining.coerceAtLeast(0.01f))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ColorSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${stats.paidTenants} Paid",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ColorGold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${stats.pendingTenants} Pending",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Tenants",
                    value = "${stats.totalTenants} Beds",
                    icon = Icons.Default.People,
                    gradientColor = ColorSuccess,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending Rents",
                    value = "${stats.pendingTenants} Due",
                    icon = Icons.Default.PendingActions,
                    gradientColor = ColorGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Quick links list
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    label = "Add Tenant",
                    icon = Icons.Default.PersonAdd,
                    color = PrimaryColor
                ) {
                    viewModel.navigateTo(AppScreen.AddTenant)
                }

                QuickActionChip(
                    label = "Mark Payments",
                    icon = Icons.Default.DoneAll,
                    color = ColorSuccess
                ) {
                    viewModel.navigateTo(AppScreen.RentCollection)
                }

                QuickActionChip(
                    label = "Sync Data",
                    icon = Icons.Default.CloudQueue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    viewModel.triggerSync()
                    Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()
                }

                QuickActionChip(
                    label = "Reports",
                    icon = Icons.Default.Analytics,
                    color = ColorGold
                ) {
                    Toast.makeText(context, "Occupancy is at ${(tenantList.size.toFloat() / 15f * 100).toInt()}% with ${stats.pendingTenants} pending balances.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Recent Active Tenants Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PG Tenants List (${tenantList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { viewModel.navigateTo(AppScreen.RentCollection) }
                ) {
                    Text("Manage Rents", color = PrimaryColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Tenant Cards
        if (tenantList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No active tenants yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Click 'Add Tenant' to get started!", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(tenantList) { tenant ->
                val isPaid = paymentMap.containsKey(tenant.id)
                TenantCompactCard(
                    tenant = tenant,
                    isPaid = isPaid,
                    onClick = { viewModel.selectTenant(tenant.id) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// RENT COLLECTION SCREEN
// -------------------------------------------------------------
@Composable
fun RentCollectionScreen(viewModel: RentPayViewModel) {
    val stats by viewModel.monthlyStatsState.collectAsState()
    val tenantsList by viewModel.tenants.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val context = LocalContext.current

    var selectedTenantToPay by remember { mutableStateOf<Tenant?>(null) }
    var payAmountString by remember { mutableStateOf("") }
    var payMode by remember { mutableStateOf("UPI") }
    var payNotes by remember { mutableStateOf("") }

    // Sort: Pending tenants first
    val paymentMap = payments.filter { it.month == selectedMonth }.associateBy { it.tenantId }
    val sortedTenants = tenantsList.sortedBy { tenant ->
        if (paymentMap.containsKey(tenant.id)) 1 else 0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Collect Header Stats
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("EXPECTED", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${rupeesFormat.format(stats.expectedAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COLLECTED", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${rupeesFormat.format(stats.totalCollected)}", fontWeight = FontWeight.Bold, color = ColorSuccess)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PENDING", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${rupeesFormat.format(stats.pendingAmount)}", fontWeight = FontWeight.Bold, color = PrimaryColor)
                }
            }
        }

        // Collection List Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedMonth Ledgers",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Select Month menu drop
            var showMonths by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { showMonths = true }) {
                    Text("Select Month", color = PrimaryColor, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = showMonths, onDismissRequest = { showMonths = false }) {
                    viewModel.availableMonths.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                viewModel.selectMonth(m)
                                showMonths = false
                            }
                        )
                    }
                }
            }
        }

        // Group Payment listings
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sortedTenants) { tenant ->
                val isPaid = paymentMap.containsKey(tenant.id)
                val statusText = if (isPaid) "PAID" else "PENDING"
                val statusBg = if (isPaid) ColorPaidBg else ColorPendingBg
                val statusTextCol = if (isPaid) ColorPaidText else ColorPendingText

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (!isPaid) PrimaryColor.copy(alpha = 0.3f) else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tenant.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(statusText, color = statusTextCol, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Room ${tenant.roomNumber} • Bed ${tenant.bedNumber}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Rent: ₹${rupeesFormat.format(tenant.monthlyRent)} • Due: ${tenant.rentDueDate}th",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Collection Actions
                        Row {
                            if (!isPaid) {
                                Button(
                                    onClick = {
                                        selectedTenantToPay = tenant
                                        payAmountString = tenant.monthlyRent.toInt().toString()
                                        payNotes = "Rent for $selectedMonth"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("collect_pay_btn_${tenant.id}")
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = "Collect", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pay", fontSize = 12.sp)
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val pm = paymentMap[tenant.id]
                                        if (pm != null) {
                                            viewModel.deleteRentRecord(pm)
                                            Toast.makeText(context, "Payment record deleted!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Refund", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // RECORD PAYMENT DIALOG
    if (selectedTenantToPay != null) {
        Dialog(onDismissRequest = { selectedTenantToPay = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Record Rent Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tenant: ${selectedTenantToPay?.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = payAmountString,
                        onValueChange = { payAmountString = it },
                        label = { Text("Amount Paid (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_pay_amount")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Payment Mode",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("UPI", "Cash", "Bank Transfer", "Cheque").forEach { mode ->
                            val isSel = payMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PrimaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    .clickable { payMode = mode }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mode,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("Payment Record Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedTenantToPay = null }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = payAmountString.toDoubleOrNull() ?: 0.0
                                val tid = selectedTenantToPay?.id
                                if (tid != null && amt > 0) {
                                    viewModel.recordRentPaid(tid, selectedMonth, amt, payMode, payNotes) {
                                        Toast.makeText(context, "Payment Saved Successfully!", Toast.LENGTH_SHORT).show()
                                        selectedTenantToPay = null
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            modifier = Modifier.testTag("record_pay_submit")
                        ) {
                            Text("Confirm Payment")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PAYMENT HISTORY SCREEN
// -------------------------------------------------------------
@Composable
fun PaymentHistoryScreen(viewModel: RentPayViewModel) {
    val payments by viewModel.payments.collectAsState()
    val searchQuery by viewModel.paymentSearchQuery.collectAsState()
    val filterMode by viewModel.paymentModeFilter.collectAsState()
    val context = LocalContext.current

    var selectedReceiptPayment by remember { mutableStateOf<RentPayment?>(null) }

    // Filter list
    val filteredPayments = payments.filter {
        val matchQuery = it.tenantName.contains(searchQuery, ignoreCase = true) ||
                it.roomNumber.contains(searchQuery, ignoreCase = true)
        val matchMode = filterMode == "All" || it.paymentMode == filterMode
        matchQuery && matchMode
    }

    // Grouping by Month
    val groupedPayments = filteredPayments.groupBy { it.month }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Filters bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchPayments(it) },
            label = { Text("Search by tenant or room number...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payments_search_bar"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Horizontal Row scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "UPI", "Cash", "Bank Transfer", "Cheque").forEach { mode ->
                val isSel = filterMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) PrimaryColor else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clickable { viewModel.filterPaymentMode(mode) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        mode,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Payment groupings
        if (groupedPayments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Empty", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedPayments.forEach { (month, monthRecordList) ->
                    item {
                        Text(
                             text = month,
                             fontWeight = FontWeight.Black,
                             fontSize = 18.sp,
                             color = PrimaryColor,
                             modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(monthRecordList) { record ->
                        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(
                            Date(record.paymentDate)
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReceiptPayment = record },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = record.tenantName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Room ${record.roomNumber} • ${record.paymentMode}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formattedDate,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${rupeesFormat.format(record.amountPaid)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = ColorSuccess
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                            .clickable {
                                                // Generate printable text receipt and prompt sharing!
                                                val shareBody = """
                                                    RentPay digital receipt
                                                    =======================
                                                    Tenant Name: ${record.tenantName}
                                                    Room Number: ${record.roomNumber}
                                                    Amount Paid: ₹${record.amountPaid}
                                                    Payment Mode: ${record.paymentMode}
                                                    Month: ${record.month}
                                                    Date: $formattedDate
                                                    Status: SUCCESSFUL
                                                    Thank you for your business!
                                                """.trimIndent()
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                     type = "text/plain"
                                                     putExtra(Intent.EXTRA_TEXT, shareBody)
                                                }
                                                safeStartActivity(context, Intent.createChooser(intent, "Share PDF Receipt"))
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Receipt", modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Receipt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // RECEIPT PREVIEW DIALOG
    if (selectedReceiptPayment != null) {
        val rec = selectedReceiptPayment!!
        Dialog(onDismissRequest = { selectedReceiptPayment = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ColorPaidBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Success", tint = ColorPaidText, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Transaction Receipt", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("RentPay PG Manager System", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Tenant Name:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(rec.tenantName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Room Number:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(rec.roomNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Amount Repaid:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("₹${rupeesFormat.format(rec.amountPaid)}", fontWeight = FontWeight.Bold, color = ColorSuccess, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Payment Mode:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(rec.paymentMode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Period / Month:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(rec.month, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR code Simulator for verifying receipt authenticity
                    val qrColor = MaterialTheme.colorScheme.onSurface
                    val qrBgColor = MaterialTheme.colorScheme.surface
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawRect(qrColor, size = size)
                        drawRect(qrBgColor, topLeft = androidx.compose.ui.geometry.Offset(10f, 10f), size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 60.dp.toPx()))
                        drawRect(qrColor, topLeft = androidx.compose.ui.geometry.Offset(20f, 20f), size = androidx.compose.ui.geometry.Size(15.dp.toPx(), 15.dp.toPx()))
                        drawRect(qrColor, topLeft = androidx.compose.ui.geometry.Offset(45f, 45f), size = androidx.compose.ui.geometry.Size(15.dp.toPx(), 15.dp.toPx()))
                    }
                    Text("Scan to Verify Transaction", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val textContent = "Payment Verified of ₹${rec.amountPaid} from room ${rec.roomNumber} for the month: ${rec.month} via RentPay."
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, textContent)
                                }
                                safeStartActivity(context, Intent.createChooser(shareIntent, "Share Receipt Info"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { selectedReceiptPayment = null },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADD TENANT SCREEN
// -------------------------------------------------------------
@Composable
fun AddTenantScreen(viewModel: RentPayViewModel) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var idProofType by remember { mutableStateOf("Aadhaar") }
    var idProofNumber by remember { mutableStateOf("") }
    var photoUriString by remember { mutableStateOf<String?>(null) }
    var roomNumber by remember { mutableStateOf("") }
    var bedNumber by remember { mutableStateOf("") }
    var sharingType by remember { mutableStateOf("2-Sharing") }
    var monthlyRent by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("") }
    var moveInDate by remember { mutableStateOf("2026-05-25") }
    var rentDueDate by remember { mutableStateOf("5") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF23221E))
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Add New PG Tenant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile image pick simulator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(2.dp, PrimaryColor, CircleShape)
                            .clickable {
                                // Simulate image picking
                                photoUriString = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=120"
                                Toast.makeText(context, "Photo Picked / Simulated Avatar Selected!", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUriString != null) {
                            AsyncImage(
                                model = photoUriString,
                                contentDescription = "Tenant Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add Avatar", tint = PrimaryColor, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Upload Profile Photo", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text("Personal Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryColor, modifier = Modifier.padding(bottom = 8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tenant Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tenant_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tenant_phone_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Gender *", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Male", "Female", "Other").forEach { g ->
                    val isSel = gender == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PrimaryColor else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { gender = g }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(g, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = idProofType,
                    onValueChange = { idProofType = it },
                    label = { Text("ID Type") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = idProofNumber,
                    onValueChange = { idProofNumber = it },
                    label = { Text("ID Number") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Room & Bed Assignment", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryColor, modifier = Modifier.padding(bottom = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room No *") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("room_no_input"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bedNumber,
                    onValueChange = { bedNumber = it },
                    label = { Text("Bed No *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Sharing Type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Single", "2-Sharing", "3-Sharing", "4-Sharing").forEach { sh ->
                    val isSel = sharingType == sh
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PrimaryColor else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSel) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { sharingType = sh }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sh, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Rent Details & Move-In", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryColor, modifier = Modifier.padding(bottom = 8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = monthlyRent,
                    onValueChange = { monthlyRent = it },
                    label = { Text("Rent / Month *") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rent_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = depositAmount,
                    onValueChange = { depositAmount = it },
                    label = { Text("Deposit Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = moveInDate,
                    onValueChange = { moveInDate = it },
                    label = { Text("Move-In Date") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rentDueDate,
                    onValueChange = { rentDueDate = it },
                    label = { Text("Due Date") },
                    modifier = Modifier.weight(0.8f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes (e.g. workspace detail)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || roomNumber.isBlank() || bedNumber.isBlank() || monthlyRent.isBlank()) {
                        Toast.makeText(context, "Please fill in all starred (*) fields!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val rent = monthlyRent.toDoubleOrNull() ?: 0.0
                    val dep = depositAmount.toDoubleOrNull() ?: 0.0
                    val dueVal = rentDueDate.toIntOrNull() ?: 5

                    viewModel.saveTenant(
                        name = name.trim(),
                        phone = phone.trim(),
                        gender = gender,
                        idProofType = idProofType,
                        idProofNumber = idProofNumber.trim(),
                        photoPath = photoUriString,
                        roomNumber = roomNumber.trim(),
                        bedNumber = bedNumber.trim(),
                        sharingType = sharingType,
                        monthlyRent = rent,
                        depositAmount = dep,
                        moveInDate = moveInDate,
                        rentDueDate = dueVal,
                        notes = notes.trim()
                    ) {
                        Toast.makeText(context, "Tenant Registered Successfully!", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo(AppScreen.Dashboard)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_tenant_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Onboard Tenant", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// TENANT DETAIL SCREEN
// -------------------------------------------------------------
@Composable
fun TenantDetailScreen(viewModel: RentPayViewModel) {
    val tenant by viewModel.tenantDetail.collectAsState()
    val tenantPayments by viewModel.tenantPayments.collectAsState()
    val context = LocalContext.current

    if (tenant == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryColor)
        }
        return
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF23221E))
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Tenant Space Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row {
                        IconButton(
                            onClick = {
                                viewModel.removeTenant(tenant!!.id) {
                                    Toast.makeText(context, "Tenant profile deleted successfully!", Toast.LENGTH_SHORT).show()
                                    viewModel.navigateTo(AppScreen.Dashboard)
                                }
                            }
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Onboard out / Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header detail card Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Profile Photo
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (tenant?.photoPath != null) {
                                AsyncImage(
                                    model = tenant?.photoPath,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Avatar", tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(tenant!!.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            Text("Phone: ${tenant!!.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Gender: ${tenant!!.gender} • ID: ${tenant!!.idProofType} (${tenant!!.idProofNumber})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Quick Actions: Remind, WhatsApp
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val msg = "Hello ${tenant!!.name}, your PG Rent of ₹${tenant!!.monthlyRent} is currently due. Please complete payment at your earliest convenience using UPI. App link: upi://pay?pa=owner@rentpay&pn=RentPay&am=${tenant!!.monthlyRent}. Thank you!"
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=91${tenant!!.phone}&text=${Uri.encode(msg)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            safeStartActivity(context, intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Reminds")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remind WhatsApp", fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant!!.phone}"))
                            safeStartActivity(context, intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Tenant", fontSize = 12.sp)
                    }
                }
            }

            // Property Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Hostel Room Assignment", fontWeight = FontWeight.Bold, color = PrimaryColor, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Room / Bed Assigned", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("Room ${tenant!!.roomNumber} • Bed ${tenant!!.bedNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Sharing Configuration", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(tenant!!.sharingType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Monthly Rent Installment", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("₹${rupeesFormat.format(tenant!!.monthlyRent)}", fontWeight = FontWeight.Bold, color = ColorSuccess, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Security Deposit Paid", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text("₹${rupeesFormat.format(tenant!!.depositAmount)}", fontWeight = FontWeight.Bold, color = ColorGold, fontSize = 13.sp)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Move-In Date", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Text(tenant!!.moveInDate, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                        if (tenant!!.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Notes:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(tenant!!.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Payment Records Title
            item {
                Text(
                    text = "Tenant Payment History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tenant Payment history items
            if (tenantPayments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No payment statements yet.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            } else {
                items(tenantPayments) { pm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pm.month, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(
                                    Date(pm.paymentDate)
                                )
                                Text("Paid via ${pm.paymentMode} on $formattedDate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "₹${rupeesFormat.format(pm.amountPaid)}",
                                fontWeight = FontWeight.Black,
                                color = ColorSuccess,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER UI WIDGETS
// -------------------------------------------------------------
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradientColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(gradientColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = gradientColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TenantCompactCard(
    tenant: Tenant,
    isPaid: Boolean = false,
    onClick: () -> Unit
) {
    val borderCol = MaterialTheme.colorScheme.outlineVariant
    val statusText = if (isPaid) "PAID" else "PENDING"
    val statusBg = if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val statusTextCol = if (isPaid) Color(0xFF1D9C6C) else Color(0xFFE8A020)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, borderCol, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (tenant.photoPath != null) {
                    AsyncImage(
                        model = tenant.photoPath,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initials = tenant.name.split(" ").map { it.take(1) }.joinToString("").take(2).uppercase()
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tenant.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Room ${tenant.roomNumber} • ${tenant.sharingType}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = statusBg)
                ) {
                    Text(
                        text = statusText,
                        color = statusTextCol,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${rupeesFormat.format(tenant.monthlyRent)}/mo",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
