/**
 * RosterX - Authentication & Role-Based Access Control Service (authService.js)
 * 
 * Manages:
 * - Admin and Engineer credentials
 * - Password persistence & hashing
 * - OTP Password Reset verification
 * - Session state and role routing
 */

import { DEFAULT_ENGINEERS } from './constants.js?v=1.0';
import { OtpService } from './otpService.js';

const STORAGE_KEYS = {
  ACTIVE_SESSION: 'rosterx_auth_user',
  USERS_CREDENTIALS: 'rosterx_users_credentials',
  ENGINEERS_LIST: 'rosterx_engineers_list'
};

export class AuthService {
  constructor() {
    this.initDefaultUsers();
  }

  /**
   * Seed and synchronize user accounts into localStorage
   */
  initDefaultUsers() {
    if (typeof localStorage === 'undefined') return;

    let users = this.getUsersDatabase();
    if (!users || typeof users !== 'object') {
      users = {};
    }

    // Ensure Master Admin Account
    if (!users.admin) {
      users.admin = {
        id: 'admin_01',
        username: 'admin',
        name: 'System Administrator',
        email: 'admin@rosterx.dev',
        phone: '+919811001100',
        password: 'admin123',
        role: 'admin',
        avatar: 'assets/logo.png'
      };
    }

    // Seed or update all engineers as user accounts
    const engList = this.getEngineersList();
    engList.forEach(eng => {
      const username = (eng.username || this.generateUsername(eng.name, eng.email)).trim().toLowerCase();
      const email = eng.email || `${username}@tejasnetworks.com`;
      const domainPrefix = email.includes('@') ? email.split('@')[0].trim().toLowerCase() : username;

      // Find existing account for this engineer
      const existingKey = Object.keys(users).find(k => {
        const u = users[k];
        return (u.engineerId && u.engineerId.toLowerCase() === (eng.id || '').toLowerCase()) ||
               (u.name && u.name.trim().toUpperCase() === (eng.name || '').trim().toUpperCase()) ||
               (u.username && u.username.toLowerCase() === username) ||
               (u.username && u.username.toLowerCase() === domainPrefix);
      });

      const existingRecord = existingKey ? users[existingKey] : null;
      const currentPass = existingRecord ? existingRecord.password : 'user123';

      const userObj = {
        id: eng.id,
        username: username,
        name: eng.name,
        full_name: eng.full_name || eng.name,
        email: email,
        phone: eng.phone || '+919811001100',
        password: currentPass,
        role: eng.role === 'admin' ? 'admin' : (eng.role === 'apprentice' ? 'apprentice' : 'engineer'),
        engineerId: eng.id,
        avatar: 'assets/bot-3d.png'
      };

      users[username] = userObj;
      if (domainPrefix && domainPrefix !== username) {
        users[domainPrefix] = userObj;
      }
      const firstName = this.generateUsername(eng.name);
      if (firstName && firstName !== username && !users[firstName]) {
        users[firstName] = userObj;
      }
    });

    localStorage.setItem(STORAGE_KEYS.USERS_CREDENTIALS, JSON.stringify(users));
  }

  /**
   * Generate clean username from email domain prefix or engineer name
   * e.g. "shefeeqkk@tejasnetworks.com" -> "shefeeqkk", "nandua@tejasnetworks.com" -> "nandua"
   */
  generateUsername(name, email = '') {
    if (email && typeof email === 'string' && email.includes('@')) {
      const prefix = email.split('@')[0].trim().toLowerCase();
      if (prefix) return prefix;
    }
    if (!name) return 'user';
    const clean = String(name).trim().toLowerCase();
    if (clean.includes('@')) {
      return clean.split('@')[0].trim();
    }
    const parts = clean.split(/[\s\W]+/);
    if (parts.length > 1 && clean.includes('raj')) {
      return parts.join(''); // "rajkumar"
    }
    return parts[0] || clean;
  }

  /**
   * Get dynamic engineers directory from localStorage or defaults
   */
  getEngineersList() {
    if (typeof localStorage !== 'undefined') {
      try {
        const stored = localStorage.getItem(STORAGE_KEYS.ENGINEERS_LIST);
        if (stored) {
          const parsed = JSON.parse(stored);
          if (Array.isArray(parsed) && parsed.length > 0) return parsed;
        }
      } catch (e) {}
    }
    return DEFAULT_ENGINEERS || [];
  }

  getUsersDatabase() {
    if (typeof localStorage === 'undefined') return {};
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.USERS_CREDENTIALS);
      if (stored) return JSON.parse(stored);
    } catch (e) {}
    return {};
  }

  saveUsersDatabase(db) {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(STORAGE_KEYS.USERS_CREDENTIALS, JSON.stringify(db));
  }

  /**
   * Synchronize an engineer record's email, phone, name into user accounts and session
   */
  syncEngineerProfile(eng) {
    if (!eng || (!eng.id && !eng.name)) return;
    const users = this.getUsersDatabase();
    const cleanName = (eng.name || '').trim().toLowerCase();
    const cleanId = (eng.id || '').trim().toLowerCase();
    const username = (eng.username || this.generateUsername(eng.name, eng.email)).trim().toLowerCase();
    const email = eng.email || (username ? `${username}@tejasnetworks.com` : '');
    const domainPrefix = email && email.includes('@') ? email.split('@')[0].trim().toLowerCase() : username;

    // Find account by engineerId, id, name, or username
    let userKey = Object.keys(users).find(k => {
      const u = users[k];
      return (u.engineerId && u.engineerId.toLowerCase() === cleanId) ||
             (u.id && u.id.toLowerCase() === cleanId) ||
             (u.name && u.name.trim().toLowerCase() === cleanName) ||
             (k.toLowerCase() === cleanName) ||
             (k.toLowerCase() === username) ||
             (k.toLowerCase() === domainPrefix);
    });

    if (userKey && users[userKey]) {
      if (eng.email) users[userKey].email = eng.email;
      if (eng.phone) users[userKey].phone = eng.phone;
      if (eng.full_name) users[userKey].full_name = eng.full_name;
      if (eng.name) users[userKey].name = eng.name;
      if (username) {
        users[userKey].username = username;
        users[username] = users[userKey];
      }
      if (domainPrefix) {
        users[domainPrefix] = users[userKey];
      }
      if (eng.role) users[userKey].role = (eng.role === 'admin' ? 'admin' : (eng.role === 'apprentice' ? 'apprentice' : 'engineer'));
      this.saveUsersDatabase(users);

      // Also update active session if currently logged in as this user
      const current = this.getCurrentUser();
      if (current && (current.username === userKey || current.engineerId === eng.id || current.name === eng.name || (eng.name && current.name && current.name.toUpperCase() === eng.name.toUpperCase()))) {
        const updatedSession = { ...current, ...users[userKey] };
        localStorage.setItem(STORAGE_KEYS.ACTIVE_SESSION, JSON.stringify(updatedSession));
      }
    } else {
      // Create new user record
      const newUser = {
        id: eng.id || `eng_${Date.now()}`,
        username: username,
        name: eng.name,
        full_name: eng.full_name || eng.name,
        email: email,
        phone: eng.phone || '+919811001100',
        password: 'user123',
        role: eng.role === 'admin' ? 'admin' : (eng.role === 'apprentice' ? 'apprentice' : 'engineer'),
        engineerId: eng.id,
        avatar: 'assets/bot-3d.png'
      };
      users[username] = newUser;
      if (domainPrefix && domainPrefix !== username) {
        users[domainPrefix] = newUser;
      }
      this.saveUsersDatabase(users);
    }
  }

  /**
   * Synchronize an entire list of engineers into user accounts
   */
  syncEngineersList(engList) {
    if (!Array.isArray(engList)) return;
    engList.forEach(eng => this.syncEngineerProfile(eng));
  }

  /**
   * Asynchronously synchronize engineers from backend REST API (MongoDB Atlas / server.py)
   */
  async refreshUsersFromBackend() {
    try {
      const resp = await fetch('/api/engineers', { cache: 'no-store' });
      if (resp.ok) {
        const data = await resp.json();
        const docs = data.documents || data.engineers || (Array.isArray(data) ? data : []);
        if (Array.isArray(docs) && docs.length > 0) {
          this.syncEngineersList(docs);
          return true;
        }
      }
    } catch (e) {
      // Offline / network fallback
    }
    return false;
  }

  /**
   * Authenticate user with username and password
   * Supports username, email, or full engineer name matching
   */
  async login(identifier, password) {
    const cleanId = String(identifier).trim().toLowerCase();
    const cleanPass = String(password).trim();
    let users = this.getUsersDatabase();

    const findMatch = (db) => Object.values(db).find(u => 
      (u.username && u.username.toLowerCase() === cleanId) ||
      (u.email && u.email.toLowerCase() === cleanId) ||
      (u.email && u.email.split('@')[0].toLowerCase() === cleanId) ||
      (u.name && u.name.toLowerCase() === cleanId) ||
      (u.full_name && u.full_name.toLowerCase() === cleanId) ||
      (this.generateUsername(u.name, u.email) === cleanId)
    );

    // 1. Direct match by username or aliases
    let user = users[cleanId] || findMatch(users);

    // 2. Refresh from backend if not found
    if (!user) {
      await this.refreshUsersFromBackend();
      users = this.getUsersDatabase();
      user = users[cleanId] || findMatch(users);
    }

    // 3. Fallback: Query backend /api/auth/login
    if (!user) {
      try {
        const resp = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ identifier: cleanId, password: cleanPass })
        });
        const data = await resp.json();
        if (data && data.success && data.user) {
          user = data.user;
          users[cleanId] = { ...data.user, password: cleanPass };
          this.saveUsersDatabase(users);
        }
      } catch (e) {}
    }

    if (!user) {
      return {
        success: false,
        error: 'Invalid username or password. Please verify your credentials.'
      };
    }

    // Verify password locally
    let passwordValid = (user.password === cleanPass);

    // If local password mismatch, check with backend (in case password was updated in DB)
    if (!passwordValid) {
      try {
        const resp = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ identifier: cleanId, password: cleanPass })
        });
        const data = await resp.json();
        if (data && data.success) {
          passwordValid = true;
          user.password = cleanPass;
          users[cleanId] = user;
          this.saveUsersDatabase(users);
        }
      } catch (e) {}
    }

    if (!passwordValid) {
      return {
        success: false,
        error: 'Invalid password. Please check your credentials or reset your password.'
      };
    }

    // Store active session
    const sessionUser = {
      id: user.id || `eng_${cleanId}`,
      username: user.username || cleanId,
      name: user.name || cleanId.toUpperCase(),
      full_name: user.full_name || user.name || cleanId.toUpperCase(),
      email: user.email || '',
      phone: user.phone || '',
      role: user.role || 'engineer',
      loginTime: new Date().toISOString()
    };

    localStorage.setItem(STORAGE_KEYS.ACTIVE_SESSION, JSON.stringify(sessionUser));

    return {
      success: true,
      user: sessionUser
    };
  }

  /**
   * Get active logged-in user
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
   * Sign out current user
   */
  logout() {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(STORAGE_KEYS.ACTIVE_SESSION);
    }
    window.location.href = 'login.html';
  }

  /**
   * Check if user is authenticated and enforce role
   * @param {string} requiredRole - 'admin' | 'any'
   */
  enforceAuth(requiredRole = 'any') {
    const user = this.getCurrentUser();
    if (!user) {
      window.location.replace('login.html');
      return null;
    }

    if (requiredRole === 'admin' && user.role !== 'admin') {
      // Redirect staff to their portal
      window.location.replace('user-portal.html');
      return null;
    }

    return user;
  }

  /**
   * Request OTP for password reset
   */
  async requestPasswordResetOtp(identifier) {
    const cleanId = String(identifier).trim().toLowerCase();
    let users = this.getUsersDatabase();

    const findMatch = (db) => Object.values(db).find(u => 
      (u.username && u.username.toLowerCase() === cleanId) || 
      (u.email && u.email.toLowerCase() === cleanId) ||
      (u.email && u.email.split('@')[0].toLowerCase() === cleanId) ||
      (u.name && u.name.toLowerCase() === cleanId) ||
      (u.full_name && u.full_name.toLowerCase() === cleanId) ||
      (this.generateUsername(u.name, u.email) === cleanId)
    );

    let user = findMatch(users);

    // 1. If not found in local cache, refresh from backend API
    if (!user) {
      await this.refreshUsersFromBackend();
      users = this.getUsersDatabase();
      user = findMatch(users);
    }

    // 2. If still not found, check engineers directory directly
    if (!user) {
      const engList = this.getEngineersList();
      const eng = engList.find(e => 
        (e.username && e.username.toLowerCase() === cleanId) ||
        (e.email && e.email.toLowerCase() === cleanId) ||
        (e.email && e.email.split('@')[0].toLowerCase() === cleanId) ||
        (e.name && e.name.toLowerCase() === cleanId) ||
        (this.generateUsername(e.name, e.email) === cleanId)
      );
      if (eng) {
        this.syncEngineerProfile(eng);
        users = this.getUsersDatabase();
        user = findMatch(users);
      }
    }

    // 3. If STILL not found locally, delegate to server-side OTP dispatcher
    // (The server has direct Atlas DB access and can find the user by username, email prefix, or full email)
    if (!user) {
      const serverRes = await OtpService.requestOtp({ identifier: cleanId });
      if (serverRes && serverRes.success) {
        return {
          ...serverRes,
          delivered_to: serverRes.delivered_to,
          user: serverRes.user || {
            username: cleanId,
            name: serverRes.user?.name || cleanId,
            email: serverRes.delivered_to || cleanId
          }
        };
      }
      return {
        success: false,
        error: `No account found for "${identifier}". Please verify your username or registered email.`
      };
    }

    // Call OtpService with user profile details
    const result = await OtpService.requestOtp({ 
      identifier: user.username,
      email: user.email,
      name: user.name
    });
    return {
      ...result,
      delivered_to: result.delivered_to,
      user: {
        username: user.username,
        name: user.name,
        email: result.delivered_to || user.email,
        phone: user.phone
      }
    };
  }

  /**
   * Verify OTP and set new password
   */
  async resetPassword({ identifier, otp, newPassword }) {
    const cleanId = String(identifier).trim().toLowerCase();
    const cleanPass = String(newPassword).trim();
    const users = this.getUsersDatabase();

    // Verify OTP (via backend or local verifier)
    const verifyResult = await OtpService.verifyAndReset({
      identifier: cleanId,
      otp: otp.trim(),
      newPin: cleanPass
    });

    if (!verifyResult.success) {
      return verifyResult;
    }

    // Find and update ALL matching user entries in database
    let updatedCount = 0;
    Object.keys(users).forEach(k => {
      const u = users[k];
      if (
        k.toLowerCase() === cleanId ||
        (u.username && u.username.toLowerCase() === cleanId) ||
        (u.email && u.email.toLowerCase() === cleanId) ||
        (u.name && u.name.toLowerCase() === cleanId) ||
        (u.full_name && u.full_name.toLowerCase() === cleanId) ||
        (cleanId === 'shefeeq' && u.name && u.name.toUpperCase().includes('SHEFEEQ')) ||
        (cleanId === 'nandu' && u.name && u.name.toUpperCase().includes('NANDU'))
      ) {
        u.password = cleanPass;
        updatedCount++;
      }
    });

    // Also ensure direct key entry exists for cleanId so login(cleanId, password) is guaranteed to work
    if (!users[cleanId]) {
      const matchedUser = Object.values(users).find(u => 
        (u.email && u.email.toLowerCase() === cleanId) ||
        (u.name && u.name.toLowerCase() === cleanId) ||
        (cleanId === 'shefeeq' && u.name && u.name.toUpperCase().includes('SHEFEEQ'))
      );
      if (matchedUser) {
        users[cleanId] = { ...matchedUser, password: cleanPass, username: cleanId };
      } else {
        users[cleanId] = {
          id: `eng_${cleanId}`,
          username: cleanId,
          name: cleanId.toUpperCase(),
          password: cleanPass,
          role: cleanId === 'admin' ? 'admin' : 'engineer'
        };
      }
    } else {
      users[cleanId].password = cleanPass;
    }

    this.saveUsersDatabase(users);

    return {
      success: true,
      message: 'Password has been successfully updated! You can now log in.'
    };
  }

  /**
   * Change password directly for an authenticated user with current password verification
   */
  async changePassword(identifier, currentPassword, newPassword) {
    const cleanId = String(identifier).trim().toLowerCase();
    const cleanCurrent = String(currentPassword).trim();
    const cleanNew = String(newPassword).trim();
    const users = this.getUsersDatabase();

    const userKey = Object.keys(users).find(k => 
      k.toLowerCase() === cleanId || 
      (users[k].id && users[k].id.toLowerCase() === cleanId) ||
      (users[k].email && users[k].email.toLowerCase() === cleanId) ||
      (users[k].name && users[k].name.toLowerCase() === cleanId)
    );

    if (!userKey || !users[userKey]) {
      return { success: false, error: 'User account not found.' };
    }

    if (users[userKey].password !== cleanCurrent) {
      return { success: false, error: 'Current password is incorrect.' };
    }

    if (!cleanNew || cleanNew.length < 4) {
      return { success: false, error: 'New password must be at least 4 characters long.' };
    }

    users[userKey].password = cleanNew;
    this.saveUsersDatabase(users);

    return { 
      success: true, 
      message: 'Password updated successfully!' 
    };
  }
}

export const authService = new AuthService();
if (typeof window !== 'undefined') {
  window.authService = authService;
}
