/* Public-record pipeline shared in spirit with the Android repository.
   Lead and copper come from EPA LCR tables. Other contaminants are not invented.
   Scores come from open violations and legal-limit exceedances, never from a preset. */
(function () {
  "use strict";

  const EPA = "https://data.epa.gov/efservice";
  const DROUGHT = "https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/US_Drought_Intensity_v1/FeatureServer/3/query";

  const ZIP_PRESETS = [
    { zip: "78701", city: "Austin", state: "TX" },
    { zip: "48502", city: "Flint", state: "MI" },
    { zip: "07102", city: "Newark", state: "NJ" },
    { zip: "39201", city: "Jackson", state: "MS" },
    { zip: "90210", city: "Beverly Hills", state: "CA" }
  ];

  const PLACE_FALLBACK = {
    "78701": { city: "Austin", state: "TX", stateFull: "Texas", lat: 30.2711, lon: -97.7437 },
    "48502": { city: "Flint", state: "MI", stateFull: "Michigan", lat: 43.0125, lon: -83.6875 },
    "07102": { city: "Newark", state: "NJ", stateFull: "New Jersey", lat: 40.7357, lon: -74.1724 },
    "39201": { city: "Jackson", state: "MS", stateFull: "Mississippi", lat: 32.2988, lon: -90.1848 },
    "90210": { city: "Beverly Hills", state: "CA", stateFull: "California", lat: 34.0901, lon: -118.4065 }
  };

  const CATALOG = {
    PB90: { name: "Lead (90th percentile)", plainName: "Lead", unit: "ppb", mcl: 15, phg: 0, category: "Metals", pfas: false, healthNote: "The health goal for lead is zero. The legal comparison is the 15 ppb action level. Lead usually comes from service lines and household plumbing." },
    CU90: { name: "Copper (90th percentile)", plainName: "Copper", unit: "ppm", mcl: 1.3, phg: 0.3, category: "Metals", pfas: false, healthNote: "The health goal used here is 0.3 ppm. The legal comparison is the 1.3 ppm action level." },
    "2950": { name: "Total trihalomethanes", plainName: "Trihalomethanes", unit: "ppb", mcl: 80, phg: 0.8, category: "Disinfection byproducts", pfas: false, healthNote: "A disinfection byproduct. This app does not invent a trihalomethane result." },
    "2456": { name: "Haloacetic acids", plainName: "Haloacetic acids", unit: "ppb", mcl: 60, phg: 0.5, category: "Disinfection byproducts", pfas: false, healthNote: "A disinfection byproduct. This app does not invent a haloacetic acid result." },
    "1040": { name: "Nitrate", plainName: "Nitrate", unit: "ppm", mcl: 10, phg: 1, category: "Inorganic", pfas: false, healthNote: "Nitrate is not in the lead-and-copper table, so it is shown only when a violation names it." },
    "1005": { name: "Arsenic", plainName: "Arsenic", unit: "ppb", mcl: 10, phg: 0.004, category: "Metals", pfas: false, healthNote: "Arsenic is not in the lead-and-copper table, so it is shown only when a violation names it." },
    "2806": { name: "PFOA", plainName: "PFOA", unit: "ppt", mcl: 4, phg: 0, category: "PFAS", pfas: true, healthNote: "The EPA goal for PFOA is zero and the legal limit is 4 ppt. This app does not invent a PFOA result." },
    "2805": { name: "PFOS", plainName: "PFOS", unit: "ppt", mcl: 4, phg: 0, category: "PFAS", pfas: true, healthNote: "The EPA goal for PFOS is zero and the legal limit is 4 ppt. This app does not invent a PFOS result." }
  };

  function fmtVal(v) {
    if (v === null || v === undefined || Number.isNaN(v)) return "—";
    if (v === 0) return "0";
    if (Math.abs(v) < 0.01) return v.toFixed(3);
    if (Math.abs(v) >= 100) return String(Math.round(v));
    return String(Math.round(v * 100) / 100);
  }

  function droughtLabel(level) {
    return {
      none: "No drought",
      D0: "Abnormally dry (D0)",
      D1: "Moderate drought (D1)",
      D2: "Severe drought (D2)",
      D3: "Extreme drought (D3)",
      D4: "Exceptional drought (D4)",
      unknown: "Not available"
    }[level] || level;
  }

  function field(obj, lower, upper) {
    if (!obj) return "";
    const a = obj[lower];
    const b = obj[upper];
    const value = a === undefined || a === null || a === "" ? b : a;
    return value === undefined || value === null ? "" : String(value);
  }

  async function fetchJson(url) {
    const resp = await fetch(url, { headers: { Accept: "application/json" }, signal: AbortSignal.timeout(20000) });
    if (!resp.ok) {
      const err = new Error("The public record could not be loaded.");
      err.failed = true;
      throw err;
    }
    const text = (await resp.text()).trim();
    if (!text) return [];
    if (text.startsWith("[")) return JSON.parse(text);
    if (text.startsWith("{")) return [JSON.parse(text)];
    return [];
  }

  function parseEpaDate(raw) {
    const clean = String(raw || "").trim();
    if (!clean) return null;
    let m = clean.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (m) return { year: Number(m[1]), iso: `${m[1]}-${m[2]}-${m[3]}` };
    m = clean.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})/);
    if (m) {
      const months = { JAN: "01", FEB: "02", MAR: "03", APR: "04", MAY: "05", JUN: "06", JUL: "07", AUG: "08", SEP: "09", OCT: "10", NOV: "11", DEC: "12" };
      const mm = months[m[2].toUpperCase()];
      if (!mm) return null;
      let yr = Number(m[3]);
      if (yr < 100) yr = yr >= 70 ? 1900 + yr : 2000 + yr;
      return { year: yr, iso: `${yr}-${mm}-${String(m[1]).padStart(2, "0")}` };
    }
    m = clean.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
    if (m) return { year: Number(m[3]), iso: `${m[3]}-${String(m[1]).padStart(2, "0")}-${String(m[2]).padStart(2, "0")}` };
    return null;
  }

  function sourceLabel(code) {
    const c = String(code || "").toUpperCase();
    if (c === "SW") return "Surface water";
    if (c === "GW") return "Groundwater";
    if (c === "GU") return "Groundwater under surface influence";
    if (c === "SWP") return "Purchased surface water";
    if (c === "GWP" || c === "GUP") return "Purchased groundwater";
    return "Not listed";
  }

  function ownerLabel(code) {
    const c = String(code || "").toUpperCase();
    if (c === "L") return "Local government";
    if (c === "M") return "Public / municipal";
    if (c === "S") return "State agency";
    if (c === "F") return "Federal government";
    if (c === "P") return "Private utility";
    if (c === "N") return "Tribal authority";
    return "Not listed";
  }

  function normalizeLcr(code, raw, unitRaw) {
    const unit = String(unitRaw || "").toLowerCase().replace(/[µμ]/g, "u").replace(/\s/g, "");
    const isUg = unit.includes("ug") || unit === "ppb";
    const isMg = unit.includes("mg") || unit === "ppm";
    if (code === "PB90") {
      if (isUg) return raw;
      if (isMg) return raw * 1000;
      if (!unit && raw < 1) return raw * 1000;
      if (!unit) return raw;
      return null;
    }
    if (code === "CU90") {
      if (isMg) return raw;
      if (isUg) return raw / 1000;
      if (!unit && raw > 20) return raw / 1000;
      if (!unit) return raw;
      return null;
    }
    return null;
  }

  function contaminantsFrom(results, samples) {
    const dates = {};
    (samples || []).forEach((row) => {
      const id = field(row, "sample_id", "SAMPLE_ID");
      const end = field(row, "sampling_end_date", "SAMPLING_END_DATE");
      if (id && end) dates[id] = end.slice(0, 10);
    });
    const grouped = {};
    (results || []).forEach((row) => {
      const code = field(row, "contaminant_code", "CONTAMINANT_CODE");
      if (code !== "PB90" && code !== "CU90") return;
      const raw = Number(field(row, "sample_measure", "SAMPLE_MEASURE"));
      if (!Number.isFinite(raw)) return;
      const normalized = normalizeLcr(code, raw, field(row, "unit_of_measure", "UNIT_OF_MEASURE"));
      if (normalized === null) return;
      const sign = field(row, "result_sign_code", "RESULT_SIGN_CODE").trim();
      const value = sign === "<" || sign.toUpperCase() === "ND" ? 0 : normalized;
      const id = field(row, "sample_id", "SAMPLE_ID");
      (grouped[code] || (grouped[code] = [])).push({ value, date: dates[id] || "" });
    });
    return Object.keys(grouped).map((code) => {
      const meta = CATALOG[code];
      const sorted = grouped[code].slice().sort((a, b) => (b.date || "").localeCompare(a.date || "") || b.value - a.value);
      const latest = sorted[0];
      const prev = sorted[1];
      const detected = Math.round(latest.value * 1000) / 1000;
      const exceedsMcl = meta.mcl != null && detected > meta.mcl;
      const exceedsHealthGoal = meta.phg != null && detected > meta.phg;
      return {
        code,
        name: meta.name,
        plainName: meta.plainName,
        category: meta.category,
        unit: meta.unit,
        detected,
        mcl: meta.mcl,
        phg: meta.phg,
        exceedsMcl,
        exceedsHealthGoal,
        healthNote: meta.healthNote,
        pfas: meta.pfas,
        reported: true,
        sampleDate: latest.date || null
      };
    });
  }

  function violationsFrom(pwsid, rows) {
    const yearNow = new Date().getFullYear();
    const parsed = [];
    (rows || []).forEach((row, i) => {
      const begin = field(row, "compl_per_begin_date", "COMPL_PER_BEGIN_DATE") || field(row, "non_compl_per_begin_date", "NON_COMPL_PER_BEGIN_DATE");
      const date = parseEpaDate(begin);
      if (!date || date.year < yearNow - 10 || date.year > yearNow) return;
      const rtc = field(row, "rtc_date", "RTC_DATE").trim();
      const statusRaw = field(row, "compliance_status_code", "COMPLIANCE_STATUS_CODE").trim().toUpperCase();
      const resolved = Boolean(rtc) || statusRaw === "R" || statusRaw === "V" || statusRaw.startsWith("RESOL");
      const status = resolved ? "resolved" : (statusRaw.startsWith("A") ? "addressed" : "open");
      const cat = field(row, "violation_category_code", "VIOLATION_CATEGORY_CODE");
      const health = field(row, "is_health_based_ind", "IS_HEALTH_BASED_IND").toUpperCase() === "Y";
      const type = health || /mcl/i.test(cat) ? "health" : (cat.toUpperCase() === "MR" ? "monitoring" : (cat.toUpperCase() === "TT" ? "treatment" : "other"));
      const code = field(row, "contaminant_code", "CONTAMINANT_CODE");
      const meta = CATALOG[code];
      parsed.push({
        id: `${pwsid}-${field(row, "violation_id", "VIOLATION_ID") || i + 1}`,
        date: date.iso,
        year: date.year,
        contaminant: meta ? meta.plainName : "Compliance item",
        type,
        status,
        description: `${health ? "Health-based standard" : type === "monitoring" ? "Monitoring or reporting" : type === "treatment" ? "Treatment technique" : "Compliance record"}. ${status[0].toUpperCase()}${status.slice(1)}. A compliance-period end date is not treated as returned to compliance.`
      });
    });
    return parsed.sort((a, b) => b.date.localeCompare(a.date)).slice(0, 45);
  }

  function scoreSystem(violations, contaminants, violationsLoaded, samplesLoaded) {
    if (!violationsLoaded || !samplesLoaded) {
      return { qualityScore: -1, riskLevel: "unknown", complianceStatus: "unavailable" };
    }
    if (contaminants.length === 0 && violations.length === 0) {
      return { qualityScore: -1, riskLevel: "unknown", complianceStatus: "insufficient_data" };
    }
    const open = violations.filter((v) => v.status === "open");
    const openHealth = open.filter((v) => v.type === "health").length;
    const openOther = open.length - openHealth;
    const aboveLegal = contaminants.filter((c) => c.exceedsMcl).length;
    const penalty = Math.min(54, openHealth * 18) + Math.min(24, openOther * 6) + Math.min(45, aboveLegal * 15);
    const qualityScore = Math.max(0, Math.min(100, 100 - penalty));
    const complianceStatus = openHealth > 0 || aboveLegal > 0 ? "under_enforcement" : (open.length ? "elevated_concern" : "in_compliance");
    const riskLevel = qualityScore < 45 ? "critical" : qualityScore < 65 ? "high" : qualityScore < 80 ? "moderate" : "low";
    return { qualityScore, riskLevel, complianceStatus };
  }

  function seriesFor(violations, startYear, yearNow) {
    const points = [];
    for (let yr = startYear; yr <= yearNow; yr += 1) {
      points.push({
        periodLabel: String(yr),
        qualityScore: -1,
        violationCount: violations.filter((v) => v.year === yr).length,
        levels: {}
      });
    }
    return { points, supplyIndication: "A past water score is not in the public feed. Points are violation counts.", deltaPts: 0 };
  }

  function treatmentFor(sourceType) {
    if (sourceType === "Not listed") {
      return [{ stepNumber: 1, title: "Treatment not listed", description: "The public record did not include a source type, so treatment steps are not shown." }];
    }
    if (/ground/i.test(sourceType)) {
      return [
        { stepNumber: 1, title: "Typical of groundwater", description: "Common steps for well water, not a plant-specific record." },
        { stepNumber: 2, title: "Conditioning", description: "Some systems aerate or soften mineral-heavy groundwater." },
        { stepNumber: 3, title: "Disinfection residual", description: "A disinfectant residual is commonly kept in the pipes." }
      ];
    }
    if (/purchased/i.test(sourceType)) {
      return [
        { stepNumber: 1, title: "Typical of purchased water", description: "Common steps for water bought from a wholesale system, not a plant-specific record." },
        { stepNumber: 2, title: "Residual disinfectant", description: "A chlorine or chloramine residual is commonly kept in the pipes." }
      ];
    }
    return [
      { stepNumber: 1, title: "Typical of surface water", description: "Common steps for river or lake water, not a plant-specific record." },
      { stepNumber: 2, title: "Clarification", description: "Sediment is often settled before filtration." },
      { stepNumber: 3, title: "Filtration and disinfection", description: "Sand or carbon beds and a disinfectant are common before the water leaves the plant." }
    ];
  }

  function withAffiliate(url) {
    const tag = String((window.H2KnowConfig && window.H2KnowConfig.AMAZON_ASSOCIATE_TAG) || "").trim();
    if (!tag) return url;
    return url + (url.includes("?") ? "&" : "?") + "tag=" + encodeURIComponent(tag);
  }

  function productFor(report) {
    const reported = report.contaminants.filter((c) => c.reported);
    const over = reported.filter((c) => c.exceedsMcl).sort((a, b) => (b.detected / (b.mcl || 1)) - (a.detected / (a.mcl || 1)))[0];
    const optional = over ? null : reported.filter((c) => (c.code === "CU90" && c.exceedsHealthGoal) || (c.code === "PB90" && c.detected > 0)).sort((a, b) => b.detected - a.detected)[0];
    const match = over || optional;
    if (!match) return null;
    const query = match.code === "CU90"
      ? "https://www.amazon.com/s?k=NSF+ANSI+53+copper+reduction+drinking+water+filter"
      : "https://www.amazon.com/s?k=NSF+ANSI+53+lead+reduction+drinking+water+filter";
    const reason = over
      ? `${match.plainName} is ${fmtVal(match.detected)} ${match.unit}, over the legal limit of ${fmtVal(match.mcl)} ${match.unit}.`
      : (match.code === "PB90"
        ? `Lead was detected at ${fmtVal(match.detected)} ${match.unit}, under the 15 ppb action level. The health goal is zero. A filter is optional, not required by this sample.`
        : `${match.plainName} is ${fmtVal(match.detected)} ${match.unit}, under the legal limit of ${fmtVal(match.mcl)} ${match.unit} and above the health goal of ${fmtVal(match.phg)} ${match.unit}.`);
    return {
      title: match.code === "CU90" ? "Copper-reduction filter" : "Lead-reduction filter",
      matchReason: reason,
      optional: !over,
      certification: "Look for NSF/ANSI 53 or 58",
      url: withAffiliate(query),
      disclosure: window.H2KnowConfig.AFFILIATE_DISCLOSURE
    };
  }

  async function fetchPlace(zip) {
    try {
      const resp = await fetch(`https://api.zippopotam.us/us/${zip}`, { headers: { Accept: "application/json" }, signal: AbortSignal.timeout(12000) });
      if (!resp.ok) throw new Error("miss");
      const body = await resp.json();
      const p = (body.places || [])[0];
      if (!p) throw new Error("miss");
      return {
        zip,
        city: p["place name"] || "",
        state: p["state abbreviation"] || "",
        stateFull: p.state || "",
        lat: Number(p.latitude) || 0,
        lon: Number(p.longitude) || 0
      };
    } catch (_) {
      const fb = PLACE_FALLBACK[zip];
      if (!fb) {
        const err = new Error(`ZIP code ${zip} was not recognized.`);
        err.failed = false;
        throw err;
      }
      return { zip, ...fb };
    }
  }

  async function fetchDrought(lat, lon) {
    try {
      const url = `${DROUGHT}?geometry=${lon},${lat}&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=dm,period&returnGeometry=false&f=json`;
      const resp = await fetch(url, { headers: { Accept: "application/json" }, signal: AbortSignal.timeout(12000) });
      if (!resp.ok) return { level: "unknown", description: "Drought status could not be loaded. This is not a report of drought conditions." };
      const root = await resp.json();
      const features = root.features || [];
      if (!features.length) return { level: "none", description: "This location is not inside a mapped U.S. Drought Monitor area." };
      const dm = features[0] && features[0].attributes ? features[0].attributes.dm : null;
      if (dm === null || dm === undefined) return { level: "none", description: "This location is not inside a mapped U.S. Drought Monitor area." };
      const idx = Math.round(Number(dm));
      if (idx < 0 || idx > 4) return { level: "none", description: "This location is not inside a mapped U.S. Drought Monitor area." };
      const level = ["D0", "D1", "D2", "D3", "D4"][idx];
      return { level, description: `The U.S. Drought Monitor lists this point as ${droughtLabel(level)}.` };
    } catch (_) {
      return { level: "unknown", description: "Drought status could not be loaded. This is not a report of drought conditions." };
    }
  }

  async function fetchCountyName(lat, lon) {
    const resp = await fetch(`https://geo.fcc.gov/api/census/area?lat=${lat}&lon=${lon}&format=json`, { headers: { Accept: "application/json" }, signal: AbortSignal.timeout(12000) });
    if (!resp.ok) return "";
    const body = await resp.json();
    const name = body.results && body.results[0] && body.results[0].county_name || "";
    return String(name).replace(/ County$/i, "").trim();
  }

  async function fetchProvidersByZip(zip) {
    const place = await fetchPlace(zip);
    const county = await fetchCountyName(place.lat, place.lon).catch(() => "");
    if (!county) throw new Error("This ZIP could not be matched to a county, so no water system was chosen.");
    place.county = county;
    let geo;
    try {
      const enc = encodeURIComponent(county);
      const state = encodeURIComponent(place.state);
      geo = await fetchJson(`${EPA}/GEOGRAPHIC_AREA/COUNTY_SERVED/${enc}/PRIMACY_AGENCY_CODE/${state}/PWS_TYPE_CODE/CWS/PWS_ACTIVITY_CODE/A/rows/0:40/JSON`);
    } catch (_) {
      throw new Error("The EPA service-area list could not be reached. Try again in a moment.");
    }
    const ids = [];
    geo.forEach((row) => {
      const id = field(row, "pwsid", "PWSID");
      if (id && !ids.includes(id)) ids.push(id);
    });
    if (!ids.length) throw new Error(`No community water system was listed for ${county} County, ${place.state}.`);
    const systems = (await Promise.all(ids.slice(0, 24).map(async (id) => {
      try {
        const rows = await fetchJson(`${EPA}/WATER_SYSTEM/PWSID/${encodeURIComponent(id)}/JSON`);
        return rows[0] || null;
      } catch (_) {
        return null;
      }
    }))).filter(Boolean);
    const providers = systems.map((obj) => ({
      pwsid: field(obj, "pwsid", "PWSID"),
      name: field(obj, "pws_name", "PWS_NAME") || field(obj, "pwsid", "PWSID"),
      city: place.city,
      state: field(obj, "state_code", "STATE_CODE") || place.state,
      zip,
      population: Number(field(obj, "population_served_count", "POPULATION_SERVED_COUNT")) || 0,
      sourceType: sourceLabel(field(obj, "primary_source_code", "PRIMARY_SOURCE_CODE")),
      currentScore: -1,
      isPrimary: false
    })).filter((p) => p.pwsid).sort((a, b) => b.population - a.population).slice(0, 8);
    providers.forEach((p, i) => { p.isPrimary = i === 0; });
    if (!providers.length) throw new Error(`No community water system was listed for ${county} County, ${place.state}.`);
    return { place, providers };
  }

  async function loadSystemReport(summary, place) {
    const pwsid = summary.pwsid;
    const [sysRows, violRows, lcrRows, sampleRows, geoRows, drought] = await Promise.all([
      fetchJson(`${EPA}/WATER_SYSTEM/PWSID/${encodeURIComponent(pwsid)}/JSON`).catch(() => null),
      fetchJson(`${EPA}/VIOLATION/PWSID/${encodeURIComponent(pwsid)}/rows/0:120/JSON`).catch(() => null),
      fetchJson(`${EPA}/LCR_SAMPLE_RESULT/PWSID/${encodeURIComponent(pwsid)}/rows/0:80/JSON`).catch(() => null),
      fetchJson(`${EPA}/LCR_SAMPLE/PWSID/${encodeURIComponent(pwsid)}/rows/0:80/JSON`).catch(() => null),
      fetchJson(`${EPA}/GEOGRAPHIC_AREA/PWSID/${encodeURIComponent(pwsid)}/JSON`).catch(() => []),
      place ? fetchDrought(place.lat, place.lon) : { level: "unknown", description: "Drought status could not be loaded." }
    ]);
    if (sysRows === null) throw new Error("The EPA water-system record could not be loaded.");
    const sys = sysRows[0] || {};
    const violationsLoaded = violRows !== null;
    const samplesLoaded = lcrRows !== null;
    const contaminants = samplesLoaded ? contaminantsFrom(lcrRows, sampleRows || []) : [];
    const violations = violationsLoaded ? violationsFrom(pwsid, violRows) : [];
    const scored = scoreSystem(violations, contaminants, violationsLoaded, samplesLoaded);
    const population = Number(field(sys, "population_served_count", "POPULATION_SERVED_COUNT")) || summary.population || 0;
    const sourceType = sourceLabel(field(sys, "primary_source_code", "PRIMARY_SOURCE_CODE")) || summary.sourceType || "Not listed";
    const ownerType = ownerLabel(field(sys, "owner_type_code", "OWNER_TYPE_CODE"));
    let county = "";
    (geoRows || []).some((g) => {
      county = field(g, "county_served", "COUNTY_SERVED");
      return Boolean(county);
    });
    const recordNote = !violationsLoaded || !samplesLoaded
      ? "Part of the EPA record did not load, so this system is not scored."
      : (contaminants.length === 0 && violations.length === 0
        ? "No lead or copper sample and no violation from the last 10 years were in the public record."
        : (contaminants.length === 0
          ? "No lead or copper sample was in the public record. The score uses violation history only."
          : "Samples shown are lead and copper from the EPA lead-and-copper table. Other contaminants are not filled in."));
    const yearNow = new Date().getFullYear();
    const cuts = { D0: 0.9, D1: 0.8, D2: 0.65, D3: 0.5, D4: 0.4 };
    const normal = 90;
    const recommended = Math.round(normal * (cuts[drought.level] || 1));
    const people = population > 0 ? population.toLocaleString("en-US") + " people" : "a population the record does not list";
    const name = field(sys, "pws_name", "PWS_NAME") || summary.name || pwsid;
    const city = (place && place.city) || field(sys, "city_name", "CITY_NAME") || summary.city || "Not listed";
    const state = field(sys, "state_code", "STATE_CODE") || summary.state || (place && place.state) || "";
    const report = {
      pwsid,
      name,
      city,
      state,
      zip: (place && place.zip) || summary.zip || "",
      county,
      population,
      sourceType: sourceType === "Not listed" && summary.sourceType ? summary.sourceType : sourceType,
      ownerType,
      ...scored,
      fiveYearScoreDelta: 0,
      tenYearScoreDelta: 0,
      droughtLevel: drought.level,
      droughtDescription: drought.description,
      contaminants,
      violations,
      recordNote,
      sourceDescription: `${name} serves ${people} in ${city}, ${state}${county ? ` (${county} County)` : ""}. Source: ${sourceType}. Operator: ${ownerType}. This is a community system EPA lists in that county. A ZIP on a water system is often a mailing address, not the pipes at that ZIP. ${recordNote}`,
      treatment: treatmentFor(sourceType),
      usage: { normalGallonsPerDay: normal, recommendedGallonsPerDay: recommended },
      lastUpdated: new Date().toISOString().slice(0, 10),
      historicalSeries: {
        ONE_YEAR: seriesFor(violations, yearNow, yearNow),
        FIVE_YEAR: seriesFor(violations, yearNow - 4, yearNow),
        TEN_YEAR: seriesFor(violations, yearNow - 9, yearNow)
      }
    };
    report.product = productFor(report);
    return report;
  }

  window.H2KnowData = { ZIP_PRESETS, fmtVal, droughtLabel, fetchProvidersByZip, loadSystemReport };
})();
