/**
 * RosterX - Authentication & Role-Based Access Control Service (authService.js)
 * 
 * Manages:
 * - Server-side authentication via POST /api/auth/login
 * - Secure JWT token persistence (zero passwords stored in browser storage)
 * - OTP Password Reset verification via backend Resend gateway
 * - Session state and role routing (Admin vs Staff)
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

  /**
   * Get active authorization token
   */
  getAuthToken() {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
  }

  /**
   * Get active logged-in user profile
   */
  getCurrentUser() {
    if (typeof localStorage === 'undefined') return null;
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.ACTIVE_SESSION);
      if (stored) return JSON.parse(stored);
    } catch (e) {}
    return null;
  }

  /**
   * Authenticate user with server backend (/api/auth/login)
   */
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
        // Persist token and user profile (NEVER store password in client storage)
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

  /**
   * Sign out current user and clear tokens
   */
  logout() {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION);
      localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    }
    window.location.href = 'login.html';
  }

  /**
   * Check if user is authenticated and enforce role
   * @param {string} requiredRole - 'admin' | 'any'
   */
  enforceAuth(requiredRole = 'any') {
    const user = this.getCurrentUser();
    const token = this.getAuthToken();

    if (!user || !token) {
      window.location.replace('login.html');
      return null;
    }

    if (requiredRole === 'admin' && user.role !== 'admin') {
      // Non-admin staff redirected to their portal
      window.location.replace('user-portal.html');
      return null;
    }

    return user;
  }

  /**
   * Request OTP for password reset (dispatched to registered email solely by server)
   */
  async requestPasswordResetOtp(identifier) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    if (!cleanId) {
      return { success: false, error: 'Please enter your username or email address.' };
    }

    const result = await OtpService.requestOtp({ identifier: cleanId });
    return result;
  }

  /**
   * Verify OTP and set new password
   */
  async resetPassword({ identifier, otp, newPassword }) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    const cleanOtp = String(otp || '').trim();
    const cleanPass = String(newPassword || '').trim();

    if (cleanPass.length < 8) {
      return { success: false, error: 'New password must be at least 8 characters long.' };
    }

    const verifyResult = await OtpService.verifyAndReset({
      identifier: cleanId,
      otp: cleanOtp,
      newPin: cleanPass
    });

    return verifyResult;
  }

  /**
   * Change password directly for an authenticated user
   */
  async changePassword(identifier, currentPassword, newPassword) {
    const cleanNew = String(newPassword || '').trim();
    if (cleanNew.length < 8) {
      return { success: false, error: 'New password must be at least 8 characters long.' };
    }

    // Authenticate with current credentials
    const loginRes = await this.login(identifier, currentPassword);
    if (!loginRes.success) {
      return { success: false, error: 'Current password is incorrect.' };
    }

    // Direct password update requires admin or OTP verification
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
