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
      const byYear = {};
      sorted.forEach((sample) => {
        const year = String(sample.date || "").slice(0, 4);
        if (!/^\d{4}$/.test(year) || byYear[year]) return;
        byYear[year] = sample;
      });
      const history = Object.keys(byYear).sort().map((year) => ({
        year: Number(year),
        value: Math.round(byYear[year].value * 1000) / 1000,
        date: byYear[year].date
      }));
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
        sampleDate: latest.date || null,
        history
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
      const rule = field(row, "violation_code", "VIOLATION_CODE") || field(row, "viol_type_code", "VIOL_TYPE_CODE");
      parsed.push({
        id: `${pwsid}-${field(row, "violation_id", "VIOLATION_ID") || i + 1}`,
        date: date.iso,
        year: date.year,
        contaminant: meta ? meta.plainName : "Compliance item",
        contaminantCode: code,
        rule: rule || "Not listed",
        type,
        status,
        healthText: meta ? meta.healthNote : "Health-effects text for this rule is not in the lead-and-copper guide.",
        description: `${health ? "Health-based standard" : type === "monitoring" ? "Monitoring or reporting" : type === "treatment" ? "Treatment technique" : "Compliance record"}. ${status[0].toUpperCase()}${status.slice(1)}. A compliance-period end date is not treated as returned to compliance.`
      });
    });
    return parsed.sort((a, b) => b.date.localeCompare(a.date));
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

  function normToken(value) {
    return String(value || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
  }

  async function mapPool(items, limit, fn) {
    const out = new Array(items.length);
    let cursor = 0;
    const workers = Math.min(limit, items.length);
    async function worker() {
      while (cursor < items.length) {
        const index = cursor;
        cursor += 1;
        out[index] = await fn(items[index], index);
      }
    }
    await Promise.all(Array.from({ length: workers }, () => worker()));
    return out;
  }

  async function fetchCounty(lat, lon) {
    const resp = await fetch(`https://geo.fcc.gov/api/census/area?lat=${lat}&lon=${lon}&format=json`, { headers: { Accept: "application/json" }, signal: AbortSignal.timeout(12000) });
    if (!resp.ok) return null;
    const body = await resp.json();
    const row = body.results && body.results[0];
    if (!row) return null;
    const name = String(row.county_name || "").replace(/ County$/i, "").trim();
    if (!name) return null;
    return { name, fips: String(row.county_fips || ""), state: row.state_code || "" };
  }

  let adjacencyPromise = null;

  function loadAdjacency() {
    if (!adjacencyPromise) {
      adjacencyPromise = fetch("./counties.json", { signal: AbortSignal.timeout(20000) })
        .then((resp) => {
          if (!resp.ok) throw new Error("adjacency");
          return resp.json();
        })
        .then((raw) => {
          const map = {};
          Object.keys(raw || {}).forEach((fips) => {
            map[fips] = (raw[fips] || []).map((item) => {
              const split = String(item).split(",");
              return { name: split[0], state: split[1] || "" };
            }).filter((item) => item.name && item.state);
          });
          return map;
        })
        .catch((err) => {
          adjacencyPromise = null;
          throw err;
        });
    }
    return adjacencyPromise;
  }

  async function fetchGeoCounty(county, state) {
    const rows = [];
    const enc = encodeURIComponent(county);
    const st = encodeURIComponent(state);
    let start = 0;
    const page = 100;
    while (start < 600) {
      let batch = [];
      try {
        batch = await fetchJson(`${EPA}/GEOGRAPHIC_AREA/COUNTY_SERVED/${enc}/PRIMACY_AGENCY_CODE/${st}/PWS_TYPE_CODE/CWS/PWS_ACTIVITY_CODE/A/rows/${start}:${start + page}/JSON`);
      } catch (_) {
        if (start === 0) throw _;
        break;
      }
      if (!batch.length) break;
      rows.push(...batch);
      if (batch.length < page) break;
      start += page;
    }
    return rows;
  }

  function rememberGeo(index, row, meta) {
    const id = field(row, "pwsid", "PWSID");
    if (!id) return;
    const slot = index[id] || (index[id] = {
      pwsid: id,
      cities: [],
      zips: [],
      county: meta.county,
      state: meta.state,
      home: meta.home
    });
    if (!slot.home && meta.home) slot.home = true;
    const city = field(row, "city_served", "CITY_SERVED");
    const zip = field(row, "zip_code_served", "ZIP_CODE_SERVED");
    if (city && !slot.cities.includes(city)) slot.cities.push(city);
    if (zip && !slot.zips.includes(zip)) slot.zips.push(zip);
    if (!slot.county && meta.county) slot.county = meta.county;
  }

  async function hydrateSystems(ids) {
    const rows = await mapPool(ids, 6, async (id) => {
      for (let attempt = 0; attempt < 2; attempt += 1) {
        try {
          const found = await fetchJson(`${EPA}/WATER_SYSTEM/PWSID/${encodeURIComponent(id)}/JSON`);
          return found[0] || null;
        } catch (_) {
          if (attempt === 1) return null;
        }
      }
      return null;
    });
    return rows.filter(Boolean);
  }

  function summaryFromSystem(obj, extra) {
    const pwsid = field(obj, "pwsid", "PWSID");
    const cityName = field(obj, "city_name", "CITY_NAME");
    const zipCode = field(obj, "zip_code", "ZIP_CODE");
    const address = [field(obj, "address_line1", "ADDRESS_LINE1"), cityName, zipCode].filter(Boolean).join(", ");
    return {
      pwsid,
      name: field(obj, "pws_name", "PWS_NAME") || pwsid,
      city: (extra && extra.city) || cityName,
      state: field(obj, "state_code", "STATE_CODE") || (extra && extra.state) || "",
      zip: (extra && extra.zip) || "",
      population: Number(field(obj, "population_served_count", "POPULATION_SERVED_COUNT")) || 0,
      sourceType: sourceLabel(field(obj, "primary_source_code", "PRIMARY_SOURCE_CODE")),
      county: (extra && extra.county) || "",
      listed: Boolean(extra && extra.listed),
      home: extra ? extra.home !== false : true,
      phone: field(obj, "phone_number", "PHONE_NUMBER"),
      address,
      admin: field(obj, "admin_name", "ADMIN_NAME"),
      connections: Number(field(obj, "service_connections_count", "SERVICE_CONNECTIONS_COUNT")) || 0,
      currentScore: -1,
      isPrimary: false
    };
  }

  function providerRank(provider) {
    if (provider.listed) return 0;
    if (provider.home) return 1;
    return 2;
  }

  async function fetchProvidersByZip(zip) {
    const place = await fetchPlace(zip);
    const county = await fetchCounty(place.lat, place.lon).catch(() => null);
    if (!county) throw new Error("This ZIP could not be matched to a county, so no water system was chosen.");
    place.county = county.name;
    place.fips = county.fips;
    const index = {};
    let homeGeo;
    try {
      homeGeo = await fetchGeoCounty(county.name, place.state);
    } catch (_) {
      throw new Error("The EPA service-area list could not be reached. Try again in a moment.");
    }
    homeGeo.forEach((row) => rememberGeo(index, row, { county: county.name, state: place.state, home: true }));
    const homeIds = Object.keys(index);
    if (!homeIds.length) throw new Error(`No community water system was listed for ${county.name} County, ${place.state}.`);
    const homeSystems = await hydrateSystems(homeIds);
    const homeProviders = homeSystems.map((obj) => {
      const id = field(obj, "pwsid", "PWSID");
      const slot = index[id] || { cities: [], zips: [], county: county.name, home: true };
      const listed = slot.cities.some((city) => normToken(city) === normToken(place.city)) || slot.zips.includes(zip);
      return summaryFromSystem(obj, { city: place.city, state: place.state, zip, county: slot.county || county.name, listed, home: true });
    }).filter((p) => p.pwsid);
    const largest = homeProviders.reduce((max, p) => Math.max(max, p.population || 0), 0);
    const rural = largest < 100000;
    place.rural = rural;
    if (rural && county.fips) {
      try {
        const neighbors = (await loadAdjacency())[county.fips] || [];
        place.neighbors = neighbors.map((n) => n.name);
        for (const neighbor of neighbors) {
          let rows = [];
          try { rows = await fetchGeoCounty(neighbor.name, neighbor.state); }
          catch (_) { continue; }
          rows.forEach((row) => rememberGeo(index, row, { county: neighbor.name, state: neighbor.state, home: false }));
        }
      } catch (_) {
        place.neighbors = [];
      }
    } else {
      place.neighbors = [];
    }
    const extraIds = Object.keys(index).filter((id) => !homeIds.includes(id));
    const extraSystems = extraIds.length ? await hydrateSystems(extraIds) : [];
    const extras = extraSystems.map((obj) => {
      const id = field(obj, "pwsid", "PWSID");
      const slot = index[id] || { cities: [], zips: [], county: "", home: false };
      const listed = slot.cities.some((city) => normToken(city) === normToken(place.city)) || slot.zips.includes(zip);
      return summaryFromSystem(obj, { city: place.city, state: slot.state || place.state, zip, county: slot.county, listed, home: Boolean(slot.home) });
    }).filter((p) => p.pwsid);
    const providers = homeProviders.concat(extras).sort((a, b) => (
      providerRank(a) - providerRank(b) || a.name.localeCompare(b.name)
    ));
    if (!providers.length) throw new Error(`No community water system was listed for ${county.name} County, ${place.state}.`);
    return { place, providers };
  }

  async function searchSystems(state, query) {
    const q = String(query || "").trim();
    if (q.length < 2) return [];
    const compact = q.replace(/\s/g, "").toUpperCase();
    if (/^[A-Z]{2}\d{3,9}$/.test(compact)) {
      const rows = await fetchJson(`${EPA}/WATER_SYSTEM/PWSID/${encodeURIComponent(compact)}/JSON`).catch(() => []);
      return rows.filter(Boolean).map((obj) => summaryFromSystem(obj, { state, home: false, listed: false })).filter((p) => p.pwsid);
    }
    const st = String(state || "").trim().toUpperCase();
    if (!st) return [];
    const rows = await fetchJson(`${EPA}/WATER_SYSTEM/PRIMACY_AGENCY_CODE/${encodeURIComponent(st)}/PWS_TYPE_CODE/CWS/PWS_ACTIVITY_CODE/A/PWS_NAME/CONTAINING/${encodeURIComponent(q)}/rows/0:40/JSON`).catch(() => []);
    return rows.filter(Boolean).map((obj) => summaryFromSystem(obj, { state: st, home: false, listed: false })).filter((p) => p.pwsid);
  }

  function explainersFor(contaminants) {
    const byCode = {};
    (contaminants || []).forEach((c) => { byCode[c.code] = c; });
    return Object.keys(CATALOG).map((code) => {
      const meta = CATALOG[code];
      const hit = byCode[code];
      return {
        code,
        plainName: meta.plainName,
        category: meta.category,
        healthNote: meta.healthNote,
        unit: meta.unit,
        mcl: meta.mcl,
        phg: meta.phg,
        measured: Boolean(hit),
        detected: hit ? hit.detected : null
      };
    });
  }

  async function fetchCountyDroughtTrend(fips) {
    const unavailable = { category: "unknown", trend: "not available", note: "County drought trend is not available." };
    if (!fips) return unavailable;
    const end = new Date();
    const start = new Date(end.getTime() - 80 * 7 * 24 * 60 * 60 * 1000);
    const stamp = (d) => `${d.getMonth() + 1}/${d.getDate()}/${d.getFullYear()}`;
    const url = `https://usdmdataservices.unl.edu/api/CountyStatistics/GetDroughtSeverityStatisticsByAreaPercent?aoi=${encodeURIComponent(fips)}&startdate=${encodeURIComponent(stamp(start))}&enddate=${encodeURIComponent(stamp(end))}&statisticsType=1`;
    try {
      const resp = await fetch(url, { headers: { Accept: "text/csv" }, signal: AbortSignal.timeout(15000) });
      if (!resp.ok) return unavailable;
      const lines = (await resp.text()).trim().split(/\r?\n/).filter(Boolean);
      if (lines.length < 2) return unavailable;
      const header = lines[0].split(",").map((cell) => cell.trim());
      const at = (name) => header.findIndex((cell) => cell.toLowerCase() === name.toLowerCase());
      const rows = lines.slice(1).map((line) => {
        const cols = line.split(",");
        const num = (name) => Number(cols[at(name)]) || 0;
        return { date: cols[at("MapDate")], D0: num("D0"), D1: num("D1"), D2: num("D2"), D3: num("D3"), D4: num("D4") };
      }).filter((row) => row.date);
      if (!rows.length) return unavailable;
      const categoryOf = (row) => ["D4", "D3", "D2", "D1", "D0"].find((key) => row[key] > 0) || "none";
      const latest = rows[rows.length - 1];
      const target = String(Number(latest.date) - 10000);
      let prior = rows[0];
      rows.forEach((row) => { if (row.date <= target) prior = row; });
      const order = { none: 0, D0: 1, D1: 2, D2: 3, D3: 4, D4: 5 };
      const nowCat = categoryOf(latest);
      const thenCat = categoryOf(prior);
      const trend = order[nowCat] > order[thenCat] ? "worse" : (order[nowCat] < order[thenCat] ? "easier" : "steady");
      return {
        category: nowCat,
        trend,
        latestDate: latest.date,
        priorDate: prior.date,
        note: `County statistic, separate from the point on the map. Highest category covering any of the county: ${droughtLabel(nowCat)}. A year earlier it was ${droughtLabel(thenCat)}, so the category is ${trend}.`
      };
    } catch (_) {
      return unavailable;
    }
  }

  async function loadSystemReport(summary, place) {
    const pwsid = summary.pwsid;
    const [sysRows, violRows, lcrRows, sampleRows, geoRows, drought, droughtTrend] = await Promise.all([
      fetchJson(`${EPA}/WATER_SYSTEM/PWSID/${encodeURIComponent(pwsid)}/JSON`).catch(() => null),
      fetchJson(`${EPA}/VIOLATION/PWSID/${encodeURIComponent(pwsid)}/rows/0:200/JSON`).catch(() => null),
      fetchJson(`${EPA}/LCR_SAMPLE_RESULT/PWSID/${encodeURIComponent(pwsid)}/rows/0:300/JSON`).catch(() => null),
      fetchJson(`${EPA}/LCR_SAMPLE/PWSID/${encodeURIComponent(pwsid)}/rows/0:300/JSON`).catch(() => null),
      fetchJson(`${EPA}/GEOGRAPHIC_AREA/PWSID/${encodeURIComponent(pwsid)}/JSON`).catch(() => []),
      place ? fetchDrought(place.lat, place.lon) : { level: "unknown", description: "Drought status could not be loaded." },
      place && place.fips ? fetchCountyDroughtTrend(place.fips) : { category: "unknown", trend: "not available", note: "County drought trend is not available." }
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
    const phone = field(sys, "phone_number", "PHONE_NUMBER") || summary.phone || "";
    const admin = field(sys, "admin_name", "ADMIN_NAME") || summary.admin || "";
    const org = field(sys, "org_name", "ORG_NAME") || "";
    const connections = Number(field(sys, "service_connections_count", "SERVICE_CONNECTIONS_COUNT")) || summary.connections || 0;
    const address = [field(sys, "address_line1", "ADDRESS_LINE1"), field(sys, "city_name", "CITY_NAME"), field(sys, "zip_code", "ZIP_CODE")].filter(Boolean).join(", ") || summary.address || "";
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
      droughtTrend,
      phone: phone || "Not listed",
      admin: admin || "Not listed",
      org: org || "Not listed",
      connections,
      address: address || "Not listed",
      echoUrl: `https://echo.epa.gov/detailed-facility-report?fid=${encodeURIComponent(pwsid)}`,
      ccrNote: "A consumer confidence report link is not in this feed.",
      explainers: explainersFor(contaminants),
      filterGuide: [
        { name: "NSF/ANSI 53", note: "Health-related reduction, including lead, when that contaminant is printed on the certification." },
        { name: "NSF/ANSI 58", note: "Reverse osmosis, used for a broader set of dissolved contaminants." },
        { name: "NSF/ANSI 42", note: "Aesthetic chlorine, taste, and odor. This standard is not a health claim by itself." }
      ],
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

  window.H2KnowData = { ZIP_PRESETS, fmtVal, droughtLabel, fetchProvidersByZip, searchSystems, loadSystemReport };
})();
