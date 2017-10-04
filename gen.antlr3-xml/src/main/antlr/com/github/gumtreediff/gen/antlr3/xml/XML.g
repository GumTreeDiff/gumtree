grammar XML;

options { 
  //tokenVocab=XMLLexer; 
  output=AST;
  ASTLabelType = CommonTree;
}

tokens {
  ELEMENT;
  ELEMENT_ID;
  ATTRIBUTE;
  ATTRIBUTES;
  ATTRIBUTE_ID;
}

@header { package com.github.gumtreediff.gen.antlr3.xml; }
@lexer::header { package com.github.gumtreediff.gen.antlr3.xml; }

@lexer::members {
    boolean tagMode = false;
}

document : element ;
 
element
    : ( startTag^
            (element
            | PCDATA
            )*
            endTag!
        | emptyElement
        )
    ;
 
startTag : TAG_START_OPEN g=GENERIC_ID attribute* TAG_CLOSE
        -> ^(ELEMENT ELEMENT_ID[g] attribute*)
    ;
 
attribute : g=GENERIC_ID ATTR_EQ ATTR_VALUE -> ^(ATTRIBUTE ATTRIBUTE_ID[g] ATTR_VALUE) ;
 
endTag! : TAG_END_OPEN GENERIC_ID TAG_CLOSE;
 
emptyElement : TAG_START_OPEN g=GENERIC_ID attribute* TAG_EMPTY_CLOSE
        -> ^(ELEMENT ELEMENT_ID[g] attribute*)
    ;
 
TAG_START_OPEN : '<' { tagMode = true; } ;
TAG_END_OPEN : '</' { tagMode = true; } ;
TAG_CLOSE : { tagMode }?=> '>' { tagMode = false; } ;
TAG_EMPTY_CLOSE : { tagMode }?=> '/>' { tagMode = false; } ;
 
ATTR_EQ : { tagMode }?=> '=' ;
 
ATTR_VALUE : { tagMode }?=>
        ( '"' (~'"')* '"'
        | '\'' (~'\'')* '\''
        )
    ;
 
PCDATA : { !tagMode }?=> (~'<')+ ;
 
GENERIC_ID
    : { tagMode }?=>
      ( LETTER | '_' | ':') (NAMECHAR)*
    ;
 
fragment NAMECHAR
    : LETTER | DIGIT | '.' | '-' | '_' | ':'
    ;
 
fragment DIGIT
    :    '0'..'9'
    ;
 
fragment LETTER
    : 'a'..'z'
    | 'A'..'Z'
    ;
 
WS  :  { tagMode }?=>
       (' '|'\r'|'\t'|'\u000C'|'\n') { _channel=99; }
    ;