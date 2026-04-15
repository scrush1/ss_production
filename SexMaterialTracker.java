import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SexMaterialTracker extends JFrame {

    // ------------------------------------------------------------------ Theme
    private boolean darkMode = false;

    private Color bg()        { return darkMode ? new Color(18,18,20)    : new Color(248,248,246); }
    private Color cardBg()    { return darkMode ? new Color(28,28,32)    : Color.WHITE; }
    private Color headerBg()  { return darkMode ? new Color(22,22,26)    : Color.WHITE; }
    private Color border()    { return darkMode ? new Color(55,55,65)    : new Color(210,208,200); }
    private Color textPri()   { return darkMode ? new Color(235,235,230) : new Color(30,30,28); }
    private Color textSec()   { return darkMode ? new Color(140,138,130) : new Color(100,98,92); }
    private Color surfaceSec(){ return darkMode ? new Color(38,38,45)    : new Color(245,245,243); }
    private Color inputBg()   { return darkMode ? new Color(35,35,42)    : Color.WHITE; }
    private Color blueBg()    { return darkMode ? new Color(20,50,90)    : new Color(230,241,251); }
    private Color blueFg()    { return darkMode ? new Color(120,180,255) : new Color(24,95,165); }
    private Color greenBg()   { return darkMode ? new Color(20,55,20)    : new Color(234,243,222); }
    private Color greenFg()   { return darkMode ? new Color(100,210,100) : new Color(39,80,10); }
    private Color warnBg()    { return darkMode ? new Color(70,45,10)    : new Color(250,238,218); }
    private Color warnFg()    { return darkMode ? new Color(255,190,80)  : new Color(99,56,6); }
    private Color dangerBg()  { return darkMode ? new Color(70,20,20)    : new Color(252,235,235); }
    private Color dangerFg()  { return darkMode ? new Color(255,110,110) : new Color(121,31,31); }
    private JComboBox<String> cmbPanelType, cmbVinilType;

    private String mapToJson(Map<String, Double> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            sb.append("\"")
              .append(entry.getKey())
              .append("\": ")
              .append(entry.getValue());
            if (i++ < map.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private void parseMap(String json, String key, Map<String, Double> map) {
    int idx = json.indexOf("\"" + key + "\":");
    if (idx < 0) return;

    int start = json.indexOf("{", idx);
    int end = json.indexOf("}", start);

    if (start < 0 || end < 0) return;

    String body = json.substring(start + 1, end);

    String[] pairs = body.split(",");

    for (String pair : pairs) {
        String[] kv = pair.split(":");

        if (kv.length == 2) {
            String k = kv[0].replace("\"", "").trim();
            double v = Double.parseDouble(kv[1].trim());

            map.put(k, v);
        }
    }
}
    

    // ------------------------------------------------------------------ Data

    // Panel növləri üzrə stok
    private Map<String, Double> panelTotalByType = new LinkedHashMap<>();
    private Map<String, Double> panelRemainingByType = new LinkedHashMap<>();

    // Vinil növləri üzrə stok
    private Map<String, Double> vinilTotalByType = new LinkedHashMap<>();
    private Map<String, Double> vinilRemainingByType = new LinkedHashMap<>();

    // Log məlumatları (Vaxt, Material, Növ, Əməliyyat, Miqdar, Qalan)
    private final ArrayList<String[]> logData = new ArrayList<>();

    // ------------------------------------------------------------------ UI refs
    private JLabel lblPanelCemi, lblPanelQalan, lblPanelIstifade, lblPanelAlert;
    private JLabel lblVinilCemi, lblVinilQalan, lblVinilIstifade, lblVinilAlert;
    private JProgressBar barPanel, barVinil;
    private JTextField txtPanelAmount, txtVinilAmount;
    private DefaultTableModel logModel;
    private JTable logTable;

    // Panels to repaint on theme change
    private final ArrayList<JComponent> allComponents = new ArrayList<>();
    private JPanel headerPanel, centerPanel, panelCard, vinilCard, logPanel;
    private JLabel statusLabel;
    private JButton btnTheme;

    // Save file path
    private static final String SAVE_FILE = System.getProperty("user.home") + File.separator + "sex_material_data.json";

    // ------------------------------------------------------------------ Constructor
    public SexMaterialTracker() {
        panelTotalByType.put("You V", 0.0);
        panelTotalByType.put("Sade", 0.0);
        panelRemainingByType.put("You V", 0.0);
        panelRemainingByType.put("Sade", 0.0);

        vinilTotalByType.put("1 m", 0.0);
        vinilTotalByType.put("126 sm", 0.0);
        vinilTotalByType.put("150 sm", 0.0);
        vinilRemainingByType.put("1 m", 0.0);
        vinilRemainingByType.put("126 sm", 0.0);
        vinilRemainingByType.put("150 sm", 0.0);

        setTitle("SS_Production Material İzləyici");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(860, 700);
        setMinimumSize(new Dimension(720, 620));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                saveData();
                System.exit(0);
            }
        });
        

        loadData();
        buildUI();
        applyTheme();
        updateCard("panel");
        updateCard("vinil");
        rebuildLogTable();
    }

    // ------------------------------------------------------------------ Build UI
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        headerPanel = buildHeader();
        add(headerPanel, BorderLayout.NORTH);

        centerPanel = new JPanel(new BorderLayout(12, 12));
        centerPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
        cards.setOpaque(false);
        panelCard = buildMaterialCard("panel");
        vinilCard = buildMaterialCard("vinil");
        cards.add(panelCard);
        cards.add(vinilCard);
        centerPanel.add(cards, BorderLayout.CENTER);

        logPanel = buildLogPanel();
        centerPanel.add(logPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("  Hazır — məlumatlar avtomatik saxlanır");
        statusLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        statusLabel.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, border()),
            new EmptyBorder(6, 14, 6, 14)
        ));
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------ Header
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, border()),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel title = new JLabel("Material İzləyici");
        title.setFont(new Font("Dialog", Font.BOLD, 20));

        JLabel sub = new JLabel("Panel və vinil ehtiyatlarını metr ilə izləyin  •  məlumatlar saxlanır");
        sub.setFont(new Font("Dialog", Font.PLAIN, 13));

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 3));
        texts.setOpaque(false);
        texts.add(title);
        texts.add(sub);
        p.add(texts, BorderLayout.WEST);

        btnTheme = new JButton("🌙 Dark");
        btnTheme.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnTheme.setFocusPainted(false);
        btnTheme.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTheme.setBorder(new CompoundBorder(
            new LineBorder(border(), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        btnTheme.addActionListener(e -> toggleTheme());

        p.add(btnTheme, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------------ Material Card
    private JPanel buildMaterialCard(String key) {
        boolean isPanel = key.equals("panel");

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
            new LineBorder(border(), 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lbl = new JLabel(isPanel ? "Panel" : "Vinil");
        lbl.setFont(new Font("Dialog", Font.BOLD, 16));

        JLabel badge = new JLabel("metr ilə");
        badge.setFont(new Font("Dialog", Font.PLAIN, 11));
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));

        titleRow.add(lbl, BorderLayout.WEST);
        titleRow.add(badge, BorderLayout.EAST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(14));

        // Stats
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 8, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel cemi = makeStatVal("0.00 m");
        JLabel qalan = makeStatVal("0.00 m");
        JLabel istifade = makeStatVal("0.00 m");
        statsRow.add(makeStatCard("Gələn (cəmi)", cemi));
        statsRow.add(makeStatCard("Qalan", qalan));
        statsRow.add(makeStatCard("İstifadə", istifade));
        card.add(statsRow);
        card.add(Box.createVerticalStrut(12));

        // Progress bar
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(0);
        bar.setStringPainted(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        bar.setBorderPainted(false);
        card.add(bar);
        card.add(Box.createVerticalStrut(10));

        // Alert
        JLabel alertLbl = new JLabel("  ");
        alertLbl.setFont(new Font("Dialog", Font.PLAIN, 12));
        alertLbl.setOpaque(true);
        alertLbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        alertLbl.setBorder(new EmptyBorder(4, 8, 4, 8));
        card.add(alertLbl);
        card.add(Box.createVerticalStrut(10));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(12));

        // Op label
        // Type selection
    JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    typePanel.setOpaque(false);
    typePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

    JLabel typeLabel = new JLabel("Növ:");
    typeLabel.setFont(new Font("Dialog", Font.PLAIN, 13));

    JComboBox<String> cmbType;
    if (isPanel) {
    cmbType = new JComboBox<>(new String[]{"Hamısı", "You V", "Sade"});
    cmbPanelType = cmbType;
} else {
    cmbType = new JComboBox<>(new String[]{"Hamısı", "1 m", "126 sm", "150 sm"});
    cmbVinilType = cmbType;
}

    cmbType.setFont(new Font("Dialog", Font.PLAIN, 13));
    cmbType.setPreferredSize(new Dimension(120, 26));
    //Növ dəyişdikdə kartı yenilə
    cmbType.addActionListener(e -> updateCard(key));

    typePanel.add(typeLabel);
    typePanel.add(cmbType);

    card.add(typePanel);
    card.add(Box.createVerticalStrut(8));
        JLabel opLbl = new JLabel("ƏMƏLİYYAT");
        opLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        card.add(opLbl);
        card.add(Box.createVerticalStrut(8));

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JTextField amountField = new JTextField(9);
        amountField.setFont(new Font("Dialog", Font.PLAIN, 14));
        amountField.setToolTipText("Miqdar (m)  •  Enter = İstifadə");

        JLabel mLabel = new JLabel("m");
        mLabel.setFont(new Font("Dialog", Font.PLAIN, 13));

        JButton btnAdd = new JButton("+ Gələn");
        JButton btnUse = new JButton("− İstifadə");
        styleButton(btnAdd);
        styleButton(btnUse);

        controls.add(amountField);
        controls.add(mLabel);
        controls.add(btnAdd);
        controls.add(btnUse);
        card.add(controls);

        // Store refs
        if (isPanel) {
            lblPanelCemi = cemi; lblPanelQalan = qalan; lblPanelIstifade = istifade;
            barPanel = bar; txtPanelAmount = amountField; lblPanelAlert = alertLbl;
        } else {
            lblVinilCemi = cemi; lblVinilQalan = qalan; lblVinilIstifade = istifade;
            barVinil = bar; txtVinilAmount = amountField; lblVinilAlert = alertLbl;
        }

        btnAdd.addActionListener(e -> handleAdd(key));
        btnUse.addActionListener(e -> handleUse(key));
        amountField.addActionListener(e -> handleUse(key));

        return card;
    }

    private JLabel makeStatVal(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Dialog", Font.BOLD, 16));
        return l;
    }

    private JPanel makeStatCard(String title, JLabel val) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setOpaque(true);
        p.setBorder(new EmptyBorder(8, 6, 8, 6));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(new Font("Dialog", Font.PLAIN, 11));
        p.add(t);
        p.add(val);
        return p;
    }

    private void styleButton(JButton b) {
        b.setFont(new Font("Dialog", Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(
            new LineBorder(border(), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
    }

    // ------------------------------------------------------------------ Log Panel
    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel title = new JLabel("Əməliyyat tarixi");
        title.setFont(new Font("Dialog", Font.PLAIN, 11));
        p.add(title, BorderLayout.NORTH);

        String[] cols = {"Vaxt", "Material", "Növ", "Əməliyyat", "Miqdar (m)", "Qalan (m)"};
        logModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        logTable = new JTable(logModel);
        logTable.setFont(new Font("Dialog", Font.PLAIN, 13));
        logTable.setRowHeight(26);
        logTable.getTableHeader().setFont(new Font("Dialog", Font.PLAIN, 12));
        logTable.setShowVerticalLines(false);

        int[] widths = {72, 72, 80, 110, 95, 95};
        for (int i = 0; i < widths.length; i++)
            logTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setPreferredSize(new Dimension(0, 155));

        JButton btnClear = new JButton("Tarixi sil");
        btnClear.setFont(new Font("Dialog", Font.PLAIN, 12));
        btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.setBorder(new CompoundBorder(
            new LineBorder(border(), 1, true),
            new EmptyBorder(5, 12, 5, 12)
        ));
        btnClear.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this,
                "Əməliyyat tarixini silmək istəyirsiniz?\n(Stok məlumatları silinməyəcək)",
                "Təsdiq", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                logData.clear();
                logModel.setRowCount(0);
                saveData();
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        bottom.setOpaque(false);
        bottom.add(btnClear);

        p.add(scroll, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    // ------------------------------------------------------------------ Theme
    private void toggleTheme() {
        darkMode = !darkMode;
        btnTheme.setText(darkMode ? "☀ Light" : "🌙 Dark");
        applyTheme();
        saveData();
    }

    private void applyTheme() {
        // Frame
        getContentPane().setBackground(bg());

        // Header
        if (headerPanel != null) {
            headerPanel.setBackground(headerBg());
            styleHeaderChildren(headerPanel);
        }

        // Center
        if (centerPanel != null) centerPanel.setBackground(bg());

        // Cards
        applyCardTheme(panelCard, "panel");
        applyCardTheme(vinilCard, "vinil");

        // Log
        if (logPanel != null) {
            applyComponentTheme(logPanel);
            if (logTable != null) {
                logTable.setBackground(cardBg());
                logTable.setForeground(textPri());
                logTable.setGridColor(border());
                logTable.getTableHeader().setBackground(cardBg());
                logTable.getTableHeader().setForeground(textSec());
                logTable.setSelectionBackground(blueBg());
                logTable.setSelectionForeground(blueFg());
                logTable.getParent().setBackground(cardBg()); // viewport
                logTable.getParent().getParent().setBackground(cardBg()); // scrollpane
                ((JScrollPane) logTable.getParent().getParent()).setBorder(new LineBorder(border(), 1, true));
            }
        }

        // Status bar
        if (statusLabel != null) {
            statusLabel.setBackground(headerBg());
            statusLabel.setForeground(textSec());
            statusLabel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, border()),
                new EmptyBorder(6, 14, 6, 14)
            ));
        }

        // Theme button
        if (btnTheme != null) {
            btnTheme.setBackground(surfaceSec());
            btnTheme.setForeground(textPri());
            btnTheme.setBorder(new CompoundBorder(
                new LineBorder(border(), 1, true),
                new EmptyBorder(6, 14, 6, 14)
            ));
        }

        // Update dynamic labels
        updateCard("panel");
        updateCard("vinil");

        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    private void applyCardTheme(JPanel card, String key) {
        if (card == null) return;
        card.setBackground(cardBg());
        card.setBorder(new CompoundBorder(
            new LineBorder(border(), 1, true),
            new EmptyBorder(16, 16, 16, 16)
        ));
        applyComponentTheme(card);

        // Badge color
        boolean isPanel = key.equals("panel");
        for (Component c : getAllChildren(card)) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                String txt = l.getText();
                if ("metr ilə".equals(txt)) {
                    l.setBackground(isPanel ? blueBg() : greenBg());
                    l.setForeground(isPanel ? blueFg() : greenFg());
                }
            }
            if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                tf.setBackground(inputBg());
                tf.setForeground(textPri());
                tf.setCaretColor(textPri());
                tf.setBorder(new CompoundBorder(
                    new LineBorder(border(), 1, true),
                    new EmptyBorder(4, 6, 4, 6)
                ));
            }
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setBackground(surfaceSec());
                b.setForeground(textPri());
                b.setBorder(new CompoundBorder(
                    new LineBorder(border(), 1, true),
                    new EmptyBorder(6, 14, 6, 14)
                ));
            }
            if (c instanceof JSeparator) {
                ((JSeparator) c).setForeground(border());
                ((JSeparator) c).setBackground(border());
            }
            if (c instanceof JPanel) {
                JPanel jp = (JPanel) c;
                if (jp.getLayout() instanceof GridLayout && ((GridLayout)jp.getLayout()).getRows() == 2) {
                    jp.setBackground(surfaceSec());
                    for (Component ch : jp.getComponents()) {
                        if (ch instanceof JLabel) ((JLabel) ch).setForeground(textSec());
                    }
                }
            }
        }
    }

    private void applyComponentTheme(JComponent comp) {
        comp.setBackground(bg());
        for (Component c : getAllChildren(comp)) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                if (!l.isOpaque()) l.setForeground(textSec());
            }
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setBackground(surfaceSec());
                b.setForeground(textPri());
                b.setBorder(new CompoundBorder(
                    new LineBorder(border(), 1, true),
                    new EmptyBorder(5, 12, 5, 12)
                ));
            }
        }
    }

    private void styleHeaderChildren(JPanel p) {
        for (Component c : getAllChildren(p)) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                if (l.getFont().isBold()) l.setForeground(textPri());
                else l.setForeground(textSec());
            }
        }
    }

    private java.util.List<Component> getAllChildren(Container parent) {
        java.util.List<Component> list = new ArrayList<>();
        for (Component c : parent.getComponents()) {
            list.add(c);
            if (c instanceof Container) list.addAll(getAllChildren((Container) c));
        }
        return list;
    }

    // ------------------------------------------------------------------ Logic
    private void handleAdd(String key) {
    Double amt = parseAmount(key);
    if (amt == null) return;

    if (key.equals("panel")) {
        String type = (String) cmbPanelType.getSelectedItem();
        if ("Hamısı".equals(type)) {
            JOptionPane.showMessageDialog(this,
                "Zəhmət olmasa banel növü seçin.",
                "Xəta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        panelTotalByType.put(type, panelTotalByType.get(type) + amt);
        panelRemainingByType.put(type, panelRemainingByType.get(type) + amt);
        addLogEntry("Panel", type, "Gəldi", amt, panelRemainingByType.get(type));
    } else {
        String type = (String) cmbVinilType.getSelectedItem();
        if ("Hamısı".equals(type)) {
            JOptionPane.showMessageDialog(this,
                "Zəhmət olmasa konkret vinil növü seçin.",
                "Xəta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        vinilTotalByType.put(type, vinilTotalByType.get(type) + amt);
        vinilRemainingByType.put(type, vinilRemainingByType.get(type) + amt);
        addLogEntry("Vinil", type, "Gəldi", amt, vinilRemainingByType.get(type));
    }

    updateCard(key);
    clearField(key);
    saveData();
}
private void handleUse(String key) {
    Double amt = parseAmount(key);
    if (amt == null) return;

    if (key.equals("panel")) {
        String type = (String) cmbPanelType.getSelectedItem();
        if ("Hamısı".equals(type)) {
            JOptionPane.showMessageDialog(this,
                "Zəhmət olmasa banel növü seçin.",
                "Xəta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double remaining = panelRemainingByType.get(type);
        if (amt > remaining) {
            JOptionPane.showMessageDialog(this,
                "Kifayət qədər stok yoxdur!",
                "Xəta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        panelRemainingByType.put(type, remaining - amt);
        addLogEntry("Panel", type, "İstifadə edildi", amt,
                panelRemainingByType.get(type));

    } else {
        String type = (String) cmbVinilType.getSelectedItem();
        if ("Hamısı".equals(type)) {
            JOptionPane.showMessageDialog(this,
                "Zəhmət olmasa konkret vinil növü seçin.",
                "Xəta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double remaining = vinilRemainingByType.get(type);
        if (amt > remaining) {
            JOptionPane.showMessageDialog(this,
                "Kifayət qədər stok yoxdur!",
                "Xəta", JOptionPane.WARNING_MESSAGE);
            return;
        }

        vinilRemainingByType.put(type, remaining - amt);
        addLogEntry("Vinil", type, "İstifadə edildi", amt,
                vinilRemainingByType.get(type));
    }

    updateCard(key);
    clearField(key);
    saveData();
}

    private void updateCard(String key) {
    boolean isPanel = key.equals("panel");

    Map<String, Double> totalMap = isPanel ? panelTotalByType : vinilTotalByType;
    Map<String, Double> remainMap = isPanel ? panelRemainingByType : vinilRemainingByType;

    JComboBox<String> cmb = isPanel ? cmbPanelType : cmbVinilType;

    String selectedType = (String) cmb.getSelectedItem();

    double total = 0;
    double remaining = 0;

    if ("Hamısı".equals(selectedType)) {
        for (String k : totalMap.keySet()) {
            total += totalMap.getOrDefault(k, 0.0);
            remaining += remainMap.getOrDefault(k, 0.0);
        }
    } else {
        total = totalMap.getOrDefault(selectedType, 0.0);
        remaining = remainMap.getOrDefault(selectedType, 0.0);
    }

    double used = total - remaining;

    int pct = total > 0 ? (int) Math.round((remaining / total) * 100) : 0;

    JLabel cemi     = isPanel ? lblPanelCemi     : lblVinilCemi;
    JLabel qalan    = isPanel ? lblPanelQalan    : lblVinilQalan;
    JLabel istifade = isPanel ? lblPanelIstifade : lblVinilIstifade;
    JProgressBar bar = isPanel ? barPanel : barVinil;
    JLabel alert    = isPanel ? lblPanelAlert    : lblVinilAlert;

    cemi.setText(fmt(total) + " m");
    qalan.setText(fmt(remaining) + " m");
    istifade.setText(fmt(used) + " m");

    bar.setValue(pct);

    // Rəng logic
    if (total > 0 && pct <= 20) {
        qalan.setForeground(dangerFg());
        bar.setForeground(dangerFg());
    } else if (total > 0 && pct <= 40) {
        qalan.setForeground(warnFg());
        bar.setForeground(isPanel ? blueFg() : greenFg());
    } else {
        qalan.setForeground(textPri());
        bar.setForeground(isPanel ? blueFg() : greenFg());
    }

    // Alert logic
    if (total == 0) {
        alert.setText("  ");
    } else if (remaining <= 0) {
        alert.setText("  Stok bitib!");
        alert.setBackground(dangerBg());
    } else if (pct <= 20) {
        alert.setText("  Stok kritik səviyyədədir!");
        alert.setBackground(dangerBg());
    } else if (pct <= 40) {
        alert.setText("  Stok azalır, diqqət edin!");
        alert.setBackground(warnBg());
    } else {
        alert.setText("  ");
    }

    revalidate();
    repaint();
}

    private void addLogEntry(String material, String type, String op, double amt, double remaining) {
    String time = new SimpleDateFormat("dd.MM HH:mm").format(new Date());
    String[] row = {time, material, type, op, fmt(amt), fmt(remaining)};
    logData.add(0, row);
    logModel.insertRow(0, row);
}

    private void rebuildLogTable() {
        logModel.setRowCount(0);
        for (String[] row : logData) logModel.addRow(row);
    }

    private Double parseAmount(String key) {
        JTextField f = key.equals("panel") ? txtPanelAmount : txtVinilAmount;
        String text = f.getText().trim().replace(",", ".");
        try {
            double val = Double.parseDouble(text);
            if (val <= 0) throw new NumberFormatException();
            return val;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Düzgün miqdar daxil edin (məs: 12.5)",
                "Giriş xətası", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void clearField(String key) {
        if (key.equals("panel")) txtPanelAmount.setText("");
        else txtVinilAmount.setText("");
    }

    private String fmt(double v) { return String.format("%.2f", v); }

    // ------------------------------------------------------------------ Save/Load
    private void saveData() {
    try {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"darkMode\": ").append(darkMode).append(",\n");

       
        sb.append("  \"panelTotalByType\": ")
          .append(mapToJson(panelTotalByType)).append(",\n");
        sb.append("  \"panelRemainingByType\": ")
          .append(mapToJson(panelRemainingByType)).append(",\n");
        sb.append("  \"vinilTotalByType\": ")
          .append(mapToJson(vinilTotalByType)).append(",\n");
        sb.append("  \"vinilRemainingByType\": ")
          .append(mapToJson(vinilRemainingByType)).append(",\n");

       
        sb.append("  \"log\": [\n");
        for (int i = 0; i < logData.size(); i++) {
            String[] r = logData.get(i);
            sb.append("    [\"")
              .append(esc(r[0])).append("\",\"")
              .append(esc(r[1])).append("\",\"")
              .append(esc(r[2])).append("\",\"")
              .append(esc(r[3])).append("\",\"")
              .append(esc(r[4])).append("\",\"")
              .append(esc(r[5])).append("\"]");
            if (i < logData.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        Files.writeString(Paths.get(SAVE_FILE), sb.toString());
    } catch (Exception e) {
        System.err.println("Saxlama xətası: " + e.getMessage());
    }
}

    private void loadData() {
    File f = new File(SAVE_FILE);
    if (!f.exists()) return;

    try {
        String content = new String(Files.readAllBytes(f.toPath()));

        darkMode = content.contains("\"darkMode\": true");

        // ---------------- PANEL TOTAL MAP
        parseMap(content, "panelTotalByType", panelTotalByType);

        // ---------------- PANEL REMAINING MAP
        parseMap(content, "panelRemainingByType", panelRemainingByType);

        // ---------------- VINIL TOTAL MAP
        parseMap(content, "vinilTotalByType", vinilTotalByType);

        // ---------------- VINIL REMAINING MAP
        parseMap(content, "vinilRemainingByType", vinilRemainingByType);

        // ---------------- LOG PARSE
        int logStart = content.indexOf("\"log\": [");
        if (logStart >= 0) {

            int arrStart = content.indexOf('[', logStart) + 1;
            int arrEnd = content.lastIndexOf(']');

            if (arrStart > 0 && arrEnd > arrStart) {
                String logSection = content.substring(arrStart, arrEnd).trim();

                int pos = 0;

                while (pos < logSection.length()) {

                    int rowStart = logSection.indexOf('[', pos);
                    if (rowStart < 0) break;

                    int rowEnd = logSection.indexOf(']', rowStart);
                    if (rowEnd < 0) break;

                    String rowStr = logSection.substring(rowStart + 1, rowEnd);

                    String[] parts = rowStr.split("\",\"");

                    if (parts.length == 6) {
                        String[] row = new String[6];

                        for (int i = 0; i < 6; i++) {
                            row[i] = parts[i]
                                    .replaceAll("^\"|\"$", "")
                                    .replace("\\\"", "\"");
                        }

                        logData.add(row);
                    }

                    pos = rowEnd + 1;
                }
            }
        }

    } catch (Exception e) {
        System.err.println("Yükləmə xətası: " + e.getMessage());
    }
}

    private double parseJsonDouble(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\": ");
            if (idx < 0) return 0;
            int start = idx + key.length() + 4;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) end++;
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) { return 0; }
    }

    private String esc(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
}

    // ------------------------------------------------------------------ Main
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SexMaterialTracker().setVisible(true));
    }
}
