/* Product UI for the side-build preview.
   Talks to preview/data.js, which is the live app's EPA / drought / scoring pipeline.
   Lookup history uses the same local keys the current app calls Firebase Firestore.
*/
(function () {
  "use strict";

  const D = window.H2KnowData;
  if (!D) return;

  const HISTORY_KEY = "h2know_cloud_history_v3";
  const ISSUES_KEY = "h2know_cloud_issues_v3";

  const state = {
    isGuest: true,
    place: null,
    providers: [],
    report: null,
    loadingZip: null,
    requestId: 0,
    error: "",
    note: "",
    timeframe: "TEN_YEAR",
    pointIdx: null,
    readingFilter: "ALL",
    expandedCode: null,
    chartCode: null,
    open: new Set(),
    issueFilter: "all",
    history: readStore(HISTORY_KEY),
    issues: readStore(ISSUES_KEY),
    issueDraft: null
  };

  const $ = (id) => document.getElementById(id);
  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function readStore(key) {
    try { return JSON.parse(localStorage.getItem(key) || "[]"); }
    catch (_) { return []; }
  }

  function writeStore() {
    try {
      localStorage.setItem(HISTORY_KEY, JSON.stringify(state.history));
      localStorage.setItem(ISSUES_KEY, JSON.stringify(state.issues));
    } catch (_) {}
  }

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[ch]));
  }

  function displayName(name) {
    const raw = String(name || "").trim();
    if (raw.length < 4 || raw !== raw.toUpperCase()) return raw;
    return raw.toLowerCase().replace(/\b([a-z])/g, (m) => m.toUpperCase())
      .replace(/\bOf\b/g, "of")
      .replace(/\bAnd\b/g, "and")
      .replace(/\bWcid\b/g, "WCID")
      .replace(/\bMud\b/g, "MUD")
      .replace(/\bDel\b/g, "del")
      .replace(/\bDept\b/g, "Dept");
  }

  function formatDay(iso) {
    const d = new Date(String(iso || "") + "T00:00:00");
    if (Number.isNaN(d.getTime())) return iso || "";
    return d.toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" });
  }

  function concern(c) {
    if (c.exceedsMcl) return 1000 + c.detected / (c.mcl || 1);
    if (c.phg === 0 && c.detected > 0) return 200 + Math.min(c.detected, 80);
    if (c.exceedsHealthGoal && c.phg > 0) return 20 + c.detected / c.phg;
    if (c.mcl) return c.detected / c.mcl;
    return 0;
  }

  function byConcern(list) {
    return list.slice().sort((a, b) => concern(b) - concern(a));
  }

  function priorityContaminant(list) {
    return byConcern(list)[0] || null;
  }

  function statusWord(c) {
    if (c.exceedsMcl) return { text: "Over legal limit", cls: "flag-bad" };
    if (c.exceedsHealthGoal) return { text: "Above health goal", cls: "flag-warn" };
    return { text: "Within health goal", cls: "flag-good" };
  }

  function verdict(r) {
    const over = r.contaminants.filter((c) => c.exceedsMcl).length;
    const above = r.contaminants.filter((c) => c.exceedsHealthGoal).length;
    if (r.complianceStatus === "under_enforcement" || over > 0) {
      return "This system needs attention. A reading is over a legal limit, or enforcement is open.";
    }
    if (r.qualityScore >= 80 && r.complianceStatus === "in_compliance") {
      return above
        ? "Within a comfortable range. A few readings sit above health goals."
        : "Within a comfortable range for treated tap water.";
    }
    if (r.qualityScore >= 65) {
      return "Usable. Several readings sit above health goals.";
    }
    return "Worth a closer look. Several readings need attention.";
  }

  function deltaPhrase(points, yearsLabel) {
    if (!points || points.length < 2) return "";
    const delta = points[points.length - 1].qualityScore - points[0].qualityScore;
    if (delta > 0) return `Up ${delta} ${delta === 1 ? "point" : "points"} over ${yearsLabel}.`;
    if (delta < 0) return `Down ${Math.abs(delta)} ${Math.abs(delta) === 1 ? "point" : "points"} over ${yearsLabel}.`;
    return `Unchanged over ${yearsLabel}.`;
  }

  function whyOrigin(cat) {
    if (cat === "Metals") return "This usually comes from older service lines, solder, or household plumbing, rather than the river or aquifer itself.";
    if (cat === "PFAS") return "These are persistent industrial chemicals. They show up from upstream runoff, firefighting foam, or long-lived contamination.";
    if (String(cat).toLowerCase().includes("byproduct")) return "These form when chlorine used for disinfection reacts with natural plant matter in the source water.";
    if (String(cat).toLowerCase().includes("inorganic")) return "This usually comes from fertilizer runoff, septic systems, or minerals already in the aquifer.";
    return "Measured in the utility's routine drinking-water samples.";
  }

  function filterSentence(c) {
    const cat = c.category;
    if (cat === "Metals") return "A carbon-block or reverse-osmosis filter certified to NSF/ANSI 53 or 58.";
    if (cat === "PFAS") return "A dense carbon-block or reverse-osmosis filter certified to NSF/ANSI 53 or 58.";
    if (String(cat).toLowerCase().includes("byproduct")) return "An activated-carbon filter certified to NSF/ANSI 42 and 53.";
    if (String(cat).toLowerCase().includes("inorganic")) return "A reverse-osmosis filter certified to NSF/ANSI 58. A basic carbon pitcher will not remove this.";
    return "A filter certified to NSF/ANSI 53.";
  }

  function complianceLine(r) {
    if (r.complianceStatus === "under_enforcement") return "Under enforcement";
    if (r.complianceStatus === "elevated_concern") return "Elevated monitoring";
    return "In compliance";
  }

  function barModel(c) {
    const scale = Math.max(c.mcl || 0, c.detected, c.phg || 0, 0.0001);
    const fill = Math.max(c.detected > 0 ? 2 : 0, Math.min(100, (c.detected / scale) * 100));
    const goal = c.phg === null ? null : Math.min(100, (c.phg / scale) * 100);
    return { fill, goal };
  }

  function openAttr(id) {
    return state.open.has(id) ? " open" : "";
  }

  function setNote(message, isError) {
    const el = $("formNote");
    state.error = isError ? message : "";
    el.textContent = message || "";
    el.classList.toggle("is-quiet", Boolean(message) && !isError);
  }

  function renderAccount() {
    $("accountBtn").textContent = state.isGuest ? "Sign in" : "Sign out";
  }

  function renderPresets() {
    $("presets").innerHTML = D.ZIP_PRESETS.map((p) => `
      <button type="button" data-action="preset" data-zip="${esc(p.zip)}">
        <span class="preset-zip">${esc(p.zip)}</span>${esc(p.city)}
      </button>
    `).join("");
  }

  function renderHomeExtra() {
    const box = $("homeExtra");
    if (state.isGuest || state.report || state.history.length === 0) {
      box.innerHTML = "";
      return;
    }
    box.innerHTML = `
      <details class="fold" data-panel="recent"${openAttr("recent")}>
        <summary>Recent lookups</summary>
        <ul class="saved">
          ${state.history.slice(0, 6).map((h) => `
            <li class="row-between">
              <button type="button" class="linkish" data-action="open-zip" data-zip="${esc(h.zip)}">${esc(displayName(h.systemName))} · ${esc(h.zip)}</button>
              <span>
                <span class="preset-zip">${esc(h.qualityScore)}</span>
                <button type="button" class="text-btn" data-action="delete-history" data-id="${esc(h.id)}" aria-label="Delete ${esc(displayName(h.systemName))}">Remove</button>
              </span>
            </li>
          `).join("")}
        </ul>
      </details>
    `;
  }

  function readingRow(c) {
    const flag = statusWord(c);
    const bar = barModel(c);
    const fillClass = c.exceedsMcl ? "is-bad" : (c.exceedsHealthGoal ? "is-warn" : "");
    const open = state.expandedCode === c.code;
    const goalLabel = c.phg === null ? "No health goal" : `goal ${D.fmtVal(c.phg)} ${c.unit}`;
    return `
      <div class="reading">
        <button type="button" class="reading-hit" data-action="toggle-reading" data-code="${esc(c.code)}" aria-expanded="${open}">
          <span class="name">${esc(c.plainName)}</span>
          <span class="value">${esc(D.fmtVal(c.detected))} ${esc(c.unit)}</span>
          <span class="cat">${esc(c.category)}</span>
          <span class="goal">${esc(goalLabel)}</span>
          <span class="flag ${flag.cls}">${esc(flag.text)}</span>
          <span class="track" aria-hidden="true">
            <span class="fill ${fillClass}" style="width:${bar.fill.toFixed(1)}%"></span>
            ${bar.goal !== null && bar.goal > 0.5 ? `<i class="goal-mark" style="left:${bar.goal.toFixed(1)}%"></i>` : ""}
          </span>
        </button>
        ${open ? `
          <div class="expand">
            <p>${esc(c.healthNote)}</p>
            <p>${esc(whyOrigin(c.category))}</p>
            <p>Legal limit: ${c.mcl === null ? "none listed" : esc(D.fmtVal(c.mcl) + " " + c.unit)}.</p>
            <button type="button" class="linkish" data-action="flag-reading" data-code="${esc(c.code)}">Report this reading</button>
          </div>
        ` : ""}
      </div>
    `;
  }

  function filteredReadings(r) {
    return r.contaminants.filter((c) => {
      if (state.readingFilter === "EXCEEDS_PHG") return c.exceedsHealthGoal;
      if (state.readingFilter === "PFAS") return c.pfas;
      if (state.readingFilter === "METALS") return String(c.category).toLowerCase() === "metals";
      if (state.readingFilter === "DBP") return String(c.category).toLowerCase().includes("byproduct");
      return true;
    });
  }

  function sparkline(points) {
    const w = 640;
    const h = 84;
    const pad = 6;
    const scores = points.map((p) => p.qualityScore);
    const min = Math.min(...scores) - 4;
    const max = Math.max(...scores) + 4;
    const coords = scores.map((s, i) => {
      const x = pad + (i / Math.max(1, scores.length - 1)) * (w - pad * 2);
      const y = pad + (1 - (s - min) / (max - min || 1)) * (h - pad * 2);
      return [x, y];
    });
    const d = coords.map((p, i) => `${i ? "L" : "M"}${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(" ");
    const last = coords[coords.length - 1];
    return `
      <svg class="spark" viewBox="0 0 ${w} ${h}" role="img" aria-label="Ten-year score trend">
        <path d="${d}" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"></path>
        <circle cx="${last[0].toFixed(1)}" cy="${last[1].toFixed(1)}" r="3.2" fill="currentColor"></circle>
      </svg>
    `;
  }

  function detailChart(points, contaminant, selectedIdx) {
    const W = 640;
    const H = 180;
    const padTop = 16;
    const padBottom = 18;
    const chartH = H - padTop - padBottom;
    const step = points.length > 1 ? W / (points.length - 1) : W;
    const scorePos = points.map((pt, i) => {
      const x = i * step;
      const norm = Math.min(1, Math.max(0, (Math.min(100, Math.max(30, pt.qualityScore)) - 30) / 70));
      return { x, y: padTop + chartH * (1 - norm) };
    });
    const values = points.map((pt) => pt.levels[contaminant.code] ?? contaminant.detected);
    const maxCont = Math.max(...values, (contaminant.mcl || contaminant.phg || 1) * 1.15, 0.01);
    const contPos = values.map((v, i) => {
      const norm = Math.min(0.92, Math.max(0.08, v / maxCont));
      return { x: i * step, y: padTop + chartH * (1 - norm) };
    });
    const scoreD = scorePos.map((p, i) => `${i ? "L" : "M"}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(" ");
    const contD = contPos.map((p, i) => `${i ? "L" : "M"}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(" ");
    const sel = scorePos[selectedIdx];
    return `
      <svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Score and ${esc(contaminant.plainName)} over time">
        ${[0, 0.5, 1].map((frac) => {
          const y = padTop + chartH * frac;
          return `<line x1="0" y1="${y}" x2="${W}" y2="${y}" stroke="rgba(232,230,226,0.1)" stroke-width="1"></line>`;
        }).join("")}
        <path d="${contD}" fill="none" stroke="#cbb892" stroke-width="1.5" stroke-dasharray="5 5" stroke-linecap="round"></path>
        <path d="${scoreD}" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"></path>
        <line x1="${sel.x}" y1="${padTop}" x2="${sel.x}" y2="${padTop + chartH}" stroke="rgba(232,230,226,0.35)" stroke-width="1"></line>
        <circle cx="${sel.x}" cy="${sel.y}" r="4" fill="currentColor"></circle>
        ${points.map((pt, i) => pt.violationCount > 0
          ? `<circle cx="${scorePos[i].x}" cy="${padTop + chartH - 4}" r="3" fill="#c9a09a"></circle>`
          : "").join("")}
      </svg>
    `;
  }

  function renderReport() {
    const r = state.report;
    const host = $("report");
    document.body.classList.toggle("has-report", Boolean(r));
    if (!r) {
      host.innerHTML = "";
      return;
    }

    const ranked = byConcern(r.contaminants);
    const top = ranked.slice(0, 4);
    const focus = (state.chartCode && r.contaminants.find((c) => c.code === state.chartCode)) || priorityContaminant(r.contaminants);
    const seriesKey = state.timeframe;
    const series = r.historicalSeries[seriesKey];
    const points = series.points;
    const safeIdx = state.pointIdx === null ? points.length - 1 : Math.min(points.length - 1, Math.max(0, state.pointIdx));
    const selectedPoint = points[safeIdx];
    const ten = r.historicalSeries.TEN_YEAR.points;
    const openViolations = r.violations.filter((v) => v.status === "open").length;
    const yearsLabel = seriesKey === "ONE_YEAR" ? "the year" : (seriesKey === "FIVE_YEAR" ? "five years" : "ten years");
    const countyName = displayName(r.county);
    const county = countyName ? (/county/i.test(countyName) ? countyName : `${countyName} County`) : "";
    const droughtName = D.droughtLabel(r.droughtLevel);
    const others = state.providers.filter((p) => p.pwsid !== r.pwsid);
    const systemIssues = state.issues.filter((issue) => {
      const mine = issue.pwsid === r.pwsid || issue.zip === r.zip;
      if (!mine) return false;
      if (state.issueFilter === "open") return issue.status !== "resolved";
      if (state.issueFilter === "resolved") return issue.status === "resolved";
      return true;
    });
    const snapshots = state.history.filter((h) => h.pwsid === r.pwsid || h.zip === r.zip);
    const allReadings = filteredReadings(r);
    const filters = [
      ["ALL", "All"],
      ["EXCEEDS_PHG", "Above goal"],
      ["PFAS", "PFAS"],
      ["METALS", "Metals"],
      ["DBP", "Byproducts"]
    ];
    const frames = [
      ["ONE_YEAR", "1 year"],
      ["FIVE_YEAR", "5 years"],
      ["TEN_YEAR", "10 years"]
    ];
    const droughtLevels = ["none", "D0", "D1", "D2", "D3", "D4"];
    const contAtPoint = selectedPoint.levels[focus.code] ?? focus.detected;
    const useCut = r.usage.recommendedGallonsPerDay < r.usage.normalGallonsPerDay;

    host.innerHTML = `
      <article class="report" id="reportTop">
        <header class="score-block">
          <div>
            <p class="place-line">${esc(displayName(r.city))}, ${esc(r.state)} · ${esc(r.zip)}</p>
            <div class="score-row">
              <p class="score"><span class="sr">Water score </span>${esc(r.qualityScore)}<span class="sr"> out of 100</span></p>
              <span class="score-denom" aria-hidden="true">/ 100</span>
            </div>
          </div>
          <div class="score-copy">
            <p class="verdict">${esc(verdict(r))}</p>
            <p class="meta-line">${esc(displayName(r.name))} · ${esc(formatDay(r.lastUpdated))}</p>
            <div class="score-actions">
              <button type="button" class="linkish" data-action="share-report">Share</button>
              ${state.isGuest
                ? `<button type="button" class="linkish" data-action="ask-sign-in">Sign in to save</button>`
                : `<button type="button" class="linkish" data-action="save-snapshot">Save lookup</button>`}
            </div>
            ${state.note ? `<p class="note">${esc(state.note)}</p>` : ""}
          </div>
        </header>

        <div class="stack">
          <section class="card" aria-labelledby="readingsHeading">
            <h2 id="readingsHeading">Contaminants</h2>
            <p class="card-lead">Lab value beside the health goal. Bar length is the lab value against the legal limit.</p>
            ${top.map((c) => readingRow(c)).join("")}
            <details class="fold" data-panel="readings"${openAttr("readings")}>
              <summary>All readings</summary>
              <div class="fold-body">
                <div class="seg" role="group" aria-label="Filter readings">
                  ${filters.map(([key, label]) => `
                    <button type="button" data-action="reading-filter" data-filter="${key}" aria-pressed="${state.readingFilter === key}">${label}</button>
                  `).join("")}
                </div>
                ${allReadings.map((c) => readingRow(c)).join("") || `<p class="more">Nothing in this group.</p>`}
              </div>
            </details>
          </section>

          <div class="pair">
            <section class="card" aria-labelledby="sourceHeading">
              <h2 id="sourceHeading">Source and utility</h2>
              <p class="utility-name">${esc(displayName(r.name))}</p>
              <p class="utility-sub">${esc(r.sourceType)} · ${esc(r.ownerType)}</p>
              <ul class="facts">
                <li><span>Serves</span><span>${esc(r.population.toLocaleString())}</span></li>
                <li><span>Area</span><span>${esc([displayName(r.city), r.state, county].filter(Boolean).join(", "))}</span></li>
                <li><span>EPA ID</span><span>${esc(r.pwsid)}</span></li>
                <li><span>Drought</span><span>${esc(droughtName)}</span></li>
                <li><span>Compliance</span><span>${esc(complianceLine(r))}</span></li>
              </ul>
              <details class="fold" data-panel="source"${openAttr("source")}>
                <summary>Treatment and watershed</summary>
                <div class="fold-body">
                  <p class="prose">${esc(r.sourceDescription)}</p>
                  <ol class="steps">
                    ${r.treatment.map((step) => `
                      <li>
                        <div class="n">0${esc(step.stepNumber)}</div>
                        <h3>${esc(step.title)}</h3>
                        <p class="prose">${esc(step.description)}</p>
                      </li>
                    `).join("")}
                  </ol>
                  <div class="drought" aria-label="Drought scale">
                    ${droughtLevels.map((lvl) => `
                      <span class="${String(r.droughtLevel).toLowerCase() === lvl.toLowerCase() ? "is-on" : ""}">${lvl === "none" ? "None" : esc(lvl)}</span>
                    `).join("")}
                  </div>
                  <p class="prose">${esc(r.droughtDescription)}</p>
                  <p class="prose">${useCut
                    ? `Daily guide: ${esc(r.usage.recommendedGallonsPerDay)} gallons per person, down from ${esc(r.usage.normalGallonsPerDay)} while this drought holds.`
                    : `Daily guide: ${esc(r.usage.recommendedGallonsPerDay)} gallons per person.`}</p>
                </div>
              </details>
            </section>

            <section class="card" aria-labelledby="filterHeading">
              <h2 id="filterHeading">Filter</h2>
              <p class="card-lead">For ${esc(focus.plainName.toLowerCase())}, ${esc(D.fmtVal(focus.detected))} ${esc(focus.unit)}.</p>
              <p class="filter-line">${esc(filterSentence(focus))}</p>
              <details class="fold" data-panel="filter"${openAttr("filter")}>
                <summary>Why this filter</summary>
                <div class="fold-body">
                  <p class="prose">${esc(whyOrigin(focus.category))}</p>
                  <p class="prose">${esc(focus.healthNote)}</p>
                  <button type="button" class="linkish" data-action="flag-reading" data-code="${esc(focus.code)}">Report this reading</button>
                </div>
              </details>
            </section>
          </div>

          <section class="card" aria-labelledby="trendHeading">
            <h2 id="trendHeading">Record</h2>
            <p class="card-lead">${openViolations ? `${openViolations} open compliance ${openViolations === 1 ? "item" : "items"}.` : "No open compliance items in this record."}</p>
            ${sparkline(ten)}
            <p class="trend-line">${esc(deltaPhrase(ten, "ten years"))}</p>
            <details class="fold" data-panel="trend"${openAttr("trend")}>
              <summary>Trend and violations</summary>
              <div class="fold-body">
                <div class="seg" role="group" aria-label="Time span">
                  ${frames.map(([key, label]) => `
                    <button type="button" data-action="timeframe" data-frame="${key}" aria-pressed="${seriesKey === key}">${label}</button>
                  `).join("")}
                </div>
                <div class="chart-wrap">
                  ${detailChart(points, focus, safeIdx)}
                  <div class="axis" style="grid-template-columns: repeat(${points.length}, minmax(0, 1fr))">
                    ${points.map((pt, idx) => {
                      const show = points.length <= 6 || idx === 0 || idx === points.length - 1 || idx === safeIdx || idx === Math.floor(points.length / 2);
                      if (!show) return "<span></span>";
                      return `<button type="button" data-action="chart-point" data-idx="${idx}" aria-pressed="${idx === safeIdx}">${esc(pt.periodLabel)}</button>`;
                    }).join("")}
                  </div>
                </div>
                <div class="readout">
                  <div><span class="k">Score</span><span class="v">${esc(selectedPoint.qualityScore)}</span></div>
                  <div><span class="k">${esc(focus.plainName)}</span><span class="v">${esc(D.fmtVal(contAtPoint))} ${esc(focus.unit)}</span></div>
                  <div><span class="k">Violations</span><span class="v">${selectedPoint.violationCount === 0 ? "None" : esc(selectedPoint.violationCount)}</span></div>
                </div>
                ${selectedPoint.milestoneNote ? `<p class="more">${esc(selectedPoint.milestoneNote)}</p>` : ""}
                <p class="more">${esc(series.supplyIndication)}</p>
                <p class="more">${esc(deltaPhrase(points, yearsLabel))}</p>
                <ol class="log">
                  ${r.violations.map((v) => `
                    <li>
                      <div class="log-top">
                        <h3>${esc(formatDay(v.date))}</h3>
                        <span class="v-status v-${esc(v.status)}">${esc(v.status === "addressed" ? "Addressed" : v.status === "open" ? "Open" : "Resolved")}</span>
                      </div>
                      <p class="prose">${esc(v.contaminant)}</p>
                      <p class="prose">${esc(v.description)}</p>
                    </li>
                  `).join("")}
                </ol>
                <div class="more">
                  ${state.isGuest
                    ? `<p>Sign in to keep a copy of this lookup in Firebase Firestore.</p><button type="button" class="linkish" data-action="ask-sign-in">Sign in</button>`
                    : `
                      <p>Saved copies of this system.</p>
                      <button type="button" class="linkish" data-action="save-snapshot">Save lookup</button>
                      ${snapshots.length === 0 ? `<p>No saved copies yet.</p>` : `
                        <ul class="saved">
                          ${snapshots.slice(0, 6).map((snap) => `
                            <li class="row-between">
                              <span>${esc(snap.createdAtText)} · ${esc(snap.qualityScore)}</span>
                              <button type="button" class="text-btn" data-action="delete-history" data-id="${esc(snap.id)}">Remove</button>
                            </li>
                          `).join("")}
                        </ul>
                      `}
                    `}
                </div>
              </div>
            </details>
          </section>

          ${others.length ? `
            <details class="card fold" data-panel="utilities"${openAttr("utilities")}>
              <summary>Other utilities in ${esc(r.zip)}</summary>
              <ul class="providers">
                ${state.providers.map((p, idx) => `
                  <li class="row-between">
                    <button type="button" class="linkish" data-action="open-provider" data-idx="${idx}">
                      ${esc(displayName(p.name))}${p.pwsid === r.pwsid ? " · showing" : ""}
                    </button>
                    <span class="prose">${esc(p.population.toLocaleString())} · ${esc(p.sourceType)}</span>
                  </li>
                `).join("")}
              </ul>
            </details>
          ` : ""}

          <details class="card fold" data-panel="notes"${openAttr("notes")}>
            <summary>Resident notes</summary>
            <div class="fold-body">
              ${state.isGuest ? `
                <p class="prose">Sign in to file a note about taste, color, pressure, or a reading. Notes are stored with Firebase Firestore.</p>
                <button type="button" class="linkish" data-action="ask-sign-in">Sign in</button>
              ` : `
                <div class="inline-actions">
                  <button type="button" class="linkish" data-action="open-issue">Report an issue</button>
                </div>
                <div class="seg" role="group" aria-label="Note status">
                  ${[["all", "All"], ["open", "Open"], ["resolved", "Resolved"]].map(([key, label]) => `
                    <button type="button" data-action="issue-filter" data-filter="${key}" aria-pressed="${state.issueFilter === key}">${label}</button>
                  `).join("")}
                </div>
                ${systemIssues.length === 0 ? `<p class="prose">No notes for this system yet.</p>` : `
                  <ul class="log">
                    ${systemIssues.map((issue) => `
                      <li>
                        <div class="log-top">
                          <h3>${esc(issue.title)}</h3>
                          <span class="v-status ${issue.status === "resolved" ? "v-resolved" : "v-open"}">${esc(issue.status === "resolved" ? "Resolved" : "Open")}</span>
                        </div>
                        <p class="prose">${esc(issue.category)} · ${esc(issue.affectedCount)} affected</p>
                        <p class="prose">${esc(issue.description)}</p>
                        <div class="inline-actions">
                          ${issue.status !== "resolved" ? `
                            <button type="button" class="linkish" data-action="upvote-issue" data-id="${esc(issue.id)}">Add affected</button>
                            <button type="button" class="linkish" data-action="resolve-issue" data-id="${esc(issue.id)}">Resolve</button>
                          ` : ""}
                          <button type="button" class="linkish" data-action="delete-issue" data-id="${esc(issue.id)}">Remove</button>
                        </div>
                      </li>
                    `).join("")}
                  </ul>
                `}
              `}
            </div>
          </details>
        </div>
      </article>
    `;
  }

  function render() {
    renderAccount();
    renderHomeExtra();
    renderReport();
    $("zipButton").textContent = state.loadingZip ? "Looking up" : "Look up";
    $("zipButton").disabled = Boolean(state.loadingZip);
    $("zipForm").setAttribute("aria-busy", state.loadingZip ? "true" : "false");
  }

  function saveSnapshot(report, announce) {
    if (state.isGuest) {
      if (announce) state.note = "Sign in to save this lookup.";
      return;
    }
    const top = report.contaminants[0];
    const entry = {
      id: "HIST-" + Date.now(),
      pwsid: report.pwsid,
      systemName: report.name,
      zip: report.zip,
      city: report.city,
      state: report.state,
      qualityScore: report.qualityScore,
      fiveYearDelta: report.fiveYearScoreDelta,
      tenYearDelta: report.tenYearScoreDelta,
      exceedsHealthGoalCount: report.contaminants.filter((c) => c.exceedsHealthGoal).length,
      violationCount: report.violations.length,
      primaryWhySummary: top ? `${top.plainName}: ${D.fmtVal(top.detected)} ${top.unit}` : "Within federal limits",
      createdAtText: new Date().toLocaleString("en-US", { month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit" })
    };
    state.history = [entry, ...state.history.filter((h) => h.pwsid !== report.pwsid)].slice(0, 12);
    writeStore();
    if (announce) state.note = "Saved to your lookup history.";
  }

  async function openProvider(summary, requestId) {
    const report = await D.loadSystemReport(summary, state.place);
    if (requestId !== state.requestId) return;
    state.providers = state.providers.map((p) => (
      p.pwsid === report.pwsid ? { ...p, currentScore: report.qualityScore } : p
    ));
    state.report = report;
    state.timeframe = "TEN_YEAR";
    state.pointIdx = null;
    state.readingFilter = "ALL";
    state.expandedCode = null;
    state.chartCode = null;
    state.open = new Set();
    state.note = "";
    state.loadingZip = null;
    setNote("", false);
    if (!state.isGuest) saveSnapshot(report, false);
    $("zipInput").value = report.zip;
    render();
    window.scrollTo({ top: 0, behavior: reducedMotion ? "auto" : "smooth" });
  }

  async function lookup(raw) {
    const clean = String(raw || "").replace(/\D/g, "").slice(0, 5);
    $("zipInput").value = clean;
    if (clean.length !== 5) {
      setNote("Enter a 5-digit U.S. ZIP code.", true);
      return;
    }
    if (state.loadingZip === clean) return;
    const requestId = ++state.requestId;
    state.loadingZip = clean;
    setNote("Checking public water records.", false);
    render();
    try {
      const res = await D.fetchProvidersByZip(clean);
      if (requestId !== state.requestId) return;
      state.place = res.place;
      state.providers = res.providers;
      const primary = res.providers.find((p) => p.isPrimary) || res.providers[0];
      setNote("", false);
      await openProvider(primary, requestId);
    } catch (err) {
      if (requestId !== state.requestId) return;
      state.loadingZip = null;
      setNote(err.message || "Could not look up that ZIP right now.", true);
      render();
    }
  }

  function shareText(r) {
    const focus = priorityContaminant(r.contaminants);
    const ten = r.historicalSeries.TEN_YEAR;
    const delta = ten.points[ten.points.length - 1].qualityScore - ten.points[0].qualityScore;
    const deltaText = delta > 0 ? `+${delta}` : String(delta);
    return [
      `H2Know · ZIP ${r.zip}`,
      `${displayName(r.name)} (${displayName(r.city)}, ${r.state})`,
      `Water score: ${r.qualityScore} / 100`,
      verdict(r),
      `Source: ${r.sourceType}. Serves ${r.population.toLocaleString()}. EPA ID ${r.pwsid}.`,
      focus ? `${focus.plainName}: ${D.fmtVal(focus.detected)} ${focus.unit} (health goal ${focus.phg === null ? "n/a" : D.fmtVal(focus.phg)} ${focus.unit}).` : "",
      `Ten-year score change: ${deltaText}. Compliance records: ${r.violations.length}.`,
      "Public EPA SDWIS and U.S. Drought Monitor records via H2Know. Not affiliated with the EPA. Aegis Civic Labs."
    ].filter(Boolean).join("\n");
  }

  function openDialog(id) {
    const el = $(id);
    el.hidden = false;
    const focus = el.querySelector("button, a, input, textarea");
    if (focus) focus.focus();
  }

  function closeDialog(id) {
    $(id).hidden = true;
  }

  function anyDialogOpen() {
    return ["accountDialog", "privacyDialog", "supportDialog", "shareDialog", "issueDialog"].some((id) => !$(id).hidden);
  }

  const ISSUE_CATEGORIES = ["Taste / Odor", "Discoloration", "Pressure Loss", "Lead / Pipe Concern", "Boil Notice"];
  const ISSUE_SEVERITY = [["low", "Low"], ["medium", "Medium"], ["high", "High"]];

  function renderIssueChoices() {
    const draft = state.issueDraft;
    if (!draft) return;
    $("issueCategories").innerHTML = ISSUE_CATEGORIES.map((cat) => `
      <button type="button" data-action="issue-cat" data-cat="${esc(cat)}" aria-pressed="${draft.category === cat}">${esc(cat)}</button>
    `).join("");
    $("issueSeverity").innerHTML = ISSUE_SEVERITY.map(([key, label]) => `
      <button type="button" data-action="issue-sev" data-sev="${key}" aria-pressed="${draft.severity === key}">${label}</button>
    `).join("");
  }

  function openIssue(contaminant) {
    if (state.isGuest) {
      openDialog("accountDialog");
      return;
    }
    const r = state.report;
    if (!r) return;
    const category = contaminant && contaminant.category === "Metals" ? "Lead / Pipe Concern" : "Taste / Odor";
    state.issueDraft = {
      category,
      severity: contaminant && contaminant.exceedsMcl ? "high" : "medium"
    };
    $("issueContext").textContent = `Saved with Firebase Firestore for ${displayName(r.name)} · ZIP ${r.zip}.`;
    $("issueTitleInput").value = contaminant
      ? `${contaminant.plainName} at ${D.fmtVal(contaminant.detected)} ${contaminant.unit}`
      : "";
    $("issueDescInput").value = contaminant
      ? `${contaminant.name} was ${D.fmtVal(contaminant.detected)} ${contaminant.unit} in the latest comparison for ${displayName(r.name)}.`
      : "";
    renderIssueChoices();
    openDialog("issueDialog");
  }

  function rememberPanels() {
    document.querySelectorAll("#report details[data-panel], #homeExtra details[data-panel]").forEach((el) => {
      const id = el.getAttribute("data-panel");
      if (!id) return;
      if (el.open) state.open.add(id);
      else state.open.delete(id);
    });
  }

  document.body.addEventListener("click", (event) => {
    const backdrop = event.target.classList && event.target.classList.contains("backdrop") ? event.target : null;
    if (backdrop) {
      backdrop.hidden = true;
      return;
    }
    const el = event.target.closest("[data-action]");
    if (!el) return;
    const action = el.getAttribute("data-action");

    if (action === "preset" || action === "open-zip") {
      lookup(el.getAttribute("data-zip"));
      return;
    }
    if (action === "close-dialog") {
      closeDialog(el.getAttribute("data-dialog"));
      return;
    }
    if (action === "open-privacy") { openDialog("privacyDialog"); return; }
    if (action === "open-support") { openDialog("supportDialog"); return; }
    if (action === "ask-sign-in") { openDialog("accountDialog"); return; }
    if (action === "confirm-sign-in") {
      state.isGuest = false;
      closeDialog("accountDialog");
      if (state.report) saveSnapshot(state.report, true);
      render();
      return;
    }
    if (action === "share-site") {
      const text = "H2Know — a 0–100 score for U.S. tap water, from public EPA records. Published independently by Aegis Civic Labs.";
      if (navigator.share) navigator.share({ title: "H2Know", text, url: window.location.href }).catch(() => {});
      else if (navigator.clipboard) navigator.clipboard.writeText(text + "\n" + window.location.href);
      return;
    }
    if (action === "share-report" && state.report) {
      const text = shareText(state.report);
      $("shareText").textContent = text;
      $("copyShareBtn").textContent = "Copy";
      openDialog("shareDialog");
      return;
    }
    if (action === "save-snapshot" && state.report) {
      saveSnapshot(state.report, true);
      rememberPanels();
      state.open.add("trend");
      render();
      return;
    }
    if (action === "delete-history") {
      state.history = state.history.filter((h) => h.id !== el.getAttribute("data-id"));
      writeStore();
      rememberPanels();
      render();
      return;
    }
    if (action === "open-provider") {
      const picked = state.providers[Number(el.getAttribute("data-idx"))];
      if (!picked) return;
      state.requestId += 1;
      state.loadingZip = state.report ? state.report.zip : null;
      setNote("Checking public water records.", false);
      render();
      openProvider(picked, state.requestId);
      return;
    }
    if (action === "toggle-reading") {
      const code = el.getAttribute("data-code");
      state.expandedCode = state.expandedCode === code ? null : code;
      state.chartCode = code;
      rememberPanels();
      if (el.closest("[data-panel='readings']")) state.open.add("readings");
      render();
      return;
    }
    if (action === "reading-filter") {
      state.readingFilter = el.getAttribute("data-filter");
      rememberPanels();
      state.open.add("readings");
      render();
      return;
    }
    if (action === "timeframe") {
      state.timeframe = el.getAttribute("data-frame");
      state.pointIdx = null;
      rememberPanels();
      state.open.add("trend");
      render();
      return;
    }
    if (action === "chart-point") {
      state.pointIdx = Number(el.getAttribute("data-idx"));
      rememberPanels();
      state.open.add("trend");
      render();
      return;
    }
    if (action === "flag-reading") {
      const code = el.getAttribute("data-code");
      const contaminant = state.report && state.report.contaminants.find((c) => c.code === code);
      openIssue(contaminant || null);
      return;
    }
    if (action === "open-issue") { openIssue(null); return; }
    if (action === "issue-cat" && state.issueDraft) {
      state.issueDraft.category = el.getAttribute("data-cat");
      renderIssueChoices();
      return;
    }
    if (action === "issue-sev" && state.issueDraft) {
      state.issueDraft.severity = el.getAttribute("data-sev");
      renderIssueChoices();
      return;
    }
    if (action === "issue-filter") {
      state.issueFilter = el.getAttribute("data-filter");
      rememberPanels();
      state.open.add("notes");
      render();
      return;
    }
    if (action === "upvote-issue") {
      const item = state.issues.find((x) => x.id === el.getAttribute("data-id"));
      if (item) item.affectedCount += 1;
      writeStore();
      rememberPanels();
      state.open.add("notes");
      render();
      return;
    }
    if (action === "resolve-issue") {
      const item = state.issues.find((x) => x.id === el.getAttribute("data-id"));
      if (item) item.status = "resolved";
      writeStore();
      rememberPanels();
      state.open.add("notes");
      render();
      return;
    }
    if (action === "delete-issue") {
      state.issues = state.issues.filter((x) => x.id !== el.getAttribute("data-id"));
      writeStore();
      rememberPanels();
      state.open.add("notes");
      render();
    }
  });

  $("report").addEventListener("toggle", (event) => {
    const panel = event.target.getAttribute && event.target.getAttribute("data-panel");
    if (!panel) return;
    if (event.target.open) state.open.add(panel);
    else state.open.delete(panel);
  }, true);

  $("zipForm").addEventListener("submit", (event) => {
    event.preventDefault();
    lookup($("zipInput").value);
  });

  $("zipInput").addEventListener("input", (event) => {
    const clean = event.target.value.replace(/\D/g, "").slice(0, 5);
    event.target.value = clean;
    if (state.error) setNote("", false);
    if (clean.length === 5) lookup(clean);
  });

  $("accountBtn").addEventListener("click", () => {
    if (state.isGuest) openDialog("accountDialog");
    else {
      state.isGuest = true;
      state.note = "";
      render();
    }
  });

  $("copyShareBtn").addEventListener("click", async () => {
    const text = $("shareText").textContent;
    try {
      if (navigator.clipboard) await navigator.clipboard.writeText(text);
      $("copyShareBtn").textContent = "Copied";
    } catch (_) {
      $("copyShareBtn").textContent = "Copy unavailable";
    }
  });

  $("nativeShareBtn").addEventListener("click", () => {
    const text = $("shareText").textContent;
    if (navigator.share) navigator.share({ title: "H2Know", text }).catch(() => {});
    else if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
      $("copyShareBtn").textContent = "Copied";
    }
  });

  $("submitIssueBtn").addEventListener("click", () => {
    const r = state.report;
    const draft = state.issueDraft;
    if (!r || !draft) return;
    const title = $("issueTitleInput").value.trim() || `${draft.category} in ZIP ${r.zip}`;
    const description = $("issueDescInput").value.trim() || `Resident reported ${draft.category} for ${r.name}.`;
    state.issues.unshift({
      id: "ISS-" + Date.now(),
      pwsid: r.pwsid,
      systemName: r.name,
      zip: r.zip,
      category: draft.category,
      title,
      description,
      status: "open",
      severity: draft.severity,
      affectedCount: 1
    });
    writeStore();
    closeDialog("issueDialog");
    state.note = "Note saved.";
    state.open.add("notes");
    render();
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && anyDialogOpen()) {
      ["accountDialog", "privacyDialog", "supportDialog", "shareDialog", "issueDialog"].forEach(closeDialog);
    }
  });

  renderPresets();
  render();
})();
