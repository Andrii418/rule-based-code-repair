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
}
