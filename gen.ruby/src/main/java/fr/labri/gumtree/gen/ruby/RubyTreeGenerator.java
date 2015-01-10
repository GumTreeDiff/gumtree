/*
 * Copyright 2011 Jean-Rémy Falleri
 * 
 * This file is part of Praxis.
 * Praxis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Praxis is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Praxis.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.labri.gumtree.gen.ruby;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.jrubyparser.CompatVersion;
import org.jrubyparser.Parser;
import org.jrubyparser.ast.Node;
import org.jrubyparser.parser.ParserConfiguration;

import fr.labri.gumtree.io.TreeGenerator;
import fr.labri.gumtree.tree.Tree;

public class RubyTreeGenerator extends TreeGenerator {

	public Tree generate(String file) {
		Parser p = new Parser();
		try {
			FileReader f = new FileReader(file);
			CompatVersion version = CompatVersion.RUBY2_0;
			ParserConfiguration config = new ParserConfiguration(0, version);
			Node n = p.parse("<code>", f, config);
			return toTree(n, new HashMap<Node, Tree>());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Tree toTree(Node n, Map<Node, Tree> trees) {
		String label = "";
		String typeLabel = n.getNodeType().name();
		int type = n.getNodeType().ordinal() + 1;
		Tree t = new Tree(type, label, typeLabel);
		Tree p = null;
		if (n.getParent() != null)
			p = trees.get(n.getParent());
		t.setParentAndUpdateChildren(p);
		
		int pos = n.getPosition().getStartOffset();
		int length = n.getPosition().getEndOffset() - n.getPosition().getStartOffset();
		t.setPos(pos);
		t.setLength(length);
		
		trees.put(n, t);
		for(Node c: n.childNodes())
			toTree(c, trees);
		
		return t;
	}
	
	@Override
	public boolean handleFile(String file) {
		return file.toLowerCase().endsWith(".ruby") ||  file.toLowerCase().endsWith(".rb");
	}

	@Override
	public String getName() {
		return "ruby-jruby";
	}
}
