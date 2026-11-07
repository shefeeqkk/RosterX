# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-07 12:50 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.13`** (Android `versionCode: 56`, Server `SERVER_VERSION = "1.14.13"`)
- **Previous Versions**: `1.14.12` (Build 55), `1.14.11` (Build 54), `1.14.10` (Build 53), `1.14.9` (Build 52), `1.14.8` (Build 51), `1.14.7` (Build 50), `1.14.6` (Build 49), `1.14.5` (Build 48), `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Operational Upgrades in v1.14.13

### 1. 3D Cutout Bot Avatar Parity in AI Chat
- Replaced retro 2D emoji `Text("🤖")` across the entire Android Chat Assistant with the official cutout 3D Robot icon (`ic_bot_3d_cutout`):
  - In the **Chat Header** (44dp with radial dark glow and cyan border accent).
  - In each **Assistant Message Bubble** avatar (32dp circle with soft cyan glow).
  - In the **Thinking / Loading Bubble** avatar.
- Added 8 interactive quick-prompt chips with icons ("⚡ On duty now?", "🌅 SG tomorrow?", "🌙 NS tomorrow?", "🏖️ Off tomorrow?").

### 2. Deep Intelligence & Multi-Model Grounding for All Message Types
- Embedded default production Gemini API key fallback in `server.py` ensuring Render Cloud always calls active `gemini-3.1-flash-lite` and `gemini-3-flash-preview`.
- Added comprehensive deterministic engine intent parsing for:
  - "Who is in [SG/FS/FG/G/SS/NS] tomorrow / today / on [date]"
  - "Who is available for [shift] tomorrow"
  - "Who is off / on leave tomorrow"
  - "Who has duty / coverage tomorrow"
- Intelligent situational fallback: When non-standard questions are asked, dynamically summarizes user's today & tomorrow shifts and on-duty personnel rather than repeating generic help text.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Bump**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.13"` and `versionCode = 56`.
2. **Chat Avatar Parity**:
   - `RosterXAssistantBottomSheet.kt` now displays `ic_bot_3d_cutout` across headers and message bubbles.
3. **Backend Live**:
   - `/api/chat/assistant` is powered by Gemini 3.1 Flash Lite and intelligent Atlas RAG.


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
