========================================================================
FOCUSBUDDY - Desktop Productivity & Digital-Wellbeing Application
========================================================================

FocusBuddy is a native Windows desktop productivity companion that helps you
stay focused during study and work sessions. It features a Pomodoro timer,
active application tracking, automated warnings and limits, productivity
statistics, and an animated desktop pink cat companion rendered in Java2D.

Built strictly with standard Java 17 (Java Swing, AWT, Java2D) and PowerShell.
Zero external libraries, zero Maven/Gradle overhead, zero internet dependency.


1. SYSTEM REQUIREMENTS
-----------------------
- Operating System: Windows 10 or Windows 11 (64-bit recommended)
- Java Runtime: JDK 17 or newer (with "java" and "javac" on your system PATH)
- PowerShell: Pre-installed standard Windows PowerShell 5.1+


2. QUICK START / HOW TO RUN
----------------------------
The easiest way to run FocusBuddy is using the provided run.bat script:

1. Open the project root folder.
2. Double-click run.bat (or execute .\run.bat in Command Prompt / PowerShell).
3. The script verifies Java 17+, creates data/ and out/ folders, compiles
   all source files, and starts the application.


3. MANUAL COMPILATION AND EXECUTION
------------------------------------
You can also compile and launch FocusBuddy manually from any terminal:

1. Open Command Prompt or PowerShell in the project directory:
   cd FocusBuddy

2. Compile all source files into the "out" directory:
   javac -encoding UTF-8 -d out src\*.java

3. Run FocusBuddy:
   java -cp out FocusBuddy

Main Class / Entry Point: FocusBuddy (src/FocusBuddy.java)


4. PROJECT ARCHITECTURE & FILES
--------------------------------
FocusBuddy/
│
├── src/
│   ├── FocusBuddy.java          - Application entry point & L&F initialization
│   ├── FocusBuddyUI.java        - Main dashboard & dialogs (Stats, Apps, Settings)
│   ├── PomodoroManager.java     - Pomodoro state machine (WORK/BREAK) & timer
│   ├── ProcessMonitor.java      - Windows foreground detection & safe taskkill
│   ├── AppTracker.java          - Per-app active usage tracking & threshold checks
│   ├── AppManager.java          - Monitored applications repository & protection
│   ├── AppInfo.java             - Monitored application data model & serialization
│   ├── FocusState.java          - Pomodoro state enum (WORK, SHORT_BREAK, LONG_BREAK)
│   ├── CatAnimation.java        - Java2D desktop pink cat companion & animations
│   ├── CatState.java            - Cat behavioral states enum (IDLE, WALK, TYPE, etc.)
│   ├── DarkButton.java          - Custom-painted modern buttons with pink accents
│   ├── StatsManager.java        - Productivity statistics tracking & persistence
│   ├── SettingsManager.java     - Configuration options & settings persistence
│   └── Utils.java               - Time formatting, validation, and debug logging
│
├── data/
│   ├── apps.txt                 - Monitored apps, time limits, and daily usage
│   ├── settings.properties      - Application configuration and preferences
│   └── stats.properties         - Daily focus and distraction metrics
│
├── README.txt                   - This documentation file
└── run.bat                      - Automated build and launch batch script


5. HOW ACTIVE APPLICATION TRACKING WORKS
-----------------------------------------
A critical requirement of FocusBuddy is tracking the application that you are
ACTUALLY USING in the foreground, not merely whether its process exists.

- How detection works:
  ProcessMonitor maintains a persistent background PowerShell bridge that calls
  Windows user32.dll (GetForegroundWindow and GetWindowThreadProcessId) via
  compiled C# P/Invoke types.
- Query speed:
  Unlike slow approaches that launch a new powershell.exe and recompile C# on every
  tick (taking 2+ seconds), FocusBuddy's query-response loop takes ~1.1 milliseconds!
- Active time only:
  If Google Chrome is in the foreground for 10 seconds, Chrome usage increases by
  10 seconds. If you switch to VS Code or IntelliJ, Chrome usage immediately stops.
  When you return to Chrome, usage resumes seamlessly.
- Case and extension normalization:
  Process names are normalized so that "chrome", "chrome.exe", and "CHROME.EXE"
  all match consistently.


6. WARNING AND TIME LIMIT ENFORCEMENT
--------------------------------------
- 60-Second Warning:
  When a monitored application reaches 60 seconds (or your configured threshold)
  remaining, FocusBuddy triggers a warning:
  - The cat companion transitions to ANGRY and displays an alert bubble:
    "Google Chrome: 1 MINUTE REMAINING!"
  - The warning is shown once per limit period.

- Time Limit Reached & The Kick Sequence:
  When usage reaches the configured daily limit (e.g. 30 minutes):
  1. The desktop cat notices the distracting application and turns ANGRY.
  2. The cat enters the KICK animation: winds up, raises its paw, and delivers
     a powerful kick with an on-screen "POW!" impact.
  3. At the moment of impact, ProcessMonitor sends a safe termination command
     to close the target process.
  4. The application is marked as BLOCKED, and repeated kills are prevented.
  5. FocusBuddy itself and unrelated programs remain running normally.


7. SAFE PROCESS TERMINATION
----------------------------
FocusBuddy includes strict protections to safeguard your system:
- Protected system processes can NEVER be monitored or terminated:
  focusbuddy.exe, java.exe, javaw.exe, explorer.exe, winlogon.exe, csrss.exe,
  services.exe, lsass.exe, dwm.exe, svchost.exe, system, system idle process,
  registry, smss.exe, wininit.exe.
- Self-termination prevention:
  FocusBuddy checks its own process ID (PID) and refuses to terminate itself.
- Only verified, enabled applications configured in Manage Apps are targeted.


8. POMODORO SYSTEM
-------------------
The Pomodoro finite-state machine follows the standard productivity cycle:
- WORK (25 min default) -> SHORT BREAK (5 min default)
- After every 4 completed WORK sessions -> LONG BREAK (15 min default)
- Controls: START, PAUSE, RESET, SKIP.
- Visual session counter and large countdown timer display.
- UI and counters update every second on the Swing Event Dispatch Thread.


9. FOCUS MODE
--------------
- The dashboard includes a dedicated "Focus Mode: ON / OFF" toggle button.
- When Focus Mode is ON:
  Active usage is tracked against distracting application limits.
- Setting: "Enforce Limits During Breaks":
  - If disabled (default): App limits are enforced only during WORK sessions.
  - If enabled: App limits continue to be enforced even during breaks.


10. DESKTOP PINK CAT COMPANION
-------------------------------
The desktop cat companion is drawn entirely in pure Java2D vector graphics:
- Color: Cute pink with soft blush and inner ear accents.
- Window: Borderless, draggable, always-on-top, translucent background.
- States & Animations:
  - IDLE: Gentle breathing body bob and tail sway.
  - WALK: Paws moving back and forth across the window.
  - THINK: Thinking pose with thought bubbles ("...").
  - HAPPY: Bouncing jump with (^◡^) curved eyes and raised tail.
  - ANGRY: Angled eyebrows and exclamation alert bubble.
  - KICK: Multi-step kick animation with "POW!" visual effect.
  - SLEEP: Curled up sleeping pose with floating "Zzz" when user is idle for 60s.
  - TYPE: Cat sitting at a mini laptop tapping paws alternately during WORK.
- Priority:
  KICK temporarily overrides all animations, switches briefly to ANGRY, then
  reverts smoothly to normal state.


11. MANAGING APPLICATIONS
--------------------------
Click "Manage Apps" on the dashboard to view and manage tracked programs:
- Table displays: Display Name, Process Name, Limit (min), Enabled, Current Usage,
  Warning Shown, Blocked.
- Default seeded apps:
  - Google Chrome (chrome.exe) - 30 min
  - Microsoft Edge (msedge.exe) - 30 min
  - Discord (discord.exe) - 45 min
  - WhatsApp (whatsapp.exe) - 20 min
- Actions: Add, Edit, Delete, Toggle Enable/Disable, Reset Usage.
- Any user-added Windows desktop program can be monitored.


12. PRODUCTIVITY STATISTICS
----------------------------
Click "Statistics" on the dashboard to open the Statistics screen:
- Today's Focus Time
- Completed Work Sessions
- Longest Focus Session
- Distracting App Usage
- Total Tracked App Usage
- Applications Blocked
- Detailed app breakdown table with per-app usage and percentage contribution.
- "Reset Statistics" button to clear daily data when needed.


13. SETTINGS
-------------
Click "Settings" to customize preferences:
- Pomodoro work and break durations
- Sessions before long break
- Warning threshold (seconds remaining)
- Monitoring interval (seconds)
- Auto Focus during WORK
- Enforce limits during breaks
- Show desktop cat, always-on-top, cat movement, full/reduced animations
- Debug mode (prints detailed tracking logs to console)


14. HOW TO PERFORM THE 2-MINUTE CHROME TEST
--------------------------------------------
To verify active tracking and automated kick enforcement:
1. Start FocusBuddy using run.bat.
2. Click "Manage Apps" -> select "Google Chrome" -> click "Edit".
3. Change the Time Limit to "2" minutes and click OK.
4. Ensure Focus Mode is ON.
5. Open Google Chrome and browse for 15 seconds.
   Notice the dashboard displays: "ACTIVE APP: Google Chrome (chrome.exe)" and
   Chrome's usage counter increases second by second.
6. Switch away to Notepad or VS Code for 15 seconds.
   Notice Chrome's usage stops increasing.
7. Return to Chrome.
   Usage resumes counting up.
8. At approximately 1 minute remaining (1:00 usage):
   The pink cat shows an ANGRY warning bubble ("Google Chrome: 1 MINUTE REMAINING!").
9. At 2:00 usage:
   The cat performs the KICK animation ("POW!"), Chrome is automatically closed,
   and Chrome is marked as BLOCKED. FocusBuddy continues running smoothly!


15. KNOWN WINDOWS LIMITATIONS
------------------------------
- Elevated / Administrator Windows:
  If a third-party application runs with higher Administrator privileges than
  FocusBuddy, Windows security restrictions may prevent non-elevated user processes
  from reading its window title or terminating it. For full administrative control,
  launch Command Prompt as Administrator before running run.bat.
- Window Transparency:
  Translucent window backgrounds require standard DWM (Desktop Window Manager)
  compositing, which is enabled by default on all standard Windows 10/11 installations.
