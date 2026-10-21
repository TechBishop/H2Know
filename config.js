/* The only revenue config. The web app reads this file. The Android build copies it into the APK.
   AMAZON_ASSOCIATE_TAG is the Associates store ID.
   STRIPE_PAYMENT_LINK stays blank until a real Payment Link exists. Do not invent one.
   PREMIUM_ENABLED stays false during beta, so nothing is locked and no checkout is shown.
   Set PREMIUM_ENABLED to true later to turn checkout and the saved-address cap back on. */
window.H2KnowConfig = {
  AMAZON_ASSOCIATE_TAG: "bishopsfolly-20",
  STRIPE_PAYMENT_LINK: "",
  PREMIUM_ENABLED: false,
  FREE_SAVED_ADDRESSES: 1,
  PAID_SAVED_ADDRESSES: 8,
  AFFILIATE_DISCLOSURE:
    "Affiliate link. H2Know may earn a commission at no extra cost to you. This suggestion is matched to a contaminant reported for this water system. It is not an EPA recommendation, and H2Know does not sell filters."
};
