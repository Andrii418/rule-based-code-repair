package pl.edu.student.patchcraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.swing.SwingUtilities;

public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            SwingUtilities.invokeLater(RepairWindow::show);
            return;
        }
        if (args.length != 2) {
            System.err.println("Użycie: mvn exec:java (okno) albo mvn exec:java -Dexec.args=\"wejscie.java wyjscie.java\"");
            System.exit(2);
        }
        String input = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        RepairEngine.Result result = new RepairEngine().repair(input);
        Files.writeString(Path.of(args[1]), result.source(), StandardCharsets.UTF_8);
        for (RepairEngine.Change c : result.changes())
            System.out.printf("%s, linia %d: %s%n", c.rule(), c.line(), c.explanation());
        for (String d : result.diagnostics()) System.err.println(d);
        System.out.printf("Zapisano %s; liczba zmian: %d%n", args[1], result.changes().size());
        if (!result.diagnostics().isEmpty()) System.exit(1);
    }
}
