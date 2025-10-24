package parser;

import lexer.*;
import error.*;
import symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private final ErrorHandler errors;
    private int current = 0;

    private final List<Statement> statements = new ArrayList<>();
    private final SymbolTable symbolTable = new SymbolTable(); // ✅ Tabla de símbolos

    public Parser(List<Token> tokens, ErrorHandler errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    // === Getter para usar la tabla desde Main ===
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    // === Método principal ===
    public List<Statement> parse() {
        while (!isAtEnd()) {
            Statement stmt = declaration();
            if (stmt != null) statements.add(stmt);
        }
        return statements;
    }

    // ====================================================
    // =============== REGLAS PRINCIPALES =================
    // ====================================================

    private Statement declaration() {
        if (match(TokenType.LONG, TokenType.DOUBLE)) {
            return varDeclaration(); // ✅ llamadas a declaraciones de variables
        }
        return statement();
    }

    // --- Declaraciones de variables ---
    private Statement varDeclaration() {
        Token typeToken = previous();

        // Esperamos un identificador después del tipo
        if (!check(TokenType.IDENTIFIER)) {
            error(peek(), "Se esperaba un identificador después del tipo.");
            synchronize();
            return null;
        }

        Token name = advance();
        StringBuilder content = new StringBuilder();
        content.append(typeToken.lexeme).append(" ").append(name.lexeme);

        // Guardar variable en la tabla de símbolos
        symbolTable.addSymbol(name.lexeme, typeToken.lexeme, null, "global", typeToken.line);

        // Si hay asignación
        if (match(TokenType.ASSIGN)) {
            String valueExpr = expressionToString();
            content.append(" = ").append(valueExpr);
        }

        consume(TokenType.SEMICOLON, "Se esperaba ';' después de la declaración.");

        return new Statement("varDecl", content.toString(), typeToken.line, typeToken.column);
    }

    // --- Sentencias generales ---
    private Statement statement() {
        if (match(TokenType.READ)) return readStatement();
        if (match(TokenType.WRITE)) return writeStatement();
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.LBRACE)) return blockStatement();

        return exprStatement();
    }

    // --- Sentencias específicas ---
    private Statement readStatement() {
        consume(TokenType.LPAREN, "Se esperaba '(' después de 'read'.");
        Token id = consume(TokenType.IDENTIFIER, "Se esperaba un identificador dentro de read().");

        // Validar si la variable fue declarada
        if (symbolTable.getSymbol(id.lexeme) == null) {
            errors.addLexError(id.line, id.column, "Variable '" + id.lexeme + "' usada sin declarar.");
        }

        consume(TokenType.RPAREN, "Falta ')' en read().");
        consume(TokenType.SEMICOLON, "Falta ';' después de read().");
        return new Statement("read", id.lexeme, id.line, id.column);
    }

    private Statement writeStatement() {
        consume(TokenType.LPAREN, "Se esperaba '(' después de 'write'.");

        // Leer toda la expresión dentro del paréntesis
        String expr = expressionToString();

        // Chequear si el primer token de la expresión es un identificador no declarado
        String firstWord = expr.split(" ")[0];
        if (Character.isLetter(firstWord.charAt(0)) || firstWord.startsWith("_")) {
            if (!symbolTable.exists(firstWord) && !firstWord.matches("\\d+(\\.\\d+)?")) {
                errors.addLexError(peek().line, peek().column,
                        "Error semántico: variable '" + firstWord + "' usada sin declarar.");
            }
        }

        consume(TokenType.RPAREN, "Falta ')' en write().");
        consume(TokenType.SEMICOLON, "Falta ';' después de write().");
        return new Statement("write", expr, previous().line, previous().column);
    }


    private Statement ifStatement() {
        consume(TokenType.LPAREN, "Se esperaba '(' después de 'if'.");
        String condition = expressionToString();

        // Verificamos que los operandos sean numéricos o booleanos válidos
        String condType = inferExpressionType(condition);
        if (!condType.equals("long") && !condType.equals("double")) {
            errors.addLexError(peek().line, peek().column,
                    "Error semántico: condición de if() debe ser numérica o booleana.");
        }

        consume(TokenType.RPAREN, "Falta ')' en condición de 'if'.");
        consume(TokenType.THEN, "Falta 'then' después del if().");

        Statement thenBranch = statement();
        Statement elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        String content = "if (" + condition + ")";
        if (elseBranch != null)
            content += " [else incluido]";
        return new Statement("if", content, previous().line, previous().column);
    }


    private Statement whileStatement() {
        consume(TokenType.LPAREN, "Se esperaba '(' después de 'while'.");
        String condition = expressionToString();
        consume(TokenType.RPAREN, "Falta ')' en condición de 'while'.");
        Statement body = blockStatement();
        return new Statement("while", condition, previous().line, previous().column);
    }

    private Statement blockStatement() {
        symbolTable.beginScope(); // ✅ Nuevo ámbito local

        StringBuilder body = new StringBuilder();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Statement stmt = declaration();
            if (stmt != null) body.append("\n  ").append(stmt);
        }

        consume(TokenType.RBRACE, "Falta '}' para cerrar el bloque.");
        symbolTable.endScope(); // ✅ Cerramos ámbito

        return new Statement("block", body.toString(), previous().line, previous().column);
    }


    private Statement exprStatement() {
        String expr = expressionToString();
        consume(TokenType.SEMICOLON, "Falta ';' después de la expresión.");

        // Verificamos si es una asignación (a = b + 3.5)
        String[] parts = expr.split("=");
        if (parts.length == 2) {
            String left = parts[0].trim();
            String right = parts[1].trim();

            // Validar que la variable exista
            if (!symbolTable.exists(left)) {
                errors.addLexError(peek().line, peek().column,
                        "Error semántico: variable '" + left + "' usada sin declarar.");
            } else {
                String leftType = symbolTable.getSymbol(left).type;
                String rightType = inferExpressionType(right);

                // Si el tipo derecho es double pero el izquierdo es long
                if (leftType.equals("long") && rightType.equals("double")) {
                    errors.addLexError(peek().line, peek().column,
                            "Error semántico: no se puede asignar un double a un long (" + left + ").");
                }
            }
        }

        return new Statement("expr", expr, previous().line, previous().column);
    }



    // ====================================================
    // =============== EXPRESIONES SIMPLES ================
    // ====================================================

    private String expressionToString() {
        StringBuilder expr = new StringBuilder();
        int parenCount = 0;

        while (!isAtEnd()) {
            Token t = peek();

            if (t.type == TokenType.RPAREN && parenCount == 0) break;
            if (t.type == TokenType.SEMICOLON && parenCount == 0) break;
            if (t.type == TokenType.RBRACE && parenCount == 0) break;

            if (t.type == TokenType.LPAREN) parenCount++;
            if (t.type == TokenType.RPAREN && parenCount > 0) parenCount--;

            expr.append(advance().lexeme).append(" ");
        }

        return expr.toString().trim();
    }

    // ====================================================
    // =================== UTILIDADES =====================
    // ====================================================

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        error(peek(), message);
        return new Token(type, "?", null, peek().line, peek().column);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() { return tokens.get(current); }
    private Token previous() { return tokens.get(current - 1); }

    private void error(Token token, String message) {
        errors.addLexError(token.line, token.column, "Error sintáctico: " + message);
        synchronize();
    }

    private void synchronize() {
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;
            switch (peek().type) {
                case IF: case WHILE: case READ: case WRITE: case LONG: case DOUBLE:
                    return;
            }
            advance();
        }
    }
    private String inferExpressionType(String expr) {
        // Si hay un punto decimal, asumimos double
        if (expr.matches(".*\\d+\\.\\d+.*")) return "double";

        // Si hay operadores aritméticos, miramos las variables
        String[] tokens = expr.split("[\\s\\+\\-\\*/]+");
        boolean hasDouble = false;
        for (String t : tokens) {
            t = t.trim();
            if (t.isEmpty()) continue;

            if (t.matches("\\d+\\.\\d+")) hasDouble = true;
            else if (symbolTable.exists(t)) {
                String type = symbolTable.getSymbol(t).type;
                if (type.equals("double")) hasDouble = true;
            }
        }

        return hasDouble ? "double" : "long";
    }

}
