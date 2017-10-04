grammar JSON;

options {
	output = AST;
}

tokens {
	STRING; NUMBER; OBJECT; FIELD; ARRAY;
	COMMA = ',';
	TRUE; FALSE; NULL;
}

@header {
package com.github.gumtreediff.gen.antlr3.json;

import java.util.regex.Pattern;
  
}

@lexer::header {
package com.github.gumtreediff.gen.antlr3.json;
}

// Optional step: Disable automatic error recovery
@members { 
protected void mismatch(IntStream input, int ttype, BitSet follow) 
throws RecognitionException 
{ 
throw new MismatchedTokenException(ttype, input); 
} 
public Object recoverFromMismatchedSet(IntStream input, 
RecognitionException e, 
BitSet follow) 
throws RecognitionException 
{ 
throw e; 
} 
} 
// Alter code generation so catch-clauses get replace with 
// this action. 
@rulecatch { 
catch (RecognitionException e) { 
throw e; 
} 
} 



value
	: string
	| number
	| object
	| array
	| 'true' -> TRUE
	| 'false' -> FALSE
	| 'null' -> NULL
	;

string 	: String
	  -> ^(STRING String)
	;

// If you want to conform to the RFC, use a validating semantic predicate to check the result.
number	: n=Number {Pattern.matches("(0|(-?[1-9]\\d*))(\\.\\d+)?", n.getText())}? 
	    Exponent? 
	  -> ^(NUMBER Number Exponent?)
	;

object	: '{' members? '}' 
	  -> ^(OBJECT members?)
	;
	
array	: '[' elements? ']'
	  -> ^(ARRAY elements?)
	;

elements: value (COMMA! value)*
	;
	
members	: pair (COMMA! pair)*
	;
	 
pair	: String ':' value 
	  -> ^(FIELD String value) 
	;

Number	: '-'? Digit+ ( '.' Digit+)?;

Exponent: ('e'|'E') '-'? ('1'..'9') Digit*;

String 	:
	'"' ( EscapeSequence | ~('\u0000'..'\u001f' | '\\' | '\"' ) )* '"'
	;

WS: (' '|'\n'|'\r'|'\t')+ {$channel=HIDDEN;} ; // ignore whitespace 

fragment EscapeSequence
    	:   '\\' (UnicodeEscape |'b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\'|'\/')
    	;

fragment UnicodeEscape
	: 'u' HexDigit HexDigit HexDigit HexDigit
	;

fragment HexDigit
	: '0'..'9' | 'A'..'F' | 'a'..'f'
	;

fragment Digit
	: '0'..'9'
	;

