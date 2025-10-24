package lexer;

public enum TokenType {
    // Palabras reservadas
    LONG, DOUBLE, IF, THEN, ELSE, WHILE, BREAK, READ, WRITE,
    TRUE, FALSE, // (opcional, por si los usás como booleanos)

    // Identificadores y literales
    IDENTIFIER, INT_LITERAL, DOUBLE_LITERAL, STRING_LITERAL,

    // Operadores aritméticos
    PLUS, MINUS, STAR, SLASH,

    // Operadores relacionales y lógicos
    GT, LT, GE, LE, EQEQ, NEQ, // NEQ vale tanto para "!=" como "<>"
    // Lógicos opcionales
    AND_AND, OR_OR, BANG,

    // Asignación y agrupación
    ASSIGN, LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON,

    // Asignaciones compuestas opcionales
    PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ,

    // Fin de archivo
    EOF
}

