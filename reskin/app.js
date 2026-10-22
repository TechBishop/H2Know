/* Score-first H2Know reskin. Public records come from data.js.
   Browser saves use reskin-only localStorage keys. Firebase writes go through cloud.js.
   This folder does not share storage keys with the root app. */
(function () {
  "use strict";

  const D = window.H2KnowData;
  if (!D) return;

  const HISTORY_KEY = "h2know_reskin_history_v1";
  const ISSUES_KEY = "h2know_reskin_issues_v1";
  const WATCH_KEY = "h2know_reskin_watch_v1";
  const CHOICE_KEY = "h2know_reskin_provider_choice_v1";
  const DEVICE_KEY = "h2know_reskin_device_save_v1";

  function deviceSaved() {
    try { return localStorage.getItem(DEVICE_KEY) === "1"; }
    catch (_) { return false; }
  }

  const state = {
    isGuest: !deviceSaved(),
    tab: "overview",
    mode: "home",
    place: null,
    providers: [],
    utilityHits: [],
    listQuery: "",
    searchingUtilities: false,
    focusSearch: false,
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
    issueDraft: null,
    cloudUser: null,
    cloudIssues: [],
    cloudHistory: [],
    cloudNote: ""
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

  function readChoices() {
    try { return JSON.parse(localStorage.getItem(CHOICE_KEY) || "{}"); }
    catch (_) { return {}; }
  }

  function rememberChoice(zip, pwsid) {
    const map = readChoices();
    map[zip] = pwsid;
    try { localStorage.setItem(CHOICE_KEY, JSON.stringify(map)); } catch (_) {}
  }

  function icon(body) {
    return `<svg class="ico" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${body}</svg>`;
  }

  const ICONS = {
    drop: icon('<path d="M12 3s6 7 6 11a6 6 0 0 1-12 0c0-4 6-11 6-11z"/>'),
    list: icon('<path d="M8 6h12M8 12h12M8 18h12"/><circle cx="4" cy="6" r="1" fill="currentColor" stroke="none"/><circle cx="4" cy="12" r="1" fill="currentColor" stroke="none"/><circle cx="4" cy="18" r="1" fill="currentColor" stroke="none"/>'),
    pin: icon('<path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z"/><circle cx="12" cy="10" r="2.2"/>'),
    filter: icon('<path d="M4 5h16l-6 7v6l-4 2v-8z"/>'),
    alert: icon('<path d="M12 4 3 19h18z"/><path d="M12 10v4M12 17h.01"/>'),
    book: icon('<path d="M5 5h10a4 4 0 0 1 4 4v11H8a3 3 0 0 0-3 3z"/><path d="M5 5v18"/>')
  };

  function peopleCount(n) {
    return n > 0 ? Number(n).toLocaleString("en-US") : "Not listed";
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
    if (c.phg === 0 && c.detected > 0) return 5;
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
    if (c.phg === 0 && c.detected > 0) return { text: "Health goal is zero", cls: "flag-quiet" };
    if (c.exceedsHealthGoal) return { text: "Above health goal", cls: "flag-warn" };
    return { text: "Within health goal", cls: "flag-good" };
  }

  function verdict(r) {
    if (r.complianceStatus === "unavailable") return "Part of the public record did not load, so there is no score.";
    if (r.complianceStatus === "insufficient_data" || r.qualityScore < 0) return "There is not enough in the public record for a score.";
    const over = r.contaminants.filter((c) => c.exceedsMcl).length;
    if (r.complianceStatus === "under_enforcement" || over > 0) {
      return "This system needs attention. A sample is over a legal limit, or a health-based violation is still open.";
    }
    if (r.complianceStatus === "elevated_concern") {
      return "A compliance record is still open. The reported samples are under their legal limits.";
    }
    if (!r.contaminants.length) {
      return "No open violations in this record. No lead or copper sample was published, so the score is only the violation history.";
    }
    const lead = r.contaminants.find((c) => c.code === "PB90");
    if (lead && lead.detected > 0 && !lead.exceedsMcl) {
      return "Under the legal limits in this record. Lead was detected, and its health goal is zero.";
    }
    return "Under the legal limits in this record.";
  }

  function deltaPhrase() {
    return "A past water score is not in the public feed.";
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
    const user = state.cloudUser;
    $("accountBtn").textContent = user ? "Signed in" : (state.isGuest ? "Sign in" : "On this device");
    const status = $("accountStatus");
    if (status) status.textContent = state.cloudNote || (user ? (user.email || "Signed in with Google.") : "");
    const fbBtn = document.querySelector("[data-action='firebase-sign-in']");
    if (fbBtn) fbBtn.textContent = user ? "Sign out" : "Sign in with Google";
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
    if (state.mode !== "home") {
      box.innerHTML = "";
      return;
    }
    const watches = readStore(WATCH_KEY);
    const browserSnaps = state.history.slice(0, 6);
    const cloudSnaps = state.cloudHistory.slice(0, 6);
    const notes = state.issues.slice(0, 4);
    const cloudNotes = state.cloudIssues.slice(0, 4);
    if (!watches.length && !browserSnaps.length && !cloudSnaps.length && !notes.length && !cloudNotes.length) {
      box.innerHTML = "";
      return;
    }
    box.innerHTML = `
      <section class="home-saves">
        ${watches.length ? `
          <h2>Saved locations</h2>
          <ul class="saved">
            ${watches.slice(0, 6).map((w) => `
              <li class="row-between">
                <button type="button" class="linkish" data-action="open-zip" data-zip="${esc(w.zip)}">${esc(displayName(w.name))} · ${esc(w.zip)}</button>
                <span class="preset-zip">${w.qualityScore < 0 ? "—" : esc(w.qualityScore)}</span>
              </li>
            `).join("")}
          </ul>
        ` : ""}
        ${browserSnaps.length ? `
          <h2>Saved snapshots · this browser</h2>
          <ul class="saved">
            ${browserSnaps.map((h) => `
              <li class="row-between">
                <button type="button" class="linkish" data-action="open-zip" data-zip="${esc(h.zip)}">${esc(displayName(h.systemName))} · ${esc(h.zip)}</button>
                <span>
                  <span class="preset-zip">${h.qualityScore < 0 ? "—" : esc(h.qualityScore)}</span>
                  <button type="button" class="text-btn" data-action="delete-history" data-id="${esc(h.id)}">Remove</button>
                </span>
              </li>
            `).join("")}
          </ul>
        ` : ""}
        ${cloudSnaps.length ? `
          <h2>Saved snapshots · Firebase</h2>
          <ul class="saved">
            ${cloudSnaps.map((h) => `
              <li class="row-between">
                <button type="button" class="linkish" data-action="open-zip" data-zip="${esc(h.zip)}">${esc(displayName(h.systemName))} · ${esc(h.zip)}</button>
                <span>
                  <span class="preset-zip">${esc(h.qualityScore)}</span>
                  <button type="button" class="text-btn" data-action="delete-cloud-history" data-id="${esc(h.id)}">Remove</button>
                </span>
              </li>
            `).join("")}
          </ul>
        ` : ""}
        ${(notes.length || cloudNotes.length) ? `
          <h2>Neighborhood reports</h2>
          <ul class="saved">
            ${cloudNotes.map((issue) => `<li>${esc(issue.title)} · Firebase · ${esc(issue.status)}</li>`).join("")}
            ${notes.map((issue) => `<li>${esc(issue.title)} · this browser · ${esc(issue.status)}</li>`).join("")}
          </ul>
        ` : ""}
      </section>
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
          <span class="value">${esc(D.fmtVal(c.detected))} ${esc(c.unit)}${c.sampleDate ? ` · ${esc(c.sampleDate)}` : ""}</span>
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
    const pad = 8;
    const counts = points.map((p) => p.violationCount || 0);
    const max = Math.max(1, ...counts);
    const coords = counts.map((s, i) => {
      const x = pad + (i / Math.max(1, counts.length - 1)) * (w - pad * 2);
      const y = pad + (1 - s / max) * (h - pad * 2);
      return [x, y];
    });
    const d = coords.map((p, i) => `${i ? "L" : "M"}${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(" ");
    return `
      <svg class="spark" viewBox="0 0 ${w} ${h}" role="img" aria-label="Violation counts by year">
        <path d="${d}" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"></path>
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
    const maxCount = Math.max(1, ...points.map((pt) => pt.violationCount || 0));
    const scorePos = points.map((pt, i) => {
      const x = i * step;
      const norm = (pt.violationCount || 0) / maxCount;
      return { x, y: padTop + chartH * (1 - norm) };
    });
    const scoreD = scorePos.map((p, i) => `${i ? "L" : "M"}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(" ");
    const sel = scorePos[selectedIdx] || scorePos[scorePos.length - 1];
    return `
      <svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="Violation counts by year">
        ${[0, 0.5, 1].map((frac) => {
          const y = padTop + chartH * frac;
          return `<line class="chart-grid" x1="0" y1="${y}" x2="${W}" y2="${y}" stroke="var(--chart-line)" stroke-width="1"></line>`;
        }).join("")}
        <path d="${scoreD}" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"></path>
        <line x1="${sel.x}" y1="${padTop}" x2="${sel.x}" y2="${padTop + chartH}" stroke="var(--chart-line)" stroke-width="1"></line>
        <circle cx="${sel.x}" cy="${sel.y}" r="4" fill="currentColor"></circle>
      </svg>
    `;
  }

  function sampleHistory(sample, label) {
    if (!sample || !sample.history || !sample.history.length) {
      return `<p class="prose">${esc(label)} history is not in this record.</p>`;
    }
    const max = Math.max(...sample.history.map((point) => point.value), 0.0001);
    return `
      <p class="prose">${esc(label)}</p>
      <div class="history-bars">
        ${sample.history.map((point) => `
          <div>
            <span>${esc(point.year)}</span>
            <i style="width:${Math.max(4, (point.value / max) * 100).toFixed(1)}%"></i>
            <span>${esc(D.fmtVal(point.value))} ${esc(sample.unit)}</span>
          </div>
        `).join("")}
      </div>
    `;
  }

  function providerButton(p, action) {
    const where = [p.county, p.state].filter(Boolean).join(", ");
    return `
      <li>
        <button type="button" class="provider-row" data-action="${action}" data-pwsid="${esc(p.pwsid)}">
          <strong>${esc(displayName(p.name))}${p.listed ? '<span class="badge">Listed for this city</span>' : ""}</strong>
          <span class="provider-meta">${esc(p.pwsid)} · ${esc(peopleCount(p.population))} people${where ? ` · ${esc(where)}` : ""}</span>
        </button>
      </li>
    `;
  }

  function visibleProviders() {
    const q = state.listQuery.trim().toLowerCase();
    if (!q) return state.providers;
    return state.providers.filter((p) => p.name.toLowerCase().includes(q) || String(p.pwsid).toLowerCase().includes(q));
  }

  function renderPicker() {
    const host = $("report");
    const place = state.place;
    const rows = visibleProviders();
    const neighborNote = place && place.neighbors && place.neighbors.length
      ? `Neighboring counties are included: ${place.neighbors.join(", ")}.`
      : (place && place.rural
        ? "Neighboring counties could not be loaded, so this list is the home county only."
        : "This county’s largest system serves 100,000 or more people, so the list stays in the home county. Search the state for a system outside it.");
    host.innerHTML = `
      <section class="report picker">
        <h2>${ICONS.list} Choose a water system</h2>
        <p class="picker-lead">${place ? `${esc(place.county || place.city)} County, ${esc(place.state)} · ${esc(place.zip)}` : ""} ${state.providers.length} community systems. Tap one. Nothing is opened until you do.</p>
        <p class="prose">${esc(neighborNote)}</p>
        <div class="utility-search">
          <label class="sr" for="utilitySearch">Find a utility by name or PWSID</label>
          <input id="utilitySearch" type="search" placeholder="Name or PWSID" value="${esc(state.listQuery)}" autocomplete="off" />
          <button type="button" class="text-btn" data-action="search-state">${state.searchingUtilities ? "Searching" : "Search state"}</button>
        </div>
        <ul class="providers">
          ${rows.map((p) => providerButton(p, "open-provider")).join("") || `<li><p class="prose">No system in this list matches that text.</p></li>`}
        </ul>
        ${state.utilityHits.length ? `
          <h2>${ICONS.book} Statewide matches</h2>
          <ul class="providers">
            ${state.utilityHits.map((p) => providerButton(p, "open-hit")).join("")}
          </ul>
        ` : ""}
      </section>
    `;
    if (state.focusSearch) {
      const field = $("utilitySearch");
      if (field) {
        field.focus();
        const end = field.value.length;
        field.setSelectionRange(end, end);
      }
      state.focusSearch = false;
    }
  }

  function renderReport() {
    const r = state.report;
    const host = $("report");
    if (!r) {
      host.innerHTML = "";
      return;
    }

    const ranked = byConcern(r.contaminants);
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
    const systemIssues = state.issues.concat(state.cloudIssues).filter((issue) => {
      const mine = issue.pwsid === r.pwsid || issue.zip === r.zip;
      if (!mine) return false;
      if (state.issueFilter === "open") return issue.status !== "resolved";
      if (state.issueFilter === "resolved") return issue.status === "resolved";
      return true;
    });
    const snapshots = state.history.filter((h) => h.pwsid === r.pwsid || h.zip === r.zip)
      .concat(state.cloudHistory.filter((h) => h.pwsid === r.pwsid || h.zip === r.zip));
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
    const tabs = [
      ["overview", "Overview"],
      ["source", "Source"],
      ["quality", "Quality"],
      ["history", "History"]
    ];
    const tab = tabs.some(([key]) => key === state.tab) ? state.tab : "overview";
    const droughtLevels = ["none", "D0", "D1", "D2", "D3", "D4"];
    const useCut = r.usage.recommendedGallonsPerDay < r.usage.normalGallonsPerDay;
    const watches = readStore(WATCH_KEY);
    const watched = watches.some((w) => w.pwsid === r.pwsid);
    const scoreValue = r.qualityScore < 0 ? 0 : r.qualityScore;
    const lead = r.contaminants.find((c) => c.code === "PB90");
    const copper = r.contaminants.find((c) => c.code === "CU90");
    const overLimit = r.contaminants.filter((c) => c.exceedsMcl).length;
    const aboveGoal = r.contaminants.filter((c) => c.exceedsHealthGoal).length;
    const measuredLine = r.contaminants.length
      ? `${overLimit} over a legal limit · ${aboveGoal} above a health goal, of ${r.contaminants.length} lead or copper ${r.contaminants.length === 1 ? "sample" : "samples"}`
      : "No lead or copper sample in this record";
    const switchLabel = state.providers.length > 1 ? `Switch (${state.providers.length})` : "Switch";

    const overview = `
      <section class="card" aria-labelledby="overviewHeading">
        <h2 id="overviewHeading">${ICONS.drop} Overview</h2>
        <p class="card-lead">${esc(measuredLine)}</p>
        <ul class="facts">
          <li><span>Serves</span><span>${r.population > 0 ? esc(r.population.toLocaleString()) : "Not listed"}</span></li>
          <li><span>Source</span><span>${esc(r.sourceType)}</span></li>
          <li><span>Owner</span><span>${esc(r.ownerType || "Not listed")}</span></li>
          <li><span>Compliance</span><span>${esc(complianceLine(r))}</span></li>
          <li><span>Risk</span><span>${r.qualityScore < 0 ? "Not scored" : esc(r.riskLevel)}</span></li>
          <li><span>Open records</span><span>${openViolations}</span></li>
        </ul>
        <p class="more unavailable-line">Trihalomethanes, haloacetic acids, nitrate, arsenic, PFAS, and fluoride are unavailable in this feed. No number is invented for them.</p>
        <div class="inline-actions">
          <button type="button" class="linkish" data-action="show-tab" data-tab="quality">View Quality</button>
          <button type="button" class="linkish" data-action="toggle-watch">${watched ? "Remove address" : "Save address"}</button>
        </div>
        <p class="prose">${watched ? "This address is saved on this device." : "Save this address to check it again later."}</p>
      </section>
      <section class="card" aria-labelledby="notesHeading">
        <h2 id="notesHeading">Neighborhood reports</h2>
        <p class="card-lead">Taste, color, pressure, and other notes for ${esc(displayName(r.name))}.</p>
        <div class="inline-actions">
          <button type="button" class="linkish" data-action="open-issue">Report Water Quality Issue</button>
        </div>
        <div class="seg" role="group" aria-label="Note status">
          ${[["all", "All"], ["open", "Open"], ["resolved", "Resolved"]].map(([key, label]) => `
            <button type="button" data-action="issue-filter" data-filter="${key}" aria-pressed="${state.issueFilter === key}">${label}</button>
          `).join("")}
        </div>
        ${systemIssues.length === 0 ? `<p class="prose">No reports for this system yet.</p>` : `
          <ul class="log">
            ${systemIssues.map((issue) => `
              <li>
                <div class="log-top">
                  <h3>${esc(issue.title)}</h3>
                  <span class="v-status ${issue.status === "resolved" ? "v-resolved" : "v-open"}">${esc(issue.status === "resolved" ? "Resolved" : "Open")}</span>
                </div>
                <p class="prose">${esc(issue.category)} · ${esc(issue.affectedCount)} affected · ${issue.source === "firebase" ? "Firebase" : "This browser"}</p>
                <p class="prose">${esc(issue.description)}</p>
                <div class="inline-actions">
                  ${issue.status !== "resolved" ? `
                    <button type="button" class="linkish" data-action="upvote-issue" data-id="${esc(issue.id)}" data-source="${esc(issue.source || "browser")}">Add affected</button>
                    <button type="button" class="linkish" data-action="resolve-issue" data-id="${esc(issue.id)}" data-source="${esc(issue.source || "browser")}">Resolve</button>
                  ` : ""}
                  <button type="button" class="linkish" data-action="delete-issue" data-id="${esc(issue.id)}" data-source="${esc(issue.source || "browser")}">Remove</button>
                </div>
              </li>
            `).join("")}
          </ul>
        `}
      </section>
    `;

    const source = `
      <section class="card" aria-labelledby="sourceHeading">
        <h2 id="sourceHeading">${ICONS.pin} Source</h2>
        <p class="utility-name">${esc(displayName(r.name))}</p>
        <p class="utility-sub">${esc(r.sourceType)} · ${esc(r.ownerType)}</p>
        <p class="prose">Service area: ZIP ${esc(r.zip)} · ${esc(displayName(r.city))}, ${esc(r.state)}${county ? ` · ${esc(county)}` : ""}</p>
        <ul class="facts">
          <li><span>EPA ID</span><span>${esc(r.pwsid)}</span></li>
          <li><span>Phone</span><span>${esc(r.phone || "Not listed")}</span></li>
          <li><span>Address</span><span>${esc(r.address || "Not listed")}</span></li>
          <li><span>Admin</span><span>${esc(r.admin || "Not listed")}</span></li>
          <li><span>Connections</span><span>${r.connections > 0 ? esc(r.connections.toLocaleString("en-US")) : "Not listed"}</span></li>
        </ul>
        <p class="prose">${esc(r.sourceDescription)}</p>
        <h3 class="section-label">How water is typically treated</h3>
        <p class="badge-note">General information, not this plant's data</p>
        <p class="prose">${esc((r.treatment && r.treatment.note) || "General information for this kind of source. Not a record from this plant.")}</p>
        <ol class="steps">
          ${((r.treatment && r.treatment.steps) || []).map((step) => `
            <li>
              <div class="n">0${esc(step.stepNumber)}</div>
              <h3>${esc(step.title)}</h3>
              <p class="prose">${esc(step.description)}</p>
            </li>
          `).join("")}
        </ol>
        <h3 class="section-label">Drought</h3>
        <div class="drought" aria-label="Drought scale">
          ${droughtLevels.map((lvl) => `
            <span class="${String(r.droughtLevel).toLowerCase() === lvl.toLowerCase() ? "is-on" : ""}">${lvl === "none" ? "None" : esc(lvl)}</span>
          `).join("")}
        </div>
        <p class="prose">${esc(droughtName)}. ${esc(r.droughtDescription)}</p>
        <p class="prose">${esc(r.droughtTrend && r.droughtTrend.note ? r.droughtTrend.note : "County drought trend is unavailable.")}</p>
        <p class="prose">${useCut
          ? `Planning figure, not a utility meter reading: ${esc(r.usage.recommendedGallonsPerDay)} gallons per person, down from ${esc(r.usage.normalGallonsPerDay)} while this drought category holds.`
          : `Planning figure, not a utility meter reading: ${esc(r.usage.recommendedGallonsPerDay)} gallons per person.`}</p>
        <p class="more unavailable-line">WaterSense fixtures and whole-home product picks are unavailable. There is no catalog from this utility.</p>
        <p class="prose">${esc(r.ccrNote || "A consumer confidence report link is not in this feed.")}</p>
        <p><a class="linkish" href="${esc(r.echoUrl)}" target="_blank" rel="noopener noreferrer">EPA facility record</a></p>
      </section>
    `;

    const emptyFilter = state.readingFilter === "PFAS" || state.readingFilter === "DBP"
      ? "Those results are not in the lead-and-copper table, so nothing is shown."
      : "Nothing in this group.";
    const quality = `
      <section class="card" aria-labelledby="qualityHeading">
        <h2 id="qualityHeading">${ICONS.drop} Quality</h2>
        <p class="card-lead">Lab value beside the health goal. Bar length is the lab value against the legal limit.</p>
        <div class="seg" role="group" aria-label="Filter readings">
          ${filters.map(([key, label]) => `
            <button type="button" data-action="reading-filter" data-filter="${key}" aria-pressed="${state.readingFilter === key}">${label}</button>
          `).join("")}
        </div>
        ${allReadings.map((c) => readingRow(c)).join("") || `<p class="more unavailable-line">${emptyFilter}</p>`}
        ${(r.unmeasured || []).length ? `
          <p class="more unavailable-line">Unavailable in this feed: ${(r.unmeasured || []).map((item) => esc(item.plainName)).join(", ")}. No number is invented for them.</p>
        ` : ""}
        <h3 class="section-label">Lead and copper by year</h3>
        ${sampleHistory(lead, "Lead")}
        ${sampleHistory(copper, "Copper")}
        <p class="more">Each year uses the latest lead or copper sample in the public table. Years with no sample are left out. Other contaminants are not estimated.</p>
      </section>
      <section class="card" aria-labelledby="filterHeading">
        <h2 id="filterHeading">${ICONS.filter} Filter</h2>
        ${r.product ? `
          <p class="card-lead">${esc(r.product.title)}${r.product.optional ? " · optional" : ""}</p>
          <p class="filter-line">${esc(r.product.matchReason)}</p>
          <p class="prose">${esc(r.product.certification)}</p>
          <p class="disclosure">${esc(r.product.disclosure)}</p>
          <p><a class="linkish" href="${esc(r.product.url)}" target="_blank" rel="noopener noreferrer">See filters · affiliate</a></p>
          <p><a class="linkish" href="${esc(r.product.testKitUrl)}" target="_blank" rel="noopener noreferrer">${esc(r.product.testKitLabel)} · affiliate</a></p>
        ` : `
          <p class="card-lead">No filter is indicated by the samples in this record.</p>
          <p class="prose">Nothing reported here is over a legal limit, and lead was not detected. A filter is not suggested from a contaminant this record does not contain.</p>
        `}
        <ul class="steps">
          ${(r.filterGuide || []).map((item) => `
            <li>
              <h3>${esc(item.name)}</h3>
              <p class="prose">${esc(item.note)}</p>
            </li>
          `).join("")}
        </ul>
        ${(r.guidance || []).map((line) => `<p class="prose">${esc(line)}</p>`).join("")}
        <p class="more unavailable-line">Prices, reduction percents, and extra products are not invented. Priced marketplace cards are unavailable.</p>
      </section>
    `;

    const history = `
      <section class="card" aria-labelledby="historyHeading">
        <h2 id="historyHeading">${ICONS.alert} History</h2>
        <p class="card-lead">${openViolations ? `${openViolations} open compliance ${openViolations === 1 ? "item" : "items"}.` : "No open compliance items in this record."} The line counts compliance records, not a score.</p>
        <div class="seg" role="group" aria-label="Time span">
          ${frames.map(([key, label]) => `
            <button type="button" data-action="timeframe" data-frame="${key}" aria-pressed="${seriesKey === key}">${label}</button>
          `).join("")}
        </div>
        ${sparkline(ten)}
        <p class="trend-line unavailable-line">${esc(deltaPhrase())}</p>
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
          <div><span class="k">Score trend</span><span class="v unavailable-line">Unavailable</span></div>
          <div><span class="k">${esc(yearsLabel)}</span><span class="v">${selectedPoint.violationCount === 0 ? "No violations" : esc(selectedPoint.violationCount) + " violations"}</span></div>
        </div>
        <p class="more">A past water score is not in the public feed, so the line counts compliance records.</p>
        ${selectedPoint.milestoneNote ? `<p class="more">${esc(selectedPoint.milestoneNote)}</p>` : ""}
        <p class="more">${esc(series.supplyIndication || "")}</p>
        <h3 class="section-label">Past testing and treatment notices</h3>
        <ol class="log">
          ${r.violations.length ? r.violations.map((v) => `
            <li>
              <div class="log-top">
                <h3>${esc(formatDay(v.date))}</h3>
                <span class="v-status v-${esc(v.status)}">${esc(v.status === "addressed" ? "Addressed" : v.status === "open" ? "Open" : "Resolved")}</span>
              </div>
              <p class="prose">${esc(v.contaminant)} · rule ${esc(v.rule || "Not listed")}</p>
              <p class="prose">${esc(v.description)}</p>
            </li>
          `).join("") : `<li><p class="prose">No violation rows were returned for this system.</p></li>`}
        </ol>
      </section>
      <section class="card" aria-labelledby="savedHeading">
        <h2 id="savedHeading">Saved snapshots</h2>
        <p class="card-lead">A copy of this lookup stays in this browser. Firebase copies are separate and need Google sign-in on an authorized web address.</p>
        <div class="inline-actions">
          ${state.isGuest
            ? `<button type="button" class="linkish" data-action="ask-sign-in">Save on this device</button>`
            : `<button type="button" class="linkish" data-action="save-snapshot">Save lookup</button>`}
          <button type="button" class="linkish" data-action="download-report">Download report</button>
        </div>
        ${snapshots.length === 0 ? `<p class="prose">No saved copies yet.</p>` : `
          <ul class="saved">
            ${snapshots.slice(0, 6).map((snap) => `
              <li class="row-between">
                <span>${esc(snap.createdAtText || "Saved")} · ${snap.qualityScore < 0 ? "—" : esc(snap.qualityScore)} · ${snap.source === "firebase" ? "Firebase" : "This browser"}</span>
                ${snap.source === "firebase"
                  ? `<button type="button" class="text-btn" data-action="delete-cloud-history" data-id="${esc(snap.id)}">Remove</button>`
                  : `<button type="button" class="text-btn" data-action="delete-history" data-id="${esc(snap.id)}">Remove</button>`}
              </li>
            `).join("")}
          </ul>
        `}
      </section>
    `;

    const panels = { overview, source, quality, history };
    host.innerHTML = `
      <article class="report" id="reportTop" data-active-tab="${esc(tab)}">
        <header class="score-block">
          <div class="score-ring" style="--score:${scoreValue}">
            <svg viewBox="0 0 120 120" aria-hidden="true">
              <circle class="ring-track" cx="60" cy="60" r="52"></circle>
              <circle class="ring-value" cx="60" cy="60" r="52"></circle>
            </svg>
            <div class="score-center">
              <p class="score"><span class="sr">Water score </span><span class="score-num" data-count="${r.qualityScore < 0 ? "" : esc(r.qualityScore)}">${r.qualityScore < 0 ? "—" : esc(r.qualityScore)}</span></p>
              <span class="score-denom" aria-hidden="true">/ 100</span>
            </div>
          </div>
          <div class="score-copy">
            <p class="place-line">${esc(displayName(r.city))}, ${esc(r.state)} · ${esc(r.zip)}</p>
            <p class="verdict">${esc(verdict(r))}</p>
            <p class="meta-line">${esc(displayName(r.name))} · ${esc(formatDay(r.lastUpdated))}</p>
            <div class="score-actions">
              <button type="button" class="linkish" data-action="change-provider">${esc(switchLabel)}</button>
              <button type="button" class="linkish" data-action="share-report">Share</button>
              <button type="button" class="linkish" data-action="download-report">Download</button>
              ${state.isGuest
                ? `<button type="button" class="linkish" data-action="ask-sign-in">Save on this device</button>`
                : `<button type="button" class="linkish" data-action="save-snapshot">Save lookup</button>`}
            </div>
            ${state.note ? `<p class="note">${esc(state.note)}</p>` : ""}
          </div>
        </header>
        <div class="tab-row" id="tabBar" role="tablist" aria-label="Report sections">
          ${tabs.map(([key, label]) => `
            <button type="button" role="tab" data-action="show-tab" data-tab="${key}" aria-selected="${tab === key}" id="tab-${key}">${label}</button>
          `).join("")}
        </div>
        <div class="stack tab-panel" id="tabPanel" role="tabpanel" aria-labelledby="tab-${tab}">
          ${panels[tab]}
        </div>
      </article>
    `;
  }

  function animateScore() {
    if (reducedMotion || state.mode !== "report") return;
    const el = document.querySelector(".score-num");
    if (!el) return;
    const raw = el.getAttribute("data-count");
    if (!raw) return;
    const target = Number(raw);
    if (!Number.isFinite(target) || target < 0) return;
    const start = performance.now();
    function tick(now) {
      const t = Math.min(1, (now - start) / 700);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = String(Math.round(target * eased));
      if (t < 1) requestAnimationFrame(tick);
    }
    el.textContent = "0";
    requestAnimationFrame(tick);
  }

  function render() {
    renderAccount();
    renderHomeExtra();
    document.body.classList.toggle("has-report", state.mode !== "home");
    if (state.mode === "pick") renderPicker();
    else renderReport();
    $("zipButton").textContent = state.loadingZip ? "Looking up" : "Look up";
    $("zipButton").disabled = Boolean(state.loadingZip);
    $("zipForm").setAttribute("aria-busy", state.loadingZip ? "true" : "false");
    animateScore();
  }

  function watchChangeNote(report) {
    const watches = readStore(WATCH_KEY);
    const prev = watches.find((w) => w.pwsid === report.pwsid);
    if (!prev) return "";
    const openNow = report.violations.filter((v) => v.status === "open").length;
    const notes = [];
    if (prev.qualityScore >= 0 && report.qualityScore >= 0 && prev.qualityScore !== report.qualityScore) {
      notes.push(`Score changed from ${prev.qualityScore} to ${report.qualityScore} since you saved this address.`);
    }
    if (Number(prev.openViolations) !== openNow) {
      notes.push(`Open violations changed from ${prev.openViolations} to ${openNow}.`);
    }
    if (prev.qualityScore !== report.qualityScore || Number(prev.openViolations) !== openNow) {
      const next = watches.map((w) => w.pwsid === report.pwsid ? { ...w, qualityScore: report.qualityScore, openViolations: openNow } : w);
      try { localStorage.setItem(WATCH_KEY, JSON.stringify(next)); } catch (_) {}
    }
    return notes.join(" ");
  }

  function premiumOn() {
    return Boolean(window.H2KnowConfig && window.H2KnowConfig.PREMIUM_ENABLED === true);
  }

  function savedAddressLimit() {
    if (!premiumOn()) return Infinity;
    const cfg = window.H2KnowConfig || {};
    const free = Number(cfg.FREE_SAVED_ADDRESSES) || 1;
    const paid = Number(cfg.PAID_SAVED_ADDRESSES) || 8;
    return String(cfg.STRIPE_PAYMENT_LINK || "").trim() ? paid : free;
  }

  function toggleWatch(report) {
    const watches = readStore(WATCH_KEY);
    const existing = watches.find((w) => w.pwsid === report.pwsid);
    if (existing) {
      try { localStorage.setItem(WATCH_KEY, JSON.stringify(watches.filter((w) => w.pwsid !== report.pwsid))); } catch (_) {}
      state.note = "Removed from saved addresses.";
      return;
    }
    if (watches.length >= savedAddressLimit()) {
      state.note = "Saved addresses are full. Remove one to add another.";
      return;
    }
    watches.unshift({
      pwsid: report.pwsid,
      zip: report.zip,
      name: report.name,
      qualityScore: report.qualityScore,
      openViolations: report.violations.filter((v) => v.status === "open").length
    });
    try { localStorage.setItem(WATCH_KEY, JSON.stringify(watches)); } catch (_) {}
    state.note = "Saved on this device. Open it again later to see if the score or open violations changed.";
  }

  function downloadReport(report) {
    const text = shareText(report);
    const blob = new Blob([text], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `h2know-${report.zip || "report"}.txt`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  function snapshotEntry(report) {
    const top = report.contaminants[0];
    return {
      id: "HIST-" + Date.now(),
      pwsid: report.pwsid,
      systemName: report.name,
      zip: report.zip,
      city: report.city,
      state: report.state,
      qualityScore: report.qualityScore,
      fiveYearDelta: 0,
      tenYearDelta: 0,
      exceedsHealthGoalCount: report.contaminants.filter((c) => c.exceedsHealthGoal).length,
      violationCount: report.violations.length,
      primaryWhySummary: top ? `${top.plainName}: ${D.fmtVal(top.detected)} ${top.unit}` : "No lead or copper sample in the public record.",
      createdAtText: new Date().toLocaleString("en-US", { month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit" })
    };
  }

  function saveSnapshot(report, announce) {
    const entry = snapshotEntry(report);
    if (state.isGuest) {
      if (announce && !state.cloudUser) state.note = "Save this lookup on this device first.";
    } else {
      state.history = [entry, ...state.history.filter((h) => h.pwsid !== report.pwsid)].slice(0, 12);
      writeStore();
      if (announce) state.note = "Saved in this browser.";
    }
    if (state.cloudUser && window.H2KnowCloud) {
      window.H2KnowCloud.saveHistory(entry).then(async (result) => {
        state.cloudNote = result.ok ? "" : result.reason;
        if (result.ok) await refreshCloud();
        if (announce && result.ok) state.note = "Saved in this browser and in Firebase.";
        if (announce && !result.ok && state.isGuest) state.note = result.reason;
        render();
      });
    }
  }

  function scrollShell(top) {
    const shell = document.querySelector(".shell");
    if (!shell) return;
    shell.scrollTo({ top, behavior: reducedMotion ? "auto" : "smooth" });
  }

  async function openProvider(summary, requestId) {
    const report = await D.loadSystemReport(summary, state.place);
    if (requestId !== state.requestId) return;
    state.providers = state.providers.map((p) => (
      p.pwsid === report.pwsid ? { ...p, currentScore: report.qualityScore } : p
    ));
    state.report = report;
    state.tab = "overview";
    state.timeframe = "TEN_YEAR";
    state.pointIdx = null;
    state.readingFilter = "ALL";
    state.expandedCode = null;
    state.chartCode = null;
    state.open = new Set();
    state.note = watchChangeNote(report);
    state.mode = "report";
    state.loadingZip = null;
    if (report.zip) rememberChoice(report.zip, report.pwsid);
    setNote("", false);
    if (!state.isGuest || state.cloudUser) saveSnapshot(report, false);
    $("zipInput").value = report.zip;
    render();
    scrollShell(0);
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
      state.utilityHits = [];
      state.listQuery = "";
      const remembered = readChoices()[clean];
      const match = remembered && res.providers.find((p) => p.pwsid === remembered);
      setNote("", false);
      if (match) {
        await openProvider(match, requestId);
      } else {
        state.report = null;
        state.mode = "pick";
        state.loadingZip = null;
        render();
      }
    } catch (err) {
      if (requestId !== state.requestId) return;
      state.loadingZip = null;
      setNote(err.message || "Could not look up that ZIP right now.", true);
      render();
    }
  }

  function shareText(r) {
    const focus = priorityContaminant(r.contaminants);
    const open = r.violations.filter((v) => v.status === "open").length;
    return [
      `H2Know · ZIP ${r.zip}`,
      `${displayName(r.name)} (${displayName(r.city)}, ${r.state})`,
      `Water score: ${r.qualityScore < 0 ? "not scored" : r.qualityScore + " / 100"}`,
      verdict(r),
      `Source: ${r.sourceType}. Serves ${r.population > 0 ? r.population.toLocaleString() : "a population that is not listed"}. EPA ID ${r.pwsid}.`,
      focus ? `${focus.plainName}: ${D.fmtVal(focus.detected)} ${focus.unit}.` : "No lead or copper sample in the public record.",
      `Open violations: ${open}. Compliance records: ${r.violations.length}.`,
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
    const r = state.report;
    if (!r) return;
    const category = contaminant && contaminant.category === "Metals" ? "Lead / Pipe Concern" : "Taste / Odor";
    state.issueDraft = {
      category,
      severity: contaminant && contaminant.exceedsMcl ? "high" : "medium"
    };
    $("issueContext").textContent = `${displayName(r.name)} · ZIP ${r.zip}.`;
    const cloudNote = $("issueCloudNote");
    if (cloudNote) {
      cloudNote.textContent = state.cloudUser
        ? "Submit to Firebase stores this report on the signed-in account."
        : "Submit to Firebase needs Google sign-in. If this web address is not authorized, Firebase stays unavailable and nothing is stored there.";
    }
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
    if (action === "firebase-sign-in") {
      const cloud = window.H2KnowCloud;
      if (!cloud) {
        state.cloudNote = "Firebase is unavailable in this build.";
        render();
        return;
      }
      const run = state.cloudUser ? cloud.signOut() : cloud.signIn();
      run.then(async (result) => {
        state.cloudNote = result.ok ? "" : result.reason;
        if (result.ok) await refreshCloud();
        render();
      });
      return;
    }
    if (action === "confirm-sign-in") {
      try { localStorage.setItem(DEVICE_KEY, "1"); } catch (_) {}
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
    if (action === "toggle-watch" && state.report) {
      toggleWatch(state.report);
      rememberPanels();
      state.open.add("saved");
      render();
      return;
    }
    if (action === "download-report" && state.report) {
      downloadReport(state.report);
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
    if (action === "open-provider" || action === "open-hit") {
      const id = el.getAttribute("data-pwsid");
      const pool = action === "open-hit" ? state.utilityHits : state.providers;
      const picked = pool.find((p) => p.pwsid === id);
      if (!picked) return;
      if (action === "open-hit" && !state.providers.some((p) => p.pwsid === picked.pwsid)) {
        state.providers = state.providers.concat(picked);
      }
      state.requestId += 1;
      state.loadingZip = (state.place && state.place.zip) || (state.report && state.report.zip) || null;
      setNote("Checking public water records.", false);
      render();
      openProvider(picked, state.requestId);
      return;
    }
    if (action === "show-tab") {
      state.tab = el.getAttribute("data-tab") || "overview";
      render();
      const bar = document.getElementById("tabBar");
      const shell = document.querySelector(".shell");
      if (bar && shell) {
        const top = shell.scrollTop + bar.getBoundingClientRect().top - shell.getBoundingClientRect().top - 8;
        scrollShell(Math.max(0, top));
      }
      return;
    }
    if (action === "change-provider") {
      if (!state.providers.length) return;
      state.report = null;
      state.mode = "pick";
      state.note = "";
      render();
      return;
    }
    if (action === "search-state") {
      searchUtilities();
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
    if (action === "upvote-issue" || action === "resolve-issue" || action === "delete-issue") {
      const id = el.getAttribute("data-id");
      const source = el.getAttribute("data-source") || "browser";
      if (source === "firebase" && window.H2KnowCloud) {
        const item = state.cloudIssues.find((x) => x.id === id);
        const op = action === "upvote-issue"
          ? window.H2KnowCloud.addAffected(item)
          : (action === "resolve-issue" ? window.H2KnowCloud.resolveIssue(item) : window.H2KnowCloud.deleteIssue(item || { id, source: "firebase" }));
        op.then(async (result) => {
          state.cloudNote = result.ok ? "" : result.reason;
          if (result.ok) await refreshCloud();
          render();
        });
        return;
      }
      if (action === "upvote-issue") {
        const item = state.issues.find((x) => x.id === id);
        if (item) item.affectedCount += 1;
      } else if (action === "resolve-issue") {
        const item = state.issues.find((x) => x.id === id);
        if (item) item.status = "resolved";
      } else {
        state.issues = state.issues.filter((x) => x.id !== id);
      }
      writeStore();
      render();
      return;
    }
    if (action === "delete-cloud-history") {
      const id = el.getAttribute("data-id");
      if (window.H2KnowCloud) {
        window.H2KnowCloud.deleteHistory(id).then(async (result) => {
          state.cloudNote = result.ok ? "" : result.reason;
          if (result.ok) await refreshCloud();
          render();
        });
      }
      return;
    }
  });

  async function searchUtilities() {
    const q = state.listQuery.trim();
    if (!state.place || q.length < 2) {
      setNote("Enter a utility name or a PWSID.", true);
      return;
    }
    state.searchingUtilities = true;
    state.focusSearch = true;
    render();
    try {
      state.utilityHits = await D.searchSystems(state.place.state, q);
      state.searchingUtilities = false;
      setNote(state.utilityHits.length ? "" : `No community system in ${state.place.state} matched that search.`, !state.utilityHits.length);
    } catch (err) {
      state.utilityHits = [];
      state.searchingUtilities = false;
      setNote(err.message || "That search did not complete.", true);
    }
    state.focusSearch = true;
    render();
  }

  $("report").addEventListener("input", (event) => {
    if (event.target.id !== "utilitySearch") return;
    state.listQuery = event.target.value;
    state.focusSearch = true;
    render();
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
    openDialog("accountDialog");
    renderAccount();
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
      source: "browser",
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
    state.note = "Saved in this browser. This is not a Firebase report.";
    render();
  });

  $("submitFirebaseIssueBtn").addEventListener("click", () => {
    const r = state.report;
    const draft = state.issueDraft;
    const cloud = window.H2KnowCloud;
    if (!r || !draft) return;
    if (!cloud || !state.cloudUser) {
      state.cloudNote = "Firebase did not store this. Sign in with Google on an authorized web address first.";
      const note = $("issueCloudNote");
      if (note) note.textContent = state.cloudNote;
      openDialog("accountDialog");
      renderAccount();
      return;
    }
    const title = $("issueTitleInput").value.trim() || `${draft.category} in ZIP ${r.zip}`;
    const description = $("issueDescInput").value.trim() || `Resident reported ${draft.category} for ${r.name}.`;
    cloud.createIssue({
      pwsid: r.pwsid,
      systemName: r.name,
      zip: r.zip,
      category: draft.category,
      title,
      description,
      severity: draft.severity
    }).then(async (result) => {
      if (!result.ok) {
        state.cloudNote = result.reason;
        const note = $("issueCloudNote");
        if (note) note.textContent = result.reason;
        render();
        return;
      }
      await refreshCloud();
      closeDialog("issueDialog");
      state.note = "Report stored in Firebase.";
      state.cloudNote = "";
      render();
    });
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && anyDialogOpen()) {
      ["accountDialog", "privacyDialog", "supportDialog", "shareDialog", "issueDialog"].forEach(closeDialog);
    }
  });

  async function refreshCloud() {
    const cloud = window.H2KnowCloud;
    if (!cloud || !state.cloudUser) {
      state.cloudIssues = [];
      state.cloudHistory = [];
      return;
    }
    try {
      const [issues, history] = await Promise.all([cloud.listIssues(), cloud.listHistory()]);
      state.cloudIssues = issues;
      state.cloudHistory = history;
    } catch (_) {
      state.cloudNote = "Firebase reports could not be loaded.";
    }
  }

  function applyTheme(theme) {
    const next = theme === "light" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", next);
    const btn = $("themeBtn");
    if (btn) {
      const showingDark = next === "dark";
      btn.textContent = showingDark ? "Light" : "Dark";
      btn.setAttribute("aria-pressed", showingDark ? "false" : "true");
      btn.setAttribute("aria-label", showingDark ? "Switch to light theme" : "Switch to dark theme");
    }
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", next === "light" ? "#f6f3ec" : "#0c0e11");
    try { localStorage.setItem("h2know_reskin_theme_v1", next); } catch (_) {}
  }

  $("themeBtn").addEventListener("click", () => {
    const current = document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
    applyTheme(current === "light" ? "dark" : "light");
  });

  applyTheme(document.documentElement.getAttribute("data-theme"));
  renderPresets();
  render();
  if (window.H2KnowCloud) {
    window.H2KnowCloud.onChange(async (user) => {
      state.cloudUser = user || null;
      await refreshCloud();
      render();
    });
  }
  if ("serviceWorker" in navigator) {
    const buildMeta = document.querySelector('meta[name="h2know-build"]');
    const build = buildMeta && buildMeta.getAttribute("content");
    const swUrl = build ? "./sw.js?v=" + encodeURIComponent(build) : "./sw.js";
    navigator.serviceWorker.register(swUrl, { scope: "./", updateViaCache: "none" }).catch(() => {});
  }
})();
