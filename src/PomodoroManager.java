import javax.swing.Timer;

/**
 * Drives the Pomodoro finite state machine (FocusState) and its countdown timer.
 *
 * Deterministic transition rules:
 *   WORK -> SHORT_BREAK -> WORK -> SHORT_BREAK -> ...
 *   After every Nth completed WORK session -> LONG_BREAK
 *   SHORT_BREAK / LONG_BREAK -> WORK
 */
public class PomodoroManager {

    public interface PomodoroListener {
        void onTick(int secondsRemaining, FocusState state, int sessionNumber);
        void onStateChanged(FocusState newState, int sessionNumber);
        void onSessionCompleted(FocusState completedState, int sessionNumber);
    }

    private int workSeconds = 25 * 60;
    private int shortBreakSeconds = 5 * 60;
    private int longBreakSeconds = 15 * 60;
    private int sessionsBeforeLongBreak = 4;

    private FocusState state = FocusState.WORK;
    private int secondsRemaining = workSeconds;
    private int completedWorkSessions = 0;
    private int sessionNumber = 1;
    private boolean running = false;

    private final Timer timer;
    private PomodoroListener listener;

    public PomodoroManager() {
        timer = new Timer(1000, e -> tick());
        timer.setRepeats(true);
    }

    public void setListener(PomodoroListener listener) {
        this.listener = listener;
    }

    public void configure(int workMinutes, int shortBreakMinutes, int longBreakMinutes, int sessionsBeforeLongBreak) {
        this.workSeconds = Math.max(1, workMinutes) * 60;
        this.shortBreakSeconds = Math.max(1, shortBreakMinutes) * 60;
        this.longBreakSeconds = Math.max(1, longBreakMinutes) * 60;
        this.sessionsBeforeLongBreak = Math.max(1, sessionsBeforeLongBreak);
        if (!running) {
            secondsRemaining = durationFor(state);
            notifyTick();
        }
    }

    public void start() {
        if (!running) {
            running = true;
            timer.start();
        }
    }

    public void resume() {
        start();
    }

    public void pause() {
        running = false;
        timer.stop();
    }

    public void reset() {
        pause();
        state = FocusState.WORK;
        completedWorkSessions = 0;
        sessionNumber = 1;
        secondsRemaining = workSeconds;
        notifyStateChanged();
        notifyTick();
    }

    public void skip() {
        advanceState(false);
    }

    private void tick() {
        secondsRemaining--;
        if (secondsRemaining <= 0) {
            advanceState(true);
        } else {
            notifyTick();
        }
    }

    private void advanceState(boolean completedNormally) {
        FocusState finishedState = state;
        if (completedNormally && listener != null) {
            listener.onSessionCompleted(finishedState, sessionNumber);
        }

        if (state == FocusState.WORK) {
            completedWorkSessions++;
            if (completedWorkSessions % sessionsBeforeLongBreak == 0) {
                state = FocusState.LONG_BREAK;
            } else {
                state = FocusState.SHORT_BREAK;
            }
        } else {
            state = FocusState.WORK;
            sessionNumber++;
        }

        secondsRemaining = durationFor(state);
        notifyStateChanged();
        notifyTick();
    }

    private int durationFor(FocusState s) {
        switch (s) {
            case WORK:
                return workSeconds;
            case SHORT_BREAK:
                return shortBreakSeconds;
            case LONG_BREAK:
                return longBreakSeconds;
            default:
                return workSeconds;
        }
    }

    private void notifyTick() {
        if (listener != null) {
            listener.onTick(secondsRemaining, state, sessionNumber);
        }
    }

    private void notifyStateChanged() {
        if (listener != null) {
            listener.onStateChanged(state, sessionNumber);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public FocusState getState() {
        return state;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public int getCompletedWorkSessions() {
        return completedWorkSessions;
    }

    public int getWorkMinutes() {
        return workSeconds / 60;
    }

    public int getShortBreakMinutes() {
        return shortBreakSeconds / 60;
    }

    public int getLongBreakMinutes() {
        return longBreakSeconds / 60;
    }

    public int getSessionsBeforeLongBreak() {
        return sessionsBeforeLongBreak;
    }
}
