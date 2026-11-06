/**
 * RosterX — Official Theme Controller (themeController.js)
 * 
 * Manages 3-Way Mode (Light, Dark, System) with:
 * - Instant zero-flicker execution before DOM paint
 * - Dynamic system media query listeners
 * - Synchronization with Neumorphic CSS variables (tokens.css)
 * - Accessible UI toggles and persistent localStorage caching
 */

const THEME_STORAGE_KEY = 'rosterx_theme';

export class ThemeController {
  constructor() {
    this.currentTheme = this.getStoredTheme();
    this.mediaQuery = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
  }

  getStoredTheme() {
    try {
      const stored = localStorage.getItem(THEME_STORAGE_KEY);
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        return stored;
      }
    } catch (e) {}
    return 'system';
  }

  isDarkEffective() {
    if (this.currentTheme === 'dark') return true;
    if (this.currentTheme === 'light') return false;
    return this.mediaQuery ? this.mediaQuery.matches : false;
  }

  applyTheme(theme = this.currentTheme) {
    this.currentTheme = theme;
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme);
    } catch (e) {}

    const htmlEl = document.documentElement;
    htmlEl.setAttribute('data-theme', theme);

    if (this.isDarkEffective()) {
      htmlEl.classList.add('dark');
    } else {
      htmlEl.classList.remove('dark');
    }

    this.updateToggleUi();
  }

  cycleTheme() {
    let nextTheme = 'light';
    if (this.currentTheme === 'system') {
      nextTheme = 'light';
    } else if (this.currentTheme === 'light') {
      nextTheme = 'dark';
    } else if (this.currentTheme === 'dark') {
      nextTheme = 'system';
    }
    this.applyTheme(nextTheme);
    return nextTheme;
  }

  setTheme(theme) {
    if (theme !== 'light' && theme !== 'dark' && theme !== 'system') return;
    this.applyTheme(theme);
  }

  updateToggleUi() {
    const isDark = this.isDarkEffective();
    const toggleButtons = document.querySelectorAll('.theme-toggle-btn, #btnThemeToggle, #portalThemeToggle, #loginThemeToggle');
    
    toggleButtons.forEach(btn => {
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
        labelText = 'Auto';
      }

      if (iconEl) {
        iconEl.className = iconClass + ' text-base';
      }
      if (labelEl) {
        labelEl.textContent = labelText;
      }
      btn.title = `Current Theme: ${labelText} (Click to switch)`;
    });
  }

  init() {
    // Apply current theme
    this.applyTheme(this.currentTheme);

    // Bind media query listener
    if (this.mediaQuery) {
      try {
        this.mediaQuery.addEventListener('change', () => {
          if (this.currentTheme === 'system') {
            this.applyTheme('system');
          }
        });
      } catch (e) {
        if (this.mediaQuery.addListener) {
          this.mediaQuery.addListener(() => {
            if (this.currentTheme === 'system') {
              this.applyTheme('system');
            }
          });
        }
      }
    }

    // Bind click events on all theme toggles
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('.theme-toggle-btn, #btnThemeToggle, #portalThemeToggle, #loginThemeToggle');
      if (btn) {
        e.preventDefault();
        this.cycleTheme();
      }
    });

    this.updateToggleUi();
  }
}

// Global Singleton
export const themeController = new ThemeController();
if (typeof window !== 'undefined') {
  window.themeController = themeController;
}
