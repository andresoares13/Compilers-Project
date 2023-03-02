grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF
    ;


importDeclaration
    : 'import' ID ('.' ID)* ';'
    ;


classDeclaration
    : 'class' ID ( 'extends' ID )? '{' ( varDeclaration )* ( methodDeclaration )* '}'
    ;

varDeclaration
    : type ID ';'
    ;


methodDeclaration
    : ('public')? type ID '(' ( type ID ( ',' type ID )* )? ')' '{' ( varDeclaration)* ( statement )* 'return' expression ';' '}'
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' ( varDeclaration)* ( statement )* '}'
    ;



type
    : 'int' '[' ']'
    | 'boolean'
    | 'String'
    | 'int'
    | ID
    ;

statement
    : 'while' '(' expression ')' statement
    | 'if' '(' expression ')' statement 'else' statement
    | '{' (statement)* '}'
    | expression ';'
    | ID '=' INTEGER ';'
    | ID '=' ID ';'
    | ID '[' expression ']' '=' expression ';'
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
