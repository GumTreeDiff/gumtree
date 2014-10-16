package fr.labri.gumtree.gen.jdt.cd;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.*;

import fr.labri.gumtree.gen.jdt.AbstractJdtVisitor;
import fr.labri.gumtree.tree.ITree;
import fr.labri.gumtree.tree.Tree;

/**
 * Combination of two ChangeDistiller's AST visitors: JavaASTBodyTransformer and JavaASTChangeDistillerVisitor.
 * Modifications are labeled as "@Inria"
 */
@SuppressWarnings("unused")
public class CdJdtVisitor extends AbstractJdtVisitor {

    private static final String COLON_SPACE = ": ";
    private boolean fEmptyJavaDoc;
    private Stack<Tree> fNodeStack = new Stack<Tree>();
    private boolean fInMethodDeclaration;

    /**
     * Creates a new declaration transformer.
     *
     * @param root
     *            the root node of the tree to generate
     * @param source
     *            the document in which the AST to parse resides
     * @param astHelper
     *            the helper that helps with conversions for the change history meta model
     */
    public CdJdtVisitor(Tree root) {
        fNodeStack.clear();
        fNodeStack.push(root);
    }

    public CdJdtVisitor() {
        fNodeStack.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(Block node) {
        // skip block as it is not interesting
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(Block node) {
    // do nothing pop is not needed (see visit(Block))
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(FieldDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        //@Inria
        push(node,node.toString());
        //
        visitList(EntityType.MODIFIERS, node.modifiers());
        node.getType().accept(this);
        visitList(EntityType.FRAGMENTS, node.fragments());


        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(FieldDeclaration node) {
       //@Inria
    	pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(Javadoc node) {
        String string = "";
        //@Inria:  exclude doc
        /*  try {
            string = fSource.get(node.getStartPosition(), node.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }*/
        if (checkEmptyJavaDoc(string)) {
            pushValuedNode(node, string);
        } else {
            fEmptyJavaDoc = true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(Javadoc node) {
        if (!fEmptyJavaDoc) {
            pop();
        }
        fEmptyJavaDoc = false;
    }

    private boolean checkEmptyJavaDoc(String doc) {
        String[] splittedDoc = doc.split("/\\*+\\s*");
        String result = "";
        for (String s : splittedDoc) {
            result += s;
        }
        try {
            result = result.split("\\s*\\*/")[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            result = result.replace('/', ' ');
        }
        result = result.replace('*', ' ').trim();

        return !result.equals("");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(MethodDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        fInMethodDeclaration = true;

        //@Inria
        push(node, node.getName().toString());
        //

        visitList(EntityType.MODIFIERS, node.modifiers());
        if (node.getReturnType2() != null) {
            node.getReturnType2().accept(this);
        }
        visitList(EntityType.TYPE_ARGUMENTS, node.typeParameters());
        visitList(EntityType.PARAMETERS, node.parameters());
        visitList(EntityType.THROW, node.thrownExceptions());

        //@Inria
        //The body can be null when the method declaration is from a interface
        if(node.getBody()!= null){
        	node.getBody().accept(this);
        }
        return false;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(MethodDeclaration node) {
        fInMethodDeclaration = false;
       pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(Modifier node) {
        pushValuedNode(node, node.getKeyword().toString());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(Modifier node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(ParameterizedType node) {
        pushEmptyNode(node);
        node.getType().accept(this);
        visitList(EntityType.TYPE_ARGUMENTS, node.typeArguments());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(ParameterizedType node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(PrimitiveType node) {
        String vName = "";
        if (fInMethodDeclaration) {
            vName += getCurrentParent().getLabel()/*getCurrentParent().getValue()*/ + COLON_SPACE;
        }
        pushValuedNode(node, vName + node.getPrimitiveTypeCode().toString());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(PrimitiveType node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(QualifiedType node) {
        pushEmptyNode(node);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(QualifiedType node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(SimpleType node) {
        String vName = "";
        if (fInMethodDeclaration) {
            vName += getCurrentParent().getLabel() /*getCurrentParent().getValue()*/ + COLON_SPACE;
        }
        pushValuedNode(node, vName + node.getName().getFullyQualifiedName());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(SimpleType node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        boolean isNotParam = getCurrentParent().getLabel() != EntityType.PARAMETERS.toString();//@inria
        pushValuedNode(node, node.getName().getIdentifier());
        if (isNotParam) {
       //     visitList(EntityType.MODIFIERS, node.modifiers());
        }
        node.getType().accept(this);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(SingleVariableDeclaration node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeDeclaration node) {
        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }
        //@Inria
        push(node, node.getName().toString());
        //
        visitList(EntityType.MODIFIERS, node.modifiers());
        visitList(EntityType.TYPE_ARGUMENTS, node.typeParameters());
        if (node.getSuperclassType() != null) {
            node.getSuperclassType().accept(this);
        }

        visitList(EntityType.SUPER_INTERFACE_TYPES, node.superInterfaceTypes());

        //@Inria
        //Change Distiller does not check the changes at Class Field declaration
       for (FieldDeclaration fd: node.getFields()){
    	   fd.accept(this);
       }
       //@Inria
       //Visit Declaration and Body (inside MD visiting)
       for(MethodDeclaration md: node.getMethods()){
    	   md.accept(this);
       }
       return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(TypeDeclaration node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(TypeDeclarationStatement node) {
        // skip, only type declaration is interesting
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(TypeDeclarationStatement node) {
    // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(TypeLiteral node) {
        pushEmptyNode(node);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(TypeLiteral node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(TypeParameter node) {
        pushValuedNode(node, node.getName().getFullyQualifiedName());
        visitList(node.typeBounds());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(TypeParameter node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(VariableDeclarationExpression node) {
        pushEmptyNode(node);
        visitList(EntityType.MODIFIERS, node.modifiers());
        node.getType().accept(this);
        visitList(EntityType.FRAGMENTS, node.fragments());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(VariableDeclarationExpression node) {
        pop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(VariableDeclarationFragment node) {
        pushValuedNode(node, node.getName().getFullyQualifiedName());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(VariableDeclarationFragment node) {
        pop();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean visit(WildcardType node) {
        String bound = node.isUpperBound() ? "extends" : "super";
        pushValuedNode(node, bound);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVisit(WildcardType node) {
        pop();
    }

    private void visitList(List<ASTNode> list) {
        for (ASTNode node : list) {
            (node).accept(this);
        }
    }

    private void visitList(EntityType parentLabel, List<ASTNode> list) {
        int[] position = extractPosition(list);
        push(-parentLabel.ordinal(), parentLabel.name(), "", position[0], position[1]);
        if (!list.isEmpty()) {
        	//@Inria
        	//As ChangeDistiller has empty nodes e.g. Type Argument, Parameter, Thown,  the push and pop are before the empty condition check	
        	//push(-parentLabel.ordinal(),parentLabel.name(), "", position[0], position[1]);
        	visitList(list);
        	//pop();
        }
       pop();
    }

    private void pushEmptyNode(ASTNode node) {
    	//@Inria
    	//push(fASTHelper.convertNode(node), "", node.getStartPosition(), node.getLength());
    	push(node, "");
    }

    private void pushValuedNode(ASTNode node, String value) {
    	//@Inria
    	// push(fASTHelper.convertNode(node), value, node.getStartPosition(), node.getLength());
    	push(node, value);
    }


    private ITree root;

    public ITree getRoot(){
    	return root;
    };

    private void push(ASTNode node, String label) {
    	int type = node.getNodeType();
    	Tree t = new Tree(CdJdtTreeGenerator.class, type, label, node.getClass().getSimpleName());
		
		t.setPos(node.getStartPosition());
		t.setLength(node.getLength());

		if (root == null) root = t;
		else {
			//@Inria
			if(fNodeStack.isEmpty()){
				System.err.println("Stack is empty");
			}
			else{
			Tree parent = fNodeStack.peek();
			t.setParentAndUpdateChildren(parent);
			}
		}
        fNodeStack.push(t);
    }

 private void push(int nType, String type, String label, int startPosition, int length) {
    	
    	
    	Tree t = new Tree(CdJdtTreeGenerator.class, nType, label, type);
		
		t.setPos(startPosition);
		t.setLength(length);
		//@Inria
		if (root == null) root = t;
		else {
			if(fNodeStack.isEmpty()){
				System.err.println("Stack is empty");
			}
			else{
			Tree parent = fNodeStack.peek();
			t.setParentAndUpdateChildren(parent);
			}
		}
	    fNodeStack.push(t);
    }


    private void pop() {
        fNodeStack.pop();
    }

	private ASTNode fLastVisitedNode;
	private ITree fLastAddedNode;


	private void pop(ASTNode node) {
		fLastVisitedNode = node;
		fLastAddedNode = fNodeStack.pop();
	}

    private ITree getCurrentParent() {
        return fNodeStack.peek();
    }

    private int[] extractPosition(List<ASTNode> list) {
        int offset = -1;
        int length = -1;
        if (!list.isEmpty()) {
            ASTNode first = list.get(0);
            ASTNode last = list.get(list.size() - 1);
            offset = first.getStartPosition();
            length = last.getStartPosition() + last.getLength() - offset;
        }
        return new int[]{offset, length};
    }

  ///***************BODY VISITOR*************************
	private static final String COLON = ":";
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(AssertStatement node) {
		String value = node.getExpression().toString();
		if (node.getMessage() != null) {
			value += COLON + node.getMessage().toString();
		}
		pushValuedNode(node, value);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(AssertStatement node) {
		pop(node);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(BreakStatement node) {
		pushValuedNode(node, node.getLabel() != null ? node.getLabel().toString() : "");
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(BreakStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(CatchClause node) {
		pushValuedNode(node, ((SimpleType) node.getException().getType()).getName().getFullyQualifiedName());
		// since exception type is used as value, visit children by hand
		node.getBody().accept(this);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(CatchClause node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ConstructorInvocation node) {
		pushValuedNode(node, node.toString());
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ConstructorInvocation node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ContinueStatement node) {
		pushValuedNode(node, node.getLabel() != null ? node.getLabel().toString() : "");
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ContinueStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(DoStatement node) {
		pushValuedNode(node, node.getExpression().toString());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(DoStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(EmptyStatement node) {
		pushEmptyNode(node);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(EmptyStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(EnhancedForStatement node) {
		pushValuedNode(node, node.getParameter().toString() + COLON + node.getExpression().toString());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(EnhancedForStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ExpressionStatement node) {
		pushValuedNode(node.getExpression(), node.toString());
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ExpressionStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ForStatement node) {
		String value = "";
		if (node.getExpression() != null) {
			value = node.getExpression().toString();
		}
		pushValuedNode(node, value);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ForStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(IfStatement node) {
		String expression = node.getExpression().toString();
		push(node, expression/*, node.getStartPosition(), node.getLength()*/);
		if (node.getThenStatement() != null) {
			 push(EntityType.THEN_STATEMENT.ordinal(),EntityType.THEN_STATEMENT.name(), expression,
			 node.getThenStatement().getStartPosition(), node
			 .getThenStatement().getLength());
			node.getThenStatement().accept(this);
			 pop(node.getThenStatement());
		}
		if (node.getElseStatement() != null) {
			push(EntityType.ELSE_STATEMENT.ordinal(),EntityType.ELSE_STATEMENT.name(), expression, node.getElseStatement().getStartPosition(), node
					.getElseStatement().getLength());
			node.getElseStatement().accept(this);
			pop(node.getElseStatement());
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(IfStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(LabeledStatement node) {
		pushValuedNode(node, node.getLabel().getFullyQualifiedName());
		node.getBody().accept(this);
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(LabeledStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ReturnStatement node) {
		pushValuedNode(node, node.getExpression() != null ? node.getExpression().toString() : "");
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ReturnStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(SuperConstructorInvocation node) {
		pushValuedNode(node, node.toString());
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(SuperConstructorInvocation node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(SwitchCase node) {
		pushValuedNode(node, node.getExpression() != null ? node.getExpression().toString() : "default");
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(SwitchCase node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(SwitchStatement node) {
		pushValuedNode(node, node.getExpression().toString());
		visitList(node.statements());
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(SwitchStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(SynchronizedStatement node) {
		pushValuedNode(node, node.getExpression().toString());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(SynchronizedStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(ThrowStatement node) {
		pushValuedNode(node, node.getExpression().toString());
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(ThrowStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(TryStatement node) {
		pushEmptyNode(node);
		 push(EntityType.BODY.ordinal(), EntityType.BODY.name(), "", node.getBody().getStartPosition(),
		 node.getBody().getLength());
		node.getBody().accept(this);
		 pop(node.getBody());
		visitList(EntityType.CATCH_CLAUSES, node.catchClauses());
		if (node.getFinally() != null) {
			//@Inria
			push(EntityType.FINALLY.ordinal(), EntityType.FINALLY.name() ,"", node.getFinally().getStartPosition(), node.getFinally().getLength());
			node.getFinally().accept(this);
			pop(node.getFinally());
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(TryStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		pushValuedNode(node, node.toString());
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(VariableDeclarationStatement node) {
		pop(node);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean visit(WhileStatement node) {
		push(node, node.getExpression().toString()/*, node.getStartPosition(), node.getLength()*/);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endVisit(WhileStatement node) {
		pop(node);
	}
}
