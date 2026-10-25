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
    let url = this.config.endpointUrl || '';
    if (!url || url.includes('data.mongodb-api.com')) {
      const origin = (typeof window !== 'undefined' && window.location && window.location.origin)
        ? window.location.origin
        : 'http://localhost:8000';
      url = `${origin}/api`;
      this.config.endpointUrl = url;
      this.saveConfig(this.config);
    }
    return url.replace(/\/+$/, '');
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
        if (!parsed.endpointUrl || parsed.endpointUrl.includes('data.mongodb-api.com')) {
          parsed.endpointUrl = `${origin}/api`;
        }
        return { ...defaultCfg, ...parsed };
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
    if (this.config.isLiveConnected) {
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
        console.warn('Atlas live fetch engineers failed, fallback to local cache:', err);
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
    if (this.config.isLiveConnected) {
      try {
        const res = await fetch(`${base}/roster/${rosterId}`, {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        });
        if (res.ok) {
          const data = await res.json();
          const doc = data.document || data.data || data;
          if (doc && (doc.engineers || doc._id)) {
            this.saveLocalRoster(doc);
            this.notifyStatusChange('synced');
            return doc;
          }
        }
      } catch (err) {
        console.warn('Atlas live fetch roster failed, fallback to local cache:', err);
      }
    }

    // LocalStorage cache fallback
    try {
      const rostersMap = JSON.parse(localStorage.getItem(STORAGE_KEYS.ROSTERS) || '{}');
      if (rostersMap[rosterId] && Array.isArray(rostersMap[rosterId].engineers) && rostersMap[rosterId].engineers.length >= 5) {
        return rostersMap[rosterId];
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
