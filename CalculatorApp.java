import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.RadialGradientPaint;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;


public class CalculatorApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            CalculatorEngine engine = new CalculatorEngine();
            Theme theme = Theme.DARK();
            CalculatorView view = new CalculatorView(theme);
            new CalculatorController(engine, view);
            view.setVisible(true);
        });
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        } catch (Exception ignored) { }
    }
}

/* ======= ENGINE (MODEL) ======= */

enum Operator {
    ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷");
    final String label;
    Operator(String label) { this.label = label; }
}

class CalculatorEngine {
    private final MathContext mc = new MathContext(16, RoundingMode.HALF_UP);

    private BigDecimal accumulator = BigDecimal.ZERO;
    private Operator pendingOperator = null;
    private Operator lastOperator = null;
    private BigDecimal lastOperand = null;

    public void clearAll() {
        accumulator = BigDecimal.ZERO;
        pendingOperator = null;
        lastOperator = null;
        lastOperand = null;
    }

    public BigDecimal getAccumulator() { return accumulator; }
    public Operator getPendingOperator() { return pendingOperator; }

    public void replacePendingOperator(Operator op) {
        pendingOperator = op;
    }

    public void setOperator(Operator op, BigDecimal entry) {
        if (pendingOperator != null) {
            accumulator = compute(accumulator, entry, pendingOperator);
        } else {
            accumulator = entry;
        }
        pendingOperator = op;
        lastOperator = null;
        lastOperand = null;
    }

    public BigDecimal equals(BigDecimal entry) {
        if (pendingOperator != null) {
            accumulator = compute(accumulator, entry, pendingOperator);
            lastOperator = pendingOperator;
            lastOperand = entry;
            pendingOperator = null;
            return accumulator;
        } else if (lastOperator != null && lastOperand != null) {
            accumulator = compute(accumulator, lastOperand, lastOperator);
            return accumulator;
        } else {
            accumulator = entry;
            return accumulator;
        }
    }

    public MathContext getMathContext() { return mc; }

    private BigDecimal compute(BigDecimal a, BigDecimal b, Operator op) {
        switch (op) {
            case ADD: return a.add(b, mc);
            case SUBTRACT: return a.subtract(b, mc);
            case MULTIPLY: return a.multiply(b, mc);
            case DIVIDE:
                if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Division by zero");
                return a.divide(b, mc);
            default:
                throw new IllegalStateException("Unknown operator: " + op);
        }
    }
}

/* ======= THEME (DARK ONLY) ======= */

class Theme {
    final Color bgGradTop, bgGradBottom;
    final Color glassFill, glassStroke, glassShadow;
    final Color txtPrimary, txtSecondary, txtMuted;
    final Color digitTop, digitBottom, digitStroke;
    final Color funcTop, funcBottom, funcStroke;
    final Color opTop, opBottom, opStroke;
    final Color eqTop, eqBottom, eqStroke;
    final Color hoverOverlay, pressOverlay, accent;

    Theme(Color bgGradTop, Color bgGradBottom,
          Color glassFill, Color glassStroke, Color glassShadow,
          Color txtPrimary, Color txtSecondary, Color txtMuted,
          Color digitTop, Color digitBottom, Color digitStroke,
          Color funcTop, Color funcBottom, Color funcStroke,
          Color opTop, Color opBottom, Color opStroke,
          Color eqTop, Color eqBottom, Color eqStroke,
          Color hoverOverlay, Color pressOverlay, Color accent) {
        this.bgGradTop = bgGradTop;
        this.bgGradBottom = bgGradBottom;
        this.glassFill = glassFill;
        this.glassStroke = glassStroke;
        this.glassShadow = glassShadow;
        this.txtPrimary = txtPrimary;
        this.txtSecondary = txtSecondary;
        this.txtMuted = txtMuted;
        this.digitTop = digitTop;
        this.digitBottom = digitBottom;
        this.digitStroke = digitStroke;
        this.funcTop = funcTop;
        this.funcBottom = funcBottom;
        this.funcStroke = funcStroke;
        this.opTop = opTop;
        this.opBottom = opBottom;
        this.opStroke = opStroke;
        this.eqTop = eqTop;
        this.eqBottom = eqBottom;
        this.eqStroke = eqStroke;
        this.hoverOverlay = hoverOverlay;
        this.pressOverlay = pressOverlay;
        this.accent = accent;
    }

    static Theme DARK() {
        return new Theme(
            new Color(0x12161C), new Color(0x0B0F14),           // bg gradient
            new Color(28, 34, 44, 220), new Color(255,255,255,30), new Color(0,0,0,120), // glass
            new Color(0xE8EDF2), new Color(0x9AA3AE), new Color(0x6B7280), // text
            new Color(0x232A34), new Color(0x1A2029), new Color(255,255,255,25), // digit keys
            new Color(0x2B3441), new Color(0x222A36), new Color(255,255,255,35), // function keys
            new Color(0x374A62), new Color(0x2B3A50), new Color(255,255,255,50), // operators
            new Color(0x22C55E), new Color(0x16A34A), new Color(0xA7F3D0),       // equals (green)
            new Color(255,255,255,35), new Color(0,0,0,60), new Color(0x22D3EE)  // hover, press, accent
        );
    }
}

/* ======= CUSTOM UI COMPONENTS ======= */

enum ButtonRole { DIGIT, FUNCTION, OPERATOR, EQUALS, MEMORY }

class GradientPanel extends JPanel {
    private final Theme theme;
    public GradientPanel(Theme theme) {
        this.theme = theme;
        setOpaque(false);
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, theme.bgGradTop, 0, getHeight(), theme.bgGradBottom);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

class GlassPanel extends JPanel {
    private final Theme theme;
    private final int arc = 24;
    public GlassPanel(Theme theme) {
        this.theme = theme;
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Shape r = new RoundRectangle2D.Double(5, 7, getWidth()-10, getHeight()-12, arc+8, arc+8);
            g2.setColor(theme.glassShadow);
            g2.fill(r);

            Shape rr = new RoundRectangle2D.Double(0, 0, getWidth()-0.5, getHeight()-0.5, arc, arc);
            g2.setColor(theme.glassFill);
            g2.fill(rr);

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(theme.glassStroke);
            g2.draw(rr);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

class NeoButton extends JButton {
    private final Theme theme;
    private final ButtonRole role;
    private boolean hovered = false;
    private boolean pressed = false;
    private float ripple = 0f;
    private Point ripplePoint = new Point(0, 0);
    private javax.swing.Timer rippleTimer; // Swing timer

    public NeoButton(String text, String cmd, Theme theme, ButtonRole role) {
        super(text);
        this.theme = theme;
        this.role = role;
        setActionCommand(cmd);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(theme.txtPrimary);
        setFont(getFont().deriveFont(Font.BOLD, 18f));
        setMargin(new Insets(10, 14, 10, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseInputAdapter mia = new MouseInputAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                pressed = true;
                ripple = 0f;
                ripplePoint = e.getPoint();
                startRipple();
                repaint();
            }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) {
                pressed = false;
                repaint();
            }
        };
        addMouseListener(mia);
    }

    private void startRipple() {
        if (rippleTimer != null && rippleTimer.isRunning()) rippleTimer.stop();
        rippleTimer = new javax.swing.Timer(16, e -> {
            ripple += 0.06f;
            if (ripple >= 1f) ((javax.swing.Timer)e.getSource()).stop();
            repaint();
        });
        rippleTimer.start();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(64, d.width + 6), Math.max(48, d.height + 6));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color top, bottom, stroke;
            switch (role) {
                case DIGIT: top = theme.digitTop; bottom = theme.digitBottom; stroke = theme.digitStroke; break;
                case FUNCTION: top = theme.funcTop; bottom = theme.funcBottom; stroke = theme.funcStroke; break;
                case OPERATOR: top = theme.opTop; bottom = theme.opBottom; stroke = theme.opStroke; break;
                case EQUALS: top = theme.eqTop; bottom = theme.eqBottom; stroke = theme.eqStroke; break;
                case MEMORY: top = mix(theme.funcTop, theme.digitTop, 0.5f); bottom = mix(theme.funcBottom, theme.digitBottom, 0.5f); stroke = theme.funcStroke; break;
                default: top = theme.digitTop; bottom = theme.digitBottom; stroke = theme.digitStroke;
            }

            Shape rr = new RoundRectangle2D.Double(2, 2, getWidth()-4, getHeight()-4, 16, 16);

            g2.setColor(new Color(0,0,0, role == ButtonRole.EQUALS ? 70 : 50));
            g2.fill(new RoundRectangle2D.Double(4, 5, getWidth()-8, getHeight()-8, 18, 18));

            GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
            g2.setPaint(gp);
            g2.fill(rr);

            if (hovered) {
                g2.setColor(theme.hoverOverlay);
                g2.fill(rr);
            }
            if (pressed) {
                g2.setColor(theme.pressOverlay);
                g2.fill(rr);
            }

            if (ripple > 0f) {
                float maxR = (float) Math.hypot(getWidth(), getHeight());
                float r = ripple * maxR;
                Paint rp = new RadialGradientPaint(ripplePoint, r,
                    new float[]{0f, 1f},
                    new Color[]{new Color(255,255,255,70), new Color(255,255,255,0)});
                g2.setPaint(rp);
                g2.setClip(rr);
                g2.fill(new java.awt.geom.Ellipse2D.Float(ripplePoint.x - r, ripplePoint.y - r, r*2, r*2));
            }

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(stroke);
            g2.draw(rr);

            g2.setFont(getFont());
            g2.setColor(getTextColor());
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int tx = (getWidth() - fm.stringWidth(text)) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            if (!isEnabled()) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }

    private Color getTextColor() {
        if (role == ButtonRole.EQUALS || role == ButtonRole.OPERATOR) return Color.WHITE;
        return theme.txtPrimary;
    }

    private static Color mix(Color a, Color b, float f) {
        float inv = 1f - f;
        return new Color(
            Math.round(a.getRed() * inv + b.getRed() * f),
            Math.round(a.getGreen() * inv + b.getGreen() * f),
            Math.round(a.getBlue() * inv + b.getBlue() * f),
            Math.round(a.getAlpha() * inv + b.getAlpha() * f)
        );
    }
}

/* ======= VIEW (UI) ======= */

class CalculatorView extends JFrame {
    private final Theme theme;

    private final JTextField display = new JTextField("0");
    private final JLabel preview = new JLabel(" ");
    private final JLabel memTag = new JLabel(" ");

    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);

    private final Map<String, NeoButton> buttons = new LinkedHashMap<>();
    private Consumer<String> actionHandler;

    private final GradientPanel root;
    private final GlassPanel glass;

    public CalculatorView(Theme theme) {
        super("Calculator — Dark");
        this.theme = theme;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 540));
        setLocationByPlatform(true);

        root = new GradientPanel(theme);
        root.setLayout(new BorderLayout(18, 18));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        glass = new GlassPanel(theme);
        glass.setLayout(new BorderLayout(12, 12));
        root.add(glass, BorderLayout.CENTER);

        JPanel main = buildMainPanel();
        glass.add(main, BorderLayout.CENTER);

        JPanel history = buildHistoryPanel();
        glass.add(history, BorderLayout.EAST);

        JPanel topBar = buildTopBar();
        root.add(topBar, BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
        refreshTheme();
    }

    private JPanel buildTopBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel title = new JLabel("Calculator");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(theme.txtPrimary);
        p.add(title, BorderLayout.WEST);
        return p;
    }

    private JPanel buildMainPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(10, 10)) { { setOpaque(false); } };

        JPanel displayPanel = new JPanel(new BorderLayout(6, 6)) {{ setOpaque(false); }};
        JPanel previewRow = new JPanel(new BorderLayout()) {{ setOpaque(false); }};
        preview.setFont(preview.getFont().deriveFont(Font.PLAIN, 14f));
        preview.setForeground(theme.txtSecondary);
        memTag.setFont(memTag.getFont().deriveFont(Font.BOLD, 12f));
        memTag.setForeground(theme.accent);
        previewRow.add(preview, BorderLayout.WEST);
        previewRow.add(memTag, BorderLayout.EAST);
        displayPanel.add(previewRow, BorderLayout.NORTH);

        display.setEditable(false);
        display.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(display.getFont().deriveFont(Font.BOLD, 36f));
        display.setForeground(theme.txtPrimary);
        display.setOpaque(false);
        displayPanel.add(display, BorderLayout.CENTER);

        // Display copy/paste
        JPopupMenu pm = new JPopupMenu();
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem paste = new JMenuItem("Paste");
        copy.addActionListener(e -> {
            display.selectAll();
            display.copy();
            display.select(0,0);
        });
        paste.addActionListener(e -> {
            try {
                String s = Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor).toString().trim();
                if (actionHandler != null) actionHandler.accept("PASTE:" + s);
            } catch (Exception ignored) {}
        });
        pm.add(copy); pm.add(paste);
        display.setComponentPopupMenu(pm);

        wrapper.add(displayPanel, BorderLayout.NORTH);

        // Memory row
        JPanel memRow = new JPanel(new GridLayout(1, 5, 8, 8)) {{ setOpaque(false); setBorder(new EmptyBorder(6,0,2,0)); }};
        addButton(memRow, "MC", "MEM_CLEAR", ButtonRole.MEMORY);
        addButton(memRow, "MR", "MEM_RECALL", ButtonRole.MEMORY);
        addButton(memRow, "MS", "MEM_STORE", ButtonRole.MEMORY);
        addButton(memRow, "M+", "MEM_ADD", ButtonRole.MEMORY);
        addButton(memRow, "M−", "MEM_SUB", ButtonRole.MEMORY);
        wrapper.add(memRow, BorderLayout.CENTER);

        // Keypad
        JPanel grid = new JPanel(new GridLayout(6, 4, 10, 10)) {{ setOpaque(false); }};
        // Row 1
        addButton(grid, "AC", "AC", ButtonRole.FUNCTION);        // All Clear (resets engine)
        addButton(grid, "C", "CE", ButtonRole.FUNCTION);         // Clear (entry only) — working
        addButton(grid, "⌫", "BACK", ButtonRole.FUNCTION);
        addButton(grid, "÷", "DIVIDE", ButtonRole.OPERATOR);
        // Row 2
        addButton(grid, "√", "SQRT", ButtonRole.FUNCTION);
        addButton(grid, "%", "PERCENT", ButtonRole.FUNCTION);
        addButton(grid, "±", "NEGATE", ButtonRole.FUNCTION);
        addButton(grid, "×", "MULTIPLY", ButtonRole.OPERATOR);
        // Row 3
        addButton(grid, "7", "DIGIT_7", ButtonRole.DIGIT);
        addButton(grid, "8", "DIGIT_8", ButtonRole.DIGIT);
        addButton(grid, "9", "DIGIT_9", ButtonRole.DIGIT);
        addButton(grid, "−", "SUBTRACT", ButtonRole.OPERATOR);
        // Row 4
        addButton(grid, "4", "DIGIT_4", ButtonRole.DIGIT);
        addButton(grid, "5", "DIGIT_5", ButtonRole.DIGIT);
        addButton(grid, "6", "DIGIT_6", ButtonRole.DIGIT);
        addButton(grid, "+", "ADD", ButtonRole.OPERATOR);
        // Row 5
        addButton(grid, "1", "DIGIT_1", ButtonRole.DIGIT);
        addButton(grid, "2", "DIGIT_2", ButtonRole.DIGIT);
        addButton(grid, "3", "DIGIT_3", ButtonRole.DIGIT);
        addButton(grid, "=", "EQUALS", ButtonRole.EQUALS);
        // Row 6
        addButton(grid, "0", "DIGIT_0", ButtonRole.DIGIT);
        addButton(grid, ".", "DECIMAL", ButtonRole.DIGIT);
        addButton(grid, "(", "NOP", ButtonRole.FUNCTION).setEnabled(false); // visual spacer
        addButton(grid, ")", "NOP", ButtonRole.FUNCTION).setEnabled(false);

        wrapper.add(grid, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6)) {{ setOpaque(false); setBorder(new EmptyBorder(0, 0, 0, 0)); }};
        JLabel hTitle = new JLabel("History");
        hTitle.setFont(hTitle.getFont().deriveFont(Font.BOLD, 16f));
        hTitle.setForeground(theme.txtSecondary);
        p.add(hTitle, BorderLayout.NORTH);

        historyList.setOpaque(false);
        historyList.setBackground(new Color(0,0,0,0));
        historyList.setForeground(theme.txtPrimary);
        historyList.setFont(historyList.getFont().deriveFont(Font.PLAIN, 14f));
        historyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setOpaque(true);
                l.setBackground(new Color(255,255,255, isSelected ? 80 : 50));
                l.setForeground(theme.txtPrimary);
                l.setBorder(new EmptyBorder(6, 8, 6, 8));
                return l;
            }
        });
        JScrollPane sp = new JScrollPane(historyList);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        p.add(sp, BorderLayout.CENTER);

        NeoButton clear = new NeoButton("Clear History", "HISTORY_CLEAR", theme, ButtonRole.FUNCTION);
        clear.setFont(clear.getFont().deriveFont(Font.PLAIN, 14f));
        p.add(clear, BorderLayout.SOUTH);
        buttons.put("HISTORY_CLEAR", clear);

        p.setPreferredSize(new Dimension(240, 0));
        return p;
    }

    private NeoButton addButton(JPanel parent, String label, String cmd, ButtonRole role) {
        NeoButton b = new NeoButton(label, cmd, theme, role);
        b.addActionListener(e -> { if (actionHandler != null) actionHandler.accept(cmd); });
        parent.add(b);
        buttons.put(cmd, b);
        return b;
    }

    public void setActionHandler(Consumer<String> handler) { this.actionHandler = handler; }

    public void setDisplay(String text) { display.setText(text); }
    public String getDisplay() { return display.getText(); }

    public void setPreview(String text) { preview.setText(text == null || text.isEmpty() ? " " : text); }

    public void setMemTagVisible(boolean visible) { memTag.setText(visible ? "M" : " "); }

    public void addHistory(String entry) {
        historyModel.add(0, entry);
        if (historyModel.size() > 50) historyModel.removeElementAt(historyModel.size() - 1);
    }
    public void clearHistory() { historyModel.clear(); }

    public void flashError() {
        Color orig = display.getForeground();
        display.setForeground(new Color(220, 70, 70));
        javax.swing.Timer t = new javax.swing.Timer(220, e -> display.setForeground(orig));
        t.setRepeats(false);
        t.start();
        Toolkit.getDefaultToolkit().beep();
    }

    public void setAllButtonsEnabled(boolean enabled, boolean keepAC) {
        for (Map.Entry<String, NeoButton> e : buttons.entrySet()) {
            String cmd = e.getKey();
            if (keepAC && "AC".equals(cmd)) {
                e.getValue().setEnabled(true);
            } else {
                e.getValue().setEnabled(enabled);
            }
        }
    }

    public void bindKey(KeyStroke ks, String command) {
        if (ks == null) return; // null-safe
        String key = "key_" + command + "_" + ks.toString();
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        im.put(ks, key);
        am.put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (actionHandler != null) actionHandler.accept(command);
            }
        });
    }

    public void refreshTheme() {
        getContentPane().repaint();
        preview.setForeground(theme.txtSecondary);
        display.setForeground(theme.txtPrimary);
        memTag.setForeground(theme.accent);
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }
}

/* ======= CONTROLLER ======= */

class CalculatorController {
    private final CalculatorEngine engine;
    private final CalculatorView view;

    private final MathContext mc;
    private boolean overwrite = true;
    private boolean justPressedOperator = false;
    private boolean errorState = false;

    private BigDecimal memory = BigDecimal.ZERO;
    private boolean memorySet = false;

    private static final int MAX_DIGITS = 16;

    CalculatorController(CalculatorEngine engine, CalculatorView view) {
        this.engine = engine;
        this.view = view;
        this.mc = engine.getMathContext();

        view.setActionHandler(this::handleAction);
        installKeyboardShortcuts();

        view.setPreview("");
        view.setDisplay("0");
        view.setMemTagVisible(false);
    }

    private void installKeyboardShortcuts() {
        for (int d = 0; d <= 9; d++) {
            String cmd = "DIGIT_" + d;
            view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_0 + d, 0), cmd);
            view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + d, 0), cmd);
            view.bindKey(KeyStroke.getKeyStroke((char)('0' + d)), cmd);
        }
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0), "DECIMAL");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DECIMAL, 0), "DECIMAL");
        view.bindKey(KeyStroke.getKeyStroke('.'), "DECIMAL");

        view.bindKey(KeyStroke.getKeyStroke('+'), "ADD");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "ADD");

        view.bindKey(KeyStroke.getKeyStroke('-'), "SUBTRACT");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "SUBTRACT");

        view.bindKey(KeyStroke.getKeyStroke('*'), "MULTIPLY");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, 0), "MULTIPLY");

        view.bindKey(KeyStroke.getKeyStroke('/'), "DIVIDE");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0), "DIVIDE");

        view.bindKey(KeyStroke.getKeyStroke('='), "EQUALS");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "EQUALS");

        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "BACK");
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "CE"); // Delete = Clear Entry
        view.bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "AC"); // Esc = All Clear

        view.bindKey(KeyStroke.getKeyStroke('%'), "PERCENT");
        view.bindKey(KeyStroke.getKeyStroke('r'), "SQRT");
        view.bindKey(KeyStroke.getKeyStroke('p'), "PERCENT");
        view.bindKey(KeyStroke.getKeyStroke('n'), "NEGATE");
    }

    public void handleAction(String cmd) {
        if (cmd.startsWith("PASTE:")) {
            pasteValue(cmd.substring("PASTE:".length()));
            return;
        }

        if (errorState && !"AC".equals(cmd) && !"HISTORY_CLEAR".equals(cmd)) {
            view.flashError();
            return;
        }

        if (cmd.startsWith("DIGIT_")) {
            int d = Integer.parseInt(cmd.substring("DIGIT_".length()));
            appendDigit(d);
            return;
        }

        switch (cmd) {
            case "DECIMAL": appendDecimal(); break;
            case "NEGATE": toggleSign(); break;
            case "SQRT": sqrtEntry(); break;
            case "PERCENT": applyPercent(); break;

            case "ADD": pressOperator(Operator.ADD); break;
            case "SUBTRACT": pressOperator(Operator.SUBTRACT); break;
            case "MULTIPLY": pressOperator(Operator.MULTIPLY); break;
            case "DIVIDE": pressOperator(Operator.DIVIDE); break;
            case "EQUALS": pressEquals(); break;

            case "BACK": backspace(); break;
            case "CE": clearEntry(); break;      // Clear button (C) — clears current display
            case "AC": allClear(); break;        // All Clear — resets engine and state

            case "MEM_CLEAR": memClear(); break;
            case "MEM_RECALL": memRecall(); break;
            case "MEM_STORE": memStore(); break;
            case "MEM_ADD": memAdd(); break;
            case "MEM_SUB": memSub(); break;

            case "HISTORY_CLEAR": view.clearHistory(); break;

            default: break;
        }
        updatePreview();
    }

    private void pasteValue(String s) {
        s = s.replaceAll(",", "").trim();
        try {
            new BigDecimal(s);
            view.setDisplay(s);
            overwrite = true;
        } catch (Exception e) {
            view.flashError();
        }
    }

    private void appendDigit(int d) {
        String txt = view.getDisplay();
        if (overwrite || "0".equals(txt)) {
            txt = Integer.toString(d);
            if (d == 0) txt = "0";
            overwrite = false;
        } else {
            if (countDigits(txt) >= MAX_DIGITS) { view.flashError(); return; }
            txt = txt + d;
        }
        justPressedOperator = false;
        view.setDisplay(txt);
    }

    private void appendDecimal() {
        String txt = view.getDisplay();
        if (overwrite) {
            txt = "0.";
            overwrite = false;
        } else if (!txt.contains(".")) {
            txt = txt + ".";
        } else {
            view.flashError(); return;
        }
        justPressedOperator = false;
        view.setDisplay(txt);
    }

    private void toggleSign() {
        String txt = view.getDisplay();
        if ("0".equals(txt) || "0.0".equals(txt)) return;
        if (txt.startsWith("-")) txt = txt.substring(1); else txt = "-" + txt;
        view.setDisplay(txt);
    }

    private void sqrtEntry() {
        BigDecimal entry = parseDisplay();
        if (entry.compareTo(BigDecimal.ZERO) < 0) {
            setErrorState("Invalid sqrt");
            return;
        }
        try {
            BigDecimal result = sqrt(entry, mc);
            view.setDisplay(format(result));
            overwrite = true;
            justPressedOperator = false;
            view.addHistory("√(" + format(entry) + ") = " + format(result));
        } catch (Exception ex) {
            setErrorState("Invalid sqrt");
        }
    }

    private void applyPercent() {
        BigDecimal entry = parseDisplay();
        Operator op = engine.getPendingOperator();
        BigDecimal percentVal;
        if (op == Operator.ADD || op == Operator.SUBTRACT) {
            percentVal = engine.getAccumulator().multiply(entry, mc).divide(new BigDecimal("100"), mc);
        } else if (op == Operator.MULTIPLY || op == Operator.DIVIDE) {
            percentVal = entry.divide(new BigDecimal("100"), mc);
        } else {
            percentVal = entry.divide(new BigDecimal("100"), mc);
        }
        view.setDisplay(format(percentVal));
        overwrite = true;
        justPressedOperator = false;
    }

    private void pressOperator(Operator op) {
        BigDecimal entry = parseDisplay();
        try {
            if (engine.getPendingOperator() != null && justPressedOperator) {
                engine.replacePendingOperator(op);
            } else {
                engine.setOperator(op, entry);
            }
        } catch (ArithmeticException ex) {
            setErrorState(ex.getMessage());
            return;
        }
        overwrite = true;
        justPressedOperator = true;
        view.setDisplay(format(engine.getAccumulator()));
    }

    private void pressEquals() {
        BigDecimal entry = parseDisplay();
        try {
            BigDecimal before = engine.getAccumulator();
            Operator usedOp = engine.getPendingOperator();
            BigDecimal usedOperand = entry;
            BigDecimal result = engine.equals(entry);

            view.setDisplay(format(result));
            overwrite = true;
            justPressedOperator = false;

            if (usedOp != null) {
                String hist = format(before) + " " + usedOp.label + " " + format(usedOperand) + " = " + format(result);
                view.addHistory(hist);
            }
        } catch (ArithmeticException ex) {
            setErrorState(ex.getMessage());
        }
    }

    private void backspace() {
        String txt = view.getDisplay();
        if (overwrite) { Toolkit.getDefaultToolkit().beep(); return; }
        if (txt.length() <= 1 || (txt.length() == 2 && txt.startsWith("-"))) {
            view.setDisplay("0");
            overwrite = true;
            return;
        }
        txt = txt.substring(0, txt.length() - 1);
        if ("-".equals(txt)) txt = "0";
        view.setDisplay(txt);
    }

    private void clearEntry() {
        view.setDisplay("0");
        overwrite = true;
        justPressedOperator = false;
    }

    private void allClear() {
        engine.clearAll();
        errorState = false;
        view.setAllButtonsEnabled(true, false);
        view.setDisplay("0");
        view.setPreview("");
        overwrite = true;
        justPressedOperator = false;
    }

    private void memClear() { memory = BigDecimal.ZERO; memorySet = false; view.setMemTagVisible(false); }
    private void memRecall() { if (memorySet) { view.setDisplay(format(memory)); overwrite = true; } }
    private void memStore() { memory = parseDisplay(); memorySet = true; view.setMemTagVisible(true); }
    private void memAdd() { memory = (memorySet ? memory : BigDecimal.ZERO).add(parseDisplay(), mc); memorySet = true; view.setMemTagVisible(true); }
    private void memSub() { memory = (memorySet ? memory : BigDecimal.ZERO).subtract(parseDisplay(), mc); memorySet = true; view.setMemTagVisible(true); }

    private void setErrorState(String msg) {
        errorState = true;
        view.setDisplay("Error");
        view.flashError();
        view.setAllButtonsEnabled(false, true);
    }

    private void updatePreview() {
        Operator op = engine.getPendingOperator();
        if (errorState || op == null) {
            view.setPreview("");
        } else {
            view.setPreview(format(engine.getAccumulator()) + " " + op.label);
        }
    }

    private BigDecimal parseDisplay() {
        String txt = view.getDisplay();
        if ("Error".equalsIgnoreCase(txt)) return BigDecimal.ZERO;
        if (isZeroString(txt)) return BigDecimal.ZERO;
        return new BigDecimal(txt);
    }

    private static boolean isZeroString(String s) {
        try { return new BigDecimal(s).compareTo(BigDecimal.ZERO) == 0; } catch (Exception e) { return false; }
    }

    private static int countDigits(String s) {
        int c=0; for (char ch : s.toCharArray()) if (Character.isDigit(ch)) c++; return c;
    }

    private static String format(BigDecimal val) {
        if (val == null) return "Error";
        if (val.compareTo(BigDecimal.ZERO) == 0) return "0";
        String s = val.stripTrailingZeros().toPlainString();
        if (s.length() > 24) {
            s = val.round(new MathContext(16, RoundingMode.HALF_UP))
                    .stripTrailingZeros()
                    .toEngineeringString();
        }
        return s;
    }

    // BigDecimal sqrt via Newton-Raphson
    private static BigDecimal sqrt(BigDecimal value, MathContext mc) {
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new ArithmeticException("Negative");
        if (value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal x = new BigDecimal(Math.sqrt(value.doubleValue()), mc);
        if (x.compareTo(BigDecimal.ZERO) == 0) x = BigDecimal.ONE;

        BigDecimal two = new BigDecimal("2");
        for (int i=0; i<30; i++) {
            x = x.add(value.divide(x, mc), mc).divide(two, mc);
        }
        return x;
    }
}
