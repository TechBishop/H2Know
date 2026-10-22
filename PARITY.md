# H2Know parity

Baseline is `bd9e18e` (`feat: add product recommendations and stable scores`): the single-file `index.html` and the Android UI. The reskin lives in `/reskin/` and keeps the calm dark score-first skin. It does not replace the root app. Its service worker is registered with scope `./`, so it only controls `/reskin/` and it only deletes caches named `h2know-reskin-shell-*`.

Status values: **ported**, **unavailable** (no real data source; no invented number), **removed by rule**.

| # | Screen or feature | Where it lived at bd9e18e | Status | Notes |
| --- | --- | --- | --- | --- |
| 1 | ZIP lookup | Search home, Android search | ported | Zippopotam, then EPA community systems for the county. |
| 2 | Preset ZIP chips | Search home | ported | Austin, Flint, Newark, Jackson, Beverly Hills. |
| 3 | Provider picker | Inline providers, Android provider list | ported | Birdseye IN 47513 lists BIRDSEYE WATER SYSTEM `IN5219001` first, marked listed for that city. |
| 4 | Score printed on the picker before a record loads | Inline provider cards used a stable hash score | removed by rule | A score is shown only after the EPA record loads. |
| 5 | Water score 0–100 | Overview gauge, Android report | ported | From open violations and samples over a legal limit. Missing feeds are not scored. |
| 6 | Verdict and compliance | Overview badge | ported | Plain sentence from the same public record. |
| 7 | Utility facts | Overview grid and source tab | ported | Name, population, source, owner, EPA ID, phone, address, admin, connections. |
| 8 | Detected lead and copper | Quality tab, LCR tables | ported | EPA lead-and-copper sample table only. |
| 9 | Lead and copper by year | Quality charts | ported | Years with a real sample. Empty years are left out. |
| 10 | Trihalomethanes, haloacetic acids, nitrate, arsenic, PFOA, PFOS, fluoride | Quality tab showed numbers for these | unavailable | They are not in the lead-and-copper table. The reskin names them and shows no number. |
| 11 | Contaminant filters | Quality chips | ported | All, above goal, PFAS, metals, byproducts. PFAS and byproduct filters say the table does not include them. |
| 12 | What the names mean | Quality detail | ported | Health note and legal limit, including “not in this record.” |
| 13 | Violations | History “Past testing & treatment notices” | ported | EPA violation rows, with open / addressed / resolved. |
| 14 | 1-year, 5-year, 10-year chart | History score line | ported | The line is a count of compliance records. |
| 15 | Historical water score | History chart plotted a score | unavailable | A past score is not in the public feed. |
| 16 | Drought status | Source tab scale | ported | U.S. Drought Monitor point. “None” means the point is outside a mapped area. |
| 17 | County drought trend | Not separate in the old page; the point description stood in | ported | County statistic when that feed returns. Otherwise the line says unavailable. |
| 18 | Daily gallon target | Source tab | ported | Labeled as a planning figure, not a utility meter. |
| 19 | How your water is treated | Source tab steps | ported | Marked **general information, not this plant’s data**. |
| 20 | Source and watershed description | Source tab | ported | Built from the EPA system record. |
| 21 | EPA facility record | Share / source links | ported | ECHO link for the PWSID. |
| 22 | Consumer confidence report link | Mentioned as missing | unavailable | That link is not in this feed. |
| 23 | Other utilities in the ZIP | Provider switcher | ported | Change provider, plus the rest of the county list. |
| 24 | Search a utility by name or PWSID | Android and the later picker | ported | Statewide search from the picker. |
| 25 | Saved locations | Home “Saved locations”, watch list | ported | Stored in this browser under reskin-only keys. Reopening compares score and open violations. |
| 26 | Saved snapshots | History “Saved report snapshots” | ported | Browser copies are labeled as this browser. Firebase copies are written only when signed in and the score is 0–100. |
| 27 | Neighborhood reports | Overview community cards | ported | Filter all / open / resolved. Browser notes are labeled as this browser. |
| 28 | Report Water Quality Issue | Dialog, Android `ReportCloudIssueDialog` | ported | Same categories and severity. Firebase uses the Android project’s named database. See the unavailable row for unauthorized web hosts. |
| 29 | Add affected, resolve, remove | Issue cards | ported | Each button talks to the store that holds that note. |
| 30 | Google sign-in for Firebase | Auth screen. The old web page only flipped a guest flag and wrote localStorage. | unavailable | The client is wired. Google rejects this web host because it is not an authorized domain (`localhost` and GitHub Pages are not on the project list). The screen says so and does not store a fake cloud report. Android sign-in remains the path that can reach Firestore. |
| 31 | Banner that said a local note was saved to Firebase | Old web `showCloudStatusBanner` | removed by rule | A browser save is never described as Firebase. |
| 32 | Support | Donate buttons, Buy Me a Coffee confirm | ported | Opens https://buymeacoffee.com/bishopsfolly |
| 33 | Privacy and EPA notice | Privacy modal, footer | ported | Independent publisher, public sources, not a lab test. |
| 34 | Share the site and share a score | Footer and snapshot dialog | ported | Copy and the system share sheet. |
| 35 | Download the report | Android share / web download | ported | Plain-text file. |
| 36 | Android APK link | Footer | ported | GitHub release APK. |
| 37 | Product recommendation | Quality marketplace and `ReferenceWaterData` | ported | One filter and one test kit, only when a reported sample calls for them. Every link gets `tag=bishopsfolly-20` and the affiliate disclosure is on the card. |
| 38 | Priced marketplace, reduction percents, home featured kits | Old HTML product cards | unavailable | Those prices and percents were not from a catalog. |
| 39 | WaterSense and whole-home product cards | Source tab | unavailable | No utility fixture catalog. |
| 40 | Household profile switcher | Android actions with invented reduction rates | unavailable | No measured reduction for a profile. Lead guidance is one general sentence when lead was actually detected. |
| 41 | WHO / WHERE / WHY / WHEN tabs and dock | Web tabs and `WhoWhereWhyWhenDashboard.kt` | removed by rule | The content is in the score-first report. The unused dashboard file is deleted. The report dialog moved to `ReportCloudIssueDialog.kt`. |
| 42 | Light/dark theme toggle | Top bar | removed by rule | The reskin is the dark score-first skin. |
| 43 | Premium checkout and saved-address cap | Config and Android saved-address card | removed by rule | `PREMIUM_ENABLED` stays false. Nothing is charged. |
| 44 | Continue without an account | Guest button | ported | Lookups and browser saves do not require Firebase. |

## Counts

- Ported: 32
- Unavailable: 7 (rows 10, 15, 22, 30, 38, 39, 40)
- Removed by rule: 5 (rows 4, 31, 41, 42, 43)

## Unavailable, and why

- **Trihalomethanes, haloacetic acids, nitrate, arsenic, PFOA, PFOS, fluoride.** The public table this app reads is EPA lead and copper. Inventing the other results is how the baseline page filled the quality tab.
- **Historical score.** EPA does not publish a past 0–100 score. The chart counts compliance records instead.
- **Consumer confidence report.** No CCR URL is in the system feed.
- **Firebase sign-in on this website.** The Firebase project only authorizes its own hosting and Cloud Run domains. A Google popup from `/reskin/` returns unauthorized-domain. The form stays, and a browser note is a separate button.
- **Priced filters, reduction percents, and featured kits.** There is no price or certification feed. A suggestion is an Amazon search matched to a real sample.
- **WaterSense and whole-home cards.** There is no catalog from the utility.
- **Household profiles.** The old actions quoted reduction percents and prices that were not measured.

## Checks

- ZIP 47513 returns Birdseye, Indiana, Dubois County, and `IN5219001` BIRDSEYE WATER SYSTEM (purchased surface water, listed for the city).
- That system’s loaded record scored 100, with no lead or copper sample, 8 violation rows and none open, drought “none,” and treatment steps labeled general information.
- Flint `MI0002310` had lead at 3 ppb. The filter and test-kit links both ended with `tag=bishopsfolly-20`, and the disclosure was on the card.
- The reskin service worker scope on the local server was `http://127.0.0.1:4174/reskin/`.
