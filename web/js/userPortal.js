/**
 * RosterX — Unified Engineer & Manager Web Portal Controller (userPortal.js)
 *
 * 1:1 Android App Parity (v2.0.0):
 * - Engineer / Apprentice 5 Tabs: Dashboard, Team Shifts, My Calendar, Month Roster, More
 * - Manager 5 Tabs: Dashboard (Shift Radar + Health), Team Shifts, Month Roster, Broadcast, More
 * - Zero Swap Request tab/UI (SW* shift codes preserved cleanly in roster grids)
 * - Adaptive iOS PWA Shell: 5-Tab Fluid Neumorphic Bottom Dock (portrait) +
 *   Slim 42px Top Bar & Left Vertical Nav Rail (landscape) + Full-Screen Landscape Month Roster
 * - Personal Sync to Calendar (.ics for iOS Apple Calendar / Google Calendar)
 * - Operations Hub (More) -> Server Credential Tracker + My Team Directory
 * - Team Celebrations + Wishes Modal, Notification Bell Drawer (@mentions + auto-sweep),
 *   Profile & Settings (Tenure/Birthday, Weekly Offs, Edit Profile + Remove Photo, 6 Brand Palettes)
 */

import { authService } from './authService.js?v=2.0.5';
import { AtlasService } from './atlasService.js?v=2.0.5';
import { themeController } from './themeController.js?v=2.0.5';
import { MobileNavShell, consumeDeepLinkHash } from './mobileNavShell.js?v=2.0.5';
import { SwipeTabController } from './swipeTabController.js?v=2.3.19';
import { CredentialTrackerController } from './credentialTracker.js?v=2.0.5';
import { MyTeamViewController } from './myTeamView.js?v=2.0.5';
import { sortEngineersByWeekOffs, SHIFT_SPECS } from './constants.js?v=2.3.19';
import {
  registerPwaServiceWorker,
  mountIosInstallBanner,
  openNeumorphicDialog,
  closeNeumorphicDialog,
  escapeHtml,
  getAuthHeaders
} from './pwaHelper.js?v=2.3.1';

const APP_VERSION = '2.3.1';
const WORKING_SHIFT_CODES = new Set(['FS', 'FG', 'G', 'SG', 'SS', 'NS']);
const OFF_OR_LEAVE_CODES = new Set(['OFF', 'L', 'CO', 'AL', 'SL', 'ML']);

function getShiftOrUnscheduled(rawCode) {
  const clean = String(rawCode || '').trim();
  if (!clean) {
    return {
      code: '—',
      name: 'Not Scheduled',
      timing: 'Not published yet',
      bg: 'var(--nm-surface-contrast)',
      fg: 'var(--nm-text-muted)',
      order: 99,
      unscheduled: true
    };
  }
  return { ...getShiftVisual(clean), unscheduled: false };
}

const SHIFT_META = {
  FS:  { name: 'First Shift',    timing: '07:30 AM – 04:30 PM', bg: '#BBF7D0', fg: '#064E3B', order: 1 },
  FG:  { name: 'First General',  timing: '08:30 AM – 05:30 PM', bg: '#166534', fg: '#FFFFFF', order: 2 },
  G:   { name: 'General Shift',  timing: '09:30 AM – 06:30 PM', bg: '#BAE6FD', fg: '#0C4A6E', order: 3 },
  SG:  { name: 'Second General', timing: '11:00 AM – 08:00 PM', bg: '#EA580C', fg: '#FFFFFF', order: 4 },
  SS:  { name: 'Second Shift',   timing: '01:00 PM – 10:00 PM', bg: '#FED7AA', fg: '#7C2D12', order: 5 },
  NS:  { name: 'Night Shift',    timing: '09:00 PM – 07:30 AM', bg: '#020617', fg: '#FFFFFF', order: 6 },
  OFF: { name: 'Weekly Off',     timing: 'Rest Day',            bg: '#FEF08A', fg: '#854D0E', order: 7 },
  L:   { name: 'Approved Leave', timing: 'Approved Leave',      bg: '#DC2626', fg: '#FFFFFF', order: 8 }
};

const STANDARD_LEGEND = ['FS', 'FG', 'G', 'SG', 'SS', 'NS', 'OFF', 'L'];
const WEEKDAYS_FULL = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

function getShiftVisual(rawCode) {
  const code = String(rawCode || 'OFF').trim().toUpperCase();
  if (SHIFT_META[code]) return { code, ...SHIFT_META[code] };
  if (code.startsWith('SW')) {
    return { code, name: 'Swap Off', timing: 'Rest Day', bg: '#FEF08A', fg: '#854D0E', order: 7 };
  }
  return { code: code || 'OFF', name: code || 'Rest', timing: 'Rest Day', bg: '#E2E8F0', fg: '#334155', order: 9 };
}

function getIstNow() {
  const now = new Date();
  return new Date(now.getTime() + (330 + now.getTimezoneOffset()) * 60000);
}

function toIsoDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function toPeriodKey(year, month) {
  return `${year}-${String(month).padStart(2, '0')}`;
}

function getInitials(name) {
  const parts = String(name || 'TM').trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return (parts[0] || 'TM').slice(0, 2).toUpperCase();
}

function formatDuration(sec) {
  const s = Math.max(0, Math.floor(sec));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const r = s % 60;
  return `${h}h ${String(m).padStart(2, '0')}m ${String(r).padStart(2, '0')}s`;
}

function isExpiredCelebration(notif, todayIstIso) {
  if (!notif) return false;
  const title = String(notif.title || '').toLowerCase();
  const msg = String(notif.message || notif.raw_message || '').toLowerCase();
  const type = String(notif.type || notif.priority || '').toLowerCase();
  const isCeleb = type === 'celebration' || title.includes('birthday') || title.includes('anniversary') || msg.includes('happy birthday');
  if (!isCeleb) return false;
  const created = String(notif.created_at || notif.date || '').slice(0, 10);
  return Boolean(created && /^\d{4}-\d{2}-\d{2}$/.test(created) && created < todayIstIso);
}

function resolveMentions(rawText, selfUser, engineers) {
  if (!rawText) return '';
  let out = String(rawText);
  const selfName = selfUser?.full_name || selfUser?.fullName || selfUser?.name || 'Team Member';
  out = out.replace(/@name\b/gi, selfName).replace(/@all\b/gi, 'Everyone');

  const candidates = new Set(['Everyone', 'everyone', 'all', 'team']);
  const engMap = {};
  (engineers || []).forEach((eng) => {
    const fn = (eng.full_name || eng.fullName || eng.name || eng.username || '').trim();
    const un = (eng.username || '').trim();
    if (fn) {
      candidates.add(fn);
      engMap[fn.toLowerCase()] = fn;
    }
    if (eng.name) {
      candidates.add(eng.name.trim());
      engMap[eng.name.trim().toLowerCase()] = fn;
    }
    if (un) {
      candidates.add(un);
      engMap[un.toLowerCase()] = fn;
    }
  });

  const knownFirstWords = new Set();
  candidates.forEach(c => {
    const firstWord = c.split(/\s+/)[0]?.trim();
    if (firstWord && firstWord.length >= 3) knownFirstWords.add(firstWord.toLowerCase());
  });

  const colonMentionRegex = /@([A-Za-z0-9_.-]+(?:\s+[A-Za-z0-9_.-]+)+)(?=\s*[:\-\(\[\n,])/g;
  let cm;
  while ((cm = colonMentionRegex.exec(out)) !== null) {
    const fullToken = cm[1].trim();
    const first = fullToken.split(/\s+/)[0]?.trim()?.toLowerCase();
    if (first && knownFirstWords.has(first)) {
      candidates.add(fullToken);
      engMap[fullToken.toLowerCase()] = fullToken;
    }
  }

  const sorted = Array.from(candidates).filter(Boolean).sort((a, b) => b.length - a.length);
  const escapeRegex = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const patternParts = sorted.map(c => escapeRegex(c) + '(?![a-zA-Z0-9_])');
  patternParts.push('[a-zA-Z0-9_.-]+');
  const re = new RegExp(`@(${patternParts.join('|')})`, 'gi');

  out = out.replace(re, (match, token) => {
    const lower = token.toLowerCase();
    if (lower === 'all' || lower === 'everyone' || lower === 'team') return '@Everyone';
    if (engMap[lower]) return `@${engMap[lower]}`;
    return match;
  });

  return out;
}

function computeTenureAndBirthday(joinDate, birthday) {
  const today = getIstNow();
  let tenureText = '--';
  if (joinDate) {
    const p = String(joinDate).trim().replace(/\//g, '-').split('-');
    if (p.length === 3) {
      const start = new Date(Number(p[0]), Number(p[1]) - 1, Number(p[2]));
      if (!Number.isNaN(start.getTime()) && start <= today) {
        let years = today.getFullYear() - start.getFullYear();
        let months = today.getMonth() - start.getMonth();
        let days = today.getDate() - start.getDate();
        if (days < 0) {
          months--;
          const prevLast = new Date(today.getFullYear(), today.getMonth(), 0).getDate();
          days += prevLast;
        }
        if (months < 0) {
          years--;
          months += 12;
        }
        tenureText = years > 0 ? `${years} yr ${months} mo` : `${months} mo ${days} d`;
      }
    }
  }

  let birthdayLabel = 'BIRTHDAY';
  let birthdayValue = '--';
  if (birthday) {
    const clean = String(birthday).trim().replace(/\//g, '-');
    const parts = clean.split('-');
    const mm = parts.length === 3 ? Number(parts[1]) : parts.length === 2 ? Number(parts[0]) : NaN;
    const dd = parts.length === 3 ? Number(parts[2]) : parts.length === 2 ? Number(parts[1]) : NaN;
    if (mm >= 1 && mm <= 12 && dd >= 1 && dd <= 31) {
      const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      let nextBday = new Date(today.getFullYear(), mm - 1, dd);
      if (nextBday < todayMidnight) nextBday = new Date(today.getFullYear() + 1, mm - 1, dd);
      const diffDays = Math.round((nextBday - todayMidnight) / 86400000);
      if (diffDays === 0) {
        birthdayLabel = 'BIRTHDAY';
        birthdayValue = 'Today! 🎉';
      } else if (diffDays === 1) {
        birthdayLabel = 'BIRTHDAY';
        birthdayValue = 'Tomorrow! 🎂';
      } else {
        birthdayLabel = 'BIRTHDAY IN';
        birthdayValue = `${diffDays} days`;
      }
    }
  }
  return { tenureText, birthdayLabel, birthdayValue };
}

class RosterXUserPortal {
  constructor() {
    this.atlas = new AtlasService();
    this.user = authService.enforceAuth('any');
    if (!this.user) return;
    if (this.user.role === 'admin') {
      window.location.replace(`index.html${window.location.hash || ''}`);
      return;
    }

    this.isManager = String(this.user.role || '').toLowerCase() === 'manager';
    this.engineers = [];
    this.rosterCache = new Map();
    this.notifications = [];
    this.templates = [];
    this.telemetry = null;
    this.credSummary = { total: 0, expired: 0, expiringSoon: 0 };
    this.dismissedCelebrations = new Set();

    const now = getIstNow();
    this.selectedDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    this.gridYear = now.getFullYear();
    this.gridMonth = now.getMonth() + 1;
    this.rosterSearchQuery = '';
    this.activeTab = 'dashboard';
    this.activeSubView = null; // 'credentials' | 'my_team' | 'profile' | null

    // Manager Broadcast state
    this.broadcastMode = 'compose';
    this.broadcastAudience = 'everyone';
    this.broadcastSpecificIds = new Set();
    this.broadcastPriority = 'info';
    this.broadcastChannels = { inApp: true, push: true, email: true };
    this.broadcastTitle = '';
    this.broadcastMessage = '';

    // AI Chat state
    this.aiMessages = [];
    this.aiLoading = false;

    this.tabs = this.isManager
      ? [
          { id: 'dashboard',    label: 'Dashboard',    icon: 'ph-squares-four' },
          { id: 'team_shifts',  label: 'Team Shifts',  icon: 'ph-users-three' },
          { id: 'month_roster', label: 'Month Roster', icon: 'ph-grid-four' },
          { id: 'broadcast',    label: 'Broadcast',    icon: 'ph-megaphone' },
          { id: 'more',         label: 'More',         icon: 'ph-dots-three' }
        ]
      : [
          { id: 'dashboard',    label: 'Dashboard',    icon: 'ph-squares-four' },
          { id: 'team_shifts',  label: 'Team Shifts',  icon: 'ph-users-three' },
          { id: 'my_calendar',  label: 'My Calendar',  icon: 'ph-calendar-blank' },
          { id: 'month_roster', label: 'Month Roster', icon: 'ph-grid-four' },
          { id: 'more',         label: 'More',         icon: 'ph-dots-three' }
        ];

    this.navShell = new MobileNavShell({
      tabs: this.tabs,
      rosterTabId: 'month_roster',
      onTabSelected: (id) => this.switchTab(id)
    });

    this.swipeController = new SwipeTabController({
      container: document.body,
      getTabs: () => this.tabs,
      getActiveTab: () => this.activeTab,
      onSwitchTab: (id) => this.switchTab(id),
      onDragProgress: (fractional, boundaryOffset, isSpring) => {
        this.navShell.setDragProgress(fractional, boundaryOffset, isSpring);
      },
      ensureTabRendered: (tabId) => this.ensureTabRendered(tabId),
      rosterTabId: 'month_roster',
      isSuspended: () => Boolean(this.activeSubView || document.querySelector('.rx-modal-backdrop, [data-modal-open="true"]'))
    });
    this.swipeController.mount();

    this.credController = new CredentialTrackerController({
      container: document.getElementById('subview_credentials'),
      currentUser: this.user,
      hideBackButton: true,
      onBack: () => this.closeSubView(),
      onSummaryUpdate: (summary) => {
        this.credSummary = summary;
        if (this.activeTab === 'more' && !this.activeSubView) this.renderMoreHub();
      },
      onToast: (msg, type) => this.toast(msg, type)
    });

    this.teamController = new MyTeamViewController({
      container: document.getElementById('subview_my_team'),
      engineers: [],
      hideBackButton: true,
      onBack: () => this.closeSubView(),
      onToast: (msg, type) => this.toast(msg, type)
    });

    this.init();
  }

  async init() {
    registerPwaServiceWorker();
    mountIosInstallBanner();
    themeController.init();

    this.renderDesktopTabs();
    this.navShell.mount();
    this.bindGlobalEvents();
    this.updateTopBarUser();

    await this.refreshAllData();

    // Handle deep-link hash (#roster, #notifications, #credentials, #profile, #dashboard)
    const deepTarget = consumeDeepLinkHash();
    if (deepTarget === 'roster') this.switchTab('month_roster');
    else if (deepTarget === 'notifications') {
      if (this.isManager) this.switchTab('broadcast');
      else this.openNotificationsDialog();
    } else if (deepTarget === 'credentials') this.openSubView('credentials');
    else if (deepTarget === 'profile') this.openSubView('profile');
    else this.switchTab('dashboard');

    // Live 1-second shift countdown timer on Engineer Dashboard
    setInterval(() => {
      if (!this.isManager && this.activeTab === 'dashboard' && !this.activeSubView) {
        this.updateLiveTimersInDom();
      }
    }, 1000);
  }

  bindGlobalEvents() {
    document.getElementById('btnOpenProfile')?.addEventListener('click', () => this.openSubView('profile'));
    document.getElementById('btnSubViewBack')?.addEventListener('click', () => this.closeSubView());
    document.getElementById('btnOpenNotifications')?.addEventListener('click', () => this.openNotificationsDialog());
    document.getElementById('btnRefreshPortal')?.addEventListener('click', () => this.refreshAllData(true));
    document.getElementById('btnRosterAiFab')?.addEventListener('click', () => this.openAiAssistantDialog());
    document.getElementById('btnJumpTodayFab')?.addEventListener('click', () => {
      const n = getIstNow();
      this.selectedDate = new Date(n.getFullYear(), n.getMonth(), n.getDate());
      this.renderTeamShifts();
      this.syncAiFabVisibility();
    });

    // Progressive Back-Stack Handling (1:1 Android parity)
    try {
      window.history.replaceState({ scope: 'rosterx_root', tab: 'dashboard' }, '');
    } catch (_) {}

    window.addEventListener('popstate', (e) => {
      // 1. Close open modal dialog first
      const openDialog = document.querySelector('dialog.rx-modal[open]');
      if (openDialog) {
        closeNeumorphicDialog(openDialog);
        try { window.history.pushState({ scope: 'rosterx_root', tab: this.activeTab }, ''); } catch (_) {}
        return;
      }

      // 2. Close active subview if any
      if (this.activeSubView) {
        this.closeSubView();
        try { window.history.pushState({ scope: 'rosterx_root', tab: this.activeTab }, ''); } catch (_) {}
        return;
      }

      // 3. Switch to Dashboard tab if on another tab
      if (this.activeTab !== 'dashboard') {
        this.switchTab('dashboard', false);
        try { window.history.pushState({ scope: 'rosterx_root', tab: 'dashboard' }, ''); } catch (_) {}
        return;
      }

      // 4. At dashboard root: Android-style double-back exit guard (Never logs out!)
      const now = Date.now();
      if (!this.lastBackPressTime || now - this.lastBackPressTime > 2500) {
        this.lastBackPressTime = now;
        this.toast('Press back again to exit', 'info');
        try { window.history.pushState({ scope: 'rosterx_root', tab: 'dashboard' }, ''); } catch (_) {}
        return;
      }

      // Second press within 2.5s: allow exit/minimize while keeping credentials 100% intact
      this.lastBackPressTime = null;
      if (window.matchMedia('(display-mode: standalone)').matches) {
        try { window.close(); } catch (_) {}
      }
      window.history.back();
    });

    // Hide bell for management accounts (exact Android parity)
    if (this.isManager) {
      document.getElementById('btnOpenNotifications')?.classList.add('rx-hidden');
    }

    window.addEventListener('rosterx:themechange', () => {
      if (this.activeSubView === 'profile') this.renderProfileView();
    });
  }

  toast(message, type = 'info') {
    const host = document.getElementById('rxToastHost');
    if (!host) return;
    const el = document.createElement('div');
    el.className = `rx-toast ${type === 'success' ? 'is-success' : type === 'error' ? 'is-error' : ''}`;
    el.textContent = message;
    host.appendChild(el);
    setTimeout(() => el.remove(), 3400);
  }

  renderDesktopTabs() {
    const host = document.getElementById('rxDesktopTabs');
    if (!host) return;
    host.innerHTML = this.tabs.map((t) => `
      <button type="button" class="rx-desktop-tab-btn" data-desktop-tab="${escapeHtml(t.id)}">
        <i class="ph ${escapeHtml(t.icon)}" aria-hidden="true"></i>
        <span>${escapeHtml(t.label)}</span>
      </button>
    `).join('');
    host.querySelectorAll('[data-desktop-tab]').forEach((btn) => {
      btn.addEventListener('click', () => this.switchTab(btn.getAttribute('data-desktop-tab')));
    });
  }

  updateTopBarUser() {
    const selfEng = this.getSelfEngineer() || this.user;
    const name = selfEng?.full_name || selfEng?.name || 'Member';
    const first = String(name).trim().split(/\s+/)[0] || 'Member';
    const firstEl = document.getElementById('rxTopFirstName');
    const avatarEl = document.getElementById('rxTopAvatar');
    if (firstEl) firstEl.textContent = first;
    if (avatarEl) {
      if (selfEng?.profile_picture) {
        avatarEl.innerHTML = `<img src="${escapeHtml(selfEng.profile_picture)}" alt="${escapeHtml(name)}" />`;
      } else {
        avatarEl.textContent = getInitials(name);
      }
    }
  }

  getSelfEngineer() {
    const uname = String(this.user?.username || '').toLowerCase();
    return this.engineers.find((e) => String(e.username || '').toLowerCase() === uname) || this.user;
  }

  async refreshAllData(showFeedback = false) {
    try {
      const [engRes, notifRes, credRes] = await Promise.allSettled([
        this.atlas.getEngineers(),
        this.atlas.getNotifications(),
        fetch('/api/credentials', { headers: getAuthHeaders() }).then((r) => r.json())
      ]);

      if (engRes.status === 'fulfilled' && Array.isArray(engRes.value)) {
        this.engineers = engRes.value;
        const freshSelf = this.getSelfEngineer();
        if (freshSelf) {
          this.user = { ...this.user, ...freshSelf };
          try { localStorage.setItem('rosterx_auth_user', JSON.stringify(this.user)); } catch (_) {}
          this.credController.currentUser = this.user;
        }
        this.teamController.setEngineers(this.engineers);
        this.updateTopBarUser();
      }

      if (notifRes.status === 'fulfilled') {
        try {
          const payload = notifRes.value || {};
          const list = Array.isArray(payload.notifications)
            ? payload.notifications
            : Array.isArray(payload.data?.notifications)
              ? payload.data.notifications
              : Array.isArray(payload.data)
                ? payload.data
                : [];
          const dismissedIds = Array.isArray(payload.dismissed_notification_ids)
            ? payload.dismissed_notification_ids
            : Array.isArray(payload.data?.dismissed_notification_ids)
              ? payload.data.dismissed_notification_ids
              : [];
          const dismissedSet = new Set(dismissedIds.map(String));
          const todayIso = toIsoDate(getIstNow());
          this.notifications = list.filter((n) => {
            if (!n) return false;
            const nid = String(n.id || n._id || '');
            if (nid && dismissedSet.has(nid)) return false;
            return !isExpiredCelebration(n, todayIso);
          });
          this.updateNotificationBadge();
        } catch (notifErr) {
          console.warn('Notification sync guard:', notifErr);
        }
      }

      if (credRes.status === 'fulfilled' && credRes.value?.success) {
        try {
          const payload = credRes.value;
          const trackers = Array.isArray(payload?.data?.trackers)
            ? payload.data.trackers
            : Array.isArray(payload?.trackers)
              ? payload.trackers
              : Array.isArray(payload?.data)
                ? payload.data
                : [];
          this.credSummary = {
            total: trackers.length,
            expired: trackers.filter((t) => String(t?.status || '').toUpperCase() === 'EXPIRED').length,
            expiringSoon: trackers.filter((t) => ['EXPIRING_SOON', 'EXPIRES_TODAY'].includes(String(t?.status || '').toUpperCase())).length
          };
        } catch (credErr) {
          console.warn('Credential summary guard:', credErr);
        }
      }

      if (this.isManager) {
        const [telRes, tplRes] = await Promise.allSettled([
          this.atlas.getServerTelemetry(),
          this.atlas.getBroadcastTemplates()
        ]);
        if (telRes.status === 'fulfilled') this.telemetry = telRes.value?.data || telRes.value;
        if (tplRes.status === 'fulfilled') this.templates = tplRes.value?.templates || tplRes.value?.data || [];
      }

      const now = getIstNow();
      await this.ensureRosterLoaded(now.getFullYear(), now.getMonth() + 1);
      await this.ensureRosterLoaded(this.gridYear, this.gridMonth);

      this.renderActiveView();
      if (showFeedback) this.toast('Synced latest shifts & status from Atlas', 'success');
    } catch (err) {
      this.toast(`Sync warning: ${err.message}`, 'error');
    }
  }

  async ensureRosterLoaded(year, month) {
    const key = toPeriodKey(year, month);
    if (this.rosterCache.has(key)) return this.rosterCache.get(key);
    try {
      const doc = await this.atlas.getRoster(key);
      if (doc) {
        this.rosterCache.set(key, doc);
        return doc;
      }
    } catch (_) {}
    this.rosterCache.set(key, { _id: key, year, month, engineers: [] });
    return this.rosterCache.get(key);
  }

  getRosterMapForEngineer(rosterDoc, eng) {
    if (!rosterDoc || !eng) return {};
    const list = (Array.isArray(rosterDoc.engineers) && rosterDoc.engineers.length > 0)
      ? rosterDoc.engineers
      : (Array.isArray(rosterDoc.roster) ? rosterDoc.roster : []);
    if (list.length === 0) return {};

    const norm = (s) => String(s || '').trim().toLowerCase().replace(/\s+/g, ' ');
    const compact = (s) => norm(s).replace(/[^a-z0-9]/g, '');

    // Build canonical username / identifier keys for this engineer
    const engIdKeys = new Set(
      [eng.username, eng.id, eng.engineer_id, eng.engineerId]
        .map(norm)
        .filter(Boolean)
    );
    const ename = norm(eng.name);
    const full = norm(eng.full_name || eng.fullName);
    const engNames = [ename, full].filter(Boolean);
    const engFirsts = [...new Set(engNames.map((n) => n.split(' ')[0]).filter((f) => f.length >= 2))];
    const engCompacts = new Set([...engIdKeys, ...engNames.map(compact), ...engNames.map((n) => `eng_${compact(n)}`)].filter(Boolean));

    const getRowIdKeys = (r) =>
      [r.username, r.engineer_id, r.engineerId, r.id]
        .map(norm)
        .filter(Boolean);

    // Tier 1: Direct Username / Canonical ID Match (handles r.username, r.engineer_id, r.id)
    let matched = list.find((r) => {
      if (!r || typeof r !== 'object') return false;
      const rowIds = getRowIdKeys(r);
      return rowIds.some((rid) => engIdKeys.has(rid) || engCompacts.has(rid));
    });

    // Tier 2: Exact Name / Full Name Match (case- & whitespace-insensitive)
    if (!matched && engNames.length > 0) {
      matched = list.find((r) => {
        if (!r || typeof r !== 'object') return false;
        const rn = norm(r.name);
        const rf = norm(r.full_name || r.fullName);
        return engNames.some((n) => (rn && rn === n) || (rf && rf === n));
      });
    }

    // Tier 3: Safe First-Name / Prefix Fallback (mirrors Android RosterRepositoryImpl & WidgetProvider)
    if (!matched && engNames.length > 0) {
      matched = list.find((r) => {
        if (!r || typeof r !== 'object') return false;
        const rn = norm(r.name);
        const rf = norm(r.full_name || r.fullName);
        const rowNames = [rn, rf].filter(Boolean);
        return rowNames.some((rowName) => {
          const rowFirst = rowName.split(' ')[0];
          return (
            engFirsts.some((f) => rowName === f || (rowFirst.length >= 2 && rowFirst === f)) ||
            engNames.some((n) => (n.length >= 3 && rowName.startsWith(`${n} `)) || (rowName.length >= 3 && n.startsWith(`${rowName} `)))
          );
        });
      });
    }

    const rawShifts = matched?.shifts;
    const map = {};
    if (Array.isArray(rawShifts)) {
      rawShifts.forEach((val, idx) => {
        const clean = String(val ?? '').trim();
        if (clean) map[String(idx + 1)] = clean;
      });
    } else if (rawShifts && typeof rawShifts === 'object') {
      Object.entries(rawShifts).forEach(([k, v]) => {
        const key = String(k).trim();
        const clean = String(v ?? '').trim();
        if (key && clean) map[key] = clean;
      });
    }
    return map;
  }

  getRosterEngineers(rosterDoc) {
    const SYSTEM_ADMIN_USERNAMES = new Set(['admin', 'sysadmin', 'administrator']);
    const raw = this.engineers.filter((e) => {
      const role = String(e.role || 'engineer').trim().toLowerCase();
      const uname = String(e.username || '').trim().toLowerCase();
      const status = String(e.status || 'active').trim().toLowerCase();
      const hasRosterShifts = Object.keys(this.getRosterMapForEngineer(rosterDoc, e)).length > 0;

      if (e.include_in_roster === true) {
        return status !== 'resigned' || hasRosterShifts;
      }
      if (e.include_in_roster === false || role === 'manager') return false;
      if (SYSTEM_ADMIN_USERNAMES.has(uname)) return false;
      if (role === 'admin' && !hasRosterShifts) return false;
      if (status === 'resigned') return hasRosterShifts;
      return true;
    });
    return sortEngineersByWeekOffs(raw, (e) => e.default_weekly_offs);
  }

  updateNotificationBadge() {
    const badge = document.getElementById('rxBellBadge');
    if (!badge) return;
    const count = this.notifications.length;
    if (count > 0) {
      badge.textContent = count > 99 ? '99+' : String(count);
      badge.classList.remove('rx-hidden');
    } else {
      badge.classList.add('rx-hidden');
    }
  }

  switchTab(tabId, pushHistory = true) {
    if (!this.tabs.some((t) => t.id === tabId)) tabId = this.tabs[0].id;
    if (pushHistory && this.activeTab !== tabId && tabId !== 'dashboard') {
      try { window.history.pushState({ scope: 'tab', tab: tabId }, ''); } catch (_) {}
    }
    this.activeTab = tabId;
    this.activeSubView = null;
    this.navShell.setSubViewOpen(false);
    this.navShell.setActive(tabId);

    document.getElementById('btnSubViewBack')?.classList.add('rx-hidden');
    document.getElementById('rxBrandLogo')?.classList.remove('rx-hidden');
    document.getElementById('rxTopBarTitle').textContent = this.isManager ? 'RosterX Manager' : 'RosterX';
    document.getElementById('rxTopBarActions')?.classList.remove('rx-hidden');
    document.getElementById('rxDesktopNav')?.classList.remove('rx-hidden');

    document.querySelectorAll('[data-desktop-tab]').forEach((btn) => {
      btn.classList.toggle('is-active', btn.getAttribute('data-desktop-tab') === tabId);
    });

    document.querySelectorAll('.rx-tab-panel').forEach((p) => p.classList.add('rx-hidden'));
    document.querySelectorAll('.rx-subview').forEach((s) => s.classList.add('rx-hidden'));
    document.getElementById(`panel_${tabId}`)?.classList.remove('rx-hidden');

    this.syncAiFabVisibility();
    this.renderActiveView();
  }

  syncAiFabVisibility() {
    const aiFab = document.getElementById('btnRosterAiFab');
    if (aiFab) {
      const showAi = this.activeTab === 'dashboard' && !this.activeSubView;
      aiFab.classList.toggle('rx-hidden', !showAi);
    }
    const todayFab = document.getElementById('btnJumpTodayFab');
    if (todayFab) {
      const today = getIstNow();
      const todayMid = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      const selectedMid = new Date(this.selectedDate.getFullYear(), this.selectedDate.getMonth(), this.selectedDate.getDate());
      const diff = Math.round((selectedMid - todayMid) / 86400000);
      const showToday = this.activeTab === 'team_shifts' && !this.activeSubView && diff !== 0;
      todayFab.classList.toggle('rx-hidden', !showToday);
    }
  }

  openSubView(viewName) {
    this.activeSubView = viewName;
    try { window.history.pushState({ scope: 'subview', name: viewName }, ''); } catch (_) {}
    this.navShell.setSubViewOpen(true);
    this.syncAiFabVisibility();

    document.getElementById('btnSubViewBack')?.classList.remove('rx-hidden');
    document.getElementById('rxBrandLogo')?.classList.add('rx-hidden');
    document.getElementById('rxTopBarActions')?.classList.add('rx-hidden');
    document.getElementById('rxDesktopNav')?.classList.add('rx-hidden');

    const titleMap = {
      credentials: 'Credential Tracker',
      my_team: 'My Team Directory',
      profile: 'Profile & Settings'
    };
    document.getElementById('rxTopBarTitle').textContent = titleMap[viewName] || 'RosterX';

    document.querySelectorAll('.rx-tab-panel').forEach((p) => p.classList.add('rx-hidden'));
    document.querySelectorAll('.rx-subview').forEach((s) => s.classList.add('rx-hidden'));
    document.getElementById(`subview_${viewName}`)?.classList.remove('rx-hidden');

    if (viewName === 'credentials') this.credController.load();
    else if (viewName === 'my_team') this.teamController.render();
    else if (viewName === 'profile') this.renderProfileView();
  }

  closeSubView() {
    this.activeSubView = null;
    this.switchTab(this.activeTab);
  }

  renderActiveView() {
    if (this.activeSubView) {
      if (this.activeSubView === 'profile') this.renderProfileView();
      else if (this.activeSubView === 'my_team') this.teamController.render();
      return;
    }
    if (this.activeTab === 'dashboard') {
      if (this.isManager) this.renderManagerDashboard();
      else this.renderEngineerDashboard();
    } else if (this.activeTab === 'team_shifts') {
      this.renderTeamShifts();
    } else if (this.activeTab === 'my_calendar') {
      this.renderMyCalendar();
    } else if (this.activeTab === 'month_roster') {
      this.renderMonthRoster();
    } else if (this.activeTab === 'broadcast') {
      this.renderManagerBroadcast();
    } else if (this.activeTab === 'more') {
      this.renderMoreHub();
    }
  }

  ensureTabRendered(tabId) {
    if (tabId === 'dashboard') {
      const p = document.getElementById('panel_dashboard');
      if (p && !p.hasChildNodes()) {
        if (this.isManager) this.renderManagerDashboard();
        else this.renderEngineerDashboard();
      }
    } else if (tabId === 'team_shifts') {
      const p = document.getElementById('panel_team_shifts');
      if (p && !p.hasChildNodes()) this.renderTeamShifts();
    } else if (tabId === 'my_calendar') {
      const p = document.getElementById('panel_my_calendar');
      if (p && !p.hasChildNodes()) this.renderMyCalendar();
    } else if (tabId === 'month_roster') {
      const p = document.getElementById('panel_month_roster');
      if (p && !p.hasChildNodes()) this.renderMonthRoster();
    } else if (tabId === 'broadcast') {
      const p = document.getElementById('panel_broadcast');
      if (p && !p.hasChildNodes()) this.renderManagerBroadcast();
    } else if (tabId === 'more') {
      const p = document.getElementById('panel_more');
      if (p && !p.hasChildNodes()) this.renderMoreHub();
    }
  }

  // =========================================================================
  // CELEBRATIONS HELPER (Today's Birthdays & Work Anniversaries)
  // =========================================================================
  getTodayCelebrations() {
    const today = getIstNow();
    const md = `${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    const list = [];
    this.engineers.forEach((eng) => {
      if (String(eng.status || '').toLowerCase() === 'resigned') return;
      const bday = String(eng.birthday || '').trim().replace(/\//g, '-');
      const jDate = String(eng.join_date || '').trim().replace(/\//g, '-');
      const isBday = bday.endsWith(md);
      let annYears = 0;
      if (jDate.endsWith(md) && /^\d{4}/.test(jDate)) {
        const diff = today.getFullYear() - Number(jDate.slice(0, 4));
        if (diff >= 1) annYears = diff;
      }
      if (isBday || annYears > 0) {
        list.push({
          username: eng.username,
          name: eng.full_name || eng.name || 'Team Member',
          phone: eng.phone || '',
          isBday,
          annYears,
          isSelf: String(eng.username || '').toLowerCase() === String(this.user?.username || '').toLowerCase()
        });
      }
    });
    return list;
  }

  renderCelebrationsBannerHtml() {
    const celebs = this.getTodayCelebrations().filter((c) => !this.dismissedCelebrations.has(c.username));
    if (!celebs.length) return '';
    return celebs.map((c) => {
      const label = c.isBday && c.annYears
        ? `Happy Birthday & ${c.annYears}y Anniversary, ${c.name}! 🎉`
        : c.isBday
          ? `Happy Birthday, ${c.name}! 🎂`
          : `Happy ${c.annYears}y Work Anniversary, ${c.name}! 🏆`;
      return `
        <div class="rx-card-subtle" style="background:rgba(245,158,11,0.12);border:1px solid rgba(245,158,11,0.38);display:flex;align-items:center;justify-content:space-between;gap:10px;cursor:pointer;" data-open-celeb="${escapeHtml(c.username)}">
          <div style="display:flex;align-items:center;gap:10px;min-inline-size:0;">
            <span style="font-size:22px;">${c.isBday ? '🎂' : '🏆'}</span>
            <div style="min-inline-size:0;">
              <div style="font-size:13px;font-weight:800;">${escapeHtml(label)}</div>
              <div style="font-size:11px;color:var(--nm-accent);font-weight:600;">${c.isSelf ? 'Tap to open your celebration wishes!' : 'Celebrating with the team today'}</div>
            </div>
          </div>
          <button type="button" data-dismiss-celeb="${escapeHtml(c.username)}" class="rx-icon-pill" aria-label="Dismiss" style="width:28px;height:28px;min-inline-size:28px;min-block-size:28px;">
            <i class="ph ph-x"></i>
          </button>
        </div>
      `;
    }).join('');
  }

  openCelebrationDialog(c) {
    if (!c) return;
    const dlg = document.getElementById('rxCelebrationDialog');
    if (!dlg) return;

    const isSelf = Boolean(c.isSelf);
    const properName = escapeHtml(c.name || 'Team Member');
    const annYears = c.annYears || 1;
    const yrStr = `${annYears}${annYears === 1 ? 'st' : annYears === 2 ? 'nd' : annYears === 3 ? 'rd' : 'th'}`;
    const yrLabel = annYears === 1 ? '1 year' : `${annYears} years`;

    let heroIcon = c.isBday ? '🎂🎉' : '🏆🎉';
    let title = '';
    let subtitle = '';
    let wishBody = '';

    if (c.isBday && c.annYears) {
      heroIcon = '🎂🏆';
      subtitle = isSelf ? 'A DOUBLE MILESTONE TO CHERISH 🌟' : 'DOUBLE MILESTONE 🌟';
      title = isSelf ? `Double Celebration: Birthday & ${yrStr} Anniversary! 🎂🏆` : `Double Celebration for ${properName}! 🎂🏆`;
      wishBody = isSelf
        ? `Today is doubly special! We celebrate both your Birthday and your ${yrStr} Work Anniversary at RosterX! Thank you for being a pillar of our operations. May this year bring you abundant happiness, fulfillment, and great shifts ahead! 🎈🥂🎉`
        : `Today is doubly special for ${properName} celebrating their birthday and ${yrLabel} with RosterX! Send your warmest congratulations! 🎈🥂🎉`;
    } else if (c.isBday) {
      subtitle = isSelf ? 'CELEBRATING YOUR SPECIAL DAY 🎉' : 'TEAM MEMBER BIRTHDAY 🎂';
      title = isSelf ? 'Happy Birthday! 🎂' : `Happy Birthday, ${properName}! 🎂`;
      wishBody = isSelf
        ? 'On this wonderful day, the entire team at RosterX wishes you joy, vibrant health, and extraordinary success! Thank you for bringing your energy, brilliance, and positive spirit to our operations every single day. Have a fabulous birthday celebration! 🎈🥳✨'
        : `Today is ${properName}'s birthday! Join the entire RosterX team in wishing them immense happiness, vibrant health, and continued success! 🎈🥳✨`;
    } else {
      subtitle = 'MILESTONE ACHIEVED 🌟';
      title = isSelf ? `Happy ${yrStr} Work Anniversary! 🏆` : `Happy ${yrStr} Work Anniversary, ${properName}! 🏆`;
      wishBody = isSelf
        ? `Congratulations on completing ${yrStr} with our team! Your unwavering dedication, operational excellence, and teamwork continue to make a lasting impact. Here's to honoring your journey and achieving many more stellar milestones together! 🥂🎊`
        : `Today ${properName} completes ${yrLabel} of operational excellence with our team! Let's celebrate their dedication, brilliance, and teamwork! 🥂🎊✨`;
    }

    const cleanDigits = String(c.phone || '').replace(/\D/g, '');
    const waNumber = cleanDigits.length === 10 ? '91' + cleanDigits : cleanDigits;
    const waGreeting = c.annYears
      ? `Happy Work Anniversary, ${c.name || 'Teammate'}! 🏆✨ Congratulations on completing ${yrLabel} with our team! Wishing you continued success and joy!`
      : `Happy Birthday, ${c.name || 'Teammate'}! 🎉🎂 Wishing you a fantastic year ahead filled with joy and success!`;
    const waUrl = waNumber
      ? `https://wa.me/${waNumber}?text=${encodeURIComponent(waGreeting)}`
      : `https://wa.me/?text=${encodeURIComponent(waGreeting)}`;

    dlg.innerHTML = `
      <div class="rx-modal-scroll" style="text-align:center;display:flex;flex-direction:column;align-items:center;gap:12px;padding:24px 20px;">
        <div style="font-size:52px;line-height:1;margin-bottom:4px;">${heroIcon}</div>
        <div style="display:inline-block;padding:3px 10px;border-radius:12px;background:rgba(245,158,11,0.14);color:#B45309;font-size:10.5px;font-weight:900;letter-spacing:0.04em;text-transform:uppercase;">
          ${escapeHtml(subtitle)}
        </div>
        <h2 id="rxCelebDialogTitle" style="margin:0;font-size:19px;font-weight:900;color:var(--nm-text);line-height:1.35;">
          ${escapeHtml(title)}
        </h2>
        <p style="margin:0;font-size:13px;color:var(--nm-text-muted);line-height:1.55;max-width:440px;">
          ${escapeHtml(wishBody)}
        </p>

        ${isSelf ? `
          <button type="button" class="rx-btn-primary" data-rx-close style="width:100%;margin-top:6px;min-block-size:46px;font-size:14.5px;font-weight:800;background:linear-gradient(135deg, #F59E0B 0%, #D97706 100%);color:#FFFFFF;box-shadow:0 4px 14px rgba(245,158,11,0.35);">
            🎉 Thank You! 🙏
          </button>
        ` : `
          <div style="display:flex;flex-direction:column;gap:10px;width:100%;margin-top:6px;">
            <div style="display:flex;gap:10px;width:100%;">
              <button type="button" id="btnCelebWhatsApp" class="rx-btn-primary" style="flex:1;min-block-size:46px;background:#25D366;color:#FFFFFF;font-weight:800;font-size:13.5px;box-shadow:0 4px 14px rgba(37,211,102,0.32);">
                <i class="ph-fill ph-whatsapp-logo" style="font-size:18px;"></i> Wish on WhatsApp
              </button>
              ${cleanDigits ? `
                <a href="tel:${cleanDigits}" id="btnCelebCall" class="rx-btn-surface" style="min-inline-size:46px;min-block-size:46px;padding:0 14px;display:inline-flex;align-items:center;justify-content:center;gap:6px;font-size:13px;font-weight:800;color:var(--nm-accent);text-decoration:none;border-radius:14px;">
                  <i class="ph-fill ph-phone-call" style="font-size:18px;"></i> Call
                </a>
              ` : ''}
            </div>
            <button type="button" class="rx-btn-surface" data-rx-close style="width:100%;min-block-size:40px;font-size:12.5px;font-weight:700;color:var(--nm-text-muted);">
              Close
            </button>
          </div>
        `}
      </div>
    `;

    dlg.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dlg));
    dlg.querySelector('#btnCelebWhatsApp')?.addEventListener('click', () => {
      window.open(waUrl, '_blank', 'noopener,noreferrer');
    });

    openNeumorphicDialog(dlg);
  }

  bindCelebrationEvents(root) {
    root.querySelectorAll('[data-dismiss-celeb]').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        this.dismissedCelebrations.add(btn.getAttribute('data-dismiss-celeb'));
        this.renderActiveView();
      });
    });
    root.querySelectorAll('[data-open-celeb]').forEach((card) => {
      card.addEventListener('click', () => {
        const uname = card.getAttribute('data-open-celeb');
        const c = this.getTodayCelebrations().find((x) => x.username === uname);
        if (!c) return;
        this.openCelebrationDialog(c);
      });
    });
  }

  // =========================================================================
  // TAB 1A: ENGINEER DASHBOARD
  // =========================================================================
  computeEngineerTimers(myShifts) {
    const now = getIstNow();
    const todayDay = now.getDate();
    if (!myShifts || Object.keys(myShifts).length === 0) {
      return { shiftLabel: 'Next Shift:', shiftTime: 'Not Published', isRunning: false, restValue: 'Pending Roster' };
    }
    const nowSec = now.getHours() * 3600 + now.getMinutes() * 60 + now.getSeconds();
    const rawToday = String(myShifts[String(todayDay)] || '').trim().toUpperCase();
    const spec = SHIFT_SPECS[rawToday];

    let shiftLabel = 'Next Shift Starts in:';
    let shiftTime = '--';
    let isRunning = false;

    if (spec) {
      const startSec = spec.startH * 3600 + spec.startM * 60;
      const endSec = (spec.nextDay ? 24 : 0) * 3600 + spec.endH * 3600 + spec.endM * 60;
      if (nowSec >= startSec && nowSec < endSec) {
        isRunning = true;
        shiftLabel = 'Shift Ends in:';
        shiftTime = formatDuration(endSec - nowSec);
      } else if (nowSec < startSec) {
        shiftTime = formatDuration(startSec - nowSec);
      }
    }

    if (shiftTime === '--') {
      const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
      for (let d = todayDay + 1; d <= daysInMonth; d++) {
        const c = String(myShifts[String(d)] || '').trim().toUpperCase();
        const s = SHIFT_SPECS[c];
        if (s) {
          const target = new Date(now.getFullYear(), now.getMonth(), d, s.startH, s.startM, 0);
          shiftTime = formatDuration((target - now) / 1000);
          break;
        }
      }
    }

    let restValue = 'Upcoming';
    if (rawToday && !spec) {
      restValue = 'Today (Full Rest)';
    } else {
      const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
      for (let d = todayDay + 1; d <= daysInMonth; d++) {
        const c = String(myShifts[String(d)] || '').trim().toUpperCase();
        if (c && !SHIFT_SPECS[c]) {
          const dt = new Date(now.getFullYear(), now.getMonth(), d);
          restValue = dt.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
          break;
        }
      }
    }
    return { shiftLabel, shiftTime, isRunning, restValue };
  }

  updateLiveTimersInDom() {
    const now = getIstNow();
    const doc = this.rosterCache.get(toPeriodKey(now.getFullYear(), now.getMonth() + 1));
    const myShifts = this.getRosterMapForEngineer(doc, this.getSelfEngineer());
    const t = this.computeEngineerTimers(myShifts);
    const lbl = document.getElementById('rxDashShiftTimerLabel');
    const val = document.getElementById('rxDashShiftTimerValue');
    if (lbl) lbl.textContent = t.shiftLabel;
    if (val) val.textContent = t.shiftTime;
  }

  renderEngineerDashboard() {
    const panel = document.getElementById('panel_dashboard');
    if (!panel) return;
    const now = getIstNow();
    const tomorrow = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const doc = this.rosterCache.get(toPeriodKey(now.getFullYear(), now.getMonth() + 1));
    const selfEng = this.getSelfEngineer();
    const myShifts = this.getRosterMapForEngineer(doc, selfEng);

    const todayShift = getShiftOrUnscheduled(myShifts[String(now.getDate())]);
    const tomDoc = this.rosterCache.get(toPeriodKey(tomorrow.getFullYear(), tomorrow.getMonth() + 1)) || doc;
    const tomShifts = this.getRosterMapForEngineer(tomDoc, selfEng);
    const tomorrowShift = getShiftOrUnscheduled(tomShifts[String(tomorrow.getDate())]);

    let dayShifts = 0;
    let nightShifts = 0;
    let restDays = 0;
    Object.values(myShifts).forEach((raw) => {
      const c = String(raw || '').trim().toUpperCase();
      if (!c) return;
      if (c === 'NS') nightShifts++;
      else if (WORKING_SHIFT_CODES.has(c)) dayShifts++;
      else if (OFF_OR_LEAVE_CODES.has(c) || c.startsWith('SW')) restDays++;
    });

    const timers = this.computeEngineerTimers(myShifts);
    const displayName = selfEng?.full_name || selfEng?.name || 'Engineer';
    const dateStr = now.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    const todayShort = now.toLocaleDateString('en-US', { month: 'short', day: '2-digit' });
    const tomShort = tomorrow.toLocaleDateString('en-US', { month: 'short', day: '2-digit' });

    const avatarHtml = selfEng?.profile_picture
      ? `<img src="${escapeHtml(selfEng.profile_picture)}" alt="${escapeHtml(displayName)}" style="width:48px;height:48px;border-radius:999px;object-fit:cover;" />`
      : `<div style="width:48px;height:48px;border-radius:999px;background:var(--nm-accent);color:#fff;font-weight:900;font-size:17px;display:flex;align-items:center;justify-content:center;">${escapeHtml(getInitials(displayName))}</div>`;

    panel.innerHTML = `
      <div class="rx-land-grid-2" style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;flex-direction:column;gap:12px;">
          <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
            <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
              <div style="min-inline-size:0;">
                <div style="font-size:11.5px;font-weight:800;color:var(--nm-accent);">${escapeHtml(dateStr)}</div>
                <h1 style="margin:3px 0 2px;font-size:20px;font-weight:900;">Welcome, ${escapeHtml(displayName)}! 👋</h1>
                <div style="font-size:12px;color:var(--nm-text-muted);">Operational shift overview &amp; live duty briefing</div>
              </div>
              ${avatarHtml}
            </div>

            <div style="display:flex;flex-direction:column;gap:8px;">
              <div class="rx-well" style="padding:8px 12px;border-radius:12px;display:flex;align-items:center;gap:8px;font-size:12px;">
                <i class="ph ph-timer" style="color:var(--nm-accent-cyan);font-size:16px;" aria-hidden="true"></i>
                <span id="rxDashShiftTimerLabel" style="color:var(--nm-text-muted);font-weight:600;">${escapeHtml(timers.shiftLabel)}</span>
                <strong id="rxDashShiftTimerValue" style="color:var(--nm-text);font-weight:800;">${escapeHtml(timers.shiftTime)}</strong>
              </div>
              <div style="padding:8px 12px;border-radius:12px;background:rgba(245,158,11,0.14);border:1px solid rgba(245,158,11,0.35);display:flex;align-items:center;gap:8px;font-size:12px;">
                <i class="ph ph-sun" style="color:#D97706;font-size:16px;" aria-hidden="true"></i>
                <span style="color:#B45309;font-weight:600;">Rest Day:</span>
                <strong style="color:#92400E;font-weight:800;">${escapeHtml(timers.restValue)}</strong>
              </div>
            </div>
          </div>

          ${this.renderCelebrationsBannerHtml()}
        </div>

        <div style="display:flex;flex-direction:column;gap:12px;">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
              <div style="font-size:10px;font-weight:800;color:var(--nm-text-muted);letter-spacing:0.04em;">TODAY'S SHIFT (${escapeHtml(todayShort)})</div>
              <div style="display:flex;align-items:center;gap:10px;">
                <div style="width:48px;height:48px;border-radius:14px;background:${todayShift.bg};color:${todayShift.fg};display:flex;align-items:center;justify-content:center;font-weight:900;font-size:14px;flex-shrink:0;">${escapeHtml(todayShift.code)}</div>
                <div style="min-inline-size:0;">
                  <div style="font-size:14px;font-weight:800;">${escapeHtml(todayShift.name)}</div>
                  <div style="font-size:11.5px;font-weight:700;color:${todayShift.unscheduled ? 'var(--nm-text-muted)' : (SHIFT_SPECS[todayShift.code] ? '#4F46E5' : '#059669')};">${todayShift.unscheduled ? 'Pending Roster' : (SHIFT_SPECS[todayShift.code] ? 'Active Duty' : 'Rest Day')}</div>
                </div>
              </div>
            </div>

            <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
              <div style="font-size:10px;font-weight:800;color:var(--nm-text-muted);letter-spacing:0.04em;">TOMORROW'S SHIFT (${escapeHtml(tomShort)})</div>
              <div style="display:flex;align-items:center;gap:10px;">
                <div style="width:48px;height:48px;border-radius:14px;background:${tomorrowShift.bg};color:${tomorrowShift.fg};display:flex;align-items:center;justify-content:center;font-weight:900;font-size:14px;flex-shrink:0;">${escapeHtml(tomorrowShift.code)}</div>
                <div style="min-inline-size:0;">
                  <div style="font-size:14px;font-weight:800;">${escapeHtml(tomorrowShift.name)}</div>
                  <div style="font-size:11.5px;font-weight:700;color:${tomorrowShift.unscheduled ? 'var(--nm-text-muted)' : (SHIFT_SPECS[tomorrowShift.code] ? '#4F46E5' : '#059669')};">${tomorrowShift.unscheduled ? 'Pending Roster' : (SHIFT_SPECS[tomorrowShift.code] ? 'Active Duty' : 'Rest Day')}</div>
                </div>
              </div>
            </div>
          </div>

          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div class="rx-card" style="display:flex;flex-direction:column;gap:8px;">
              <div style="font-size:10px;font-weight:800;color:var(--nm-text-muted);">MONTHLY DUTIES</div>
              <div style="display:flex;justify-content:space-around;text-align:center;">
                <div>
                  <div style="font-size:9.5px;font-weight:700;color:var(--nm-text-muted);">DAY SHIFTS</div>
                  <div style="font-size:26px;font-weight:900;color:#0284C7;">${dayShifts}</div>
                </div>
                <div>
                  <div style="font-size:9.5px;font-weight:700;color:var(--nm-text-muted);">NIGHT SHIFTS</div>
                  <div style="font-size:26px;font-weight:900;color:#0284C7;">${nightShifts}</div>
                </div>
              </div>
              <div style="font-size:11px;font-weight:700;color:var(--nm-text-muted);">Total Assigned: ${dayShifts + nightShifts} days</div>
            </div>

            <div class="rx-card" style="display:flex;flex-direction:column;gap:8px;">
              <div style="font-size:10px;font-weight:800;color:var(--nm-text-muted);">SCHEDULED REST DAYS</div>
              <div style="text-align:center;">
                <div style="font-size:9.5px;font-weight:700;color:var(--nm-text-muted);">Weekly Offs / Rest</div>
                <div style="font-size:26px;font-weight:900;color:#D97706;">${restDays}</div>
              </div>
              <div style="font-size:11px;font-weight:700;color:#B45309;">Rest Day: ${escapeHtml(timers.restValue)}</div>
            </div>
          </div>
        </div>
      </div>
    `;
    this.bindCelebrationEvents(panel);
  }

  // =========================================================================
  // TAB 1B: MANAGER DASHBOARD (Shift Radar + System Health — Android Manager Parity)
  // =========================================================================
  renderManagerDashboard() {
    const panel = document.getElementById('panel_dashboard');
    if (!panel) return;
    const now = getIstNow();
    const doc = this.rosterCache.get(toPeriodKey(now.getFullYear(), now.getMonth() + 1));
    const selfEng = this.getSelfEngineer();
    const displayName = selfEng?.full_name || selfEng?.name || 'Manager';
    const rosterEngs = this.getRosterEngineers(doc);

    const groups = new Map();
    rosterEngs.forEach((e) => {
      const m = this.getRosterMapForEngineer(doc, e);
      const rawCode = String(m[String(now.getDate())] || '').trim();
      if (!rawCode) return;
      const vis = getShiftVisual(rawCode);
      if (!groups.has(vis.code)) groups.set(vis.code, { vis, members: [] });
      groups.get(vis.code).members.push(e);
    });
    const sorted = Array.from(groups.values()).sort((a, b) => a.vis.order - b.vis.order);

    const uptime = this.telemetry?.uptime_formatted || 'Online';
    const pingMs = this.telemetry?.atlas?.ping_ms ?? '--';
    const atlasStatus = this.telemetry?.atlas?.status || 'CONNECTED';

    panel.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div class="rx-card" style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
          <div>
            <div style="font-size:11px;font-weight:800;color:var(--nm-accent);">${escapeHtml(now.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric', year: 'numeric' }))}</div>
            <h1 style="margin:2px 0;font-size:19px;font-weight:900;">Welcome, ${escapeHtml(displayName)}! 👋</h1>
            <div style="font-size:12px;color:var(--nm-text-muted);">Live operations telemetry &amp; today's team shift radar</div>
          </div>
          <div style="width:46px;height:46px;border-radius:999px;background:var(--nm-accent-soft);color:var(--nm-accent);display:flex;align-items:center;justify-content:center;font-weight:900;">${escapeHtml(getInitials(displayName))}</div>
        </div>

        ${this.renderCelebrationsBannerHtml()}

        <div class="rx-land-grid-2" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:14px;">
          <div class="rx-card" style="display:flex;flex-direction:column;gap:10px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <strong style="font-size:13px;color:var(--nm-accent);">TODAY'S SHIFT RADAR</strong>
              <span style="font-size:11px;font-weight:700;color:var(--nm-text-muted);">${rosterEngs.length} Active Staff</span>
            </div>
            ${sorted.length === 0
              ? `<div class="rx-well" style="padding:14px;text-align:center;font-size:12px;font-weight:700;color:var(--nm-text-muted);">No shifts published for today yet</div>`
              : sorted.map(({ vis, members }) => `
              <div class="rx-well" style="padding:9px 12px;display:flex;align-items:center;justify-content:space-between;gap:8px;">
                <div style="display:flex;align-items:center;gap:8px;min-inline-size:0;">
                  <span style="padding:3px 7px;border-radius:7px;background:${vis.bg};color:${vis.fg};font-size:10.5px;font-weight:900;">${escapeHtml(vis.code)}</span>
                  <span style="font-size:12.5px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${members.map((m) => escapeHtml(m.name)).join(', ')}</span>
                </div>
                <span style="font-size:11px;font-weight:800;color:var(--nm-text-muted);flex-shrink:0;">${members.length}</span>
              </div>
            `).join('')}
          </div>

          <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <strong style="font-size:13px;color:var(--nm-accent);">SYSTEM &amp; ATLAS HEALTH</strong>
              <span style="padding:3px 8px;border-radius:8px;background:rgba(16,185,129,0.14);color:#059669;font-size:10.5px;font-weight:800;">${escapeHtml(atlasStatus)}</span>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
              <div class="rx-well" style="padding:10px;">
                <div style="font-size:10px;font-weight:700;color:var(--nm-text-muted);">SERVER UPTIME</div>
                <div style="font-size:15px;font-weight:900;color:#059669;margin-top:3px;">${escapeHtml(uptime)}</div>
              </div>
              <div class="rx-well" style="padding:10px;">
                <div style="font-size:10px;font-weight:700;color:var(--nm-text-muted);">ATLAS PING</div>
                <div style="font-size:15px;font-weight:900;color:#0284C7;margin-top:3px;">${escapeHtml(String(pingMs))} ms</div>
              </div>
            </div>
            <div class="rx-well" style="padding:10px;display:flex;justify-content:space-between;align-items:center;">
              <span style="font-size:12px;font-weight:700;">Credential Trackers</span>
              <span style="font-size:12px;font-weight:800;color:${this.credSummary.expired > 0 ? '#DC2626' : '#059669'};">${this.credSummary.total} Tracked • ${this.credSummary.expired} Expired</span>
            </div>
          </div>
        </div>
      </div>
    `;
    this.bindCelebrationEvents(panel);
  }

  // =========================================================================
  // TAB 2: TEAM SHIFTS (Daily Grouped View + Call & WhatsApp)
  // =========================================================================
  async renderTeamShifts() {
    const panel = document.getElementById('panel_team_shifts');
    if (!panel) return;
    const d = this.selectedDate;
    const doc = await this.ensureRosterLoaded(d.getFullYear(), d.getMonth() + 1);
    const today = getIstNow();
    const todayMid = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const diff = Math.round((d - todayMid) / 86400000);
    const dayTag = diff === 0 ? 'TODAY' : diff === -1 ? 'YESTERDAY' : diff === 1 ? 'TOMORROW' : null;

    const engs = this.getRosterEngineers(doc);
    const groups = new Map();
    engs.forEach((e) => {
      const m = this.getRosterMapForEngineer(doc, e);
      const rawCode = String(m[String(d.getDate())] || '').trim();
      if (!rawCode) return;
      const vis = getShiftVisual(rawCode);
      if (!groups.has(vis.code)) groups.set(vis.code, { vis, members: [] });
      groups.get(vis.code).members.push(e);
    });
    const sortedGroups = Array.from(groups.values()).sort((a, b) => a.vis.order - b.vis.order);
    const formattedDateLabel = d.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });

    const selfUname = String(this.user?.username || '').toLowerCase();
    this.syncAiFabVisibility();

    panel.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div class="rx-card-subtle" style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:8px 12px;">
          <button type="button" class="rx-icon-pill" id="btnPrevDay" aria-label="Previous Day"><i class="ph ph-arrow-left"></i></button>
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;justify-content:center;">
            <strong style="font-size:14px;">${escapeHtml(d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: '2-digit', year: 'numeric' }))}</strong>
          </div>
          <button type="button" class="rx-icon-pill" id="btnNextDay" aria-label="Next Day"><i class="ph ph-arrow-right"></i></button>
        </div>

        <div class="rx-land-grid-2" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(290px,1fr));gap:12px;">
          ${sortedGroups.length === 0 ? `
            <div class="rx-card" style="grid-column:1 / -1;text-align:center;padding:24px 16px;">
              <i class="ph ph-calendar-blank" style="font-size:28px;color:var(--nm-text-muted);display:block;margin-bottom:6px;" aria-hidden="true"></i>
              <div style="font-size:14px;font-weight:800;">Not Scheduled</div>
              <div style="font-size:12px;color:var(--nm-text-muted);margin-top:3px;">No roster published for ${escapeHtml(formattedDateLabel)} yet</div>
            </div>
          ` : sortedGroups.map(({ vis, members }) => `
            <div class="rx-card" style="display:flex;flex-direction:column;gap:10px;">
              <div style="display:flex;align-items:center;justify-content:space-between;padding-bottom:8px;border-bottom:1px solid var(--nm-divider);">
                <div style="display:flex;align-items:center;gap:10px;">
                  <div style="width:36px;height:36px;border-radius:10px;background:${vis.bg};color:${vis.fg};font-weight:900;font-size:12px;display:flex;align-items:center;justify-content:center;">${escapeHtml(vis.code)}</div>
                  <div>
                    <div style="font-size:14px;font-weight:800;">${escapeHtml(vis.name)}</div>
                    <div style="font-size:11px;color:var(--nm-text-muted);">${escapeHtml(vis.timing)}</div>
                  </div>
                </div>
                <span style="padding:3px 9px;border-radius:10px;background:var(--nm-accent-soft);color:var(--nm-accent);font-size:11px;font-weight:800;">${members.length} Eng</span>
              </div>

              <div style="display:flex;flex-direction:column;gap:6px;">
                ${members.map((eng) => {
                  const isMe = String(eng.username || '').toLowerCase() === selfUname;
                  const name = eng.full_name || eng.name || 'Member';
                  const tel = String(eng.phone || '').replace(/[^0-9+]/g, '');
                  const digits = String(eng.phone || '').replace(/[^0-9]/g, '');
                  const wa = digits.length === 10 ? `91${digits}` : digits;
                  return `
                    <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:7px 10px;border-radius:12px;background:${isMe ? 'var(--nm-accent-soft)' : 'var(--nm-input-surface)'};">
                      <div style="display:flex;align-items:center;gap:8px;min-inline-size:0;">
                        <span class="rx-avatar-circle">${eng.profile_picture ? `<img src="${escapeHtml(eng.profile_picture)}" alt="${escapeHtml(name)}" />` : escapeHtml(getInitials(name))}</span>
                        <span style="font-size:13px;font-weight:${isMe ? '800' : '600'};color:${isMe ? 'var(--nm-accent)' : 'var(--nm-text)'};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(name)} ${isMe ? '★' : ''}</span>
                      </div>
                      <div style="display:flex;gap:6px;flex-shrink:0;">
                        ${tel ? `<a href="tel:${escapeHtml(tel)}" class="rx-icon-pill" aria-label="Call ${escapeHtml(name)}" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;background:rgba(5,150,105,0.13);color:#059669;box-shadow:none;text-decoration:none;"><i class="ph ph-phone"></i></a>` : ''}
                        ${wa ? `<a href="https://wa.me/${escapeHtml(wa)}" target="_blank" rel="noopener" class="rx-icon-pill" aria-label="WhatsApp ${escapeHtml(name)}" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;background:rgba(37,211,102,0.15);color:#16A34A;box-shadow:none;text-decoration:none;"><i class="ph ph-whatsapp-logo"></i></a>` : ''}
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    panel.querySelector('#btnPrevDay')?.addEventListener('click', () => {
      this.selectedDate = new Date(this.selectedDate.getFullYear(), this.selectedDate.getMonth(), this.selectedDate.getDate() - 1);
      this.renderTeamShifts();
    });
    panel.querySelector('#btnNextDay')?.addEventListener('click', () => {
      this.selectedDate = new Date(this.selectedDate.getFullYear(), this.selectedDate.getMonth(), this.selectedDate.getDate() + 1);
      this.renderTeamShifts();
    });
    panel.querySelector('#btnJumpToday')?.addEventListener('click', () => {
      const n = getIstNow();
      this.selectedDate = new Date(n.getFullYear(), n.getMonth(), n.getDate());
      this.renderTeamShifts();
    });
  }

  // =========================================================================
  // TAB 3: MY CALENDAR (Interactive Grid + Sync to Calendar .ics + Shift Legend)
  // =========================================================================
  async renderMyCalendar() {
    const panel = document.getElementById('panel_my_calendar');
    if (!panel) return;
    const doc = await this.ensureRosterLoaded(this.gridYear, this.gridMonth);
    const selfEng = this.getSelfEngineer();
    const myShifts = this.getRosterMapForEngineer(doc, selfEng);

    const monthStart = new Date(this.gridYear, this.gridMonth - 1, 1);
    const monthLabel = monthStart.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    const daysInMonth = new Date(this.gridYear, this.gridMonth, 0).getDate();
    const firstDow = (monthStart.getDay() + 6) % 7; // Mon=0..Sun=6
    const today = getIstNow();

    const dayShiftMapInt = {};
    Object.entries(myShifts).forEach(([k, v]) => {
      const n = Number(k);
      if (Number.isInteger(n) && n >= 1 && n <= 31) dayShiftMapInt[n] = v;
    });
    const hasAnyDays = Object.keys(dayShiftMapInt).length > 0;

    let cellsHtml = '';
    for (let i = 0; i < firstDow; i++) cellsHtml += `<div></div>`;
    for (let day = 1; day <= daysInMonth; day++) {
      const raw = myShifts[String(day)];
      const vis = raw ? getShiftVisual(raw) : null;
      const isToday = this.gridYear === today.getFullYear() && this.gridMonth === (today.getMonth() + 1) && day === today.getDate();
      const bg = vis ? vis.bg : 'var(--nm-surface-contrast)';
      const fg = vis ? vis.fg : 'var(--nm-text)';
      cellsHtml += `
        <button type="button" class="rx-calendar-cell ${isToday ? 'is-today' : ''}" data-cal-day="${day}" style="background:${bg};color:${fg};">
          <span class="rx-calendar-day">${day}</span>
          ${vis ? `<span class="rx-calendar-code">${escapeHtml(vis.code)}</span>` : ''}
        </button>
      `;
    }

    panel.innerHTML = `
      <div class="rx-calendar-split">
        <div style="display:flex;flex-direction:column;gap:10px;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
            <button type="button" class="rx-icon-pill" id="btnCalPrevMonth" aria-label="Previous Month"><i class="ph ph-arrow-left"></i></button>
            <strong style="font-size:17px;font-weight:900;">${escapeHtml(monthLabel)}</strong>
            <button type="button" class="rx-icon-pill" id="btnCalNextMonth" aria-label="Next Month"><i class="ph ph-arrow-right"></i></button>
          </div>

          <div class="rx-cal-weekdays">
            ${['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((w) => `<div class="rx-cal-weekday">${w}</div>`).join('')}
          </div>
          ${!hasAnyDays ? `
            <div class="rx-well" style="padding:8px 12px;border-radius:12px;font-size:12px;font-weight:700;color:var(--nm-text-muted);display:flex;align-items:center;gap:8px;">
              <i class="ph ph-info" style="color:var(--nm-accent);font-size:15px;" aria-hidden="true"></i>
              <span>No roster published for ${escapeHtml(monthLabel)} yet</span>
            </div>
          ` : ''}
          <div class="rx-cal-grid">${cellsHtml}</div>
        </div>

        <div style="display:flex;flex-direction:column;gap:12px;">
          <div class="rx-card" style="display:flex;flex-direction:column;gap:10px;">
            <div style="font-size:11px;font-weight:900;color:var(--nm-accent);letter-spacing:0.05em;">SHIFT LEGEND &amp; TIMINGS</div>
            <div style="display:flex;flex-direction:column;gap:7px;">
              ${STANDARD_LEGEND.map((code) => {
                const m = SHIFT_META[code];
                return `
                  <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;font-size:12px;">
                    <div style="display:flex;align-items:center;gap:8px;">
                      <span style="min-width:34px;text-align:center;padding:2px 6px;border-radius:6px;background:${m.bg};color:${m.fg};font-size:10px;font-weight:900;">${code}</span>
                      <strong style="font-weight:700;">${m.name}</strong>
                    </div>
                    <span style="font-size:11px;color:var(--nm-text-muted);">${m.timing}</span>
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        </div>
      </div>
    `;

    panel.querySelector('#btnCalPrevMonth')?.addEventListener('click', () => {
      if (this.gridMonth === 1) { this.gridMonth = 12; this.gridYear--; } else this.gridMonth--;
      this.renderMyCalendar();
    });
    panel.querySelector('#btnCalNextMonth')?.addEventListener('click', () => {
      if (this.gridMonth === 12) { this.gridMonth = 1; this.gridYear++; } else this.gridMonth++;
      this.renderMyCalendar();
    });
    panel.querySelectorAll('[data-cal-day]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const day = Number(btn.getAttribute('data-cal-day'));
        this.selectedDate = new Date(this.gridYear, this.gridMonth - 1, day);
        this.switchTab('team_shifts');
      });
    });
  }

  // =========================================================================
  // TAB 4: MONTH ROSTER (Sticky Semantic Matrix + Full-Screen Landscape!)
  // =========================================================================
  async renderMonthRoster() {
    const panel = document.getElementById('panel_month_roster');
    if (!panel) return;
    const doc = await this.ensureRosterLoaded(this.gridYear, this.gridMonth);
    const daysInMonth = new Date(this.gridYear, this.gridMonth, 0).getDate();
    const monthLabel = new Date(this.gridYear, this.gridMonth - 1, 1)
      .toLocaleDateString('en-US', { month: 'long', year: 'numeric' }).toUpperCase();

    const q = this.rosterSearchQuery.trim().toLowerCase();
    const allEngs = this.getRosterEngineers(doc);
    const filtered = !q ? allEngs : allEngs.filter((e) =>
      String(e.name || '').toLowerCase().includes(q) ||
      String(e.full_name || '').toLowerCase().includes(q)
    );

    const dayHeaders = [];
    for (let d = 1; d <= daysInMonth; d++) {
      const dow = new Date(this.gridYear, this.gridMonth - 1, d).toLocaleDateString('en-US', { weekday: 'short' }).slice(0, 2).toUpperCase();
      dayHeaders.push({ d, dow });
    }

    const dayCov = new Array(daysInMonth + 1).fill(0);
    const nightCov = new Array(daysInMonth + 1).fill(0);
    let hasAnyShifts = false;
    allEngs.forEach((e) => {
      const m = this.getRosterMapForEngineer(doc, e);
      if (Object.keys(m).length > 0) hasAnyShifts = true;
      if (String(e.role || '').toLowerCase() === 'apprentice') return;
      for (let d = 1; d <= daysInMonth; d++) {
        const c = String(m[String(d)] || '').trim().toUpperCase();
        if (!c) continue;
        if (c === 'NS') nightCov[d]++;
        else if (SHIFT_SPECS[c]) dayCov[d]++;
      }
    });
    const prettyMonthLabel = new Date(this.gridYear, this.gridMonth - 1, 1)
      .toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

    panel.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:8px;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;">
          <div style="display:flex;align-items:center;gap:6px;">
            <button type="button" class="rx-icon-pill" id="btnRosterPrev" aria-label="Previous Month" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;"><i class="ph ph-arrow-left"></i></button>
            <strong style="font-size:14px;font-weight:900;color:var(--nm-accent);">${escapeHtml(monthLabel)}</strong>
            <button type="button" class="rx-icon-pill" id="btnRosterNext" aria-label="Next Month" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;"><i class="ph ph-arrow-right"></i></button>
          </div>
          <div class="rx-well" style="display:flex;align-items:center;gap:6px;padding:5px 10px;border-radius:10px;width:175px;">
            <i class="ph ph-magnifying-glass" style="color:var(--nm-accent);font-size:14px;"></i>
            <input type="text" id="rxRosterSearch" value="${escapeHtml(this.rosterSearchQuery)}" placeholder="Filter..." autocomplete="off" autocorrect="off" autocapitalize="off" spellcheck="false" inputmode="search" style="flex:1;border:0;background:transparent;color:var(--nm-text);font-size:13px;outline:none;min-inline-size:0;" />
          </div>
        </div>

        ${!hasAnyShifts ? `
          <div class="rx-well" style="padding:8px 12px;border-radius:12px;font-size:12px;font-weight:700;color:var(--nm-text-muted);display:flex;align-items:center;gap:8px;">
            <i class="ph ph-calendar-blank" style="color:var(--nm-accent);font-size:15px;" aria-hidden="true"></i>
            <span>No roster published for ${escapeHtml(prettyMonthLabel)} yet</span>
          </div>
        ` : ''}

        <div class="rx-roster-table-wrap" tabindex="0" role="region" aria-label="Monthly Team Roster">
          <table class="rx-roster-table">
            <thead>
              <tr>
                <th class="rx-sticky-corner">ENGINEER</th>
                ${dayHeaders.map(({ d, dow }) => `<th><div>${d}</div><div style="font-size:9px;color:var(--nm-text-muted);">${dow}</div></th>`).join('')}
                <th style="color:#059669;min-width:38px;">WRK</th>
                <th style="color:#4F46E5;min-width:34px;">NS</th>
                <th style="color:#D97706;min-width:38px;">OFF</th>
              </tr>
            </thead>
            <tbody>
              ${allEngs.map((eng) => {
                const shifts = this.getRosterMapForEngineer(doc, eng);
                const name = eng.full_name || eng.name || 'Member';
                const searchKey = `${name} ${eng.name || ''} ${eng.username || ''}`.toLowerCase();
                const hiddenStyle = (q && !searchKey.includes(q)) ? 'display:none;' : '';
                let wrkCount = 0;
                let nsCount = 0;
                let offCount = 0;
                const cells = dayHeaders.map(({ d }) => {
                  const rawShift = String(shifts[String(d)] || '').trim();
                  if (!rawShift) {
                    return `<td style="background:transparent;color:transparent;"></td>`;
                  }
                  const upper = rawShift.toUpperCase();
                  if (WORKING_SHIFT_CODES.has(upper)) wrkCount++;
                  if (upper === 'NS') nsCount++;
                  if (OFF_OR_LEAVE_CODES.has(upper) || upper.startsWith('SW')) offCount++;
                  const vis = getShiftVisual(rawShift);
                  return `<td style="background:${vis.bg};color:${vis.fg};">${escapeHtml(vis.code)}</td>`;
                }).join('');
                return `
                  <tr data-eng-search="${escapeHtml(searchKey)}" style="${hiddenStyle}">
                    <th class="rx-sticky-col">${escapeHtml(name)}</th>
                    ${cells}
                    <td style="background:rgba(5,150,105,0.10);color:#059669;font-weight:900;">${wrkCount}</td>
                    <td style="background:rgba(79,70,229,0.10);color:#4F46E5;font-weight:900;">${nsCount}</td>
                    <td style="background:rgba(217,119,6,0.10);color:#D97706;font-weight:900;">${offCount}</td>
                  </tr>
                `;
              }).join('')}
              <tr class="rx-coverage-row">
                <th class="rx-sticky-col rx-sticky-coverage" style="font-size:10px;">DAY COVERAGE (≥3)</th>
                ${dayHeaders.map(({ d }) => `<td style="background:rgba(2,132,199,0.12);color:${hasAnyShifts && dayCov[d] < 3 ? '#DC2626' : '#0284C7'};">${dayCov[d]}</td>`).join('')}
                <td style="background:var(--nm-surface-contrast);"></td>
                <td style="background:var(--nm-surface-contrast);"></td>
                <td style="background:var(--nm-surface-contrast);"></td>
              </tr>
              <tr class="rx-coverage-row">
                <th class="rx-sticky-col rx-sticky-coverage" style="font-size:10px;">NIGHT COVERAGE (1)</th>
                ${dayHeaders.map(({ d }) => `<td style="background:rgba(99,102,241,0.12);color:${hasAnyShifts && nightCov[d] !== 1 ? '#DC2626' : '#4F46E5'};">${nightCov[d]}</td>`).join('')}
                <td style="background:var(--nm-surface-contrast);"></td>
                <td style="background:var(--nm-surface-contrast);"></td>
                <td style="background:var(--nm-surface-contrast);"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    `;

    panel.querySelector('#btnRosterPrev')?.addEventListener('click', () => {
      if (this.gridMonth === 1) { this.gridMonth = 12; this.gridYear--; } else this.gridMonth--;
      this.renderMonthRoster();
    });
    panel.querySelector('#btnRosterNext')?.addEventListener('click', () => {
      if (this.gridMonth === 12) { this.gridMonth = 1; this.gridYear++; } else this.gridMonth++;
      this.renderMonthRoster();
    });
    const searchEl = panel.querySelector('#rxRosterSearch');
    if (searchEl) {
      let filterRaf = null;
      searchEl.addEventListener('input', (e) => {
        this.rosterSearchQuery = e.target.value;
        if (filterRaf) cancelAnimationFrame(filterRaf);
        filterRaf = requestAnimationFrame(() => {
          const qNorm = (this.rosterSearchQuery || '').trim().toLowerCase();
          panel.querySelectorAll('tbody tr[data-eng-search]').forEach((row) => {
            const hay = row.getAttribute('data-eng-search') || '';
            row.style.display = (!qNorm || hay.includes(qNorm)) ? '' : 'none';
          });
        });
      });
      searchEl.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') e.preventDefault();
      });
    }
  }

  // =========================================================================
  // TAB 4B (MANAGER): BROADCAST CONSOLE (Title & Body @Mentions + Templates + History)
  // =========================================================================
  renderManagerBroadcast() {
    const panel = document.getElementById('panel_broadcast');
    if (!panel) return;
    const dutyEngs = this.engineers.filter((e) => String(e.status || '').toLowerCase() !== 'resigned' && !['admin', 'manager'].includes(String(e.role || '').toLowerCase()));
    const previewEng = dutyEngs[0] || { name: 'Team Member', username: 'member_username' };

    panel.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
          <div>
            <div style="font-size:11px;font-weight:800;color:var(--nm-accent);">TEAM BROADCAST CONSOLE</div>
            <h2 style="margin:2px 0 0;font-size:18px;font-weight:900;">Multi-Channel Operations Dispatcher</h2>
          </div>
          <div style="display:flex;gap:8px;">
            <button type="button" class="rx-chip-btn ${this.broadcastMode === 'compose' ? 'is-active' : ''}" data-bc-mode="compose">Compose</button>
            <button type="button" class="rx-chip-btn ${this.broadcastMode === 'history' ? 'is-active' : ''}" data-bc-mode="history">History (${this.notifications.length})</button>
          </div>
        </div>

        ${this.broadcastMode === 'compose' ? `
          <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
            ${this.templates.length ? `
              <div>
                <div class="rx-field-label">SAVED TEMPLATES (${this.templates.length})</div>
                <div style="display:flex;gap:8px;overflow-x:auto;padding-bottom:4px;">
                  ${this.templates.map((tpl) => `
                    <button type="button" class="rx-chip-btn" data-bc-tpl="${escapeHtml(tpl.id || tpl._id)}" style="white-space:nowrap;">
                      <i class="ph ph-bookmark-simple"></i> ${escapeHtml(tpl.name || tpl.title)}
                    </button>
                  `).join('')}
                </div>
              </div>
            ` : ''}

            <div>
              <div class="rx-field-label">TARGET AUDIENCE</div>
              <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
                <button type="button" class="rx-chip-btn ${this.broadcastAudience === 'everyone' ? 'is-active' : ''}" data-bc-aud="everyone">Everyone (${dutyEngs.length})</button>
                <button type="button" class="rx-chip-btn ${this.broadcastAudience === 'specific' ? 'is-active' : ''}" data-bc-aud="specific">Specific (${this.broadcastSpecificIds.size})</button>
              </div>
              ${this.broadcastAudience === 'specific' ? `
                <div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:8px;">
                  ${dutyEngs.map((e) => {
                    const id = e.username || e.id;
                    const sel = this.broadcastSpecificIds.has(id);
                    return `<button type="button" class="rx-chip-btn ${sel ? 'is-active' : ''}" data-bc-eng="${escapeHtml(id)}" style="min-block-size:32px;padding:4px 10px;font-size:11px;">${escapeHtml(e.name)}</button>`;
                  }).join('')}
                </div>
              ` : ''}
            </div>

            <div>
              <div class="rx-field-label">DELIVERY CHANNELS</div>
              <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;">
                <button type="button" class="rx-chip-btn ${this.broadcastChannels.inApp ? 'is-active' : ''}" data-bc-ch="inApp">In-App</button>
                <button type="button" class="rx-chip-btn ${this.broadcastChannels.push ? 'is-active' : ''}" data-bc-ch="push">Push (FCM)</button>
                <button type="button" class="rx-chip-btn ${this.broadcastChannels.email ? 'is-active' : ''}" data-bc-ch="email">Email</button>
              </div>
            </div>

            <div>
              <div class="rx-field-label">PRIORITY LEVEL</div>
              <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:6px;">
                ${[
                  { id: 'info', label: 'Info' },
                  { id: 'shift_update', label: 'Shift' },
                  { id: 'urgent', label: 'Urgent' },
                  { id: 'broadcast', label: 'Broadcast' }
                ].map((p) => `<button type="button" class="rx-chip-btn ${this.broadcastPriority === p.id ? 'is-active' : ''}" data-bc-prio="${p.id}">${p.label}</button>`).join('')}
              </div>
            </div>

            <div style="position:relative;">
              <div class="rx-field-label">NOTIFICATION TITLE * (supports @name, @all, @username)</div>
              <input type="text" id="rxBcTitle" value="${escapeHtml(this.broadcastTitle)}" placeholder="Enter broadcast title..." style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
              <div id="rxBcTitleMentions" class="rx-mention-popup rx-hidden"></div>
            </div>

            <div style="position:relative;">
              <div class="rx-field-label">MESSAGE BODY * (supports @name, @all, @username)</div>
              <textarea id="rxBcMessage" rows="3" placeholder="Type broadcast message..." style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;resize:vertical;">${escapeHtml(this.broadcastMessage)}</textarea>
              <div id="rxBcMsgMentions" class="rx-mention-popup rx-hidden"></div>
            </div>

            ${(this.broadcastTitle || this.broadcastMessage) ? `
              <div class="rx-well" style="display:flex;flex-direction:column;gap:4px;">
                <div style="font-size:10px;font-weight:800;color:var(--nm-accent);">LIVE PREVIEW (AS ${escapeHtml(previewEng.name)})</div>
                <strong style="font-size:13.5px;">${escapeHtml(resolveMentions(this.broadcastTitle, previewEng, dutyEngs) || '(Untitled)')}</strong>
                <div style="font-size:12px;color:var(--nm-text-muted);white-space:pre-wrap;">${escapeHtml(resolveMentions(this.broadcastMessage, previewEng, dutyEngs))}</div>
              </div>
            ` : ''}

            <button type="button" id="btnSendBroadcast" class="rx-btn-primary" style="width:100%;">
              <i class="ph ph-paper-plane-tilt"></i> Dispatch Broadcast
            </button>
          </div>
        ` : `
          <div class="rx-land-grid-2" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(290px,1fr));gap:12px;">
            ${this.notifications.length === 0 ? `<div class="rx-card" style="text-align:center;color:var(--nm-text-muted);">No broadcast history yet.</div>` : this.notifications.map((n) => {
              const nid = n.id || n._id;
              return `
                <div class="rx-card-subtle" style="display:flex;flex-direction:column;gap:6px;">
                  <div style="display:flex;justify-content:space-between;gap:8px;align-items:center;">
                    <strong style="font-size:13.5px;">${escapeHtml(n.title || 'Announcement')}</strong>
                    <button type="button" class="rx-icon-pill" data-bc-retract="${escapeHtml(nid)}" aria-label="Retract" style="width:30px;height:30px;min-inline-size:30px;min-block-size:30px;color:#DC2626;"><i class="ph ph-trash"></i></button>
                  </div>
                  <div style="font-size:12px;color:var(--nm-text-muted);">${escapeHtml(n.message || n.raw_message || '')}</div>
                  <div style="font-size:10.5px;color:var(--nm-text-faint);">${escapeHtml(n.created_at || '')}</div>
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;

    panel.querySelectorAll('[data-bc-mode]').forEach((b) => b.addEventListener('click', () => { this.broadcastMode = b.getAttribute('data-bc-mode'); this.renderManagerBroadcast(); }));
    panel.querySelectorAll('[data-bc-aud]').forEach((b) => b.addEventListener('click', () => { this.broadcastAudience = b.getAttribute('data-bc-aud'); this.renderManagerBroadcast(); }));
    panel.querySelectorAll('[data-bc-eng]').forEach((b) => b.addEventListener('click', () => {
      const id = b.getAttribute('data-bc-eng');
      if (this.broadcastSpecificIds.has(id)) this.broadcastSpecificIds.delete(id); else this.broadcastSpecificIds.add(id);
      this.renderManagerBroadcast();
    }));
    panel.querySelectorAll('[data-bc-ch]').forEach((b) => b.addEventListener('click', () => {
      const k = b.getAttribute('data-bc-ch');
      this.broadcastChannels[k] = !this.broadcastChannels[k];
      this.renderManagerBroadcast();
    }));
    panel.querySelectorAll('[data-bc-prio]').forEach((b) => b.addEventListener('click', () => { this.broadcastPriority = b.getAttribute('data-bc-prio'); this.renderManagerBroadcast(); }));
    panel.querySelectorAll('[data-bc-tpl]').forEach((b) => b.addEventListener('click', () => {
      const tpl = this.templates.find((t) => String(t.id || t._id) === b.getAttribute('data-bc-tpl'));
      if (tpl) {
        this.broadcastTitle = tpl.title || '';
        this.broadcastMessage = tpl.message || '';
        this.broadcastPriority = tpl.priority || 'info';
        this.renderManagerBroadcast();
      }
    }));

    const wireMentionInput = (inputId, popupId, field) => {
      const input = panel.querySelector(`#${inputId}`);
      const popup = panel.querySelector(`#${popupId}`);
      if (!input || !popup) return;
      input.addEventListener('input', () => {
        this[field] = input.value;
        const pos = input.selectionStart || 0;
        const before = input.value.slice(0, pos);
        const m = before.match(/@([a-zA-Z0-9._-]*)$/);
        if (!m) { popup.classList.add('rx-hidden'); return; }
        const q = m[1].toLowerCase();
        const opts = [
          { token: '@name', label: 'Recipient Name' },
          { token: '@all', label: 'Everyone' },
          ...dutyEngs.map((e) => ({ token: `@${e.username}`, label: e.full_name || e.name }))
        ].filter((o) => o.token.toLowerCase().includes(q) || o.label.toLowerCase().includes(q)).slice(0, 6);
        if (!opts.length) { popup.classList.add('rx-hidden'); return; }
        popup.innerHTML = opts.map((o) => `<button type="button" class="rx-mention-item" data-tok="${escapeHtml(o.token)}"><span>${escapeHtml(o.token)}</span><span style="color:var(--nm-text-muted);font-weight:500;">${escapeHtml(o.label)}</span></button>`).join('');
        popup.classList.remove('rx-hidden');
        popup.querySelectorAll('[data-tok]').forEach((item) => {
          item.addEventListener('click', () => {
            const tok = item.getAttribute('data-tok');
            const replaced = before.replace(/@([a-zA-Z0-9._-]*)$/, `${tok} `) + input.value.slice(pos);
            input.value = replaced;
            this[field] = replaced;
            popup.classList.add('rx-hidden');
            input.focus();
          });
        });
      });
    };
    wireMentionInput('rxBcTitle', 'rxBcTitleMentions', 'broadcastTitle');
    wireMentionInput('rxBcMessage', 'rxBcMsgMentions', 'broadcastMessage');

    panel.querySelector('#btnSendBroadcast')?.addEventListener('click', async () => {
      if (!this.broadcastTitle.trim() || !this.broadcastMessage.trim()) {
        this.toast('Title and Message are required.', 'error');
        return;
      }
      const target = this.broadcastAudience === 'everyone' ? 'everyone' : Array.from(this.broadcastSpecificIds);
      if (Array.isArray(target) && target.length === 0) {
        this.toast('Select at least one recipient.', 'error');
        return;
      }
      try {
        const res = await this.atlas.sendBroadcast({
          title: this.broadcastTitle.trim(),
          message: this.broadcastMessage.trim(),
          target,
          priority: this.broadcastPriority,
          send_in_app: this.broadcastChannels.inApp,
          send_push: this.broadcastChannels.push,
          send_email: this.broadcastChannels.email
        });
        if (res?.success) {
          this.broadcastTitle = '';
          this.broadcastMessage = '';
          this.toast('Broadcast dispatched!', 'success');
          await this.refreshAllData();
        } else {
          this.toast(res?.message || 'Broadcast failed', 'error');
        }
      } catch (err) {
        this.toast(err.message, 'error');
      }
    });

    panel.querySelectorAll('[data-bc-retract]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const nid = btn.getAttribute('data-bc-retract');
        if (!window.confirm('Retract this broadcast for all recipients?')) return;
        const res = await this.atlas.retractBroadcast(nid);
        if (res?.success) {
          this.toast('Broadcast retracted', 'success');
          await this.refreshAllData();
        }
      });
    });
  }

  // =========================================================================
  // TAB 5: MORE — OPERATIONS HUB (Credential Tracker + My Team)
  // =========================================================================
  renderMoreHub() {
    const panel = document.getElementById('panel_more');
    if (!panel) return;
    const activeMembers = this.engineers.filter((e) =>
      String(e.status || 'active').toLowerCase() !== 'resigned' &&
      ['manager', 'engineer', 'apprentice'].includes(String(e.role || '').toLowerCase()) &&
      String(e.username || '').toLowerCase() !== 'admin'
    ).length;

    const credBadge = this.credSummary.expired > 0
      ? `<span style="padding:3px 9px;border-radius:10px;background:rgba(220,38,38,0.14);color:#DC2626;font-size:11px;font-weight:800;">${this.credSummary.expired} Expired</span>`
      : this.credSummary.expiringSoon > 0
        ? `<span style="padding:3px 9px;border-radius:10px;background:rgba(217,119,6,0.14);color:#D97706;font-size:11px;font-weight:800;">${this.credSummary.expiringSoon} Expiring Soon</span>`
        : `<span style="padding:3px 9px;border-radius:10px;background:rgba(16,185,129,0.14);color:#059669;font-size:11px;font-weight:800;">${this.credSummary.total > 0 ? `${this.credSummary.total} Active Trackers` : 'All Healthy'}</span>`;

    panel.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div>
          <h1 style="margin:0;font-size:22px;font-weight:900;">Operations Hub</h1>
          <div style="font-size:12.5px;color:var(--nm-text-muted);margin-top:2px;">Extended duty utilities &amp; automated server monitors</div>
        </div>

        <div class="rx-land-grid-2" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:14px;">
          <div role="button" tabindex="0" id="btnHubCredentials" class="rx-card" style="border:0;text-align:left;cursor:pointer;display:flex;flex-direction:column;gap:12px;color:var(--nm-text);">
            <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
              <div style="display:flex;align-items:center;gap:12px;">
                <span style="width:44px;height:44px;border-radius:999px;background:var(--nm-accent-soft);color:var(--nm-accent);display:flex;align-items:center;justify-content:center;font-size:22px;flex-shrink:0;"><i class="ph ph-key"></i></span>
                <div>
                  <div style="font-size:16px;font-weight:800;">Server Credential Tracker</div>
                  <div style="font-size:12px;color:var(--nm-text-muted);margin-top:2px;line-height:1.4;">Monitor root &amp; service account password validity with automated 9:00 AM IST alerts.</div>
                </div>
              </div>
              <i class="ph ph-arrow-right" style="color:var(--nm-text-muted);font-size:18px;"></i>
            </div>
            <div>${credBadge}</div>
          </div>

          <div role="button" tabindex="0" id="btnHubMyTeam" class="rx-card" style="border:0;text-align:left;cursor:pointer;display:flex;flex-direction:column;gap:12px;color:var(--nm-text);">
            <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;">
              <div style="display:flex;align-items:center;gap:12px;">
                <span style="width:44px;height:44px;border-radius:999px;background:rgba(99,102,241,0.14);color:#6366F1;display:flex;align-items:center;justify-content:center;font-size:22px;flex-shrink:0;"><i class="ph ph-users-three"></i></span>
                <div>
                  <div style="font-size:16px;font-weight:800;">My Team</div>
                  <div style="font-size:12px;color:var(--nm-text-muted);margin-top:2px;line-height:1.4;">Team directory, hierarchy, and direct Call &amp; WhatsApp options.</div>
                </div>
              </div>
              <i class="ph ph-arrow-right" style="color:var(--nm-text-muted);font-size:18px;"></i>
            </div>
            <div>
              <span style="padding:3px 9px;border-radius:10px;background:rgba(99,102,241,0.14);color:#4F46E5;font-size:11px;font-weight:800;">${activeMembers} Active Members</span>
            </div>
          </div>
        </div>
      </div>
    `;

    const handleKeyNav = (e, callback) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        callback();
      }
    };

    const credBtn = panel.querySelector('#btnHubCredentials');
    credBtn?.addEventListener('click', () => this.openSubView('credentials'));
    credBtn?.addEventListener('keydown', (e) => handleKeyNav(e, () => this.openSubView('credentials')));

    const teamBtn = panel.querySelector('#btnHubMyTeam');
    teamBtn?.addEventListener('click', () => this.openSubView('my_team'));
    teamBtn?.addEventListener('keydown', (e) => handleKeyNav(e, () => this.openSubView('my_team')));
  }

  // =========================================================================
  // SUBSCREEN: PROFILE & SETTINGS (Tenure/Birthday, Weekly Offs, 6 Palettes, Edit/Remove Photo)
  // =========================================================================
  renderProfileView() {
    const view = document.getElementById('subview_profile');
    if (!view) return;
    const eng = this.getSelfEngineer() || this.user;
    const name = eng?.full_name || eng?.name || 'Team Member';
    const role = String(eng?.role || 'engineer').toUpperCase();
    const { tenureText, birthdayLabel, birthdayValue } = computeTenureAndBirthday(eng?.join_date, eng?.birthday);
    const weeklyOffs = Array.isArray(eng?.default_weekly_offs) ? eng.default_weekly_offs.map((d) => String(d).toLowerCase()) : [];

    const isAppearanceOpen = Boolean(this.appearanceExpanded);

    view.innerHTML = `
      <div class="rx-land-grid-2">
        <div style="display:flex;flex-direction:column;gap:14px;">
          <div style="border-radius:24px;padding:18px;background:var(--nm-accent-gradient);color:#fff;box-shadow:0 8px 22px var(--nm-accent-glow);display:flex;align-items:center;justify-content:space-between;gap:12px;">
            <div style="display:flex;align-items:center;gap:14px;min-inline-size:0;">
              <div style="width:60px;height:60px;border-radius:999px;border:2.5px solid rgba(255,255,255,0.85);background:rgba(255,255,255,0.15);display:flex;align-items:center;justify-content:center;font-weight:900;font-size:20px;overflow:hidden;flex-shrink:0;">
                ${eng?.profile_picture ? `<img src="${escapeHtml(eng.profile_picture)}" alt="${escapeHtml(name)}" style="width:100%;height:100%;object-fit:cover;" />` : escapeHtml(getInitials(name))}
              </div>
              <div style="min-inline-size:0;">
                <div style="font-size:18px;font-weight:900;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(name)}</div>
                <div style="display:flex;gap:6px;margin-top:5px;flex-wrap:wrap;">
                  <span style="padding:2px 9px;border-radius:8px;background:#fff;color:var(--nm-accent);font-size:10px;font-weight:900;">${escapeHtml(role)}</span>
                  <span style="padding:2px 9px;border-radius:8px;background:rgba(255,255,255,0.2);color:#fff;font-size:10.5px;font-weight:700;">@${escapeHtml(eng?.username || '')}</span>
                </div>
              </div>
            </div>
            <button type="button" id="btnOpenEditProfile" aria-label="Edit Profile" style="width:40px;height:40px;border:0;border-radius:12px;background:rgba(255,255,255,0.22);color:#fff;font-size:18px;display:flex;align-items:center;justify-content:center;cursor:pointer;flex-shrink:0;">
              <i class="ph ph-pencil-simple"></i>
            </button>
          </div>

          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div class="rx-card-subtle">
              <div style="font-size:10.5px;font-weight:700;color:var(--nm-text-muted);">Tenure</div>
              <div style="font-size:17px;font-weight:900;color:var(--nm-accent);margin-top:4px;">${escapeHtml(tenureText)}</div>
            </div>
            <div class="rx-card-subtle">
              <div style="font-size:10.5px;font-weight:700;color:var(--nm-text-muted);">${escapeHtml(birthdayLabel)}</div>
              <div style="font-size:17px;font-weight:900;color:var(--nm-accent);margin-top:4px;">${escapeHtml(birthdayValue)}</div>
            </div>
          </div>

          ${!this.isManager ? `
            <div class="rx-card">
              <div class="rx-field-label">DEFAULT WEEKLY OFFS</div>
              <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:5px;">
                ${WEEKDAYS_FULL.map((day) => {
                  const active = weeklyOffs.includes(day.toLowerCase());
                  return `<div style="text-align:center;padding:7px 2px;border-radius:10px;font-size:10.5px;font-weight:800;${active ? 'background:var(--nm-accent);color:#fff;box-shadow:0 2px 6px var(--nm-accent-glow);' : 'background:var(--nm-input-surface);color:var(--nm-text-muted);'}">${day.slice(0, 3)}</div>`;
                }).join('')}
              </div>
            </div>
          ` : ''}

          <div class="rx-card" style="display:flex;flex-direction:column;gap:10px;">
            <div class="rx-field-label" style="color:var(--nm-accent);">ACCOUNT DETAILS</div>
            <div class="rx-well" style="display:flex;justify-content:space-between;align-items:center;padding:10px 12px;">
              <div><div style="font-size:10.5px;color:var(--nm-text-muted);">Domain Username</div><strong style="font-size:13.5px;">${escapeHtml(eng?.username || '--')}</strong></div>
            </div>
            <div class="rx-well" style="display:flex;justify-content:space-between;align-items:center;padding:10px 12px;">
              <div style="min-inline-size:0;"><div style="font-size:10.5px;color:var(--nm-text-muted);">Registered Email</div><strong style="font-size:13px;overflow-wrap:anywhere;">${escapeHtml(eng?.email || '--')}</strong></div>
              ${eng?.email ? `<button type="button" id="btnCopyEmail" class="rx-icon-pill" aria-label="Copy Email" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;"><i class="ph ph-copy"></i></button>` : ''}
            </div>
            <div class="rx-well" style="display:flex;justify-content:space-between;align-items:center;padding:10px 12px;">
              <div><div style="font-size:10.5px;color:var(--nm-text-muted);">Contact Phone</div><strong style="font-size:13.5px;">${escapeHtml(eng?.phone || '--')}</strong></div>
            </div>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:14px;">
          <!-- Appearance & Theme Accordion (Default Collapsed with Summary Pill) -->
          <div class="rx-card" id="rxAppearanceCard" style="display:flex;flex-direction:column;gap:0;">
            <div class="rx-accordion-toggle" id="rxAppearanceToggle" style="display:flex;justify-content:space-between;align-items:center;cursor:pointer;padding:2px 0;">
              <div style="display:flex;align-items:center;gap:8px;">
                <div class="rx-field-label" style="margin:0;color:var(--nm-accent);font-size:11px;">APPEARANCE &amp; THEME</div>
                <span data-rx-appearance-summary style="font-size:10.5px;font-weight:800;padding:2px 8px;border-radius:8px;background:var(--nm-accent-soft);color:var(--nm-accent);"></span>
              </div>
              <button type="button" class="rx-icon-pill rx-accordion-caret ${isAppearanceOpen ? 'is-rotated' : ''}" style="width:28px;height:28px;min-inline-size:28px;min-block-size:28px;" aria-label="Toggle Appearance Options">
                <i class="ph ph-caret-down"></i>
              </button>
            </div>
            <div class="rx-accordion-body ${isAppearanceOpen ? '' : 'is-collapsed'}" id="rxAppearanceBody" style="margin-top:12px;">
              ${themeController.renderAppearancePickerHtml()}
            </div>
          </div>

          <div class="rx-card" style="display:flex;flex-direction:column;gap:8px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <div class="rx-field-label" style="margin:0;color:var(--nm-accent);">ABOUT ROSTERX &amp; SPECS</div>
              <span style="padding:3px 9px;border-radius:8px;background:var(--nm-accent-soft);color:var(--nm-accent);font-size:11px;font-weight:900;">v${APP_VERSION}</span>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:12.5px;"><span style="color:var(--nm-text-muted);">Application</span><strong>RosterX Web</strong></div>
            <div style="display:flex;justify-content:space-between;font-size:12.5px;"><span style="color:var(--nm-text-muted);">Production Gateway</span><strong>api.rosterx.online</strong></div>
            <div style="display:flex;justify-content:space-between;font-size:12.5px;"><span style="color:var(--nm-text-muted);">Timezone Anchor</span><strong>Asia/Kolkata (IST)</strong></div>
          </div>

          <button type="button" id="btnSignOutPortal" class="rx-btn-surface" style="width:100%;color:#DC2626;font-weight:800;">
            <i class="ph ph-sign-out"></i> Sign Out
          </button>
        </div>
      </div>
    `;

    themeController.bindAppearancePicker(view.querySelector('#rxAppearanceCard'));
    themeController.updatePaletteUi();

    view.querySelector('#rxAppearanceToggle')?.addEventListener('click', () => {
      this.appearanceExpanded = !this.appearanceExpanded;
      const body = view.querySelector('#rxAppearanceBody');
      const caret = view.querySelector('.rx-accordion-caret');
      if (this.appearanceExpanded) {
        body?.classList.remove('is-collapsed');
        caret?.classList.add('is-rotated');
      } else {
        body?.classList.add('is-collapsed');
        caret?.classList.remove('is-rotated');
      }
    });
    view.querySelector('#btnOpenEditProfile')?.addEventListener('click', () => this.openEditProfileDialog());
    view.querySelector('#btnCopyEmail')?.addEventListener('click', () => {
      navigator.clipboard?.writeText(eng?.email || '').then(() => this.toast('Email copied to clipboard', 'success'));
    });
    view.querySelector('#btnSignOutPortal')?.addEventListener('click', () => authService.logout());
  }

  openEditProfileDialog() {
    const eng = this.getSelfEngineer() || this.user;
    const dlg = document.getElementById('rxEditProfileDialog');
    let pendingPic = eng?.profile_picture || '';

    const render = () => {
      dlg.innerHTML = `
        <div class="rx-modal-scroll">
          <div class="rx-modal-header">
            <div class="rx-modal-title-wrap">
              <div class="rx-modal-badge-icon"><i class="ph ph-user-circle-gear"></i></div>
              <div>
                <h2 id="rxEditProfileTitle" style="margin:0;font-size:17px;font-weight:800;">Edit Profile</h2>
                <div style="font-size:12px;color:var(--nm-text-muted);">Update display details &amp; profile photo</div>
              </div>
            </div>
            <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close"><i class="ph ph-x"></i></button>
          </div>

          <div class="rx-well" style="display:flex;align-items:center;gap:12px;">
            <div style="width:52px;height:52px;border-radius:999px;background:var(--nm-accent-soft);color:var(--nm-accent);display:flex;align-items:center;justify-content:center;font-weight:900;overflow:hidden;flex-shrink:0;">
              ${pendingPic ? `<img src="${escapeHtml(pendingPic)}" alt="Avatar" style="width:100%;height:100%;object-fit:cover;" />` : escapeHtml(getInitials(eng?.name))}
            </div>
            <div style="display:flex;flex-direction:column;gap:6px;flex:1;">
              <label class="rx-btn-surface" style="padding:7px 12px;font-size:12px;width:fit-content;">
                <i class="ph ph-camera"></i> Upload Photo
                <input type="file" accept="image/*" id="rxPhotoFileInput" style="display:none;" />
              </label>
              ${pendingPic ? `<button type="button" id="btnPromptRemovePhoto" style="border:0;background:transparent;color:#DC2626;font-size:11.5px;font-weight:700;text-align:left;padding:0;cursor:pointer;">Remove photo</button>` : ''}
            </div>
          </div>

          <form id="rxEditProfileForm" style="display:flex;flex-direction:column;gap:10px;">
            <div>
              <div class="rx-field-label">FULL NAME</div>
              <input type="text" name="name" value="${escapeHtml(eng?.full_name || eng?.name || '')}" required style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
              <div>
                <div class="rx-field-label">PHONE</div>
                <input type="tel" name="phone" value="${escapeHtml(eng?.phone || '')}" placeholder="+1 555-0100" style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
              </div>
              <div>
                <div class="rx-field-label">EMAIL</div>
                <input type="email" name="email" value="${escapeHtml(eng?.email || '')}" placeholder="user@example.com" style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
              </div>
            </div>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
              <div>
                <div class="rx-field-label">COMPANY JOIN DATE</div>
                <input type="date" name="join_date" value="${escapeHtml(eng?.joining_date || eng?.join_date || '')}" style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
              </div>
              <div>
                <div class="rx-field-label">BIRTHDAY</div>
                <input type="date" name="birthday" value="${escapeHtml(eng?.birthday || '')}" style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
              </div>
            </div>

            <div style="margin-top:4px;border-top:1px solid var(--nm-border);padding-top:10px;">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                <span class="rx-field-label" style="margin:0;color:var(--nm-accent);font-size:11px;font-weight:800;">SECURITY &amp; PASSWORD</span>
                <span style="font-size:10.5px;color:var(--nm-text-muted);">(Leave blank to keep current)</span>
              </div>
              <div style="display:flex;flex-direction:column;gap:8px;">
                <div style="position:relative;">
                  <input type="password" name="current_password" id="rxCurrentPwd" placeholder="Current password..." autocomplete="current-password" style="width:100%;padding:10px 36px 10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13px;" />
                  <button type="button" class="rx-pwd-toggle" data-rx-toggle="rxCurrentPwd" aria-label="Toggle password visibility" style="position:absolute;right:8px;top:50%;transform:translateY(-50%);background:transparent;border:0;color:var(--nm-text-muted);cursor:pointer;padding:4px;display:flex;align-items:center;"><i class="ph ph-eye"></i></button>
                </div>
                <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
                  <div style="position:relative;">
                    <input type="password" name="new_password" id="rxNewPwd" placeholder="New password (min 6)..." autocomplete="new-password" style="width:100%;padding:10px 36px 10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13px;" />
                    <button type="button" class="rx-pwd-toggle" data-rx-toggle="rxNewPwd" aria-label="Toggle password visibility" style="position:absolute;right:8px;top:50%;transform:translateY(-50%);background:transparent;border:0;color:var(--nm-text-muted);cursor:pointer;padding:4px;display:flex;align-items:center;"><i class="ph ph-eye"></i></button>
                  </div>
                  <div style="position:relative;">
                    <input type="password" name="confirm_password" id="rxConfirmPwd" placeholder="Confirm password..." autocomplete="new-password" style="width:100%;padding:10px 36px 10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13px;" />
                    <button type="button" class="rx-pwd-toggle" data-rx-toggle="rxConfirmPwd" aria-label="Toggle password visibility" style="position:absolute;right:8px;top:50%;transform:translateY(-50%);background:transparent;border:0;color:var(--nm-text-muted);cursor:pointer;padding:4px;display:flex;align-items:center;"><i class="ph ph-eye"></i></button>
                  </div>
                </div>
              </div>
            </div>

            <button type="submit" class="rx-btn-primary" style="width:100%;margin-top:6px;">Save Profile</button>
          </form>
        </div>
      `;

      dlg.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dlg));
      dlg.querySelector('#rxPhotoFileInput')?.addEventListener('change', (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = async () => {
          const img = new Image();
          img.onload = async () => {
            const canvas = document.createElement('canvas');
            const scale = Math.min(1, 256 / Math.max(img.width, img.height));
            canvas.width = Math.round(img.width * scale);
            canvas.height = Math.round(img.height * scale);
            canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
            const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
            const resp = await fetch('/api/users/profile-picture', {
              method: 'POST',
              headers: getAuthHeaders(),
              body: JSON.stringify({ username: eng.username, profile_picture: dataUrl })
            });
            const res = await resp.json();
            if (resp.ok && res.success) {
              pendingPic = dataUrl;
              this.toast('Profile photo updated!', 'success');
              await this.refreshAllData();
              render();
            } else {
              this.toast(res.message || 'Photo upload failed', 'error');
            }
          };
          img.src = reader.result;
        };
        reader.readAsDataURL(file);
      });

      dlg.querySelector('#btnPromptRemovePhoto')?.addEventListener('click', () => {
        const cDlg = document.getElementById('rxConfirmRemovePhotoDialog');
        cDlg.innerHTML = `
          <div class="rx-modal-scroll" style="text-align:center;">
            <h3 id="rxRemovePhotoTitle" style="margin:0;font-size:17px;font-weight:800;">Remove Profile Photo?</h3>
            <p style="margin:0;font-size:12.5px;color:var(--nm-text-muted);">Your avatar will revert to your initials monogram.</p>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:6px;">
              <button type="button" class="rx-btn-surface" data-rx-cancel>Cancel</button>
              <button type="button" class="rx-btn-primary" data-rx-confirm style="background:#DC2626;">Remove</button>
            </div>
          </div>
        `;
        cDlg.querySelector('[data-rx-cancel]')?.addEventListener('click', () => closeNeumorphicDialog(cDlg));
        cDlg.querySelector('[data-rx-confirm]')?.addEventListener('click', async () => {
          const resp = await fetch('/api/users/profile-picture', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ username: eng.username, profile_picture: '' })
          });
          const res = await resp.json();
          if (resp.ok && res.success) {
            pendingPic = '';
            closeNeumorphicDialog(cDlg);
            this.toast('Profile photo removed', 'success');
            await this.refreshAllData();
            render();
          } else {
            this.toast(res.message || 'Could not remove photo', 'error');
          }
        });
        openNeumorphicDialog(cDlg);
      });

      dlg.querySelectorAll('.rx-pwd-toggle').forEach((btn) => {
        btn.addEventListener('click', () => {
          const targetId = btn.getAttribute('data-rx-toggle');
          const input = dlg.querySelector(`#${targetId}`);
          if (!input) return;
          const isPwd = input.type === 'password';
          input.type = isPwd ? 'text' : 'password';
          const icon = btn.querySelector('i');
          if (icon) {
            icon.className = isPwd ? 'ph ph-eye-slash' : 'ph ph-eye';
          }
        });
      });

      dlg.querySelector('#rxEditProfileForm')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const f = e.target;

        const curPwd = f.current_password?.value?.trim() || '';
        const newPwd = f.new_password?.value?.trim() || '';
        const confirmPwd = f.confirm_password?.value?.trim() || '';

        if (newPwd || curPwd || confirmPwd) {
          if (!curPwd) {
            this.toast('Current password is required to change password', 'error');
            return;
          }
          if (newPwd.length < 6) {
            this.toast('New password must be at least 6 characters', 'error');
            return;
          }
          if (newPwd !== confirmPwd) {
            this.toast('New password and confirm password do not match', 'error');
            return;
          }
        }

        try {
          const payload = {
            username: eng.username,
            name: f.name.value.trim(),
            full_name: f.name.value.trim(),
            phone: f.phone.value.trim(),
            email: f.email.value.trim(),
            birthday: f.birthday.value.trim(),
            join_date: f.join_date?.value?.trim() || ''
          };
          if (newPwd) {
            payload.current_password = curPwd;
            payload.new_password = newPwd;
          }

          const resp = await fetch('/api/users/profile', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
          });
          const res = await resp.json();
          if (resp.ok && res.success) {
            this.toast('Profile & security updated successfully!', 'success');
            closeNeumorphicDialog(dlg);
            await this.refreshAllData();
          } else {
            this.toast(res.message || 'Update failed', 'error');
          }
        } catch (err) {
          this.toast(err.message, 'error');
        }
      });
    };

    render();
    openNeumorphicDialog(dlg);
  }

  // =========================================================================
  // NOTIFICATIONS BELL DRAWER (<dialog>)
  // =========================================================================
  openNotificationsDialog() {
    const dlg = document.getElementById('rxNotificationsDialog');
    const selfEng = this.getSelfEngineer();

    const formatNotifTime = (raw) => {
      if (!raw) return '';
      try {
        const d = new Date(raw);
        if (!isNaN(d.getTime())) {
          return d.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true,
            timeZone: 'Asia/Kolkata'
          }) + ' IST';
        }
      } catch (_) {}
      return String(raw).replace(' UTC', ' IST');
    };

    const highlightMentions = (text, engineers) => {
      if (!text) return '';
      let safe = escapeHtml(text);

      const candidates = new Set(['Everyone', 'everyone', 'all', 'team']);
      (engineers || []).forEach((eng) => {
        if (eng.full_name) candidates.add(eng.full_name.trim());
        if (eng.fullName) candidates.add(eng.fullName.trim());
        if (eng.name) candidates.add(eng.name.trim());
        if (eng.username) candidates.add(eng.username.trim());
      });

      // Dynamic extraction for colon/delimiter-separated shift update lines (e.g. @Shefeeq K K : or @Silambarasan R :)
      const knownFirstWords = new Set();
      candidates.forEach((c) => {
        const first = c.split(/\s+/)[0]?.trim();
        if (first && first.length >= 3) knownFirstWords.add(first.toLowerCase());
      });

      const colonMentionRegex = /@([A-Za-z0-9_.-]+(?:\s+[A-Za-z0-9_.-]+)+)(?=\s*[:\-\(\[\n,])/g;
      let cm;
      while ((cm = colonMentionRegex.exec(text)) !== null) {
        const fullToken = cm[1].trim();
        const first = fullToken.split(/\s+/)[0]?.trim()?.toLowerCase();
        if (first && knownFirstWords.has(first)) {
          candidates.add(fullToken);
        }
      }

      const sorted = Array.from(candidates)
        .filter(Boolean)
        .sort((a, b) => b.length - a.length);

      const escapeRegex = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

      const patternParts = sorted.map(c => escapeHtml(escapeRegex(c)) + '(?![a-zA-Z0-9_])');
      patternParts.push('[a-zA-Z0-9_.-]+');

      const mentionRegex = new RegExp(`(@(?:${patternParts.join('|')}))`, 'gi');
      safe = safe.replace(mentionRegex, '<strong style="color:var(--nm-accent);background:var(--nm-accent-soft);padding:1px 4px;border-radius:4px;">$1</strong>');
      return safe;
    };

    const render = () => {
      dlg.innerHTML = `
        <div class="rx-modal-scroll">
          <div class="rx-modal-header">
            <div class="rx-modal-title-wrap">
              <div class="rx-modal-badge-icon"><i class="ph-fill ph-bell-ringing"></i></div>
              <div>
                <h2 id="rxNotifDialogTitle" style="margin:0;font-size:17px;font-weight:800;">Notifications</h2>
                <div style="font-size:12px;color:var(--nm-text-muted);">${this.notifications.length} active alert${this.notifications.length === 1 ? '' : 's'}</div>
              </div>
            </div>
            <div style="display:flex;gap:6px;">
              ${this.notifications.length ? `<button type="button" class="rx-btn-surface" data-clear-all style="padding:6px 10px;font-size:11px;color:#DC2626;font-weight:800;">Clear All</button>` : ''}
              <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close"><i class="ph ph-x"></i></button>
            </div>
          </div>

          ${this.notifications.length === 0 ? `
            <div class="rx-well" style="text-align:center;padding:32px 20px;color:var(--nm-text-muted);font-weight:600;display:flex;flex-direction:column;align-items:center;gap:8px;">
              <i class="ph ph-bell-slash" style="font-size:32px;color:var(--nm-text-muted);opacity:0.6;"></i>
              <div>You're all caught up! No active notifications.</div>
            </div>
          ` : `
            <div class="rx-notif-list" style="display:flex;flex-direction:column;gap:12px;">
              ${this.notifications.map((n) => {
                const nid = n.id || n._id;
                const title = resolveMentions(n.title || 'Announcement', selfEng, this.engineers);
                const rawMsg = resolveMentions(n.message || n.raw_message || '', selfEng, this.engineers);
                const timeStr = formatNotifTime(n.created_at || n.timestamp);

                const isCelebrationNotif = Boolean(
                  n.celebration_type ||
                  (n.sender && /celebration/i.test(n.sender)) ||
                  (n.title && /🎂|🏆|birthday|anniversary/i.test(n.title)) ||
                  (n.category === 'celebration')
                );

                const isCredentialAlert = Boolean(
                  n.category === 'credential_expiry' ||
                  /credential/i.test(n.title || '') ||
                  /credential/i.test(rawMsg)
                );

                const isRosterAlert = Boolean(
                  (n.priority && /shift_update/i.test(n.priority)) ||
                  n.category === 'shift_update' ||
                  /roster|shift/i.test(n.title || '') ||
                  /roster|shift/i.test(rawMsg)
                );

                let badgeText = 'OPERATIONS';
                let badgeIcon = 'ph-info';
                let badgeBg = 'rgba(16,185,129,0.14)';
                let badgeColor = '#047857';

                if (isCelebrationNotif) {
                  badgeText = 'CELEBRATION 🎉';
                  badgeIcon = 'ph-cake';
                  badgeBg = 'rgba(245,158,11,0.14)';
                  badgeColor = '#B45309';
                } else if (isCredentialAlert) {
                  badgeText = 'SECURITY';
                  badgeIcon = 'ph-shield-check';
                  badgeBg = 'rgba(239,68,68,0.14)';
                  badgeColor = '#B91C1C';
                } else if (isRosterAlert) {
                  badgeText = 'SHIFT UPDATE';
                  badgeIcon = 'ph-calendar-check';
                  badgeBg = 'rgba(99,102,241,0.14)';
                  badgeColor = '#4338CA';
                } else if (String(n.priority || '').toLowerCase() === 'urgent') {
                  badgeText = 'URGENT';
                  badgeIcon = 'ph-warning';
                  badgeBg = 'rgba(239,68,68,0.14)';
                  badgeColor = '#B91C1C';
                }

                let authorStr = 'By Operations Desk';
                if (isCelebrationNotif) authorStr = 'By RosterX Celebrations Desk';
                else if (isCredentialAlert) authorStr = 'By Security Desk';
                else if (n.sender) authorStr = `By ${n.sender}`;

                return `
                  <div class="rx-notif-card rx-notif-item" data-notif-card="${escapeHtml(nid)}" style="transition:all 0.2s ease;">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
                      <span class="rx-notif-badge" style="background:${badgeBg};color:${badgeColor};">
                        <i class="ph ${badgeIcon}"></i> ${escapeHtml(badgeText)}
                      </span>
                      <button type="button" class="rx-icon-pill" data-dismiss-one="${escapeHtml(nid)}" aria-label="Dismiss notification" style="width:26px;height:26px;min-inline-size:26px;min-block-size:26px;">
                        <i class="ph ph-x"></i>
                      </button>
                    </div>

                    <strong style="font-size:13.5px;color:var(--nm-text);">${escapeHtml(title)}</strong>
                    <div style="font-size:12.5px;color:var(--nm-text-muted);white-space:pre-wrap;line-height:1.45;">${highlightMentions(rawMsg, this.engineers)}</div>

                    ${isCelebrationNotif ? `
                      <button type="button" class="rx-btn-surface" data-action-celeb="${escapeHtml(nid)}" style="padding:6px 11px;font-size:11.5px;color:#B45309;font-weight:800;display:inline-flex;align-items:center;gap:6px;width:fit-content;border-radius:10px;">
                        <i class="ph-fill ph-sparkle"></i> Tap to open wishes ✨
                      </button>
                    ` : isCredentialAlert ? `
                      <button type="button" class="rx-btn-surface" data-action-creds="${escapeHtml(nid)}" style="padding:6px 11px;font-size:11.5px;color:#B91C1C;font-weight:800;display:inline-flex;align-items:center;gap:6px;width:fit-content;border-radius:10px;">
                        <i class="ph-fill ph-shield-check"></i> Tap to view Credential Tracker →
                      </button>
                    ` : isRosterAlert ? `
                      <button type="button" class="rx-btn-surface" data-action-roster="${escapeHtml(nid)}" style="padding:6px 11px;font-size:11.5px;color:#4338CA;font-weight:800;display:inline-flex;align-items:center;gap:6px;width:fit-content;border-radius:10px;">
                        <i class="ph-fill ph-calendar"></i> Tap to view Month Roster →
                      </button>
                    ` : ''}

                    <div style="display:flex;justify-content:space-between;align-items:center;gap:6px;font-size:10.5px;color:var(--nm-text-muted);margin-top:2px;">
                      <span>${escapeHtml(authorStr)}</span>
                      ${timeStr ? `<span style="display:flex;align-items:center;gap:4px;"><i class="ph ph-clock"></i> ${escapeHtml(timeStr)}</span>` : ''}
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </div>
      `;

      dlg.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dlg));

      dlg.querySelector('[data-clear-all]')?.addEventListener('click', async () => {
        const items = dlg.querySelectorAll('.rx-notif-item');
        if (items.length > 0) {
          items.forEach((el, idx) => {
            setTimeout(() => el.classList.add('rx-notif-dismissing'), idx * 35);
          });
          setTimeout(async () => {
            this.notifications = [];
            this.updateNotificationBadge();
            render();
            await this.atlas.dismissNotification('', true);
          }, items.length * 35 + 260);
        } else {
          await this.atlas.dismissNotification('', true);
          this.notifications = [];
          this.updateNotificationBadge();
          render();
        }
      });

      dlg.querySelectorAll('[data-dismiss-one]').forEach((b) => {
        b.addEventListener('click', (e) => {
          e.stopPropagation();
          const nid = b.getAttribute('data-dismiss-one');
          const card = b.closest('.rx-notif-item');
          if (card) {
            card.classList.add('rx-notif-dismissing');
            setTimeout(async () => {
              this.notifications = this.notifications.filter((x) => String(x.id || x._id) !== nid);
              this.updateNotificationBadge();
              render();
              await this.atlas.dismissNotification(nid, false);
            }, 260);
          } else {
            this.notifications = this.notifications.filter((x) => String(x.id || x._id) !== nid);
            this.updateNotificationBadge();
            render();
            this.atlas.dismissNotification(nid, false);
          }
        });
      });

      // Interactive Action Triggers
      dlg.querySelectorAll('[data-action-celeb]').forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          const nid = btn.getAttribute('data-action-celeb');
          const notif = this.notifications.find((x) => String(x.id || x._id) === nid);
          closeNeumorphicDialog(dlg);
          if (notif) {
            const cUser = notif.celebration_user || notif.username || '';
            const engObj = this.engineers.find((eng) => String(eng.username || '').toLowerCase() === String(cUser).toLowerCase());
            const celebObj = {
              username: cUser,
              name: notif.celebration_name || engObj?.full_name || engObj?.name || cUser || 'Team Member',
              phone: notif.celebration_phone || engObj?.phone || '',
              isBday: Boolean(notif.celebration_type === 'birthday' || (notif.title && /birthday|🎂/i.test(notif.title))),
              annYears: Number(notif.celebration_years || 1),
              isSelf: String(cUser || '').toLowerCase() === String(this.user?.username || '').toLowerCase()
            };
            this.openCelebrationDialog(celebObj);
          }
        });
      });

      dlg.querySelectorAll('[data-action-creds]').forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          closeNeumorphicDialog(dlg);
          this.openSubView('credentials');
        });
      });

      dlg.querySelectorAll('[data-action-roster]').forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          closeNeumorphicDialog(dlg);
          this.switchTab('month_roster');
        });
      });
    };

    render();
    openNeumorphicDialog(dlg);
  }

  // =========================================================================
  // FLOATING AI ASSISTANT DIALOG (/api/chat/assistant)
  // =========================================================================
  openAiAssistantDialog() {
    let dlg = document.getElementById('rxAiChatDialog');
    if (!dlg) {
      dlg = document.createElement('dialog');
      dlg.id = 'rxAiChatDialog';
      dlg.className = 'rx-modal';
      dlg.setAttribute('closedby', 'any');
      document.body.appendChild(dlg);
    }

    const QUICK_PROMPTS = [
      '⚡ On duty now?',
      '🌙 NS tomorrow?',
      '📅 My shift today?',
      '📋 Duties tomorrow?',
      '🏖️ Off tomorrow?',
      '🌙 Next night shift?',
      '🏖️ Rest days count?'
    ];

    const formatAiMarkdown = (raw) => {
      if (!raw) return '';
      let str = escapeHtml(String(raw));
      str = str.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
      str = str.replace(/`([^`]+)`/g, '<code style="background:var(--nm-input-surface);padding:1px 5px;border-radius:4px;font-size:11px;font-family:monospace;">$1</code>');
      const shiftColors = {
        FS: '#059669', FG: '#047857', G: '#0284C7', SG: '#EA580C', SS: '#D97706', NS: '#4F46E5', OFF: '#94A3B8', L: '#DC2626'
      };
      Object.entries(shiftColors).forEach(([code, color]) => {
        const re = new RegExp(`\\b(${code})\\b`, 'g');
        str = str.replace(re, `<span class="rx-chat-badge" style="background:${color}22;color:${color};border:1px solid ${color}44;">$1</span>`);
      });
      str = str.replace(/\n/g, '<br>');
      return str;
    };

    const sendAiPrompt = async (prompt) => {
      const clean = String(prompt || '').trim();
      if (!clean || this.aiLoading) return;
      this.aiMessages.push({ role: 'user', text: clean });
      this.aiLoading = true;
      render();

      try {
        const recentHistory = (this.aiMessages || []).slice(-8).map((m) => ({
          role: m.role === 'user' ? 'user' : 'assistant',
          text: m.text
        }));
        const targetMonth = `${this.gridYear}-${String(this.gridMonth).padStart(2, '0')}`;

        const resp = await fetch('/api/chat/assistant', {
          method: 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify({
            prompt: clean,
            message: clean,
            history: recentHistory,
            target_month: targetMonth,
            current_user: this.user,
            platform: 'iOS/Web PWA'
          })
        });
        const data = await resp.json();
        if (resp.ok && data.success) {
          this.aiMessages.push({ role: 'ai', text: data.reply || data.response || data.message || 'No response.' });
        } else {
          const errMsg = data.error || data.message || 'Server temporarily unable to reach Gemini API. Please ensure active API keys are configured in Admin Settings.';
          this.aiMessages.push({ role: 'ai', text: `⚠️ ${errMsg}` });
        }
      } catch (err) {
        this.aiMessages.push({ role: 'ai', text: `Error: ${err.message}` });
      } finally {
        this.aiLoading = false;
        render();
      }
    };

    const render = () => {
      dlg.innerHTML = `
        <div class="rx-modal-scroll">
          <div class="rx-sheet-drag-handle"></div>

          <div class="rx-modal-header" style="padding-bottom:2px;">
            <div class="rx-modal-title-wrap">
              <div class="rx-modal-badge-icon" style="box-shadow:0 0 16px var(--nm-accent-glow);"><i class="ph ph-robot"></i></div>
              <div>
                <h2 style="margin:0;font-size:16px;font-weight:900;">RosterX Assistant</h2>
                <div style="font-size:11px;color:var(--nm-text-muted);display:flex;align-items:center;gap:6px;margin-top:2px;">
                  <span class="rx-pulsing-dot"></span>
                  <span>Grounded Gemini • Live Atlas NOC</span>
                </div>
              </div>
            </div>
            <div style="display:flex;gap:6px;">
              ${this.aiMessages.length ? `
                <button type="button" class="rx-icon-pill" id="btnClearAiChat" aria-label="Clear chat" title="Clear chat history">
                  <i class="ph ph-trash"></i>
                </button>
              ` : ''}
              <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close">
                <i class="ph ph-x"></i>
              </button>
            </div>
          </div>

          <!-- Quick Action Chips Strip -->
          <div class="rx-ai-chips">
            ${QUICK_PROMPTS.map((p) => `
              <button type="button" class="rx-ai-chip" data-quick-prompt="${escapeHtml(p)}" ${this.aiLoading ? 'disabled' : ''}>
                ${escapeHtml(p)}
              </button>
            `).join('')}
          </div>

          <!-- Message History (Expands to Full Available Space) -->
          <div class="rx-ai-messages" id="rxAiMessageContainer">
            ${this.aiMessages.length === 0 ? `
              <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;text-align:center;padding:24px 16px;color:var(--nm-text-muted);gap:10px;">
                <div style="width:48px;height:48px;border-radius:16px;background:var(--nm-surface);box-shadow:var(--nm-shadow-raised-subtle);display:flex;align-items:center;justify-content:center;color:var(--nm-accent);font-size:24px;">
                  <i class="ph ph-sparkle"></i>
                </div>
                <div style="font-size:13px;font-weight:700;color:var(--nm-text);">How can I help with your schedule today?</div>
                <div style="font-size:11.5px;max-width:320px;line-height:1.45;">
                  Ask about active on-duty engineers, your next night shift, weekend coverage, or tap a quick chip above!
                </div>
              </div>
            ` : this.aiMessages.map((m) => {
              if (m.role === 'user') {
                return `
                  <div class="rx-ai-bubble-user">
                    ${escapeHtml(m.text)}
                  </div>
                `;
              }
              return `
                <div class="rx-ai-bubble-ai">
                  <div style="width:28px;height:28px;border-radius:10px;background:var(--nm-surface);box-shadow:var(--nm-shadow-raised-subtle);display:flex;align-items:center;justify-content:center;color:var(--nm-accent);font-size:15px;flex-shrink:0;margin-top:2px;">
                    <i class="ph ph-robot"></i>
                  </div>
                  <div class="rx-ai-bubble-ai-content">
                    ${formatAiMarkdown(m.text)}
                  </div>
                </div>
              `;
            }).join('')}

            ${this.aiLoading ? `
              <div class="rx-ai-bubble-ai">
                <div style="width:28px;height:28px;border-radius:10px;background:var(--nm-surface);box-shadow:var(--nm-shadow-raised-subtle);display:flex;align-items:center;justify-content:center;color:var(--nm-accent);font-size:15px;flex-shrink:0;">
                  <i class="ph ph-robot"></i>
                </div>
                <div class="rx-typing-indicator">
                  <span class="rx-typing-dot"></span>
                  <span class="rx-typing-dot"></span>
                  <span class="rx-typing-dot"></span>
                </div>
              </div>
            ` : ''}
          </div>

          <!-- Input Basin -->
          <form id="rxAiForm" style="display:flex;gap:8px;align-items:center;margin-top:2px;">
            <input 
              type="text" 
              name="prompt" 
              placeholder="Ask NOC schedules or active shifts..." 
              required 
              autocomplete="off"
              ${this.aiLoading ? 'disabled' : ''}
              style="flex:1;padding:12px 14px;border-radius:14px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13px;box-shadow:var(--nm-shadow-inset-subtle);outline:none;" 
            />
            <button type="submit" class="rx-btn-primary" style="padding:12px 18px;border-radius:14px;" ${this.aiLoading ? 'disabled' : ''} aria-label="Send query">
              <i class="ph ph-paper-plane-right" style="font-size:16px;"></i>
            </button>
          </form>
        </div>
      `;

      // Auto-scroll messages to bottom
      const msgBox = dlg.querySelector('#rxAiMessageContainer');
      if (msgBox) {
        msgBox.scrollTop = msgBox.scrollHeight;
      }

      const closeAiDialog = () => {
        dlg.classList.add('is-closing');
        dlg.classList.remove('is-visible');
        setTimeout(() => {
          dlg.classList.remove('is-closing');
          closeNeumorphicDialog(dlg);
        }, 190);
      };

      dlg.querySelector('[data-rx-close]')?.addEventListener('click', closeAiDialog);

      dlg.querySelector('#btnClearAiChat')?.addEventListener('click', () => {
        this.aiMessages = [];
        render();
      });

      dlg.querySelectorAll('[data-quick-prompt]').forEach((btn) => {
        btn.addEventListener('click', () => {
          const prompt = btn.getAttribute('data-quick-prompt');
          sendAiPrompt(prompt);
        });
      });

      dlg.querySelector('#rxAiForm')?.addEventListener('submit', (e) => {
        e.preventDefault();
        const input = e.target.querySelector('input[name="prompt"]');
        const val = input ? input.value : '';
        if (input) input.value = '';
        sendAiPrompt(val);
      });
    };

    render();
    openNeumorphicDialog(dlg);
    requestAnimationFrame(() => {
      dlg.classList.add('is-visible');
    });
    dlg.addEventListener('close', () => {
      dlg.classList.remove('is-visible', 'is-closing');
    }, { once: true });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.rosterXPortal = new RosterXUserPortal();
});
