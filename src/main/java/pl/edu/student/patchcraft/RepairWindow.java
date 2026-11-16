package pl.edu.student.patchcraft;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public final class RepairWindow {
    private static final String EXAMPLE = "class Demo {\n"
            + "    boolean same(String a, String b) {\n"
            + "        return a == b;\n"
            + "    }\n"
            + "}\n";

    private RepairWindow() {}

    public static void show() {
        JFrame frame = new JFrame("PatchCraft JavaLite");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JTextArea input = editor(EXAMPLE);
        JTextArea output = editor("");
        output.setEditable(false);
        JTextArea report = editor("Wklej kod po lewej i kliknij „Napraw kod”.");
        report.setEditable(false);
        report.setRows(5);

        JButton repair = new JButton("Napraw kod");
        repair.addActionListener(event -> {
            RepairEngine.Result result = new RepairEngine().repair(input.getText());
            output.setText(result.source());
            output.setCaretPosition(0);
            StringBuilder message = new StringBuilder("Liczba zmian: ").append(result.changes().size());
            for (RepairEngine.Change change : result.changes()) {
                message.append("\n").append(change.rule()).append(" (linia ")
                        .append(change.line()).append("): ").append(change.explanation());
            }
            for (String diagnostic : result.diagnostics()) message.append("\nUwaga: ").append(diagnostic);
            report.setText(message.toString());
            report.setCaretPosition(0);
        });
        JButton copy = new JButton("Kopiuj wynik");
        copy.addActionListener(event -> {
            if (!output.getText().isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(output.getText()), null);
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(repair);
        actions.add(copy);

        JSplitPane code = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panel("Kod do sprawdzenia", input), panel("Kod po naprawie", output));
        code.setResizeWeight(0.5);
        code.setDividerLocation(0.5);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(actions, BorderLayout.NORTH);
        content.add(code, BorderLayout.CENTER);
        content.add(panel("Raport", report), BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JTextArea editor(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setTabSize(4);
        return area;
    }

    private static JComponent panel(String title, JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }
}
