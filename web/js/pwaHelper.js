/**
 * RosterX — iOS PWA Helper & Universal Accessible Dialog Controller (pwaHelper.js)
 *
 * Provides:
 * - Service Worker registration (/sw.js)
 * - Dismissible iOS Safari "Add to Home Screen" installation guide banner
 * - Native <dialog closedby="any"> opener with mandatory Safari light-dismiss fallback
 */

const IOS_INSTALL_DISMISS_KEY = 'rosterx_ios_install_dismissed_v2';

export function registerPwaServiceWorker() {
  if (typeof navigator === 'undefined' || !('serviceWorker' in navigator)) return;
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch(() => {});
  });
}

export function isIosSafariNonStandalone() {
  if (typeof navigator === 'undefined' || typeof window === 'undefined') return false;
  const ua = navigator.userAgent || '';
  const isIos = /iPad|iPhone|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  const isStandalone = window.matchMedia?.('(display-mode: standalone)')?.matches || window.navigator.standalone === true;
  const isSafari = /Safari/.test(ua) && !/CriOS|FxiOS|EdgiOS|OPiOS/.test(ua);
  return isIos && isSafari && !isStandalone;
}

export function mountIosInstallBanner(container = document.body) {
  if (!isIosSafariNonStandalone()) return;
  try {
    if (localStorage.getItem(IOS_INSTALL_DISMISS_KEY) === '1') return;
  } catch (_) {}
  if (document.getElementById('rxIosInstallBanner')) return;

  const banner = document.createElement('div');
  banner.id = 'rxIosInstallBanner';
  banner.className = 'rx-ios-install-banner';
  banner.setAttribute('role', 'region');
  banner.setAttribute('aria-label', 'Install RosterX App on iPhone');
  banner.innerHTML = `
    <div class="rx-ios-install-inner">
      <img src="assets/logo.png" alt="RosterX" class="rx-ios-install-icon" />
      <div class="rx-ios-install-copy">
        <strong>Install RosterX App</strong>
        <span>Tap <b>Share</b> <i class="ph ph-export" aria-hidden="true"></i> then <b>Add to Home Screen</b> for full-screen iOS app mode.</span>
      </div>
      <button type="button" class="rx-ios-install-close" aria-label="Dismiss install tip">
        <i class="ph ph-x" aria-hidden="true"></i>
      </button>
    </div>
  `;

  banner.querySelector('.rx-ios-install-close')?.addEventListener('click', () => {
    try { localStorage.setItem(IOS_INSTALL_DISMISS_KEY, '1'); } catch (_) {}
    banner.remove();
  });

  container.appendChild(banner);
}

/**
 * Attaches light-dismiss fallback and background touch-lock for <dialog closedby="any">.
 */
export function wireDialogLightDismiss(dialogEl) {
  if (!dialogEl || dialogEl.dataset.rxLightDismissWired === '1') return;
  dialogEl.dataset.rxLightDismissWired = '1';

  // Backdrop Touch-Lock (prevents background scroll bleed on iOS/Android)
  dialogEl.addEventListener('touchmove', (event) => {
    if (event.target === dialogEl) {
      event.preventDefault();
    }
  }, { passive: false });

  const syncBodyScrollLock = () => {
    setTimeout(() => {
      if (!document.querySelector('dialog.rx-modal[open]')) {
        document.documentElement.classList.remove('rx-modal-open');
        document.body.classList.remove('rx-modal-open');
      }
    }, 50);
  };

  dialogEl.addEventListener('close', syncBodyScrollLock);
  dialogEl.addEventListener('cancel', syncBodyScrollLock);

  if (!('closedBy' in HTMLDialogElement.prototype)) {
    dialogEl.addEventListener('click', (event) => {
      if (event.target !== dialogEl) return;
      const rect = dialogEl.getBoundingClientRect();
      const inside = (
        rect.top <= event.clientY &&
        event.clientY <= rect.top + rect.height &&
        rect.left <= event.clientX &&
        event.clientX <= rect.left + rect.width
      );
      if (!inside) {
        dialogEl.close();
      }
    });
  }
}

/**
 * Opens a native <dialog> using showModal() with automatic light-dismiss wiring and background scroll-lock.
 */
export function openNeumorphicDialog(dialogEl) {
  if (!dialogEl) return;
  if (!dialogEl.hasAttribute('closedby')) {
    dialogEl.setAttribute('closedby', 'any');
  }
  wireDialogLightDismiss(dialogEl);
  if (!dialogEl.open) {
    try {
      window.history.pushState({ scope: 'dialog', id: dialogEl.id || 'rx_modal' }, '');
    } catch (_) {}
    document.documentElement.classList.add('rx-modal-open');
    document.body.classList.add('rx-modal-open');
    dialogEl.showModal();
  }
}

export function closeNeumorphicDialog(dialogEl) {
  if (dialogEl && dialogEl.open) {
    dialogEl.close();
  }
  if (!document.querySelector('dialog.rx-modal[open]')) {
    document.documentElement.classList.remove('rx-modal-open');
    document.body.classList.remove('rx-modal-open');
  }
}

export function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function getAuthHeaders(extra = {}) {
  let token = null;
  try {
    token = localStorage.getItem('rosterx_auth_token');
  } catch (_) {}
  const headers = { 'Content-Type': 'application/json', ...extra };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  return headers;
}
