package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioRecord
import com.example.data.EmergencyContact
import com.example.data.PoliceStation
import com.example.data.SafetyTip
import com.example.util.GpsCoordinates
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Colors matching our "Vibrant Palette" theme
val DeepCharcoal = Color(0xFFFEF7FF) // Main background (vibrant light tint)
val SurfaceDark = Color(0xFFFFFFFF)  // Card / surface background (pure white)
val SafetyCoral = Color(0xFF6750A4)  // Primary brand color (vibrant purple)
val SafetyAmber = Color(0xFFB3261E)  // Danger/Emergency accent (vibrant red)
val PoliceBlue = Color(0xFF21005D)   // Deep container text/header (deep purple/indigo)
val MintGreen = Color(0xFF2E7D32)    // Positive accent green (vibrant green)
val TextLight = Color(0xFF1D1B20)    // High contrast dark text
val TextMuted = Color(0xFF49454F)    // Soft medium body text

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KavalanApp(viewModel: KavalanViewModel) {
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.SEND_SMS",
            "android.permission.CALL_PHONE",
            "android.permission.RECORD_AUDIO"
        )
    )

    // Current screen index
    var selectedTab by remember { mutableStateOf(0) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    // Read values from ViewModel state
    val contacts by viewModel.allContacts.collectAsState()
    val tips by viewModel.allTips.collectAsState()
    val records by viewModel.allRecords.collectAsState()
    val stations by viewModel.allStations.collectAsState()
    val gpsLoc by viewModel.gpsLocation.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsState()
    val sosMsgStatus by viewModel.sosStatus.collectAsState()
    val playingRecId by viewModel.playingRecordId.collectAsState()

    // Automatic dismissal of SOS status notification after delay
    LaunchedEffect(sosMsgStatus) {
        if (sosMsgStatus != null) {
            delay(8000)
            viewModel.sosStatus.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEADDFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = "Shield Logo",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "KAVALAN SOS",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "காவலன் • TN Women Safety",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SafetyCoral
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshLocation() }) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = "Fetch GPS",
                            tint = TextLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepCharcoal,
                    titleContentColor = TextLight
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Emergency, contentDescription = "SOS") },
                    label = { Text("SOS Dashboard", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D1B20),
                        selectedTextColor = Color(0xFF1D1B20),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.People, contentDescription = "Guardians") },
                    label = { Text("Guardians", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D1B20),
                        selectedTextColor = Color(0xFF1D1B20),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI Assistant", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D1B20),
                        selectedTextColor = Color(0xFF1D1B20),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Outlined.LocalPolice, contentDescription = "Stations") },
                    label = { Text("Stations", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D1B20),
                        selectedTextColor = Color(0xFF1D1B20),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = "Safety Tips") },
                    label = { Text("Defense Tips", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1D1B20),
                        selectedTextColor = Color(0xFF1D1B20),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )
            }
        },
        containerColor = DeepCharcoal
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views routing
            when (selectedTab) {
                0 -> SosDashboardView(
                    viewModel = viewModel,
                    permissionStateGranted = permissionState.allPermissionsGranted,
                    onRequestPermissions = { permissionState.launchMultiplePermissionRequest() },
                    gpsLoc = gpsLoc,
                    isRecording = isRecording,
                    isSirenPlaying = isSirenPlaying,
                    records = records,
                    playingRecId = playingRecId,
                    onTipsClick = { selectedTab = 4 }
                )
                1 -> GuardiansView(
                    contacts = contacts,
                    onAddContactClick = { showAddContactDialog = true },
                    onRemoveContact = { viewModel.removeContact(it) }
                )
                2 -> AiAssistantView(
                    viewModel = viewModel
                )
                3 -> StationsDirectoryView(
                    stations = stations,
                    context = context,
                    gpsLoc = gpsLoc
                )
                4 -> DefenseTipsView(
                    tips = tips
                )
            }

            // Floated Alert status banner
            sosMsgStatus?.let { status ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(SafetyCoral, RoundedCornerShape(12.dp))
                        .align(Alignment.TopCenter)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = status,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { viewModel.sosStatus.value = null }
                        )
                    }
                }
            }

            // Dialog for adding Emergency Guardian Contact
            if (showAddContactDialog) {
                AddContactDialog(
                    onDismiss = { showAddContactDialog = false },
                    onConfirm = { name, phone, rel ->
                        viewModel.addContact(name, phone, rel)
                        showAddContactDialog = false
                        Toast.makeText(context, "Guardian Added Successfully!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

private fun addSimExtras(intent: Intent, simSlot: Int) {
    if (simSlot == 1 || simSlot == 2) {
        val slotIndex = simSlot - 1 // 0 for SIM 1, 1 for SIM 2
        intent.putExtra("com.android.phone.extra.slot", slotIndex)
        intent.putExtra("com.android.phone.extra.slot_id", slotIndex)
        intent.putExtra("simSlot", slotIndex)
        intent.putExtra("phone", slotIndex)
        intent.putExtra("subscription", slotIndex.toLong())
        intent.putExtra("android.telephony.extra.SLOT_INDEX", slotIndex)
    }
}

@Composable
fun SosDashboardView(
    viewModel: KavalanViewModel,
    permissionStateGranted: Boolean,
    onRequestPermissions: () -> Unit,
    gpsLoc: GpsCoordinates?,
    isRecording: Boolean,
    isSirenPlaying: Boolean,
    records: List<AudioRecord>,
    playingRecId: Int?,
    onTipsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferredSimSlot by viewModel.preferredSimSlot.collectAsState()
    val sirenSoundType by viewModel.sirenSoundType.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Quick Safety Status Badge (Live Tracking Capsule style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (permissionStateGranted) Color(0xFFE8DEF8) else Color(0xFFF9DEDC))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Location",
                            tint = if (permissionStateGranted) Color(0xFF21005D) else Color(0xFFB3261E),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (permissionStateGranted) {
                                if (gpsLoc != null) {
                                    String.format(Locale.US, "GPS: %.4f, %.4f • Live Tracking", gpsLoc.latitude, gpsLoc.longitude)
                                } else {
                                    "Anna Salai, Chennai • Live Tracking"
                                }
                            } else {
                                "Setup Required (Permissions)"
                            },
                            color = if (permissionStateGranted) Color(0xFF21005D) else Color(0xFFB3261E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    if (!permissionStateGranted) {
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Grant", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: PULSATING SOS TRIGGER BUTTON
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background pulse circles (Dual layered depth from mock)
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale1 by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse1"
                )
                val pulseAlpha1 by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseAlpha1"
                )

                // Large outer pulse ring (20% base opacity)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale1)
                        .clip(CircleShape)
                        .background(Color(0xFFF2B8B5).copy(alpha = 0.2f * pulseAlpha1))
                )

                // Medium inner pulse ring (40% base opacity)
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale1 * 0.85f)
                        .clip(CircleShape)
                        .background(Color(0xFFF2B8B5).copy(alpha = 0.4f * pulseAlpha1))
                )

                // Actual SOS button (Vibrant safety red with 8dp soft container border)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3261E))
                        .border(8.dp, Color(0xFFF9DEDC), CircleShape)
                        .clickable {
                            if (!permissionStateGranted) {
                                onRequestPermissions()
                                Toast
                                    .makeText(
                                        context,
                                        "Please grant permission to send SMS, Location & Phone Call!",
                                        Toast.LENGTH_LONG
                                    )
                                    .show()
                            } else {
                                viewModel.triggerSosAlert { targetPhone ->
                                    // Trigger call intent
                                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                                        data = Uri.parse("tel:$targetPhone")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    addSimExtras(callIntent, preferredSimSlot)
                                    try {
                                        context.startActivity(callIntent)
                                    } catch (e: SecurityException) {
                                        // Fallback to dialer if direct CALL_PHONE fails
                                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$targetPhone")
                                        }
                                        addSimExtras(dialIntent, preferredSimSlot)
                                        context.startActivity(dialIntent)
                                    }
                                }
                            }
                        }
                        .testTag("sos_trigger_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Emergency,
                            contentDescription = "SOS",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "TRIGGER ALERT",
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Section: Siren & Recording Tools Row (Vibrant Palette Cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Siren Toggle Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(105.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSirenPlaying) Color(0xFFD3E3FD) else Color.White)
                        .border(
                            1.dp,
                            if (isSirenPlaying) Color(0xFF041E49) else Color(0xFFCAC4D0),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.toggleSiren() }
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFD3E3FD), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSirenPlaying) Icons.Filled.Campaign else Icons.Outlined.Campaign,
                                contentDescription = "Siren",
                                tint = Color(0xFF041E49),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Siren Mode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Text(
                                text = if (isSirenPlaying) "Siren On" else "Play Siren",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }
                }

                // Voice evidence recorder toggle card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(105.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isRecording) Color(0xFFF9DEDC) else Color.White)
                        .border(
                            1.dp,
                            if (isRecording) Color(0xFFB3261E) else Color(0xFFCAC4D0),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            if (!permissionStateGranted) {
                                onRequestPermissions()
                            } else {
                                if (isRecording) {
                                    viewModel.stopVoiceRecording()
                                } else {
                                    viewModel.startVoiceRecording()
                                }
                            }
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFD8E4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Filled.Mic else Icons.Outlined.Mic,
                                contentDescription = "Recorder",
                                tint = Color(0xFF31111D),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Evidence Log",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F)
                            )
                            Text(
                                text = if (isRecording) "Recording..." else "Record Voice",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }
                }
            }
        }

        // Section: LIVE REAL-TIME LOCATION TRACKER CARD
        item {
            val isLiveTracking by viewModel.isLiveTracking.collectAsState()
            val trackingPath by viewModel.liveTrackingPath.collectAsState()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isLiveTracking) Color(0xFFE8DEF8) else SurfaceDark, RoundedCornerShape(20.dp))
                    .border(
                        1.dp, 
                        if (isLiveTracking) SafetyCoral else Color(0xFFCAC4D0), 
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isLiveTracking) SafetyCoral.copy(alpha = 0.15f) else Color(0xFFEADDFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLiveTracking) Icons.Filled.MyLocation else Icons.Outlined.MyLocation,
                                contentDescription = "Live GPS Tracker",
                                tint = if (isLiveTracking) SafetyCoral else TextLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "REAL-TIME TRACKING",
                                color = if (isLiveTracking) SafetyCoral else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isLiveTracking) "Active Live Broadcast" else "Live Location Tracker",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = TextLight
                            )
                        }
                    }
                    
                    // Switch / Button toggle
                    Button(
                        onClick = { viewModel.toggleLiveTracking() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLiveTracking) SafetyAmber else SafetyCoral
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (isLiveTracking) "STOP" else "START",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isLiveTracking) {
                    // Pulsating indicators and list of paths
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom blinking live indicator
                        var tick by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(700)
                                tick = !tick
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (tick) SafetyAmber else Color.Transparent)
                        )
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Text(
                            text = "Sending real-time GPS coords to Guardians every 10s...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    }
                    
                    if (trackingPath.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "RECENT PATH LOGS (REAL-TIME):",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            trackingPath.takeLast(4).reversed().forEachIndexed { index, pos ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TripOrigin,
                                        contentDescription = null,
                                        tint = if (index == 0) SafetyCoral else TextMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format(Locale.US, "Point %d: %.5f, %.5f (Live Node)", trackingPath.size - index, pos.latitude, pos.longitude),
                                        fontSize = 11.sp,
                                        color = if (index == 0) TextLight else TextMuted,
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Turn this on to stream continuous real-time coordinates to family and police dispatch during transit.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // Section: Dialer & Sound Options Card (SIM Options and Buzzer/Siren Choice)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "EMERGENCY CALL & SOUND OPTIONS",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Preferred SIM Card
                Text(
                    text = "Preferred SIM Card for Calls",
                    color = TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val simOptions = listOf("Default", "SIM 1", "SIM 2")
                    simOptions.forEachIndexed { index, option ->
                        val isSelected = preferredSimSlot == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SafetyCoral else Color.White)
                                .border(1.dp, if (isSelected) SafetyCoral else Color(0xFFCAC4D0), RoundedCornerShape(10.dp))
                                .clickable { viewModel.preferredSimSlot.value = index }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SimCard,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF49454F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = option,
                                    color = if (isSelected) Color.White else Color(0xFF1D1B20),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFCAC4D0).copy(alpha = 0.4f))
                        .padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Siren Sound style
                Text(
                    text = "Siren Sound style",
                    color = TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val soundOptions = listOf("Buzzer & Siren", "Industrial Buzzer", "Police Siren")
                    soundOptions.forEach { option ->
                        val isSelected = sirenSoundType == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF6750A4) else Color.White)
                                .border(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0), RoundedCornerShape(10.dp))
                                .clickable { viewModel.sirenSoundType.value = option }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) Color.White else Color(0xFF1D1B20),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Section: Call Emergency Directly Badges
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "DIRECT HELPLINES (TAMIL NADU)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Women Police Helpline 1091
                    Button(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:1091")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            addSimExtras(callIntent, preferredSimSlot)
                            try {
                                context.startActivity(callIntent)
                            } catch (e: SecurityException) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1091"))
                                addSimExtras(dialIntent, preferredSimSlot)
                                context.startActivity(dialIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PoliceBlue),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Women Helpline 1091", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Police Control 112 (Outlined style matching mockup secondary)
                    Button(
                        onClick = {
                            val callIntent = Intent(Intent.ACTION_CALL).apply {
                                data = Uri.parse("tel:112")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            addSimExtras(callIntent, preferredSimSlot)
                            try {
                                context.startActivity(callIntent)
                            } catch (e: SecurityException) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                addSimExtras(dialIntent, preferredSimSlot)
                                context.startActivity(dialIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                    ) {
                        Icon(Icons.Filled.PhoneInTalk, contentDescription = null, tint = TextLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Emergency 112", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Real-time GPS Location Status Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Location",
                                tint = SafetyCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GPS TRACE COORDINATES",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.refreshLocation()
                                Toast.makeText(context, "Location refreshed!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = TextLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (gpsLoc != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LATITUDE", fontSize = 9.sp, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%.5f", gpsLoc.latitude),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LONGITUDE", fontSize = 9.sp, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%.5f", gpsLoc.longitude),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Emergency Coordinates: https://maps.google.com/?q=${gpsLoc.latitude},${gpsLoc.longitude}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Emergency Location"))
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share Coordinates", tint = TextLight)
                            }
                        }
                    } else {
                        Text(
                            text = "Searching satellite lock... Make sure GPS Location is enabled in phone settings.",
                            color = SafetyAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section: Secure Voice Records List (Saved Evidence Logs)
        if (records.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "History",
                        tint = SafetyCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURED AUDIO EVIDENCE LOGS (${records.size})",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            items(records) { record ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.playAudioRecord(record) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SafetyCoral.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (playingRecId == record.id) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Stop",
                                    tint = SafetyCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = record.fileName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = TextLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Duration: ${record.durationSeconds}s | ${
                                        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                    }",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.deleteAudioRecord(record) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete Recording",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section: Daily Technique Banner (Vibrant Palette Mockup)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E7F3)),
                border = BorderStroke(1.dp, Color(0xFFEADDFF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTipsClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAILY TECHNIQUE",
                            color = Color(0xFF6750A4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Palm strike: Aim for the nose using the heel of your palm for maximum impact.",
                            color = Color(0xFF49454F),
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Details",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun GuardiansView(
    contacts: List<EmergencyContact>,
    onAddContactClick: () -> Unit,
    onRemoveContact: (EmergencyContact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Guardians",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = TextLight
                )
                Text(
                    text = "These contacts receive instant GPS coordinates via SMS during SOS.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Button(
                onClick = onAddContactClick,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyCoral),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Guardian", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ShieldMoon,
                        contentDescription = "Empty",
                        tint = TextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Guardians Configured Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextLight
                    )
                    Text(
                        text = "Add family members, trusted friends, or guardians.\nIn critical danger, the app sends them SMS alerts instantly.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(contacts) { contact ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SafetyCoral.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(Locale.ROOT),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = SafetyCoral
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextLight
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = contact.relationship.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SafetyAmber,
                                            modifier = Modifier
                                                .background(SafetyAmber.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = contact.phoneNumber,
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onRemoveContact(contact) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Remove Contact",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

@Composable
fun StationsDirectoryView(
    stations: List<PoliceStation>,
    context: Context,
    gpsLoc: GpsCoordinates?
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("All") }

    val districts = listOf("All", "Chennai", "Coimbatore", "Madurai", "Trichy")

    // Calculate distances if GPS is available
    val stationsWithDistance = remember(stations, gpsLoc) {
        stations.map { station ->
            val distance = if (gpsLoc != null) {
                calculateDistance(gpsLoc.latitude, gpsLoc.longitude, station.latitude, station.longitude)
            } else {
                null
            }
            station to distance
        }
    }

    // Filter list based on search, district, and sort by proximity (nearest first) if GPS is available
    val filteredStations = remember(stationsWithDistance, searchQuery, selectedDistrict) {
        stationsWithDistance.filter { (station, _) ->
            val matchesSearch = station.name.contains(searchQuery, ignoreCase = true) ||
                    station.address.contains(searchQuery, ignoreCase = true)
            val matchesDistrict = selectedDistrict == "All" || station.district.equals(selectedDistrict, ignoreCase = true)
            matchesSearch && matchesDistrict
        }.sortedWith { a, b ->
            val distA = a.second
            val distB = b.second
            if (distA != null && distB != null) {
                distA.compareTo(distB)
            } else {
                a.first.name.compareTo(b.first.name)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AWPS Police Directory",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = TextLight
        )
        Text(
            text = "Tamil Nadu All Women Police Stations (AWPS) quick contacts & navigation.",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search police stations...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = SafetyCoral,
                unfocusedBorderColor = Color(0xFFCAC4D0)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // District Quick Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(districts) { district ->
                val isSelected = selectedDistrict == district
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) SafetyCoral else SurfaceDark)
                        .clickable { selectedDistrict = district }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = district,
                        color = if (isSelected) Color.White else TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredStations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No police stations match your query.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredStations) { pair ->
                    val station = pair.first
                    val distance = pair.second
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = station.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextLight
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "DISTRICT: ${station.district.uppercase()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SafetyCoral,
                                            letterSpacing = 0.5.sp
                                        )
                                        if (distance != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "•",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextMuted
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.1f KM AWAY", distance),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MintGreen,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PoliceBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalPolice,
                                        contentDescription = null,
                                        tint = SafetyCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = station.address,
                                fontSize = 12.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Call Button
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${station.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PoliceBlue.copy(alpha = 0.8f)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call AWPS Station", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Directions Button
                                Button(
                                    onClick = {
                                        val mapIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("geo:${station.latitude},${station.longitude}?q=${Uri.encode(station.name)}")
                                        )
                                        context.startActivity(mapIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Navigate Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefenseTipsView(tips: List<SafetyTip>) {
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Tactical Manual, 1 = Tamil Video Library
    var expandedTipId by remember { mutableStateOf<Int?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }
    val context = LocalContext.current

    val categories = listOf("All", "Physical Defense", "Digital Safety", "Emergency Action", "Rights & Law")

    val filteredTips = tips.filter { tip ->
        selectedCategory == "All" || tip.category.equals(selectedCategory, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Self Defense & Techniques",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = TextLight
        )
        Text(
            text = "Tactical instructions to defend yourself and understand women's rights in India.",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Sub-Tabs Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(Color(0xFFEADDFF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeSubTab == 0) SafetyCoral else Color.Transparent)
                    .clickable { activeSubTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tactical Manual",
                    color = if (activeSubTab == 0) Color.White else TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activeSubTab == 1) SafetyCoral else Color.Transparent)
                    .clickable { activeSubTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tamil Video Library",
                    color = if (activeSubTab == 1) Color.White else TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        if (activeSubTab == 0) {
            // Horizontal Category Row Filters
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) SafetyCoral else SurfaceDark)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) DeepCharcoal else TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTips) { tip ->
                    val isExpanded = expandedTipId == tip.id

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedTipId = if (isExpanded) null else tip.id
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tip.category.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 9.sp,
                                        color = SafetyCoral,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = tip.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextLight
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Expand/Collapse",
                                    tint = TextMuted
                                )
                            }

                            // Detailed expanded content
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = TextMuted.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = tip.content,
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "TACTICAL STEPS / ADVICE:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyAmber,
                                    letterSpacing = 0.5.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Steps breakdown
                                val stepsList = tip.steps.split(";")
                                stepsList.forEachIndexed { index, step ->
                                    if (step.isNotBlank()) {
                                        val parts = step.split(":", limit = 2)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(16.dp)
                                                    .background(SafetyCoral.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = (index + 1).toString(),
                                                    color = SafetyCoral,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                if (parts.size == 2) {
                                                    Text(
                                                        text = parts[0].trim(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextLight
                                                    )
                                                    Text(
                                                        text = parts[1].trim(),
                                                        fontSize = 12.sp,
                                                        color = TextMuted
                                                    )
                                                } else {
                                                    Text(
                                                        text = step.trim(),
                                                        fontSize = 13.sp,
                                                        color = TextLight
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tamil Defense Videos section
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tamilDefenseVideos) { video ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Cinematic visual thumbnail placeholder with play button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF31111D), SafetyAmber.copy(alpha = 0.85f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircleFilled,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "வீடியோவைப் பார்க்கவும் (WATCH)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                
                                // Duration tag
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = video.duration,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = video.tamilTitle,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextLight
                            )
                            Text(
                                text = video.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SafetyCoral
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = video.tamilDesc,
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = video.description,
                                fontSize = 11.sp,
                                color = TextMuted.copy(alpha = 0.8f),
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyCoral),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("பார்க்க / Watch Video", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, relationship: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") }

    val relationships = listOf("Family", "Friend", "Spouse", "Colleague", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Emergency Guardian",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Register a trusted person who will receive SOS text alerts.",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "RELATIONSHIP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(relationships) { rel ->
                        val isSelected = relationship == rel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SafetyCoral else SurfaceDark)
                                .clickable { relationship = rel }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = rel,
                                color = if (isSelected) DeepCharcoal else TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, relationship)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyCoral)
            ) {
                Text("Save Guardian", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

// Data models and assets for the Tamil Defense Video library
data class DefenseVideo(
    val title: String,
    val tamilTitle: String,
    val description: String,
    val tamilDesc: String,
    val duration: String,
    val videoId: String
)

val tamilDefenseVideos = listOf(
    DefenseVideo(
        title = "Easy Self Defense for Women",
        tamilTitle = "பெண்களுக்கான எளிய தற்காப்பு முறைகள்",
        description = "Simple physical moves to escape sudden grabs, punches, or hostile holds.",
        tamilDesc = "திடீர் தாக்குதல்கள் மற்றும் பிடிகளில் இருந்து தப்பிக்க எளிய தற்காப்பு வழிகள்.",
        duration = "8:25 mins",
        videoId = "A2G5D8zX0Fk"
    ),
    DefenseVideo(
        title = "Escape from Wrist Grabs & Neck Locks",
        tamilTitle = "கைப்பிடியில் இருந்து தப்பிப்பது எப்படி?",
        description = "Effective leverage tricks to break out of tight wrist, arm, and hair locks.",
        tamilDesc = "எதிரி கைகளை அல்லது கழுத்தை பிடிக்கும் போது நொடியில் தப்பிக்கும் உத்திகள்.",
        duration = "5:40 mins",
        videoId = "Kz6lS9j1L78"
    ),
    DefenseVideo(
        title = "Kavalan Safety App & Awareness Guide",
        tamilTitle = "காவலன் செயலி மற்றும் தற்காப்பு விழிப்புணர்வு",
        description = "Tamil Nadu police official recommendations and emergency response guide.",
        tamilDesc = "தமிழ்நாடு காவல்துறையின் காவலன் செயலி பயன்பாடு மற்றும் அவசர கால வழிகாட்டி.",
        duration = "10:15 mins",
        videoId = "F0S_7E_j6U0"
    ),
    DefenseVideo(
        title = "Using Umbrella and Everyday Objects",
        tamilTitle = "அன்றாட பொருட்கள் தற்காப்பு முறைகள்",
        description = "How to transform keychains, umbrellas, or pens into tactical safety items.",
        tamilDesc = "குடை, பேனா, சாவிக்கொத்து போன்ற அன்றாட பொருட்களை தற்காப்பு ஆயுதமாக மாற்றுவது.",
        duration = "6:50 mins",
        videoId = "UqQY9318q0U"
    )
)

@Composable
fun AiAssistantView(viewModel: KavalanViewModel) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val aiRiskLevel by viewModel.aiRiskLevel.collectAsState()
    val aiGuidance by viewModel.aiGuidance.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val context = LocalContext.current

    var scenarioText by remember { mutableStateOf("") }

    val quickScenarios = listOf(
        "Someone is following me down a dark alley in Chennai, I'm very scared.",
        "The auto driver took a wrong turn into an isolated field and locked the doors.",
        "A stranger is staring aggressively on the bus and moving closer to me.",
        "I feel slightly uneasy walking back home alone in Coimbatore."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Kavalan AI Guard",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = TextLight
        )
        Text(
            text = "AI-powered real-time incident detector & situational danger assessment.",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DESCRIBE WHAT IS HAPPENING:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafetyCoral,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = scenarioText,
                    onValueChange = { scenarioText = it },
                    placeholder = { Text("Example: A stranger is following me on Kutchery Road, Mylapore...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines = 4,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.assessSituation(scenarioText)
                    },
                    enabled = scenarioText.isNotBlank() && !isAiLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyCoral),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detecting Threats...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze Situation & Detect Risk", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick assess suggestions
        Text(
            text = "QUICK ASSESS PRESETS:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickScenarios) { scenario ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEADDFF).copy(alpha = 0.4f))
                        .clickable {
                            scenarioText = scenario
                            viewModel.assessSituation(scenario)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 200.dp)
                ) {
                    Text(
                        text = scenario,
                        fontSize = 11.sp,
                        color = TextLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Assessment Result Screen
        if (aiResponse != null || aiGuidance != null) {
            val riskColor = when (aiRiskLevel) {
                "CRITICAL" -> Color(0xFFB3261E)
                "HIGH" -> Color(0xFFC33D12)
                "MEDIUM" -> Color(0xFFE65100)
                else -> Color(0xFF2E7D32)
            }
            
            val riskBg = when (aiRiskLevel) {
                "CRITICAL" -> Color(0xFFF9DEDC)
                "HIGH" -> Color(0xFFFBE9E7)
                "MEDIUM" -> Color(0xFFFFF3E0)
                else -> Color(0xFFE8F5E9)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = riskBg),
                border = BorderStroke(1.5.dp, riskColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "INCIDENT DETECTION ANALYSIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = riskColor,
                                letterSpacing = 0.5.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(riskColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = aiRiskLevel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Text(
                            text = aiResponse ?: "",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            lineHeight = 18.sp
                        )
                    }

                    if (aiGuidance != null) {
                        item {
                            Divider(color = riskColor.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = riskColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI RESCUE INSTRUCTIONS:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = riskColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiGuidance ?: "",
                                fontSize = 13.sp,
                                color = TextMuted,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Dangerous situation -> offer immediate Auto-Trigger SOS or Sirens
                    if (aiRiskLevel == "HIGH" || aiRiskLevel == "CRITICAL") {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.triggerSosAlert { targetPhone ->
                                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                                            data = Uri.parse("tel:$targetPhone")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(callIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AUTO-TRIGGER RESCUE SOS NOW", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        } else {
            // Initial/Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "AI Waiting",
                        tint = TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AI Incident Detector",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextLight
                    )
                    Text(
                        text = "Type or select an incident scenario above. Gemini will instantly evaluate danger levels and prescribe step-by-step rescue plans.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
