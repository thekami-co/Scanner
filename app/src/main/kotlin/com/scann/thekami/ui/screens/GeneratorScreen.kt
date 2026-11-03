package com.scann.thekami.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.scann.thekami.model.QrContentEncoder
import com.scann.thekami.model.QrContentType
import com.scann.thekami.ui.components.bottomNavClearance
import com.scann.thekami.util.QrCodeGenerator

private val FieldRadius = 16.dp
private val WifiSecurityOptions = listOf("WPA/WPA2", "WEP", "None")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen() {
    val focusManager = LocalFocusManager.current
    var selectedType by remember { mutableStateOf(QrContentType.TEXT) }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Form fields
    var textValue by remember { mutableStateOf("") }
    var urlValue by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf(WifiSecurityOptions.first()) }
    var wifiHidden by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }
    var phoneValue by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var smsNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }

    fun buildEncodedContent(): String {
        return when (selectedType) {
            QrContentType.TEXT -> QrContentEncoder.encodeText(textValue)
            QrContentType.URL -> if (urlValue.isNotBlank()) QrContentEncoder.encodeUrl(urlValue) else ""
            QrContentType.WIFI -> if (wifiSsid.isNotBlank()) {
                QrContentEncoder.encodeWifi(wifiSsid, wifiPassword, wifiSecurity, wifiHidden)
            } else ""
            QrContentType.CONTACT -> if (contactName.isNotBlank() || contactPhone.isNotBlank()) {
                QrContentEncoder.encodeContact(contactName, contactPhone, contactEmail, contactOrg)
            } else ""
            QrContentType.PHONE -> if (phoneValue.isNotBlank()) QrContentEncoder.encodePhone(phoneValue) else ""
            QrContentType.EMAIL -> if (emailAddress.isNotBlank()) {
                QrContentEncoder.encodeEmail(emailAddress, emailSubject, emailBody)
            } else ""
            QrContentType.SMS -> if (smsNumber.isNotBlank()) {
                QrContentEncoder.encodeSms(smsNumber, smsMessage)
            } else ""
        }
    }

    fun generateQr() {
        focusManager.clearFocus()
        val content = buildEncodedContent()
        if (content.isBlank() || isGenerating) return
        isGenerating = true
        scope.launch {
            generatedBitmap = withContext(Dispatchers.Default) { QrCodeGenerator.generate(content) }
            isGenerating = false
        }
    }

    val currentBitmap = generatedBitmap
    if (currentBitmap != null) {
        QrResultScreen(
            qrBitmap = currentBitmap,
            onBack = { generatedBitmap = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create QR Code") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QrContentType.entries) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.label) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (selectedType) {
                        QrContentType.TEXT -> {
                            QrTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                label = "Enter your text",
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default,
                                minLines = 2,
                                maxLines = 5
                            )
                        }
                        QrContentType.URL -> {
                            QrTextField(
                                value = urlValue,
                                onValueChange = { urlValue = it },
                                label = "Enter a link (e.g. example.com)",
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            )
                        }
                        QrContentType.WIFI -> {
                            QrTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = "Network name (SSID)"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WifiSecurityOptions.forEach { option ->
                                    FilterChip(
                                        selected = wifiSecurity == option,
                                        onClick = { wifiSecurity = option },
                                        label = { Text(option) }
                                    )
                                }
                            }
                            if (wifiSecurity != "None") {
                                QrTextField(
                                    value = wifiPassword,
                                    onValueChange = { wifiPassword = it },
                                    label = "Password",
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .toggleable(
                                        value = wifiHidden,
                                        role = Role.Checkbox,
                                        onValueChange = { wifiHidden = it }
                                    )
                            ) {
                                Checkbox(checked = wifiHidden, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Hidden network", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        QrContentType.CONTACT -> {
                            QrTextField(
                                value = contactName,
                                onValueChange = { contactName = it },
                                label = "Full name",
                                capitalization = KeyboardCapitalization.Words
                            )
                            QrTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = "Phone number",
                                keyboardType = KeyboardType.Phone
                            )
                            QrTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = "Email (optional)",
                                keyboardType = KeyboardType.Email
                            )
                            QrTextField(
                                value = contactOrg,
                                onValueChange = { contactOrg = it },
                                label = "Organization (optional)",
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            )
                        }
                        QrContentType.PHONE -> {
                            QrTextField(
                                value = phoneValue,
                                onValueChange = { phoneValue = it },
                                label = "Phone number",
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            )
                        }
                        QrContentType.EMAIL -> {
                            QrTextField(
                                value = emailAddress,
                                onValueChange = { emailAddress = it },
                                label = "Email address",
                                keyboardType = KeyboardType.Email
                            )
                            QrTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = "Subject (optional)",
                                capitalization = KeyboardCapitalization.Sentences
                            )
                            QrTextField(
                                value = emailBody,
                                onValueChange = { emailBody = it },
                                label = "Message (optional)",
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default,
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                        QrContentType.SMS -> {
                            QrTextField(
                                value = smsNumber,
                                onValueChange = { smsNumber = it },
                                label = "Phone number",
                                keyboardType = KeyboardType.Phone
                            )
                            QrTextField(
                                value = smsMessage,
                                onValueChange = { smsMessage = it },
                                label = "Message (optional)",
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default,
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = ::generateQr,
                    shape = RoundedCornerShape(FieldRadius),
                    enabled = !isGenerating && buildEncodedContent().isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate QR Code")
                }

                Spacer(modifier = Modifier.height(bottomNavClearance()))
            }
        }
    }
}

@Composable
private fun QrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    minLines: Int = 1,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FieldRadius),
        singleLine = maxLines == 1,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        )
    )
}
