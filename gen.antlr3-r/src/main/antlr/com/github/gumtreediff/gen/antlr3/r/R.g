grammar R;

options {
  output = AST;
  language = Java ;
  ASTLabelType = CommonTree;
  memoize = true; // This is FUCK** important because of `if` 
}

tokens {
  CALL;
  BRAKET;
  KW;
  PARMS;
  SEQUENCE;
  //NULL;
  MISSING_VAL;
  UPLUS;
  UMINUS;
  UTILDE;
 }

@header { package com.github.gumtreediff.gen.antlr3.r; }
@lexer::header { package com.github.gumtreediff.gen.antlr3.r; }
@rulecatch {
  catch(RecognitionException re){
    throw re; // Stop at first error
  }
}
@lexer::rulecatch {
  catch(RecognitionException re){
    throw re; // Stop at first error ??? Doesn't work at all ??? why ??
  }
}
@members {
  public void display_next_tokens(){
    System.err.print("Allowed tokens: ");
    for(int next: next_tokens())
      System.err.print(tokenNames[next]);
    System.err.println("");
  }
  public int[] next_tokens(){
    return state.following[state._fsp].toArray();
  }
}

@lexer::members{
  int incomplete_stack[] = new int[100]; // MAX_SIZE ???
  int incomplete_depth = 0;
  @Override
  public void reportError(RecognitionException e) {
        throw new RuntimeException(e);
    }
}
@lexer::init{
  incomplete_stack[incomplete_depth] = 0;
}

/****************************************************
** Known errors : 
** - foo * if(...) ... because of priority
** - No help support '?' & '??'
** - %OP% not very robust, maybe allow everything
** - More than 3x '.' are handled like ...
** - '.' is a valid id
** - Line break are tolerated in strings even without a '\' !!! (ugly)
** - EOF does'nt work with unbalanced structs
** - Improve the stack of balanced structures 
**
** - Must add NA values ... 
*****************************************************/
script
  : n_ statement* -> ^(SEQUENCE statement*)
  ;
interactive
  : n_! statement
  ;
statement
  : expr_or_assign n!
  | '--EOF--' .* EOF ->  // Stop processing of a file (non std)
  ;

n_  : (NEWLINE | COMMENT)*;
n : (NEWLINE | COMMENT)+ | EOF | SEMICOLUMN n_;

expr_or_assign
  : alter_assign
//  | expr_wo_assign
  ;
expr
  : assign
//  | expr_wo_assign
  ; 
expr_wo_assign
  : while_expr
  | if_expr
  | for_expr
  | repeat_expr
  | function
  | NEXT (LPAR n_ RPAR)? -> NEXT
  | BREAK (LPAR n_ RPAR)? -> BREAK
  ;
sequence
  : lbb=LBRACE n_ (e+=expr_or_assign (n e+=expr_or_assign)* n?)?  RBRACE  -> ^(SEQUENCE[lbb] $e*)
  ;
// $<Assignments
assign
  : l=tilde_expr  
    ( ARROW n_ r=expr -> ^(ARROW $l $r)
    | SUPER_ARROW n_ r=expr -> ^(SUPER_ARROW $l $r)
    | a=RIGHT_ARROW n_ r=expr -> ^(ARROW[$a] $r $l)
    | a=SUPER_RIGHT_ARROW n_ r=expr -> ^(SUPER_ARROW[$a] $r $l)
    | -> $l
    )
  ;
alter_assign
  : l=tilde_expr  
    ( ARROW n_ r=expr_or_assign -> ^(ARROW $l $r)
    | SUPER_ARROW n_ r=expr_or_assign -> ^(SUPER_ARROW $l $r)
    | a=RIGHT_ARROW n_ r=expr_or_assign -> ^(ARROW[$a] $r $l)
    | a=SUPER_RIGHT_ARROW n_ r=expr_or_assign -> ^(SUPER_ARROW[$a] $r $l)
    | a=ASSIGN n_ r=expr_or_assign -> ^(ARROW[$a] $l $r)
    | -> $l
    )
  ;
// $>
if_expr
  :
  IF n_ LPAR n_ cond=expr_or_assign n_ RPAR n_ t=expr_or_assign
  /*(n_ ELSE)=>*/(
    options {greedy=false; backtrack = true;}:
    n_ ELSE n_ f=expr_or_assign
  )?
  -> ^(IF $cond $t $f?)
  ;
// $<loops
while_expr
  : WHILE n_ LPAR n_ c=expr_or_assign n_ RPAR n_ body=expr_or_assign -> ^(WHILE $c $body)
  ;
for_expr
  : FOR n_ LPAR n_ ID n_ IN n_ in=expr_or_assign n_ RPAR n_ body=expr_or_assign -> ^(FOR ID $in $body)
  ;
repeat_expr
  : REPEAT n_ body=expr_or_assign -> ^(REPEAT $body)
  ;
// $>

function
  : FUNCTION n_ LPAR  n_ (par_decl (n_ COMMA n_ par_decl)* n_)? RPAR n_ body=expr_or_assign -> ^(FUNCTION par_decl* $body)
  ;
par_decl
  : iid=ID -> ^(ID NULL[iid])
  | ID n_ ASSIGN n_ expr -> ^(ID expr)
  | VARIATIC -> VARIATIC
  ;
tilde_expr
  : or_expr (TILDE^ n_! or_expr)*
  ;
or_expr
  : and_expr (or_operator^ n_! and_expr)*
  ;
and_expr
  : comp_expr (and_operator^ n_! comp_expr)*  
  ;
comp_expr
  : add_expr (comp_operator^ n_! add_expr)* 
  ;
add_expr
  : mult_expr (add_operator^ n_! mult_expr)*
  ;
mult_expr
  : operator_expr (mult_operator^ n_! operator_expr)*
  ;
operator_expr
  : column_expr (OP^ n_! column_expr)*
  ;
column_expr
  : power_expr (COLUMN^ n_! power_expr)*
  ;
power_expr
  : l=unary_expression (power_operator^ n_! r=unary_expression)* // buggy l'associativite est a droite (?)
  ;
unary_expression
  : NOT n_ unary_expression -> ^(NOT unary_expression)
  | pl=PLUS n_ unary_expression -> ^(UPLUS[pl] unary_expression)
  | m=MINUS n_ unary_expression -> ^(UMINUS[m] unary_expression)
  | t=TILDE n_ unary_expression -> ^(UTILDE[t] unary_expression)
  | basic_expr
  ;
basic_expr
  : (lhs=simple_expr -> $lhs)
      ( (FIELD n_ name=id)  -> ^(FIELD $basic_expr $name)
      | (AT n_ name=id) -> ^(AT $basic_expr $name)
      | (LBRAKET subscript=expr_list RBRAKET) -> ^(BRAKET[lhs.start] $basic_expr $subscript?)
      | (LBB subscript=expr_list RBRAKET RBRAKET) -> ^(LBB $basic_expr $subscript?)
            // Must use RBRAKET instead of RBB beacause of : a[b[1]]
      | (LPAR a=args RPAR)   -> ^(CALL[lhs.start] $basic_expr $a?)
      )*
  ;
simple_expr
  : id
  | bool
  | DD
  | NULL
  | NUMBER
  | id NS_GET^ n_! id
  | id NS_GET_INT^ n_! id
  | LPAR! n_! expr_or_assign n_! RPAR!
  | sequence
  | expr_wo_assign
  ;
id  : ID | STRING | VARIATIC;
bool: TRUE | FALSE;
or_operator
  : OR | BITWISEOR;
and_operator
  : AND | BITWISEAND;
comp_operator
  : GT | GE | LT | LE | EQ | NE;
add_operator
  : PLUS | MINUS; 
mult_operator
  : MULT | DIV | MOD;
power_operator
  : CARRET
  ;
expr_list
  : (n_ expr_list_arg)? n_ (COMMA (n_ expr_list_arg)? n_)* -> expr_list_arg*
  ;
expr_list_arg
  : expr -> expr
  | name=id n_ ASSIGN n_ v=expr -> ^(KW[name.start] $name $v)
  ;
args: (n_ arg_expr)? n_ (COMMA (n_ arg_expr)? n_)* -> arg_expr*
  ;
arg_expr 
  : expr -> expr
  | name=id n_ ASSIGN n_ v=expr -> ^(KW[name.start] $name $v)
  | name=id n_ ass=ASSIGN -> ^(KW[name.start] $name NULL[ass]) //FIXME
  | nn=NULL n_ ASSIGN n_ v=expr -> ^(KW[nn] $nn $v)
  | nnn=NULL n_ ASSIGN -> ^(KW[nnn] $nnn $nnn) //FIXME
  ;
///////////////////////////////////////////////////////////////////////////////
/// Lexer
///
COMMENT
    :   '#' ~('\n'|'\r'|'\f')* (LINE_BREAK | EOF) { if(incomplete_stack[incomplete_depth]>0) $channel=HIDDEN; }
    ;
ARROW
  : '<-' | ':='
  ;
SUPER_ARROW 
  :  '<<-' ;
RIGHT_ARROW 
  : '->'
  ;
SUPER_RIGHT_ARROW 
  : '->>'
  ;
VARIATIC 
  : '..' '.'+
  ; // FIXME
EQ  : '==';
NE  : '!=';
GE  : '>=';
LE  : '<=';
GT  : '>';
LT  : '<';
ASSIGN 
  : '=';


NS_GET_INT
  : ':::';
NS_GET
  : '::';

COLUMN
  : ':';
SEMICOLUMN
  : ';';
COMMA
  : ',';
AND
  : '&&';
BITWISEAND 
  : '&';
OR  : '||';
BITWISEOR
  :'|';
LBRACE 
  : '{' {incomplete_stack[++incomplete_depth] = 0;}; // TODO grow the stack
RBRACE 
  : '}' {incomplete_depth -- ;};
LPAR 
  : '(' { incomplete_stack[incomplete_depth] ++; };
RPAR
  : ')' { incomplete_stack[incomplete_depth]--; };
LBB
  : '[['  { incomplete_stack[incomplete_depth] += 2; }; // Must increase by two beacause of ']'']' used for closing
LBRAKET
  : '[' { incomplete_stack[incomplete_depth] ++; };
RBRAKET
  : ']' { incomplete_stack[incomplete_depth] --;};
CARRET
  : '^' | '**';
TILDE
  : '~' ;
MOD
  : '%%' ;

NOT
  : '!';
PLUS
  : '+';
MULT
  : '*';
DIV : '/';
MINUS
  : '-';

FIELD
  : '$';
AT  : '@';

FUNCTION
  : 'function';
NULL
  : 'NULL';

TRUE
  : 'TRUE';
FALSE
  : 'FALSE';

WHILE 
  : 'while';
FOR : 'for';
REPEAT
  : 'repeat';
IN  : 'in';
IF  : 'if';
ELSE
  : 'else';
NEXT
  : 'next';
BREAK
  : 'break';
// ?

WS  :   ( ' '
        | '\t'
        ) {$channel=HIDDEN;}
    ;
NEWLINE 
  : LINE_BREAK  { if(incomplete_stack[incomplete_depth]>0) $channel=HIDDEN; };
NUMBER
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT? ('i'|'L')?
    |   '.'? ('0'..'9')+ EXPONENT? ('i'|'L')?
    | '0x' HEX_DIGIT+ 'L'?
    ;
DD  : '..' ('0'..'9')+
  ;  
ID  : '.'* ID_NAME
  | '.'
  | '`' ( ESC_SEQ | ~('\\'|'`') )* '`'  {setText(getText().substring(1, getText().length()-1));} 
  ;
OP  : '%' OP_NAME+ '%'
  ;
STRING
    :
    ( '"' ( ESC_SEQ | ~('\\'|'"') )* '"' 
    | '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\'' 
    ) {setText(getText().substring(1, getText().length()-1));} 
    ;
fragment
LINE_BREAK
  :
   (('\f'|'\r')? '\n')
  | ('\n'? ('\r'|'\f')) // This rule fix very old Mac/Dos/Windows encoded files
  ;
fragment
EXPONENT
  : ('e'|'E') ('+'|'-')? ('0'..'9')+ 
  ;
fragment
OP_NAME
  : ID_NAME
  | ('*'|'/'|'+'|'-'|'>'|'<'|'='|'|'|'&'|':'|'^'|'.'|'~'|',')
  ;
fragment
ID_NAME
  : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'.')*
  ;
fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'"'|'\''|'`'|'\\'|' '|'a'|'v')
    | '\\' LINE_BREAK // FIXME that's an ugly way to fix this
    |   UNICODE_ESC
    |   OCTAL_ESC
    | HEX_ESC
    ;
fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
fragment
HEX_ESC
  : '\\x' HEX_DIGIT HEX_DIGIT?
  ;
fragment
HEX_DIGIT
  : ('0'..'9'|'a'..'f'|'A'..'F')
  ;
fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;
