import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Modern dark-themed dashboard with pink accents matching the desktop cat companion.
 *
 * Provides real-time controls and display for:
 *   - Pomodoro Timer (WORK / BREAK)
 *   - Focus Mode toggle
 *   - Active Application tracking
 *   - Daily statistics
 *   - Monitored apps overview
 *   - Manage Apps, Statistics, and Settings dialogs
 */
public class FocusBuddyUI extends JFrame {

    private final PomodoroManager pomodoroManager;
    private final AppManager appManager;
    private final AppTracker appTracker;
    private final ProcessMonitor processMonitor;
    private final CatAnimation catAnimation;
    private final SettingsManager settingsManager;
    private final StatsManager statsManager;

    private boolean kickInProgress = false;
    private Point dragOrigin;

    // UI Elements
    private JLabel timeLabel;
    private JLabel stateLabel;
    private JLabel sessionLabel;
    private JLabel focusModeStatusLabel;
    private DarkButton focusModeToggleButton;
    private JLabel activeAppLabel;

    private DarkButton startButton;
    private DarkButton pauseButton;
    private DarkButton resetButton;
    private DarkButton skipButton;

    private JLabel focusStatValue;
    private JLabel distractionStatValue;
    private JLabel pomodoroStatValue;
    private JLabel blockedStatValue;

    private DefaultTableModel monitoredAppsTableModel;

    // Theme Colors
    public static final Color BG_DARK = new Color(26, 26, 32);
    public static final Color PANEL_DARK = new Color(36, 36, 44);
    public static final Color CARD_DARK = new Color(42, 42, 52);
    public static final Color FIELD_DARK = new Color(50, 50, 62);
    public static final Color ACCENT_PINK = new Color(240, 98, 146);
    public static final Color ACCENT_HOVER_PINK = new Color(244, 143, 177);
    public static final Color TEXT_LIGHT = new Color(240, 240, 245);
    public static final Color TEXT_MUTED = new Color(155, 155, 168);
    public static final Color BORDER_COLOR = new Color(64, 64, 76);

    public FocusBuddyUI(PomodoroManager pomodoroManager, AppManager appManager, AppTracker appTracker,
                        ProcessMonitor processMonitor, CatAnimation catAnimation,
                        SettingsManager settingsManager, StatsManager statsManager) {
        this.pomodoroManager = pomodoroManager;
        this.appManager = appManager;
        this.appTracker = appTracker;
        this.processMonitor = processMonitor;
        this.catAnimation = catAnimation;
        this.settingsManager = settingsManager;
        this.statsManager = statsManager;

        initFrame();
        buildUI();
        wireListeners();
    }

    private void initFrame() {
        setTitle("FocusBuddy");
        setUndecorated(true);
        setSize(400, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void buildUI() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_DARK);
        contentPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        contentPanel.add(buildTitleBar(), BorderLayout.NORTH);
        contentPanel.add(buildMainDashboard(), BorderLayout.CENTER);
        setContentPane(contentPanel);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL_DARK);
        bar.setPreferredSize(new Dimension(10, 36));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        titleBox.setOpaque(false);
        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(ACCENT_PINK);
        dot.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel title = new JLabel("FocusBuddy");
        title.setForeground(TEXT_LIGHT);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleBox.add(dot);
        titleBox.add(title);
        bar.add(titleBox, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        buttons.setOpaque(false);
        DarkButton minBtn = smallTitleButton("_");
        DarkButton closeBtn = smallTitleButton("X");
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        closeBtn.addActionListener(e -> confirmExit());
        buttons.add(minBtn);
        buttons.add(closeBtn);
        bar.add(buttons, BorderLayout.EAST);

        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOrigin = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point onScreen = e.getLocationOnScreen();
                setLocation(onScreen.x - dragOrigin.x, onScreen.y - dragOrigin.y);
            }
        };
        bar.addMouseListener(dragHandler);
        bar.addMouseMotionListener(dragHandler);
        return bar;
    }

    private DarkButton smallTitleButton(String text) {
        DarkButton b = new DarkButton(text);
        b.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        return b;
    }

    private JPanel buildMainDashboard() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(BG_DARK);
        main.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // 1. Focus Mode Toggle Header
        JPanel focusModeRow = new JPanel(new BorderLayout());
        focusModeRow.setOpaque(false);
        focusModeRow.setMaximumSize(new Dimension(4000, 32));

        focusModeStatusLabel = new JLabel("FOCUS MODE: ON");
        focusModeStatusLabel.setForeground(ACCENT_PINK);
        focusModeStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        focusModeToggleButton = new DarkButton(settingsManager.isFocusMode() ? "Turn OFF" : "Turn ON");
        focusModeToggleButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
        focusModeToggleButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        focusModeToggleButton.addActionListener(e -> toggleFocusMode());

        focusModeRow.add(focusModeStatusLabel, BorderLayout.WEST);
        focusModeRow.add(focusModeToggleButton, BorderLayout.EAST);
        main.add(focusModeRow);
        main.add(Box.createVerticalStrut(10));

        // 2. Pomodoro Timer Display
        timeLabel = new JLabel("25:00");
        timeLabel.setForeground(TEXT_LIGHT);
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 46));
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(timeLabel);

        stateLabel = new JLabel("WORK");
        stateLabel.setForeground(ACCENT_PINK);
        stateLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        stateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(stateLabel);

        sessionLabel = new JLabel("Session 1");
        sessionLabel.setForeground(TEXT_MUTED);
        sessionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sessionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(sessionLabel);
        main.add(Box.createVerticalStrut(10));

        // 3. Pomodoro Action Buttons
        JPanel timerControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        timerControls.setOpaque(false);
        startButton = new DarkButton("START");
        startButton.setAccent(true);
        pauseButton = new DarkButton("PAUSE");
        resetButton = new DarkButton("RESET");
        skipButton = new DarkButton("SKIP");

        timerControls.add(startButton);
        timerControls.add(pauseButton);
        timerControls.add(resetButton);
        timerControls.add(skipButton);
        main.add(timerControls);

        startButton.addActionListener(e -> pomodoroManager.start());
        pauseButton.addActionListener(e -> pomodoroManager.pause());
        resetButton.addActionListener(e -> pomodoroManager.reset());
        skipButton.addActionListener(e -> pomodoroManager.skip());

        main.add(Box.createVerticalStrut(12));

        // 4. Live Active Application Card
        JPanel activeAppCard = new JPanel(new BorderLayout());
        activeAppCard.setBackground(CARD_DARK);
        activeAppCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        activeAppCard.setMaximumSize(new Dimension(4000, 36));

        JLabel activeTag = new JLabel("ACTIVE APP: ");
        activeTag.setForeground(TEXT_MUTED);
        activeTag.setFont(new Font("SansSerif", Font.BOLD, 11));

        activeAppLabel = new JLabel("None (Desktop)");
        activeAppLabel.setForeground(ACCENT_PINK);
        activeAppLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        activeAppCard.add(activeTag, BorderLayout.WEST);
        activeAppCard.add(activeAppLabel, BorderLayout.CENTER);
        main.add(activeAppCard);

        main.add(Box.createVerticalStrut(12));

        // 5. Daily Stats Summary
        main.add(sectionHeader("TODAY'S PRODUCTIVITY"));
        main.add(Box.createVerticalStrut(4));

        focusStatValue = new JLabel("00:00");
        distractionStatValue = new JLabel("00:00");
        pomodoroStatValue = new JLabel("0");
        blockedStatValue = new JLabel("0");

        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 8, 6));
        statsGrid.setOpaque(false);
        statsGrid.setMaximumSize(new Dimension(4000, 52));
        statsGrid.add(miniStatCard("Focus Time", focusStatValue));
        statsGrid.add(miniStatCard("Distraction", distractionStatValue));
        statsGrid.add(miniStatCard("Pomodoros", pomodoroStatValue));
        statsGrid.add(miniStatCard("Blocked Apps", blockedStatValue));
        main.add(statsGrid);

        main.add(Box.createVerticalStrut(12));

        // 6. Monitored Apps Overview Table
        main.add(sectionHeader("MONITORED APPLICATIONS"));
        main.add(Box.createVerticalStrut(4));

        monitoredAppsTableModel = new DefaultTableModel(new Object[]{"Application", "Usage / Limit", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(monitoredAppsTableModel);
        styleTable(table);
        table.setRowHeight(22);
        JScrollPane scrollPane = new JScrollPane(table);
        styleScrollPane(scrollPane);
        scrollPane.setPreferredSize(new Dimension(340, 95));
        scrollPane.setMaximumSize(new Dimension(4000, 95));
        main.add(scrollPane);

        main.add(Box.createVerticalStrut(12));

        // 7. Navigation Actions (Manage Apps, Statistics, Settings)
        JPanel bottomNav = new JPanel(new GridLayout(1, 3, 8, 0));
        bottomNav.setOpaque(false);
        bottomNav.setMaximumSize(new Dimension(4000, 34));

        DarkButton manageAppsBtn = new DarkButton("Manage Apps");
        DarkButton statsBtn = new DarkButton("Statistics");
        DarkButton settingsBtn = new DarkButton("Settings");

        manageAppsBtn.addActionListener(e -> showManageAppsDialog());
        statsBtn.addActionListener(e -> showStatisticsDialog());
        settingsBtn.addActionListener(e -> showSettingsDialog());

        bottomNav.add(manageAppsBtn);
        bottomNav.add(statsBtn);
        bottomNav.add(settingsBtn);
        main.add(bottomNav);

        return main;
    }

    private JPanel miniStatCard(String title, JLabel valLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(PANEL_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JLabel t = new JLabel(title);
        t.setForeground(TEXT_MUTED);
        t.setFont(new Font("SansSerif", Font.PLAIN, 10));

        valLabel.setForeground(TEXT_LIGHT);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        valLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(t, BorderLayout.WEST);
        card.add(valLabel, BorderLayout.EAST);
        return card;
    }

    private JLabel sectionHeader(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private void toggleFocusMode() {
        boolean newState = !settingsManager.isFocusMode();
        settingsManager.setFocusMode(newState);
        settingsManager.save();
        updateFocusModeUI();
    }

    private void updateFocusModeUI() {
        boolean on = settingsManager.isFocusMode();
        focusModeStatusLabel.setText(on ? "FOCUS MODE: ON" : "FOCUS MODE: OFF");
        focusModeStatusLabel.setForeground(on ? ACCENT_PINK : TEXT_MUTED);
        focusModeToggleButton.setText(on ? "Turn OFF" : "Turn ON");
    }

    // ---------------------------------------------------------------
    // Component Styling Helpers
    // ---------------------------------------------------------------

    private void styleTable(JTable table) {
        table.setBackground(PANEL_DARK);
        table.setForeground(TEXT_LIGHT);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(ACCENT_PINK.darker());
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(PANEL_DARK);
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scrollPane.getViewport().setBackground(PANEL_DARK);
    }

    private void styleTextField(JTextField field) {
        field.setBackground(FIELD_DARK);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    private void styleCheckBox(JCheckBox box) {
        box.setForeground(TEXT_LIGHT);
        box.setOpaque(false);
    }

    private void styleDialog(JDialog dialog) {
        dialog.getContentPane().setBackground(BG_DARK);
    }

    private JLabel dialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_LIGHT);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return label;
    }

    // ---------------------------------------------------------------
    // Event Wiring
    // ---------------------------------------------------------------

    private void wireListeners() {
        pomodoroManager.setListener(new PomodoroManager.PomodoroListener() {
            @Override
            public void onTick(int secondsRemaining, FocusState state, int sessionNumber) {
                timeLabel.setText(Utils.formatTime(secondsRemaining));
                stateLabel.setText(state.toString().replace('_', ' '));
                sessionLabel.setText("Session " + sessionNumber);

                if (state == FocusState.WORK) {
                    statsManager.addFocusTime(1);
                    focusStatValue.setText(Utils.formatTime(statsManager.getTodayFocusTimeSeconds()));
                }
            }

            @Override
            public void onStateChanged(FocusState newState, int sessionNumber) {
                stateLabel.setText(newState.toString().replace('_', ' '));
                if (newState == FocusState.WORK && settingsManager.isAutoFocus()) {
                    catAnimation.setState(CatState.HAPPY);
                    catAnimation.setBubbleText("Let's Focus!");
                    Timer clearBubble = new Timer(2200, ev -> catAnimation.setBubbleText(null));
                    clearBubble.setRepeats(false);
                    clearBubble.start();
                    Timer setWorkState = new Timer(2400, ev -> catAnimation.setState(CatState.TYPE));
                    setWorkState.setRepeats(false);
                    setWorkState.start();
                } else if (newState != FocusState.WORK) {
                    catAnimation.setState(CatState.HAPPY);
                    catAnimation.setBubbleText("Break Time!");
                    Timer clearBubble = new Timer(2200, ev -> catAnimation.setBubbleText(null));
                    clearBubble.setRepeats(false);
                    clearBubble.start();
                }
            }

            @Override
            public void onSessionCompleted(FocusState completedState, int sessionNumber) {
                statsManager.onSessionCompleted(completedState == FocusState.WORK);
                pomodoroStatValue.setText(String.valueOf(statsManager.getCompletedWorkSessions()));
                catAnimation.setState(CatState.HAPPY);
                catAnimation.setBubbleText("Session Done! Great Work!");
                Timer t = new Timer(3000, ev -> catAnimation.setBubbleText(null));
                t.setRepeats(false);
                t.start();
            }
        });

        processMonitor.setListener((foregroundProcessName, runningProcessNames) -> {
            boolean focusModeActive = settingsManager.isFocusMode();
            FocusState currentState = pomodoroManager.getState();
            boolean shouldTrack = focusModeActive && (currentState == FocusState.WORK || settingsManager.isEnforceLimitsDuringBreaks());

            if (shouldTrack && foregroundProcessName != null) {
                appTracker.update(foregroundProcessName, settingsManager.getMonitoringIntervalSeconds());
                AppInfo monitored = appManager.findByProcessName(foregroundProcessName);
                if (monitored != null && monitored.isEnabled() && !monitored.isBlocked()) {
                    statsManager.addAppUsage(monitored.getProcessName(), settingsManager.getMonitoringIntervalSeconds(), true);
                }
            }

            SwingUtilities.invokeLater(() -> {
                updateActiveAppDisplay(foregroundProcessName);
                refreshMonitoredAppsTable();
            });
        });

        appTracker.setListener(new AppTracker.TrackerListener() {
            @Override
            public void onWarning(AppInfo app) {
                SwingUtilities.invokeLater(() -> catAnimation.playWarning(app.getAppName()));
            }

            @Override
            public void onLimitReached(AppInfo app) {
                SwingUtilities.invokeLater(() -> handleLimitReached(app));
            }

            @Override
            public void onActiveAppChanged(String foregroundProcessName, AppInfo monitoredApp) {
                SwingUtilities.invokeLater(() -> updateActiveAppDisplay(foregroundProcessName));
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
    }

    private void updateActiveAppDisplay(String processName) {
        if (processName == null || processName.trim().isEmpty()) {
            activeAppLabel.setText("None (Desktop)");
            activeAppLabel.setForeground(TEXT_MUTED);
            return;
        }
        AppInfo app = appManager.findByProcessName(processName);
        if (app != null) {
            activeAppLabel.setText(app.getAppName() + " (" + app.getProcessName() + ")");
            activeAppLabel.setForeground(ACCENT_PINK);
        } else {
            activeAppLabel.setText(processName);
            activeAppLabel.setForeground(TEXT_LIGHT);
        }
    }

    private void handleLimitReached(AppInfo app) {
        if (kickInProgress || app.isBlocked()) {
            return;
        }
        kickInProgress = true;

        catAnimation.playKickSequence(app.getAppName(), new CatAnimation.KickSequenceListener() {
            @Override
            public void onTerminationPoint() {
                ProcessMonitor.TerminationOutcome outcome =
                        processMonitor.terminateProcessByName(app.getProcessName(), appManager);

                SwingUtilities.invokeLater(() -> {
                    if (outcome.success) {
                        appTracker.markBlocked(app);
                        statsManager.incrementBlockedCount();
                    } else {
                        Utils.showInfo(FocusBuddyUI.this,
                                "Could not close " + app.getAppName() + ".\n"
                                        + "Please close it manually.\n(" + outcome.message + ")");
                        appTracker.markBlocked(app);
                    }
                    blockedStatValue.setText(String.valueOf(statsManager.getAppsBlockedCount()));
                    refreshMonitoredAppsTable();
                });
            }

            @Override
            public void onSequenceComplete() {
                kickInProgress = false;
            }
        });
    }

    private void refreshMonitoredAppsTable() {
        monitoredAppsTableModel.setRowCount(0);
        long totalDistraction = 0;
        for (AppInfo app : appManager.getApps()) {
            String usageStr = Utils.formatTime(app.getCurrentUsageSeconds()) + " / " + Utils.formatTime(app.getTimeLimitSeconds());
            String status = "Active";
            if (app.isBlocked()) {
                status = "BLOCKED";
            } else if (!app.isEnabled()) {
                status = "DISABLED";
            } else if (app.isWarningShown()) {
                status = "WARNING";
            }
            monitoredAppsTableModel.addRow(new Object[]{app.getAppName(), usageStr, status});
            totalDistraction += app.getCurrentUsageSeconds();
        }
        distractionStatValue.setText(Utils.formatTime(totalDistraction));
        blockedStatValue.setText(String.valueOf(statsManager.getAppsBlockedCount()));
        focusStatValue.setText(Utils.formatTime(statsManager.getTodayFocusTimeSeconds()));
        pomodoroStatValue.setText(String.valueOf(statsManager.getCompletedWorkSessions()));
    }

    private void confirmExit() {
        int result = JOptionPane.showConfirmDialog(this, "Exit FocusBuddy?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            pomodoroManager.pause();
            processMonitor.stop();
            catAnimation.stop();
            catAnimation.hide();
            statsManager.save();
            settingsManager.save();
            appManager.save();
            dispose();
            System.exit(0);
        }
    }

    // ---------------------------------------------------------------
    // Statistics Dialog
    // ---------------------------------------------------------------

    private void showStatisticsDialog() {
        JDialog dialog = new JDialog(this, "Productivity Statistics", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        styleDialog(dialog);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Metric Cards Grid
        JPanel metricsGrid = new JPanel(new GridLayout(3, 2, 8, 8));
        metricsGrid.setOpaque(false);
        metricsGrid.setMaximumSize(new Dimension(4000, 110));

        metricsGrid.add(miniStatCard("Today's Focus Time", new JLabel(Utils.formatHumanDuration(statsManager.getTodayFocusTimeSeconds()))));
        metricsGrid.add(miniStatCard("Completed Work Sessions", new JLabel(String.valueOf(statsManager.getCompletedWorkSessions()))));
        metricsGrid.add(miniStatCard("Longest Focus Session", new JLabel(Utils.formatHumanDuration(statsManager.getLongestFocusSessionSeconds()))));
        metricsGrid.add(miniStatCard("Distracting App Usage", new JLabel(Utils.formatHumanDuration(statsManager.getDistractingAppUsageSeconds()))));
        metricsGrid.add(miniStatCard("Total Monitored Usage", new JLabel(Utils.formatHumanDuration(statsManager.getTotalTrackedAppUsageSeconds()))));
        metricsGrid.add(miniStatCard("Applications Blocked", new JLabel(String.valueOf(statsManager.getAppsBlockedCount()))));
        mainPanel.add(metricsGrid);
        mainPanel.add(Box.createVerticalStrut(14));

        JLabel breakdownTitle = new JLabel("APP USAGE BREAKDOWN");
        breakdownTitle.setForeground(TEXT_MUTED);
        breakdownTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        breakdownTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(breakdownTitle);
        mainPanel.add(Box.createVerticalStrut(6));

        DefaultTableModel statsModel = new DefaultTableModel(
                new Object[]{"Application", "Process", "Usage Time", "% of Distraction"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        long totalDistract = Math.max(1, statsManager.getDistractingAppUsageSeconds());
        for (AppInfo a : appManager.getApps()) {
            long usage = a.getCurrentUsageSeconds();
            int pct = (int) Math.round((usage * 100.0) / totalDistract);
            statsModel.addRow(new Object[]{
                    a.getAppName(),
                    a.getProcessName(),
                    Utils.formatHumanDuration(usage),
                    pct + "%"
            });
        }

        JTable statsTable = new JTable(statsModel);
        styleTable(statsTable);
        statsTable.setRowHeight(22);
        JScrollPane statsScroll = new JScrollPane(statsTable);
        styleScrollPane(statsScroll);
        mainPanel.add(statsScroll);

        dialog.add(mainPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonPanel.setBackground(PANEL_DARK);
        DarkButton resetBtn = new DarkButton("Reset Statistics");
        DarkButton closeBtn = new DarkButton("Close");

        resetBtn.addActionListener(e -> {
            int conf = JOptionPane.showConfirmDialog(dialog, "Reset all statistics for today?", "Reset", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                statsManager.resetStats();
                appManager.resetAllUsage();
                refreshMonitoredAppsTable();
                dialog.dispose();
                showStatisticsDialog();
            }
        });
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(resetBtn);
        buttonPanel.add(closeBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ---------------------------------------------------------------
    // Manage Apps Dialog
    // ---------------------------------------------------------------

    private void showManageAppsDialog() {
        JDialog dialog = new JDialog(this, "Manage Monitored Applications", true);
        dialog.setSize(620, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        styleDialog(dialog);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Display Name", "Process Name", "Limit (min)", "Enabled", "Usage", "Warning", "Blocked"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        populateManageTable(model);

        JTable table = new JTable(model);
        styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        buttonPanel.setBackground(PANEL_DARK);
        DarkButton addBtn = new DarkButton("Add App");
        DarkButton editBtn = new DarkButton("Edit");
        DarkButton deleteBtn = new DarkButton("Delete");
        DarkButton toggleBtn = new DarkButton("Enable/Disable");
        DarkButton resetBtn = new DarkButton("Reset Usage");
        DarkButton closeBtn = new DarkButton("Close");

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(toggleBtn);
        buttonPanel.add(resetBtn);
        buttonPanel.add(closeBtn);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> {
            promptAppDetails(dialog, null);
            populateManageTable(model);
            refreshMonitoredAppsTable();
        });

        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                Utils.showError(dialog, "Please select an application to edit.");
                return;
            }
            AppInfo target = appManager.getApps().get(row);
            promptAppDetails(dialog, target);
            populateManageTable(model);
            refreshMonitoredAppsTable();
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                Utils.showError(dialog, "Please select an application to remove.");
                return;
            }
            AppInfo target = appManager.getApps().get(row);
            int conf = JOptionPane.showConfirmDialog(dialog, "Remove \"" + target.getAppName() + "\" from monitoring?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                appManager.removeApp(target);
                populateManageTable(model);
                refreshMonitoredAppsTable();
            }
        });

        toggleBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                Utils.showError(dialog, "Please select an application first.");
                return;
            }
            AppInfo target = appManager.getApps().get(row);
            target.setEnabled(!target.isEnabled());
            appManager.save();
            populateManageTable(model);
            refreshMonitoredAppsTable();
        });

        resetBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                Utils.showError(dialog, "Please select an application first.");
                return;
            }
            AppInfo target = appManager.getApps().get(row);
            appManager.resetUsage(target);
            populateManageTable(model);
            refreshMonitoredAppsTable();
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void populateManageTable(DefaultTableModel model) {
        model.setRowCount(0);
        for (AppInfo a : appManager.getApps()) {
            model.addRow(new Object[]{
                    a.getAppName(),
                    a.getProcessName(),
                    a.getTimeLimitSeconds() / 60,
                    a.isEnabled() ? "Yes" : "No",
                    Utils.formatTime(a.getCurrentUsageSeconds()),
                    a.isWarningShown() ? "Yes" : "No",
                    a.isBlocked() ? "Yes" : "No"
            });
        }
    }

    private void promptAppDetails(Component parent, AppInfo existing) {
        JTextField nameField = new JTextField(existing != null ? existing.getAppName() : "");
        JTextField processField = new JTextField(existing != null ? existing.getProcessName() : "");
        JTextField limitField = new JTextField(existing != null ? String.valueOf(existing.getTimeLimitSeconds() / 60) : "30");
        JCheckBox enabledBox = new JCheckBox("Enabled", existing == null || existing.isEnabled());
        styleTextField(nameField);
        styleTextField(processField);
        styleTextField(limitField);
        styleCheckBox(enabledBox);

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(dialogLabel("Application Display Name:"));
        panel.add(nameField);
        panel.add(dialogLabel("Process Executable Name (e.g. chrome.exe):"));
        panel.add(processField);
        panel.add(dialogLabel("Daily Time Limit (minutes):"));
        panel.add(limitField);
        panel.add(enabledBox);

        int result = JOptionPane.showConfirmDialog(parent, panel,
                existing == null ? "Add Monitored Application" : "Edit Monitored Application",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Integer minutes = Utils.parsePositiveInt(limitField.getText());
        if (minutes == null) {
            Utils.showError(parent, "Time limit must be a positive whole number of minutes.");
            return;
        }

        String error = (existing == null)
                ? appManager.addApp(nameField.getText(), processField.getText(), minutes)
                : appManager.updateApp(existing, nameField.getText(), processField.getText(), minutes);

        if (error != null) {
            Utils.showError(parent, error);
            return;
        }
        if (existing != null) {
            existing.setEnabled(enabledBox.isSelected());
            appManager.save();
        }
    }

    // ---------------------------------------------------------------
    // Settings Dialog
    // ---------------------------------------------------------------

    private void showSettingsDialog() {
        JDialog dialog = new JDialog(this, "Settings", true);
        dialog.setSize(440, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        styleDialog(dialog);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(PANEL_DARK);
        tabs.setForeground(TEXT_LIGHT);

        // Pomodoro Tab
        JPanel pomodoroTab = new JPanel(new GridLayout(0, 2, 6, 8));
        pomodoroTab.setBackground(BG_DARK);
        pomodoroTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextField workField = new JTextField(String.valueOf(settingsManager.getWorkDurationMinutes()));
        JTextField shortBreakField = new JTextField(String.valueOf(settingsManager.getShortBreakMinutes()));
        JTextField longBreakField = new JTextField(String.valueOf(settingsManager.getLongBreakMinutes()));
        JTextField sessionsField = new JTextField(String.valueOf(settingsManager.getSessionsBeforeLongBreak()));
        for (JTextField f : new JTextField[]{workField, shortBreakField, longBreakField, sessionsField}) {
            styleTextField(f);
        }
        pomodoroTab.add(dialogLabel("Work Duration (min):"));
        pomodoroTab.add(workField);
        pomodoroTab.add(dialogLabel("Short Break (min):"));
        pomodoroTab.add(shortBreakField);
        pomodoroTab.add(dialogLabel("Long Break (min):"));
        pomodoroTab.add(longBreakField);
        pomodoroTab.add(dialogLabel("Sessions Before Long Break:"));
        pomodoroTab.add(sessionsField);
        tabs.addTab("Pomodoro", pomodoroTab);

        // Monitoring Tab
        JPanel monitoringTab = new JPanel(new GridLayout(0, 2, 6, 8));
        monitoringTab.setBackground(BG_DARK);
        monitoringTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTextField intervalField = new JTextField(String.valueOf(settingsManager.getMonitoringIntervalSeconds()));
        JTextField thresholdField = new JTextField(String.valueOf(settingsManager.getWarningThresholdSeconds()));
        styleTextField(intervalField);
        styleTextField(thresholdField);
        monitoringTab.add(dialogLabel("Monitoring Interval (sec):"));
        monitoringTab.add(intervalField);
        monitoringTab.add(dialogLabel("Warning Threshold (sec remaining):"));
        monitoringTab.add(thresholdField);
        tabs.addTab("Monitoring", monitoringTab);

        // Focus Mode Tab
        JPanel focusTab = new JPanel(new GridLayout(0, 1, 6, 6));
        focusTab.setBackground(BG_DARK);
        focusTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JCheckBox autoFocusBox = new JCheckBox("Auto Focus Mode during WORK", settingsManager.isAutoFocus());
        JCheckBox enforceBreaksBox = new JCheckBox("Enforce Limits During Breaks", settingsManager.isEnforceLimitsDuringBreaks());
        JCheckBox debugBox = new JCheckBox("Enable Debug Mode (Console Logging)", settingsManager.isDebugMode());
        for (JCheckBox b : new JCheckBox[]{autoFocusBox, enforceBreaksBox, debugBox}) {
            styleCheckBox(b);
            focusTab.add(b);
        }
        tabs.addTab("Focus Mode", focusTab);

        // Cat Companion Tab
        JPanel catTab = new JPanel(new GridLayout(0, 1, 6, 6));
        catTab.setBackground(BG_DARK);
        catTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JCheckBox showCatBox = new JCheckBox("Show Desktop Pink Cat", settingsManager.isShowCat());
        JCheckBox alwaysOnTopBox = new JCheckBox("Cat Always On Top", settingsManager.isAlwaysOnTop());
        JCheckBox movementBox = new JCheckBox("Cat Movement Across Screen", settingsManager.isCatMovement());
        JCheckBox enableAnimBox = new JCheckBox("Enable Animations", settingsManager.isAnimations());
        JCheckBox reduceAnimBox = new JCheckBox("Reduce Animations (Power Saver)", settingsManager.isReducedAnimations());
        for (JCheckBox b : new JCheckBox[]{showCatBox, alwaysOnTopBox, movementBox, enableAnimBox, reduceAnimBox}) {
            styleCheckBox(b);
            catTab.add(b);
        }
        tabs.addTab("Cat Companion", catTab);

        dialog.add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setBackground(PANEL_DARK);
        DarkButton saveBtn = new DarkButton("Save Settings");
        saveBtn.setAccent(true);
        DarkButton cancelBtn = new DarkButton("Cancel");
        bottom.add(saveBtn);
        bottom.add(cancelBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> {
            Integer work = Utils.parsePositiveInt(workField.getText());
            Integer shortB = Utils.parsePositiveInt(shortBreakField.getText());
            Integer longB = Utils.parsePositiveInt(longBreakField.getText());
            Integer sessions = Utils.parsePositiveInt(sessionsField.getText());
            Integer interval = Utils.parsePositiveInt(intervalField.getText());
            Integer threshold = Utils.parsePositiveInt(thresholdField.getText());

            if (work == null || shortB == null || longB == null || sessions == null
                    || interval == null || threshold == null) {
                Utils.showError(dialog, "Please enter valid positive whole numbers for all numerical fields.");
                return;
            }

            // Save to settings manager
            settingsManager.setWorkDurationMinutes(work);
            settingsManager.setShortBreakMinutes(shortB);
            settingsManager.setLongBreakMinutes(longB);
            settingsManager.setSessionsBeforeLongBreak(sessions);
            settingsManager.setMonitoringIntervalSeconds(interval);
            settingsManager.setWarningThresholdSeconds(threshold);

            settingsManager.setAutoFocus(autoFocusBox.isSelected());
            settingsManager.setEnforceLimitsDuringBreaks(enforceBreaksBox.isSelected());
            settingsManager.setDebugMode(debugBox.isSelected());
            Utils.DEBUG = debugBox.isSelected();

            settingsManager.setShowCat(showCatBox.isSelected());
            settingsManager.setAlwaysOnTop(alwaysOnTopBox.isSelected());
            settingsManager.setCatMovement(movementBox.isSelected());
            settingsManager.setAnimations(enableAnimBox.isSelected());
            settingsManager.setReducedAnimations(reduceAnimBox.isSelected());
            settingsManager.save();

            // Apply to active components
            pomodoroManager.configure(work, shortB, longB, sessions);
            processMonitor.setIntervalSeconds(interval);
            appTracker.setWarningThresholdSeconds(threshold);

            catAnimation.setAlwaysOnTop(alwaysOnTopBox.isSelected());
            catAnimation.setMovementEnabled(movementBox.isSelected());
            catAnimation.setAnimationsEnabled(enableAnimBox.isSelected());
            catAnimation.setReducedAnimation(reduceAnimBox.isSelected());
            if (showCatBox.isSelected()) {
                catAnimation.show();
            } else {
                catAnimation.hide();
            }

            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    public void initializeDisplay() {
        timeLabel.setText(Utils.formatTime(pomodoroManager.getSecondsRemaining()));
        stateLabel.setText(pomodoroManager.getState().toString().replace('_', ' '));
        sessionLabel.setText("Session " + pomodoroManager.getSessionNumber());
        updateFocusModeUI();
        refreshMonitoredAppsTable();
    }
}
