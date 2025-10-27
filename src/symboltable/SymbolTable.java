package symboltable;

import java.util.*;

public class SymbolTable {
    private final Deque<Map<String, SymbolInfo>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        beginScope();
    }

    public void beginScope() {
        scopes.push(new HashMap<>());
    }

    public void endScope() {
        scopes.pop();
    }

    public boolean existsInCurrentScope(String name) {
        return scopes.peek().containsKey(name);
    }

    public boolean exists(String name) {
        return scopes.stream().anyMatch(scope -> scope.containsKey(name));
    }

    public void addSymbol(String name, String type, Object value, String scopeName, int line) {
        Map<String, SymbolInfo> current = scopes.peek();
        if (current.containsKey(name)) {
            System.err.println("⚠️ Advertencia semántica [línea " + line + "]: la variable '" + name +
                    "' ya fue declarada en este ámbito.");
            return;
        }
        current.put(name, new SymbolInfo(name, type, value, scopeName, line));
    }

    public SymbolInfo getSymbol(String name) {
        for (Map<String, SymbolInfo> scope : scopes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    public void print() {
        System.out.println("\n=== TABLA DE SÍMBOLOS ===");
        System.out.printf("%-10s %-10s %-10s %-10s %-10s%n", "Nombre", "Tipo", "Valor", "Ámbito", "Línea");
        System.out.println("------------------------------------------------------");
        List<Map<String, SymbolInfo>> list = new ArrayList<>(scopes);
        Collections.reverse(list);
        for (Map<String, SymbolInfo> scope : list) {
            for (SymbolInfo s : scope.values()) {
                System.out.printf("%-10s %-10s %-10s %-10s %-10d%n",
                        s.name, s.type, (s.value != null ? s.value : "-"), s.scope, s.line);
            }
        }
    }

    public static class SymbolInfo {
        public final String name;
        public final String type;
        public Object value;
        public final String scope;
        public final int line;

        public SymbolInfo(String name, String type, Object value, String scope, int line) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.scope = scope;
            this.line = line;
        }
    }
}

