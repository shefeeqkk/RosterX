/**
 * RosterX — Official Theme & 6-Palette Brand Controller (themeController.js)
 *
 * 1:1 parity with Android NeumorphicTokens.kt:
 * - 3-Way Mode: system (default) | light | dark
 * - 6 Dual-State Brand Palettes: tory_blue | crimson_coral | emerald_green |
 *   indigo_violet | sunset_amber | ocean_cyan
 * - Keeps <meta name="color-scheme"> and <meta name="theme-color"> synchronized
 * - Listens to cross-tab storage events and system prefers-color-scheme changes
 */

const THEME_STORAGE_KEY = 'rosterx_theme';
const PALETTE_STORAGE_KEY = 'rosterx_palette';

export const BRAND_PALETTES = [
  { id: 'tory_blue',     name: 'Tory Blue',     lightHex: '#174EAC', darkHex: '#7294CC' },
  { id: 'crimson_coral', name: 'Crimson Coral', lightHex: '#FF5252', darkHex: '#FB7185' },
  { id: 'emerald_green', name: 'Emerald Green', lightHex: '#059669', darkHex: '#34D399' },
  { id: 'indigo_violet', name: 'Indigo Violet', lightHex: '#6366F1', darkHex: '#818CF8' },
  { id: 'sunset_amber',  name: 'Sunset Amber',  lightHex: '#D97706', darkHex: '#FBBF24' },
  { id: 'ocean_cyan',    name: 'Ocean Cyan',    lightHex: '#0284C7', darkHex: '#38BDF8' }
];

export class ThemeController {
  constructor() {
    this.currentTheme = this.getStoredTheme();
    this.currentPalette = this.getStoredPalette();
    this.mediaQuery = (typeof window !== 'undefined' && window.matchMedia)
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : null;
  }

  getStoredTheme() {
    try {
      if (typeof localStorage !== 'undefined') {
        const stored = localStorage.getItem(THEME_STORAGE_KEY);
        if (stored === 'light' || stored === 'dark' || stored === 'system') {
          return stored;
        }
      }
    } catch (_) {}
    return 'system';
  }

  getStoredPalette() {
    try {
      if (typeof localStorage !== 'undefined') {
        const stored = localStorage.getItem(PALETTE_STORAGE_KEY);
        if (BRAND_PALETTES.some((p) => p.id === stored)) {
          return stored;
        }
      }
    } catch (_) {}
    return 'tory_blue';
  }

  isDarkEffective() {
    if (this.currentTheme === 'dark') return true;
    if (this.currentTheme === 'light') return false;
    return this.mediaQuery ? this.mediaQuery.matches : false;
  }

  getActivePaletteObj() {
    return BRAND_PALETTES.find((p) => p.id === this.currentPalette) || BRAND_PALETTES[0];
  }

  applyPalette(paletteId = this.currentPalette) {
    const valid = BRAND_PALETTES.find((p) => p.id === paletteId) || BRAND_PALETTES[0];
    this.currentPalette = valid.id;
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(PALETTE_STORAGE_KEY, valid.id);
      }
    } catch (_) {}

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-palette', valid.id);
    }
    this.updateMetaTags();
    this.updatePaletteUi();
    window.dispatchEvent(new CustomEvent('rosterx:themechange', {
      detail: { theme: this.currentTheme, palette: this.currentPalette, isDark: this.isDarkEffective() }
    }));
  }

  applyTheme(theme = this.currentTheme) {
    if (theme !== 'light' && theme !== 'dark' && theme !== 'system') {
      theme = 'system';
    }
    this.currentTheme = theme;
    try {
      if (typeof localStorage !== 'undefined') {
        if (theme === 'system') {
          localStorage.removeItem(THEME_STORAGE_KEY);
        } else {
          localStorage.setItem(THEME_STORAGE_KEY, theme);
        }
      }
    } catch (_) {}

    const htmlEl = (typeof document !== 'undefined') ? document.documentElement : null;
    if (htmlEl) {
      htmlEl.setAttribute('data-theme', theme);
      htmlEl.setAttribute('data-palette', this.currentPalette);
      if (this.isDarkEffective()) {
        htmlEl.classList.add('dark');
      } else {
        htmlEl.classList.remove('dark');
      }
    }

    this.updateMetaTags();
    this.updateToggleUi();
    this.updatePaletteUi();
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('rosterx:themechange', {
        detail: { theme: this.currentTheme, palette: this.currentPalette, isDark: this.isDarkEffective() }
      }));
    }
  }

  updateMetaTags() {
    if (typeof document === 'undefined') return;
    const isDark = this.isDarkEffective();

    // Sync <meta name="color-scheme">
    let csMeta = document.querySelector('meta[name="color-scheme"]');
    if (!csMeta) {
      csMeta = document.createElement('meta');
      csMeta.name = 'color-scheme';
      document.head.appendChild(csMeta);
    }
    csMeta.content = this.currentTheme === 'system' ? 'light dark' : this.currentTheme;

    // Sync <meta name="theme-color"> for iOS status bar / Safari toolbar chrome
    let tcMeta = document.querySelector('meta[name="theme-color"]');
    if (!tcMeta) {
      tcMeta = document.createElement('meta');
      tcMeta.name = 'theme-color';
      document.head.appendChild(tcMeta);
    }
    tcMeta.content = isDark ? '#1E293B' : '#EEF2F6';
  }

  cycleTheme() {
    let nextTheme = 'light';
    if (this.currentTheme === 'system') {
      nextTheme = 'light';
    } else if (this.currentTheme === 'light') {
      nextTheme = 'dark';
    } else {
      nextTheme = 'system';
    }
    this.applyTheme(nextTheme);
    return nextTheme;
  }

  setTheme(theme) {
    if (theme !== 'light' && theme !== 'dark' && theme !== 'system') return;
    this.applyTheme(theme);
  }

  setPalette(paletteId) {
    this.applyPalette(paletteId);
  }

  updateToggleUi() {
    const isDark = this.isDarkEffective();
    const toggleButtons = document.querySelectorAll('.theme-toggle-btn, #btnThemeToggle, #portalThemeToggle, #loginThemeToggle');
    toggleButtons.forEach((btn) => {
      const iconEl = btn.querySelector('.theme-toggle-icon') || btn.querySelector('i');
      const labelEl = btn.querySelector('.theme-toggle-label');
      let iconClass = 'ph ph-desktop';
      let labelText = 'System';
      if (this.currentTheme === 'light') {
        iconClass = 'ph ph-sun text-amber-500';
        labelText = 'Light';
      } else if (this.currentTheme === 'dark') {
        iconClass = 'ph ph-moon text-indigo-400';
        labelText = 'Dark';
      } else {
        iconClass = isDark ? 'ph ph-desktop text-indigo-300' : 'ph ph-desktop text-slate-500';
        labelText = 'System';
      }
      if (iconEl) iconEl.className = iconClass + ' text-base';
      if (labelEl) labelEl.textContent = labelText;
      btn.title = `Theme: ${labelText} (Click to switch)`;
    });

    // Update 3-state mode chips inside Profile Appearance settings
    document.querySelectorAll('[data-rx-theme-choice]').forEach((el) => {
      const active = el.getAttribute('data-rx-theme-choice') === this.currentTheme;
      el.classList.toggle('is-active', active);
      el.setAttribute('aria-pressed', active ? 'true' : 'false');
    });
  }

  updatePaletteUi() {
    document.querySelectorAll('[data-rx-palette-choice]').forEach((el) => {
      const active = el.getAttribute('data-rx-palette-choice') === this.currentPalette;
      el.classList.toggle('is-active', active);
      el.setAttribute('aria-pressed', active ? 'true' : 'false');
    });
    const activePal = this.getActivePaletteObj();
    const modeLabel = this.currentTheme === 'system' ? 'System' : this.currentTheme === 'dark' ? 'Dark' : 'Light';
    document.querySelectorAll('[data-rx-appearance-summary]').forEach((el) => {
      el.textContent = `${modeLabel} • ${activePal.name}`;
    });
  }

  /**
   * Renders a reusable Appearance & Brand Theme selector (3-state mode + 6 palettes)
   */
  renderAppearancePickerHtml() {
    const isDark = this.isDarkEffective();
    const sysState = this.mediaQuery?.matches ? 'Dark' : 'Light';
    const modes = [
      { id: 'system', label: `System (${sysState})`, icon: 'ph-desktop' },
      { id: 'light',  label: 'Light', icon: 'ph-sun' },
      { id: 'dark',   label: 'Dark', icon: 'ph-moon' }
    ];
    return `
      <div class="rx-appearance-panel">
        <div class="rx-field-label">THEME MODE</div>
        <div class="rx-mode-row">
          ${modes.map((m) => `
            <button type="button" class="rx-chip-btn ${this.currentTheme === m.id ? 'is-active' : ''}" data-rx-theme-choice="${m.id}" aria-pressed="${this.currentTheme === m.id}">
              <i class="ph ${m.icon}" aria-hidden="true"></i>
              <span>${m.label}</span>
            </button>
          `).join('')}
        </div>
        <div class="rx-field-label" style="margin-top:12px;">BRAND COLOR PALETTE</div>
        <div class="rx-palette-grid">
          ${BRAND_PALETTES.map((p) => {
            const swatch = isDark ? p.darkHex : p.lightHex;
            const active = this.currentPalette === p.id;
            return `
              <button type="button" class="rx-palette-btn ${active ? 'is-active' : ''}" data-rx-palette-choice="${p.id}" aria-pressed="${active}">
                <span class="rx-palette-dot" style="background:${swatch}"></span>
                <span>${p.name}</span>
              </button>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  bindAppearancePicker(container) {
    if (!container) return;
    container.querySelectorAll('[data-rx-theme-choice]').forEach((btn) => {
      btn.addEventListener('click', () => {
        this.setTheme(btn.getAttribute('data-rx-theme-choice'));
      });
    });
    container.querySelectorAll('[data-rx-palette-choice]').forEach((btn) => {
      btn.addEventListener('click', () => {
        this.setPalette(btn.getAttribute('data-rx-palette-choice'));
      });
    });
  }

  init() {
    this.applyPalette(this.currentPalette);
    this.applyTheme(this.currentTheme);

    if (this.mediaQuery) {
      const onSystemChange = () => {
        if (this.currentTheme === 'system') {
          this.applyTheme('system');
        }
      };
      try {
        this.mediaQuery.addEventListener('change', onSystemChange);
      } catch (_) {
        try { this.mediaQuery.addListener(onSystemChange); } catch (__) {}
      }
    }

    // Cross-tab synchronization
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', (e) => {
        if (e.key === THEME_STORAGE_KEY) {
          this.currentTheme = this.getStoredTheme();
          this.applyTheme(this.currentTheme);
        } else if (e.key === PALETTE_STORAGE_KEY) {
          this.currentPalette = this.getStoredPalette();
          this.applyPalette(this.currentPalette);
        }
      });
    }

    const bindButtons = () => {
      const toggleButtons = document.querySelectorAll('.theme-toggle-btn, #btnThemeToggle, #portalThemeToggle, #loginThemeToggle');
      toggleButtons.forEach((btn) => {
        if (!btn.dataset.themeBound) {
          btn.dataset.themeBound = 'true';
          btn.addEventListener('click', (e) => {
            e.preventDefault();
            this.cycleTheme();
          });
        }
      });
      this.updateToggleUi();
      this.updatePaletteUi();
    };

    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', bindButtons);
    } else {
      bindButtons();
    }
  }
}

export const themeController = new ThemeController();
themeController.init();

if (typeof window !== 'undefined') {
  window.themeController = themeController;
  window.BRAND_PALETTES = BRAND_PALETTES;
}
