package com.example.data.repository

import android.content.Context
import java.net.URLEncoder

/** Set a support page URL here. Empty hides Support and Buy Me a Coffee. */
const val SUPPORT_URL = ""

/** Set an Amazon Associates tag here. Empty leaves filter links untagged and hides the affiliate disclosure. */
const val AMAZON_AFFILIATE_TAG = ""

/**
 * The two constants above are the one-line handles.
 * Other revenue settings still load from the repo-root config.js.
 * PREMIUM_ENABLED stays false during beta, which hides checkout and removes the address cap.
 */
object MonetizationConfig {
    var AMAZON_ASSOCIATE_TAG: String = AMAZON_AFFILIATE_TAG
        private set
    var STRIPE_PAYMENT_LINK: String = ""
        private set
    var premiumEnabled: Boolean = false
        private set
    var freeSavedAddresses: Int = 1
        private set
    var paidSavedAddresses: Int = 8
        private set

    const val AFFILIATE_DISCLOSURE =
        "Affiliate link. H2Know may earn a commission at no extra cost to you. " +
            "This suggestion is matched to a contaminant reported for this water system. " +
            "It is not an EPA recommendation, and H2Know does not sell filters."

    val checkoutConfigured: Boolean
        get() = premiumEnabled && STRIPE_PAYMENT_LINK.isNotBlank()

    val savedAddressLimit: Int
        get() = when {
            !premiumEnabled -> Int.MAX_VALUE
            checkoutConfigured -> paidSavedAddresses
            else -> freeSavedAddresses
        }

    fun load(context: Context) {
        val text = try {
            context.assets.open("config.js").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return
        }
        apply(parse(text))
    }

    fun amazonUrl(searchUrl: String, tag: String = AMAZON_ASSOCIATE_TAG): String {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return searchUrl
        val join = if (searchUrl.contains("?")) "&" else "?"
        return "$searchUrl$join" + "tag=" + URLEncoder.encode(trimmed, Charsets.UTF_8.name())
    }

    internal fun parse(source: String): ParsedKeys {
        val body = source.substringAfter("window.H2KnowConfig", source)
        return ParsedKeys(
            amazonTag = stringValue(body, "AMAZON_ASSOCIATE_TAG"),
            stripeLink = stringValue(body, "STRIPE_PAYMENT_LINK"),
            premiumEnabled = booleanValue(body, "PREMIUM_ENABLED", false),
            freeSavedAddresses = numberValue(body, "FREE_SAVED_ADDRESSES", 1),
            paidSavedAddresses = numberValue(body, "PAID_SAVED_ADDRESSES", 8)
        )
    }

    private fun apply(parsed: ParsedKeys) {
        AMAZON_ASSOCIATE_TAG = AMAZON_AFFILIATE_TAG.trim()
        STRIPE_PAYMENT_LINK = parsed.stripeLink
        premiumEnabled = parsed.premiumEnabled
        freeSavedAddresses = parsed.freeSavedAddresses
        paidSavedAddresses = parsed.paidSavedAddresses
    }

    private fun booleanValue(source: String, key: String, fallback: Boolean): Boolean {
        val match = Regex("""\b$key\s*:\s*(true|false)\b""").find(source) ?: return fallback
        return match.groupValues[1] == "true"
    }

    private fun stringValue(source: String, key: String): String {
        val match = Regex("""\b$key\s*:\s*"([^"]*)"""").find(source) ?: return ""
        return match.groupValues[1].trim()
    }

    private fun numberValue(source: String, key: String, fallback: Int): Int {
        val match = Regex("""\b$key\s*:\s*(\d+)""").find(source) ?: return fallback
        return match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: fallback
    }
}

internal data class ParsedKeys(
    val amazonTag: String,
    val stripeLink: String,
    val premiumEnabled: Boolean,
    val freeSavedAddresses: Int,
    val paidSavedAddresses: Int
) {
    val checkoutConfigured: Boolean
        get() = premiumEnabled && stripeLink.isNotBlank()

    val savedAddressLimit: Int
        get() = when {
            !premiumEnabled -> Int.MAX_VALUE
            checkoutConfigured -> paidSavedAddresses
            else -> freeSavedAddresses
        }
}
