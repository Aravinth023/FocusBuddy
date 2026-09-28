import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * A custom self-painting JButton designed with FocusBuddy's dark theme
 * and vibrant pink accents matching the desktop cat companion.
 */
public class DarkButton extends JButton {

    private static final Color BASE_BG = new Color(46, 46, 56);
    private static final Color HOVER_BG = new Color(64, 64, 78);
    private static final Color PRESSED_BG = new Color(34, 34, 42);
    private static final Color DISABLED_BG = new Color(40, 40, 46);

    // Pink accent theme colors
    private static final Color ACCENT_BG = new Color(216, 27, 96);
    private static final Color ACCENT_HOVER_BG = new Color(233, 30, 99);
    private static final Color ACCENT_PRESSED_BG = new Color(173, 20, 87);

    private static final Color BORDER_COLOR = new Color(74, 74, 88);
    private static final Color BORDER_HOVER = new Color(244, 143, 177);
    private static final Color FOCUS_RING = new Color(240, 98, 146, 140);

    private static final Color TEXT_COLOR = new Color(240, 240, 245);
    private static final Color TEXT_DISABLED = new Color(125, 125, 135);

    private boolean accent = false;

    public DarkButton(String text) {
        super(text);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(TEXT_COLOR);
        setFont(new Font("SansSerif", Font.BOLD, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
    }

    public void setAccent(boolean accent) {
        this.accent = accent;
        repaint();
    }

    public boolean isAccent() {
        return accent;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg;
        if (!isEnabled()) {
            bg = DISABLED_BG;
        } else if (accent) {
            if (getModel().isPressed()) {
                bg = ACCENT_PRESSED_BG;
            } else if (getModel().isRollover()) {
                bg = ACCENT_HOVER_BG;
            } else {
                bg = ACCENT_BG;
            }
        } else {
            if (getModel().isPressed()) {
                bg = PRESSED_BG;
            } else if (getModel().isRollover()) {
                bg = HOVER_BG;
            } else {
                bg = BASE_BG;
            }
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

        boolean highlightBorder = isEnabled() && (accent || getModel().isRollover());
        g2.setColor(highlightBorder ? BORDER_HOVER : BORDER_COLOR);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

        if (isFocusOwner() && isEnabled()) {
            g2.setColor(FOCUS_RING);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 9, 9);
        }
        g2.dispose();

        setForeground(isEnabled() ? TEXT_COLOR : TEXT_DISABLED);
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 74), Math.max(d.height, 30));
    }
}
