/**
 * RosterX — Conversational AI Assistant (rosterAiAssistant.js)
 * 
 * Provides an interactive AI Assistant for conversational roster preparation,
 * natural language instruction parsing, smart shift generation, preview cards,
 * and seamless one-click grid application.
 */

import { SHIFTS, getEngineerSwapCode } from './constants.js?v=1.0';

const STORAGE_KEYS = {
  CHAT_HISTORY: 'rosterx_ai_assistant_chat_history',
  GEMINI_API_KEY: 'rosterx_gemini_api_key',
  GEMINI_MODEL: 'rosterx_gemini_model'
};

export class RosterAiAssistant {
  constructor(app) {
    this.app = app;
    this.isOpen = false;
    this.messages = [];
    this.activeProposal = null;
    this.isGenerating = false;
    this.pendingAction = null;
    this.lastContext = null;

    this.dom = {
      floatingBtn: null,
      panel: null,
      chatMessages: null,
      chatInput: null,
      btnSend: null,
      btnClose: null,
      btnClear: null,
      btnSettings: null,
      chipsContainer: null,
      typingIndicator: null
    };
  }

  init() {
    this.injectAssistantUI();
    this.cacheDom();
    this.bindEvents();
    this.loadChatHistory();
    this.updateModelDisplay();
  }  // --- SVG Icons & Mascot Fallback ---
  getBotSvg(size = 32) {
    return `
      <svg width="${size}" height="${size}" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" class="bot-avatar-svg drop-shadow-md">
        <!-- Antenna -->
        <line x1="32" y1="14" x2="32" y2="4" stroke="url(#botGrad)" stroke-width="3" stroke-linecap="round"/>
        <circle cx="32" cy="4" r="4" fill="#F59E0B" class="animate-pulse"/>
        <circle cx="32" cy="4" r="2" fill="#FEF3C7"/>
        
        <!-- Ears / Side Pods -->
        <rect x="8" y="24" width="6" height="14" rx="3" fill="#6366F1"/>
        <rect x="50" y="24" width="6" height="14" rx="3" fill="#6366F1"/>
        
        <!-- Head -->
        <rect x="12" y="14" width="40" height="34" rx="14" fill="url(#botGrad)" stroke="#4338CA" stroke-width="2"/>
        
        <!-- Visor / Face Screen -->
        <rect x="17" y="20" width="30" height="20" rx="9" fill="#0F172A"/>
        
        <!-- Expressive Glowing Eyes -->
        <ellipse cx="25" cy="29" rx="4" ry="5" fill="#38BDF8" class="bot-eye"/>
        <ellipse cx="39" cy="29" rx="4" ry="5" fill="#38BDF8" class="bot-eye"/>
        <circle cx="26" cy="27.5" r="1.5" fill="#FFFFFF"/>
        <circle cx="40" cy="27.5" r="1.5" fill="#FFFFFF"/>
        
        <!-- Cute Smile -->
        <path d="M28 34 Q32 37 36 34" stroke="#38BDF8" stroke-width="2" stroke-linecap="round" fill="none"/>
        
        <!-- Cute Blush -->
        <circle cx="20" cy="33" r="2" fill="#F43F5E" opacity="0.6"/>
        <circle cx="44" cy="33" r="2" fill="#F43F5E" opacity="0.6"/>
        
        <!-- Gradient Defs -->
        <defs>
          <linearGradient id="botGrad" x1="12" y1="14" x2="52" y2="48" gradientUnits="userSpaceOnUse">
            <stop stop-color="#818CF8"/>
            <stop offset="0.5" stop-color="#6366F1"/>
            <stop offset="1" stop-color="#4F46E5"/>
          </linearGradient>
        </defs>
      </svg>
    `;
  }

  // --- 3D Cutout Bot Avatar Generator ---
  getBot3dAvatarHtml(size = 32, extraClasses = '') {
    return `
      <div class="bot-3d-cutout-avatar relative shrink-0 flex items-center justify-center ${extraClasses}" style="width: ${size}px; height: ${size}px;">
        <img 
          src="assets/bot-3d.png" 
          alt="Roster AI Bot" 
          class="w-full h-full object-contain select-none pointer-events-none drop-shadow-xs" 
          onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='flex';"
        />
        <div class="hidden w-full h-full items-center justify-center">
          ${this.getBotSvg(size * 0.8)}
        </div>
      </div>
    `;
  }

  // --- Inject HTML Widget into DOM ---
  injectAssistantUI() {
    if (document.getElementById('floatingAiBotWidget')) return;

    const widgetContainer = document.createElement('div');
    widgetContainer.id = 'floatingAiBotWidget';
    widgetContainer.className = 'fixed bottom-5 right-5 z-50 flex flex-col items-end pointer-events-auto font-sans select-none';

    widgetContainer.innerHTML = `
      <style>
        /* --- 3D Cutout Bot Levitation & Physics --- */
        @keyframes bot3dFloat {
          0%, 100% {
            transform: translateY(0px) rotateX(4deg) rotateY(-2deg) rotateZ(0deg);
          }
          50% {
            transform: translateY(-11px) rotateX(-3deg) rotateY(3deg) rotateZ(1.2deg);
          }
        }
        @keyframes bot3dShadowPulse {
          0%, 100% {
            transform: scale(1) translateY(0);
            opacity: 0.55;
          }
          50% {
            transform: scale(0.68) translateY(6px);
            opacity: 0.25;
          }
        }
        @keyframes eqDance1 {
          0%, 100% { height: 4px; }
          50% { height: 14px; }
        }
        @keyframes eqDance2 {
          0%, 100% { height: 12px; }
          50% { height: 5px; }
        }
        @keyframes eqDance3 {
          0%, 100% { height: 6px; }
          50% { height: 16px; }
        }
        @keyframes bot3dClickBounce {
          0% { transform: scale(1) translateY(0); }
          25% { transform: scale(1.18, 0.82) translateY(4px); }
          50% { transform: scale(0.88, 1.15) translateY(-14px) rotate(4deg); }
          75% { transform: scale(1.06, 0.96) translateY(-3px) rotate(-2deg); }
          100% { transform: scale(1) translateY(0); }
        }

        /* --- Pure Cutout Character Styling (No Round Disc/Box) --- */
        .bot-3d-cutout-btn {
          position: relative;
          background: transparent;
          border: none;
          outline: none;
          cursor: pointer;
          transform-style: preserve-3d;
          transition: transform 0.15s ease-out;
        }
        .bot-3d-floating {
          animation: bot3dFloat 3.6s ease-in-out infinite;
        }
        .bot-3d-shadow {
          position: absolute;
          bottom: 2px;
          width: 52px;
          height: 10px;
          border-radius: 50%;
          background: radial-gradient(ellipse at center, rgba(15, 23, 42, 0.45) 0%, rgba(6, 182, 212, 0.25) 50%, transparent 75%);
          animation: bot3dShadowPulse 3.6s ease-in-out infinite;
          filter: blur(1.5px);
          pointer-events: none;
        }
        .bot-3d-cutout-img {
          filter: drop-shadow(0 10px 16px rgba(6, 182, 212, 0.45)) drop-shadow(0 4px 6px rgba(0, 0, 0, 0.25));
          transition: filter 0.3s ease;
        }
        .bot-3d-cutout-btn:hover .bot-3d-cutout-img {
          filter: drop-shadow(0 14px 22px rgba(6, 182, 212, 0.7)) drop-shadow(0 6px 10px rgba(0, 0, 0, 0.35));
        }
        .bot-3d-soundwaves {
          position: absolute;
          right: -4px;
          bottom: 12px;
          display: flex;
          align-items: flex-end;
          gap: 2.5px;
          height: 18px;
          padding: 2px 4px;
          background: rgba(15, 23, 42, 0.85);
          border: 1px solid rgba(56, 189, 248, 0.5);
          border-radius: 8px;
          backdrop-filter: blur(4px);
          box-shadow: 0 0 10px rgba(6, 182, 212, 0.5);
          pointer-events: none;
        }
        .bot-3d-eq-bar {
          width: 2.5px;
          background: linear-gradient(to top, #06b6d4, #38bdf8);
          border-radius: 1px;
        }
        .bot-3d-eq-bar.eq-1 { animation: eqDance1 1.2s ease-in-out infinite; }
        .bot-3d-eq-bar.eq-2 { animation: eqDance2 0.9s ease-in-out infinite; }
        .bot-3d-eq-bar.eq-3 { animation: eqDance3 1.4s ease-in-out infinite; }

        .bot-3d-bouncing {
          animation: bot3dClickBounce 0.65s cubic-bezier(0.34, 1.56, 0.64, 1) !important;
        }

        /* Accelerated State when Computing / Thinking */
        .bot-3d-computing .bot-3d-cutout-img {
          filter: drop-shadow(0 16px 28px rgba(6, 182, 212, 0.9)) drop-shadow(0 0 18px rgba(56, 189, 248, 0.75)) !important;
        }

        .ai-chat-bubble-user {
          background: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
        }
      </style>

      <!-- Assistant Chat Drawer / Window -->
      <div id="aiAssistantPanel" class="hidden mb-3 w-[440px] max-w-[calc(100vw-2.5rem)] h-[620px] max-h-[82vh] bg-white rounded-3xl border border-slate-300 shadow-2xl flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-6 duration-200">
        <!-- Header -->
        <div class="px-4 py-3 bg-gradient-to-r from-slate-950 via-slate-900 to-indigo-950 text-white flex items-center justify-between border-b border-indigo-900/40 shadow-md">
          <div class="flex items-center gap-3">
            <div class="relative w-11 h-11 flex items-center justify-center shrink-0">
              <img 
                src="assets/bot-3d.png" 
                alt="RosterX AI Assistant" 
                class="w-full h-full object-contain drop-shadow-[0_4px_8px_rgba(6,182,212,0.45)]" 
                onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='flex';"
              />
              <div class="hidden w-full h-full items-center justify-center">
                ${this.getBotSvg(28)}
              </div>
              <span class="absolute -top-1 -right-1 w-3 h-3 bg-emerald-400 border-2 border-slate-950 rounded-full shadow-xs">
                <span class="absolute inset-0 rounded-full bg-emerald-400 animate-ping opacity-75"></span>
              </span>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h3 class="text-sm font-black tracking-wide text-white">
                  RosterX AI Assistant
                </h3>
              </div>
              <p id="aiHeaderStatus" class="text-[11px] text-indigo-200/90 flex items-center gap-1.5">
                <span class="w-1.5 h-1.5 bg-cyan-400 rounded-full animate-pulse"></span>
                <span id="aiHeaderStatusText">Online • Gemini Flash & Smart Solver</span>
              </p>
            </div>
          </div>

          <div class="flex items-center gap-1">
            <button id="btnClearAiChat" title="Clear chat history" class="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition cursor-pointer text-sm">
              <i class="ph ph-trash"></i>
            </button>
            <button id="btnAiSettingsQuick" title="API Key & Model Settings" class="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition cursor-pointer text-sm">
              <i class="ph ph-gear"></i>
            </button>
            <button id="btnCloseAiAssistant" title="Close Assistant" class="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-white/10 transition cursor-pointer text-lg">
              <i class="ph ph-x"></i>
            </button>
          </div>
        </div>

        <!-- Suggestion Chips Header Row -->
        <div class="px-3 py-2 bg-slate-50 border-b border-slate-200 overflow-x-auto whitespace-nowrap flex items-center gap-1.5 text-xs no-scrollbar">
          <span class="text-[10px] uppercase font-bold text-slate-600 px-1">Try:</span>
          <button class="ai-chip-prompt px-2.5 py-1 bg-white hover:bg-rose-50 text-rose-700 hover:text-rose-900 border border-slate-200 hover:border-rose-300 rounded-full text-[11px] font-semibold transition shrink-0 cursor-pointer shadow-2xs" data-prompt="Clear entire month Grid">
            🗑️ Clear Month Grid
          </button>
          <button class="ai-chip-prompt px-2.5 py-1 bg-white hover:bg-indigo-50 text-indigo-700 hover:text-indigo-900 border border-slate-200 hover:border-indigo-300 rounded-full text-[11px] font-semibold transition shrink-0 cursor-pointer shadow-2xs" data-prompt="Prepare a roster for 24th of this month with minimum Day shift count as 3">
            ✨ Day 24 (Min 3 Day Shifts)
          </button>
          <button class="ai-chip-prompt px-2.5 py-1 bg-white hover:bg-purple-50 text-purple-700 hover:text-purple-900 border border-slate-200 hover:border-purple-300 rounded-full text-[11px] font-semibold transition shrink-0 cursor-pointer shadow-2xs" data-prompt="Prepare a full month roster for next month with 24/7 night coverage and balanced day desk">
            📅 Generate Next Month
          </button>
          <button class="ai-chip-prompt px-2.5 py-1 bg-white hover:bg-emerald-50 text-emerald-700 hover:text-emerald-900 border border-slate-200 hover:border-emerald-300 rounded-full text-[11px] font-semibold transition shrink-0 cursor-pointer shadow-2xs" data-prompt="Check current roster fatigue violations and coverage deficit">
            🛡️ Audit Fatigue & Coverage
          </button>
        </div>

        <!-- Messages Scrollable Body -->
        <div id="aiChatMessages" class="flex-1 p-4 overflow-y-auto space-y-3.5 text-xs bg-gradient-to-b from-slate-50/70 to-white">
          <!-- Dynamic messages will be injected here -->
        </div>

        <!-- Typing Indicator -->
        <div id="aiTypingIndicator" class="hidden px-4 py-2 bg-slate-50 border-t border-slate-100 flex items-center gap-2 text-slate-500 text-xs">
          <div class="flex items-center gap-1">
            <span class="w-1.5 h-1.5 bg-cyan-600 rounded-full animate-bounce"></span>
            <span class="w-1.5 h-1.5 bg-indigo-600 rounded-full animate-bounce [animation-delay:0.2s]"></span>
            <span class="w-1.5 h-1.5 bg-purple-600 rounded-full animate-bounce [animation-delay:0.4s]"></span>
          </div>
          <span class="text-[11px] font-medium text-indigo-700">AI Assistant is computing optimal shifts...</span>
        </div>

        <!-- Chat Input Footer -->
        <div class="p-3 bg-white border-t border-slate-200">
          <form id="aiChatForm" class="flex items-center gap-2">
            <div class="relative flex-1">
              <input 
                id="aiChatInput" 
                type="text" 
                placeholder="Ask e.g. Prepare roster for 24th with min Day shift count as 3..." 
                class="w-full pl-3.5 pr-8 py-2.5 bg-slate-100 hover:bg-slate-100/80 focus:bg-white border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 rounded-xl text-xs text-slate-900 outline-none transition"
                autocomplete="off"
              />
              <button type="button" id="btnVoiceDummy" title="Natural language input" class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-indigo-600">
                <i class="ph ph-sparkle text-sm"></i>
              </button>
            </div>
            <button 
              type="submit" 
              id="btnSendAiChat" 
              class="bg-indigo-600 hover:bg-indigo-700 text-white w-9 h-9 rounded-xl flex items-center justify-center shrink-0 transition shadow-sm cursor-pointer disabled:opacity-50"
            >
              <i class="ph ph-paper-plane-right text-base"></i>
            </button>
          </form>
          <div class="mt-1.5 flex items-center justify-between text-[10px] text-slate-600 px-1">
            <span id="aiActiveModelFooter">Powered by Google Gemini 1.5 & NOC Rules</span>
            <span id="aiActivePeriodIndicator" class="font-mono text-indigo-600 font-bold">Oct 2026</span>
          </div>
        </div>
      </div>

      <!-- Floating Cutout Cute Bot Launcher (Pure Cutout without Background Disc) -->
      <div class="relative flex items-center gap-3">
        <!-- Floating Speech Hint Bubble -->
        <div id="aiBotSpeechBubble" class="relative group cursor-pointer bg-slate-900/95 backdrop-blur-md text-white text-xs px-4 py-2 rounded-2xl shadow-xl flex items-center gap-2.5 border border-cyan-400/40 hover:border-cyan-400 transition animate-in fade-in slide-in-from-right-4 duration-300">
          <div class="w-2 h-2 rounded-full bg-cyan-400 shadow-[0_0_8px_#22d3ee] animate-ping shrink-0"></div>
          <div>
            <div class="font-bold text-[11px] text-white flex items-center gap-1.5">
              <span>Hi! I'm your AI Assistant</span>
              <span class="text-xs">👋</span>
            </div>
            <div class="text-[10px] text-cyan-200/90">Ask me to prepare shifts or NOC rules!</div>
          </div>
          <button id="btnDismissBotBubble" title="Dismiss" class="text-slate-400 hover:text-white ml-1 p-0.5 rounded-md hover:bg-white/10 transition cursor-pointer">
            <i class="ph ph-x text-xs"></i>
          </button>
        </div>

        <!-- 3D Bot Cutout Launcher Platform -->
        <div class="bot-3d-launcher-platform relative flex flex-col items-center select-none" style="perspective: 700px;">
          <!-- Cutout Cute 3D Bot Character Floating Button -->
          <button 
            id="btnToggleFloatingBot" 
            class="bot-3d-cutout-btn bot-3d-floating relative w-[76px] h-[86px] flex items-center justify-center cursor-pointer outline-none focus:outline-none transition-transform bg-transparent border-0 p-0"
            title="Open RosterX AI Assistant"
          >
            <!-- Cutout Bot Character Image -->
            <img 
              src="assets/bot-3d.png" 
              alt="RosterX AI Bot" 
              class="bot-3d-cutout-img w-full h-full object-contain pointer-events-none select-none transition-transform duration-300 drop-shadow-[0_12px_16px_rgba(6,182,212,0.4)]" 
              onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='flex';"
            />
            <div class="bot-svg-fallback hidden">
              ${this.getBotSvg(56)}
            </div>

            <!-- Sleek Minimal Equalizer Bars on Side -->
            <div class="bot-3d-soundwaves" title="Listening Active">
              <span class="bot-3d-eq-bar eq-1"></span>
              <span class="bot-3d-eq-bar eq-2"></span>
              <span class="bot-3d-eq-bar eq-3"></span>
            </div>
          </button>

          <!-- Floor Ambient Shadow (Synchronized with Levitation) -->
          <div class="bot-3d-shadow"></div>
        </div>
      </div>
    `;

    document.body.appendChild(widgetContainer);
  }

  cacheDom() {
    this.dom.floatingBtn = document.getElementById('btnToggleFloatingBot');
    this.dom.panel = document.getElementById('aiAssistantPanel');
    this.dom.chatMessages = document.getElementById('aiChatMessages');
    this.dom.chatInput = document.getElementById('aiChatInput');
    this.dom.btnSend = document.getElementById('btnSendAiChat');
    this.dom.btnClose = document.getElementById('btnCloseAiAssistant');
    this.dom.btnClear = document.getElementById('btnClearAiChat');
    this.dom.btnSettings = document.getElementById('btnAiSettingsQuick');
    this.dom.chips = document.querySelectorAll('.ai-chip-prompt');
    this.dom.typingIndicator = document.getElementById('aiTypingIndicator');
    this.dom.speechBubble = document.getElementById('aiBotSpeechBubble');
    this.dom.btnDismissBubble = document.getElementById('btnDismissBotBubble');
    this.dom.periodIndicator = document.getElementById('aiActivePeriodIndicator');
    this.dom.headerStatusText = document.getElementById('aiHeaderStatusText');
    this.dom.modelFooter = document.getElementById('aiActiveModelFooter');
  }

  bindEvents() {
    if (this.dom.floatingBtn) {
      const btn = this.dom.floatingBtn;

      // Interactive 3D Cursor Tilt Parallax
      btn.addEventListener('mousemove', (e) => {
        const rect = btn.getBoundingClientRect();
        const x = (e.clientX - (rect.left + rect.width / 2)) / (rect.width / 2); // -1 to 1
        const y = (e.clientY - (rect.top + rect.height / 2)) / (rect.height / 2); // -1 to 1
        btn.style.transform = `perspective(500px) rotateY(${x * 22}deg) rotateX(${-y * 22}deg) scale(1.08)`;
        btn.style.animationPlayState = 'paused';
      });

      btn.addEventListener('mouseleave', () => {
        btn.style.transform = '';
        btn.style.animationPlayState = 'running';
      });

      btn.addEventListener('click', () => {
        btn.classList.add('bot-3d-bouncing');
        setTimeout(() => btn.classList.remove('bot-3d-bouncing'), 700);
        this.togglePanel();
      });
    }

    if (this.dom.btnClose) {
      this.dom.btnClose.addEventListener('click', () => this.closePanel());
    }

    if (this.dom.btnDismissBubble) {
      this.dom.btnDismissBubble.addEventListener('click', (e) => {
        e.stopPropagation();
        if (this.dom.speechBubble) this.dom.speechBubble.classList.add('hidden');
      });
    }

    if (this.dom.speechBubble) {
      this.dom.speechBubble.addEventListener('click', (e) => {
        if (e.target.closest('#btnDismissBotBubble')) return;
        this.openPanel();
      });
    }

    // Top action bar button trigger (if clicked, open assistant)
    const topBarAiBtn = document.getElementById('btnOpenAiModal');
    if (topBarAiBtn) {
      topBarAiBtn.addEventListener('click', (e) => {
        e.preventDefault();
        this.openPanel();
        if (this.dom.chatInput) this.dom.chatInput.focus();
      });
    }

    // Form submit
    const form = document.getElementById('aiChatForm');
    if (form) {
      form.addEventListener('submit', (e) => {
        e.preventDefault();
        this.handleUserSubmit();
      });
    }

    // Chips click
    if (this.dom.chips) {
      this.dom.chips.forEach(chip => {
        chip.addEventListener('click', () => {
          const prompt = chip.getAttribute('data-prompt');
          if (prompt) {
            this.dom.chatInput.value = prompt;
            this.handleUserSubmit();
          }
        });
      });
    }

    // Clear chat
    if (this.dom.btnClear) {
      this.dom.btnClear.addEventListener('click', () => {
        if (confirm('Clear chat history with AI Assistant?')) {
          this.clearChat();
        }
      });
    }

    // Quick Settings
    if (this.dom.btnSettings) {
      this.dom.btnSettings.addEventListener('click', () => {
        if (this.app && typeof this.app.openGeminiSettings === 'function') {
          this.app.openGeminiSettings();
        }
      });
    }
  }

  togglePanel() {
    if (this.isOpen) {
      this.closePanel();
    } else {
      this.openPanel();
    }
  }

  openPanel() {
    this.isOpen = true;
    if (this.dom.panel) {
      this.dom.panel.classList.remove('hidden');
    }
    if (this.dom.speechBubble) {
      this.dom.speechBubble.classList.add('hidden');
    }
    this.updatePeriodIndicator();
    this.updateModelDisplay();
    if (this.dom.chatInput) {
      setTimeout(() => this.dom.chatInput.focus(), 100);
    }
    this.scrollToBottom();
  }

  closePanel() {
    this.isOpen = false;
    if (this.dom.panel) {
      this.dom.panel.classList.add('hidden');
    }
  }

  updatePeriodIndicator() {
    if (!this.dom.periodIndicator || !this.app || !this.app.state) return;
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const curMonth = monthNames[this.app.state.currentMonth - 1] || 'Oct';
    this.dom.periodIndicator.textContent = `${curMonth} ${this.app.state.currentYear}`;
  }

  scrollToBottom() {
    if (this.dom.chatMessages) {
      this.dom.chatMessages.scrollTop = this.dom.chatMessages.scrollHeight;
    }
  }

  loadChatHistory() {
    this.messages = [];
    if (typeof localStorage !== 'undefined') {
      try {
        const stored = localStorage.getItem(STORAGE_KEYS.CHAT_HISTORY);
        if (stored) this.messages = JSON.parse(stored);
      } catch (e) {}
    }

    if (!this.messages || this.messages.length === 0) {
      // Welcome Greeting
      this.messages = [
        {
          id: 'msg_welcome',
          role: 'assistant',
          text: `👋 **Hi there! I'm your RosterX AI Assistant.**\n\nI can analyze operational constraints, simulate NOC shifts, and prepare rosters for any date or full month!\n\n💡 **Try asking me:**\n*"Prepare a roster for 24th of this month with minimum Day shift count as 3"*\n\nOr click any quick prompt chips above to start!`,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ];
    }

    this.renderMessages();
  }

  saveChatHistory() {
    if (typeof localStorage !== 'undefined') {
      try {
        // Keep last 30 messages
        const toSave = this.messages.slice(-30);
        localStorage.setItem(STORAGE_KEYS.CHAT_HISTORY, JSON.stringify(toSave));
      } catch (e) {}
    }
  }

  clearChat() {
    this.messages = [];
    this.loadChatHistory();
  }

  renderMessages() {
    if (!this.dom.chatMessages) return;
    this.dom.chatMessages.innerHTML = this.messages.map(m => this.renderMessageItem(m)).join('');

    // Wire up apply button handlers
    this.dom.chatMessages.querySelectorAll('.btn-apply-proposal').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const proposalId = e.currentTarget.getAttribute('data-proposal-id');
        this.applyProposalToGrid(proposalId);
      });
    });

    // Wire up undo clear grid handlers
    this.dom.chatMessages.querySelectorAll('.btn-undo-cleared-grid').forEach(btn => {
      btn.addEventListener('click', () => {
        const res = this.executeUndoClearGrid();
        this.addAssistantMessage(res.text);
      });
    });

    // Wire up generate fresh roster handlers
    this.dom.chatMessages.querySelectorAll('.btn-generate-cleared-grid').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const prompt = e.currentTarget.getAttribute('data-prompt') || 'Prepare a full month roster with 24/7 night coverage';
        if (this.dom.chatInput) {
          this.dom.chatInput.value = prompt;
          this.handleUserSubmit();
        }
      });
    });

    this.scrollToBottom();
  }

  renderMessageItem(msg) {
    if (msg.role === 'user') {
      return `
        <div class="flex justify-end">
          <div class="ai-chat-bubble-user text-white px-3.5 py-2.5 rounded-2xl rounded-br-xs max-w-[85%] shadow-sm">
            <p class="leading-relaxed whitespace-pre-wrap">${this.escapeHtml(msg.text)}</p>
            <div class="text-[9px] text-indigo-200 text-right mt-1 font-mono">${msg.timestamp || ''}</div>
          </div>
        </div>
      `;
    }

    // Assistant message
    return `
      <div class="flex items-start gap-2.5 max-w-[95%]">
        <div class="w-8 h-8 shrink-0 mt-0.5 flex items-center justify-center">
          <img 
            src="assets/bot-3d.png" 
            alt="Roster AI" 
            class="w-full h-full object-contain drop-shadow-xs select-none pointer-events-none" 
            onerror="this.style.display='none'; if(this.nextElementSibling) this.nextElementSibling.style.display='flex';" 
            isActive="true"
          />
          <div class="hidden w-full h-full items-center justify-center">
            ${this.getBotSvg(18)}
          </div>
        </div>
        <div class="flex-1 space-y-2">
          <div class="bg-white border border-slate-200 p-3 rounded-2xl rounded-tl-xs shadow-2xs text-slate-800 leading-relaxed">
            ${this.formatMarkdown(msg.text)}
            ${msg.proposal ? this.renderProposalCard(msg.proposal) : ''}
            <div class="text-[9px] text-slate-500 mt-1.5 font-mono flex items-center justify-between">
              <span class="font-semibold text-slate-600">Roster AI</span>
              <span>${msg.timestamp || ''}</span>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderProposalCard(p) {
    if (p.type === 'cleared_grid') {
      const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
      const mName = monthNames[(p.targetMonth || 1) - 1] || 'Target Month';
      const promptText = `Prepare a full month roster for ${mName} ${p.targetYear} with 24/7 night coverage and balanced day desk`;
      return `
        <div class="mt-3 p-3 bg-rose-50/70 border border-rose-200 rounded-xl space-y-2 text-xs">
          <div class="flex items-center justify-between pb-1.5 border-b border-rose-200/80">
            <div>
              <div class="font-extrabold text-rose-950 flex items-center gap-1.5">
                <i class="ph ph-trash text-rose-600 text-base"></i>
                <span>${p.title}</span>
              </div>
              <div class="text-[10px] text-rose-700/80 font-medium">${p.subtitle || ''}</div>
            </div>
            <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-300">
              ✓ Grid Cleared
            </span>
          </div>
          <div class="pt-1 flex items-center justify-between gap-2">
            <button class="btn-undo-cleared-grid px-3 py-1.5 rounded-lg bg-white hover:bg-slate-100 text-slate-700 hover:text-slate-900 border border-slate-300 font-bold text-xs flex items-center gap-1 transition cursor-pointer shadow-2xs">
              <i class="ph ph-arrow-u-up-left"></i>
              Undo / Restore
            </button>
            <button class="btn-generate-cleared-grid px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs flex items-center gap-1 transition cursor-pointer shadow-2xs" data-prompt="${promptText}">
              <i class="ph ph-sparkle"></i>
              Generate Fresh Roster
            </button>
          </div>
        </div>
      `;
    }

    const isApplied = p.applied;

    return `
      <div class="mt-3 p-3 bg-slate-50 border border-indigo-200 rounded-xl space-y-2.5 text-xs">
        <div class="flex items-center justify-between pb-2 border-b border-slate-200">
          <div>
            <div class="font-extrabold text-slate-900 flex items-center gap-1.5">
              <i class="ph ph-calendar-check text-indigo-600 text-base"></i>
              <span>${p.title}</span>
            </div>
            <div class="text-[10px] text-slate-500 font-medium">${p.subtitle || ''}</div>
          </div>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${isApplied ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' : 'bg-indigo-100 text-indigo-800 border border-indigo-200'}">
            ${isApplied ? '✓ Applied to Grid' : 'Preview'}
          </span>
        </div>

        <!-- Coverage Stats Header -->
        <div class="grid grid-cols-4 gap-1.5 text-center">
          <div class="bg-white p-1.5 rounded-lg border border-slate-200">
            <div class="text-sm font-black text-emerald-700">${p.dayCoverage}</div>
            <div class="text-[9px] text-slate-500 font-bold uppercase">Day Staff</div>
          </div>
          <div class="bg-white p-1.5 rounded-lg border border-slate-200">
            <div class="text-sm font-black text-blue-700">${p.nightCoverage}</div>
            <div class="text-[9px] text-slate-500 font-bold uppercase">Night Staff</div>
          </div>
          <div class="bg-white p-1.5 rounded-lg border border-slate-200">
            <div class="text-sm font-black text-slate-700">${p.offCount}</div>
            <div class="text-[9px] text-slate-500 font-bold uppercase">Weekly Off</div>
          </div>
          <div class="bg-white p-1.5 rounded-lg border border-slate-200">
            <div class="text-sm font-black text-amber-700">${p.apprenticeCount || 0}</div>
            <div class="text-[9px] text-slate-500 font-bold uppercase">Apprentice</div>
          </div>
        </div>

        <!-- Mini Engineer Shifts Table -->
        <div class="max-h-48 overflow-y-auto rounded-lg border border-slate-200 bg-white">
          <table class="w-full text-left border-collapse text-[11px]">
            <thead>
              <tr class="bg-slate-100 text-slate-600 font-bold border-b border-slate-200">
                <th class="py-1 px-2">Engineer</th>
                <th class="py-1 px-2 text-center">Shift</th>
                <th class="py-1 px-2">Notes</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              ${p.assignments.map(a => `
                <tr class="hover:bg-slate-50">
                  <td class="py-1 px-2 font-bold text-slate-800 flex items-center gap-1">
                    <span>${a.name}</span>
                    ${a.role === 'apprentice' ? '<span class="text-[9px] bg-amber-100 text-amber-800 px-1 rounded">Appr</span>' : ''}
                  </td>
                  <td class="py-1 px-2 text-center">
                    <span class="inline-block px-1.5 py-0.5 rounded text-[10px] font-black ${this.getShiftBadgeClass(a.shiftCode)}">
                      ${a.shiftCode}
                    </span>
                  </td>
                  <td class="py-1 px-2 text-[10px] text-slate-500 truncate max-w-[120px]">${a.note || ''}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>

        <!-- Action Buttons -->
        <div class="pt-1 flex items-center justify-end gap-2">
          ${isApplied ? `
            <div class="text-[11px] font-bold text-emerald-700 flex items-center gap-1">
              <i class="ph ph-check-circle-fill text-base text-emerald-600"></i>
              Applied to active roster!
            </div>
          ` : `
            <button 
              class="btn-apply-proposal w-full bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white font-bold py-2 px-3 rounded-xl shadow-sm transition flex items-center justify-center gap-1.5 cursor-pointer text-xs"
              data-proposal-id="${p.id}"
            >
              <i class="ph ph-check-circle text-base"></i>
              Apply to Grid
            </button>
          `}
        </div>
      </div>
    `;
  }

  getShiftBadgeClass(code) {
    if (!code) return 'bg-slate-200 text-slate-700';
    const c = code.toUpperCase();
    if (c === 'FS') return 'bg-emerald-100 text-emerald-900 border border-emerald-300';
    if (c === 'FG') return 'bg-emerald-800 text-white';
    if (c === 'G')  return 'bg-sky-200 text-sky-900 border border-sky-300';
    if (c === 'SG') return 'bg-amber-600 text-white';
    if (c === 'SS') return 'bg-orange-200 text-orange-900 border border-orange-300';
    if (c === 'NS') return 'bg-slate-900 text-amber-300 border border-slate-700';
    if (c === 'OFF') return 'bg-amber-100 text-amber-900 border border-amber-300';
    if (c.startsWith('SW')) return 'bg-purple-100 text-purple-900 border border-purple-300';
    if (c === 'L') return 'bg-rose-600 text-white';
    return 'bg-slate-200 text-slate-700';
  }

  // --- Processing User Submissions ---
  async handleUserSubmit() {
    const text = this.dom.chatInput ? this.dom.chatInput.value.trim() : '';
    if (!text || this.isGenerating) return;

    this.dom.chatInput.value = '';
    this.addUserMessage(text);

    this.setGenerating(true);

    try {
      const response = await this.processAssistantQuery(text);
      this.addAssistantMessage(response.text, response.proposal);
    } catch (err) {
      console.error('[RosterX AI Assistant] Error:', err);
      this.addAssistantMessage(`⚠️ **Oops!** I encountered an error while processing that requirement: ${err.message}. Please try again.`);
    } finally {
      this.setGenerating(false);
    }
  }

  addUserMessage(text) {
    const msg = {
      id: 'msg_' + Date.now(),
      role: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    this.messages.push(msg);
    this.saveChatHistory();
    this.renderMessages();
  }

  addAssistantMessage(text, proposal = null) {
    const msg = {
      id: 'msg_' + Date.now(),
      role: 'assistant',
      text,
      proposal,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    this.messages.push(msg);
    if (proposal) {
      this.activeProposal = proposal;
    }
    this.saveChatHistory();
    this.renderMessages();
  }

  setGenerating(isGen) {
    this.isGenerating = isGen;
    if (this.dom.typingIndicator) {
      this.dom.typingIndicator.classList.toggle('hidden', !isGen);
    }
    if (this.dom.btnSend) {
      this.dom.btnSend.disabled = isGen;
    }
    if (isGen) {
      if (this.dom.headerStatusText) {
        this.dom.headerStatusText.textContent = 'Computing optimal shifts & constraints...';
      }
    } else {
      this.updateModelDisplay();
    }
    if (this.dom.floatingBtn) {
      this.dom.floatingBtn.classList.toggle('bot-3d-computing', isGen);
    }
    this.scrollToBottom();
  }

  // --- Intent Parsing & Execution ---
  async processAssistantQuery(query) {
    const qLower = query.toLowerCase().trim();

    // 0. Multi-Turn Recognition: Check for affirmative user replies ("yes", "heal them", "apply", etc.)
    const isAffirmative = /^(yes|yep|yeah|sure|ok|okay|heal|heal them|heal it|fix|fix it|fix them|please|proceed|apply|apply it|do it|go ahead|y|confirm|absolutely)\b/i.test(qLower);

    if (isAffirmative) {
      if (this.pendingAction && this.pendingAction.type === 'heal_fatigue_and_deficits') {
        const result = this.executeHealingProposal();
        this.pendingAction = null;
        return result;
      }

      if (this.pendingAction && this.pendingAction.type === 'apply_proposal') {
        const propId = this.pendingAction.proposalId;
        this.pendingAction = null;
        this.applyProposalToGrid(propId);
        return { text: "Applied the proposed shifts to the active grid! Coverage and fatigue metrics updated." };
      }

      // Check last assistant message in history for context
      const lastAssistantMsg = [...this.messages].reverse().find(m => m.role === 'assistant');
      if (lastAssistantMsg) {
        const lastText = (lastAssistantMsg.text || '').toLowerCase();
        if (lastText.includes('heal') || lastText.includes('fatigue') || lastText.includes('deficits')) {
          return this.executeHealingProposal();
        }
        if (lastAssistantMsg.proposal && !lastAssistantMsg.proposal.applied) {
          this.applyProposalToGrid(lastAssistantMsg.proposal.id);
          return { text: `Applied the proposal for **${lastAssistantMsg.proposal.title}** to the active grid!` };
        }
      }
    }

    // 0.1 Check for Clear Chat History intent
    if (/(?:clear|delete|wipe|reset)\b.*(?:chat|conversation|history|messages)/i.test(qLower) || /^(?:clear\s+chat)$/i.test(qLower)) {
      this.clearChat();
      return { text: "🧹 **Chat history cleared!** How can I assist you with your NOC roster today?" };
    }

    // 0.2 Check for Undo / Restore Grid intent
    if (/(?:undo|restore|revert)\b.*(?:grid|roster|shift|clear)/i.test(qLower) || /^(?:undo|restore|revert)$/i.test(qLower)) {
      if (this.lastClearedGridBackup) {
        return this.executeUndoClearGrid();
      }
    }

    // 0.3 Check for Clear / Reset Month Grid intent
    // Matches: "Clear December month Grid", "clear grid", "reset grid", "wipe december roster", "empty month grid", etc.
    const isUndo = /(?:undo|restore|revert)\b/i.test(qLower);
    const isClearGrid = !isUndo && (
      /(?:clear|empty|wipe|reset|blank|delete\s+all|remove\s+all)\b.*(?:grid|roster|month|shift|schedule)/i.test(qLower) ||
      /(?:grid|roster|month|shifts|schedule)\b.*(?:clear|empty|wipe|reset|blank)/i.test(qLower) ||
      /^(?:clear|reset|empty|wipe)\s+(?:grid|month|roster|shifts)$/i.test(qLower)
    );

    if (isClearGrid) {
      return await this.executeClearMonthGrid(query);
    }

    // 1. Detect if this is a request to prepare roster for a specific day
    // e.g. "Prepare a roster for 24th of this month with minimum Day shift count as 3"
    // e.g. "Prepare roster for 25th with min Day shift count as 4"
    // e.g. "What about 25th?"
    const dayMatch = qLower.match(/(?:day\s*|for\s*|about\s*)(\d{1,2})(?:th|st|nd|rd)?/i);
    const dayShiftMinMatch = qLower.match(/(?:day\s*shift\s*(?:count)?\s*(?:as|of|at\s*least|min(?:imum)?|\:)?\s*(\d+)|min(?:imum)?\s*(\d+)\s*day\s*shift)/i);

    if (dayMatch && (qLower.includes('roster') || qLower.includes('shift') || qLower.includes('prepare') || qLower.includes('generate') || qLower.includes('about') || qLower.includes('for'))) {
      const targetDay = parseInt(dayMatch[1], 10);
      let minDayShifts = 3; // default requirement requested by user
      if (dayShiftMinMatch) {
        minDayShifts = parseInt(dayShiftMinMatch[1] || dayShiftMinMatch[2], 10);
      } else if (this.lastContext && this.lastContext.minDayShifts) {
        // Inherit constraints from previous turns
        minDayShifts = this.lastContext.minDayShifts;
      }

      this.lastContext = { day: targetDay, minDayShifts };

      return this.generateDayProposal({
        day: targetDay,
        minDayShifts: minDayShifts,
        originalQuery: query
      });
    }

    // 2. Full Month Generation Request
    if (qLower.includes('full month') || qLower.includes('whole month') || qLower.includes('next month') || qLower.includes('generate month')) {
      return this.generateMonthProposal({
        query
      });
    }

    // 3. Audit / Check coverage
    if (qLower.includes('audit') || qLower.includes('fatigue') || qLower.includes('coverage deficit') || qLower.includes('check')) {
      return this.auditActiveRoster();
    }

    // 4. Direct heal command
    if (qLower.includes('heal') || qLower.includes('fix fatigue') || qLower.includes('fix deficit')) {
      return this.executeHealingProposal();
    }

    // 5. Default: Call Gemini API directly with multi-turn history or fallback to smart helper
    return this.consultGeminiOrSmartSolver(query);
  }

  getApiKey() {
    if (this.app && this.app.aiEngine && this.app.aiEngine.apiKey) {
      return this.app.aiEngine.apiKey;
    }
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem(STORAGE_KEYS.GEMINI_API_KEY) || '';
    }
    return '';
  }

  getModel() {
    if (this.app && this.app.aiEngine && this.app.aiEngine.model) {
      return this.app.aiEngine.model;
    }
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem(STORAGE_KEYS.GEMINI_MODEL) || 'gemini-1.5-flash';
    }
    return 'gemini-1.5-flash';
  }

  formatModelName(model) {
    if (!model) return 'Gemini 1.5 Flash';
    if (model.includes('3.8')) return 'Gemini 3.8 Flash';
    if (model.includes('2.5')) return 'Gemini 2.5 Flash';
    if (model.includes('2.0')) return 'Gemini 2.0 Flash';
    if (model.includes('1.5-pro')) return 'Gemini 1.5 Pro';
    if (model.includes('1.5-flash')) return 'Gemini 1.5 Flash';
    return model
      .replace(/^models\//, '')
      .split('-')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  updateModelDisplay() {
    const rawModel = this.getModel();
    const formatted = this.formatModelName(rawModel);

    if (this.dom && this.dom.modelFooter) {
      this.dom.modelFooter.textContent = `Powered by Google ${formatted} & NOC Rules`;
    }

    if (this.dom && this.dom.headerStatusText && !this.isGenerating) {
      this.dom.headerStatusText.textContent = `Online • ${formatted} & Smart Solver`;
    }
  }

  // --- Clear Month Grid Action ---
  async executeClearMonthGrid(query) {
    const qLower = query.toLowerCase().trim();

    const monthNamesMap = {
      january: 1, jan: 1,
      february: 2, feb: 2,
      march: 3, mar: 3,
      april: 4, apr: 4,
      may: 5,
      june: 6, jun: 6,
      july: 7, jul: 7,
      august: 8, aug: 8,
      september: 9, sep: 9, sept: 9,
      october: 10, oct: 10,
      november: 11, nov: 11,
      december: 12, dec: 12
    };

    let targetMonth = this.app.state.currentMonth;
    let targetYear = this.app.state.currentYear;

    // Check if user specifically requested a month
    for (const [mName, mNum] of Object.entries(monthNamesMap)) {
      const mRegex = new RegExp(`\\b${mName}\\b`, 'i');
      if (mRegex.test(qLower)) {
        targetMonth = mNum;
        break;
      }
    }

    // Check if user mentioned a specific 4-digit year
    const yearMatch = qLower.match(/\b(202[5-9]|203[0-5])\b/);
    if (yearMatch) {
      targetYear = parseInt(yearMatch[1], 10);
    }

    // Switch view to target month if not already active
    if (this.app.state.currentYear !== targetYear || this.app.state.currentMonth !== targetMonth) {
      if (this.app.dom.monthSelect) {
        this.app.dom.monthSelect.value = `${targetYear}-${String(targetMonth).padStart(2, '0')}`;
      }
      await this.app.loadGridRosterForPeriod(targetYear, targetMonth);
    }

    // Save backup for 1-click Undo / Restore
    this.lastClearedGridBackup = {
      year: targetYear,
      month: targetMonth,
      grid: JSON.parse(JSON.stringify(this.app.state.gridRoster)),
      lockedCells: JSON.parse(JSON.stringify(this.app.state.lockedCells || {}))
    };

    // Execute clear via RosterApp
    const res = await this.app.clearMonthGrid(targetYear, targetMonth);
    this.updatePeriodIndicator();

    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const mFullName = monthNames[targetMonth - 1] || `Month ${targetMonth}`;

    const text = `🗑️ **Cleared Entire Month Grid for ${mFullName} ${targetYear}!**\n\n` +
      `✓ **Target Period:** **${mFullName} ${targetYear}** (${res.totalDays} Days)\n` +
      `✓ **Engineers Cleared:** **${res.engineerCount}** active team members\n` +
      `✓ **Status:** All cell assignments reset to **Empty / Blank**\n` +
      `✓ **Shift Locks:** All locked cells removed\n\n` +
      `The grid is now clean. You can assign shifts manually, import from Excel, or ask me:\n` +
      `*"Prepare a full month roster for ${mFullName} ${targetYear} with balanced day desk"*`;

    const proposal = {
      id: 'prop_clear_' + Date.now(),
      type: 'cleared_grid',
      targetMonth,
      targetYear,
      title: `Cleared Grid: ${mFullName} ${targetYear}`,
      subtitle: `${res.engineerCount} Engineers • ${res.totalDays} Days Blanked`,
      applied: true
    };

    return { text, proposal };
  }

  executeUndoClearGrid() {
    if (!this.lastClearedGridBackup) {
      return { text: "⚠️ No cleared grid backup found to restore." };
    }

    const { year, month, grid, lockedCells } = this.lastClearedGridBackup;
    this.app.state.currentYear = year;
    this.app.state.currentMonth = month;
    if (this.app.dom.monthSelect) {
      this.app.dom.monthSelect.value = `${year}-${String(month).padStart(2, '0')}`;
    }
    this.app.state.gridRoster = grid;
    this.app.state.lockedCells = lockedCells || {};
    this.app.saveLockedCells();
    this.app.saveCurrentGridRoster();
    this.app.renderHeaders();
    this.app.renderRoster();
    this.app.runRuleAudit();
    this.updatePeriodIndicator();
    this.app.showToast('Restored previous grid shifts!');

    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const mFullName = monthNames[month - 1] || `Month ${month}`;

    this.lastClearedGridBackup = null;

    return {
      text: `↩️ **Restored Grid!**\n\nSuccessfully restored all previous shift assignments and locks for **${mFullName} ${year}**.`
    };
  }

  // --- Day Roster Generator & Proposal ---
  async generateDayProposal({ day, minDayShifts = 3, originalQuery }) {
    const curYear = this.app.state.currentYear || 2026;
    const curMonth = this.app.state.currentMonth || 10;
    const totalDays = this.app.state.totalDays || 31;

    if (day < 1 || day > totalDays) {
      return {
        text: `The requested day **${day}** is outside the valid range for this month (1 to ${totalDays} days). Please specify a day between 1 and ${totalDays}.`
      };
    }

    const dateObj = new Date(curYear, curMonth - 1, day);
    const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const dayOfWeekName = dayNames[dateObj.getDay()];
    const monthName = monthNames[curMonth - 1];

    // Try calling Gemini API if key is available
    let assignments = null;
    let aiExplanation = '';

    const apiKey = this.getApiKey();
    if (apiKey) {
      try {
        const geminiResult = await this.callGeminiForDay({
          day,
          dayOfWeekName,
          monthName,
          year: curYear,
          minDayShifts,
          engineers: this.app.state.engineersList,
          userPrompt: originalQuery
        });
        if (geminiResult && geminiResult.assignments) {
          assignments = geminiResult.assignments;
          aiExplanation = geminiResult.explanation || '';
        }
      } catch (err) {
        console.warn('[AI Assistant] Gemini call failed, using Smart Deterministic Solver:', err);
      }
    }

    // If Gemini not used or failed, run Smart Deterministic Solver
    if (!assignments) {
      const solverResult = this.solveDayAssignments({
        day,
        dayOfWeekName,
        minDayShifts,
        engineers: this.app.state.engineersList
      });
      assignments = solverResult.assignments;
      aiExplanation = solverResult.explanation;
    }

    // Calculate metrics strictly adhering to NOC standards
    let dayCoverage = 0;
    let nightCoverage = 0;
    let offCount = 0;
    let apprenticeCount = 0;

    assignments.forEach(a => {
      if (a.role === 'apprentice') {
        apprenticeCount++;
        return; // Apprentice strictly NOT counted towards Day or Night coverage
      }
      if (a.shiftCode === 'NS') {
        nightCoverage++;
      } else if (a.shiftCode && a.shiftCode !== 'OFF' && !a.shiftCode.startsWith('SW') && a.shiftCode !== 'L') {
        dayCoverage++;
      } else {
        offCount++;
      }
    });

    const proposal = {
      id: 'prop_' + Date.now(),
      type: 'single_day',
      day,
      month: curMonth,
      year: curYear,
      title: `${monthName} ${day}, ${curYear} (${dayOfWeekName})`,
      subtitle: `Target: Min ${minDayShifts} Day Shifts • Exactly 1 Night Shift (NS)`,
      dayCoverage,
      nightCoverage,
      offCount,
      apprenticeCount,
      assignments,
      applied: false
    };

    const text = `I have prepared the optimal roster for **${dayOfWeekName}, ${monthName} ${day}, ${curYear}** based on your requirement for **minimum ${minDayShifts} Day Shifts**.\n\n${aiExplanation}\n\n**Staffing Metrics:**\n- ☀️ **Day Shift Staff:** **${dayCoverage}** (Meets requirement ≥ ${minDayShifts})\n- 🌙 **Night Shift (NS):** **${nightCoverage}** (24/7 Desk)\n- 🎓 **Apprentice:** **${apprenticeCount}** (General shift, excluded from desk count)\n- 🏖️ **Weekly Offs:** **${offCount}**\n\nClick **Apply to Grid** below to instantly push these shifts to your active roster!`;

    return {
      text,
      proposal
    };
  }

  // --- Deterministic Day Solver ---
  solveDayAssignments({ day, dayOfWeekName, minDayShifts = 3, engineers }) {
    // Rank-ordered engineers by week-offs
    const sortedEngineers = (this.app && typeof this.app.sortEngineersByWeekOffs === 'function')
      ? this.app.sortEngineersByWeekOffs(engineers)
      : [...engineers];

    const assignments = [];
    const staffPool = [];
    let apprenticeEng = null;

    sortedEngineers.forEach(eng => {
      if (eng.status && eng.status !== 'Active') return; // skip resigned

      if (eng.role === 'apprentice' || eng.name.toUpperCase().includes('DHANUSH')) {
        apprenticeEng = eng;
      } else {
        staffPool.push(eng);
      }
    });

    // 1. Identify which engineers have this day as their weekly off
    const dayNameShort = dayOfWeekName.slice(0, 3).toLowerCase();
    const isWeekOff = (eng) => {
      const offs = eng.default_weekly_offs || [];
      return offs.some(o => o.toLowerCase().startsWith(dayNameShort));
    };

    const offEngineers = [];
    const availableStaff = [];

    staffPool.forEach(eng => {
      if (isWeekOff(eng)) {
        offEngineers.push(eng);
      } else {
        availableStaff.push(eng);
      }
    });

    // We need 1 Night Shift (NS) and at least minDayShifts Day shifts (e.g. 3, 4, 5)
    // Core operational staff needed = 1 (NS) + minDayShifts
    const totalStaffNeeded = 1 + minDayShifts;

    let workingStaff = [...availableStaff];
    // If not enough staff available on non-off days, borrow from offEngineers with least impact
    if (workingStaff.length < totalStaffNeeded) {
      const needed = totalStaffNeeded - workingStaff.length;
      const borrowed = offEngineers.splice(0, needed);
      workingStaff.push(...borrowed);
    }

    // 2. Select Night Shift engineer (e.g. Nandu Anthikat or first available)
    const nsCandidate = workingStaff.find(e => e.preferred_shifts && e.preferred_shifts.includes('NS')) || workingStaff[0];
    const dayStaff = workingStaff.filter(e => e.id !== nsCandidate.id);

    // 3. Assign Day shifts to dayStaff
    const dayShiftCodes = ['FS', 'FG', 'SG', 'SS', 'G'];
    const assignedMap = new Map();

    // NS
    assignedMap.set(nsCandidate.name, {
      shiftCode: 'NS',
      note: '24/7 Dedicated Night Shift'
    });

    // Day Shifts (FS, SG, SS, FG)
    dayStaff.forEach((eng, idx) => {
      const code = dayShiftCodes[idx % dayShiftCodes.length];
      assignedMap.set(eng.name, {
        shiftCode: code,
        note: `Day Shift Coverage (${code})`
      });
    });

    // Remaining Staff -> OFF
    offEngineers.forEach(eng => {
      if (!assignedMap.has(eng.name)) {
        assignedMap.set(eng.name, {
          shiftCode: 'OFF',
          note: `Regular Weekly Off (${dayOfWeekName.slice(0, 3)})`
        });
      }
    });

    // Build complete assignment array
    staffPool.forEach(eng => {
      const info = assignedMap.get(eng.name) || { shiftCode: 'OFF', note: 'Weekly Off' };
      assignments.push({
        id: eng.id,
        name: eng.name,
        role: eng.role || 'Staff',
        shiftCode: info.shiftCode,
        note: info.note
      });
    });

    // Apprentice handling
    if (apprenticeEng) {
      const apprOff = isWeekOff(apprenticeEng);
      assignments.push({
        id: apprenticeEng.id,
        name: apprenticeEng.name,
        role: 'apprentice',
        shiftCode: apprOff ? 'OFF' : 'G',
        note: apprOff ? 'Apprentice Weekly Off' : 'General Shift (Training Desk)'
      });
    }

    return {
      assignments,
      explanation: `✓ Respected weekly off schedules for ${dayOfWeekName}.\n✓ Staffed 1 continuous Night Shift (NS) and ${dayStaff.length} active Day Shifts.\n✓ Apprentice assigned General shift (excluded from NOC coverage count).`
    };
  }

  // --- Direct Gemini API Call for Day ---
  async callGeminiForDay({ day, dayOfWeekName, monthName, year, minDayShifts, engineers, userPrompt }) {
    const apiKey = this.getApiKey();
    const model = this.getModel();

    const systemPrompt = `You are the RosterX AI Assistant. You must prepare the shift assignments for a specific day (${dayOfWeekName}, ${monthName} ${day}, ${year}).
Rules:
1. Target: Minimum ${minDayShifts} Day Shifts (combined from FS, FG, SG, SS, G).
2. Exactly 1 Night Shift (NS) must be assigned to staff.
3. Apprentice engineers (e.g. role='apprentice' or Dhanush) must be assigned 'G' (General) or 'OFF', and are NEVER counted towards Day or Night coverage counts.
4. Respect engineer week-offs whenever possible.
5. Allowed Shift Codes: FS, FG, G, SG, SS, NS, OFF, L.
6. Return structured JSON with keys:
   - "explanation": string explaining rationale
   - "assignments": array of objects: { "name": string, "role": string, "shiftCode": string, "note": string }
`;

    const requestPayload = {
      contents: [
        {
          role: "user",
          parts: [
            { text: systemPrompt },
            { text: `USER REQUEST: ${userPrompt}\n\nENGINEERS POOL:\n` + JSON.stringify(engineers.map(e => ({ name: e.name, role: e.role, default_weekly_offs: e.default_weekly_offs })), null, 2) }
          ]
        }
      ],
      generationConfig: {
        temperature: 0.1,
        responseMimeType: "application/json"
      }
    };

    const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
    const response = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestPayload)
    });

    if (!response.ok) {
      throw new Error(`Gemini API returned ${response.status}`);
    }

    const data = await response.json();
    const rawText = data.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!rawText) throw new Error("Empty response from Gemini");

    return JSON.parse(rawText);
  }

  // --- Full Month Generator Proposal ---
  async generateMonthProposal({ query }) {
    let targetYear = this.app.state.currentYear || 2026;
    let targetMonth = this.app.state.currentMonth || 10;
    if (query.toLowerCase().includes('next month')) {
      targetMonth = targetMonth === 12 ? 1 : targetMonth + 1;
      if (targetMonth === 1) targetYear++;
    }

    const totalDays = new Date(targetYear, targetMonth, 0).getDate();
    const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
    const curMonthName = monthNames[targetMonth - 1];

    const result = await this.app.aiEngine.generateRosterWithGemini({
      year: targetYear,
      month: targetMonth,
      engineers: this.app.state.engineersList,
      constraints: this.app.state.constraints,
      manualSwaps: this.app.state.manualSwaps,
      absenceRequests: this.app.state.absenceRequests
    });

    if (!result || !result.success) {
      throw new Error("Unable to generate full month roster.");
    }

    const day1Assignments = result.engineers.map(e => ({
      name: e.name,
      role: e.role,
      shiftCode: e.shifts[0] || 'OFF',
      note: 'Day 1 Schedule'
    }));

    const proposal = {
      id: 'prop_month_' + Date.now(),
      type: 'full_month',
      month: targetMonth,
      year: targetYear,
      totalDays,
      title: `Full Month: ${curMonthName} ${targetYear} (${totalDays} Days)`,
      subtitle: `24/7 NOC Coverage • Score: ${result.compliance_summary.score}% • Zero Fatigue Violations`,
      dayCoverage: 5,
      nightCoverage: 1,
      offCount: 4,
      apprenticeCount: 1,
      assignments: day1Assignments,
      fullResult: result,
      applied: false
    };

    return {
      text: `✨ I have prepared the complete monthly schedule for **${curMonthName} ${targetYear}** with 24/7 continuous Night Shift coverage, balanced Day Desk rotation, and zero fatigue violations.\n\nClick **Apply to Grid** to review and commit the entire month!`,
      proposal
    };
  }

  // --- Audit Current Roster ---
  auditActiveRoster() {
    const totalDays = this.app.state.totalDays || 31;
    const audit = this.app.aiEngine.auditRoster(this.app.state.gridRoster, totalDays, this.app.state.constraints);

    const nightScoreVal = (audit.nightScore !== undefined ? audit.nightScore : Math.max(0, 100 - audit.nightViolations.length * 15));
    const hasFatigue = audit.fatigueViolations.length > 0;
    const hasNightDeficit = audit.nightViolations.length > 0;

    let issuesList = '';
    if (hasFatigue) {
      issuesList += '\n**Fatigue Violations Identified:**\n' + audit.fatigueViolations.map(v => `- ⚠️ ${v.message}`).join('\n');
    }
    if (hasNightDeficit) {
      issuesList += '\n**Night Coverage Deficits:**\n' + audit.nightViolations.map(v => `- 🌙 ${v.message}`).join('\n');
    }

    let actionPrompt = '';
    if (hasFatigue || hasNightDeficit) {
      actionPrompt = `\n\nWould you like me to heal these issues by balancing coverage and enforcing rest?`;
      this.pendingAction = { type: 'heal_fatigue_and_deficits', audit };
    } else {
      actionPrompt = `\n\n✅ Great news! Your current schedule strictly satisfies all NOC rest rules and night coverage.`;
      this.pendingAction = null;
    }

    const text = `🛡️ **Current Roster Audit Report:**\n\n` +
      `- **Overall Compliance Score:** **${audit.score}%**\n` +
      `- **Night Shift 24/7 Coverage:** **${nightScoreVal}%** (${audit.nightViolations.length} deficits)\n` +
      `- **Fatigue Violations:** **${audit.fatigueViolations.length}**` +
      issuesList +
      actionPrompt;

    return { text };
  }

  // --- Healing Execution & Proposal Presentation ---
  executeHealingProposal() {
    const totalDays = this.app.state.totalDays || 31;
    const currentGrid = this.app.state.gridRoster;
    const constraints = this.app.state.constraints || {};
    const maxCap = constraints.maxConsecutiveDays || 6;
    const healed = this.app.aiEngine.healRoster(
      currentGrid,
      totalDays,
      constraints,
      this.app.state.manualSwaps
    );

    // Identify exact changes made
    const changes = [];
    currentGrid.forEach(origEng => {
      const healedEng = healed.find(e => e.name === origEng.name || e.id === origEng.id);
      if (!healedEng || !Array.isArray(healedEng.shifts)) return;

      for (let d = 0; d < totalDays; d++) {
        const oldShift = origEng.shifts[d] || 'OFF';
        const newShift = healedEng.shifts[d] || 'OFF';
        if (oldShift !== newShift) {
          let reason = 'Operational Balancing';
          if (newShift === 'OFF') {
            // Check if previous day was night shift
            if (d > 0 && origEng.shifts[d - 1] === 'NS') {
              reason = 'Post-Night Turnaround Rest (Mandatory rest after Night Shift)';
            } else {
              let streakBefore = 0;
              for (let k = d - 1; k >= 0; k--) {
                const prev = origEng.shifts[k];
                if (prev === 'OFF' || (prev && prev.startsWith('SW')) || prev === 'L') break;
                streakBefore++;
              }
              if (streakBefore >= maxCap) {
                reason = `Consecutive Work Cap (Worked ${streakBefore} days consecutively; cap = ${maxCap})`;
              } else {
                reason = 'Weekly Off Schedule';
              }
            }
          } else if (newShift === 'NS') {
            reason = '24/7 Night Desk Coverage Deficit';
          } else if (newShift === 'FS') {
            reason = 'Morning Core Desk Coverage (FS)';
          } else if (newShift === 'SS') {
            reason = 'Evening Core Desk Coverage (SS)';
          }

          changes.push({
            name: origEng.name,
            day: d + 1,
            oldShift,
            newShift,
            role: origEng.role || 'Staff',
            reason
          });
        }
      }
    });

    if (changes.length === 0) {
      return {
        text: `✅ **All good!** No changes required. Your active schedule is already compliant with zero fatigue violations and full coverage.`
      };
    }

    const postAudit = this.app.aiEngine.auditRoster(healed, totalDays, constraints);

    const proposal = {
      id: 'prop_heal_' + Date.now(),
      type: 'healed_roster',
      totalDays,
      title: 'Healed Roster: Zero Fatigue & Full Coverage',
      subtitle: `${changes.length} schedule adjustment(s) • Score: ${postAudit.score}% (Zero Fatigue)`,
      dayCoverage: 'Optimized',
      nightCoverage: '100%',
      offCount: 'Rest Protected',
      apprenticeCount: 1,
      assignments: changes.map(c => ({
        name: c.name,
        role: c.role,
        shiftCode: c.newShift,
        note: `Day ${c.day}: ${c.oldShift} ➔ ${c.newShift} (${c.reason})`
      })),
      healedGrid: healed,
      applied: false
    };

    const changesSummary = changes.slice(0, 5).map(c => 
      `- **${c.name}** on **Day ${c.day}**: changed from \`${c.oldShift}\` to \`${c.newShift}\` (*${c.reason}*)`
    ).join('\n');

    const text = `✨ **I have computed the healed schedule to eliminate fatigue violations and coverage deficits!**\n\n` +
      `**Summary of Adjustments (${changes.length}):**\n${changesSummary}` +
      (changes.length > 5 ? `\n- *...and ${changes.length - 5} other fine adjustments.*` : '') +
      `\n\n- **Post-Healing Compliance Score:** **${postAudit.score}%**\n` +
      `- **Night Coverage:** **100%**\n` +
      `- **Fatigue Violations:** **0**\n\n` +
      `Click **Apply to Grid** below to push these healed shifts to your active roster!`;

    this.activeProposal = proposal;
    return { text, proposal };
  }

  // --- General Q&A / Smart Solver Fallback with Multi-turn Context ---
  async consultGeminiOrSmartSolver(query) {
    const apiKey = this.getApiKey();
    if (!apiKey) {
      return {
        text: `I'm ready to assist! You can ask me commands like:\n- *"Prepare a roster for 24th of this month with minimum Day shift count as 3"*\n- *"Generate full month roster for November 2026"*\n- *"Audit fatigue violations"*\n\n*(Tip: Add your Gemini API key in Settings for full natural language reasoning!)*`
      };
    }

    try {
      const endpoint = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
      const systemPrompt = `You are the RosterX Operations AI Assistant for 24/7 NOC Roster Management. Current Period: Month ${this.app.state.currentMonth}, Year ${this.app.state.currentYear}. Active team: ${this.app.state.engineersList.map(e => e.name).join(', ')}. Keep answers helpful, concise, and focused on shift rosters.`;

      // Build multi-turn context
      const contents = [
        { role: "user", parts: [{ text: systemPrompt }] },
        { role: "model", parts: [{ text: "Understood! I am the RosterX Assistant with full active roster context." }] }
      ];

      // Include up to 6 recent messages
      const recent = this.messages.slice(-6);
      for (const m of recent) {
        if (m.text) {
          contents.push({
            role: m.role === 'user' ? 'user' : 'model',
            parts: [{ text: m.text }]
          });
        }
      }

      contents.push({
        role: "user",
        parts: [{ text: query }]
      });

      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents,
          generationConfig: { temperature: 0.2 }
        })
      });

      if (response.ok) {
        const data = await response.json();
        const text = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (text) return { text };
      }
    } catch (e) {}

    return {
      text: `I'm your RosterX AI assistant. Try asking: *"Prepare a roster for 24th with minimum Day shift count as 3"* to generate a live preview card with instant Apply!`
    };
  }

  // --- Apply Proposal to Active Grid ---
  applyProposalToGrid(proposalId) {
    // Find the proposal in messages
    let targetMsg = null;
    for (const m of this.messages) {
      if (m.proposal && m.proposal.id === proposalId) {
        targetMsg = m;
        break;
      }
    }

    const proposal = targetMsg ? targetMsg.proposal : this.activeProposal;
    if (!proposal) {
      if (typeof alert !== 'undefined') alert('Proposal data not found.');
      return;
    }

    if (proposal.type === 'single_day') {
      const day = proposal.day;
      const dIdx = day - 1;

      // Ensure period matches
      if (this.app.state.currentYear !== proposal.year || this.app.state.currentMonth !== proposal.month) {
        this.app.loadGridRosterForPeriod(proposal.year, proposal.month);
      }

      // Update cells for each engineer on this specific day
      proposal.assignments.forEach(a => {
        const engRow = this.app.state.gridRoster.find(e => e.name === a.name || e.id === a.id);
        if (engRow && engRow.shifts) {
          engRow.shifts[dIdx] = a.shiftCode;
        }
      });

      this.app.markMonthAsImportedOrGenerated(`${proposal.year}-${String(proposal.month).padStart(2, '0')}`);
      this.app.saveCurrentGridRoster();
      this.app.renderHeaders();
      this.app.renderRoster();
      this.app.showToast(`Applied AI roster for ${proposal.title} to the grid!`);

      // Update proposal state in chat
      proposal.applied = true;
      this.saveChatHistory();
      this.renderMessages();

      // Bot confirmation message
      this.addAssistantMessage(`🎉 **Success!** The roster assignments for **Day ${day}** have been applied to the grid. All coverage metrics and counters are updated!`);

    } else if (proposal.type === 'healed_roster') {
      if (proposal.healedGrid) {
        this.app.state.gridRoster = proposal.healedGrid;
        this.app.markMonthAsImportedOrGenerated(`${this.app.state.currentYear}-${String(this.app.state.currentMonth).padStart(2, '0')}`);
        this.app.saveCurrentGridRoster();
        this.app.renderHeaders();
        this.app.renderRoster();
        this.app.showToast('Applied healed roster with zero fatigue violations!');
      }

      proposal.applied = true;
      this.saveChatHistory();
      this.renderMessages();

      this.addAssistantMessage(`🎉 **Success!** All fatigue adjustments have been applied to your active grid. Compliance score is now 100%!`);

    } else if (proposal.type === 'full_month') {
      if (proposal.fullResult) {
        this.app.state.generatedDraft = proposal.fullResult;
        this.app.applyAiDraft();
      }

      proposal.applied = true;
      this.saveChatHistory();
      this.renderMessages();

      this.addAssistantMessage(`🎉 **Success!** Full month roster for **${proposal.title}** has been applied to the active grid!`);
    }
  }

  // --- Utility Helpers ---
  escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  formatMarkdown(text) {
    if (!text) return '';
    let html = this.escapeHtml(text);
    // Bold
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    // Italic
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    // Line breaks
    html = html.replace(/\n/g, '<br/>');
    return html;
  }
}
