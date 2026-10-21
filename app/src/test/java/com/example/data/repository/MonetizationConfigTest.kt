package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MonetizationConfigTest {
    @Test
    fun shippedConfigUsesTheAssociatesIdAndLeavesPremiumOff() {
        val parsed = MonetizationConfig.parse(configFile().readText())
        assertEquals("bishopsfolly-20", parsed.amazonTag)
        assertEquals("", parsed.stripeLink)
        assertFalse(parsed.premiumEnabled)
        assertFalse(parsed.checkoutConfigured)
        assertEquals(Int.MAX_VALUE, parsed.savedAddressLimit)
        assertEquals(
            "https://www.amazon.com/s?k=NSF+ANSI+53+lead+reduction+drinking+water+filter&tag=bishopsfolly-20",
            MonetizationConfig.amazonUrl(
                "https://www.amazon.com/s?k=NSF+ANSI+53+lead+reduction+drinking+water+filter",
                parsed.amazonTag
            )
        )
    }

    @Test
    fun premiumFlagOffIgnoresAPaymentLink() {
        val parsed = MonetizationConfig.parse(
            """
            window.H2KnowConfig = {
              AMAZON_ASSOCIATE_TAG: "bishopsfolly-20",
              STRIPE_PAYMENT_LINK: "https://buy.stripe.com/test_abc",
              PREMIUM_ENABLED: false
            };
            """.trimIndent()
        )
        assertFalse(parsed.checkoutConfigured)
        assertEquals(Int.MAX_VALUE, parsed.savedAddressLimit)
    }

    @Test
    fun premiumFlagOnUsesTheStripeLinkAndThePaidCap() {
        val parsed = MonetizationConfig.parse(
            """
            window.H2KnowConfig = {
              AMAZON_ASSOCIATE_TAG: "bishopsfolly-20",
              STRIPE_PAYMENT_LINK: "https://buy.stripe.com/test_abc",
              PREMIUM_ENABLED: true,
              FREE_SAVED_ADDRESSES: 1,
              PAID_SAVED_ADDRESSES: 8
            };
            """.trimIndent()
        )
        assertTrue(parsed.checkoutConfigured)
        assertEquals(8, parsed.savedAddressLimit)
        assertEquals("https://buy.stripe.com/test_abc", parsed.stripeLink)
    }

    private fun configFile(): File {
        val dir = File(System.getProperty("user.dir") ?: ".")
        return listOf(
            File(dir, "config.js"),
            File(dir, "../config.js"),
            File(dir, "../../config.js")
        ).first { it.isFile }
    }
}
