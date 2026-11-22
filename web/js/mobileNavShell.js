/**
 * RosterX — Adaptive Mobile/iOS Neumorphic Navigation Shell (mobileNavShell.js)
 *
 * Mirrors Android v1.36.0 1:1:
 * - Mobile Portrait (< 768px): 5-Tab Fluid Neumorphic Bottom Dock
 * - Mobile Landscape (max-height: 540px): Left Vertical FluidNeumorphicNavRail
 * - Full-Screen Landscape Month Roster (body.rx-fullscreen-roster)
 * - Deep-Link Hash Router (#roster, #notifications, #credentials, #profile, #dashboard)
 */

import { escapeHtml } from './pwaHelper.js';

export class MobileNavShell {
  /**
   * @param {Object} opts
   * @param {Array<{id:string, label:string, icon:string}>} opts.tabs - Exact 5 Android tabs
   * @param {string} opts.rosterTabId - Tab ID that uses full-screen landscape (e.g. 'month_roster' or 'matrix')
   * @param {(tabId:string) => void} opts.onTabSelected
   */
  constructor({ tabs, rosterTabId, onTabSelected }) {
    this.tabs = tabs || [];
    this.rosterTabId = rosterTabId;
    this.onTabSelected = onTabSelected;
    this.activeTab = this.tabs[0]?.id || '';
    this.bottomNavEl = null;
    this.railEl = null;
    this.sliderEl = null;
  }

  mount() {
    document.getElementById('rxMobileBottomNav')?.remove();
    document.getElementById('rxLandscapeNavRail')?.remove();

    // 1. Mobile Portrait Bottom Dock
    const bottomNav = document.createElement('nav');
    bottomNav.id = 'rxMobileBottomNav';
    bottomNav.className = 'rx-mobile-bottom-nav no-print';
    bottomNav.setAttribute('aria-label', 'Primary navigation');
    bottomNav.innerHTML = this.tabs.map((tab) => `
        <button type="button" class="rx-nav-tab" data-rx-tab="${escapeHtml(tab.id)}" aria-label="${escapeHtml(tab.label)}">
          <span class="rx-nav-tab-bubble"><i class="ph ${escapeHtml(tab.icon)}" aria-hidden="true"></i></span>
          <span class="rx-nav-tab-label">${escapeHtml(tab.label)}</span>
        </button>
      `).join('');

    // 2. Mobile Landscape Left Vertical Rail
    const rail = document.createElement('nav');
    rail.id = 'rxLandscapeNavRail';
    rail.className = 'rx-landscape-nav-rail no-print';
    rail.setAttribute('aria-label', 'Landscape navigation');
    rail.innerHTML = this.tabs.map((tab) => `
      <button type="button" class="rx-rail-tab" data-rx-tab="${escapeHtml(tab.id)}" aria-label="${escapeHtml(tab.label)}">
        <span class="rx-rail-bubble"><i class="ph ${escapeHtml(tab.icon)}" aria-hidden="true"></i></span>
        <span class="rx-rail-label">${escapeHtml(tab.label)}</span>
      </button>
    `).join('');

    const bindClicks = (root) => {
      root.querySelectorAll('[data-rx-tab]').forEach((btn) => {
        btn.addEventListener('click', () => {
          const targetId = btn.getAttribute('data-rx-tab');
          if (targetId) {
            this.setActive(targetId);
            this.onTabSelected?.(targetId);
          }
        });
      });
    };

    bindClicks(bottomNav);
    bindClicks(rail);

    document.body.appendChild(bottomNav);
    document.body.appendChild(rail);
    this.bottomNavEl = bottomNav;
    this.railEl = rail;
    this.sliderEl = null;

    this.setActive(this.activeTab);
  }

  setActive(tabId) {
    this.activeTab = tabId;
    [this.bottomNavEl, this.railEl].forEach((root) => {
      if (!root) return;
      root.querySelectorAll('[data-rx-tab]').forEach((btn) => {
        const active = btn.getAttribute('data-rx-tab') === tabId;
        btn.classList.toggle('is-active', active);
        btn.setAttribute('aria-current', active ? 'page' : 'false');
      });
    });

    const index = this.tabs.findIndex((t) => t.id === tabId);
    if (index !== -1 && this.sliderEl) {
      this.sliderEl.style.transition = 'transform 0.28s cubic-bezier(0.2, 0.9, 0.3, 1)';
      this.sliderEl.style.transform = `translateX(${index * 100}%)`;
    }
    if (this.bottomNavEl && this.bottomNavEl.style.transform) {
      this.bottomNavEl.style.transform = '';
    }

    this.syncFullscreenRoster();
  }

  setDragProgress(fractionalIndex, boundaryOffsetPx = 0, isSpring = false) {
    if (!this.bottomNavEl) return;

    if (this.sliderEl) {
      if (isSpring) {
        this.sliderEl.style.transition = 'transform 0.42s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
      } else {
        this.sliderEl.style.transition = 'none';
      }
      this.sliderEl.style.transform = `translateX(${fractionalIndex * 100}%)`;
    }

    // Entire dock remains rigidly pinned to the viewport (zero translation jitter)
    if (this.bottomNavEl.style.transform) {
      this.bottomNavEl.style.transform = '';
    }

    // Active button highlight updates at 50% midpoint
    const roundedIndex = Math.round(fractionalIndex);
    if (roundedIndex >= 0 && roundedIndex < this.tabs.length) {
      const activeId = this.tabs[roundedIndex].id;
      this.bottomNavEl.querySelectorAll('[data-rx-tab]').forEach((btn) => {
        const active = btn.getAttribute('data-rx-tab') === activeId;
        btn.classList.toggle('is-active', active);
        btn.setAttribute('aria-current', active ? 'page' : 'false');
      });
    }
  }

  setSubViewOpen(isSubView) {
    const hidden = Boolean(isSubView);
    if (this.bottomNavEl) this.bottomNavEl.style.display = hidden ? 'none' : '';
    if (this.railEl) this.railEl.style.display = hidden ? 'none' : '';
    if (hidden) {
      document.body.classList.remove('rx-fullscreen-roster');
    } else {
      this.syncFullscreenRoster();
    }
  }

  syncFullscreenRoster() {
    const isRoster = Boolean(this.rosterTabId) && this.activeTab === this.rosterTabId;
    document.body.classList.toggle('rx-fullscreen-roster', isRoster);
  }
}

/**
 * Parses URL hash (#roster, #notifications, #credentials, #profile, #dashboard)
 * produced by /open/* redirects or shortcuts.
 */
export function consumeDeepLinkHash() {
  const raw = (window.location.hash || '').replace(/^#\/?/, '').trim().toLowerCase();
  if (!raw) return null;
  if (raw === 'roster' || raw === 'matrix' || raw === 'month_roster') return 'roster';
  if (raw === 'notifications' || raw === 'alerts' || raw === 'broadcast') return 'notifications';
  if (raw === 'credentials' || raw === 'credential_tracker') return 'credentials';
  if (raw === 'profile' || raw === 'account' || raw === 'settings') return 'profile';
  if (raw === 'dashboard' || raw === 'home') return 'dashboard';
  return null;
}
