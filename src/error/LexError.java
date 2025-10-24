package error;

public class LexError {
    public final int line;
    public final int column;
    public final String message;

    public LexError(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Error léxico [línea " + line + ", columna " + column + "]: " + message;
    }
}

