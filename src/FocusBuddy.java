import java.awt.Color;
import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for FocusBuddy.
 *
 * Responsibilities:
 *   - Ensure data/ directory exists
 *   - Configure Look and Feel with dark theme and pink accents
 *   - Construct and link manager components
 *   - Show GUI and desktop cat companion
 *   - Register clean shutdown hook for process cleanup and data persistence
 */
public class FocusBuddy {

    public static void main(String[] args) {
        // Ensure data directory exists
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        applyDarkTheme();
        SwingUtilities.invokeLater(FocusBuddy::launch);
    }

    /**
     * Installs a consistent dark theme across standard Swing components
     * with pink accents matching the desktop cat companion.
     */
    private static void applyDarkTheme() {
        boolean installed = false;
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    installed = true;
                    break;
                }
            }
        } catch (Exception ignored) {
            installed = false;
        }
        if (!installed) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception ignored) {}
        }

        Color background = new Color(26, 26, 32);
        Color panel = new Color(36, 36, 44);
        Color field = new Color(50, 50, 62);
        Color text = new Color(240, 240, 245);
        Color muted = new Color(155, 155, 168);
        Color accentPink = new Color(240, 98, 146);
        Color grid = new Color(64, 64, 76);

        UIManager.put("nimbusBase", panel);
        UIManager.put("nimbusBlueGrey", panel);
        UIManager.put("nimbusLightBackground", field);
        UIManager.put("nimbusSelectionBackground", accentPink.darker());
        UIManager.put("control", panel);
        UIManager.put("text", text);
        UIManager.put("info", panel);

        UIManager.put("Panel.background", background);
        UIManager.put("OptionPane.background", background);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("Label.foreground", text);
        UIManager.put("Label.background", background);
        UIManager.put("CheckBox.foreground", text);
        UIManager.put("CheckBox.background", panel);
        UIManager.put("TextField.background", field);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.caretForeground", text);
        UIManager.put("Table.background", panel);
        UIManager.put("Table.foreground", text);
        UIManager.put("Table.gridColor", grid);
        UIManager.put("Table.selectionBackground", accentPink.darker());
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", panel);
        UIManager.put("TableHeader.foreground", muted);
        UIManager.put("ScrollPane.background", background);
        UIManager.put("Viewport.background", panel);
        UIManager.put("TabbedPane.background", panel);
        UIManager.put("TabbedPane.foreground", text);
        UIManager.put("TabbedPane.selected", accentPink.darker());
        UIManager.put("TabbedPane.selectHighlight", accentPink.darker());
        UIManager.put("Button.background", panel);
        UIManager.put("Button.foreground", text);
        UIManager.put("Dialog.background", background);
        UIManager.put("Viewport.foreground", text);
        UIManager.put("ScrollBar.thumb", panel.brighter());
        UIManager.put("ScrollBar.track", background);
    }

    private static void launch() {
        SettingsManager settingsManager = new SettingsManager();
        StatsManager statsManager = new StatsManager();
        Utils.DEBUG = settingsManager.isDebugMode();

        PomodoroManager pomodoroManager = new PomodoroManager();
        pomodoroManager.configure(
                settingsManager.getWorkDurationMinutes(),
                settingsManager.getShortBreakMinutes(),
                settingsManager.getLongBreakMinutes(),
                settingsManager.getSessionsBeforeLongBreak()
        );

        AppManager appManager = new AppManager();
        AppTracker appTracker = new AppTracker(appManager);
        appTracker.setWarningThresholdSeconds(settingsManager.getWarningThresholdSeconds());

        ProcessMonitor processMonitor = new ProcessMonitor();
        processMonitor.setIntervalSeconds(settingsManager.getMonitoringIntervalSeconds());

        CatAnimation catAnimation = new CatAnimation();
        catAnimation.setAlwaysOnTop(settingsManager.isAlwaysOnTop());
        catAnimation.setMovementEnabled(settingsManager.isCatMovement());
        catAnimation.setAnimationsEnabled(settingsManager.isAnimations());
        catAnimation.setReducedAnimation(settingsManager.isReducedAnimations());

        FocusBuddyUI ui = new FocusBuddyUI(
                pomodoroManager, appManager, appTracker, processMonitor,
                catAnimation, settingsManager, statsManager
        );
        ui.initializeDisplay();
        ui.setVisible(true);

        if (settingsManager.isShowCat()) {
            catAnimation.start();
            catAnimation.show();
            catAnimation.setState(CatState.WALK);
        }

        processMonitor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pomodoroManager.pause();
                processMonitor.stop();
                catAnimation.stop();
                statsManager.save();
                settingsManager.save();
                appManager.save();
            } catch (Exception e) {
                System.err.println("FocusBuddy: Error during shutdown - " + e.getMessage());
            }
        }, "FocusBuddy-Shutdown"));
    }
}
