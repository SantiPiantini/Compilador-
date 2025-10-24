package error;
import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private final List<LexError> lexErrors = new ArrayList<>();

    public void addLexError(int line, int column, String message) {
        lexErrors.add(new LexError(line, column, message));
    }

    public boolean hasLexErrors() {
        return !lexErrors.isEmpty();
    }

    public List<LexError> getLexErrors() {
        return lexErrors;
    }

    public void printLexErrors() {
        for (LexError e : lexErrors) {
            System.err.println(e);
        }
    }
}

