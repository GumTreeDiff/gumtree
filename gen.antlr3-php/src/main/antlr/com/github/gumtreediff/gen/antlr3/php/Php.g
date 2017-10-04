//Todo: Labels and goto have been disabled, need to test on 5.3 

grammar Php;

options {
    backtrack = true; 
    memoize = true;
    k=2;
    output = AST;
    ASTLabelType = CommonTree;
}

tokens{
    SemiColon = ';';
    Comma = ',';
    OpenBrace = '(';
    CloseBrace = ')';
    OpenSquareBrace = '[';
    CloseSquareBrace = ']';
    OpenCurlyBrace = '{';
    CloseCurlyBrace = '}';
    ArrayAssign = '=>';
    LogicalOr = '||';
    LogicalAnd = '&&';
    ClassMember = '::';
    InstanceMember = '->';
    SuppressWarnings = '@';
    QuestionMark = '?';
    Dollar = '$';
    Colon = ':';
    Dot = '.';
    Ampersand = '&';
    Pipe = '|';
    Bang = '!';
    Plus = '+';
    Minus = '-';
    Asterisk = '*';
    Percent = '%';
    Forwardslash = '/'; 
    Tilde = '~';
    Equals = '=';
    New = 'new';
    Clone = 'clone';
    Echo = 'echo';
    If = 'if';
    Else = 'else';
    ElseIf = 'elseif';
    For = 'for';
    Foreach = 'foreach';
    While = 'while';
    Do = 'do';
    Switch = 'switch';
    Case = 'case';
    Default = 'default';
    Function = 'function';
    Break = 'break';
    Continue = 'continue';
    //Goto = 'goto';
    Return = 'return';
    Global = 'global';
    Static = 'static';
    And = 'and';
    Or = 'or';
    Xor = 'xor';
    Instanceof = 'instanceof';
    Throw = 'throw';
    
    Class = 'class';
    Interface = 'interface';
    Extends = 'extends';
    Implements = 'implements';
    Abstract = 'abstract';
    Var = 'var';
    Const = 'const';
    Modifiers;
    ClassDefinition;
    
    Block;
    Params;
    Apply;
    Member;
    Reference;
    Empty;
    Prefix;
    Postfix;
    IfExpression;
    Label;
    Cast;
    ForInit;
    ForCondition;
    ForUpdate;
    Field;
    Method;
}

@header { package com.github.gumtreediff.gen.antlr3.php; }
@lexer::header { package com.github.gumtreediff.gen.antlr3.php; }
@rulecatch {
  catch(RecognitionException re){
    throw re; // Stop at first error
  }
}
@lexer::members{
    // Handle the first token, which will always be a BodyString.
    public Token nextToken(){
        //The following code was pulled out from super.nextToken()
        if (input.index() == 0) {
            try {
                state.token = null;
                state.channel = Token.DEFAULT_CHANNEL;
                state.tokenStartCharIndex = input.index();
                state.tokenStartCharPositionInLine = input.getCharPositionInLine();
                state.tokenStartLine = input.getLine();
                state.text = null;
                mFirstBodyString();
                state.type = BodyString;
                emit();
                return state.token;
            } catch (NoViableAltException nva) {
                reportError(nva);
                recover(nva); // throw out current char and try again
            } catch (RecognitionException re) {
                reportError(re);
                // match() routine has already called recover()
            }    
        }
        return super.nextToken();
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

prog : statement*;

statement
    : simpleStatement? BodyString
    | '{' statement '}' -> statement
    | bracketedBlock
    //| UnquotedString Colon statement -> ^(Label UnquotedString statement)
    | classDefinition
    | interfaceDefinition
    | complexStatement
    | simpleStatement ';'!
    ;
    
bracketedBlock
    : '{' stmts=statement* '}' -> ^(Block $stmts)
    ;

interfaceDefinition
    : Interface interfaceName=UnquotedString interfaceExtends?
        OpenCurlyBrace
        interfaceMember*
        CloseCurlyBrace
        -> ^(Interface $interfaceName interfaceExtends? interfaceMember*)
    ;

interfaceExtends
    : Extends^ UnquotedString (Comma! UnquotedString)*
    ;
interfaceMember
    : Const UnquotedString (Equals atom)? ';' 
        -> ^(Const UnquotedString atom?)
    | fieldModifier* Function UnquotedString parametersDefinition ';'
        -> ^(Method ^(Modifiers fieldModifier*) UnquotedString parametersDefinition)
    ;

classDefinition
    :   classModifier? 
        Class className=UnquotedString 
        (Extends extendsclass=UnquotedString)? 
        classImplements?
        OpenCurlyBrace
        classMember*
        CloseCurlyBrace 
        -> ^(Class ^(Modifiers classModifier?) $className ^(Extends $extendsclass)? classImplements?
            classMember*
        )
    ;
    
classImplements
    :  Implements^ (UnquotedString (Comma! UnquotedString)*)
    ;

classMember
    : fieldModifier* Function UnquotedString parametersDefinition 
        (bracketedBlock | ';')
        -> ^(Method ^(Modifiers fieldModifier*) UnquotedString parametersDefinition bracketedBlock?)
    | Var Dollar UnquotedString (Equals atom)? ';' 
        -> ^(Var ^(Dollar UnquotedString) atom?) 
    | Const UnquotedString (Equals atom)? ';' 
        -> ^(Const UnquotedString atom?)
    | fieldModifier* (Dollar UnquotedString) (Equals atom)? ';' 
        -> ^(Field ^(Modifiers fieldModifier*) ^(Dollar UnquotedString) atom?)
    ;

fieldDefinition
    : Dollar UnquotedString (Equals atom)? ';'-> ^(Field ^(Dollar UnquotedString) atom?)
    ;
    
classModifier
    : 'abstract';
    
fieldModifier
    : AccessModifier | 'abstract' | 'static' 
    ;


complexStatement
    : If '(' ifCondition=expression ')' ifTrue=statement conditional?
        -> ^('if' expression $ifTrue conditional?)
    | For '(' forInit forCondition forUpdate ')' statement -> ^(For forInit forCondition forUpdate statement)
    | Foreach '(' expression 'as' arrayEntry ')' statement -> ^(Foreach expression arrayEntry statement)
    | While '(' whileCondition=expression? ')' statement -> ^(While $whileCondition statement)
    | Do statement While '(' doCondition=expression ')' ';' -> ^(Do statement $doCondition)
    | Switch '(' expression ')' '{'cases'}' -> ^(Switch expression cases)
    | functionDefinition
    ;

simpleStatement
    : Echo^ commaList
    | Throw^ expression
    | Global^ name (','! name)*
    | Static^ variable Equals! atom
    | Break^ Integer?
    | Continue^ Integer?
    //| Goto^ UnquotedString
    | Return^ expression?
    | RequireOperator^ expression
    | expression
    ;


conditional
    : ElseIf '(' ifCondition=expression ')' ifTrue=statement conditional? -> ^(If $ifCondition $ifTrue conditional?)
    | Else statement -> statement
    ;

forInit
    : commaList? ';' -> ^(ForInit commaList?)
    ;

forCondition
    : commaList? ';' -> ^(ForCondition commaList?)
    ;
    
forUpdate
    : commaList? -> ^(ForUpdate commaList?)
    ;

cases 
    : casestatement*  defaultcase?
    ;

casestatement
    : Case^ expression ':'! statement*
    ;

defaultcase 
    : (Default^ ':'! statement*)
    ;

functionDefinition
    : Function UnquotedString parametersDefinition bracketedBlock -> 
        ^(Function UnquotedString parametersDefinition bracketedBlock)
    ;

parametersDefinition
    : OpenBrace (paramDef (Comma paramDef)*)? CloseBrace -> ^(Params paramDef*) 
    ;

paramDef
    : paramName (Equals^ atom)?
    ;

paramName
    : Dollar^ UnquotedString
    | Ampersand Dollar UnquotedString -> ^(Ampersand ^(Dollar UnquotedString))
    ;

commaList
    : expression (','! expression)* 
    ;
    
expression
    : weakLogicalOr
    ;

weakLogicalOr
    : weakLogicalXor (Or^ weakLogicalXor)*
    ;

weakLogicalXor
    : weakLogicalAnd (Xor^ weakLogicalAnd)*
    ;
    
weakLogicalAnd
    : assignment (And^ assignment)*
    ;

assignment
    : name ((Equals | AsignmentOperator)^ assignment)
    | ternary
    ;

ternary
    : logicalOr QuestionMark expression Colon expression -> ^(IfExpression logicalOr expression*)
    | logicalOr
    ;
    
logicalOr
    : logicalAnd (LogicalOr^ logicalAnd)*
    ;

logicalAnd
    : bitwiseOr (LogicalAnd^ bitwiseOr)*
    ;
    
bitwiseOr
    : bitWiseAnd (Pipe^ bitWiseAnd)*
    ;

bitWiseAnd
    : equalityCheck (Ampersand^ equalityCheck)*
    ;

equalityCheck
    : comparisionCheck (EqualityOperator^ comparisionCheck)?
    ;
    
comparisionCheck
    : bitWiseShift (ComparisionOperator^ bitWiseShift)?
    ;

bitWiseShift
    : addition (ShiftOperator^ addition)*
    ;
    
addition
    : multiplication ((Plus | Minus | Dot)^ multiplication)*
    ;

multiplication
    : logicalNot ((Asterisk | Forwardslash | Percent)^ logicalNot)*
    ;

logicalNot
    : Bang^ logicalNot
    | instanceOf
    ;

instanceOf
    : negateOrCast (Instanceof^ negateOrCast)?
    ;

negateOrCast
    : (Tilde | Minus | SuppressWarnings)^ increment
    | OpenBrace PrimitiveType CloseBrace increment -> ^(Cast PrimitiveType increment)
    | OpenBrace! weakLogicalAnd CloseBrace!
    | increment
    ;

increment
    : IncrementOperator name -> ^(Prefix IncrementOperator name)
    | name IncrementOperator -> ^(Postfix IncrementOperator name)
    | newOrClone
    ;

newOrClone
    : New^ nameOrFunctionCall
    | Clone^ name
    | atomOrReference
    ;

atomOrReference
    : atom
    | reference
    ;

arrayDeclaration
    : Array OpenBrace (arrayEntry (Comma arrayEntry)*)? CloseBrace -> ^(Array arrayEntry*)
    ;

arrayEntry
    : (keyValuePair | expression)
    ;

keyValuePair
    : (expression ArrayAssign expression) -> ^(ArrayAssign expression+)
    ;

atom: SingleQuotedString | DoubleQuotedString | HereDoc | Integer | Real | Boolean | arrayDeclaration
    ;

//Need to be smarter with references, they have their own tower of application.
reference
    : Ampersand^ nameOrFunctionCall
    | nameOrFunctionCall
    ;

nameOrFunctionCall
    : name OpenBrace (expression (Comma expression)*)? CloseBrace -> ^(Apply name expression*)
    | name
    ;

name: staticMemberAccess (OpenSquareBrace^ CloseSquareBrace)?
    | memberAccess (OpenSquareBrace^ CloseSquareBrace)?
    | variable (OpenSquareBrace^ CloseSquareBrace)?
    ;
    
staticMemberAccess
    : UnquotedString '::'^ variable
    ;

memberAccess
    : variable 
        ( OpenSquareBrace^ expression CloseSquareBrace!
        | '->'^ UnquotedString)*
    ;
    
variable
    : Dollar^ variable
    | UnquotedString
    ;

BodyString 
    : '?>' (('<' ~ '?')=> '<' | ~'<' )* ('<?' ('php'?))?
    ;

fragment
FirstBodyString
    : (('<' ~ '?')=> '<' | ~'<' )* '<?' ('php'?)
    ;

MultilineComment    
    : '/*' (('*' ~ '/')=>'*' | ~ '*')* '*/' {$channel=HIDDEN;}
    ;

SinglelineComment
    : '//'  (('?' ~'>')=>'?' | ~('\n'|'?'))* {$channel=HIDDEN;}
    ;

UnixComment
    : '#' (('?' ~'>')=>'?' | ~('\n'|'?'))* {$channel=HIDDEN;}
    ;
    

Array
    : ('a'|'A')('r'|'R')('r'|'R')('a'|'A')('y'|'Y')
    ;

RequireOperator
    : 'require' | 'require_once' | 'include' | 'include_once'
    ;

PrimitiveType
    : 'int'|'float'|'string'|'array'|'object'|'bool'
    ;

AccessModifier
    : 'public' | 'private' | 'protected' 
    ;

fragment
Decimal	
	:('1'..'9' ('0'..'9')*)|'0'
	;
fragment
Hexadecimal
	: '0'('x'|'X')('0'..'9'|'a'..'f'|'A'..'F')+
	;
	
fragment
Octal
	: '0'('0'..'7')+
	;
Integer
	:Octal|Decimal|Hexadecimal
	;
	
fragment
Digits
	: '0'..'9'+
	;
	
fragment
DNum
	:(('.' Digits)=>('.' Digits)|(Digits '.' Digits?))
	;
	
fragment
Exponent_DNum
	:((Digits|DNum)('e'|'E')('+''-')?Digits)
	;
	
Real
    : DNum|Exponent_DNum
    ;

Boolean
    : 'true' | 'false'
    ;

SingleQuotedString
    : '\'' (('\\' '\'')=>'\\' '\''
    |         ('\\' '\\')=>'\\' '\\' 
    |         '\\' | ~ ('\'' | '\\'))* 
      '\''
    ;

fragment
EscapeCharector
    : 'n' | 'r' | 't' | '\\' | '$' | '"' | Digits | 'x'
    ;

DoubleQuotedString
    : '"'  ( ('\\' EscapeCharector)=> '\\' EscapeCharector 
    | '\\' 
    | ~('\\'|'"') )* 
      '"'
    ;

HereDoc 
    : '<<<' HereDocContents
    ;

//Todo handle '\x7f' - '\xff'
UnquotedString
   : ('a'..'z' | 'A'..'Z' | '_')  ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')*
   ;
   
//TODO: add error handling
fragment 
HereDocContents
    : {
        StringBuilder sb = new StringBuilder();
        while(input.LA(1)!='\n'){
            sb.append((char)input.LA(1));
            input.consume();
        }
        input.consume();
        String hereDocName = sb.toString();
        int hdnl = hereDocName.length();
        while(true){
            boolean matchEnd = true;
            for(int i = 0; i<hdnl; i++){
                if(input.LA(1)!=hereDocName.charAt(i)){
                    matchEnd=false;
                    break;
                }
                input.consume();
            }
            if(matchEnd==false){
                while(input.LA(1)!='\n'){
                    input.consume();
                }
                input.consume();
            }else{
                break;
            }
        }
    }
    ;

AsignmentOperator
    : '+='|'-='|'*='|'/='|'.='|'%='|'&='|'|='|'^='|'<<='|'>>='
    ;
    
EqualityOperator
    : '==' | '!=' | '===' | '!=='
    ;

ComparisionOperator
    : '<' | '<=' | '>' | '>=' | '<>'
    ;
    
ShiftOperator
    : '<<' | '>>'
    ;

IncrementOperator
    : '--'|'++'
    ;
    

fragment
Eol : '\n'
    ;

WhiteSpace
@init{
    $channel=HIDDEN;
}
	:	(' '| '\t'| '\n'|'\r')*
	;