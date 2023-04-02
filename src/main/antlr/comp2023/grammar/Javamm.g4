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
    : '{' (statement)* '}' #Brackets
    | 'if' '(' expression ')' statement 'else' statement #IfElseStatement
    | 'while' '(' expression ')' statement #WhileStatement
    | expression ';' #SemiColon
    | ID '=' expression ';' #VarDeclareStatement
    | ID '[' expression ']' '=' expression ';' #ArrayAccess
    ;

expression
    : expression op='&&' expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression '[' expression ']' #IndexOp
    | expression '.' 'length' #LengthOp
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #FuncOp
    | 'new' 'int' '[' expression ']' #NewArr
    | 'new' ID '(' ')' #NewFunc
    | '!' expression #NegationOp
    | '(' expression ')' #ParOp
    | value=INTEGER #Integer
    | value=('true' | 'false') #Bool
    | value=ID #Identifier
    | 'this' #This
    ;
