/**
 * RosterX — Dynamic Admin Engine & AI Smart Roster Generator
 * Master Grid Controller & Feature Integration
 * 
 * Implements:
 * - Module A: Engineer Directory & Lifecycle Management (CRUD, Status, Join/Resign Dates, Weekly Offs)
 * - Module B: Editable Operational Constraints & Shift Engine (Headcounts, Rest Guards, Custom Directives)
 * - Module C: Shift Codes & Reference Timings (Exact Palette, Rest Windows)
 * - Module D: Smart Swaps, Re-swaps & "Swap-First" Policy (Pre-Locked Mutual Swaps & AI-Arbitrated Absences)
 * - Gemini AI Integration: Structured Output JSON Schema & Deterministic Fallback Engine
 * - Zero-Hardcoding: All data dynamic and persisted via localStorage & MongoDB Atlas M0
 */

import { 
  SHIFTS, 
  DEFAULT_CONSTRAINTS, 
  DEFAULT_ENGINEERS, 
  shiftsArrayToMap, 
  shiftsMapToArray,
  getWeekOffSortScore,
  sortEngineersByWeekOffs,
  getEngineerOrderWeight,
  getEngineerSwapCode,
  isSwapShift,
  isOffCategory
} from './constants.js?v=1.0';
import { RosterAiEngine } from './rosterAiEngine.js';
import { RosterAiAssistant } from './rosterAiAssistant.js';
import { AtlasService } from './atlasService.js';
import { ExcelService } from './excelService.js?v=3.2';
if (typeof window !== 'undefined') window.ExcelService = ExcelService;

const STORAGE_KEYS = {
  ENGINEERS_LIST: 'rosterx_engineers_list',
  CONSTRAINTS: 'rosterx_constraints',
  MANUAL_SWAPS: 'rosterx_manual_swaps',
  ABSENCE_REQUESTS: 'rosterx_absence_requests',
  LOCKED_CELLS: 'rosterx_locked_cells',
  GRID_ROSTER_PREFIX: 'rosterx_grid_roster_',
  GENERATED_OR_IMPORTED_MONTHS: 'rosterx_generated_or_imported_months'
};

class RosterXApp {
  constructor() {
    if (typeof window !== 'undefined') window.rosterApp = this;
    this.atlasService = new AtlasService();
    this.aiEngine = new RosterAiEngine();
    this.aiAssistant = new RosterAiAssistant(this);

    // Load dynamic state
    this.state = {
      currentYear: 2026,
      currentMonth: 10, // October 2026 default
      totalDays: 31,

      // Module A: Dynamic Engineer Directory
      engineersList: this.loadEngineersList(),

      // Current Grid Rows: [{ id, name, shifts: [...], status, role }]
      gridRoster: [],

      // Module B: Dynamic Operational Constraints
      constraints: this.loadConstraints(),

      // Module D: Dynamic Swaps & Absences
      manualSwaps: this.loadManualSwaps(),
      absenceRequests: this.loadAbsenceRequests(),

      // Cell Locks: { [engineerName]: { [day]: true } }
      lockedCells: this.loadLockedCells(),

      activeCell: null, // { engIndex, dayIndex }
      filterTerm: '',
      generatedDraft: null,
      auditState: null,

      // Shift Visibility Filter (Set of visible shift categories: SW, OFF, NS, FS, FG, G, SG, SS, L, OTHER)
      visibleShifts: new Set(['SW', 'OFF', 'NS', 'FS', 'FG', 'G', 'SG', 'SS', 'L', 'OTHER'])
    };

    this.dom = {};
  }

  isMonthImportedOrGenerated(monthKey) {
    if (typeof localStorage === 'undefined' || !monthKey) return false;
    try {
      const set = JSON.parse(localStorage.getItem(STORAGE_KEYS.GENERATED_OR_IMPORTED_MONTHS) || '[]');
      return Array.isArray(set) && set.includes(monthKey);
    } catch (e) {
      return false;
    }
  }

  markMonthAsImportedOrGenerated(monthKey) {
    if (typeof localStorage === 'undefined' || !monthKey) return;
    try {
      const set = JSON.parse(localStorage.getItem(STORAGE_KEYS.GENERATED_OR_IMPORTED_MONTHS) || '[]');
      if (!set.includes(monthKey)) {
        set.push(monthKey);
        localStorage.setItem(STORAGE_KEYS.GENERATED_OR_IMPORTED_MONTHS, JSON.stringify(set));
      }
    } catch (e) {
      console.error('Failed to mark month', e);
    }
  }

  // --- Dynamic Storage Loaders & Week-off Sorting Helpers ---
  getEngineerWeekOffs(eng) {
    if (eng && eng.default_weekly_offs && Array.isArray(eng.default_weekly_offs) && eng.default_weekly_offs.length > 0) {
      return eng.default_weekly_offs;
    }
    const engName = String(typeof eng === 'string' ? eng : (eng?.name || eng?.id || '')).trim().toUpperCase();
    const engId = typeof eng === 'object' && eng ? eng.id : '';
    const meta = (this.state && Array.isArray(this.state.engineersList))
      ? this.state.engineersList.find(e => (e.name && e.name.trim().toUpperCase() === engName) || (engId && e.id === engId))
      : null;
    return meta?.default_weekly_offs || [];
  }

  sortEngineersByWeekOffs(list) {
    return sortEngineersByWeekOffs(list, (e) => this.getEngineerWeekOffs(e));
  }

  sortGridRosterByWeekOffs(grid) {
    return sortEngineersByWeekOffs(grid, (e) => this.getEngineerWeekOffs(e));
  }

  getEngineerUsername(eng) {
    if (!eng) return '';
    // Priority 1: Domain ID / prefix from email (e.g. shefeeqkk@tejasnetworks.com -> shefeeqkk)
    if (eng.email && typeof eng.email === 'string' && eng.email.includes('@')) {
      const prefix = eng.email.split('@')[0].trim().toLowerCase();
      if (prefix) return prefix;
    }
    // Priority 2: Explicit assigned username
    if (eng.username && typeof eng.username === 'string' && eng.username.trim()) {
      return eng.username.trim().toLowerCase();
    }
    // Priority 3: AuthService generator
    if (typeof window !== 'undefined' && window.authService && typeof window.authService.generateUsername === 'function') {
      return window.authService.generateUsername(eng.name, eng.email);
    }
    // Priority 4: Display name fallback
    const clean = String(eng.name || '').trim().toLowerCase();
    const parts = clean.split(/[\s\W]+/);
    if (parts.length > 1 && clean.includes('raj')) {
      return parts.join('');
    }
    return parts[0] || clean || 'user';
  }

  ensureEngineerUsernames() {
    if (!Array.isArray(this.state.engineersList)) return;
    let modified = false;
    this.state.engineersList.forEach(eng => {
      const proper = this.getEngineerUsername(eng);
      if (eng.username !== proper) {
        eng.username = proper;
        modified = true;
      }
    });
    if (modified) {
      this.saveEngineersList();
      if (typeof window !== 'undefined' && window.authService && typeof window.authService.syncEngineersList === 'function') {
        window.authService.syncEngineersList(this.state.engineersList);
      }
    }
  }

  loadEngineersList() {
    let list = null;
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(STORAGE_KEYS.ENGINEERS_LIST);
      if (stored) {
        try { list = JSON.parse(stored); } catch (e) { console.error('Failed to parse stored engineers', e); }
      }
    }
    if (!list || !Array.isArray(list) || list.length === 0) {
      list = JSON.parse(JSON.stringify(DEFAULT_ENGINEERS));
    }
    return this.sortEngineersByWeekOffs(list);
  }

  saveEngineersList() {
    this.state.engineersList = this.sortEngineersByWeekOffs(this.state.engineersList);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.ENGINEERS_LIST, JSON.stringify(this.state.engineersList));
    }
  }

  async syncEngineersFromAtlas() {
    try {
      const docs = await this.atlasService.getEngineers();
      if (Array.isArray(docs) && docs.length > 0) {
        // Merge with current state (so local edits and defaults are harmonized)
        const merged = this.atlasService.mergeEngineers(docs, this.state.engineersList);
        this.state.engineersList = this.sortEngineersByWeekOffs(merged);
        this.saveEngineersList();
        if (typeof window !== 'undefined' && window.authService) {
          window.authService.syncEngineersList(this.state.engineersList);
        }
      }
    } catch (e) {
      console.warn('Failed to sync engineers from Atlas on init:', e);
    }
  }

  loadConstraints() {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(STORAGE_KEYS.CONSTRAINTS);
      if (stored) {
        try { return { ...DEFAULT_CONSTRAINTS, ...JSON.parse(stored) }; } catch (e) { console.error('Failed to parse constraints', e); }
      }
    }
    return JSON.parse(JSON.stringify(DEFAULT_CONSTRAINTS));
  }

  saveConstraints() {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.CONSTRAINTS, JSON.stringify(this.state.constraints));
    }
  }

  loadManualSwaps() {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(STORAGE_KEYS.MANUAL_SWAPS);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed)) {
            return parsed.filter(s => s && s.id !== 'swap_01');
          }
        } catch (e) { console.error('Failed to parse manual swaps', e); }
      }
    }
    return [];
  }

  saveManualSwaps() {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.MANUAL_SWAPS, JSON.stringify(this.state.manualSwaps));
    }
  }

  loadAbsenceRequests() {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(STORAGE_KEYS.ABSENCE_REQUESTS);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed)) {
            return parsed.filter(a => a && a.id !== 'abs_01');
          }
        } catch (e) { console.error('Failed to parse absence requests', e); }
      }
    }
    return [];
  }

  saveAbsenceRequests() {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.ABSENCE_REQUESTS, JSON.stringify(this.state.absenceRequests));
    }
  }

  loadLockedCells() {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(STORAGE_KEYS.LOCKED_CELLS);
      if (stored) {
        try {
          const parsed = JSON.parse(stored);
          if (parsed && typeof parsed === 'object') {
            // Clean up legacy fake locks
            if (parsed["DYANI DESOZA"] && parsed["DYANI DESOZA"]["24"]) {
              delete parsed["DYANI DESOZA"]["24"];
            }
            return parsed;
          }
        } catch (e) { console.error('Failed to parse locked cells', e); }
      }
    }
    return {};
  }

  saveLockedCells() {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.LOCKED_CELLS, JSON.stringify(this.state.lockedCells));
    }
  }

  // --- Initializer ---
  async init() {
    this.cacheDomElements();
    this.bindEvents();
    this.setupAtlasSyncBadge();

    // 1. Sync live engineers directory with MongoDB Atlas & LocalStorage
    await this.syncEngineersFromAtlas();
    this.ensureEngineerUsernames();

    // 1b. Sync operational policies/constraints from MongoDB Atlas
    await this.syncConstraintsFromAtlas();

    // 2. Synchronize month selector dropdown with active period
    const activePeriodKey = `${this.state.currentYear}-${String(this.state.currentMonth).padStart(2, '0')}`;
    if (this.dom.monthSelect) {
      this.dom.monthSelect.value = activePeriodKey;
    }

    // 3. Initialize Grid Roster for current period (with Atlas sync)
    await this.loadGridRosterForPeriod(this.state.currentYear, this.state.currentMonth);

    // Apply and display active policies
    this.updateActiveRulesBanner();
    this.updateTeamBadge();

    // Render interface
    this.renderHeaders();
    this.renderRoster();
    this.updateShiftFilterUI();
    this.runRuleAudit();

    // Initialize Conversational AI Assistant
    if (this.aiAssistant) {
      this.aiAssistant.init();
    }
  }

  cacheDomElements() {
    // Top Nav & Toolbar
    this.dom.searchInput = document.getElementById('searchInput');
    this.dom.monthSelect = document.getElementById('monthSelect');
    this.dom.btnPrevMonth = document.getElementById('btnPrevMonth');
    this.dom.btnNextMonth = document.getElementById('btnNextMonth');
    this.dom.activeMonthDisplay = document.getElementById('activeMonthDisplay');
    this.dom.periodLabelCell = document.getElementById('periodLabelCell');
    this.dom.syncBadgeIndicator = document.getElementById('syncBadgeIndicator');
    this.dom.syncBadgeText = document.getElementById('syncBadgeText');
    this.dom.engineerCountLabel = document.getElementById('engineerCountLabel');
    this.dom.activeTeamBadge = document.getElementById('activeTeamBadge');

    // Action Buttons
    this.dom.btnOpenConstraintsModal = document.getElementById('btnOpenConstraintsModal');
    this.dom.btnOpenTeamModal = document.getElementById('btnOpenTeamModal');
    this.dom.btnOpenSwapsModal = document.getElementById('btnOpenSwapsModal');
    this.dom.btnOpenAiModal = document.getElementById('btnOpenAiModal');
    this.dom.btnExportXlsx = document.getElementById('btnExportXlsx');
    this.dom.exportModal = document.getElementById('exportModal');
    this.dom.exportModalPeriodLabel = document.getElementById('exportModalPeriodLabel');
    this.dom.btnCloseExportModal = document.getElementById('btnCloseExportModal');
    this.dom.btnCancelExportModal = document.getElementById('btnCancelExportModal');
    this.dom.btnExportMonthGrid = document.getElementById('btnExportMonthGrid');
    this.dom.btnExportAppSheet = document.getElementById('btnExportAppSheet');
    this.dom.btnOpenImportModal = document.getElementById('btnOpenImportModal');
    this.dom.btnSaveAtlasDirect = document.getElementById('btnSaveAtlasDirect');
    this.dom.btnQuickEditRules = document.getElementById('btnQuickEditRules');

    // Recovery & Publish Confirmation Modals
    this.dom.btnOpenRecoveryModal = document.getElementById('btnOpenRecoveryModal');
    this.dom.recoveryModal = document.getElementById('recoveryModal');
    this.dom.recoveryModalPeriodLabel = document.getElementById('recoveryModalPeriodLabel');
    this.dom.btnCloseRecoveryModal = document.getElementById('btnCloseRecoveryModal');
    this.dom.btnCloseRecoveryModalFooter = document.getElementById('btnCloseRecoveryModalFooter');
    this.dom.recoverySnapshotsLoading = document.getElementById('recoverySnapshotsLoading');
    this.dom.recoverySnapshotsEmpty = document.getElementById('recoverySnapshotsEmpty');
    this.dom.recoverySnapshotsList = document.getElementById('recoverySnapshotsList');

    this.dom.publishConfirmModal = document.getElementById('publishConfirmModal');
    this.dom.publishConfirmPeriod = document.getElementById('publishConfirmPeriod');
    this.dom.publishConfirmEngCount = document.getElementById('publishConfirmEngCount');
    this.dom.btnCancelPublishConfirm = document.getElementById('btnCancelPublishConfirm');
    this.dom.btnExecutePublishConfirm = document.getElementById('btnExecutePublishConfirm');

    // Shift Visibility Filter Elements
    this.dom.shiftFilterToolbar = document.getElementById('shiftFilterToolbar');
    this.dom.btnPresetAllShifts = document.getElementById('btnPresetAllShifts');
    this.dom.btnPresetSwOffNs = document.getElementById('btnPresetSwOffNs');
    this.dom.btnPresetNsOnly = document.getElementById('btnPresetNsOnly');
    this.dom.btnPresetOffOnly = document.getElementById('btnPresetOffOnly');
    this.dom.btnPresetSwOnly = document.getElementById('btnPresetSwOnly');
    this.dom.shiftFilterPillsContainer = document.getElementById('shiftFilterPillsContainer');
    this.dom.shiftFilterActiveBadge = document.getElementById('shiftFilterActiveBadge');
    this.dom.shiftFilterActiveText = document.getElementById('shiftFilterActiveText');
    this.dom.btnResetShiftFilter = document.getElementById('btnResetShiftFilter');

    // Active Rules Banner Text
    this.dom.ruleNightText = document.getElementById('ruleNightText');
    this.dom.ruleFsText = document.getElementById('ruleFsText');
    this.dom.ruleSsText = document.getElementById('ruleSsText');
    this.dom.ruleRestText = document.getElementById('ruleRestText');
    this.dom.ruleDirectivesSnippet = document.getElementById('ruleDirectivesSnippet');

    // Headers & Grid Table
    this.dom.headerDatesColTh = document.getElementById('headerDatesColTh');
    this.dom.headerDaysColTh = document.getElementById('headerDaysColTh');
    this.dom.headerDatesRow = document.getElementById('headerDatesRow');
    this.dom.headerDaysRow = document.getElementById('headerDaysRow');
    this.dom.headerDaysOfWeekRow = document.getElementById('headerDaysOfWeekRow');
    this.dom.rosterTableBody = document.getElementById('rosterTableBody');
    this.dom.footerDayColTd = document.getElementById('footerDayColTd');
    this.dom.footerNightColTd = document.getElementById('footerNightColTd');
    this.dom.coverageDayRow = document.getElementById('coverageDayRow');
    this.dom.coverageNightRow = document.getElementById('coverageNightRow');
    this.dom.footerTargetDay = document.getElementById('footerTargetDay');
    this.dom.footerTargetNs = document.getElementById('footerTargetNs');

    // Real-Time Rule Audit Banner
    this.dom.ruleAuditAlertBanner = document.getElementById('ruleAuditAlertBanner');
    this.dom.ruleAuditSummaryText = document.getElementById('ruleAuditSummaryText');
    this.dom.ruleAuditChipsContainer = document.getElementById('ruleAuditChipsContainer');
    this.dom.btnViewAuditDetails = document.getElementById('btnViewAuditDetails');
    this.dom.auditDetailsModal = document.getElementById('auditDetailsModal');
    this.dom.btnCloseAuditDetails = document.getElementById('btnCloseAuditDetails');
    this.dom.btnCloseAuditDetailsFooter = document.getElementById('btnCloseAuditDetailsFooter');
    this.dom.auditDetailsList = document.getElementById('auditDetailsList');

    // MODAL 1: Team Directory Modal
    this.dom.teamModal = document.getElementById('teamModal');
    this.dom.btnCloseTeamModal = document.getElementById('btnCloseTeamModal');
    this.dom.btnAddNewEngineer = document.getElementById('btnAddNewEngineer');
    this.dom.teamTableBody = document.getElementById('teamTableBody');
    this.dom.engineerFormContainer = document.getElementById('engineerFormContainer');
    this.dom.engineerFormTitle = document.getElementById('engineerFormTitle');
    this.dom.btnCloseEngineerFormHeader = document.getElementById('btnCloseEngineerFormHeader');
    this.dom.btnResetEngineerForm = document.getElementById('btnResetEngineerForm');
    this.dom.formEngId = document.getElementById('formEngId');
    this.dom.formEngName = document.getElementById('formEngName');
    this.dom.formEngUsername = document.getElementById('formEngUsername');
    this.dom.formEngFullName = document.getElementById('formEngFullName');
    this.dom.formEngEmail = document.getElementById('formEngEmail');
    this.dom.formEngPhone = document.getElementById('formEngPhone');
    this.dom.formEngRole = document.getElementById('formEngRole');
    this.dom.formEngStatus = document.getElementById('formEngStatus');
    this.dom.formEngJoinDate = document.getElementById('formEngJoinDate');
    this.dom.formEngResignDate = document.getElementById('formEngResignDate');
    this.dom.formEngOff1 = document.getElementById('formEngOff1');
    this.dom.formEngOff2 = document.getElementById('formEngOff2');
    this.dom.btnCancelEngineerForm = document.getElementById('btnCancelEngineerForm');
    this.dom.btnSaveEngineerForm = document.getElementById('btnSaveEngineerForm');
    this.dom.btnDeleteEngineerForm = document.getElementById('btnDeleteEngineerForm');

    // MODAL 2: Constraints Modal
    this.dom.constraintsModal = document.getElementById('constraintsModal');
    this.dom.btnCloseConstraintsModal = document.getElementById('btnCloseConstraintsModal');
    this.dom.cfgNightHeadcount = document.getElementById('cfgNightHeadcount');
    this.dom.cfgFsMin = document.getElementById('cfgFsMin');
    this.dom.cfgSsMin = document.getElementById('cfgSsMin');
    this.dom.cfgDaytimeOverlap = document.getElementById('cfgDaytimeOverlap');
    this.dom.cfgPostNightRest = document.getElementById('cfgPostNightRest');
    this.dom.cfgMaxConsecutive = document.getElementById('cfgMaxConsecutive');
    this.dom.cfgCustomDirectives = document.getElementById('cfgCustomDirectives');
    this.dom.btnSaveConstraints = document.getElementById('btnSaveConstraints');

    // MODAL 3: Swaps & Absences Modal
    this.dom.swapsModal = document.getElementById('swapsModal');
    this.dom.btnCloseSwapsModal = document.getElementById('btnCloseSwapsModal');
    this.dom.tabBtnMutualSwaps = document.getElementById('tabBtnMutualSwaps');
    this.dom.tabBtnSmartAbsences = document.getElementById('tabBtnSmartAbsences');
    this.dom.tabMutual = document.getElementById('tabMutual');
    this.dom.tabSmart = document.getElementById('tabSmart');
    this.dom.mutSwapDay1 = document.getElementById('mutSwapDay1');
    this.dom.mutSwapEngA = document.getElementById('mutSwapEngA');
    this.dom.mutSwapEngB = document.getElementById('mutSwapEngB');
    this.dom.mutSwapDay2 = document.getElementById('mutSwapDay2');
    this.dom.btnAddMutualSwap = document.getElementById('btnAddMutualSwap');
    this.dom.mutualSwapsList = document.getElementById('mutualSwapsList');
    this.dom.absReqEngineer = document.getElementById('absReqEngineer');
    this.dom.absReqDay = document.getElementById('absReqDay');
    this.dom.btnAddAbsenceRequest = document.getElementById('btnAddAbsenceRequest');
    this.dom.absenceRequestsList = document.getElementById('absenceRequestsList');

    // MODAL 4: Gemini AI Smart Roster Generator
    this.dom.aiModal = document.getElementById('aiModal');
    this.dom.btnCloseAiModal = document.getElementById('btnCloseAiModal');
    this.dom.aiModalPeriodLabel = document.getElementById('aiModalPeriodLabel');
    this.dom.aiActiveEngineersCount = document.getElementById('aiActiveEngineersCount');
    this.dom.aiProgressBox = document.getElementById('aiProgressBox');
    this.dom.aiProgressText = document.getElementById('aiProgressText');
    this.dom.aiScorecard = document.getElementById('aiScorecard');
    this.dom.aiScoreBadge = document.getElementById('aiScoreBadge');
    this.dom.aiNightCov = document.getElementById('aiNightCov');
    this.dom.aiFatigueViolations = document.getElementById('aiFatigueViolations');
    this.dom.aiEngineLabel = document.getElementById('aiEngineLabel');
    this.dom.aiArbitratedSwapsBox = document.getElementById('aiArbitratedSwapsBox');
    this.dom.aiArbitratedSwapsList = document.getElementById('aiArbitratedSwapsList');
    this.dom.btnOpenGeminiSettingsInline = document.getElementById('btnOpenGeminiSettingsInline');
    this.dom.btnRunAiGeneration = document.getElementById('btnRunAiGeneration');
    this.dom.btnApplyAiDraft = document.getElementById('btnApplyAiDraft');

    // MODAL 5: Shift Picker Popover
    this.dom.shiftPickerModal = document.getElementById('shiftPickerModal');
    this.dom.pickerSubHeader = document.getElementById('pickerSubHeader');
    this.dom.pickerLockIndicator = document.getElementById('pickerLockIndicator');
    this.dom.btnClosePicker = document.getElementById('btnClosePicker');
    this.dom.pickerLockCheckbox = document.getElementById('pickerLockCheckbox');
    this.dom.pickerSwapButtonsContainer = document.getElementById('pickerSwapButtonsContainer');

    // Gemini Settings Modal
    this.dom.geminiSettingsModal = document.getElementById('geminiSettingsModal');
    this.dom.btnCloseGeminiSettings = document.getElementById('btnCloseGeminiSettings');
    this.dom.geminiApiKeyInput = document.getElementById('geminiApiKeyInput');
    this.dom.geminiModelSelect = document.getElementById('geminiModelSelect');
    this.dom.btnSaveGeminiSettings = document.getElementById('btnSaveGeminiSettings');

    // Atlas Settings Modal
    this.dom.atlasConfigModal = document.getElementById('atlasConfigModal');
    this.dom.btnCloseAtlasConfig = document.getElementById('btnCloseAtlasConfig');
    this.dom.atlasUriInput = document.getElementById('atlasUriInput');
    this.dom.atlasEndpointInput = document.getElementById('atlasEndpointInput');
    this.dom.atlasApiKeyInput = document.getElementById('atlasApiKeyInput');
    this.dom.atlasClusterInput = document.getElementById('atlasClusterInput');
    this.dom.atlasDatabaseInput = document.getElementById('atlasDatabaseInput');
    this.dom.btnTestAtlasConn = document.getElementById('btnTestAtlasConn');
    this.dom.btnSaveAtlasConfig = document.getElementById('btnSaveAtlasConfig');
    this.dom.atlasTestFeedback = document.getElementById('atlasTestFeedback');

    // MODAL: Import Excel Modal
    this.dom.importModal = document.getElementById('importModal');
    this.dom.btnCloseImportModal = document.getElementById('btnCloseImportModal');
    this.dom.btnCancelImport = document.getElementById('btnCancelImport');
    this.dom.importTargetMonthInput = document.getElementById('importTargetMonthInput');
    this.dom.importTargetMonthSelect = document.getElementById('importTargetMonthSelect');
    this.dom.importModalPeriodLabel = document.getElementById('importModalPeriodLabel');
    this.dom.importActiveEngCount = document.getElementById('importActiveEngCount');
    this.dom.importEngineerInput = document.getElementById('importEngineerInput');
    this.dom.btnAddImportEngineer = document.getElementById('btnAddImportEngineer');
    this.dom.importEngineerSuggestions = document.getElementById('importEngineerSuggestions');
    this.dom.btnClearImportEngineerInput = document.getElementById('btnClearImportEngineerInput');
    this.dom.importEngineerTagsContainer = document.getElementById('importEngineerTagsContainer');
    this.dom.btnDownloadSampleTemplate = document.getElementById('btnDownloadSampleTemplate');
    this.dom.dropZone = document.getElementById('dropZone');
    this.dom.excelFileInput = document.getElementById('excelFileInput');
    this.dom.selectedFileInfo = document.getElementById('selectedFileInfo');
    this.dom.selectedFileName = document.getElementById('selectedFileName');
    this.dom.selectedFileSize = document.getElementById('selectedFileSize');
    this.dom.btnClearFile = document.getElementById('btnClearFile');
    this.dom.importPreviewBox = document.getElementById('importPreviewBox');
    this.dom.importSummaryBadge = document.getElementById('importSummaryBadge');
    this.dom.previewEngCount = document.getElementById('previewEngCount');
    this.dom.previewDaysCount = document.getElementById('previewDaysCount');
    this.dom.previewStatus = document.getElementById('previewStatus');
    this.dom.importWarningsBox = document.getElementById('importWarningsBox');
    this.dom.importWarningsList = document.getElementById('importWarningsList');
    this.dom.previewTableHead = document.getElementById('previewTableHead');
    this.dom.previewTableBody = document.getElementById('previewTableBody');
    this.dom.btnApplyImportedRoster = document.getElementById('btnApplyImportedRoster');

    // Toast
    this.dom.toast = document.getElementById('toast');
    this.dom.toastMsg = document.getElementById('toastMsg');
  }

  bindEvents() {
    // Search Filter
    this.dom.searchInput.addEventListener('input', (e) => {
      this.state.filterTerm = e.target.value.toLowerCase().trim();
      this.renderRoster();
    });

    // Month / Year Selector & Navigation Arrows
    this.dom.monthSelect.addEventListener('change', () => this.handlePeriodChange());
    this.dom.btnPrevMonth?.addEventListener('click', () => this.navigateMonth(-1));
    this.dom.btnNextMonth?.addEventListener('click', () => this.navigateMonth(1));

    // Shift Visibility Filter Events
    this.dom.btnPresetAllShifts?.addEventListener('click', () => this.setShiftFilterPreset('ALL'));
    this.dom.btnPresetSwOffNs?.addEventListener('click', () => this.setShiftFilterPreset('SW_OFF_NS'));
    this.dom.btnPresetNsOnly?.addEventListener('click', () => this.setShiftFilterPreset('NS_ONLY'));
    this.dom.btnPresetOffOnly?.addEventListener('click', () => this.setShiftFilterPreset('OFF_ONLY'));
    this.dom.btnPresetSwOnly?.addEventListener('click', () => this.setShiftFilterPreset('SW_ONLY'));
    this.dom.btnResetShiftFilter?.addEventListener('click', () => this.setShiftFilterPreset('ALL'));

    if (this.dom.shiftFilterPillsContainer) {
      this.dom.shiftFilterPillsContainer.addEventListener('click', (e) => {
        const chipBtn = e.target.closest('.shift-filter-chip');
        if (chipBtn) {
          const category = chipBtn.dataset.shiftCat;
          if (category) {
            this.toggleShiftVisibility(category);
          }
        }
      });
    }

    // Top Action Buttons
    this.dom.btnOpenConstraintsModal.addEventListener('click', () => this.openConstraintsModal());
    this.dom.btnQuickEditRules.addEventListener('click', () => this.openConstraintsModal());
    this.dom.btnCloseConstraintsModal.addEventListener('click', () => this.dom.constraintsModal.classList.add('hidden'));
    this.dom.btnSaveConstraints.addEventListener('click', () => this.saveOperationalConstraints());

    this.dom.btnOpenTeamModal.addEventListener('click', () => this.openTeamModal());
    this.dom.btnCloseTeamModal.addEventListener('click', () => {
      this.closeEngineerForm();
      this.dom.teamModal.classList.add('hidden');
    });
    this.dom.btnAddNewEngineer.addEventListener('click', () => {
      if (!this.dom.engineerFormContainer.classList.contains('hidden') && !this.state.editingEngineerId) {
        this.closeEngineerForm();
      } else {
        this.openEngineerForm();
      }
    });
    this.dom.btnCancelEngineerForm.addEventListener('click', () => this.closeEngineerForm());
    if (this.dom.btnCloseEngineerFormHeader) {
      this.dom.btnCloseEngineerFormHeader.addEventListener('click', () => this.closeEngineerForm());
    }
    if (this.dom.btnResetEngineerForm) {
      this.dom.btnResetEngineerForm.addEventListener('click', () => this.resetEngineerFormFields());
    }
    this.dom.btnSaveEngineerForm.addEventListener('click', () => this.handleSaveEngineer());
    if (this.dom.btnDeleteEngineerForm) {
      this.dom.btnDeleteEngineerForm.addEventListener('click', () => this.handleDeleteEngineer());
    }
    if (this.dom.teamTableBody) {
      this.dom.teamTableBody.addEventListener('click', (e) => {
        const editBtn = e.target.closest('[data-action="edit-engineer"]');
        if (editBtn) {
          e.preventDefault();
          const idx = parseInt(editBtn.dataset.idx, 10);
          if (!isNaN(idx)) this.openEngineerFormByIndex(idx);
          return;
        }
        const retireBtn = e.target.closest('[data-action="retire-engineer"]');
        if (retireBtn) {
          e.preventDefault();
          const idx = parseInt(retireBtn.dataset.idx, 10);
          if (!isNaN(idx)) this.toggleRetireEngineer(idx);
        }
      });
    }
    if (this.dom.formEngName && this.dom.formEngUsername) {
      this.dom.formEngName.addEventListener('input', () => {
        if (!this.state.editingEngineerId) {
          const emailVal = this.dom.formEngEmail ? this.dom.formEngEmail.value : '';
          this.dom.formEngUsername.value = this.getEngineerUsername({ name: this.dom.formEngName.value, email: emailVal });
        }
      });
    }
    if (this.dom.formEngEmail && this.dom.formEngUsername) {
      this.dom.formEngEmail.addEventListener('input', () => {
        if (!this.state.editingEngineerId) {
          const emailVal = this.dom.formEngEmail.value;
          if (emailVal.includes('@')) {
            const domainPrefix = emailVal.split('@')[0].trim().toLowerCase();
            if (domainPrefix) this.dom.formEngUsername.value = domainPrefix;
          } else if (!this.dom.formEngUsername.value) {
            this.dom.formEngUsername.value = this.getEngineerUsername({ name: this.dom.formEngName.value, email: emailVal });
          }
        }
      });
    }

    this.dom.btnOpenSwapsModal.addEventListener('click', () => this.openSwapsModal());
    this.dom.btnCloseSwapsModal.addEventListener('click', () => this.dom.swapsModal.classList.add('hidden'));
    this.dom.tabBtnMutualSwaps.addEventListener('click', () => this.switchSwapsTab('tabMutual'));
    this.dom.tabBtnSmartAbsences.addEventListener('click', () => this.switchSwapsTab('tabSmart'));
    this.dom.btnAddMutualSwap.addEventListener('click', () => this.handleAddMutualSwap());
    this.dom.btnAddAbsenceRequest.addEventListener('click', () => this.handleAddAbsenceRequest());

    this.dom.btnOpenAiModal.addEventListener('click', () => this.openAiModal());
    this.dom.btnCloseAiModal.addEventListener('click', () => this.dom.aiModal.classList.add('hidden'));
    this.dom.btnRunAiGeneration.addEventListener('click', () => this.runAiGeneration());
    this.dom.btnApplyAiDraft.addEventListener('click', () => this.applyAiDraft());
    this.dom.btnOpenGeminiSettingsInline.addEventListener('click', () => this.openGeminiSettings());

    // Export, Import & Publish
    this.dom.btnExportXlsx.addEventListener('click', () => this.openExportModal());
    if (this.dom.btnCloseExportModal) {
      this.dom.btnCloseExportModal.addEventListener('click', () => this.closeExportModal());
    }
    if (this.dom.btnCancelExportModal) {
      this.dom.btnCancelExportModal.addEventListener('click', () => this.closeExportModal());
    }
    if (this.dom.btnExportMonthGrid) {
      this.dom.btnExportMonthGrid.addEventListener('click', () => this.exportToMonthGrid());
    }
    if (this.dom.btnExportAppSheet) {
      this.dom.btnExportAppSheet.addEventListener('click', () => this.exportToAppSheet());
    }
    if (this.dom.btnOpenImportModal) {
      this.dom.btnOpenImportModal.addEventListener('click', () => this.openImportModal());
    }
    if (this.dom.btnCloseImportModal) {
      this.dom.btnCloseImportModal.addEventListener('click', () => this.closeImportModal());
    }
    if (this.dom.btnCancelImport) {
      this.dom.btnCancelImport.addEventListener('click', () => this.closeImportModal());
    }
    if (this.dom.importTargetMonthInput) {
      this.dom.importTargetMonthInput.addEventListener('change', () => {
        if (this.dom.importTargetMonthSelect) {
          this.dom.importTargetMonthSelect.value = this.dom.importTargetMonthInput.value;
        }
        this.updateImportModalPeriodLabel();
      });
    }
    if (this.dom.importTargetMonthSelect) {
      this.dom.importTargetMonthSelect.addEventListener('change', () => {
        if (this.dom.importTargetMonthInput) {
          this.dom.importTargetMonthInput.value = this.dom.importTargetMonthSelect.value;
        }
        this.updateImportModalPeriodLabel();
      });
    }
    if (this.dom.btnAddImportEngineer) {
      this.dom.btnAddImportEngineer.addEventListener('click', () => this.handleAddImportEngineer());
    }
    if (this.dom.importEngineerInput) {
      this.dom.importEngineerInput.addEventListener('input', (e) => {
        const val = e.target.value;
        if (this.dom.btnClearImportEngineerInput) {
          this.dom.btnClearImportEngineerInput.classList.toggle('hidden', !val);
        }
        this.renderImportEngineerSuggestions(val);
      });

      this.dom.importEngineerInput.addEventListener('focus', (e) => {
        this.renderImportEngineerSuggestions(e.target.value);
      });

      this.dom.importEngineerInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.handleAddImportEngineer();
        } else if (e.key === 'Escape') {
          this.closeImportEngineerSuggestions();
        }
      });
    }

    if (this.dom.btnClearImportEngineerInput) {
      this.dom.btnClearImportEngineerInput.addEventListener('click', () => {
        if (this.dom.importEngineerInput) {
          this.dom.importEngineerInput.value = '';
          this.dom.importEngineerInput.focus();
        }
        this.dom.btnClearImportEngineerInput.classList.add('hidden');
        this.closeImportEngineerSuggestions();
      });
    }

    document.addEventListener('click', (e) => {
      if (this.dom.importEngineerSuggestions && !this.dom.importEngineerSuggestions.classList.contains('hidden')) {
        const isClickInside = this.dom.importEngineerInput?.contains(e.target) || 
                              this.dom.importEngineerSuggestions?.contains(e.target) ||
                              this.dom.btnAddImportEngineer?.contains(e.target);
        if (!isClickInside) {
          this.closeImportEngineerSuggestions();
        }
      }
    });
    if (this.dom.btnDownloadSampleTemplate) {
      this.dom.btnDownloadSampleTemplate.addEventListener('click', () => this.downloadSampleTemplate());
    }
    if (this.dom.dropZone && this.dom.excelFileInput) {
      this.dom.dropZone.addEventListener('click', () => this.dom.excelFileInput.click());
      this.dom.dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        this.dom.dropZone.classList.add('border-blue-600', 'bg-blue-50/50');
      });
      this.dom.dropZone.addEventListener('dragleave', () => {
        this.dom.dropZone.classList.remove('border-blue-600', 'bg-blue-50/50');
      });
      this.dom.dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        this.dom.dropZone.classList.remove('border-blue-600', 'bg-blue-50/50');
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
          this.handleExcelFileSelected(e.dataTransfer.files[0]);
        }
      });
      this.dom.excelFileInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
          this.handleExcelFileSelected(e.target.files[0]);
        }
      });
    }
    if (this.dom.btnClearFile) {
      this.dom.btnClearFile.addEventListener('click', () => this.clearImportFile());
    }
    if (this.dom.btnApplyImportedRoster) {
      this.dom.btnApplyImportedRoster.addEventListener('click', () => this.applyImportedRoster());
    }

    if (this.dom.btnSaveAtlasDirect) {
      this.dom.btnSaveAtlasDirect.addEventListener('click', () => this.openPublishConfirmation());
    }
    if (this.dom.btnCancelPublishConfirm) {
      this.dom.btnCancelPublishConfirm.addEventListener('click', () => {
        if (this.dom.publishConfirmModal) this.dom.publishConfirmModal.classList.add('hidden');
      });
    }
    if (this.dom.btnExecutePublishConfirm) {
      this.dom.btnExecutePublishConfirm.addEventListener('click', () => {
        if (this.dom.publishConfirmModal) this.dom.publishConfirmModal.classList.add('hidden');
        this.saveAndPublishToAtlas();
      });
    }

    if (this.dom.btnOpenRecoveryModal) {
      this.dom.btnOpenRecoveryModal.addEventListener('click', () => this.openRecoveryModal());
    }
    if (this.dom.btnCloseRecoveryModal) {
      this.dom.btnCloseRecoveryModal.addEventListener('click', () => {
        if (this.dom.recoveryModal) this.dom.recoveryModal.classList.add('hidden');
      });
    }
    if (this.dom.btnCloseRecoveryModalFooter) {
      this.dom.btnCloseRecoveryModalFooter.addEventListener('click', () => {
        if (this.dom.recoveryModal) this.dom.recoveryModal.classList.add('hidden');
      });
    }
    if (this.dom.syncBadgeIndicator) {
      this.dom.syncBadgeIndicator.addEventListener('click', () => this.openAtlasConfig());
    }

    // Rule Audit Details
    this.dom.btnViewAuditDetails.addEventListener('click', () => this.openAuditDetailsModal());
    this.dom.btnCloseAuditDetails.addEventListener('click', () => this.dom.auditDetailsModal.classList.add('hidden'));
    this.dom.btnCloseAuditDetailsFooter.addEventListener('click', () => this.dom.auditDetailsModal.classList.add('hidden'));

    // Cell Shift Picker
    this.dom.btnClosePicker.addEventListener('click', () => this.closePicker());
    document.querySelectorAll('.btn-picker').forEach(btn => {
      btn.addEventListener('click', () => {
        this.applyShiftToActiveCell(btn.dataset.code);
      });
    });
    this.dom.pickerLockCheckbox.addEventListener('change', (e) => {
      if (this.state.activeCell) {
        const { engIndex, dayIndex } = this.state.activeCell;
        const engName = this.state.gridRoster[engIndex].name;
        if (!this.state.lockedCells[engName]) this.state.lockedCells[engName] = {};
        this.state.lockedCells[engName][dayIndex + 1] = e.target.checked;
        this.saveLockedCells();
        this.renderRoster();
      }
    });

    // Gemini Settings Modal
    this.dom.btnCloseGeminiSettings.addEventListener('click', () => this.dom.geminiSettingsModal.classList.add('hidden'));
    this.dom.btnSaveGeminiSettings.addEventListener('click', () => this.saveGeminiSettings());

    // Atlas Settings Modal
    if (this.dom.btnCloseAtlasConfig) {
      this.dom.btnCloseAtlasConfig.addEventListener('click', () => this.dom.atlasConfigModal.classList.add('hidden'));
      this.dom.btnTestAtlasConn.addEventListener('click', () => this.testAtlasConnection());
      this.dom.btnSaveAtlasConfig.addEventListener('click', () => this.saveAtlasConfig());
    }
  }

  showToast(msg) {
    if (!this.dom.toast) return;
    this.dom.toastMsg.textContent = msg;
    this.dom.toast.style.opacity = '1';
    setTimeout(() => {
      this.dom.toast.style.opacity = '0';
    }, 2800);
  }

  setupAtlasSyncBadge() {
    this.atlasService.onStatusChange((status) => {
      const badge = this.dom.syncBadgeIndicator;
      const text = this.dom.syncBadgeText;
      const dot = badge.querySelector('span');

      if (status === 'synced') {
        badge.className = 'inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-300 cursor-pointer shadow-xs';
        text.textContent = 'Synced to Atlas M0';
        dot.className = 'w-2 h-2 rounded-full bg-emerald-600 inline-block shadow-sm';
      } else if (status === 'unsaved') {
        badge.className = 'inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-300 cursor-pointer shadow-xs';
        text.textContent = 'Unsaved Changes';
        dot.className = 'w-2 h-2 rounded-full bg-amber-600 inline-block animate-pulse';
      } else if (status === 'draft') {
        badge.className = 'inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-purple-100 text-purple-800 border border-purple-300 cursor-pointer shadow-xs';
        text.textContent = 'Gemini Draft Generated';
        dot.className = 'w-2 h-2 rounded-full bg-purple-600 inline-block animate-pulse';
      } else {
        badge.className = 'inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-200 text-slate-700 cursor-pointer';
        text.textContent = 'Local Storage';
        dot.className = 'w-2 h-2 rounded-full bg-slate-500 inline-block';
      }
    });
  }

  updateTeamBadge() {
    const activeCount = this.state.engineersList.filter(e => e.status !== 'Resigned').length;
    if (this.dom.activeTeamBadge) {
      this.dom.activeTeamBadge.textContent = activeCount;
    }
  }

  updateActiveRulesBanner() {
    const c = this.state.constraints;
    if (this.dom.ruleNightText) this.dom.ruleNightText.textContent = `${c.nightHeadcount}/night`;
    if (this.dom.ruleFsText) this.dom.ruleFsText.textContent = `≥${c.fsMin}`;
    if (this.dom.ruleSsText) this.dom.ruleSsText.textContent = `≥${c.ssMin}`;
    if (this.dom.ruleRestText) this.dom.ruleRestText.textContent = c.postNightRestDays === 2 ? '48h (2d)' : '24h (1d)';
    
    if (this.dom.ruleDirectivesSnippet) {
      if (c.customDirectives && c.customDirectives.trim()) {
        const snippet = c.customDirectives.trim();
        this.dom.ruleDirectivesSnippet.textContent = `• Directives: "${snippet.length > 40 ? snippet.substring(0, 40) + '...' : snippet}"`;
      } else {
        this.dom.ruleDirectivesSnippet.textContent = '';
      }
    }

    const minDay = c.daytimeOverlapMin || 3;
    if (this.dom.footerTargetDay) this.dom.footerTargetDay.textContent = `Day Target: ≥${minDay}`;
    if (this.dom.footerTargetNs) this.dom.footerTargetNs.textContent = `Target: ${c.nightHeadcount}`;
  }

  async loadGridRosterForPeriod(year, month) {
    this.state.currentYear = year;
    this.state.currentMonth = month;
    this.state.totalDays = new Date(year, month, 0).getDate();

    const monthKey = `${year}-${String(month).padStart(2, '0')}`;
    const cacheKey = `${STORAGE_KEYS.GRID_ROSTER_PREFIX}${year}_${String(month).padStart(2, '0')}`;
    let loaded = null;

    // 1. Attempt to fetch remote shifts from MongoDB Atlas via /api/roster/<selectedMonth>
    try {
      const remoteDoc = await this.atlasService.getRoster(monthKey);
      if (remoteDoc && remoteDoc.engineers && remoteDoc.engineers.length >= 5) {
        const isComingMonth = (year > 2026 || (year === 2026 && month > 10));
        const remoteEngineers = remoteDoc.engineers.map(e => {
          let shifts = e.shifts;
          if (shifts && !Array.isArray(shifts) && typeof shifts === 'object') {
            shifts = shiftsMapToArray(shifts, this.state.totalDays);
          }
          const defEng = this.state.engineersList.find(x => x.name === e.name || x.id === e.engineer_id || x.id === e.id) || {};
          return {
            id: e.engineer_id || e.id || defEng.id || `eng_${e.name.toLowerCase().replace(/[\s\W]+/g, '')}`,
            name: e.name,
            shifts: Array.isArray(shifts) ? [...shifts] : [],
            status: e.status || defEng.status || 'Active',
            role: e.role || defEng.role || 'engineer',
            default_weekly_offs: defEng.default_weekly_offs || ['Saturday', 'Sunday']
          };
        });

        let filteredRemote = remoteEngineers;
        if (isComingMonth) {
          filteredRemote = filteredRemote.filter(eng => {
            const meta = this.state.engineersList.find(x => x.name === eng.name || x.id === eng.id);
            return meta ? meta.status !== 'Resigned' : eng.status !== 'Resigned';
          });
        }

        this.state.gridRoster = this.sortGridRosterByWeekOffs(filteredRemote);
        this.saveCurrentGridRoster();
        this.atlasService.notifyStatusChange('synced');
        return;
      }
    } catch (err) {
      console.warn(`[Atlas] Remote fetch for ${monthKey} failed, falling back to local storage:`, err);
    }

    // 2. Fallback to LocalStorage (ONLY IF genuinely imported, generated, or edited by user)
    const isImportedOrGenerated = this.isMonthImportedOrGenerated(monthKey);
    if (typeof localStorage !== 'undefined' && isImportedOrGenerated) {
      const stored = localStorage.getItem(cacheKey);
      if (stored) {
        try { loaded = JSON.parse(stored); } catch (e) { console.error('Cache parse error', e); }
      }
    }

    const isComingMonth = (year > 2026 || (year === 2026 && month > 10));

    // Verify loaded actually contains valid shift assignments (not all empty strings)
    const hasValidShifts = loaded && Array.isArray(loaded) && loaded.length >= 5 &&
      loaded.some(eng => Array.isArray(eng.shifts) && eng.shifts.some(s => s && String(s).trim() !== ''));

    if (hasValidShifts) {
      if (isComingMonth) {
        // Exclude resigned engineers in coming months
        this.state.gridRoster = loaded.filter(eng => {
          const meta = this.state.engineersList.find(e => e.name === eng.name || e.id === eng.id);
          return meta ? meta.status !== 'Resigned' : eng.status !== 'Resigned';
        });
      } else {
        this.state.gridRoster = loaded;
      }
      this.state.gridRoster = this.sortGridRosterByWeekOffs(this.state.gridRoster);
      this.atlasService.notifyStatusChange('draft');
      return;
    }

    // 3. Direct relative fetch fallback (e.g., when served via local server or reverse proxy)
    if (typeof fetch !== 'undefined' && typeof window !== 'undefined' && window.location) {
      try {
        const directRes = await fetch(`/api/roster/${monthKey}`, { headers: { 'Accept': 'application/json' } });
        if (directRes.ok) {
          const directData = await directRes.json();
          const doc = directData.document || directData.data || directData;
          if (doc && doc.engineers && doc.engineers.length >= 5) {
            const remoteEngineers = doc.engineers.map(e => {
              let shifts = e.shifts;
              if (shifts && !Array.isArray(shifts) && typeof shifts === 'object') {
                shifts = shiftsMapToArray(shifts, this.state.totalDays);
              }
              const defEng = this.state.engineersList.find(x => x.name === e.name || x.id === e.engineer_id || x.id === e.id) || {};
              return {
                id: e.engineer_id || e.id || defEng.id || `eng_${e.name.toLowerCase().replace(/[\s\W]+/g, '')}`,
                name: e.name,
                shifts: Array.isArray(shifts) ? [...shifts] : [],
                status: e.status || defEng.status || 'Active',
                role: e.role || defEng.role || 'engineer',
                default_weekly_offs: defEng.default_weekly_offs || ['Saturday', 'Sunday']
              };
            });
            this.state.gridRoster = this.sortGridRosterByWeekOffs(remoteEngineers);
            this.saveCurrentGridRoster();
            this.atlasService.notifyStatusChange('synced');
            return;
          }
        }
      } catch (eDirect) {
        console.warn('Direct relative fallback fetch failed:', eDirect);
      }
    }


    // Future / ungenerated months: blank roster with active engineers (no mock data!)
    const activeEngineers = this.state.engineersList.filter(e => e.status !== 'Resigned');
    this.state.gridRoster = this.sortGridRosterByWeekOffs(
      activeEngineers.map(eng => {
        const def = this.state.engineersList.find(x => x.name === eng.name || x.id === eng.id) || {};
        return {
          id: eng.id || def.id || `eng_${eng.name.toLowerCase().replace(/[\s\W]+/g, '')}`,
          name: eng.name,
          shifts: new Array(this.state.totalDays).fill(''),
          status: def.status || eng.status || 'Active',
          role: def.role || eng.role || 'engineer',
          default_weekly_offs: def.default_weekly_offs || eng.default_weekly_offs || ['Saturday', 'Sunday']
        };
      })
    );
    this.atlasService.notifyStatusChange('local');
  }

  saveCurrentGridRoster() {
    const monthKey = `${this.state.currentYear}-${String(this.state.currentMonth).padStart(2, '0')}`;
    const cacheKey = `${STORAGE_KEYS.GRID_ROSTER_PREFIX}${this.state.currentYear}_${String(this.state.currentMonth).padStart(2, '0')}`;
    this.markMonthAsImportedOrGenerated(monthKey);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(cacheKey, JSON.stringify(this.state.gridRoster));
    }
  }

  // --- Shift Visibility & Selective View Filtering ---
  isShiftVisible(shiftCode) {
    if (!shiftCode) return true;
    if (!this.state.visibleShifts) return true;
    const clean = String(shiftCode).trim().toUpperCase();
    if (clean.startsWith('SW')) {
      return this.state.visibleShifts.has('SW');
    }
    const standardCategories = ['OFF', 'NS', 'FS', 'FG', 'G', 'SG', 'SS', 'L'];
    if (standardCategories.includes(clean)) {
      return this.state.visibleShifts.has(clean);
    }
    return this.state.visibleShifts.has('OTHER');
  }

  isShiftFilterActive() {
    const allCategories = ['SW', 'OFF', 'NS', 'FS', 'FG', 'G', 'SG', 'SS', 'L'];
    if (!this.state.visibleShifts) return false;
    return allCategories.some(cat => !this.state.visibleShifts.has(cat));
  }

  setShiftFilterPreset(presetName) {
    const all = ['SW', 'OFF', 'NS', 'FS', 'FG', 'G', 'SG', 'SS', 'L', 'OTHER'];
    if (presetName === 'ALL') {
      this.state.visibleShifts = new Set(all);
    } else if (presetName === 'SW_OFF_NS') {
      // Focus View: Only Swaps (SW*), Weekly Offs (OFF), and Night Shifts (NS)
      this.state.visibleShifts = new Set(['SW', 'OFF', 'NS']);
    } else if (presetName === 'NS_ONLY') {
      this.state.visibleShifts = new Set(['NS']);
    } else if (presetName === 'OFF_ONLY') {
      this.state.visibleShifts = new Set(['OFF']);
    } else if (presetName === 'SW_ONLY') {
      this.state.visibleShifts = new Set(['SW']);
    }
    this.updateShiftFilterUI();
    this.renderRoster();
  }

  toggleShiftVisibility(category) {
    if (!this.state.visibleShifts) {
      this.state.visibleShifts = new Set(['SW', 'OFF', 'NS', 'FS', 'FG', 'G', 'SG', 'SS', 'L', 'OTHER']);
    }
    if (this.state.visibleShifts.has(category)) {
      this.state.visibleShifts.delete(category);
    } else {
      this.state.visibleShifts.add(category);
    }
    this.updateShiftFilterUI();
    this.renderRoster();
  }

  updateShiftFilterUI() {
    if (!this.dom.shiftFilterToolbar) return;

    const visible = this.state.visibleShifts || new Set();
    const isFiltered = this.isShiftFilterActive();

    // 1. Update Preset Buttons styling
    const presets = [
      { id: 'btnPresetAllShifts', active: !isFiltered },
      { id: 'btnPresetSwOffNs', active: visible.has('SW') && visible.has('OFF') && visible.has('NS') && !visible.has('FS') && !visible.has('FG') && !visible.has('G') && !visible.has('SG') && !visible.has('SS') && !visible.has('L') },
      { id: 'btnPresetNsOnly', active: visible.has('NS') && !visible.has('SW') && !visible.has('OFF') && !visible.has('FS') && !visible.has('FG') && !visible.has('G') && !visible.has('SG') && !visible.has('SS') && !visible.has('L') },
      { id: 'btnPresetOffOnly', active: visible.has('OFF') && !visible.has('SW') && !visible.has('NS') && !visible.has('FS') && !visible.has('FG') && !visible.has('G') && !visible.has('SG') && !visible.has('SS') && !visible.has('L') },
      { id: 'btnPresetSwOnly', active: visible.has('SW') && !visible.has('OFF') && !visible.has('NS') && !visible.has('FS') && !visible.has('FG') && !visible.has('G') && !visible.has('SG') && !visible.has('SS') && !visible.has('L') }
    ];

    presets.forEach(p => {
      const btn = this.dom[p.id];
      if (btn) {
        if (p.active) {
          btn.className = 'px-2.5 py-1 rounded-md text-xs font-black transition cursor-pointer bg-indigo-600 text-white shadow-2xs';
        } else {
          btn.className = 'px-2.5 py-1 rounded-md text-xs font-extrabold transition cursor-pointer text-slate-700 hover:text-indigo-700 hover:bg-slate-100';
        }
      }
    });

    // 2. Update Individual Chips
    if (this.dom.shiftFilterPillsContainer) {
      const chips = this.dom.shiftFilterPillsContainer.querySelectorAll('.shift-filter-chip');
      chips.forEach(chip => {
        const cat = chip.dataset.shiftCat;
        const checkEl = chip.querySelector('.chip-check');
        const isCatVisible = visible.has(cat);

        if (isCatVisible) {
          chip.classList.add('active');
          chip.style.backgroundColor = chip.dataset.bg;
          chip.style.color = chip.dataset.text;
          chip.style.borderColor = chip.dataset.border;
          chip.style.borderStyle = 'solid';
          chip.style.opacity = '1';
          chip.style.textDecoration = 'none';
          if (checkEl) checkEl.textContent = '✓';
        } else {
          chip.classList.remove('active');
          chip.style.backgroundColor = '#f1f5f9';
          chip.style.color = '#94a3b8';
          chip.style.borderColor = '#cbd5e1';
          chip.style.borderStyle = 'dashed';
          chip.style.opacity = '0.55';
          chip.style.textDecoration = 'line-through';
          if (checkEl) checkEl.textContent = '✕';
        }
      });
    }

    // 3. Update Badge and Reset Button
    if (this.dom.shiftFilterActiveBadge) {
      if (isFiltered) {
        this.dom.shiftFilterActiveBadge.classList.remove('hidden');
        const hiddenCount = 9 - Array.from(visible).filter(c => ['SW', 'OFF', 'NS', 'FS', 'FG', 'G', 'SG', 'SS', 'L'].includes(c)).length;
        if (this.dom.shiftFilterActiveText) {
          this.dom.shiftFilterActiveText.textContent = `${hiddenCount} Shift Type${hiddenCount > 1 ? 's' : ''} Hidden`;
        }
      } else {
        this.dom.shiftFilterActiveBadge.classList.add('hidden');
      }
    }

    if (this.dom.btnResetShiftFilter) {
      if (isFiltered) {
        this.dom.btnResetShiftFilter.classList.remove('hidden');
      } else {
        this.dom.btnResetShiftFilter.classList.add('hidden');
      }
    }
  }

  // --- Sticky Dual Headers ---
  renderHeaders() {
    const datesContainer = this.dom.headerDatesRow;
    const daysContainer = this.dom.headerDaysRow;
    datesContainer.innerHTML = '';
    daysContainer.innerHTML = '';

    const year = this.state.currentYear;
    const month = this.state.currentMonth;
    const totalDays = this.state.totalDays;

    // Dynamically adjust header and footer container colspan to match totalDays (28/29/30/31)
    if (this.dom.headerDatesColTh) this.dom.headerDatesColTh.colSpan = totalDays;
    if (this.dom.headerDaysColTh) this.dom.headerDaysColTh.colSpan = totalDays;
    if (this.dom.footerDayColTd) this.dom.footerDayColTd.colSpan = totalDays;
    if (this.dom.footerNightColTd) this.dom.footerNightColTd.colSpan = totalDays;

    const dayShortNames = ['SUN', 'MON', 'TUES', 'WED', 'THU', 'FRI', 'SAT'];
    const monthCodes = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    const monthFullNames = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
    if (this.dom.activeMonthDisplay) {
      this.dom.activeMonthDisplay.textContent = `${monthFullNames[month - 1]} ${year}`;
    }
    if (this.dom.periodLabelCell) {
      this.dom.periodLabelCell.textContent = `${monthCodes[month - 1]}-${String(year).slice(-2)}`;
    }

    for (let day = 1; day <= totalDays; day++) {
      const dow = new Date(year, month - 1, day).getDay();
      const dayOfWeek = dayShortNames[dow];

      const isWeekend = (dayOfWeek === 'SAT' || dayOfWeek === 'SUN');

      // Top Date Box (1 to 31)
      const dateBox = document.createElement('div');
      dateBox.className = `cell-box flex items-center justify-center border-r border-slate-300 text-sm font-black ${
        isWeekend ? 'bg-red-50 text-red-700 font-black' : 'bg-slate-100 text-slate-900 font-black'
      }`;
      dateBox.textContent = day;
      dateBox.dataset.day = day;
      datesContainer.appendChild(dateBox);

      // Sub Day-of-week Box
      const dayBox = document.createElement('div');
      dayBox.className = `cell-box flex items-center justify-center border-r border-slate-300 text-[11px] font-black tracking-tight ${
        isWeekend ? 'bg-red-600 text-white font-black' : 'bg-slate-200 text-slate-800 font-black'
      }`;
      dayBox.textContent = dayOfWeek;
      dayBox.dataset.day = day;
      daysContainer.appendChild(dayBox);
    }
  }

  // --- Master Roster Matrix Rendering ---
  renderRoster() {
    const tbody = this.dom.rosterTableBody;
    tbody.innerHTML = '';

    // Maintain Weekoffs Day Order
    this.state.gridRoster = this.sortGridRosterByWeekOffs(this.state.gridRoster);

    const filtered = this.state.gridRoster.filter(eng =>
      eng.name.toLowerCase().includes(this.state.filterTerm)
    );

    this.dom.engineerCountLabel.textContent = filtered.length;

    filtered.forEach((eng) => {
      const actualIndex = this.state.gridRoster.findIndex(e => e.name === eng.name);
      const tr = document.createElement('tr');
      tr.className = 'hover:bg-slate-50/70 transition-colors';

      // 1. Sticky Engineer Name Column
      const nameTd = document.createElement('td');
      nameTd.className = 'name-col sticky left-0 z-20 bg-white border-r border-b border-slate-300 px-3 py-2 text-left font-bold text-slate-900 tracking-wide flex items-center justify-between text-sm';
      nameTd.innerHTML = `
        <span class="truncate font-bold text-slate-900 text-sm" title="${eng.name}">${eng.name}</span>
        <span class="text-[11px] text-slate-400 font-mono font-semibold">#${actualIndex + 1}</span>
      `;
      tr.appendChild(nameTd);

      // 3. Days Cell Group (1 to 31)
      const daysContainerTd = document.createElement('td');
      daysContainerTd.colSpan = this.state.totalDays;
      daysContainerTd.className = 'p-0 border-b border-slate-300';

      const flexRow = document.createElement('div');
      flexRow.className = 'flex';

      let totalWorking = 0;
      let totalNight = 0;
      let totalOff = 0;

      for (let d = 0; d < this.state.totalDays; d++) {
        const rawShift = (eng.shifts && eng.shifts[d] !== undefined && eng.shifts[d] !== null) ? eng.shifts[d] : '';
        const shift = String(rawShift).trim().toUpperCase();
        const dayNum = d + 1;
        const cell = document.createElement('div');

        // Color Class: all codes starting with SW receive the Light Slate swap styling
        let shiftClass = '';
        let isBlank = false;
        if (!shift) {
          shiftClass = 'shift-EMPTY bg-white hover:bg-blue-50/70 text-transparent';
          isBlank = true;
        } else if (shift.startsWith('SW')) {
          shiftClass = `shift-SW shift-${shift}`;
        } else if (!SHIFTS[shift]) {
          shiftClass = 'shift-CUSTOM';
        } else {
          shiftClass = `shift-${shift}`;
        }

        // Locked Pin check (🔒)
        const isLocked = !isBlank && ((this.state.lockedCells[eng.name] && this.state.lockedCells[eng.name][dayNum]) || (shift === 'L') || shift.startsWith('SW'));
        const lockClass = isLocked ? 'cell-locked' : '';

        const isSwap = shift && shift.startsWith('SW');
        const swapDesc = isSwap ? ' (Swap OFF)' : '';
        const isVisible = isBlank || this.isShiftVisible(shift);

        if (!isVisible) {
          // Temporarily cleared/hidden by shift visibility filter (focus view)
          cell.className = `cell-box flex items-center justify-center cursor-pointer shift-cell-hidden ${lockClass}`;
          cell.innerHTML = `<span class="opacity-0 hover:opacity-100 text-[10px] text-slate-400 font-mono select-none">${shift}</span>`;
          cell.title = `${eng.name} • Day ${dayNum}: ${shift}${swapDesc} (Hidden by shift filter - click to inspect/edit)`;
        } else {
          cell.className = `cell-box flex items-center justify-center border-r border-slate-300 cursor-pointer text-[13px] font-extrabold transition hover:opacity-90 hover:ring-2 hover:ring-slate-800 ${shiftClass} ${lockClass}`;
          cell.textContent = shift || '';
          cell.title = isBlank
            ? `${eng.name} • Day ${dayNum}: Blank (Click to assign shift)`
            : `${eng.name} • Day ${dayNum}: ${shift}${swapDesc}${isLocked ? ' (🔒 Locked)' : ''}`;
        }
        cell.tabIndex = 0;
        cell.dataset.engIndex = actualIndex;
        cell.dataset.dayIndex = d;

        cell.onclick = () => this.openPicker(actualIndex, d, eng.name, shift, isLocked);

        flexRow.appendChild(cell);

        // Stats calculation: only count populated shifts
        if (shift === 'OFF' || shift.startsWith('SW')) {
          totalOff++;
        } else if (shift === 'NS') {
          totalNight++;
          totalWorking++;
        } else if (shift === 'L') {
          // Leave day
        } else if (shift) {
          totalWorking++;
        }
      }

      daysContainerTd.appendChild(flexRow);
      tr.appendChild(daysContainerTd);

      // 4. Stats Columns
      const wrkTd = document.createElement('td');
      wrkTd.className = 'cell-box font-black text-sm bg-slate-50 text-slate-900 border-l border-b border-slate-300';
      wrkTd.textContent = totalWorking;
      tr.appendChild(wrkTd);

      const nsTd = document.createElement('td');
      nsTd.className = 'cell-box font-black text-sm bg-indigo-50/50 text-indigo-950 border-l border-b border-slate-300';
      nsTd.textContent = totalNight;
      tr.appendChild(nsTd);

      const offTd = document.createElement('td');
      offTd.className = 'cell-box font-black text-sm bg-amber-50/50 text-amber-950 border-l border-b border-slate-300';
      offTd.textContent = totalOff;
      tr.appendChild(offTd);

      // 5. STATUS Column (Section 4 Layout)
      const statusTd = document.createElement('td');
      statusTd.className = 'status-col bg-slate-50 border-l border-b border-slate-300 px-2 py-1 text-center';
      
      const engMeta = this.state.engineersList.find(e => e.name === eng.name) || { status: eng.status || 'Active' };
      statusTd.innerHTML = this.getStatusPillHtml(engMeta.status || 'Active');
      tr.appendChild(statusTd);

      tbody.appendChild(tr);
    });

    this.renderDailyCoverage();
    this.runRuleAudit();
  }

  getStatusPillHtml(status) {
    const isResigned = status === 'Resigned';
    const displayStatus = isResigned ? 'Resigned' : 'Active';
    const classes = isResigned
      ? 'text-[11px] font-bold px-2.5 py-1 rounded-full inline-block uppercase bg-red-100 text-red-900 border border-red-300 shadow-2xs'
      : 'text-[11px] font-bold px-2.5 py-1 rounded-full inline-block uppercase bg-emerald-100 text-emerald-900 border border-emerald-300 shadow-2xs';
    return `<span class="${classes}">${displayStatus}</span>`;
  }

  // --- Daily Coverage Footers ---
  renderDailyCoverage() {
    // Ensure footer colspan strictly matches current month totalDays
    if (this.dom.footerDayColTd) this.dom.footerDayColTd.colSpan = this.state.totalDays;
    if (this.dom.footerNightColTd) this.dom.footerNightColTd.colSpan = this.state.totalDays;

    const dayContainer = this.dom.coverageDayRow;
    const nightContainer = this.dom.coverageNightRow;
    if (dayContainer) dayContainer.innerHTML = '';
    if (nightContainer) nightContainer.innerHTML = '';

    const c = this.state.constraints;
    const minDayStaff = c.daytimeOverlapMin || 3;
    const hasAnyShifts = this.state.gridRoster.some(e => e.shifts && e.shifts.some(s => s && s.trim() !== ''));

    for (let d = 0; d < this.state.totalDays; d++) {
      let dayCount = 0;
      let nsCount = 0;

      if (hasAnyShifts) {
        this.state.gridRoster.forEach(eng => {
          // Look up engineer metadata for role
          const meta = this.state.engineersList.find(e => e.name === eng.name || e.id === eng.id);
          const role = (meta && meta.role) || eng.role || 'engineer';

          // NOTE: Apprentice Engineers shifts are strictly NOT considered in coverage
          if (role === 'apprentice') {
            return;
          }

          const s = eng.shifts[d];
          if (s === 'NS') {
            nsCount++;
          } else if (s && s !== 'OFF' && !s.startsWith('SW') && s !== 'L' && s.trim() !== '') {
            // Combined Day Shift (all active operational shifts except Night Shift and Rest/Leave/Swap)
            dayCount++;
          }
        });
      }

      // Day Shift Cell (Target >= minDayStaff)
      if (dayContainer) {
        const dayCell = document.createElement('div');
        if (!hasAnyShifts) {
          dayCell.className = 'cell-box flex items-center justify-center border-r border-slate-300 text-slate-400 font-bold';
          dayCell.textContent = '—';
        } else {
          dayCell.className = `cell-box flex items-center justify-center border-r border-slate-300 font-black text-sm ${
            dayCount < minDayStaff ? 'bg-amber-100 text-amber-900 font-black' : 'text-emerald-800'
          }`;
          dayCell.textContent = dayCount;
        }
        dayContainer.appendChild(dayCell);
      }

      // Night Shift Cell (Target strictly == nightHeadcount)
      if (nightContainer) {
        const nightCell = document.createElement('div');
        if (!hasAnyShifts) {
          nightCell.className = 'cell-box flex items-center justify-center border-r border-slate-300 text-slate-400 font-bold';
          nightCell.textContent = '—';
        } else {
          let nsClass = 'text-slate-900 font-black text-sm';
          if (nsCount === 0) {
            nsClass = 'bg-red-200 text-red-900 font-black animate-pulse';
          } else if (nsCount !== c.nightHeadcount) {
            nsClass = 'bg-amber-200 text-amber-900 font-black';
          }
          nightCell.className = `cell-box flex items-center justify-center border-r border-slate-300 font-black text-sm ${nsClass}`;
          nightCell.textContent = nsCount;
        }
        nightContainer.appendChild(nightCell);
      }
    }
  }

  // --- Real-Time Rule Validator (Module 3) ---
  runRuleAudit() {
    const hasAnyShifts = this.state.gridRoster.some(e => e.shifts && e.shifts.some(s => s && s.trim() !== ''));
    if (!hasAnyShifts) {
      this.dom.ruleAuditAlertBanner.classList.add('hidden');
      return;
    }

    const audit = this.aiEngine.auditRoster(this.state.gridRoster, this.state.constraints, this.state.totalDays);
    this.state.auditState = audit;

    const allViolations = [
      ...audit.nightViolations,
      ...audit.deskViolations,
      ...audit.fatigueViolations
    ];

    if (allViolations.length > 0) {
      this.dom.ruleAuditAlertBanner.classList.remove('hidden');
      this.dom.ruleAuditSummaryText.textContent = `Rule Audit Alert (${allViolations.length} issue${allViolations.length > 1 ? 's' : ''}):`;
      
      this.dom.ruleAuditChipsContainer.innerHTML = allViolations.slice(0, 4).map(v => `
        <span class="bg-red-600 text-white px-2 py-0.5 rounded text-[11px] font-mono cursor-pointer hover:bg-red-700" title="${v.message}">
          ${v.day ? 'Day ' + v.day : ''} ${v.type || 'Rule Issue'}
        </span>
      `).join('') + (allViolations.length > 4 ? `<span class="text-xs text-red-700 font-bold">+${allViolations.length - 4} more</span>` : '');
    } else {
      this.dom.ruleAuditAlertBanner.classList.add('hidden');
    }
  }

  openAuditDetailsModal() {
    const audit = this.state.auditState || this.aiEngine.auditRoster(this.state.gridRoster, this.state.constraints, this.state.totalDays);
    const list = this.dom.auditDetailsList;
    list.innerHTML = '';

    const allViolations = [
      ...audit.nightViolations,
      ...audit.deskViolations,
      ...audit.fatigueViolations
    ];

    if (allViolations.length === 0) {
      list.innerHTML = '<div class="p-3 bg-emerald-50 text-emerald-800 rounded-lg font-bold">✅ 100% Compliant: Zero night coverage gaps, zero fatigue violations, core desks staffed!</div>';
    } else {
      allViolations.forEach(v => {
        const item = document.createElement('div');
        item.className = 'p-2.5 rounded bg-red-50 border border-red-200 text-red-900 text-xs flex items-center justify-between';
        item.innerHTML = `
          <span>⚠️ <strong>${v.type || 'FATIGUE_GUARD'}:</strong> ${v.message}</span>
          ${v.day ? `<button class="bg-red-600 text-white px-2 py-0.5 rounded text-[10px] font-bold" onclick="document.querySelector('[data-day=\\'${v.day}\\']')?.scrollIntoView({inline:'center',behavior:'smooth'})">Inspect Day ${v.day}</button>` : ''}
        `;
        list.appendChild(item);
      });
    }

    this.dom.auditDetailsModal.classList.remove('hidden');
  }

  // --- Cell Override Popover (Module 4) ---
  openPicker(engIndex, dayIndex, engName, currentShift, isLocked) {
    this.state.activeCell = { engIndex, dayIndex };
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    this.dom.pickerSubHeader.textContent = `${monthNames[this.state.currentMonth - 1]} ${dayIndex + 1} • ${engName}`;
    
    if (isLocked) {
      this.dom.pickerLockIndicator.classList.remove('hidden');
      this.dom.pickerLockCheckbox.checked = true;
    } else {
      this.dom.pickerLockIndicator.classList.add('hidden');
      this.dom.pickerLockCheckbox.checked = false;
    }

    // Populate Dynamic Peer Swap Buttons (SWM, SWDY, SWSH, SWSI, etc.)
    if (this.dom.pickerSwapButtonsContainer) {
      this.dom.pickerSwapButtonsContainer.innerHTML = '';
      const peers = this.state.engineersList.filter(e => e.status !== 'Resigned' && e.name !== engName);
      peers.forEach(peer => {
        const swCode = getEngineerSwapCode(peer.name, this.state.engineersList);
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn-picker px-2 py-1.5 rounded shift-SW text-xs hover:scale-105 transition flex items-center gap-1 font-bold shadow-2xs';
        btn.dataset.code = swCode;
        btn.title = `Swap with ${peer.name} (${engName} gets OFF; ${peer.name} covers shift)`;
        btn.innerHTML = `<span class="text-amber-900 font-extrabold">${swCode}</span> <span class="text-[10px] font-normal text-slate-600 truncate max-w-[85px]">(${peer.name.split(' ')[0]})</span>`;
        btn.onclick = () => this.applyShiftToActiveCell(swCode);
        this.dom.pickerSwapButtonsContainer.appendChild(btn);
      });
    }

    this.dom.shiftPickerModal.classList.remove('hidden');
  }

  closePicker() {
    this.dom.shiftPickerModal.classList.add('hidden');
    this.state.activeCell = null;
  }

  applyShiftToActiveCell(shiftCode) {
    if (!this.state.activeCell) return;
    const { engIndex, dayIndex } = this.state.activeCell;
    const engName = this.state.gridRoster[engIndex].name;
    const dayNum = dayIndex + 1;
    const normalizedCode = (shiftCode || '').trim().toUpperCase();

    this.state.gridRoster[engIndex].shifts[dayIndex] = normalizedCode;

    if (this.dom.pickerLockCheckbox.checked || normalizedCode === 'L' || (normalizedCode && normalizedCode.startsWith('SW'))) {
      if (!this.state.lockedCells[engName]) this.state.lockedCells[engName] = {};
      this.state.lockedCells[engName][dayNum] = true;
    } else if (this.state.lockedCells[engName]) {
      delete this.state.lockedCells[engName][dayNum];
    }

    this.saveLockedCells();
    this.saveCurrentGridRoster();
    this.renderRoster();
    this.closePicker();
    this.atlasService.notifyStatusChange('unsaved');
    if (normalizedCode) {
      this.showToast(`Updated ${engName} to [${normalizedCode}] on Day ${dayNum}`);
    } else {
      this.showToast(`Cleared Day ${dayNum} for ${engName}`);
    }
  }

  // --- MODAL 1: Engineer Directory & Lifecycle Management (Module A) ---
  openTeamModal() {
    this.renderTeamTable();
    this.dom.engineerFormContainer.classList.add('hidden');
    this.dom.teamModal.classList.remove('hidden');
  }

  renderTeamTable() {
    const tbody = this.dom.teamTableBody;
    tbody.innerHTML = '';

    // Maintain Weekoffs Day Order
    this.state.engineersList = this.sortEngineersByWeekOffs(this.state.engineersList);

    this.state.engineersList.forEach((eng, idx) => {
      const tr = document.createElement('tr');
      const isEditing = this.state.editingEngineerId && (eng.id === this.state.editingEngineerId || eng.name === this.state.editingEngineerId);
      tr.className = isEditing ? 'bg-blue-50/80 border-l-4 border-l-blue-600 transition' : 'hover:bg-slate-50 transition';

      const statusPillHtml = this.getStatusPillHtml(eng.status);

      let roleBadgeHtml = '';
      if (eng.role === 'admin') {
        roleBadgeHtml = '<span class="px-2 py-0.5 rounded text-[11px] font-bold bg-purple-100 text-purple-800 border border-purple-200">Lead Admin</span>';
      } else if (eng.role === 'apprentice') {
        roleBadgeHtml = '<span class="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-900 border border-amber-300 inline-flex items-center gap-1"><i class="ph ph-student"></i> Apprentice</span>';
      } else {
        roleBadgeHtml = '<span class="px-2 py-0.5 rounded text-[11px] font-semibold bg-slate-100 text-slate-700">NOC Engineer</span>';
      }

      const username = this.getEngineerUsername(eng);
      tr.innerHTML = `
        <td class="p-2.5">
          <div class="font-bold text-slate-900">${eng.name}</div>
          <div class="text-[11px] text-slate-500">${eng.full_name || '—'}</div>
        </td>
        <td class="p-2.5">
          <span class="inline-flex items-center gap-1 font-mono font-bold text-xs px-2.5 py-1 rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-2xs" title="Portal Login Username">
            <i class="ph ph-user text-xs text-indigo-500"></i>
            ${username}
          </span>
        </td>
        <td class="p-2.5">
          <div class="text-slate-800">${eng.email || '—'}</div>
          <div class="text-[11px] text-slate-400 font-mono">${eng.phone || '—'}</div>
        </td>
        <td class="p-2.5">${roleBadgeHtml}</td>
        <td class="p-2.5">${statusPillHtml}</td>
        <td class="p-2.5">
          <div class="flex gap-1">
            <span class="px-1.5 py-0.5 rounded bg-yellow-100 text-yellow-900 text-[10px] font-bold border border-yellow-300">
              ${(eng.default_weekly_offs && eng.default_weekly_offs[0]) ? eng.default_weekly_offs[0].substring(0, 3) : 'OFF'}
            </span>
            <span class="px-1.5 py-0.5 rounded bg-yellow-100 text-yellow-900 text-[10px] font-bold border border-yellow-300">
              ${(eng.default_weekly_offs && eng.default_weekly_offs[1]) ? eng.default_weekly_offs[1].substring(0, 3) : 'OFF'}
            </span>
          </div>
        </td>
        <td class="p-2.5 text-[11px] font-mono">
          <div>Join: ${eng.join_date || '—'}</div>
          ${eng.resignation_date ? `<div class="text-red-600">Exit: ${eng.resignation_date}</div>` : ''}
        </td>
        <td class="p-2.5 text-right space-x-1.5 whitespace-nowrap sticky right-0 ${isEditing ? 'bg-blue-50' : 'bg-white'} shadow-[-4px_0_6px_-2px_rgba(0,0,0,0.06)] min-w-[140px]">
          <button type="button" data-action="edit-engineer" data-idx="${idx}" class="inline-flex items-center gap-1 bg-blue-600 hover:bg-blue-700 text-white px-2.5 py-1 rounded text-xs font-bold transition shadow-xs cursor-pointer" onclick="window.rosterApp.openEngineerFormByIndex(${idx})">
            <i class="ph ph-pencil-simple text-xs"></i> Edit
          </button>
          <button type="button" data-action="retire-engineer" data-idx="${idx}" class="inline-flex items-center gap-1 bg-slate-100 hover:bg-red-50 text-slate-700 hover:text-red-700 border border-slate-300 hover:border-red-200 px-2 py-1 rounded text-xs font-semibold transition cursor-pointer" onclick="window.rosterApp.toggleRetireEngineer(${idx})">
            ${eng.status === 'Resigned' ? '<span class="text-emerald-700 font-bold"><i class="ph ph-check-circle text-emerald-600"></i> Activate</span>' : '<i class="ph ph-user-minus text-slate-400"></i> Retire'}
          </button>
        </td>
      `;

      tbody.appendChild(tr);
    });
  }

  openEngineerFormByIndex(idx) {
    this.openEngineerForm(this.state.engineersList[idx], idx);
  }

  openEngineerForm(eng = null, idx = null) {
    this.state.editingEngineerId = eng ? eng.id : null;
    this.dom.engineerFormContainer.classList.remove('hidden');

    if (eng) {
      const username = this.getEngineerUsername(eng);
      this.dom.engineerFormTitle.innerHTML = `<i class="ph ph-pencil-simple text-blue-600"></i> Edit Engineer: <span class="text-blue-700 font-black">${eng.name}</span> <span class="ml-2 font-mono text-xs font-bold px-2 py-0.5 rounded-md bg-indigo-100 text-indigo-800 border border-indigo-200">@${username}</span>`;
      this.dom.formEngId.value = eng.id || '';
      this.dom.formEngName.value = eng.name || '';
      if (this.dom.formEngUsername) this.dom.formEngUsername.value = username;
      this.dom.formEngFullName.value = eng.full_name || '';
      this.dom.formEngEmail.value = eng.email || '';
      this.dom.formEngPhone.value = eng.phone || '';
      this.dom.formEngRole.value = eng.role || 'engineer';
      this.dom.formEngStatus.value = eng.status || 'Active';
      this.dom.formEngJoinDate.value = eng.join_date || '';
      this.dom.formEngResignDate.value = eng.resignation_date || '';
      
      const offs = eng.default_weekly_offs || ['Saturday', 'Sunday'];
      this.dom.formEngOff1.value = offs[0] || 'Saturday';
      this.dom.formEngOff2.value = offs[1] || 'Sunday';
      if (this.dom.btnDeleteEngineerForm) this.dom.btnDeleteEngineerForm.classList.remove('hidden');
    } else {
      this.dom.engineerFormTitle.innerHTML = `<i class="ph ph-user-plus text-blue-600"></i> Add New Engineer`;
      this.dom.formEngId.value = `eng_${Date.now().toString().slice(-4)}`;
      this.dom.formEngName.value = '';
      if (this.dom.formEngUsername) this.dom.formEngUsername.value = '';
      this.dom.formEngFullName.value = '';
      this.dom.formEngEmail.value = '';
      this.dom.formEngPhone.value = '';
      this.dom.formEngRole.value = 'engineer';
      this.dom.formEngStatus.value = 'Active';
      this.dom.formEngJoinDate.value = new Date().toISOString().split('T')[0];
      this.dom.formEngResignDate.value = '';
      this.dom.formEngOff1.value = 'Saturday';
      this.dom.formEngOff2.value = 'Sunday';
      if (this.dom.btnDeleteEngineerForm) this.dom.btnDeleteEngineerForm.classList.add('hidden');
    }

    // Auto-scroll directly to editing section with smooth animation
    this.dom.engineerFormContainer.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    setTimeout(() => {
      if (this.dom.formEngName) this.dom.formEngName.focus();
    }, 120);

    // Re-render table to display active row highlight
    this.renderTeamTable();
  }

  closeEngineerForm() {
    this.state.editingEngineerId = null;
    this.dom.engineerFormContainer.classList.add('hidden');
    if (this.dom.btnDeleteEngineerForm) this.dom.btnDeleteEngineerForm.classList.add('hidden');
    this.renderTeamTable();
  }

  async handleDeleteEngineer() {
    const id = this.dom.formEngId.value.trim();
    const name = this.dom.formEngName.value.trim().toUpperCase();
    const username = this.dom.formEngUsername ? this.dom.formEngUsername.value.trim().toLowerCase() : '';

    if (!id && !name) {
      alert('Unable to identify engineer to delete.');
      return;
    }

    const confirmMsg = `Are you sure you want to permanently delete engineer "${name}"?\n\nThis will remove them from the active directory, MongoDB Atlas database, and roster grids.`;
    if (!window.confirm(confirmMsg)) {
      return;
    }

    try {
      const res = await fetch('/api/engineers/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, name, username })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.success) {
        console.log(`[DeleteEngineer] Server confirmed deletion: ${data.message}`);
      }
    } catch (err) {
      console.warn(`[DeleteEngineer] Server delete request warning:`, err);
    }

    // Remove from local state
    this.state.engineersList = this.state.engineersList.filter(e => {
      const matchId = id && e.id === id;
      const matchName = name && e.name.toUpperCase() === name;
      const matchUser = username && e.username && e.username.toLowerCase() === username;
      return !(matchId || matchName || matchUser);
    });

    this.saveEngineersList();
    if (typeof window !== 'undefined' && window.authService && typeof window.authService.syncEngineersList === 'function') {
      window.authService.syncEngineersList(this.state.engineersList);
    }

    this.closeEngineerForm();
    this.renderTeamTable();
    this.showToast(`Engineer "${name}" has been permanently deleted.`);
  }

  resetEngineerFormFields() {
    this.dom.formEngName.value = '';
    if (this.dom.formEngUsername) this.dom.formEngUsername.value = '';
    this.dom.formEngFullName.value = '';
    this.dom.formEngEmail.value = '';
    this.dom.formEngPhone.value = '';
    this.dom.formEngRole.value = 'engineer';
    this.dom.formEngStatus.value = 'Active';
    this.dom.formEngJoinDate.value = new Date().toISOString().split('T')[0];
    this.dom.formEngResignDate.value = '';
    this.dom.formEngOff1.value = 'Saturday';
    this.dom.formEngOff2.value = 'Sunday';
    if (this.dom.formEngName) this.dom.formEngName.focus();
  }

  handleSaveEngineer() {
    const id = this.dom.formEngId.value.trim() || `eng_${Date.now().toString().slice(-4)}`;
    const name = this.dom.formEngName.value.trim().toUpperCase();
    const email = this.dom.formEngEmail.value.trim();
    const rawUser = this.dom.formEngUsername ? this.dom.formEngUsername.value.trim().toLowerCase() : '';
    const username = rawUser || this.getEngineerUsername({ name, email });
    const fullName = this.dom.formEngFullName.value.trim();
    const phone = this.dom.formEngPhone.value.trim();
    const role = this.dom.formEngRole.value;
    const status = this.dom.formEngStatus.value;
    const joinDate = this.dom.formEngJoinDate.value || null;
    const resignDate = this.dom.formEngResignDate.value || null;
    const off1 = this.dom.formEngOff1.value;
    const off2 = this.dom.formEngOff2.value;

    if (!name) {
      alert('Roster display name is required.');
      return;
    }

    const existingIdx = this.state.engineersList.findIndex(e => e.id === id || (e.name && e.name.trim().toUpperCase() === name));
    const engData = {
      id,
      name,
      username,
      full_name: fullName,
      email,
      phone,
      role,
      status,
      join_date: joinDate,
      resignation_date: resignDate,
      default_weekly_offs: [off1, off2],
      preferred_shifts: ["FS", "FG", "SS"]
    };

    const isComingMonth = (this.state.currentYear > 2026 || (this.state.currentYear === 2026 && this.state.currentMonth > 10));

    if (existingIdx >= 0) {
      this.state.engineersList[existingIdx] = engData;
      if (isComingMonth && status === 'Resigned') {
        this.state.gridRoster = this.state.gridRoster.filter(e => (e.name && e.name.trim().toUpperCase() !== name) && e.id !== id);
      } else {
        const gridIdx = this.state.gridRoster.findIndex(e => (e.name && e.name.trim().toUpperCase() === name) || e.id === id);
        if (gridIdx >= 0) {
          this.state.gridRoster[gridIdx].status = status;
          this.state.gridRoster[gridIdx].role = role;
          this.state.gridRoster[gridIdx].default_weekly_offs = [off1, off2];
        } else if (status === 'Active') {
          this.state.gridRoster.push({
            id,
            name,
            shifts: new Array(this.state.totalDays).fill('OFF'),
            status: 'Active',
            role,
            default_weekly_offs: [off1, off2]
          });
        }
      }
    } else {
      this.state.engineersList.push(engData);
      if (!(isComingMonth && status === 'Resigned')) {
        const newShifts = new Array(this.state.totalDays).fill('OFF');
        this.state.gridRoster.push({
          id,
          name,
          shifts: newShifts,
          status,
          role,
          default_weekly_offs: [off1, off2]
        });
      }
    }

    this.state.engineersList = this.sortEngineersByWeekOffs(this.state.engineersList);
    this.state.gridRoster = this.sortGridRosterByWeekOffs(this.state.gridRoster);

    this.saveEngineersList();
    this.saveCurrentGridRoster();
    this.updateTeamBadge();
    this.renderTeamTable();
    this.renderRoster();
    this.closeEngineerForm();

    // 1. Sync updated email & phone into authService user accounts and active session
    if (typeof window !== 'undefined' && window.authService) {
      window.authService.syncEngineerProfile(engData);
    }

    // 2. Persist to MongoDB Atlas asynchronously
    this.atlasService.saveEngineers(this.state.engineersList).then(res => {
      if (res && res.success) {
        this.atlasService.notifyStatusChange('synced');
        console.log(`[Atlas] Synced engineer ${name} to database.`);
      }
    }).catch(err => {
      console.warn('[Atlas] Failed to sync engineer to database:', err);
    });

    this.showToast(`Saved engineer record: ${name}`);
  }

  toggleRetireEngineer(idx) {
    const eng = this.state.engineersList[idx];
    if (!eng) return;

    if (eng.status === 'Resigned') {
      eng.status = 'Active';
      eng.resignation_date = null;
      this.showToast(`${eng.name} reactivated!`);
    } else {
      eng.status = 'Resigned';
      eng.resignation_date = new Date().toISOString().split('T')[0];
      this.showToast(`${eng.name} marked as Resigned (excluded from upcoming month rosters).`);
    }

    const isComingMonth = (this.state.currentYear > 2026 || (this.state.currentYear === 2026 && this.state.currentMonth > 10));
    if (isComingMonth && eng.status === 'Resigned') {
      this.state.gridRoster = this.state.gridRoster.filter(e => e.name !== eng.name && e.id !== eng.id);
    } else if (isComingMonth && eng.status === 'Active') {
      const gridIdx = this.state.gridRoster.findIndex(e => e.name === eng.name);
      if (gridIdx < 0) {
        this.state.gridRoster.push({
          id: eng.id,
          name: eng.name,
          shifts: new Array(this.state.totalDays).fill('OFF'),
          status: 'Active',
          role: eng.role || 'engineer'
        });
      } else {
        this.state.gridRoster[gridIdx].status = 'Active';
      }
    } else {
      const gridIdx = this.state.gridRoster.findIndex(e => e.name === eng.name);
      if (gridIdx >= 0) {
        this.state.gridRoster[gridIdx].status = eng.status;
      }
    }

    this.saveEngineersList();
    this.saveCurrentGridRoster();
    this.atlasService.saveEngineers(this.state.engineersList);
    this.updateTeamBadge();
    this.renderTeamTable();
    this.renderRoster();
  }

  // --- MODAL 2: Operational Constraints (Module B) ---
  openConstraintsModal() {
    const c = this.state.constraints;
    this.dom.cfgNightHeadcount.value = c.nightHeadcount || 1;
    this.dom.cfgFsMin.value = c.fsMin || 1;
    this.dom.cfgSsMin.value = c.ssMin || 1;
    this.dom.cfgDaytimeOverlap.value = c.daytimeOverlapMin || 3;
    this.dom.cfgPostNightRest.value = String(c.postNightRestDays || 2);
    this.dom.cfgMaxConsecutive.value = c.maxConsecutiveDays || 6;
    this.dom.cfgCustomDirectives.value = c.customDirectives || '';
    this.dom.constraintsModal.classList.remove('hidden');
  }

  saveOperationalConstraints() {
    this.state.constraints.nightHeadcount = parseInt(this.dom.cfgNightHeadcount.value, 10) || 1;
    this.state.constraints.fsMin = parseInt(this.dom.cfgFsMin.value, 10) || 1;
    this.state.constraints.ssMin = parseInt(this.dom.cfgSsMin.value, 10) || 1;
    this.state.constraints.daytimeOverlapMin = parseInt(this.dom.cfgDaytimeOverlap.value, 10) || 3;
    this.state.constraints.postNightRestDays = parseInt(this.dom.cfgPostNightRest.value, 10) || 2;
    this.state.constraints.maxConsecutiveDays = parseInt(this.dom.cfgMaxConsecutive.value, 10) || 6;
    this.state.constraints.customDirectives = this.dom.cfgCustomDirectives.value.trim();

    this.saveConstraints();
    this.updateActiveRulesBanner();
    this.renderDailyCoverage();
    this.runRuleAudit();
    this.dom.constraintsModal.classList.add('hidden');
    this.showToast('Operational policies saved & applied to live validator!');

    // Asynchronously sync operational constraints to MongoDB Atlas backend
    try {
      const base = this.atlasService.getApiBase();
      fetch(`${base}/constraints`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.state.constraints)
      }).catch(err => console.warn('Atlas constraints sync background notice:', err));
    } catch (e) {
      console.warn('Failed to dispatch constraints sync:', e);
    }
  }

  // --- MODAL 3: Swaps & Absences Ledger (Module D) ---
  openSwapsModal() {
    this.populateSwapsDropdowns();
    this.renderMutualSwapsList();
    this.renderAbsenceRequestsList();
    this.switchSwapsTab('tabMutual');
    this.dom.swapsModal.classList.remove('hidden');
  }

  switchSwapsTab(tabId) {
    if (tabId === 'tabMutual') {
      this.dom.tabMutual.classList.remove('hidden');
      this.dom.tabSmart.classList.add('hidden');
      this.dom.tabBtnMutualSwaps.className = 'px-4 py-2.5 border-b-2 border-amber-600 text-amber-700 font-bold flex items-center gap-1.5';
      this.dom.tabBtnSmartAbsences.className = 'px-4 py-2.5 border-b-2 border-transparent hover:text-slate-900 flex items-center gap-1.5';
    } else {
      this.dom.tabMutual.classList.add('hidden');
      this.dom.tabSmart.classList.remove('hidden');
      this.dom.tabBtnMutualSwaps.className = 'px-4 py-2.5 border-b-2 border-transparent hover:text-slate-900 flex items-center gap-1.5';
      this.dom.tabBtnSmartAbsences.className = 'px-4 py-2.5 border-b-2 border-purple-600 text-purple-700 font-bold flex items-center gap-1.5';
    }
  }

  populateSwapsDropdowns() {
    const selA = this.dom.mutSwapEngA;
    const selB = this.dom.mutSwapEngB;
    const selReq = this.dom.absReqEngineer;

    const activeEngs = this.state.engineersList.filter(e => e.status !== 'Resigned');

    selA.innerHTML = activeEngs.map(e => `<option value="${e.name}">${e.name}</option>`).join('');
    selB.innerHTML = activeEngs.map(e => `<option value="${e.name}">${e.name}</option>`).join('');
    selReq.innerHTML = activeEngs.map(e => `<option value="${e.id}">${e.name}</option>`).join('');

    if (selB.options && selB.options.length > 1) selB.selectedIndex = 1;
    this.dom.mutSwapDay1.max = this.state.totalDays;
    this.dom.mutSwapDay2.max = this.state.totalDays;
    this.dom.absReqDay.max = this.state.totalDays;
  }

  handleAddMutualSwap() {
    const day1 = parseInt(this.dom.mutSwapDay1.value, 10);
    const engA = this.dom.mutSwapEngA.value;
    const engB = this.dom.mutSwapEngB.value;
    const day2 = parseInt(this.dom.mutSwapDay2.value, 10);

    if (engA === engB) {
      alert('Engineer A and Engineer B must be different individuals.');
      return;
    }

    const codeForA = getEngineerSwapCode(engB, this.state.engineersList);
    const codeForB = getEngineerSwapCode(engA, this.state.engineersList);

    const swapObj = {
      id: `swap_${Date.now()}`,
      day1,
      engineer_a: engA,
      engineer_b: engB,
      day2,
      shift1: codeForA,
      shift2: codeForB
    };

    this.state.manualSwaps.push(swapObj);
    this.saveManualSwaps();

    // Lock cells in grid
    if (!this.state.lockedCells[engA]) this.state.lockedCells[engA] = {};
    if (!this.state.lockedCells[engB]) this.state.lockedCells[engB] = {};
    this.state.lockedCells[engA][day1] = true;
    this.state.lockedCells[engB][day1] = true;
    this.state.lockedCells[engA][day2] = true;
    this.state.lockedCells[engB][day2] = true;
    this.saveLockedCells();

    // Directly assign swap codes to grid if viewing this period
    const idxA = this.state.gridRoster.findIndex(e => e.name === engA);
    const idxB = this.state.gridRoster.findIndex(e => e.name === engB);
    if (idxA !== -1 && day1 >= 1 && day1 <= this.state.totalDays) {
      this.state.gridRoster[idxA].shifts[day1 - 1] = codeForA;
    }
    if (idxB !== -1 && day2 >= 1 && day2 <= this.state.totalDays) {
      this.state.gridRoster[idxB].shifts[day2 - 1] = codeForB;
    }
    this.saveCurrentGridRoster();

    this.renderMutualSwapsList();
    this.renderRoster();
    this.showToast(`Mutual swap registered: ${engA} [${codeForA}] & ${engB} [${codeForB}]`);
  }

  renderMutualSwapsList() {
    const container = this.dom.mutualSwapsList;
    if (this.state.manualSwaps.length === 0) {
      container.innerHTML = '<span class="text-slate-400 italic">No mutual swaps registered.</span>';
      return;
    }

    container.innerHTML = this.state.manualSwaps.map((sw, idx) => `
      <div class="p-2.5 bg-amber-50 border border-amber-200 rounded-lg flex items-center justify-between text-amber-900 text-xs">
        <div>
          <strong>🔒 Day ${sw.day1}:</strong> ${sw.engineer_a} receives <code>${sw.shift1}</code> (covered by ${sw.engineer_b})<br>
          <strong>🔒 Day ${sw.day2}:</strong> ${sw.engineer_b} receives <code>${sw.shift2}</code> (covered by ${sw.engineer_a})
        </div>
        <button class="text-amber-700 hover:text-amber-900 font-black p-1 text-sm" onclick="window.rosterApp.removeMutualSwap(${idx})">✕</button>
      </div>
    `).join('');
  }

  removeMutualSwap(idx) {
    this.state.manualSwaps.splice(idx, 1);
    this.saveManualSwaps();
    this.renderMutualSwapsList();
    this.showToast('Mutual swap removed.');
  }

  handleAddAbsenceRequest() {
    const engId = this.dom.absReqEngineer.value;
    const eng = this.state.engineersList.find(e => e.id === engId);
    const day = parseInt(this.dom.absReqDay.value, 10);

    if (!eng || !day) return;

    this.state.absenceRequests.push({
      id: `abs_${Date.now()}`,
      engineer_id: eng.id,
      engineer_name: eng.name,
      day: day,
      reason: 'Unscheduled absence'
    });

    this.saveAbsenceRequests();
    this.renderAbsenceRequestsList();
    this.showToast(`Submitted absence request for ${eng.name} on Day ${day} for AI arbitration.`);
  }

  renderAbsenceRequestsList() {
    const container = this.dom.absenceRequestsList;
    if (this.state.absenceRequests.length === 0) {
      container.innerHTML = '<span class="text-slate-400 italic">No absence requests registered.</span>';
      return;
    }

    container.innerHTML = this.state.absenceRequests.map((req, idx) => `
      <div class="p-2.5 bg-purple-50 border border-purple-200 rounded-lg flex items-center justify-between text-purple-900 text-xs">
        <div>
          <span>✨ Day ${req.day}: <strong>${req.engineer_name}</strong></span>
          <span class="text-[11px] text-purple-600 block">AI will arbitrate reciprocal swap with eligible peer before applying Leave (L)</span>
        </div>
        <button class="text-purple-700 hover:text-purple-900 font-black p-1 text-sm" onclick="window.rosterApp.removeAbsenceRequest(${idx})">✕</button>
      </div>
    `).join('');
  }

  removeAbsenceRequest(idx) {
    this.state.absenceRequests.splice(idx, 1);
    this.saveAbsenceRequests();
    this.renderAbsenceRequestsList();
    this.showToast('Absence request removed.');
  }

  // --- MODAL 4: Gemini AI Smart Roster Assistant ---
  openAiModal() {
    if (this.aiAssistant) {
      this.aiAssistant.openPanel();
      return;
    }
    const monthNames = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
    let targetYear = this.state.currentYear;
    let targetMonth = this.state.currentMonth;

    // If currently on October 2026, suggest November 2026 as target
    if (targetYear === 2026 && targetMonth === 10) {
      targetMonth = 11;
    }

    const totalDays = new Date(targetYear, targetMonth, 0).getDate();
    this.dom.aiModalPeriodLabel.textContent = `${monthNames[targetMonth - 1]} ${targetYear} (${totalDays} Days)`;

    const activeEngs = this.aiEngine.getActiveEngineersForPeriod(this.state.engineersList, targetYear, targetMonth);
    this.dom.aiActiveEngineersCount.textContent = activeEngs.length;

    this.dom.aiProgressBox.classList.add('hidden');
    this.dom.aiScorecard.classList.add('hidden');
    this.dom.btnApplyAiDraft.classList.add('hidden');
    this.dom.aiModal.classList.remove('hidden');
  }

  async runAiGeneration() {
    let targetYear = this.state.currentYear;
    let targetMonth = this.state.currentMonth;
    if (targetYear === 2026 && targetMonth === 10) {
      targetMonth = 11;
    }

    this.dom.aiProgressBox.classList.remove('hidden');
    this.dom.aiProgressText.textContent = 'Checking active engineers & operational constraints...';
    this.dom.aiScorecard.classList.add('hidden');
    this.dom.btnApplyAiDraft.classList.add('hidden');

    try {
      const result = await this.aiEngine.generateRosterWithGemini({
        year: targetYear,
        month: targetMonth,
        engineers: this.state.engineersList,
        constraints: this.state.constraints,
        manualSwaps: this.state.manualSwaps,
        absenceRequests: this.state.absenceRequests,
        onProgress: (msg) => {
          this.dom.aiProgressText.textContent = msg;
        }
      });

      this.dom.aiProgressBox.classList.add('hidden');

      if (result && result.success) {
        this.state.generatedDraft = result;
        this.dom.aiScorecard.classList.remove('hidden');
        this.dom.aiScoreBadge.textContent = `${result.compliance_summary.score}% Score`;
        this.dom.aiNightCov.textContent = `${result.compliance_summary.audit.nightScore}%`;
        this.dom.aiFatigueViolations.textContent = result.compliance_summary.audit.fatigueViolations.length;
        this.dom.aiEngineLabel.textContent = result.source === 'gemini_api' ? 'Gemini 1.5' : 'Smart Swap Engine';

        // Render AI-Arbitrated Swaps
        const swapsBox = this.dom.aiArbitratedSwapsBox;
        const swapsList = this.dom.aiArbitratedSwapsList;
        if (result.ai_arbitrated_swaps && result.ai_arbitrated_swaps.length > 0) {
          swapsBox.classList.remove('hidden');
          swapsList.innerHTML = result.ai_arbitrated_swaps.map(s => `
            <div class="p-2 bg-white rounded border border-purple-200">
              <span class="font-bold text-purple-900">${s.requester_id || 'Engineer'} off on ${s.swap_date}</span>
              <span>covered by <strong>${s.covered_by_id || 'Peer'}</strong>; payback: ${s.reswap_date || 'later'}.</span>
              <div class="text-[10px] text-slate-500 italic mt-0.5">${s.reasoning || 'Swap-First compliance priority over Leave'}</div>
            </div>
          `).join('');
        } else {
          swapsBox.classList.add('hidden');
        }

        this.dom.btnApplyAiDraft.classList.remove('hidden');
        this.showToast('Gemini AI Smart Roster generated successfully!');
      }
    } catch (err) {
      this.dom.aiProgressBox.classList.add('hidden');
      alert('Error generating roster: ' + err.message);
    }
  }

  applyAiDraft() {
    if (!this.state.generatedDraft) return;
    const { year, month, totalDays, engineers, ai_arbitrated_swaps } = this.state.generatedDraft;

    this.state.currentYear = year;
    this.state.currentMonth = month;
    this.state.totalDays = totalDays;

    this.state.gridRoster = this.sortGridRosterByWeekOffs(engineers.map(e => {
      const meta = this.state.engineersList.find(x => x.name === e.name) || {};
      return {
        ...e,
        status: meta.status || 'Active',
        role: meta.role || 'engineer',
        default_weekly_offs: meta.default_weekly_offs || e.default_weekly_offs || ['Saturday', 'Sunday']
      };
    }));

    // Apply locked cells from mutual swaps & leaves
    this.state.lockedCells = {};
    this.state.manualSwaps.forEach(sw => {
      if (!this.state.lockedCells[sw.engineer_a]) this.state.lockedCells[sw.engineer_a] = {};
      if (!this.state.lockedCells[sw.engineer_b]) this.state.lockedCells[sw.engineer_b] = {};
      this.state.lockedCells[sw.engineer_a][sw.day1] = true;
      this.state.lockedCells[sw.engineer_b][sw.day1] = true;
      this.state.lockedCells[sw.engineer_a][sw.day2] = true;
      this.state.lockedCells[sw.engineer_b][sw.day2] = true;
    });

    if (ai_arbitrated_swaps) {
      ai_arbitrated_swaps.forEach(s => {
        const d1 = parseInt(s.swap_date.split('-')[2] || s.swap_date, 10);
        const reqName = this.state.engineersList.find(x => x.id === s.requester_id)?.name || s.requester_id;
        const covName = this.state.engineersList.find(x => x.id === s.covered_by_id)?.name || s.covered_by_id;
        if (reqName && d1) {
          if (!this.state.lockedCells[reqName]) this.state.lockedCells[reqName] = {};
          this.state.lockedCells[reqName][d1] = true;
        }
        if (covName && d1) {
          if (!this.state.lockedCells[covName]) this.state.lockedCells[covName] = {};
          this.state.lockedCells[covName][d1] = true;
        }
      });
    }

    this.saveLockedCells();
    this.saveCurrentGridRoster();

    // Update select element
    const val = `${year}-${String(month).padStart(2, '0')}`;
    let opt = Array.from(this.dom.monthSelect.options).find(o => o.value === val);
    if (!opt) {
      const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
      opt = new Option(`${monthNames[month - 1]} ${year} (${monthNames[month - 1]}-${String(year).slice(-2)})`, val);
      this.dom.monthSelect.add(opt);
    }
    this.dom.monthSelect.value = val;

    this.renderHeaders();
    this.renderRoster();
    this.dom.aiModal.classList.add('hidden');
    this.atlasService.notifyStatusChange('draft');
    this.showToast(`Applied generated roster for ${val}!`);
  }

  // --- Period Selector Change ---
  navigateMonth(delta) {
    if (!this.dom.monthSelect) return;
    const curIdx = this.dom.monthSelect.selectedIndex;
    const newIdx = curIdx + delta;
    if (newIdx >= 0 && newIdx < this.dom.monthSelect.options.length) {
      this.dom.monthSelect.selectedIndex = newIdx;
      this.handlePeriodChange();
    }
  }

  async handlePeriodChange() {
    const val = this.dom.monthSelect.value;
    const [yStr, mStr] = val.split('-');
    const year = parseInt(yStr, 10);
    const month = parseInt(mStr, 10);

    await this.loadGridRosterForPeriod(year, month);
    this.renderHeaders();
    this.renderRoster();
    this.showToast(`Switched view to ${val}`);
  }

  async clearMonthGrid(targetYear = this.state.currentYear, targetMonth = this.state.currentMonth) {
    if (this.state.currentYear !== targetYear || this.state.currentMonth !== targetMonth) {
      this.state.currentYear = targetYear;
      this.state.currentMonth = targetMonth;
      if (this.dom.monthSelect) {
        this.dom.monthSelect.value = `${targetYear}-${String(targetMonth).padStart(2, '0')}`;
      }
      await this.loadGridRosterForPeriod(targetYear, targetMonth);
    }

    const totalDays = this.state.totalDays || new Date(targetYear, targetMonth, 0).getDate();

    // Reset all shift codes in the active roster to empty string
    this.state.gridRoster.forEach(eng => {
      eng.shifts = new Array(totalDays).fill('');
    });

    // Clear locked cells for this period
    this.state.lockedCells = {};
    this.saveLockedCells();

    // Persist to localStorage & mark status
    const monthKey = `${targetYear}-${String(targetMonth).padStart(2, '0')}`;
    this.markMonthAsImportedOrGenerated(monthKey);
    this.saveCurrentGridRoster();

    // Re-render UI
    this.renderHeaders();
    this.renderRoster();
    this.runRuleAudit();
    this.updateActiveRulesBanner();
    this.atlasService.notifyStatusChange('unsaved');

    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const curMonthName = monthNames[targetMonth - 1] || `Month ${targetMonth}`;
    this.showToast(`Cleared entire grid for ${curMonthName} ${targetYear}!`);

    return {
      year: targetYear,
      month: targetMonth,
      monthName: curMonthName,
      totalDays,
      engineerCount: this.state.gridRoster.length
    };
  }

  // --- Export Modal & Options (Month Grid & AppSheet Master Data) ---
  openExportModal() {
    if (!this.dom.exportModal) {
      this.dom.exportModal = document.getElementById('exportModal');
    }
    if (!this.dom.exportModal) {
      console.warn('[RosterX] exportModal not found in DOM, exporting Month Grid directly');
      return this.exportToMonthGrid();
    }
    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const curMonthName = monthNames[this.state.currentMonth - 1] || 'Target Month';
    if (this.dom.exportModalPeriodLabel) {
      this.dom.exportModalPeriodLabel.textContent = `Period: ${curMonthName} ${this.state.currentYear} (${this.state.totalDays} Days)`;
    }
    this.dom.exportModal.classList.remove('hidden');
    this.dom.exportModal.style.display = 'flex';
  }

  closeExportModal() {
    if (this.dom.exportModal) {
      this.dom.exportModal.classList.add('hidden');
      this.dom.exportModal.style.display = 'none';
    }
  }

  async exportToMonthGrid() {
    try {
      this.showToast('Generating styled Month Grid spreadsheet...');
      const s = window.ExcelService || ExcelService;
      await s.exportRosterToExcel(
        this.state.gridRoster,
        this.state.currentYear,
        this.state.currentMonth,
        this.state.totalDays,
        this.state.constraints
      );
      this.closeExportModal();
      this.showToast('Month Grid exported (.xlsx)');
    } catch (err) {
      alert('Error exporting Month Grid: ' + err.message);
    }
  }

  async exportToAppSheet() {
    try {
      this.showToast('Generating AppSheet Master Data spreadsheet...');
      const s = window.ExcelService || ExcelService;
      await s.exportRosterToAppSheet(
        this.state.gridRoster,
        this.state.currentYear,
        this.state.currentMonth,
        this.state.totalDays,
        this.state.manualSwaps,
        this.state.engineersList
      );
      this.closeExportModal();
      this.showToast('AppSheet Master Data exported (.xlsx)');
    } catch (err) {
      alert('Error exporting AppSheet data: ' + err.message);
    }
  }

  async exportToExcel() {
    return this.exportToMonthGrid();
  }

  // --- Excel Import Modal & File Processing ---
  getImportSelectedPeriod() {
    if (this.dom.importTargetMonthInput && this.dom.importTargetMonthInput.value) {
      return this.dom.importTargetMonthInput.value;
    }
    if (this.dom.importTargetMonthSelect && this.dom.importTargetMonthSelect.value) {
      return this.dom.importTargetMonthSelect.value;
    }
    return `${this.state.currentYear}-${String(this.state.currentMonth).padStart(2, '0')}`;
  }

  openImportModal() {
    if (!this.dom.importModal) {
      this.dom.importModal = document.getElementById('importModal');
    }
    if (!this.dom.importModal) {
      console.error('[RosterX] importModal not found in DOM');
      return;
    }

    const curVal = `${this.state.currentYear}-${String(this.state.currentMonth).padStart(2, '0')}`;
    if (this.dom.importTargetMonthInput) {
      this.dom.importTargetMonthInput.value = curVal;
    }
    if (this.dom.importTargetMonthSelect) {
      this.dom.importTargetMonthSelect.value = curVal;
    }

    this.updateImportModalPeriodLabel();
    this.renderImportEngineerTags();
    this.clearImportFile();

    this.dom.importModal.classList.remove('hidden');
    this.dom.importModal.style.display = 'flex';
  }

  updateImportModalPeriodLabel() {
    const val = this.getImportSelectedPeriod();
    const [yStr, mStr] = val.split('-');
    const year = parseInt(yStr, 10);
    const month = parseInt(mStr, 10);
    const totalDays = new Date(year, month, 0).getDate();
    const monthNames = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
    const curMonthName = monthNames[month - 1] || 'OCTOBER';

    if (this.dom.importModalPeriodLabel) {
      this.dom.importModalPeriodLabel.textContent = `${curMonthName} ${year} (${totalDays} Days)`;
    }
  }

  renderImportEngineerTags() {
    if (!this.dom.importEngineerTagsContainer) return;
    const activeEngineers = this.sortEngineersByWeekOffs(
      this.state.engineersList.filter(e => e.status !== 'Resigned')
    );

    if (this.dom.importActiveEngCount) {
      this.dom.importActiveEngCount.textContent = activeEngineers.length;
    }

    this.dom.importEngineerTagsContainer.innerHTML = activeEngineers.map(e => `
      <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-bold bg-white text-slate-800 border border-slate-200 shadow-2xs group hover:border-slate-300 transition">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 inline-block"></span>
        <span>${e.name}</span>
        <button type="button" title="Exclude from Import (Mark Resigned)" onclick="window.rosterApp.toggleEngineerFromImport('${e.id}')" class="text-slate-400 hover:text-red-600 transition ml-1 p-0.5 rounded cursor-pointer">
          <i class="ph ph-x-circle text-xs"></i>
        </button>
      </span>
    `).join('');
  }

  toggleEngineerFromImport(engId) {
    const eng = this.state.engineersList.find(e => e.id === engId);
    if (!eng) return;
    eng.status = 'Resigned';
    eng.resignation_date = new Date().toISOString().split('T')[0];

    const isComingMonth = (this.state.currentYear > 2026 || (this.state.currentYear === 2026 && this.state.currentMonth > 10));
    if (isComingMonth) {
      this.state.gridRoster = this.state.gridRoster.filter(e => e.name !== eng.name && e.id !== eng.id);
      this.saveCurrentGridRoster();
      this.renderHeaders();
      this.renderRoster();
      this.runRuleAudit();
    }

    this.saveEngineersList();
    this.updateTeamBadge();
    this.renderTeamTable();
    this.renderImportEngineerTags();
    this.showToast(`${eng.name} marked as Resigned (excluded from template and coming months).`);
  }

  // --- Autocomplete & Configured Engineer Suggestion Engine for Import Modal ---
  renderImportEngineerSuggestions(query = '') {
    if (!this.dom.importEngineerSuggestions) return;
    const term = (query || '').trim().toUpperCase();
    const allConfigured = this.state.engineersList || [];

    let filtered = allConfigured;
    if (term) {
      filtered = allConfigured.filter(e => {
        const name = (e.name || '').toUpperCase();
        const fullName = (e.full_name || '').toUpperCase();
        const username = (e.username || '').toUpperCase();
        return name.includes(term) || fullName.includes(term) || username.includes(term);
      });
    } else {
      // If empty query, prioritize engineers who are currently excluded (Resigned) so they can be easily re-added!
      filtered = [...allConfigured].sort((a, b) => {
        if (a.status === 'Resigned' && b.status !== 'Resigned') return -1;
        if (a.status !== 'Resigned' && b.status === 'Resigned') return 1;
        return a.name.localeCompare(b.name);
      });
    }

    if (filtered.length === 0) {
      this.dom.importEngineerSuggestions.innerHTML = `
        <div class="px-3.5 py-3 text-slate-500 text-xs flex items-center gap-2 bg-amber-50/50">
          <i class="ph ph-warning-circle text-amber-600 text-base shrink-0"></i>
          <div>
            <p class="font-bold text-slate-800">No configured engineer matches "${query.trim()}"</p>
            <p class="text-[11px] text-slate-500">Only configured team engineers can be added to the import roster.</p>
          </div>
        </div>
      `;
      this.dom.importEngineerSuggestions.classList.remove('hidden');
      return;
    }

    const rowsHtml = filtered.map(e => {
      const isAlreadyActive = e.status !== 'Resigned';
      const roleText = (e.role || 'engineer').toUpperCase();
      const offsText = (e.default_weekly_offs || []).join(', ') || 'Sat, Sun';

      return `
        <div data-eng-id="${e.id}" class="suggestion-row flex items-center justify-between px-3.5 py-2 hover:bg-slate-50 transition border-b border-slate-100 last:border-b-0 cursor-pointer ${isAlreadyActive ? 'bg-white' : 'bg-emerald-50/30 hover:bg-emerald-50/70'}">
          <div class="flex items-center gap-2.5">
            <div class="w-6 h-6 rounded-full ${isAlreadyActive ? 'bg-slate-200 text-slate-700' : 'bg-emerald-600 text-white'} flex items-center justify-center font-bold text-[10px] shrink-0">
              ${e.name.charAt(0)}
            </div>
            <div>
              <div class="flex items-center gap-1.5">
                <span class="font-bold text-slate-900 leading-tight">${e.name}</span>
                ${e.username ? `<span class="text-[10px] text-slate-400 font-mono">@${e.username}</span>` : ''}
              </div>
              <p class="text-[10px] text-slate-500">${roleText} • Offs: ${offsText}</p>
            </div>
          </div>
          <div>
            ${isAlreadyActive 
              ? `<span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-600 border border-slate-200"><i class="ph ph-check"></i> Already in List</span>`
              : `<button type="button" class="btn-add-suggested px-2.5 py-1 rounded-md bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-[10px] flex items-center gap-1 shadow-2xs transition cursor-pointer"><i class="ph ph-plus-circle"></i> Add to List</button>`
            }
          </div>
        </div>
      `;
    }).join('');

    this.dom.importEngineerSuggestions.innerHTML = `
      <div class="px-3 py-1 bg-slate-50 border-b border-slate-100 text-[10px] font-bold text-slate-500 uppercase tracking-wider flex justify-between items-center">
        <span>Matching Configured Engineers (${filtered.length})</span>
        <span class="text-[9px] text-slate-400 lowercase">click name to add</span>
      </div>
      <div class="divide-y divide-slate-100">
        ${rowsHtml}
      </div>
    `;

    this.dom.importEngineerSuggestions.querySelectorAll('.suggestion-row').forEach(row => {
      row.addEventListener('click', () => {
        const engId = row.dataset.engId;
        this.addConfiguredEngineerToImport(engId);
      });
    });

    this.dom.importEngineerSuggestions.classList.remove('hidden');
  }

  closeImportEngineerSuggestions() {
    if (this.dom.importEngineerSuggestions) {
      this.dom.importEngineerSuggestions.classList.add('hidden');
    }
  }

  addConfiguredEngineerToImport(engId) {
    if (!engId) return;
    const eng = this.state.engineersList.find(e => e.id === engId || e.name.toUpperCase() === engId.toUpperCase());
    if (!eng) {
      this.showToast('⚠️ Selected engineer is not in the configured team list.');
      return;
    }

    if (eng.status === 'Resigned') {
      eng.status = 'Active';
      eng.resignation_date = null;

      const isComingMonth = (this.state.currentYear > 2026 || (this.state.currentYear === 2026 && this.state.currentMonth > 10));
      if (isComingMonth && !this.state.gridRoster.some(r => r.name === eng.name || r.id === eng.id)) {
        this.state.gridRoster.push({
          id: eng.id,
          name: eng.name,
          shifts: new Array(this.state.totalDays).fill('EMPTY'),
          status: 'Active',
          role: eng.role || 'engineer',
          default_weekly_offs: eng.default_weekly_offs || ['Saturday', 'Sunday']
        });
        this.state.gridRoster = this.sortGridRosterByWeekOffs(this.state.gridRoster);
        this.saveCurrentGridRoster();
        this.renderHeaders();
        this.renderRoster();
        this.runRuleAudit();
      }

      this.saveEngineersList();
      this.updateTeamBadge();
      this.renderTeamTable();
      this.renderImportEngineerTags();
      this.showToast(`✅ Added ${eng.name} to active import list!`);
    } else {
      this.showToast(`ℹ️ ${eng.name} is already in the active import list.`);
    }

    if (this.dom.importEngineerInput) {
      this.dom.importEngineerInput.value = '';
    }
    if (this.dom.btnClearImportEngineerInput) {
      this.dom.btnClearImportEngineerInput.classList.add('hidden');
    }
    this.closeImportEngineerSuggestions();
  }

  handleAddImportEngineer() {
    const input = this.dom.importEngineerInput;
    if (!input) return;
    const rawVal = input.value.trim();
    if (!rawVal) return;
    const clean = rawVal.toUpperCase();

    // 1. Exact match on name, full_name, username, or id
    let eng = this.state.engineersList.find(e => 
      e.name.trim().toUpperCase() === clean || 
      (e.full_name && e.full_name.trim().toUpperCase() === clean) ||
      (e.username && e.username.trim().toUpperCase() === clean) ||
      e.id === rawVal
    );

    // 2. Partial match if unambiguous
    if (!eng) {
      const partials = this.state.engineersList.filter(e => 
        e.name.toUpperCase().includes(clean) || 
        (e.full_name && e.full_name.toUpperCase().includes(clean))
      );
      if (partials.length === 1) {
        eng = partials[0];
      }
    }

    // 3. Strict guard: If NOT in configured engineers, REJECT!
    if (!eng) {
      this.showToast(`⚠️ "${rawVal}" is not in the configured engineer list. Only configured engineers can be added.`);
      this.renderImportEngineerSuggestions(rawVal);
      return;
    }

    // 4. Add the configured engineer
    this.addConfiguredEngineerToImport(eng.id);
  }


  closeImportModal() {
    if (!this.dom.importModal) {
      this.dom.importModal = document.getElementById('importModal');
    }
    if (!this.dom.importModal) return;
    this.dom.importModal.classList.add('hidden');
    this.dom.importModal.style.display = 'none';
    this.clearImportFile();
    if (this.dom.importEngineerInput) this.dom.importEngineerInput.value = '';
    if (this.dom.btnClearImportEngineerInput) this.dom.btnClearImportEngineerInput.classList.add('hidden');
    this.closeImportEngineerSuggestions();
  }

  async downloadSampleTemplate() {
    try {
      this.showToast('Generating blank sample template with active engineers...');
      const s = window.ExcelService || ExcelService;
      if (!s || typeof s.downloadSampleTemplate !== 'function') {
        throw new Error('ExcelService is still initializing. Please try again.');
      }

      const val = this.getImportSelectedPeriod();
      const [yStr, mStr] = val.split('-');
      const targetYear = parseInt(yStr, 10);
      const targetMonth = parseInt(mStr, 10);

      // Exclude Resigned engineers
      const activeOnly = this.state.engineersList.filter(e => e.status !== 'Resigned');

      await s.downloadSampleTemplate(
        targetYear,
        targetMonth,
        activeOnly,
        this.state.constraints
      );

      const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
      this.showToast(`Blank template downloaded for ${monthNames[targetMonth - 1]}-${targetYear} (${activeOnly.length} active engineers)`);
    } catch (err) {
      alert('Error generating template: ' + err.message);
    }
  }

  async handleExcelFileSelected(file) {
    if (!file) return;

    const fileName = file.name;
    const ext = fileName.slice((fileName.lastIndexOf(".") - 1 >>> 0) + 2).toLowerCase();
    if (!['xlsx', 'xls'].includes(ext)) {
      alert('Please upload a valid Excel spreadsheet file (.xlsx or .xls).');
      return;
    }

    // Update file info display
    if (this.dom.selectedFileName) this.dom.selectedFileName.textContent = fileName;
    if (this.dom.selectedFileSize) {
      const sizeKb = (file.size / 1024).toFixed(1);
      this.dom.selectedFileSize.textContent = `${sizeKb} KB`;
    }
    if (this.dom.dropZone) this.dom.dropZone.classList.add('hidden');
    if (this.dom.selectedFileInfo) this.dom.selectedFileInfo.classList.remove('hidden');

    try {
      this.showToast('Reading and validating Excel spreadsheet...');

      const val = this.getImportSelectedPeriod();
      const [yStr, mStr] = val.split('-');
      const targetYear = parseInt(yStr, 10);
      const targetMonth = parseInt(mStr, 10);
      const targetTotalDays = new Date(targetYear, targetMonth, 0).getDate();

      const parsed = await ExcelService.parseExcelRoster(file, targetTotalDays, this.state.engineersList);

      // Filter out any Resigned engineers from the imported draft
      const filteredEngineers = parsed.engineers.filter(importedEng => {
        const meta = this.state.engineersList.find(e => e.name.toUpperCase() === importedEng.name.toUpperCase());
        return meta ? meta.status !== 'Resigned' : true;
      });

      this.state.importedExcelDraft = filteredEngineers;
      parsed.engineers = filteredEngineers;

      this.renderImportPreview(parsed);
      if (this.dom.btnApplyImportedRoster) this.dom.btnApplyImportedRoster.disabled = false;
      this.showToast(`Excel parsed: ${filteredEngineers.length} active engineers`);
    } catch (err) {
      alert('Failed to parse Excel file: ' + err.message);
      this.clearImportFile();
    }
  }

  renderImportPreview(parsed) {
    if (!this.dom.importPreviewBox) return;

    this.dom.previewEngCount.textContent = parsed.engineers.length;
    this.dom.previewDaysCount.textContent = parsed.detectedDays;
    this.dom.previewStatus.textContent = 'Verified';

    // Warnings
    if (parsed.warnings && parsed.warnings.length > 0) {
      this.dom.importWarningsList.innerHTML = parsed.warnings.map(w => `<li>${w}</li>`).join('');
      this.dom.importWarningsBox.classList.remove('hidden');
    } else {
      this.dom.importWarningsBox.classList.add('hidden');
    }

    // Mini Table Preview (show up to 6 engineers and first 12 days)
    const maxPreviewDays = Math.min(this.state.totalDays, 12);
    let headHtml = '<tr><th class="p-1.5 text-left border-r border-slate-200">Engineer</th>';
    for (let d = 1; d <= maxPreviewDays; d++) {
      headHtml += `<th class="p-1 border-r border-slate-200">${d}</th>`;
    }
    if (this.state.totalDays > maxPreviewDays) {
      headHtml += '<th class="p-1">...</th>';
    }
    headHtml += '</tr>';
    this.dom.previewTableHead.innerHTML = headHtml;

    let bodyHtml = '';
    const sampleEngs = parsed.engineers.slice(0, 6);
    sampleEngs.forEach(eng => {
      bodyHtml += `<tr><td class="p-1.5 text-left font-bold border-r border-slate-200">${eng.name}</td>`;
      for (let d = 0; d < maxPreviewDays; d++) {
        const code = eng.shifts[d] || 'OFF';
        const isOff = code === 'OFF' || (code && code.startsWith('SW'));
        const isNs = code === 'NS';
        const bg = isNs ? 'bg-slate-200 text-slate-800' : isOff ? 'bg-yellow-50 text-yellow-800' : 'bg-emerald-50 text-emerald-800';
        bodyHtml += `<td class="p-1 border-r border-slate-100 ${bg}">${code}</td>`;
      }
      if (this.state.totalDays > maxPreviewDays) {
        bodyHtml += '<td class="p-1 text-slate-400">...</td>';
      }
      bodyHtml += '</tr>';
    });
    this.dom.previewTableBody.innerHTML = bodyHtml;

    this.dom.importPreviewBox.classList.remove('hidden');
  }

  clearImportFile() {
    this.state.importedExcelDraft = null;
    if (this.dom.excelFileInput) this.dom.excelFileInput.value = '';
    if (this.dom.dropZone) this.dom.dropZone.classList.remove('hidden');
    if (this.dom.selectedFileInfo) this.dom.selectedFileInfo.classList.add('hidden');
    if (this.dom.importPreviewBox) this.dom.importPreviewBox.classList.add('hidden');
    if (this.dom.btnApplyImportedRoster) this.dom.btnApplyImportedRoster.disabled = true;
  }

  applyImportedRoster() {
    if (!this.state.importedExcelDraft || this.state.importedExcelDraft.length === 0) {
      alert('No imported data available to apply.');
      return;
    }

    const val = this.getImportSelectedPeriod();
    const [yStr, mStr] = val.split('-');
    const targetYear = parseInt(yStr, 10);
    const targetMonth = parseInt(mStr, 10);

    // Filter out Resigned engineers and sort by Weekoffs Day Order
    const activeImportedRows = this.sortGridRosterByWeekOffs(
      this.state.importedExcelDraft.filter(imported => {
        const def = this.state.engineersList.find(x => x.name.toUpperCase() === imported.name.toUpperCase()) || {};
        return def.status !== 'Resigned';
      }).map(imported => {
        const def = this.state.engineersList.find(x => x.name.toUpperCase() === imported.name.toUpperCase()) || {};
        return {
          id: imported.id || def.id || `eng_${imported.name.toLowerCase().replace(/[\s\W]+/g, '_')}`,
          name: imported.name,
          shifts: [...imported.shifts],
          status: 'Active',
          role: def.role || 'engineer',
          default_weekly_offs: def.default_weekly_offs || ['Saturday', 'Sunday']
        };
      })
    );

    // Save under the target month cache key
    const monthKey = `${targetYear}-${String(targetMonth).padStart(2, '0')}`;
    this.markMonthAsImportedOrGenerated(monthKey);
    const cacheKey = `${STORAGE_KEYS.GRID_ROSTER_PREFIX}${targetYear}_${String(targetMonth).padStart(2, '0')}`;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(cacheKey, JSON.stringify(activeImportedRows));
    }

    // If currently viewing the target period, refresh view immediately
    if (this.state.currentYear === targetYear && this.state.currentMonth === targetMonth) {
      this.state.gridRoster = activeImportedRows;
      this.renderHeaders();
      this.renderRoster();
      this.runRuleAudit();
    } else {
      // Switch view to imported month
      this.dom.monthSelect.value = val;
      this.handlePeriodChange();
    }

    const monthNames = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
    const curMonthName = monthNames[targetMonth - 1] || 'SELECTED MONTH';
    this.showToast(`Roster for ${curMonthName} ${targetYear} imported from Excel!`);
    this.closeImportModal();
  }

  // --- MongoDB Atlas Direct Save & Publish ---
  async saveAndPublishToAtlas() {
    const val = this.dom.monthSelect.value;
    this.markMonthAsImportedOrGenerated(val);
    const mongoDoc = {
      _id: val,
      month: this.state.currentMonth,
      year: this.state.currentYear,
      status: 'published',
      engineers: this.state.gridRoster.map(e => ({
        engineer_id: e.id || `eng_${e.name.toLowerCase().replace(/[\s\W]+/g, '')}`,
        name: e.name,
        status: e.status || 'Active',
        role: e.role || 'engineer',
        shifts: shiftsArrayToMap(e.shifts)
      })),
      metadata: {
        generated_by: this.state.generatedDraft ? 'gemini_ai_engine' : 'admin_manual_editor',
        updated_at: new Date().toISOString(),
        total_engineers: this.state.gridRoster.length,
        compliance_score: (this.state.auditState || {}).score || 100,
        constraints_snapshot: this.state.constraints
      }
    };

    this.showToast(`Publishing ${val} to MongoDB Atlas...`);
    const res = await this.atlasService.saveRoster(mongoDoc);
    await this.atlasService.saveEngineers(this.state.engineersList);
    if (res.success) {
      this.atlasService.notifyStatusChange('synced');
    }
    this.showToast(res.message || 'Synced to Atlas M0');
  }

  // --- Publish Confirmation Dialog ---
  openPublishConfirmation() {
    if (!this.dom.publishConfirmModal) {
      this.saveAndPublishToAtlas();
      return;
    }
    const val = this.dom.monthSelect ? this.dom.monthSelect.value : `${this.state.currentYear}-${String(this.state.currentMonth).padStart(2, '0')}`;
    const selectedOption = this.dom.monthSelect?.options[this.dom.monthSelect.selectedIndex];
    const monthText = selectedOption ? selectedOption.text : val;
    if (this.dom.publishConfirmPeriod) this.dom.publishConfirmPeriod.textContent = monthText;
    if (this.dom.publishConfirmEngCount) this.dom.publishConfirmEngCount.textContent = `${this.state.gridRoster.length} Engineers`;
    this.dom.publishConfirmModal.classList.remove('hidden');
  }

  // --- Project Recovery Snapshots Modal ---
  async openRecoveryModal() {
    if (!this.dom.recoveryModal) return;
    const val = this.dom.monthSelect ? this.dom.monthSelect.value : `${this.state.currentYear}-${String(this.state.currentMonth).padStart(2, '0')}`;
    const selectedOption = this.dom.monthSelect?.options[this.dom.monthSelect.selectedIndex];
    const monthText = selectedOption ? selectedOption.text : val;
    if (this.dom.recoveryModalPeriodLabel) {
      this.dom.recoveryModalPeriodLabel.textContent = `Auto-saved recovery points for ${monthText}`;
    }

    this.dom.recoveryModal.classList.remove('hidden');
    if (this.dom.recoverySnapshotsLoading) this.dom.recoverySnapshotsLoading.classList.remove('hidden');
    if (this.dom.recoverySnapshotsEmpty) this.dom.recoverySnapshotsEmpty.classList.add('hidden');
    if (this.dom.recoverySnapshotsList) this.dom.recoverySnapshotsList.innerHTML = '';

    const backups = await this.atlasService.getRosterBackups(val);
    if (this.dom.recoverySnapshotsLoading) this.dom.recoverySnapshotsLoading.classList.add('hidden');

    if (!backups || backups.length === 0) {
      if (this.dom.recoverySnapshotsEmpty) this.dom.recoverySnapshotsEmpty.classList.remove('hidden');
      return;
    }

    if (this.dom.recoverySnapshotsList) {
      this.dom.recoverySnapshotsList.innerHTML = backups.map(b => {
        const dateStr = b.created_at ? new Date(b.created_at).toLocaleString() : 'Recent Snapshot';
        const engCount = b.total_engineers || (b.document?.engineers?.length || 0);
        const reason = b.reason || 'Auto-saved before overwrite';
        return `
          <div class="flex items-center justify-between p-3.5 bg-slate-50 hover:bg-slate-100/80 rounded-xl border border-slate-200 transition">
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center font-bold text-sm">
                <i class="ph ph-clock-counter-clockwise"></i>
              </div>
              <div>
                <p class="text-xs font-bold text-slate-800">${dateStr}</p>
                <p class="text-[11px] text-slate-500">${reason} • <span class="font-semibold text-slate-700">${engCount} Engineers</span></p>
              </div>
            </div>
            <button type="button" data-backup-id="${b.backup_id}" class="btn-restore-snapshot px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center gap-1 shadow-sm transition cursor-pointer">
              <i class="ph ph-arrow-counter-clockwise"></i> Restore
            </button>
          </div>
        `;
      }).join('');

      this.dom.recoverySnapshotsList.querySelectorAll('.btn-restore-snapshot').forEach(btn => {
        btn.addEventListener('click', async () => {
          const backupId = btn.dataset.backupId;
          if (confirm(`Restore snapshot from ${backupId}? This will replace current grid changes.`)) {
            const res = await this.atlasService.restoreRosterBackup(backupId, val);
            if (res.success) {
              await this.loadGridRosterForPeriod(this.state.currentYear, this.state.currentMonth);
              this.renderHeaders();
              this.renderRoster();
              this.renderDailyCoverage();
              this.runRuleAudit();
              this.dom.recoveryModal.classList.add('hidden');
              this.showToast('✅ Snapshot restored successfully!');
            } else {
              alert(res.message || 'Failed to restore snapshot.');
            }
          }
        });
      });
    }
  }

  // --- Operational Policies Atlas Sync ---
  async syncConstraintsFromAtlas() {
    try {
      const base = this.atlasService.getApiBase();
      const res = await fetch(`${base}/constraints`);
      if (res.ok) {
        const data = await res.json();
        if (data.success && data.constraints) {
          this.state.constraints = { ...this.state.constraints, ...data.constraints };
          this.saveConstraints();
          this.updateActiveRulesBanner();
        }
      }
    } catch (e) {
      console.warn('Failed to sync constraints from Atlas:', e);
    }
  }

  // --- Settings Modals ---
  openGeminiSettings() {
    this.dom.geminiApiKeyInput.value = this.aiEngine.apiKey || '';
    this.dom.geminiModelSelect.value = this.aiEngine.model || 'gemini-1.5-flash';
    this.dom.geminiSettingsModal.classList.remove('hidden');
  }

  saveGeminiSettings() {
    this.aiEngine.saveApiKey(this.dom.geminiApiKeyInput.value);
    this.aiEngine.saveModel(this.dom.geminiModelSelect.value);
    this.dom.geminiSettingsModal.classList.add('hidden');
    if (this.aiAssistant && typeof this.aiAssistant.updateModelDisplay === 'function') {
      this.aiAssistant.updateModelDisplay();
    }
    this.showToast('Gemini API settings saved.');
  }

  openAtlasConfig() {
    if (!this.dom.atlasConfigModal) return;
    const cfg = this.atlasService.config;
    const defaultApiUrl = `${window.location.origin}/api`;
    if (this.dom.atlasUriInput) {
      this.dom.atlasUriInput.value = cfg.mongoUri || '';
    }
    this.dom.atlasEndpointInput.value = cfg.endpointUrl || defaultApiUrl;
    this.dom.atlasApiKeyInput.value = cfg.apiKey || '';
    this.dom.atlasClusterInput.value = cfg.clusterName || 'Cluster0';
    this.dom.atlasDatabaseInput.value = cfg.database || 'rosterx';
    this.dom.atlasTestFeedback.classList.add('hidden');
    this.dom.atlasConfigModal.classList.remove('hidden');
  }

  saveAtlasConfig() {
    this.atlasService.saveConfig({
      mongoUri: this.dom.atlasUriInput ? this.dom.atlasUriInput.value.trim() : '',
      endpointUrl: this.dom.atlasEndpointInput.value.trim() || `${window.location.origin}/api`,
      apiKey: this.dom.atlasApiKeyInput.value.trim(),
      clusterName: this.dom.atlasClusterInput.value.trim(),
      database: this.dom.atlasDatabaseInput.value.trim()
    });
    this.dom.atlasConfigModal.classList.add('hidden');
    this.showToast('Atlas configuration saved.');
  }

  async testAtlasConnection() {
    const fb = this.dom.atlasTestFeedback;
    fb.classList.remove('hidden');
    fb.className = 'p-2 rounded text-xs bg-blue-50 text-blue-800';
    fb.textContent = 'Testing connection to MongoDB Atlas...';

    this.atlasService.config.endpointUrl = this.dom.atlasEndpointInput.value.trim() || `${window.location.origin}/api`;
    this.atlasService.config.apiKey = this.dom.atlasApiKeyInput.value.trim();
    this.atlasService.config.clusterName = this.dom.atlasClusterInput.value.trim();
    this.atlasService.config.database = this.dom.atlasDatabaseInput.value.trim();

    const res = await this.atlasService.testConnection();
    if (res.success) {
      fb.className = 'p-2 rounded text-xs bg-emerald-50 text-emerald-800 font-semibold';
      fb.textContent = '✅ ' + (res.message || 'Atlas Cluster0 Connected & Verified!');
    } else {
      fb.className = 'p-2 rounded text-xs bg-amber-50 text-amber-800';
      fb.textContent = '⚠️ ' + res.message;
    }
  }
}

// Global bootstrap
window.openImportModal = () => {
  if (window.rosterApp && window.rosterApp.openImportModal) {
    window.rosterApp.openImportModal();
  }
};

window.addEventListener('DOMContentLoaded', () => {
  window.rosterApp = new RosterXApp();
  window.rosterApp.init();
});

// Event Delegation Fallback for Import Data Modal
document.addEventListener('click', (e) => {
  const btn = e.target.closest('#btnOpenImportModal');
  if (btn) {
    e.preventDefault();
    if (window.rosterApp && window.rosterApp.openImportModal) {
      window.rosterApp.openImportModal();
    }
  }
});
