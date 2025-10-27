import lexer.*;
import error.*;
import parser.*;
import symboltable.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String source;
        if (args.length > 0) {
            source = Files.readString(Path.of(args[0]));
        } else {
            // Programa de ejemplo del enunciado:
            source = """
            /* Programa de ejemplo */
                    long _x;
                                            long _y;
                                            double _prom;
                    
                                            read(_x);
                                            read(_y);
                                            
                                            if (_x > _y) then
                                                _prom = (_x + _y) / 2;
                                            else
                                                _prom = (_y - _x) / 2;
                    
                                            write(_prom);
                    

            // Fin del programa
            """;
        }

        // === 1. ANALIZADOR LÉXICO ===
        ErrorHandler err = new ErrorHandler();
        Lexer lexer = new Lexer(source, err);
        List<Token> tokens = lexer.scanTokens();

        System.out.println("=== TOKENS ===");
        for (Token t : tokens) {
            System.out.println(t);
        }

        if (err.hasLexErrors()) {
            System.out.println("\n❌ Se detectaron errores léxicos:");
            err.printLexErrors();
            return;
        }

        // === 2. ANALIZADOR SINTÁCTICO ===
        Parser parser = new Parser(tokens, err);
        List<Statement> statements = parser.parse();

        if (err.hasLexErrors()) {
            System.out.println("\n❌ Se detectaron errores sintácticos:");
            err.printLexErrors();
            return;
        }

        // === 3. RESULTADO DEL PARSER ===
        System.out.println("\n✅ Análisis sintáctico exitoso.\n");
        System.out.println("=== ESTRUCTURA DEL PROGRAMA ===");
        for (Statement s : statements) {
            System.out.println(s);
        }
        System.out.println();
        parser.getSymbolTable().print();
    }
}
