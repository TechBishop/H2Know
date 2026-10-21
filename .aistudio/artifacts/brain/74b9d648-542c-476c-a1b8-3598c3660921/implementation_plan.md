# H2Know — Mobile Usability, Gesture Navigation & Tactile Polish Plan

This plan introduces industry-standard Android mobile usability patterns to **H2Know**, focusing on fluid horizontal swipe navigation between the 4W tabs, bidirectional floating dock synchronization, tactile haptic feedback on gestures and actions, smooth micro-animations, and visual report snapshot sharing.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following usability decisions were confirmed during the interactive interview:
> - **Navigation Gesture**: Add horizontal swiping between **WHO**, **WHERE**, **WHY**, and **WHEN** tabs, fully synchronized with the floating dock and tab header.
> - **Tactile & Sensory Feedback**: Implement native Android haptic feedback (`HapticFeedbackType.TextHandleMove` / `LongPress`) on tab changes, dock switching, and report interactions, combined with subtle spring-based micro-animations.
> - **Report Sharing & Snapshots**: Provide a dedicated visual report snapshot card preview in addition to the native Android text share sheet so users can share and capture clean report summaries.

---

## 1. Overview & Core Concept

- **What It Delivers**: Transforms the 4W inspection dashboard into a native mobile experience where users can effortlessly swipe between utility governance (**WHO**), watershed/treatment (**WHERE**), contaminant lab analysis (**WHY**), and historical compliance (**WHEN**).
- **Target Audience / Persona**: Everyday municipal water consumers, homeowners, and renters checking tap water safety on mobile screens with one-handed or thumb-driven gestures.
- **Key Value**: Eliminates rigid button-only navigation; users can fluidly explore complex EPA drinking water data through intuitive horizontal gestures, tactile confirmation, and shareable visual summaries.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Fluid Swipe Navigation Across 4W Tabs**:
   - Swiping left or right anywhere across the report smoothly pans between **WHO**, **WHERE**, **WHY**, and **WHEN**.
   - As the swipe occurs, the floating dock's pill indicator smoothly animates to the new tab with tactile haptic feedback.
   - Tapping an item on the floating dock smoothly scrolls the horizontal pager to the target tab with matching haptic feedback.

2. **Visual Report Snapshot & Sharing**:
   - Tapping **Share** in the header or bottom sheet opens a report snapshot modal featuring a high-contrast visual water quality card (Score, Primary Contaminant, Compliance Status, Water Source, and EPA PWSID).
   - Provides 1-tap **Copy Summary**, **Share via Android Sheet**, and clean visual preview formatted for screenshots.

3. **Tactile Feedback & Micro-Animations**:
   - Light haptic ticks when switching tabs (dock click or swipe snap).
   - Spring-based tab transition physics providing tactile weight and responsiveness.
   - Smooth expand/collapse animations for dropdowns and filter chips.

### Visual Identity & Theme Tokens

- **Active Tab Indicators**:
  - `WHO` → Brand Cyan (`palette.brand`)
  - `WHERE` → Emerald Safe (`palette.safe`)
  - `WHY` → Amber Caution (`palette.caution`)
  - `WHEN` → Indigo Brand Accent (`palette.brand`)
- **Pager Swipe Physics**: Natural damping spring spec (`spring(dampingRatio = 0.85f, stiffness = 400f)`) to avoid abrupt transitions.
- **Typography & Proportions**: Consistent with the previously refined typography tokens (JetBrains Mono for metrics and PWSIDs, Google Sans / Roboto for body copy).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Jetpack Compose Foundation `HorizontalPager` vs. Custom Swipe Modifier**
  - *Chosen Approach*: Use `androidx.compose.foundation.pager.HorizontalPager` with `rememberPagerState(pageCount = { 4 })`.
  - *Why*: Provides out-of-the-box native touch velocity tracking, fling behavior, accessibility semantics, and seamless synchronization with coroutine animations (`pagerState.animateScrollToPage()`).
  - *Alternatives Considered*: Custom pointer drag gestures were rejected because they require re-implementing nested scrolling and fling math.

- **Decision 2: Synchronization Architecture Between Floating Dock & Pager**
  - *Chosen Approach*: Two-way reactive synchronization using `LaunchedEffect(pagerState.currentPage)` to update the view model's active tab, and `LaunchedEffect(activeTab)` or direct click handlers to animate the pager.
  - *Why*: Guarantees that neither the floating dock nor the swipe gesture gets out of sync, even if the user rapidly switches tabs.

- **Decision 3: Haptic Feedback Strategy**
  - *Chosen Approach*: Use Compose `LocalHapticFeedback.current` with `HapticFeedbackType.TextHandleMove` for tab transitions and `HapticFeedbackType.LongPress` for important actions (issue reporting, bookmarking).
  - *Why*: Zero runtime permission overhead (`VIBRATE` is already standard/install-time), respect for user system vibration settings, and battery friendly.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                      MainActivity                      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │             Search & Utility Switcher            │  │
│  └──────────────────────────────────────────────────┘  │
│                           │                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │             WhoWhereWhyWhenDashboard             │  │
│  │                                                  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │        ActiveFourWHeaderStrip (Share)      │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │                                                  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │          HorizontalPager (4 Pages)         │  │  │
│  │  │                                            │  │  │
│  │  │  Page 0: WHO (Governance & Issues)        │  │  │
│  │  │  Page 1: WHERE (Watershed & Treatment)     │  │  │
│  │  │  Page 2: WHY (Contaminants & Limits)       │  │  │
│  │  │  Page 3: WHEN (Score Timeline & SDWIS)     │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────┘  │
│                           │                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │            FloatingFourWDock (Haptics)           │  │
│  │       WHO   •   WHERE   •   WHY   •   WHEN       │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Component & State Mapping

1. **`WhoWhereWhyWhenDashboard.kt`**:
   - Replace the static `AnimatedContent` tab container with `HorizontalPager(state = pagerState)`.
   - Wire `rememberPagerState(initialPage = activeTab.ordinal, pageCount = { 4 })`.
   - Add bidirectional sync: updating `activeTab` triggers `pagerState.animateScrollToPage()`; dragging the pager triggers `onSelectTab(FourWTab.entries[pagerState.currentPage])`.
   - Trigger `hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)` on page settled changes.

2. **`FloatingFourWDock.kt`**:
   - Add `LocalHapticFeedback` to trigger subtle tactile feedback when the user taps any of the 4 dock icons.
   - Keep active indicator animation smooth and responsive to pager swipe progress.

3. **`H2KnowReleaseAndPrivacyComponents.kt`**:
   - Add a `ReportSnapshotModal` showing a high-fidelity visual summary card formatted for quick screenshots and sharing.
