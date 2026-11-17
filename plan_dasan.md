# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `COMPLETED` (Sprint v1.31.0 - Build 129 Preloaded dummy templates purged, dynamic @name personalization live, clean slate verified)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Dasa! Nammude Broadcast Console-il preloaded dummy templates remove cheythu, user create cheyyunna **pure custom templates** maathram retain cheyyaanum, dynamic `@name` recipient personalization support cheyyaanumulla blueprint Gafoorka set aakki vechittundu.

### 🎯 Key Directives for Dasan:
1. **Purge Preloaded Dummy Templates & Defaults (`server.py`)**:
   - In `GET /api/broadcast/templates`:
     - **Remove** auto-upserting `DEFAULT_BROADCAST_TEMPLATES`.
     - In MongoDB `ATLAS_DB.broadcast_templates`, purge any default preloaded templates with legacy titles (`"Shift Operations Announcement"`, `"Urgent Operational Handover"`, `"Scheduled System Maintenance"`, `"Shift Coverage Swap Request"`).
     - Return only user-created custom templates: `{ "success": True, "templates": user_templates, "count": len(user_templates) }`. If none exist, return empty list `[]`.
     - Ensure NO mock text, preloaded defaults, or personal names are seeded.
2. **Support Saving Custom Templates**:
   - Ensure `POST /api/broadcast/templates` allows Admins and Managers to save templates with:
     - `name`: string (e.g. template title or name)
     - `title`: string
     - `message`: string
     - `priority`: string (optional, defaults to "broadcast")
     - `target`: string/dict (optional)
     - Returns `{ "success": True, "message": "Template saved successfully", "template": ... }`.
   - Ensure `POST /api/broadcast/templates/delete` properly deletes a custom template by `id`.
3. **Dynamic `@name` Personalization Support**:
   - When a broadcast notification is dispatched to multiple users or everyone:
     - If the broadcast message contains `@name` (or `{name}`):
       - Personalize each recipient's notification & email by replacing `@name` with that specific recipient's first name / display name (e.g. `Dear Shefeeq,` instead of raw `@name`).
4. **Version Bump**:
   - Update `SERVER_VERSION = "1.31.0"` in `server.py`.

---

## 2. Technical Blueprint for Dasan

### 📁 Primary File:
- `server.py`

### 🛠️ Execution & Verification:
1. Update `GET /api/broadcast/templates` to stop injecting `DEFAULT_BROADCAST_TEMPLATES`.
2. Delete legacy default templates from `ATLAS_DB.broadcast_templates`.
3. Test template CRUD: create a custom template, fetch templates list, delete template.
4. Test `@name` token replacement in notification broadcast engine.
5. Commit changes and update `AGENT_HANDOFF.md`.
