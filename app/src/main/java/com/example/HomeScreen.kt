package com.example

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(prefs: AppPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vpnConfig by prefs.vpnConfig.collectAsState(initial = "")
    val targetApps by prefs.targetApps.collectAsState(initial = "")
    val isAutoVpnEnabled by prefs.autoVpnEnabled.collectAsState(initial = false)

    var configInput by remember(vpnConfig) { mutableStateOf(vpnConfig) }
    var appsInput by remember(targetApps) { mutableStateOf(targetApps) }

    var hasShizuku by remember { mutableStateOf(ShizukuHelper.hasShizukuPermission()) }
    val tunnelState by VpnTunnelManager.tunnelState.collectAsState()
    
    val isVpnConnected = tunnelState == com.wireguard.android.backend.Tunnel.State.UP

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Launcher request VPN permission
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, start service if switch is on
            if (isAutoVpnEnabled) {
                context.startForegroundService(Intent(context, AutoVpnService::class.java))
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            hasShizuku = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shizuku Status Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (hasShizuku) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasShizuku) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (hasShizuku) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Shizuku",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasShizuku) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (hasShizuku) "Đã kết nối và cấp quyền" else "Chưa cấp quyền chạy ngầm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasShizuku) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                if (!hasShizuku) {
                    Button(
                        onClick = { ShizukuHelper.requestShizukuPermission(101) { _, _ -> } },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cấp quyền")
                    }
                }
            }
        }

        // VPN Status Card
        val vpnColor by animateColorAsState(
            if (isVpnConnected) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
            label = "vpnColor"
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(vpnColor.copy(alpha = 0.8f), vpnColor)
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Trạng thái VPN",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (isVpnConnected) "Đang kết nối Singapore" else "Đã ngắt kết nối",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Auto VPN Switch
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kích hoạt Auto-VPN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Tự động bật VPN khi mở Target Apps", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = isAutoVpnEnabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            prefs.setAutoVpnEnabled(checked)
                            if (checked) {
                                val vpnIntent = VpnService.prepare(context)
                                if (vpnIntent != null) {
                                    vpnPermissionLauncher.launch(vpnIntent)
                                } else {
                                    context.startForegroundService(Intent(context, AutoVpnService::class.java))
                                }
                            } else {
                                context.stopService(Intent(context, AutoVpnService::class.java))
                            }
                        }
                    }
                )
            }
        }

        // Config Inputs
        OutlinedTextField(
            value = configInput,
            onValueChange = { configInput = it },
            label = { Text("Cấu hình WireGuard Singapore (.conf)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            maxLines = 10
        )
        
        OutlinedTextField(
            value = appsInput,
            onValueChange = { appsInput = it },
            label = { Text("Danh sách Target Apps (Package Name)") },
            placeholder = { Text("com.netflix.mediaclient, com.spotify.music") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    prefs.saveVpnConfig(configInput)
                    prefs.saveTargetApps(appsInput)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Lưu Cấu Hình", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
        }
    }
}
