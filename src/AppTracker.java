/**
 * Tracks foreground application usage time, updates statistics,
 * and triggers warnings and limit-reached events.
 */
public class AppTracker {

    public interface TrackerListener {
        void onWarning(AppInfo app);
        void onLimitReached(AppInfo app);
        void onActiveAppChanged(String foregroundProcessName, AppInfo monitoredApp);
    }

    private final AppManager appManager;
    private TrackerListener listener;
    private int warningThresholdSeconds = 60;
    private int blockedCount = 0;
    private String lastForegroundProcess = null;

    public AppTracker(AppManager appManager) {
        this.appManager = appManager;
    }

    public void setListener(TrackerListener listener) {
        this.listener = listener;
    }

    public void setWarningThresholdSeconds(int seconds) {
        this.warningThresholdSeconds = Math.max(5, seconds);
    }

    public int getWarningThresholdSeconds() {
        return warningThresholdSeconds;
    }

    /**
     * Called on each monitoring tick (typically every second).
     *
     * @param foregroundProcessName normalized executable name (e.g. "chrome.exe") or null.
     * @param elapsedSeconds        active seconds to credit.
     */
    public synchronized void update(String foregroundProcessName, int elapsedSeconds) {
        if (foregroundProcessName == null || elapsedSeconds <= 0) {
            if (lastForegroundProcess != null) {
                lastForegroundProcess = null;
                if (listener != null) {
                    listener.onActiveAppChanged(null, null);
                }
            }
            return;
        }

        AppInfo app = appManager.findByProcessName(foregroundProcessName);

        if (!foregroundProcessName.equalsIgnoreCase(lastForegroundProcess)) {
            lastForegroundProcess = foregroundProcessName;
            if (listener != null) {
                listener.onActiveAppChanged(foregroundProcessName, app);
            }
        }

        // If not a monitored app or app is disabled or blocked, don't accumulate usage
        if (app == null || !app.isEnabled() || app.isBlocked()) {
            return;
        }

        Utils.debug("AppTracker", "Active: " + app.getProcessName());

        boolean wasAlreadyLimitReached = app.isLimitReached();
        app.addUsage(elapsedSeconds);

        Utils.debug("AppTracker", app.getAppName() + " usage: " + app.getCurrentUsageSeconds() + " sec");

        // 1. Limit reached check
        if (app.isLimitReached()) {
            if (!wasAlreadyLimitReached && !app.isBlocked()) {
                if (listener != null) {
                    listener.onLimitReached(app);
                }
            }
            return;
        }

        // 2. Warning check: trigger when remaining time <= warningThresholdSeconds (e.g. 60s)
        int remaining = app.getRemainingSeconds();
        if (!app.isWarningShown() && remaining <= warningThresholdSeconds) {
            app.setWarningShown(true);
            if (listener != null) {
                listener.onWarning(app);
            }
        }
    }

    public synchronized void markBlocked(AppInfo app) {
        if (!app.isBlocked()) {
            app.setBlocked(true);
            blockedCount++;
            appManager.save();
        }
    }

    public synchronized int getBlockedCount() {
        return blockedCount;
    }

    public synchronized void resetStats() {
        blockedCount = 0;
    }
}
