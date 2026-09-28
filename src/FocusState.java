/**
 * The three states of the Pomodoro focus cycle.
 *
 * Transition rules (handled by PomodoroManager):
 *   WORK -> SHORT_BREAK -> WORK -> SHORT_BREAK -> ...
 *   After every N completed WORK sessions: WORK -> LONG_BREAK -> WORK
 */
public enum FocusState {
    WORK,
    SHORT_BREAK,
    LONG_BREAK
}
