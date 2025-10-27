package lexer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import error.ErrorHandler;

public class Lexer {
    private final String source;
    private final ErrorHandler errors;

    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startColumn = 1;

    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("long", TokenType.LONG);
        keywords.put("double", TokenType.DOUBLE);
        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("break", TokenType.BREAK);
        keywords.put("read", TokenType.READ);
        keywords.put("write", TokenType.WRITE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
    }

    public Lexer(String source, ErrorHandler errors) {
        this.source = source != null ? source : "";
        this.errors = errors;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            startColumn = column;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, startColumn));
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ': case '\r': case '\t':
                break;
            case '\n':
                break;

            case '(' : addToken(TokenType.LPAREN); break;
            case ')' : addToken(TokenType.RPAREN); break;
            case '{' : addToken(TokenType.LBRACE); break;
            case '}' : addToken(TokenType.RBRACE); break;
            case ';' : addToken(TokenType.SEMICOLON); break;

            case '!':
                if (match('=')) addToken(TokenType.EQEQ, "!="); // lo normal sería NEQ, pero mantenemos coherencia
                else addToken(TokenType.BANG);
                break;

            case '=':
                if (match('=')) addToken(TokenType.EQEQ);
                else addToken(TokenType.ASSIGN);
                break;

            case '<':
                if (match('=')) addToken(TokenType.LE);
                else if (match('>')) addToken(TokenType.NEQ); // "<>"
                else addToken(TokenType.LT);
                break;

            case '>':
                if (match('=')) addToken(TokenType.GE);
                else addToken(TokenType.GT);
                break;

            case '&':
                if (match('&')) addToken(TokenType.AND_AND);
                else error("Símbolo '&' inesperado. ¿Quisiste escribir '&&'?");
                break;

            case '|':
                if (match('|')) addToken(TokenType.OR_OR);
                else error("Símbolo '|' inesperado. ¿Quisiste escribir '||'?");
                break;

            case '+':
                if (match('=')) addToken(TokenType.PLUS_EQ);
                else addToken(TokenType.PLUS);
                break;

            case '-':
                if (match('=')) addToken(TokenType.MINUS_EQ);
                else addToken(TokenType.MINUS);
                break;

            case '*':
                if (match('=')) addToken(TokenType.STAR_EQ);
                else addToken(TokenType.STAR);
                break;

            case '/':
                if (match('/')) {
                    // comentario de línea
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    blockComment();
                } else if (match('=')) {
                    addToken(TokenType.SLASH_EQ);
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number(c);
                } else if (isAlpha(c) || c == '_') {
                    identifier();
                } else {
                    error("Carácter inesperado: '" + c + "'");
                }
                break;
        }
    }

    private void blockComment() {
        int openLine = line;
        int openCol = column - 2;
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }
            advance();
        }
        errors.addLexError(openLine, openCol, "comentario multilínea sin cierre.");
    }

    private void string() {
        StringBuilder sb = new StringBuilder();
        int strLine = line;
        int strCol = startColumn;

        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\n') {
                errors.addLexError(strLine, strCol, "cadena sin cierre en la misma línea.");
                return;
            }
            sb.append(c);
        }

        if (isAtEnd()) {
            errors.addLexError(strLine, strCol, "cadena sin cierre.");
            return;
        }

        advance();
        addToken(TokenType.STRING_LITERAL, sb.toString());
    }

    private void number(char first) {
        while (isDigit(peek())) advance();

        boolean isDouble = false;
        if (peek() == '.' && isDigit(peekNext())) {
            isDouble = true;
            advance();
            while (isDigit(peek())) advance();
        }

        String text = source.substring(start, current);
        if (isDouble) {
            try {
                double value = Double.parseDouble(text);
                addToken(TokenType.DOUBLE_LITERAL, value);
            } catch (NumberFormatException e) {
                error("real inválido: " + text);
            }
        } else {
            try {
                int value = Integer.parseInt(text);
                addToken(TokenType.INT_LITERAL, value);
            } catch (NumberFormatException e) {
                error("entero inválido: " + text);
            }
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek()) || peek() == '_') advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) {
            addToken(TokenType.IDENTIFIER, text);
        } else {
            if (type == TokenType.TRUE) addToken(TokenType.TRUE, true);
            else if (type == TokenType.FALSE) addToken(TokenType.FALSE, false);
            else addToken(type);
        }
    }

    private void error(String message) {
        errors.addLexError(line, startColumn, message);
    }

    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}

