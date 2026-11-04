package com.scann.thekami.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.scann.thekami.model.ScanAction
import com.scann.thekami.model.ScanResult
import com.scann.thekami.model.ScanResultType

private val CardContentPadding = 16.dp
private val CardRadius = 16.dp
private val ActionButtonRadius = 18.dp
private val TypeIconTileSize = 44.dp
private val TypeIconTileRadius = 12.dp

private data class TypeInfo(val label: String, val icon: ImageVector)

private fun ScanResultType.info(): TypeInfo = when (this) {
    is ScanResultType.Url -> TypeInfo("Website Link", Icons.Filled.Language)
    is ScanResultType.PlainText -> TypeInfo("Text", Icons.Filled.TextFields)
    is ScanResultType.Phone -> TypeInfo("Phone Number", Icons.Filled.Call)
    is ScanResultType.Email -> TypeInfo("Email", Icons.Filled.Email)
    is ScanResultType.Sms -> TypeInfo("SMS Message", Icons.Filled.Sms)
    is ScanResultType.Location -> TypeInfo("Location", Icons.Filled.Place)
    is ScanResultType.WifiNetwork -> TypeInfo("Wi-Fi Network", Icons.Filled.Wifi)
    is ScanResultType.Contact -> TypeInfo("Contact", Icons.Filled.Person)
    is ScanResultType.CalendarEvent -> TypeInfo("Calendar Event", Icons.Filled.Event)
    is ScanResultType.Unknown -> TypeInfo("Unknown", Icons.Filled.QrCode2)
}

// (label, value) pairs; a null label means the value is shown on its own
private fun ScanResultType.fields(): List<Pair<String?, String>> = buildList {
    when (val type = this@fields) {
        is ScanResultType.Url -> add(null to type.value)
        is ScanResultType.PlainText -> add(null to type.value)
        is ScanResultType.Phone -> add(null to type.value)
        is ScanResultType.Email -> {
            add("To" to type.address)
            if (!type.subject.isNullOrBlank()) add("Subject" to type.subject)
            if (!type.body.isNullOrBlank()) add("Message" to type.body)
        }
        is ScanResultType.Sms -> {
            add("To" to type.phoneNumber)
            if (!type.message.isNullOrBlank()) add("Message" to type.message)
        }
        is ScanResultType.Location -> {
            add("Latitude" to type.latitude.toString())
            add("Longitude" to type.longitude.toString())
        }
        is ScanResultType.WifiNetwork -> {
            add("Network" to type.ssid)
            add("Security" to (type.encryptionType ?: "Open (no password)"))
            if (!type.password.isNullOrBlank()) add("Password" to type.password)
        }
        is ScanResultType.Contact -> {
            type.name?.let { add("Name" to it) }
            type.phone?.let { add("Phone" to it) }
            type.email?.let { add("Email" to it) }
            type.organization?.let { add("Organization" to it) }
        }
        is ScanResultType.CalendarEvent -> {
            type.title?.let { add("Title" to it) }
            type.location?.let { add("Location" to it) }
            type.description?.let { add("Description" to it) }
        }
        is ScanResultType.Unknown -> add(null to type.raw.ifBlank { "No data found" })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    scanResult: ScanResult,
    onBack: () -> Unit,
    onAction: (ScanAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ResultHeader(scanResult.type.info())

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CardRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CardContentPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        scanResult.type.fields().forEach { (label, value) ->
                            ResultField(label, value)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ResultActions(scanResult, onAction)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Format: ${scanResult.format.replace('_', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultHeader(info: TypeInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(TypeIconTileRadius),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(TypeIconTileSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = info.label, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun ResultField(label: String?, value: String) {
    Column {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultActions(scanResult: ScanResult, onAction: (ScanAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (val type = scanResult.type) {
            is ScanResultType.Url -> {
                ActionButton("Open Link", Icons.Filled.OpenInBrowser) {
                    onAction(ScanAction.OpenUrl(type.value))
                }
                ActionButton("Copy Link", Icons.Filled.ContentCopy, filled = false) {
                    onAction(ScanAction.CopyText(type.value))
                }
            }
            is ScanResultType.PlainText -> {
                ActionButton("Copy Text", Icons.Filled.ContentCopy) {
                    onAction(ScanAction.CopyText(type.value))
                }
                ActionButton("Share", Icons.Filled.Share, filled = false) {
                    onAction(ScanAction.ShareText(type.value))
                }
            }
            is ScanResultType.Phone -> {
                ActionButton("Call", Icons.Filled.Call) {
                    onAction(ScanAction.CallPhone(type.value))
                }
                ActionButton("Copy Number", Icons.Filled.ContentCopy, filled = false) {
                    onAction(ScanAction.CopyText(type.value))
                }
            }
            is ScanResultType.Email -> {
                ActionButton("Send Email", Icons.Filled.Email) {
                    onAction(ScanAction.SendEmail(type.address, type.subject, type.body))
                }
                ActionButton("Copy Email", Icons.Filled.ContentCopy, filled = false) {
                    onAction(ScanAction.CopyText(type.address))
                }
            }
            is ScanResultType.Sms -> {
                ActionButton("Send SMS", Icons.Filled.Send) {
                    onAction(ScanAction.SendSms(type.phoneNumber, type.message))
                }
            }
            is ScanResultType.Location -> {
                ActionButton("Open in Maps", Icons.Filled.Map) {
                    onAction(ScanAction.OpenLocation(type.latitude, type.longitude))
                }
            }
            is ScanResultType.WifiNetwork -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ActionButton("Connect to Wi-Fi", Icons.Filled.Wifi) {
                        onAction(ScanAction.ConnectWifi(type.ssid, type.password, type.encryptionType))
                    }
                    type.password?.let {
                        ActionButton("Copy Password", Icons.Filled.ContentCopy, filled = false) {
                            onAction(ScanAction.CopyText(it))
                        }
                    }
                } else {
                    val password = type.password?.takeIf { it.isNotBlank() }
                    ActionButton(
                        if (password != null) "Copy Wi-Fi password" else "Copy network name",
                        Icons.Filled.ContentCopy
                    ) {
                        onAction(ScanAction.CopyText(password ?: type.ssid))
                    }
                }
            }
            is ScanResultType.Contact -> {
                ActionButton("Add to Contacts", Icons.Filled.PersonAdd) {
                    onAction(ScanAction.AddContact(type.name, type.phone, type.email, type.organization))
                }
            }
            is ScanResultType.CalendarEvent -> {
                ActionButton("Add to Calendar", Icons.Filled.Event) {
                    onAction(
                        ScanAction.AddCalendarEvent(
                            type.title, type.description, type.location,
                            type.startTimeMillis, type.endTimeMillis, type.isAllDay
                        )
                    )
                }
            }
            is ScanResultType.Unknown -> {
                ActionButton("Copy Raw Data", Icons.Filled.ContentCopy) {
                    onAction(ScanAction.CopyText(scanResult.rawValue))
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    filled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(ActionButtonRadius)
    val modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }

    if (filled) {
        Button(onClick = onClick, shape = shape, modifier = modifier, content = content)
    } else {
        OutlinedButton(onClick = onClick, shape = shape, modifier = modifier, content = content)
    }
}
