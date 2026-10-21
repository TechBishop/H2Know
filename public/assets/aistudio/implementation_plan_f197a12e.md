# H2Know — Civic Water Quality Intelligence, Historical Trends & Issue Tracker

An upgraded, full-stack version of **H2Know** that transforms U.S. EPA drinking water records, multi-decade sampling histories, U.S. Drought Monitor data, and community reports into fast water quality scorecards, **1-year / 5-year / 10-year historical trend visualizations**, **quality-change alerts**, dual-benchmark contaminant analysis, household-tailored filtration guidance, and an interactive Open Tickets tracker.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions incorporate your confirmed preferences along with the newly added **Historical Trends (1Y / 5Y / 10Y)** and **Quality Change Alerts** capabilities:

- **Confirmed Decision — Historical Water Quality Trends (1-Year, 5-Year, 10-Year)**: A dedicated **Historical Trends** analytics section (and embedded contaminant sparklines) will let users toggle between **Past 1 Year** (quarterly/seasonal view), **Past 5 Years** (annual medium-term trajectory), and **Past 10 Years** (decade-long infrastructure and compliance history) with interactive SVG line/area charts, MCL/Health Goal threshold lines, and plain-English explanations of what each trend indicates about the water supply.
- **Confirmed Decision — Quality Change Alerts & Watchlist**: The app will automatically detect and surface **Quality Change Alerts** (such as sudden contaminant spikes, seasonal disinfection byproduct shifts, newly opened EPA violations, or multi-year quality score changes) and let users enable a **ZIP / Provider Alert Watchlist** with customizable alert triggers (*Score Drops*, *Health Goal Exceedances*, *New Violations*, *Local Resident Ticket Spikes*).
- **Confirmed Decision — Open Tickets & Issue Linking**: Each water provider (`PWSID`) features a unified **Tickets & Issues** board combining **official EPA open compliance/enforcement items** and **resident-submitted local quality/service tickets** linked directly to contaminants and historical anomalies.
- **Confirmed Decision — Dual Contaminant Benchmarks & Household Profiles**: Evaluates water samples against both the **EPA Legal Limit (MCL)** and stricter **Public Health Goals (Health Benchmarks)**, paired with a **Household Profile Selector** (*General Household*, *Infants & Expecting*, *Pre-1986 Plumbing*, *Sensitive Health*) that customizes filtration and priority actions.
- **Confirmed Decision — Fast Server-Side EPA Proxy + Local Storage**: Express backend routes (`/api/*`) fetch, aggregate, and cache 10-year EPA Envirofacts histories (`WATER_SYSTEM`, `VIOLATION`, `LCR_SAMPLE_RESULT`, `GEOGRAPHIC_AREA`), Zippopotam.us, and U.S. Drought Monitor data, while `localStorage` persists saved ZIPs, alert subscriptions, household profiles, and user-created tickets.

---

## 1. Overview & Core Concept

- **What It Does**: Lets residents enter any 5-digit U.S. ZIP code, pick their local water utility, and explore a comprehensive workspace covering **Overview & Active Alerts**, **Historical Trends (1Y / 5Y / 10Y)**, **Contaminants (MCL vs. Health Goal)**, **Violations & Compliance**, **Open Tickets & Community Issues**, **Water Source & Treatment**, and **Tailored Actions & Filtration**.
- **Target Audience / Persona**: Homeowners, renters, parents, and civic-minded residents who want to understand not only what is in their tap water today, but whether their water supply is improving or degrading over time, why those shifts happen, and how to stay alerted to quality changes.
- **Key Value**: Turns static federal water records into living longitudinal intelligence—showing 1-, 5-, and 10-year trajectories with clear diagnostic explanations, proactive quality-change alerts, and actionable filtration and ticket-tracking tools.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **ZIP Lookup, Provider Selection & Watchlist Pinning**:
   - Users enter a 5-digit ZIP code (or pick quick-launch examples like Austin `78701`, Flint `48502`, Newark `07102`, Jackson `39201`, Beverly Hills `90210`, or their Saved/Watched ZIPs).
   - Matching Community Water Systems display population served, water source, current quality score, 5-year trend direction (`Improving`, `Stable`, `Declining`), and open ticket count.
2. **Overview & Quality Change Alert Banner**:
   - Displays the 0–100 Water Quality Gauge, multi-year score delta badge (e.g., `+6 pts vs. 5-yr avg`), system metadata, and summary counters.
   - Features the **Quality Change Alerts & Watchlist Panel**: highlights detected shifts (e.g., *"TTHM disinfection byproducts rose +22% in the past year"*, *"Lead 90th percentile decreased -40% over 10 years following corrosion control upgrades"*, or *"1 new monitoring violation opened this quarter"*) and lets the user toggle **Watch This ZIP / Provider** to configure custom alert thresholds.
3. **Historical Water Quality Trends (1-Year, 5-Year, 10-Year)**:
   - **Timeframe Switcher**: Clean segmented control switching between **Past 1 Year** (quarterly seasonal breakdown), **Past 5 Years** (annual medium-term trend), and **Past 10 Years** (decade-long historical view).
   - **Interactive Trend Charts**:
     - *Overall Water Quality Score Trajectory (0–100)*: Area/line chart plotting system score history alongside compliance milestones.
     - *Contaminant Concentration Trend Explorer*: Lets users select specific regulated contaminants (Lead, Copper, TTHMs, HAA5, Nitrate, Arsenic, PFOA/PFOS) and view their measured values over 1Y, 5Y, and 10Y relative to both the **EPA Legal Limit (MCL)** and **Public Health Goal** lines.
   - **What These Trends Indicate (Diagnostic Explanations)**:
     - Context-aware explanatory cards interpreting the observed pattern in plain English:
       - *Seasonal / 1-Year Fluctuations*: Explains how warmer summer temperatures and surface-water organic matter increase chlorine demand and cause seasonal peaks in disinfection byproducts (`TTHMs` / `HAA5`), or how spring snowmelt/rainfall affects turbidity and nitrates.
       - *5-Year Mid-Term Shifts*: Explains how drought cycles concentrate dissolved minerals in reservoirs/aquifers, or how newly mandated EPA testing (such as PFAS monitoring rules) surfaces previously unmeasured compounds.
       - *10-Year Long-Term Trajectories*: Explains infrastructure lifecycles—such as aging cast-iron/lead service lines, orthophosphate corrosion-control interventions, treatment plant upgrades, or chronic repeat monitoring gaps.
4. **Contaminants (Dual MCL & Health Goal Comparison)**:
   - Category filter (`All`, `Exceeds Health Goal`, `Regulated`, `PFAS`, `Metals`, `Disinfection Byproducts`) and dual-threshold progress bars showing both the **Public Health Goal** and **EPA Legal Limit (MCL)**, tabular multipliers, mini trend indicators, and direct links to view historical trends or log a ticket.
5. **Violations & Compliance History**:
   - Summary counters and a stacked bar chart showing health-based, monitoring, and treatment violations across the selected 5- or 10-year window, with one-click navigation to linked compliance tickets.
6. **Open Tickets & Community Issue Tracker**:
   - Combines **Official EPA Compliance Tickets** and **Resident Quality Tickets** (discoloration, taste/odor, pressure loss, boil notices, home test results).
   - Users can filter by source/status, confirm local issues (`+1 Affected`), add follow-up notes, or submit a new ticket linked to a contaminant or quality-change alert.
7. **Water Source, Treatment & Household-Tailored Actions**:
   - Visualizes the treatment process, links to the official EPA ECHO report, and provides the **Household Profile Selector** (*General Household*, *Infants & Expecting*, *Pre-1986 Plumbing*, *Sensitive Health*) to tailor priority actions, NSF/ANSI filter certifications, and drought-aware daily water usage targets.

### Visual Identity & Theme
- **Aesthetic Direction**: High-craft civic environmental dashboard—authoritative, calm, and scannable, supporting both Dark and Light modes across a full 1440px desktop layout and responsive mobile viewports.
- **Color Palette & Mood**:
  - *Dark Mode (Default)*: Deep slate canvas (`#0B0E11`), elevated card surface (`#12161B` / `#1A1F26`), crisp alabaster ink (`#E6E8EB`), muted slate secondary text (`#A8B0BB`), and hairline borders (`rgba(255,255,255,0.07)`).
  - *Light Mode*: Cool off-white canvas (`#F4F6F8`), pure white surface (`#FFFFFF`), deep charcoal ink (`#12161B`), and subtle slate borders (`rgba(15,23,42,0.08)`).
  - *Semantic Accents*: Hydrological Cerulean (`#2B7FD4` / `#4A8FC9`) for primary controls and trend lines, Emerald (`#1A8F5C` / `#3D9B6E`) for improving trends and nominal values, Amber (`#C4841D` / `#C9A04A`) for health-goal exceedances and seasonal warnings, Crimson (`#C43C3C` / `#C96B6B`) for legal MCL exceedances and critical alerts, and Warm Coral (`#FF5E5B`) for the Bishop's Folly creator support link.
- **Typography & Hierarchy**:
  - *Display & Navigation*: `Plus Jakarta Sans` (600/700 weights).
  - *Body Prose*: `DM Sans` (400/500 weights) for accessible scientific explanations.
  - *Metrics, PWSIDs, & Concentrations*: `JetBrains Mono` with `tabular-nums` for concentrations, trend deltas, percentages, and dates.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: 10-Year Historical Aggregation on the Express Proxy**
  - *Chosen Approach*: Expand the backend EPA Envirofacts parser (`LCR_SAMPLE_RESULT` and `VIOLATION` tables) to retain a full 10-year window instead of truncating at 5 years, and synthesize structured 1-Year (4 quarters), 5-Year (annual), and 10-Year (annual) time series for both overall quality score and individual contaminants.
  - *Why*: EPA's Lead & Copper Rule (`LCR_SAMPLE_RESULT`) and compliance histories span multiple years; aggregating them server-side into ready-to-plot 1Y/5Y/10Y series enables instant interactive chart switching on the client without layout lag.
- **Decision 2: Automated Trend Diagnostics Engine**
  - *Chosen Approach*: Pair each historical chart with a deterministic diagnostic analyzer that evaluates slope, volatility, and threshold crossings across the 1Y, 5Y, and 10Y windows to generate plain-English explanations of *why* the water supply changed (e.g., corrosion control adjustments, seasonal surface-water disinfection cycles, drought concentration effects, or monitoring gaps).
  - *Why*: Raw time-series graphs can confuse non-experts; explaining what an upward or downward trend actually indicates empowers residents to distinguish between routine seasonal chemistry shifts and structural infrastructure problems.
- **Decision 3: Proactive Quality Change Alerts & Watchlist**
  - *Chosen Approach*: Compute automatic change-detection alerts whenever a system's latest readings diverge from its historical baseline (or when open violations/drought levels shift), and provide a persistent Watchlist drawer where users can subscribe to ZIPs/providers and test/simulate alert triggers.
  - *Why*: Gives residents a reason to bookmark and return to H2Know, directly connecting historical trend shifts with immediate protective actions and open tickets.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                          H2Know React Client (SPA)                             │
│                                                                                │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ Top Bar (Brand Wordmark · Workspace Navigation · Watchlist/Theme/Support)│  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
│               │                                             │                  │
│               ▼                                             ▼                  │
│  ┌──────────────────────────┐             ┌─────────────────────────────────┐  │
│  │  ZIP Search & Provider   │             │  System Workspace Views         │  │
│  │  Selector View           │             │  ├─ 1. Overview & Change Alerts │  │
│  │  ├─ ZIP Input & Presets  │────────────▶│  ├─ 2. Historical Trends        │  │
│  │  ├─ Watched ZIPs & Alerts│             │  │     (1Y / 5Y / 10Y Charts &  │  │
│  │  └─ Provider List Cards  │             │  │      Supply Explanations)    │  │
│  └──────────────────────────┘             │  ├─ 3. Contaminants (MCL + PHG) │  │
│               │                           │  ├─ 4. Violations (10Y Record)  │  │
│               │                           │  ├─ 5. Open Tickets & Issues    │  │
│               │                           │  ├─ 6. Source & Treatment Flow  │  │
│               │                           │  └─ 7. Actions & Household Fit  │  │
│               ▼                           └─────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │ Client State & LocalStorage Persistence                                  │  │
│  │ ├─ Watched ZIPs/Providers & Custom Alert Threshold Preferences           │  │
│  │ ├─ Selected Household Profile (General / Infant / Old Pipes / Sensitive) │  │
│  │ └─ Resident Issue Tickets & Upvotes (linked to Contaminants & Alerts)    │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────┬──────────────────────────────────────┘
                                          │ HTTP JSON (/api/*)
                                          ▼
┌────────────────────────────────────────────────────────────────────────────────┐
│                      Express Backend Proxy (server.ts)                         │
│                                                                                │
│  ├─ GET /api/search?zip=:zip     ──▶ Zippopotam + EPA WATER_SYSTEM Lookup      │
│  ├─ GET /api/system/:pwsid       ──▶ 10-Yr Historical Fetch & Normalization:   │
│  │                                    ├─ 1Y (Quarterly), 5Y, & 10Y Time Series │
│  │                                    ├─ Automated Trend Diagnostics & Causes  │
│  │                                    ├─ Quality Change Alert Detection        │
│  │                                    └─ US Drought Monitor + EPA ECHO Links   │
│  └─ In-Memory TTL Cache + Resilient 10-Yr Reference System Snapshots           │
└────────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State
- **HistoricalTimeSeries (`1y` | `5y` | `10y`)**:
  - `points[]`: `{ periodLabel, timestamp, qualityScore, violationCount, droughtIndex, contaminantLevels: Record<code, number> }`.
  - `diagnostics[]`: `{ id, timeframe: '1y' | '5y' | '10y', direction: 'improving' | 'stable' | 'elevated' | 'seasonal', title, summary, supplyIndication, recommendedAction, relatedContaminantCode? }`.
- **QualityChangeAlert**:
  - `id`, `pwsid`, `zip`, `severity` (`danger` | `caution` | `info` | `positive`), `changeType` (`contaminant_spike` | `new_violation` | `score_shift` | `seasonal_byproduct` | `long_term_improvement` | `drought_shift`), `deltaLabel` (e.g., `+24% vs 1Y avg`, `-38% over 10Y`), `title`, `message`, `supplyExplanation`, `detectedAt`, `linkedTab`.
- **AlertSubscription (Watchlist)**:
  - `pwsid`, `systemName`, `zip`, `city`, `state`, `notifyOnScoreDrop`, `notifyOnHealthGoalExceed`, `notifyOnNewViolation`, `notifyOnCommunityTicket`, `lastCheckedScore`.

### Interactive Component & State Mapping
- **Historical Trends Explorer (`1Y` / `5Y` / `10Y`)**:
  - Clicking the timeframe selector (`Past 1 Year`, `Past 5 Years`, `Past 10 Years`) updates both the **System Quality Score Trend Chart** and the **Contaminant Concentration Trend Chart**, recalculating period-over-period percentage deltas and displaying interactive hover tooltips with exact sample values, MCL legal limits, and Public Health Goal lines.
  - Selecting a specific contaminant pill (e.g., `Lead (PB90)`, `TTHMs`, `Nitrate`, `PFOA/PFOS`, `Copper`) switches the concentration graph and updates the **What This Trend Indicates About Your Water Supply** diagnostic cards below it.
- **Quality Change Alerts & Watchlist Manager**:
  - Users can click **"Watch ZIP & Alerts"** on any provider to subscribe that ZIP/system to their personal Watchlist, customize which quality-change triggers are active, dismiss or inspect individual change alerts, or click **"Investigate / Open Ticket"** on an alert to jump straight to the relevant trend chart or ticket board.
