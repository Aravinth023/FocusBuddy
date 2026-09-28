import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tracks productivity statistics, session counters, and per-app usage,
 * persisting them to data/stats.properties.
 */
public class StatsManager {

    private static final String FILE_PATH = "data/stats.properties";

    private long todayFocusTimeSeconds = 0;
    private int completedWorkSessions = 0;
    private long totalTrackedAppUsageSeconds = 0;
    private long distractingAppUsageSeconds = 0;
    private int appsBlockedCount = 0;
    private long longestFocusSessionSeconds = 0;
    private long currentSessionFocusSeconds = 0;

    // Map: processName (lowercase) -> cumulative usage in seconds
    private final Map<String, Long> appUsageMap = new HashMap<>();

    public StatsManager() {
        load();
    }

    public synchronized void addFocusTime(int seconds) {
        if (seconds <= 0) return;
        todayFocusTimeSeconds += seconds;
        currentSessionFocusSeconds += seconds;
        if (currentSessionFocusSeconds > longestFocusSessionSeconds) {
            longestFocusSessionSeconds = currentSessionFocusSeconds;
        }
    }

    public synchronized void onSessionCompleted(boolean isWork) {
        if (isWork) {
            completedWorkSessions++;
        }
        currentSessionFocusSeconds = 0;
        save();
    }

    public synchronized void addAppUsage(String processName, int seconds, boolean isDistracting) {
        if (seconds <= 0) return;
        totalTrackedAppUsageSeconds += seconds;
        if (isDistracting) {
            distractingAppUsageSeconds += seconds;
        }
        if (processName != null && !processName.trim().isEmpty()) {
            String norm = processName.trim().toLowerCase();
            long current = appUsageMap.getOrDefault(norm, 0L);
            appUsageMap.put(norm, current + seconds);
        }
    }

    public synchronized void incrementBlockedCount() {
        appsBlockedCount++;
        save();
    }

    public synchronized void resetStats() {
        todayFocusTimeSeconds = 0;
        completedWorkSessions = 0;
        totalTrackedAppUsageSeconds = 0;
        distractingAppUsageSeconds = 0;
        appsBlockedCount = 0;
        longestFocusSessionSeconds = 0;
        currentSessionFocusSeconds = 0;
        appUsageMap.clear();
        save();
    }

    public synchronized void load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return;
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
            todayFocusTimeSeconds = parseLong(props.getProperty("todayFocusTimeSeconds"), 0);
            completedWorkSessions = (int) parseLong(props.getProperty("completedWorkSessions"), 0);
            totalTrackedAppUsageSeconds = parseLong(props.getProperty("totalTrackedAppUsageSeconds"), 0);
            distractingAppUsageSeconds = parseLong(props.getProperty("distractingAppUsageSeconds"), 0);
            appsBlockedCount = (int) parseLong(props.getProperty("appsBlockedCount"), 0);
            longestFocusSessionSeconds = parseLong(props.getProperty("longestFocusSessionSeconds"), 0);

            appUsageMap.clear();
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("app.")) {
                    String proc = key.substring(4);
                    long sec = parseLong(props.getProperty(key), 0);
                    appUsageMap.put(proc, sec);
                }
            }
        } catch (Exception e) {
            System.err.println("FocusBuddy: Could not load stats - " + e.getMessage());
        }
    }

    public synchronized void save() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        Properties props = new Properties();
        props.setProperty("todayFocusTimeSeconds", String.valueOf(todayFocusTimeSeconds));
        props.setProperty("completedWorkSessions", String.valueOf(completedWorkSessions));
        props.setProperty("totalTrackedAppUsageSeconds", String.valueOf(totalTrackedAppUsageSeconds));
        props.setProperty("distractingAppUsageSeconds", String.valueOf(distractingAppUsageSeconds));
        props.setProperty("appsBlockedCount", String.valueOf(appsBlockedCount));
        props.setProperty("longestFocusSessionSeconds", String.valueOf(longestFocusSessionSeconds));

        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            props.setProperty("app." + entry.getKey(), String.valueOf(entry.getValue()));
        }

        try (FileOutputStream out = new FileOutputStream(FILE_PATH)) {
            props.store(out, "FocusBuddy Statistics");
        } catch (IOException e) {
            System.err.println("FocusBuddy: Could not save stats - " + e.getMessage());
        }
    }

    private static long parseLong(String val, long defaultVal) {
        if (val == null) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    // Getters
    public synchronized long getTodayFocusTimeSeconds() { return todayFocusTimeSeconds; }
    public synchronized int getCompletedWorkSessions() { return completedWorkSessions; }
    public synchronized long getTotalTrackedAppUsageSeconds() { return totalTrackedAppUsageSeconds; }
    public synchronized long getDistractingAppUsageSeconds() { return distractingAppUsageSeconds; }
    public synchronized int getAppsBlockedCount() { return appsBlockedCount; }
    public synchronized long getLongestFocusSessionSeconds() { return longestFocusSessionSeconds; }
    public synchronized Map<String, Long> getAppUsageMap() { return Collections.unmodifiableMap(new HashMap<>(appUsageMap)); }
}
