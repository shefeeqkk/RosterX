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
import {
  currentPeriodKey,
  parsePeriodKey,
  daysInMonth,
  monthLabel,
  monthNames,
  periodRange,
  populatePeriodSelect,
  todayFormatted,
  getIstNow
} from './dateUtils.js';
import { themeController } from './themeController.js';



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
    // Dynamically synchronized to current real IST date and time
    const istNow = getIstNow();
    const curPeriod = currentPeriodKey();
    const { year: curYear, month: curMonth } = parsePeriodKey(curPeriod);
    const totalDays = daysInMonth(curYear, curMonth);
    let curDay = istNow.getDate();
    if (curDay > totalDays) curDay = totalDays;

    const tomorrow = new Date(istNow);
    tomorrow.setDate(istNow.getDate() + 1);
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
    themeController.init();
    // 1. Enforce Authentication
    this.currentUser = authService.enforceAuth('any');
    if (!this.currentUser) return;

    // 2. Instant Optimistic Data Load from Cache / Seed
    this.loadEngineersList();
    this.loadConstraints();

    // Optimistically load cached live roster for the current month so table & hero are NEVER blank or stuck in loading
    const activeKey = `${this.liveState.year}-${String(this.liveState.month).padStart(2, '0')}`;
    const cacheKey = `${STORAGE_KEYS.GRID_ROSTER_PREFIX}${this.liveState.year}_${String(this.liveState.month).padStart(2, '0')}`;
    try {
      const stored = localStorage.getItem(cacheKey);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.liveState.roster = parsed;
          this.gridState.roster = parsed;
        }
      }
    } catch (e) {}

    // If still no roster, populate optimistic baseline from directory
    if (!this.liveState.roster || this.liveState.roster.length === 0) {
      const active = this.engineersList.filter(e => e.status !== 'Resigned' && !this.isPureMgmt(e));
      this.liveState.roster = active.map(e => ({
        id: e.id,
        name: e.name,
        shifts: new Array(this.liveState.totalDays).fill(''),
        status: e.status || 'Active',
        role: e.role || 'engineer',
        default_weekly_offs: e.default_weekly_offs || ['Saturday', 'Sunday']
      }));
      this.gridState.roster = this.liveState.roster;
    }

    this.matchCurrentUserToEngineer();

    // Sync portalMonthSelect dropdown dynamically via dateUtils
    const portalMonthEl = document.getElementById('portalMonthSelect');
    if (portalMonthEl) {
      populatePeriodSelect(portalMonthEl, { back: 2, forward: 6, selectedKey: activeKey, style: 'upper' });
      portalMonthEl.value = activeKey;
    }

    // 3. Instant Optimistic Render (<5ms) - Never blank
    this.renderHeaderUserInfo();
    this.renderDashboardHero();
    this.renderPersonalShiftCards();
    this.renderTeamDutyBoard();
    this.renderMonthGridHeaders();
    this.renderMonthGridRoster();
    this.renderMyMonthlyShifts();
    this.renderTeamHandoverView();
    this.populateSwapColleagues();
    this.renderMySwapsHistory();

    // 4. Setup Tab Controls, Modals & Timers
    this.bindTabEvents();
    this.bindEvents();
    this.setupAccountProfileModal();
    this.initAiAssistant();
    this.startCountdownTimer();

    // 5. Non-blocking Background Atlas Synchronization
    (async () => {
      try {
        await this.syncEngineersFromAtlas();
        await this.loadLiveRoster(this.liveState.year, this.liveState.month);
        this.matchCurrentUserToEngineer();
        await this.loadGridRoster(this.gridState.year, this.gridState.month);

        this.renderHeaderUserInfo();
        this.renderDashboardHero();
        this.renderPersonalShiftCards();
        this.renderTeamDutyBoard();
        this.renderMonthGridHeaders();
        this.renderMonthGridRoster();
        this.renderMyMonthlyShifts();
        this.renderTeamHandoverView();
        this.populateSwapColleagues();
        this.renderMySwapsHistory();
        this.updateCountdownTimer();

        const badge = document.getElementById('userPortalStatusBadge');
        if (badge) badge.textContent = 'Atlas Connected';
      } catch (err) {
        console.warn('UserPortal Atlas background sync error:', err);
      }
    })();

    // 6. Auto-refresh live operational status every minute
    setInterval(() => {
      this.renderDashboardHero();
      this.renderPersonalShiftCards();
      this.renderTeamDutyBoard();
      this.updateCountdownTimer();
    }, 60000);
  }



  loadEngineersList() {

    let list = null;

    try {

      const stored = localStorage.getItem(STORAGE_KEYS.ENGINEERS_LIST);

      if (stored) list = JSON.parse(stored);

    } catch (e) { }

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

    } catch (e) { }

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



    // 2. Direct relative fetch fallback

    if (!loaded && typeof fetch !== 'undefined' && typeof window !== 'undefined' && window.location) {

      try {

        const directRes = await fetch(`/api/roster/${monthKey}`, { headers: { 'Accept': 'application/json' } });

        if (directRes.ok) {

          const directData = await directRes.json();

          const doc = directData.document || directData.data || directData;

          if (doc && doc.engineers && doc.engineers.length > 0) {

            loaded = doc.engineers.map(e => {

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

        }

      } catch (eDirect) { }

    }



    // 3. Fallback to LocalStorage (must have valid shifts)

    if (!loaded) {

      try {

        const stored = localStorage.getItem(cacheKey);

        if (stored) {

          const parsed = JSON.parse(stored);

          const hasValid = Array.isArray(parsed) && parsed.length > 0 &&

            parsed.some(e => Array.isArray(e.shifts) && e.shifts.some(s => s && String(s).trim() !== ''));

          if (hasValid) loaded = parsed;

        }

      } catch (e) { }

    }



    // 4. Fallback: Dynamic blank roster with active directory personnel

    if (!loaded) {

      const active = this.engineersList.filter(e => e.status !== 'Resigned' && !this.isPureMgmt(e));

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



    // 2. Direct relative fetch fallback

    if (!loaded && typeof fetch !== 'undefined' && typeof window !== 'undefined' && window.location) {

      try {

        const directRes = await fetch(`/api/roster/${monthKey}`, { headers: { 'Accept': 'application/json' } });

        if (directRes.ok) {

          const directData = await directRes.json();

          const doc = directData.document || directData.data || directData;

          if (doc && doc.engineers && doc.engineers.length > 0) {

            loaded = doc.engineers.map(e => {

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

        }

      } catch (eDirect) { }

    }



    // 3. Fallback to LocalStorage (must have valid shifts)

    if (!loaded) {

      try {

        const stored = localStorage.getItem(cacheKey);

        if (stored) {

          const parsed = JSON.parse(stored);

          const hasValid = Array.isArray(parsed) && parsed.length > 0 &&

            parsed.some(e => Array.isArray(e.shifts) && e.shifts.some(s => s && String(s).trim() !== ''));

          if (hasValid) loaded = parsed;

        }

      } catch (e) { }

    }



    // 4. Fallback: Dynamic blank roster with active directory personnel

    if (!loaded) {

      const active = this.engineersList.filter(e => e.status !== 'Resigned' && !this.isPureMgmt(e));

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



  isPureMgmt(e) {
    if (!e) return false;
    const u = String(e.username || e.id || '').trim().toLowerCase();
    const n = String(e.name || '').trim().toLowerCase();
    const r = String(e.role || '').trim().toLowerCase();
    if (u === 'shefeeqkk' || u === 'shefeeq' || n === 'shefeeq' || u === 'athulka') return false;
    return u === 'admin' || u === 'manager' || r === 'manager' || (r === 'admin' && u === 'admin');
  }

  /**
   * Match current logged in user to an engineer in the roster
   */
  matchCurrentUserToEngineer() {
    const u = this.currentUser;
    if (!u) return;

    const uName = String(u.name || '').trim().toUpperCase();
    const uFull = String(u.full_name || '').trim().toUpperCase();
    const uUser = String(u.username || '').trim().toUpperCase();
    const uId = String(u.engineerId || u.id || '').trim().toUpperCase();

    const pool = (this.liveState.roster && this.liveState.roster.length > 0)
      ? this.liveState.roster
      : (this.engineersList || []);

    let match = pool.find(e => {
      const eName = String(e.name || '').trim().toUpperCase();
      const eFull = String(e.full_name || '').trim().toUpperCase();
      const eUser = String(e.username || '').trim().toUpperCase();
      const eId = String(e.id || e.engineer_id || '').trim().toUpperCase();

      return (uId && eId && (eId === uId || eId.includes(uId))) ||
             (uName && eName && (eName === uName || eName.includes(uName) || uName.includes(eName))) ||
             (uFull && eName && (eName === uFull || uFull.includes(eName) || eName.includes(uFull))) ||
             (uUser && eUser && eUser === uUser) ||
             (uUser && eName && (eName.includes(uUser) || uUser.includes(eName.toLowerCase())));
    });

    if (!match && this.engineersList && this.engineersList.length > 0) {
      match = this.engineersList.find(e => {
        const eName = String(e.name || '').trim().toUpperCase();
        const eFull = String(e.full_name || '').trim().toUpperCase();
        const eUser = String(e.username || '').trim().toUpperCase();
        return (uName && eName && (eName === uName || eName.includes(uName))) ||
               (uUser && eUser && eUser === uUser) ||
               (uFull && eFull && (eFull === uFull || eFull.includes(uFull)));
      });
    }

    if (!match && (u.role === 'admin' || uUser === 'ADMIN')) {
      // Pure system administrator fallback only
      match = pool.find(e => e.name && e.name.toUpperCase().includes('SHEFEEQ')) || pool[0];
    }

    if (!match) {
      // NEVER assign another person's identity to the logged-in engineer!
      match = {
        id: uId || `eng_${uUser.toLowerCase() || 'user'}`,
        name: u.name || u.full_name || u.username || 'Engineer',
        username: u.username || '',
        shifts: new Array(this.liveState.totalDays).fill(''),
        status: 'Active',
        role: u.role || 'engineer',
        default_weekly_offs: u.default_weekly_offs || ['Monday', 'Tuesday']
      };
    }

    this.matchedEngineer = match;
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

      FS: { start: 7 * 60 + 30, end: 16 * 60 + 30, startStr: '07:30 AM' },

      FG: { start: 8 * 60 + 30, end: 17 * 60 + 30, startStr: '08:30 AM' },

      G: { start: 9 * 60 + 30, end: 18 * 60 + 30, startStr: '09:30 AM' },

      SG: { start: 11 * 60, end: 20 * 60, startStr: '11:00 AM' },

      SS: { start: 13 * 60, end: 22 * 60, startStr: '01:00 PM' },

      NS: { start: 21 * 60, end: 7 * 60 + 30, startStr: '09:00 PM' }

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



    // Anchor: Today's live calendar date in IST
    if (dateText) dateText.textContent = todayFormatted('long');

    const now = getIstNow();
    const opDay = this.liveState.todayDay || now.getDate();
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



    const periodTag = monthLabel(`${this.gridState.year}-${String(this.gridState.month).padStart(2, '0')}`, 'code');



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



    // Export Month Grid as HD PNG Image

    const btnExportPng = document.getElementById('btnPortalExportPng');

    if (btnExportPng) {

      btnExportPng.addEventListener('click', async () => {

        try {

          const s = window.ExcelService || ExcelService;

          await s.exportMonthGridToPng(

            this.gridState.roster,

            this.gridState.year,

            this.gridState.month,

            this.gridState.totalDays,

            this.constraints

          );

        } catch (err) {

          alert('Error downloading PNG image: ' + err.message);

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



  markdownToHtml(md) {
    if (!md) return '';
    return md
      .replace(/^### (.*$)/gim, '<h3 class="text-xs font-bold text-cyan-300 mt-2 mb-1">$1</h3>')
      .replace(/^## (.*$)/gim, '<h2 class="text-sm font-bold text-cyan-300 mt-2 mb-1">$1</h2>')
      .replace(/^# (.*$)/gim, '<h1 class="text-base font-bold text-cyan-300 mt-2 mb-1">$1</h1>')
      .replace(/\*\*(.*?)\*\*/gim, '<strong class="text-white font-bold">$1</strong>')
      .replace(/\*(.*?)\*/gim, '<em class="text-slate-300 italic">$1</em>')
      .replace(/`([^`]+)`/gim, '<code class="px-1 py-0.5 rounded bg-slate-900 border border-slate-700 text-cyan-300 font-mono text-[10px]">$1</code>')
      .replace(/^\s*-\s+(.*$)/gim, '<li class="ml-3 list-disc text-slate-200">$1</li>')
      .replace(/\n\n+/gim, '<br><br>')
      .replace(/\n/gim, '<br>');
  }

  sendBotGreeting() {
    const uName = (this.matchedEngineer ? this.matchedEngineer.name : this.currentUser.name) || 'there';
    const opDay = this.liveState.todayDay || new Date().getDate();
    const nextDay = this.liveState.tomorrowDay || (opDay + 1);
    const curMName = monthNames('short')[(this.liveState.month || (getIstNow().getMonth() + 1)) - 1] || 'Nov';
    const myToday = this.getShiftForDay(this.matchedEngineer, opDay) || 'OFF';
    const myTomorrow = this.getShiftForDay(this.matchedEngineer, nextDay) || 'OFF';

    const greeting = `
      Hello <strong>${uName}</strong>! 👋<br><br>
      I'm your <strong>RosterX Operations Assistant</strong> powered by Grounded Gemini AI. Here is your current duty briefing:
      <ul class="list-disc pl-4 mt-1.5 space-y-1 text-xs">
        <li><strong>Today (${curMName} ${opDay}):</strong> Shift <span class="text-cyan-300 font-bold">${myToday}</span></li>
        <li><strong>Tomorrow (${curMName} ${nextDay}):</strong> Shift <span class="text-cyan-300 font-bold">${myTomorrow}</span></li>
      </ul>
      <p class="mt-2 text-slate-300">Ask me in plain natural language:<br>
      • <em>"Who all are in shift now?"</em><br>
      • <em>"When is my next night shift?"</em><br>
      • <em>"Who is on night duty today?"</em></p>
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

  async handleUserQuestion(query) {
    this.appendUserMessage(query);

    const container = document.getElementById('aiMessagesContainer');
    let thinkingEl = null;
    if (container) {
      thinkingEl = document.createElement('div');
      thinkingEl.className = 'flex items-center gap-2 text-cyan-400 text-xs italic p-2';
      thinkingEl.innerHTML = '<span class="w-2 h-2 rounded-full bg-cyan-400 animate-ping"></span> Consulting Grounded AI Operations Agent...';
      container.appendChild(thinkingEl);
      container.scrollTop = container.scrollHeight;
    }

    try {
      const token = (typeof localStorage !== 'undefined' && localStorage.getItem('rosterx_auth_token')) || '';
      const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetch('/api/chat/assistant', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          message: query,
          current_user: this.currentUser,
          platform: 'Web Portal'
        })
      });

      if (thinkingEl) thinkingEl.remove();

      if (res.ok) {
        const data = await res.json();
        if (data.success && data.reply) {
          const html = this.markdownToHtml(data.reply);
          this.appendBotMessage(html);
          return;
        }
      }
    } catch (err) {
      console.warn('[AI Chat] Backend query error, using local fallback:', err);
    }

    if (thinkingEl) thinkingEl.remove();

    this.appendBotMessage(
      `### 🤖 RosterX Operations Assistant<br><br>` +
      `I can help coordinate your operational shift schedule live from Atlas! Try asking:<br>` +
      `• <em>"Who all are in shift now?"</em><br>` +
      `• <em>"When is my next night shift?"</em><br>` +
      `• <em>"What is my shift today and tomorrow?"</em>`
    );
  }

  // ==============================================================
  // TAB ARCHITECTURE & DOCK CONTROLLER
  // ==============================================================

  bindTabEvents() {
    const desktopTabBtns = document.querySelectorAll('.user-nav-tab-btn');
    const mobileTabBtns = document.querySelectorAll('.user-mobile-nav-btn');

    desktopTabBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const tab = btn.getAttribute('data-tab');
        if (tab) this.switchUserTab(tab);
      });
    });

    mobileTabBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const tab = btn.getAttribute('data-tab');
        if (tab) this.switchUserTab(tab);
      });
    });
  }

  switchUserTab(tabId) {
    // 1. Update Desktop Tab Button styling
    const desktopBtns = document.querySelectorAll('.user-nav-tab-btn');
    desktopBtns.forEach(b => {
      const match = b.getAttribute('data-tab') === tabId;
      if (match) {
        b.className = 'user-nav-tab-btn flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition shadow-md bg-cyan-600 text-white cursor-pointer';
      } else {
        b.className = 'user-nav-tab-btn flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 hover:text-slate-900 dark:hover:text-white border border-slate-300 dark:border-slate-700 cursor-pointer shadow-xs';
      }
    });

    // 2. Update Mobile Tab Button styling
    const mobileBtns = document.querySelectorAll('.user-mobile-nav-btn');
    mobileBtns.forEach(b => {
      const match = b.getAttribute('data-tab') === tabId;
      const dot = b.querySelector('.active-dot');
      if (match) {
        b.classList.remove('text-slate-400');
        b.classList.add('text-cyan-400');
        if (dot) dot.classList.remove('hidden');
      } else {
        b.classList.remove('text-cyan-400');
        b.classList.add('text-slate-400');
        if (dot) dot.classList.add('hidden');
      }
    });

    // 3. Show selected tab panel, hide others
    const panels = document.querySelectorAll('.user-tab-panel');
    panels.forEach(p => {
      if (p.id === `userTabContent_${tabId}`) {
        p.classList.remove('hidden');
      } else {
        p.classList.add('hidden');
      }
    });

    // 4. Component-specific triggers
    if (tabId === 'my_schedule') {
      this.renderMyMonthlyShifts();
      this.updateCountdownTimer();
    } else if (tabId === 'team_handover') {
      this.renderTeamHandoverView();
    } else if (tabId === 'request_swap') {
      this.populateSwapColleagues();
      this.renderMySwapsHistory();
    } else if (tabId === 'monthly_overview') {
      this.renderMonthGridHeaders();
      this.renderMonthGridRoster();
    }
  }

  // ==============================================================
  // TAB 1: PERSONAL MONTHLY SHIFTS & COUNTDOWN TIMER
  // ==============================================================

  renderMyMonthlyShifts() {
    const container = document.getElementById('myMonthlyShiftsContainer');
    if (!container) return;

    const roster = this.liveState.roster || [];
    const myName = (this.currentUser?.name || this.currentUser?.full_name || '').trim().toLowerCase();
    const myId = (this.currentUser?.id || this.currentUser?.username || '').trim().toLowerCase();

    const myRow = roster.find(r => {
      const rId = String(r.id || '').trim().toLowerCase();
      const rName = String(r.name || '').trim().toLowerCase();
      return (myId && rId === myId) || (myName && rName === myName) || rName.includes(myId) || (myId && rId.includes(myId));
    }) || roster[0] || null;

    const totalDays = this.liveState.totalDays;
    const year = this.liveState.year;
    const month = this.liveState.month;
    const todayNum = this.liveState.todayDay;

    const badgeEl = document.getElementById('myMonthShiftBadge');
    if (badgeEl) {
      badgeEl.textContent = monthLabel(`${year}-${String(month).padStart(2, '0')}`, 'upper');
    }

    const shiftColors = {
      FS: 'bg-emerald-100 text-emerald-900 border-emerald-300',
      FG: 'bg-emerald-800 text-white border-emerald-900',
      G: 'bg-sky-100 text-sky-900 border-sky-300',
      SG: 'bg-orange-600 text-white border-orange-700',
      SS: 'bg-orange-100 text-orange-950 border-orange-300',
      NS: 'bg-slate-950 text-white border-slate-700',
      OFF: 'bg-amber-100 text-amber-900 border-amber-300',
      SWOFF: 'bg-amber-100 text-amber-900 border-amber-300',
      L: 'bg-rose-100 text-rose-900 border-rose-300'
    };

    const shiftTimings = {
      FS: '07:30 – 16:30',
      FG: '08:30 – 17:30',
      G: '09:30 – 18:30',
      SG: '11:00 – 20:00',
      SS: '13:00 – 22:00',
      NS: '21:00 – 07:30 (+1)',
      OFF: 'Full Rest Day',
      SWOFF: 'Swapped Rest Day',
      L: 'Scheduled Leave'
    };

    let html = `
      <table class="w-full text-left border-collapse text-xs">
        <thead>
          <tr class="bg-slate-900/90 text-slate-400 uppercase text-[10px] tracking-wider border-b border-slate-800">
            <th class="py-2.5 px-4 font-bold">Day & Date</th>
            <th class="py-2.5 px-4 font-bold text-center">Shift Code</th>
            <th class="py-2.5 px-4 font-bold">Operational Timing</th>
            <th class="py-2.5 px-4 font-bold text-right pr-4">Duty Status</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-800/60">
    `;

    for (let day = 1; day <= totalDays; day++) {
      const dateObj = new Date(year, month - 1, day);
      const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' });
      const dateStr = `${dayName}, ${month}/${String(day).padStart(2, '0')}`;
      const code = (myRow && myRow.shifts && myRow.shifts[day - 1]) ? myRow.shifts[day - 1].trim().toUpperCase() : 'OFF';
      const badgeStyle = shiftColors[code] || 'bg-slate-800 text-slate-300 border-slate-700';
      const timing = shiftTimings[code] || '--:-- – --:--';
      const isToday = (day === todayNum);
      const rowClass = isToday
        ? 'bg-cyan-950/40 border-l-4 border-l-cyan-400 font-bold'
        : (day % 2 === 0 ? 'bg-slate-900/30' : 'bg-transparent');

      html += `
        <tr class="${rowClass} hover:bg-slate-800/40 transition">
          <td class="py-2 px-4 flex items-center gap-2">
            <span class="w-6 text-slate-500 font-mono text-[11px]">${String(day).padStart(2, '0')}</span>
            <span class="${isToday ? 'text-cyan-300 font-extrabold' : 'text-slate-200'}">${dateStr}</span>
            ${isToday ? '<span class="text-[9px] font-extrabold px-1.5 py-0.5 rounded bg-cyan-400 text-slate-950 ml-1.5 animate-pulse">TODAY</span>' : ''}
          </td>
          <td class="py-2 px-4 text-center">
            <span class="inline-block px-2.5 py-0.5 rounded font-black text-[11px] border shadow-xs ${badgeStyle}">
              ${code || 'OFF'}
            </span>
          </td>
          <td class="py-2 px-4 font-mono text-slate-400 text-[11px]">
            ${timing}
          </td>
          <td class="py-2 px-4 text-right pr-4">
            ${code === 'NS' ? '<span class="text-indigo-400 text-[10px] font-bold">Solo Night Duty</span>' :
          (code === 'OFF' || code === 'SWOFF' ? '<span class="text-amber-400 text-[10px] font-semibold">Scheduled Rest</span>' :
            (code === 'L' ? '<span class="text-rose-400 text-[10px] font-semibold">Approved Leave</span>' :
              '<span class="text-emerald-400 text-[10px] font-semibold">Standard Shift</span>'))}
          </td>
        </tr>
      `;
    }

    html += `
        </tbody>
      </table>
    `;

    container.innerHTML = html;
  }

  startCountdownTimer() {
    this.updateCountdownTimer();
    if (this._countdownInterval) clearInterval(this._countdownInterval);
    this._countdownInterval = setInterval(() => {
      this.updateCountdownTimer();
    }, 1000);
  }

  updateCountdownTimer() {
    const labelEl = document.getElementById('shiftCountdownLabel');
    const timeEl = document.getElementById('shiftCountdownTime');
    if (!timeEl) return;

    const roster = this.liveState.roster || [];
    const myName = (this.currentUser?.name || this.currentUser?.full_name || '').trim().toLowerCase();
    const myId = (this.currentUser?.id || this.currentUser?.username || '').trim().toLowerCase();

    const myRow = roster.find(r => {
      const rId = String(r.id || '').trim().toLowerCase();
      const rName = String(r.name || '').trim().toLowerCase();
      return (myId && rId === myId) || (myName && rName === myName) || rName.includes(myId) || (myId && rId.includes(myId));
    }) || roster[0] || null;

    const todayNum = this.liveState.todayDay || new Date().getDate();
    const code = (myRow && myRow.shifts && myRow.shifts[todayNum - 1]) ? myRow.shifts[todayNum - 1].trim().toUpperCase() : 'OFF';

    const now = new Date();
    const curHours = now.getHours();
    const curMinutes = now.getMinutes();
    const curSeconds = now.getSeconds();
    const curTotalSec = curHours * 3600 + curMinutes * 60 + curSeconds;

    const windows = {
      FS: { start: 7.5 * 3600, end: 16.5 * 3600, name: 'First Shift' },
      FG: { start: 8.5 * 3600, end: 17.5 * 3600, name: 'First General' },
      G: { start: 9.5 * 3600, end: 18.5 * 3600, name: 'General Shift' },
      SG: { start: 11 * 3600, end: 20 * 3600, name: 'Second General' },
      SS: { start: 13 * 3600, end: 22 * 3600, name: 'Second Shift' },
      NS: { start: 21 * 3600, end: (24 + 7.5) * 3600, name: 'Night Shift' }
    };

    const formatDelta = (sec) => {
      const h = Math.floor(sec / 3600);
      const m = Math.floor((sec % 3600) / 60);
      const s = Math.floor(sec % 60);
      return `${String(h).padStart(2, '0')}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
    };

    if (windows[code]) {
      const w = windows[code];
      let adjCur = curTotalSec;
      if (code === 'NS' && curTotalSec < 7.5 * 3600) {
        adjCur += 24 * 3600;
      }

      if (adjCur >= w.start && adjCur < w.end) {
        if (labelEl) labelEl.textContent = 'Shift Handover in:';
        timeEl.textContent = formatDelta(w.end - adjCur);
        timeEl.className = 'font-bold text-cyan-300 tracking-wide';
        return;
      } else if (adjCur < w.start) {
        if (labelEl) labelEl.textContent = 'Duty Starts in:';
        timeEl.textContent = formatDelta(w.start - adjCur);
        timeEl.className = 'font-bold text-amber-300 tracking-wide';
        return;
      }
    }

    // Today's shift has ended OR today is OFF / Leave:
    // Dynamically calculate time until the NEXT scheduled duty shift starts
    const shifts = (myRow && myRow.shifts) || [];
    let nextDutyCode = null;
    let daysAhead = 0;

    for (let idx = todayNum; idx < shifts.length; idx++) {
      const candidate = String(shifts[idx] || '').trim().toUpperCase();
      if (windows[candidate]) {
        nextDutyCode = candidate;
        daysAhead = (idx - todayNum) + 1; // 1 = tomorrow, 2 = day after tomorrow, etc.
        break;
      }
    }

    if (nextDutyCode && windows[nextDutyCode]) {
      const secondsUntilMidnight = Math.max(0, 24 * 3600 - curTotalSec);
      const intermediateDaysSeconds = (daysAhead - 1) * 24 * 3600;
      const nextShiftStartSeconds = windows[nextDutyCode].start;
      const totalSecUntilNextDuty = secondsUntilMidnight + intermediateDaysSeconds + nextShiftStartSeconds;

      if (labelEl) labelEl.textContent = 'Next Shift Starts in:';
      timeEl.textContent = formatDelta(totalSecUntilNextDuty);
      timeEl.className = 'font-bold text-cyan-300 tracking-wide';
      return;
    }

    if (labelEl) labelEl.textContent = 'Next Shift Starts in:';
    timeEl.textContent = 'No upcoming duty scheduled';
    timeEl.className = 'font-bold text-slate-400 tracking-wide';
  }

  // ==============================================================
  // TAB 2: DESK HANDOVER & TEAM DUTY VIEW
  // ==============================================================

  renderTeamHandoverView() {
    const morningEl = document.getElementById('teamHandoverMorning');
    const secondEl = document.getElementById('teamHandoverSecond');
    const nightEl = document.getElementById('teamHandoverNight');
    const offEl = document.getElementById('teamHandoverOff');

    if (!morningEl || !secondEl || !nightEl || !offEl) return;

    const roster = this.liveState.roster || [];
    const todayNum = this.liveState.todayDay || new Date().getDate();

    const morning = [];
    const second = [];
    const night = [];
    const off = [];

    roster.forEach(eng => {
      const code = (eng.shifts && eng.shifts[todayNum - 1]) ? eng.shifts[todayNum - 1].trim().toUpperCase() : 'OFF';
      const name = eng.name || eng.full_name || 'Engineer';
      const phone = eng.phone || '';
      const email = eng.email || '';

      const item = { name, code, phone, email };

      if (['FS', 'FG', 'G'].includes(code)) morning.push(item);
      else if (['SG', 'SS'].includes(code)) second.push(item);
      else if (code === 'NS') night.push(item);
      else off.push(item);
    });

    const renderSlotList = (arr, badgeClass) => {
      if (!arr || arr.length === 0) {
        return '<span class="text-slate-500 italic text-xs">No active personnel</span>';
      }
      return arr.map(eng => `
        <div class="flex items-center justify-between p-1.5 rounded-lg bg-slate-900/60 border border-slate-800/80">
          <div class="flex items-center gap-1.5 truncate">
            <span class="w-1.5 h-1.5 rounded-full ${badgeClass}"></span>
            <span class="font-bold text-slate-200 text-xs truncate">${this.escapeHtml(eng.name)}</span>
          </div>
          <span class="text-[10px] font-black px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700 font-mono">${eng.code}</span>
        </div>
      `).join('');
    };

    morningEl.innerHTML = renderSlotList(morning, 'bg-emerald-400');
    secondEl.innerHTML = renderSlotList(second, 'bg-orange-400');
    nightEl.innerHTML = renderSlotList(night, 'bg-indigo-400');
    offEl.innerHTML = renderSlotList(off, 'bg-amber-400');
  }

  // ==============================================================
  // TAB 3: REQUEST SHIFT SWAP & SWAP QUEUE
  // ==============================================================

  populateSwapColleagues() {
    const myDateSelect = document.getElementById('userSwapMyDate');
    const colleagueSelect = document.getElementById('userSwapColleague');
    const colleagueDateSelect = document.getElementById('userSwapColleagueDate');
    if (!myDateSelect || !colleagueSelect || !colleagueDateSelect) return;

    const totalDays = this.liveState.totalDays || daysInMonth(this.liveState.year, this.liveState.month);
    const year = this.liveState.year || getIstNow().getFullYear();
    const month = this.liveState.month || (getIstNow().getMonth() + 1);
    const engineers = this.engineersList || [];
    const myId = (this.currentUser?.id || this.currentUser?.username || '').trim().toLowerCase();

    // Populate dates
    if (myDateSelect.options.length <= 1) {
      let dateOpts = '<option value="">Select your shift date...</option>';
      let colDateOpts = '<option value="">Select target date...</option>';
      for (let d = 1; d <= totalDays; d++) {
        const dateObj = new Date(year, month - 1, d);
        const dayName = dateObj.toLocaleDateString('en-US', { weekday: 'short' });
        const val = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
        const label = `${val} (${dayName})`;
        dateOpts += `<option value="${val}">${label}</option>`;
        colDateOpts += `<option value="${val}">${label}</option>`;
      }
      myDateSelect.innerHTML = dateOpts;
      colleagueDateSelect.innerHTML = colDateOpts;
    }

    // Populate colleagues
    let colOpts = '<option value="">Select colleague...</option>';
    engineers.forEach(eng => {
      const engId = (eng.id || eng.username || '').toLowerCase();
      if (engId !== myId && eng.status !== 'Resigned' && eng.role !== 'admin' && eng.role !== 'manager') {
        colOpts += `<option value="${this.escapeHtml(eng.name)}">${this.escapeHtml(eng.name)} (${eng.role || 'NOC'})</option>`;
      }
    });
    colleagueSelect.innerHTML = colOpts;

    // Attach form submit handler if not already attached
    const form = document.getElementById('userSwapForm');
    if (form && !form._bound) {
      form._bound = true;
      form.addEventListener('submit', (e) => this.handleSwapSubmit(e));
    }
  }

  handleSwapSubmit(e) {
    e.preventDefault();
    const myDate = document.getElementById('userSwapMyDate')?.value;
    const colleague = document.getElementById('userSwapColleague')?.value;
    const colleagueDate = document.getElementById('userSwapColleagueDate')?.value;
    const category = document.getElementById('userSwapCategory')?.value || 'mutual';
    const reason = document.getElementById('userSwapReason')?.value || '';

    if (!myDate || !colleague || !colleagueDate) {
      alert('Please fill out all required fields (My Date, Colleague, and Colleague Date).');
      return;
    }

    const swapRecord = {
      id: 'swp_' + Date.now(),
      created_at: new Date().toISOString(),
      engineer_a: this.currentUser?.name || this.currentUser?.full_name || 'Engineer',
      engineer_b: colleague,
      date_a: myDate,
      date_b: colleagueDate,
      category,
      reason,
      status: 'Pending Operational Review'
    };

    try {
      const stored = JSON.parse(localStorage.getItem('rosterx_manual_swaps') || '[]');
      stored.unshift(swapRecord);
      localStorage.setItem('rosterx_manual_swaps', JSON.stringify(stored));
    } catch (err) {
      console.warn('Error saving swap locally:', err);
    }

    alert('Shift Swap Request submitted successfully! Handled by RosterX Swap-First Policy Engine.');
    document.getElementById('userSwapForm')?.reset();
    this.renderMySwapsHistory();
  }

  renderMySwapsHistory() {
    const list = document.getElementById('userSwapsQueueList');
    if (!list) return;

    let swaps = [];
    try {
      swaps = JSON.parse(localStorage.getItem('rosterx_manual_swaps') || '[]');
    } catch (e) { }

    const myName = (this.currentUser?.name || this.currentUser?.full_name || '').trim().toLowerCase();
    const mySwaps = swaps.filter(s => {
      const a = String(s.engineer_a || '').toLowerCase();
      const b = String(s.engineer_b || '').toLowerCase();
      return a.includes(myName) || b.includes(myName) || myName.includes(a);
    });

    if (mySwaps.length === 0) {
      list.innerHTML = `
        <div class="p-6 rounded-xl bg-slate-900/60 border border-slate-800 text-center text-slate-500 text-xs">
          <i class="ph ph-clipboard-text text-2xl text-slate-600 mb-1.5 block"></i>
          No Active Records<br><span class="text-[10px] text-slate-600">Your submitted shift swap requests will appear here dynamically.</span>
        </div>
      `;
      return;
    }

    list.innerHTML = mySwaps.map(s => `
      <div class="p-3 rounded-xl bg-slate-900/70 border border-slate-800 text-xs space-y-1">
        <div class="flex items-center justify-between">
          <span class="font-bold text-slate-200">${this.escapeHtml(s.engineer_a)} ⇄ ${this.escapeHtml(s.engineer_b)}</span>
          <span class="text-[9px] font-black uppercase tracking-wider px-2 py-0.5 rounded bg-amber-950 text-amber-300 border border-amber-800">${s.status || 'Pending'}</span>
        </div>
        <div class="text-[11px] text-slate-400 font-mono">
          <span>Date: ${s.date_a || s.date}</span> ${s.date_b ? `⇄ <span>${s.date_b}</span>` : ''}
        </div>
        ${s.reason ? `<div class="text-[10px] text-slate-500 italic">"${this.escapeHtml(s.reason)}"</div>` : ''}
      </div>
    `).join('');
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

const bootstrapUserPortal = () => {
  if (!window.userPortalApp) {
    window.userPortalApp = new UserPortalController();
    window.userPortalApp.init();
  }
};

if (document.readyState === 'loading') {
  window.addEventListener('DOMContentLoaded', bootstrapUserPortal);
} else {
  bootstrapUserPortal();
}

