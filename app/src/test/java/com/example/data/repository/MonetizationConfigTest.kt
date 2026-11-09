package com.example.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationConfigTest {
    @Test
    fun emptyHandlesLeaveSupportHiddenAndAmazonLinksUntagged() {
        assertEquals("", SUPPORT_URL)
        assertEquals("", AMAZON_AFFILIATE_TAG)
        assertEquals(
            "https://www.amazon.com/s?k=NSF+ANSI+53+lead+reduction+drinking+water+filter",
            MonetizationConfig.amazonUrl(
                "https://www.amazon.com/s?k=NSF+ANSI+53+lead+reduction+drinking+water+filter",
                AMAZON_AFFILIATE_TAG
            )
        )
        val parsed = MonetizationConfig.parse(
            """
            window.H2KnowConfig = {
              AMAZON_ASSOCIATE_TAG: "",
              STRIPE_PAYMENT_LINK: "",
              PREMIUM_ENABLED: false
            };
            """.trimIndent()
        )
        assertEquals("", parsed.amazonTag)
        assertEquals("", parsed.stripeLink)
        assertFalse(parsed.premiumEnabled)
        assertFalse(parsed.checkoutConfigured)
        assertEquals(Int.MAX_VALUE, parsed.savedAddressLimit)
    }

    @Test
    fun premiumFlagOffIgnoresAPaymentLink() {
        val parsed = MonetizationConfig.parse(
            """
            window.H2KnowConfig = {
              AMAZON_ASSOCIATE_TAG: "example-20",
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
              AMAZON_ASSOCIATE_TAG: "example-20",
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
}
