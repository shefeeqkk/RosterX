# RosterX Android Mobile App — Release v1.4.5 (Build 15)

## 📌 Release Summary
- **App Name**: RosterX NOC Management & Roster System
- **Version**: `1.4.5`
- **Build / VersionCode**: `15`
- **Target OS**: Android 8.0 (API 26) through Android 15 (API 35)
- **Architecture**: Modern Jetpack Compose + Clean MVVM Architecture + Hilt DI + Material 3
- **File**: `RosterX-v1.4.5.apk`
- **Server Compatibility**: RosterX Backend Server `v1.4.5`+

---

## 🚀 Key Highlights & New Features

### 1. 🛡️ Smart Biometric & Credential Protection
- **Hardware-Backed Biometrics**: Fingerprint and Face Unlock support integrated via AndroidX Biometric API.
- **Admin Privilege Safeguard**: Automatically enforces strict master password entry for all accounts with administrator privileges—preventing unauthorized device-level biometric overrides on sensitive roster mutations.

### 2. 📢 Rich Broadcast Notification Center
- **Engineer @Mentions & Personalized Alerts**: Supports targeting individual team members with `@engineer_id` tags or broadcasting to all active team engineers.
- **Dismissible Dashboard Banners**: Engineers can dismiss persistent broadcast announcements once read; state is preserved locally and synchronizes smoothly with the server.
- **High-Priority NOC Alerts**: Urgent network announcements feature vivid, contextual warning icons and color cues.

### 3. 👥 Resilient Team & Engineer Management
- **In-App Engineer Modification**: Seamless editing of engineer names, designations, phone numbers, email addresses, and active/resigned operational status.
- **Direct Engineer Offboarding / Deletion**: Integrated deletion with confirmation dialogs directly inside the edit modal for rapid team roster updates.
- **Auto-Sync Across Mobile & Web**: Automatic state consistency between the Android client, Admin Web portal, and backend MongoDB Atlas instance.

### 4. 📅 Smart Monthly Roster & Intelligent Shift Filtering
- **Status-Aware Roster Rendering**: Resigned and inactive engineers are automatically filtered from active scheduling views according to unified operational rules.
- **Daily Shift Briefings**: Quick-glance cards showing Morning, General, Afternoon, and Night shift allocations for NOC operations.
- **Interactive Calendar Navigation**: Smooth month/year switching with offline-resilient local cache fallback.

### 5. ⚙️ Streamlined Settings & Diagnostic Visibility
- **Dedicated Settings Hub**: Simplified navigation consolidating application preferences, security controls, and endpoint configurations.
- **App & Architecture Diagnostics**: Clear, transparent build details, version numbering (`1.4.5`), and active API connection indicators in both Admin and User views.

---

## 🛠️ Complete Changelog (`v1.4.0` ➔ `v1.4.5`)

### Added
- **Dynamic @Mention System**: Added smart tag parsing (`@all`, `@<engineer_id>`) for broadcast alerts with instant team distribution.
- **Dashboard Notification Card**: Added dismissible dashboard broadcast card with local read state management.
- **Offboarding Flow in Edit Modal**: Added "Delete Engineer" button with alert verification inside `ManageTeamTab`.
- **Release Automation**: Pre-configured signed APK assembly pipeline with automated lint verification bypass.

### Fixed
- **State Synchronization on Edit**: Fixed Kotlin Compose state remembering bug in `ManageTeamTab` dialogs where previous engineer data persisted upon reopening.
- **Resigned Engineer Filtering**: Fixed discrepancy where resigned engineers appeared in the mobile monthly calendar but were hidden on web.
- **Engineer Matching on Update**: Normalized case-insensitive lookup across `id`, `engineer_id`, `username`, and `name`.
- **Biometric Bypass Guard**: Extended biometric disabling logic to any authenticated user with admin roles, not just the default admin user.

---

## 📲 Installation Instructions
1. Download `RosterX-v1.4.5.apk` to your Android device (or transfer via USB / adb).
2. If prompted, allow **"Install unknown apps"** from your browser or file manager.
3. Tap **Install** and open **RosterX**.
4. The app will automatically connect to your configured NOC server gateway or Cloudflare tunnel.
