grammar JavaLite;

program: classDecl EOF;
classDecl: 'class' ID '{' member* '}';
member: fieldDecl | methodDecl;
fieldDecl: type ID ('=' expr)? ';';
methodDecl: type ID '(' params? ')' block;
params: param (',' param)*;
param: type ID;
type: ('int' | 'double' | 'boolean' | 'String' | 'void') ('[' ']')?;
block: '{' stmt* '}';
stmt
    : block
    | type ID ('=' expr)? ';'
    | 'if' '(' expr ')' stmt ('else' stmt)?
    | 'while' '(' expr ')' stmt
    | 'for' '(' forInit? ';' expr? ';' expr? ')' stmt
    | 'return' expr? ';'
    | expr ';'
    ;
forInit: type ID ('=' expr)? | expr;
expr
    : <assoc=right> expr '=' expr                 # AssignExpr
    | expr '.' ID '(' args? ')'                    # MethodCallExpr
    | expr '.' ID                                 # MemberExpr
    | expr '[' expr ']'                            # IndexExpr
    | expr op=('*'|'/') expr                       # BinaryExpr
    | expr op=('+'|'-') expr                       # BinaryExpr
    | expr op=('<'|'<='|'>'|'>=') expr              # BinaryExpr
    | expr op=('=='|'!=') expr                     # BinaryExpr
    | expr op=('&&'|'||') expr                     # BinaryExpr
    | '!' expr                                     # NotExpr
    | ID '(' args? ')'                            # CallExpr
    | '(' expr ')'                                # ParenExpr
    | 'new' type '[' expr ']'                      # NewArrayExpr
    | ID                                         # IdExpr
    | INT                                        # IntExpr
    | DECIMAL                                    # DecimalExpr
    | STRING                                     # StringExpr
    | ('true'|'false'|'null')                     # LiteralExpr
    ;
args: expr (',' expr)*;
ID: [a-zA-Z_][a-zA-Z_0-9]*;
DECIMAL: [0-9]+ '.' [0-9]+;
INT: [0-9]+;
STRING: '"' ('\\' . | ~["\\])* '"';
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN);
BLOCK_COMMENT: '/*' .*? '*/' -> channel(HIDDEN);
WS: [ \t\r\n]+ -> channel(HIDDEN);
