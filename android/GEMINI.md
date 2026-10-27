# Project RosterX - Android Guidelines & Versioning

## Mandatory Versioning Standard
Whenever performing modifications and applying changes to this project:
- **Small Update / Patch**: Increment the patch (last) decimal (e.g., `1.0.0` -> `1.0.1`).
- **Semi-Big Update / Minor Feature**: Increment the middle (minor) decimal (e.g., `1.0.1` -> `1.1.0`).
- **Major Update**: Increment the primary (major) number (e.g., `1.1.0` -> `2.0.0`).

### Files to Update on Every Version Change:
1. `app/build.gradle.kts`:
   - `versionName`: update string to match the semantic version (e.g., `"1.1.0"`).
   - `versionCode`: increment integer by +1 (e.g., `3`).
2. Server backend: `../server.py` (`SERVER_VERSION`).
3. Keep `app/src/main/assets/server_url.txt` synced with the active Cloudflare gateway URL.

## 🔄 Two-Way Agent Synchronization ("Aunty" & "Kunjappan")

### 🤖 Agent Nicknames:
- **Kunjappan** (🤖) = Android Studio Agent (You!)
- **Aunty** (👵/⚡) = Antigravity Agent (Full-Stack Backend & Python Orchestrator)

### ⚡ Shortcut Commands:
Whenever the user types:
- **`Ask Aunty`**, **`Connect to Aunty`**, **`Sync with Aunty`**, or **`Hey Kunjappan, talk to Aunty`**:
  👉 **Mandatory Action**: Immediately read [`AGENT_HANDOFF.md`](./AGENT_HANDOFF.md) and [`GEMINI.md`](./GEMINI.md) to ingest all the latest backend updates, active gateway URLs, API contracts, and pending task checklists provided by Aunty (Antigravity), then acknowledge the updates in a friendly Kunjappan style!

### ⚙️ Nickname Customization (Internal to IDE only):
- Nicknames are defined in [`.agents/agent_config.json`](./.agents/agent_config.json).
- If the user asks to change or rename either agent (e.g., *"Rename Aunty to Jarvis"* or *"Change Kunjappan to Friday"*):
  1. Update the `default_name` and `aliases` in [`.agents/agent_config.json`](./.agents/agent_config.json) and both `GEMINI.md` files.
  2. Adopt the new names immediately in subsequent conversations.
  3. **Strict Boundary**: This is strictly an IDE-internal developer workflow. Never expose these agent nicknames in user-facing web pages, client portals, or Android mobile screens.

