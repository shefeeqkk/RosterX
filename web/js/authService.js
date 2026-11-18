/**
 * RosterX - Authentication & Role-Based Access Control Service (authService.js)
 *
 * Manages:
 * - Server-side authentication via POST /api/auth/login
 * - Secure JWT token persistence (zero passwords stored in browser storage)
 * - OTP Password Reset verification via backend Resend gateway
 * - Session state and 1:1 Android role routing (Admin -> index.html; Manager/Engineer/Apprentice -> user-portal.html)
 */

import { OtpService } from './otpService.js';

const STORAGE_KEYS = {
  ACTIVE_SESSION: 'rosterx_auth_user',
  AUTH_TOKEN: 'rosterx_auth_token',
  ENGINEERS_LIST: 'rosterx_engineers_list'
};

export class AuthService {
  constructor() {
    // Zero plaintext passwords or credentials seeded in client storage
  }

  getAuthToken() {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
  }

  getCurrentUser() {
    if (typeof localStorage === 'undefined') return null;
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.ACTIVE_SESSION);
      if (stored) return JSON.parse(stored);
    } catch (_) {}
    return null;
  }

  async login(identifier, password) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    const cleanPass = String(password || '').trim();

    if (!cleanId || !cleanPass) {
      return {
        success: false,
        error: 'Username and password are required.'
      };
    }

    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identifier: cleanId, password: cleanPass })
      });

      const data = await resp.json();
      if (resp.ok && data.success && data.user) {
        if (data.token) {
          localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, data.token);
        }
        localStorage.setItem(STORAGE_KEYS.ACTIVE_SESSION, JSON.stringify(data.user));

        return {
          success: true,
          user: data.user,
          token: data.token
        };
      } else {
        return {
          success: false,
          error: data.message || 'Invalid username or password.'
        };
      }
    } catch (err) {
      return {
        success: false,
        error: `Could not reach authentication server: ${err.message}`
      };
    }
  }

  clearSession() {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION);
      localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    }
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem('rosterx_last_redirect');
    }
  }

  logout() {
    this.clearSession();
    window.location.href = 'login.html?logout=1';
  }

  /**
   * Enforce authentication & exact Android role parity:
   * - requiredRole === 'admin': only 'admin' stays on index.html; manager/engineer/apprentice route to user-portal.html
   * - preserves deep-link #hash across redirects
   */
  enforceAuth(requiredRole = 'any') {
    const user = this.getCurrentUser();
    const token = this.getAuthToken();
    const hash = window.location.hash || '';

    if (!user || !token) {
      this.clearSession();
      window.location.replace(`login.html?session_expired=1${hash}`);
      return null;
    }

    if (requiredRole === 'admin' && user.role !== 'admin') {
      window.location.replace(`user-portal.html${hash}`);
      return null;
    }

    return user;
  }

  async requestPasswordResetOtp(identifier) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    if (!cleanId) {
      return { success: false, error: 'Please enter your username or email address.' };
    }
    return OtpService.requestOtp({ identifier: cleanId });
  }

  async resetPassword({ identifier, otp, newPassword }) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    const cleanOtp = String(otp || '').trim();
    const cleanPass = String(newPassword || '').trim();

    if (cleanPass.length < 8) {
      return { success: false, error: 'New password must be at least 8 characters long.' };
    }

    return OtpService.verifyAndReset({
      identifier: cleanId,
      otp: cleanOtp,
      newPin: cleanPass
    });
  }

  async changePassword(identifier, currentPassword, newPassword) {
    const cleanNew = String(newPassword || '').trim();
    if (cleanNew.length < 8) {
      return { success: false, error: 'New password must be at least 8 characters long.' };
    }

    const loginRes = await this.login(identifier, currentPassword);
    if (!loginRes.success) {
      return { success: false, error: 'Current password is incorrect.' };
    }

    return {
      success: false,
      error: 'Please use the secure OTP verification flow to update your password.'
    };
  }
}

export const authService = new AuthService();
if (typeof window !== 'undefined') {
  window.authService = authService;
}
