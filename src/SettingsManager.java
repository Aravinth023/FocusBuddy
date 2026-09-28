import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Manages user settings and preferences for FocusBuddy,
 * persisting them to data/settings.properties.
 *
 * All settings have safe defaults and handle corrupted or missing files gracefully.
 */
public class SettingsManager {

    private static final String FILE_PATH = "data/settings.properties";

    private boolean showCat = true;
    private boolean alwaysOnTop = true;
    private boolean catMovement = true;
    private boolean animations = true;
    private boolean reducedAnimations = false;
    private boolean autoFocus = true;
    private boolean enforceLimitsDuringBreaks = false;
    private int warningThresholdSeconds = 60;
    private int workDurationMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int sessionsBeforeLongBreak = 4;
    private int monitoringIntervalSeconds = 1;
    private boolean debugMode = false;
    private boolean focusMode = true;

    public SettingsManager() {
        load();
    }

    public synchronized void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            save(); // Create defaults
            return;
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            showCat = Boolean.parseBoolean(props.getProperty("showCat", "true"));
            alwaysOnTop = Boolean.parseBoolean(props.getProperty("alwaysOnTop", "true"));
            catMovement = Boolean.parseBoolean(props.getProperty("catMovement", "true"));
            animations = Boolean.parseBoolean(props.getProperty("animations", "true"));
            reducedAnimations = Boolean.parseBoolean(props.getProperty("reducedAnimations", "false"));
            autoFocus = Boolean.parseBoolean(props.getProperty("autoFocus", "true"));
            enforceLimitsDuringBreaks = Boolean.parseBoolean(props.getProperty("enforceLimitsDuringBreaks", "false"));
            warningThresholdSeconds = parseInt(props.getProperty("warningThresholdSeconds"), 60, 5, 600);
            workDurationMinutes = parseInt(props.getProperty("workDurationMinutes"), 25, 1, 180);
            shortBreakMinutes = parseInt(props.getProperty("shortBreakMinutes"), 5, 1, 60);
            longBreakMinutes = parseInt(props.getProperty("longBreakMinutes"), 15, 1, 90);
            sessionsBeforeLongBreak = parseInt(props.getProperty("sessionsBeforeLongBreak"), 4, 1, 12);
            monitoringIntervalSeconds = parseInt(props.getProperty("monitoringIntervalSeconds"), 1, 1, 10);
            debugMode = Boolean.parseBoolean(props.getProperty("debugMode", "false"));
            focusMode = Boolean.parseBoolean(props.getProperty("focusMode", "true"));
        } catch (Exception e) {
            System.err.println("FocusBuddy: Could not load settings, using defaults - " + e.getMessage());
        }
    }

    public synchronized void save() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        Properties props = new Properties();
        props.setProperty("showCat", String.valueOf(showCat));
        props.setProperty("alwaysOnTop", String.valueOf(alwaysOnTop));
        props.setProperty("catMovement", String.valueOf(catMovement));
        props.setProperty("animations", String.valueOf(animations));
        props.setProperty("reducedAnimations", String.valueOf(reducedAnimations));
        props.setProperty("autoFocus", String.valueOf(autoFocus));
        props.setProperty("enforceLimitsDuringBreaks", String.valueOf(enforceLimitsDuringBreaks));
        props.setProperty("warningThresholdSeconds", String.valueOf(warningThresholdSeconds));
        props.setProperty("workDurationMinutes", String.valueOf(workDurationMinutes));
        props.setProperty("shortBreakMinutes", String.valueOf(shortBreakMinutes));
        props.setProperty("longBreakMinutes", String.valueOf(longBreakMinutes));
        props.setProperty("sessionsBeforeLongBreak", String.valueOf(sessionsBeforeLongBreak));
        props.setProperty("monitoringIntervalSeconds", String.valueOf(monitoringIntervalSeconds));
        props.setProperty("debugMode", String.valueOf(debugMode));
        props.setProperty("focusMode", String.valueOf(focusMode));

        try (FileOutputStream out = new FileOutputStream(FILE_PATH)) {
            props.store(out, "FocusBuddy User Settings");
        } catch (IOException e) {
            System.err.println("FocusBuddy: Could not save settings - " + e.getMessage());
        }
    }

    private static int parseInt(String val, int defaultVal, int min, int max) {
        if (val == null) return defaultVal;
        try {
            int parsed = Integer.parseInt(val.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Getters and Setters
    public synchronized boolean isShowCat() { return showCat; }
    public synchronized void setShowCat(boolean showCat) { this.showCat = showCat; }

    public synchronized boolean isAlwaysOnTop() { return alwaysOnTop; }
    public synchronized void setAlwaysOnTop(boolean alwaysOnTop) { this.alwaysOnTop = alwaysOnTop; }

    public synchronized boolean isCatMovement() { return catMovement; }
    public synchronized void setCatMovement(boolean catMovement) { this.catMovement = catMovement; }

    public synchronized boolean isAnimations() { return animations; }
    public synchronized void setAnimations(boolean animations) { this.animations = animations; }

    public synchronized boolean isReducedAnimations() { return reducedAnimations; }
    public synchronized void setReducedAnimations(boolean reducedAnimations) { this.reducedAnimations = reducedAnimations; }

    public synchronized boolean isAutoFocus() { return autoFocus; }
    public synchronized void setAutoFocus(boolean autoFocus) { this.autoFocus = autoFocus; }

    public synchronized boolean isEnforceLimitsDuringBreaks() { return enforceLimitsDuringBreaks; }
    public synchronized void setEnforceLimitsDuringBreaks(boolean enforceLimitsDuringBreaks) { this.enforceLimitsDuringBreaks = enforceLimitsDuringBreaks; }

    public synchronized int getWarningThresholdSeconds() { return warningThresholdSeconds; }
    public synchronized void setWarningThresholdSeconds(int warningThresholdSeconds) { this.warningThresholdSeconds = warningThresholdSeconds; }

    public synchronized int getWorkDurationMinutes() { return workDurationMinutes; }
    public synchronized void setWorkDurationMinutes(int workDurationMinutes) { this.workDurationMinutes = workDurationMinutes; }

    public synchronized int getShortBreakMinutes() { return shortBreakMinutes; }
    public synchronized void setShortBreakMinutes(int shortBreakMinutes) { this.shortBreakMinutes = shortBreakMinutes; }

    public synchronized int getLongBreakMinutes() { return longBreakMinutes; }
    public synchronized void setLongBreakMinutes(int longBreakMinutes) { this.longBreakMinutes = longBreakMinutes; }

    public synchronized int getSessionsBeforeLongBreak() { return sessionsBeforeLongBreak; }
    public synchronized void setSessionsBeforeLongBreak(int sessionsBeforeLongBreak) { this.sessionsBeforeLongBreak = sessionsBeforeLongBreak; }

    public synchronized int getMonitoringIntervalSeconds() { return monitoringIntervalSeconds; }
    public synchronized void setMonitoringIntervalSeconds(int monitoringIntervalSeconds) { this.monitoringIntervalSeconds = monitoringIntervalSeconds; }

    public synchronized boolean isDebugMode() { return debugMode; }
    public synchronized void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

    public synchronized boolean isFocusMode() { return focusMode; }
    public synchronized void setFocusMode(boolean focusMode) { this.focusMode = focusMode; }
}
