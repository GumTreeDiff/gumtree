tree grammar JSONTree;

options { 
tokenVocab=JSON; // reuse token types 
ASTLabelType=CommonTree; // $label will have type CommonTree 
} 

@header {
package com.github.gumtreediff.gen.antlr3.json;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

}

@members {
    private Object extractNumber(CommonTree numberToken, CommonTree exponentToken) {
        String numberBody = numberToken.getText();
        String exponent = (exponentToken == null) ? null : exponentToken.getText().substring(1); // remove the 'e' prefix if there
        boolean isReal = numberBody.indexOf('.') >= 0 || exponent != null;
        if (!isReal) {
            return new Integer(numberBody);
        } else {
            double result = Double.parseDouble(numberBody);
            if (exponent != null) {
                result = result * Math.pow(10.0f, Double.parseDouble(exponent));
            }
            return new Double(result);
        }
    }
    
    private String extractString(CommonTree token) {
        // StringBuffers are an efficient way to modify strings
        StringBuffer sb = new StringBuffer(token.getText());
        // Process character escapes
        int startPoint = 1; // skip initial quotation mark
        for (;;) {
            int slashIndex = sb.indexOf("\\", startPoint); // search for a single backslash
            if (slashIndex == -1) break;
            // Else, we have a backslash
            char escapeType = sb.charAt(slashIndex + 1);
            switch (escapeType) {
                case'u':
                    // Unicode escape.
                    String unicode = extractUnicode(sb, slashIndex);
                    sb.replace(slashIndex, slashIndex + 6, unicode); // backspace
                    break; // back to the loop

                    // note: Java's character escapes match JSON's, which is why it looks like we're replacing
                // "\b" with "\b". We're actually replacing 2 characters (slash-b) with one (backspace).
                case 'b':
                    sb.replace(slashIndex, slashIndex + 2, "\b"); // backspace
                    break;

                case 't':
                    sb.replace(slashIndex, slashIndex + 2, "\t"); // tab
                    break;

                case 'n':
                    sb.replace(slashIndex, slashIndex + 2, "\n"); // newline
                    break;

                case 'f':
                    sb.replace(slashIndex, slashIndex + 2, "\f"); // form feed
                    break;

                case 'r':
                    sb.replace(slashIndex, slashIndex + 2, "\r"); // return
                    break;

                case '\'':
                    sb.replace(slashIndex, slashIndex + 2, "\'"); // single quote
                    break;

                case '\"':
                    sb.replace(slashIndex, slashIndex + 2, "\""); // double quote
                    break;

                case '\\':
                    sb.replace(slashIndex, slashIndex + 2, "\\"); // backslash
                    break;
                    
                case '/':
                    sb.replace(slashIndex, slashIndex + 2, "/"); // solidus
                    break;

            }
            startPoint = slashIndex+1;

        }

        // remove surrounding quotes
        sb.deleteCharAt(0);
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    private String extractUnicode(StringBuffer sb, int slashIndex) {
        // Gather the 4 hex digits, convert to an integer, translate the number to a unicode char, replace
        String result;
        String code = sb.substring(slashIndex + 2, slashIndex + 6);
        int charNum = Integer.parseInt(code, 16); // hex to integer
        // There's no simple way to go from an int to a unicode character.
        // We'll have to pass this through an output stream writer to do
        // the conversion.
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
            osw.write(charNum);
            osw.flush();
            result = baos.toString("UTF-8"); // Thanks to Silvester Pozarnik for the tip about adding "UTF-8" here
        } catch (Exception e) {
            e.printStackTrace();
            result = null;
        }
        return result;
    }

}

value returns [Object result]
	: s=string { $result = s; } 
	| n=number { $result = n; }
	| o=object { $result = o; }
	| a=array { $result = a; }
	| TRUE { $result=Boolean.TRUE; }
	| FALSE {$result = Boolean.FALSE; }
	| NULL {$result = null; }
	;

string returns [String result]
	: ^(STRING String)
	  { $result = extractString($String); }
	;
	
object returns [Map result]
@init { result = new HashMap(); }
	: ^(OBJECT pair[$result]+)
	;

number	returns [Object result] 
	: ^(NUMBER Number Exponent?)
	  { $result = extractNumber($Number, $Exponent); }
	;

array	returns [List list]
@init{ list = new ArrayList(); }
	: ^(ARRAY (v=value {$list.add(v); })* )
	;
	
pair [Map map]
	: ^(FIELD key=String v=value) 
	   { $map.put(extractString($key), v); }
	;



