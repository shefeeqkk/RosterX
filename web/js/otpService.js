/**
 * RosterX - OTP & Password Reset Client Service (Module C)
 * 
 * Supports:
 * 1. Requesting 6-digit verification code dispatch via backend Resend gateway
 * 2. Verifying OTP and resetting credentials securely
 */

export class OtpService {
  /**
   * Request password reset OTP from backend
   * @param {Object} params
   * @param {string} params.identifier - Username or Email
   */
  static async requestOtp({ identifier }) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    if (!cleanId) {
      return { success: false, error: 'Username or email is required.' };
    }

    try {
      const response = await fetch('/api/auth/send-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identifier: cleanId })
      });
      const data = await response.json();
      if (data && data.success) {
        return {
          success: true,
          message: data.message || 'If the account exists, a verification code has been dispatched.'
        };
      } else {
        return {
          success: false,
          error: data.message || 'Failed to request verification code.'
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
   * Verify OTP and reset password
   * @param {Object} params
   * @param {string} params.identifier
   * @param {string} params.otp
   * @param {string} params.newPin
   */
  static async verifyAndReset({ identifier, otp, newPin }) {
    const cleanId = String(identifier || '').trim().toLowerCase();
    const cleanOtp = String(otp || '').trim();
    const cleanPassword = String(newPin || '').trim();

    if (!cleanId || !cleanOtp || !cleanPassword) {
      return { success: false, error: 'Identifier, verification code, and new password are required.' };
    }

    if (cleanPassword.length < 8) {
      return { success: false, error: 'New password must be at least 8 characters long.' };
    }

    try {
      const response = await fetch('/api/auth/verify-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          identifier: cleanId,
          otp: cleanOtp,
          newPassword: cleanPassword
        })
      });
      const data = await response.json();
      if (data && data.success) {
        return {
          success: true,
          message: data.message || 'Password reset successfully! You can now log in.'
        };
      } else {
        return {
          success: false,
          error: data.message || 'Invalid or expired verification code.'
        };
      }
    } catch (err) {
      return {
        success: false,
        error: `Verification service unavailable: ${err.message}`
      };
    }
  }
}
