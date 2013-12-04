grammar R;

options {
  language = Java ;
  memoize = true;
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

@header {
package r.parser;

import r.*;
import r.data.*;
import r.nodes.*;
import r.nodes.Call.*;
import r.nodes.UnaryOperation.*;
import r.nodes.BinaryOperation.*;
//Checkstyle: stop
}
@lexer::header {
package r.parser;
//Checkstyle: stop
}
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

@lexer::members{
    public final int MAX_INCOMPLETE_SIZE = 1000;
    int incomplete_stack[] = new int[MAX_INCOMPLETE_SIZE]; // TODO probably go for an ArrayList of int
    int incomplete_depth;
    
    @Override
    public void reportError(RecognitionException e) {
        throw new RuntimeException(e);
    }
}
@lexer::init{
    incomplete_depth = 0;
    incomplete_stack[incomplete_depth] = 0;
}

/****************************************************
** Known errors : 
** - No help support '?' & '??'
** - %OP% not very robust, maybe allow everything
** - '.' is a valid id
*****************************************************/

script
	: n_ (statement )*
	;
interactive
	: n_ (statement )*
	;
statement
	: expr_or_assign n 
	| '--EOF--' .* EOF
	;

n_	: (NEWLINE | COMMENT)*;
n	: (NEWLINE | COMMENT)+ | EOF | SEMICOLON n_;

expr_or_assign
	: alter_assign 
	;
expr
	: assign 
	;	
expr_wo_assign
	: while_expr 
	| if_expr 
	| for_expr 
	| repeat_expr 
	| function 
	| NEXT /* ((LPAR)=>LPAR n_ RPAR)? */  
	| BREAK /* ((LPAR)=>LPAR n_ RPAR)? */ 
	;
sequence
	: LBRACE n_ (expr_or_assign  (n expr_or_assign )* n?)?  RBRACE  
	;
assign
	: tilde_expr	
		( ARROW n_ expr 
		| SUPER_ARROW n_ expr 
		| RIGHT_ARROW n_ expr 
		| SUPER_RIGHT_ARROW n_ expr 
		| 
		)
	;
alter_assign
	: tilde_expr	
		( (ARROW)=>ARROW n_ expr_or_assign 
		| (SUPER_ARROW)=>SUPER_ARROW n_ expr_or_assign 
		| (RIGHT_ARROW)=>RIGHT_ARROW n_ expr_or_assign 
		| (SUPER_RIGHT_ARROW)=>SUPER_RIGHT_ARROW n_ expr_or_assign 
		| (ASSIGN)=>ASSIGN n_ expr_or_assign 
		| 
		)
	;
if_expr
	:
	IF n_ LPAR n_ expr_or_assign n_ RPAR n_ expr_or_assign
	((n_ ELSE)=>(options {greedy=false; backtrack=true;}: n_ ELSE n_ expr_or_assign )
    | 
	)
	;
while_expr
	: WHILE n_ LPAR n_ expr_or_assign n_ RPAR n_ expr_or_assign 
	;
for_expr
	: FOR n_ LPAR n_ ID n_ IN n_ expr_or_assign n_ RPAR n_ expr_or_assign  
	;
repeat_expr
	: REPEAT n_ expr_or_assign 
	;
function
	: FUNCTION n_ LPAR  n_ (par_decl (n_ COMMA n_ par_decl)* n_)? RPAR n_ expr_or_assign  
	;
par_decl
	: ID  
	| ID n_ ASSIGN n_ expr 
	| VARIATIC  // FIXME This is not quite good, since `...` is a special token
	                                 // For this reason let's call RSymbol.xxxx(...)
	// This 3 cases were not handled ... and everything was working fine
	// I add them for completeness, however note that the function create
	// with such a signature will always fail if they try to access them !
 	| VARIATIC n_ ASSIGN n_ expr
 	| DD
 	| DD n_ ASSIGN n_ expr
	;
tilde_expr
	: utilde_expr 
	( ((TILDE)=>TILDE n_ utilde_expr  ))*
	;
utilde_expr
	: TILDE n_ or_expr 
	| or_expr ;
or_expr
	: and_expr 
	(((or_operator)=>or_operator n_ and_expr  ))*
	;
and_expr
	: not_expr 
    (((and_operator)=>and_operator n_ not_expr  ))*
	;
not_expr
	: NOT n_ not_expr 
	| comp_expr 
	;
comp_expr
	: add_expr 
    (((comp_operator)=>comp_operator n_ add_expr  ))*
    ;
add_expr
	: mult_expr 
	 (((add_operator)=>add_operator n_ mult_expr  ))*
	;
mult_expr
	: operator_expr 
	(((mult_operator)=>mult_operator n_ operator_expr  ))*
	;
operator_expr
	: colon_expr 
	(((OP)=>OP n_ colon_expr  ))*  /* FIXME BinaryOperation.create(op, $operator_expr.v, $r.v); */ 
	;
colon_expr
	: unary_expression 
	(((COLON)=>COLON n_ unary_expression  ))*
	;
unary_expression
	: PLUS n_ unary_expression 
	| MINUS n_ unary_expression 
	| power_expr 
	;
power_expr
	: basic_expr 
    (((power_operator)=>power_operator n_ power_expr  )
    |)
    ;
basic_expr
	: simple_expr 
	(((FIELD|AT|LBRAKET|LBB|LPAR)=>expr_subset )+ | (n_)=>)
	;
expr_subset
    : (FIELD n_ id)  
    | (AT n_ id)   
    | (LBRAKET args RBRAKET) 
    | (LBB args RBRAKET RBRAKET) 
    // Must use RBRAKET in`stead of RBB beacause of : a[b[1]]
    | (LPAR args RPAR)   
    //| 
    ;
simple_expr
	: id 
	| bool 
	| DD
	| NULL 
	| number 
	| conststring 
	| id NS_GET n_ id
	| id NS_GET_INT n_ id
	| LPAR n_ expr_or_assign n_ RPAR 
	| sequence 
	| expr_wo_assign 
	;
number
    : INTEGER 
    | DOUBLE 
    | COMPLEX 
    ;
conststring
    : STRING 
    ;
id
    : ID 
    | VARIATIC 
    ;
bool
    : TRUE 
    | FALSE 
    | NA 
    ;
or_operator
	: OR          
 	| BITWISEOR   ;
and_operator
	: AND          
	| BITWISEAND   ;
comp_operator
	: GT 
	| GE 
	| LT 
	| LE 
	| EQ 
	| NE ;
add_operator
	: PLUS 
	| MINUS ;	
mult_operator
	: MULT 
	| MAT_MULT 
	| DIV  
	| MOD  ;
power_operator
	: CARRET 
	;
args
    : (n_ arg_expr)? n_ (COMMA (  | n_ arg_expr) n_)* 
	;
arg_expr
	: expr 
	| id n_ ASSIGN n_ expr 
	| id n_ ASSIGN  
	| NULL n_ ASSIGN n_ expr 
	| NULL n_ ASSIGN 
	;
///////////////////////////////////////////////////////////////////////////////
/// Lexer
///
COMMENT
    :   '#' ~('\n'|'\r'|'\f')* (LINE_BREAK | EOF)	{ if(incomplete_stack[incomplete_depth]>0) $channel=HIDDEN; }
    ;
ARROW
	: '<-' | ':='
	;
SUPER_ARROW 
	:	 '<<-' ;
RIGHT_ARROW 
	: '->'
	;
SUPER_RIGHT_ARROW 
	:	'->>'
	;
VARIATIC 
	: '..' '.'+
	; // FIXME
EQ	: '==';
NE 	: '!=';
GE	: '>=';
LE	: '<=';
GT	: '>';
LT 	: '<';
ASSIGN 
	: '=';


NS_GET_INT
	: ':::';
NS_GET
	: '::';

COLON
	: ':';
SEMICOLON
	: ';';
COMMA
	: ',';
AND
	: '&&';
BITWISEAND 
	: '&';
OR	: '||';
BITWISEOR
	:'|';
LBRACE 
	: '{'	{incomplete_stack[++incomplete_depth] = 0; }; // TODO grow the stack
RBRACE 
	: '}'	{incomplete_depth -- ;};
LPAR 
	: '('	{ incomplete_stack[incomplete_depth] ++; };
RPAR
	: ')'	{ incomplete_stack[incomplete_depth]--; };
LBB
	: '[['	{ incomplete_stack[incomplete_depth] += 2; }; // Must increase by two beacause of ']'']' used for closing
LBRAKET
	: '['	{ incomplete_stack[incomplete_depth] ++; };
RBRAKET
	: ']'	{ incomplete_stack[incomplete_depth] --;};
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
MAT_MULT
	: '%*%';

DIV	: '/';
MINUS
	: '-';

FIELD
	: '$';
AT	: '@';

FUNCTION
	: 'function';
NULL
	: 'NULL';

NA
    : 'NA';
TRUE
	: 'TRUE';
FALSE
	: 'FALSE';

WHILE 
	: 'while';
FOR	: 'for';
REPEAT
	: 'repeat';
IN	: 'in';
IF	: 'if';
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
	: LINE_BREAK	{ if(incomplete_stack[incomplete_depth]>0) $channel=HIDDEN; };
INTEGER
    :   ('0'..'9')+ '.' ('0'..'9')* 'L' 
    |   '.'? ('0'..'9')+ EXPONENT? 'L' 
    |   '0x' HEX_DIGIT+ 'L' 
    ;
COMPLEX
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT? 'i'  
    |   '.'? ('0'..'9')+ EXPONENT? 'i' 
    |   '0x' HEX_DIGIT 'i' 
    ;
DOUBLE
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.'? ('0'..'9')+ EXPONENT?
    |	'0x' HEX_DIGIT
    ;
DD	: '..' ('0'..'9')+
	;  
ID  : '.'* ID_NAME
	| '.'
	| '`' ( ESC_SEQ | ~('\\'|'`') )* '`'   
	;
OP	: '%' OP_NAME+ '%'
	;
/*
STRING
    :
    ( '"' ( ESC_SEQ | ~('\\'|'"') )* '"' 
    | '\'' ( ESC_SEQ | ~('\\'|'\'') )* '\'' 
    ) {setText(getText().substring(1, getText().length()-1));} 
    ;
*/
STRING
:
    '"'
    (
    ESCAPE
    | ~( '\\' | '"' ) 
    )*
    '"'
    ;

/* not supporting \v and \a */
fragment ESCAPE :
    '\\'
    ( 't' 
    | 'n' 
    | 'r' 
    | 'b' 
    | 'f' 
    | '"' 
    | '\\' 
    | 'x' HEX_DIGIT HEX_DIGIT 
    | 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT 
    | 'U' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT 
    );
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
    |	'\\' LINE_BREAK // FIXME that's an ugly way to fix this
    |   UNICODE_ESC
    |   OCTAL_ESC
    |	HEX_ESC
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
