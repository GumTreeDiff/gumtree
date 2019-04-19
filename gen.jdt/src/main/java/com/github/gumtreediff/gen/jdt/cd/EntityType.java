/*
 * Copyright 2009 University of Zurich, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.gumtreediff.gen.jdt.cd;

/**
 * All types for source code entities that are used by ChangeDistiller to build up the AST (abstract syntax tree). Most
 * are taken from {@link org.eclipse.jdt.core.dom.ASTNode}.
 * 
 * @author zubi
 */
public enum EntityType {
    ANNOTATION_TYPE_DECLARATION(true),
    ANNOTATION_TYPE_MEMBER_DECLARATION(true),
    ARGUMENTS(false),
    ARRAY_ACCESS(true),
    ARRAY_CREATION(true),
    ARRAY_DIMENSION(true),
    ARRAY_INITIALIZER(true),
    ARRAY_TYPE(true),
    ASSERT_STATEMENT(true),
    ASSIGNMENT(true),
    ATTRIBUTE(false),
    BLOCK(false),
    BLOCK_COMMENT(true),
    BODY(false),
    BODY_DECLARATIONS(false),
    BOOLEAN_LITERAL(true),
    BREAK_STATEMENT(true),
    CAST_EXPRESSION(true),
    CATCH_CLAUSE(true),
    CATCH_CLAUSES(false),
    CHARACTER_LITERAL(true),
    CLASS(false),
    CLASS_INSTANCE_CREATION(true),
    COMPILATION_UNIT(true),
    CONDITIONAL_EXPRESSION(true),
    CONSTRUCTOR_INVOCATION(true),
    CONTINUE_STATEMENT(true),
    DO_STATEMENT(true),
    ELSE_STATEMENT(true),
    EMPTY_STATEMENT(true),
    ENHANCED_FOR_STATEMENT(true),
    ENUM_CONSTANT_DECLARATION(true),
    ENUM_CONSTANTS(false),
    ENUM_DECLARATION(true),
    EXPRESSION_STATEMENT(true),
    EXTENDED_OPERANDS(false),
    FIELD_ACCESS(true),
    FIELD_DECLARATION(true),
    FINALLY(false),
    FOR_STATEMENT(true),
    FRAGMENTS(false),
    IF_STATEMENT(true),
    IMPORT_DECLARATION(true),
    INFIX_EXPRESSION(true),
    INITIALIZER(true),
    INITIALIZERS(false),
    INSTANCEOF_EXPRESSION(true),
    JAVADOC(true),
    LABELED_STATEMENT(true),
    LINE_COMMENT(true),
    MARKER_ANNOTATION(true),
    MEMBER_REF(true),
    MEMBER_VALUE_PAIR(true),
    METHOD(false),
    METHOD_DECLARATION(true),
    METHOD_INVOCATION(true),
    METHOD_REF(true),
    METHOD_REF_PARAMETER(true),
    MODIFIER(true),
    MODIFIERS(false),
    NORMAL_ANNOTATION(true),
    NULL_LITERAL(true),
    NUMBER_LITERAL(true),
    PACKAGE_DECLARATION(true),
    PARAMETERIZED_TYPE(true),
    PARAMETERS(false),
    PARENTHESIZED_EXPRESSION(true),
    POSTFIX_EXPRESSION(true),
    PREFIX_EXPRESSION(true),
    PRIMITIVE_TYPE(true),
    QUALIFIED_NAME(true),
    QUALIFIED_TYPE(true),
    RETURN_STATEMENT(true),
    ROOT_NODE(true),
    SIMPLE_NAME(true),
    SIMPLE_TYPE(true),
    SINGLE_MEMBER_ANNOTATION(true),
    SINGLE_VARIABLE_DECLARATION(true),
    STRING_LITERAL(true),
    SUPER_CONSTRUCTOR_INVOCATION(true),
    SUPER_FIELD_ACCESS(true),
    SUPER_INTERFACE_TYPES(false),
    SUPER_METHOD_INVOCATION(true),
    SWITCH_CASE(true),
    SWITCH_STATEMENT(true),
    SYNCHRONIZED_STATEMENT(true),
    TAG_ELEMENT(true),
    TEXT_ELEMENT(true),
    THEN_STATEMENT(true),
    THIS_EXPRESSION(true),
    THROW(false),
    THROW_STATEMENT(true),
    TRY_STATEMENT(true),
    TYPE_ARGUMENTS(false),
    TYPE_DECLARATION(true),
    TYPE_DECLARATION_STATEMENT(true),
    TYPE_LITERAL(true),
    TYPE_PARAMETER(true),
    UPDATERS(false),
    VARIABLE_DECLARATION_EXPRESSION(true),
    VARIABLE_DECLARATION_FRAGMENT(true),
    VARIABLE_DECLARATION_STATEMENT(true),
    WHILE_STATEMENT(true),
    WILDCARD_TYPE(true);

    private final boolean fIsValidChange;

    EntityType(boolean isValidChange) {
        fIsValidChange = isValidChange;
    }

    /**
     * Returns number of defined entity types.
     * 
     * @return number of entity types.
     */
    public static int getNumberOfEntityTypes() {
        return values().length;
    }

    /**
     * Returns whether changes occurred on this source code entity type are extracted by ChangeDistiller or not (e.g.
     * changes in the <code>finally</code> clause are ignored).
     * 
     * @return <code>true</code> if changes on this entity type are considered and extracted, <code>false</code>
     *         otherwise.
     */
    public boolean isValidChange() {
        return fIsValidChange;
    }

    /**
     * Returns whether the given entity type is a type of a type declaration or not.
     * 
     * @param type
     *            to analyze
     * @return <code>true</code> if given entity type is a type, <code>false</code> otherwise.
     */
    public static boolean isType(EntityType type) {
        switch (type) {
            case ARRAY_TYPE:
            case PARAMETERIZED_TYPE:
            case PRIMITIVE_TYPE:
            case QUALIFIED_TYPE:
            case SIMPLE_TYPE:
            case WILDCARD_TYPE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns whether the given entity type is a statement or not.
     * 
     * @param type
     *            to analyze
     * @return <code>true</code> if given entity type is a statement, <code>false</code> otherwise.
     */
    public static boolean isAtStatementLevel(EntityType type) {
        switch (type) {
            case ASSERT_STATEMENT:
            case ASSIGNMENT:
            case BREAK_STATEMENT:
            case CATCH_CLAUSE:
            case CLASS_INSTANCE_CREATION:
            case CONSTRUCTOR_INVOCATION:
            case CONTINUE_STATEMENT:
            case DO_STATEMENT:
            case FINALLY:
            case FOR_STATEMENT:
            case IF_STATEMENT:
            case LABELED_STATEMENT:
            case METHOD_INVOCATION:
            case RETURN_STATEMENT:
            case SUPER_CONSTRUCTOR_INVOCATION:
            case SUPER_METHOD_INVOCATION:
            case SWITCH_CASE:
            case SWITCH_STATEMENT:
            case SYNCHRONIZED_STATEMENT:
            case THROW_STATEMENT:
            case TRY_STATEMENT:
            case VARIABLE_DECLARATION_STATEMENT:
            case WHILE_STATEMENT:
            case ENHANCED_FOR_STATEMENT:
                return true;
            default:
                return false;
        }
    }
}
