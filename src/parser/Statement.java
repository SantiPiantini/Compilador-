package parser;

public class Statement extends ASTNode {
    public String kind;
    public String content;

    public Statement(String kind, String content, int line, int column) {
        this.kind = kind;
        this.content = content;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return kind + " -> " + content + " (línea " + line + ")";
    }
}
