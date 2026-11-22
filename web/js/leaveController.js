/**
 * RosterX — Leave & Swap Request Controller (leaveController.js)
 *
 * Implements Task 15 (Sprint v2.4.0):
 * 1. Segmented tab switcher: [ Apply Request ] ⇄ [ My History ]
 * 2. Multi-date monthly calendar with off-day & past-date protection
 * 3. Max 5 days selection with real-time counter
 * 4. Smart Swap Partner Dropdown with Scheduled-Off ranking
 * 5. Annual 14-day leave balance tracker
 * 6. Admin & Manager Leave Approvals Queue
 */

import { escapeHtml, getAuthHeaders } from './pwaHelper.js';

function getIstNow() {
  const d = new Date();
  const utc = d.getTime() + d.getTimezoneOffset() * 60000;
  return new Date(utc + 5.5 * 3600000);
}

function formatDateIso(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export class LeaveController {
  constructor({ container, currentUser, isManager, onBack, onToast, onRefreshRoster }) {
    this.container = container;
    this.user = currentUser;
    this.isManager = Boolean(isManager);
    this.onBack = onBack || (() => {});
    this.onToast = onToast || (() => {});
    this.onRefreshRoster = onRefreshRoster || (() => {});

    this.activeTab = 'apply'; // 'apply' | 'history'
    const now = getIstNow();
    this.calYear = now.getFullYear();
    this.calMonth = now.getMonth() + 1; // 1-12

    this.selectedDates = new Set();
    this.preferredPartner = '';
    this.reason = '';

    this.myRequests = [];
    this.leaveStats = {
      approved_leave_days: 0,
      remaining_leave_balance: 14,
      annual_quota: 14,
      active_count: 0
    };
    this.eligiblePartners = [];
    this.isLoadingPartners = false;
    this.isSubmitting = false;

    // Monthly roster cache: { "YYYY-MM": doc }
    this.rosterCache = {};
  }

  async load() {
    await this.fetchMyRequests();
    await this.fetchEligiblePartners();
    this.render();
  }

  async fetchMyRequests() {
    try {
      const resp = await fetch('/api/leave/my-requests', {
        headers: getAuthHeaders()
      });
      const data = await resp.json();
      if (data.success) {
        this.myRequests = data.requests || [];
        this.leaveStats = {
          approved_leave_days: data.approved_leave_days || 0,
          remaining_leave_balance: data.remaining_leave_balance ?? 14,
          annual_quota: data.annual_quota || 14,
          active_count: data.active_count || 0
        };
      }
    } catch (e) {
      console.warn('Failed loading leave requests:', e);
    }
  }

  async fetchEligiblePartners(targetDate = '') {
    this.isLoadingPartners = true;
    try {
      const dateParam = targetDate || Array.from(this.selectedDates)[0] || formatDateIso(getIstNow());
      const resp = await fetch(`/api/leave/eligible-partners?date=${encodeURIComponent(dateParam)}`, {
        headers: getAuthHeaders()
      });
      const data = await resp.json();
      if (data.success) {
        this.eligiblePartners = data.partners || [];
      }
    } catch (e) {
      console.warn('Failed fetching eligible partners:', e);
    } finally {
      this.isLoadingPartners = false;
    }
  }

  async fetchRosterForMonth(year, month) {
    const key = `${year}-${String(month).padStart(2, '0')}`;
    if (this.rosterCache[key]) return this.rosterCache[key];
    try {
      const resp = await fetch(`/api/roster/${key}`, { headers: getAuthHeaders() });
      const data = await resp.json();
      if (data.success) {
        this.rosterCache[key] = data;
        return data;
      }
    } catch (_) {}
    return null;
  }

  toggleDate(dateStr, isOff, isPast) {
    if (isPast || isOff) return;
    if (this.selectedDates.has(dateStr)) {
      this.selectedDates.delete(dateStr);
    } else {
      if (this.selectedDates.size >= 5) {
        this.onToast('Maximum 5 days can be selected per request', 'error');
        return;
      }
      this.selectedDates.add(dateStr);
    }
    // Refresh partner recommendations when first date changes
    if (this.selectedDates.size === 1) {
      this.fetchEligiblePartners(Array.from(this.selectedDates)[0]).then(() => this.render());
    } else {
      this.render();
    }
  }

  async submitLeaveRequest() {
    if (this.selectedDates.size === 0) {
      this.onToast('Please select at least 1 date on the calendar', 'error');
      return;
    }
    if (this.isSubmitting) return;

    this.isSubmitting = true;
    const btn = this.container.querySelector('#btnSubmitLeave');
    if (btn) {
      btn.disabled = true;
      btn.textContent = 'Submitting...';
    }

    try {
      const payload = {
        dates: Array.from(this.selectedDates).sort(),
        reason: this.reason.trim(),
        preferred_partner: this.preferredPartner
      };

      const resp = await fetch('/api/leave/apply', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders()
        },
        body: JSON.stringify(payload)
      });
      const data = await resp.json();

      if (data.success) {
        this.onToast('Leave request submitted successfully!', 'success');
        this.selectedDates.clear();
        this.reason = '';
        this.preferredPartner = '';
        this.activeTab = 'history';
        await this.load();
      } else {
        this.onToast(data.message || 'Failed to submit leave request', 'error');
      }
    } catch (e) {
      this.onToast('Network error submitting leave request', 'error');
    } finally {
      this.isSubmitting = false;
      this.render();
    }
  }

  render() {
    if (!this.container) return;
    const now = getIstNow();
    const todayStr = formatDateIso(now);

    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    const monthLabel = `${monthNames[this.calMonth - 1]} ${this.calYear}`;

    // Compute Calendar Days
    const firstDayIndex = new Date(this.calYear, this.calMonth - 1, 1).getDay(); // 0 = Sun
    const totalDays = new Date(this.calYear, this.calMonth, 0).getDate();

    // Check applicant scheduled shift for day
    const mKey = `${this.calYear}-${String(this.calMonth).padStart(2, '0')}`;
    const rData = this.rosterCache[mKey];
    const selfName = this.user?.full_name || this.user?.name || this.user?.username || '';
    const selfUname = this.user?.username || '';

    const dayCells = [];
    // Leading blanks
    for (let i = 0; i < firstDayIndex; i++) {
      dayCells.push('<div class="rx-cal-day-blank"></div>');
    }

    for (let d = 1; d <= totalDays; d++) {
      const dStr = `${this.calYear}-${String(this.calMonth).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const isPast = dStr < todayStr;
      const isSelected = this.selectedDates.has(dStr);

      // Check shift from roster cache if available
      let shiftCode = '';
      if (rData) {
        const shifts = rData.shifts || {};
        shiftCode = shifts[selfName]?.[String(d)] || shifts[selfUname]?.[String(d)] || '';
      }
      const isOff = shiftCode === 'OFF' || shiftCode === 'L' || shiftCode.startsWith('SW');

      let cellClass = 'rx-cal-day-cell';
      let badgeHtml = '';
      if (isPast) {
        cellClass += ' is-past';
      } else if (isOff) {
        cellClass += ' is-off';
        badgeHtml = '<span class="rx-cal-cell-off">OFF</span>';
      } else if (isSelected) {
        cellClass += ' is-selected';
      } else if (shiftCode) {
        badgeHtml = `<span class="rx-cal-cell-shift">${escapeHtml(shiftCode)}</span>`;
      }

      dayCells.push(`
        <button type="button" class="${cellClass}" data-leave-date="${dStr}" data-is-off="${isOff}" data-is-past="${isPast}" ${isPast || isOff ? 'disabled' : ''} aria-label="${dStr} ${isOff ? 'Scheduled Off' : ''}">
          <span class="rx-cal-cell-num">${d}</span>
          ${badgeHtml}
        </button>
      `);
    }

    const selectedCount = this.selectedDates.size;

    this.container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:16px;max-width:860px;margin:0 auto;padding-bottom:32px;">
        
        <!-- Segmented Tab Header (Apply Request ⇄ My History) -->
        <div style="display:flex;align-items:center;background:var(--nm-input-surface);padding:4px;border-radius:16px;box-shadow:var(--nm-shadow-inset-subtle);">
          <button type="button" id="tabBtnApply" class="rx-btn-surface" style="flex:1;border-radius:12px;padding:9px;font-size:13px;font-weight:800;border:0;${this.activeTab === 'apply' ? 'background:var(--nm-accent-gradient);color:#fff;box-shadow:0 4px 12px var(--nm-accent-glow);' : 'background:transparent;color:var(--nm-text-muted);'}">
            <i class="ph ph-calendar-plus" style="margin-right:6px;"></i> Apply Request
          </button>
          <button type="button" id="tabBtnHistory" class="rx-btn-surface" style="flex:1;border-radius:12px;padding:9px;font-size:13px;font-weight:800;border:0;${this.activeTab === 'history' ? 'background:var(--nm-accent-gradient);color:#fff;box-shadow:0 4px 12px var(--nm-accent-glow);' : 'background:transparent;color:var(--nm-text-muted);'}">
            <i class="ph ph-clock-counter-clockwise" style="margin-right:6px;"></i> My History ${this.myRequests.length > 0 ? `(${this.myRequests.length})` : ''}
          </button>
        </div>

        ${this.activeTab === 'apply' ? `
          <!-- APPLY REQUEST VIEW -->
          <div style="display:flex;flex-direction:column;gap:14px;">
            
            <!-- Calendar Card -->
            <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
              <div style="display:flex;align-items:center;justify-content:space-between;">
                <div>
                  <div style="font-size:16px;font-weight:900;">Select Duty Dates</div>
                  <div style="font-size:11.5px;color:var(--nm-text-muted);">Pick 1 to 5 active duty days. Scheduled off-days are locked.</div>
                </div>
                <div style="display:flex;align-items:center;gap:6px;">
                  <button type="button" id="btnCalPrev" class="rx-btn-surface" style="width:34px;height:34px;padding:0;min-inline-size:34px;border-radius:10px;" aria-label="Previous Month">
                    <i class="ph ph-caret-left"></i>
                  </button>
                  <span style="font-size:13px;font-weight:800;min-width:110px;text-align:center;">${escapeHtml(monthLabel)}</span>
                  <button type="button" id="btnCalNext" class="rx-btn-surface" style="width:34px;height:34px;padding:0;min-inline-size:34px;border-radius:10px;" aria-label="Next Month">
                    <i class="ph ph-caret-right"></i>
                  </button>
                </div>
              </div>

              <!-- Weekday Header -->
              <div style="display:grid;grid-template-columns:repeat(7,1fr);text-align:center;font-size:11px;font-weight:800;color:var(--nm-text-muted);padding-bottom:4px;">
                <div>Sun</div><div>Mon</div><div>Tue</div><div>Wed</div><div>Thu</div><div>Fri</div><div>Sat</div>
              </div>

              <!-- Month Grid -->
              <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:4px;">
                ${dayCells.join('')}
              </div>

              <!-- Selection Counter Pill -->
              <div style="display:flex;align-items:center;justify-content:space-between;padding-top:6px;border-top:1px solid rgba(148,163,184,0.15);">
                <div style="display:flex;align-items:center;gap:8px;font-size:12px;font-weight:700;">
                  <span style="padding:3px 9px;border-radius:999px;background:${selectedCount > 0 ? 'var(--nm-accent-soft)' : 'var(--nm-input-surface)'};color:${selectedCount > 0 ? 'var(--nm-accent)' : 'var(--nm-text-muted)'};">
                    Selected: ${selectedCount} / 5 days
                  </span>
                  ${selectedCount > 0 ? `
                    <button type="button" id="btnClearDates" style="background:none;border:0;color:#DC2626;font-size:11px;font-weight:700;cursor:pointer;padding:2px 6px;">
                      Clear
                    </button>
                  ` : ''}
                </div>
                <div style="font-size:11px;color:var(--nm-text-muted);">
                  ${this.leaveStats.remaining_leave_balance} annual days remaining
                </div>
              </div>
            </div>

            <!-- Partner Selection Card -->
            <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
              <div>
                <label for="selectPartner" style="display:block;font-size:13.5px;font-weight:800;margin-bottom:3px;">
                  Preferred Shift Swap Partner <span style="font-size:11px;font-weight:400;color:var(--nm-text-muted);">(Optional)</span>
                </label>
                <div style="font-size:11.5px;color:var(--nm-text-muted);margin-bottom:8px;">
                  Colleagues scheduled OFF on your selected dates appear first as recommended partners.
                </div>
                <select id="selectPartner" class="rx-well" style="width:100%;border:0;padding:12px;font-size:13px;font-weight:600;color:var(--nm-text);box-sizing:border-box;">
                  <option value="">No preference / Any available partner</option>
                  <optgroup label="⭐ Scheduled Off (Recommended)">
                    ${this.eligiblePartners.filter((p) => p.is_off).map((p) => `
                      <option value="${escapeHtml(p.username)}" ${this.preferredPartner === p.username ? 'selected' : ''}>
                        ${escapeHtml(p.name)} (${escapeHtml(p.shift)})
                      </option>
                    `).join('')}
                  </optgroup>
                  <optgroup label="Other Team Members">
                    ${this.eligiblePartners.filter((p) => !p.is_off).map((p) => `
                      <option value="${escapeHtml(p.username)}" ${this.preferredPartner === p.username ? 'selected' : ''}>
                        ${escapeHtml(p.name)} (${escapeHtml(p.shift)})
                      </option>
                    `).join('')}
                  </optgroup>
                </select>
              </div>

              <!-- Reason Notes Input -->
              <div>
                <label for="inputLeaveReason" style="display:block;font-size:13.5px;font-weight:800;margin-bottom:3px;">
                  Reason / Operational Notes <span style="font-size:11px;font-weight:400;color:var(--nm-text-muted);">(Optional)</span>
                </label>
                <textarea id="inputLeaveReason" rows="2" class="rx-well" placeholder="Brief note for shift supervisor or partner..." style="width:100%;border:0;padding:10px 12px;font-size:13px;font-family:inherit;color:var(--nm-text);box-sizing:border-box;resize:vertical;">${escapeHtml(this.reason)}</textarea>
              </div>

              <!-- Submit Button -->
              <button type="button" id="btnSubmitLeave" class="rx-btn-primary" style="width:100%;padding:14px;font-size:14px;border-radius:16px;" ${selectedCount === 0 || this.isSubmitting ? 'disabled' : ''}>
                <i class="ph ph-paper-plane-tilt" style="font-size:17px;"></i>
                <span>Submit Leave &amp; Swap Request</span>
              </button>
            </div>
          </div>
        ` : `
          <!-- MY HISTORY VIEW -->
          <div style="display:flex;flex-direction:column;gap:14px;">
            
            <!-- Annual Leave Quota Card -->
            <div class="rx-card" style="display:flex;align-items:center;justify-content:space-between;gap:12px;background:var(--nm-surface);">
              <div>
                <div style="font-size:11px;font-weight:900;color:var(--nm-accent);letter-spacing:0.05em;">ANNUAL LEAVE ALLOWANCE</div>
                <div style="font-size:18px;font-weight:900;margin-top:2px;">${this.leaveStats.remaining_leave_balance} of ${this.leaveStats.annual_quota} Days Remaining</div>
                <div style="font-size:11.5px;color:var(--nm-text-muted);margin-top:2px;">${this.leaveStats.approved_leave_days} days approved in ${this.calYear}</div>
              </div>
              <div style="width:52px;height:52px;border-radius:999px;background:var(--nm-accent-soft);color:var(--nm-accent);display:flex;align-items:center;justify-content:center;font-size:24px;flex-shrink:0;">
                <i class="ph ph-calendar-check"></i>
              </div>
            </div>

            <!-- Requests List -->
            <div style="display:flex;flex-direction:column;gap:10px;">
              <div style="font-size:13px;font-weight:800;color:var(--nm-text-muted);">Request History (${this.myRequests.length})</div>
              
              ${this.myRequests.length === 0 ? `
                <div class="rx-card" style="text-align:center;padding:32px 16px;color:var(--nm-text-muted);">
                  <i class="ph ph-calendar-blank" style="font-size:36px;margin-bottom:8px;display:block;"></i>
                  <div style="font-weight:700;font-size:14px;">No leave requests yet</div>
                  <div style="font-size:12px;margin-top:4px;">When you apply for duty leave or shift swaps, your submissions will appear here.</div>
                </div>
              ` : this.myRequests.map((req) => {
                const st = String(req.status || 'pending').toLowerCase();
                let statusBadge = '';
                if (st === 'pending') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(245,158,11,0.15);color:#D97706;font-size:11px;font-weight:800;">Pending Review</span>';
                } else if (st === 'approved_swap') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(16,185,129,0.15);color:#059669;font-size:11px;font-weight:800;">Approved as Swap</span>';
                } else if (st === 'approved_leave') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(59,130,246,0.15);color:#2563EB;font-size:11px;font-weight:800;">Approved as Leave</span>';
                } else if (st === 'rejected') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(220,38,38,0.15);color:#DC2626;font-size:11px;font-weight:800;">Declined</span>';
                }

                const datesList = req.dates || [];
                const partner = req.approval?.swap_partner || req.preferred_partner;

                return `
                  <div class="rx-card-subtle" style="display:flex;flex-direction:column;gap:8px;padding:14px;">
                    <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                      <div style="font-weight:900;font-size:13.5px;color:var(--nm-text);">${escapeHtml(req.request_id)}</div>
                      ${statusBadge}
                    </div>
                    
                    <!-- Dates Chips -->
                    <div style="display:flex;gap:6px;flex-wrap:wrap;margin:2px 0;">
                      ${datesList.map((d) => `
                        <span style="padding:2px 8px;border-radius:6px;background:var(--nm-input-surface);color:var(--nm-text);font-size:11.5px;font-weight:700;">
                          ${escapeHtml(d)}
                        </span>
                      `).join('')}
                    </div>

                    ${req.reason ? `
                      <div style="font-size:12px;color:var(--nm-text-muted);font-style:italic;">
                        &ldquo;${escapeHtml(req.reason)}&rdquo;
                      </div>
                    ` : ''}

                    <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;font-size:11px;color:var(--nm-text-muted);padding-top:4px;border-top:1px solid rgba(148,163,184,0.1);">
                      <div>${partner ? `Partner: <strong>${escapeHtml(partner)}</strong>` : 'No partner specified'}</div>
                      <div>${escapeHtml(String(req.created_at || '').slice(0, 10))}</div>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        `}
      </div>
    `;

    // Bind Event Listeners
    this.container.querySelector('#tabBtnApply')?.addEventListener('click', () => {
      this.activeTab = 'apply';
      this.render();
    });
    this.container.querySelector('#tabBtnHistory')?.addEventListener('click', () => {
      this.activeTab = 'history';
      this.render();
    });

    this.container.querySelector('#btnCalPrev')?.addEventListener('click', () => {
      if (this.calMonth === 1) { this.calMonth = 12; this.calYear--; } else this.calMonth--;
      this.fetchRosterForMonth(this.calYear, this.calMonth).then(() => this.render());
    });
    this.container.querySelector('#btnCalNext')?.addEventListener('click', () => {
      if (this.calMonth === 12) { this.calMonth = 1; this.calYear++; } else this.calMonth++;
      this.fetchRosterForMonth(this.calYear, this.calMonth).then(() => this.render());
    });

    this.container.querySelector('#btnClearDates')?.addEventListener('click', () => {
      this.selectedDates.clear();
      this.render();
    });

    this.container.querySelectorAll('[data-leave-date]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const dStr = btn.getAttribute('data-leave-date');
        const isOff = btn.getAttribute('data-is-off') === 'true';
        const isPast = btn.getAttribute('data-is-past') === 'true';
        this.toggleDate(dStr, isOff, isPast);
      });
    });

    this.container.querySelector('#selectPartner')?.addEventListener('change', (e) => {
      this.preferredPartner = e.target.value;
    });

    this.container.querySelector('#inputLeaveReason')?.addEventListener('input', (e) => {
      this.reason = e.target.value;
    });

    this.container.querySelector('#btnSubmitLeave')?.addEventListener('click', () => {
      this.submitLeaveRequest();
    });
  }
}

/**
 * Admin & Manager Leave Approvals Queue Controller
 */
export class LeaveApprovalsController {
  constructor({ container, currentUser, onToast, onBack, onRosterMutated }) {
    this.container = container;
    this.user = currentUser;
    this.onToast = onToast || (() => {});
    this.onBack = onBack || (() => {});
    this.onRosterMutated = onRosterMutated || (() => {});

    this.pendingRequests = [];
    this.historicalRequests = [];
    this.isLoading = false;
  }

  async load() {
    this.isLoading = true;
    try {
      const resp = await fetch('/api/leave/admin-queue', { headers: getAuthHeaders() });
      const data = await resp.json();
      if (data.success) {
        this.pendingRequests = data.pending_requests || [];
        this.historicalRequests = data.historical_requests || [];
      } else {
        this.onToast(data.message || 'Failed loading approvals queue', 'error');
      }
    } catch (e) {
      this.onToast('Network error loading approvals queue', 'error');
    } finally {
      this.isLoading = false;
      this.render();
    }
  }

  async takeAction(requestId, action, partner = '', swapCode = '') {
    try {
      const resp = await fetch('/api/leave/admin-action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify({
          request_id: requestId,
          action,
          swap_partner: partner,
          swap_code: swapCode
        })
      });
      const data = await resp.json();
      if (data.success) {
        this.onToast(`Request ${requestId} ${action}d successfully.`, 'success');
        this.onRosterMutated();
        await this.load();
      } else {
        this.onToast(data.message || 'Action failed', 'error');
      }
    } catch (e) {
      this.onToast('Network error taking action', 'error');
    }
  }

  render() {
    if (!this.container) return;

    this.container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:16px;max-width:860px;margin:0 auto;padding-bottom:32px;">
        <div>
          <h1 style="margin:0;font-size:22px;font-weight:900;">Leave &amp; Swap Approvals</h1>
          <div style="font-size:12.5px;color:var(--nm-text-muted);margin-top:2px;">Review pending duty leave and shift swap requests from team members.</div>
        </div>

        <div style="display:flex;flex-direction:column;gap:12px;">
          <div style="font-size:13px;font-weight:800;color:var(--nm-text-muted);">
            Pending Requests (${this.pendingRequests.length})
          </div>

          ${this.pendingRequests.length === 0 ? `
            <div class="rx-card" style="text-align:center;padding:32px 16px;color:var(--nm-text-muted);">
              <i class="ph ph-check-circle" style="font-size:36px;color:#10B981;margin-bottom:8px;display:block;"></i>
              <div style="font-weight:700;font-size:14px;color:var(--nm-text);">All Caught Up!</div>
              <div style="font-size:12px;margin-top:4px;">No pending leave or swap requests in the queue.</div>
            </div>
          ` : this.pendingRequests.map((req) => {
            const datesList = req.dates || [];
            const applicant = req.engineer_name || req.engineer_id;
            return `
              <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
                <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                  <div>
                    <div style="font-size:16px;font-weight:900;">${escapeHtml(applicant)}</div>
                    <div style="font-size:11px;color:var(--nm-text-muted);">ID: ${escapeHtml(req.request_id)} &bull; ${escapeHtml(req.engineer_id)}</div>
                  </div>
                  <span style="padding:3px 9px;border-radius:10px;background:rgba(245,158,11,0.15);color:#D97706;font-size:11px;font-weight:800;">Pending</span>
                </div>

                <!-- Dates -->
                <div style="display:flex;gap:6px;flex-wrap:wrap;">
                  ${datesList.map((d) => `
                    <span style="padding:3px 9px;border-radius:8px;background:var(--nm-input-surface);color:var(--nm-accent);font-size:12px;font-weight:800;">
                      ${escapeHtml(d)}
                    </span>
                  `).join('')}
                </div>

                ${req.reason ? `
                  <div style="font-size:12.5px;color:var(--nm-text);font-style:italic;background:var(--nm-input-surface);padding:8px 12px;border-radius:10px;">
                    &ldquo;${escapeHtml(req.reason)}&rdquo;
                  </div>
                ` : ''}

                <div style="font-size:12px;color:var(--nm-text-muted);">
                  Preferred Partner: <strong>${escapeHtml(req.preferred_partner || 'None specified')}</strong>
                </div>

                <!-- Actions -->
                <div style="display:flex;align-items:center;gap:8px;padding-top:6px;border-top:1px solid rgba(148,163,184,0.15);flex-wrap:wrap;">
                  <button type="button" class="rx-btn-primary" data-action="swap" data-req-id="${escapeHtml(req.request_id)}" data-partner="${escapeHtml(req.preferred_partner || '')}" style="padding:8px 14px;font-size:12.5px;border-radius:12px;">
                    <i class="ph ph-arrows-left-right"></i> Approve as Swap
                  </button>
                  <button type="button" class="rx-btn-surface" data-action="leave" data-req-id="${escapeHtml(req.request_id)}" style="padding:8px 14px;font-size:12.5px;border-radius:12px;color:#2563EB;">
                    <i class="ph ph-calendar-check"></i> Approve as Leave
                  </button>
                  <button type="button" class="rx-btn-surface" data-action="reject" data-req-id="${escapeHtml(req.request_id)}" style="padding:8px 14px;font-size:12.5px;border-radius:12px;color:#DC2626;">
                    <i class="ph ph-x-circle"></i> Reject
                  </button>
                </div>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;

    this.container.querySelectorAll('[data-action]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const action = btn.getAttribute('data-action');
        const reqId = btn.getAttribute('data-req-id');
        const partner = btn.getAttribute('data-partner') || '';
        this.takeAction(reqId, action, partner);
      });
    });
  }
}
