package com.scann.thekami.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.scann.thekami.model.ScanAction

object ScanActionHandler {

    fun handle(context: Context, action: ScanAction) {
        when (action) {
            is ScanAction.OpenUrl -> openUrl(context, action.url)
            is ScanAction.CopyText -> copyToClipboard(context, action.text)
            is ScanAction.ShareText -> shareText(context, action.text)
            is ScanAction.CallPhone -> callPhone(context, action.number)
            is ScanAction.SendEmail -> sendEmail(context, action.address, action.subject, action.body)
            is ScanAction.SendSms -> sendSms(context, action.number, action.message)
            is ScanAction.OpenLocation -> openLocation(context, action.latitude, action.longitude)
            is ScanAction.ConnectWifi -> connectWifi(context, action.ssid, action.password, action.encryptionType)
            is ScanAction.AddContact -> addContact(context, action.name, action.phone, action.email, action.organization)
            is ScanAction.AddCalendarEvent -> addCalendarEvent(
                context, action.title, action.description, action.location,
                action.startTimeMillis, action.endTimeMillis, action.isAllDay
            )
        }
    }

    private fun openUrl(context: Context, url: String) {
        try {
            val fixedUrl = when {
                url.startsWith("https://") || url.startsWith("http://") -> url
                url.startsWith("https://", ignoreCase = true) -> "https://${url.substring(8)}"
                url.startsWith("http://", ignoreCase = true) -> "http://${url.substring(7)}"
                else -> "https://$url"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "Could not open link")
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Scann", text))
        toast(context, "Copied to clipboard")
    }

    private fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun callPhone(context: Context, number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "Could not open dialer")
        }
    }

    private fun sendEmail(context: Context, address: String, subject: String?, body: String?) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$address")
                subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                body?.let { putExtra(Intent.EXTRA_TEXT, it) }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "No email app found")
        }
    }

    private fun sendSms(context: Context, number: String, message: String?) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                message?.let { putExtra("sms_body", it) }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "No messaging app found")
        }
    }

    private fun openLocation(context: Context, lat: Double, lng: Double) {
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "No maps app found")
        }
    }

    private fun connectWifi(context: Context, ssid: String, password: String?, encryptionType: String?) {
        if (ssid.isBlank()) {
            toast(context, "Invalid Wi-Fi network")
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val builder = android.net.wifi.WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)

                if (!password.isNullOrBlank()) {
                    when (encryptionType) {
                        "WEP" -> {
                            // WEP is not supported by WifiNetworkSuggestion API.
                            copyToClipboard(context, password)
                            toast(context, "WEP networks aren't supported for auto-connect. Password copied.")
                            return
                        }
                        else -> builder.setWpa2Passphrase(password)
                    }
                }

                val suggestion = builder.build()

                val intent = Intent(android.provider.Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                    putParcelableArrayListExtra(
                        android.provider.Settings.EXTRA_WIFI_NETWORK_LIST,
                        arrayListOf(suggestion)
                    )
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    copyToClipboard(context, password ?: ssid)
                    toast(context, "Could not open Wi-Fi settings. Password copied instead.")
                }
            } catch (e: Exception) {
                copyToClipboard(context, password ?: ssid)
                toast(context, "Could not connect automatically: ${e.message ?: "unknown error"}")
            }
        } else {
            copyToClipboard(context, password ?: ssid)
            toast(context, "Wi-Fi setup needs Android 11+. Network details copied instead.")
        }
    }

    private fun addContact(context: Context, name: String?, phone: String?, email: String?, organization: String?) {
        val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
            name?.let { putExtra(android.provider.ContactsContract.Intents.Insert.NAME, it) }
            phone?.let { putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, it) }
            email?.let { putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, it) }
            organization?.let { putExtra(android.provider.ContactsContract.Intents.Insert.COMPANY, it) }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "Could not open contacts app")
        }
    }

    private fun addCalendarEvent(
        context: Context,
        title: String?,
        description: String?,
        location: String?,
        startTimeMillis: Long?,
        endTimeMillis: Long?,
        isAllDay: Boolean
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = android.provider.CalendarContract.Events.CONTENT_URI
            title?.let { putExtra(android.provider.CalendarContract.Events.TITLE, it) }
            description?.let { putExtra(android.provider.CalendarContract.Events.DESCRIPTION, it) }
            location?.let { putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, it) }
            startTimeMillis?.let { putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
            endTimeMillis?.let { putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, it) }
            if (isAllDay) putExtra(android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            toast(context, "Could not open calendar app")
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
