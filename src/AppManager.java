import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owns the list of monitored applications: add, remove, update, enable/disable,
 * validation, persistence (data/apps.txt), and protection of critical system processes.
 *
 * All public methods are synchronized because this list is accessed both from
 * the Swing EDT (UI) and the background monitoring thread.
 */
public class AppManager {

    private static final String STORAGE_PATH = "data/apps.txt";

    private final List<AppInfo> apps = new ArrayList<>();

    /**
     * Processes that FocusBuddy will never allow the user to monitor or terminate,
     * including FocusBuddy itself and core Windows system processes.
     */
    private static final Set<String> PROTECTED_PROCESSES = new HashSet<>(Arrays.asList(
            "focusbuddy.exe", "java.exe", "javaw.exe",
            "system", "system.exe", "system idle process", "registry",
            "smss.exe", "csrss.exe", "wininit.exe", "services.exe",
            "lsass.exe", "svchost.exe", "explorer.exe", "winlogon.exe", "dwm.exe"
    ));

    public AppManager() {
        load();
    }

    public synchronized boolean isProtected(String processName) {
        if (processName == null || processName.trim().isEmpty()) {
            return true;
        }
        String normalized = AppInfo.normalizeProcessName(processName);
        if (PROTECTED_PROCESSES.contains(normalized)) {
            return true;
        }
        // Also shield base name without .exe
        String base = normalized.endsWith(".exe") ? normalized.substring(0, normalized.length() - 4) : normalized;
        return PROTECTED_PROCESSES.contains(base);
    }

    /**
     * Adds a new monitored application and persists changes.
     *
     * @return null on success, or a human-readable error message on failure.
     */
    public synchronized String addApp(String appName, String processName, int timeLimitMinutes) {
        String validationError = validate(appName, processName, timeLimitMinutes, null);
        if (validationError != null) {
            return validationError;
        }
        apps.add(new AppInfo(appName.trim(), AppInfo.normalizeProcessName(processName), timeLimitMinutes * 60));
        save();
        return null;
    }

    /**
     * Updates an existing monitored application in place and persists changes.
     *
     * @return null on success, or a human-readable error message on failure.
     */
    public synchronized String updateApp(AppInfo existing, String appName, String processName, int timeLimitMinutes) {
        String validationError = validate(appName, processName, timeLimitMinutes, existing);
        if (validationError != null) {
            return validationError;
        }
        existing.setAppName(appName.trim());
        existing.setProcessName(processName);
        existing.setTimeLimitSeconds(timeLimitMinutes * 60);
        save();
        return null;
    }

    private String validate(String appName, String processName, int timeLimitMinutes, AppInfo ignoreExisting) {
        if (appName == null || appName.trim().isEmpty()) {
            return "Application name cannot be empty.";
        }
        if (processName == null || processName.trim().isEmpty()) {
            return "Process name cannot be empty.";
        }
        if (!Utils.isValidProcessName(processName)) {
            return "Process name may only contain letters, numbers, dots, dashes and underscores.";
        }
        String normalized = AppInfo.normalizeProcessName(processName);
        if (isProtected(normalized)) {
            return "That process is protected and cannot be monitored or terminated.";
        }
        if (timeLimitMinutes <= 0) {
            return "Time limit must be greater than zero.";
        }
        for (AppInfo a : apps) {
            if (a != ignoreExisting && a.getProcessName().equalsIgnoreCase(normalized)) {
                return "This process is already being monitored as \"" + a.getAppName() + "\".";
            }
        }
        return null;
    }

    public synchronized void removeApp(AppInfo app) {
        apps.remove(app);
        save();
    }

    public synchronized void resetUsage(AppInfo app) {
        app.resetUsage();
        save();
    }

    public synchronized void resetAllUsage() {
        for (AppInfo a : apps) {
            a.resetUsage();
        }
        save();
    }

    public synchronized List<AppInfo> getApps() {
        return new ArrayList<>(apps);
    }

    public synchronized AppInfo findByProcessName(String processName) {
        if (processName == null) {
            return null;
        }
        String normalized = AppInfo.normalizeProcessName(processName);
        for (AppInfo a : apps) {
            if (a.getProcessName().equalsIgnoreCase(normalized)) {
                return a;
            }
        }
        return null;
    }

    public synchronized void load() {
        apps.clear();
        File file = new File(STORAGE_PATH);
        if (!file.exists()) {
            seedDefaults();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                AppInfo info = AppInfo.fromStorageLine(line);
                if (info != null) {
                    apps.add(info);
                }
            }
        } catch (Exception e) {
            System.err.println("FocusBuddy: Error loading apps from " + STORAGE_PATH + " - " + e.getMessage());
        }

        if (apps.isEmpty()) {
            seedDefaults();
        }
    }

    public synchronized void save() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STORAGE_PATH))) {
            writer.write("# FocusBuddy Monitored Applications\n");
            writer.write("# Format: AppName|ProcessName|TimeLimitSeconds|CurrentUsageSeconds|Enabled|Blocked|WarningShown\n");
            for (AppInfo a : apps) {
                writer.write(a.toStorageLine());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("FocusBuddy: Error saving apps to " + STORAGE_PATH + " - " + e.getMessage());
        }
    }

    private void seedDefaults() {
        apps.clear();
        apps.add(new AppInfo("Google Chrome", "chrome.exe", 30 * 60));
        apps.add(new AppInfo("Microsoft Edge", "msedge.exe", 30 * 60));
        apps.add(new AppInfo("Discord", "discord.exe", 45 * 60));
        apps.add(new AppInfo("WhatsApp", "WhatsApp.exe", 20 * 60));
        save();
    }
}
