grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

SINGLE_LINE_COMMENT : '//' [ \t]* ~[\r\n]* -> skip ;
MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' name+=ID ('.' name+=ID)* ';' #ImportDeclare
    ;

classDeclaration
    : 'class' name=ID ( 'extends' extend=ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}' #ClassDeclare
    ;

varDeclaration
    : type name=ID ';' #VarDeclare
    ;

methodDeclaration
    : ('public')? type name=ID '(' ( param ( ',' param )* )? ')' '{' ( varDeclaration)* ( statement )* 'return' expression ';' '}' #MethodDeclare
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' parameter=ID ')' '{' ( varDeclaration)* ( statement )* '}' #MethodDeclareMain
    ;

param
    : type name=ID
    ;

type locals[boolean isArray=false]
    : name = 'int' ( '[' ']' {$isArray=true;} )?
    | name = 'boolean'
    | name = 'String'
    | name = ID
    ;


statement
    : 'if' '(' expression ')' statement 'else' statement #Todo
    | 'while' '(' expression ')' statement #Todo
    | '{' (statement)* '}' #Todo
    | ID '=' expression ';' #Todo
    | expression ';' #Todo
    | ID '[' expression ']' '=' expression ';' #Todo
    ;

expression
    : '(' expression ')' #ParOp
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #FuncOp
    | 'new' 'int' '[' expression ']' #NewArr
    | 'new' ID '(' ')' #NewFunc
    | '!' expression #NegationOp
    | expression '.' 'length' #LengthOp
    | expression '[' expression ']' #IndexOp
    | expression op='&&' expression #BinaryOp
    | expression op = '<' expression #BinaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | value=INTEGER #Integer
    | value=ID #Identifier
    | 'true' #True
    | 'false' #False
    | 'this' #This
    ;
