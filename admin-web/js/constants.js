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
    name: 'Swap Shift (Treated as OFF)',
    timings: 'Rest Day / Peer Covers Shift',
    bg: '#fef08a',
    text: '#854d0e',
    category: 'off',
    minRestHours: 0,
    description: 'Assigned engineer is OFF on that day; peer identified by initial(s) takes shift'
  }
};

/**
 * Standardized Swap Code Generator
 * Rule:
 * 1. Prefix: 'SW'
 * 2. Uses the first letter of the engineer's first name (e.g. MARCUS -> SWM).
 * 3. If multiple engineers share the same first letter, uses the first 2 letters
 *    (e.g., SARAH -> SWSA, SAMIR -> SWSM, DAVID -> SWDA, DANIEL -> SWDN).
 * 4. Any code starting with 'SW' represents a Swap Code where the engineer is OFF.
 */
export function getEngineerSwapCode(engName, allEngineers = null) {
  if (!engName) return 'SW';
  const clean = String(engName).trim().toUpperCase();
  if (clean.includes('SHEFEEQ')) return 'SWSH';
  if (clean.includes('SILAMBARASAN')) return 'SWSI';
  if (clean.includes('DYANI')) return 'SWDY';
  if (clean.includes('RAJ') || clean.includes('KUMAR')) return 'SWRK';
  if (clean.includes('NANDU')) return 'SWN';
  if (clean.includes('ATUL') || clean.includes('ATHUL')) return 'SWA';
  if (clean.includes('HEMANTH')) return 'SWH';
  if (clean.includes('MANOJ')) return 'SWM';
  if (clean.includes('ROHIT')) return 'SWR';
  if (clean.includes('DHANUSH')) return 'SWD';

  const parts = clean.split(/[\s\W]+/).filter(Boolean);
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

  const KNOWN_ORDER = {
    'ALEX RIVERA': 1,
    'DAVID SILVA': 2,
    'SARAH CONNOR': 3,
    'HANNAH KIM': 4,
    'ELENA ROSTOVA': 5,
    'SAMIR PATEL': 6,
    'MARCUS VANCE': 7,
    'ROBERT KING': 8,
    'RACHEL RAY': 9,
    'DANIEL CHEN': 10,
    // Legacy name fallbacks
    'NANDU ANTHIKAT': 1,
    'DYANI DESOZA': 2,
    'SHEFEEQ': 3,
    'HEMANTH': 4,
    'ATUL KANGRALKAR': 5,
    'SILAMBARASAN': 6,
    'MANOJ': 7,
    'RAJ KUMAR': 8,
    'ROHIT': 9,
    'DHANUSH': 10
  };

  const nameUpper = String(eng.name || '').trim().toUpperCase();
  if (KNOWN_ORDER[nameUpper] !== undefined) {
    return KNOWN_ORDER[nameUpper];
  }

  const idStr = String(eng.id || '');
  const match = idStr.match(/\d+/);
  if (match) {
    return parseInt(match[0], 10);
  }
  return 9999;
}

export function sortEngineersByWeekOffs(engineers, getOffsFn = null) {
  if (!Array.isArray(engineers)) return [];
  return [...engineers].sort((a, b) => {
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
    name: "NANDU ANTHIKAT",
    username: "nandua",
    full_name: "Nandu Anthikat",
    email: "nandua@tejasnetworks.com",
    phone: "+919811001103",
    role: "engineer",
    status: "Active",
    join_date: "2024-03-10",
    resignation_date: null,
    default_weekly_offs: ["Sunday", "Monday"],
    preferred_shifts: ["FS", "FG"]
  },
  // Rank 2: Mon, Tue
  {
    name: "SHEFEEQ",
    username: "shefeeqkk",
    full_name: "Shefeeq K K",
    email: "shefeeqkk@tejasnetworks.com",
    phone: "+91 9809226169",
    role: "engineer",
    status: "Active",
    join_date: "2023-05-01",
    resignation_date: null,
    default_weekly_offs: ["Monday", "Tuesday"],
    preferred_shifts: ["G", "FG"]
  },
  // Rank 3: Tue, Wed
  {
    name: "DYANI DESOZA",
    username: "dyanid",
    full_name: "Dyani DeSoza",
    email: "dyanid@tejasnetworks.com",
    phone: "+919811001105",
    role: "engineer",
    status: "Active",
    join_date: "2024-02-01",
    resignation_date: null,
    default_weekly_offs: ["Tuesday", "Wednesday"],
    preferred_shifts: ["SS", "SG"]
  },
  // Rank 4: Wed, Thu
  {
    name: "ATUL KANGRALKAR",
    username: "athulka",
    full_name: "Atul Kangralkar",
    email: "athulka@tejasnetworks.com",
    phone: "+919811001106",
    role: "engineer",
    status: "Active",
    join_date: "2023-08-15",
    resignation_date: null,
    default_weekly_offs: ["Wednesday", "Thursday"],
    preferred_shifts: ["FG", "G"]
  },
  // Rank 5: Thu, Fri
  {
    name: "SILAMBARASAN",
    username: "silambarasan",
    full_name: "Silambarasan V",
    email: "silambarasan@tejasnetworks.com",
    phone: "+919811001107",
    role: "engineer",
    status: "Active",
    join_date: "2024-04-01",
    resignation_date: null,
    default_weekly_offs: ["Thursday", "Friday"],
    preferred_shifts: ["FS", "SS"]
  },
  // Rank 6: Fri, Sat
  {
    name: "HEMANTH",
    username: "hemanthti",
    full_name: "Hemanth K",
    email: "hemanthti@tejasnetworks.com",
    phone: "+919811001108",
    role: "engineer",
    status: "Active",
    join_date: "2024-05-15",
    resignation_date: null,
    default_weekly_offs: ["Friday", "Saturday"],
    preferred_shifts: ["SG", "G"]
  },
  // Rank 7: Sat, Sun
  {
    name: "MANOJ",
    username: "manoj",
    full_name: "Manoj N",
    email: "manoj@tejasnetworks.com",
    phone: "+919811001101",
    role: "engineer",
    status: "Active",
    join_date: "2024-01-15",
    resignation_date: null,
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["FS", "FG", "SS"]
  },
  {
    name: "DHANUSH",
    username: "dhanush",
    full_name: "Dhanush P",
    email: "dhanush@tejasnetworks.com",
    phone: "+919811001110",
    role: "apprentice",
    status: "Active",
    join_date: "2024-06-01",
    resignation_date: null,
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["SS", "G"]
  },
  {
    name: "ROHIT",
    username: "rohit",
    full_name: "Rohit M",
    email: "rohit@tejasnetworks.com",
    phone: "+919811001109",
    role: "apprentice",
    status: "Active",
    join_date: "2023-12-01",
    resignation_date: null,
    default_weekly_offs: ["Saturday", "Sunday"],
    preferred_shifts: ["G"]
  },
  {
    name: "RAJ KUMAR",
    username: "rajkumar",
    full_name: "Raj Kumar S",
    email: "rajkumar@tejasnetworks.com",
    phone: "+919811001102",
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
