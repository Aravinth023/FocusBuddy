import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

/**
 * Draws and animates the desktop PINK cat companion using pure Java2D (no images),
 * hosted in a borderless, transparent, draggable always-on-top window.
 *
 * Supported states:
 *   IDLE, WALK, THINK, HAPPY, ANGRY, KICK, SLEEP, TYPE
 *
 * Enforces animation priority:
 *   KICK > ANGRY/WARNING > normal states (IDLE, WALK, THINK, HAPPY, SLEEP, TYPE).
 */
public class CatAnimation {

    public interface KickSequenceListener {
        void onTerminationPoint();
        void onSequenceComplete();
    }

    // Pink Cat Color Palette
    private static final Color BODY_COLOR = new Color(255, 175, 200);
    private static final Color BELLY_COLOR = new Color(255, 235, 243);
    private static final Color OUTLINE_COLOR = new Color(75, 45, 55);
    private static final Color NOSE_COLOR = new Color(230, 80, 115);
    private static final Color EAR_INNER = new Color(255, 140, 170);
    private static final Color BLUSH_COLOR = new Color(255, 120, 155, 130);
    private static final Color LAPTOP_COLOR = new Color(180, 185, 195);
    private static final Color LAPTOP_SCREEN = new Color(120, 200, 255);

    private CatState state = CatState.WALK;
    private CatState stateBeforeTemporary = CatState.WALK;
    private boolean temporaryActive = false;

    private int frame = 0;
    private int legPhase = 0;
    private String bubbleText = null;
    private String targetIndicatorText = null;

    private boolean animationsEnabled = true;
    private boolean reducedAnimation = false;
    private boolean movementEnabled = true;

    private double catX = 40;
    private int direction = 1;

    private final Timer animTimer;
    private Timer sequenceTimer;
    private Timer warningRevertTimer;
    private final CatWindow window;

    private KickSequenceListener kickListener;
    private int kickStep = 0;

    private long lastActivityTime = System.currentTimeMillis();
    private boolean isUserActive = true;

    public CatAnimation() {
        animTimer = new Timer(150, e -> onAnimationTick());
        window = new CatWindow();
    }

    public void start() {
        animTimer.start();
    }

    public void stop() {
        animTimer.stop();
        if (sequenceTimer != null) {
            sequenceTimer.stop();
        }
        if (warningRevertTimer != null) {
            warningRevertTimer.stop();
        }
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }

    public boolean isShown() {
        return window.isVisible();
    }

    public Window getWindowComponent() {
        return window;
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
    }

    public void setReducedAnimation(boolean reduced) {
        this.reducedAnimation = reduced;
        animTimer.setDelay(reduced ? 320 : 150);
    }

    public void setMovementEnabled(boolean enabled) {
        this.movementEnabled = enabled;
    }

    public void setAlwaysOnTop(boolean top) {
        window.setAlwaysOnTop(top);
    }

    public CatState getState() {
        return state;
    }

    public void setState(CatState newState) {
        if (temporaryActive) {
            return;
        }
        applyState(newState);
    }

    private void applyState(CatState newState) {
        this.state = newState;
        this.frame = 0;
        window.repaintCat();
    }

    public void setBubbleText(String text) {
        this.bubbleText = text;
        window.repaintCat();
    }

    public void notifyUserActivity() {
        lastActivityTime = System.currentTimeMillis();
        if (state == CatState.SLEEP && !temporaryActive) {
            applyState(CatState.IDLE);
        }
    }

    /**
     * Plays a non-interruptible ANGRY warning animation when an app is nearing its limit.
     */
    public void playWarning(String appName) {
        if (temporaryActive) {
            return;
        }
        CatState previous = state;
        temporaryActive = true;
        this.targetIndicatorText = appName + "\n1 MINUTE REMAINING!";
        applyState(CatState.ANGRY);
        setBubbleText("Focus!");

        if (warningRevertTimer != null) {
            warningRevertTimer.stop();
        }
        warningRevertTimer = new Timer(2500, e -> {
            temporaryActive = false;
            setBubbleText(null);
            targetIndicatorText = null;
            applyState(previous == CatState.ANGRY || previous == CatState.KICK ? CatState.WALK : previous);
        });
        warningRevertTimer.setRepeats(false);
        warningRevertTimer.start();
    }

    /**
     * Plays the mandatory KICK sequence when an application limit is reached:
     * ANGRY -> walk toward target -> raise foot -> kick -> impact ("POW!")
     * -> [onTerminationPoint: close app] -> ANGRY -> return to normal -> [onSequenceComplete]
     */
    public void playKickSequence(String targetAppName, KickSequenceListener listener) {
        if (temporaryActive) {
            return;
        }
        this.kickListener = listener;
        this.temporaryActive = true;
        this.stateBeforeTemporary = (state == CatState.KICK || state == CatState.ANGRY) ? CatState.WALK : state;

        this.targetIndicatorText = targetAppName + "\nLIMIT REACHED!";
        applyState(CatState.ANGRY);
        setBubbleText("HEY!");
        kickStep = 0;

        int stepDelay = reducedAnimation ? 300 : 220;
        if (sequenceTimer != null) {
            sequenceTimer.stop();
        }
        sequenceTimer = new Timer(stepDelay, e -> advanceKickStep());
        sequenceTimer.start();
    }

    private void advanceKickStep() {
        kickStep++;
        switch (kickStep) {
            case 1:
                // Cat is angry
                break;
            case 2:
                // Cat advances and prepares to kick
                applyState(CatState.KICK);
                frame = 1;
                catX += direction * 6;
                break;
            case 3:
                // Cat raises one foot
                frame = 2;
                break;
            case 4:
                // Cat extends foot forward in powerful kick
                frame = 3;
                break;
            case 5:
                // Impact ("POW!")
                frame = 4;
                setBubbleText("POW!");
                break;
            case 6:
                // App is closed at the impact moment
                frame = 5;
                if (kickListener != null) {
                    kickListener.onTerminationPoint();
                }
                break;
            case 7:
                // Cat stays briefly angry and satisfied
                applyState(CatState.ANGRY);
                setBubbleText("Blocked!");
                break;
            case 8:
                // Return to normal state
                frame = 0;
                setBubbleText(null);
                targetIndicatorText = null;
                temporaryActive = false;
                applyState(stateBeforeTemporary);
                sequenceTimer.stop();
                if (kickListener != null) {
                    kickListener.onSequenceComplete();
                }
                break;
            default:
                sequenceTimer.stop();
        }
        window.repaintCat();
    }

    private void onAnimationTick() {
        if (!animationsEnabled) {
            return;
        }
        frame = (frame + 1) % 4;

        // Inactivity detection: sleep after 60s of no activity
        if (!temporaryActive && state != CatState.SLEEP) {
            if (System.currentTimeMillis() - lastActivityTime > 60000) {
                applyState(CatState.SLEEP);
            }
        }

        if (state == CatState.WALK && movementEnabled && !temporaryActive) {
            legPhase = (legPhase + 1) % 4;
            catX += direction * (reducedAnimation ? 1.0 : 2.0);
            Rectangle bounds = window.getWalkBounds();
            if (catX < bounds.x) {
                catX = bounds.x;
                direction = 1;
            } else if (catX > bounds.x + bounds.width) {
                catX = bounds.x + bounds.width;
                direction = -1;
            }
        }
        window.repaintCat();
    }

    // ---------------------------------------------------------------
    // Java2D Rendering
    // ---------------------------------------------------------------

    private void paintCat(Graphics2D g2, int panelWidth, int panelHeight) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cx = panelWidth / 2;
        int cy = panelHeight / 2 + 10;
        g2.translate(cx, cy);
        if (direction < 0 && state != CatState.TYPE) {
            g2.scale(-1, 1);
        }

        boolean walking = state == CatState.WALK && !reducedAnimation;
        int bodyBob = walking && (legPhase % 2 == 0) ? -2 : 0;
        if (state == CatState.HAPPY) {
            bodyBob = (frame % 2 == 0) ? -4 : 0;
        } else if (state == CatState.IDLE) {
            bodyBob = (frame % 2 == 0) ? -1 : 0;
        }

        boolean angryFace = state == CatState.ANGRY || state == CatState.KICK;

        // 1. Tail
        g2.setColor(BODY_COLOR);
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D tail = new Path2D.Double();
        tail.moveTo(-18, 6 + bodyBob);
        int tailLift = state == CatState.HAPPY ? -24 : (state == CatState.SLEEP ? 4 : -6);
        tail.quadTo(-32, tailLift, -26, -16 + bodyBob);
        g2.draw(tail);

        // 2. Back legs
        int legY = 14 + bodyBob;
        int[] legSwing = walking ? ((legPhase == 0) ? new int[]{-4, 4, 4, -4}
                : (legPhase == 2) ? new int[]{4, -4, -4, 4} : new int[]{0, 0, 0, 0})
                : new int[]{0, 0, 0, 0};

        if (state != CatState.SLEEP && state != CatState.TYPE) {
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(OUTLINE_COLOR);
            g2.drawLine(-14, legY, -14 + legSwing[0], legY + 10);
            g2.drawLine(-4, legY, -4 + legSwing[1], legY + 10);
        }

        // 3. Front legs
        int kickLift = 0;
        int kickExtend = 0;
        if (state == CatState.KICK) {
            if (frame == 2) {
                kickLift = -6;
            } else if (frame == 3 || frame == 4) {
                kickLift = -12;
                kickExtend = 16;
            }
        }

        if (state != CatState.SLEEP && state != CatState.TYPE) {
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(OUTLINE_COLOR);
            g2.drawLine(8, legY + kickLift, 8 + legSwing[2], legY + 10 + (kickLift < 0 ? -kickLift : 0));
            g2.drawLine(14, legY + kickLift, 14 + legSwing[3] + kickExtend, legY + 10 + (kickLift < 0 ? -kickLift : 0));
        }

        // 4. Body
        g2.setColor(BODY_COLOR);
        Ellipse2D bodyShape = (state == CatState.SLEEP)
                ? new Ellipse2D.Double(-22, -4, 44, 26)
                : new Ellipse2D.Double(-20, -10 + bodyBob, 38, 26);
        g2.fill(bodyShape);
        g2.setColor(OUTLINE_COLOR);
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(bodyShape);

        // 5. Belly
        g2.setColor(BELLY_COLOR);
        if (state != CatState.SLEEP) {
            g2.fill(new Ellipse2D.Double(-10, 1 + bodyBob, 20, 13));
        }

        // 6. Head
        int headY = -24 + bodyBob + (state == CatState.THINK ? -3 : 0);
        if (state == CatState.SLEEP) {
            headY = -12;
        }
        g2.setColor(BODY_COLOR);
        Ellipse2D head = new Ellipse2D.Double(-14, headY, 28, 24);
        g2.fill(head);
        g2.setColor(OUTLINE_COLOR);
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(head);

        // 7. Ears & Inner Ears
        Path2D earLeft = new Path2D.Double();
        earLeft.moveTo(-12, headY + 3);
        earLeft.lineTo(-6, headY + 3);
        earLeft.lineTo(-13, headY - 10);
        earLeft.closePath();

        Path2D earRight = new Path2D.Double();
        earRight.moveTo(12, headY + 3);
        earRight.lineTo(6, headY + 3);
        earRight.lineTo(13, headY - 10);
        earRight.closePath();

        g2.setColor(BODY_COLOR);
        g2.fill(earLeft);
        g2.fill(earRight);
        g2.setColor(OUTLINE_COLOR);
        g2.draw(earLeft);
        g2.draw(earRight);

        // Inner ears pink
        g2.setColor(EAR_INNER);
        Path2D inLeft = new Path2D.Double();
        inLeft.moveTo(-11, headY + 2);
        inLeft.lineTo(-7, headY + 2);
        inLeft.lineTo(-12, headY - 7);
        inLeft.closePath();
        Path2D inRight = new Path2D.Double();
        inRight.moveTo(11, headY + 2);
        inRight.lineTo(7, headY + 2);
        inRight.lineTo(12, headY - 7);
        inRight.closePath();
        g2.fill(inLeft);
        g2.fill(inRight);

        // 8. Cheeks / Blush
        g2.setColor(BLUSH_COLOR);
        g2.fillOval(-12, headY + 13, 5, 3);
        g2.fillOval(7, headY + 13, 5, 3);

        // 9. Eyes
        g2.setColor(Color.BLACK);
        if (angryFace) {
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(-9, headY + 8, -3, headY + 11);
            g2.drawLine(3, headY + 11, 9, headY + 8);
            g2.fillOval(-8, headY + 10, 5, 5);
            g2.fillOval(3, headY + 10, 5, 5);
        } else if (state == CatState.SLEEP) {
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(-9, headY + 9, 6, 4, 180, 180);
            g2.drawArc(3, headY + 9, 6, 4, 180, 180);
        } else if (state == CatState.HAPPY) {
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawArc(-9, headY + 8, 6, 5, 0, 180);
            g2.drawArc(3, headY + 8, 6, 5, 0, 180);
        } else {
            // Cute round eyes with white shine
            g2.fillOval(-8, headY + 9, 5, 6);
            g2.fillOval(3, headY + 9, 5, 6);
            g2.setColor(Color.WHITE);
            g2.fillOval(-7, headY + 10, 2, 2);
            g2.fillOval(4, headY + 10, 2, 2);
        }

        // 10. Nose
        g2.setColor(NOSE_COLOR);
        g2.fillOval(-2, headY + 15, 4, 3);

        // 11. Mouth
        g2.setColor(OUTLINE_COLOR);
        g2.setStroke(new BasicStroke(1.4f));
        if (state == CatState.HAPPY) {
            g2.drawArc(-6, headY + 15, 12, 7, 200, 140);
        } else if (angryFace) {
            g2.drawLine(-4, headY + 19, 4, headY + 19);
        } else if (state != CatState.SLEEP) {
            g2.drawArc(-4, headY + 16, 4, 3, 200, 140);
            g2.drawArc(0, headY + 16, 4, 3, 200, 140);
        }

        // 12. Whiskers
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(-13, headY + 14, -22, headY + 12);
        g2.drawLine(-13, headY + 17, -22, headY + 17);
        g2.drawLine(13, headY + 14, 22, headY + 12);
        g2.drawLine(13, headY + 17, 22, headY + 17);

        // 13. TYPE state: mini laptop + typing paws
        if (state == CatState.TYPE) {
            // Laptop base
            g2.setColor(LAPTOP_COLOR);
            g2.fill(new RoundRectangle2D.Double(8, 8, 22, 6, 2, 2));
            // Laptop screen
            g2.setColor(Color.DARK_GRAY);
            g2.fill(new RoundRectangle2D.Double(22, -6, 4, 16, 2, 2));
            g2.setColor(LAPTOP_SCREEN);
            g2.fill(new Rectangle(23, -5, 2, 14));

            // Typing paws alternately tapping
            int paw1Y = (frame % 2 == 0) ? 6 : 9;
            int paw2Y = (frame % 2 == 0) ? 9 : 6;
            g2.setColor(BODY_COLOR);
            g2.fillOval(6, paw1Y, 7, 5);
            g2.fillOval(14, paw2Y, 7, 5);
            g2.setColor(OUTLINE_COLOR);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(6, paw1Y, 7, 5);
            g2.drawOval(14, paw2Y, 7, 5);
        }
    }

    private void paintOverlays(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Thought / Speech Bubble
        if (bubbleText != null) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            int textW = g2.getFontMetrics().stringWidth(bubbleText);
            int bw = textW + 14;
            int bx = w / 2 + 8;
            int by = h / 2 - 50;
            g2.setColor(new Color(255, 255, 255, 240));
            g2.fillRoundRect(bx, by, bw, 20, 10, 10);
            g2.setColor(new Color(60, 60, 70));
            g2.drawRoundRect(bx, by, bw, 20, 10, 10);
            g2.drawString(bubbleText, bx + 7, by + 14);
        } else if (state == CatState.THINK) {
            // Little thought bubbles
            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillOval(w / 2 + 12, h / 2 - 32, 5, 5);
            g2.fillOval(w / 2 + 17, h / 2 - 42, 8, 8);
            g2.fillOval(w / 2 + 22, h / 2 - 58, 20, 16);
            g2.setColor(new Color(80, 80, 90));
            g2.drawOval(w / 2 + 22, h / 2 - 58, 20, 16);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("...", w / 2 + 28, h / 2 - 46);
        } else if (state == CatState.SLEEP) {
            // Floating "Zzz"
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(new Color(120, 165, 255, 220));
            int zOffset = (frame % 4) * 2;
            g2.drawString("z", w / 2 + 14, h / 2 - 20 - zOffset);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Z", w / 2 + 22, h / 2 - 28 - zOffset);
        }

        // Target Indicator Notification
        if (targetIndicatorText != null) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            String[] lines = targetIndicatorText.split("\n");
            int lineHeight = 13;
            int boxHeight = lines.length * lineHeight + 8;
            int maxTextWidth = 0;
            for (String line : lines) {
                maxTextWidth = Math.max(maxTextWidth, g2.getFontMetrics().stringWidth(line));
            }
            int boxY = h - boxHeight - 4;
            g2.setColor(new Color(230, 60, 90, 240));
            g2.fillRoundRect(w / 2 - maxTextWidth / 2 - 8, boxY, maxTextWidth + 16, boxHeight, 8, 8);
            g2.setColor(Color.WHITE);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineX = w / 2 - g2.getFontMetrics().stringWidth(line) / 2;
                g2.drawString(line, lineX, boxY + 13 + i * lineHeight);
            }
        }

        // POW effect during kick
        if (state == CatState.KICK && frame == 4) {
            g2.setColor(new Color(255, 215, 0));
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            int px = w / 2 + (direction > 0 ? 22 : -54);
            g2.drawString("POW!", px, h / 2 - 4);
        }
    }

    // ---------------------------------------------------------------
    // Desktop Companion Window
    // ---------------------------------------------------------------

    private class CatWindow extends JWindow {
        private final CatPanel panel;
        private Point dragOffset;

        CatWindow() {
            setSize(160, 140);
            setLocation(250, 250);
            setAlwaysOnTop(true);
            setFocusableWindowState(false);
            applyTransparencyIfSupported();

            panel = new CatPanel();
            setContentPane(panel);

            MouseAdapter dragHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragOffset = e.getPoint();
                    notifyUserActivity();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    Point onScreen = e.getLocationOnScreen();
                    setLocation(onScreen.x - dragOffset.x, onScreen.y - dragOffset.y);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    notifyUserActivity();
                    if (!temporaryActive) {
                        applyState(CatState.HAPPY);
                        setBubbleText("Meow!");
                        Timer t = new Timer(1500, ev -> {
                            setBubbleText(null);
                            applyState(CatState.IDLE);
                        });
                        t.setRepeats(false);
                        t.start();
                    }
                }
            };
            panel.addMouseListener(dragHandler);
            panel.addMouseMotionListener(dragHandler);
        }

        private void applyTransparencyIfSupported() {
            try {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                    setBackground(new Color(0, 0, 0, 0));
                }
            } catch (Exception ignored) {}
        }

        Rectangle getWalkBounds() {
            return new Rectangle(-40, -40, 80, 80);
        }

        void repaintCat() {
            panel.repaint();
        }

        private class CatPanel extends JPanel {
            CatPanel() {
                setOpaque(false);
                setPreferredSize(new Dimension(160, 140));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.Clear);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
                g2.dispose();

                Graphics2D catG2 = (Graphics2D) g.create();
                paintCat(catG2, getWidth(), getHeight());
                catG2.dispose();

                Graphics2D overlayG2 = (Graphics2D) g.create();
                paintOverlays(overlayG2, getWidth(), getHeight());
                overlayG2.dispose();
            }
        }
    }
}
