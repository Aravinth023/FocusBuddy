/**
 * Represents one application that FocusBuddy is monitoring for usage limits.
 *
 * Time is tracked in whole seconds. currentUsageSeconds only increases while
 * the process is the active/foreground Windows application -- simply being open
 * in the background does not consume the limit.
 */
public class AppInfo {

    private String appName;
    private String processName;
    private int timeLimitSeconds;
    private int currentUsageSeconds;
    private boolean enabled;
    private boolean blocked;
    private boolean warningShown;

    public AppInfo(String appName, String processName, int timeLimitSeconds) {
        this.appName = appName != null ? appName.trim() : "";
        this.processName = normalizeProcessName(processName);
        this.timeLimitSeconds = Math.max(1, timeLimitSeconds);
        this.currentUsageSeconds = 0;
        this.enabled = true;
        this.blocked = false;
        this.warningShown = false;
    }

    public AppInfo(String appName, String processName, int timeLimitSeconds, int currentUsageSeconds,
                   boolean enabled, boolean blocked, boolean warningShown) {
        this.appName = appName != null ? appName.trim() : "";
        this.processName = normalizeProcessName(processName);
        this.timeLimitSeconds = Math.max(1, timeLimitSeconds);
        this.currentUsageSeconds = Math.max(0, currentUsageSeconds);
        this.enabled = enabled;
        this.blocked = blocked;
        this.warningShown = warningShown;
    }

    /**
     * Normalizes process names consistently:
     * lowercase, trimmed, with ".exe" suffix guaranteed.
     * Examples:
     *   "chrome" -> "chrome.exe"
     *   "chrome.exe" -> "chrome.exe"
     *   "CHROME.EXE" -> "chrome.exe"
     */
    public static String normalizeProcessName(String name) {
        if (name == null) {
            return "";
        }
        String s = name.trim().toLowerCase();
        if (!s.isEmpty() && !s.endsWith(".exe")) {
            s = s + ".exe";
        }
        return s;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName != null ? appName.trim() : "";
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = normalizeProcessName(processName);
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
        this.timeLimitSeconds = Math.max(1, timeLimitSeconds);
    }

    public int getCurrentUsageSeconds() {
        return currentUsageSeconds;
    }

    public void setCurrentUsageSeconds(int currentUsageSeconds) {
        this.currentUsageSeconds = Math.max(0, currentUsageSeconds);
    }

    public void addUsage(int seconds) {
        if (seconds > 0) {
            this.currentUsageSeconds += seconds;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isWarningShown() {
        return warningShown;
    }

    public void setWarningShown(boolean warningShown) {
        this.warningShown = warningShown;
    }

    public int getRemainingSeconds() {
        return Math.max(0, timeLimitSeconds - currentUsageSeconds);
    }

    public boolean isLimitReached() {
        return currentUsageSeconds >= timeLimitSeconds;
    }

    /** Clears usage and blocked/warning flags. */
    public void resetUsage() {
        currentUsageSeconds = 0;
        blocked = false;
        warningShown = false;
    }

    /** Serializes this app entry to a single delimited line for data/apps.txt. */
    public String toStorageLine() {
        return appName.replace("|", "/") + "|"
                + processName + "|"
                + timeLimitSeconds + "|"
                + currentUsageSeconds + "|"
                + enabled + "|"
                + blocked + "|"
                + warningShown;
    }

    /** Deserializes an AppInfo from a line in data/apps.txt. Returns null if invalid. */
    public static AppInfo fromStorageLine(String line) {
        if (line == null || line.trim().isEmpty() || line.startsWith("#")) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            return null;
        }
        try {
            String name = parts[0].trim();
            String proc = parts[1].trim();
            int limitSec = Integer.parseInt(parts[2].trim());
            int usageSec = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 0;
            boolean enabled = parts.length > 4 ? Boolean.parseBoolean(parts[4].trim()) : true;
            boolean blocked = parts.length > 5 ? Boolean.parseBoolean(parts[5].trim()) : false;
            boolean warning = parts.length > 6 ? Boolean.parseBoolean(parts[6].trim()) : false;
            return new AppInfo(name, proc, limitSec, usageSec, enabled, blocked, warning);
        } catch (Exception e) {
            return null;
        }
    }
}
