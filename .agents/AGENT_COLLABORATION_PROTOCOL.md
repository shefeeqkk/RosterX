# 🛡️ Project RosterX - Multi-Agent Parallel Collaboration Protocol

> **Mandatory Standard for All Parallel Antigravity Conversations (Dasan & Vijayan)**  
> *Enforced to prevent build lock collisions, lost modifications, daemon crashes, and out-of-sync commits.*

---

## 1. 🔒 Concurrency Lock for Android Builds (`safe_build.bat`)
- **NEVER** run raw `./gradlew.bat assembleDebug` or `gradlew assembleDebug` directly!
- **ALWAYS** run through the unified mutex wrapper:
  ```bash
  # Windows PowerShell / CMD
  cd android && .\safe_build.bat assembleDebug
  ```
- **How It Works**:
  1. Checks `android/build.lock`.
  2. If another agent is actively building, it waits patiently with a live countdown (up to 4 minutes) instead of starting a colliding daemon.
  3. Registers process ID and active task name.
  4. Automatically cleans up dead locks if a process crashed.
  5. Always releases `android/build.lock` in a protected `finally` block upon completion.

---

## 2. 🌳 Git Pre-Flight Synchronization Protocol (Preventing Overwritten Work)
Whenever an agent begins a task or prepares to apply edits:
1. **Step 1 - Inspect Working Tree**:
   ```bash
   git status
   ```
   - If uncommitted files belong to another parallel agent, DO NOT overwrite them blindly.
2. **Step 2 - Atomic Commits**:
   - Every agent MUST commit completed features or bugfixes immediately with a semantic commit message:
     ```bash
     git add <modified-files>
     git commit -m "fix/feat(...): concise message (v1.x.y)"
     ```
   - Never leave dirty working tree modifications sitting across extended periods.
3. **Step 3 - Continuous Handoff Bridge (`AGENT_HANDOFF.md`)**:
   - Update `AGENT_HANDOFF.md` with:
     - The active task in progress.
     - Files modified.
     - Active version number.

---

## 3. 🎯 Clear Domain & Conversation Ownership Boundaries

To eliminate conflicting file writes across Antigravity conversations:

| Operational Domain | Lead Antigravity Agent | Key Files & Boundaries |
|---|---|---|
| **System Orchestration, Server & Build Lead** | **Dasan** (🕵️‍♂️ - Agent 1) | `server.py`, `safe_build.*`, `render.yaml`, MongoDB Atlas bridge, API routing, server logging & timezone formatting |
| **Android UI & Jetpack Compose Screens** | **Vijayan** (🕵️ - Agent 2) | `ProfileComponents.kt`, `ServerManagementTab.kt`, `EngineerPortalScreen.kt`, `Theme.kt`, Compose layouts |
| **Operations Radar & Personal Widgets** | **Shared (Coordinated via Lock)** | `RosterXAppWidgetProvider.kt`, `RosterXWidgetService.kt`, `widget_*.xml`, `RosterSyncWorker.kt` |

---

## 4. 🧠 Memory Calibration for 8GB Host Machines
- Keep Gradle daemon max heap bounded to `-Xmx2048m -XX:+UseG1GC`.
- Keep Kotlin compile daemon bounded to `-Xmx1024m -XX:+UseG1GC`.
- This ensures that even when browser tabs, ADB, and Python servers are running, the system physical RAM headroom never drops below safe OS thresholds.
