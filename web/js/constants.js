/**
 * RosterX - Official Domain Constants & Operational Configurations
 * 
 * Provides:
 * - Module A: Official Engineer Directory Schema with lifecycle (Active, Resigned)
 * - Module B: Configurable Operational Constraints & Policies
 * - Module C: Standard 24/7 Shift Codes, Timings & Hex Color Palette
 * - Helpers: Weekly off ordering, shift swap code resolvers, format adapters
 */

export const SHIFTS = {
  FS: {
    code: 'FS',
    name: 'First Shift',
    timings: '07:30 AM - 04:30 PM',
    bg: '#bbf7d0',
    text: '#064e3b',
    category: 'morning',
    minRestHours: 15,
    description: 'Early morning operations & handover'
  },
  FG: {
    code: 'FG',
    name: 'First General Shift',
    timings: '08:30 AM - 05:30 PM',
    bg: '#166534',
    text: '#ffffff',
    category: 'morning',
    minRestHours: 14,
    description: 'Peak morning operations'
  },
  G: {
    code: 'G',
    name: 'General Shift',
    timings: '09:30 AM - 06:30 PM',
    bg: '#bae6fd',
    text: '#0c4a6e',
    category: 'general',
    minRestHours: 13,
    description: 'Standard day operations & coordination'
  },
  SG: {
    code: 'SG',
    name: 'Second General Shift',
    timings: '11:00 AM - 08:00 PM',
    bg: '#ea580c',
    text: '#ffffff',
    category: 'general',
    minRestHours: 11.5,
    description: 'Midday to evening overlap'
  },
  SS: {
    code: 'SS',
    name: 'Second Shift',
    timings: '01:00 PM - 10:00 PM',
    bg: '#fed7aa',
    text: '#7c2d12',
    category: 'evening',
    minRestHours: 9.5,
    description: 'Afternoon & evening monitoring (Cannot rotate to FS)'
  },
  NS: {
    code: 'NS',
    name: 'Night Shift',
    timings: '09:00 PM - 07:30 AM',
    bg: '#020617',
    text: '#ffffff',
    category: 'night',
    minRestHours: 24,
    description: '24/7 overnight critical surveillance'
  },
  OFF: {
    code: 'OFF',
    name: 'Weekly Off',
    timings: 'Rest Day',
    bg: '#fef08a',
    text: '#854d0e',
    category: 'off',
    minRestHours: 0,
    description: 'Mandatory rest / weekend off'
  },
  L: {
    code: 'L',
    name: 'Approved Leave',
    timings: 'Standalone Leave',
    bg: '#dc2626',
    text: '#ffffff',
    category: 'leave',
    minRestHours: 0,
    description: 'Planned vacation or approved leave'
  },
  SW: {
    code: 'SW',
    name: 'Swap Off (Treated as OFF)',
    timings: 'Rest Day / Peer Covers Shift',
    bg: '#fef08a',
    text: '#854d0e',
    category: 'off',
    minRestHours: 0,
    description: 'Assigned engineer is OFF on that day; peer identified by initial(s) takes shift'
  }
};

export const SHIFT_SPECS = {
  FS: { title: 'First Shift (FS)',    startH: 7,  startM: 30, endH: 16, endM: 30, nextDay: false },
  FG: { title: 'First General (FG)',  startH: 8,  startM: 30, endH: 17, endM: 30, nextDay: false },
  G:  { title: 'General Shift (G)',   startH: 9,  startM: 30, endH: 18, endM: 30, nextDay: false },
  SG: { title: 'Second General (SG)', startH: 11, startM: 0,  endH: 20, endM: 0,  nextDay: false },
  SS: { title: 'Second Shift (SS)',   startH: 13, startM: 0,  endH: 22, endM: 0,  nextDay: false },
  NS: { title: 'Night Shift (NS)',    startH: 21, startM: 0,  endH: 7,  endM: 30, nextDay: true }
};

/**
 * Standardized Swap Code Generator
 * Rule:
 * 1. If the engineer record defines an explicit DB `swap_code` (or `swapCode`), use it directly.
 * 2. Prefix: 'SW'
 * 3. Uses the first letter of the engineer's first name (e.g. Member -> SWM).
 * 4. If multiple active engineers share the same first letter, disambiguates using the first 2 letters
 *    (or first + last initials when two words exist and 2-letter prefix collides).
 * 5. Any code starting with 'SW' represents a Swap Code where the engineer is OFF.
 */
export function getEngineerSwapCode(engNameOrObj, allEngineers = null) {
  if (!engNameOrObj) return 'SW';

  // 1. Direct DB-driven swap_code when an engineer object is passed
  let engObj = (typeof engNameOrObj === 'object') ? engNameOrObj : null;
  const rawName = engObj ? (engObj.name || engObj.full_name || engObj.username || '') : String(engNameOrObj);
  const clean = String(rawName).trim().toUpperCase();

  // Lookup in allEngineers pool if name string was passed
  if (!engObj && Array.isArray(allEngineers) && clean) {
    engObj = allEngineers.find(e =>
      e && (
        String(e.name || '').trim().toUpperCase() === clean ||
        String(e.full_name || '').trim().toUpperCase() === clean ||
        String(e.username || '').trim().toUpperCase() === clean
      )
    ) || null;
  }

  const dbCode = engObj && (engObj.swap_code || engObj.swapCode);
  if (dbCode && String(dbCode).trim()) {
    return String(dbCode).trim().toUpperCase();
  }

  if (!clean) return 'SW';
  const parts = clean.split(/[\s\W]+/).filter(Boolean);
  if (parts.length === 0) return 'SW';
  const firstLetter = parts[0][0];

  // 2. Algorithmic disambiguation against peer pool
  if (Array.isArray(allEngineers) && allEngineers.length > 0) {
    const sameInitialPeers = allEngineers.filter(e => {
      if (!e || e.status === 'Resigned') return false;
      const pName = String(e.name || e.full_name || '').trim().toUpperCase();
      const pParts = pName.split(/[\s\W]+/).filter(Boolean);
      return pParts.length > 0 && pParts[0][0] === firstLetter;
    });
    if (sameInitialPeers.length <= 1) {
      return `SW${firstLetter}`;
    }
    const twoLetter = parts[0].slice(0, 2);
    const twoLetterCollisions = sameInitialPeers.filter(e => {
      const pParts = String(e.name || e.full_name || '').trim().toUpperCase().split(/[\s\W]+/).filter(Boolean);
      return pParts.length > 0 && pParts[0].slice(0, 2) === twoLetter;
    });
    if (twoLetterCollisions.length > 1 && parts.length >= 2) {
      return `SW${parts[0][0]}${parts[1][0]}`;
    }
    return `SW${twoLetter}`;
  }

  // Fallback without peer pool
  if (parts.length >= 2) {
    return `SW${parts[0][0]}${parts[1][0]}`;
  }
  return `SW${parts[0].slice(0, 2)}`;
}

export function isSwapShift(code) {
  if (!code) return false;
  return String(code).trim().toUpperCase().startsWith('SW');
}

export function isOffCategory(code) {
  if (!code) return false;
  const upper = String(code).trim().toUpperCase();
  return upper === 'OFF' || upper.startsWith('SW');
}

/**
 * Module B: Default Configurable Operational Constraints
 */
export const DEFAULT_CONSTRAINTS = {
  nightHeadcount: 1, // Exactly 1 per night
  fsMin: 1,          // Minimum 1 FS per morning
  ssMin: 1,          // Minimum 1 SS per evening
  daytimeOverlapMin: 3, // Min staff across FG, G, SG
  postNightRestDays: 2, // 24h - 48h rest immediately following NS
  maxConsecutiveDays: 6, // Max working days before mandatory OFF
  nightBlockMin: 3,  // Min contiguous nights
  nightBlockMax: 5,  // Max contiguous nights
  swapFirstPolicy: true, // Prioritize AI-negotiated swaps over unstaffed Leave (L)
  customDirectives: "System maintenance window scheduled: Ensure senior engineer on SS. Maintain balanced morning presence."
};

/**
 * Module Week-Off Day Order Sorting
 * Standard weekly off pairs starting from Sunday:
 * Rank 1: Sun, Mon
 * Rank 2: Mon, Tue
 * Rank 3: Tue, Wed
 * Rank 4: Wed, Thu
 * Rank 5: Thu, Fri
 * Rank 6: Fri, Sat
 * Rank 7: Sat, Sun
 */
export function getWeekOffSortScore(weeklyOffs) {
  if (!weeklyOffs || !Array.isArray(weeklyOffs) || weeklyOffs.length === 0) {
    return 999;
  }

  const normDays = weeklyOffs
    .map(d => (d ? String(d).trim().toUpperCase().substring(0, 3) : ''))
    .filter(Boolean);

  if (normDays.length === 0) return 999;

  const has = (day) => normDays.includes(day);

  // Exact standard pairs (order-independent)
  if (has('SUN') && has('MON')) return 1;
  if (has('MON') && has('TUE')) return 2;
  if (has('TUE') && has('WED')) return 3;
  if (has('WED') && has('THU')) return 4;
  if (has('THU') && has('FRI')) return 5;
  if (has('FRI') && has('SAT')) return 6;
  if (has('SAT') && has('SUN')) return 7;

  // Fallback for single or non-standard day combinations
  const DAY_INDICES = { 'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6 };
  const indices = normDays.map(d => (d in DAY_INDICES ? DAY_INDICES[d] : 9)).sort((a, b) => a - b);
  const primary = indices[0] !== undefined ? indices[0] : 9;
  const secondary = indices[1] !== undefined ? indices[1] : 9;
  return 10 + primary * 2 + secondary * 0.1;
}

export function getEngineerOrderWeight(eng) {
  if (!eng) return 9999;

  // 1. Primary: DB-driven display_order / displayOrder
  const dbOrder = eng.display_order !== undefined && eng.display_order !== null
    ? eng.display_order
    : eng.displayOrder;
  if (dbOrder !== undefined && dbOrder !== null && String(dbOrder).trim() !== '') {
    const parsed = parseInt(dbOrder, 10);
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }

  // 2. Fallback: numeric ID if present
  const idStr = String(eng.id || '');
  const match = idStr.match(/\d+/);
  if (match) {
    return parseInt(match[0], 10);
  }

  // 3. Default tie-breaker handled alphabetically by sortEngineersByWeekOffs
  return 9999;
}

export function sortEngineersByWeekOffs(engineers, getOffsFn = null, getRoleFn = null) {
  if (!Array.isArray(engineers)) return [];
  return [...engineers].sort((a, b) => {
    // Helper to resolve role for an engineer object
    const resolveRole = (eng) => {
      if (!eng) return '';
      if (getRoleFn) {
        const r = getRoleFn(eng);
        if (r) return String(r).toLowerCase().trim();
      }
      if (eng.role) return String(eng.role).toLowerCase().trim();
      if (typeof window !== 'undefined' && window.app && window.app.state && Array.isArray(window.app.state.engineersList)) {
        const found = window.app.state.engineersList.find(x =>
          (x.id && eng.id && x.id === eng.id) ||
          (x.username && eng.username && x.username === eng.username) ||
          (x.name && eng.name && x.name.toLowerCase().trim() === eng.name.toLowerCase().trim()) ||
          (x.full_name && eng.name && x.full_name.toLowerCase().trim() === eng.name.toLowerCase().trim())
        );
        if (found && found.role) return String(found.role).toLowerCase().trim();
      }
      if (typeof DEFAULT_ENGINEERS !== 'undefined' && Array.isArray(DEFAULT_ENGINEERS)) {
        const found = DEFAULT_ENGINEERS.find(x =>
          (x.id && eng.id && x.id === eng.id) ||
          (x.username && eng.username && x.username === eng.username) ||
          (x.name && eng.name && x.name.toLowerCase().trim() === eng.name.toLowerCase().trim()) ||
          (x.full_name && eng.name && x.full_name.toLowerCase().trim() === eng.name.toLowerCase().trim())
        );
        if (found && found.role) return String(found.role).toLowerCase().trim();
      }
      return '';
    };

    // Global Rule: Apprentices are always placed at the very bottom
    const isApprA = resolveRole(a) === 'apprentice' ? 1 : 0;
    const isApprB = resolveRole(b) === 'apprentice' ? 1 : 0;
    if (isApprA !== isApprB) {
      return isApprA - isApprB;
    }

    let offsA = getOffsFn ? getOffsFn(a) : a.default_weekly_offs;
    if (!offsA || offsA.length === 0) {
      const match = typeof DEFAULT_ENGINEERS !== 'undefined' && Array.isArray(DEFAULT_ENGINEERS)
        ? DEFAULT_ENGINEERS.find(e => e.name === a.name || e.id === a.id)
        : null;
      offsA = match ? match.default_weekly_offs : [];
    }

    let offsB = getOffsFn ? getOffsFn(b) : b.default_weekly_offs;
    if (!offsB || offsB.length === 0) {
      const match = typeof DEFAULT_ENGINEERS !== 'undefined' && Array.isArray(DEFAULT_ENGINEERS)
        ? DEFAULT_ENGINEERS.find(e => e.name === b.name || e.id === b.id)
        : null;
      offsB = match ? match.default_weekly_offs : [];
    }

    const scoreA = getWeekOffSortScore(offsA);
    const scoreB = getWeekOffSortScore(offsB);
    if (scoreA !== scoreB) {
      return scoreA - scoreB;
    }
    const weightA = getEngineerOrderWeight(a);
    const weightB = getEngineerOrderWeight(b);
    if (weightA !== weightB) {
      return weightA - weightB;
    }
    return (a.name || '').localeCompare(b.name || '');
  });
}

/**
 * Module A: Engineer Directory Schema with Lifecycle Management
 * Sorted by Weekoffs Day Order (Sun, Mon -> Mon, Tue -> etc.)
 */
export const DEFAULT_ENGINEERS = [
  // Rank 1: Sun, Mon
  {
    name: "Engineer One",
    username: "engineer1",
    full_name: "Engineer One",
    email: "engineer1@rosterx.example",
    phone: "+919800000001",
    role: "engineer",
    status: "Active",
    join_date: "2024-03-10",
    resignation_date: null,
    default_weekly_offs: ["Sunday", "Monday"],
    preferred_shifts: ["FS", "FG"]
  },
  // Rank 2: Mon, Tue
  {
    name: "Engineer Two",
    username: "engineer2",
    full_name: "Engineer Two",
    email: "engineer2@rosterx.example",
    phone: "+919800000002",
    role: "engineer",
    status: "Active",
    join_date: "2023-05-01",
    resignation_date: null,
    default_weekly_offs: ["Monday", "Tuesday"],
    preferred_shifts: ["G", "FG"]
  },
  // Rank 3: Tue, Wed
  {
    name: "Engineer Three",
    username: "engineer3",
    full_name: "Engineer Three",
    email: "engineer3@rosterx.example",
    phone: "+919800000003",
    role: "engineer",
    status: "Active",
    join_date: "2024-02-01",
    resignation_date: null,
    default_weekly_offs: ["Tuesday", "Wednesday"],
    preferred_shifts: ["SS", "SG"]
  },
  // Rank 4: Wed, Thu
  {
    name: "Engineer Four",
    username: "engineer4",
    full_name: "Engineer Four",
    email: "engineer4@rosterx.example",
    phone: "+919800000004",
    role: "engineer",
    status: "Active",
    join_date: "2023-08-15",
    resignation_date: null,
    default_weekly_offs: ["Wednesday", "Thursday"],
    preferred_shifts: ["FG", "G"]
  },
  // Rank 5: Thu, Fri
  {
    name: "Engineer Five",
    username: "engineer5",
    full_name: "Engineer Five",
    email: "engineer5@rosterx.example",
    phone: "+919800000005",
    role: "engineer",
    status: "Active",
    join_date: "2024-04-01",
    resignation_date: null,
    default_weekly_offs: ["Thursday", "Friday"],
    preferred_shifts: ["FS", "SS"]
  },
  // Rank 6: Fri, Sat
  {
    name: "Engineer Six",
    username: "engineer6",
    full_name: "Engineer Six",
    email: "engineer6@rosterx.example",
    phone: "+919800000006",
    role: "engineer",
    status: "Active",
    join_date: "2024-05-15",
    resignation_date: null,
    default_weekly_offs: ["Friday", "Saturday"],
    preferred_shifts: ["SG", "G"]
  },
  // Rank 7: Sat, Sun
  {
    name: "Engineer Seven",
    username: "engineer7",
    full_name: "Engineer Seven",
    email: "engineer7@rosterx.example",
    phone: "+919800000007",
    role: "engineer",
    status: "Active",
    join_date: "2024-01-15",
    resignation_date: null,
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["FS", "FG", "SS"]
  },
  {
    name: "Apprentice One",
    username: "apprentice1",
    full_name: "Apprentice One",
    email: "apprentice1@rosterx.example",
    phone: "+919800000008",
    role: "apprentice",
    status: "Active",
    join_date: "2024-06-01",
    resignation_date: null,
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["SS", "G"]
  },
  {
    name: "Apprentice Two",
    username: "apprentice2",
    full_name: "Apprentice Two",
    email: "apprentice2@rosterx.example",
    phone: "+919800000009",
    role: "apprentice",
    status: "Active",
    join_date: "2023-12-01",
    resignation_date: null,
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["G"]
  },
  {
    name: "Former Engineer",
    username: "former1",
    full_name: "Former Engineer",
    email: "former1@rosterx.example",
    phone: "+919800000010",
    role: "engineer",
    status: "Resigned",
    join_date: "2023-11-01",
    resignation_date: "2026-10-20",
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["SS", "G"]
  }
];

export function shiftsArrayToMap(arr) {
  const map = {};
  arr.forEach((shift, idx) => {
    map[String(idx + 1)] = shift;
  });
  return map;
}

export function shiftsMapToArray(map, totalDays = 31) {
  const arr = [];
  for (let d = 1; d <= totalDays; d++) {
    arr.push(map[String(d)] || map[d] || 'OFF');
  }
  return arr;
}

export const ENGINEERS = DEFAULT_ENGINEERS;

export function toProperCase(str) {
  if (!str || typeof str !== 'string') return '';
  return str.trim().split(/\s+/).map(word => {
    if (word.length === 1) return word.toUpperCase();
    return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
  }).join(' ');
}
