package com.example.upipaymenttracker

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.example.upipaymenttracker.ui.components.*
import com.example.upipaymenttracker.ui.theme.UPIPAYMENTTRACKERTheme
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat {
    PDF, EXCEL, WORD, PPT
}

class MainActivity : FragmentActivity() {
    private val viewModel: TransactionViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        TransactionViewModelFactory(application, TransactionRepository(database.transactionDao(), database.budgetDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()

            UPIPAYMENTTRACKERTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                var permissionsGranted by remember { mutableStateOf(checkAllPermissions()) }
                var isAuthenticated by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionsGranted = results.values.all { it }
                }

                if (!permissionsGranted) {
                    PermissionScreen(onRequestPermissions = {
                        val perms = mutableListOf(
                            android.Manifest.permission.RECEIVE_SMS,
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    })
                } else if (!isAuthenticated) {
                    AuthScreen(onAuthenticated = { isAuthenticated = true })
                } else {
                    MainAppScreen(viewModel)
                }
            }
        }
    }

    private fun checkAllPermissions(): Boolean {
        val sms = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val camera = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val location = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        return sms && camera && notifications && location
    }
}

fun Context.findActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    
    val biometricPrompt = remember(activity) {
        if (activity == null) null else
        BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(activity, "Auth Error: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vyaay")
            .setSubtitle("Authenticate to access your data")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
    }

    LaunchedEffect(activity) {
        if (activity == null) {
            onAuthenticated()
            return@LaunchedEffect
        }
        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt?.authenticate(promptInfo)
        } else {
            onAuthenticated()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("App Locked", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { biometricPrompt?.authenticate(promptInfo) }) {
                Text("Unlock App")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: TransactionViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var showHeatmapSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) }
    var transactionToSplit by remember { mutableStateOf<TransactionModel?>(null) }
    var transactionToEditCategory by remember { mutableStateOf<TransactionModel?>(null) }
    
    val transactions by viewModel.filteredTransactions.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val budgets by viewModel.allBudgets.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val context = LocalContext.current
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val title = "Vyaay Spending Report - ${monthYearFormat.format(selectedDate.time)}"

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { TransactionExporter.generatePdf(context, transactions, title, it) }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { TransactionExporter.writeToUri(context, it, TransactionExporter.generateCsv(transactions)) }
    }
    val wordLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/msword")) { uri ->
        uri?.let { TransactionExporter.writeToUri(context, it, TransactionExporter.generateTextReport(transactions, title)) }
    }
    val pptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.ms-powerpoint")) { uri ->
        uri?.let { TransactionExporter.writeToUri(context, it, TransactionExporter.generateTextReport(transactions, title)) }
    }

    val categories = listOf("All", "Food & Groceries", "Shopping", "Travel", "Hotels", "Utilities", "Entertainment", "Business", "Personal", "Other Expenses")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (currentTab == 0) "Vyaay" else "Shared Wallet") },
                actions = {
                    if (currentTab == 0) {
                        IconButton(onClick = { showMap = !showMap }) {
                            Icon(imageVector = if (showMap) Icons.Default.List else Icons.Default.Map, null)
                        }
                    }
                    IconButton(onClick = { showExportDialog = true }) { 
                        Icon(Icons.Default.Download, contentDescription = "Export") 
                    }
                    IconButton(onClick = { showSettingsDialog = true }) { Icon(Icons.Default.Settings, null) }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Analytics, null) },
                    label = { Text("Tracker") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Group, null) },
                    label = { Text("Shared") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Default.Add, null) }
            }
        }
    ) { innerPadding ->
        if (showDialog) {
            AddTransactionDialog(onDismiss = { showDialog = false }, onSave = { viewModel.insert(it) })
        }
        if (showSettingsDialog) {
            SettingsDialog(onDismiss = { showSettingsDialog = false }, viewModel = viewModel)
        }
        if (showExportDialog) {
            ExportOptionsDialog(
                onDismiss = { showExportDialog = false },
                onFormatSelected = { format ->
                    val fileName = "Vyaay_${monthYearFormat.format(selectedDate.time).replace(" ", "_")}"
                    when (format) {
                        ExportFormat.PDF -> pdfLauncher.launch("$fileName.pdf")
                        ExportFormat.EXCEL -> csvLauncher.launch("$fileName.csv")
                        ExportFormat.WORD -> wordLauncher.launch("$fileName.doc")
                        ExportFormat.PPT -> pptLauncher.launch("$fileName.ppt")
                    }
                    showExportDialog = false
                }
            )
        }
        transactionToSplit?.let { trans ->
            SplitDialog(transaction = trans, onDismiss = { transactionToSplit = null }, onSave = { updatedTrans -> viewModel.insert(updatedTrans) } )
        }
        transactionToEditCategory?.let { trans ->
            EditCategoryDialog(
                transaction = trans,
                onDismiss = { transactionToEditCategory = null },
                onConfirm = { newCat ->
                    viewModel.update(trans.copy(category = newCat))
                }
            )
        }
        
        if (showHeatmapSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHeatmapSheet = false }
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.changeMonth(-1) }) { Icon(Icons.Default.ChevronLeft, null) }
                        Text(text = monthYearFormat.format(selectedDate.time), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.changeMonth(1) }) { Icon(Icons.Default.ChevronRight, null) }
                    }
                    HeatmapCalendar(transactions = transactions, selectedDate = selectedDate)
                }
            }
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            if (currentTab == 1) {
                SharedWalletScreen(
                    transactions = transactions,
                    onImportCode = { code ->
                        val imported = TransactionExporter.decodeSyncCode(code)
                        if (imported.isNotEmpty()) {
                            imported.forEach { viewModel.insert(it) }
                            Toast.makeText(context, "Imported ${imported.size} transactions!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid Sync Code", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else if (showMap) {
                SpendingMap(transactions = transactions, modifier = Modifier.weight(1f))
            } else {
                TransactionList(
                    transactions = transactions,
                    modifier = Modifier.weight(1f),
                    onTransactionClick = { transactionToEditCategory = it },
                    onDeleteTransaction = { viewModel.delete(it) },
                    headerContent = {
                        item { SummaryCard(totalAmount = totalSpending, categoryWise = categorySpending, budgets = budgets, onCalendarClick = { showHeatmapSheet = true }, currentMonthText = monthYearFormat.format(selectedDate.time)) }
                        item { SubscriptionSummary(subscriptions = subscriptions) }
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                placeholder = { Text("Search transactions...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Spacer(modifier = Modifier.width(8.dp))
                                categories.forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { viewModel.updateCategory(category) },
                                        label = { Text(category) }
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        item {
                            Text(
                                text = "Recent Transactions (Tap to Split)",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ExportOptionsDialog(onDismiss: () -> Unit, onFormatSelected: (ExportFormat) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Report") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select desired format:")
                Button(onClick = { onFormatSelected(ExportFormat.PDF) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Professional PDF")
                }
                Button(onClick = { onFormatSelected(ExportFormat.EXCEL) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.TableChart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Excel (CSV)")
                }
                Button(onClick = { onFormatSelected(ExportFormat.WORD) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Description, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Word Document")
                }
                Button(onClick = { onFormatSelected(ExportFormat.PPT) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PresentToAll, null)
                    Spacer(Modifier.width(8.dp))
                    Text("PowerPoint")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, viewModel: TransactionViewModel) {
    var budgetAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Groceries") }
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Food & Groceries", "Shopping", "Travel", "Hotels", "Utilities", "Entertainment", "Business", "Personal", "Other Expenses")
    
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Appearance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dynamic Color", modifier = Modifier.weight(1f))
                        Switch(checked = dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
                    }
                    Text("Theme Mode", style = MaterialTheme.typography.bodySmall)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set Category Budget", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Category: $selectedCategory")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = budgetAmount,
                        onValueChange = { budgetAmount = it },
                        label = { Text("Monthly Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val amt = budgetAmount.toDoubleOrNull()
                            if (amt != null) {
                                viewModel.setBudget(selectedCategory, amt)
                                budgetAmount = "" 
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Save Budget") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}