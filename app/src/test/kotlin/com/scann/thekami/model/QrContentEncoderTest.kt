package com.scann.thekami.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QrContentEncoderTest {
    @Test
    fun encodeUrlPreservesUppercaseHttpScheme() {
        assertEquals("HTTPS://example.com/path", QrContentEncoder.encodeUrl("HTTPS://example.com/path"))
    }

    @Test
    fun encodeUrlAddsHttpsWhenSchemeIsMissing() {
        assertEquals("https://example.com/path", QrContentEncoder.encodeUrl("example.com/path"))
    }
}
