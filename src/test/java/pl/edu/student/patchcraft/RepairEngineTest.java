package pl.edu.student.patchcraft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RepairEngineTest {
    private final RepairEngine engine = new RepairEngine();

    @Test void repairsMissingSemicolon() {
        var r = engine.repair("class Demo { int run() { int x = 1\n return x; } }");
        assertTrue(r.source().contains("int x = 1;"));
        assertTrue(r.diagnostics().isEmpty());
    }
    @Test void repairsAssignmentCondition() {
        var r = engine.repair("class Demo { void run() { int x = 0; if (x = 5) { x = 2; } } }");
        assertTrue(r.source().contains("if (x == 5)"));
    }
    @Test void repairsStringEquality() {
        var r = engine.repair("class Demo { boolean run(String a, String b) { return a == b; } }");
        assertTrue(r.source().contains("a.equals(b)"));
    }
    @Test void repairsArrayBound() {
        var r = engine.repair("class Demo { void run(int[] arr) { int i = 0; while (i <= arr.length) { i = i + 1; } } }");
        assertTrue(r.source().contains("i < arr.length"));
    }
    @Test void unchangedAndIdempotent() {
        String input = "class Demo { int run() { return 1; } }";
        var r = engine.repair(input);
        assertEquals(input, r.source());
        assertTrue(r.changes().isEmpty());
        assertTrue(engine.repair(r.source()).changes().isEmpty());
    }

    private static final String REALISTIC = "package javalearning;\n"
            + "public class Main {\n"
            + "  public static void main(String args[]) {\n"
            + "    int i = 3;\n"
            + "    do { System.out.println(i); i--; } while (i >= 1);\n"
            + "  }\n"
            + "}\n";

    @Test void acceptsScreenshotStyleProgram() {
        var result = engine.repair(REALISTIC);
        assertEquals(REALISTIC, result.source());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test void repairsMissingSemicolonInScreenshotStyleProgram() {
        var result = engine.repair(REALISTIC.replace("int i = 3;", "int i = 3"));
        assertTrue(result.source().contains("int i = 3;"));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test void repairsMissingFinalBrace() {
        var result = engine.repair(REALISTIC.substring(0, REALISTIC.lastIndexOf('}')));
        assertTrue(result.source().trim().endsWith("}"));
        assertTrue(result.changes().stream().anyMatch(c -> c.rule().equals("R02")), result.toString());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test void repairsSingleMinusInDecrement() {
        var result = engine.repair(REALISTIC.replace("i--;", "i-;"));
        assertTrue(result.source().contains("i--;"));
        assertTrue(result.changes().stream().anyMatch(c -> c.rule().equals("R10")));
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test void repairsThreeErrorsTogether() {
        String input = REALISTIC.replace("int i = 3;", "int i = 3")
                .replace("i--;", "i-;");
        input = input.substring(0, input.lastIndexOf('}'));
        var result = engine.repair(input);
        assertTrue(result.source().contains("int i = 3;"), result.toString());
        assertTrue(result.source().contains("i--;"), result.toString());
        assertTrue(result.changes().stream().anyMatch(c -> c.rule().equals("R02")), result.toString());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }

    @Test void repairsMissingDoBlockBrace() {
        var result = engine.repair(REALISTIC.replace("i--; } while", "i--; while"));
        assertTrue(result.source().contains("i--; } while"), result.toString());
        assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    }
}
