/* H2Know data pipeline for the side-build preview.
   Copied from the live app's index.html so this folder can ship
   without editing the page Argon is changing. Behavior matches the
   EPA SDWIS, Drought Monitor, and scoring path in that file.
*/
(function () {
  "use strict";

  const ZIP_PRESETS = [
    { zip: "78701", city: "Austin", state: "TX" },
    { zip: "48502", city: "Flint", state: "MI" },
    { zip: "07102", city: "Newark", state: "NJ" },
    { zip: "39201", city: "Jackson", state: "MS" },
    { zip: "90210", city: "Beverly Hills", state: "CA" }
  ];

  const CONTAMINANT_CATALOG = {
    PB90: { code: "PB90", name: "Lead (90th percentile)", plainName: "Lead", unit: "ppb", unitLong: "parts per billion (ppb)", mcl: 15.0, phg: 0.0, category: "Metals", pfas: false, healthNote: "Health experts treat lead as unsafe at any level (0 ppb Public Health Goal), especially for infants, children, and expecting households. Primarily enters water from aging service lines and household plumbing." },
    "2950": { code: "2950", name: "Total Trihalomethanes (TTHMs)", plainName: "Chlorine Byproducts (TTHMs)", unit: "ppb", unitLong: "parts per billion (ppb)", mcl: 80.0, phg: 0.8, category: "Disinfection byproducts", pfas: false, healthNote: "Formed when chlorine disinfectant reacts with natural organic matter in surface water. Warmer summer months typically drive seasonal spikes." },
    "2456": { code: "2456", name: "Haloacetic Acids (HAA5)", plainName: "Haloacetic Acids (HAA5)", unit: "ppb", unitLong: "parts per billion (ppb)", mcl: 60.0, phg: 0.5, category: "Disinfection byproducts", pfas: false, healthNote: "Disinfection byproduct class formed alongside TTHMs during chlorination. Activated carbon filtration significantly reduces tap levels." },
    "2806": { code: "2806", name: "PFOA (Perfluorooctanoic Acid)", plainName: "PFOA (Forever Chemical)", unit: "ppt", unitLong: "parts per trillion (ppt)", mcl: 4.0, phg: 0.007, category: "PFAS", pfas: true, healthNote: "Persistent 'forever chemical' newly regulated at 4 ppt federal MCL. Granular activated carbon (NSF 53) or reverse osmosis (NSF 58) is recommended." },
    "2805": { code: "2805", name: "PFOS (Perfluorooctanesulfonic Acid)", plainName: "PFOS (Forever Chemical)", unit: "ppt", unitLong: "parts per trillion (ppt)", mcl: 4.0, phg: 0.02, category: "PFAS", pfas: true, healthNote: "Persistent fluorinated compound bioaccumulating over time. Newly mandated EPA monitoring has surfaced low-ppt detections nationwide." },
    "1040": { code: "1040", name: "Nitrate (as N)", plainName: "Nitrate (Fertilizer Runoff)", unit: "ppm", unitLong: "parts per million (ppm)", mcl: 10.0, phg: 1.0, category: "Inorganic", pfas: false, healthNote: "Agricultural runoff and watershed fertilizer can elevate nitrate in spring. Infants under 6 months are highly vulnerable to methemoglobinemia." },
    CU90: { code: "CU90", name: "Copper (90th percentile)", plainName: "Copper", unit: "ppm", unitLong: "parts per million (ppm)", mcl: 1.3, phg: 0.3, category: "Metals", pfas: false, healthNote: "Keep copper as low as practical (0.3 ppm health benchmark vs 1.3 ppm EPA Action Level) for infants and sensitive households to prevent gastrointestinal distress and liver strain." },
    "1005": { code: "1005", name: "Arsenic", plainName: "Arsenic", unit: "ppb", unitLong: "parts per billion (ppb)", mcl: 10.0, phg: 0.004, category: "Metals", pfas: false, healthNote: "Naturally occurring trace metalloid in bedrock aquifers that concentrates during multi-year drought drawdown." },
    "1025": { code: "1025", name: "Fluoride", plainName: "Fluoride", unit: "ppm", unitLong: "parts per million (ppm)", mcl: 4.0, phg: 0.7, category: "Inorganic", pfas: false, healthNote: "Public health dental guidance centers near 0.7 ppm for cavity prevention while avoiding dental fluorosis." }
  };

  const PRESET_SYSTEMS = {
    "78701": [
      { pwsid: "TX2270001", name: "AUSTIN WATER", city: "Austin", state: "TX", zip: "78701", county: "Travis", population: 1045200, sourceType: "Surface water", isPrimary: true, currentScore: 84, fiveYearDeltaPts: 2 },
      { pwsid: "TX2270049", name: "TRAVIS COUNTY WCID 10", city: "Austin", state: "TX", zip: "78701", county: "Travis", population: 18450, sourceType: "Purchased surface water", isPrimary: false, currentScore: 88, fiveYearDeltaPts: 4 }
    ],
    "48502": [
      { pwsid: "MI0002310", name: "FLINT, CITY OF", city: "Flint", state: "MI", zip: "48502", county: "Genesee", population: 81252, sourceType: "Purchased surface water", isPrimary: true, currentScore: 76, fiveYearDeltaPts: 14 },
      { pwsid: "MI0002635", name: "GENESEE COUNTY DRAIN COMM - WATER", city: "Flint", state: "MI", zip: "48502", county: "Genesee", population: 142000, sourceType: "Surface water", isPrimary: false, currentScore: 86, fiveYearDeltaPts: 3 }
    ],
    "07102": [
      { pwsid: "NJ0714001", name: "NEWARK WATER DEPARTMENT", city: "Newark", state: "NJ", zip: "07102", county: "Essex", population: 311549, sourceType: "Surface water", isPrimary: true, currentScore: 82, fiveYearDeltaPts: 19 }
    ],
    "39201": [
      { pwsid: "MS0250008", name: "CITY OF JACKSON WATER SYSTEM", city: "Jackson", state: "MS", zip: "39201", county: "Hinds", population: 153701, sourceType: "Surface water", isPrimary: true, currentScore: 61, fiveYearDeltaPts: 8 }
    ],
    "90210": [
      { pwsid: "CA1910003", name: "BEVERLY HILLS PUBLIC WORKS", city: "Beverly Hills", state: "CA", zip: "90210", county: "Los Angeles", population: 42180, sourceType: "Purchased surface water", isPrimary: true, currentScore: 86, fiveYearDeltaPts: 1 },
      { pwsid: "CA1910067", name: "LOS ANGELES DEPT OF WATER AND POWER", city: "Los Angeles", state: "CA", zip: "90210", county: "Los Angeles", population: 3898747, sourceType: "Surface water", isPrimary: false, currentScore: 85, fiveYearDeltaPts: 2 }
    ]
  };
  function fmtVal(v) {
    if (v === null || v === undefined || Number.isNaN(v)) return "—";
    if (v === 0) return "0";
    if (Math.abs(v) < 0.01) return Number(v).toFixed(3);
    if (Math.abs(v) >= 100) return Math.round(v).toString();
    return Number(v).toFixed(2).replace(/\.?0+$/, "");
  }

  function hashStr(str) {
    let h = 0;
    for (let i = 0; i < str.length; i++) h = ((h << 5) - h + str.charCodeAt(i)) | 0;
    return Math.abs(h);
  }

  /* ==========================================================================
     REPORT SYNTHESIS & LIVE EPA/USDM PIPELINE (1:1 port of WaterRepository.kt)
     ========================================================================== */
  const systemReportCache = {};

  function sourceLabel(code) {
    const c = String(code || "SW").trim().toUpperCase();
    if (c === "SW") return "Surface water";
    if (c === "GW") return "Groundwater";
    if (c === "GU") return "Groundwater (surface influence)";
    if (c === "SWP") return "Purchased surface water";
    if (c === "GWP" || c === "GUP") return "Purchased groundwater";
    return "Surface water";
  }

  function ownerLabel(code) {
    const c = String(code || "L").trim().toUpperCase();
    if (c === "L") return "Local government";
    if (c === "M") return "Public / municipal";
    if (c === "S") return "State agency";
    if (c === "F") return "Federal government";
    if (c === "P") return "Private utility";
    if (c === "N") return "Tribal authority";
    return "Public utility";
  }

  function droughtLabel(level) {
    switch (String(level || "").toUpperCase()) {
      case "D0": return "Abnormally Dry (D0)";
      case "D1": return "Moderate Drought (D1)";
      case "D2": return "Severe Drought (D2)";
      case "D3": return "Extreme Drought (D3)";
      case "D4": return "Exceptional Drought (D4)";
      default: return "No Active Drought";
    }
  }

  function parseEpaDate(raw) {
    const clean = String(raw || "").trim();
    if (!clean) return null;
    const isoMatch = clean.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (isoMatch) {
      const yr = parseInt(isoMatch[1], 10);
      if (!Number.isNaN(yr)) return { year: yr, isoDate: `${isoMatch[1]}-${isoMatch[2]}-${isoMatch[3]}` };
    }
    const oracleMatch = clean.match(/^(\d{1,2})-([A-Za-z]{3})-(\d{2,4})/);
    if (oracleMatch) {
      const dd = oracleMatch[1].padStart(2, "0");
      const monMap = { JAN: "01", FEB: "02", MAR: "03", APR: "04", MAY: "05", JUN: "06", JUL: "07", AUG: "08", SEP: "09", OCT: "10", NOV: "11", DEC: "12" };
      const mm = monMap[oracleMatch[2].toUpperCase()];
      if (!mm) return null;
      const rawYr = parseInt(oracleMatch[3], 10);
      if (Number.isNaN(rawYr)) return null;
      const yr = rawYr >= 100 ? rawYr : (rawYr >= 70 ? 1900 + rawYr : 2000 + rawYr);
      return { year: yr, isoDate: `${yr}-${mm}-${dd}` };
    }
    const slashMatch = clean.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
    if (slashMatch) {
      const mm = slashMatch[1].padStart(2, "0");
      const dd = slashMatch[2].padStart(2, "0");
      const yr = parseInt(slashMatch[3], 10);
      if (!Number.isNaN(yr)) return { year: yr, isoDate: `${yr}-${mm}-${dd}` };
    }
    return null;
  }

  async function fetchJsonArraySafe(url) {
    try {
      const resp = await fetch(url, { headers: { Accept: "application/json" } });
      if (!resp.ok) return [];
      const data = await resp.json();
      if (Array.isArray(data)) return data;
      if (data && typeof data === "object") return [data];
      return [];
    } catch (_) {
      return [];
    }
  }

  async function fetchDroughtSafe(lat, lon, stateCode) {
    if (lat && lon) {
      try {
        const url = `https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/US_Drought_Intensity_v1/FeatureServer/3/query?geometry=${lon},${lat}&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&outFields=dm,period&returnGeometry=false&f=json`;
        const resp = await fetch(url, { headers: { Accept: "application/json" } });
        if (resp.ok) {
          const root = await resp.json();
          const attr = root && root.features && root.features[0] && root.features[0].attributes;
          if (attr && attr.dm !== null && attr.dm !== undefined) {
            const dm = Math.min(4, Math.max(0, Math.round(Number(attr.dm))));
            const level = ["D0", "D1", "D2", "D3", "D4"][dm];
            return {
              level,
              desc: `The U.S. Drought Monitor currently lists this area in ${droughtLabel(level)}. Reservoir mineral concentration and outdoor watering guidance apply.`
            };
          }
        }
      } catch (_) {}
    }
    const st = String(stateCode || "").toUpperCase();
    if (["TX", "NM", "AZ"].includes(st)) {
      return { level: "D1", desc: "The U.S. Drought Monitor lists parts of this watershed in Moderate Drought (D1). Seasonal conservation targets apply." };
    }
    if (["CA", "NV", "UT"].includes(st)) {
      return { level: "D0", desc: "The U.S. Drought Monitor lists this area as Abnormally Dry (D0). Mindful outdoor irrigation is advised." };
    }
    return { level: "none", desc: "No active drought reported by the U.S. Drought Monitor for this location." };
  }

  function buildViolations10Y(pwsid, violArr) {
    const currentYear = 2026;
    const minYear = currentYear - 10;
    const parsed = [];
    for (let i = 0; i < violArr.length; i++) {
      const r = violArr[i];
      if (!r) continue;
      const beginDate = r.compl_per_begin_date || r.COMPL_PER_BEGIN_DATE || r.non_compl_per_begin_date || r.NON_COMPL_PER_BEGIN_DATE || "";
      const parsedDate = parseEpaDate(beginDate);
      if (!parsedDate) continue;
      const yr = parsedDate.year;
      const isoDate = parsedDate.isoDate;
      if (yr < minYear || yr > currentYear) continue;
      const vid = r.violation_id || r.VIOLATION_ID || String(i + 1);
      const contCodeRaw = String(r.contaminant_code || r.CONTAMINANT_CODE || "").trim();
      const contCode = contCodeRaw ? contCodeRaw : null;
      const catCode = String(r.violation_category_code || r.VIOLATION_CATEGORY_CODE || "MR");
      const isHealth = String(r.is_health_based_ind || r.IS_HEALTH_BASED_IND || "N") === "Y";
      const rtcDate = String(r.rtc_date || r.RTC_DATE || r.compl_per_end_date || r.COMPL_PER_END_DATE || "").trim();
      const statusRaw = String(r.compliance_status_code || r.COMPLIANCE_STATUS_CODE || r.violation_status || r.VIOLATION_STATUS || "R").trim().toUpperCase();
      let status = "resolved";
      if (rtcDate.length > 0 || yr < 2024) {
        status = "resolved";
      } else if (statusRaw.startsWith("A")) {
        status = "addressed";
      } else if (statusRaw === "O" || statusRaw === "UNADDRESSED") {
        status = "open";
      }
      let vType = "other";
      if (isHealth || catCode.toUpperCase().includes("MCL")) vType = "health";
      else if (catCode.toUpperCase() === "MR") vType = "monitoring";
      else if (catCode.toUpperCase() === "TT") vType = "treatment";

      const contMeta = contCode ? CONTAMINANT_CATALOG[contCode] : null;
      parsed.push({
        id: `${pwsid}-${vid}`,
        date: isoDate,
        year: yr,
        contaminantCode: contCode,
        contaminant: contMeta ? contMeta.name : (contCode ? `Regulated Parameter ${contCode}` : "Disinfectant / Monitoring Rule"),
        type: vType,
        status,
        severity: isHealth ? "serious" : "minor",
        description: [catCode || "Compliance item", isHealth ? "health-based standard" : "monitoring/reporting schedule", status].join(" · ")
      });
    }

    if (parsed.length > 0) {
      return parsed.sort((a, b) => (b.date > a.date ? 1 : (b.date < a.date ? -1 : 0))).slice(0, 45);
    }

    const isJackson = pwsid === "MS0250008";
    const isFlint = pwsid === "MI0002310";
    const isNewark = pwsid === "NJ0714001";
    if (isJackson) {
      return [
        { id: `${pwsid}-V26`, date: "2026-03-12", year: 2026, contaminant: "Turbidity / Treatment Technique", type: "treatment", status: "open", severity: "serious", description: "TT · Surface water turbidity & pressure advisory · open" },
        { id: `${pwsid}-V25`, date: "2025-07-19", year: 2025, contaminant: "Lead & Copper Rule Corrosion Control", type: "treatment", status: "addressed", severity: "serious", description: "TT · Optimal corrosion control treatment milestone · addressed" },
        { id: `${pwsid}-V23`, date: "2023-11-04", year: 2023, contaminant: "Total Trihalomethanes (TTHMs)", type: "monitoring", status: "resolved", severity: "minor", description: "MR · Stage 2 DBP routine sample reporting · resolved" }
      ];
    }
    if (isFlint) {
      return [
        { id: `${pwsid}-V25`, date: "2025-09-01", year: 2025, contaminant: "Total Trihalomethanes (TTHMs)", type: "monitoring", status: "open", severity: "minor", description: "MR · Routine DBP quarterly reporting verification · open" },
        { id: `${pwsid}-V22`, date: "2022-05-14", year: 2022, contaminant: "Lead (90th percentile)", type: "treatment", status: "resolved", severity: "serious", description: "TT · Service line replacement & orthophosphate verification · resolved" },
        { id: `${pwsid}-V18`, date: "2018-06-20", year: 2018, contaminant: "Lead & Copper Rule Follow-up", type: "health", status: "resolved", severity: "serious", description: "LCR · Historical corrosion control remediation · resolved" }
      ];
    }
    if (isNewark) {
      return [
        { id: `${pwsid}-V24`, date: "2024-08-10", year: 2024, contaminant: "Total Trihalomethanes (TTHMs)", type: "monitoring", status: "resolved", severity: "minor", description: "MR · Quarterly disinfection byproduct sample schedule · resolved" },
        { id: `${pwsid}-V19`, date: "2019-07-01", year: 2019, contaminant: "Lead (90th percentile)", type: "health", status: "resolved", severity: "serious", description: "LCR · Pequannock service area corrosion upgrade completed · resolved" }
      ];
    }
    return [
      { id: `${pwsid}-V25`, date: "2025-08-15", year: 2025, contaminant: "Total Trihalomethanes (TTHMs)", type: "monitoring", status: "resolved", severity: "minor", description: "MR · Routine Stage 2 Disinfection Byproducts monitoring · resolved" },
      { id: `${pwsid}-V22`, date: "2022-04-10", year: 2022, contaminant: "Nitrate (as N)", type: "monitoring", status: "resolved", severity: "minor", description: "MR · Annual inorganic watershed sample schedule · resolved" }
    ];
  }

  function buildContaminantsDualBenchmark(pwsid, sourceType, lcrArr) {
    const lcrLatest = {};
    for (let i = 0; i < lcrArr.length; i++) {
      const s = lcrArr[i];
      if (!s) continue;
      const code = String(s.contaminant_code || s.CONTAMINANT_CODE || "");
      if (code !== "PB90" && code !== "CU90") continue;
      const rawMeasure = parseFloat(s.sample_measure || s.SAMPLE_MEASURE || "");
      if (Number.isNaN(rawMeasure)) continue;
      const unit = String(s.unit_of_measure || s.UNIT_OF_MEASURE || "").toLowerCase();
      let normalized = rawMeasure;
      if (code === "PB90" && (unit === "mg/l" || unit === "ppm")) normalized = rawMeasure * 1000.0;
      else if (code === "CU90" && (unit === "ug/l" || unit === "ppb")) normalized = rawMeasure / 1000.0;
      if (lcrLatest[code] === undefined) {
        lcrLatest[code] = normalized;
      }
    }

    const isSurface = String(sourceType || "").toLowerCase().includes("surface");
    const isFlint = pwsid === "MI0002310";
    const isNewark = pwsid === "NJ0714001";
    const isJackson = pwsid === "MS0250008";
    const h = hashStr(pwsid);

    const defaults = {
      PB90: isFlint ? 5.8 : (isNewark ? 4.1 : (isJackson ? 6.4 : 1.8 + (h % 25) * 0.1)),
      "2950": isSurface ? 44.0 + (h % 24) : 14.0 + (h % 12),
      "2456": isSurface ? 29.0 + (h % 18) : 9.0 + (h % 9),
      "2806": isSurface ? 1.9 + (h % 15) * 0.1 : 1.2 + (h % 12) * 0.1,
      "2805": isSurface ? 1.6 + (h % 12) * 0.1 : 0.9 + (h % 10) * 0.1,
      "1040": 1.3 + (h % 16) * 0.1,
      CU90: isFlint ? 0.28 : (isJackson ? 0.42 : 0.19 + (h % 18) * 0.01),
      "1005": 0.9 + (h % 14) * 0.1,
      "1025": 0.68
    };

    const codesOrder = ["PB90", "2950", "2456", "2806", "2805", "1040", "CU90", "1005", "1025"];
    return codesOrder.map((code) => {
      const meta = CONTAMINANT_CATALOG[code];
      const rawVal = lcrLatest[code] !== undefined ? lcrLatest[code] : (defaults[code] || 0);
      const detected = Math.round(rawVal * 100) / 100;
      return {
        ...meta,
        detected,
        exceedsHealthGoal: meta.phg !== null && detected > meta.phg,
        exceedsMcl: meta.mcl !== null && detected > meta.mcl
      };
    });
  }

  function findPresetByPwsid(pwsid) {
    for (const z of Object.keys(PRESET_SYSTEMS)) {
      const found = PRESET_SYSTEMS[z].find((s) => s.pwsid === pwsid);
      if (found) return found;
    }
    return null;
  }

  function scoreSystem(pwsid, violations, contaminants) {
    const presetSummary = findPresetByPwsid(pwsid);
    const baseScore = presetSummary ? presetSummary.currentScore : (78 + (hashStr(pwsid) % 16));
    const open = violations.filter((v) => v.status === "open");
    const openHealth = open.filter((v) => v.type === "health" || v.severity === "serious");
    const recentHealth = violations.filter((v) => v.type === "health" && v.year >= 2023);
    const aboveLegal = contaminants.filter((c) => c.exceedsMcl).length;
    const raw = presetSummary
      ? presetSummary.currentScore
      : (baseScore - openHealth.length * 14 - (open.length - openHealth.length) * 5 - recentHealth.length * 4 - aboveLegal * 12);
    const score = Math.min(96, Math.max(25, raw));
    let compliance = "in_compliance";
    if (pwsid === "MS0250008" || openHealth.length > 0 || aboveLegal > 0) {
      compliance = "under_enforcement";
    } else if (pwsid === "MI0002310" || open.length > 0 || recentHealth.length > 1) {
      compliance = "elevated_concern";
    }
    const risk = score < 45 ? "critical" : (score < 65 ? "high" : (score < 78 ? "moderate" : "low"));
    return { score, riskLevel: risk, complianceStatus: compliance };
  }

  async function loadSystemReport(summary, place) {
    const pwsid = summary.pwsid;
    if (systemReportCache[pwsid]) return systemReportCache[pwsid];

    const EPA_BASE = "https://data.epa.gov/efservice";
    const [sysArr, violArr, lcrArr, geoArr, drought] = await Promise.all([
      fetchJsonArraySafe(`${EPA_BASE}/WATER_SYSTEM/PWSID/${pwsid}/JSON`),
      fetchJsonArraySafe(`${EPA_BASE}/VIOLATION/PWSID/${pwsid}/rows/0:120/JSON`),
      fetchJsonArraySafe(`${EPA_BASE}/LCR_SAMPLE_RESULT/PWSID/${pwsid}/rows/0:80/JSON`),
      fetchJsonArraySafe(`${EPA_BASE}/GEOGRAPHIC_AREA/PWSID/${pwsid}/JSON`),
      fetchDroughtSafe(place && place.lat, place && place.lon, (place && place.state) || summary.state)
    ]);

    const sysObj = sysArr[0] || null;
    const presetSummary = findPresetByPwsid(pwsid);

    const name = String((sysObj && (sysObj.pws_name || sysObj.PWS_NAME)) || "").trim()
      || (presetSummary && presetSummary.name)
      || summary.name
      || `${(place && place.city ? place.city.toUpperCase() : "LOCAL")} WATER SYSTEM`;

    const city = String((sysObj && (sysObj.city_name || sysObj.CITY_NAME)) || "").trim()
      || (presetSummary && presetSummary.city)
      || summary.city
      || (place && place.city)
      || "Metro Area";

    const st = String((sysObj && (sysObj.state_code || sysObj.STATE_CODE)) || "").trim()
      || (presetSummary && presetSummary.state)
      || summary.state
      || (place && place.state)
      || pwsid.slice(0, 2);

    const zip = (place && place.zip)
      || (presetSummary && presetSummary.zip)
      || summary.zip
      || String((sysObj && (sysObj.zip_code || sysObj.ZIP_CODE)) || "").slice(0, 5)
      || "78701";

    const popRaw = parseInt((sysObj && (sysObj.population_served_count || sysObj.POPULATION_SERVED_COUNT)) || "0", 10);
    const population = (!Number.isNaN(popRaw) && popRaw > 0)
      ? popRaw
      : ((presetSummary && presetSummary.population) || summary.population || 75000);

    const rawSrcCode = String((sysObj && (sysObj.primary_source_code || sysObj.PRIMARY_SOURCE_CODE)) || "").trim();
    const sourceType = rawSrcCode
      ? sourceLabel(rawSrcCode)
      : ((presetSummary && presetSummary.sourceType) || summary.sourceType || "Surface water");

    const rawOwnerCode = String((sysObj && (sysObj.owner_type_code || sysObj.OWNER_TYPE_CODE)) || "").trim();
    const ownerType = rawOwnerCode ? ownerLabel(rawOwnerCode) : "Local government";

    let county = summary.county || "";
    for (let i = 0; i < geoArr.length; i++) {
      const g = geoArr[i];
      const c = g && String(g.county_served || g.COUNTY_SERVED || "").trim();
      if (c) {
        county = c;
        break;
      }
    }

    const violations = buildViolations10Y(pwsid, violArr);
    const contaminants = buildContaminantsDualBenchmark(pwsid, sourceType, lcrArr);
    const { score, riskLevel, complianceStatus } = scoreSystem(pwsid, violations, contaminants);

    const isSurface = sourceType.toLowerCase().includes("surface");
    const isFlint = pwsid === "MI0002310" || zip === "48502";
    const isNewark = pwsid === "NJ0714001" || zip === "07102";
    const isJackson = pwsid === "MS0250008" || zip === "39201";
    const h = hashStr(pwsid);

    const baseMap = {};
    contaminants.forEach((c) => { baseMap[c.code] = c.detected; });
    const pbNow = baseMap.PB90 ?? 2.4;
    const cuNow = baseMap.CU90 ?? 0.22;
    const tthmNow = baseMap["2950"] ?? (isSurface ? 46.0 : 18.0);
    const haa5Now = baseMap["2456"] ?? (isSurface ? 31.0 : 11.0);
    const no3Now = baseMap["1040"] ?? 1.4;
    const asNow = baseMap["1005"] ?? 1.1;
    const pfoaNow = baseMap["2806"] ?? 1.8;
    const pfosNow = baseMap["2805"] ?? 1.4;

    const qLabels = ["Q1 '26", "Q2 '26", "Q3 '26", "Q4 '26"];
    const qMult = isSurface ? [0.78, 0.92, 1.18, 1.0] : [0.92, 0.98, 1.06, 1.0];
    const qOff = isSurface ? [2, 1, -3, 0] : [1, 0, -1, 0];
    const oneYearPts = qLabels.map((lbl, idx) => ({
      periodLabel: lbl,
      qualityScore: Math.min(98, Math.max(20, score + qOff[idx])),
      violationCount: (idx === 2 && violations.some((v) => v.status === "open")) ? 1 : 0,
      levels: {
        PB90: Math.round(pbNow * (0.94 + idx * 0.02) * 100) / 100,
        CU90: Math.round(cuNow * (0.96 + (idx % 2) * 0.04) * 100) / 100,
        "2950": Math.round(tthmNow * qMult[idx] * 100) / 100,
        "2456": Math.round(haa5Now * (0.82 + (qMult[idx] - 0.78) * 0.85) * 100) / 100,
        "1040": Math.round(no3Now * (idx === 1 ? 1.22 : 0.96) * 100) / 100,
        "1005": Math.round(asNow * (0.95 + idx * 0.02) * 100) / 100,
        "2806": Math.round(pfoaNow * (0.95 + idx * 0.02) * 100) / 100,
        "2805": pfosNow,
        "1025": 0.68
      },
      milestoneNote: idx === 1 ? "Spring runoff & watershed nutrient monitoring" : (idx === 2 ? (isSurface ? "Warm-water peak chlorine demand & TTHM cycle" : "Summer aquifer demand peak") : null)
    }));

    const fiveYearLabels = ["2022", "2023", "2024", "2025", "2026"];
    const fiveYearPts = fiveYearLabels.map((yr, idx) => {
      const p = idx / 4.0;
      const sc = isFlint ? Math.round(62 + (score - 62) * p) : (isNewark ? Math.round(63 + (score - 63) * p) : (isJackson ? Math.round(53 + (score - 53) * p) : Math.min(98, Math.max(25, score - 2 + (idx % 3) + (idx === 4 ? 1 : 0)))));
      const lf = isFlint ? 2.1 - 1.1 * p : (isNewark ? 2.4 - 1.4 * p : 1.18 - 0.18 * p);
      return {
        periodLabel: yr,
        qualityScore: sc,
        violationCount: violations.filter((v) => String(v.year) === yr).length,
        levels: {
          PB90: Math.round(pbNow * lf * 100) / 100,
          CU90: Math.round(cuNow * (1.12 - 0.12 * p) * 100) / 100,
          "2950": Math.round(tthmNow * (0.90 + 0.10 * p + (idx % 2) * 0.05) * 100) / 100,
          "2456": Math.round(haa5Now * (0.92 + 0.08 * p) * 100) / 100,
          "1040": Math.round(no3Now * (0.88 + 0.12 * p) * 100) / 100,
          "1005": Math.round(asNow * (0.90 + 0.10 * p) * 100) / 100,
          "2806": Math.round(pfoaNow * (0.75 + 0.25 * p) * 100) / 100,
          "2805": pfosNow,
          "1025": 0.68
        },
        milestoneNote: yr === "2023" ? "Expanded UCMR5 / PFAS laboratory reporting" : (yr === "2025" ? "Updated EPA Lead & Copper Rule Improvements (LCRI) baseline" : null)
      };
    });

    const tenYearPts = Array.from({ length: 10 }, (_, idx) => {
      const yr = String(2017 + idx);
      const p = idx / 9.0;
      const sc = isFlint ? Math.round(42 + (score - 42) * p) : (isNewark ? Math.round(48 + (score - 48) * p) : (isJackson ? Math.round(49 + (score - 49) * p) : Math.min(98, Math.max(22, score - 4 + Math.round(p * 4) + ((idx + h) % 3 - 1)))));
      const lf = isFlint ? 4.2 - 3.2 * p : (isNewark ? 3.8 - 2.8 * p : 1.45 - 0.45 * p);
      return {
        periodLabel: "'" + yr.slice(-2),
        qualityScore: sc,
        violationCount: violations.filter((v) => String(v.year) === yr).length,
        levels: {
          PB90: Math.round(pbNow * lf * 100) / 100,
          CU90: Math.round(cuNow * (1.28 - 0.28 * p) * 100) / 100,
          "2950": Math.round(tthmNow * (0.85 + 0.15 * p + (idx % 2) * 0.06) * 100) / 100,
          "2456": Math.round(haa5Now * (0.86 + 0.14 * p) * 100) / 100,
          "1040": Math.round(no3Now * (0.82 + 0.18 * p) * 100) / 100,
          "1005": Math.round(asNow * (0.88 + 0.12 * p) * 100) / 100,
          "2806": Math.round(pfoaNow * (0.65 + 0.35 * p) * 100) / 100,
          "2805": pfosNow,
          "1025": 0.68
        },
        milestoneNote: yr === "2019" ? (isNewark || isFlint ? "Major service line replacement & orthophosphate upgrade" : "Routine distribution corrosion review") : (yr === "2024" ? "EPA finalized 4.0 ppt PFAS National Primary Drinking Water Regulation" : null)
      };
    });

    const treatmentSteps = sourceType.toLowerCase().includes("purchased")
      ? [
          { stepNumber: 1, title: "Wholesale Intake & Filtration", description: "Water is clarified and filtered at a regional wholesale treatment plant." },
          { stepNumber: 2, title: "Corrosion & Residual Control", description: "Orthophosphate and chloramine/chlorine residual are maintained in transmission mains." },
          { stepNumber: 3, title: "Storage & Pressurized Delivery", description: "Local tanks and pump stations maintain steady pressure to neighborhood taps." }
        ]
      : (sourceType.toLowerCase().includes("ground")
        ? [
            { stepNumber: 1, title: "Deep Aquifer Wells", description: "Water is pumped from protected underground bedrock or alluvial aquifers." },
            { stepNumber: 2, title: "Mineral & Iron Conditioning", description: "Aeration, sequestration, or ion-exchange balances mineral hardness and trace metals." },
            { stepNumber: 3, title: "Disinfection Residual", description: "Protective chlorine residual safeguards water quality across distribution pipes." }
          ]
        : [
            { stepNumber: 1, title: "Surface Reservoir Intake", description: "Raw water drawn from rivers, lakes, or protected watershed reservoirs." },
            { stepNumber: 2, title: "Coagulation & Flocculation", description: "Suspended organic matter and sediment are bound and settled in clarification basins." },
            { stepNumber: 3, title: "Multi-Media Filtration", description: "Sand, anthracite, and granular carbon beds remove fine particulates and precursors." },
            { stepNumber: 4, title: "Primary Disinfection & pH Control", description: "Pathogens are inactivated and corrosion inhibitors protect household plumbing." }
          ]);

    const cutMult = { D0: 0.90, D1: 0.80, D2: 0.65, D3: 0.50, D4: 0.40 }[drought.level] ?? 1.0;
    const recommendedGpd = Math.round(90 * cutMult);
    const now = new Date();
    const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

    const report = {
      pwsid,
      name,
      city,
      state: st,
      zip,
      county,
      population,
      sourceType,
      ownerType,
      qualityScore: score,
      fiveYearScoreDelta: (presetSummary && presetSummary.fiveYearDeltaPts !== undefined)
        ? presetSummary.fiveYearDeltaPts
        : (fiveYearPts[4].qualityScore - fiveYearPts[0].qualityScore),
      tenYearScoreDelta: tenYearPts[9].qualityScore - tenYearPts[0].qualityScore,
      riskLevel,
      complianceStatus,
      droughtLevel: drought.level,
      droughtDescription: drought.desc,
      contaminants,
      violations,
      treatment: treatmentSteps,
      usage: { normalGallonsPerDay: 90, recommendedGallonsPerDay: recommendedGpd },
      lastUpdated: todayStr,
      sourceDescription: `${name} supplies drinking water as a community public water system (ID ${pwsid}). Primary source: ${sourceType}. Operated by: ${ownerType}.${county ? ` Service area includes ${county} County.` : ""} 10-year compliance and sampling histories are synthesized from U.S. EPA Envirofacts (SDWIS) and paired with U.S. Drought Monitor telemetry.`,
      historicalSeries: {
        ONE_YEAR: {
          label: "1-Year Quarterly",
          shortLabel: "1Y",
          points: oneYearPts,
          deltaPts: oneYearPts[3].qualityScore - oneYearPts[0].qualityScore,
          supplyIndication: isSurface
            ? "Warmer summer reservoir and river temperatures increase natural organic matter and chlorine demand, causing routine late-summer rises in Total Trihalomethanes (TTHMs) and Haloacetic Acids (HAA5)."
            : "Deep aquifer supplies are thermally insulated from summer heat waves, keeping disinfection byproduct formation low year-round."
        },
        FIVE_YEAR: {
          label: "5-Year Multi-Year",
          shortLabel: "5Y",
          points: fiveYearPts,
          deltaPts: fiveYearPts[4].qualityScore - fiveYearPts[0].qualityScore,
          supplyIndication: drought.level !== "none"
            ? `Active drought conditions (${drought.level}) reduce reservoir dilution and concentrate dissolved minerals, bromide, and trace metals such as arsenic in source water.`
            : "Consistent watershed storage over the 5-year window has prevented drought-driven mineral concentration spikes."
        },
        TEN_YEAR: {
          label: "10-Year Decade",
          shortLabel: "10Y",
          points: tenYearPts,
          deltaPts: tenYearPts[9].qualityScore - tenYearPts[0].qualityScore,
          supplyIndication: "Decade-long declines in 90th-percentile lead and copper reflect utility orthophosphate corrosion-control optimization and progressive replacement of pre-1986 lead service lines."
        }
      }
    };

    systemReportCache[pwsid] = report;
    return report;
  }

  /* ==========================================================================
     ZIP CODE LOOKUP (1:1 port of WaterRepository.searchProvidersByZip)
     ========================================================================== */
  const PLACE_FALLBACKS = {
    "78701": { zip: "78701", city: "Austin", state: "TX", lat: 30.2711, lon: -97.7437 },
    "48502": { zip: "48502", city: "Flint", state: "MI", lat: 43.0125, lon: -83.6875 },
    "07102": { zip: "07102", city: "Newark", state: "NJ", lat: 40.7357, lon: -74.1724 },
    "39201": { zip: "39201", city: "Jackson", state: "MS", lat: 32.2988, lon: -90.1848 },
    "90210": { zip: "90210", city: "Beverly Hills", state: "CA", lat: 34.0901, lon: -118.4065 }
  };

  async function fetchPlaceFromZip(zip) {
    try {
      const zRes = await fetch(`https://api.zippopotam.us/us/${zip}`, { headers: { Accept: "application/json" } });
      if (zRes.ok) {
        const zData = await zRes.json();
        const p = zData.places && zData.places[0];
        if (p) {
          return {
            zip,
            city: p["place name"] || "",
            state: p["state abbreviation"] || "",
            lat: parseFloat(p.latitude || "0") || 0,
            lon: parseFloat(p.longitude || "0") || 0
          };
        }
      }
    } catch (_) {}
    return PLACE_FALLBACKS[zip] || null;
  }

  async function fetchEpaSystems(zip, city, stateCode) {
    try {
      const EPA_BASE = "https://data.epa.gov/efservice";
      const byZip = await fetchJsonArraySafe(`${EPA_BASE}/WATER_SYSTEM/ZIP_CODE/BEGINNING/${zip}/PWS_ACTIVITY_CODE/A/rows/0:30/JSON`);
      const encCity = encodeURIComponent(String(city || "").toUpperCase());
      const encState = encodeURIComponent(String(stateCode || "").toUpperCase());
      const byCity = byZip.length < 3
        ? await fetchJsonArraySafe(`${EPA_BASE}/WATER_SYSTEM/CITY_NAME/${encCity}/STATE_CODE/${encState}/PWS_ACTIVITY_CODE/A/rows/0:30/JSON`)
        : [];

      const map = new Map();
      function absorb(arr) {
        for (let i = 0; i < arr.length; i++) {
          const obj = arr[i];
          if (!obj) continue;
          const pwsid = String(obj.pwsid || obj.PWSID || "").trim();
          if (!pwsid) continue;
          const st = String(obj.state_code || obj.STATE_CODE || "").trim();
          if (st.toUpperCase() !== stateCode.toUpperCase() && !pwsid.toUpperCase().startsWith(stateCode.toUpperCase())) continue;
          const pop = parseInt(obj.population_served_count || obj.POPULATION_SERVED_COUNT || "0", 10) || 0;
          const prev = map.get(pwsid);
          const prevPop = prev ? (parseInt(prev.population_served_count || prev.POPULATION_SERVED_COUNT || "0", 10) || -1) : -1;
          if (!prev || pop > prevPop) {
            map.set(pwsid, obj);
          }
        }
      }
      absorb(byZip);
      absorb(byCity);

      const all = Array.from(map.values());
      const cws = all.filter((it) => {
        const t = String(it.pws_type_code || it.PWS_TYPE_CODE || "").toUpperCase();
        return t === "CWS" || t === "NTNCWS";
      });
      const filtered = cws.length > 0 ? cws : all;
      return filtered.sort((a, b) => {
        const pa = parseInt(a.population_served_count || a.POPULATION_SERVED_COUNT || "0", 10) || 0;
        const pb = parseInt(b.population_served_count || b.POPULATION_SERVED_COUNT || "0", 10) || 0;
        return pb - pa;
      }).slice(0, 25);
    } catch (_) {
      return [];
    }
  }

  async function fetchProvidersByZip(zip) {
    const place = await fetchPlaceFromZip(zip);
    if (!place) {
      throw new Error(`ZIP code ${zip} was not recognized. Try a nearby 5-digit U.S. ZIP.`);
    }

    const presetList = PRESET_SYSTEMS[zip] || null;
    const liveRows = await fetchEpaSystems(zip, place.city, place.state);

    if (liveRows.length === 0 && presetList) {
      return { place, providers: presetList.map((p) => ({ ...p })) };
    }

    if (liveRows.length === 0) {
      const st = place.state.toUpperCase();
      return {
        place,
        providers: [
          {
            pwsid: `${st}${zip}01`,
            name: `${place.city.toUpperCase()} MUNICIPAL WATER SYSTEM`,
            city: place.city, state: place.state, zip, county: "",
            population: 68400,
            sourceType: "Surface water",
            isPrimary: true,
            currentScore: 83,
            fiveYearDeltaPts: 3
          },
          {
            pwsid: `${st}${zip}02`,
            name: `${place.city.toUpperCase()} REGIONAL WATER AUTHORITY`,
            city: place.city, state: place.state, zip, county: "",
            population: 21900,
            sourceType: "Groundwater",
            isPrimary: false,
            currentScore: 87,
            fiveYearDeltaPts: 5
          }
        ]
      };
    }

    const rawSummaries = liveRows.map((obj) => {
      const pwsid = String(obj.pwsid || obj.PWSID || "");
      const name = String(obj.pws_name || obj.PWS_NAME || pwsid).trim() || pwsid;
      const city = String(obj.city_name || obj.CITY_NAME || place.city).trim() || place.city;
      const st = String(obj.state_code || obj.STATE_CODE || place.state).trim() || place.state;
      const pop = parseInt(obj.population_served_count || obj.POPULATION_SERVED_COUNT || "0", 10) || 0;
      const srcCode = String(obj.primary_source_code || obj.PRIMARY_SOURCE_CODE || "SW");
      const presetMatch = presetList ? presetList.find((it) => it.pwsid === pwsid) : null;
      const seedScore = presetMatch ? presetMatch.currentScore : (78 + (hashStr(pwsid) % 16));
      const delta = presetMatch ? presetMatch.fiveYearDeltaPts : ((hashStr(pwsid) % 9) - 2);
      return {
        pwsid,
        name,
        city,
        state: st,
        zip,
        county: presetMatch ? (presetMatch.county || "") : "",
        population: pop,
        sourceType: sourceLabel(srcCode),
        isPrimary: Boolean(presetMatch && presetMatch.isPrimary),
        currentScore: seedScore,
        fiveYearDeltaPts: delta
      };
    });

    const combined = [];
    if (presetList) {
      presetList.forEach((p) => combined.push({ ...p }));
    }
    rawSummaries.forEach((live) => {
      if (!combined.some((x) => x.pwsid === live.pwsid)) {
        combined.push(live);
      }
    });
    combined.sort((a, b) => {
      if (a.isPrimary !== b.isPrimary) return a.isPrimary ? -1 : 1;
      return b.population - a.population;
    });
    const providers = combined.map((s, idx) => ({ ...s, isPrimary: idx === 0 }));
    return { place, providers };
  }

  window.H2KnowData = {
    ZIP_PRESETS,
    fmtVal,
    droughtLabel,
    fetchProvidersByZip,
    loadSystemReport
  };
})();
