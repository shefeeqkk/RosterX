/**
 * RosterX — Dynamic Admin Engine & AI Smart Roster Generator (rosterAiEngine.js)
 * 
 * Implements:
 * 1. Dynamic Gemini API Client with Structured Output Schema matching Section 3.B.
 * 2. Dynamic Prompt Builder with custom directives & constraints matching Section 3.A.
 * 3. Swap-First Arbitration: Evaluates absences, finds eligible peers, establishes reciprocal re-swaps.
 * 4. Lifecycle-aware scheduling: Filters Active engineers, respects join_date and resignation_date.
 * 5. Deterministic fallback and real-time rule auditor.
 */

import { SHIFTS, DEFAULT_CONSTRAINTS, getEngineerSwapCode, isSwapShift, isOffCategory } from './constants.js?v=1.0';

const STORAGE_KEYS = {
  GEMINI_API_KEY: 'rosterx_gemini_api_key',
  GEMINI_MODEL: 'rosterx_gemini_model'
};

export class RosterAiEngine {
  constructor() {
    this.apiKey = this.loadApiKey();
    this.model = this.loadModel();
  }

  loadApiKey() {
    if (typeof localStorage === 'undefined') return '';
    return localStorage.getItem(STORAGE_KEYS.GEMINI_API_KEY) || '';
  }

  saveApiKey(key) {
    this.apiKey = key.trim();
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.GEMINI_API_KEY, this.apiKey);
    }
  }

  loadModel() {
    if (typeof localStorage === 'undefined') return 'gemini-1.5-flash';
    return localStorage.getItem(STORAGE_KEYS.GEMINI_MODEL) || 'gemini-1.5-flash';
  }

  saveModel(model) {
    this.model = model;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.GEMINI_MODEL, this.model);
    }
  }

  /**
   * Gemini API Structured Output Schema (Section 3.B)
   */
  getStructuredResponseSchema() {
    return {
      type: "OBJECT",
      properties: {
        month: { type: "INTEGER" },
        year: { type: "INTEGER" },
        roster: {
          type: "ARRAY",
          items: {
            type: "OBJECT",
            properties: {
              engineer_id: { type: "STRING" },
              engineer_name: { type: "STRING" },
              shifts: {
                type: "ARRAY",
                items: {
                  type: "OBJECT",
                  properties: {
                    day: { type: "INTEGER" },
                    shift_code: { 
                      type: "STRING", 
                      description: "Core shift code (FS, FG, G, SG, SS, NS, OFF, L) or Standardized Swap Code (SW + Peer Initials, e.g. SWM, SWDY, SWSH)"
                    },
                    is_swap: { type: "BOOLEAN" },
                    swap_partner_id: { type: "STRING" },
                    note: { type: "STRING" }
                  },
                  required: ["day", "shift_code"]
                }
              }
            },
            required: ["engineer_id", "engineer_name", "shifts"]
          }
        },
        ai_arbitrated_swaps: {
          type: "ARRAY",
          items: {
            type: "OBJECT",
            properties: {
              requester_id: { type: "STRING" },
              covered_by_id: { type: "STRING" },
              swap_date: { type: "STRING" },
              reswap_date: { type: "STRING" },
              reasoning: { type: "STRING" }
            },
            required: ["requester_id", "covered_by_id", "swap_date"]
          }
        },
        audit_metrics: {
          type: "OBJECT",
          properties: {
            night_coverage_valid: { type: "BOOLEAN" },
            zero_fatigue_violations: { type: "BOOLEAN" },
            swaps_vs_leaves_ratio: { type: "STRING" }
          },
          required: ["night_coverage_valid", "zero_fatigue_violations"]
        }
      },
      required: ["month", "year", "roster", "ai_arbitrated_swaps", "audit_metrics"]
    };
  }

  /**
   * Filter active engineers based on lifecycle status, join_date, and resignation_date
   */
  getActiveEngineersForPeriod(engineers, year, month) {
    const periodStart = new Date(year, month - 1, 1);
    const totalDays = new Date(year, month, 0).getDate();
    const periodEnd = new Date(year, month - 1, totalDays);

    return engineers.filter(eng => {
      // 1. Resigned prior to period starts? Exclude
      if (eng.status === 'Resigned') {
        if (!eng.resignation_date) return false;
        const resDate = new Date(eng.resignation_date);
        if (resDate < periodStart) return false;
      }

      // 2. Resignation date set prior to this month? Exclude
      if (eng.resignation_date) {
        const resDate = new Date(eng.resignation_date);
        if (resDate < periodStart) return false;
      }

      // 3. Join date in the future beyond this month? Exclude
      if (eng.join_date) {
        const jDate = new Date(eng.join_date);
        if (jDate > periodEnd) return false;
      }

      return true;
    });
  }

  /**
   * Dynamic Prompt Builder matching Section 3.A
   */
  buildSystemPrompt(constraints, customDirectivesText) {
    return `
You are the Chief Operations Planner for a 24/7 Mission-Critical NOC.
Your goal is to build an optimal, fair, and fatigue-safe monthly shift schedule.

OPERATIONAL CONSTRAINTS ENFORCED:
1. SWAP-FIRST POLICY: Swapping is strictly prioritized over unstaffed Leave (L). If an engineer requests off, find an eligible peer to swap shifts before applying Leave.
2. NIGHT SHIFT: Exactly ${constraints.nightHeadcount} engineer on NS per night.
3. MINIMUM COVERAGE: At least ${constraints.fsMin} FS and ${constraints.ssMin} SS on duty every day.
4. MANDATORY POST-NIGHT REST: Any engineer finishing an NS shift MUST receive ${constraints.postNightRestDays} consecutive rest days (OFF or Swap Code SW...) immediately following the night shift.
5. CONSECUTIVE WORK CAP: Maximum ${constraints.maxConsecutiveDays} consecutive working days before an OFF or Swap rest.
6. SWAP CODE FORMAT: Swap codes start with 'SW' followed by peer initials (e.g., SWM for Manoj, SWSH for Shefeeq, SWSI for Silambarasan, SWDY for Dyani). Any SW... code means the engineer is OFF.
7. WEEKLY OFF ANCHORS: Honor each engineer's paired weekly off anchor days unless overridden by an approved swap.
8. ACTIVE ROSTER: Only schedule engineers marked as Active during the target month.

CUSTOM DIRECTIVES:
${customDirectivesText || "None provided."}
`.trim();
  }

  /**
   * Execute Gemini API Call with Structured Output & Swap-First Negotiation
   */
  async generateRosterWithGemini({
    year,
    month,
    engineers,
    constraints = DEFAULT_CONSTRAINTS,
    manualSwaps = [],
    absenceRequests = [],
    onProgress
  }) {
    const totalDays = new Date(year, month, 0).getDate();
    const activeEngineers = this.getActiveEngineersForPeriod(engineers, year, month);

    if (activeEngineers.length < 5) {
      throw new Error(`Insufficient active engineers (${activeEngineers.length}) to staff a 24/7 NOC roster.`);
    }

    // Fallback if no API key
    if (!this.apiKey) {
      if (onProgress) onProgress("No Gemini API key configured. Executing local Smart Swap-First engine...");
      return this.generateDeterministicRoster({
        year,
        month,
        totalDays,
        engineers: activeEngineers,
        constraints,
        manualSwaps,
        absenceRequests
      });
    }

    if (onProgress) onProgress(`Connecting to Gemini API (${this.model})...`);

    const systemPrompt = this.buildSystemPrompt(constraints, constraints.customDirectives);
    const dayShortNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const calendar = [];
    for (let d = 1; d <= totalDays; d++) {
      const dow = new Date(year, month - 1, d).getDay();
      calendar.push({ day: d, day_of_week: dayShortNames[dow] });
    }

    const payloadContext = {
      target_month: { year, month, total_days: totalDays, calendar },
      team_roster: activeEngineers.map(e => ({
        id: e.id,
        name: e.name,
        role: e.role,
        default_weekly_offs: e.default_weekly_offs || ["Saturday", "Sunday"],
        preferred_shifts: e.preferred_shifts || ["FS", "FG", "SS"],
        join_date: e.join_date,
        resignation_date: e.resignation_date
      })),
      pre_locked_mutual_swaps: manualSwaps,
      absence_requests_for_ai_arbitration: absenceRequests
    };

    const requestPayload = {
      contents: [
        {
          role: "user",
          parts: [
            { text: systemPrompt },
            { text: "INPUT CONTEXT:\n" + JSON.stringify(payloadContext, null, 2) }
          ]
        }
      ],
      generationConfig: {
        temperature: 0.1,
        responseMimeType: "application/json",
        responseSchema: this.getStructuredResponseSchema()
      }
    };

    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${this.model}:generateContent?key=${this.apiKey}`;

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestPayload)
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Gemini API Error (${response.status}): ${errText}`);
      }

      if (onProgress) onProgress("Parsing Gemini structured JSON response...");
      const resultData = await response.json();

      let parsed = null;
      if (resultData.candidates?.[0]?.content?.parts?.[0]?.text) {
        parsed = JSON.parse(resultData.candidates[0].content.parts[0].text);
      } else {
        throw new Error("Invalid response format from Gemini API.");
      }

      // Convert to grid format
      const mappedEngineers = activeEngineers.map(eng => {
        const found = parsed.roster?.find(r => r.engineer_id === eng.id || r.engineer_name?.toUpperCase() === eng.name.toUpperCase());
        const shiftsArr = new Array(totalDays).fill("OFF");
        if (found && Array.isArray(found.shifts)) {
          found.shifts.forEach(s => {
            const idx = s.day - 1;
            if (idx >= 0 && idx < totalDays) shiftsArr[idx] = s.shift_code;
          });
        }
        return {
          id: eng.id,
          name: eng.name,
          role: eng.role,
          status: eng.status,
          shifts: shiftsArr
        };
      });

      if (onProgress) onProgress("Auditing & healing constraints...");
      const healed = this.healRoster(mappedEngineers, totalDays, constraints, manualSwaps);
      const audit = this.auditRoster(healed, totalDays, constraints);

      return {
        success: true,
        source: 'gemini_api',
        model: this.model,
        year,
        month,
        totalDays,
        engineers: healed,
        ai_arbitrated_swaps: parsed.ai_arbitrated_swaps || [],
        compliance_summary: {
          night_coverage_valid: audit.nightViolations.length === 0,
          zero_fatigue_violations: audit.fatigueViolations.length === 0,
          score: audit.score,
          audit
        }
      };

    } catch (err) {
      console.warn("Gemini API call failed, falling back to local Smart Swap-First engine:", err);
      if (onProgress) onProgress(`API issue (${err.message}). Executing local engine...`);
      return this.generateDeterministicRoster({
        year,
        month,
        totalDays,
        engineers: activeEngineers,
        constraints,
        manualSwaps,
        absenceRequests,
        fallbackReason: err.message
      });
    }
  }

  /**
   * Local Smart Swap-First Deterministic Engine
   */
  generateDeterministicRoster({
    year,
    month,
    totalDays,
    engineers,
    constraints = DEFAULT_CONSTRAINTS,
    manualSwaps = [],
    absenceRequests = [],
    fallbackReason = null
  }) {
    const dayShortNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

    // 1. Initialize grid
    const grid = engineers.map(eng => ({
      id: eng.id,
      name: eng.name,
      role: eng.role,
      status: eng.status,
      shifts: new Array(totalDays).fill(null)
    }));

    // 2. Pre-Locked Mutual Swaps
    manualSwaps.forEach(sw => {
      const engA = grid.find(e => e.id === sw.engineer_a_id || e.name === sw.engineer_a);
      const engB = grid.find(e => e.id === sw.engineer_b_id || e.name === sw.engineer_b);
      const d1 = sw.day1 - 1;
      const d2 = sw.day2 ? sw.day2 - 1 : -1;

      if (engA && d1 >= 0 && d1 < totalDays) {
        engA.shifts[d1] = engB ? getEngineerSwapCode(engB.name, engineers) : (sw.shift1 || 'SW');
      }
      if (engB && d1 >= 0 && d1 < totalDays) {
        engB.shifts[d1] = sw.coveringShift1 || 'SS';
      }

      if (d2 >= 0 && d2 < totalDays) {
        if (engB) engB.shifts[d2] = engA ? getEngineerSwapCode(engA.name, engineers) : (sw.shift2 || 'SW');
        if (engA) engA.shifts[d2] = sw.coveringShift2 || 'FS';
      }
    });

    // 3. AI-Arbitrated Swap-First Absence Requests
    const arbitratedSwaps = [];
    absenceRequests.forEach(req => {
      const requester = grid.find(e => e.id === req.engineer_id || e.name === req.engineer_name);
      const reqDay = req.day - 1;
      if (!requester || reqDay < 0 || reqDay >= totalDays) return;

      // Swap-First Policy: Find an eligible peer who can cover
      const eligibleCover = grid.find(peer => 
        peer.id !== requester.id &&
        peer.shifts[reqDay] === null &&
        peer.status === 'Active'
      );

      if (eligibleCover) {
        // Successful swap negotiation: Requester gets SW code (treated as OFF)
        const swapCodeForRequester = getEngineerSwapCode(eligibleCover.name, engineers);
        requester.shifts[reqDay] = swapCodeForRequester;
        eligibleCover.shifts[reqDay] = 'SS';

        // Propose payback re-swap date later in the month
        const paybackDay = Math.min(totalDays - 1, reqDay + 7);
        if (eligibleCover.shifts[paybackDay] === null && requester.shifts[paybackDay] === null) {
          const swapCodeForCover = getEngineerSwapCode(requester.name, engineers);
          eligibleCover.shifts[paybackDay] = swapCodeForCover;
          requester.shifts[paybackDay] = 'FS';
        }

        arbitratedSwaps.push({
          requester_id: requester.id,
          covered_by_id: eligibleCover.id,
          swap_date: `Day ${req.day}`,
          reswap_date: `Day ${paybackDay + 1}`,
          reasoning: `Swap-First: ${eligibleCover.name} covers on Day ${req.day}; ${requester.name} provides payback cover on Day ${paybackDay + 1}.`
        });
      } else {
        // No peer available to cover -> assign unstaffed Leave (L)
        requester.shifts[reqDay] = 'L';
      }
    });

    // 4. Assign continuous 24/7 Night Shift (NS): Strictly exactly nightHeadcount per night
    let poolIdx = 0;
    let d = 0;
    while (d < totalDays) {
      const existingNight = grid.find(e => e.shifts[d] === 'NS');
      if (existingNight) {
        d++;
        continue;
      }

      const blockSize = Math.min(constraints.nightBlockMin || 3, totalDays - d);
      const eng = grid[poolIdx % grid.length];
      poolIdx++;

      let canTake = true;
      for (let offset = 0; offset < blockSize; offset++) {
        if (eng.shifts[d + offset] !== null) {
          canTake = false;
          break;
        }
      }

      if (canTake) {
        // Pre-night rest
        if (d > 0 && eng.shifts[d - 1] === null) eng.shifts[d - 1] = 'OFF';

        for (let offset = 0; offset < blockSize; offset++) {
          eng.shifts[d + offset] = 'NS';
        }
        // Mandatory post-night rest (e.g. 2 days)
        const postRest = d + blockSize;
        for (let r = 0; r < constraints.postNightRestDays && (postRest + r) < totalDays; r++) {
          if (eng.shifts[postRest + r] === null) eng.shifts[postRest + r] = 'OFF';
        }
        d += blockSize;
      } else {
        const fallback = grid.find(e => e.shifts[d] === null) || grid[0];
        if (fallback.shifts[d] === null) {
          if (d > 0 && fallback.shifts[d - 1] === null) fallback.shifts[d - 1] = 'OFF';
          fallback.shifts[d] = 'NS';
          if (d + 1 < totalDays && fallback.shifts[d + 1] === null) fallback.shifts[d + 1] = 'OFF';
        }
        d++;
      }
    }

    // 5. Apply Weekly Off Anchors
    grid.forEach(eng => {
      const anchorOffs = eng.default_weekly_offs || ["Saturday", "Sunday"];
      for (let day = 1; day <= totalDays; day++) {
        const dIdx = day - 1;
        if (eng.shifts[dIdx] !== null) continue;

        const dow = new Date(year, month - 1, day).getDay();
        const dowName = dayShortNames[dow];

        if (dIdx > 0 && eng.shifts[dIdx - 1] === 'NS') {
          eng.shifts[dIdx] = 'OFF';
          continue;
        }

        if (anchorOffs.includes(dowName) || anchorOffs.some(o => dowName.startsWith(o.slice(0, 3)))) {
          eng.shifts[dIdx] = 'OFF';
        }
      }
    });

    // 6. Fill Day Shifts (FS >= fsMin, SS >= ssMin, Daytime Overlap)
    const dayCycle = ['FS', 'FG', 'SS', 'SG', 'G'];
    grid.forEach((eng, engIdx) => {
      let streak = 0;
      for (let dIdx = 0; dIdx < totalDays; dIdx++) {
        const s = eng.shifts[dIdx];
        if (s === 'OFF' || (s && s.startsWith('SW')) || s === 'L') {
          streak = 0;
          continue;
        }
        if (s !== null) {
          streak++;
          continue;
        }

        if (streak >= constraints.maxConsecutiveDays) {
          eng.shifts[dIdx] = 'OFF';
          streak = 0;
          continue;
        }

        const assigned = grid.map(e => e.shifts[dIdx]).filter(Boolean);
        const fsCount = assigned.filter(x => x === 'FS').length;
        const ssCount = assigned.filter(x => x === 'SS').length;

        if (fsCount < constraints.fsMin) {
          eng.shifts[dIdx] = 'FS';
        } else if (ssCount < constraints.ssMin) {
          eng.shifts[dIdx] = 'SS';
        } else {
          eng.shifts[dIdx] = dayCycle[(engIdx * 2 + dIdx) % dayCycle.length];
        }
        streak++;
      }
    });

    // 7. Healing pass
    const healed = this.healRoster(grid, totalDays, constraints, manualSwaps);
    const audit = this.auditRoster(healed, totalDays, constraints);

    return {
      success: true,
      source: 'local_deterministic_engine',
      fallbackReason,
      year,
      month,
      totalDays,
      engineers: healed,
      ai_arbitrated_swaps: arbitratedSwaps,
      compliance_summary: {
        night_coverage_valid: audit.nightViolations.length === 0,
        zero_fatigue_violations: audit.fatigueViolations.length === 0,
        score: audit.score,
        audit
      }
    };
  }

  /**
   * Healing pass enforcing strict night coverage & zero fatigue violations
   */
  healRoster(engineers, totalDays, constraints, manualSwaps = []) {
    const copy = JSON.parse(JSON.stringify(engineers));
    const targetNight = constraints.nightHeadcount || 1;

    // Guarantee: Exactly nightHeadcount NS per night
    for (let d = 0; d < totalDays; d++) {
      const nsEngs = copy.filter(e => e.shifts[d] === 'NS');
      if (nsEngs.length < targetNight) {
        const needed = targetNight - nsEngs.length;
        for (let i = 0; i < needed; i++) {
          const candidate = copy.find(e => ['SG', 'SS', 'G'].includes(e.shifts[d]));
          if (candidate) {
            candidate.shifts[d] = 'NS';
            if (d + 1 < totalDays && candidate.shifts[d + 1] !== 'L' && !(candidate.shifts[d + 1] && candidate.shifts[d + 1].startsWith('SW'))) {
              candidate.shifts[d + 1] = 'OFF';
            }
          }
        }
      } else if (nsEngs.length > targetNight) {
        for (let i = targetNight; i < nsEngs.length; i++) {
          nsEngs[i].shifts[d] = 'SS';
        }
      }
    }

    // Guarantee: FS >= fsMin & SS >= ssMin
    for (let d = 0; d < totalDays; d++) {
      const fsCount = copy.filter(e => e.shifts[d] === 'FS').length;
      if (fsCount < constraints.fsMin) {
        const cand = copy.find(e => ['FG', 'G', 'SG'].includes(e.shifts[d]));
        if (cand) cand.shifts[d] = 'FS';
      }

      const ssCount = copy.filter(e => e.shifts[d] === 'SS').length;
      if (ssCount < constraints.ssMin) {
        const cand = copy.find(e => ['SG', 'G', 'FG'].includes(e.shifts[d]));
        if (cand) cand.shifts[d] = 'SS';
      }
    }

    // Guarantee: Zero Post-Night Fatigue Violations (Never NS -> FS, FG, G)
    copy.forEach(eng => {
      for (let d = 0; d < totalDays; d++) {
        if (d > 0 && eng.shifts[d - 1] === 'NS') {
          if (['FS', 'FG', 'G', 'SG', 'SS'].includes(eng.shifts[d])) {
            eng.shifts[d] = 'OFF';
          }
        }
      }
    });

    // Guarantee: Consecutive working days cap
    copy.forEach(eng => {
      let streak = 0;
      for (let d = 0; d < totalDays; d++) {
        const s = eng.shifts[d];
        if (s === 'OFF' || (s && s.startsWith('SW')) || s === 'L') {
          streak = 0;
        } else {
          streak++;
          if (streak > constraints.maxConsecutiveDays) {
            let healed = false;
            for (let back = d; back >= Math.max(0, d - constraints.maxConsecutiveDays); back--) {
              if (eng.shifts[back] !== 'NS' && eng.shifts[back] !== 'L' && !(eng.shifts[back] && eng.shifts[back].startsWith('SW'))) {
                eng.shifts[back] = 'OFF';
                healed = true;
                streak = d - back;
                break;
              }
            }
            if (!healed && s !== 'NS') {
              eng.shifts[d] = 'OFF';
              streak = 0;
            }
          }
        }
      }
    });

    return copy;
  }

  /**
   * Rule Auditor matching Module B and C requirements
   */
  auditRoster(engineers, totalDays, constraints = DEFAULT_CONSTRAINTS) {
    const dailyCoverage = [];
    const nightViolations = [];
    const deskViolations = [];
    const fatigueViolations = [];

    const targetNight = constraints.nightHeadcount || 1;

    for (let d = 0; d < totalDays; d++) {
      const dayNum = d + 1;
      let nsCount = 0;
      let fsCount = 0;
      let ssCount = 0;

      engineers.forEach(eng => {
        // Apprentice engineers are not counted towards core desk or night coverage requirements
        if (eng.role === 'apprentice') return;

        const s = eng.shifts[d];
        if (s === 'NS') nsCount++;
        if (s === 'FS') fsCount++;
        if (s === 'SS') ssCount++;
      });

      dailyCoverage.push({ day: dayNum, nsCount, fsCount, ssCount });

      if (nsCount < targetNight) {
        nightViolations.push({ day: dayNum, type: 'UNDER_NIGHT', message: `Day ${dayNum}: Night coverage below target (NS = ${nsCount}, required = ${targetNight})` });
      } else if (nsCount > targetNight) {
        nightViolations.push({ day: dayNum, type: 'OVER_NIGHT', message: `Day ${dayNum}: Night coverage overstaffed (NS = ${nsCount}, target = ${targetNight})` });
      }

      if (fsCount < constraints.fsMin) {
        deskViolations.push({ day: dayNum, type: 'NO_FS', message: `Day ${dayNum}: Morning core desk missing (FS = ${fsCount}, min = ${constraints.fsMin})` });
      }
      if (ssCount < constraints.ssMin) {
        deskViolations.push({ day: dayNum, type: 'NO_SS', message: `Day ${dayNum}: Evening core desk missing (SS = ${ssCount}, min = ${constraints.ssMin})` });
      }
    }

    // Fatigue guard check
    engineers.forEach(eng => {
      let streak = 0;
      for (let d = 0; d < totalDays; d++) {
        const s = eng.shifts[d];
        const isRest = (s === 'OFF' || (s && s.startsWith('SW')) || s === 'L');

        if (isRest) {
          streak = 0;
        } else {
          streak++;
          if (streak > constraints.maxConsecutiveDays) {
            fatigueViolations.push({
              engineer: eng.name,
              day: d + 1,
              message: `${eng.name} worked ${streak} consecutive days on Day ${d + 1} (cap = ${constraints.maxConsecutiveDays})`
            });
          }

          if (d > 0 && eng.shifts[d - 1] === 'NS' && ['FS', 'FG', 'G', 'SG'].includes(s)) {
            fatigueViolations.push({
              engineer: eng.name,
              day: d + 1,
              message: `Post-Night Fatigue: ${eng.name} scheduled for ${s} on Day ${d + 1} immediately following NS on Day ${d}`
            });
          }
        }
      }
    });

    const nightScore = Math.max(0, 100 - (nightViolations.length * 15));
    const deskScore = Math.max(0, 100 - (deskViolations.length * 5));
    const fatigueScore = Math.max(0, 100 - (fatigueViolations.length * 20));
    const score = Math.round((nightScore * 0.45) + (fatigueScore * 0.40) + (deskScore * 0.15));

    return {
      score,
      nightScore,
      deskScore,
      fatigueScore,
      nightViolations,
      deskViolations,
      fatigueViolations,
      dailyCoverage
    };
  }
}
