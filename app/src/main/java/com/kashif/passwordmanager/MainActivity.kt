package com.kashif.passwordmanager

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kashif.passwordmanager.manager.TEECapabilityManager
import com.kashif.passwordmanager.model.BiometricStrength
import com.kashif.passwordmanager.model.Credential
import com.kashif.passwordmanager.model.Screen
import com.kashif.passwordmanager.model.SecurityLevel
import com.kashif.passwordmanager.model.TEECapabilities
import com.kashif.passwordmanager.ui.theme.PasswordManagerTheme
import com.kashif.passwordmanager.viewmodel.CredentialManagerViewModel
import com.kashif.passwordmanager.viewmodel.CredentialManagerViewModelFactory
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: CredentialManagerViewModel
    private lateinit var teeCapabilityManager: TEECapabilityManager
    private lateinit var  addCredential : FloatingActionButton

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        addCredential= findViewById<FloatingActionButton>(R.id.addCredentialFab)
        // Set onClick listener
        // Initialize TEE capability detection
        teeCapabilityManager = TEECapabilityManager(this)
        val capabilities = teeCapabilityManager.detectTEECapabilities()

        Log.i("TEE", "StrongBox: ${capabilities.hasStrongBox}")
        Log.i("TEE", "TEE: ${capabilities.hasTEE}")
        Log.i("TEE", "Biometric: ${capabilities.biometricStrength}")

        // Initialize ViewModel
        val factory = CredentialManagerViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[CredentialManagerViewModel::class.java]
        addCredential.setOnClickListener {
            // Show AddCredentialDialog (Compose)
            setContent {
                var showDialog by remember { mutableStateOf(true) }
                if (showDialog) {
                    AddCredentialDialog(
                        onDismiss = { showDialog = false },
                        onAdd = { name, username, password ->

                            lifecycleScope.launch {

                                val credential = Credential(
                                    id = UUID.randomUUID().toString(),
                                    title = name,
                                    username = username,
                                    password = password,
                                    category = "",
                                    createdAt = System.currentTimeMillis(),
                                    lastModified = System.currentTimeMillis()
                                )
                               viewModel.addCredential(credential)
                            }
                            showDialog = false
                        }
                    )
                }
            }
        }

        // Observe authentication state
        lifecycleScope.launch {
            viewModel.isAuthenticated.collect { isAuthenticated ->
                if (isAuthenticated) {
                    // Show main UI
                    setContent {
                        showMainInterface(capabilities)
                    }
                } else {
                    // Show authentication UI
                    showAuthenticationInterface()
                }
            }
        }

        // Observe credentials
        lifecycleScope.launch {
            viewModel.credentials.collect { credentials ->
                // Update UI with credentials
                updateCredentialsList(credentials)
            }
        }

        // Observe errors
        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    showError(it)
                    viewModel.clearError()
                }
            }
        }
    }

    private fun showAuthenticationInterface() {
        // Trigger biometric authentication
        viewModel.authenticate(this)
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.P)
    private fun showMainInterface(capabilities: TEECapabilities) {
        // Show main credential management interface
        CredentialManagerApp(capabilities)
    }

    private fun updateCredentialsList(credentials: List<Credential>) {
        // Update RecyclerView or other UI components
    }

    private fun showError(message: String) {
        // Show error message to user
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PasswordManagerTheme {
        Greeting("Android")
    }
}

@Composable
fun AddCredentialDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Credential")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username/Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "This credential will be encrypted using TEE-backed keys",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                        onAdd(name, username, password)
                    }
                },
                enabled = name.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun CredentialManagerApp(capabilities: TEECapabilities) {
    var currentScreen by remember { mutableStateOf(Screen.Security) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var credentials by remember { mutableStateOf<List<Credential>>(emptyList()) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1421),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "Secure Credential Manager",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = { currentScreen = Screen.Security }) {
                    Icon(Icons.Default.Star, contentDescription = "Security", tint = Color.White)
                }
                IconButton(onClick = { currentScreen = Screen.Credentials }) {
                    Icon(Icons.Default.Lock, contentDescription = "Credentials", tint = Color.White)
                }
            }
        )

        // Main Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (currentScreen) {
                Screen.Security -> SecurityStatusScreen(
                    securityStatus = capabilities,
                    onAuthenticated = {
                        isAuthenticated = true
                        currentScreen = Screen.Credentials
                    }
                )

                Screen.Credentials -> TODO()
            }
        }
    }
}


@Composable
fun SecurityStatusScreen(
    securityStatus: TEECapabilities?,
) {
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SecurityStatusCard(securityStatus)
        }
    }
}

@Composable
fun SecurityStatusCard(securityStatus: TEECapabilities?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2D3D).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = if (securityStatus?.hasStrongBox == true)
                        Color(0xFF4CAF50) else Color(0xFFFF9800),
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        "Security Status",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        securityStatus?.biometricStrength?.name ?: "Checking...",
                        fontSize = 14.sp,
                        color = if (securityStatus?.biometricStrength == BiometricStrength.STRONG)
                            Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f))
            securityStatus?.let { status ->
                SecurityMetricRow("TEE Available", status.hasStrongBox)
                SecurityMetricRow("Hardware Keystore", status.hasSecureDisplay)
                SecurityMetricRow("Biometric TEE", status.biometricStrength==BiometricStrength.STRONG)
                SecurityMetricRow("Secure Display", status.hasSecureDisplay)
            }
        }
    }
}
@Composable
fun SecurityMetricRow(label: String, isAvailable: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp
        )

        Icon(
            if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SecurityStatusScreen(
    securityStatus: TEECapabilities?,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SecurityStatusCard(securityStatus)
        }
    }
}


