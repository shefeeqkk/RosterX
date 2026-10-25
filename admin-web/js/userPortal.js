/**
 * RosterX — NOC Operations Portal Controller (userPortal.js)
 * 
 * Features:
 * - 24/7 Operations Command view for NOC Engineers & Apprentices
 * - Live Operational Duty Briefing (Today: Oct 24, Tomorrow: Oct 25) remains persistent and unaffected by grid month browsing
 * - Month Dropdown positioned in Month Grid toolbar; changing month ONLY updates the Month Shift Master Grid
 * - Pixel-aligned Month Shift Master Grid with standard NOC color codes & Excel export
 * - User Profile & Account Data viewer with Change Password capability
 * - Interactive Cutout 3D Bot AI Assistant
 */

import { authService } from './authService.js';
import { AtlasService } from './atlasService.js';
import { 
  SHIFTS, 
  DEFAULT_ENGINEERS, 
  sortEngineersByWeekOffs,
  shiftsMapToArray,
  isOffCategory
} from './constants.js?v=1.0';
import { ExcelService } from './excelService.js?v=3.2';

const STORAGE_KEYS = {
  GRID_ROSTER_PREFIX: 'rosterx_grid_roster_',
  ENGINEERS_LIST: 'rosterx_engineers_list',
  CONSTRAINTS: 'rosterx_constraints'
};

export class UserPortalController {
  constructor() {
    this.atlasService = new AtlasService();
    this.currentUser = null;
    this.matchedEngineer = null;

    // 1. Live Operational Anchor State (Today & Tomorrow & Team Board)
    // Dynamically synchronized to current real date and time
    const now = new Date();
    const curYear = now.getFullYear();
    const curMonth = now.getMonth() + 1; // 1-12
    let curDay = now.getDate();

    const totalDays = new Date(curYear, curMonth, 0).getDate();
    if (curDay > totalDays) curDay = totalDays;

    const tomorrow = new Date(now);
    tomorrow.setDate(now.getDate() + 1);
    const tomDay = tomorrow.getDate();

    this.liveState = {
      year: curYear,
      month: curMonth,
      todayDay: curDay,
      tomorrowDay: tomDay,
      totalDays: totalDays,
      roster: []
    };

    // 2. Month Shift Grid State (Independently selected in the Grid section)
    this.gridState = {
      year: curYear,
      month: curMonth,
      totalDays: totalDays,
      roster: [],
      filterTerm: ''
    };

    this.engineersList = [];
    this.constraints = {};
    this.chatMessages = [];
  }

  async init() {
    // 1. Enforce Authentication
    this.currentUser = authService.enforceAuth('any');
    if (!this.currentUser) return;

    // 2. Load Directory & Constraints
    this.loadEngineersList();
    this.loadConstraints();
    await this.syncEngineersFromAtlas();

    // 3. Load Live Operational Roster (October 2026 Anchor)
    await this.loadLiveRoster(this.liveState.year, this.liveState.month);
    this.matchCurrentUserToEngineer();

    // 4. Load Initial Month Grid Roster (Initially October 2026)
    await this.loadGridRoster(this.gridState.year, this.gridState.month);

    // 5. Render Live Operational Components (Persistent Today, Tomorrow, Team Schedule)
    this.renderHeaderUserInfo();
    this.renderDashboardHero();
    this.renderPersonalShiftCards();
    this.renderTeamDutyBoard();

    // 6. Render Month Grid Components (Isolated to Month Grid section)
    this.renderMonthGridHeaders();
    this.renderMonthGridRoster();

    // 7. Setup Events, Account Modal & Chat Assistant
    this.bindEvents();
    this.setupAccountProfileModal();
    this.initAiAssistant();

    // 8. Auto-refresh live operational status every minute
    setInterval(() => {
      this.renderDashboardHero();
      this.renderPersonalShiftCards();
      this.renderTeamDutyBoard();
    }, 60000);
  }

  loadEngineersList() {
    let list = null;
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.ENGINEERS_LIST);
      if (stored) list = JSON.parse(stored);
    } catch (e) {}
    if (!list || !Array.isArray(list) || list.length === 0) {
      list = JSON.parse(JSON.stringify(DEFAULT_ENGINEERS));
    }
    this.engineersList = sortEngineersByWeekOffs(list);
  }

  async syncEngineersFromAtlas() {
    try {
      const docs = await this.atlasService.getEngineers();
      if (Array.isArray(docs) && docs.length > 0) {
        const merged = this.atlasService.mergeEngineers(docs, this.engineersList);
        this.engineersList = sortEngineersByWeekOffs(merged);
        localStorage.setItem(STORAGE_KEYS.ENGINEERS_LIST, JSON.stringify(this.engineersList));
        if (typeof window !== 'undefined' && window.authService) {
          window.authService.syncEngineersList(this.engineersList);
        }
      }
    } catch (e) {
      console.warn('UserPortal: failed to sync engineers from Atlas:', e);
    }
  }

  loadConstraints() {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.CONSTRAINTS);
      if (stored) this.constraints = JSON.parse(stored);
    } catch (e) {}
  }

  /**
   * Load Live Operational Roster (stays anchored to active period)
   */
  async loadLiveRoster(year, month) {
    const monthKey = `${year}-${String(month).padStart(2, '0')}`;
    const cacheKey = `${STORAGE_KEYS.GRID_ROSTER_PREFIX}${year}_${String(month).padStart(2, '0')}`;
    let loaded = null;

    // 1. Fetch remote published roster from MongoDB Atlas
    try {
      const remoteDoc = await this.atlasService.getRoster(monthKey);
      if (remoteDoc && remoteDoc.engineers && remoteDoc.engineers.length > 0) {
        loaded = remoteDoc.engineers.map(e => {
          let shifts = e.shifts;
          if (shifts && !Array.isArray(shifts) && typeof shifts === 'object') {
            shifts = shiftsMapToArray(shifts, this.liveState.totalDays);
          }
          const defEng = this.engineersList.find(x => x.name === e.name || x.id === e.engineer_id || x.id === e.id) || {};
          return {
            id: e.engineer_id || e.id || defEng.id || `eng_${e.name.toLowerCase().replace(/[\s\W]+/g, '')}`,
            name: e.name,
            shifts: Array.isArray(shifts) ? [...shifts] : new Array(this.liveState.totalDays).fill(''),
            status: e.status || defEng.status || 'Active',
            role: e.role || defEng.role || 'engineer',
            default_weekly_offs: defEng.default_weekly_offs || e.default_weekly_offs || ['Saturday', 'Sunday']
          };
        });
        localStorage.setItem(cacheKey, JSON.stringify(loaded));
      }
    } catch (e) {
      console.warn('UserPortal: failed to fetch live roster from Atlas:', e);
    }

    // 2. Fallback to LocalStorage
    if (!loaded) {
      try {
        const stored = localStorage.getItem(cacheKey);
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) loaded = parsed;
        }
      } catch (e) {}
    }

    // 3. Fallback: Dynamic blank roster with active directory personnel
    if (!loaded) {
      const active = this.engineersList.filter(e => e.status !== 'Resigned');
      loaded = active.map(e => ({
        id: e.id,
        name: e.name,
        shifts: new Array(this.liveState.totalDays).fill(''),
        status: e.status || 'Active',
        role: e.role || 'engineer',
        default_weekly_offs: e.default_weekly_offs || ['Saturday', 'Sunday']
      }));
    }

    this.liveState.roster = sortEngineersByWeekOffs(loaded);
  }

  /**
   * Load Grid Roster for the Month Grid section ONLY
   */
  async loadGridRoster(year, month) {
    this.gridState.year = year;
    this.gridState.month = month;
    this.gridState.totalDays = new Date(year, month, 0).getDate();

    const monthKey = `${year}-${String(month).padStart(2, '0')}`;
    const cacheKey = `${STORAGE_KEYS.GRID_ROSTER_PREFIX}${year}_${String(month).padStart(2, '0')}`;
    let loaded = null;

    // 1. Fetch remote published roster from MongoDB Atlas
    try {
      const remoteDoc = await this.atlasService.getRoster(monthKey);
      if (remoteDoc && remoteDoc.engineers && remoteDoc.engineers.length > 0) {
        loaded = remoteDoc.engineers.map(e => {
          let shifts = e.shifts;
          if (shifts && !Array.isArray(shifts) && typeof shifts === 'object') {
            shifts = shiftsMapToArray(shifts, this.gridState.totalDays);
          }
          const defEng = this.engineersList.find(x => x.name === e.name || x.id === e.engineer_id || x.id === e.id) || {};
          return {
            id: e.engineer_id || e.id || defEng.id || `eng_${e.name.toLowerCase().replace(/[\s\W]+/g, '')}`,
            name: e.name,
            shifts: Array.isArray(shifts) ? [...shifts] : new Array(this.gridState.totalDays).fill(''),
            status: e.status || defEng.status || 'Active',
            role: e.role || defEng.role || 'engineer',
            default_weekly_offs: defEng.default_weekly_offs || e.default_weekly_offs || ['Saturday', 'Sunday']
          };
        });
        localStorage.setItem(cacheKey, JSON.stringify(loaded));
      }
    } catch (e) {
      console.warn('UserPortal: failed to fetch grid roster from Atlas:', e);
    }

    // 2. Fallback to LocalStorage
    if (!loaded) {
      try {
        const stored = localStorage.getItem(cacheKey);
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) loaded = parsed;
        }
      } catch (e) {}
    }

    // 3. Fallback: Dynamic blank roster with active directory personnel
    if (!loaded) {
      const active = this.engineersList.filter(e => e.status !== 'Resigned');
      loaded = active.map(e => ({
        id: e.id,
        name: e.name,
        shifts: new Array(this.gridState.totalDays).fill(''),
        status: e.status || 'Active',
        role: e.role || 'engineer',
        default_weekly_offs: e.default_weekly_offs || ['Saturday', 'Sunday']
      }));
    }

    this.gridState.roster = sortEngineersByWeekOffs(loaded);
  }

  /**
   * Match current logged in user to an engineer in the roster
   */
  matchCurrentUserToEngineer() {
    const u = this.currentUser;
    if (!u) return;

    const uName = (u.name || '').trim().toUpperCase();
    const uFull = (u.full_name || '').trim().toUpperCase();
    const uUser = (u.username || '').trim().toUpperCase();
    const uId = u.engineerId || u.id;

    let match = this.liveState.roster.find(e => 
      (uId && (e.id === uId)) ||
      (e.name && e.name.trim().toUpperCase() === uName) ||
      (e.name && uFull && e.name.trim().toUpperCase() === uFull) ||
      (e.name && e.name.trim().toUpperCase().includes(uUser))
    );

    if (!match && u.role === 'admin') {
      match = this.liveState.roster.find(e => e.name.toUpperCase().includes('SHEFEEQ')) || this.liveState.roster[0];
    }

    this.matchedEngineer = match || this.liveState.roster[0];
  }

  renderHeaderUserInfo() {
    const nameEl = document.getElementById('userDisplayName');
    const roleEl = document.getElementById('userRoleBadge');

    if (nameEl) nameEl.textContent = this.currentUser.name || this.currentUser.username;
    if (roleEl) {
      const role = (this.currentUser.role || 'engineer').toUpperCase();
      roleEl.textContent = role === 'ADMIN' ? 'ADMINISTRATOR' : (role === 'APPRENTICE' ? 'APPRENTICE' : 'NOC ENGINEER');
      if (role === 'ADMIN') {
        roleEl.className = 'text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 border border-purple-500/30';
      } else if (role === 'APPRENTICE') {
        roleEl.className = 'text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30';
      } else {
        roleEl.className = 'text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-500/30';
      }
    }
  }

  /**
   * Determine exact real-time operational duty status for an engineer
   * ONLY reports "On Duty" if current clock time is strictly within the active shift hours
   */
  getRealtimeDutyStatus(engineer, dayNumber, now = new Date()) {
    if (!engineer) {
      return { isOnDuty: false, activeCode: '', label: 'Unscheduled', statusHtml: '<span class="text-xs text-slate-500 font-mono">--</span>' };
    }

    const curMins = now.getHours() * 60 + now.getMinutes();
    const todayCode = this.getShiftForDay(engineer, dayNumber) || 'OFF';
    
    // Yesterday's shift code (needed for NS overnight duty before 07:30 AM)
    const yesterdayCode = dayNumber > 1 ? this.getShiftForDay(engineer, dayNumber - 1) : '';

    // Shift Schedule Definitions (in minutes from midnight)
    const SHIFT_RANGES = {
      FS: { start: 7 * 60 + 30,  end: 16 * 60 + 30, startStr: '07:30 AM' },
      FG: { start: 8 * 60 + 30,  end: 17 * 60 + 30, startStr: '08:30 AM' },
      G:  { start: 9 * 60 + 30,  end: 18 * 60 + 30, startStr: '09:30 AM' },
      SG: { start: 11 * 60,      end: 20 * 60,       startStr: '11:00 AM' },
      SS: { start: 13 * 60,      end: 22 * 60,       startStr: '01:00 PM' },
      NS: { start: 21 * 60,      end: 7 * 60 + 30,  startStr: '09:00 PM' }
    };

    // 1. Check Night Shift (spans overnight from 09:00 PM to 07:30 AM)
    // Overnight early morning (between midnight 00:00 and 07:30 AM):
    if (curMins < 7 * 60 + 30) {
      if (yesterdayCode === 'NS') {
        return {
          isOnDuty: true,
          activeCode: 'NS',
          label: 'On Duty (NS)',
          statusHtml: `<span class="inline-flex items-center gap-1.5 text-xs text-indigo-300 font-bold bg-indigo-950/80 px-2.5 py-1 rounded-full border border-indigo-500/60 shadow-sm"><span class="w-2 h-2 rounded-full bg-indigo-400 animate-pulse"></span> On Duty (NS) 🌙</span>`
        };
      }
    }
    // Night evening (from 09:00 PM onwards):
    if (curMins >= 21 * 60) {
      if (todayCode === 'NS') {
        return {
          isOnDuty: true,
          activeCode: 'NS',
          label: 'On Duty (NS)',
          statusHtml: `<span class="inline-flex items-center gap-1.5 text-xs text-indigo-300 font-bold bg-indigo-950/80 px-2.5 py-1 rounded-full border border-indigo-500/60 shadow-sm"><span class="w-2 h-2 rounded-full bg-indigo-400 animate-pulse"></span> On Duty (NS) 🌙</span>`
        };
      }
    }

    // 2. Check Day Shifts (FS, FG, G, SG, SS)
    const range = SHIFT_RANGES[todayCode];
    if (range && todayCode !== 'NS') {
      if (curMins >= range.start && curMins < range.end) {
        return {
          isOnDuty: true,
          activeCode: todayCode,
          label: `On Duty (${todayCode})`,
          statusHtml: `<span class="inline-flex items-center gap-1.5 text-xs text-emerald-300 font-bold bg-emerald-950/80 px-2.5 py-1 rounded-full border border-emerald-500/60 shadow-sm"><span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span> On Duty (${todayCode}) 🟢</span>`
        };
      }
    }

    // 3. Not currently on duty:
    if (isOffCategory(todayCode) || todayCode === 'OFF') {
      return {
        isOnDuty: false,
        activeCode: todayCode,
        label: 'Weekly Off',
        statusHtml: `<span class="inline-flex items-center gap-1 text-xs text-amber-400 font-medium bg-amber-950/40 px-2.5 py-1 rounded-full border border-amber-800/40">Weekly Off</span>`
      };
    }

    if (todayCode === 'L') {
      return {
        isOnDuty: false,
        activeCode: 'L',
        label: 'On Leave',
        statusHtml: `<span class="inline-flex items-center gap-1 text-xs text-rose-400 font-medium bg-rose-950/40 px-2.5 py-1 rounded-full border border-rose-800/40">On Leave</span>`
      };
    }

    // Shift upcoming later today
    if (range && curMins < range.start) {
      return {
        isOnDuty: false,
        activeCode: todayCode,
        label: `Off Duty (Starts ${range.startStr})`,
        statusHtml: `<span class="inline-flex items-center gap-1 text-xs text-slate-400 bg-slate-800/70 px-2.5 py-1 rounded-full border border-slate-700/60 font-medium">Off Duty • Starts ${range.startStr}</span>`
      };
    }

    // Shift finished earlier today
    if (range && curMins >= range.end && todayCode !== 'NS') {
      return {
        isOnDuty: false,
        activeCode: todayCode,
        label: 'Shift Ended',
        statusHtml: `<span class="inline-flex items-center gap-1 text-xs text-slate-500 bg-slate-900/60 px-2.5 py-1 rounded-full border border-slate-800 font-medium">Shift Ended • Off Duty</span>`
      };
    }

    return {
      isOnDuty: false,
      activeCode: todayCode,
      label: 'Off Duty',
      statusHtml: `<span class="inline-flex items-center gap-1 text-xs text-slate-400 bg-slate-800/60 px-2.5 py-1 rounded-full border border-slate-700/60 font-medium">Off Duty</span>`
    };
  }

  /**
   * Render hero welcome section — persistently anchored to Today
   */
  renderDashboardHero() {
    const welcomeName = document.getElementById('heroWelcomeName');
    const dateText = document.getElementById('heroCurrentDate');
    const shiftBadge = document.getElementById('heroDutyBadge');

    const displayName = (this.matchedEngineer ? this.matchedEngineer.name : this.currentUser.name) || 'Engineer';
    if (welcomeName) welcomeName.textContent = displayName;

    // Anchor: Today's live calendar date
    const now = new Date();
    const opDay = this.liveState.todayDay;
    const dateObj = new Date(this.liveState.year, this.liveState.month - 1, opDay);
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const dateStr = dateObj.toLocaleDateString('en-US', options);
    if (dateText) dateText.textContent = dateStr;

    const dutyStatus = this.getRealtimeDutyStatus(this.matchedEngineer, opDay, now);
    if (shiftBadge) {
      if (dutyStatus.isOnDuty) {
        if (dutyStatus.activeCode === 'NS') {
          shiftBadge.className = 'px-3.5 py-1.5 rounded-xl text-xs font-bold bg-indigo-500/20 text-indigo-200 border border-indigo-500/40 flex items-center gap-2 shadow-sm';
          shiftBadge.innerHTML = `<span class="w-2 h-2 rounded-full bg-indigo-400 animate-pulse"></span> On Duty (NS) 🌙`;
        } else {
          shiftBadge.className = 'px-3.5 py-1.5 rounded-xl text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center gap-2 shadow-sm';
          shiftBadge.innerHTML = `<span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span> On Duty (${dutyStatus.activeCode}) 🟢`;
        }
      } else {
        const todayShiftCode = this.getShiftForDay(this.matchedEngineer, opDay);
        if (isOffCategory(todayShiftCode)) {
          shiftBadge.className = 'px-3.5 py-1.5 rounded-xl text-xs font-bold bg-amber-500/20 text-amber-300 border border-amber-500/40 flex items-center gap-2 shadow-sm';
          shiftBadge.innerHTML = `<span class="w-2 h-2 rounded-full bg-amber-400"></span> Weekly Off (${todayShiftCode})`;
        } else if (todayShiftCode) {
          shiftBadge.className = 'px-3.5 py-1.5 rounded-xl text-xs font-bold bg-slate-800/80 text-slate-300 border border-slate-700 flex items-center gap-2 shadow-sm';
          shiftBadge.innerHTML = `<span class="w-2 h-2 rounded-full bg-slate-400"></span> ${dutyStatus.label}`;
        } else {
          shiftBadge.className = 'px-3.5 py-1.5 rounded-xl text-xs font-bold bg-slate-800/80 text-slate-400 border border-slate-700 flex items-center gap-2 shadow-sm';
          shiftBadge.innerHTML = `<span class="w-2 h-2 rounded-full bg-slate-500"></span> Unscheduled`;
        }
      }
    }
  }

  getShiftForDay(engineer, dayNumber) {
    if (!engineer || !engineer.shifts) return 'OFF';
    const idx = dayNumber - 1;
    return engineer.shifts[idx] || '';
  }

  /**
   * Render personal Today & Tomorrow cards + monthly metrics — persistently anchored to Today
   */
  renderPersonalShiftCards() {
    const opDay = this.liveState.todayDay;
    const nextDay = this.liveState.tomorrowDay;
    const eng = this.matchedEngineer;
    if (!eng) return;

    const todayDateObj = new Date(this.liveState.year, this.liveState.month - 1, opDay);
    const todayMonthStr = todayDateObj.toLocaleDateString('en-US', { month: 'short' });
    const tomDateObj = new Date(this.liveState.year, this.liveState.month - 1, opDay);
    tomDateObj.setDate(tomDateObj.getDate() + 1);
    const tomMonthStr = tomDateObj.toLocaleDateString('en-US', { month: 'short' });

    // Card Titles
    const title1 = document.getElementById('todayCardTitle');
    const title2 = document.getElementById('tomorrowCardTitle');
    if (title1) title1.innerHTML = `<i class="ph ph-clock text-cyan-400 text-sm"></i> Today's Shift (${todayMonthStr} ${opDay})`;
    if (title2) title2.innerHTML = `<i class="ph ph-calendar-plus text-indigo-400 text-sm"></i> Tomorrow's Shift (${tomMonthStr} ${nextDay})`;

    // Today Card Live Operational Status Badge
    const todayCardStatusBadge = document.getElementById('todayCardStatusBadge');
    if (todayCardStatusBadge) {
      const dutyStatus = this.getRealtimeDutyStatus(eng, opDay);
      if (dutyStatus.isOnDuty) {
        todayCardStatusBadge.className = 'text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 border border-emerald-800 flex items-center gap-1 shadow-sm';
        todayCardStatusBadge.innerHTML = '<span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span> Active Now';
      } else if (dutyStatus.activeCode === 'OFF' || isOffCategory(dutyStatus.activeCode)) {
        todayCardStatusBadge.className = 'text-[10px] font-bold px-2 py-0.5 rounded bg-amber-950/80 text-amber-300 border border-amber-800/60';
        todayCardStatusBadge.textContent = 'Weekly Off';
      } else {
        todayCardStatusBadge.className = 'text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700';
        todayCardStatusBadge.textContent = dutyStatus.label || 'Scheduled';
      }
    }

    const todayCode = this.getShiftForDay(eng, opDay) || 'OFF';
    const tomorrowCode = this.getShiftForDay(eng, nextDay) || 'OFF';

    const todayInfo = SHIFTS[todayCode] || { name: todayCode, timings: 'Operational Duty', bg: '#334155', text: '#ffffff', desc: 'Assigned duty' };
    const tomorrowInfo = SHIFTS[tomorrowCode] || { name: tomorrowCode, timings: 'Operational Duty', bg: '#334155', text: '#ffffff', desc: 'Assigned duty' };

    // Today's Shift Card DOM
    const todayCodeEl = document.getElementById('todayShiftCode');
    const todayNameEl = document.getElementById('todayShiftName');
    const todayTimingsEl = document.getElementById('todayShiftTimings');
    const todayDescEl = document.getElementById('todayShiftDesc');

    if (todayCodeEl) {
      todayCodeEl.textContent = todayCode;
      todayCodeEl.style.backgroundColor = todayInfo.bg;
      todayCodeEl.style.color = todayInfo.text;
    }
    if (todayNameEl) todayNameEl.textContent = todayInfo.name;
    if (todayTimingsEl) todayTimingsEl.textContent = todayInfo.timings;
    if (todayDescEl) todayDescEl.textContent = todayInfo.description || todayInfo.desc || 'Standard scheduled operational assignment.';

    // Tomorrow's Shift Card DOM
    const tomorrowCodeEl = document.getElementById('tomorrowShiftCode');
    const tomorrowNameEl = document.getElementById('tomorrowShiftName');
    const tomorrowTimingsEl = document.getElementById('tomorrowShiftTimings');
    const tomorrowDescEl = document.getElementById('tomorrowShiftDesc');

    if (tomorrowCodeEl) {
      tomorrowCodeEl.textContent = tomorrowCode;
      tomorrowCodeEl.style.backgroundColor = tomorrowInfo.bg;
      tomorrowCodeEl.style.color = tomorrowInfo.text;
    }
    if (tomorrowNameEl) tomorrowNameEl.textContent = tomorrowInfo.name;
    if (tomorrowTimingsEl) tomorrowTimingsEl.textContent = tomorrowInfo.timings;
    if (tomorrowDescEl) {
      const restReq = tomorrowInfo.minRestHours ? `Rest requirement: ${tomorrowInfo.minRestHours}h` : 'Standard rotation sequence.';
      tomorrowDescEl.textContent = `${tomorrowInfo.description || tomorrowInfo.desc || 'Scheduled duty'}. ${restReq}`;
    }

    // Monthly Metrics (Live Month)
    let dayCount = 0;
    let nightCount = 0;
    let offCount = 0;

    if (Array.isArray(eng.shifts)) {
      eng.shifts.forEach(s => {
        if (!s) return;
        if (s === 'NS') nightCount++;
        else if (isOffCategory(s)) offCount++;
        else dayCount++;
      });
    }

    const metricDayEl = document.getElementById('metricDayCount');
    const metricNightEl = document.getElementById('metricNightCount');
    const metricOffEl = document.getElementById('metricOffCount');
    const metricTotalEl = document.getElementById('metricTotalDuty');

    if (metricDayEl) metricDayEl.textContent = dayCount;
    if (metricNightEl) metricNightEl.textContent = nightCount;
    if (metricOffEl) metricOffEl.textContent = offCount;
    if (metricTotalEl) metricTotalEl.textContent = (dayCount + nightCount);
  }

  /**
   * Render 24/7 Team Duty Schedule — persistently shows Today & Tomorrow for everyone
   */
  renderTeamDutyBoard() {
    const tableBody = document.getElementById('teamDutyTableBody');
    if (!tableBody) return;

    const opDay = this.liveState.todayDay;
    const nextDay = this.liveState.tomorrowDay;

    const todayDateObj = new Date(this.liveState.year, this.liveState.month - 1, opDay);
    const todayMonthStr = todayDateObj.toLocaleDateString('en-US', { month: 'short' });
    const tomDateObj = new Date(this.liveState.year, this.liveState.month - 1, opDay);
    tomDateObj.setDate(tomDateObj.getDate() + 1);
    const tomMonthStr = tomDateObj.toLocaleDateString('en-US', { month: 'short' });

    const h1 = document.getElementById('teamHeaderToday');
    const h2 = document.getElementById('teamHeaderTomorrow');
    if (h1) h1.textContent = `Today's Shift (${todayMonthStr} ${opDay})`;
    if (h2) h2.textContent = `Tomorrow's Shift (${tomMonthStr} ${nextDay})`;

    const loggedInId = this.matchedEngineer ? this.matchedEngineer.id : null;
    const loggedInName = this.matchedEngineer ? this.matchedEngineer.name : '';
    const now = new Date();

    const rowsHtml = this.liveState.roster.map(eng => {
      const isMe = (loggedInId && eng.id === loggedInId) || (eng.name === loggedInName);
      const todayCode = this.getShiftForDay(eng, opDay) || 'OFF';
      const tomorrowCode = this.getShiftForDay(eng, nextDay) || 'OFF';

      const todayShiftMeta = SHIFTS[todayCode] || { bg: '#334155', text: '#ffffff', timings: 'Standard Duty' };
      const tomorrowShiftMeta = SHIFTS[tomorrowCode] || { bg: '#334155', text: '#ffffff', timings: 'Standard Duty' };

      const trClass = isMe 
        ? 'bg-gradient-to-r from-emerald-950/70 via-slate-900/90 to-emerald-950/70 border-l-4 border-l-emerald-400 border-y border-emerald-500/30 font-semibold shadow-md relative' 
        : 'hover:bg-slate-800/40 transition border-b border-slate-800';

      const userStar = isMe 
        ? `<i class="ph-fill ph-star text-amber-400 text-xs ml-1.5 shrink-0" title="Your Schedule"></i>`
        : '';

      const roleBadge = eng.role === 'apprentice'
        ? `<span class="text-[10px] text-amber-400 font-mono">(Apprentice)</span>`
        : `<span class="text-[10px] text-slate-400 font-mono">(NOC Eng)</span>`;

      const offs = (eng.default_weekly_offs || ['Saturday', 'Sunday']).join(', ');
      const dutyStatus = this.getRealtimeDutyStatus(eng, opDay, now);

      return `
        <tr class="${trClass}">
          <td class="py-3 px-4 text-left">
            <div class="flex items-center gap-2">
              <div class="w-8 h-8 rounded-full ${isMe ? 'bg-emerald-500 text-slate-950 font-black ring-2 ring-emerald-400 shadow-[0_0_10px_rgba(16,185,129,0.5)]' : 'bg-slate-700 text-slate-300'} flex items-center justify-center text-xs font-bold shrink-0">
                ${eng.name.slice(0, 2).toUpperCase()}
              </div>
              <div>
                <div class="text-sm font-bold ${isMe ? 'text-emerald-300 flex items-center' : 'text-slate-200'}">
                  ${eng.name} ${userStar}
                </div>
                <div class="text-xs text-slate-400">
                  ${roleBadge} • Offs: <span class="text-slate-300">${offs}</span>
                </div>
              </div>
            </div>
          </td>

          <!-- Today Shift Cell -->
          <td class="py-3 px-4 text-center">
            <div class="inline-flex flex-col items-center">
              <span class="px-3 py-1 rounded-lg text-xs font-extrabold shadow-sm" style="background-color: ${todayShiftMeta.bg}; color: ${todayShiftMeta.text};">
                ${todayCode}
              </span>
              <span class="text-[10px] text-slate-400 mt-0.5 font-mono">
                ${todayShiftMeta.timings || ''}
              </span>
            </div>
          </td>

          <!-- Tomorrow Shift Cell -->
          <td class="py-3 px-4 text-center">
            <div class="inline-flex flex-col items-center">
              <span class="px-3 py-1 rounded-lg text-xs font-extrabold shadow-sm" style="background-color: ${tomorrowShiftMeta.bg}; color: ${tomorrowShiftMeta.text};">
                ${tomorrowCode}
              </span>
              <span class="text-[10px] text-slate-400 mt-0.5 font-mono">
                ${tomorrowShiftMeta.timings || ''}
              </span>
            </div>
          </td>

          <!-- Operational Status Cell: ONLY On Duty if current clock time is in shift window -->
          <td class="py-3 px-4 text-right pr-6">
            ${dutyStatus.statusHtml}
          </td>
        </tr>
      `;
    }).join('');

    tableBody.innerHTML = rowsHtml;
  }

  /**
   * Render headers for Month Shift Grid using gridState (Selected Month)
   */
  renderMonthGridHeaders() {
    const datesRow = document.getElementById('gridHeaderDatesRow');
    const daysRow = document.getElementById('gridHeaderDaysRow');
    if (!datesRow || !daysRow) return;

    const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    const periodTag = `${monthNames[this.gridState.month - 1]}-${String(this.gridState.year).slice(-2)}`;

    let row1 = `
      <th rowspan="2" class="sticky left-0 z-30 bg-slate-900 border border-slate-700 text-left font-bold text-xs text-slate-300 px-3 py-2 name-col-portal" style="width:195px; min-width:195px; max-width:195px;">
        <div class="flex items-center gap-1.5"><i class="ph ph-users text-cyan-400 text-sm"></i> ENGINEER</div>
      </th>
    `;

    let row2 = ``;

    for (let day = 1; day <= this.gridState.totalDays; day++) {
      const dateObj = new Date(this.gridState.year, this.gridState.month - 1, day);
      const dowIndex = dateObj.getDay();
      const dowName = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'][dowIndex];
      const isWeekend = (dowIndex === 0 || dowIndex === 6);

      const cellBg1 = isWeekend ? 'bg-red-950/40 text-red-300' : 'bg-slate-800 text-slate-200';
      const cellBg2 = isWeekend ? 'bg-red-950/60 text-red-400' : 'bg-slate-900/90 text-slate-400';

      row1 += `
        <th class="border border-slate-700 py-1.5 px-0 text-center font-bold text-xs ${cellBg1} cell-box-portal" style="width:44px; min-width:44px; max-width:44px;">
          ${day}
        </th>
      `;

      row2 += `
        <th class="border border-slate-700 py-1 px-0 text-center text-[10px] font-semibold ${cellBg2} cell-box-portal" style="width:44px; min-width:44px; max-width:44px;">
          ${dowName}
        </th>
      `;
    }

    row1 += `
      <th class="border border-slate-700 bg-slate-800 text-slate-300 font-bold px-1 py-1 text-xs stat-col-portal" style="width:48px; min-width:48px; max-width:48px;" title="Total Working Duty Shifts">WRK</th>
      <th class="border border-slate-700 bg-slate-800 text-slate-300 font-bold px-1 py-1 text-xs stat-col-portal" style="width:48px; min-width:48px; max-width:48px;" title="Total Night Shifts">NS</th>
      <th class="border border-slate-700 bg-slate-800 text-slate-300 font-bold px-1 py-1 text-xs stat-col-portal" style="width:48px; min-width:48px; max-width:48px;" title="Total Weekly Offs">OFF</th>
    `;

    row2 += `
      <th colspan="3" class="border border-slate-700 bg-slate-900/90 text-slate-400 text-[10px] py-1 text-center font-bold">STATS</th>
    `;

    datesRow.innerHTML = row1;
    daysRow.innerHTML = row2;
  }

  /**
   * Render Month Shift Grid using gridState (Selected Month ONLY)
   */
  renderMonthGridRoster() {
    const tableBody = document.getElementById('monthGridTableBody');
    if (!tableBody) return;

    const loggedInId = this.matchedEngineer ? this.matchedEngineer.id : null;
    const loggedInName = this.matchedEngineer ? this.matchedEngineer.name : '';
    const filter = (this.gridState.filterTerm || '').trim().toUpperCase();

    const isViewingLiveMonth = (this.gridState.year === 2026 && this.gridState.month === 10);
    const activeOpDay = isViewingLiveMonth ? this.liveState.todayDay : 0;

    const filtered = this.gridState.roster.filter(eng => {
      if (!filter) return true;
      return eng.name.toUpperCase().includes(filter);
    });

    const rowsHtml = filtered.map(eng => {
      const isMe = (loggedInId && eng.id === loggedInId) || (eng.name === loggedInName);

      const trClass = isMe 
        ? 'bg-gradient-to-r from-emerald-950/60 via-slate-900 to-emerald-950/60 border-l-4 border-l-emerald-400 border-y border-emerald-500/30 font-bold shadow-md' 
        : 'hover:bg-slate-800/40 transition border-b border-slate-800';

      let cellsHtml = '';
      let wrkCount = 0;
      let nsCount = 0;
      let offCount = 0;

      for (let day = 1; day <= this.gridState.totalDays; day++) {
        const code = this.getShiftForDay(eng, day) || '';
        const shiftMeta = SHIFTS[code] || { bg: '#1e293b', text: '#94a3b8', name: code };
        
        if (code === 'NS') nsCount++;
        else if (isOffCategory(code)) offCount++;
        else if (code) wrkCount++;

        const isToday = (day === activeOpDay);
        const borderPulse = isToday ? 'ring-2 ring-cyan-400/80 z-10' : '';

        cellsHtml += `
          <td 
            class="border border-slate-800 text-center text-xs font-extrabold select-none p-0 cell-box-portal ${borderPulse}"
            style="width:44px; min-width:44px; max-width:44px; height:38px; background-color: ${shiftMeta.bg || '#1e293b'}; color: ${shiftMeta.text || '#ffffff'};"
            title="Day ${day}: ${code} - ${shiftMeta.name || ''} (${shiftMeta.timings || ''})"
          >
            ${code}
          </td>
        `;
      }

      return `
        <tr class="${trClass}">
          <!-- Sticky Engineer Name Column -->
          <td class="sticky left-0 z-20 bg-slate-900 border-r border-b border-slate-700 px-3 py-2 text-left text-xs whitespace-nowrap shadow-sm name-col-portal overflow-hidden" style="width:195px; min-width:195px; max-width:195px;">
            <div class="flex items-center gap-1.5 overflow-hidden">
              <span class="w-2 h-2 rounded-full shrink-0 ${isMe ? 'bg-emerald-400 shadow-[0_0_8px_#34d399]' : 'bg-slate-500'}"></span>
              <span class="truncate ${isMe ? 'text-emerald-300 font-extrabold flex items-center gap-1' : 'text-slate-200 font-bold'}">
                ${eng.name} ${isMe ? '<i class="ph-fill ph-star text-amber-400 text-xs shrink-0 inline"></i>' : ''}
              </span>
            </div>
          </td>

          <!-- Day Shift Cells -->
          ${cellsHtml}

          <!-- Stats Columns -->
          <td class="border border-slate-800 text-center text-xs font-bold text-emerald-400 bg-slate-900/80 px-1 py-1 stat-col-portal" style="width:48px; min-width:48px; max-width:48px;">
            ${wrkCount}
          </td>
          <td class="border border-slate-800 text-center text-xs font-bold text-indigo-300 bg-slate-900/80 px-1 py-1 stat-col-portal" style="width:48px; min-width:48px; max-width:48px;">
            ${nsCount}
          </td>
          <td class="border border-slate-800 text-center text-xs font-bold text-amber-300 bg-slate-900/80 px-1 py-1 stat-col-portal" style="width:48px; min-width:48px; max-width:48px;">
            ${offCount}
          </td>
        </tr>
      `;
    }).join('');

    tableBody.innerHTML = rowsHtml;
  }

  // ==============================================================
  // ACCOUNT PROFILE & CHANGE PASSWORD MODAL LOGIC
  // ==============================================================
  setupAccountProfileModal() {
    const userBtn = document.getElementById('userProfileBtn');
    const modal = document.getElementById('accountProfileModal');
    const btnClose = document.getElementById('btnCloseAccountModal');
    const tabProfile = document.getElementById('tabBtnProfile');
    const tabSecurity = document.getElementById('tabBtnSecurity');
    const contentProfile = document.getElementById('accountTabProfile');
    const contentSecurity = document.getElementById('accountTabSecurity');

    if (!userBtn || !modal) return;

    const openModal = () => {
      const u = this.currentUser || {};
      const eng = this.matchedEngineer || {};
      const engMeta = this.engineersList.find(e => (eng.id && e.id === eng.id) || (e.name && (e.name === eng.name || e.name === u.name))) || eng;

      const latestName = engMeta.name || u.name || u.username;
      const latestFullName = engMeta.full_name || u.full_name || latestName;
      const latestEmail = engMeta.email || u.email || `${u.username}@noc.company.com`;
      const latestPhone = engMeta.phone || u.phone || '—';

      document.getElementById('modalAccountName').textContent = latestName;
      document.getElementById('profileFullName').textContent = latestFullName;
      document.getElementById('profileUsername').textContent = u.username || 'staff';
      document.getElementById('profileEmail').textContent = latestEmail;
      document.getElementById('profilePhone').textContent = latestPhone;
      document.getElementById('profileRole').textContent = (engMeta.role || u.role || 'NOC Engineer').toUpperCase();
      document.getElementById('profileOffs').textContent = (engMeta.default_weekly_offs || eng.default_weekly_offs || ['Saturday', 'Sunday']).join(', ');

      document.getElementById('currentPassInput').value = '';
      document.getElementById('newPassChangeInput').value = '';
      document.getElementById('confirmPassChangeInput').value = '';
      document.getElementById('changePassAlert').classList.add('hidden');

      tabProfile.click();
      modal.classList.remove('hidden');
    };

    userBtn.addEventListener('click', openModal);
    btnClose?.addEventListener('click', () => modal.classList.add('hidden'));

    tabProfile?.addEventListener('click', () => {
      tabProfile.className = 'pb-2 px-3 text-xs font-bold text-cyan-400 border-b-2 border-cyan-400 cursor-pointer';
      tabSecurity.className = 'pb-2 px-3 text-xs font-semibold text-slate-400 hover:text-slate-200 border-b-2 border-transparent cursor-pointer';
      contentProfile.classList.remove('hidden');
      contentSecurity.classList.add('hidden');
    });

    tabSecurity?.addEventListener('click', () => {
      tabSecurity.className = 'pb-2 px-3 text-xs font-bold text-cyan-400 border-b-2 border-cyan-400 cursor-pointer';
      tabProfile.className = 'pb-2 px-3 text-xs font-semibold text-slate-400 hover:text-slate-200 border-b-2 border-transparent cursor-pointer';
      contentSecurity.classList.remove('hidden');
      contentProfile.classList.add('hidden');
    });

    const setupEye = (toggleId, inputId, eyeId) => {
      const toggle = document.getElementById(toggleId);
      const input = document.getElementById(inputId);
      const eye = document.getElementById(eyeId);
      if (toggle && input) {
        toggle.addEventListener('click', () => {
          if (input.type === 'password') {
            input.type = 'text';
            if (eye) eye.className = 'ph ph-eye-slash text-sm text-cyan-400';
          } else {
            input.type = 'password';
            if (eye) eye.className = 'ph ph-eye text-sm';
          }
        });
      }
    };

    setupEye('toggleCurrentPass', 'currentPassInput', 'eyeCurrentPass');
    setupEye('toggleNewPass', 'newPassChangeInput', 'eyeNewPass');

    const btnSubmit = document.getElementById('btnSubmitChangePass');
    const alertBox = document.getElementById('changePassAlert');
    const alertText = document.getElementById('changePassAlertText');

    btnSubmit?.addEventListener('click', async () => {
      const currentPass = document.getElementById('currentPassInput').value.trim();
      const newPass = document.getElementById('newPassChangeInput').value.trim();
      const confirmPass = document.getElementById('confirmPassChangeInput').value.trim();

      alertBox.classList.remove('hidden');

      if (!currentPass || !newPass || !confirmPass) {
        alertBox.className = 'p-3 rounded-xl text-xs flex items-start gap-2 bg-rose-950/70 border border-rose-500/50 text-rose-200';
        alertText.textContent = 'Please fill in all password fields.';
        return;
      }

      if (newPass !== confirmPass) {
        alertBox.className = 'p-3 rounded-xl text-xs flex items-start gap-2 bg-rose-950/70 border border-rose-500/50 text-rose-200';
        alertText.textContent = 'New Password and Confirm New Password do not match.';
        return;
      }

      btnSubmit.disabled = true;
      btnSubmit.innerHTML = '<i class="ph ph-spinner animate-spin text-sm"></i> Updating...';

      const res = await authService.changePassword(this.currentUser.username, currentPass, newPass);

      btnSubmit.disabled = false;
      btnSubmit.innerHTML = '<i class="ph ph-key text-sm"></i><span>Update Account Password</span>';

      if (!res.success) {
        alertBox.className = 'p-3 rounded-xl text-xs flex items-start gap-2 bg-rose-950/70 border border-rose-500/50 text-rose-200';
        alertText.textContent = res.error || 'Failed to update password.';
      } else {
        alertBox.className = 'p-3 rounded-xl text-xs flex items-start gap-2 bg-emerald-950/70 border border-emerald-500/50 text-emerald-200';
        alertText.textContent = '✅ ' + res.message;
        document.getElementById('currentPassInput').value = '';
        document.getElementById('newPassChangeInput').value = '';
        document.getElementById('confirmPassChangeInput').value = '';
      }
    });
  }

  bindEvents() {
    // Sign out button
    const btnSignOut = document.getElementById('btnSignOut');
    if (btnSignOut) {
      btnSignOut.addEventListener('click', () => {
        authService.logout();
      });
    }

    // Month Selector Change: ONLY updates the Month Shift Master Grid!
    // Today, Tomorrow, and Team Duty board remain untouched.
    const monthSelect = document.getElementById('portalMonthSelect');
    if (monthSelect) {
      monthSelect.addEventListener('change', async (e) => {
        const [yStr, mStr] = e.target.value.split('-');
        const year = parseInt(yStr, 10);
        const month = parseInt(mStr, 10);

        // Update ONLY gridState and re-render Month Grid!
        await this.loadGridRoster(year, month);
        this.renderMonthGridHeaders();
        this.renderMonthGridRoster();
      });
    }

    // Month Navigation Arrows (Previous / Next Month)
    const btnPortalPrevMonth = document.getElementById('btnPortalPrevMonth');
    const btnPortalNextMonth = document.getElementById('btnPortalNextMonth');
    const navigatePortalMonth = (delta) => {
      if (!monthSelect) return;
      const curIdx = monthSelect.selectedIndex;
      const newIdx = curIdx + delta;
      if (newIdx >= 0 && newIdx < monthSelect.options.length) {
        monthSelect.selectedIndex = newIdx;
        monthSelect.dispatchEvent(new Event('change'));
      }
    };
    btnPortalPrevMonth?.addEventListener('click', () => navigatePortalMonth(-1));
    btnPortalNextMonth?.addEventListener('click', () => navigatePortalMonth(1));

    // Search / Filter Input for Month Grid
    const searchInput = document.getElementById('portalSearchInput');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.gridState.filterTerm = e.target.value;
        this.renderMonthGridRoster();
      });
    }

    // Export Month Grid Button
    const btnExport = document.getElementById('btnPortalExportMonthGrid');
    if (btnExport) {
      btnExport.addEventListener('click', async () => {
        try {
          const s = window.ExcelService || ExcelService;
          await s.exportRosterToExcel(
            this.gridState.roster,
            this.gridState.year,
            this.gridState.month,
            this.gridState.totalDays,
            this.constraints
          );
        } catch (err) {
          alert('Error downloading Excel file: ' + err.message);
        }
      });
    }
  }

  // ==============================================================
  // OPERATIONS AI ASSISTANT (FLOATING 3D BOT)
  // ==============================================================
  initAiAssistant() {
    const botTrigger = document.getElementById('floatingAiBotTrigger');
    const chatDrawer = document.getElementById('aiChatDrawer');
    const btnCloseChat = document.getElementById('btnCloseAiChat');
    const chatInput = document.getElementById('aiChatInput');
    const btnSend = document.getElementById('btnSendAiChat');

    if (!botTrigger || !chatDrawer) return;

    botTrigger.addEventListener('click', () => {
      chatDrawer.classList.toggle('hidden');
      if (!chatDrawer.classList.contains('hidden')) {
        if (this.chatMessages.length === 0) {
          this.sendBotGreeting();
        }
        chatInput?.focus();
      }
    });

    btnCloseChat?.addEventListener('click', () => {
      chatDrawer.classList.add('hidden');
    });

    const handleSend = () => {
      const q = chatInput.value.trim();
      if (!q) return;
      chatInput.value = '';
      this.handleUserQuestion(q);
    };

    btnSend?.addEventListener('click', handleSend);
    chatInput?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleSend();
      }
    });

    document.querySelectorAll('.ai-prompt-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const prompt = chip.getAttribute('data-prompt');
        if (prompt) {
          this.handleUserQuestion(prompt);
        }
      });
    });
  }

  sendBotGreeting() {
    const uName = (this.matchedEngineer ? this.matchedEngineer.name : this.currentUser.name) || 'there';
    const opDay = this.liveState.todayDay;
    const nextDay = this.liveState.tomorrowDay;
    const myToday = this.getShiftForDay(this.matchedEngineer, opDay) || 'OFF';
    const myTomorrow = this.getShiftForDay(this.matchedEngineer, nextDay) || 'OFF';

    const greeting = `
      Hello <strong>${uName}</strong>! 👋<br><br>
      I'm your <strong>RosterX Operations Assistant</strong>. Here is your current duty briefing:
      <ul class="list-disc pl-4 mt-1.5 space-y-1 text-xs">
        <li><strong>Today (Oct ${opDay}):</strong> Shift <span class="text-cyan-300 font-bold">${myToday}</span></li>
        <li><strong>Tomorrow (Oct ${nextDay}):</strong> Shift <span class="text-cyan-300 font-bold">${myTomorrow}</span></li>
      </ul>
      <p class="mt-2 text-slate-300">You can ask me questions like <em>"Who is on night duty today?"</em>, <em>"When is my next OFF day?"</em>, or <em>"Who is covering day shift tomorrow?"</em>.</p>
    `;

    this.appendBotMessage(greeting);
  }

  appendUserMessage(text) {
    this.chatMessages.push({ role: 'user', content: text });
    const container = document.getElementById('aiMessagesContainer');
    if (!container) return;

    const div = document.createElement('div');
    div.className = 'flex justify-end';
    div.innerHTML = `
      <div class="max-w-[80%] rounded-2xl rounded-tr-xs bg-gradient-to-r from-blue-600 to-cyan-600 text-white p-3 text-xs shadow-md">
        ${this.escapeHtml(text)}
      </div>
    `;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
  }

  appendBotMessage(html) {
    this.chatMessages.push({ role: 'assistant', content: html });
    const container = document.getElementById('aiMessagesContainer');
    if (!container) return;

    const div = document.createElement('div');
    div.className = 'flex items-start gap-2.5';
    div.innerHTML = `
      <div class="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center shrink-0">
        <img src="assets/bot-3d-cutout.png" alt="Bot" class="w-6 h-6 object-contain" />
      </div>
      <div class="max-w-[85%] rounded-2xl rounded-tl-xs bg-slate-800/90 border border-slate-700 text-slate-200 p-3 text-xs shadow-md leading-relaxed">
        ${html}
      </div>
    `;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
  }

  handleUserQuestion(query) {
    this.appendUserMessage(query);

    const q = query.toLowerCase();
    const opDay = this.liveState.todayDay;
    const nextDay = this.liveState.tomorrowDay;
    const eng = this.matchedEngineer;

    let reply = '';

    // Query 1: My shifts
    if (q.includes('my shift') || q.includes('my duty') || q.includes('when is my shift') || q.includes('today')) {
      const todayCode = this.getShiftForDay(eng, opDay) || 'OFF';
      const tomorrowCode = this.getShiftForDay(eng, nextDay) || 'OFF';
      const todayInfo = SHIFTS[todayCode] || { name: todayCode, timings: 'Standard Duty' };
      const tomInfo = SHIFTS[tomorrowCode] || { name: tomorrowCode, timings: 'Standard Duty' };

      reply = `
        Your duty schedule for today and tomorrow:<br>
        • <strong>Today (Oct ${opDay}):</strong> <strong>${todayCode}</strong> (${todayInfo.name}) — <em>${todayInfo.timings}</em><br>
        • <strong>Tomorrow (Oct ${nextDay}):</strong> <strong>${tomorrowCode}</strong> (${tomInfo.name}) — <em>${tomInfo.timings}</em>
      `;
    }
    // Query 2: Night duty today / tomorrow
    else if (q.includes('night') || q.includes('night duty') || q.includes('ns')) {
      const targetDay = (q.includes('tomorrow') || q.includes('next')) ? nextDay : opDay;
      const dayLabel = (targetDay === opDay) ? `Today (Oct ${opDay})` : `Tomorrow (Oct ${nextDay})`;
      const nightStaff = this.liveState.roster.filter(e => this.getShiftForDay(e, targetDay) === 'NS');
      const names = nightStaff.map(e => e.name).join(', ') || 'None assigned';

      reply = `
        🌙 <strong>Night Shift (NS) Coverage for ${dayLabel}:</strong><br>
        • Assigned Staff: <strong class="text-cyan-300">${names}</strong><br>
        • Timings: <strong>09:00 PM – 07:30 AM</strong> (Mandatory 48h rest applies after completion).
      `;
    }
    // Query 3: Day shift
    else if (q.includes('tomorrow') || q.includes('day shift') || q.includes('fs') || q.includes('morning')) {
      const targetDay = nextDay;
      const morningStaff = this.liveState.roster.filter(e => {
        const s = this.getShiftForDay(e, targetDay);
        return s === 'FS' || s === 'FG' || s === 'G';
      });
      const names = morningStaff.map(e => `${e.name} (${this.getShiftForDay(e, targetDay)})`).join('<br>• ') || 'None';

      reply = `
        ☀️ <strong>Day / Morning Shift Coverage Tomorrow (Oct ${targetDay}):</strong><br>
        • ${names}
      `;
    }
    // Query 4: Off days
    else if (q.includes('off') || q.includes('leave') || q.includes('holiday') || q.includes('weekend')) {
      let offDays = [];
      if (eng && Array.isArray(eng.shifts)) {
        eng.shifts.forEach((s, idx) => {
          if (isOffCategory(s)) offDays.push(idx + 1);
        });
      }
      reply = `
        🏖️ <strong>Your Scheduled Weekly Offs this month:</strong><br>
        You have <strong>${offDays.length} OFF days</strong> scheduled on dates:<br>
        <div class="mt-1 flex flex-wrap gap-1 font-mono text-[11px] text-amber-300">
          ${offDays.map(d => `<span class="bg-amber-950/60 px-1.5 py-0.5 rounded border border-amber-800">${d}th</span>`).join(' ')}
        </div>
      `;
    }
    // Query 5: Operational change requests
    else if (q.includes('change') || q.includes('swap') || q.includes('generate') || q.includes('apply')) {
      reply = `
        ℹ️ <strong>Operational Schedule Notice:</strong><br>
        Active operational rosters are published by Operations Command.<br>
        To request a shift swap or time off, please submit a mutual swap request to your Operations Lead.
      `;
    }
    // Fallback general query
    else {
      reply = `
        I have full visibility of the active month roster for <strong>${this.liveState.roster.length} engineers</strong>.<br>
        Try asking:<br>
        • <em>"When is my next night shift?"</em><br>
        • <em>"Who is working night duty today?"</em><br>
        • <em>"How many OFF days do I have this month?"</em>
      `;
    }

    setTimeout(() => {
      this.appendBotMessage(reply);
    }, 350);
  }

  escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, (m) => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    })[m]);
  }
}

// Bootstrap
window.addEventListener('DOMContentLoaded', () => {
  window.userPortalApp = new UserPortalController();
  window.userPortalApp.init();
});
