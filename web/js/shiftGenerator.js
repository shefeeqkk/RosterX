/**
 * RosterX — Shift Roster Generator Controller (shiftGenerator.js)
 * Dedicated helper modal for automated Night Shift (NS) and Baseline (OFF, L, Swaps) scheduling.
 * 
 * Rules Enforced:
 * 1. 24/7 Night Coverage: Exactly 1 engineer on NS per calendar day.
 * 2. Consecutive blocks: 1 to 5 nights maximum per engineer.
 * 3. Inviolable baseline: OFF, Approved Leaves (L), and Swaps are preserved as locked fixtures.
 * 4. Per-engineer settings:
 *    - Flexible: 1 to 5 day blocks based on fair rotation.
 *    - Fixed 5 Nights: Guaranteed 5 nights for engineers with specific monthly quota requests.
 *    - Excluded: Never assigned night shifts (0 NS).
 * 5. Admin Web only: Previews generated schedule with 1-click apply to active grid.
 */

import { monthLabel, daysInMonth } from './dateUtils.js?v=2.5.18';

export class ShiftGeneratorController {
  constructor(app) {
    this.app = app;
    this.dom = {};
    this.currentGeneratedResult = null;
    this.engineerRules = {}; // uname -> { mode: 'flexible' | 'fixed_5' | 'excluded', fixed_nights: 5, preferred_start_day: null }
  }

  init() {
    this.cacheDom();
    this.bindEvents();
  }

  cacheDom() {
    this.dom = {
      modal: document.getElementById('shiftGeneratorModal'),
      btnClose: document.getElementById('btnCloseShiftGeneratorModal'),
      btnCancel: document.getElementById('btnCancelShiftGenerator'),
      btnTriggerHeader: document.getElementById('btnOpenShiftGeneratorModal'),
      btnTriggerToolbar: document.getElementById('btnToolbarShiftGenerator'),
      monthSelect: document.getElementById('sgMonthSelect'),
      engineersListContainer: document.getElementById('sgEngineersListContainer'),
      btnPreFillOnly: document.getElementById('btnPreFillOnly'),
      btnRun: document.getElementById('btnRunShiftGenerator'),
      btnApply: document.getElementById('btnApplyShiftGenerator'),
      previewSection: document.getElementById('sgPreviewSection'),
      statsBadges: document.getElementById('sgStatsBadges'),
      previewGridContainer: document.getElementById('sgPreviewGridContainer'),
    };
  }

  bindEvents() {
    if (this.dom.btnTriggerHeader) {
      this.dom.btnTriggerHeader.addEventListener('click', () => this.openModal());
    }
    if (this.dom.btnTriggerToolbar) {
      this.dom.btnTriggerToolbar.addEventListener('click', () => this.openModal());
    }
    if (this.dom.btnClose) {
      this.dom.btnClose.addEventListener('click', () => this.closeModal());
    }
    if (this.dom.btnCancel) {
      this.dom.btnCancel.addEventListener('click', () => this.closeModal());
    }
    if (this.dom.monthSelect) {
      this.dom.monthSelect.addEventListener('change', () => this.onMonthChange());
    }
    if (this.dom.btnPreFillOnly) {
      this.dom.btnPreFillOnly.addEventListener('click', () => this.generateDraft(true));
    }
    if (this.dom.btnRun) {
      this.dom.btnRun.addEventListener('click', () => this.generateDraft(false));
    }
    if (this.dom.btnApply) {
      this.dom.btnApply.addEventListener('click', () => this.applyToActiveGrid());
    }
  }

  openModal() {
    if (!this.dom.modal) return;
    this.populateMonthOptions();
    this.populateEngineersList();
    this.dom.modal.classList.remove('hidden');
    // Hide preview on fresh open
    if (this.dom.previewSection) {
      this.dom.previewSection.classList.add('hidden');
    }
    if (this.dom.btnApply) {
      this.dom.btnApply.classList.add('hidden');
    }
  }

  closeModal() {
    if (!this.dom.modal) return;
    this.dom.modal.classList.add('hidden');
  }

  populateMonthOptions() {
    if (!this.dom.monthSelect) return;
    this.dom.monthSelect.innerHTML = '';

    const currentYear = this.app.state.currentYear || new Date().getFullYear();
    const currentMonth = this.app.state.currentMonth || new Date().getMonth() + 1;
    const currentKey = `${currentYear}-${String(currentMonth).padStart(2, '0')}`;

    // Provide past 1 month, current month, and next 6 months
    const options = [];
    const baseDate = new Date(currentYear, currentMonth - 2, 1);
    for (let i = 0; i < 8; i++) {
      const d = new Date(baseDate.getFullYear(), baseDate.getMonth() + i, 1);
      const y = d.getFullYear();
      const m = d.getMonth() + 1;
      const key = `${y}-${String(m).padStart(2, '0')}`;
      options.push({
        key,
        label: monthLabel(key, 'long')
      });
    }

    options.forEach(opt => {
      const optEl = document.createElement('option');
      optEl.value = opt.key;
      optEl.textContent = opt.label;
      if (opt.key === currentKey) {
        optEl.selected = true;
      }
      this.dom.monthSelect.appendChild(optEl);
    });
  }

  onMonthChange() {
    // Reset preview on month switch
    if (this.dom.previewSection) {
      this.dom.previewSection.classList.add('hidden');
    }
    if (this.dom.btnApply) {
      this.dom.btnApply.classList.add('hidden');
    }
    this.populateEngineersList();
  }

  getEligibleEngineers() {
    const list = this.app.state.engineersList || [];
    return list.filter(eng => {
      const role = String(eng.role || '').toLowerCase();
      const status = String(eng.status || '').toLowerCase();
      if (role === 'admin' || role === 'manager') return false;
      if (status === 'resigned') return false;
      if (eng.include_in_roster === false) return false;
      return true;
    });
  }

  populateEngineersList() {
    if (!this.dom.engineersListContainer) return;
    const engineers = this.getEligibleEngineers();

    if (!engineers.length) {
      this.dom.engineersListContainer.innerHTML = `
        <div class="p-4 text-center text-xs text-slate-400">
          No eligible active engineers found.
        </div>
      `;
      return;
    }

    this.dom.engineersListContainer.innerHTML = engineers.map(eng => {
      const uname = String(eng.username || eng.id || '').trim().toLowerCase();
      const name = eng.name || eng.full_name || uname;
      const rawOffs = (Array.isArray(eng.default_weekly_offs) && eng.default_weekly_offs.length > 0)
        ? eng.default_weekly_offs
        : ((typeof this.app.getEngineerWeekOffs === 'function')
            ? this.app.getEngineerWeekOffs(eng)
            : []);
      const offStr = rawOffs.map(x => String(x).slice(0, 3)).join(', ') || 'None';

      const currentRule = this.engineerRules[uname] || { mode: 'flexible' };
      const isFlex = currentRule.mode === 'flexible' || !currentRule.mode;
      const isFixed5 = currentRule.mode === 'continuous_5' || currentRule.mode === 'fixed_5';
      const isExcluded = currentRule.mode === 'excluded';
      const maxBlock = currentRule.max_consecutive_nights || 5;

      return `
        <div class="p-3 sm:px-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition" data-uname="${uname}">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center font-bold text-xs text-slate-700 dark:text-slate-300">
              ${name.charAt(0).toUpperCase()}
            </div>
            <div>
              <div class="font-bold text-xs text-slate-900 dark:text-white flex items-center gap-1.5">
                ${name}
                <span class="text-[10px] font-mono text-slate-400">@${uname}</span>
              </div>
              <div class="text-[11px] text-slate-500 dark:text-slate-400 flex items-center gap-1">
                <span>Offs:</span>
                <span class="font-semibold text-red-500 dark:text-red-400">${offStr}</span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2 flex-wrap">
            <!-- Mode Radio Pills -->
            <div class="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-0.5 border border-slate-200 dark:border-slate-700 text-[11px]">
              <button type="button" class="sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition ${isFlex ? 'bg-indigo-600 text-white shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'}" data-uname="${uname}" data-mode="flexible">
                Flexible
              </button>
              <button type="button" class="sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition ${isFixed5 ? 'bg-amber-500 text-slate-950 shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'}" data-uname="${uname}" data-mode="continuous_5" title="Guaranteed 5 continuous nights allocation">
                ⭐ Continuous 5
              </button>
              <button type="button" class="sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition ${isExcluded ? 'bg-red-600 text-white shadow-xs' : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'}" data-uname="${uname}" data-mode="excluded" title="Zero night shifts assigned">
                🚫 Excluded
              </button>
            </div>

            <!-- Block Limit / Split Selector -->
            <div class="sg-block-wrap ${isExcluded ? 'hidden' : 'flex'} items-center gap-1" id="sgBlockWrap_${uname}">
              <span class="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Block:</span>
              <select class="sg-block-select text-[11px] font-bold bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-2 py-1 text-slate-800 dark:text-slate-200 shadow-2xs" data-uname="${uname}" title="Max consecutive nights / split preference">
                <option value="5" ${maxBlock === 5 ? 'selected' : ''}>1–5 Nights (Normal)</option>
                <option value="3" ${maxBlock === 3 ? 'selected' : ''}>Max 3 (Split)</option>
                <option value="2" ${maxBlock === 2 ? 'selected' : ''}>Max 2 (Split)</option>
                <option value="1" ${maxBlock === 1 ? 'selected' : ''}>Single (1 Night Only)</option>
              </select>
            </div>

            <!-- Optional preferred start day for continuous_5 -->
            <div class="sg-start-day-wrap ${isFixed5 ? 'flex' : 'hidden'} items-center gap-1" id="sgStartWrap_${uname}">
              <span class="text-[10px] text-slate-500 font-bold">Start:</span>
              <input type="number" min="1" max="31" placeholder="Auto" value="${currentRule.preferred_start_day || ''}" class="sg-start-input w-12 px-1.5 py-1 text-[11px] font-mono rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-center" data-uname="${uname}" title="Preferred starting day (1-31, optional)" />
            </div>
          </div>
        </div>
      `;
    }).join('');

    // Attach event listeners to mode buttons
    this.dom.engineersListContainer.querySelectorAll('.sg-mode-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const uname = btn.dataset.uname;
        const mode = btn.dataset.mode;
        this.setEngineerMode(uname, mode);
      });
    });

    // Attach event listeners to block select dropdowns
    this.dom.engineersListContainer.querySelectorAll('.sg-block-select').forEach(sel => {
      sel.addEventListener('change', (e) => {
        const uname = sel.dataset.uname;
        const val = parseInt(sel.value, 10);
        if (!this.engineerRules[uname]) {
          this.engineerRules[uname] = { mode: 'flexible' };
        }
        this.engineerRules[uname].max_consecutive_nights = (!isNaN(val) && val >= 1 && val <= 5) ? val : 5;
      });
    });

    this.dom.engineersListContainer.querySelectorAll('.sg-start-input').forEach(inp => {
      inp.addEventListener('change', (e) => {
        const uname = inp.dataset.uname;
        const val = parseInt(inp.value, 10);
        if (!this.engineerRules[uname]) {
          this.engineerRules[uname] = { mode: 'continuous_5' };
        }
        this.engineerRules[uname].preferred_start_day = (!isNaN(val) && val >= 1 && val <= 31) ? val : null;
      });
    });
  }

  setEngineerMode(uname, mode) {
    if (!this.engineerRules[uname]) {
      this.engineerRules[uname] = {};
    }
    this.engineerRules[uname].mode = mode;
    if (mode === 'continuous_5' || mode === 'fixed_5') {
      this.engineerRules[uname].fixed_nights = 5;
    }

    const row = this.dom.engineersListContainer.querySelector(`[data-uname="${uname}"]`);
    if (row) {
      row.querySelectorAll('.sg-mode-btn').forEach(b => {
        const bMode = b.dataset.mode;
        const isSelected = (bMode === mode) ||
          ((mode === 'continuous_5' || mode === 'fixed_5') && (bMode === 'continuous_5' || bMode === 'fixed_5'));
        if (isSelected) {
          if (bMode === 'flexible') {
            b.className = 'sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition bg-indigo-600 text-white shadow-xs';
          } else if (bMode === 'continuous_5' || bMode === 'fixed_5') {
            b.className = 'sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition bg-amber-500 text-slate-950 shadow-xs';
          } else if (bMode === 'excluded') {
            b.className = 'sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition bg-red-600 text-white shadow-xs';
          }
        } else {
          b.className = 'sg-mode-btn px-2.5 py-1 rounded-lg font-bold transition text-slate-600 dark:text-slate-400 hover:text-slate-900';
        }
      });

      const blockWrap = row.querySelector(`#sgBlockWrap_${uname}`);
      if (blockWrap) {
        if (mode === 'excluded') {
          blockWrap.classList.add('hidden');
          blockWrap.classList.remove('flex');
        } else {
          blockWrap.classList.remove('hidden');
          blockWrap.classList.add('flex');
        }
      }

      const startWrap = row.querySelector(`#sgStartWrap_${uname}`);
      if (startWrap) {
        if (mode === 'continuous_5' || mode === 'fixed_5') {
          startWrap.classList.remove('hidden');
          startWrap.classList.add('flex');
        } else {
          startWrap.classList.add('hidden');
          startWrap.classList.remove('flex');
        }
      }
    }
  }

  async generateDraft(baselineOnly = false) {
    const selectedMonth = this.dom.monthSelect ? this.dom.monthSelect.value : null;
    if (!selectedMonth) {
      if (typeof this.app.showToast === 'function') {
        this.app.showToast('Please select a target month', 'error');
      }
      return;
    }

    if (baselineOnly && this.dom.btnPreFillOnly) {
      this.dom.btnPreFillOnly.disabled = true;
      this.dom.btnPreFillOnly.innerHTML = `<i class="ph ph-spinner animate-spin"></i> Pre-filling...`;
    } else if (this.dom.btnRun) {
      this.dom.btnRun.disabled = true;
      this.dom.btnRun.innerHTML = `<i class="ph ph-spinner animate-spin"></i> Solving 24/7 Coverage...`;
    }

    try {
      let headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      };
      if (this.app.atlasService && typeof this.app.atlasService.getAuthHeaders === 'function') {
        headers = this.app.atlasService.getAuthHeaders({ 'Content-Type': 'application/json' });
      } else {
        const token = (typeof localStorage !== 'undefined' && (localStorage.getItem('rosterx_auth_token') || localStorage.getItem('rosterx_token'))) ||
                      (typeof sessionStorage !== 'undefined' && (sessionStorage.getItem('rosterx_auth_token') || sessionStorage.getItem('rosterx_token'))) || '';
        if (token) headers['Authorization'] = `Bearer ${token}`;
      }

      const apiBase = (this.app.atlasService && typeof this.app.atlasService.getApiBase === 'function')
        ? this.app.atlasService.getApiBase()
        : '/api';
      const endpoint = `${apiBase.replace(/\/+$/, '')}/roster/generate-night-shifts`;

      const resp = await fetch(endpoint, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          month: selectedMonth,
          engineer_rules: this.engineerRules,
          baseline_only: Boolean(baselineOnly),
          post_night_rest_days: (this.app.state.constraints && this.app.state.constraints.postNightRestDays) ? this.app.state.constraints.postNightRestDays : 2
        })
      });

      const result = await resp.json();
      if (!resp.ok || !result.success) {
        throw new Error(result.message || 'Solver failed to satisfy all night coverage constraints.');
      }

      this.currentGeneratedResult = result;
      this.renderPreview(result);

      if (typeof this.app.showToast === 'function') {
        if (baselineOnly) {
          this.app.showToast(`Baseline ready! Offs, Leaves & Swaps anchored for ${selectedMonth}.`);
        } else {
          this.app.showToast(`Full draft generated! 100% night coverage solved for ${selectedMonth}.`);
        }
      }
    } catch (err) {
      console.error('[ShiftGenerator] Error:', err);
      if (typeof this.app.showToast === 'function') {
        this.app.showToast(`Generation failed: ${err.message}`, 'error');
      }
      alert(`Shift Generator Error: ${err.message}\n\nPlease verify that enough non-excluded engineers are free on each day.`);
    } finally {
      if (this.dom.btnPreFillOnly) {
        this.dom.btnPreFillOnly.disabled = false;
        this.dom.btnPreFillOnly.innerHTML = `<i class="ph ph-calendar-check text-sm font-bold"></i> <span>Pre-Fill Baseline Only</span>`;
      }
      if (this.dom.btnRun) {
        this.dom.btnRun.disabled = false;
        this.dom.btnRun.innerHTML = `<i class="ph ph-sparkle text-amber-300 text-sm"></i> <span>Generate Full Roster</span>`;
      }
    }
  }

  renderPreview(result) {
    if (!this.dom.previewSection || !result) return;
    this.dom.previewSection.classList.remove('hidden');
    if (this.dom.btnApply) {
      this.dom.btnApply.classList.remove('hidden');
    }

    const isBaseline = Boolean(result.baseline_only);
    const stats = result.stats || {};

    // Render Stats
    if (this.dom.statsBadges) {
      if (isBaseline) {
        this.dom.statsBadges.innerHTML = `
          <span class="px-2.5 py-0.5 rounded-full bg-amber-100 dark:bg-amber-950/70 text-amber-800 dark:text-amber-300 font-bold border border-amber-300 dark:border-amber-800 flex items-center gap-1">
            <i class="ph ph-calendar-check"></i> Baseline Only (Offs, Leaves & Swaps Anchored)
          </span>
          <span class="px-2.5 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-950/70 text-indigo-800 dark:text-indigo-300 font-bold border border-indigo-300 dark:border-indigo-800">
            ${stats.engineers_count || 0} Team Members
          </span>
        `;
      } else {
        this.dom.statsBadges.innerHTML = `
          <span class="px-2.5 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950/70 text-emerald-800 dark:text-emerald-300 font-bold border border-emerald-300 dark:border-emerald-800 flex items-center gap-1">
            <i class="ph ph-check-circle"></i> ${stats.nights_scheduled || 0} / ${stats.total_days || 0} Nights (100% Coverage)
          </span>
          <span class="px-2.5 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-950/70 text-indigo-800 dark:text-indigo-300 font-bold border border-indigo-300 dark:border-indigo-800">
            ${stats.engineers_count || 0} Team Members
          </span>
        `;
      }
    }

    // Render Matrix Table Preview
    if (this.dom.previewGridContainer) {
      const totalDays = result.total_days || 31;
      const rosterRows = result.roster || [];

      let headerHtml = `
        <table class="w-full text-left border-collapse border border-slate-200 dark:border-slate-800">
          <thead>
            <tr class="bg-slate-100 dark:bg-slate-800 text-[10px] text-slate-700 dark:text-slate-300 font-extrabold">
              <th class="p-2 border border-slate-200 dark:border-slate-700 sticky left-0 bg-slate-100 dark:bg-slate-800 z-10 w-28">Engineer</th>
              <th class="p-1 border border-slate-200 dark:border-slate-700 text-center w-10">NS</th>
      `;

      for (let d = 1; d <= totalDays; d++) {
        headerHtml += `<th class="p-1 border border-slate-200 dark:border-slate-700 text-center min-w-[24px]">${d}</th>`;
      }
      headerHtml += `</tr></thead><tbody>`;

      let bodyHtml = '';
      rosterRows.forEach(row => {
        const nsCount = (row.shifts || []).filter(s => s === 'NS').length;
        bodyHtml += `
          <tr class="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 text-[10px] border-b border-slate-100 dark:border-slate-800">
            <td class="p-2 font-bold border border-slate-200 dark:border-slate-700 sticky left-0 bg-white dark:bg-slate-900 z-10 whitespace-nowrap">
              ${row.name}
            </td>
            <td class="p-1 font-bold text-center border border-slate-200 dark:border-slate-700 ${nsCount > 0 ? 'text-indigo-600 dark:text-indigo-400 font-extrabold' : 'text-slate-400'}">
              ${nsCount}
            </td>
        `;

        (row.shifts || []).forEach(shift => {
          let cellStyle = 'border border-slate-200 dark:border-slate-700 text-center font-bold ';
          let label = shift || '';
          if (shift === 'NS') {
            cellStyle += 'bg-indigo-600 text-white rounded-xs';
          } else if (shift === 'OFF') {
            cellStyle += 'bg-red-50 text-red-600 dark:bg-red-950/40 dark:text-red-400';
          } else if (shift === 'L') {
            cellStyle += 'bg-amber-100 text-amber-800 dark:bg-amber-950/50 dark:text-amber-300';
          } else if (shift && shift.startsWith('SW')) {
            cellStyle += 'bg-purple-100 text-purple-800 dark:bg-purple-950/50 dark:text-purple-300';
          } else {
            cellStyle += 'text-slate-300 dark:text-slate-700';
          }
          bodyHtml += `<td class="${cellStyle}">${label}</td>`;
        });

        bodyHtml += `</tr>`;
      });

      bodyHtml += `</tbody></table>`;
      this.dom.previewGridContainer.innerHTML = headerHtml + bodyHtml;
    }
  }

  async applyToActiveGrid() {
    if (!this.currentGeneratedResult || !this.currentGeneratedResult.roster) {
      alert('No generated schedule available to apply.');
      return;
    }

    const genRoster = this.currentGeneratedResult.roster;
    const targetMonth = this.currentGeneratedResult.month;

    // Check if the current app month matches the generated month
    const curAppMonth = `${this.app.state.currentYear}-${String(this.app.state.currentMonth).padStart(2, '0')}`;
    if (curAppMonth !== targetMonth) {
      const confirmSwitch = confirm(
        `Generated schedule is for ${targetMonth}, but your grid is currently viewing ${curAppMonth}.\n\nDo you want to switch your active view to ${targetMonth} and apply the generated shifts?`
      );
      if (!confirmSwitch) return;

      const [y, m] = targetMonth.split('-').map(x => parseInt(x, 10));
      this.app.state.currentYear = y;
      this.app.state.currentMonth = m;
      if (this.app.dom.monthSelect) {
        this.app.dom.monthSelect.value = targetMonth;
      }
      if (typeof this.app.loadGridRosterForPeriod === 'function') {
        await this.app.loadGridRosterForPeriod(y, m);
      }
      if (typeof this.app.renderHeaders === 'function') {
        this.app.renderHeaders();
      }
    }

    // Snapshot for Undo / Redo (Ctrl+Z)
    if (typeof this.app.recordHistorySnapshot === 'function') {
      this.app.recordHistorySnapshot(this.currentGeneratedResult.baseline_only ? 'Pre-Fill Baseline' : 'Shift Generator');
    }

    // Merge or set generated shifts into app.state.gridRoster
    if (!Array.isArray(this.app.state.gridRoster) || this.app.state.gridRoster.length === 0) {
      this.app.state.gridRoster = JSON.parse(JSON.stringify(genRoster));
    } else {
      genRoster.forEach(genRow => {
        const genUname = String(genRow.username || genRow.id || '').trim().toLowerCase();
        const genName = String(genRow.name || '').trim().toLowerCase();

        const targetGridRow = this.app.state.gridRoster.find(r => {
          const u = String(r.username || r.id || '').trim().toLowerCase();
          const n = String(r.name || '').trim().toLowerCase();
          return (genUname && u === genUname) || (genName && n === genName);
        });

        if (targetGridRow) {
          targetGridRow.shifts = [...genRow.shifts];
        } else {
          this.app.state.gridRoster.push(JSON.parse(JSON.stringify(genRow)));
        }
      });
    }

    // Immediately re-render active table, radar, footers and mark state dirty/saved
    if (typeof this.app.renderRoster === 'function') {
      this.app.renderRoster();
    }
    if (typeof this.app.renderDailyCoverage === 'function') {
      this.app.renderDailyCoverage();
    }
    if (typeof this.app.runRuleAudit === 'function') {
      this.app.runRuleAudit();
    }
    if (typeof this.app.saveCurrentGridRoster === 'function') {
      this.app.saveCurrentGridRoster();
    }
    if (this.app.atlasService && typeof this.app.atlasService.notifyStatusChange === 'function') {
      this.app.atlasService.notifyStatusChange('unsaved');
    }
    if (typeof this.app.updatePeriodIndicator === 'function') {
      this.app.updatePeriodIndicator();
    }
    if (typeof this.app.renderOverviewCoverageRadar === 'function') {
      this.app.renderOverviewCoverageRadar();
    }

    if (typeof this.app.showToast === 'function') {
      const modeText = this.currentGeneratedResult.baseline_only ? 'Baseline (Offs, Leaves & Swaps)' : 'Full Shift Roster (Offs, Leaves & Nights)';
      this.app.showToast(`Applied ${modeText} to ${targetMonth}! Review and click 'Save & Publish' to persist.`);
    }

    this.closeModal();
  }
}
