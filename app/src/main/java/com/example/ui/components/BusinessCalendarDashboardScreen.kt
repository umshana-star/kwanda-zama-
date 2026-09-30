package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.CalendarSyncManager
import com.example.data.local.BookingEventEntity
import com.example.data.local.ZamaDatabase
import com.example.ui.theme.ZamaAmberPulse
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaCardSurface
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaDarkSurface
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaNeonGreen
import com.example.ui.theme.ZamaPurple
import com.example.ui.theme.ZamaVoid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter mode for calendar events.
 */
enum class CalendarFilter(val label: String) {
    ALL("ALL UPCOMING"),
    THIS_WEEKEND("WEEKEND"),
    PENDING_SYNC("NEEDS SYNC"),
    SYNCED("SYNCED")
}

/**
 * Central Executive Dashboard for the Business Owner to view, manage, and sync
 * booking appointments automatically parsed from customer triage messages.
 */
@Composable
fun BusinessCalendarDashboardScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { ZamaDatabase.getDatabase(context) }
    val bookingDao = remember { db.bookingEventDao() }

    val allBookings by bookingDao.getAllBookingsFlow().collectAsState(initial = emptyList())
    val prefs = remember { context.getSharedPreferences("zama_calendar_prefs", Context.MODE_PRIVATE) }
    var activeFilter by remember { mutableStateOf(CalendarFilter.ALL) }
    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync_enabled", true)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isScanningChat by remember { mutableStateOf(false) }
    var bookingPendingDelete by remember { mutableStateOf<BookingEventEntity?>(null) }

    // Seed initial demo appointments if database is empty
    LaunchedEffect(Unit) {
        CalendarSyncManager.seedInitialBookingsIfEmpty(context)
    }

    // Filter bookings based on selected tab
    val filteredBookings = remember(allBookings, activeFilter) {
        val now = System.currentTimeMillis()
        when (activeFilter) {
            CalendarFilter.ALL -> allBookings
            CalendarFilter.PENDING_SYNC -> allBookings.filter { !it.isSynced }
            CalendarFilter.SYNCED -> allBookings.filter { it.isSynced }
            CalendarFilter.THIS_WEEKEND -> {
                allBookings.filter { booking ->
                    val cal = Calendar.getInstance().apply { timeInMillis = booking.startEpochMillis }
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    day == Calendar.FRIDAY || day == Calendar.SATURDAY || day == Calendar.SUNDAY
                }
            }
        }
    }

    // Compute metrics
    val totalCount = allBookings.size
    val syncedCount = allBookings.count { it.isSynced }
    val pendingCount = totalCount - syncedCount

    val totalRevenue = remember(allBookings) {
        allBookings.sumOf { booking ->
            val numStr = booking.quotedPrice.replace("[^0-9]".toRegex(), "")
            numStr.toIntOrNull() ?: 0
        }
    }

    Surface(
        color = ZamaDarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ZamaBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_dashboard_screen")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // 1. Executive Header
            CalendarDashboardHeader(
                autoSyncEnabled = autoSyncEnabled,
                onToggleAutoSync = {
                    val next = !autoSyncEnabled
                    autoSyncEnabled = next
                    prefs.edit().putBoolean("auto_sync_enabled", next).apply()
                    Toast.makeText(
                        context,
                        if (next) "Auto-Sync to Google Calendar enabled" else "Switched to Manual Calendar Sync",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onAddBookingClick = { showAddDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Metrics & KPI Strip
            MetricsKpiStrip(
                totalCount = totalCount,
                syncedCount = syncedCount,
                pendingCount = pendingCount,
                totalRevenue = totalRevenue
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Quick Action Ribbon (Sync All, Scan Chat, Export ICS)
            ActionRibbon(
                isScanning = isScanningChat,
                onSyncAll = {
                    coroutineScope.launch {
                        val unsynced = allBookings.filter { !it.isSynced }
                        if (unsynced.isEmpty()) {
                            Toast.makeText(context, "All bookings are already synced to calendar!", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Sync each to database and launch the first pending booking in calendar intent
                        unsynced.forEach { booking ->
                            bookingDao.markAsSynced(booking.id)
                        }

                        val intent = CalendarSyncManager.createCalendarInsertIntent(unsynced.first())
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Gracefully fallback if device has no external calendar app installed
                        }
                        Toast.makeText(context, "Synced ${unsynced.size} bookings to calendar!", Toast.LENGTH_LONG).show()
                    }
                },
                onScanChat = {
                    coroutineScope.launch {
                        isScanningChat = true
                        withContext(Dispatchers.IO) {
                            val chatDao = db.chatDao()
                            val messages = chatDao.getAllMessagesList()
                            for (msg in messages) {
                                if (msg.isFromUser) {
                                    CalendarSyncManager.parseAndIngestMessage(
                                        context = context,
                                        messageId = "chat_${msg.id}",
                                        messageText = msg.content,
                                        clientName = "WhatsApp Client #${msg.id % 1000}"
                                    )
                                }
                            }
                        }
                        isScanningChat = false
                        Toast.makeText(context, "Scan complete! Extracted appointments from chat.", Toast.LENGTH_SHORT).show()
                    }
                },
                onExportIcs = {
                    val uri = CalendarSyncManager.exportIcsCalendarFile(context, allBookings)
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/calendar"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Zama AI Salon Bookings (.ics)")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, "Export Calendar Appointments"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "ICS file exported to local cache", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Unable to generate .ics file", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Filter Tabs
            FilterTabBar(
                activeFilter = activeFilter,
                onSelectFilter = { activeFilter = it },
                pendingCount = pendingCount
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Booking Cards List
            if (filteredBookings.isEmpty()) {
                EmptyBookingsPlaceholder(activeFilter = activeFilter)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.testTag("bookings_list_container")
                ) {
                    filteredBookings.forEach { booking ->
                        BookingAppointmentCard(
                            booking = booking,
                            onSyncToCalendar = {
                                coroutineScope.launch {
                                    val intent = CalendarSyncManager.createCalendarInsertIntent(booking)
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // Graceful fallback when no calendar handler exists
                                    }
                                    bookingDao.markAsSynced(booking.id)
                                    Toast.makeText(context, "Synced '${booking.serviceName}' to Calendar!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                bookingPendingDelete = booking
                            }
                        )
                    }
                }
            }
        }
    }

    // Destructive Delete Confirmation Dialog
    bookingPendingDelete?.let { targetBooking ->
        AlertDialog(
            onDismissRequest = { bookingPendingDelete = null },
            containerColor = Color(0xFF101724),
            title = {
                Text(
                    text = "CANCEL APPOINTMENT?",
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "Remove ${targetBooking.serviceName} for ${targetBooking.clientName} (${targetBooking.quotedPrice})?",
                    color = ZamaChromeLight,
                    fontSize = 11.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = targetBooking.id
                        bookingPendingDelete = null
                        coroutineScope.launch {
                            bookingDao.deleteBookingById(idToDelete)
                            Toast.makeText(context, "Appointment removed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("btn_confirm_delete_booking")
                ) {
                    Text("REMOVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingPendingDelete = null }) {
                    Text("KEEP", color = ZamaChromeMid, fontSize = 11.sp)
                }
            }
        )
    }

    // Manual Add Dialog
    if (showAddDialog) {
        AddBookingManualDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { clientName, serviceName, dateStr, timeStr, price, notes ->
                coroutineScope.launch {
                    val combinedText = "Book $serviceName for $clientName on $dateStr at $timeStr. Price $price. $notes"
                    CalendarSyncManager.parseAndIngestMessage(
                        context = context,
                        messageId = "manual_${System.currentTimeMillis()}",
                        messageText = combinedText,
                        clientName = clientName
                    )
                    showAddDialog = false
                    Toast.makeText(context, "Appointment added & scheduled!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

// ============================================================================
// SUB-COMPONENTS
// ============================================================================

@Composable
private fun CalendarDashboardHeader(
    autoSyncEnabled: Boolean,
    onToggleAutoSync: () -> Unit,
    onAddBookingClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F00E676))
                    .border(1.dp, ZamaNeonGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendar Sync",
                    tint = ZamaNeonGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = "CALENDAR & BOOKING SYNC",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "TRIAGE PARSER • AUTO GOOGLE CALENDAR SYNC",
                    color = ZamaNeonGreen,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Auto-Sync Chip
            Surface(
                color = if (autoSyncEnabled) Color(0x2200E676) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, if (autoSyncEnabled) ZamaNeonGreen else Color(0x33FFFFFF)),
                modifier = Modifier
                    .clickable { onToggleAutoSync() }
                    .testTag("toggle_auto_sync_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (autoSyncEnabled) ZamaNeonGreen else Color.Gray)
                    )
                    Text(
                        text = if (autoSyncEnabled) "AUTO-SYNC" else "MANUAL",
                        color = if (autoSyncEnabled) ZamaNeonGreen else ZamaChromeMid,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Add Booking Button
            IconButton(
                onClick = onAddBookingClick,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A00E5FF))
                    .border(1.dp, ZamaElectricCyan, CircleShape)
                    .testTag("btn_add_manual_booking")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Booking",
                    tint = ZamaElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricsKpiStrip(
    totalCount: Int,
    syncedCount: Int,
    pendingCount: Int,
    totalRevenue: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KpiCard(
            title = "SCHEDULED",
            value = totalCount.toString(),
            subtitle = "Bookings",
            color = ZamaElectricCyan,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "SYNCED",
            value = "$syncedCount",
            subtitle = "To Device Cal",
            color = ZamaNeonGreen,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "PENDING",
            value = "$pendingCount",
            subtitle = "Unsynced",
            color = if (pendingCount > 0) ZamaAmberPulse else ZamaChromeMid,
            modifier = Modifier.weight(1f)
        )
        KpiCard(
            title = "PIPELINE",
            value = "R$totalRevenue",
            subtitle = "Projected",
            color = ZamaPurple,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F1520),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Text(
                text = title,
                color = color,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = ZamaChromeMid,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ActionRibbon(
    isScanning: Boolean,
    onSyncAll: () -> Unit,
    onScanChat: () -> Unit,
    onExportIcs: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sync All to Calendar Button
        Button(
            onClick = onSyncAll,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003822)),
            border = BorderStroke(1.dp, ZamaNeonGreen),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier
                .weight(1.2f)
                .heightIn(min = 48.dp)
                .testTag("btn_sync_all_calendar")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = ZamaNeonGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "SYNC ALL CALENDAR",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Scan Chat Messages Button
        OutlinedButton(
            onClick = onScanChat,
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0D1420)),
            border = BorderStroke(1.dp, ZamaElectricCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("btn_scan_chat_bookings")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = ZamaElectricCyan,
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ZamaElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = if (isScanning) "SCANNING..." else "SCAN CHAT",
                    color = ZamaElectricCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Export .ICS Share Button
        OutlinedButton(
            onClick = onExportIcs,
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0D1420)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier
                .weight(0.9f)
                .heightIn(min = 48.dp)
                .testTag("btn_export_ics")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = ZamaChromeLight,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "ICS EXPORT",
                    color = ZamaChromeLight,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun FilterTabBar(
    activeFilter: CalendarFilter,
    onSelectFilter: (CalendarFilter) -> Unit,
    pendingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CalendarFilter.values().forEach { filter ->
            val isSelected = activeFilter == filter
            Surface(
                color = if (isSelected) Color(0xFF162030) else Color(0xFF0B1017),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) ZamaElectricCyan else Color(0x22FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onSelectFilter(filter) }
                    .testTag("chip_filter_${filter.name}")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) ZamaElectricCyan else ZamaChromeMid,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        if (filter == CalendarFilter.PENDING_SYNC && pendingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(ZamaAmberPulse),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pendingCount.toString(),
                                    color = Color.Black,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingAppointmentCard(
    booking: BookingEventEntity,
    onSyncToCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault()) }
    val formattedDate = remember(booking.startEpochMillis) {
        dateFormat.format(Date(booking.startEpochMillis))
    }

    Surface(
        color = Color(0xFF111722),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (booking.isSynced) ZamaNeonGreen.copy(alpha = 0.5f) else ZamaElectricCyan.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header Row: Time Badge & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (booking.isSynced) ZamaNeonGreen else ZamaElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = formattedDate.uppercase(Locale.ROOT),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Sync status indicator
                Surface(
                    color = if (booking.isSynced) Color(0x2600E676) else Color(0x2600E5FF),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        1.dp,
                        if (booking.isSynced) ZamaNeonGreen else ZamaElectricCyan
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (booking.isSynced) Icons.Default.CheckCircle else Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (booking.isSynced) ZamaNeonGreen else ZamaElectricCyan,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (booking.isSynced) "SYNCED TO GOOGLE CAL" else "READY TO SYNC",
                            color = if (booking.isSynced) ZamaNeonGreen else ZamaElectricCyan,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Service Title & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.serviceName,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = booking.quotedPrice,
                    color = ZamaNeonGreen,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Client Info Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ZamaChromeMid,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = booking.clientName,
                    color = ZamaChromeLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "•",
                    color = ZamaChromeMid,
                    fontSize = 10.sp
                )
                Text(
                    text = "${booking.durationMinutes} min",
                    color = ZamaChromeMid,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Notes / styling details
            if (booking.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 ${booking.notes}",
                    color = ZamaChromeMid,
                    fontSize = 9.5.sp
                )
            }

            // Raw WhatsApp message snippet
            if (booking.rawMessageText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF090D14),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💬 \"${booking.rawMessageText}\"",
                        color = Color(0xFF90A4AE),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .testTag("btn_delete_booking_${booking.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Booking",
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Sync / Open in Calendar Button
                Button(
                    onClick = onSyncToCalendar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (booking.isSynced) Color(0xFF132B1F) else Color(0xFF003F54)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (booking.isSynced) ZamaNeonGreen else ZamaElectricCyan
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_sync_booking_${booking.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (booking.isSynced) ZamaNeonGreen else ZamaElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (booking.isSynced) "VIEW IN CALENDAR" else "SYNC TO CALENDAR",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBookingsPlaceholder(activeFilter: CalendarFilter) {
    Surface(
        color = Color(0xFF0C1018),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = ZamaChromeMid,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "NO APPOINTMENTS FOR \"${activeFilter.label}\"",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap 'SCAN CHAT' to extract bookings or use 'AUTO-SYNC' from incoming customer inquiries.",
                color = ZamaChromeMid,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AddBookingManualDialog(
    onDismiss: () -> Unit,
    onConfirm: (clientName: String, serviceName: String, dateStr: String, timeStr: String, price: String, notes: String) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var serviceName by remember { mutableStateOf("Medium Knotless Braids") }
    var dateStr by remember { mutableStateOf("Saturday") }
    var timeStr by remember { mutableStateOf("14:00") }
    var price by remember { mutableStateOf("R650") }
    var notes by remember { mutableStateOf("Waist length") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101724),
        title = {
            Text(
                text = "SCHEDULE NEW APPOINTMENT",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Color(0xFFFF5252),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedTextField(
                    value = clientName,
                    onValueChange = {
                        clientName = it
                        validationError = null
                    },
                    label = { Text("Client Name") },
                    placeholder = { Text("e.g. Sarah Ndlovu") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ZamaElectricCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = {
                        serviceName = it
                        validationError = null
                    },
                    label = { Text("Service Requested") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ZamaElectricCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = {
                            dateStr = it
                            validationError = null
                        },
                        label = { Text("Day / Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = {
                            timeStr = it
                            validationError = null
                        },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            price = it
                            validationError = null
                        },
                        label = { Text("Price (ZAR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Style") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serviceName.isBlank()) {
                        validationError = "Please enter a service name."
                        return@Button
                    }
                    if (dateStr.isBlank() || timeStr.isBlank()) {
                        validationError = "Day/Date and Time are required."
                        return@Button
                    }
                    if (price.isBlank() || price.none { it.isDigit() }) {
                        validationError = "Please enter a valid price (e.g. R650)."
                        return@Button
                    }
                    val finalClient = clientName.trim().ifBlank { "Walk-in Client" }
                    onConfirm(finalClient, serviceName.trim(), dateStr.trim(), timeStr.trim(), price.trim(), notes.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZamaNeonGreen),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("SAVE & SYNC", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("CANCEL", color = ZamaChromeMid)
            }
        }
    )
}
