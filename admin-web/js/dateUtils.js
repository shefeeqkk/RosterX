/**
 * RosterX — Official Unified Date & Operational Time Engine (dateUtils.js)
 * 
 * Strict Mandate:
 * - Single authoritative source for all calendar, month, period, and time calculations.
 * - Anchored permanently to Indian Standard Time (IST, Asia/Kolkata, UTC+5:30).
 * - Zero hardcoded month names, years, or static dates.
 * - Dynamic rolling period generation (e.g. -3 months to +12 months).
 */

export const ROSTER_TIMEZONE = 'Asia/Kolkata';

/**
 * Returns current Date object anchored to Indian Standard Time (IST).
 * If a custom baseDate is provided (e.g. for simulation/testing), shifts it to IST.
 */
export function getIstNow(baseDate = new Date()) {
  try {
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: ROSTER_TIMEZONE,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
    const parts = formatter.formatToParts(baseDate);
    const p = {};
    for (const part of parts) {
      p[part.type] = part.value;
    }
    // Construct local-representation Date of the IST moment
    return new Date(
      parseInt(p.year, 10),
      parseInt(p.month, 10) - 1,
      parseInt(p.day, 10),
      parseInt(p.hour, 10),
      parseInt(p.minute, 10),
      parseInt(p.second, 10)
    );
  } catch (e) {
    // Fallback: standard UTC+5:30 offset
    const utc = baseDate.getTime() + (baseDate.getTimezoneOffset() * 60000);
    return new Date(utc + (5.5 * 3600000));
  }
}

/**
 * Returns standard operational period key 'YYYY-MM' (e.g. '2026-11') for IST now.
 */
export function currentPeriodKey(baseDate = new Date()) {
  const ist = getIstNow(baseDate);
  const y = ist.getFullYear();
  const m = String(ist.getMonth() + 1).padStart(2, '0');
  return `${y}-${m}`;
}

/**
 * Parses 'YYYY-MM' period key into { year: number, month: number (1-12) }
 */
export function parsePeriodKey(key) {
  if (!key || typeof key !== 'string') {
    const cur = currentPeriodKey();
    const [y, m] = cur.split('-');
    return { year: parseInt(y, 10), month: parseInt(m, 10) };
  }
  const parts = key.trim().split(/[-_/]/);
  const year = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10);
  if (isNaN(year) || isNaN(month) || month < 1 || month > 12) {
    throw new Error(`[dateUtils] Invalid period key: "${key}". Expected YYYY-MM.`);
  }
  return { year, month };
}

/**
 * Returns total days in month for a given period key or (year, month).
 * Properly accounts for leap years (e.g. 2024-02 -> 29 days).
 */
export function daysInMonth(keyOrYear, maybeMonth) {
  let year, month;
  if (typeof keyOrYear === 'string' && keyOrYear.includes('-')) {
    const parsed = parsePeriodKey(keyOrYear);
    year = parsed.year;
    month = parsed.month;
  } else {
    year = parseInt(keyOrYear, 10);
    month = parseInt(maybeMonth, 10);
  }
  return new Date(year, month, 0).getDate();
}

/**
 * Returns array of 12 month names based on requested style.
 * Uses Intl.DateTimeFormat so no hardcoded arrays exist.
 * Styles: 'long' (January), 'upper' (JANUARY), 'short' (Jan), 'shortUpper' (JAN)
 */
export function monthNames(style = 'upper') {
  const list = [];
  const formatter = new Intl.DateTimeFormat('en-US', {
    month: (style === 'short' || style === 'shortUpper') ? 'short' : 'long',
    timeZone: ROSTER_TIMEZONE
  });
  for (let m = 0; m < 12; m++) {
    // 2026-01-15 is arbitrary middle-of-month sample date
    const d = new Date(Date.UTC(2026, m, 15));
    let name = formatter.format(d);
    if (style === 'upper' || style === 'shortUpper') {
      name = name.toUpperCase();
    }
    list.push(name);
  }
  return list;
}

/**
 * Formats a period key into human-readable label.
 * Styles:
 * - 'long': "November 2026"
 * - 'upper': "NOVEMBER 2026"
 * - 'short': "Nov 2026"
 * - 'code': "NOV-26"
 * - 'select': "NOVEMBER 2026 (NOV-26)"
 * - 'withDays': "NOVEMBER 2026 (30 Days)"
 */
export function monthLabel(key, style = 'upper') {
  const { year, month } = parsePeriodKey(key);
  const totalDays = daysInMonth(year, month);
  const sampleDate = new Date(Date.UTC(year, month - 1, 15));

  const longName = new Intl.DateTimeFormat('en-US', { month: 'long', timeZone: ROSTER_TIMEZONE }).format(sampleDate);
  const shortName = new Intl.DateTimeFormat('en-US', { month: 'short', timeZone: ROSTER_TIMEZONE }).format(sampleDate).toUpperCase();
  const yrShort = String(year).slice(-2);

  switch (style) {
    case 'long':
      return `${longName} ${year}`;
    case 'short':
      return `${shortName} ${year}`;
    case 'code':
      return `${shortName}-${yrShort}`;
    case 'select':
      return `${longName.toUpperCase()} ${year} (${shortName}-${yrShort})`;
    case 'withDays':
      return `${longName.toUpperCase()} ${year} (${totalDays} Days)`;
    case 'upper':
    default:
      return `${longName.toUpperCase()} ${year}`;
  }
}

/**
 * Generates rolling range of period keys relative to current month or base date.
 * E.g., periodRange(3, 12) returns 16 periods: [cur - 3 mos, ..., cur, ..., cur + 12 mos]
 */
export function periodRange(back = 3, forward = 12, baseDate = new Date()) {
  const ist = getIstNow(baseDate);
  const curY = ist.getFullYear();
  const curM = ist.getMonth(); // 0-indexed
  const curKey = currentPeriodKey(baseDate);

  const periods = [];
  for (let offset = -back; offset <= forward; offset++) {
    const d = new Date(curY, curM + offset, 1);
    const y = d.getFullYear();
    const m = d.getMonth() + 1;
    const key = `${y}-${String(m).padStart(2, '0')}`;
    periods.push({
      key,
      year: y,
      month: m,
      totalDays: daysInMonth(y, m),
      label: monthLabel(key, 'select'),
      labelWithDays: monthLabel(key, 'withDays'),
      labelUpper: monthLabel(key, 'upper'),
      isCurrent: key === curKey
    });
  }
  return periods;
}

/**
 * Dynamically populates any HTML <select> element with period options.
 * Preserves or selects requested active key (or defaults to current IST month).
 */
export function populatePeriodSelect(selectEl, {
  back = 3,
  forward = 12,
  selectedKey = null,
  style = 'select',
  baseDate = new Date()
} = {}) {
  if (!selectEl) return;
  const currentKey = currentPeriodKey(baseDate);
  const targetKey = selectedKey || currentKey;
  const periods = periodRange(back, forward, baseDate);

  selectEl.innerHTML = '';
  periods.forEach(p => {
    const opt = document.createElement('option');
    opt.value = p.key;
    opt.textContent = style === 'withDays' ? p.labelWithDays : (style === 'upper' ? p.labelUpper : p.label);
    if (p.key === targetKey) {
      opt.selected = true;
    }
    selectEl.appendChild(opt);
  });
  selectEl.value = targetKey;
}

/**
 * Returns formatted localized date string for today in IST.
 * E.g., 'Friday, November 6, 2026'
 */
export function todayFormatted(style = 'long', baseDate = new Date()) {
  const ist = getIstNow(baseDate);
  const options = style === 'short' 
    ? { month: 'short', day: 'numeric', year: 'numeric', timeZone: ROSTER_TIMEZONE }
    : { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric', timeZone: ROSTER_TIMEZONE };
  return new Intl.DateTimeFormat('en-US', options).format(baseDate);
}

/**
 * Returns relative adjacent period key (+1 or -1 month)
 */
export function getAdjacentPeriodKey(key, deltaMonths = 1) {
  const { year, month } = parsePeriodKey(key);
  const d = new Date(year, month - 1 + deltaMonths, 1);
  const nextY = d.getFullYear();
  const nextM = String(d.getMonth() + 1).padStart(2, '0');
  return `${nextY}-${nextM}`;
}

