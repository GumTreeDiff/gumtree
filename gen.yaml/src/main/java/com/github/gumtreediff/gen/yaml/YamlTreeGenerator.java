/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2023 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.gen.yaml;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.utils.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.events.*;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.snakeyaml.engine.v2.parser.Parser;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;

import java.io.Reader;
import java.util.Stack;

import static com.github.gumtreediff.tree.TypeSet.type;

@Register(id = "yaml-snakeyaml", accept = {"\\.yml$", "\\.yaml$"}, priority = Registry.Priority.MAXIMUM)
public class YamlTreeGenerator extends TreeGenerator {
    @Override
    public TreeContext generate(Reader r) {
        TreeContext ctx = new TreeContext();
        try {
            LoadSettings settings = LoadSettings.builder().build();
            Parser p = new ParserImpl(settings, new StreamReader(settings, r));
            Tree root = ctx.createTree(type("YamlDocument"));
            Stack<Tree> stack = new Stack<>();
            stack.push(root);
            ctx.setRoot(root);
            while (p.hasNext()) {
                Event e = p.next();
                if (e.getEventId().equals(Event.ID.DocumentStart)) {
                    DocumentStartEvent de = (DocumentStartEvent) e;
                    root.setPos(de.getStartMark().get().getIndex());
                }
                else if (e.getEventId().equals(Event.ID.DocumentEnd)) {
                    stack.peek().setLength(e.getEndMark().get().getIndex() - stack.peek().getPos());
                    stack.pop();
                }
                else if (e.getEventId().equals(Event.ID.MappingStart)) {
                    MappingStartEvent me = (MappingStartEvent) e;
                    Tree mappingNode = ctx.createTree(type("YamlHash"));
                    mappingNode.setPos(me.getStartMark().get().getIndex());
                    mappingNode.setParentAndUpdateChildren(stack.peek());
                    stack.push(mappingNode);
                }
                else if (e.getEventId().equals(Event.ID.MappingEnd)) {
                    stack.peek().setLength(e.getEndMark().get().getIndex() - stack.peek().getPos());
                    stack.pop();
                    if (stack.peek().getType() == type("YamlTuple")) {
                        stack.peek().setLength(e.getEndMark().get().getIndex() - stack.peek().getPos());
                        stack.pop();
                    }
                }
                else if (e.getEventId().equals(Event.ID.SequenceStart)) {
                    SequenceStartEvent se = (SequenceStartEvent) e;
                    Tree sequenceNode = ctx.createTree(type("YamlSequence"));
                    sequenceNode.setPos(se.getStartMark().get().getIndex());
                    sequenceNode.setParentAndUpdateChildren(stack.peek());
                    stack.push(sequenceNode);
                }
                else if (e.getEventId().equals(Event.ID.SequenceEnd)) {
                    stack.peek().setLength(e.getEndMark().get().getIndex() - stack.peek().getPos());
                    stack.pop();
                    if (stack.peek().getType() == type("YamlTuple")) {
                        stack.peek().setLength(e.getEndMark().get().getIndex() - stack.peek().getPos());
                        stack.pop();
                    }
                }
                else if (e.getEventId().equals(Event.ID.Scalar)) {
                    ScalarEvent se = (ScalarEvent) e;
                    if (stack.peek().getType() == type("YamlHash")) {
                        Tree tupleNode = ctx.createTree(type("YamlTuple"));
                        tupleNode.setParentAndUpdateChildren(stack.peek());
                        tupleNode.setPos(se.getStartMark().get().getIndex());
                        stack.push(tupleNode);
                        Tree keyNode = ctx.createTree(type("YamlTupleKey"));
                        keyNode.setPos(se.getStartMark().get().getIndex());
                        keyNode.setLength(se.getEndMark().get().getIndex() - se.getStartMark().get().getIndex());
                        keyNode.setLabel(se.getValue());
                        keyNode.setParentAndUpdateChildren(stack.peek());
                    }
                    else {
                        Tree scalarNode = ctx.createTree(type("YamlValue"));
                        scalarNode.setPos(se.getStartMark().get().getIndex());
                        scalarNode.setLength(se.getEndMark().get().getIndex() - se.getStartMark().get().getIndex());
                        scalarNode.setLabel(se.getValue());
                        scalarNode.setParentAndUpdateChildren(stack.peek());
                        if (stack.peek().getType() == type("YamlTuple")) {
                            stack.peek().setLength(e.getEndMark().get().getIndex() - stack.peek().getPos());
                            stack.pop();
                        }
                    }
                }
            }
        }
        catch (YamlEngineException e) {
            throw new SyntaxException(this, r, e);
        }
        return ctx;
    }
}
