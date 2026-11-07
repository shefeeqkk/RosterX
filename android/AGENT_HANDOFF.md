# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-07 10:20 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.12`** (Android `versionCode: 55`, Server `SERVER_VERSION = "1.14.12"`)
- **Previous Versions**: `1.14.11` (Build 54), `1.14.10` (Build 53), `1.14.9` (Build 52), `1.14.8` (Build 51), `1.14.7` (Build 50), `1.14.6` (Build 49), `1.14.5` (Build 48), `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Operational Upgrades in v1.14.12

### 1. Web Portal Team Modal Redesign (Zero Duplicate Table)
- Transformed `#teamModal` into a dedicated, focused Account Form modal (`max-w-3xl`) for user registration and profile editing.
- Completely eliminated the redundant secondary user directory table inside the modal so clicking "Register Account" or "Edit Profile" immediately presents only the clean account form.

### 2. Android Retrofit Wildcard Reflection Fix
- Replaced generic `@Body Map<String, ...>` wildcard parameters with concrete DTO data classes:
  - `SyncEngineersRequest(engineers: List<EngineerDto>)`
  - `DeleteEngineerRequest(...)`
  - `DismissNotificationRequest(...)`
- Completely eliminates Android Retrofit runtime exception: `"Parameter type must not include a type variable or wildcard: java.util.Map"`.

### 3. Operations Dashboard & Coverage Radar Strict Sorting
- **Morning Shift**: Sorted strictly by shift start timing: `FS` (07:30) ➔ `FG` (08:30) ➔ `G` (09:30) ➔ `SG` (11:00), then alphabetically.
- **No Duty / Rest Days**: Sorted strictly by operational off priority: `OFF` ➔ `SW*` ➔ `L` ➔ others, then alphabetically.
- Parity verified across Web Admin console (`app.js`) and User Portal (`userPortal.js`).

### 4. Upgraded AI Assistant (Gemini 3.1 Flash Lite + Grounded RAG)
- Configured active `gemini-3.1-flash-lite` with instant multi-model fallback (`gemini-3-flash-preview`).
- Integrated full MongoDB Atlas real-time roster grounding with 100% accurate shift resolution (`SG` for Second General, `FG`, `FS`, `SS`, `NS`, `OFF`).
- Strict directive for ultra-brief, concise answers (1–2 sentences, zero telemetry walls).

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Bump**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.12"` and `versionCode = 55`.
2. **Retrofit Fix**:
   - `RosterApiService` and `RosterRepositoryImpl` now use concrete DTOs for engineer synchronization and deletion. Saving user edits in Manage Team now executes smoothly without wildcard reflection errors.
3. **AI Chat & Radar Sorting**:
   - Backend `/api/chat/assistant` is grounded in live Atlas M0 data and running Gemini 3.1 Flash Lite for quick, direct responses.

   - Asking queries like *"when is my second general shift"* now instantly returns direct, brief answers without unrequested status dumps!
3. **Wireless Deploy Ready**:
   - Run `.\deploy-wireless.ps1` from the `android/` directory to compile and deploy wirelessly in one step!
     - No Duty: `OFF, L, SW*`
3. **Dual Theme Tokens**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
