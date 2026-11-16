# PatchCraft JavaLite

**A deterministic, explainable source repair tool for a documented subset of Java.**

ANTLR 4 generates the lexer and parser from `JavaLite.g4`. The repair engine applies four explicit rules and parses the result again. It reports each change and leaves unsupported syntax errors visible.

## Requirements

- JDK 17 or newer
- Maven 3.9 or newer

## Quick start — paste code into the desktop window

Open a terminal in the project directory (`C:\Users\andri\patchcraft-javalite`) and run:

```powershell
mvn exec:java
```

Paste JavaLite code on the left, click **Napraw kod**, and review the result and report on the right. **Kopiuj wynik** copies the repaired code. No input file is needed.

To run the automated tests, use `mvn test`. The optional file-based mode is:

```powershell
mvn exec:java '-Dexec.args=examples/broken.java examples/fixed.java'
```

It writes `examples/fixed.java` and prints the applied rules. The quoted argument syntax above works in PowerShell.

## Supported rules

| Rule | Example | Repair |
| --- | --- | --- |
| R01 | `int x = 1` before the next statement | insert `;` |
| R02 | missing final `}`, or missing `}` before the final `while` of a `do...while` loop | insert `}` |
| R03 | `if (x = 5)` | `if (x == 5)` |
| R04 | `a == b` when both are declared `String` | `a.equals(b)` |
| R05 | `i <= arr.length` in a loop condition when `arr` is declared as an array | `i < arr.length` |
| R10 | `i-;` after a variable name | `i--;` with a review note |

## Scope and limitations

JavaLite supports an optional package declaration, one public or package-private class, fields, public/static methods, primitive and String types, one-dimensional arrays (including `String args[]`), basic expressions, blocks, if/else, while, do/while, for, return, calls, and `++`/`--`. This is a teaching grammar, not a full Java parser. Type lookup is simple and does not model nested scopes or shadowing. R02 handles a final missing brace and one specific missing brace in `do...while`; it cannot infer every missing brace inside a program. R10 only handles the narrow `name-;` typo; `i-` by itself is ambiguous. R04 does not handle `null` specially. R05 is a suggested repair for typical array traversal; it does not prove the programmer's intent. R03 may change intended assignment semantics. Review every reported change before using repaired code.

## Project layout

- `src/main/antlr4/pl/edu/student/patchcraft/JavaLite.g4`: grammar used by ANTLR
- `src/main/java/.../RepairEngine.java`: parsing, matching, rewriting, verification
- `src/main/java/.../Main.java`: application entry point
- `src/main/java/.../RepairWindow.java`: desktop paste-and-repair window
- `src/test/java/.../RepairEngineTest.java`: regression tests
- `examples/broken.java`: sample input


