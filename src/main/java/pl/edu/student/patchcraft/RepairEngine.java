package pl.edu.student.patchcraft;

import org.antlr.v4.runtime.*;
import java.util.*;

public final class RepairEngine {
    public record Change(String rule, int line, String explanation) {}
    public record Result(String source, List<Change> changes, List<String> diagnostics) {}
    private record Issue(Token token, String message, boolean missingSemicolon) {}
    private record Parsed(JavaLiteParser.ProgramContext tree, CommonTokenStream tokens, List<Issue> issues) {}

    private Parsed parse(String source) {
        JavaLiteLexer lexer = new JavaLiteLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaLiteParser parser = new JavaLiteParser(tokens);
        List<Issue> issues = new ArrayList<>();
        BaseErrorListener errors = new BaseErrorListener() {
            @Override public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                    int line, int column, String message, RecognitionException exception) {
                Token token = (Token) offendingSymbol;
                boolean semicolon = message.startsWith("missing ';' at");
                issues.add(new Issue(token, line + ":" + (column + 1) + " " + message, semicolon));
            }
        };
        lexer.removeErrorListeners(); lexer.addErrorListener(errors);
        parser.removeErrorListeners(); parser.addErrorListener(errors);
        return new Parsed(parser.program(), tokens, issues);
    }

    public Result repair(String original) {
        String source = original;
        List<Change> changes = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (int iteration = 0; iteration < 10; iteration++) {
            Parsed parsed = parse(source);
            if (!parsed.issues.isEmpty()) {
                Issue first = parsed.issues.get(0);
                if (!first.missingSemicolon) {
                    diagnostics.add("Nieobsługiwany błąd składni: " + first.message);
                    break;
                }
                int index = first.token.getTokenIndex() - 1;
                while (index >= 0 && parsed.tokens.get(index).getChannel() != Token.DEFAULT_CHANNEL) index--;
                if (index < 0) { diagnostics.add(first.message); break; }
                Token previous = parsed.tokens.get(index);
                int offset = previous.getStopIndex() + 1;
                String candidate = source.substring(0, offset) + ";" + source.substring(offset);
                if (parse(candidate).issues.size() >= parsed.issues.size()) {
                    diagnostics.add("R01 nie poprawiła składni: " + first.message);
                    break;
                }
                source = candidate;
                changes.add(new Change("R01", previous.getLine(), "Dodano brakujący średnik."));
                continue;
            }
            FixFinder finder = new FixFinder(parsed.tokens);
            finder.visit(parsed.tree);
            if (finder.fix == null) break;
            String candidate = finder.fix.rewriter.getText();
            if (!parse(candidate).issues.isEmpty()) {
                diagnostics.add("Wycofano " + finder.fix.change.rule() + ": poprawka psuje składnię.");
                break;
            }
            source = candidate;
            changes.add(finder.fix.change);
        }
        Parsed finalParse = parse(source);
        if (!finalParse.issues.isEmpty() && diagnostics.isEmpty())
            diagnostics.add("Pozostał błąd składni: " + finalParse.issues.get(0).message);
        return new Result(source, List.copyOf(changes), List.copyOf(diagnostics));
    }

    private record Fix(TokenStreamRewriter rewriter, Change change) {}
    private static final class FixFinder extends JavaLiteBaseVisitor<Void> {
        private final CommonTokenStream tokens;
        private final Map<String, String> types = new HashMap<>();
        private Fix fix;
        FixFinder(CommonTokenStream tokens) { this.tokens = tokens; }

        private void offer(String rule, Token token, String replacement, String explanation) {
            if (fix != null) return;
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            rewriter.replace(token, replacement);
            fix = new Fix(rewriter, new Change(rule, token.getLine(), explanation));
        }
        @Override public Void visitFieldDecl(JavaLiteParser.FieldDeclContext ctx) {
            types.put(ctx.ID().getText(), ctx.type().getText());
            return super.visitFieldDecl(ctx);
        }
        @Override public Void visitParam(JavaLiteParser.ParamContext ctx) {
            types.put(ctx.ID().getText(), ctx.type().getText());
            return super.visitParam(ctx);
        }
        @Override public Void visitStmt(JavaLiteParser.StmtContext ctx) {
            if (ctx.type() != null && ctx.ID() != null) types.put(ctx.ID().getText(), ctx.type().getText());
            return super.visitStmt(ctx);
        }
        @Override public Void visitForInit(JavaLiteParser.ForInitContext ctx) {
            if (ctx.type() != null && ctx.ID() != null) types.put(ctx.ID().getText(), ctx.type().getText());
            return super.visitForInit(ctx);
        }
        @Override public Void visitChildren(org.antlr.v4.runtime.tree.RuleNode node) {
            if (node instanceof JavaLiteParser.StmtContext stmt && stmt.getChildCount() > 0
                    && "if".equals(stmt.getChild(0).getText())
                    && !stmt.expr().isEmpty()
                    && stmt.expr(0) instanceof JavaLiteParser.AssignExprContext assignment
                    && assignment.expr(0) instanceof JavaLiteParser.IdExprContext) {
                Token equals = tokens.get(assignment.expr(0).getStop().getTokenIndex() + 1);
                while (!"=".equals(equals.getText())) equals = tokens.get(equals.getTokenIndex() + 1);
                offer("R03", equals, "==", "Zamieniono przypisanie w warunku if na porównanie.");
            }
            return super.visitChildren(node);
        }
        @Override public Void visitBinaryExpr(JavaLiteParser.BinaryExprContext ctx) {
            if (fix != null) return null;
            String op = ctx.op.getText();
            JavaLiteParser.ExprContext left = ctx.expr(0), right = ctx.expr(1);
            if ("==".equals(op) && left instanceof JavaLiteParser.IdExprContext
                    && right instanceof JavaLiteParser.IdExprContext
                    && "String".equals(types.get(left.getText())) && "String".equals(types.get(right.getText()))) {
                TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
                rewriter.replace(left.getStart(), right.getStop(), left.getText() + ".equals(" + right.getText() + ")");
                fix = new Fix(rewriter, new Change("R04", ctx.getStart().getLine(), "Porównano wartości String przez equals."));
                return null;
            }
            if ("<=".equals(op) && insideLoopCondition(ctx) && left instanceof JavaLiteParser.IdExprContext
                    && right instanceof JavaLiteParser.MemberExprContext member
                    && "length".equals(member.ID().getText())
                    && member.expr() instanceof JavaLiteParser.IdExprContext
                    && types.getOrDefault(member.expr().getText(), "").endsWith("[]")) {
                Token operator = tokens.get(left.getStop().getTokenIndex() + 1);
                while (!"<=".equals(operator.getText())) operator = tokens.get(operator.getTokenIndex() + 1);
                offer("R05", operator, "<", "Granica indeksowania tablicy: <= zmieniono na <.");
            }
            return super.visitBinaryExpr(ctx);
        }
        private boolean insideLoopCondition(JavaLiteParser.BinaryExprContext expr) {
            org.antlr.v4.runtime.RuleContext parent = expr.getParent();
            while (parent != null) {
                if (parent instanceof JavaLiteParser.StmtContext stmt) {
                    String first = stmt.getChild(0).getText();
                    return ("while".equals(first) || "for".equals(first))
                            && !stmt.expr().isEmpty() && stmt.expr(0) == expr;
                }
                parent = parent.getParent();
            }
            return false;
        }
    }
}
