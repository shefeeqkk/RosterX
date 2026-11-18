/**
 * RosterX — Reusable Server Credential Tracker Module (credentialTracker.js)
 *
 * 1:1 parity with Android CredentialTrackerScreen.kt:
 * - Usable inside BOTH User/Manager Portal (More Hub) AND Admin Console (More Hub)
 * - KPI Summary Cards (Total, Expiring Soon, Expired)
 * - Live Search + Filter Chips (All, Expiring Soon, Expired, Healthy)
 * - Server-grouped account cards with 1-tap Renew Date picker (past/today guard)
 * - Add / Edit / Delete Tracker + Audit Logs modal for Admin & Manager roles
 */

import { escapeHtml, getAuthHeaders, openNeumorphicDialog, closeNeumorphicDialog } from './pwaHelper.js';

function formatCredDate(iso) {
  if (!iso) return '—';
  const parts = String(iso).trim().split('-');
  if (parts.length !== 3) return iso;
  const dt = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
  if (Number.isNaN(dt.getTime())) return iso;
  return dt.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }).replace(/ /g, '-');
}

function getTodayIsoIst() {
  const now = new Date();
  const ist = new Date(now.getTime() + (330 + now.getTimezoneOffset()) * 60000);
  const y = ist.getFullYear();
  const m = String(ist.getMonth() + 1).padStart(2, '0');
  const d = String(ist.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function statusPill(tracker) {
  const st = String(tracker.status || '').toUpperCase();
  const days = Number(tracker.days_remaining);
  if (st === 'EXPIRED' || days < 0) {
    const overdue = Math.abs(Number.isFinite(days) ? days : 0);
    return { label: `Expired (${overdue}d ago)`, bg: 'rgba(220,38,38,0.13)', fg: '#DC2626', dot: '#DC2626' };
  }
  if (st === 'EXPIRES_TODAY' || days === 0) {
    return { label: 'Expires Today', bg: 'rgba(217,119,6,0.16)', fg: '#D97706', dot: '#D97706' };
  }
  if (st === 'EXPIRING_SOON') {
    return { label: `${days}d remaining`, bg: 'rgba(217,119,6,0.14)', fg: '#D97706', dot: '#D97706' };
  }
  return { label: `${Number.isFinite(days) ? days : '—'}d remaining`, bg: 'rgba(5,150,105,0.13)', fg: '#059669', dot: '#059669' };
}

export class CredentialTrackerController {
  constructor({ container, currentUser, onBack, onSummaryUpdate, onToast }) {
    this.container = container;
    this.currentUser = currentUser || {};
    this.onBack = onBack;
    this.onSummaryUpdate = onSummaryUpdate;
    this.onToast = onToast || ((m) => console.log(m));
    this.trackers = [];
    this.loading = false;
    this.searchQuery = '';
    this.selectedFilter = 'ALL';
  }

  get isManagerOrAdmin() {
    const r = String(this.currentUser?.role || '').toLowerCase();
    return r === 'admin' || r === 'manager';
  }

  canRenew(tracker) {
    if (this.isManagerOrAdmin) return true;
    const myEmail = String(this.currentUser?.email || '').trim().toLowerCase();
    const myUser = String(this.currentUser?.username || '').trim().toLowerCase();
    const emails = Array.isArray(tracker.spokesperson_emails) ? tracker.spokesperson_emails : [];
    return emails.some((e) => {
      const clean = String(e || '').trim().toLowerCase();
      return (myEmail && clean === myEmail) || (myUser && clean.includes(myUser));
    });
  }

  async load() {
    this.loading = true;
    this.render();
    try {
      const resp = await fetch('/api/credentials', { headers: getAuthHeaders() });
      const data = await resp.json();
      if (resp.ok && data.success) {
        this.trackers = data.trackers || data.data || [];
        this.notifySummary();
      } else {
        this.onToast(data.message || 'Could not load credential trackers', 'error');
      }
    } catch (err) {
      this.onToast(`Credential Tracker error: ${err.message}`, 'error');
    } finally {
      this.loading = false;
      this.render();
    }
  }

  notifySummary() {
    const total = this.trackers.length;
    const expired = this.trackers.filter((t) => String(t.status).toUpperCase() === 'EXPIRED').length;
    const expiringSoon = this.trackers.filter((t) => ['EXPIRING_SOON', 'EXPIRES_TODAY'].includes(String(t.status).toUpperCase())).length;
    this.onSummaryUpdate?.({ total, expired, expiringSoon });
  }

  getFiltered() {
    const q = this.searchQuery.trim().toLowerCase();
    return this.trackers.filter((t) => {
      const emails = Array.isArray(t.spokesperson_emails) ? t.spokesperson_emails : [];
      const matchesQ = !q ||
        String(t.server_name || '').toLowerCase().includes(q) ||
        String(t.username || '').toLowerCase().includes(q) ||
        emails.some((e) => String(e).toLowerCase().includes(q));
      const st = String(t.status || '').toUpperCase();
      const matchesF =
        this.selectedFilter === 'ALL' ||
        (this.selectedFilter === 'EXPIRING' && (st === 'EXPIRING_SOON' || st === 'EXPIRES_TODAY')) ||
        (this.selectedFilter === 'EXPIRED' && st === 'EXPIRED') ||
        (this.selectedFilter === 'HEALTHY' && st === 'HEALTHY');
      return matchesQ && matchesF;
    });
  }

  render() {
    if (!this.container) return;
    const total = this.trackers.length;
    const expired = this.trackers.filter((t) => String(t.status).toUpperCase() === 'EXPIRED').length;
    const expiring = this.trackers.filter((t) => ['EXPIRING_SOON', 'EXPIRES_TODAY'].includes(String(t.status).toUpperCase())).length;
    const healthy = this.trackers.filter((t) => String(t.status).toUpperCase() === 'HEALTHY').length;

    const filtered = this.getFiltered();
    const grouped = new Map();
    filtered.forEach((t) => {
      const srv = t.server_name || 'Server';
      if (!grouped.has(srv)) grouped.set(srv, []);
      grouped.get(srv).push(t);
    });

    this.container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
          <div style="display:flex;align-items:center;gap:10px;">
            ${this.onBack ? `
              <button type="button" class="rx-icon-pill" data-cred-back aria-label="Back to Operations Hub">
                <i class="ph ph-arrow-left" aria-hidden="true"></i>
              </button>
            ` : ''}
            <div>
              <h2 style="margin:0;font-size:19px;font-weight:800;color:var(--nm-accent);">Credential Tracker</h2>
              <div style="font-size:11.5px;color:var(--nm-text-muted);">Automated 9:00 AM IST expiry monitoring</div>
            </div>
          </div>
          <div style="display:flex;align-items:center;gap:8px;">
            <button type="button" class="rx-icon-pill" data-cred-refresh aria-label="Refresh Trackers">
              <i class="ph ph-arrows-clockwise" aria-hidden="true"></i>
            </button>
            ${this.isManagerOrAdmin ? `
              <button type="button" class="rx-icon-pill" data-cred-logs aria-label="Audit Logs" style="color:#6366F1;">
                <i class="ph ph-clock-counter-clockwise" aria-hidden="true"></i>
              </button>
              <button type="button" class="rx-btn-primary" data-cred-add style="padding:8px 13px;border-radius:12px;font-size:12px;">
                <i class="ph ph-plus" aria-hidden="true"></i> Add
              </button>
            ` : ''}
          </div>
        </div>

        <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;">
          <div class="rx-card-subtle">
            <div style="font-size:11px;font-weight:700;color:var(--nm-text-muted);">Total</div>
            <div style="font-size:22px;font-weight:900;color:var(--nm-text);margin-top:2px;">${total}</div>
          </div>
          <div class="rx-card-subtle">
            <div style="font-size:11px;font-weight:700;color:var(--nm-text-muted);">Expiring Soon</div>
            <div style="font-size:22px;font-weight:900;color:#D97706;margin-top:2px;">${expiring}</div>
          </div>
          <div class="rx-card-subtle">
            <div style="font-size:11px;font-weight:700;color:var(--nm-text-muted);">Expired</div>
            <div style="font-size:22px;font-weight:900;color:#DC2626;margin-top:2px;">${expired}</div>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:8px;">
          <div class="rx-well" style="display:flex;align-items:center;gap:8px;padding:8px 12px;border-radius:14px;">
            <i class="ph ph-magnifying-glass" style="color:var(--nm-text-muted);font-size:16px;" aria-hidden="true"></i>
            <input type="search" data-cred-search value="${escapeHtml(this.searchQuery)}" placeholder="Search by server, username, or alert email..." style="flex:1;border:0;background:transparent;color:var(--nm-text);font-size:13px;outline:none;min-inline-size:0;" />
            ${this.searchQuery ? `<button type="button" data-cred-clear-search style="border:0;background:transparent;color:var(--nm-text-muted);cursor:pointer;" aria-label="Clear search"><i class="ph ph-x"></i></button>` : ''}
          </div>

          <div style="display:flex;gap:8px;overflow-x:auto;padding-bottom:2px;">
            ${[
              { id: 'ALL', label: `All (${total})` },
              { id: 'EXPIRING', label: `Expiring Soon (${expiring})` },
              { id: 'EXPIRED', label: `Expired (${expired})` },
              { id: 'HEALTHY', label: `Healthy (${healthy})` }
            ].map((f) => `
              <button type="button" class="rx-chip-btn ${this.selectedFilter === f.id ? 'is-active' : ''}" data-cred-filter="${f.id}" style="white-space:nowrap;">
                ${f.label}
              </button>
            `).join('')}
          </div>
        </div>

        ${this.loading && this.trackers.length === 0 ? `
          <div class="rx-card" style="text-align:center;padding:32px;color:var(--nm-text-muted);">Loading credential trackers...</div>
        ` : grouped.size === 0 ? `
          <div class="rx-card" style="text-align:center;padding:32px;color:var(--nm-text-muted);">
            <i class="ph ph-key" style="font-size:32px;color:var(--nm-accent);display:block;margin-bottom:8px;"></i>
            <strong>${this.searchQuery || this.selectedFilter !== 'ALL' ? 'No matching credentials' : 'No Credential Trackers Yet'}</strong>
          </div>
        ` : `
          <div class="rx-land-grid-2" style="display:flex;flex-direction:column;gap:12px;">
            ${Array.from(grouped.entries()).map(([serverName, items]) => `
              <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
                <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding-bottom:8px;border-bottom:1px solid var(--nm-divider);">
                  <div style="display:flex;align-items:center;gap:10px;min-inline-size:0;">
                    <span style="width:34px;height:34px;border-radius:10px;background:var(--nm-accent-soft);color:var(--nm-accent);display:inline-flex;align-items:center;justify-content:center;font-size:18px;flex-shrink:0;">
                      <i class="ph ph-hard-drives" aria-hidden="true"></i>
                    </span>
                    <strong style="font-size:15px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(serverName)}</strong>
                  </div>
                  <span style="padding:3px 9px;border-radius:9px;background:var(--nm-input-surface);color:var(--nm-text-muted);font-size:11px;font-weight:700;white-space:nowrap;">
                    ${items.length} Account${items.length === 1 ? '' : 's'}
                  </span>
                </div>

                ${items.map((t) => {
                  const pill = statusPill(t);
                  const emails = Array.isArray(t.spokesperson_emails) ? t.spokesperson_emails : [];
                  const id = t.id || t._id;
                  return `
                    <div style="display:flex;flex-direction:column;gap:8px;padding:10px;border-radius:14px;background:var(--nm-input-surface);">
                      <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;flex-wrap:wrap;">
                        <div style="display:flex;align-items:center;gap:6px;font-weight:800;font-size:13.5px;">
                          <i class="ph ph-user-circle" style="color:var(--nm-accent);font-size:17px;"></i>
                          <span>${escapeHtml(t.username || 'service')}</span>
                        </div>
                        <span style="display:inline-flex;align-items:center;gap:5px;padding:3px 9px;border-radius:999px;background:${pill.bg};color:${pill.fg};font-size:11px;font-weight:800;">
                          <span style="width:6px;height:6px;border-radius:999px;background:${pill.dot};"></span>
                          ${escapeHtml(pill.label)}
                        </span>
                      </div>

                      <div style="display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;font-size:11.5px;color:var(--nm-text-muted);">
                        <span>Updated: ${escapeHtml(formatCredDate(t.last_updated || t.last_updated_date))} (${Number(t.validity_days || 45)}d cycle)</span>
                        <span style="font-weight:700;color:${pill.fg};">Expires: ${escapeHtml(formatCredDate(t.expiry_date))}</span>
                      </div>

                      ${emails.length ? `
                        <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap;font-size:11px;color:var(--nm-text-muted);">
                          <span>Alerts:</span>
                          ${emails.map((e) => `<span style="padding:2px 7px;border-radius:8px;background:var(--nm-surface);color:var(--nm-text-muted);">${escapeHtml(e)}</span>`).join('')}
                        </div>
                      ` : ''}

                      <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:2px;">
                        ${this.canRenew(t) ? `
                          <button type="button" class="rx-btn-surface" data-cred-renew="${escapeHtml(id)}" style="padding:6px 12px;font-size:12px;border-radius:11px;color:var(--nm-accent);">
                            <i class="ph ph-calendar-check" aria-hidden="true"></i> Update Date
                          </button>
                        ` : '<span></span>'}
                        ${this.isManagerOrAdmin ? `
                          <div style="display:flex;gap:6px;">
                            <button type="button" class="rx-icon-pill" data-cred-edit="${escapeHtml(id)}" aria-label="Edit tracker" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;">
                              <i class="ph ph-pencil-simple" aria-hidden="true"></i>
                            </button>
                            <button type="button" class="rx-icon-pill" data-cred-delete="${escapeHtml(id)}" aria-label="Delete tracker" style="width:32px;height:32px;min-inline-size:32px;min-block-size:32px;color:#DC2626;">
                              <i class="ph ph-trash" aria-hidden="true"></i>
                            </button>
                          </div>
                        ` : ''}
                      </div>
                    </div>
                  `;
                }).join('')}
              </div>
            `).join('')}
          </div>
        `}
      </div>
    `;

    this.bindEvents();
  }

  bindEvents() {
    this.container.querySelector('[data-cred-back]')?.addEventListener('click', () => this.onBack?.());
    this.container.querySelector('[data-cred-refresh]')?.addEventListener('click', () => this.load());
    this.container.querySelector('[data-cred-add]')?.addEventListener('click', () => this.openEditModal(null));
    this.container.querySelector('[data-cred-logs]')?.addEventListener('click', () => this.openLogsModal());

    const searchInput = this.container.querySelector('[data-cred-search]');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        this.searchQuery = e.target.value;
        this.render();
        const nextInput = this.container.querySelector('[data-cred-search]');
        if (nextInput) {
          nextInput.focus();
          nextInput.setSelectionRange(nextInput.value.length, nextInput.value.length);
        }
      });
    }
    this.container.querySelector('[data-cred-clear-search]')?.addEventListener('click', () => {
      this.searchQuery = '';
      this.render();
    });

    this.container.querySelectorAll('[data-cred-filter]').forEach((btn) => {
      btn.addEventListener('click', () => {
        this.selectedFilter = btn.getAttribute('data-cred-filter') || 'ALL';
        this.render();
      });
    });

    this.container.querySelectorAll('[data-cred-renew]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-cred-renew');
        const tracker = this.trackers.find((t) => String(t.id || t._id) === id);
        if (tracker) this.openRenewModal(tracker);
      });
    });

    this.container.querySelectorAll('[data-cred-edit]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-cred-edit');
        const tracker = this.trackers.find((t) => String(t.id || t._id) === id);
        if (tracker) this.openEditModal(tracker);
      });
    });

    this.container.querySelectorAll('[data-cred-delete]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const id = btn.getAttribute('data-cred-delete');
        const tracker = this.trackers.find((t) => String(t.id || t._id) === id);
        if (!tracker) return;
        if (!window.confirm(`Delete credential tracker for ${tracker.server_name} (${tracker.username})?`)) return;
        try {
          const resp = await fetch(`/api/credentials/${encodeURIComponent(id)}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
          });
          const data = await resp.json();
          if (resp.ok && data.success) {
            this.onToast('Credential tracker deleted', 'success');
            await this.load();
          } else {
            this.onToast(data.message || 'Delete failed', 'error');
          }
        } catch (err) {
          this.onToast(err.message, 'error');
        }
      });
    });
  }

  openRenewModal(tracker) {
    const id = tracker.id || tracker._id;
    const todayIso = getTodayIsoIst();
    let dialog = document.getElementById('rxCredRenewDialog');
    if (!dialog) {
      dialog = document.createElement('dialog');
      dialog.id = 'rxCredRenewDialog';
      dialog.className = 'rx-modal';
      dialog.setAttribute('closedby', 'any');
      document.body.appendChild(dialog);
    }
    dialog.innerHTML = `
      <div class="rx-modal-scroll">
        <div class="rx-modal-header">
          <div class="rx-modal-title-wrap">
            <div class="rx-modal-badge-icon"><i class="ph ph-calendar-check"></i></div>
            <div>
              <h3 style="margin:0;font-size:16px;font-weight:800;">Update Rotation Date</h3>
              <div style="font-size:12px;color:var(--nm-text-muted);">${escapeHtml(tracker.server_name)} • ${escapeHtml(tracker.username)}</div>
            </div>
          </div>
          <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close"><i class="ph ph-x"></i></button>
        </div>
        <form data-renew-form style="display:flex;flex-direction:column;gap:12px;">
          <div>
            <div class="rx-field-label">PASSWORD ROTATED ON (TODAY OR PAST ONLY)</div>
            <input type="date" name="last_updated" max="${todayIso}" value="${todayIso}" required style="width:100%;padding:11px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:14px;" />
          </div>
          <button type="submit" class="rx-btn-primary" style="width:100%;">Confirm Renewal Date</button>
        </form>
      </div>
    `;
    dialog.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dialog));
    dialog.querySelector('[data-renew-form]')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const dt = e.target.last_updated.value;
      if (!dt || dt > todayIso) {
        this.onToast('Renewal date cannot be in the future.', 'error');
        return;
      }
      try {
        const resp = await fetch(`/api/credentials/${encodeURIComponent(id)}`, {
          method: 'PUT',
          headers: getAuthHeaders(),
          body: JSON.stringify({ last_updated: dt, last_updated_date: dt })
        });
        const data = await resp.json();
        if (resp.ok && data.success) {
          this.onToast('Password rotation date updated!', 'success');
          closeNeumorphicDialog(dialog);
          await this.load();
        } else {
          this.onToast(data.message || 'Renewal failed', 'error');
        }
      } catch (err) {
        this.onToast(err.message, 'error');
      }
    });
    openNeumorphicDialog(dialog);
  }

  openEditModal(tracker) {
    const isEdit = Boolean(tracker);
    const todayIso = getTodayIsoIst();
    let dialog = document.getElementById('rxCredEditDialog');
    if (!dialog) {
      dialog = document.createElement('dialog');
      dialog.id = 'rxCredEditDialog';
      dialog.className = 'rx-modal';
      dialog.setAttribute('closedby', 'any');
      document.body.appendChild(dialog);
    }
    const emailsStr = Array.isArray(tracker?.spokesperson_emails) ? tracker.spokesperson_emails.join(', ') : '';
    dialog.innerHTML = `
      <div class="rx-modal-scroll">
        <div class="rx-modal-header">
          <div class="rx-modal-title-wrap">
            <div class="rx-modal-badge-icon"><i class="ph ph-key"></i></div>
            <div>
              <h3 style="margin:0;font-size:16px;font-weight:800;">${isEdit ? 'Edit Credential Tracker' : 'Add Credential Tracker'}</h3>
              <div style="font-size:12px;color:var(--nm-text-muted);">Track server account expiry without storing passwords</div>
            </div>
          </div>
          <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close"><i class="ph ph-x"></i></button>
        </div>
        <form data-edit-form style="display:flex;flex-direction:column;gap:12px;">
          <div>
            <div class="rx-field-label">SERVER NAME *</div>
            <input type="text" name="server_name" value="${escapeHtml(tracker?.server_name || '')}" placeholder="e.g. NMS Server" required style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
            <div>
              <div class="rx-field-label">ACCOUNT USERNAME *</div>
              <input type="text" name="username" value="${escapeHtml(tracker?.username || '')}" placeholder="e.g. root" required style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
            </div>
            <div>
              <div class="rx-field-label">VALIDITY CYCLE (DAYS) *</div>
              <input type="number" name="validity_days" min="1" max="365" value="${Number(tracker?.validity_days || 45)}" required style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
            </div>
          </div>
          <div>
            <div class="rx-field-label">LAST UPDATED DATE *</div>
            <input type="date" name="last_updated" max="${todayIso}" value="${escapeHtml(tracker?.last_updated || tracker?.last_updated_date || todayIso)}" required style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
          </div>
          <div>
            <div class="rx-field-label">ALERT EMAILS (COMMA-SEPARATED)</div>
            <input type="text" name="emails" value="${escapeHtml(emailsStr)}" placeholder="user@example.com, ops@example.com" style="width:100%;padding:10px 12px;border-radius:12px;border:1px solid var(--nm-border);background:var(--nm-input-surface);color:var(--nm-text);font-size:13.5px;" />
          </div>
          <button type="submit" class="rx-btn-primary" style="width:100%;">${isEdit ? 'Save Changes' : 'Create Tracker'}</button>
        </form>
      </div>
    `;
    dialog.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dialog));
    dialog.querySelector('[data-edit-form]')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const f = e.target;
      const payload = {
        server_name: f.server_name.value.trim(),
        username: f.username.value.trim(),
        validity_days: Number(f.validity_days.value) || 45,
        last_updated: f.last_updated.value,
        last_updated_date: f.last_updated.value,
        spokesperson_emails: f.emails.value.split(',').map((s) => s.trim()).filter(Boolean)
      };
      const url = isEdit ? `/api/credentials/${encodeURIComponent(tracker.id || tracker._id)}` : '/api/credentials';
      try {
        const resp = await fetch(url, {
          method: isEdit ? 'PUT' : 'POST',
          headers: getAuthHeaders(),
          body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (resp.ok && data.success) {
          this.onToast(isEdit ? 'Tracker updated!' : 'Tracker created!', 'success');
          closeNeumorphicDialog(dialog);
          await this.load();
        } else {
          this.onToast(data.message || 'Save failed', 'error');
        }
      } catch (err) {
        this.onToast(err.message, 'error');
      }
    });
    openNeumorphicDialog(dialog);
  }

  async openLogsModal() {
    let dialog = document.getElementById('rxCredLogsDialog');
    if (!dialog) {
      dialog = document.createElement('dialog');
      dialog.id = 'rxCredLogsDialog';
      dialog.className = 'rx-modal';
      dialog.setAttribute('closedby', 'any');
      document.body.appendChild(dialog);
    }
    dialog.innerHTML = `<div class="rx-modal-scroll"><div style="text-align:center;padding:24px;color:var(--nm-text-muted);">Loading audit logs...</div></div>`;
    openNeumorphicDialog(dialog);

    try {
      const resp = await fetch('/api/credentials/logs', { headers: getAuthHeaders() });
      const data = await resp.json();
      const logs = data.logs || data.data || [];
      dialog.innerHTML = `
        <div class="rx-modal-scroll">
          <div class="rx-modal-header">
            <div class="rx-modal-title-wrap">
              <div class="rx-modal-badge-icon"><i class="ph ph-clock-counter-clockwise"></i></div>
              <div>
                <h3 style="margin:0;font-size:16px;font-weight:800;">Credential Audit Logs</h3>
                <div style="font-size:12px;color:var(--nm-text-muted);">${logs.length} recorded event${logs.length === 1 ? '' : 's'}</div>
              </div>
            </div>
            <button type="button" class="rx-icon-pill" data-rx-close aria-label="Close"><i class="ph ph-x"></i></button>
          </div>
          ${logs.length === 0 ? `<div class="rx-well" style="text-align:center;color:var(--nm-text-muted);">No credential audit logs yet.</div>` : `
            <div style="display:flex;flex-direction:column;gap:8px;">
              ${logs.slice(0, 50).map((log) => `
                <div class="rx-well" style="padding:10px 12px;display:flex;flex-direction:column;gap:3px;">
                  <div style="display:flex;justify-content:space-between;gap:8px;font-size:12px;font-weight:800;">
                    <span>${escapeHtml(log.action || 'UPDATE')} • ${escapeHtml(log.server_name || '')}</span>
                    <span style="font-size:10.5px;color:var(--nm-text-muted);font-weight:600;">${escapeHtml(log.timestamp || log.created_at || '')}</span>
                  </div>
                  <div style="font-size:11.5px;color:var(--nm-text-muted);">
                    ${escapeHtml(log.details || log.message || '')} ${log.performed_by ? `• by @${escapeHtml(log.performed_by)}` : ''}
                  </div>
                </div>
              `).join('')}
            </div>
          `}
        </div>
      `;
      dialog.querySelector('[data-rx-close]')?.addEventListener('click', () => closeNeumorphicDialog(dialog));
    } catch (err) {
      dialog.innerHTML = `<div class="rx-modal-scroll"><div style="color:#DC2626;">Failed to load logs: ${escapeHtml(err.message)}</div></div>`;
    }
  }
}
