/**
 * RosterX - MongoDB Atlas Native Bridge & Sync Service
 * 
 * Supports:
 * 1. Native Python PyMongo REST API bridge (/api/roster, /api/engineers, /api/test-connection)
 * 2. LocalStorage offline cache with full BSON/JSON parity.
 * 3. Connection state monitoring ("Synced to Atlas M0", "Unsaved Changes", "Offline / Mock mode").
 * 4. Automatic month switching sync and live publishing.
 */

import { ENGINEERS } from './constants.js?v=1.0';

const STORAGE_KEYS = {
  CONFIG: 'rosterx_atlas_config',
  ENGINEERS: 'rosterx_engineers_list',
  ENGINEERS_LEGACY: 'rosterx_engineers_cache',
  ROSTERS: 'rosterx_rosters_cache',
  ACTIVE_ROSTER_ID: 'rosterx_active_roster_id'
};

export class AtlasService {
  constructor() {
    this.config = this.loadConfig();
    this.status = 'offline'; // 'synced' | 'unsaved' | 'offline' | 'draft'
    this.listeners = [];
  }

  getApiBase() {
    if (typeof window !== 'undefined' && window.location && window.location.protocol.startsWith('http')) {
      const currentOriginApi = `${window.location.origin}/api`.replace(/\/+$/, '');
      let url = (this.config.endpointUrl || '').trim().replace(/\/+$/, '');

      // Auto-heal condition:
      // If no url, or points to mongo data-api, or points to a trycloudflare.com tunnel that doesn't match current origin,
      // or if running on localhost/127.0.0.1 while url points to an external host
      const isStaleTunnel = url.includes('trycloudflare.com') && !url.startsWith(window.location.origin);
      const isLocalhostMisaligned = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && 
        url && !url.includes('localhost') && !url.includes('127.0.0.1');

      if (!url || url.includes('data.mongodb-api.com') || isStaleTunnel || isLocalhostMisaligned) {
        url = currentOriginApi;
        this.config.endpointUrl = url;
        this.saveConfig(this.config);
      }
      return url;
    }
    return (this.config.endpointUrl || 'http://localhost:8000/api').replace(/\/+$/, '');
  }

  loadConfig() {
    const origin = (typeof window !== 'undefined' && window.location && window.location.origin)
      ? window.location.origin
      : 'http://localhost:8000';

    const defaultCfg = {
      appId: '',
      endpointUrl: `${origin}/api`,
      apiKey: '',
      clusterName: 'Cluster0',
      database: 'rosterx',
      mongoUri: '',
      isLiveConnected: true
    };

    try {
      const stored = localStorage.getItem(STORAGE_KEYS.CONFIG);
      if (stored) {
        const parsed = JSON.parse(stored);
        const url = parsed.endpointUrl || '';
        const isStaleTunnel = url.includes('trycloudflare.com') && typeof window !== 'undefined' && window.location && !url.startsWith(window.location.origin);
        const isLocalhostMisaligned = typeof window !== 'undefined' && window.location && 
          (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') && 
          url && !url.includes('localhost') && !url.includes('127.0.0.1');

        if (!url || url.includes('data.mongodb-api.com') || isStaleTunnel || isLocalhostMisaligned) {
          parsed.endpointUrl = `${origin}/api`;
        }
        return { ...defaultCfg, ...parsed, isLiveConnected: true };
      }
      return defaultCfg;
    } catch {
      return defaultCfg;
    }
  }

  saveConfig(newConfig) {
    this.config = { ...this.config, ...newConfig };
    localStorage.setItem(STORAGE_KEYS.CONFIG, JSON.stringify(this.config));
    this.notifyStatusChange();
  }

  onStatusChange(callback) {
    this.listeners.push(callback);
  }

  notifyStatusChange(status = this.status) {
    this.status = status;
    this.listeners.forEach(cb => cb(this.status, this.config));
  }

  /**
   * Test connection to Atlas via native Python bridge (/api/test-connection)
   */
  async testConnection() {
    const base = this.getApiBase();
    try {
      const response = await fetch(`${base}/test-connection`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      });

      const data = await response.json();
      if (response.ok && data.success) {
        this.config.isLiveConnected = true;
        this.saveConfig(this.config);
        this.notifyStatusChange('synced');
        return {
          success: true,
          message: `Atlas ${data.cluster || this.config.clusterName || 'Cluster0'} Connected & Verified!`
        };
      } else {
        return {
          success: false,
          message: data.message || `Atlas status check failed (HTTP ${response.status})`
        };
      }
    } catch (err) {
      return {
        success: false,
        message: `Could not reach backend bridge at ${base}: ${err.message}`
      };
    }
  }

  /**
   * Helper to merge two engineer lists while preserving updated fields
   */
  mergeEngineers(primaryList, fallbackList) {
    const map = new Map();
    // 1. Put fallback defaults first
    (fallbackList || []).forEach(eng => {
      if (eng && (eng.id || eng.name)) {
        const key = (eng.name || eng.id).trim().toUpperCase();
        map.set(key, { ...eng });
      }
    });
    // 2. Overlay primary list from DB/cache
    (primaryList || []).forEach(eng => {
      if (eng && (eng.id || eng.name)) {
        const key = (eng.name || eng.id).trim().toUpperCase();
        const existing = map.get(key) || {};
        map.set(key, { ...existing, ...eng });
      }
    });
    return Array.from(map.values());
  }

  /**
   * Retrieve list of all engineers (from Atlas /api/engineers or offline cache)
   */
  async getEngineers() {
    const base = this.getApiBase();
    try {
      const res = await fetch(`${base}/engineers`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });
      if (res.ok) {
        const data = await res.json();
        if (data.documents && Array.isArray(data.documents) && data.documents.length > 0) {
          const merged = this.mergeEngineers(data.documents, ENGINEERS);
          localStorage.setItem(STORAGE_KEYS.ENGINEERS, JSON.stringify(merged));
          localStorage.setItem(STORAGE_KEYS.ENGINEERS_LEGACY, JSON.stringify(merged));
          return merged;
        }
      }
    } catch (err) {
      console.warn('Atlas live fetch engineers failed, checking relative fallback:', err);
      // Secondary relative origin fallback
      if (typeof window !== 'undefined' && window.location && window.location.origin) {
        const rel = `${window.location.origin}/api/engineers`;
        if (`${base}/engineers` !== rel) {
          try {
            const r2 = await fetch(rel, { headers: { 'Accept': 'application/json' } });
            if (r2.ok) {
              const d2 = await r2.json();
              if (d2.documents && Array.isArray(d2.documents) && d2.documents.length > 0) {
                const merged = this.mergeEngineers(d2.documents, ENGINEERS);
                localStorage.setItem(STORAGE_KEYS.ENGINEERS, JSON.stringify(merged));
                localStorage.setItem(STORAGE_KEYS.ENGINEERS_LEGACY, JSON.stringify(merged));
                return merged;
              }
            }
          } catch (e2) {}
        }
      }
    }

    // Fallback to LocalStorage cache or Initial Seed Mock
    try {
      const cached = localStorage.getItem(STORAGE_KEYS.ENGINEERS) || localStorage.getItem(STORAGE_KEYS.ENGINEERS_LEGACY);
      if (cached) {
        const parsed = JSON.parse(cached);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return this.mergeEngineers(parsed, ENGINEERS);
        }
      }
    } catch (e) {
      console.warn('Error reading local engineers cache:', e);
    }

    const defaultSeed = JSON.parse(JSON.stringify(ENGINEERS));
    localStorage.setItem(STORAGE_KEYS.ENGINEERS, JSON.stringify(defaultSeed));
    localStorage.setItem(STORAGE_KEYS.ENGINEERS_LEGACY, JSON.stringify(defaultSeed));
    return defaultSeed;
  }

  /**
   * Sync engineers list to Atlas (/api/engineers)
   */
  async saveEngineers(engineersList) {
    if (!Array.isArray(engineersList) || engineersList.length === 0) return { success: false, message: 'No engineers to save' };
    
    // Save locally immediately
    localStorage.setItem(STORAGE_KEYS.ENGINEERS, JSON.stringify(engineersList));
    localStorage.setItem(STORAGE_KEYS.ENGINEERS_LEGACY, JSON.stringify(engineersList));

    const base = this.getApiBase();
    try {
      const res = await fetch(`${base}/engineers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ engineers: engineersList })
      });
      const data = await res.json();
      return data;
    } catch (err) {
      console.warn('Failed to sync engineers to Atlas:', err);
      return { success: false, message: err.message };
    }
  }

  /**
   * Fetch roster by ID (e.g., '2026-10' or '2026-11') from /api/roster/<id>
   */
  async getRoster(rosterId) {
    const base = this.getApiBase();
    // 1. Attempt primary API base
    try {
      const res = await fetch(`${base}/roster/${rosterId}`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });
      if (res.ok) {
        const data = await res.json();
        const doc = data.document || data.data || data;
        if (doc && (doc.engineers || doc._id)) {
          this.config.isLiveConnected = true;
          this.saveLocalRoster(doc);
          this.notifyStatusChange('synced');
          return doc;
        }
      }
    } catch (err) {
      console.warn('Atlas live fetch roster failed on base ' + base + ':', err);
    }

    // 2. Relative Origin Fallback (if base was different from current origin)
    if (typeof window !== 'undefined' && window.location && window.location.origin) {
      const relativeBase = `${window.location.origin}/api`;
      if (base !== relativeBase) {
        try {
          const res = await fetch(`${relativeBase}/roster/${rosterId}`, {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
          });
          if (res.ok) {
            const data = await res.json();
            const doc = data.document || data.data || data;
            if (doc && (doc.engineers || doc._id)) {
              this.config.endpointUrl = relativeBase;
              this.config.isLiveConnected = true;
              this.saveConfig(this.config);
              this.saveLocalRoster(doc);
              this.notifyStatusChange('synced');
              return doc;
            }
          }
        } catch (e2) {
          console.warn('Fallback fetch to relativeBase ' + relativeBase + ' failed:', e2);
        }
      }
    }

    // 3. LocalStorage cache fallback
    try {
      const rostersMap = JSON.parse(localStorage.getItem(STORAGE_KEYS.ROSTERS) || '{}');
      const cached = rostersMap[rosterId];
      if (cached && Array.isArray(cached.engineers) && cached.engineers.length >= 5) {
        return cached;
      }
    } catch (e) {
      console.warn('Error reading local rosters cache:', e);
    }

    return null;
  }

  /**
   * Save roster document to Atlas (/api/roster) & LocalStorage
   */
  async saveRoster(rosterDoc) {
    // 1. Always persist in LocalStorage first
    this.saveLocalRoster(rosterDoc);
    const base = this.getApiBase();

    try {
      const res = await fetch(`${base}/roster`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(rosterDoc)
      });

      const data = await res.json();
      if (res.ok && data.success) {
        this.notifyStatusChange('synced');
        return { success: true, message: data.message || 'Synced to Atlas M0' };
      } else {
        this.notifyStatusChange('unsaved');
        return { success: false, message: data.message || `Atlas returned HTTP ${res.status}` };
      }
    } catch (err) {
      this.notifyStatusChange('unsaved');
      return { success: false, message: `Saved locally, network error during Atlas sync: ${err.message}` };
    }
  }

  saveLocalRoster(rosterDoc) {
    const rostersMap = JSON.parse(localStorage.getItem(STORAGE_KEYS.ROSTERS) || '{}');
    rostersMap[rosterDoc._id] = rosterDoc;
    localStorage.setItem(STORAGE_KEYS.ROSTERS, JSON.stringify(rostersMap));
  }

  /**
   * Fetch project recovery snapshots for a month from /api/roster-backups/<monthId>
   */
  async getRosterBackups(monthId) {
    const base = this.getApiBase();
    try {
      const res = await fetch(`${base}/roster-backups/${monthId}`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });
      if (res.ok) {
        const data = await res.json();
        return data.backups || [];
      }
    } catch (err) {
      console.warn('Failed to fetch roster backups:', err);
    }
    return [];
  }

  /**
   * Restore a prior published roster snapshot via /api/roster-backups/restore
   */
  async restoreRosterBackup(backupId, monthId) {
    const base = this.getApiBase();
    try {
      const res = await fetch(`${base}/roster-backups/restore`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ backup_id: backupId, month_id: monthId })
      });
      const data = await res.json();
      if (res.ok && data.success && data.document) {
        this.saveLocalRoster(data.document);
        this.notifyStatusChange('synced');
        return { success: true, message: data.message, document: data.document };
      }
      return { success: false, message: data.message || `Restore failed with HTTP ${res.status}` };
    } catch (err) {
      return { success: false, message: `Restore network error: ${err.message}` };
    }
  }

  /**
   * Export database JSON backup file
   */
  exportJsonBackup(rosterDoc) {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(rosterDoc, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', `rosterx_${rosterDoc._id}_mongodb_export.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  }

  /**
   * Import JSON file
   */
  async importJsonBackup(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = async (e) => {
        try {
          const parsed = JSON.parse(e.target.result);
          if (!parsed._id || !Array.isArray(parsed.engineers)) {
            throw new Error('Invalid RosterX JSON format: Missing _id or engineers array.');
          }
          await this.saveRoster(parsed);
          resolve(parsed);
        } catch (err) {
          reject(err);
        }
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsText(file);
    });
  }
}

