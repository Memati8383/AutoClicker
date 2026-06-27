package com.example.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showHelp by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        vm.checkAccessibilityService()
        vm.checkOverlayPermission()
    }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    if (!vm.isServiceEnabled && !vm.dismissedA11yDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissedA11yDialog = true },
            title = { Text(Lang.get("a11y_title")) },
            text = {
                Text(Lang.get("a11y_text"))
            },
            confirmButton = {
                Button(onClick = { vm.dismissedA11yDialog = true; vm.openAccessibilitySettings() }) {
                    Text(Lang.get("a11y_go"))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissedA11yDialog = true }) { Text(Lang.get("a11y_later")) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(290.dp)) {
                DrawerHeader()
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                val onHome = !showHelp && !showFeedback && !showAbout
                val activeColor = MaterialTheme.colorScheme.primary
                val activeBg = MaterialTheme.colorScheme.primaryContainer

                NavigationDrawerItem(
                    icon = { Text("🏠", fontSize = 18.sp) },
                    label = { Text(Lang.get("home"), fontWeight = if (onHome) FontWeight.Bold else FontWeight.Medium) },
                    selected = onHome,
                    onClick = { scope.launch { drawerState.close() }; showHelp = false; showFeedback = false; showAbout = false },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = activeBg,
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor
                    )
                )
                NavigationDrawerItem(
                    icon = { Text("❓", fontSize = 18.sp) },
                    label = { Text(Lang.get("help"), fontWeight = if (showHelp) FontWeight.Bold else FontWeight.Medium) },
                    selected = showHelp,
                    onClick = { scope.launch { drawerState.close() }; showHelp = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = activeBg,
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor
                    )
                )
                NavigationDrawerItem(
                    icon = { Text("📧", fontSize = 18.sp) },
                    label = { Text(Lang.get("feedback"), fontWeight = if (showFeedback) FontWeight.Bold else FontWeight.Medium) },
                    selected = showFeedback,
                    onClick = { scope.launch { drawerState.close() }; showFeedback = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = activeBg,
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor
                    )
                )
                NavigationDrawerItem(
                    icon = { Text("ℹ️", fontSize = 18.sp) },
                    label = { Text(Lang.get("about"), fontWeight = if (showAbout) FontWeight.Bold else FontWeight.Medium) },
                    selected = showAbout,
                    onClick = { scope.launch { drawerState.close() }; showAbout = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = activeBg,
                        selectedIconColor = activeColor,
                        selectedTextColor = activeColor
                    )
                )
                NavigationDrawerItem(
                    icon = { Text("🌐", fontSize = 18.sp) },
                    label = {
                        Row {
                            Text(
                                if (Lang.current == "tr") Lang.get("lang_tr") else Lang.get("lang_en"),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "(${if (Lang.current == "tr") "EN" else "TR"})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        AutoClickerViewModel.toggleLang(context)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                NavigationDrawerItem(
                    icon = { Text("📤", fontSize = 18.sp) },
                    label = { Text(Lang.get("share"), fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, Lang.get("share_text"))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, Lang.get("share")))
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(4.dp))
                NavigationDrawerItem(
                    icon = { Text("🚪", fontSize = 18.sp) },
                    label = { Text(Lang.get("exit")) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("☰", fontSize = 20.sp)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Lang.get("app_name"), fontWeight = FontWeight.Bold)
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
        val screen by remember { derivedStateOf {
            when {
                showHelp -> "help"
                showFeedback -> "feedback"
                showAbout -> "about"
                else -> "home"
            }
        } }
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val dir = if (targetState == "home") -1 else 1
                (slideInHorizontally { it * dir } + fadeIn()) togetherWith
                (slideOutHorizontally { it * -dir } + fadeOut())
            },
            label = "screenTransition",
            modifier = Modifier.padding(pad)
        ) { currentScreen ->
            when (currentScreen) {
                "help" -> HelpScreen(onBack = { showHelp = false })
                "feedback" -> FeedbackScreen(onBack = { showFeedback = false })
                "about" -> AboutScreen(onBack = { showAbout = false })
                else -> HomeScreen(vm, context)
            }
        }
    }
}

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(500)),
            label = "splashVisibility"
        ) {
            SplashScreen()
        }
    }
}

@Composable
private fun HomeScreen(vm: AutoClickerViewModel, context: Context) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val serviceAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dotPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically { it } + fadeIn(tween(400)),
            label = "welcomeAnim"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("🖱️", fontSize = 24.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(Lang.get("app_name"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(Lang.get("app_subtitle"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        QuickStatus(vm, serviceAlpha)

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically { it / 2 } + fadeIn(tween(500, delayMillis = 100)),
            label = "modeCardAnim"
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(Lang.get("mode_title"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModeChip(
                            label = Lang.get("mode_single"),
                            selected = !vm.isMultiTarget,
                            onClick = { vm.setMode(false) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeChip(
                            label = Lang.get("mode_multi"),
                            selected = vm.isMultiTarget,
                            onClick = { vm.setMode(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AnimatedVisibility(visible = vm.isMultiTarget, enter = fadeIn(tween(300)) + slideInVertically { it / 2 }, label = "targetsInfo") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Lang.targets(vm.targets.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically { it / 2 } + fadeIn(tween(500, delayMillis = 200)),
            label = "panelCardAnim"
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪟", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(Lang.get("panel_title"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(10.dp).clip(CircleShape).background(
                                    if (vm.isOverlayActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                                ).then(
                                    if (vm.isOverlayActive) Modifier.graphicsLayer(alpha = serviceAlpha) else Modifier
                                )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (vm.isOverlayActive) Lang.get("panel_active")
                            else Lang.get("panel_off"),
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
        }

        Spacer(Modifier.weight(1f))

        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(600, delayMillis = 350)),
            label = "footerAnim"
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚙", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        Lang.get("panel_hint"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatus(vm: AutoClickerViewModel, serviceAlpha: Float = 1f) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(
            label = Lang.get("badge_service"),
            ok = vm.isServiceEnabled,
            actionLabel = if (!vm.isServiceEnabled) Lang.get("badge_enable") else null,
            onAction = vm::openAccessibilitySettings,
            modifier = Modifier.weight(1f),
            pulseAlpha = if (vm.isServiceEnabled) serviceAlpha else 1f
        )
        StatusBadge(
            label = Lang.get("badge_overlay"),
            ok = vm.hasOverlayPermission,
            actionLabel = if (!vm.hasOverlayPermission) Lang.get("badge_grant") else null,
            onAction = vm::openOverlaySettings,
            modifier = Modifier.weight(1f),
            pulseAlpha = if (vm.hasOverlayPermission) serviceAlpha else 1f
        )
    }
}



@Composable
private fun StatusBadge(
    label: String, ok: Boolean, actionLabel: String?,
    onAction: (() -> Unit)?, modifier: Modifier = Modifier,
    pulseAlpha: Float = 1f
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape).background(
                    if (ok) Color(0xFF4CAF50) else Color(0xFFE53935)
                ).graphicsLayer(alpha = if (ok) pulseAlpha else 1f)
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

@Composable
private fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp)
            }
            Spacer(Modifier.width(4.dp))
            Text(Lang.get("help_title"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.get("help_setup"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                HelpItem("1", Lang.get("help_step1"), Lang.get("help_step1_desc"))
                Spacer(Modifier.height(8.dp))
                HelpItem("2", Lang.get("help_step2"), Lang.get("help_step2_desc"))
                Spacer(Modifier.height(8.dp))
                HelpItem("3", Lang.get("help_step3"), Lang.get("help_step3_desc"))
            }
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.get("help_usage"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                HelpItem("", Lang.get("help_single"), Lang.get("help_single_desc"))
                Spacer(Modifier.height(8.dp))
                HelpItem("", Lang.get("help_multi"), Lang.get("help_multi_desc"))
                Spacer(Modifier.height(8.dp))
                HelpItem("", Lang.get("help_stop"), Lang.get("help_stop_desc"))
            }
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    Lang.get("help_info"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun HelpItem(step: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        if (step.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(step, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeedbackScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var feedbackType by remember { mutableStateOf("feedback_idea") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current

    data class TypeOption(val id: String, val icon: String)
    val types = listOf(
        TypeOption("feedback_bug", "🐛"),
        TypeOption("feedback_idea", "💡"),
        TypeOption("feedback_opinion", "💬")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp)
            }
            Spacer(Modifier.width(4.dp))
            Text(Lang.get("feedback_title"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Column {
            Text(Lang.get("feedback_type"), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                types.forEach { type ->
                    val selected = feedbackType == type.id
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        animationSpec = tween(250)
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(250)
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.05f else 1f,
                        animationSpec = tween(200)
                    )
                    Surface(
                        onClick = { feedbackType = type.id },
                        shape = RoundedCornerShape(14.dp),
                        color = bgColor,
                        tonalElevation = if (selected) 0.dp else 2.dp,
                        modifier = Modifier.weight(1f).graphicsLayer(
                            scaleX = scale, scaleY = scale,
                            shadowElevation = if (selected) 6f else 0f
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(type.icon, fontSize = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                Lang.get(type.id),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.get("feedback_subject"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(Lang.get("feedback_subject_hint")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Card(shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.get("feedback_desc"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(Lang.get("feedback_desc_hint")) },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Button(
            onClick = {
                val body = buildString {
                    appendLine("${Lang.get("feedback_type")}: ${Lang.get(feedbackType)}")
                    appendLine("${Lang.get("feedback_subject")}: $title")
                    appendLine()
                    appendLine(description)
                }
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:feedback@autoclicker.app")
                    putExtra(Intent.EXTRA_SUBJECT, "[${Lang.get(feedbackType)}] $title")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = title.isNotBlank() && description.isNotBlank()
        ) {
            Text(Lang.get("feedback_send"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 22.sp)
            }
            Spacer(Modifier.width(4.dp))
            Text(Lang.get("about_title"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(Lang.get("app_name"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    Lang.get("about_version"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    Lang.get("about_desc"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Lang.get("about_links"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SocialButton(
                        icon = "📸",
                        label = "Instagram",
                        url = "https://instagram.com/ferit22901",
                        context = context
                    )
                    SocialButton(
                        icon = "💻",
                        label = "GitHub",
                        url = "https://github.com/Memati8383",
                        context = context
                    )
                    SocialButton(
                        icon = "📧",
                        label = Lang.get("email"),
                        url = "mailto:akdemirferit608@gmail.com",
                        context = context
                    )
                    SocialButton(
                        icon = "📂",
                        label = Lang.get("source_code"),
                        url = "https://github.com/Memati8383/AutoClicker",
                        context = context
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SplashScreen() {
    val iconAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(600), label = "iconAlpha")
    val nameOffset by animateFloatAsState(targetValue = 0f, animationSpec = tween(600, delayMillis = 200), label = "nameOffset")
    val subAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(500, delayMillis = 700), label = "subAlpha")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(80.dp).graphicsLayer(alpha = iconAlpha)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                Lang.get("app_name"),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.offset(y = (20 * (1 - nameOffset)).dp).graphicsLayer(alpha = nameOffset)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Lang.get("app_subtitle"),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer(alpha = subAlpha)
            )
        }
    }
}

@Composable
private fun SocialButton(icon: String, label: String, url: String, context: Context) {
    Surface(
        onClick = {
            val intent = if (url.startsWith("mailto:")) {
                Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse(url) }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text("→", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(Lang.get("app_name"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                Lang.get("app_subtitle"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
