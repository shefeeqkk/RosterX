/**
 * RosterX — Edge-Safe Horizontal Touch Swipe Tab Switching with Rubber-Band Spring Effect
 * (swipeTabController.js)
 *
 * Implements smooth touch gestures for mobile tab switching:
 * 1. Horizontal swipe left -> next tab
 * 2. Horizontal swipe right -> previous tab
 * 3. Tactile rubber-band overscroll resistance when swiping past Tab 0 (Dashboard) or Last Tab (More)
 * 4. Bouncy cubic-bezier spring recoil (cubic-bezier(0.175, 0.885, 0.32, 1.275)) on boundary release
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
   * @param {string} [opts.rosterTabId] - The ID of the roster tab with horizontal table scrolling
   * @param {() => boolean} [opts.isSuspended] - Returns true if gestures should be ignored (e.g. subview open)
   */
  constructor({ container, getTabs, getActiveTab, onSwitchTab, rosterTabId = 'month_roster', isSuspended = () => false }) {
    this.container = typeof container === 'string' ? document.querySelector(container) : container;
    this.getTabs = getTabs;
    this.getActiveTab = getActiveTab;
    this.onSwitchTab = onSwitchTab;
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

    // Bound listeners for cleanup
    this._onTouchStart = this.onTouchStart.bind(this);
    this._onTouchMove = this.onTouchMove.bind(this);
    this._onTouchEnd = this.onTouchEnd.bind(this);
    this._onTouchCancel = this.onTouchCancel.bind(this);
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
    this.resetStyles();
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
      if (absY > absX && absY > 8) {
        // Vertical page scroll detected -> do not intercept
        this.isTracking = false;
        return;
      }
      if (absX > 10 && absX > absY * 1.25) {
        this.isHorizSwipe = true;
      } else {
        return;
      }
    }

    // Determine current tab index
    const tabs = this.getTabs() || [];
    const activeId = this.getActiveTab();
    const activeIndex = tabs.findIndex((t) => t.id === activeId);
    if (activeIndex === -1) return;

    // Check if dragging past bounds:
    // Tab 0 dragging right (deltaX > 0) OR Last Tab dragging left (deltaX < 0)
    const isAtStart = activeIndex === 0 && deltaX > 0;
    const isAtEnd = activeIndex === (tabs.length - 1) && deltaX < 0;

    if (isAtStart || isAtEnd) {
      this.isBoundaryStretch = true;
      // Rubber-band resistance decay curve: max ~60px
      const dampFactor = 0.26;
      const dampedOffset = Math.sign(deltaX) * Math.min(Math.abs(deltaX) * dampFactor, 62);
      this.currentOffset = dampedOffset;

      if (this.container) {
        this.container.style.transition = 'none';
        this.container.style.transform = `translateX(${dampedOffset}px)`;
      }
      if (e.cancelable) e.preventDefault();
    } else {
      // Normal inner tab swipe
      this.isBoundaryStretch = false;
      if (e.cancelable && Math.abs(deltaX) > 20) {
        e.preventDefault();
      }
    }
  }

  onTouchEnd(e) {
    if (!this.isTracking || this.ignoreCurrentGesture) {
      this.resetStyles();
      return;
    }

    const changedTouch = e.changedTouches?.[0];
    const endX = changedTouch ? changedTouch.clientX : this.startX;
    const deltaX = endX - this.startX;
    const duration = Math.max(1, Date.now() - this.startTime);
    const velocity = Math.abs(deltaX) / duration;

    // Handle boundary spring recoil
    if (this.isBoundaryStretch && this.container) {
      this.container.style.transition = 'transform 0.42s cubic-bezier(0.175, 0.885, 0.32, 1.275)';
      this.container.style.transform = 'translateX(0px)';
      setTimeout(() => {
        this.resetStyles();
      }, 420);
      this.isTracking = false;
      this.isHorizSwipe = false;
      this.isBoundaryStretch = false;
      return;
    }

    // Evaluate normal tab switch threshold:
    // Min distance 55px or fast flick (velocity > 0.4px/ms and distance > 30px)
    const isValidSwipe = this.isHorizSwipe && (Math.abs(deltaX) > 55 || (velocity > 0.4 && Math.abs(deltaX) > 30));

    if (isValidSwipe) {
      const tabs = this.getTabs() || [];
      const activeId = this.getActiveTab();
      const activeIndex = tabs.findIndex((t) => t.id === activeId);

      if (activeIndex !== -1) {
        let targetIndex = activeIndex;
        let slideDirection = 0; // 1 = slide in from right, -1 = slide in from left

        if (deltaX < 0 && activeIndex < tabs.length - 1) {
          // Swipe Left -> Next Tab
          targetIndex = activeIndex + 1;
          slideDirection = 1;
        } else if (deltaX > 0 && activeIndex > 0) {
          // Swipe Right -> Previous Tab
          targetIndex = activeIndex - 1;
          slideDirection = -1;
        }

        if (targetIndex !== activeIndex) {
          const targetTab = tabs[targetIndex];
          if (targetTab?.id) {
            this.animateTabTransition(slideDirection);
            this.onSwitchTab(targetTab.id);
          }
        }
      }
    }

    this.resetStyles();
    this.isTracking = false;
    this.isHorizSwipe = false;
    this.isBoundaryStretch = false;
  }

  onTouchCancel() {
    this.resetStyles();
    this.isTracking = false;
    this.isHorizSwipe = false;
    this.isBoundaryStretch = false;
    this.ignoreCurrentGesture = false;
  }

  animateTabTransition(direction) {
    if (!this.container) return;
    // Directional entry subtle slide animation (16px)
    const startX = direction > 0 ? 18 : -18;
    this.container.style.transition = 'none';
    this.container.style.transform = `translateX(${startX}px)`;
    this.container.style.opacity = '0.85';

    requestAnimationFrame(() => {
      if (!this.container) return;
      this.container.style.transition = 'transform 0.22s cubic-bezier(0.2, 0.9, 0.3, 1), opacity 0.2s ease';
      this.container.style.transform = 'translateX(0px)';
      this.container.style.opacity = '1';

      setTimeout(() => {
        this.resetStyles();
      }, 230);
    });
  }

  resetStyles() {
    if (!this.container) return;
    this.container.style.transition = '';
    this.container.style.transform = '';
    this.container.style.opacity = '';
  }
}
