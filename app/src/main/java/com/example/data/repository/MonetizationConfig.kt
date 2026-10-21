package com.example.data.repository

/**
 * Placeholders only. Do not put a live Associates tag or payment URL in source.
 * Set [AMAZON_ASSOCIATE_TAG] to your Amazon Associates ID when you have one.
 * Set [STRIPE_PAYMENT_LINK] to a Stripe Payment Link when billing is ready.
 */
object MonetizationConfig {
    const val AMAZON_ASSOCIATE_TAG = ""
    const val STRIPE_PAYMENT_LINK = ""

    const val AFFILIATE_DISCLOSURE =
        "Affiliate link. If a tag is configured, H2Know may earn a commission at no extra cost to you. " +
            "This suggestion is matched to a contaminant reported for this water system. " +
            "It is not an EPA recommendation, and H2Know does not sell filters."

    const val FREE_SAVED_ADDRESSES = 1

    val checkoutConfigured: Boolean
        get() = STRIPE_PAYMENT_LINK.isNotBlank()

    fun amazonUrl(searchUrl: String): String {
        val tag = AMAZON_ASSOCIATE_TAG.trim()
        if (tag.isEmpty()) return searchUrl
        val join = if (searchUrl.contains("?")) "&" else "?"
        return "$searchUrl${join}tag=$tag"
    }
}
