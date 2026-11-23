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

import { escapeHtml, getAuthHeaders, getApiBase } from './pwaHelper.js';

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
      capping_enabled: true,
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
      const resp = await fetch(`${getApiBase()}/leave/my-requests`, {
        headers: getAuthHeaders()
      });
      let data = {};
      try { data = await resp.json(); } catch (_) {}
      if (data && data.success) {
        this.myRequests = data.requests || [];
        this.leaveStats = {
          approved_leave_days: data.approved_leave_days || 0,
          remaining_leave_balance: data.remaining_leave_balance ?? 14,
          annual_quota: data.annual_quota || 14,
          capping_enabled: Boolean(data.capping_enabled),
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
      const resp = await fetch(`${getApiBase()}/leave/eligible-partners?date=${encodeURIComponent(dateParam)}`, {
        headers: getAuthHeaders()
      });
      let data = {};
      try { data = await resp.json(); } catch (_) {}
      if (data && data.success) {
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
      const resp = await fetch(`${getApiBase()}/roster/${key}`, { headers: getAuthHeaders() });
      let data = {};
      try { data = await resp.json(); } catch (_) {}
      if (data && data.success) {
        this.rosterCache[key] = data;
        return data;
      }
    } catch (_) {}
    return null;
  }

  toggleDate(dateStr, isOff, isPast) {
    if (isPast) return;
    if (this.selectedDates.has(dateStr)) {
      this.selectedDates.delete(dateStr);
    } else {
      if (this.selectedDates.size >= 5) {
        this.onToast('Maximum 5 days can be selected per request', 'error');
        return;
      }
      this.selectedDates.add(dateStr);
    }
    this.render();
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

      const resp = await fetch(`${getApiBase()}/leave/apply`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders()
        },
        body: JSON.stringify(payload)
      });
      let data = {};
      try {
        data = await resp.json();
      } catch (parseErr) {
        data = { success: false, message: `Server returned HTTP ${resp.status}` };
      }

      if (data && data.success) {
        this.onToast('Leave request submitted successfully!', 'success');
        this.selectedDates.clear();
        this.reason = '';
        this.preferredPartner = '';
        this.activeTab = 'history';
        await this.load();
      } else {
        this.onToast((data && data.message) || 'Failed to submit leave request', 'error');
      }
    } catch (e) {
      console.error('Leave submission error:', e);
      this.onToast(e.message || 'Network error submitting leave request', 'error');
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

      // Check default weekly offs
      const dateObj = new Date(this.calYear, this.calMonth - 1, d);
      const dayName = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][dateObj.getDay()];
      const defaultOffs = Array.isArray(this.user?.default_weekly_offs) ? this.user.default_weekly_offs.map((x) => String(x).toUpperCase()) : [];
      const isDefaultOff = defaultOffs.includes(dayName);

      // Check shift from roster cache if matching month
      let shiftCode = '';
      const curMonthKey = `${this.calYear}-${String(this.calMonth).padStart(2, '0')}`;
      if (rData && (rData.month === curMonthKey || rData.month_key === curMonthKey)) {
        const shifts = rData.shifts || {};
        shiftCode = shifts[selfName]?.[String(d)] || shifts[selfUname]?.[String(d)] || '';
      }
      const isOff = shiftCode ? (shiftCode === 'OFF' || shiftCode.startsWith('SW')) : isDefaultOff;

      let cellClass = 'rx-cal-day-cell';
      let badgeHtml = '';
      if (isPast) {
        cellClass += ' is-past';
      }
      if (isSelected) {
        cellClass += ' is-selected';
      }
      if (isOff) {
        badgeHtml = '<span class="rx-cal-cell-off" style="color:#d97706;font-size:9px;font-weight:700;">OFF</span>';
      } else if (shiftCode) {
        badgeHtml = `<span class="rx-cal-cell-shift">${escapeHtml(shiftCode)}</span>`;
      }

      dayCells.push(`
        <button type="button" class="${cellClass}" data-leave-date="${dStr}" data-is-off="${isOff}" data-is-past="${isPast}" ${isPast ? 'disabled' : ''} aria-label="${dStr} ${isOff ? 'Default Off' : ''}">
          <span class="rx-cal-cell-num">${d}</span>
          ${badgeHtml}
        </button>
      `);
    }

    const selectedCount = this.selectedDates.size;

    this.container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:16px;max-width:560px;margin:0 auto;padding-bottom:32px;">
        
        <!-- Segmented Tab Header (Apply Request ⇄ My History) -->
        <div style="display:flex;align-items:center;background:var(--nm-input-surface);padding:4px;border-radius:16px;box-shadow:var(--nm-shadow-inset-subtle);">
          <button type="button" id="tabBtnApply" style="flex:1;border-radius:12px;padding:9px;font-size:13px;font-weight:800;border:0;cursor:pointer;transition:var(--nm-transition);${this.activeTab === 'apply' ? 'background:var(--nm-accent-gradient);color:#fff;box-shadow:0 4px 12px var(--nm-accent-glow);' : 'background:transparent;color:var(--nm-text-muted);box-shadow:none;'}">
            <i class="ph ph-calendar-plus" style="margin-right:6px;"></i> Apply Request
          </button>
          <button type="button" id="tabBtnHistory" style="flex:1;border-radius:12px;padding:9px;font-size:13px;font-weight:800;border:0;cursor:pointer;transition:var(--nm-transition);${this.activeTab === 'history' ? 'background:var(--nm-accent-gradient);color:#fff;box-shadow:0 4px 12px var(--nm-accent-glow);' : 'background:transparent;color:var(--nm-text-muted);box-shadow:none;'}">
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
                  <div style="font-size:16px;font-weight:900;">Select Leave Dates</div>
                  <div style="font-size:11.5px;color:var(--nm-text-muted);">Pick 1 to 5 days (Default OFF indicated)</div>
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
                  ${this.leaveStats.capping_enabled && this.leaveStats.remaining_leave_balance != null ? `${this.leaveStats.remaining_leave_balance} annual days remaining` : ''}
                </div>
              </div>
            </div>

            <!-- Leave Request Form Card -->
            <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
              <!-- Reason Notes Input -->
              <div>
                <label for="inputLeaveReason" style="display:block;font-size:13.5px;font-weight:800;margin-bottom:3px;">
                  Reason / Operational Notes <span style="font-size:11px;font-weight:400;color:var(--nm-text-muted);">(Optional)</span>
                </label>
                <textarea id="inputLeaveReason" rows="2" class="rx-well" placeholder="Brief note for shift supervisor..." style="width:100%;border:0;padding:10px 12px;font-size:13px;font-family:inherit;color:var(--nm-text);box-sizing:border-box;resize:vertical;">${escapeHtml(this.reason)}</textarea>
              </div>

              <!-- Submit Button -->
              <button type="button" id="btnSubmitLeave" class="rx-btn-primary" style="width:100%;padding:14px;font-size:14px;border-radius:16px;" ${selectedCount === 0 || this.isSubmitting ? 'disabled' : ''}>
                <i class="ph ph-paper-plane-tilt" style="font-size:17px;"></i>
                <span>Submit Leave Request</span>
              </button>
            </div>
          </div>
        ` : `
          <!-- MY HISTORY VIEW -->
          <div style="display:flex;flex-direction:column;gap:14px;">
            
            <!-- Annual Leave Quota Card -->
            <div class="rx-card" style="display:flex;align-items:center;justify-content:space-between;gap:12px;background:var(--nm-surface);">
              <div>
                <div style="font-size:11px;font-weight:900;color:var(--nm-accent);letter-spacing:0.05em;">ANNUAL LEAVE</div>
                <div style="font-size:18px;font-weight:900;margin-top:2px;">
                  ${this.leaveStats.capping_enabled && this.leaveStats.remaining_leave_balance != null && this.leaveStats.annual_quota != null ? `${this.leaveStats.remaining_leave_balance} of ${this.leaveStats.annual_quota} Days Remaining` : 'Leave Requests'}
                </div>
                ${this.leaveStats.capping_enabled ? `
                  <div style="font-size:11.5px;color:var(--nm-text-muted);margin-top:2px;">
                    ${this.leaveStats.approved_leave_days} days approved in ${this.calYear}
                  </div>
                ` : `
                  <div style="font-size:11.5px;color:var(--nm-text-muted);margin-top:2px;">
                    Submit and track your annual leave requests
                  </div>
                `}
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
                  <div style="font-size:12px;margin-top:4px;">When you apply for duty leave, your submissions will appear here.</div>
                </div>
              ` : this.myRequests.map((req) => {
                const st = String(req.status || 'pending').toLowerCase();
                let statusBadge = '';
                if (st === 'pending') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(245,158,11,0.15);color:#D97706;font-size:11px;font-weight:800;">Pending Review</span>';
                } else if (st === 'approved_swap') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(16,185,129,0.15);color:#059669;font-size:11px;font-weight:800;">Approved</span>';
                } else if (st === 'approved_leave') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(59,130,246,0.15);color:#2563EB;font-size:11px;font-weight:800;">Approved</span>';
                } else if (st === 'rejected') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(220,38,38,0.15);color:#DC2626;font-size:11px;font-weight:800;">Declined</span>';
                } else if (st === 'revoked') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(148,163,184,0.2);color:var(--nm-text-muted);font-size:11px;font-weight:800;">Revoked</span>';
                } else if (st === 'cancelled') {
                  statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(148,163,184,0.2);color:var(--nm-text-muted);font-size:11px;font-weight:800;">Cancelled</span>';
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

                    ${req.approval?.admin_note ? `
                      <div style="font-size:11.5px;color:${st === 'rejected' ? '#DC2626' : 'var(--nm-accent)'};font-weight:700;">
                        Admin Note: ${escapeHtml(req.approval.admin_note)}
                      </div>
                    ` : ''}

                    <div style="display:flex;align-items:center;justify-content:flex-end;gap:8px;font-size:11px;color:var(--nm-text-muted);padding-top:4px;border-top:1px solid rgba(148,163,184,0.1);">
                      <div>Submitted ${escapeHtml(String(req.created_at || '').slice(0, 10))}</div>
                    </div>

                    ${st === 'pending' ? `
                      <div style="display:flex;justify-content:flex-end;padding-top:4px;">
                        <button type="button" class="btn-cancel-leave" data-req-id="${escapeHtml(req.request_id)}" style="background:none;border:1px solid #DC2626;color:#DC2626;border-radius:8px;padding:3px 10px;font-size:11px;font-weight:700;cursor:pointer;">
                          Cancel Request
                        </button>
                      </div>
                    ` : ''}
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

    this.container.querySelectorAll('.btn-cancel-leave').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const reqId = btn.getAttribute('data-req-id');
        if (!confirm(`Are you sure you want to cancel leave request ${reqId}?`)) return;
        try {
          const resp = await fetch('/api/leave/cancel', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
            body: JSON.stringify({ request_id: reqId })
          });
          const data = await resp.json();
          if (data.success) {
            this.onToast('Leave request cancelled successfully', 'success');
            await this.load();
          } else {
            this.onToast(data.message || 'Failed cancelling request', 'error');
          }
        } catch (e) {
          this.onToast('Network error cancelling request', 'error');
        }
      });
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

    this.activeTab = 'pending'; // 'pending' | 'audit'
    this.pendingRequests = [];
    this.historicalRequests = [];
    this.policySettings = { capping_enabled: true, annual_quota: 14 };
    this.showSettingsModal = false;
    this.isLoading = false;
  }

  async load() {
    this.isLoading = true;
    try {
      const [queueResp, settingsResp] = await Promise.all([
        fetch(`${getApiBase()}/leave/admin-queue`, { headers: getAuthHeaders() }),
        fetch(`${getApiBase()}/leave/settings`, { headers: getAuthHeaders() })
      ]);
      let queueData = {}, settingsData = {};
      try { queueData = await queueResp.json(); } catch (_) {}
      try { settingsData = await settingsResp.json(); } catch (_) {}

      if (queueData && queueData.success) {
        this.pendingRequests = queueData.pending_requests || [];
        this.historicalRequests = queueData.historical_requests || [];
      } else {
        this.onToast((queueData && queueData.message) || 'Failed loading approvals queue', 'error');
      }

      if (settingsData && settingsData.success) {
        this.policySettings = {
          capping_enabled: settingsData.capping_enabled ?? true,
          annual_quota: settingsData.annual_quota || 14
        };
      }
    } catch (e) {
      console.warn('Queue loading error:', e);
      this.onToast('Network error loading approvals queue', 'error');
    } finally {
      this.isLoading = false;
      this.render();
    }
  }

  async takeAction(requestId, action, adminNote = '') {
    try {
      const resp = await fetch(`${getApiBase()}/leave/admin-action`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify({
          request_id: requestId,
          action,
          admin_note: adminNote
        })
      });
      let data = {};
      try { data = await resp.json(); } catch (_) {}
      if (data && data.success) {
        const actionLabel = action === 'leave' ? 'approved' : action === 'reject' ? 'rejected' : action === 'revoke' ? 'revoked' : action;
        this.onToast(`Request ${requestId} ${actionLabel} successfully.`, 'success');
        this.onRosterMutated();
        await this.load();
      } else {
        this.onToast((data && data.message) || 'Action failed', 'error');
      }
    } catch (e) {
      this.onToast('Network error taking action', 'error');
    }
  }

  async savePolicySettings(cappingEnabled, annualQuota) {
    try {
      const resp = await fetch(`${getApiBase()}/leave/settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
        body: JSON.stringify({
          capping_enabled: cappingEnabled,
          annual_quota: annualQuota
        })
      });
      let data = {};
      try { data = await resp.json(); } catch (_) {}
      if (data && data.success) {
        this.policySettings.capping_enabled = data.capping_enabled;
        this.policySettings.annual_quota = data.annual_quota;
        this.showSettingsModal = false;
        this.onToast('Leave policy settings updated successfully.', 'success');
        this.render();
      } else {
        this.onToast(data.message || 'Failed updating settings', 'error');
      }
    } catch (e) {
      this.onToast('Network error updating settings', 'error');
    }
  }

  render() {
    if (!this.container) return;

    this.container.innerHTML = `
      <div style="display:flex;flex-direction:column;gap:16px;max-width:860px;margin:0 auto;padding-bottom:32px;">
        <!-- Header with Settings Button -->
        <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;">
          <div>
            <h1 style="margin:0;font-size:22px;font-weight:900;">Leave Approvals</h1>
            <div style="font-size:12.5px;color:var(--nm-text-muted);margin-top:2px;">Review pending duty leave requests &amp; policy controls</div>
          </div>
          <button type="button" id="btnLeaveSettings" class="rx-btn-surface" style="display:flex;align-items:center;gap:6px;padding:8px 14px;border-radius:12px;font-size:12.5px;font-weight:700;cursor:pointer;">
            <i class="ph ph-gear" style="font-size:16px;"></i>
            <span>Leave Policy</span>
          </button>
        </div>

        <!-- Segmented Tab Switcher (Pending Queue ⇄ Audit History) -->
        <div style="display:grid;grid-template-columns:1fr 1fr;background:var(--nm-input-surface);padding:4px;border-radius:16px;gap:4px;">
          <button type="button" id="tabBtnPending" style="padding:10px;border-radius:12px;border:0;font-size:13px;font-weight:800;cursor:pointer;transition:all 0.2s ease;${this.activeTab === 'pending' ? 'background:var(--nm-surface);color:var(--nm-accent);box-shadow:var(--nm-shadow-raised-subtle);' : 'background:transparent;color:var(--nm-text-muted);'}">
            Pending Queue (${this.pendingRequests.length})
          </button>
          <button type="button" id="tabBtnAudit" style="padding:10px;border-radius:12px;border:0;font-size:13px;font-weight:800;cursor:pointer;transition:all 0.2s ease;${this.activeTab === 'audit' ? 'background:var(--nm-surface);color:var(--nm-accent);box-shadow:var(--nm-shadow-raised-subtle);' : 'background:transparent;color:var(--nm-text-muted);'}">
            Audit History (${this.historicalRequests.length})
          </button>
        </div>

        ${this.activeTab === 'pending' ? `
          <!-- PENDING QUEUE -->
          <div style="display:flex;flex-direction:column;gap:12px;">
            ${this.pendingRequests.length === 0 ? `
              <div class="rx-card" style="text-align:center;padding:36px 16px;color:var(--nm-text-muted);">
                <i class="ph ph-check-circle" style="font-size:40px;color:#10B981;margin-bottom:8px;display:block;"></i>
                <div style="font-weight:800;font-size:15px;color:var(--nm-text);">All Caught Up!</div>
                <div style="font-size:12px;margin-top:4px;">No pending leave requests in the queue.</div>
              </div>
            ` : this.pendingRequests.map((req) => {
              const datesList = req.dates || [];
              const applicant = req.engineer_name || req.engineer_id;
              return `
                <div class="rx-card" style="display:flex;flex-direction:column;gap:12px;">
                  <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                    <div>
                      <div style="font-size:16px;font-weight:900;">${escapeHtml(applicant)}</div>
                      <div style="font-size:11px;color:var(--nm-text-muted);margin-top:1px;">ID: ${escapeHtml(req.request_id)} &bull; @${escapeHtml(req.engineer_id)}</div>
                    </div>
                    <span style="padding:3px 9px;border-radius:10px;background:rgba(245,158,11,0.15);color:#D97706;font-size:11px;font-weight:800;">Pending</span>
                  </div>

                  <!-- Dates Badges -->
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

                  <!-- Actions with Confirmation -->
                  <div style="display:flex;align-items:center;gap:8px;padding-top:6px;border-top:1px solid rgba(148,163,184,0.15);flex-wrap:wrap;">
                    <button type="button" class="rx-btn-primary btn-action-approve" data-req-id="${escapeHtml(req.request_id)}" data-applicant="${escapeHtml(applicant)}" style="padding:8px 18px;font-size:12.5px;border-radius:12px;background:#10B981;border:0;cursor:pointer;">
                      <i class="ph ph-check-circle"></i> Approve Leave
                    </button>
                    <button type="button" class="rx-btn-surface btn-action-reject" data-req-id="${escapeHtml(req.request_id)}" data-applicant="${escapeHtml(applicant)}" style="padding:8px 18px;font-size:12.5px;border-radius:12px;color:#DC2626;cursor:pointer;">
                      <i class="ph ph-x-circle"></i> Reject
                    </button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        ` : `
          <!-- AUDIT HISTORY -->
          <div style="display:flex;flex-direction:column;gap:12px;">
            ${this.historicalRequests.length === 0 ? `
              <div class="rx-card" style="text-align:center;padding:36px 16px;color:var(--nm-text-muted);">
                <i class="ph ph-clock-counter-clockwise" style="font-size:40px;margin-bottom:8px;display:block;"></i>
                <div style="font-weight:800;font-size:15px;color:var(--nm-text);">No Historical Decisions</div>
                <div style="font-size:12px;margin-top:4px;">Actioned leave requests and audit logs will appear here.</div>
              </div>
            ` : this.historicalRequests.map((req) => {
              const datesList = req.dates || [];
              const applicant = req.engineer_name || req.engineer_id;
              const st = String(req.status || '').toLowerCase();
              let statusBadge = '';
              const isApproved = st === 'approved_leave' || st === 'approved' || st === 'approved_swap';
              if (isApproved) {
                statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(16,185,129,0.15);color:#059669;font-size:11px;font-weight:800;">Approved</span>';
              } else if (st === 'rejected') {
                statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(220,38,38,0.15);color:#DC2626;font-size:11px;font-weight:800;">Declined</span>';
              } else if (st === 'revoked') {
                statusBadge = '<span style="padding:3px 9px;border-radius:10px;background:rgba(148,163,184,0.2);color:var(--nm-text-muted);font-size:11px;font-weight:800;">Revoked</span>';
              } else {
                statusBadge = `<span style="padding:3px 9px;border-radius:10px;background:rgba(148,163,184,0.2);color:var(--nm-text-muted);font-size:11px;font-weight:800;">${escapeHtml(st)}</span>`;
              }

              const app = req.approval || req.decision || {};
              const actionedBy = app.approved_name || app.approved_by_name || app.approved_by || app.rejected_by || 'Admin';
              const actionedAt = app.approved_at || app.timestamp || req.created_at || '';
              const decisionInfo = `Actioned by ${escapeHtml(actionedBy)}`;

              return `
                <div class="rx-card-subtle" style="display:flex;flex-direction:column;gap:10px;padding:16px;">
                  <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                    <div>
                      <div style="font-size:15px;font-weight:800;">${escapeHtml(applicant)}</div>
                      <div style="font-size:11px;color:var(--nm-text-muted);">ID: ${escapeHtml(req.request_id)}</div>
                    </div>
                    ${statusBadge}
                  </div>

                  <!-- Dates Badges -->
                  <div style="display:flex;gap:6px;flex-wrap:wrap;">
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

                  ${app.admin_note ? `
                    <div style="font-size:11.5px;color:${st === 'rejected' ? '#DC2626' : 'var(--nm-accent)'};font-weight:700;">
                      Admin Note: ${escapeHtml(app.admin_note)}
                    </div>
                  ` : ''}

                  <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;font-size:11px;color:var(--nm-text-muted);padding-top:4px;border-top:1px solid rgba(148,163,184,0.1);">
                    <div>${decisionInfo}</div>
                    <div>${escapeHtml(String(actionedAt).slice(0, 10))}</div>
                  </div>

                  ${isApproved ? `
                    <div style="display:flex;justify-content:flex-end;padding-top:4px;">
                      <button type="button" class="btn-action-revoke rx-btn-surface" data-req-id="${escapeHtml(req.request_id)}" data-applicant="${escapeHtml(applicant)}" style="padding:6px 14px;font-size:11.5px;font-weight:700;color:#DC2626;border-radius:10px;cursor:pointer;">
                        <i class="ph ph-arrow-counter-clockwise"></i> Revoke Leave
                      </button>
                    </div>
                  ` : ''}
                </div>
              `;
            }).join('')}
          </div>
        `}

        <!-- Settings Modal -->
        ${this.showSettingsModal ? `
          <div id="leaveSettingsModalBackdrop" style="position:fixed;inset:0;background:rgba(0,0,0,0.55);backdrop-filter:blur(4px);z-index:999;display:flex;align-items:center;justify-content:center;padding:16px;">
            <div class="rx-card" style="width:100%;max-width:440px;display:flex;flex-direction:column;gap:16px;box-shadow:var(--nm-shadow-floating);">
              <div style="display:flex;align-items:center;justify-content:space-between;">
                <h3 style="margin:0;font-size:17px;font-weight:900;">Annual Leave Policy</h3>
                <button type="button" id="btnCloseLeaveSettings" style="background:none;border:0;color:var(--nm-text-muted);font-size:20px;cursor:pointer;">&times;</button>
              </div>

              <div style="font-size:12.5px;color:var(--nm-text-muted);line-height:1.4;">
                Configure whether annual leave capping is strictly enforced or open for unrestricted requests.
              </div>

              <!-- Capping Toggle -->
              <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px;background:var(--nm-input-surface);border-radius:14px;">
                <div>
                  <div style="font-size:13px;font-weight:800;">Enforce Annual Leave Capping</div>
                  <div style="font-size:11px;color:var(--nm-text-muted);">If disabled, no quota cap applies to engineers.</div>
                </div>
                <input type="checkbox" id="chkCappingEnabled" ${this.policySettings.capping_enabled ? 'checked' : ''} style="width:20px;height:20px;accent-color:var(--nm-accent);cursor:pointer;" />
              </div>

              <!-- Quota Days Field -->
              <div id="grpQuotaDays" style="${this.policySettings.capping_enabled ? '' : 'opacity:0.5;pointer-events:none;'}">
                <label for="inputAnnualQuota" style="display:block;font-size:12.5px;font-weight:800;margin-bottom:4px;">
                  Annual Allowance (Days per Year)
                </label>
                <input type="number" id="inputAnnualQuota" min="1" max="365" value="${this.policySettings.annual_quota || 14}" class="rx-well" style="width:100%;padding:10px 12px;border:0;font-size:13px;font-family:inherit;color:var(--nm-text);box-sizing:border-box;border-radius:10px;" />
              </div>

              <!-- Modal Actions -->
              <div style="display:flex;justify-content:flex-end;gap:8px;padding-top:6px;">
                <button type="button" id="btnCancelLeaveSettings" class="rx-btn-surface" style="padding:8px 16px;border-radius:12px;font-size:12.5px;font-weight:700;cursor:pointer;">
                  Cancel
                </button>
                <button type="button" id="btnSaveLeaveSettings" class="rx-btn-primary" style="padding:8px 18px;border-radius:12px;font-size:12.5px;font-weight:700;cursor:pointer;">
                  Save Policy
                </button>
              </div>
            </div>
          </div>
        ` : ''}
      </div>
    `;

    // Event Bindings
    this.container.querySelector('#tabBtnPending')?.addEventListener('click', () => {
      this.activeTab = 'pending';
      this.render();
    });
    this.container.querySelector('#tabBtnAudit')?.addEventListener('click', () => {
      this.activeTab = 'audit';
      this.render();
    });

    this.container.querySelector('#btnLeaveSettings')?.addEventListener('click', () => {
      this.showSettingsModal = true;
      this.render();
    });

    this.container.querySelector('#btnCloseLeaveSettings')?.addEventListener('click', () => {
      this.showSettingsModal = false;
      this.render();
    });
    this.container.querySelector('#btnCancelLeaveSettings')?.addEventListener('click', () => {
      this.showSettingsModal = false;
      this.render();
    });

    this.container.querySelector('#chkCappingEnabled')?.addEventListener('change', (e) => {
      const isChecked = e.target.checked;
      const grp = this.container.querySelector('#grpQuotaDays');
      if (grp) {
        grp.style.opacity = isChecked ? '1' : '0.5';
        grp.style.pointerEvents = isChecked ? 'auto' : 'none';
      }
    });

    this.container.querySelector('#btnSaveLeaveSettings')?.addEventListener('click', () => {
      const chk = this.container.querySelector('#chkCappingEnabled');
      const inputQuota = this.container.querySelector('#inputAnnualQuota');
      const capping = chk ? chk.checked : true;
      const quota = inputQuota ? parseInt(inputQuota.value, 10) || 14 : 14;

      if (!confirm('Are you sure you want to update the global leave policy settings?')) return;
      this.savePolicySettings(capping, quota);
    });

    // Approve Leave
    this.container.querySelectorAll('.btn-action-approve').forEach((btn) => {
      btn.addEventListener('click', () => {
        const reqId = btn.getAttribute('data-req-id');
        const applicant = btn.getAttribute('data-applicant') || 'this engineer';
        if (!confirm(`Are you sure you want to approve leave for ${applicant} (Request ${reqId})?`)) return;
        this.takeAction(reqId, 'leave');
      });
    });

    // Reject Leave
    this.container.querySelectorAll('.btn-action-reject').forEach((btn) => {
      btn.addEventListener('click', () => {
        const reqId = btn.getAttribute('data-req-id');
        const applicant = btn.getAttribute('data-applicant') || 'this engineer';
        const note = prompt(`Reject leave request ${reqId} for ${applicant}?\n\nEnter reason / review comment (optional):`, '');
        if (note === null) return;
        this.takeAction(reqId, 'reject', note.trim());
      });
    });

    // Revoke Leave
    this.container.querySelectorAll('.btn-action-revoke').forEach((btn) => {
      btn.addEventListener('click', () => {
        const reqId = btn.getAttribute('data-req-id');
        const applicant = btn.getAttribute('data-applicant') || 'this engineer';
        if (!confirm(`Are you sure you want to revoke approved leave for ${applicant} (Request ${reqId})?\n\nThis will restore the shift in the active roster and refund the engineer's annual leave balance.`)) return;
        this.takeAction(reqId, 'revoke');
      });
    });
  }
}
