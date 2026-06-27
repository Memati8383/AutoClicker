package com.example.autoclicker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autoclicker.ui.theme.AutoclickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AutoClickerViewModel.initTheme(this)
        enableEdgeToEdge()
        setContent {
            AutoclickerTheme(darkTheme = AutoClickerViewModel._isDarkTheme.value) {
                AutoClickerScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AutoClickerViewModel.checkServiceState(this)
        AutoClickerViewModel.checkOverlayState(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerScreen() {
    val context = LocalContext.current
    val vm = remember { AutoClickerViewModel(context) }

    LaunchedEffect(Unit) {
        vm.checkAccessibilityService()
        vm.checkOverlayPermission()
    }

    if (!vm.isServiceEnabled && !vm.dismissedA11yDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissedA11yDialog = true },
            title = { Text("Erişilebilirlik Servisi Gerekli") },
            text = {
                Text("Auto Clicker'ın çalışması için erişilebilirlik servisini açmanız gerekiyor:\nAyarlar > Erişilebilirlik > Auto Clicker")
            },
            confirmButton = {
                Button(onClick = { vm.dismissedA11yDialog = true; vm.openAccessibilitySettings() }) {
                    Text("Ayarlara Git")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissedA11yDialog = true }) { Text("Daha Sonra") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto Clicker", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(10.dp).clip(CircleShape).background(
                                if (vm.isServiceEnabled) Color(0xFF4CAF50) else Color(0xFFE53935)
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { AutoClickerViewModel.toggleTheme(context) }) {
                        Text(
                            if (AutoClickerViewModel._isDarkTheme.value) "☀" else "☾",
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickStatus(vm)

            Card(shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Çalışma Modu", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeChip(
                            label = "Tek Hedef",
                            selected = !vm.isMultiTarget,
                            onClick = { vm.setMode(false) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeChip(
                            label = "Çoklu Hedef",
                            selected = vm.isMultiTarget,
                            onClick = { vm.setMode(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (vm.isMultiTarget) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${vm.targets.size} hedef tanımlı — yüzen panelden düzenleyin",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Yüzen Panel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(8.dp).clip(CircleShape).background(
                                    if (vm.isOverlayActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                                )
                            )
                        }
                        Text(
                            if (vm.isOverlayActive) "Aktif — tüm ayarlar panelden yapılır"
                            else "Kapalı",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vm.isOverlayActive,
                        onCheckedChange = { vm.toggleOverlay(context) },
                        enabled = vm.hasOverlayPermission && vm.isServiceEnabled
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tüm ayarlar yüzen paneldeki ⚙ butonundan yapılır",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatus(vm: AutoClickerViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(
            label = "Servis",
            ok = vm.isServiceEnabled,
            actionLabel = if (!vm.isServiceEnabled) "Aç" else null,
            onAction = vm::openAccessibilitySettings,
            modifier = Modifier.weight(1f)
        )
        StatusBadge(
            label = "Overlay",
            ok = vm.hasOverlayPermission,
            actionLabel = if (!vm.hasOverlayPermission) "İzin Ver" else null,
            onAction = vm::openOverlaySettings,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusBadge(
    label: String, ok: Boolean, actionLabel: String?,
    onAction: (() -> Unit)?, modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(
                    if (ok) Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    label: String, selected: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (selected) 0.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp)
                )
                .padding(vertical = 14.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
