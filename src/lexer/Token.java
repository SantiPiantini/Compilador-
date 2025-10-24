package lexer;

public class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal; // Integer, Double, String, Boolean, o null
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        String lit = (literal != null) ? " " + literal : "";
        return type + "('" + lexeme + "')" + "@" + line + ":" + column + lit;
    }
}

