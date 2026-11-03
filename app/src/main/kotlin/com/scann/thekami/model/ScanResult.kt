package com.scann.thekami.model

sealed class ScanResultType {
    data class Url(val value: String) : ScanResultType()
    data class PlainText(val value: String) : ScanResultType()
    data class Phone(val value: String) : ScanResultType()
    data class Email(val address: String, val subject: String?, val body: String?) : ScanResultType()
    data class Sms(val phoneNumber: String, val message: String?) : ScanResultType()
    data class Location(val latitude: Double, val longitude: Double) : ScanResultType()
    data class WifiNetwork(
        val ssid: String,
        val password: String?,
        val encryptionType: String?
    ) : ScanResultType()
    data class Contact(
        val name: String?,
        val phone: String?,
        val email: String?,
        val organization: String?
    ) : ScanResultType()
    data class CalendarEvent(
        val title: String?,
        val description: String?,
        val location: String?,
        val startTimeMillis: Long?,
        val endTimeMillis: Long?,
        val isAllDay: Boolean = false
    ) : ScanResultType()

    data class Unknown(val raw: String) : ScanResultType()
}

data class ScanResult(
    val type: ScanResultType,
    val rawValue: String,
    val format: String
)
