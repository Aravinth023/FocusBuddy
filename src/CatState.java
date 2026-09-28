/**
 * Visual and behavioral states of the desktop cat companion.
 *
 * Supported states:
 *   IDLE, WALK, THINK, HAPPY, ANGRY, KICK, SLEEP, TYPE
 *
 * KICK is a temporary, non-interruptible sequence (see CatAnimation) that
 * plays when a monitored application reaches its time limit.
 */
public enum CatState {
    IDLE,
    WALK,
    THINK,
    HAPPY,
    ANGRY,
    KICK,
    SLEEP,
    TYPE
}
