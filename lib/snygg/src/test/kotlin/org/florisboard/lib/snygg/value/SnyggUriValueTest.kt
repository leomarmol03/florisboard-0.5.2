package org.florisboard.lib.snygg.value

import org.junit.jupiter.api.assertAll
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SnyggUriValueTest {
    private val encoder = SnyggUriValue

    @Test
    fun `deserialize uri values`() {
        val pairs = listOf(
            // valid
            "uri(`flex:/my_image.png`)" to SnyggUriValue("flex:/my_image.png"),
            "uri(`flex:/roboto.ttf`)" to SnyggUriValue("flex:/roboto.ttf"),
            "uri(`res:/font/google_sans`)" to SnyggUriValue("res:/font/google_sans"),
            "uri(`https://example.com/font.ttf`)" to SnyggUriValue("https://example.com/font.ttf"),
            // invalid
            "some-color" to null,
            "uri(`flex:/my image.png`)" to null,
        )
        assertAll(pairs.map { (raw, expected) -> {
            assertEquals(expected, encoder.deserialize(raw).getOrNull(), "deserialize $raw")
        } })
    }

    @Test
    fun `serialize uri values`() {
        val pairs = listOf(
            // valid
            SnyggUriValue("flex:/my_image.png") to "uri(`flex:/my_image.png`)",
            SnyggUriValue("flex:/roboto.ttf") to "uri(`flex:/roboto.ttf`)",
            SnyggUriValue("res:/font/google_sans") to "uri(`res:/font/google_sans`)",
            // invalid
            SnyggDefinedVarValue("shenanigans") to null
        )
        assertAll(pairs.map { (snyggValue, expected) -> {
            assertEquals(expected, encoder.serialize(snyggValue).getOrNull(), "serialize $snyggValue")
        } })
    }

    @Test
    fun `check class of default value`() {
        assertIs<SnyggUriValue>(encoder.defaultValue())
    }
}
