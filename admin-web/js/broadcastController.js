/**
 * RosterX — Official Admin Broadcast Controller (broadcastController.js)
 * 
 * Standard Multi-Channel Operations Broadcaster:
 * 1. Target Audience Selector (Everyone vs Specific chips with search & toggle)
 * 2. Operational Templates Engine (MongoDB Atlas persistence)
 * 3. Live '@' Mention Autocomplete Popup with keyboard navigation
 * 4. Dynamic Name Translation Engine (@username -> @Full Name badges)
 * 5. Real-Time Live Preview Simulator
 * 6. Multi-Channel Dispatch: In-App, Android Notification Panel (FCM), Resend Email
 * 7. Broadcast History & Live Read Receipts Breakdown (Read vs Pending)
 * 8. Audit-Preserving Soft Retraction
 */

export class BroadcastController {
  constructor() {
    this.atlasService = null;
    this.getEngineers = () => [];
    this.showToast = (msg, type) => console.log(`[Toast ${type}]: ${msg}`);
    
    // Internal state
    this.audienceMode = 'everyone'; // 'everyone' | 'specific'
    this.selectedEngineerIds = new Set();
    this.templates = [];
    this.history = [];
    this.previewEngineerId = null;
    
    // Live '@' Mention popup state
    this.mentionSuggestions = [];
    this.selectedMentionIndex = 0;
    this.mentionQueryStart = -1;
    this.mentionQueryEnd = -1;
    this.isMentionDropdownOpen = false;
  }

  getEngineerKey(eng) {
    if (!eng) return '';
    return String(eng.id || eng.username || ('eng_' + (eng.name || eng.fullName || '').toLowerCase().replace(/[^a-z0-9]/g, ''))).trim();
  }

  init({ atlasService, getEngineers, showToast }) {
    this.atlasService = atlasService;
    this.getEngineers = getEngineers;
    this.showToast = showToast || this.showToast;

    this.cacheDom();
    this.bindEvents();
    this.renderRecipientChips();
    this.loadTemplates();
    this.populatePreviewEngineers();
    this.updatePreview();
    this.loadHistory();
  }

  cacheDom() {
    this.dom = {
      // Audience
      audienceSummaryBadge: document.getElementById('broadcastAudienceSummaryBadge'),
      audienceEveryoneBtn: document.getElementById('broadcastAudienceEveryoneBtn'),
      audienceSpecificBtn: document.getElementById('broadcastAudienceSpecificBtn'),
      specificContainer: document.getElementById('broadcastSpecificEngineersContainer'),
      engineerSearchInput: document.getElementById('broadcastEngineerSearch'),
      selectAllBtn: document.getElementById('btnSelectAllRecipients'),
      clearAllBtn: document.getElementById('btnClearAllRecipients'),
      recipientChipsContainer: document.getElementById('broadcastRecipientChips'),
      
      // Templates & Priority
      templateSelect: document.getElementById('broadcastTemplateSelect'),
      openSaveTemplateModalBtn: document.getElementById('btnOpenSaveTemplateModal'),
      deleteTemplateBtn: document.getElementById('btnDeleteSelectedTemplate'),
      priorityRadios: document.querySelectorAll('input[name="broadcastPriority"]'),
      
      // Message Form
      titleInput: document.getElementById('broadcastTitleInput'),
      messageInput: document.getElementById('broadcastMessageInput'),
      mentionDropdown: document.getElementById('broadcastMentionDropdown'),
      charCountEl: document.getElementById('broadcastCharCount'),
      
      // Quick Tokens
      btnInsertTokenName: document.getElementById('btnInsertTokenName'),
      btnInsertTokenUsername: document.getElementById('btnInsertTokenUsername'),
      btnInsertMentionAll: document.getElementById('btnInsertMentionAll'),
      
      // Channels
      channelInApp: document.getElementById('broadcastChannelInApp'),
      channelPush: document.getElementById('broadcastChannelPush'),
      channelEmail: document.getElementById('broadcastChannelEmail'),
      
      // Dispatch
      dispatchBtn: document.getElementById('btnDispatchBroadcast'),
      dispatchBtnText: document.getElementById('dispatchBtnText'),
      dispatchBtnIcon: document.getElementById('dispatchBtnIcon'),
      
      // Live Preview Simulator
      previewSelect: document.getElementById('broadcastPreviewEngineerSelect'),
      previewPriorityPill: document.getElementById('previewPriorityPill'),
      previewTitle: document.getElementById('previewTitle'),
      previewBody: document.getElementById('previewMessageBody'),
      previewSender: document.getElementById('previewSender'),
      previewChannelsBadges: document.getElementById('previewActiveChannelsBadges'),
      
      // History
      historyList: document.getElementById('broadcastHistoryList'),
      historyEmpty: document.getElementById('broadcastHistoryEmpty'),
      historyCountBadge: document.getElementById('broadcastHistoryCountBadge'),
      btnRefreshHistory: document.getElementById('btnRefreshBroadcasts'),
      
      // Modals
      modalSaveTemplate: document.getElementById('modalSaveBroadcastTemplate'),
      saveTemplateNameInput: document.getElementById('saveTemplateNameInput'),
      saveTemplateTitleInput: document.getElementById('saveTemplateTitleInput'),
      saveTemplateBodyPreview: document.getElementById('saveTemplateBodyPreview'),
      btnConfirmSaveTemplate: document.getElementById('btnConfirmSaveTemplate'),
      btnCancelSaveTemplate: document.getElementById('btnCancelSaveTemplate'),
      btnCloseSaveTemplateModal: document.getElementById('btnCloseSaveTemplateModal'),
      
      modalDetails: document.getElementById('modalBroadcastDetails'),
      detailTitle: document.getElementById('detailModalTitle'),
      detailMeta: document.getElementById('detailModalMeta'),
      detailBody: document.getElementById('detailModalBody'),
      detailTargetCount: document.getElementById('detailModalTargetCount'),
      detailReadCount: document.getElementById('detailModalReadCount'),
      detailPendingCount: document.getElementById('detailModalPendingCount'),
      detailReadList: document.getElementById('detailModalReadList'),
      detailReadListCount: document.getElementById('detailModalReadListCount'),
      detailPendingList: document.getElementById('detailModalPendingList'),
      detailPendingListCount: document.getElementById('detailModalPendingListCount'),
      btnCloseDetailsModal: document.getElementById('btnCloseBroadcastDetailsModal'),
      btnCloseDetailsModalFooter: document.getElementById('btnCloseBroadcastDetailsModalFooter'),
    };
  }

  bindEvents() {
    // 1. Audience Toggle
    this.dom.audienceEveryoneBtn?.addEventListener('click', () => this.setAudienceMode('everyone'));
    this.dom.audienceSpecificBtn?.addEventListener('click', () => this.setAudienceMode('specific'));
    this.dom.selectAllBtn?.addEventListener('click', () => this.selectAllEngineers());
    this.dom.clearAllBtn?.addEventListener('click', () => this.clearAllEngineers());
    this.dom.engineerSearchInput?.addEventListener('input', (e) => this.filterRecipientChips(e.target.value));

    // 2. Templates
    this.dom.templateSelect?.addEventListener('change', () => this.handleTemplateSelect());
    this.dom.openSaveTemplateModalBtn?.addEventListener('click', () => this.openSaveTemplateModal());
    this.dom.deleteTemplateBtn?.addEventListener('click', () => this.deleteCurrentTemplate());

    // 3. Priority Radios
    this.dom.priorityRadios?.forEach(radio => {
      radio.addEventListener('change', () => this.updatePreview());
    });

    // 4. Title & Message Typing
    this.dom.titleInput?.addEventListener('input', () => this.updatePreview());
    this.dom.messageInput?.addEventListener('input', (e) => this.handleMessageInput(e));
    this.dom.messageInput?.addEventListener('keydown', (e) => this.handleMessageKeydown(e));
    this.dom.messageInput?.addEventListener('blur', () => {
      // Delay closing to allow clicking suggestions
      setTimeout(() => this.closeMentionDropdown(), 200);
    });

    // 5. Quick Token Buttons
    this.dom.btnInsertTokenName?.addEventListener('click', () => this.insertToken('{name}'));
    this.dom.btnInsertTokenUsername?.addEventListener('click', () => this.insertToken('{username}'));
    this.dom.btnInsertMentionAll?.addEventListener('click', () => this.insertToken('@all '));

    // 6. Channels
    this.dom.channelInApp?.addEventListener('change', () => this.updatePreviewChannels());
    this.dom.channelPush?.addEventListener('change', () => this.updatePreviewChannels());
    this.dom.channelEmail?.addEventListener('change', () => this.updatePreviewChannels());

    // 7. Preview Engineer Select
    this.dom.previewSelect?.addEventListener('change', (e) => {
      this.previewEngineerId = e.target.value;
      this.updatePreview();
    });

    // 8. Dispatch Button
    this.dom.dispatchBtn?.addEventListener('click', () => this.dispatchBroadcast());

    // 9. History Refresh
    this.dom.btnRefreshHistory?.addEventListener('click', () => this.loadHistory(true));

    // 10. Modals
    this.dom.btnCancelSaveTemplate?.addEventListener('click', () => this.closeSaveTemplateModal());
    this.dom.btnCloseSaveTemplateModal?.addEventListener('click', () => this.closeSaveTemplateModal());
    this.dom.btnConfirmSaveTemplate?.addEventListener('click', () => this.saveCustomTemplate());

    this.dom.btnCloseDetailsModal?.addEventListener('click', () => this.closeDetailsModal());
    this.dom.btnCloseDetailsModalFooter?.addEventListener('click', () => this.closeDetailsModal());
  }

  getActiveEngineers() {
    const all = this.getEngineers() || [];
    return all.filter(e => e.status !== 'Resigned' && !e.isManagementAccount);
  }

  // =========================================================================
  // 1. Audience Selector Methods
  // =========================================================================

  setAudienceMode(mode) {
    this.audienceMode = mode;
    const isEveryone = mode === 'everyone';

    if (this.dom.audienceEveryoneBtn && this.dom.audienceSpecificBtn) {
      if (isEveryone) {
        this.dom.audienceEveryoneBtn.className = 'py-2 px-3 rounded-lg flex items-center justify-center gap-1.5 transition cursor-pointer bg-white dark:bg-slate-800 text-purple-700 dark:text-purple-300 shadow-xs';
        this.dom.audienceSpecificBtn.className = 'py-2 px-3 rounded-lg flex items-center justify-center gap-1.5 transition cursor-pointer text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white';
        this.dom.specificContainer?.classList.add('hidden');
      } else {
        this.dom.audienceEveryoneBtn.className = 'py-2 px-3 rounded-lg flex items-center justify-center gap-1.5 transition cursor-pointer text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white';
        this.dom.audienceSpecificBtn.className = 'py-2 px-3 rounded-lg flex items-center justify-center gap-1.5 transition cursor-pointer bg-white dark:bg-slate-800 text-purple-700 dark:text-purple-300 shadow-xs';
        this.dom.specificContainer?.classList.remove('hidden');
      }
    }

    this.updateAudienceSummary();
  }

  renderRecipientChips() {
    if (!this.dom.recipientChipsContainer) return;
    const engineers = this.getActiveEngineers();
    this.dom.recipientChipsContainer.innerHTML = '';

    engineers.forEach(eng => {
      const engKey = this.getEngineerKey(eng);
      const isSelected = this.selectedEngineerIds.has(engKey);
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.className = `recipient-chip inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition cursor-pointer select-none border ${
        isSelected
          ? 'bg-purple-600 text-white border-purple-700 dark:border-purple-500 shadow-2xs'
          : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-700'
      }`;
      chip.dataset.engId = engKey;
      chip.dataset.engName = (eng.fullName || eng.name || '').toLowerCase();
      chip.dataset.engUsername = (eng.username || '').toLowerCase();

      const initial = (eng.fullName || eng.name || 'E').charAt(0).toUpperCase();
      chip.innerHTML = `
        <span class="w-4 h-4 rounded-full flex items-center justify-center text-[10px] font-bold ${
          isSelected ? 'bg-purple-700 text-white' : 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-200'
        }">${initial}</span>
        <span>${eng.fullName || eng.name}</span>
      `;

      chip.addEventListener('click', () => {
        this.toggleEngineerSelection(engKey);
      });

      this.dom.recipientChipsContainer.appendChild(chip);
    });

    this.updateAudienceSummary();
  }

  toggleEngineerSelection(engKey) {
    if (!engKey) return;
    if (this.selectedEngineerIds.has(engKey)) {
      this.selectedEngineerIds.delete(engKey);
    } else {
      this.selectedEngineerIds.add(engKey);
    }
    this.renderRecipientChips();
  }

  selectAllEngineers() {
    const engineers = this.getActiveEngineers();
    this.selectedEngineerIds = new Set(engineers.map(e => this.getEngineerKey(e)).filter(Boolean));
    this.renderRecipientChips();
  }

  clearAllEngineers() {
    this.selectedEngineerIds.clear();
    this.renderRecipientChips();
  }

  filterRecipientChips(query) {
    const q = (query || '').trim().toLowerCase();
    const chips = this.dom.recipientChipsContainer?.querySelectorAll('.recipient-chip');
    chips?.forEach(chip => {
      const name = chip.dataset.engName || '';
      const uname = chip.dataset.engUsername || '';
      if (!q || name.includes(q) || uname.includes(q)) {
        chip.classList.remove('hidden');
      } else {
        chip.classList.add('hidden');
      }
    });
  }

  updateAudienceSummary() {
    const totalActive = this.getActiveEngineers().length;
    const selectedCount = this.audienceMode === 'everyone' ? totalActive : this.selectedEngineerIds.size;

    if (this.dom.audienceSummaryBadge) {
      this.dom.audienceSummaryBadge.textContent = this.audienceMode === 'everyone'
        ? `${totalActive} Active Engineers`
        : `${selectedCount} Selected`;
    }

    if (this.dom.dispatchBtnText) {
      this.dom.dispatchBtnText.textContent = `Broadcast to ${selectedCount} Engineer${selectedCount !== 1 ? 's' : ''}`;
    }
  }

  // =========================================================================
  // 2. Live '@' Mention Autocomplete Engine
  // =========================================================================

  handleMessageInput(e) {
    const textarea = this.dom.messageInput;
    if (!textarea) return;

    // Character counter
    if (this.dom.charCountEl) {
      this.dom.charCountEl.textContent = `${textarea.value.length} characters`;
    }

    // Detect '@' at current cursor position
    const textBeforeCursor = textarea.value.slice(0, textarea.selectionStart);
    const mentionMatch = textBeforeCursor.match(/@([a-zA-Z0-9_\-\.]*)$/);

    if (mentionMatch) {
      const query = mentionMatch[1].toLowerCase();
      this.mentionQueryStart = textarea.selectionStart - mentionMatch[0].length;
      this.mentionQueryEnd = textarea.selectionStart;
      this.showMentionDropdown(query);
    } else {
      this.closeMentionDropdown();
    }

    this.updatePreview();
  }

  handleMessageKeydown(e) {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      this.dispatchBroadcast();
      return;
    }

    if (!this.isMentionDropdownOpen) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.selectedMentionIndex = (this.selectedMentionIndex + 1) % this.mentionSuggestions.length;
      this.highlightMentionItem();
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.selectedMentionIndex = (this.selectedMentionIndex - 1 + this.mentionSuggestions.length) % this.mentionSuggestions.length;
      this.highlightMentionItem();
    } else if (e.key === 'Enter' || e.key === 'Tab') {
      e.preventDefault();
      if (this.mentionSuggestions[this.selectedMentionIndex]) {
        this.selectMention(this.mentionSuggestions[this.selectedMentionIndex]);
      }
    } else if (e.key === 'Escape') {
      this.closeMentionDropdown();
    }
  }

  showMentionDropdown(query) {
    const engineers = this.getActiveEngineers();
    let matches = [];

    // Include team broadcast tags
    if ('all'.includes(query) || 'everyone'.includes(query)) {
      matches.push({ isTag: true, tag: 'all', name: 'Everyone (All Active Engineers)' });
    }

    // Match engineers by username, full name, or ID
    engineers.forEach(eng => {
      const uname = (eng.username || '').toLowerCase();
      const fname = (eng.fullName || eng.name || '').toLowerCase();
      if (!query || uname.includes(query) || fname.includes(query)) {
        matches.push(eng);
      }
    });

    this.mentionSuggestions = matches.slice(0, 7);
    this.selectedMentionIndex = 0;

    if (this.mentionSuggestions.length === 0) {
      this.closeMentionDropdown();
      return;
    }

    this.renderMentionDropdown();
  }

  renderMentionDropdown() {
    const dd = this.dom.mentionDropdown;
    if (!dd) return;

    dd.innerHTML = '';
    this.mentionSuggestions.forEach((item, idx) => {
      const isSelected = idx === this.selectedMentionIndex;
      const row = document.createElement('div');
      row.className = `mention-item px-3 py-2 rounded-lg text-xs flex items-center justify-between cursor-pointer transition select-none ${
        isSelected ? 'bg-purple-100 dark:bg-purple-900/60 text-purple-900 dark:text-purple-200 font-bold' : 'hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
      }`;

      if (item.isTag) {
        row.innerHTML = `
          <div class="flex items-center gap-2">
            <span class="w-6 h-6 rounded-full bg-purple-600 text-white flex items-center justify-center text-xs font-bold">@</span>
            <span>@${item.tag}</span>
          </div>
          <span class="text-[10px] text-purple-600 font-semibold">${item.name}</span>
        `;
      } else {
        const initial = (item.fullName || item.name || 'E').charAt(0).toUpperCase();
        row.innerHTML = `
          <div class="flex items-center gap-2">
            <span class="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200 flex items-center justify-center text-[10px] font-bold">${initial}</span>
            <div class="flex flex-col">
              <span class="leading-tight">${item.fullName || item.name}</span>
              <span class="text-[10px] text-slate-400 font-mono">@${item.username || 'eng'}</span>
            </div>
          </div>
          <span class="text-[10px] text-purple-600 font-bold">Select</span>
        `;
      }

      // Use mousedown with preventDefault so textarea does not blur before selection
      row.addEventListener('mousedown', (e) => {
        e.preventDefault();
        this.selectMention(item);
      });
      row.addEventListener('click', (e) => {
        e.preventDefault();
        this.selectMention(item);
      });

      dd.appendChild(row);
    });

    dd.classList.remove('hidden');
    this.isMentionDropdownOpen = true;
  }

  highlightMentionItem() {
    const items = this.dom.mentionDropdown?.querySelectorAll('.mention-item');
    items?.forEach((item, idx) => {
      if (idx === this.selectedMentionIndex) {
        item.className = 'mention-item px-3 py-2 rounded-lg text-xs flex items-center justify-between cursor-pointer transition select-none bg-purple-100 dark:bg-purple-900/60 text-purple-900 dark:text-purple-200 font-bold';
      } else {
        item.className = 'mention-item px-3 py-2 rounded-lg text-xs flex items-center justify-between cursor-pointer transition select-none hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300';
      }
    });
  }

  selectMention(item) {
    const textarea = this.dom.messageInput;
    if (!textarea) return;

    const uname = item.isTag ? item.tag : (item.username || (item.name || 'engineer').toLowerCase().replace(/[\s\W]+/g, ''));
    const mentionTag = `@${uname}`;

    const startIdx = (this.mentionQueryStart >= 0) ? this.mentionQueryStart : 0;
    const endIdx = (this.mentionQueryEnd >= startIdx) ? this.mentionQueryEnd : textarea.selectionStart;

    const before = textarea.value.slice(0, startIdx);
    const after = textarea.value.slice(endIdx);

    textarea.value = `${before}${mentionTag} ${after}`;
    const newCursorPos = before.length + mentionTag.length + 1;
    textarea.setSelectionRange(newCursorPos, newCursorPos);
    textarea.focus();

    // Reset query range
    this.mentionQueryStart = -1;
    this.mentionQueryEnd = -1;

    // Auto-select in audience if in specific mode
    if (!item.isTag && this.audienceMode === 'specific') {
      const key = this.getEngineerKey(item);
      if (key) {
        this.selectedEngineerIds.add(key);
        this.renderRecipientChips();
      }
    }

    this.closeMentionDropdown();
    this.updatePreview();
  }

  closeMentionDropdown() {
    this.dom.mentionDropdown?.classList.add('hidden');
    this.isMentionDropdownOpen = false;
    this.mentionQueryStart = -1;
    this.mentionQueryEnd = -1;
  }

  insertToken(token) {
    const textarea = this.dom.messageInput;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;

    textarea.value = text.slice(0, start) + token + text.slice(end);
    textarea.setSelectionRange(start + token.length, start + token.length);
    textarea.focus();
    this.updatePreview();
  }

  // =========================================================================
  // 3. Dynamic Name Translation & Live Preview Simulator
  // =========================================================================

  populatePreviewEngineers() {
    if (!this.dom.previewSelect) return;
    const engineers = this.getActiveEngineers();
    this.dom.previewSelect.innerHTML = '';

    engineers.forEach((eng, idx) => {
      const opt = document.createElement('option');
      const key = this.getEngineerKey(eng);
      opt.value = key;
      opt.textContent = eng.fullName || eng.name || `Engineer ${idx + 1}`;
      this.dom.previewSelect.appendChild(opt);
    });

    if (engineers.length > 0) {
      this.previewEngineerId = this.getEngineerKey(engineers[0]);
    }
  }

  getSelectedPriority() {
    const checked = document.querySelector('input[name="broadcastPriority"]:checked');
    return checked ? checked.value : 'info';
  }

  resolveMessageContent(rawMessage, previewEngineer) {
    if (!rawMessage) return '';
    const engineers = this.getActiveEngineers();
    
    // Map username/id/name -> Full Name
    const nameByUser = {};
    engineers.forEach(e => {
      const uname = (e.username || '').toLowerCase();
      const uid = (e.id || '').toLowerCase();
      const rawName = (e.name || '').toLowerCase();
      const fname = e.fullName || e.name || uname;
      if (uname) nameByUser[uname] = fname;
      if (uid) nameByUser[uid] = fname;
      if (rawName) nameByUser[rawName] = fname;
    });

    const engName = previewEngineer?.fullName || previewEngineer?.name || 'Engineer';
    const engUsername = previewEngineer?.username || 'engineer';

    // 1. Substitute {name} & {username}
    let res = rawMessage
      .replace(/\{\s*(?:Their\s+name|their\s+name|name)\s*\}/gi, engName)
      .replace(/\{\s*username\s*\}/gi, engUsername)
      .replace(/@\{\s*username\s*\}/gi, `@${engUsername}`);

    // 2. Resolve @username mentions to styled badges with Full Name
    res = res.replace(/@([a-zA-Z0-9_\-\.]+)/g, (match, username) => {
      const u = username.toLowerCase();
      if (u === 'all' || u === 'everyone' || u === 'team') {
        return `<span class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-bold text-purple-700 bg-purple-100 dark:bg-purple-900/60 dark:text-purple-300">@Everyone (All Team)</span>`;
      }
      const fullName = nameByUser[u] || u;
      return `<span class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-bold text-purple-700 bg-purple-100 dark:bg-purple-900/60 dark:text-purple-300">@${fullName}</span>`;
    });

    return res;
  }

  updatePreview() {
    const rawTitle = this.dom.titleInput?.value.trim() || 'Shift Operations Announcement';
    const rawMessage = this.dom.messageInput?.value || '';
    const priority = this.getSelectedPriority();
    const engineers = this.getActiveEngineers();
    const previewEng = engineers.find(e => this.getEngineerKey(e) === this.previewEngineerId) || engineers[0];

    // Priority styling
    const priorityConfigs = {
      info: { label: 'INFO', bg: 'bg-blue-100 text-blue-800 dark:bg-blue-950/80 dark:text-blue-300 border-blue-200 dark:border-blue-800', dot: 'bg-blue-500' },
      shift_update: { label: 'SHIFT UPDATE', bg: 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300 border-amber-200 dark:border-amber-800', dot: 'bg-amber-500' },
      urgent: { label: 'URGENT', bg: 'bg-red-100 text-red-800 dark:bg-red-950/80 dark:text-red-300 border-red-200 dark:border-red-800 animate-pulse', dot: 'bg-red-500' },
      broadcast: { label: 'BROADCAST', bg: 'bg-purple-100 text-purple-800 dark:bg-purple-950/80 dark:text-purple-300 border-purple-200 dark:border-purple-800', dot: 'bg-purple-500' }
    };

    const cfg = priorityConfigs[priority] || priorityConfigs.info;
    if (this.dom.previewPriorityPill) {
      this.dom.previewPriorityPill.className = `px-2 py-0.5 rounded-full text-[10px] font-bold ${cfg.bg} border uppercase tracking-wider flex items-center gap-1`;
      this.dom.previewPriorityPill.innerHTML = `<span class="w-1.5 h-1.5 rounded-full ${cfg.dot}"></span> ${cfg.label}`;
    }

    if (this.dom.previewTitle) {
      this.dom.previewTitle.textContent = rawTitle;
    }

    if (this.dom.previewBody) {
      this.dom.previewBody.innerHTML = this.resolveMessageContent(rawMessage, previewEng);
    }

    this.updatePreviewChannels();
  }

  updatePreviewChannels() {
    if (!this.dom.previewChannelsBadges) return;
    const inApp = this.dom.channelInApp?.checked;
    const push = this.dom.channelPush?.checked;
    const email = this.dom.channelEmail?.checked;

    let html = '';
    if (inApp) {
      html += `<span class="inline-flex items-center gap-1 text-[10px] font-medium text-emerald-600 dark:text-emerald-400"><i class="ph ph-check-circle"></i> In-App</span>`;
    }
    if (push) {
      html += `<span class="inline-flex items-center gap-1 text-[10px] font-medium text-amber-600 dark:text-amber-400 font-bold"><i class="ph ph-bell"></i> FCM Push</span>`;
    }
    if (email) {
      html += `<span class="inline-flex items-center gap-1 text-[10px] font-medium text-blue-600 dark:text-blue-400"><i class="ph ph-check-circle"></i> Email</span>`;
    }

    this.dom.previewChannelsBadges.innerHTML = html || '<span class="text-[10px] text-red-500">No channels selected</span>';
  }

  // =========================================================================
  // 4. Operational Templates Engine
  // =========================================================================

  async loadTemplates() {
    if (!this.atlasService) return;
    const res = await this.atlasService.getBroadcastTemplates();
    if (res && res.success && res.templates) {
      this.templates = res.templates;
      this.renderTemplateSelect();
    }
  }

  renderTemplateSelect() {
    const sel = this.dom.templateSelect;
    if (!sel) return;

    const currentVal = sel.value;
    sel.innerHTML = '<option value="">-- Load Template --</option>';

    this.templates.forEach(t => {
      const opt = document.createElement('option');
      opt.value = t.id;
      opt.textContent = t.name || t.title;
      sel.appendChild(opt);
    });

    if (currentVal) sel.value = currentVal;
    this.updateTemplateActionButtons();
  }

  handleTemplateSelect() {
    const tplId = this.dom.templateSelect?.value;
    const tpl = this.templates.find(t => t.id === tplId);
    if (!tpl) {
      this.updateTemplateActionButtons();
      return;
    }

    if (this.dom.titleInput) this.dom.titleInput.value = tpl.title || '';
    if (this.dom.messageInput) this.dom.messageInput.value = tpl.message || '';

    // Set priority radio
    if (tpl.priority) {
      const r = document.querySelector(`input[name="broadcastPriority"][value="${tpl.priority}"]`);
      if (r) r.checked = true;
    }

    this.updateTemplateActionButtons();
    this.updatePreview();
  }

  updateTemplateActionButtons() {
    const tplId = this.dom.templateSelect?.value;
    const isCustom = tplId && !tplId.startsWith('tpl_shift_') && !tplId.startsWith('tpl_urgent_') && !tplId.startsWith('tpl_maintenance') && !tplId.startsWith('tpl_handover');
    if (this.dom.deleteTemplateBtn) {
      if (isCustom) {
        this.dom.deleteTemplateBtn.classList.remove('hidden');
      } else {
        this.dom.deleteTemplateBtn.classList.add('hidden');
      }
    }
  }

  openSaveTemplateModal() {
    const title = this.dom.titleInput?.value.trim() || '';
    const message = this.dom.messageInput?.value.trim() || '';
    if (!message) {
      this.showToast('Please type a message before saving as a template', 'error');
      return;
    }

    if (this.dom.saveTemplateNameInput) this.dom.saveTemplateNameInput.value = title;
    if (this.dom.saveTemplateTitleInput) this.dom.saveTemplateTitleInput.value = title;
    if (this.dom.saveTemplateBodyPreview) this.dom.saveTemplateBodyPreview.textContent = message;

    this.dom.modalSaveTemplate?.classList.remove('hidden');
  }

  closeSaveTemplateModal() {
    this.dom.modalSaveTemplate?.classList.add('hidden');
  }

  async saveCustomTemplate() {
    const name = this.dom.saveTemplateNameInput?.value.trim();
    const title = this.dom.saveTemplateTitleInput?.value.trim();
    const message = this.dom.messageInput?.value.trim();
    const priority = this.getSelectedPriority();

    if (!name || !title || !message) {
      this.showToast('Template name, title, and message are required', 'error');
      return;
    }

    const payload = {
      id: `tpl_custom_${Date.now()}`,
      name,
      title,
      message,
      priority,
      target_default: this.audienceMode
    };

    const res = await this.atlasService.saveBroadcastTemplate(payload);
    if (res && res.success) {
      this.showToast(`Template '${name}' saved to cloud database!`, 'success');
      this.closeSaveTemplateModal();
      await this.loadTemplates();
      if (this.dom.templateSelect) this.dom.templateSelect.value = payload.id;
      this.updateTemplateActionButtons();
    } else {
      this.showToast(res?.message || 'Failed to save template', 'error');
    }
  }

  async deleteCurrentTemplate() {
    const tplId = this.dom.templateSelect?.value;
    if (!tplId) return;

    if (!confirm('Are you sure you want to delete this custom template?')) return;

    const res = await this.atlasService.deleteBroadcastTemplate(tplId);
    if (res && res.success) {
      this.showToast('Template deleted successfully', 'success');
      await this.loadTemplates();
    } else {
      this.showToast(res?.message || 'Failed to delete template', 'error');
    }
  }

  // =========================================================================
  // 5. Multi-Channel Broadcast Dispatch
  // =========================================================================

  async dispatchBroadcast() {
    const title = this.dom.titleInput?.value.trim();
    const message = this.dom.messageInput?.value.trim();
    const priority = this.getSelectedPriority();
    const sendInApp = this.dom.channelInApp?.checked || false;
    const sendPush = this.dom.channelPush?.checked || false;
    const sendEmail = this.dom.channelEmail?.checked || false;

    if (!title) {
      this.showToast('Please enter an announcement title/subject', 'error');
      this.dom.titleInput?.focus();
      return;
    }

    if (!message) {
      this.showToast('Please enter announcement message content', 'error');
      this.dom.messageInput?.focus();
      return;
    }

    if (!sendInApp && !sendPush && !sendEmail) {
      this.showToast('Please select at least one delivery channel', 'error');
      return;
    }

    let target = 'everyone';
    if (this.audienceMode === 'specific') {
      const validIds = Array.from(this.selectedEngineerIds).filter(Boolean);
      if (validIds.length === 0) {
        this.showToast('Please select at least one engineer in the target audience', 'error');
        return;
      }
      target = validIds;
    }

    const payload = {
      title,
      message,
      target,
      priority,
      send_in_app: sendInApp,
      send_push: sendPush,
      send_email: sendEmail
    };

    // Button Spinner
    if (this.dom.dispatchBtn) this.dom.dispatchBtn.disabled = true;
    if (this.dom.dispatchBtnIcon) this.dom.dispatchBtnIcon.className = 'ph ph-spinner animate-spin text-base';
    if (this.dom.dispatchBtnText) this.dom.dispatchBtnText.textContent = 'Broadcasting Announcement...';

    try {
      const res = await this.atlasService.sendBroadcast(payload);
      if (res && res.success) {
        this.showToast(res.message || 'Broadcast successfully dispatched!', 'success');
        await this.loadHistory(true);
      } else {
        this.showToast(res?.message || 'Failed to dispatch broadcast', 'error');
      }
    } catch (err) {
      this.showToast(`Dispatch failed: ${err.message}`, 'error');
    } finally {
      if (this.dom.dispatchBtn) this.dom.dispatchBtn.disabled = false;
      if (this.dom.dispatchBtnIcon) this.dom.dispatchBtnIcon.className = 'ph ph-paper-plane-tilt text-base';
      this.updateAudienceSummary();
    }
  }

  // =========================================================================
  // 6. Broadcast History, Read Receipts & Retraction
  // =========================================================================

  async loadHistory(forceToast = false) {
    if (!this.atlasService) return;
    const res = await this.atlasService.getNotifications();
    if (res && res.success && res.notifications) {
      this.history = res.notifications;
      this.renderHistory();
      if (forceToast) {
        this.showToast(`Loaded ${this.history.length} broadcast records`, 'success');
      }
    }
  }

  renderHistory() {
    const list = this.dom.historyList;
    const empty = this.dom.historyEmpty;
    const badge = this.dom.historyCountBadge;
    if (!list) return;

    if (badge) badge.textContent = `${this.history.length} Dispatched`;

    if (!this.history || this.history.length === 0) {
      list.innerHTML = '';
      empty?.classList.remove('hidden');
      return;
    }

    empty?.classList.add('hidden');
    list.innerHTML = '';

    const priorityColors = {
      urgent: 'bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300 border-red-200 dark:border-red-800',
      shift_update: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300 border-amber-200 dark:border-amber-800',
      broadcast: 'bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300 border-purple-200 dark:border-purple-800',
      info: 'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border-blue-200 dark:border-blue-800'
    };

    this.history.forEach(notif => {
      const card = document.createElement('div');
      const isRetracted = bool(notif.is_retracted);
      const prioKey = (notif.priority || 'info').toLowerCase();
      const prioBadgeClass = priorityColors[prioKey] || priorityColors.info;

      card.className = `p-4 rounded-xl border transition ${
        isRetracted
          ? 'bg-slate-100/60 dark:bg-slate-900/40 border-slate-300 dark:border-slate-800 opacity-70'
          : 'bg-white dark:bg-slate-800/90 border-slate-200 dark:border-slate-700 shadow-xs hover:border-purple-300 dark:hover:border-purple-700'
      } space-y-3`;

      const readCount = notif.read_count || 0;
      const pendingCount = notif.pending_count || 0;
      const targetCount = notif.recipients_count || (notif.recipients ? notif.recipients.length : 0);

      card.innerHTML = `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div class="flex items-center gap-2 flex-wrap">
            <span class="px-2 py-0.5 rounded-full text-[10px] font-bold border uppercase tracking-wider ${prioBadgeClass}">
              ${notif.priority || 'INFO'}
            </span>
            ${isRetracted ? `
              <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300 border border-slate-300 dark:border-slate-600 flex items-center gap-1">
                <i class="ph ph-prohibit"></i> RETRACTED
              </span>
            ` : ''}
            <h4 class="text-xs sm:text-sm font-bold text-slate-900 dark:text-white">${notif.title || 'Untitled Notice'}</h4>
          </div>
          <span class="text-[10px] font-mono text-slate-400">${notif.created_at || 'Recently'}</span>
        </div>

        <div class="text-xs text-slate-600 dark:text-slate-300 line-clamp-2 leading-relaxed whitespace-pre-wrap font-sans">
          ${notif.raw_message || ''}
        </div>

        <div class="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-slate-100 dark:border-slate-700/60 text-[11px] text-slate-500">
          <div class="flex items-center gap-3">
            <span class="flex items-center gap-1">
              <i class="ph ph-user"></i> ${notif.sender || 'Admin'}
            </span>
            <span class="flex items-center gap-1">
              <i class="ph ph-users"></i> ${targetCount} Targeted
            </span>
            ${notif.emails_dispatched ? `
              <span class="flex items-center gap-1 text-blue-600 dark:text-blue-400">
                <i class="ph ph-envelope-simple"></i> ${notif.emails_dispatched} Emails
              </span>
            ` : ''}
            ${notif.push_dispatched ? `
              <span class="flex items-center gap-1 text-amber-600 dark:text-amber-400 font-semibold">
                <i class="ph ph-bell"></i> ${notif.push_dispatched} FCM Pushes
              </span>
            ` : ''}
            <!-- Live Read Receipts Pill -->
            <button type="button" class="btn-view-details inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800 hover:bg-emerald-100 cursor-pointer">
              <i class="ph ph-check-circle"></i> ${readCount} Read / ${pendingCount} Pending
            </button>
          </div>

          <!-- Actions -->
          <div class="flex items-center gap-1.5 ml-auto">
            <button type="button" class="btn-clone-broadcast px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 hover:bg-purple-50 hover:text-purple-700 dark:hover:bg-purple-950 dark:hover:text-purple-300 transition cursor-pointer" title="Load into composer">
              <i class="ph ph-copy"></i> Clone
            </button>
            <button type="button" class="btn-view-details px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-200 hover:bg-slate-200 dark:hover:bg-slate-600 transition cursor-pointer" title="View full details and recipient read status">
              <i class="ph ph-info"></i> Details
            </button>
            ${!isRetracted ? `
              <button type="button" class="btn-retract-broadcast px-2.5 py-1 rounded-lg text-xs font-semibold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/60 transition cursor-pointer" title="Retract announcement immediately from engineer portals">
                <i class="ph ph-arrow-u-up-left"></i> Retract
              </button>
            ` : ''}
          </div>
        </div>
      `;

      // Attach button event listeners
      card.querySelectorAll('.btn-view-details').forEach(btn => {
        btn.addEventListener('click', () => this.showBroadcastDetails(notif));
      });

      card.querySelector('.btn-clone-broadcast')?.addEventListener('click', () => {
        this.cloneToComposer(notif);
      });

      card.querySelector('.btn-retract-broadcast')?.addEventListener('click', () => {
        this.retractBroadcast(notif.id);
      });

      list.appendChild(card);
    });
  }

  cloneToComposer(notif) {
    if (this.dom.titleInput) this.dom.titleInput.value = notif.title || '';
    if (this.dom.messageInput) this.dom.messageInput.value = notif.raw_message || '';
    
    // Priority
    if (notif.priority) {
      const r = document.querySelector(`input[name="broadcastPriority"][value="${notif.priority.toLowerCase()}"]`);
      if (r) r.checked = true;
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.updatePreview();
    this.showToast('Loaded announcement into composer!', 'success');
  }

  async retractBroadcast(notificationId) {
    if (!notificationId) return;
    if (!confirm('Are you sure you want to retract this broadcast? It will be hidden immediately from all engineer in-app views.')) return;

    const res = await this.atlasService.retractBroadcast(notificationId);
    if (res && res.success) {
      this.showToast('Announcement retracted successfully.', 'success');
      await this.loadHistory(false);
    } else {
      this.showToast(res?.message || 'Failed to retract broadcast', 'error');
    }
  }

  showBroadcastDetails(notif) {
    if (!this.dom.modalDetails) return;

    if (this.dom.detailTitle) this.dom.detailTitle.textContent = notif.title || 'Announcement Details';
    if (this.dom.detailMeta) this.dom.detailMeta.textContent = `Dispatched by ${notif.sender || 'Admin'} on ${notif.created_at || 'Recently'}`;
    if (this.dom.detailBody) this.dom.detailBody.textContent = notif.raw_message || '';

    const targetCount = notif.recipients_count || (notif.recipients ? notif.recipients.length : 0);
    const readCount = notif.read_count || 0;
    const pendingCount = notif.pending_count || 0;

    if (this.dom.detailTargetCount) this.dom.detailTargetCount.textContent = targetCount;
    if (this.dom.detailReadCount) this.dom.detailReadCount.textContent = readCount;
    if (this.dom.detailPendingCount) this.dom.detailPendingCount.textContent = pendingCount;

    if (this.dom.detailReadListCount) this.dom.detailReadListCount.textContent = readCount;
    if (this.dom.detailPendingListCount) this.dom.detailPendingListCount.textContent = pendingCount;

    // Read Engineers List
    if (this.dom.detailReadList) {
      if (!notif.read_by || notif.read_by.length === 0) {
        this.dom.detailReadList.innerHTML = '<span class="text-slate-400 italic">No engineers have dismissed/read yet.</span>';
      } else {
        this.dom.detailReadList.innerHTML = notif.read_by.map(name => `
          <div class="flex items-center gap-1.5 py-0.5">
            <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            <span>${name}</span>
          </div>
        `).join('');
      }
    }

    // Pending Engineers List
    if (this.dom.detailPendingList) {
      if (!notif.pending || notif.pending.length === 0) {
        this.dom.detailPendingList.innerHTML = '<span class="text-emerald-600 dark:text-emerald-400 font-bold">All targeted engineers have read this announcement!</span>';
      } else {
        this.dom.detailPendingList.innerHTML = notif.pending.map(name => `
          <div class="flex items-center gap-1.5 py-0.5">
            <span class="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
            <span>${name}</span>
          </div>
        `).join('');
      }
    }

    this.dom.modalDetails.classList.remove('hidden');
  }

  closeDetailsModal() {
    this.dom.modalDetails?.classList.add('hidden');
  }
}

function bool(val) {
  return val === true || val === 'true' || val === 1;
}

export const broadcastController = new BroadcastController();
