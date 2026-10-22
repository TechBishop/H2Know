# H2Know parity checklist

Reference is the restored Argon app on `main` (commit `830c7e3`), live at the site root. The reskin is a separate app in `/reskin/`. It does not replace `index.html` or the root shell. Its service worker is registered with scope `./`, so it only controls `/reskin/`, and it only deletes caches named `h2know-reskin-shell-*`.

| Feature | Main | Reskin | Status |
| --- | --- | --- | --- |
| ZIP lookup | Search field, Zippopotam, then a short EPA list | Same lookup, then EPA community systems for the county | Matches |
| Preset chips | Austin, Flint, Newark, Jackson, Beverly Hills | Same five chips | Matches |
| Provider list and Switch | Inline cards, then Switch (n) on the report | Picker, then Switch (n) back to that list | Matches |
| Birdseye IN 47513 | ZIP search, often a short list sorted by population | `IN5219001` BIRDSEYE WATER SYSTEM is listed for the city and opens | Matches |
| Score printed before a record loads | Hash score on the provider card | No score until the EPA record loads | Removed by rule |
| Overview tab | Score gauge, people, source, risk, neighborhood reports | Score ring, the same facts, neighborhood reports, save address | Matches |
| Water score 0–100 | Gauge from the page's own formula, including filled-in samples | Open violations and samples over a legal limit. A missing feed is not scored | Matches |
| Source tab | Source, treatment steps, drought scale, daily gallons, product cards | Source, drought, planning gallons, EPA facility link | Matches |
| How water is treated | Shown as this plant's process | Marked "General information, not this plant's data" | Matches |
| Drought | U.S. Drought Monitor point, with extra sentences when the level is none | The point on the map, plus the county line when that feed returns | Matches |
| Quality tab | Contaminant bars, filters, Amazon buttons on every row | Lead and copper bars, filters, a filter only when a sample calls for it | Matches |
| Contaminant filters | All, above goal, PFAS, metals, byproducts | Same chips. PFAS and byproducts say the lead-and-copper table does not include them | Matches |
| Lead and copper | Numbers on the quality tab | EPA lead-and-copper table only, including the yearly samples that exist | Matches |
| Trihalomethanes, haloacetic acids, nitrate, arsenic, PFAS, fluoride | Numbers are filled in | Named as unavailable. No number is invented | Unavailable |
| History tab | 1Y / 5Y / 10Y score line, notices, snapshots | 1Y / 5Y / 10Y control, notices, snapshots | Matches |
| Historical water score | The line is a 0–100 score | "Score trend: Unavailable." The line counts compliance records | Unavailable |
| Violations | Past testing and treatment notices | EPA violation rows, open / addressed / resolved | Matches |
| Share | Header Share opens a snapshot card | Share opens the score text, with copy and the system share sheet | Matches |
| Support | Donate opens Buy Me a Coffee | Support opens the same page, `buymeacoffee.com/bishopsfolly` | Matches |
| Sign in | "Sign in with Google" only clears the guest flag in this browser | Device save is separate. Google sign-in uses the Android Firebase project and says when this host is not authorized | Unavailable for Firebase on this host |
| Saved locations | Shown after the guest flag is cleared | Saved addresses on this device, reskin-only storage | Matches |
| Saved snapshots | History tab, stored in this browser under a cloud name | History tab. Browser copies say "This browser." Firebase copies need a real sign-in | Matches |
| Neighborhood reports | Overview, all / open / resolved | Overview, same filters, source labeled Firebase or this browser | Matches |
| Report Water Quality Issue | Dialog. On the web it stays in this browser | Same dialog. "Save in this browser" is labeled as not Firebase. "Submit to Firebase" needs an authorized Google sign-in | Matches |
| Download for Android | Footer and the desktop bar. GitHub release APK | Home, report, and footer. Same APK URL | Matches |
| Phone frame | Desktop frame, full screen on a narrow phone | Desktop frame, full screen below 720px | Matches |
| Amazon affiliate tag | Quality and home links are not tagged | Every Amazon link ends with `tag=bishopsfolly-20` | Matches |
| Affiliate disclosure | Not shown next to the links | Shown on the filter card | Matches |
| Priced marketplace and home kits | Prices, reduction percents, and featured kits | Not shown. The card says those figures are unavailable | Unavailable |
| WaterSense and whole-home cards | Source tab Amazon cards | Named unavailable. There is no utility catalog | Unavailable |
| Premium and checkout | Not charged in this build | `PREMIUM_ENABLED` is false. No checkout and no paid-tier copy | Removed by rule |
| Theme toggle | Light and dark | Fixed dark score-first skin | Removed by rule |
| Privacy | EPA and publisher notice | Same notice: independent publisher, public sources, not a lab test | Matches |
| Download report | Share text | Plain-text file of the same public facts, without an invented trend | Matches |
| Statewide utility search | Not on the main page | Name or PWSID search from the picker | Matches |
| Service worker | Root worker for the main app | Separate worker, scope `/reskin/`, cache prefix `h2know-reskin-shell-` | Matches |
