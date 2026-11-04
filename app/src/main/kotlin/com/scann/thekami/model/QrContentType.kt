package com.scann.thekami.model

enum class QrContentType(val label: String) {
    TEXT("Text"),
    URL("Link / URL"),
    WIFI("Wi-Fi"),
    CONTACT("Contact"),
    PHONE("Phone Number"),
    EMAIL("Email"),
    SMS("SMS")
}

object QrContentEncoder {

    fun encodeText(text: String): String = text

    fun encodeUrl(url: String): String {
        return if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            "https://$url"
        } else url
    }

    fun encodeWifi(ssid: String, password: String, security: String, isHidden: Boolean): String {
        val escapedSsid = escape(ssid)
        val escapedPassword = escape(password)
        val securityCode = when (security) {
            "WPA/WPA2" -> "WPA"
            "WEP" -> "WEP"
            "None" -> "nopass"
            else -> "WPA"
        }
        return if (securityCode == "nopass") {
            "WIFI:T:nopass;S:$escapedSsid;H:${if (isHidden) "true" else "false"};;"
        } else {
            "WIFI:T:$securityCode;S:$escapedSsid;P:$escapedPassword;H:${if (isHidden) "true" else "false"};;"
        }
    }

    fun encodeContact(
        name: String,
        phone: String,
        email: String,
        organization: String
    ): String {
        val builder = StringBuilder()
        builder.append("BEGIN:VCARD\n")
        builder.append("VERSION:3.0\n")
        if (name.isNotBlank()) builder.append("N:$name\nFN:$name\n")
        if (phone.isNotBlank()) builder.append("TEL:$phone\n")
        if (email.isNotBlank()) builder.append("EMAIL:$email\n")
        if (organization.isNotBlank()) builder.append("ORG:$organization\n")
        builder.append("END:VCARD")
        return builder.toString()
    }

    fun encodePhone(number: String): String = "tel:$number"

    fun encodeEmail(address: String, subject: String, body: String): String {
        val builder = StringBuilder("mailto:$address")
        val params = mutableListOf<String>()
        if (subject.isNotBlank()) params.add("subject=${java.net.URLEncoder.encode(subject, "UTF-8")}")
        if (body.isNotBlank()) params.add("body=${java.net.URLEncoder.encode(body, "UTF-8")}")
        if (params.isNotEmpty()) builder.append("?").append(params.joinToString("&"))
        return builder.toString()
    }

    fun encodeSms(number: String, message: String): String {
        return if (message.isNotBlank()) {
            "smsto:$number:$message"
        } else {
            "smsto:$number"
        }
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
    }
}
