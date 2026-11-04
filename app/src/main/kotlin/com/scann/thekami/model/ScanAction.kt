package com.scann.thekami.model

sealed class ScanAction {
    data class OpenUrl(val url: String) : ScanAction()
    data class CopyText(val text: String) : ScanAction()
    data class ShareText(val text: String) : ScanAction()
    data class CallPhone(val number: String) : ScanAction()
    data class SendEmail(val address: String, val subject: String?, val body: String?) : ScanAction()
    data class SendSms(val number: String, val message: String?) : ScanAction()
    data class OpenLocation(val latitude: Double, val longitude: Double) : ScanAction()
    data class ConnectWifi(val ssid: String, val password: String?, val encryptionType: String?) : ScanAction()
    data class AddContact(
        val name: String?,
        val phone: String?,
        val email: String?,
        val organization: String?
    ) : ScanAction()
    data class AddCalendarEvent(
        val title: String?,
        val description: String?,
        val location: String?,
        val startTimeMillis: Long?,
        val endTimeMillis: Long?,
        val isAllDay: Boolean = false
    ) : ScanAction()
}
