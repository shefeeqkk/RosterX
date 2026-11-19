/**
 * RosterX — Personal "Sync to Calendar" (.ics) Modal (calendarSync.js)
 *
 * 1:1 parity with Android ShiftCalendarExporter.kt + CalendarSyncDialog.kt:
 * - Strictly personal scope (only logged-in user's own shifts)
 * - Working shifts (FS/FG/G/SG/SS/NS): Timed IST (Asia/Kolkata), Busy (OPAQUE), configurable VALARM
 * - Non-working days (OFF/L/SW*): All-Day VALUE=DATE (no clock time), Free (TRANSPARENT), no VALARM
 * - Opens natively in Apple Calendar "Add All Events" sheet on iOS Safari!
 */

import { openNeumorphicDialog, closeNeumorphicDialog, escapeHtml, getAuthHeaders } from './pwaHelper.js';

const REMINDER_KEY = 'rosterx_calendar_reminder_minutes';

export const SHIFT_SPECS = {
  FS: { title: 'First Shift (FS)',    startH: 7,  startM: 30, endH: 16, endM: 30, nextDay: false },
  FG: { title: 'First General (FG)',  startH: 8,  startM: 30, endH: 17, endM: 30, nextDay: false },
  G:  { title: 'General Shift (G)',   startH: 9,  startM: 30, endH: 18, endM: 30, nextDay: false },
  SG: { title: 'Second General (SG)', startH: 11, startM: 0,  endH: 20, endM: 0,  nextDay: false },
  SS: { title: 'Second Shift (SS)',   startH: 13, startM: 0,  endH: 22, endM: 0,  nextDay: false },
  NS: { title: 'Night Shift (NS)',    startH: 21, startM: 0,  endH: 7,  endM: 30, nextDay: true }
};

const REMINDER_CHOICES = [
  { label: 'None', minutes: null },
  { label: '15m',  minutes: 15 },
  { label: '30m',  minutes: 30 },
  { label: '1h',   minutes: 60 }
];

function pad2(n) {
  return String(n).padStart(2, '0');
}

function escapeIcsText(raw) {
  return String(raw || '')
    .replace(/\\/g, '\\\\')
    .replace(/;/g, '\\;')
    .replace(/,/g, '\\,')
    .replace(/\r\n/g, '\\n')
    .replace(/\n/g, '\\n');
}

function getSavedReminder() {
  try {
    const val = localStorage.getItem(REMINDER_KEY);
    if (val === null) return 30;
    const n = Number(val);
    return Number.isFinite(n) && n > 0 ? n : null;
  } catch (_) {
    return 30;
  }
}

function saveReminder(minutes) {
  try {
    localStorage.setItem(REMINDER_KEY, minutes == null ? '-1' : String(minutes));
  } catch (_) {}
}

export function computeSyncPreview(dayShiftCodes = {}) {
  let working = 0;
  let off = 0;
  let leave = 0;
  let swap = 0;
  Object.values(dayShiftCodes).forEach((raw) => {
    const code = String(raw || '').trim().toUpperCase();
    if (SHIFT_SPECS[code]) working++;
    else if (code === 'OFF') off++;
    else if (code === 'L' || code === 'LEAVE') leave++;
    else if (code.startsWith('SW')) swap++;
  });
  const nonWorking = off + leave + swap;
  return { working, off, leave, swap, nonWorking, total: working + nonWorking };
}

export function buildPersonalIcsContent({
  engineerName,
  username,
  year,
  month,
  dayShiftCodes,
  reminderMinutes = 30
}) {
  const cleanUser = String(username || 'member').trim().toLowerCase().replace(/[^a-z0-9._-]/g, '_') || 'member';
  const safeName = String(engineerName || 'Team Member').trim() || 'Team Member';
  const now = new Date();
  const dtStamp = `${now.getUTCFullYear()}${pad2(now.getUTCMonth() + 1)}${pad2(now.getUTCDate())}T${pad2(now.getUTCHours())}${pad2(now.getUTCMinutes())}${pad2(now.getUTCSeconds())}Z`;

  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//RosterX//Personal Shift Calendar//EN',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH',
    `X-WR-CALNAME:RosterX - ${escapeIcsText(safeName)} (${year}-${pad2(month)})`,
    'X-WR-TIMEZONE:Asia/Kolkata',
    'BEGIN:VTIMEZONE',
    'TZID:Asia/Kolkata',
    'BEGIN:STANDARD',
    'DTSTART:19700101T000000',
    'TZOFFSETFROM:+0530',
    'TZOFFSETTO:+0530',
    'TZNAME:IST',
    'END:STANDARD',
    'END:VTIMEZONE'
  ];

  let exportedCount = 0;
  const days = Object.keys(dayShiftCodes)
    .map((d) => Number(d))
    .filter((d) => Number.isInteger(d) && d >= 1 && d <= 31)
    .sort((a, b) => a - b);

  days.forEach((day) => {
    const code = String(dayShiftCodes[day] ?? dayShiftCodes[String(day)] ?? '').trim().toUpperCase();
    if (!code) return;

    const dateObj = new Date(Date.UTC(year, month - 1, day));
    if (dateObj.getUTCMonth() !== month - 1) return;

    const nextDateObj = new Date(Date.UTC(year, month - 1, day + 1));
    const ymd = `${year}${pad2(month)}${pad2(day)}`;
    const nextYmd = `${nextDateObj.getUTCFullYear()}${pad2(nextDateObj.getUTCMonth() + 1)}${pad2(nextDateObj.getUTCDate())}`;
    const uid = `rosterx-${year}-${pad2(month)}-${pad2(day)}-${cleanUser}@rosterx.online`;
    const syncTag = `[RosterX-Sync:${year}-${pad2(month)}:${cleanUser}:${pad2(day)}]`;

    const spec = SHIFT_SPECS[code];
    if (spec) {
      const endYmd = spec.nextDay ? nextYmd : ymd;
      const dtStart = `${ymd}T${pad2(spec.startH)}${pad2(spec.startM)}00`;
      const dtEnd = `${endYmd}T${pad2(spec.endH)}${pad2(spec.endM)}00`;
      const summary = `RosterX: ${spec.title}`;
      const desc = `Scheduled ${spec.title} duty for ${safeName} (Shift Code: ${code}).\\n${syncTag}`;

      lines.push(
        'BEGIN:VEVENT',
        `UID:${uid}`,
        `DTSTAMP:${dtStamp}`,
        `DTSTART;TZID=Asia/Kolkata:${dtStart}`,
        `DTEND;TZID=Asia/Kolkata:${dtEnd}`,
        'TRANSP:OPAQUE',
        `SUMMARY:${escapeIcsText(summary)}`,
        `DESCRIPTION:${escapeIcsText(desc)}`,
        'STATUS:CONFIRMED'
      );
      if (reminderMinutes && reminderMinutes > 0) {
        lines.push(
          'BEGIN:VALARM',
          'ACTION:DISPLAY',
          `DESCRIPTION:Upcoming ${escapeIcsText(summary)} in ${reminderMinutes} minutes`,
          `TRIGGER:-PT${reminderMinutes}M`,
          'END:VALARM'
        );
      }
      lines.push('END:VEVENT');
      exportedCount++;
    } else if (code === 'OFF' || code === 'L' || code === 'LEAVE' || code.startsWith('SW')) {
      const title = code === 'OFF'
        ? 'RosterX: Weekly Off (OFF)'
        : (code === 'L' || code === 'LEAVE')
          ? 'RosterX: Approved Leave (L)'
          : `RosterX: Swap Off (${code})`;
      const desc = `${title.replace('RosterX: ', '')} for ${safeName}.\\n${syncTag}`;
      lines.push(
        'BEGIN:VEVENT',
        `UID:${uid}`,
        `DTSTAMP:${dtStamp}`,
        `DTSTART;VALUE=DATE:${ymd}`,
        `DTEND;VALUE=DATE:${nextYmd}`,
        'TRANSP:TRANSPARENT',
        `SUMMARY:${escapeIcsText(title)}`,
        `DESCRIPTION:${escapeIcsText(desc)}`,
        'STATUS:CONFIRMED',
        'END:VEVENT'
      );
      exportedCount++;
    }
  });

  lines.push('END:VCALENDAR', '');
  return { content: lines.join('\r\n'), count: exportedCount, fileName: `rosterx_${cleanUser}_${year}_${pad2(month)}.ics` };
}

export function triggerIcsDownload({ content, fileName }) {
  const blob = new Blob([content], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.rel = 'noopener';
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    a.remove();
    URL.revokeObjectURL(url);
  }, 1500);
}

/**
 * Opens the authentic frosted-blur Neumorphic "Sync to Calendar" <dialog> modal.
 * 1:1 Parity with Android CalendarSyncDialog.kt:
 * - Reflects target Google Calendar / Apple Calendar sync status from server
 * - Allows Re-Sync, Share/Export .ics, and Clear Month actions
 * - Persists sync status to MongoDB Atlas via POST /api/users/calendar-sync
 */
export function openCalendarSyncModal({ engineerName, username, year, month, dayShiftCodes, onToast }) {
  const preview = computeSyncPreview(dayShiftCodes);
  const monthKey = `${year}-${pad2(month)}`;
  const monthLabel = new Date(year, month - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  let selectedReminder = getSavedReminder();

  let syncState = null;
  let isLoadingSync = true;
  let isSyncing = false;
  let isClearing = false;

  let dialog = document.getElementById('rxCalendarSyncDialog');
  if (!dialog) {
    dialog = document.createElement('dialog');
    dialog.id = 'rxCalendarSyncDialog';
    dialog.className = 'rx-modal';
    dialog.setAttribute('closedby', 'any');
    dialog.setAttribute('aria-labelledby', 'rxCalSyncTitle');
    document.body.appendChild(dialog);
  }

  // Fetch initial sync status from server
  fetch(`/api/users/calendar-sync?month=${encodeURIComponent(monthKey)}`, {
    headers: getAuthHeaders()
  })
    .then((r) => r.json())
    .then((res) => {
      isLoadingSync = false;
      if (res && res.success && res.sync && res.synced) {
        syncState = res.sync;
      }
      render();
    })
    .catch(() => {
      isLoadingSync = false;
      render();
    });

  const recordSyncOnServer = async (count, target) => {
    try {
      const res = await fetch('/api/users/calendar-sync', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          month: monthKey,
          count: count,
          target: target,
          synced: true
        })
      });
      const data = await res.json();
      if (data && data.success && data.data) {
        syncState = data.data;
      } else {
        syncState = { synced: true, count, target, synced_at: 'Just now' };
      }
    } catch (_) {
      syncState = { synced: true, count, target, synced_at: 'Just now' };
    }
  };

  const clearSyncOnServer = async () => {
    try {
      const res = await fetch('/api/users/calendar-sync/clear', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ month: monthKey })
      });
      return await res.json();
    } catch (e) {
      return { success: false, message: e.message };
    }
  };

  const handleExport = async (isResync = false) => {
    if (preview.total === 0 || isSyncing || isClearing) return;
    isSyncing = true;
    render();

    try {
      const ics = buildPersonalIcsContent({
        engineerName,
        username,
        year,
        month,
        dayShiftCodes,
        reminderMinutes: selectedReminder
      });

      if (ics.count > 0) {
        triggerIcsDownload(ics);
        const targetLabel = 'Apple / Google Calendar (.ics)';
        await recordSyncOnServer(ics.count, targetLabel);
        const msg = isResync
          ? `Re-synced ${ics.count} personal calendar days for ${monthLabel}`
          : `Synced ${ics.count} personal calendar days for ${monthLabel}`;
        onToast?.(msg, 'success');
      } else {
        onToast?.('No shifts available to export for this month.', 'info');
      }
    } catch (err) {
      onToast?.(err.message || 'Failed to sync calendar', 'error');
    } finally {
      isSyncing = false;
      render();
    }
  };

  const handleClear = async () => {
    if (isSyncing || isClearing) return;
    if (!window.confirm(`Clear calendar sync status for ${monthLabel}?`)) return;

    isClearing = true;
    render();

    try {
      const res = await clearSyncOnServer();
      if (res && res.success) {
        syncState = null;
        onToast?.(`Cleared calendar sync for ${monthLabel}`, 'success');
      } else {
        onToast?.(res?.message || 'Failed to clear calendar sync', 'error');
      }
    } catch (err) {
      onToast?.(err.message || 'Failed to clear calendar sync', 'error');
    } finally {
      isClearing = false;
      render();
    }
  };

  const render = () => {
    const isSynced = Boolean(syncState && syncState.synced);
    const targetName = syncState?.target || 'Google Calendar';
    const targetSyncedAt = syncState?.synced_at || 'Active';
    const syncedCount = syncState?.count || preview.total;

    dialog.innerHTML = `
      <div class="rx-modal-scroll">
        <div class="rx-modal-header">
          <div class="rx-modal-title-wrap">
            <div class="rx-modal-badge-icon"><i class="ph ph-calendar-check" aria-hidden="true"></i></div>
            <div>
              <h2 id="rxCalSyncTitle" style="margin:0;font-size:17px;font-weight:800;">Sync to Calendar</h2>
              <div style="font-size:12px;font-weight:600;color:var(--nm-text-muted);">${escapeHtml(monthLabel)}</div>
            </div>
          </div>
          <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close" ${isSyncing || isClearing ? 'disabled' : ''}>
            <i class="ph ph-x" aria-hidden="true"></i>
          </button>
        </div>

        <!-- Target Calendar Status Card (1:1 Android Parity) -->
        ${isSynced ? `
          <div class="rx-card" style="display:flex;flex-direction:column;gap:8px;padding:12px 14px;background:var(--nm-surface);">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <span class="rx-field-label" style="margin:0;color:var(--nm-accent);font-size:10.5px;letter-spacing:0.04em;">TARGET CALENDAR</span>
              <span style="font-size:10.5px;font-weight:800;padding:2px 8px;border-radius:8px;background:rgba(16,185,129,0.14);color:#059669;display:flex;align-items:center;gap:4px;">
                <i class="ph-fill ph-check-circle" style="font-size:12px;"></i> Synced (${syncedCount})
              </span>
            </div>
            <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
              <div style="display:flex;align-items:center;gap:8px;min-inline-size:0;">
                <span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:#059669;flex-shrink:0;"></span>
                <strong style="font-size:13px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${escapeHtml(targetName)}</strong>
              </div>
              <span style="font-size:11px;color:var(--nm-text-muted);">${escapeHtml(targetSyncedAt)}</span>
            </div>
          </div>
        ` : ''}

        <div class="rx-well" style="display:flex;flex-direction:column;gap:10px;">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
            <div style="min-inline-size:0;">
              <div style="font-size:14px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(engineerName || 'Team Member')}</div>
              <div style="font-size:11px;color:var(--nm-text-muted);">@${escapeHtml(username || 'member')} • Personal schedule only</div>
            </div>
            <span style="padding:4px 9px;border-radius:10px;background:var(--nm-accent-soft);color:var(--nm-accent);font-size:11px;font-weight:800;white-space:nowrap;">${preview.total} days</span>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <div style="text-align:center;padding:6px 8px;border-radius:10px;background:var(--nm-accent-soft);color:var(--nm-accent);font-size:10.5px;font-weight:800;">
              ${preview.working} Shifts • Timed
            </div>
            <div style="text-align:center;padding:6px 8px;border-radius:10px;background:rgba(16,185,129,0.13);color:#059669;font-size:10.5px;font-weight:800;">
              ${preview.nonWorking} Off/Leave • All-Day
            </div>
          </div>
          <div style="font-size:11px;color:var(--nm-text-muted);line-height:1.4;">
            Only your personal shifts are included. On iPhone Safari, tapping Sync opens Apple Calendar's native <strong>Add All</strong> prompt.
          </div>
        </div>

        <div>
          <div class="rx-field-label" style="display:flex;align-items:center;gap:6px;">
            <i class="ph ph-bell-ringing" style="color:var(--nm-accent);" aria-hidden="true"></i> SHIFT REMINDER
          </div>
          <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;">
            ${REMINDER_CHOICES.map((c) => {
              const active = selectedReminder === c.minutes;
              return `
                <button type="button" class="rx-chip-btn ${active ? 'is-active' : ''}" data-rx-rem="${c.minutes === null ? 'none' : c.minutes}" aria-pressed="${active}" ${isSyncing || isClearing ? 'disabled' : ''}>
                  ${c.label}
                </button>
              `;
            }).join('')}
          </div>
          <div style="font-size:10.5px;color:var(--nm-text-muted);margin-top:6px;line-height:1.35;">
            Applies to working shifts (Busy). Off, Leave &amp; Swap Off sync silently as All-Day (Free).
          </div>
        </div>

        ${isSynced ? `
          <button type="button" class="rx-btn-primary" style="width:100%;" data-rx-resync ${preview.total === 0 || isSyncing || isClearing ? 'disabled' : ''}>
            <i class="ph ph-arrows-clockwise ${isSyncing ? 'ph-spin' : ''}" aria-hidden="true"></i>
            <span>${isSyncing ? 'Re-Syncing...' : `Re-Sync ${preview.total} Days`}</span>
          </button>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
            <button type="button" class="rx-btn-surface" data-rx-export-ics style="font-weight:700;" ${isSyncing || isClearing ? 'disabled' : ''}>
              <i class="ph ph-share-network"></i> Share / .ics
            </button>
            <button type="button" class="rx-btn-surface" data-rx-clear-month style="color:#DC2626;font-weight:800;" ${isSyncing || isClearing ? 'disabled' : ''}>
              <i class="ph ph-trash"></i> ${isClearing ? 'Clearing...' : 'Clear Month'}
            </button>
          </div>
        ` : `
          <button type="button" class="rx-btn-primary" style="width:100%;" data-rx-export ${preview.total === 0 || isSyncing || isClearing ? 'disabled' : ''}>
            <i class="ph ph-calendar-plus" aria-hidden="true"></i>
            <span>${isSyncing ? 'Syncing...' : `Add ${preview.total} Days to Calendar (.ics)`}</span>
          </button>
        `}
      </div>
    `;

    dialog.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dialog));
    dialog.querySelectorAll('[data-rx-rem]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const raw = btn.getAttribute('data-rx-rem');
        selectedReminder = raw === 'none' ? null : Number(raw);
        saveReminder(selectedReminder);
        render();
      });
    });

    dialog.querySelector('[data-rx-export]')?.addEventListener('click', () => handleExport(false));
    dialog.querySelector('[data-rx-resync]')?.addEventListener('click', () => handleExport(true));
    dialog.querySelector('[data-rx-export-ics]')?.addEventListener('click', () => {
      const ics = buildPersonalIcsContent({
        engineerName,
        username,
        year,
        month,
        dayShiftCodes,
        reminderMinutes: selectedReminder
      });
      if (ics.count > 0) {
        triggerIcsDownload(ics);
        onToast?.(`Downloaded .ics for ${monthLabel}`, 'success');
      }
    });
    dialog.querySelector('[data-rx-clear-month]')?.addEventListener('click', handleClear);
  };

  render();
  openNeumorphicDialog(dialog);
}
