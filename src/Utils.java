import java.awt.Component;
import javax.swing.JOptionPane;

/** Small, dependency-free helper methods shared across the app. */
public final class Utils {

    /** Global debug switch. Can be toggled at runtime or via settings. */
    public static volatile boolean DEBUG = false;

    private Utils() {
    }

    /** Logs debug messages to standard output if DEBUG is enabled. */
    public static void debug(String tag, String message) {
        if (DEBUG) {
            System.out.println("[" + tag + "] " + message);
        }
    }

    /** Formats whole seconds as MM:SS (or HH:MM:SS if >= 1 hour). */
    public static String formatTime(long totalSeconds) {
        long clamped = Math.max(0, totalSeconds);
        long hours = clamped / 3600;
        long minutes = (clamped % 3600) / 60;
        long seconds = clamped % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /** Formats duration into human-readable text like "32 min" or "1 hr 15 min". */
    public static String formatHumanDuration(long totalSeconds) {
        long clamped = Math.max(0, totalSeconds);
        long hours = clamped / 3600;
        long minutes = (clamped % 3600) / 60;
        long seconds = clamped % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + " min";
        } else {
            return seconds + " sec";
        }
    }

    /** A conservative check for Windows executable/process names. */
    public static boolean isValidProcessName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        return !trimmed.isEmpty() && trimmed.matches("[A-Za-z0-9._-]+");
    }

    /** Parses a strictly positive integer, or returns null if invalid. */
    public static Integer parsePositiveInt(String text) {
        if (text == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "FocusBuddy", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "FocusBuddy", JOptionPane.INFORMATION_MESSAGE);
    }
}
