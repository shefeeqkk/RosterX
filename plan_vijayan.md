# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `COMPLETED` (Sprint v1.29.5 - Build 127)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! User nammude Edit Account form-il 4 critical refinements request cheythittundu:
1. **Eliminate Bottom Shadow Clipping**: Action buttons (`Save changes`, `Cancel`, `Delete`)-nte tactile drop shadow bottom edge-il cut aavunnath maattaan dialog card-il bottom padding (`22.dp`), outer vertical margin (`20.dp`), and `Spacer(14.dp)` buffer add cheythu shadow diffusion safe aakkuka.
2. **Remove Photo Confirmation Dialog**: `• Remove photo` tap cheythaal direct remove aavunnathinu pakaram confirmation dialog ("Remove profile photo?") render cheythu [Cancel] / [Remove] actions provide cheyyeanam.
3. **Frosted Background Scrim Tint**: Android OEM cross-window blur disabled aakkiyittulla devices-ilum (e.g. Vivo/iQOO FuntouchOS) background text distracting aayi kaanathe, soft frosted glass veil (`Color(0xB8F1F5F9)` light / `Color(0x990A0F1A)` dark) + `0.45f` dimming implement cheythu frosted backdrop experience create cheyyuka.
4. **Resignation Date Input upon choosing Resigned**: `Employment status`-il `Resigned` choose cheythaal dynamic slide-down animation-il `Resignation date *` calendar picker input varanam, empty aayirunnal today's date default aayi populate aavanam, and mandatory validation & backend persistence set cheyyeanam.

### 🎯 Key Design Directives (Matching Image 2 Reference):
1. **Compact & Less Scrolling**:
   - Replace the tall single-column stacked forms with intelligent mixed layout based on character length:
     - Short fields (Full Name & Username, Phone & Password, Join Date & Birthday) paired in **Dual Columns (50% / 50%)**.
     - Long fields (Corporate Email, Role Strip, Spokespersons) in **Single Columns (100%)**.
2. **Authentic Neumorphic Styling**:
   - Sunken / Inset text input basins (`Modifier.neumorphicInset(...)` with subtle inner occlusion and specular highlight).
   - Molded floating card surfaces (`NeumorphicTokens.Surface`).
   - Tactile Neumorphic buttons with active press kinetics.
3. **Scope of Forms**:
   - **Form 1: Edit Account & Register Engineer Dialog** (`ManageTeamTab.kt` & `ProfileComponents.kt`).
   - **Form 2: Login Screen & Reset Password Dialog** (`LoginScreen.kt`).
   - **Form 3: Add/Edit Credential Tracker Dialog** (`CredentialTrackerScreen.kt`).

---

## 2. Scope of Work for Vijayan

### 1. Reusable Compact Neumorphic Input Component (`NeumorphicFormInput.kt`)
- Create `android/app/src/main/java/com/rosterx/presentation/components/NeumorphicFormInput.kt`:
  - Label: `Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeumorphicTokens.TextMuted)`.
  - Sunken basin:
    ```kotlin
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicInset(shape = RoundedCornerShape(12.dp), elevation = NeumorphicTokens.ElevationInsetSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(...)
    }
    ```
  - Trailing icon support (e.g., eye toggle for passwords, calendar icon for dates).

### 2. Form 1: Edit Account & Register Engineer Dialog (`ManageTeamTab.kt` & `ProfileComponents.kt`)
Overhaul `EngineerDialog` in `ManageTeamTab.kt` and `UnifiedEditProfileDialog` in `ProfileComponents.kt` to mirror Image 2:
- **Card**: Floating Neumorphic surface with `RoundedCornerShape(24.dp)` and `padding(18.dp)`.
- **Header**:
  - Left: Circular `UserAvatar` (size `44.dp`) with camera badge icon overlay `📷`. Tapping triggers photo picker.
  - Middle: Title `"Edit account"` / `"Register new account"` (`18.sp`, ExtraBold), Subtitle `nameInput` / `"Create system access"` (`11.sp`, `TextMuted`).
  - Right: Theme toggle / close icon button.
- **Row 1 (Dual-Column 50/50)**:
  - Left: `Full name *` (`NeumorphicFormInput`)
  - Right: `Username *` (`NeumorphicFormInput`)
- **Row 2 (Single-Column 100%)**:
  - `Corporate email *` (full width for long addresses).
- **Row 3 (Dual-Column 50/50)**:
  - Left: `Phone` (`NeumorphicFormInput`)
  - Right: `New password` (with eye toggle).
- **Row 4 (Dual-Column 50/50)**:
  - Left: `Join date` (with date picker / calendar icon).
  - Right: `Birthday` (with date picker / cake icon).
- **Row 5 (Single-Column 100% - Role Selector)**:
  - Label: `"Role and access"`
  - Segmented Neumorphic recessed container with tactile raised pill buttons for:
    `[Engineer]  [Manager]  [Admin]  [Apprent.]`
    Active pill styled with brand Blue / Salmon accent.
- **Row 6 (Action Row)**:
  - Left: Square Neumorphic delete button (`[🗑️]`) with red tint (visible when editing existing account).
  - Middle: Neumorphic `[Cancel]` button.
  - Right: Neumorphic Primary `[✓ Save changes]` button with checkmark icon and solid accent styling.

### 3. Form 2: Login Screen (`LoginScreen.kt`)
Transform `LoginScreen.kt` from flat Material 3 to compact Neumorphic styling:
- Floating Neumorphic Card: `Modifier.neumorphicFloating(shape = RoundedCornerShape(24.dp))` on `NeumorphicTokens.Canvas`.
- Compact vertical sizing so the entire screen fits without vertical scrolling on mobile viewports:
  - Logo (`56.dp`), `"Welcome to RosterX"` (`22.sp`, Bold), subtitle (`11.sp`).
  - Username: Inset Neumorphic input with Person icon + Key icon for Google Password Manager.
  - Password: Inset Neumorphic input with Lock icon + Eye toggle icon.
  - **Standard "Forgot password?" Link**:
    - Change legacy text `"Forgot Username / Need Help?"` to standard `"Forgot password?"`.
    - Position it **right-aligned immediately below the Password field** (`Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End)`).
    - Tapping opens the Neumorphic OTP Password Reset dialog.
  - Primary **"Sign In"** button: Elevated Neumorphic button with smooth press animation (`neumorphicClickable`).
- Server Settings Dialog & OTP Password Reset Dialog:
  - Converted to matching Neumorphic dialogs with inset inputs.

### 4. Form 3: Add/Edit Credential Tracker Dialog (`CredentialTrackerScreen.kt`)
Overhaul `AddEditCredentialTrackerDialog`:
- Dual-Column Row 1: `Server Name *` | `Username / Account *`
- Dual-Column Row 2: `Validity Days *` (manual input) | `Alert Threshold Days *` (manual input)
- Single-Column Row 3: `Last Updated Date *` (with calendar picker & 'Today' chip)
- Single-Column Row 4: `Designated Spokesperson Emails` (chips + teammate picker)
- Action Row: Delete button (if editing), Cancel button, and Save Tracker button.

### 5. Post-Build Activities (Mandatory)
- Version bump in `android/app/build.gradle.kts`:
  - `versionName = "1.29.0"`
  - `versionCode = 122`
- Version bump in `server.py`:
  - `SERVER_VERSION = "1.29.0"`
- Execute safe build:
  - `cd android && .\safe_build.bat assembleDebug`
- Install on device via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Verification:
  - Test Login Screen: Verify compact, inset fields, Google Password Manager autofill, and zero scrolling.
  - Test Edit Account Dialog in Manage Team: Verify dual-column layout, compact height, and visual parity with Image 2.
  - Test Credential Tracker Dialog: Verify dual-column inputs and smooth Neumorphic styling.
- Commit changes and update `AGENT_HANDOFF.md`.
