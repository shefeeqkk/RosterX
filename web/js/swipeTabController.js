/**
 * RosterX — Edge-Safe Horizontal Touch Swipe Tab Switching with Real-Time 1:1 Finger Tracking & Rubber-Band Spring Effect
 * (swipeTabController.js)
 *
 * Implements WhatsApp-style active response for mobile tab switching:
 * 1. Active 1:1 real-time finger tracking: adjacent panel slides into view as finger moves
 * 2. Bottom nav dock active indicator glides in continuous real-time proportional to drag progress
 * 3. Tactile rubber-band overscroll resistance when swiping past Tab 0 (Dashboard) or Last Tab (More)
 * 4. Bouncy spring recoil (cubic-bezier(0.175, 0.885, 0.32, 1.275)) on boundary release
 * 5. Edge-safe content protection: prioritizes wide 31-day horizontal table scrolling on Month Roster
 * 6. Automatic gesture suspension when full-screen subviews, dialogs, or text inputs are active
 */

export class SwipeTabController {
  /**
   * @param {Object} opts
   * @param {HTMLElement|string} opts.container - The main container element or selector containing tab panels
   * @param {() => Array<{id: string}>} opts.getTabs - Returns the active list of tab objects in order
   * @param {() => string} opts.getActiveTab - Returns the current active tab id
   * @param {(tabId: string) => void} opts.onSwitchTab - Callback when a new tab should be selected
   * @param {(tabId: string) => HTMLElement|null} [opts.getPanelElement] - Custom panel resolver
   * @param {(tabId: string) => void} [opts.ensureTabRendered] - Pre-populates tab HTML before it slides in
   * @param {(fractionalIndex: number, boundaryOffsetPx: number, isSpring?: boolean) => void} [opts.onDragProgress] - Continuous dock indicator updater
   * @param {string} [opts.rosterTabId] - The ID of the roster tab with horizontal table scrolling
   * @param {() => boolean} [opts.isSuspended] - Returns true if gestures should be ignored (e.g. subview open)
   */
  constructor({
    container,
    getTabs,
    getActiveTab,
    onSwitchTab,
    getPanelElement,
    ensureTabRendered,
    onDragProgress,
    rosterTabId = 'month_roster',
    isSuspended = () => false
  }) {
    this.container = typeof container === 'string' ? document.querySelector(container) : container;
    this.getTabs = getTabs;
    this.getActiveTab = getActiveTab;
    this.onSwitchTab = onSwitchTab;
    this.getPanelElement = getPanelElement;
    this.ensureTabRendered = ensureTabRendered;
    this.onDragProgress = onDragProgress;
    this.rosterTabId = rosterTabId;
    this.isSuspended = isSuspended;

    // Gesture tracking state
    this.startX = 0;
    this.startY = 0;
    this.startTime = 0;
    this.isTracking = false;
    this.isHorizSwipe = false;
    this.isBoundaryStretch = false;
    this.currentOffset = 0;
    this.ignoreCurrentGesture = false;

    // Active sliding panel references
    this.currentPanel = null;
    this.adjacentEl = null;
    this.adjacentTabId = null;

    // Bound listeners for cleanup
    this._onTouchStart = this.onTouchStart.bind(this);
    this._onTouchMove = this.onTouchMove.bind(this);
    this._onTouchEnd = this.onTouchEnd.bind(this);
    this._onTouchCancel = this.onTouchCancel.bind(this);
  }

  getPanel(tabId) {
    if (!tabId) return null;
    if (this.getPanelElement) {
      const el = this.getPanelElement(tabId);
      if (el) return el;
    }
    return document.getElementById(`panel_${tabId}`) || document.getElementById(`tabContent_${tabId}`);
  }

  mount() {
    if (!this.container) return;
    this.container.addEventListener('touchstart', this._onTouchStart, { passive: true });
    this.container.addEventListener('touchmove', this._onTouchMove, { passive: false });
    this.container.addEventListener('touchend', this._onTouchEnd, { passive: true });
    this.container.addEventListener('touchcancel', this._onTouchCancel, { passive: true });
  }

  destroy() {
    if (!this.container) return;
    this.container.removeEventListener('touchstart', this._onTouchStart);
    this.container.removeEventListener('touchmove', this._onTouchMove);
    this.container.removeEventListener('touchend', this._onTouchEnd);
    this.container.removeEventListener('touchcancel', this._onTouchCancel);
    this.resetAllStyles();
  }

  onTouchStart(e) {
    if (!e.touches || e.touches.length !== 1) return;

    // 1. Suspension & device checks (mobile viewport or touch)
    if (this.isSuspended() || window.innerWidth >= 1024) {
      this.ignoreCurrentGesture = true;
      return;
    }

    const target = e.target;
    // 2. Ignore form inputs, sliders, and interactive control surfaces
    if (target.closest('input, textarea, select, button, [contenteditable="true"], .rx-no-swipe, [data-no-swipe]')) {
      this.ignoreCurrentGesture = true;
      return;
    }

    // 3. Edge-safe Month Roster protection
    // If on Month Roster table, prioritize 31-day table horizontal scrolling unless dragging from screen edge
    const activeId = this.getActiveTab();
    if (activeId === this.rosterTabId) {
      const scrollableTable = target.closest('.rx-month-grid, .rx-roster-scroll-table, .overflow-x-auto, table');
      const touchX = e.touches[0].clientX;
      const isEdgeSwipe = touchX < 36 || touchX > (window.innerWidth - 36);
      if (scrollableTable && !isEdgeSwipe) {
        this.ignoreCurrentGesture = true;
        return;
      }
    }

    this.ignoreCurrentGesture = false;
    this.startX = e.touches[0].clientX;
    this.startY = e.touches[0].clientY;
    this.startTime = Date.now();
    this.isTracking = true;
    this.isHorizSwipe = false;
    this.isBoundaryStretch = false;
    this.currentOffset = 0;
    this.currentPanel = this.getPanel(activeId);
    this.adjacentEl = null;
    this.adjacentTabId = null;
  }

  onTouchMove(e) {
    if (!this.isTracking || this.ignoreCurrentGesture || !e.touches || e.touches.length !== 1) return;

    const currentX = e.touches[0].clientX;
    const currentY = e.touches[0].clientY;
    const deltaX = currentX - this.startX;
    const deltaY = currentY - this.startY;

    // Detect gesture angle on initial movement
    if (!this.isHorizSwipe) {
      const absX = Math.abs(deltaX);
      const absY = Math.abs(deltaY);
      if (absY > absX && absY > 7) {
        // Vertical page scroll detected -> do not intercept
        this.isTracking = false;
        return;
      }
      if (absX > 8 && absX > absY * 1.15) {
        this.isHorizSwipe = true;
      } else {
        return;
      }
    }

    if (e.cancelable) e.preventDefault();

    // Determine current tab index
    const tabs = this.getTabs() || [];
    const activeId = this.getActiveTab();
    const activeIndex = tabs.findIndex((t) => t.id === activeId);
    if (activeIndex === -1) return;

    if (!this.currentPanel) {
      this.currentPanel = this.getPanel(activeId);
      if (!this.currentPanel) return;
    }

    const containerWidth = this.container?.getBoundingClientRect().width || window.innerWidth;

    // Check if dragging past bounds:
    // Tab 0 dragging right (deltaX > 0) OR Last Tab dragging left (deltaX < 0)
    const isAtStart = activeIndex === 0 && deltaX > 0;
    const isAtEnd = activeIndex === (tabs.length - 1) && deltaX < 0;

    if (isAtStart || isAtEnd) {
      this.isBoundaryStretch = true;
      if (this.adjacentEl) {
        this.resetAdjacentStyles();
      }

      // Rubber-band resistance decay curve: max ~65px
      const dampFactor = 0.28;
      const dampedOffset = Math.sign(deltaX) * Math.min(Math.abs(deltaX) * dampFactor, 65);
      this.currentOffset = dampedOffset;

      this.currentPanel.style.transition = 'none';
      this.currentPanel.style.willChange = 'transform';
      this.currentPanel.style.transform = `translateX(${dampedOffset}px)`;

      this.onDragProgress?.(activeIndex, dampedOffset, false);
      return;
    }

    // Normal active 1:1 swipe between adjacent tabs
    this.isBoundaryStretch = false;
    const targetIndex = deltaX < 0 ? activeIndex + 1 : activeIndex - 1;
    const targetTab = tabs[targetIndex];
    if (!targetTab) return;

    // Mount and prepare adjacent panel if target tab changed
    if (this.adjacentTabId !== targetTab.id) {
      if (this.adjacentEl) {
        this.resetAdjacentStyles();
      }
      this.adjacentTabId = targetTab.id;
      this.ensureTabRendered?.(this.adjacentTabId);
      this.adjacentEl = this.getPanel(this.adjacentTabId);

      if (this.adjacentEl) {
        this.adjacentEl.classList.remove('rx-hidden', 'hidden');
        this.adjacentEl.style.position = 'absolute';
        this.adjacentEl.style.top = '0';
        this.adjacentEl.style.left = '0';
        this.adjacentEl.style.width = '100%';
        this.adjacentEl.style.minHeight = '100%';
        this.adjacentEl.style.pointerEvents = 'none';
        this.adjacentEl.style.zIndex = '10';
        this.adjacentEl.style.willChange = 'transform';

        if (this.container && getComputedStyle(this.container).position === 'static') {
          this.container.style.position = 'relative';
        }
      }
    }

    // 1:1 active translation of both panels
    this.currentPanel.style.transition = 'none';
    this.currentPanel.style.willChange = 'transform';
    this.currentPanel.style.transform = `translateX(${deltaX}px)`;

    if (this.adjacentEl) {
      this.adjacentEl.style.transition = 'none';
      const adjacentOffset = deltaX < 0 ? (containerWidth + deltaX) : (-containerWidth + deltaX);
      this.adjacentEl.style.transform = `translateX(${adjacentOffset}px)`;
    }

    // Notify continuous dock indicator
    const dragFraction = -deltaX / containerWidth;
    const fractionalPos = activeIndex + dragFraction;
    this.onDragProgress?.(fractionalPos, 0, false);
  }

  onTouchEnd(e) {
    if (!this.isTracking || this.ignoreCurrentGesture) {
      this.resetAllStyles();
      return;
    }

    const changedTouch = e.changedTouches?.[0];
    const endX = changedTouch ? changedTouch.clientX : this.startX;
    const deltaX = endX - this.startX;
    const duration = Math.max(1, Date.now() - this.startTime);
    const velocity = Math.abs(deltaX) / duration;

    const tabs = this.getTabs() || [];
    const activeId = this.getActiveTab();
    const activeIndex = tabs.findIndex((t) => t.id === activeId);

    // 1. Handle boundary spring recoil
    if (this.isBoundaryStretch && this.currentPanel) {
      this.currentPanel.style.transition = 'transform 0.42s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
      this.currentPanel.style.transform = 'translateX(0px)';
      this.onDragProgress?.(activeIndex, 0, true);

      setTimeout(() => {
        this.resetAllStyles();
      }, 420);
      this.cleanupGestureState();
      return;
    }

    // 2. Evaluate normal tab switch threshold
    const containerWidth = this.container?.getBoundingClientRect().width || window.innerWidth;
    const shouldSwitch = this.isHorizSwipe && (
      Math.abs(deltaX) > (containerWidth * 0.22) ||
      (velocity > 0.35 && Math.abs(deltaX) > 28)
    );

    if (shouldSwitch && this.adjacentEl && this.adjacentTabId) {
      const targetTabId = this.adjacentTabId;
      const targetIndex = tabs.findIndex((t) => t.id === targetTabId);

      this.currentPanel.style.transition = 'transform 0.26s cubic-bezier(0.2, 0.9, 0.3, 1)';
      this.adjacentEl.style.transition = 'transform 0.26s cubic-bezier(0.2, 0.9, 0.3, 1)';

      this.currentPanel.style.transform = deltaX < 0 ? `translateX(-${containerWidth}px)` : `translateX(${containerWidth}px)`;
      this.adjacentEl.style.transform = 'translateX(0px)';

      this.onDragProgress?.(targetIndex, 0, true);

      setTimeout(() => {
        this.resetAdjacentStyles();
        this.resetStyles();
        this.onSwitchTab(targetTabId);
      }, 260);
    } else {
      // Snap back to current tab
      if (this.currentPanel) {
        this.currentPanel.style.transition = 'transform 0.22s ease-out';
        this.currentPanel.style.transform = 'translateX(0px)';
      }
      if (this.adjacentEl) {
        this.adjacentEl.style.transition = 'transform 0.22s ease-out';
        this.adjacentEl.style.transform = deltaX < 0 ? `translateX(${containerWidth}px)` : `translateX(-${containerWidth}px)`;
      }
      this.onDragProgress?.(activeIndex, 0, true);

      setTimeout(() => {
        this.resetAdjacentStyles();
        this.resetStyles();
      }, 230);
    }

    this.cleanupGestureState();
  }

  onTouchCancel() {
    this.resetAllStyles();
    this.cleanupGestureState();
  }

  cleanupGestureState() {
    this.isTracking = false;
    this.isHorizSwipe = false;
    this.isBoundaryStretch = false;
    this.ignoreCurrentGesture = false;
  }

  resetStyles() {
    if (this.currentPanel) {
      this.currentPanel.style.transition = '';
      this.currentPanel.style.transform = '';
      this.currentPanel.style.willChange = '';
    }
  }

  resetAdjacentStyles() {
    if (this.adjacentEl) {
      this.adjacentEl.style.transition = '';
      this.adjacentEl.style.transform = '';
      this.adjacentEl.style.position = '';
      this.adjacentEl.style.top = '';
      this.adjacentEl.style.left = '';
      this.adjacentEl.style.width = '';
      this.adjacentEl.style.minHeight = '';
      this.adjacentEl.style.pointerEvents = '';
      this.adjacentEl.style.zIndex = '';
      this.adjacentEl.style.willChange = '';
      this.adjacentEl.classList.add('rx-hidden', 'hidden');
      this.adjacentEl = null;
      this.adjacentTabId = null;
    }
  }

  resetAllStyles() {
    this.resetStyles();
    this.resetAdjacentStyles();
  }
}
