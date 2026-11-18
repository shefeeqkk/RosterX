/**
 * RosterX — My Team Directory Module (myTeamView.js)
 *
 * 1:1 parity with Android MyTeamView.kt:
 * - Operations Management + Shift Engineers & Apprentices sections
 * - Instant search by name, role, or phone
 * - Direct tel: Call and https://wa.me/ WhatsApp action buttons
 * - Responsive 2-column grid in landscape & desktop
 */

import { escapeHtml } from './pwaHelper.js';

function getInitials(name) {
  const parts = String(name || 'TM').trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  return (parts[0] || 'TM').slice(0, 2).toUpperCase();
}

function cleanPhoneForTel(phone) {
  return String(phone || '').replace(/[^0-9+]/g, '');
}

function cleanPhoneForWhatsApp(phone) {
  const digits = String(phone || '').replace(/[^0-9]/g, '');
  return digits.length === 10 ? `91${digits}` : digits;
}

export class MyTeamViewController {
  constructor({ container, engineers = [], onBack, hideBackButton = false, onToast }) {
    this.container = container;
    this.engineers = engineers;
    this.onBack = onBack;
    this.hideBackButton = Boolean(hideBackButton);
    this.onToast = onToast || (() => {});
    this.searchQuery = '';
  }

  setEngineers(engineers) {
    this.engineers = Array.isArray(engineers) ? engineers : [];
    this.render();
  }

  renderMemberCard(eng, roleLabel, badgeColor) {
    const displayName = eng.full_name || eng.name || 'Team Member';
    const rawPhone = String(eng.phone || '').trim();
    const tel = cleanPhoneForTel(rawPhone);
    const wa = cleanPhoneForWhatsApp(rawPhone);
    const avatarHtml = eng.profile_picture
      ? `<img src="${escapeHtml(eng.profile_picture)}" alt="${escapeHtml(displayName)}" style="width:44px;height:44px;border-radius:999px;object-fit:cover;flex-shrink:0;" />`
      : `<div style="width:44px;height:44px;border-radius:999px;background:var(--nm-accent-soft);color:var(--nm-accent);font-weight:800;font-size:14px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">${escapeHtml(getInitials(displayName))}</div>`;

    return `
      <div class="rx-card-subtle" style="display:flex;align-items:center;gap:12px;padding:12px 14px;">
        ${avatarHtml}
        <div style="flex:1;min-inline-size:0;display:flex;flex-direction:column;gap:3px;">
          <div style="font-weight:800;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(displayName)}</div>
          <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap;">
            <span style="padding:2px 8px;border-radius:8px;background:${badgeColor}20;color:${badgeColor};font-size:10px;font-weight:800;">${escapeHtml(roleLabel)}</span>
            ${eng.email ? `<span style="font-size:11px;color:var(--nm-text-muted);overflow-wrap:anywhere;">${escapeHtml(eng.email)}</span>` : ''}
          </div>
          ${rawPhone ? `<div style="font-size:11.5px;color:var(--nm-text-muted);font-weight:600;">${escapeHtml(rawPhone)}</div>` : ''}
        </div>
        <div style="display:flex;align-items:center;gap:6px;flex-shrink:0;">
          ${tel ? `
            <a href="tel:${escapeHtml(tel)}" class="rx-icon-pill" aria-label="Call ${escapeHtml(displayName)}" style="background:rgba(5,150,105,0.13);color:#059669;text-decoration:none;box-shadow:none;">
              <i class="ph ph-phone" aria-hidden="true"></i>
            </a>
          ` : ''}
          ${wa ? `
            <a href="https://wa.me/${escapeHtml(wa)}" target="_blank" rel="noopener" class="rx-icon-pill" aria-label="WhatsApp ${escapeHtml(displayName)}" style="background:rgba(37,211,102,0.16);color:#16A34A;text-decoration:none;box-shadow:none;">
              <i class="ph ph-whatsapp-logo" aria-hidden="true"></i>
            </a>
          ` : ''}
        </div>
      </div>
    `;
  }

  renderListHtml(active) {
    const q = this.searchQuery.trim().toLowerCase();
    const filtered = !q ? active : active.filter((e) =>
      String(e.name || '').toLowerCase().includes(q) ||
      String(e.full_name || '').toLowerCase().includes(q) ||
      String(e.role || '').toLowerCase().includes(q) ||
      String(e.phone || '').toLowerCase().includes(q)
    );

    const managers = filtered.filter((e) => String(e.role || '').toLowerCase() === 'manager')
      .sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')));
    const team = filtered.filter((e) => String(e.role || '').toLowerCase() !== 'manager')
      .sort((a, b) => String(a.name || '').localeCompare(String(b.name || '')));

    return `
      <div style="display:flex;flex-direction:column;gap:14px;">
        ${managers.length ? `
          <div style="display:flex;flex-direction:column;gap:8px;">
            <div class="rx-field-label" style="color:#6366F1;">Operations Management (${managers.length})</div>
            <div class="rx-land-grid-2" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:10px;">
              ${managers.map((m) => this.renderMemberCard(m, 'Operations Manager', '#6366F1')).join('')}
            </div>
          </div>
        ` : ''}

        ${team.length ? `
          <div style="display:flex;flex-direction:column;gap:8px;">
            <div class="rx-field-label" style="color:var(--nm-accent);">Shift Engineers &amp; Operations (${team.length})</div>
            <div class="rx-land-grid-2" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:10px;">
              ${team.map((m) => {
                const isAppr = String(m.role || '').toLowerCase() === 'apprentice';
                return this.renderMemberCard(m, isAppr ? 'Apprentice Engineer' : 'Shift Engineer', isAppr ? '#D97706' : '#174EAC');
              }).join('')}
            </div>
          </div>
        ` : ''}

        ${!managers.length && !team.length ? `
          <div class="rx-card" style="text-align:center;padding:32px;color:var(--nm-text-muted);">No team members match your search.</div>
        ` : ''}
      </div>
    `;
  }

  getActiveMembers() {
    const SYSTEM_ADMINS = new Set(['admin', 'sysadmin', 'administrator']);
    return this.engineers.filter((e) => {
      const status = String(e.status || 'active').toLowerCase();
      const role = String(e.role || 'engineer').toLowerCase();
      const uname = String(e.username || '').toLowerCase();
      if (status === 'resigned' || SYSTEM_ADMINS.has(uname)) return false;
      if (e.include_in_roster === true) return true;
      return ['manager', 'engineer', 'apprentice'].includes(role);
    });
  }

  render() {
    if (!this.container) return;
    const active = this.getActiveMembers();

    this.container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;align-items:center;gap:10px;">
          ${this.onBack && !this.hideBackButton ? `
            <button type="button" class="rx-icon-pill" data-team-back aria-label="Back to Operations Hub">
              <i class="ph ph-arrow-left" aria-hidden="true"></i>
            </button>
          ` : ''}
          <div>
            ${this.hideBackButton
              ? `<div style="font-size:12px;font-weight:700;color:var(--nm-text-muted);">${active.length} Active Team Members</div>`
              : `<h2 style="margin:0;font-size:19px;font-weight:800;">My Team Directory</h2>
                 <div style="font-size:12px;color:var(--nm-text-muted);">${active.length} Active Team Members</div>`}
          </div>
        </div>

        <div class="rx-well" style="display:flex;align-items:center;gap:8px;padding:8px 12px;border-radius:14px;">
          <i class="ph ph-magnifying-glass" style="color:var(--nm-text-muted);font-size:16px;" aria-hidden="true"></i>
          <input type="search" data-team-search value="${escapeHtml(this.searchQuery)}" placeholder="Search by name, role, or phone..." style="flex:1;border:0;background:transparent;color:var(--nm-text);font-size:13px;outline:none;min-inline-size:0;" />
          <button type="button" data-team-clear style="border:0;background:transparent;color:var(--nm-text-muted);cursor:pointer;${this.searchQuery ? '' : 'display:none;'}" aria-label="Clear"><i class="ph ph-x"></i></button>
        </div>

        <div data-team-list>${this.renderListHtml(active)}</div>
      </div>
    `;

    this.container.querySelector('[data-team-back]')?.addEventListener('click', () => this.onBack?.());
    const searchEl = this.container.querySelector('[data-team-search]');
    const clearBtn = this.container.querySelector('[data-team-clear]');
    const listHost = this.container.querySelector('[data-team-list]');
    if (searchEl) {
      searchEl.addEventListener('input', (e) => {
        this.searchQuery = e.target.value;
        if (listHost) listHost.innerHTML = this.renderListHtml(this.getActiveMembers());
        if (clearBtn) clearBtn.style.display = this.searchQuery ? '' : 'none';
      });
    }
    clearBtn?.addEventListener('click', () => {
      this.searchQuery = '';
      if (searchEl) searchEl.value = '';
      if (listHost) listHost.innerHTML = this.renderListHtml(this.getActiveMembers());
      clearBtn.style.display = 'none';
    });
  }
}
